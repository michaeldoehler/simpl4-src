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
package org.ms123.common.system.hook;

import flexjson.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ms123.common.libhelper.Inflector;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Processor;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.util.ExchangeHelper;
import org.ms123.common.system.ThreadContext;
import org.ms123.common.git.GitService;
import org.ms123.common.permission.api.PermissionService;
import org.ms123.common.camel.api.CamelService;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.rpc.RpcException;
import groovy.lang.*;

/**
 *
 */
@SuppressWarnings("unchecked")
abstract class BaseHookServiceImpl {

	protected CamelService m_camelService;

	protected static final String HOOKS = ".etc/hooks.json";

	protected static final String ADMINROLE = "admin";

	protected GroovyShell m_groovyShell;

	protected PermissionService m_permissionService;

	protected GitService m_gitService;

	protected Inflector m_inflector = Inflector.getInstance();

	protected BundleContext m_bundleContext;

	protected List m_hooks;

	private Map<String,Script> m_scriptCache = new HashMap();

	protected JSONDeserializer m_ds = new JSONDeserializer();

	protected JSONSerializer m_js = new JSONSerializer();

	protected void camelAction(String ns, String loginUser, String serviceName, String methodName, String endpoint, boolean sync, Object params, Object result) {
		String execUser = "admin";
		//if (startCamelUser.equals("user")) {
		//	user = ThreadContext.getThreadContext().getUserName();
		//}
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
			Object answer = camelSend(endpoint, propMap);
			info("HookServiceImpl.CamelSend.sync.answer:" + answer);
		} else {
			new CamelThread(endpoint, execUser, propMap).start();
		}
	}

	private class CamelThread extends Thread {

		String endpoint;

		String execUser;

		Map propMap;

		public CamelThread(String endpoint, String execUser, Map propMap) {
			this.endpoint = endpoint;
			this.execUser = execUser;
			this.propMap = propMap;
		}

		public void run() {
			try {
				String ns = (String) propMap.get("namespace");
				ThreadContext.loadThreadContext(ns, execUser);
				m_permissionService.loginInternal(ns);
				Object answer = camelSend(endpoint, propMap);
				info("HookServiceImpl.CamelSend.async.answer:" + answer);
			} catch (Exception e) {
				e.printStackTrace();
				ThreadContext.getThreadContext().finalize(e);
				m_logger.error("BaseHookServiceImpl.CamelThread:", e);
			} finally {
				ThreadContext.getThreadContext().finalize(null);
			}
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
		m_scriptCache.clear();
		return m_hooks;
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

	protected Boolean isPreConditionOk(String expr, Object vars) {
		try {
			Script script = m_scriptCache.get(expr);
			if( script == null){
				script = m_groovyShell.parse(expr);
				m_scriptCache.put(expr,script);
			}
			Binding binding = new Binding((Map) vars);
			script.setBinding(binding);
			return (Boolean) script.run();
		} catch (Throwable e) {
			e.printStackTrace();
			return false;
		}
	}

	public String getUserName() {
		return getThreadContext().getUserName();
	}

	private org.ms123.common.system.ThreadContext getThreadContext() {
		return org.ms123.common.system.ThreadContext.getThreadContext();
	}

	public Object camelSend(String epUri, final Map<String, Object> properties) {
		return camelSend(epUri, null, null, properties);
	}

	public Object camelSend(String epUri, final Object body, final Map<String, Object> properties) {
		return camelSend(epUri, body, null, properties);
	}

	public Object camelSend(String epUri, final Object body, final Map<String, Object> headers, final Map<String, Object> properties) {
		Processor p = new Processor() {

			public void process(Exchange exchange) {
				if (properties != null) {
					for (String key : properties.keySet()) {
						exchange.setProperty(key, properties.get(key));
					}
				}
				Message in = exchange.getIn();
				if (headers != null) {
					for (String key : headers.keySet()) {
						in.setHeader(key, headers.get(key));
					}
				}
				in.setBody(body);
			}
		};
		ProducerTemplate template = m_camelService.getCamelContext((String) properties.get("namespace"), CamelService.DEFAULT_CONTEXT).createProducerTemplate();
		Exchange result = template.send(epUri, p);
		return ExchangeHelper.extractResultBody(result, null);
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

	private static final Logger m_logger = LoggerFactory.getLogger(BaseHookServiceImpl.class);
}
