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
import java.io.OutputStream;
import java.io.InputStream;
import org.ms123.common.nucleus.api.NucleusService;
import org.ms123.common.permission.api.PermissionService;
import flexjson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import au.com.ds.ef.*;
import au.com.ds.ef.call.StateHandler;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.CloseStatus;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import static au.com.ds.ef.FlowBuilder.from;
import static au.com.ds.ef.FlowBuilder.fromTransitions;
import static au.com.ds.ef.FlowBuilder.on;
import au.com.ds.ef.call.ContextHandler;
import static org.ms123.common.wamp.WampRouterSession.Events.*;
import static org.ms123.common.wamp.WampRouterSession.States.*;
import org.ms123.common.wamp.WampMessages.WampMessage;
import org.ms123.common.wamp.WampMessages.WelcomeMessage;
import org.ms123.common.wamp.WampMessages.SubscribeMessage;
import org.ms123.common.wamp.WampMessages.SubscribedMessage;
import org.ms123.common.wamp.WampMessages.PublishMessage;
import org.ms123.common.wamp.WampMessages.PublishedMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;

/**
 *
 */
class WampRouterSession {

	protected PermissionService m_permissionService;
	protected NucleusService m_nucleusService;
	protected JSONDeserializer m_ds = new JSONDeserializer();
	protected JSONSerializer m_js = new JSONSerializer();
	private WampService m_wampService;
	private WebSocketListener m_wsListener;
	private EasyFlow<FlowContext> m_flow;
	private FlowContext m_context = new FlowContext();
	private Session m_wsSession;
	final ObjectMapper m_objectMapper = new ObjectMapper();

	private static class FlowContext extends StatefulContext {
		long sessionId;
		WampMessage last;
	}

	enum States implements StateEnum {
		WEBSOCKET_CREATED, CONNECTED, SESSION_START, SESSION_IDLE, SUBSCRIPING, PUBLISHING, EXECUTE_CALL, REGISTERING, ERROR
	}

	enum Events implements EventEnum {
		websocketConnection, sessionStarted, hello, register, subscribe, publish, call, error, jobReady
	}

	protected WampRouterSession(WampService wampService, WebSocketListener ws) {
		m_wampService = wampService;
		m_wsListener = ws;
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

		.whenEnter(CONNECTED, new ContextHandler<FlowContext>() {
			@Override
			public void call(FlowContext context) throws Exception {
				debug("    CONNECTED");
			}
		})

		.whenEnter(SESSION_START, new ContextHandler<FlowContext>() {
			@Override
			public void call(FlowContext context) throws Exception {
				debug("    SESSION_START");
				long sessionId = IdGenerator.newRandomId(null);
				ObjectNode welcomeDetails = m_objectMapper.createObjectNode();
				welcomeDetails.put("agent", "simpl4-1.0");
				ObjectNode routerRoles = welcomeDetails.putObject("roles");
				ObjectNode roleNode = routerRoles.putObject("broker");
				WelcomeMessage wm = new WampMessages.WelcomeMessage(sessionId, welcomeDetails);
				String encodedMessage = WampDecode.encode(wm);
				debug("--> SendMessage(welcome):" + encodedMessage);
				m_wsSession.getRemote().sendStringByFuture(encodedMessage);
				m_context.safeTrigger(sessionStarted);
			}
		}).whenEnter(SESSION_IDLE, new ContextHandler<FlowContext>() {
			@Override
			public void call(FlowContext context) throws Exception {
				debug("    SESSION_IDLE");
				//m_context.safeTrigger(jobReady);
			}
		}).whenEnter(SUBSCRIPING, new ContextHandler<FlowContext>() {
			@Override
			public void call(FlowContext context) throws Exception {
				SubscribeMessage sub = ((WampMessages.SubscribeMessage)context.last);
				debug("    SUBSCRIPING");
				SubscribedMessage subscribed = new SubscribedMessage(sub.requestId,123456789);
				String encodedMessage = WampDecode.encode(subscribed);
				debug("--> SendMessage(subscribed):" + encodedMessage);
				m_wsSession.getRemote().sendString(encodedMessage);

				m_context.safeTrigger(jobReady);
			}
		}).whenEnter(PUBLISHING, new ContextHandler<FlowContext>() {
			@Override
			public void call(FlowContext context) throws Exception {
				PublishMessage pub = ((WampMessages.PublishMessage)context.last);
				debug("    PUBLISHING");
				PublishedMessage published = new PublishedMessage(pub.requestId,1234567890);

				boolean sendAcknowledge = false;
				JsonNode ackOption = pub.options.get("acknowledge");
				if (ackOption != null && ackOption.asBoolean() == true){
						sendAcknowledge = true;
				}
				if( !sendAcknowledge){
					return;
				}
				String encodedMessage = WampDecode.encode(published);
				debug("--> SendMessage(published):" + encodedMessage);
				m_wsSession.getRemote().sendString(encodedMessage);

				m_context.safeTrigger(jobReady);
			}
		}).whenEnter(REGISTERING, new ContextHandler<FlowContext>() {
			@Override
			public void call(FlowContext context) throws Exception {
				debug("    REGISTERING");
				m_context.safeTrigger(jobReady);
			}
		}).whenEnter(EXECUTE_CALL, new ContextHandler<FlowContext>() {
			@Override
			public void call(FlowContext context) throws Exception {
				debug("    EXECUTE_CALL");
				m_context.safeTrigger(jobReady);
			}
		});
	}

	public void wsConnect(Session sess) {
		m_wsSession = sess;
		debug("<-- SocketConnect");
		m_context.safeTrigger(websocketConnection);
	}

	public void wsBinaryMessage(byte[] payload, int offset, int len) {
		try {
			debug("BinMessage.recveived:" + payload);
			WampMessage msg = WampDecode.decode(payload);
			debug("Message.recveived:" + msg);
		} catch (Exception e) {
			e.printStackTrace();

		}
	}

	public void wsMessage(String message) {
		try {
			WampMessage msg = WampDecode.decode(message.getBytes());
			m_context.last = msg;
			EventEnum e = getMessageEnum(msg);
			debug("<-- ReceiveMessage(" + e + "):" + message);
			m_context.safeTrigger(e);
		} catch (Exception e) {
			e.printStackTrace();

		}
	}

	public void wsClose(int statusCode, String reason) {
		debug("<-- SocketClose:"+statusCode+"/"+reason);
		
	}

	public void wsError(Throwable cause) {
		debug("<-- SocketError:"+cause);
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

