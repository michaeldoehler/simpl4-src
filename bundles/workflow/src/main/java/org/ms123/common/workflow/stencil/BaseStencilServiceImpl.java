/**
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
package org.ms123.common.workflow.stencil;

import flexjson.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.File;
import java.io.Reader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import org.ms123.common.store.StoreDesc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javassist.*;
import javassist.bytecode.*;
import static java.text.MessageFormat.format;
import org.ms123.common.libhelper.FileSystemClassLoader;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import static org.apache.commons.lang3.StringEscapeUtils.escapeJava;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.Event;
import net.sf.sojo.common.ObjectGraphWalker;
import net.sf.sojo.interchange.SerializerException;
import org.ms123.common.libhelper.Inflector;

/**
 *
 */
@SuppressWarnings("unchecked")
class BaseStencilServiceImpl {

	protected MetaData m_gitMetaData;
	protected EventAdmin m_eventAdmin;
	protected Inflector m_inflector = Inflector.getInstance();

	public static String CONVERTER_CLAZZTEMPLATE = "{0}.activiti.jsonconverter.{1}StencilJsonConverter";

	public static String CAMEL_CLAZZTEMPLATE = "{0}.camel.activitibehavior.{1}CamelBehavior";

	public static String CLASSDIR = "{0}/java/classes";

	public static String WORKSPACE = "workspace";

	public static String PROPERTIES = "properties";

	public static String ID = "id";

	public static String ROUTING = "routing";

	private ClassLoader m_classLoader = null;

	public Class getConverterClass(String namespace, String name) throws Exception {
		createClassLoader();
		String clazzName = format(CONVERTER_CLAZZTEMPLATE, namespace, m_inflector.capitalizeFirst(name));
		return m_classLoader.loadClass(clazzName);
	}

	public Class getCamelClass(String namespace, String name) throws Exception {
		createClassLoader();
		String clazzName = format(CAMEL_CLAZZTEMPLATE, namespace, m_inflector.capitalizeFirst(name));
		return m_classLoader.loadClass(clazzName);
	}

	public String getCamelClassName(String namespace, String name) {
		String clazzName = format(CAMEL_CLAZZTEMPLATE, namespace, m_inflector.capitalizeFirst(name));
		return clazzName;
	}

	public void _generateClasses(String namespace, List<String> stencilNameList) throws Exception {
		List<Map> stencilList = new ArrayList();
		if (stencilNameList != null) {
			for (String t : stencilNameList) {
				Map m = m_gitMetaData.getAddonStencil(namespace, t);
				stencilList.add(m);
			}
		} else {
			stencilList = m_gitMetaData.getAddonStencils(namespace);
		}
		for (Map<String, Object> stencil : stencilList) {
			String id = (String) stencil.get(ID);
			createConverterClass(namespace, id, (List<Map>) stencil.get(PROPERTIES));
			createCamelClass(namespace, id, stencil);
		}
		sendEvent("classes_generated", namespace);
	}

	/*CamelBeg*/
	private void createCamelClass(String namespace, String name, Map<String, Object> stencilConfig) {
		try {
			ClassPool cp = ClassPool.getDefault();
			String clazzName = getCamelClassName(namespace, name);
			CtClass ctCamel = cp.makeClass(clazzName);
			CtClass sc = cp.getCtClass("org.ms123.common.camel.components.BaseCamelBehavior");
			List<Map> fields = (List) stencilConfig.get(PROPERTIES);
			String routingDSL = (String) stencilConfig.get(ROUTING);
			for (Map<String, String> field : fields) {
				CtClass expr = cp.get("org.activiti.engine.delegate.Expression");
				ctCamel.addField(new CtField(expr, field.get(ID), ctCamel));
			}
			ctCamel.setSuperclass(sc);
			addGetParamsMethod(fields, ctCamel);
			addGetParameterNamesMethod(fields,ctCamel);
			addGetRoutingMethod(ctCamel);
			addRoutingDSLMethod(routingDSL,ctCamel);
			addAddRouteMethod(fields, ctCamel);
			ClassFile mClassFile = ctCamel.getClassFile();
			String ws = System.getProperty(WORKSPACE);
			File dest = new File(format(CLASSDIR, ws));
			ctCamel.writeFile(dest.toString());
			ctCamel.detach();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("BaseStencilServiceImpl.createCamelClass", e);
		}
	}
	private void addGetParameterNamesMethod(List<Map> fields,CtClass ctCamel) throws Exception {
		String m = "public java.util.List getParameterNames() {";

		m += "java.util.List paramNames = new java.util.ArrayList();";
		for (Map<String, String> field : fields) {
			String f = field.get(ID);
			m += "paramNames.add(\""+f+"\");";
		}
		m += "return paramNames;";
		m += "}";
		CtMethod me = CtNewMethod.make(m, ctCamel);
		ctCamel.addMethod(me);
	}


	private void addGetRoutingMethod(CtClass ctCamel) throws Exception {
		String m = "public java.lang.String getRouting() {";
		m += "Class clz = this.getClass();";
		m += "info(\"clz:\"+clz+\"/\"+this);";
		m += "try {";
		m += "	java.lang.reflect.Field f = clz.getDeclaredField(\""+ROUTING+"\");";
		m += "	info(\"hasRouting:yes\");";
		m += "	org.activiti.engine.delegate.Expression ex = (org.activiti.engine.delegate.Expression)f.get(this);";
		m += "	info(\"Expression:\"+ex);";
		m += "	return ex.getExpressionText();";
		m += "} catch ( NoSuchFieldException e) {";
		m += "	info(\"hasRouting:no1\");";
		m += "} catch (Exception e) {";
		m += "	info(\"getRouting.\"+e);";
		m += "	e.printStackTrace();";
		m += "}";
		m += "info(\"hasRouting:no2\");";
		m += "return null;";
		m += "}";
		CtMethod me = CtNewMethod.make(m, ctCamel);
		ctCamel.addMethod(me);
	}


	private void addRoutingDSLMethod(String routingDSL, CtClass ctCamel) throws Exception {
		String m = "public java.lang.String getRoutingDSL() {";

		m += "String r = getRouting();";
		m += "if(r != null) return org.apache.commons.lang3.StringEscapeUtils.escapeJava(r);";
		m += "java.lang.String dsl = \"" + escapeJava(routingDSL)+ "\";";
		m += "return dsl;";
		m += "}";
		debug("addRoutingDSLMethod:"+m);
		CtMethod me = CtNewMethod.make(m, ctCamel);
		ctCamel.addMethod(me);
	}
	private void addGetParamsMethod(List<Map> fields, CtClass ctCamel) throws Exception {
		String m = "public java.util.Map getParams(org.activiti.engine.impl.pvm.delegate.ActivityExecution execution) throws Exception {";

		m += "java.util.Map params = new java.util.HashMap();";
		for (Map<String, String> field : fields) {
			String f = field.get(ID);
			if( f.equals(ROUTING)){
				m += "params.put(\""+f+"\"," + f + ".getExpressionText());";
			}else{
				m += "params.put(\""+f+"\",getStringFromField(" + f + ",execution));";
			}
		}
		m += "return params;";
		m += "}";
		CtMethod me = CtNewMethod.make(m, ctCamel);
		ctCamel.addMethod(me);
	}

	private void addAddRouteMethod(List<Map> fields, CtClass ctCamel) throws Exception {
		String m = "public void addRoute(org.apache.camel.CamelContext cc, org.activiti.engine.impl.pvm.delegate.ActivityExecution execution) throws Exception {";

		m += "java.util.Map params = getParams(execution);";
		m += "params.putAll(getRoutingVariables());";
		m += "org.apache.camel.builder.RouteBuilder rb = newRouteBuilder();";
		m += "((org.ms123.common.workflow.stencil.StencilRouteBuilder)rb).init(params);";
		m += "cc.addRoutes(rb);";
		m += "}";
		CtMethod me = CtNewMethod.make(m, ctCamel);
		ctCamel.addMethod(me);
	}
	/*CamelEnd*/

	/*ConvertClassBeg*/
	private void createConverterClass(String namespace, String name, List<Map> fields) {
		try {
			ClassPool cp = ClassPool.getDefault();
			cp.insertClassPath(new ClassClassPath(BaseStencilJsonConverter.class));
			cp.insertClassPath(new ClassClassPath(org.activiti.bpmn.model.FlowElement.class));
			String clazzName = format(CONVERTER_CLAZZTEMPLATE, namespace, m_inflector.capitalizeFirst(name));
			CtClass ctConverter = cp.makeClass(clazzName);
			CtClass sc = cp.getCtClass(BaseStencilJsonConverter.class.getName());
			ctConverter.setSuperclass(sc);
			ctConverter.addMethod(getClazzMethod(ctConverter, getCamelClassName(namespace, name)));
			ctConverter.addMethod(getStencilIdMethod(ctConverter, name));
			ctConverter.addMethod(getCreateFieldsMethod(ctConverter, fields));
			ClassFile mClassFile = ctConverter.getClassFile();
			String ws = System.getProperty(WORKSPACE);
			File dest = new File(format(CLASSDIR, ws));
			ctConverter.writeFile(dest.toString());
			ctConverter.detach();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("BaseStencilServiceImpl.createConverterClass", e);
		}
	}

	private CtMethod getClazzMethod(CtClass ctConverter, String className) throws Exception {
		String m = "protected String getClassName(){ return \"" + className + "\"; }";
		return CtNewMethod.make(m, ctConverter);
	}

	private CtMethod getStencilIdMethod(CtClass ctConverter, String name) throws Exception {
		String m = "protected String getStencilId(org.activiti.bpmn.model.FlowElement flowElement){ return \"" + name + "\"; }";
		return CtNewMethod.make(m, ctConverter);
	}

	private CtMethod getCreateFieldsMethod(CtClass ctConverter, List<Map> fields) throws Exception {
		String m = "protected void createFields(java.util.Map properties,org.activiti.bpmn.model.ServiceTask stencil){";
		for (Map<String, String> field : fields) {
			String id = field.get(ID);
			m += "\taddField(\"" + id + "\", properties, stencil);";
		}
		m += "\taddField(\"variablesmapping\", properties, stencil);";
		m += "\taddField(\"resultvar\", properties, stencil);";
		m += "}";
		return CtNewMethod.make(m, ctConverter);
	}

	/*ConvertClassEnd*/


	/**
	 * getStencilSet
	 *
	 */
	public String _getStencilSet(String namespace, String name) throws Exception {
		JSONDeserializer ds = new JSONDeserializer();
		JSONSerializer js = new JSONSerializer();
		String gitSpace = System.getProperty("git.repos");
		File file = new File(gitSpace + "/global/stencilsets", name + ".json");
		InputStream is = new FileInputStream(file);
		Reader in = new InputStreamReader(is, "UTF-8");
		Map<String, List> ssMap = (Map) ds.deserialize(in);
		List<Map> stencilList = ssMap.get("stencils");
		Map<String,Object> definitions = (Map)ssMap.get("definitions");
		ssMap.remove("definitions");
		for (Map<String, Object> stencil : stencilList) {
			_handleProperties(name, stencil);
			stencil.put("view", m_gitMetaData.getStencilView(name, (String) stencil.get("view")));
			stencil.put("icon", m_gitMetaData.getStencilIcon(name, (String) stencil.get("icon")));
		}
		if( MetaData.PROCESS_SS.equals(name)){
			//List stencils = m_gitMetaData.getAddonStencils(namespace);
			//stencilList.addAll(stencils);
			//stencils = m_gitMetaData.getAddonStencils("global");
			//stencilList.addAll(stencils);
		}
		long start = new Date().getTime();
		String s = JsonFilterSerializer.toJson(ssMap,definitions);
		return s;
	}

	private void _handleProperties(String ssname, Map<String, Object> stencil) throws Exception {
		List<Map> properties = (List) stencil.get("properties");
		if (properties == null) {
			System.out.println("Stencil without properties:" + stencil);
			return;
		}
		for (Map<String, Object> prop : properties) {
			if ("Choice".equals(prop.get("type"))) {
				List<Map> items = (List) prop.get("items");
				for (Map item : items) {
					if (item.get("icon") != null) {
						item.put("icon", m_gitMetaData.getStencilIcon(ssname, (String) item.get("icon")));
					}
				}
			}
		}
	}



	private static class JsonFilterSerializer extends net.sf.sojo.interchange.json.JsonWalkerInterceptor {
		private JSONSerializer m_js = new JSONSerializer();
		private Map<String,Object> m_definitions;
		private JsonFilterSerializer(Map<String,Object> defs){
			m_definitions = defs;
		}

		private static String toJson(Object o,Map<String,Object> definitions) {
			ObjectGraphWalker walker = new ObjectGraphWalker();
			walker.setIgnoreNullValues(true);
			JsonFilterSerializer interceptor = new JsonFilterSerializer(definitions);
			walker.addInterceptor(interceptor);
			walker.walk(o);
			return interceptor.getJsonString();
		}

		public boolean visitElement(Object pvKey, int pvIndex, Object pvValue, int pvType, String pvPath, int pvNumberOfRecursion) {
			if (pvType == net.sf.sojo.core.Constants.TYPE_SIMPLE) {
				if (pvKey != null && pvKey.getClass().equals(String.class)) {
					jsonString.append(handleJsonValue(pvKey)).append(":");
				} else if (pvKey != null) {
					throw new SerializerException("JSON support only properties/keys from type String and not: '" + pvKey.getClass().getName() + "' (" + pvKey + ")");
				}
				if( pvValue instanceof String && ((String)pvValue).startsWith("###")){
					String defKey = ((String)pvValue).substring(3);
					if( m_definitions.get(defKey) != null){
						jsonString.append(m_js.deepSerialize( m_definitions.get(defKey))).append(",");
					}
				}else{
					jsonString.append(handleJsonValue(pvValue)).append(",");
				}
			} else if (pvType == net.sf.sojo.core.Constants.TYPE_NULL) {
				if (pvPath.endsWith(")")) {
					if (getWithNullValuesInMap()) {
						jsonString.append(handleJsonValue(pvKey)).append(":null,");
					}
				} else {
					jsonString.append("null,");
				}
			} else if (pvKey != null && pvValue != null) {
				if (pvKey != null && pvKey.getClass().equals(String.class)) {
					jsonString.append(handleJsonValue(pvKey)).append(":");
				} else {
					throw new SerializerException("JSON support only properties/keys from type String and not: '" + pvKey.getClass().getName() + "' (" + pvKey + ")");
				}
			}
			return false;
		}
	}


	private ClassLoader createClassLoader() {
		if (m_classLoader != null)
			return null;
		File[] locations = new File[1];
		String ws = System.getProperty(WORKSPACE);
		locations[0] = new File(format(CLASSDIR, ws));
		m_classLoader = new FileSystemClassLoader(org.ms123.common.workflow.stencil.BaseStencilServiceImpl.class.getClassLoader(), locations);
		return m_classLoader;
	}

	private void sendEvent(String what, String namespace) {
		Map props = new HashMap();
		props.put("namespace", namespace);
		info("StencilService.sendEvent.postEvent:" + m_eventAdmin);
		m_eventAdmin.sendEvent(new Event("stencil/" + what, props));
	}
	protected void debug(String msg) {
		m_logger.debug(msg);
	}

	protected void info(String msg) {
		System.out.println(msg);
		m_logger.info(msg);
	}

	private static final org.slf4j.Logger m_logger = org.slf4j.LoggerFactory.getLogger(BaseStencilServiceImpl.class);
}
