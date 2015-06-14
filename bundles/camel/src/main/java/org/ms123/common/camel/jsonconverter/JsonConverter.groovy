/*
 * This file is part of SIMPL4(http://simpl4.org).
 *
 * 	Copyright [2014] [Manfred Sattler] <manfred@ms123.org>
 *
 * SIMPL4 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SIMPL4 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SIMPL4.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.ms123.common.camel.jsonconverter;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.RoutesDefinition;
import org.apache.camel.model.ExpressionSubElementDefinition;
import org.apache.camel.model.WhenDefinition;
import org.apache.camel.model.DataFormatDefinition;
import org.apache.camel.impl.DefaultRouteContext;
import org.apache.camel.Expression;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.Exchange;
import org.apache.camel.processor.aggregate.UseLatestAggregationStrategy;
import org.apache.camel.processor.aggregate.UseOriginalAggregationStrategy;
import org.apache.camel.processor.aggregate.GroupedExchangeAggregationStrategy;
import groovy.text.SimpleTemplateEngine;
import java.net.URLEncoder;
import org.apache.camel.model.language.*;
import org.apache.camel.util.IntrospectionSupport;
import static org.apache.camel.util.ObjectHelper.isEmpty;
import static org.apache.camel.util.ObjectHelper.isNotEmpty;
import org.ms123.common.libhelper.Utils;
import flexjson.*;
import java.util.Collection;
import javax.xml.namespace.QName;
import org.ms123.common.system.compile.java.JavaCompiler;
import org.osgi.framework.BundleContext;

class JsonConverterVisitor {
	def m_ctx;
	void visit(JsonConverter j) {
		j.convertToCamel(m_ctx);
		j.children.each { visit(it) }
	}
	void visit(MessageChoiceJsonConverter j) {
		j.convertToCamel(m_ctx);
		j.children.each { visit(it) }
		j.finishToCamel(m_ctx);
	}
	/*void visit(MessageAggregateJsonConverter j) {
		j.convertToCamel(m_ctx);
		j.children.each { visit(it) }
		j.finishToCamel(m_ctx);
	}*/
}

interface JsonConverter {
	def getChildren()
	void convertToCamel(ctx);
	void finishToCamel(ctx);
}

abstract class JsonConverterImpl implements JsonConverter{
	public def  rootProperties;
	public def  shapeProperties;
	public def  resourceId;
	public def  branding;
	public def  bundleContext;;
	def children = []
	def engine = new SimpleTemplateEngine();
	def serializer = new JSONSerializer();
	void finishToCamel(ctx){}
	def constructUri(ctx){
		def uriValueMap = createMap(ctx,"urivalue_");
		def uriParamMap = createMap(ctx,"uriparam_");
		println("uriValueMap:"+uriValueMap);
		println("uriParamMap:"+uriParamMap);
		def extraParams = shapeProperties.extraParams;
		if( extraParams != null && extraParams.totalCount>0){
			def items = extraParams.items;
			items.each(){item->
				if( isNotEmpty(item.name)  && isNotEmpty(item.value)){
					uriParamMap[item.name] = item.value;
				}
			}
		}
		def uri=shapeProperties.uri;
		if( shapeProperties.uri_template ){
			uri = engine.createTemplate(shapeProperties.uri_template).make(uriValueMap).toString();
		}
		uri = ctx.buildEnvSubstitutor.replace( uri );
		if( uriParamMap.size() > 0){
			def delim = "?";
			uriParamMap.each(){key,value->
				if( isNotEmpty(value)){
					uri+=delim+key+"=RAW("+value+")";
					delim = "&";
				}
			}
		}
		println("URI:"+uri);
		return uri;
	}
	def createExpression(exprText, language){
		def expr=null;
		if(language == "simple"){
			expr = org.apache.camel.builder.SimpleBuilder.simple(exprText);
		}else if(language == "constant"){
			expr = new ConstantExpression(exprText);
		}else if(language == "groovy"){
			expr = new GroovyExpression(exprText);
		}else if(language == "header"){
			expr = new HeaderExpression(exprText);
	//	}else if(language == "property"){
	//		expr = new PropertyExpression(exprText);
		}else if(language == "el"){
			expr = new ELExpression(exprText);
		}else if(language == "ognl"){
			expr = new OgnlExpression(exprText);
		}else if(language == "javascript"){
			expr = new JavaScriptExpression(exprText);
		}else if(language == "xpath"){
			expr = new XPathExpression(exprText);
		}else if(language == "sql"){
			expr = new SqlExpression(exprText);
		}else if(language == "mvel"){
			expr = new MvelExpression(exprText);
		}else if(language == "bean"){
			def m = exprText.split(",");
			if( m.length == 2){
				expr = new MethodCallExpression(m[0],m[1]);
			}else{
				expr = new MethodCallExpression(exprText);
			}
		}else if(language == "tokenize"){
			expr = new TokenizerExpression();
			expr.setToken(exprText);
		}else{
			expr = org.apache.camel.builder.SimpleBuilder.simple(exprText);
		}
		return expr;
	}
	
	def getDataformat(ctx){
		def format = shapeProperties.format;
		
		def map = createMap(ctx,format+"_");
		println("ParameterMap:"+map);
		def dataFormatDef = null;
		if( shapeProperties.json_library == "flexjson"){
			def ff = new org.ms123.common.camel.components.FlexJsonDataFormat();
			ff.setPrettyPrinting(shapeProperties.json_prettyPrint);
			dataFormatDef = new DataFormatDefinition(ff);	
		}else{
			dataFormatDef = new DataFormatDefinition(getFormatName());	
		}
		def routeContext = new DefaultRouteContext(ctx.modelCamelContext);
		def dataFormat = dataFormatDef.getDataFormat(routeContext);
		println("FormatName:"+getFormatName()+"="+dataFormat);
		IntrospectionSupport.setProperties(dataFormat,map);
		prettyPrint("dataFormatType",dataFormat);
		return dataFormat;
	}
	def getFormatName(){
		if( shapeProperties.format=='json'){
			return "json-"+shapeProperties.json_library;
		}
		return shapeProperties.format;
	}

	def createMap(ctx,prefix){
		def map=[:];
		shapeProperties.each(){key,value->
			if(key.startsWith(prefix)){
				if( value instanceof Map || value instanceof Collection){
					value = serializer.deepSerialize(value);
				}
				map[key.substring(prefix.length())] = value!=null ? ctx.buildEnvSubstitutor.replace(value.toString()) : "";
			}
		}	
		return map;
	}
	def createMapNoEmptyValues(prefix){
		def map=[:];
		shapeProperties.each(){key,value->
			if(key.startsWith(prefix) && isNotEmpty(value)){
				map[key.substring(prefix.length())] = value;
			}
		}	
		return map;
	}
	
	def createOptionMap(){
		def optionsMap = [:];
		def extraOptions = shapeProperties.extraOptions;
		if( extraOptions != null && extraOptions.totalCount>0){
			def items = extraOptions.items;
			items.each(){item->
				if( isNotEmpty(item.name)  && isNotEmpty(item.value)){
					optionsMap[item.name] = item.value;
				}
			}
		}
		optionsMap.putAll(createMapNoEmptyValues("option_"));
		return optionsMap;
	}

	def createProcessorGroovy(processMethodStr) {
		def code = buildScript(processMethodStr);
		System.out.println("Processor.Code.groovy:" + code);
		try {
			def gs = new GroovyShell();
			def clazz  = (Class) gs.evaluate(code);
			return clazz.newInstance();
		} catch (Throwable e) {
			String msg = Utils.formatGroovyException(e,code);
			throw new RuntimeException(msg);
		}
	}

	def createProcessorJava(processMethodStr) {
		def code = buildScript(processMethodStr);
		System.out.println("Processor.Code.java:" + code);
		try {
			def clazz = JavaCompiler.compile(bundleContext.getBundle(), "MyProcessor", code);
			return clazz.newInstance();
		} catch (Throwable e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	def getSharedLinkRef() {
		if( "link".equals(shapeProperties.get("shared"))){
			return shapeProperties.get("shareRef");
		}
		return null;
	}
	def getOrigLinkRef() {
		if( "origin".equals(shapeProperties.get("shared"))){
			return shapeProperties.get("shareRef");
		}
		return null;
	}

	def prettyPrint(msg, obj){
		def js = new JSONSerializer();
		js.prettyPrint(true);
		println(msg+js.serialize(obj));
	}

	def buildScript(processMethodStr) {
		def script = "import org.apache.camel.*\n;";
		script += "import org.apache.camel.impl.*\n;";
		script += "import org.apache.camel.builder.*\n;";
		script += "import org.apache.camel.model.dataformat.*\n;";
		script += "import org.ms123.common.data.api.SessionContext\n;";
		script += "import org.ms123.common.data.api.DataLayer\n;";
		script += "import org.ms123.common.git.GitService\n;";
		script += "import org.ms123.common.auth.api.AuthService\n;";
		script += "import org.ms123.common.nucleus.api.NucleusService\n;";
		script += "import org.ms123.common.store.StoreDesc\n;";
		script += "import org.ms123.common.permission.api.PermissionService\n;";
		script += "import org.ms123.common.team.api.TeamService\n;";
		script += "import org.ms123.common.system.thread.ThreadContext\n;";
		script += "import org.ms123.common.permission.api.PermissionException\n;";
		script += "import org.ms123.common.libhelper.Inflector\n;";
		script += "import java.util.*\n;";
		script += "import flexjson.JSONSerializer\n;";
		script += "import flexjson.JSONDeserializer\n;";
		script += "class MyProcessor implements Processor{\n;";
		script += processMethodStr;
		script += "}\n;";
		script += "return MyProcessor.class\n;";
		return script;
	}


	def getDefaultAggregationStrategy(key){
		if( key == "useLatest") return new UseLatestAggregationStrategy();
		if( key == "useOriginal") return new UseOriginalAggregationStrategy();
		if( key == "groupedExchange") return new GroupedExchangeAggregationStrategy();
		return null;
	}

	def setConstants(routeDefinition, properties){
		def constList = properties?.settings?.items;
		if( constList != null){
			for( def item : constList){
				def dest = item.destination;
				def constant = item["const"];
				def name = item.name;
				def source = item.source;
				if( isEmpty(constant) || isEmpty(name)) continue;
				if( "branding".equals(source)){
					constant = branding.get(name);
				}
				def constExpr = new ConstantExpression(constant as String);
				if( "property".equals(dest)){
					println("setting property:"+name+"="+constExpr);
					routeDefinition.setProperty(name, constExpr);
				}else{
					println("setting header:"+name+"="+constExpr);
					routeDefinition.setHeader(name, constExpr);
				}
			}
		}
	}
}

class OnExceptionJsonConverter extends JsonConverterImpl{
	void convertToCamel(ctx){
		Set exList = [];
		def items = shapeProperties.exceptions?.items;
		if( items!=null){
			for( def item : items){
				try{
					exList.add(Class.forName(item.exception));	
				}catch(Exception e){
					throw new RuntimeException("JsonConverter.OnException:cannot convert \""+item.exception+"\" to a ExceptionClass");
				}
			}
		}
		if( exList.size()==0){
			exList.add(java.lang.Exception.class);
		}
		RouteDefinition rd = ctx.routesDefinition.getRoutes().get(0);
		ctx.current = rd.onException(exList  as Class[]);
		ctx.current.id(resourceId);
		if(isNotEmpty(shapeProperties.continued)){
			ctx.current.setContinued(new ExpressionSubElementDefinition((Expression)createExpression(shapeProperties.continued,shapeProperties.continuedLanguage)));
		}
		if(isNotEmpty(shapeProperties.handled)){
			ctx.current.setHandled(new ExpressionSubElementDefinition((Expression)createExpression(shapeProperties.handled,shapeProperties.handledLanguage)));
		}
		if(isNotEmpty(shapeProperties.retryWhile)){
			ctx.current.setRetryWhile(new ExpressionSubElementDefinition((Expression)createExpression(shapeProperties.retryWhile,shapeProperties.retryWhileLanguage)));
		}
	}
}
class OnCompletionJsonConverter extends JsonConverterImpl{
	void convertToCamel(ctx){
		RouteDefinition rd = ctx.routesDefinition.getRoutes().get(0);
		ctx.current = rd.onCompletion();
		ctx.current.id(resourceId);
	}
}

class EndpointJsonConverter extends JsonConverterImpl{
	void convertToCamel(ctx){
		def link = getSharedLinkRef();
		if( link == null){
			link = getOrigLinkRef();
		}
		def sharedEndpoint = null;
		if( link != null){
			sharedEndpoint = ctx.sharedEndpoints[link];
			if( sharedEndpoint == null) throw new RuntimeException("EndpointJsonConverter:sharedEnpoint("+link+") not found.");
		}
		if( ctx.routesDefinition == null){
			ctx.routesDefinition = new RoutesDefinition();
			ctx.routesDefinition.setCamelContext( ctx.modelCamelContext);
			def routeDefinition = ctx.routesDefinition.from(sharedEndpoint ? sharedEndpoint : constructUri(ctx));
			setConstants(routeDefinition, rootProperties);
			ctx.current = routeDefinition;
			ctx.current.getInputs().get(0).id(resourceId);
			ctx.routeStart = false;
		}else if(ctx.routeStart==true){
			ctx.routeStart = false;
			ctx.current = ctx.routesDefinition.from(sharedEndpoint ? sharedEndpoint : constructUri(ctx));
			//ctx.current.getInputs().get(0).id(resourceId);
		}else{
			ctx.current = ctx.current.to(sharedEndpoint ? sharedEndpoint : constructUri(ctx));
			ctx.current.id(resourceId);
		}
	}
}

class FileEndpointJsonConverter extends EndpointJsonConverter{
	void convertToCamel(ctx){
		super.convertToCamel(ctx);
	}
}

class DirectEndpointJsonConverter extends EndpointJsonConverter{
	void convertToCamel(ctx){
		super.convertToCamel(ctx);
	}
}

class WebsocketEndpointJsonConverter extends EndpointJsonConverter{
	void convertToCamel(ctx){
		super.convertToCamel(ctx);
	}
}

class EventBusEndpointJsonConverter extends EndpointJsonConverter{
	void convertToCamel(ctx){
		super.convertToCamel(ctx);
	}
}

class SedaEndpointJsonConverter extends EndpointJsonConverter{
	void convertToCamel(ctx){
		super.convertToCamel(ctx);
	}
}

class WampEndpointJsonConverter extends EndpointJsonConverter{
	void convertToCamel(ctx){
		super.convertToCamel(ctx);
	}
}

class VMEndpointJsonConverter extends EndpointJsonConverter{
	void convertToCamel(ctx){
		super.convertToCamel(ctx);
	}
}

class FtpEndpointJsonConverter extends EndpointJsonConverter{
	void convertToCamel(ctx){
		super.convertToCamel(ctx);
	}
}

class SqlEndpointJsonConverter extends EndpointJsonConverter{
	void convertToCamel(ctx){
		super.convertToCamel(ctx);
	}
}

class RepoEndpointJsonConverter extends EndpointJsonConverter{
	void convertToCamel(ctx){
		super.convertToCamel(ctx);
	}
}

class XDocReportEndpointJsonConverter extends EndpointJsonConverter{
	void convertToCamel(ctx){
		super.convertToCamel(ctx);
	}
}

class GroovyTemplateEndpointJsonConverter extends EndpointJsonConverter{
	void convertToCamel(ctx){
		super.convertToCamel(ctx);
	}
}

class AsciidoctorEndpointJsonConverter extends EndpointJsonConverter{
	void convertToCamel(ctx){
		super.convertToCamel(ctx);
	}
}

class HttpClientEndpointJsonConverter extends EndpointJsonConverter{
	void convertToCamel(ctx){
		super.convertToCamel(ctx);
	}
}

class MailEndpointJsonConverter extends EndpointJsonConverter{
	void convertToCamel(ctx){
		super.convertToCamel(ctx);
	}
}

class XmppEndpointJsonConverter extends EndpointJsonConverter{
	void convertToCamel(ctx){
		super.convertToCamel(ctx);
	}
}

class JmsEndpointJsonConverter extends EndpointJsonConverter{
	void convertToCamel(JsonConverterContext ctx){
		super.convertToCamel(ctx);
	}
}
class DatabaseEndpointJsonConverter extends EndpointJsonConverter{
	void convertToCamel(JsonConverterContext ctx){
		super.convertToCamel(ctx);
	}
}

class DelayJsonConverter extends JsonConverterImpl{
	void convertToCamel(ctx){
		String charset = shapeProperties.charset!= null ? shapeProperties.charset : "utf-8";
		def expr = createExpression(shapeProperties.expression, shapeProperties.language);
		ctx.current = ctx.current.delay(expr);
		ctx.current.id(resourceId);
	}
}

class TransactedJsonConverter extends JsonConverterImpl{
	void convertToCamel(ctx){
		ctx.current = ctx.current.transacted(shapeProperties.propagationBehavior);
		ctx.current.id(resourceId);
	}
}

class RollbackJsonConverter extends JsonConverterImpl{
	void convertToCamel(ctx){
		ctx.current = ctx.current.rollback();
		ctx.current.id(resourceId);
		if( shapeProperties.markRollbackOnly){
			ctx.current = ctx.current.markRollbackOnly();
		}
		if( shapeProperties.markRollbackOnlyLast){
			ctx.current = ctx.current.markRollbackOnlyLast();
		}
	}
}

class ConvertBodyToJsonConverter extends JsonConverterImpl{
	void convertToCamel(ctx){
		String charset = shapeProperties.charset!= null ? shapeProperties.charset : "utf-8";
		Class type = null;
		try{
			type = Class.forName(shapeProperties.type!=null ? shapeProperties.type : "java.lang.String");
		}catch(Exception e){
			throw new RuntimeException("ConvertBodyToJsonConverter:Class.forName("+shapeProperties.type+"):"+e.getMessage());
		}
		ctx.current = ctx.current.convertBodyTo(type,charset);
		ctx.current.id(resourceId);
	}
}

class MarshalJsonConverter extends JsonConverterImpl{
	void convertToCamel(ctx){
		ctx.current = ctx.current.marshal(getDataformat(ctx));
		ctx.current.id(resourceId);
	}
}
class UnmarshalJsonConverter extends JsonConverterImpl{
	void convertToCamel(ctx){
		ctx.current = ctx.current.unmarshal(getDataformat(ctx));
		ctx.current.id(resourceId);
	}
}

class SimpleConnectionJsonConverter extends JsonConverterImpl{
	void convertToCamel(ctx){
	}
}

class MessageChoiceJsonConverter extends JsonConverterImpl{
	void convertToCamel(ctx){
		ctx.current = ctx.current.choice();
		ctx.current.setId(resourceId);
		//def cb = new JsonConverterContext.ChoiceBlock();
		//ctx.choiceStack.push(cb);
	}
	void finishToCamel(ctx){
		ctx.current = ctx.current.endChoice();
		//def cb = ctx.choiceStack.pop();
	}
}

class WhenConnectionJsonConverter extends JsonConverterImpl{
	void convertToCamel(ctx){
		if( shapeProperties.defaultflow == "none"){
			ctx.current = ctx.current.when(createExpression(shapeProperties.expression,shapeProperties.language));
			def whenList = ctx.current.getWhenClauses();
			whenList[whenList.size()-1].id(resourceId);
		}else{
			ctx.current = ctx.current.otherwise();
			ctx.current.getOtherwise().id(resourceId);
		}
	}
}

class MessageFilterJsonConverter extends JsonConverterImpl{
	void convertToCamel(ctx){
		ctx.current = ctx.current.filter(createExpression(shapeProperties.expression,shapeProperties.language));
		def options = createOptionMap();
		IntrospectionSupport.setProperties(ctx.current,options);
		prettyPrint("FilterDefinition:", ctx.current);
		ctx.current.id(resourceId);
	}
}

class RecipientListJsonConverter extends JsonConverterImpl{
	void convertToCamel(ctx){
		ctx.current = ctx.current.recipientList(createExpression(shapeProperties.expression,shapeProperties.language));
		def options = createOptionMap();
		println("options:"+options);
		IntrospectionSupport.setProperties(ctx.current,options);
		prettyPrint("RecipientListDefinition:", ctx.current);
		ctx.current.id(resourceId);
	}
}

class MessageAggregateJsonConverter extends JsonConverterImpl{
	void convertToCamel(ctx){
		if( isEmpty(shapeProperties.option_strategyRef)){
			ctx.current = ctx.current.aggregate(createExpression(shapeProperties.correlationExpression,shapeProperties.correlationLanguage), getDefaultAggregationStrategy(shapeProperties.defaultAggregationStrategy));
		}else{
			ctx.current = ctx.current.aggregate(createExpression(shapeProperties.correlationExpression,shapeProperties.correlationLanguage));
		}

		if(isNotEmpty(shapeProperties.completionPredicate)){
			ctx.current.setCompletionPredicate(new ExpressionSubElementDefinition((Predicate)createExpression(shapeProperties.completionPredicate,shapeProperties.completionLanguage)));
		}
		if(isNotEmpty(shapeProperties.completionSizeExpression)){
			ctx.current.setCompletionSizeExpression(new ExpressionSubElementDefinition((Expression)createExpression(shapeProperties.completionSizeExpression,shapeProperties.completionSizeLanguage)));
		}
		if(isNotEmpty(shapeProperties.completionTimeoutExpression)){
			ctx.current.setCompletionTimeoutExpression(new ExpressionSubElementDefinition((Expression)createExpression(shapeProperties.completionTimeoutExpression,shapeProperties.completionTimeoutLanguage)));
		}
		def options = createOptionMap();
		println("MessageAggregateJsonConverter:"+options);
		IntrospectionSupport.setProperties(ctx.current,options);
		//prettyPrint("AggregateDefinition:", ctx.current);
		ctx.current.id(resourceId);
	}
	void finishToCamel(ctx){
		ctx.current = ctx.current.end();
	}
}

class MessageSplitterJsonConverter extends JsonConverterImpl{
	void convertToCamel(ctx){
		ctx.current = ctx.current.split(createExpression(shapeProperties.expression,shapeProperties.language));
		def options = createOptionMap();
		IntrospectionSupport.setProperties(ctx.current,options);
		//prettyPrint("SplitDefinition:", ctx.current);
		if( shapeProperties.loggingOff == true){
			ctx.current.setProperty( "__loggingOff",new ConstantExpression("true"));
		}
		ctx.current.id(resourceId);
	}
}

class SetHeaderJsonConverter extends JsonConverterImpl{
	void convertToCamel(ctx){
		def items = shapeProperties?.headers?.items;
		if( items != null){
			items.each(){item->
				def expr = createExpression(item.value, item.language);
				ctx.current = ctx.current.setHeader(item.name,expr);
			}
		}
		ctx.current.id(resourceId);
	}
}

class SetPropertyJsonConverter extends JsonConverterImpl{
	void convertToCamel(ctx){
		def items = shapeProperties?.properties?.items;
		if( items != null){
			items.each(){item->
				def expr = createExpression(item.value, item.language);
				ctx.current = ctx.current.setProperty(item.name,expr);
			}
		}
		ctx.current.id(resourceId);
	}
}
class ProcessorJsonConverter extends JsonConverterImpl{
	void convertToCamel(ctx){
		def code = shapeProperties.code;
		def codeLanguage = shapeProperties.codeLanguage;
		def isEndpoint = shapeProperties.isEndpoint;
		def ref = shapeProperties.ref;
		if( isNotEmpty(ref)){
			if( isEndpoint){
				ctx.current = ctx.current.to(ref);
			}else{
				ctx.current = ctx.current.processRef(ref);
			}
		}else if( code != null && code.length()> 10){
			def processor=null;
			if( "java".equals(codeLanguage)){
				processor = createProcessorJava(code);
			}else{
				processor = createProcessorGroovy(code);
			}
			println("processor:"+processor);
			ctx.current = ctx.current.process(processor);
		}
		ctx.current.id(resourceId);
	}
}

class DatamapperJsonConverter extends JsonConverterImpl{
	void convertToCamel(ctx){
		def strategy = shapeProperties.strategy;
		if( isNotEmpty(strategy)){
			def processor = new Processor(){
				public void process(Exchange ex){
					def dm = ctx.modelCamelContext.getRegistry().lookupByName("datamapper");
					System.out.println("DataMapper.process:"+strategy+"/"+dm);
					def answer = dm.transform(ex.getIn().getBody(),strategy, ex);
					System.out.println("DataMapper.answer:"+answer);
					ex.getIn().setBody(answer);
				}
				public String toString(){
					return "Datamapper:"+strategy;
				}
			};
			println("processor:"+processor);
			ctx.current = ctx.current.process(processor);
			ctx.current.id(resourceId);
		}
	}
}
