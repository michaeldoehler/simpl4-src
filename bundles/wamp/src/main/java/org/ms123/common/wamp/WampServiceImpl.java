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
package org.ms123.common.wamp;

import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.metatype.*;
import java.io.*;
import java.util.*;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.ms123.common.rpc.PDefaultBool;
import org.ms123.common.rpc.PDefaultFloat;
import org.ms123.common.rpc.PDefaultInt;
import org.ms123.common.rpc.PDefaultLong;
import org.ms123.common.rpc.PDefaultString;
import org.ms123.common.rpc.PName;
import org.ms123.common.rpc.POptional;
import org.ms123.common.rpc.RpcException;
import org.ms123.common.permission.api.PermissionException;
import org.ms123.common.permission.api.PermissionService;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.ms123.common.rpc.JsonRpcServlet.ERROR_FROM_METHOD;
import static org.ms123.common.rpc.JsonRpcServlet.INTERNAL_SERVER_ERROR;
import static org.ms123.common.rpc.JsonRpcServlet.PERMISSION_DENIED;
import flexjson.*;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.CloseStatus;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import java.util.Map;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;

/** WampService implementation
 */
@SuppressWarnings("unchecked")
@Component(enabled = true, configurationPolicy = ConfigurationPolicy.optional, immediate = true, properties = { "rpc.prefix=wamp" })
public class WampServiceImpl extends BaseWampServiceImpl implements WampService {

	private static final Logger m_logger = LoggerFactory.getLogger(WampServiceImpl.class);
	private JSONDeserializer m_ds = new JSONDeserializer();
	private JSONSerializer m_js = new JSONSerializer();

	public WampServiceImpl() {
	}

	protected void activate(BundleContext bundleContext, Map<?, ?> props) {
	}

	protected void deactivate() throws Exception {
	}

	public WebSocketListener createWebSocket(Map<String, Object> config, Map<String, String> parameterMap) {
		return new WebSocket(config, parameterMap);
	}

	public class WebSocket extends BaseWebSocket {
		private Map<String, Object> m_config = null;
		private Map<String, String> m_params;
		private Subscription m_subscription;
		private WampRouterSession m_wampRouterSession;

		public WebSocket(Map<String, Object> config, Map<String, String> parameterMap) {
			m_js.prettyPrint(true);
			m_config = config;
			m_params = parameterMap;
			String namespace = m_params.get("namespace");
			String routesName = m_params.get("routes");
			m_wampRouterSession = new WampRouterSession(WampServiceImpl.this, this);
		}

		@Override
		public void onWebSocketConnect(Session sess) {
			super.onWebSocketConnect(sess);
			m_wampRouterSession.wsConnect(sess);
		}

		@Override
		public void onWebSocketText(String message) {
			m_wampRouterSession.wsMessage(message);
		}
		@Override
		public void onWebSocketBinary(byte[] payload, int offset, int len) {
			m_wampRouterSession.wsBinaryMessage(payload, offset, len);
		}

		@Override
		public void onWebSocketClose(int statusCode, String reason) {
			super.onWebSocketClose(statusCode, reason);
			m_wampRouterSession.wsClose(statusCode, reason);
		}

		@Override
		public void onWebSocketError(Throwable cause) {
			m_wampRouterSession.wsError(cause);
		}

	}

	//@Reference(dynamic = true, optional = true)
	//public void setCamelService(CamelService paramService) {
		//this.m_camelService = paramService;
		//info("WampServiceImpl.setCamelService:" + paramService);
	//}
}
