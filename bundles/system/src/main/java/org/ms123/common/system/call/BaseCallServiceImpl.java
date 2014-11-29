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
package org.ms123.common.system.call;

import flexjson.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ms123.common.libhelper.Inflector;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Processor;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Endpoint;
import org.apache.camel.util.ExchangeHelper;
import org.ms123.common.system.ThreadContext;
import org.ms123.common.git.GitService;
import org.ms123.common.permission.api.PermissionService;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.rpc.RpcException;
import org.ms123.common.rpc.JsonRpcServlet;
import org.ms123.common.camel.api.CamelService;
import static org.ms123.common.camel.api.CamelService.CAMEL_TYPE;
import static org.ms123.common.camel.api.CamelService.PROPERTIES;
import static org.ms123.common.camel.api.CamelService.OVERRIDEID;
import groovy.lang.*;
import org.apache.commons.beanutils.ConvertUtils;

/**
 *
 */
@SuppressWarnings("unchecked")
abstract class BaseCallServiceImpl {

	protected CamelService m_camelService;

	protected PermissionService m_permissionService;

	protected GitService m_gitService;

	protected static final Map<String, Class> m_types = new HashMap<String, Class>() {

		{
			put("string", java.lang.String.class);
			put("integer", java.lang.Integer.class);
			put("double", java.lang.Double.class);
			put("boolean", java.lang.Boolean.class);
			put("map", java.util.Map.class);
			put("list", java.util.List.class);
		}
	};

	protected static final String HOOKS = ".etc/hooks.json";

	protected static final String ADMINROLE = "admin";

	protected GroovyShell m_groovyShell = new GroovyShell();

	protected Inflector m_inflector = Inflector.getInstance();

	protected JSONDeserializer m_ds = new JSONDeserializer();

	protected JSONSerializer m_js = new JSONSerializer();

	protected List m_hooks;

	private Map<String, Script> m_hookPreConditionScriptCache = new HashMap();

	protected void camelHook(String ns, String loginUser, String serviceName, String methodName, String routeId, boolean sync, Object params, Object result) {
		String execUser = "admin";
		boolean hasAdminRole = m_permissionService.hasRole(ADMINROLE);
		Map propMap = new HashMap();
		propMap.put("namespace", ns);
		propMap.put("loginUser", loginUser);
		propMap.put("userName", loginUser);
		propMap.put("hasAdminRole", hasAdminRole);
		propMap.put("serviceName", serviceName);
		propMap.put("methodName", methodName);
		propMap.put("methodParams", params);
		propMap.put("methodResult", result);
		if (sync) {
			Object answer =m_camelService.camelSend(ns, routeId, propMap);
			info("CallServiceImpl.CamelSend.sync.answer:" + answer);
		} else {
			new CamelThread(routeId, execUser, propMap).start();
		}
	}

	protected List getHooks(String ns) {
		if (m_hooks != null)
			return m_hooks;
		try {
			String s = m_gitService.getContentRaw(ns, HOOKS);
			m_hooks = (List) m_ds.deserialize(s);
		} catch (RpcException e) {
			if (e.getErrorCode() == 101) {
				return null;
			}
			throw e;
		}
		m_hookPreConditionScriptCache.clear();
		return m_hooks;
	}

	protected Boolean isHookPreConditionOk(String expr, Object vars) {
		try {
			Script script = m_hookPreConditionScriptCache.get(expr);
			if (script == null) {
				script = m_groovyShell.parse(expr);
				m_hookPreConditionScriptCache.put(expr, script);
			}
			Binding binding = new Binding((Map) vars);
			script.setBinding(binding);
			return (Boolean) script.run();
		} catch (Throwable e) {
			e.printStackTrace();
			return false;
		}
	}

	private class CamelThread extends Thread {

		String routeId;

		String execUser;

		Map propMap;

		public CamelThread(String routeId, String execUser, Map propMap) {
			this.routeId = routeId;
			this.execUser = execUser;
			this.propMap = propMap;
		}

		public void run() {
			try {
				String ns = (String) propMap.get("namespace");
				ThreadContext.loadThreadContext(ns, execUser);
				m_permissionService.loginInternal(ns);
				Object answer = m_camelService.camelSend(ns, routeId, propMap);
				info("CallServiceImpl.CamelSend.async.answer:" + answer);
			} catch (Exception e) {
				e.printStackTrace();
				ThreadContext.getThreadContext().finalize(e);
				m_logger.error("BaseCallServiceImpl.CamelThread:", e);
			} finally {
				ThreadContext.getThreadContext().finalize(null);
			}
		}
	}

	protected Map getCamelShape(String ns, String name) {
		Map shape = m_camelService.getShapeByRouteId(ns, name);
		if (shape == null) {
			if( !name.endsWith(".camel")){
				shape = m_camelService.getShapeByRouteId(ns, name+".camel");
			}
		}
		if (shape == null) {
			throw new RpcException(JsonRpcServlet.ERROR_FROM_SERVER, JsonRpcServlet.METHOD_NOT_FOUND, "Method " + name + "(.camel) not found");
		}
		return shape;
	}

	protected String getId(Map shape) {
		Map properties = (Map) shape.get(PROPERTIES);
		String id = ((String) properties.get(OVERRIDEID));
		return id;
	}
	private org.ms123.common.system.ThreadContext getThreadContext() {
		return org.ms123.common.system.ThreadContext.getThreadContext();
	}

	protected String getNamespace(Object params) {
		String ns = null;
		if (params instanceof Map) {
			Map pmap = (Map) params;
			ns = (String) pmap.get(StoreDesc.NAMESPACE);
			if (ns == null) {
				ns = (String) pmap.get(StoreDesc.STORE_ID);
				if (ns != null) {
					ns = ns.substring(0, ns.indexOf("_"));
				}
			}
		}
		return ns;
	}

	protected boolean isPermitted(String userName, List<String> userRoleList, List<String> permittedUserList, List<String> permittedRoleList) {
		if (permittedUserList.contains(userName)) {
			info("userName(" + userName + " is allowed:" + permittedUserList);
			return true;
		}
		for (String userRole : userRoleList) {
			if (permittedRoleList.contains(userRole)) {
				info("userRole(" + userRole + " is allowed:" + permittedRoleList);
				return true;
			}
		}
		return false;
	}

	protected Object getValue(String name, Object value, Object def, boolean optional, Class type) {
		if (value == null && def != null) {
			def = convertTo(def, type);
			value = def;
		}
		if (value == null && optional == false) {
			throw new RpcException(JsonRpcServlet.ERROR_FROM_METHOD, JsonRpcServlet.INTERNAL_SERVER_ERROR, "CamelRouteService:Missing parameter:" + name);
		}
		if (value == null) {
			return null;
		}
		if (!type.isAssignableFrom(value.getClass())) {
			throw new RpcException(JsonRpcServlet.ERROR_FROM_METHOD, JsonRpcServlet.INTERNAL_SERVER_ERROR, "CamelRouteService:parameter(" + name + ") wrong type:" + value.getClass() + " needed:" + type);
		}
		return value;
	}

	public static Object convertTo(Object sourceObject, Class<?> targetClass) {
		try {
			return ConvertUtils.convert(sourceObject, targetClass);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	protected List<String> getUserRoles(String userName){
		List<String> userRoleList = null;
		try {
			userRoleList = m_permissionService.getUserRoles(userName);
		} catch (Exception e) {
			userRoleList = new ArrayList();
		}
		return userRoleList;
	}

	protected List<Map> getItemList(Map shape, String name) {
		Map properties = (Map) shape.get(PROPERTIES);
		Map m = (Map) properties.get(name);
		if (m == null)
			return new ArrayList();
		return (List) m.get("items");
	}

	protected List<String> getStringList(Map shape, String name) {
		String s = getString(shape, name, "");
		return Arrays.asList(s.split(","));
	}

	protected String getString(Map shape, String name, String _default) {
		Map properties = (Map) shape.get(PROPERTIES);
		Object value = properties.get(name);
		if (value == null)
			return _default;
		return (String) value;
	}

	protected Boolean getBoolean(Map shape, String name, boolean def) {
		Map properties = (Map) shape.get(PROPERTIES);
		Object value = properties.get(name);
		if (value == null)
			return def;
		return (Boolean) value;
	}

	protected String getUserName() {
		return getThreadContext().getUserName();
	}

	protected static void debug(String msg) {
		m_logger.debug(msg);
	}

	protected static void error(String msg) {
		System.out.println(msg);
		m_logger.error(msg);
	}

	protected static void info(String msg) {
		System.out.println(msg);
		m_logger.info(msg);
	}

	private static final Logger m_logger = LoggerFactory.getLogger(BaseCallServiceImpl.class);
}
