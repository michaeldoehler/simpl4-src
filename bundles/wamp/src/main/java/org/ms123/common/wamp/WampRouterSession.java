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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.eclipse.jetty.websocket.api.Session;
import org.ms123.common.wamp.BaseWebSocket;
import org.ms123.common.wamp.WampMessages.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.ms123.common.wamp.WampRouterSession.State.*;

/**
 *
 */
class WampRouterSession {

	enum State {
		DISCONNECTED, CONNECTED, SESSION
	}

	private ObjectMapper m_objectMapper = new ObjectMapper();
	private SessionContext m_context = new SessionContext();
	private Map<String, Realm> m_realms;
	private State state = DISCONNECTED;

	protected static class SessionContext {
		long sessionId;
		long lastUsedId = IdValidator.MIN_VALID_ID;
		Realm realm;
		Set<WampRoles> roles;
		Map<Long, Procedure> providedProcedures;
		Map<Long, Invocation> pendingInvocations;
		Map<Long, Subscription> subscriptionsById;
		BaseWebSocket webSocket;
	}


	protected WampRouterSession(BaseWebSocket ws, Map<String, Realm> realms) {
		m_realms = realms;
		m_context.webSocket = ws;
	}

	private void handleMessage(SessionContext context, WampMessage msg) {
		if (state == CONNECTED && msg instanceof HelloMessage) {
			debug("    SESSION_START");
			HelloMessage hello = ((HelloMessage) msg);
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
				String abort = WampCodec.encode(new AbortMessage(null, errorMsg));
				debug("--> SendMessage(abort):" + abort);
				context.webSocket.sendStringByFuture(abort);
				return;
			}
			long sessionId = IdGenerator.newRandomId(null);
			Set<WampRoles> roles = new HashSet<WampRoles>();
			realm.includeSession(context, sessionId, roles);
			roles.add(WampRoles.Broker);
			ObjectNode welcomeDetails = m_objectMapper.createObjectNode();
			welcomeDetails.put("agent", "simpl4-1.0");
			ObjectNode routerRoles = welcomeDetails.putObject("roles");
			ObjectNode roleNode = routerRoles.putObject("broker");
			String wm = WampCodec.encode(new WampMessages.WelcomeMessage(sessionId, welcomeDetails));
			debug("--> SendMessage(welcome):" + wm);
			context.webSocket.sendStringByFuture(wm);
			state = SESSION;
		}
		if (state != SESSION) {
			debug("Unexpected Message:" + msg);
			return;
		}
		if (msg instanceof SubscribeMessage) {
			SubscribeMessage sub = (WampMessages.SubscribeMessage) msg;
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
			if (!UriValidator.tryValidate(sub.topic, context.realm.config.useStrictUriValidation, flags == SubscriptionFlags.Wildcard)) {
				err = ApplicationError.INVALID_URI;
			}
			if (err == null && !(IdValidator.isValidId(sub.requestId))) {
				err = ApplicationError.INVALID_ARGUMENT;
			}
			if (err != null) {
				String errMsg = WampCodec.encode(new ErrorMessage(SubscribeMessage.ID, sub.requestId, null, err, null, null));
				info("   ErrorMessage:" + errMsg);
				context.webSocket.sendStringByFuture(errMsg);
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
			String subscribed = WampCodec.encode(new SubscribedMessage(sub.requestId, subscription.subscriptionId));
			debug("--> SendMessage(subscribed):" + subscribed);
			context.webSocket.sendStringByFuture(subscribed);
		}
		if (msg instanceof UnsubscribeMessage) {
			debug("    UNSUBSCRIPING");
			UnsubscribeMessage unsub = (UnsubscribeMessage) msg;
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
				String errMsg = WampCodec.encode(new ErrorMessage(UnsubscribeMessage.ID, unsub.requestId, null, err, null, null));
				info("   ErrorMessage:" + errMsg);
				context.webSocket.sendStringByFuture(errMsg);
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
			String unsubscribed = WampCodec.encode(new UnsubscribedMessage(unsub.requestId));
			context.webSocket.sendStringByFuture(unsubscribed);
		}
		if (msg instanceof PublishMessage) {
			PublishMessage pub = ((WampMessages.PublishMessage) msg);
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
					String errMsg = WampCodec.encode(new ErrorMessage(PublishMessage.ID, pub.requestId, null, err, null, null));
					context.webSocket.sendStringByFuture(errMsg);
				}
				return;
			}
			long publicationId = IdGenerator.newRandomId(null);
			// Store that somewhere?
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
					for (int i = 0; i < components.length; i++) {
						if (wildcardSubscription.components[i].length() > 0 && !components[i].equals(wildcardSubscription.components[i])) {
							matched = false;
							break;
						}
					}
				} else
					matched = false;
				if (matched) {
					publishEvent(context, pub, publicationId, wildcardSubscription);
				}
			}
			if (sendAcknowledge) {
				String published = WampCodec.encode(new PublishedMessage(pub.requestId, publicationId));
				debug("--> SendMessage(published):" + published);
				context.webSocket.sendStringByFuture(published);
			}
		}
		if (msg instanceof RegisterMessage) {
			RegisterMessage reg = ((WampMessages.RegisterMessage) msg);
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
				String errMsg = WampCodec.encode(new ErrorMessage(RegisterMessage.ID, reg.requestId, null, err, null, null));
				info("   ErrorMessage:" + errMsg);
				context.webSocket.sendStringByFuture(errMsg);
				return;
			}
			long registrationId = IdGenerator.newLinearId(context.lastUsedId, context.providedProcedures);
			context.lastUsedId = registrationId;
			Procedure procInfo = new Procedure(reg.procedure, context, registrationId);

			context.realm.procedures.put(reg.procedure, procInfo);
			if (context.providedProcedures == null) {
				context.providedProcedures = new HashMap<Long, Procedure>();
				context.pendingInvocations = new HashMap<Long, Invocation>();
			}
			context.providedProcedures.put(procInfo.registrationId, procInfo);
			String response = WampCodec.encode(new RegisteredMessage(reg.requestId, procInfo.registrationId));
			context.webSocket.sendStringByFuture(response);
		}
		if (msg instanceof UnregisterMessage) {
			UnregisterMessage unreg = ((UnregisterMessage) msg);
			debug("    UNREGISTERING");
			String err = null;
			if (!(IdValidator.isValidId(unreg.requestId)) || !(IdValidator.isValidId(unreg.registrationId))) {
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
				String errMsg = WampCodec.encode(new ErrorMessage(UnregisterMessage.ID, unreg.requestId, null, err, null, null));
				context.webSocket.sendStringByFuture(errMsg);
				return;
			}
			for (Invocation invoc : proc.pendingCalls) {
				context.pendingInvocations.remove(invoc.invocationRequestId);
				if (invoc.caller.isConnected()) {
					String errMsg = WampCodec.encode(new ErrorMessage(CallMessage.ID, invoc.callRequestId, null, ApplicationError.NO_SUCH_PROCEDURE, null, null));
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
			String unregister = WampCodec.encode(new UnregisteredMessage(unreg.requestId));
			context.webSocket.sendStringByFuture(unregister);
		}
		if (msg instanceof CallMessage) {
			CallMessage callMsg = (CallMessage) msg;
			String err = null;
			if (!UriValidator.tryValidate(callMsg.procedure, context.realm.config.useStrictUriValidation)) {
				err = ApplicationError.INVALID_URI;
			}
			if (err == null && !(IdValidator.isValidId(callMsg.requestId))) {
				err = ApplicationError.INVALID_ARGUMENT;
			}
			Procedure proc = null;
			if (err == null) {
				info("Procedures:" + context.realm.procedures);
				proc = context.realm.procedures.get(callMsg.procedure);
				if (proc == null)
					err = ApplicationError.NO_SUCH_PROCEDURE;
			}
			if (err != null) {
				String errMsg = WampCodec.encode(new ErrorMessage(CallMessage.ID, callMsg.requestId, null, err, null, null));
				info("   ErrorMessage.Call:" + errMsg);
				context.webSocket.sendStringByFuture(errMsg);
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
			String imsg = WampCodec.encode(new InvocationMessage(invoc.invocationRequestId, proc.registrationId, null, callMsg.arguments, callMsg.argumentsKw));
			debug("    InvocationMessage:" + imsg + "/ThreadId:" + Thread.currentThread().getId());
			proc.provider.sendStringByFuture(imsg);
		}
		if (msg instanceof YieldMessage) {
			debug("    RESULT");
			YieldMessage yieldMsg = (YieldMessage) msg;
			if (!(IdValidator.isValidId(yieldMsg.requestId))) {
				return;
			}
			debug("    Result.pendingInvocations:" + context.pendingInvocations);
			if (context.pendingInvocations == null) {
				return;
			}
			Invocation invoc = context.pendingInvocations.get(yieldMsg.requestId);
			if (invoc == null) {
				return;
			}
			context.pendingInvocations.remove(yieldMsg.requestId);
			invoc.procedure.pendingCalls.remove(invoc);
			String result = WampCodec.encode(new ResultMessage(invoc.callRequestId, null, yieldMsg.arguments, yieldMsg.argumentsKw));
			debug("    SendResult:" + result);
			invoc.caller.sendStringByFuture(result);
		}
		if (msg instanceof ErrorMessage) {
			ErrorMessage errorMsg = (ErrorMessage) msg;
			if (errorMsg.requestType == InvocationMessage.ID) {
				Invocation invoc = context.pendingInvocations.get(errorMsg.requestId);
				if (invoc == null) {
					return;
				}
				context.pendingInvocations.remove(errorMsg.requestId);
				invoc.procedure.pendingCalls.remove(invoc);
				errorMsg.requestType = CallMessage.ID;
				errorMsg.requestId = invoc.callRequestId;
				String error = WampCodec.encode(errorMsg);
				debug("    SendError:" + error);
				invoc.caller.sendStringByFuture(error);
			} else {
			}
		}
	}

	public void onWebSocketConnect(Session sess) {
		debug("<-- SocketConnect");
		state = CONNECTED;
	}

	public void onWebSocketBinary(byte[] payload, int offset, int len) {
	}

	public void onWebSocketText(String message) {
		try {
			WampMessage msg = WampCodec.decode(message.getBytes());
			if (msg instanceof ErrorMessage) {
				ErrorMessage errMsg = (ErrorMessage) msg;
				debug("<-- ReceiveMessage(error):" + errMsg.error + "/requestId:" + errMsg.requestId + "/requestType:" + errMsg.requestType);
			} else {
				debug("<-- ReceiveMessage(" + getMessageName(msg) + "):" + message);
			}
			handleMessage(m_context, msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void onWebSocketClose(int statusCode, String reason) {
		debug("<-- SocketClose:" + statusCode + "/" + reason);
		m_context.realm.removeSession(m_context, true);
		state = DISCONNECTED;
	}

	public void onWebSocketError(Throwable cause) {
		debug("<-- SocketError:" + cause);
	}

	private void publishEvent(SessionContext publisher, PublishMessage pub, long publicationId, Subscription subscription) {
		ObjectNode details = null;
		if (subscription.flags != SubscriptionFlags.Exact) {
			details = m_objectMapper.createObjectNode();
			details.put("topic", pub.topic);
		}
		String ev = WampCodec.encode(new EventMessage(subscription.subscriptionId, publicationId, details, pub.arguments, pub.argumentsKw));
		for (SessionContext receiver : subscription.subscribers) {
			if (receiver == publisher) {
				boolean skipPublisher = true;
				if (pub.options != null) {
					JsonNode excludeMeNode = pub.options.get("exclude_me");
					if (excludeMeNode != null) {
						skipPublisher = excludeMeNode.asBoolean(true);
					}
				}
				if (skipPublisher)
					continue;
			}
			receiver.webSocket.sendStringByFuture(ev);
		}
	}

	private String getMessageName(Object o) {
		String s = o.toString();
		int nameEndIndex = s.indexOf("Message@");
		int dollarIndex = s.lastIndexOf("$");
		String name = s.substring(dollarIndex + 1, nameEndIndex);
		return name.toLowerCase();
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
