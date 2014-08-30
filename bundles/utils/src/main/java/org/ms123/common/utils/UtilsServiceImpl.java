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
package org.ms123.common.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Collections;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.lang.reflect.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.osgi.framework.BundleContext;
import java.lang.reflect.Field;
import org.apache.commons.beanutils.BeanMap;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Element;
import javax.jdo.annotations.PrimaryKey;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import javax.script.ScriptEngineManager;
import aQute.bnd.annotation.metatype.*;
import aQute.bnd.annotation.component.*;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import java.io.*;
import javax.script.*;
import java.util.zip.*;
import com.Ostermiller.util.*;
import org.ms123.common.data.api.DataLayer;
import org.ms123.common.store.StoreDesc;
import javax.servlet.http.*;
import org.ms123.common.rpc.PName;
import org.ms123.common.rpc.POptional;
import org.ms123.common.rpc.RpcException;
import org.mvel2.MVEL;
import static org.ms123.common.rpc.JsonRpcServlet.ERROR_FROM_METHOD;
import static org.ms123.common.rpc.JsonRpcServlet.INTERNAL_SERVER_ERROR;
import static org.ms123.common.rpc.JsonRpcServlet.PERMISSION_DENIED;

/** UtilsService implementation
 */
@SuppressWarnings("unchecked")
@Component(enabled = true, configurationPolicy = ConfigurationPolicy.optional, immediate = true, properties = { "rpc.prefix=utils" })
public class UtilsServiceImpl implements UtilsService {

	private static final Logger m_logger = LoggerFactory.getLogger(UtilsServiceImpl.class);

	protected Inflector m_inflector = Inflector.getInstance();

	private ScriptEngineService m_scriptEngineService;
	private BundleContext m_bundleContext;

	protected DataLayer m_dataLayer;
	protected Method m_getClassMethod;
	protected Object m_nucleusServiceObject;


	public UtilsServiceImpl() {
		m_logger.info("UtilsServiceImpl construct");
	}

	protected void activate(BundleContext bundleContext, Map<?, ?> props) {
		System.out.println("UtilsServiceImpl.activate.props:" + props);
		try {
			m_logger.info("UtilsServiceImpl.activate -->");
			m_bundleContext = bundleContext;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void update(Map<String, Object> props) {
		System.out.println("UtilsServiceImpl.updated:" + props);
	}

	protected void deactivate() throws Exception {
		m_logger.info("deactivate");
		System.out.println("deactivate");
	}

	public List<Map> mapToList(Map<String, Map> map, Map mapping, String filter) {
		List<Map> retList = new ArrayList();
		Iterator<Map> it = map.values().iterator();
		while (it.hasNext()) {
			Map m = it.next();
			if (filter != null && filter.length() > 0) {
				boolean ok = _isOk(m, filter);
				if (!ok) {
					continue;
				}
			}
			if (mapping != null) {
				retList.add(mapValues(m, mapping));
			} else {
				retList.add(m);
			}
		}
		return retList;
	}

	public void sortListByField(List<Map> list, String sortField) {
		Collections.sort(list, new ListSortByField(sortField));
	}

	private class ListSortByField implements Comparator<Map> {

		String[] m_field;

		public ListSortByField(String field) {
			m_field = field.split(",");
		}

		public int compare(Map o1, Map o2) {
			if (m_field.length == 1) {
				String field = m_field[0];
				return compareTo(o1.get(field), o2.get(field));
			} else {
				String field1 = m_field[0];
				int res = compareTo(o1.get(field1), o2.get(field1));
				if (res == 0) {
					String field2 = m_field[1];
					return compareTo(o1.get(field2), o2.get(field2));
				} else {
					return res;
				}
			}
		}
	}

	private int compareTo(Object o1, Object o2) {
		if (o1 == null || o2 == null) {
			return 0;
		}
		if (o1 instanceof Integer) {
			int s1 = (Integer) o1;
			int s2 = (Integer) o2;
			return s1 - s2;
		} else if (o1 instanceof Boolean) {
			boolean s1 = (Boolean) o1;
			boolean s2 = (Boolean) o2;
			return s1 == s2 ? 0 : (s1 ? 1 : -1);
		} else if (o1 instanceof String) {
			String s1 = (String) o1;
			String s2 = (String) o2;
			return s1.compareTo(s2);
		}
		return 0;
	}

	public List<Map> listToList(List list, Map mapping, String filter) {
		return listToList(list,mapping,filter,false);
	}
	public List<Map> listToList(List list, Map mapping, String filter,boolean checkValid) {
		List<Map> retList = new ArrayList();
		Iterator it = list.iterator();
		while (it.hasNext()) {
			Map m = (Map) it.next();
			if(checkValid){
				boolean isValid = _isValid(m);
				if (!isValid) {
					continue;
				}
			}
			if (filter != null && filter.length() > 0) {
				boolean ok = _isOk(m, filter);
				if (!ok) {
					continue;
				}
			}
			if (mapping != null) {
				retList.add(mapValues(m, mapping));
			} else {
				retList.add(m);
			}
		}
		return retList;
	}

	protected boolean _isOk(Map<String, Object> props, String filter) {
		try {
			return MVEL.evalToBoolean(filter, props);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	protected boolean _isValid(Map<String, Object> props) {
		return !getBoolean(props.get("invalid")) && dateValid(props.get("invalid_from"));
	}

	public Map mapValues(Map props, Map<String, String> mapping) {
		Iterator<String> it = mapping.keySet().iterator();
		Map retMap = new HashMap();
		m_logger.debug("props:" + props);
		while (it.hasNext()) {
			String key = it.next();
			String val = mapping.get(key);
			if ("_all_".equals(key)) {
				retMap.putAll(props);
			} else if (val == null) {
				retMap.put(key, props.get(key));
			} else {
				if (val.startsWith("(")) {
					int len = val.length();
					val = val.substring(1, len - 1);
					try {
						props.put("inflector", m_inflector);
						val = MVEL.evalToString(val, props);
						props.remove("inflector");
					} catch (Exception e) {
						e.printStackTrace();
					}
					retMap.put(key, val);
				} else {
					if( val.indexOf("${") != -1){
					retMap.put(key, expandString(val,props));
					}else{
						retMap.put(key, props.get(val));
					}
				}
			}
		}
		return retMap;
	}

	public static  Map copyObject(Object o) throws Exception{
		Map n = new HashMap();
		BeanMap beanMap = new BeanMap(o);
		Iterator itv = beanMap.keyIterator();
		while (itv.hasNext()) {
			String prop = (String) itv.next();
			if ("class".equals(prop)) {
				continue;
			}
			Object value = beanMap.get(prop);
			if ("_team_list".equals(prop)) {
				Set teamSet = new HashSet();	
				Set teams = (Set)beanMap.get(prop);
				if( teams!= null){
					for (Object team : teams) {
						Map t = new HashMap(new BeanMap(team));
						t.remove("teamintern");
						teamSet.add(t);
					}
				}
				value = teamSet;
			}else if( value instanceof Collection){
				continue;
			}else{
				java.lang.reflect.Field field = o.getClass().getDeclaredField(prop);
				if (field != null) {
					if (!field.isAnnotationPresent(PrimaryKey.class) && (field.isAnnotationPresent(Element.class) || field.isAnnotationPresent(Persistent.class))) {
						continue;
					}
				}
			}
			n.put(prop, value);
		}
		return n;
	}
	protected boolean getBoolean(Object value) {
		try {
			return (Boolean) value;
		} catch (Exception e) {
		}
		return false;
	}
	protected boolean dateValid(Object value) {
		if( value == null) return true;
		long today = new java.util.Date().getTime();
		try {
			return today < (Long) value;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}
	private Object expandString(String str, Map binding) {
		if( str.startsWith("~" )){
			return str.substring(1);
		}
		int countRepl = 0;
		int countPlainStr = 0;
		Object replacement = null;
		String   newString = "";
		int      openBrackets = 0;
		int      first = 0;
		for (int i = 0; i < str.length(); i++) {
			if (i < str.length() - 2 && str.substring(i, i + 2).compareTo("${") == 0) {
				if (openBrackets == 0) {
					first = i + 2;
				}
				openBrackets++;
			} else if (str.charAt(i) == '}' && openBrackets > 0) {
				openBrackets -= 1;
				if (openBrackets == 0) {
					countRepl++;
					replacement = MVEL.evalToString(str.substring(first, i), binding);
					newString += replacement;
				}
			} else if (openBrackets == 0) {
				newString += str.charAt(i);
				countPlainStr++;
			}
		}
		if (countRepl == 1 && countPlainStr == 0) {
			return replacement;
		} else {
			return newString;
		}
	}
	public Object executeScript(String scriptName, String namespace, String user, Map params) throws Exception {
		System.out.println("UtilsServiceImpl.executeScript:" + params);
		String storeId= (String)params.get("storeId");
		StoreDesc sdesc = StoreDesc.get(storeId);
		namespace= sdesc.getNamespace();
		System.out.println("UtilsServiceImpl.namespace:" + sdesc+"/"+namespace);
		ScriptEngine se = m_scriptEngineService.getEngineByName("groovy");
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		se.getContext().setWriter(pw);
		Bindings b = se.createBindings();
		b.putAll(params);
		b.put("jdo", m_dataLayer);
		b.put("ws", lookupServiceByName("org.ms123.common.workflow.WorkflowService"));
		b.put("ss", lookupServiceByName("org.ms123.common.setting.SettingService"));
		b.put("et", lookupServiceByName("org.ms123.common.entity.EntityService"));
		b.put("storeDesc", sdesc);
		b.put("home", System.getProperty("simpl4.dir"));
		b.put("log", m_logger);
		b.put("user", user);
		b.put("namespace", namespace);
		se.setBindings(b, ScriptContext.ENGINE_SCOPE);
		b.put("se", se);
		Object r = "";
		FileReader fr = getScriptFile(namespace, scriptName);
		r = se.eval(fr);
		System.out.println("r:" + r);
		pw.close();
		m_logger.info("executeScript:" + sw);
		System.out.println("executeScript:" + sw);
		return null;
	}

	public Object lookupServiceByName(String name) {
		Object service = null;
		ServiceReference sr = m_bundleContext.getServiceReference(name);            
		if (sr != null) {
			service = m_bundleContext.getService(sr);
		} 
		if( service == null){
			throw new RuntimeException("Cannot resolve service:" +name);
		}
		return service;
	}

	public Class getClass(String name){
		try{
			if( m_getClassMethod == null){
				m_nucleusServiceObject = lookupServiceByName("org.ms123.common.nucleus.NucleusService");
				Class[] paramString = new Class[1];	
				paramString[0] = String.class;
				m_getClassMethod = m_nucleusServiceObject.getClass().getDeclaredMethod("getClass", paramString);
			}
			return (Class)m_getClassMethod.invoke(m_nucleusServiceObject, name );
		}catch(Exception e){
			throw new RuntimeException("UtilsServiceImpl.getClass()", e);
		}
	}
	private FileReader getScriptFile(String namespace, String scriptname) throws Exception {
		File scriptFile = null;
		if (scriptname.indexOf("/") != -1) {
			scriptFile = new File(System.getProperty("workspace"), scriptname);
			System.out.println("getScriptFile1:" + scriptFile + "->" + scriptFile.exists());
			if (scriptFile.exists()) {
				return new FileReader(scriptFile);
			}
		}
		scriptFile = new File(System.getProperty("simpl4.dir") + "/etc/"+namespace+"/scripts", scriptname);
		System.out.println("getScriptFile2:" + scriptFile + "->" + scriptFile.exists());
		if (scriptFile.exists()) {
			return new FileReader(scriptFile);
		}
		throw new RuntimeException("scriptFile not exists:" + scriptname);
	}

	private Object checkNull(Object o, String msg) {
		if (o == null) {
			throw new RuntimeException(msg);
		}
		return o;
	}

	/*BEGIN JSON-RPC-API*/
	public Object executeScript(
			@PName("scriptname") String scriptname, 
			@PName("namespace") String namespace, 
			@PName("params") @POptional Map params) throws RpcException {
		try {
			System.out.println("params:" + params);
			String user = org.ms123.common.system.ThreadContext.getThreadContext().getUserName();
			return executeScript(scriptname, namespace, user, params);
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "UtilsServiceImpl.executeScript:", e);
		}
	}

	/*END JSON-RPC-API*/
	@Reference
	public void setScriptEngineService(ScriptEngineService paramScriptEngineService) {
		System.out.println("UtilsServiceImpl:" + paramScriptEngineService);
		this.m_scriptEngineService = paramScriptEngineService;
	}

	@Reference(target = "(kind=jdo)")
	public void setDataLayer(DataLayer paramDataLayer) {
		this.m_dataLayer = paramDataLayer;
		System.out.println("UtilsServiceImpl.setDataLayer:" + paramDataLayer);
	}
}
