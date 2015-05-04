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
package org.ms123.common.jetty;

import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import java.lang.reflect.Method;
import org.ms123.common.libhelper.Inflector;

/**
 *
 */
public class S4WebSocketCreator implements WebSocketCreator {

	protected Inflector m_inflector = Inflector.getInstance();
	private Map m_config = null;
	private Map<String, Object> m_sockets = new ConcurrentHashMap();
	private BundleContext m_bundleContext;

	private String getServiceClassName(String serviceName) {
		String serviceClassName = null;
		int dot = serviceName.lastIndexOf(".");
		if (dot != -1) {
			String part1 = serviceName.substring(0, dot);
			String part2 = serviceName.substring(dot + 1);
			System.out.println("serviceName:" + serviceName);
			serviceClassName = "org.ms123.common." + part1 + "." + m_inflector.upperCamelCase(part2, '-') + "Service";
		} else {
			String s = m_inflector.upperCamelCase(serviceName, '-');
			serviceClassName = "org.ms123.common." + s.toLowerCase() + "." + s + "Service";
		}
		System.out.println("ServiceClassName:" + serviceClassName);
		return serviceClassName;
	}

	private Object getWebSocket(String className, Map<String,List<String>> parameterMap) {
		Object service = null;
		ServiceReference sr = m_bundleContext.getServiceReference(className);
		if (sr != null) {
			service = m_bundleContext.getService(sr);
		}
		if (service == null) {
			throw new RuntimeException("WebSocketCreator.Cannot resolve service:" + className);
		}
		Class[] cargs = new Class[2];
		cargs[0] = Map.class;
		cargs[1] = Map.class;
		try {
			Method meth = service.getClass().getDeclaredMethod("createWebSocket", cargs);
			Object[] args = new Object[2];
			args[0] = m_config;
			args[1] = parameterMap;
			return meth.invoke(service, args);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("WebSocketCreator.Cannot create WebSocket:" + e.getMessage());
		}
	}

	private String getSessionIdParameter(Map<String, List<String>> map) {
		List<String> paramList = map.get("sessionId");
		if (paramList == null || paramList.size() == 0) {
			throw new RuntimeException("WebSocketCreator.Cannot get sessionId parameter from querystring");
		}
		return paramList.get(0);
	}

	private String getServiceParameter(Map<String, List<String>> map) {
		List<String> paramList = map.get("service");
		if (paramList == null || paramList.size() == 0) {
			System.out.println("WebSocketCreator.Cannot get service parameter from querystring");
			throw new RuntimeException("WebSocketCreator.Cannot get service parameter from querystring");
		}
		return paramList.get(0);
	}

	public S4WebSocketCreator(Map config) {
		m_bundleContext = (BundleContext) config.get("bundleContext");
		m_config = config;
	}

	@Override
	public synchronized Object createWebSocket(ServletUpgradeRequest req, ServletUpgradeResponse resp) {
		try {
			String sessionId = getSessionIdParameter(req.getParameterMap());
			System.out.println("createWebSocket:"+sessionId+"/"+m_sockets);
			Object socket = m_sockets.get(sessionId);
			if (socket == null) {
				socket = getWebSocket(getServiceClassName(getServiceParameter(req.getParameterMap())), req.getParameterMap());
				m_sockets.put(sessionId, socket);
				System.out.println("createWebSocket:" + socket);
			}
			return socket;
		} catch (Exception e) {
			e.printStackTrace();
			try {
				resp.sendError(400, e.getMessage());
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
		return null;
	}
}
