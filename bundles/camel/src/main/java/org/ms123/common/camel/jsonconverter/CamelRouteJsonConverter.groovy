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

import flexjson.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.ms123.common.utils.Utils;

import org.apache.camel.CamelContext;
import org.apache.camel.Producer;
import org.apache.camel.Endpoint;
import org.apache.camel.Route;
import org.apache.camel.Exchange;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.view.RouteDotGenerator;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.CamelContext;
/**
 */
class CamelRouteJsonConverter extends BaseRouteJsonConverter implements Constansts{
	def m_path;
	def m_ctx;
	def m_typesMap = [:];
	def m_shapeMap = [:];
	CamelRouteJsonConverter(String path, ModelCamelContext camelContext, Map rootShape) {
		m_path = path;
		m_ctx = new JsonConverterContext();
		m_ctx.modelCamelContext = camelContext;
		fillShapeMap(rootShape);
		fillTypesMap();
		def startList = getStartList(rootShape);
		for( def startShape : startList){
			def converter = m_typesMap[getStencilId(startShape)];
			if( converter == null){
				throw new RuntimeException("No converter for StencilId:"+getStencilId(startShape));
			}
			def startJsonConverter = converter.newInstance(shapeProperties:startShape.properties,resourceId:getId(startShape));
			createConverterGraph(startJsonConverter, startShape);
			new JsonConverterVisitor(m_ctx:m_ctx).visit(startJsonConverter);
		}
		m_ctx.routeDefinition.routeId( getId(rootShape));
		println("RouteDefinition:"+m_ctx.routeDefinition);
	}
	private def getStartList(Map rootShape) {
		def outgoings =[];
		rootShape.childShapes.each(){shape->
			shape.outgoing.each() { out ->
				outgoings.add(out.resourceId);
			}
		}
		def startList=[];
		rootShape.childShapes.each(){shape->
			if( !outgoings.contains(shape.resourceId) && shape.outgoing.size()>0){
				startList.add(shape);
			}
		}
		if( isStartShapeListOk(startList)){
			sortStartShapeList(startList);
			def ids = []; startList.each(){ f -> ids.add(getStencilId(f)); } println("startList:"+ids);
			return startList;
		}
		if( startList.size() == 0) throw new RuntimeException("CamelRouteJsonConverter("+m_path+"):no From Block");
		if( startList.size() == 1){
			throw new RuntimeException("CamelRouteJsonConverter("+m_path+"):no From Block, only a onException Block");
		}

		def ids = [];
		startList.each(){ f ->
			ids.add(getStencilId(f));
		}
		throw new RuntimeException("CamelRouteJsonConverter("+m_path+"):more as one From,OnException or OnCompletion Block:"+ids.join(","));
	}
	private void fillShapeMap(Map shape) {
		m_shapeMap[shape.resourceId] = shape;
		for (Map childShape : shape.childShapes) {
			fillShapeMap(childShape);
		}
	}
	private void fillTypesMap(){
		m_typesMap["onexception"] = OnExceptionJsonConverter.class;
		m_typesMap["oncompletion"] = OnCompletionJsonConverter.class;
		m_typesMap["endpoint"] = EndpointJsonConverter.class;
		m_typesMap["fileendpoint"] = FileEndpointJsonConverter.class;
		m_typesMap["directendpoint"] = DirectEndpointJsonConverter.class;
		m_typesMap["sedaendpoint"] = SedaEndpointJsonConverter.class;
		m_typesMap["vmendpoint"] = VMEndpointJsonConverter.class;
		m_typesMap["mailendpoint"] = MailEndpointJsonConverter.class;
		m_typesMap["ftpendpoint"] = FtpEndpointJsonConverter.class;
		m_typesMap["httpclientendpoint"] = HttpClientEndpointJsonConverter.class;
		m_typesMap["jmsendpoint"] = JmsEndpointJsonConverter.class;
		m_typesMap["databaseendpoint"] = DatabaseEndpointJsonConverter.class;
		m_typesMap["simpleconnection"] = SimpleConnectionJsonConverter.class;
		m_typesMap["whenconnection"] = WhenConnectionJsonConverter.class;
		m_typesMap["setheader"] = SetHeaderJsonConverter.class;
		m_typesMap["setproperty"] = SetPropertyJsonConverter.class;
		m_typesMap["convertbodyto"] = ConvertBodyToJsonConverter.class;
		m_typesMap["delay"] = DelayJsonConverter.class;
		m_typesMap["transacted"] = TransactedJsonConverter.class;
		m_typesMap["rollback"] = RollbackJsonConverter.class;
		m_typesMap["processor"] = ProcessorJsonConverter.class;
		m_typesMap["datamapper"] = DatamapperJsonConverter.class;
		m_typesMap["messagechoice"] = MessageChoiceJsonConverter.class;
		m_typesMap["messagefilter"] = MessageFilterJsonConverter.class;
		m_typesMap["messageaggregate"] = MessageAggregateJsonConverter.class;
		m_typesMap["messagesplitter"] = MessageSplitterJsonConverter.class;
		m_typesMap["recipientlist"] = RecipientListJsonConverter.class;
		m_typesMap["marshal"] = MarshalJsonConverter.class;
		m_typesMap["unmarshal"] = UnmarshalJsonConverter.class;
	}
	private void createConverterGraph(JsonConverter jsonConverter, Map shape) throws Exception {
		def outgoing = shape.outgoing;
		for (def out : outgoing) {
			def childShape = m_shapeMap[out.resourceId];
			def id = getStencilId(childShape);
			def converter = m_typesMap[id];
			if( converter == null){
				throw new RuntimeException(m_path+":No converter for StencilId:"+id);
			}
			def childJsonConverter = converter.newInstance(shapeProperties:childShape.properties, resourceId:getId(childShape));
			jsonConverter.children.add(childJsonConverter);
			createConverterGraph(childJsonConverter, childShape);
		}

	}
	public RouteDefinition getRouteDefinition() {
		return m_ctx.routeDefinition;
	}

	public void toDot(){
		RouteDotGenerator generator = new RouteDotGenerator("/tmp/camel");
		CamelContext cc = new DefaultCamelContext();
		cc.addRouteDefinition(m_ctx.routeDefinition);
		String text = generator.getRoutesText(cc);
		System.out.println("Text:" + text);
	}
	public static void main(String[] args) {
		def fileContents = new File('/tmp/json.camel').text
		def ds = new JSONDeserializer();
		def shape = ds.deserialize(fileContents);
		new CamelRouteJsonConverter(shape);
	}
}
