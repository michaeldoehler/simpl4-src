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
import org.ms123.common.system.ThreadContext;
import org.ms123.common.git.GitService;
import org.ms123.common.permission.api.PermissionService;
import org.ms123.common.camel.api.CamelService;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.rpc.RpcException;

/**
 *
 */
@SuppressWarnings("unchecked")
abstract class BaseHookServiceImpl {

	protected CamelService m_camelService;
	protected static final String HOOKS = ".etc/hooks.json";

	protected PermissionService m_permissionService;
	protected GitService m_gitService;
	protected Inflector m_inflector = Inflector.getInstance();

	protected BundleContext m_bundleContext;
	protected List m_hooks;

	protected JSONDeserializer m_ds = new JSONDeserializer();

	protected JSONSerializer m_js = new JSONSerializer();

	protected void camelAction(String ns, String endpoint, Object params,Object result) {
		String user = "admin";
		//if (startCamelUser.equals("user")) {
		//	user = ThreadContext.getThreadContext().getUserName();
		//}
		Map paramMap = new HashMap();
		paramMap.put("params", params);
		paramMap.put("result", result);
		new CamelThread(ns, endpoint, user, paramMap).start();
	}

	private class CamelThread extends Thread {
		String endpoint;
		String ns;
		String user;
		Map paramMap;

		public CamelThread(String ns, String endpoint, String user, Map paramMap) {
			this.endpoint = endpoint;
			this.ns = ns;
			this.user = user;
			this.paramMap = paramMap;
		}

		public void run() {
			try {
				ThreadContext.loadThreadContext(ns, user);
				m_permissionService.loginInternal(ns);
				ProducerTemplate template = m_camelService.getCamelContext(ns,CamelService.DEFAULT_CONTEXT).createProducerTemplate();
				template.sendBody(endpoint, paramMap);
				info("calling cameltemplate:" + endpoint + "/ns:" + ns + "/user:" + user);
			} catch (Exception e) {
				e.printStackTrace();
				ThreadContext.getThreadContext().finalize(e);
				m_logger.error("BaseHookServiceImpl.CamelThread:", e);
			} finally {
				ThreadContext.getThreadContext().finalize(null);
			}
		}
	}
	protected List getHooks(String ns){
		if( m_hooks != null) return m_hooks;
		try{
			String s = m_gitService.getContentRaw(ns, HOOKS);
			info("getHooks:"+s);
			m_hooks = (List)m_ds.deserialize(s);
		}catch(RpcException e){
			if( e.getErrorCode() == 101){
				return null;
			}
			throw e;
		}
		return m_hooks;
	}
	protected String getNamespace(Object params){
		String ns = null;
		if( params instanceof Map){
			Map pmap = (Map)params;
			ns = (String)pmap.get(StoreDesc.NAMESPACE);
			if( ns == null){
				ns = (String)pmap.get(StoreDesc.STORE_ID);
				if( ns != null){
					ns = ns.substring(0, ns.indexOf("_"));
				}
			}
		}
		return ns;
	}
	protected static void debug(String msg) {
		System.out.println(msg);
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
