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

import au.com.ds.ef.*;
import au.com.ds.ef.call.ContextHandler;
import au.com.ds.ef.call.StateHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import flexjson.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.jetty.websocket.api.CloseStatus;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.ms123.common.permission.api.PermissionService;
import org.ms123.common.system.registry.RegistryService;
import org.ms123.common.wamp.WampMessages.ErrorMessage;
import org.ms123.common.wamp.WampMessages.AbortMessage;
import org.ms123.common.wamp.WampMessages.HelloMessage;
import org.ms123.common.wamp.WampMessages.PublishedMessage;
import org.ms123.common.wamp.WampMessages.PublishMessage;
import org.ms123.common.wamp.WampMessages.RegisteredMessage;
import org.ms123.common.wamp.WampMessages.RegisterMessage;
import org.ms123.common.wamp.WampMessages.SubscribedMessage;
import org.ms123.common.wamp.WampMessages.SubscribeMessage;
import org.ms123.common.wamp.WampMessages.WampMessage;
import org.ms123.common.wamp.WampMessages.WelcomeMessage;
import org.ms123.common.wamp.WampServiceImpl.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static au.com.ds.ef.FlowBuilder.from;
import static au.com.ds.ef.FlowBuilder.fromTransitions;
import static au.com.ds.ef.FlowBuilder.on;
import static org.ms123.common.wamp.WampRouterSession.Events.*;
import static org.ms123.common.wamp.WampRouterSession.States.*;

/**
 *
 */
class WampRouterSession {

	protected PermissionService m_permissionService;
	protected JSONDeserializer m_ds = new JSONDeserializer();
	protected JSONSerializer m_js = new JSONSerializer();
	private WampService m_wampService;
	private RegistryService m_registryService;
	private EasyFlow<SessionContext> m_flow;
	private SessionContext m_context = new SessionContext();
	final ObjectMapper m_objectMapper = new ObjectMapper();
	private Map<String, Realm> m_realms;



	public static class SessionContext extends StatefulContext {
		long sessionId;
		long lastUsedId = IdValidator.MIN_VALID_ID;
		Realm realm;
		Set<WampRoles> roles;
		Map<Long, Procedure> providedProcedures;
		Map<Long, Invocation> pendingInvocations;
		Map<Long, Subscription> subscriptionsById;
		WampMessage currentMsg;
		WebSocket webSocket;
	}

	final static Set<WampRoles> SUPPORTED_CLIENT_ROLES;
	static {
		SUPPORTED_CLIENT_ROLES = new HashSet<WampRoles>();
		SUPPORTED_CLIENT_ROLES.add(WampRoles.Caller);
		SUPPORTED_CLIENT_ROLES.add(WampRoles.Callee);
		SUPPORTED_CLIENT_ROLES.add(WampRoles.Publisher);
		SUPPORTED_CLIENT_ROLES.add(WampRoles.Subscriber);
	}

	enum States implements StateEnum {
		WEBSOCKET_CREATED, CONNECTED, SESSION_START, SESSION_IDLE, SUBSCRIPING, PUBLISHING, EXECUTE_CALL, REGISTERING, ERROR
	}

	enum Events implements EventEnum {
		websocketConnection, sessionStarted, hello, register, subscribe, publish, call, error, jobReady
	}

	protected WampRouterSession(WampService wampService, WampServiceImpl.WebSocket ws,Map<String, Realm> realms) {
		m_wampService = wampService;
		//m_registryService = m_wampService.getRegistryService();
		m_realms = realms;
		m_context.webSocket = ws;

		initFlow();
		m_flow.start(m_context);
	}

	public void initFlow() {
		if (m_flow != null) {
			return;
		}
		m_flow = from(WEBSOCKET_CREATED).transit(
				on(websocketConnection).to(CONNECTED).transit(
						on(hello).to(SESSION_START).transit(
								on(sessionStarted).to(SESSION_IDLE).transit(
										on(subscribe).to(SUBSCRIPING).transit(on(jobReady).to(SESSION_IDLE)),
										on(publish).to(PUBLISHING).transit(on(jobReady).to(SESSION_IDLE)),
										on(call).to(EXECUTE_CALL).transit(on(jobReady).to(SESSION_IDLE)),
										on(register).to(REGISTERING).transit(on(jobReady).to(SESSION_IDLE)),
										on(error).finish(ERROR)))));

		m_flow.executor(new SyncExecutor())

		.whenEnter(CONNECTED, new ContextHandler<SessionContext>() {
			@Override
			public void call(SessionContext context) throws Exception {
				debug("    CONNECTED");
			}
		})

		.whenEnter(SESSION_START, new ContextHandler<SessionContext>() {
			@Override
			public void call(SessionContext context) throws Exception {
				debug("    SESSION_START:");
				HelloMessage hello = ((HelloMessage) context.currentMsg);
				Realm realm = null;
				String errorMsg = null;
        if (!UriValidator.tryValidate(hello.realm, false)) {
            errorMsg = ApplicationError.INVALID_URI;
        } else {
            realm = m_realms.get(hello.realm);
            if (realm == null) {
                errorMsg = ApplicationError.NO_SUCH_REALM;
            }
        }
            
        if (errorMsg != null) {
            String abort = WampDecode.encode(new AbortMessage(null, errorMsg));
						debug("--> SendMessage(abort):" + abort);
						m_context.webSocket.sendStringByFuture(abort);
						m_context.safeTrigger(sessionStarted);
            return;
        }

				long sessionId = IdGenerator.newRandomId(null);
        Set<WampRoles> roles = new HashSet<WampRoles>();
        realm.includeSession(m_context, sessionId, roles);
				roles.add( WampRoles.Broker);
				ObjectNode welcomeDetails = m_objectMapper.createObjectNode();
				welcomeDetails.put("agent", "simpl4-1.0");
				ObjectNode routerRoles = welcomeDetails.putObject("roles");
				ObjectNode roleNode = routerRoles.putObject("broker");
				WelcomeMessage wm = new WampMessages.WelcomeMessage(sessionId, welcomeDetails);
				String encodedMessage = WampDecode.encode(wm);
				debug("--> SendMessage(welcome):" + encodedMessage);
				m_context.webSocket.sendStringByFuture(encodedMessage);
				m_context.safeTrigger(sessionStarted);
			}
		}).whenEnter(SESSION_IDLE, new ContextHandler<SessionContext>() {
			@Override
			public void call(SessionContext context) throws Exception {
				debug("    SESSION_IDLE");
				//m_context.safeTrigger(jobReady);
			}
		}).whenEnter(SUBSCRIPING, new ContextHandler<SessionContext>() {
			@Override
			public void call(SessionContext context) throws Exception {
				SubscribeMessage sub = ((WampMessages.SubscribeMessage) context.currentMsg);
				debug("    SUBSCRIPING");
				SubscribedMessage subscribed = new SubscribedMessage(sub.requestId, 123456789);
				String encodedMessage = WampDecode.encode(subscribed);
				debug("--> SendMessage(subscribed):" + encodedMessage);
				m_context.webSocket.sendStringByFuture(encodedMessage);

				m_context.safeTrigger(jobReady);
			}
		}).whenEnter(PUBLISHING, new ContextHandler<SessionContext>() {
			@Override
			public void call(SessionContext context) throws Exception {
				PublishMessage pub = ((WampMessages.PublishMessage) context.currentMsg);
				debug("    PUBLISHING");
				PublishedMessage published = new PublishedMessage(pub.requestId, 1234567890);

				boolean sendAcknowledge = false;
				JsonNode ackOption = pub.options.get("acknowledge");
				if (ackOption != null && ackOption.asBoolean() == true) {
					sendAcknowledge = true;
				}
				if (!sendAcknowledge) {
					return;
				}
				String encodedMessage = WampDecode.encode(published);
				debug("--> SendMessage(published):" + encodedMessage);
				m_context.webSocket.sendStringByFuture(encodedMessage);

				m_context.safeTrigger(jobReady);
			}
		}).whenEnter(REGISTERING, new ContextHandler<SessionContext>() {
			@Override
			public void call(SessionContext context) throws Exception {
				RegisterMessage reg = ((WampMessages.RegisterMessage) context.currentMsg);
				debug("    REGISTERING");
				String err = null;
				if (!UriValidator.tryValidate(reg.procedure, true)) {
					err = ApplicationError.INVALID_URI;
				}
				if (err == null && !(IdValidator.isValidId(reg.requestId))) {
					err = ApplicationError.INVALID_ARGUMENT;
				}

				Procedure proc = null;
				if (err == null) {
					proc = m_context.realm.procedures.get(reg.procedure);
					if (proc != null) err = ApplicationError.PROCEDURE_ALREADY_EXISTS;
				}

				if (err != null) {
					String errMsg = WampDecode.encode(new ErrorMessage(RegisterMessage.ID, reg.requestId, null, err, null, null));
					info("ErrorMessage:" + errMsg);
					m_context.webSocket.sendStringByFuture(errMsg);
					m_context.safeTrigger(jobReady);
					return;
				}
				// Everything checked, we can register the caller as the procedure provider
				long registrationId = IdGenerator.newLinearId(m_context.lastUsedId, m_context.providedProcedures);
				m_context.lastUsedId = registrationId;
				Procedure procInfo = new Procedure(reg.procedure, m_context.webSocket, registrationId);
				
				// Insert new procedure
				m_context.realm.procedures.put(reg.procedure, procInfo);
				if (m_context.providedProcedures == null) {
						m_context.providedProcedures = new HashMap<Long, Procedure>();
						m_context.pendingInvocations = new HashMap<Long, Invocation>();
				}
				m_context.providedProcedures.put(procInfo.registrationId, procInfo);
				
				String response = WampDecode.encode(new RegisteredMessage(reg.requestId, procInfo.registrationId));
				m_context.webSocket.sendStringByFuture(response);
				m_context.safeTrigger(jobReady);
			}
		}).whenEnter(EXECUTE_CALL, new ContextHandler<SessionContext>() {
			@Override
			public void call(SessionContext context) throws Exception {
				debug("    EXECUTE_CALL");
				m_context.safeTrigger(jobReady);
			}
		});
	}

	public void onWebSocketConnect(Session sess) {
		debug("<-- SocketConnect");
		m_context.safeTrigger(websocketConnection);
	}

	public void onWebSocketBinary(byte[] payload, int offset, int len) {
		try {
			debug("BinMessage.recveived:" + payload);
			WampMessage msg = WampDecode.decode(payload);
			debug("Message.recveived:" + msg);
		} catch (Exception e) {
			e.printStackTrace();

		}
	}

	public void onWebSocketText(String message) {
		try {
			WampMessage msg = WampDecode.decode(message.getBytes());
			m_context.currentMsg = msg;
			EventEnum e = getMessageEnum(msg);
			debug("<-- ReceiveMessage(" + e + "):" + message);
			m_context.safeTrigger(e);
		} catch (Exception e) {
			e.printStackTrace();

		}
	}

	public void onWebSocketClose(int statusCode, String reason) {
		debug("<-- SocketClose:" + statusCode + "/" + reason);
		m_context.realm.removeSession(m_context, true );

	}

	public void onWebSocketError(Throwable cause) {
		debug("<-- SocketError:" + cause);
	}

	private EventEnum getMessageEnum(Object o) {
		String s = o.toString();
		int nameEndIndex = s.indexOf("Message@");
		int dollarIndex = s.lastIndexOf("$");
		String name = s.substring(dollarIndex + 1, nameEndIndex);
		return Events.valueOf(name.toLowerCase());
	}


	protected static void debug(String msg) {
		System.out.println(msg);
		m_logger.debug(msg);
	}

	protected static void info(String msg) {
		System.out.println(msg);
		m_logger.info(msg);
	}

	private static final Logger m_logger = LoggerFactory.getLogger(WampRouterSession.class);
}

