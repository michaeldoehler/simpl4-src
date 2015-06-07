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
import org.ms123.common.wamp.WampMessages.AbortMessage;
import org.ms123.common.wamp.WampMessages.CallMessage;
import org.ms123.common.wamp.WampMessages.ErrorMessage;
import org.ms123.common.wamp.WampMessages.EventMessage;
import org.ms123.common.wamp.WampMessages.HelloMessage;
import org.ms123.common.wamp.WampMessages.InvocationMessage;
import org.ms123.common.wamp.WampMessages.PublishedMessage;
import org.ms123.common.wamp.WampMessages.PublishMessage;
import org.ms123.common.wamp.WampMessages.RegisteredMessage;
import org.ms123.common.wamp.WampMessages.RegisterMessage;
import org.ms123.common.wamp.WampMessages.ResultMessage;
import org.ms123.common.wamp.WampMessages.SubscribedMessage;
import org.ms123.common.wamp.WampMessages.SubscribeMessage;
import org.ms123.common.wamp.WampMessages.UnregisteredMessage;
import org.ms123.common.wamp.WampMessages.UnregisterMessage;
import org.ms123.common.wamp.WampMessages.UnsubscribedMessage;
import org.ms123.common.wamp.WampMessages.UnsubscribeMessage;
import org.ms123.common.wamp.WampMessages.WampMessage;
import org.ms123.common.wamp.WampMessages.WelcomeMessage;
import org.ms123.common.wamp.WampMessages.YieldMessage;
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
		WEBSOCKET_CREATED, CONNECTED, SESSION_START, SESSION_IDLE, SUBSCRIPING, UNSUBSCRIPING, PUBLISHING, RESULT, EXECUTE_CALL, REGISTERING, UNREGISTERING, ERROR
	}

	enum Events implements EventEnum {
		websocketConnection, sessionStarted, hello, register, unregister, subscribe, unsubscribe, publish, call, yield, error, jobReady
	}

	protected WampRouterSession(WampService wampService, WampServiceImpl.WebSocket ws, Map<String, Realm> realms) {
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
							on(unregister).to(UNREGISTERING).transit(on(jobReady).to(SESSION_IDLE)),
							on(unsubscribe).to(UNSUBSCRIPING).transit(on(jobReady).to(SESSION_IDLE)),
							on(yield).to(RESULT).transit(on(jobReady).to(SESSION_IDLE)),
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
				roles.add(WampRoles.Broker);
				ObjectNode welcomeDetails = m_objectMapper.createObjectNode();
				welcomeDetails.put("agent", "simpl4-1.0");
				ObjectNode routerRoles = welcomeDetails.putObject("roles");
				ObjectNode roleNode = routerRoles.putObject("broker");
				WelcomeMessage wm = new WampMessages.WelcomeMessage(sessionId, welcomeDetails);
				String encodedMessage = WampDecode.encode(wm);
				debug("--> SendMessage(welcome):" + encodedMessage);
				context.webSocket.sendStringByFuture(encodedMessage);
				context.safeTrigger(sessionStarted);
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
				SubscribeMessage sub = (WampMessages.SubscribeMessage) context.currentMsg;
				debug("    SUBSCRIPING");

				String err = null;
				SubscriptionFlags flags = SubscriptionFlags.Exact;
				if (sub.options != null) {
					JsonNode match = sub.options.get("match");
					if (match != null) {
						String matchValue = match.asText();
						if ("prefix".equals(matchValue)) {
							flags = SubscriptionFlags.Prefix;
						} else if ("wildcard".equals(matchValue)) {
							flags = SubscriptionFlags.Wildcard;
						}
					}
				}
				if (!UriValidator.tryValidate(sub.topic, context.realm.config.useStrictUriValidation,
							flags == SubscriptionFlags.Wildcard)) {
					err = ApplicationError.INVALID_URI;
				}

				if (err == null && !(IdValidator.isValidId(sub.requestId))) {
					err = ApplicationError.INVALID_ARGUMENT;
				}

				if (err != null) {
					String errMsg = WampDecode.encode(new ErrorMessage(SubscribeMessage.ID, sub.requestId, null, err, null, null));
					info("   ErrorMessage:" + errMsg);
					context.webSocket.sendStringByFuture(errMsg);
					context.safeTrigger(jobReady);
					return;
				}

				if (context.subscriptionsById == null) {
					context.subscriptionsById = new HashMap<Long, Subscription>();
				}

				Map<String, Subscription> subscriptionMap = context.realm.subscriptionsByFlags.get(flags);
				Subscription subscription = subscriptionMap.get(sub.topic);
				if (subscription == null) {
					long subscriptionId = IdGenerator.newLinearId(context.realm.lastUsedSubscriptionId, context.realm.subscriptionsById);
					context.realm.lastUsedSubscriptionId = subscriptionId;
					subscription = new Subscription(sub.topic, flags, subscriptionId);
					subscriptionMap.put(sub.topic, subscription);
					context.realm.subscriptionsById.put(subscriptionId, subscription);
				}

				if (subscription.subscribers.add(context)) {
					context.subscriptionsById.put(subscription.subscriptionId, subscription);
				}

				String subscribed = WampDecode.encode(new SubscribedMessage(sub.requestId, subscription.subscriptionId));
				debug("--> SendMessage(subscribed):" + subscribed);
				context.webSocket.sendStringByFuture(subscribed);
				context.safeTrigger(jobReady);
				}
		}).whenEnter(UNSUBSCRIPING, new ContextHandler<SessionContext>() {
			@Override
			public void call(SessionContext context) throws Exception {
				debug("    UNSUBSCRIPING");
				UnsubscribeMessage unsub = (UnsubscribeMessage) context.currentMsg;
				String err = null;
				if (!(IdValidator.isValidId(unsub.requestId)) || !(IdValidator.isValidId(unsub.subscriptionId))) {
					err = ApplicationError.INVALID_ARGUMENT;
				}

				Subscription s = null;
				if (err == null) {
					if (context.subscriptionsById != null) {
						s = context.subscriptionsById.get(unsub.subscriptionId);
					}
					if (s == null) {
						err = ApplicationError.NO_SUCH_SUBSCRIPTION;
					}
				}

				if (err != null) { 
					String errMsg = WampDecode.encode(new ErrorMessage(UnsubscribeMessage.ID, unsub.requestId, null, err, null, null));
					info("   ErrorMessage:" + errMsg);
					context.webSocket.sendStringByFuture(errMsg);
					context.safeTrigger(jobReady);
					return;
				}

				s.subscribers.remove(context);
				context.subscriptionsById.remove(s.subscriptionId);
				if (context.subscriptionsById.isEmpty()) {
						context.subscriptionsById = null;
				}
				
				if (s.subscribers.isEmpty()) {
						context.realm.subscriptionsByFlags.get(s.flags).remove(s.topic);
						context.realm.subscriptionsById.remove(s.subscriptionId);
				}
				
				String unsubscribed = WampDecode.encode(new UnsubscribedMessage(unsub.requestId));
				context.webSocket.sendStringByFuture(unsubscribed);
				context.safeTrigger(jobReady);
			}
		}).whenEnter(PUBLISHING, new ContextHandler<SessionContext>() {
			@Override
			public void call(SessionContext context) throws Exception {
				PublishMessage pub = ((WampMessages.PublishMessage) context.currentMsg);
				debug("    PUBLISHING");

				boolean sendAcknowledge = false;
				JsonNode ackOption = pub.options.get("acknowledge");
				if (ackOption != null && ackOption.asBoolean() == true) {
					sendAcknowledge = true;
				}

				String err = null;
				if (!UriValidator.tryValidate(pub.topic, context.realm.config.useStrictUriValidation)) {
					err = ApplicationError.INVALID_URI;
				}
				
				if (err == null && !(IdValidator.isValidId(pub.requestId))) {
					err = ApplicationError.INVALID_ARGUMENT;
				}
				
				if (err != null) { 
					if (sendAcknowledge) {
						String errMsg = WampDecode.encode(new ErrorMessage(PublishMessage.ID, pub.requestId, null, err, null, null));
						context.webSocket.sendStringByFuture(errMsg);
					}
					context.safeTrigger(jobReady);
					return;
				}
				
				long publicationId = IdGenerator.newRandomId(null); // Store that somewhere?

				Subscription exactSubscription = context.realm.subscriptionsByFlags.get(SubscriptionFlags.Exact).get(pub.topic);
				if (exactSubscription != null) {
					publishEvent(context, pub, publicationId, exactSubscription);
				}

				Map<String, Subscription> prefixSubscriptionMap = context.realm.subscriptionsByFlags.get(SubscriptionFlags.Prefix);
				for (Subscription prefixSubscription : prefixSubscriptionMap.values()) {
					if (pub.topic.startsWith(prefixSubscription.topic)) {
						publishEvent(context, pub, publicationId, prefixSubscription);
					}
				}

				Map<String, Subscription> wildcardSubscriptionMap = context.realm.subscriptionsByFlags.get(SubscriptionFlags.Wildcard);
				String[] components = pub.topic.split("\\.", -1);
				for (Subscription wildcardSubscription : wildcardSubscriptionMap.values()) {
					boolean matched = true;
					if (components.length == wildcardSubscription.components.length) {
						for (int i=0; i < components.length; i++) {
							if (wildcardSubscription.components[i].length() > 0
									&& !components[i].equals(wildcardSubscription.components[i])) {
								matched = false;
								break;
							}
						}
					}else
						matched = false;

					if (matched) {
						publishEvent(context, pub, publicationId, wildcardSubscription);
					}
				}

				if (sendAcknowledge) {
						String published = WampDecode.encode(new PublishedMessage(pub.requestId, publicationId));
						debug("--> SendMessage(published):" + published);
						context.webSocket.sendStringByFuture(published);
				}

				context.safeTrigger(jobReady);
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
					proc = context.realm.procedures.get(reg.procedure);
					if (proc != null)
						err = ApplicationError.PROCEDURE_ALREADY_EXISTS;
				}

				if (err != null) {
					String errMsg = WampDecode.encode(new ErrorMessage(RegisterMessage.ID, reg.requestId, null, err, null, null));
					info("   ErrorMessage:" + errMsg);
					context.webSocket.sendStringByFuture(errMsg);
					context.safeTrigger(jobReady);
					return;
				}
				// Everything checked, we can register the caller as the procedure provider
				long registrationId = IdGenerator.newLinearId(context.lastUsedId, context.providedProcedures);
				context.lastUsedId = registrationId;
				Procedure procInfo = new Procedure(reg.procedure, context, registrationId);

				// Insert new procedure
				context.realm.procedures.put(reg.procedure, procInfo);
				if (context.providedProcedures == null) {
					context.providedProcedures = new HashMap<Long, Procedure>();
					context.pendingInvocations = new HashMap<Long, Invocation>();
				}
				context.providedProcedures.put(procInfo.registrationId, procInfo);

				String response = WampDecode.encode(new RegisteredMessage(reg.requestId, procInfo.registrationId));
				context.webSocket.sendStringByFuture(response);
				context.safeTrigger(jobReady);
			}
		}).whenEnter(UNREGISTERING, new ContextHandler<SessionContext>() {
			@Override
			public void call(SessionContext context) throws Exception {
				UnregisterMessage unreg = ((UnregisterMessage) context.currentMsg);
				debug("    UNREGISTERING");

				String err = null;
				if (!(IdValidator.isValidId(unreg.requestId))
				 || !(IdValidator.isValidId(unreg.registrationId))) {
						err = ApplicationError.INVALID_ARGUMENT;
				}
				
				Procedure proc = null;
				if (err == null) {
					if (context.providedProcedures != null) {
						proc = context.providedProcedures.get(unreg.registrationId);
					}
					if (proc == null) {
						err = ApplicationError.NO_SUCH_REGISTRATION;
					}
				}
				
				if (err != null) {
					String errMsg = WampDecode.encode(new ErrorMessage(UnregisterMessage.ID, unreg.requestId, null, err, null, null));
					context.webSocket.sendStringByFuture(errMsg);
					context.safeTrigger(jobReady);
					return;
				}
				
				for (Invocation invoc : proc.pendingCalls) {
					context.pendingInvocations.remove(invoc.invocationRequestId);
					if (invoc.caller.isConnected()) {
						String errMsg = WampDecode.encode(new ErrorMessage(CallMessage.ID, invoc.callRequestId, null, ApplicationError.NO_SUCH_PROCEDURE, null, null));
						context.webSocket.sendStringByFuture(errMsg);
					}
				}
				proc.pendingCalls.clear();

				context.realm.procedures.remove(proc.procName);
				context.providedProcedures.remove(proc.registrationId);
				
				if (context.providedProcedures.size() == 0) {
					context.providedProcedures = null;
					context.pendingInvocations = null;
				}
				
				String unregister = WampDecode.encode(new UnregisteredMessage(unreg.requestId));
				context.webSocket.sendStringByFuture(unregister);
				context.safeTrigger(jobReady);
			}
		}).whenEnter(EXECUTE_CALL, new ContextHandler<SessionContext>() {
			@Override
			public void call(SessionContext context) throws Exception {
				debug("    EXECUTE_CALL");
				CallMessage callMsg = (CallMessage) context.currentMsg;
				String err = null;
				if (!UriValidator.tryValidate(callMsg.procedure, context.realm.config.useStrictUriValidation)) {
					err = ApplicationError.INVALID_URI;
				}

				if (err == null && !(IdValidator.isValidId(callMsg.requestId))) {
					err = ApplicationError.INVALID_ARGUMENT;
				}

				Procedure proc = null;
				if (err == null) {
					proc = context.realm.procedures.get(callMsg.procedure);
					if (proc == null)
						err = ApplicationError.NO_SUCH_PROCEDURE;
				}
				if (err != null) {
					String errMsg = WampDecode.encode(new ErrorMessage(CallMessage.ID, callMsg.requestId, null, err, null, null));
					context.webSocket.sendStringByFuture(errMsg);
					context.safeTrigger(jobReady);
					return;
				}

				Invocation invoc = new Invocation();
				invoc.callRequestId = callMsg.requestId;
				invoc.caller = context.webSocket;
				invoc.procedure = proc;
				invoc.invocationRequestId = IdGenerator.newLinearId(context.lastUsedId, context.pendingInvocations);
				context.lastUsedId = invoc.invocationRequestId;

				if (proc.context.pendingInvocations == null) {
					proc.context.pendingInvocations = new HashMap<Long, Invocation>();
				}
				proc.context.pendingInvocations.put(invoc.invocationRequestId, invoc);
				proc.pendingCalls.add(invoc);

				String imsg = WampDecode.encode(new InvocationMessage(invoc.invocationRequestId, proc.registrationId, null, callMsg.arguments, callMsg.argumentsKw));
				debug("    InvocationMessage:" + imsg);
				proc.provider.sendStringByFuture(imsg);
				context.safeTrigger(jobReady);
			}
		}).whenEnter(RESULT, new ContextHandler<SessionContext>() {
			@Override
			public void call(SessionContext context) throws Exception {
				debug("    RESULT");
				YieldMessage yieldMsg = (YieldMessage) context.currentMsg;

				if (!(IdValidator.isValidId(yieldMsg.requestId))) {
					context.safeTrigger(jobReady);
					return;
				}
				debug("    Result.pendingInvocations:" + context.pendingInvocations);
				if (context.pendingInvocations == null) {
					context.safeTrigger(jobReady);
					return;
				}
				Invocation invoc = context.pendingInvocations.get(yieldMsg.requestId);
				if (invoc == null) {
					context.safeTrigger(jobReady);
					return;
				}
				context.pendingInvocations.remove(yieldMsg.requestId);
				invoc.procedure.pendingCalls.remove(invoc);
				String result = WampDecode.encode(new ResultMessage(invoc.callRequestId, null, yieldMsg.arguments, yieldMsg.argumentsKw));
				debug("    SendResult:" + result);
				invoc.caller.sendStringByFuture(result);
				context.safeTrigger(jobReady);
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
		m_context.realm.removeSession(m_context, true);

	}

	public void onWebSocketError(Throwable cause) {
		debug("<-- SocketError:" + cause);
	}

	private void publishEvent(SessionContext publisher, PublishMessage pub, long publicationId, Subscription subscription){
		ObjectNode details = null;
		if (subscription.flags != SubscriptionFlags.Exact) {
			details = m_objectMapper.createObjectNode();
			details.put("topic", pub.topic);
		}

		String ev = WampDecode.encode(new EventMessage(subscription.subscriptionId, publicationId, details, pub.arguments, pub.argumentsKw));

		for (SessionContext receiver : subscription.subscribers) {
			if (receiver == publisher ) { 
				boolean skipPublisher = true;
				if (pub.options != null) {
					JsonNode excludeMeNode = pub.options.get("exclude_me");
					if (excludeMeNode != null) {
						skipPublisher = excludeMeNode.asBoolean(true);
					}
				}
				if (skipPublisher) continue;
			}

			receiver.webSocket.sendStringByFuture(ev);
		}
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

