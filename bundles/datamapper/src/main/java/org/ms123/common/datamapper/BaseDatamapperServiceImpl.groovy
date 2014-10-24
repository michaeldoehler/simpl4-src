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
package org.ms123.common.datamapper;

import flexjson.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.Writer;
import java.io.ByteArrayOutputStream;
import org.ms123.common.data.api.DataLayer;
import org.ms123.common.utils.UtilsService;
import org.ms123.common.git.GitService;
import org.ms123.common.nucleus.api.NucleusService;
import org.ms123.common.reporting.ReportingService;
import org.ms123.common.data.api.SessionContext;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.utils.Inflector;
import org.ms123.common.permission.api.PermissionService;

import org.milyn.Smooks;
import org.milyn.SmooksException;
import org.milyn.event.report.HtmlReportGenerator;
import org.milyn.payload.JavaResult;
import org.milyn.javabean.*;
import org.milyn.GenericReaderConfigurator;
import org.milyn.io.StreamUtils;
import org.milyn.container.ExecutionContext;
import org.xml.sax.SAXException;
import flexjson.*;

import org.apache.camel.Exchange;
import org.apache.commons.beanutils.*;
import javax.xml.transform.stream.StreamSource;
import org.apache.camel.Handler;

import org.milyn.container.ExecutionContext;
import org.milyn.Smooks;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Source;
import javax.xml.transform.OutputKeys;
import org.milyn.payload.JavaSource;
import net.sf.sojo.common.ObjectUtil;
import net.sf.sojo.interchange.json.JsonSerializer;

/**
 *
 */
@groovy.transform.CompileStatic
@groovy.transform.TypeChecked
abstract class BaseDatamapperServiceImpl implements Constants,DatamapperService {

	protected Inflector m_inflector = Inflector.getInstance();

	protected JSONDeserializer m_ds = new JSONDeserializer();
	private JSONSerializer m_js = new JSONSerializer();

	protected DataLayer m_dataLayer;

	protected GitService m_gitService;
	private ObjectUtil m_objUtils = new ObjectUtil();

	protected PermissionService m_permissionService;

	protected UtilsService m_utilsService;
	protected ReportingService m_reportingService;
	protected NucleusService m_nucleusService;

	protected void info(String msg) {
		System.out.println(msg);
	}
	protected Bean createSmooksConfig(String namespace, String name) throws Exception {
		String json = m_gitService.searchContent(namespace, name, "sw.datamapper");
		Map mapping = (Map) m_ds.deserialize(json);
		return null;
	}

	@Handler
	public Object transform( String body, String configName, Exchange exchange) throws Exception{
		println("transform.body:"+body+"/"+configName);
		return transform(exchange.getContext().getRegistry().lookupByName("namespace") as String,null,configName,body);
	}

	public Object transform( String namespace, Map config, String configName, String data) throws Exception {
		return transform(namespace,config, configName, data,null);
	}
	public Object transform( String namespace, Map config, String configName, String data, BeanFactory bf) throws Exception {
		Map mconfig = config;
		if( namespace != null && configName != null){
			String json = m_gitService.searchContent(namespace, configName, "sw.datamapper");
			mconfig = (Map) m_ds.deserialize(json);
		}
		Transformer t = new Transformer(namespace,configName,m_nucleusService,bf);
		if( true){
			m_js.prettyPrint(true);
			Object ret = t.transform(mconfig,data);		
			if( ret == null) return null;
			if( ret instanceof Set){
				println("ret:"+((Set)ret).size());
			}
			if(((Map)mconfig.output).format == FORMAT_CSV){
				return toCSV((Collection<Map>)ret, mconfig);
			}
			if(((Map)mconfig.output).format == FORMAT_JSON){
				return toJSON(ret, mconfig);
			}
			if(((Map)mconfig.output).format == FORMAT_POJO){
				return ret;
			}
			if(((Map)mconfig.output).format == FORMAT_MAP){
				return ret;
			}
			if(((Map)mconfig.output).format == FORMAT_XML){
				return toXML(ret, mconfig);
			}
			return ret;
		}
	}
	private Object createBean(SessionContext sessionContext, String entityName) throws Exception {
		Class clazz = sessionContext.getClass(m_inflector.getClassName(entityName));
		Object bean = clazz.newInstance();
		/*Map defaults = getDefaults(entityName);
		if (defaults != null) {
			sessionContext.populate(defaults, bean);
		}*/
		return bean;
	}
	protected ClassLoader _setContextClassLoader(String ns) {
		ClassLoader saveCl = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(m_nucleusService.getClassLoader(StoreDesc.getNamespaceData(ns)));
		return saveCl;
	}
	private String toPOJO(String namespace,Object data, Map config){
		StoreDesc data_sdesc = StoreDesc.getNamespaceData(namespace);
		SessionContext sessionContext = m_dataLayer.getSessionContext(data_sdesc);
		Set<Map> tmpList;
		if( data instanceof Map){
			tmpList = new HashSet();
			tmpList.add( (Map) data);
		}else{
			tmpList = (Set)data;
		}
		Map output = config.output as Map;
		System.out.println("Name:"+output.name);	
		def retList = [];
		ClassLoader saveCl = _setContextClassLoader( namespace);
		try{
			for( Map m in tmpList){
				Object bean = createBean(sessionContext, output.name as String);
			println("m:"+m_js.deepSerialize(m));
	println("bean:"+bean+"/"+bean.getClass()+",m:"+m);
				 m_objUtils.makeComplex(m, bean);
					//jodd.bean.BeanUtilBean bub = new jodd.bean.BeanUtilBean();
					//bub.populateBean(bean,m);
				retList.add(bean);
			}
		}finally{
			Thread.currentThread().setContextClassLoader(saveCl);
		}
		println("retList:"+m_js.deepSerialize(retList));
		return m_js.deepSerialize(retList);;
	}
	private static String prettyXML(String input, int indent) {
		try {
			Source xmlInput = new StreamSource(new StringReader(input));
			StringWriter stringWriter = new StringWriter();
			StreamResult xmlOutput = new StreamResult(stringWriter);
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			transformerFactory.setAttribute("indent-number", indent);
			javax.xml.transform.Transformer transformer = transformerFactory.newTransformer(); 
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.transform(xmlInput, xmlOutput);
			return xmlOutput.getWriter().toString();
		} catch (Exception e) {
			throw new RuntimeException(e); // simple exception handling, please review it
		}
	}
	private String toXML(Object data, Map config){
		Map<String,String> output = (Map)config.output;
		Smooks smooks = new Smooks();
		GenericReaderConfigurator grc = new GenericReaderConfigurator(BeanReader.class);
		smooks.setReaderConfig(grc);
		try {
			ExecutionContext executionContext = smooks.createExecutionContext();
			if( output.type == NODETYPE_COLLECTION){
				executionContext.setAttribute("rootTag", output.name+"List");
			}else{
				executionContext.setAttribute("rootTag", output.name);
			}
			StringWriter writer = new StringWriter();
			smooks.filterSource(executionContext, new JavaSource(data), new StreamResult(writer));
			return prettyXML(writer.toString(),2);
		} finally {
			smooks.close();
		}
	}
	private String toJSON(Object data, Map config){
			return m_js.deepSerialize(data);
	}
	private String toCSV(Collection<Map> data, Map config){
		Map output = config.output as Map;
		List<Map> children = output.children as List;
		List<String> fieldList = new ArrayList();
		Map<String,String> datatypes = new HashMap();
		for( Map field in children){
			fieldList.add(field.name as String);
			datatypes.put(field.name as String, field.fieldType as String);
		}
		
		Map options = new HashMap();
		options.put("rowDelim", "UNIX");
		options.put("columnDelim", output.columnDelim as String);
		options.put("quote", output.quote as String);
		options.put("alwaysQuote", false);
		options.put("header", output.header);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		m_reportingService.createCSV(data, fieldList, null, datatypes,options,bos);
		return bos.toString();
	}
}
