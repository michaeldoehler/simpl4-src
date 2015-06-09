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

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.component.Reference;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import flexjson.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.eclipse.jetty.websocket.api.CloseStatus;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.ms123.common.rpc.PDefaultBool;
import org.ms123.common.rpc.PDefaultFloat;
import org.ms123.common.rpc.PDefaultInt;
import org.ms123.common.rpc.PDefaultLong;
import org.ms123.common.rpc.PDefaultString;
import org.ms123.common.rpc.PName;
import org.ms123.common.rpc.POptional;
import org.ms123.common.rpc.RpcException;
import org.ms123.common.system.registry.RegistryService;
import org.ms123.common.wamp.WampMessages.GoodbyeMessage;
import org.ms123.common.wamp.WampMessages.HelloMessage;
import org.ms123.common.wamp.WampMessages.InvocationMessage;
import org.ms123.common.wamp.WampMessages.RegisteredMessage;
import org.ms123.common.wamp.WampMessages.RegisterMessage;
import org.ms123.common.wamp.WampMessages.WampMessage;
import org.ms123.common.wamp.WampMessages.ResultMessage;
import org.ms123.common.wamp.WampMessages.WelcomeMessage;
import org.ms123.common.wamp.WampMessages.YieldMessage;
import org.ms123.common.wamp.WampMessages.ErrorMessage;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.Observable;
import rx.Subscription;
import org.ms123.common.rpc.JsonRpc;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.ms123.common.rpc.JsonRpcServlet.ERROR_FROM_METHOD;
import static org.ms123.common.rpc.JsonRpcServlet.INTERNAL_SERVER_ERROR;
import static org.ms123.common.rpc.JsonRpcServlet.PERMISSION_DENIED;

/** WampService implementation
 */
@SuppressWarnings("unchecked")
@Component(enabled = true, configurationPolicy = ConfigurationPolicy.optional, immediate = true, properties = { "rpc.prefix=wamp" })
public class WampServiceImpl extends BaseWampServiceImpl implements WampService {

	private static final Logger m_logger = LoggerFactory.getLogger(WampServiceImpl.class);

	private JSONDeserializer m_ds = new JSONDeserializer();
	private JSONSerializer m_js = new JSONSerializer();

	private Map<String, Realm> m_realms;
	private String DEFAULT_REALM = "realm1";

	private List<String> m_registeredMethodList = new ArrayList();
	private Map<Long, Procedure> m_registeredMethodMap = new HashMap();

long m_counter=0;
	private WampRouterSession m_localWampRouterSession;
	private ObjectMapper m_objectMapper = new ObjectMapper();
	private JsonRpc m_jsonRpc;

	public WampServiceImpl() {
		m_realms = new HashMap();
		Set<WampRoles> roles = new HashSet();
		roles.add(WampRoles.Broker);
		RealmConfig realmConfig = new RealmConfig(roles, false);
		m_realms.put(DEFAULT_REALM, new Realm(realmConfig));
	}

	protected void activate(BundleContext bundleContext, Map<?, ?> props) {
		m_jsonRpc = new JsonRpc(bundleContext);
	}

	protected void deactivate() throws Exception {
		for (Realm realm : m_realms.values()) {
			for (WampRouterSession.SessionContext context : realm.m_contextList) {
				realm.removeSession(context, false);
				String goodbye = WampDecode.encode(new GoodbyeMessage(null, ApplicationError.SYSTEM_SHUTDOWN));
				context.webSocket.sendStringByFuture(goodbye);
			}
			realm.m_contextList.clear();
		}
	}

	@RequiresRoles("admin")
	public void registerMethods() throws RpcException {
		List<String> methodList = new ArrayList();
		methodList.add("enumeration.get");
		if (m_localWampRouterSession == null) {
			BaseWebSocket dummyWebSocket = new BaseWebSocket() {

				public void sendStringByFuture(String message) {
					ExecutorService executor = Executors.newSingleThreadExecutor();
					executor.submit(() ->  {
						WampMessage msg = WampDecode.decode(message.getBytes());
						info("Local.sendStringByFuture:" + msg);
						if (msg instanceof RegisteredMessage) {
							RegisteredMessage regMsg = (RegisteredMessage) msg;
							Procedure proc = new Procedure(m_registeredMethodList.get((int) regMsg.requestId), null, regMsg.registrationId);
							m_registeredMethodMap.put(regMsg.registrationId, proc);
						} else if (msg instanceof WelcomeMessage) {
							long i = m_registeredMethodList.size();
							for (String meth : methodList) {
								if (m_registeredMethodList.contains(meth)) {
									continue;
								}
								m_registeredMethodList.add(meth);
								String register = WampDecode.encode(new RegisterMessage(i++, null, meth));
								m_localWampRouterSession.onWebSocketText(register);
							}
						} else if (msg instanceof InvocationMessage) {
							InvocationMessage invMsg = (InvocationMessage) msg;
							Procedure proc = m_registeredMethodMap.get(invMsg.registrationId);
							info("Invocation:" + proc.procName + "/" + invMsg.arguments + "/" + invMsg.argumentsKw+"/ThreadId"+ Thread.currentThread().getId());

							String paramString = invMsg.argumentsKw != null ? invMsg.argumentsKw.toString() : "";
							Map<String,Object> result = m_jsonRpc.handleRPC(proc.procName, paramString);
							Object error = result.get("error");
							m_counter++;
							try{
								Thread.sleep((m_counter % 2)==0 ? 0: 0);
							}catch(Exception e){
							}
							if ( error != null ) {
								String errMsg = WampDecode.encode(new ErrorMessage(InvocationMessage.ID, invMsg.requestId, null, result.toString(), null, null));
								m_localWampRouterSession.onWebSocketText(errMsg);
							} else {
								ArrayNode resultNode = m_objectMapper.createArrayNode();
								resultNode.add( (JsonNode)m_objectMapper.valueToTree(result));
								String yield = WampDecode.encode(new YieldMessage(invMsg.requestId, null, resultNode, null));

								m_localWampRouterSession.onWebSocketText(yield);

							}
						}
					});
				}
			};
			m_localWampRouterSession = new WampRouterSession(this, dummyWebSocket, m_realms);
			m_localWampRouterSession.onWebSocketConnect(null);
			m_localWampRouterSession.onWebSocketText(WampDecode.encode(new HelloMessage(DEFAULT_REALM, null)));
		}
	}

	public RegistryService getRegistryService() {
		return m_registryService;
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
			m_wampRouterSession = new WampRouterSession(WampServiceImpl.this, this, m_realms);
		}

		@Override
		public void onWebSocketConnect(Session sess) {
			super.onWebSocketConnect(sess);
			m_wampRouterSession.onWebSocketConnect(sess);
		}

		@Override
		public void onWebSocketText(String message) {
			m_wampRouterSession.onWebSocketText(message);
		}

		@Override
		public void onWebSocketBinary(byte[] payload, int offset, int len) {
			m_wampRouterSession.onWebSocketBinary(payload, offset, len);
		}

		@Override
		public void onWebSocketClose(int statusCode, String reason) {
			super.onWebSocketClose(statusCode, reason);
			m_wampRouterSession.onWebSocketClose(statusCode, reason);
		}

		@Override
		public void onWebSocketError(Throwable cause) {
			m_wampRouterSession.onWebSocketError(cause);
		}
	}

	@Reference(dynamic = true, optional = true)
	public void setRegistryService(RegistryService paramService) {
		this.m_registryService = paramService;
		info("WampServiceImpl.setRegistryService:" + paramService);
	}
}
