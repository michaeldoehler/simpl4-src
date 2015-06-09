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
import java.util.ArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.eclipse.jetty.websocket.api.Session;
import org.ms123.common.wamp.BaseWebSocket;
import org.ms123.common.wamp.WampMessages.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.exceptions.OnErrorThrowable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observer;
import rx.Scheduler;
import rx.schedulers.Schedulers;
import rx.subjects.AsyncSubject;
import rx.subjects.BehaviorSubject;
import rx.Subscriber;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;
import static org.ms123.common.wamp.WampClientSession.State.*;

/**
 *
 */
class WampClientSession {

	enum State {
		DISCONNECTED, CONNECTED, SESSION
	}

    /**
     * Possible states for a WAMP session between client and router
     */
    public static enum Status {
        /** The session is not connected */
        Disconnected,
        /** The session is trying to connect to the router */
        Connecting,
        /** The session is connected to the router */
        Connected
    }

    /** The current status */
    Status status = Status.Disconnected;
    BehaviorSubject<Status> statusObservable = BehaviorSubject.create(Status.Disconnected);
	private ObjectMapper m_objectMapper = new ObjectMapper();
	private SessionContext m_context = new SessionContext();
	private Realm m_realm;
	private State state = DISCONNECTED;
    ObjectNode welcomeDetails = null;
    long sessionId;
//    final WampRoles[] clientRoles;
    WampRoles[] routerRoles;

    final ObjectMapper objectMapper = new ObjectMapper();

    

    /** Returns the name of the realm on the router */
    
    final boolean closeClientOnErrors=false;;
    boolean isCompleted = false;

    enum PubSubState {
        Subscribing,
        Subscribed,
        Unsubscribing,
        Unsubscribed
    }
    
    enum RegistrationState {
        Registering,
        Registered,
        Unregistering,
        Unregistered
    }

    static class RequestMapEntry {
        public final int requestType;
        public final AsyncSubject<?> resultSubject;
        
        public RequestMapEntry(int requestType, AsyncSubject<?> resultSubject) {
            this.requestType = requestType;
            this.resultSubject = resultSubject;
        }
    }
    
    static class SubscriptionMapEntry {
        public PubSubState state;
        public final SubscriptionFlags flags;
        public long subscriptionId = 0;
        
        public final List<Subscriber<? super PubSubData>> subscribers
            = new ArrayList<Subscriber<? super PubSubData>>();
        
        public SubscriptionMapEntry(SubscriptionFlags flags, PubSubState state) {
            this.flags = flags;
            this.state = state;
        }
    }
    
    static class RegisteredProceduresMapEntry {
        public RegistrationState state;
        public long registrationId = 0;
        /*public final Subscriber<? super Request> subscriber;
        
        public RegisteredProceduresMapEntry(Subscriber<? super Request> subscriber, RegistrationState state) {
            this.subscriber = subscriber;
            this.state = state;
        }*/
    }
    
    HashMap<Long, RequestMapEntry> requestMap = 
        new HashMap<Long, WampClientSession.RequestMapEntry>();

    EnumMap<SubscriptionFlags, HashMap<String, SubscriptionMapEntry>> subscriptionsByFlags =
        new EnumMap<SubscriptionFlags, HashMap<String, SubscriptionMapEntry>>(SubscriptionFlags.class);
    HashMap<Long, SubscriptionMapEntry> subscriptionsBySubscriptionId =
        new HashMap<Long, SubscriptionMapEntry>();
    
    HashMap<String, RegisteredProceduresMapEntry> registeredProceduresByUri = 
            new HashMap<String, RegisteredProceduresMapEntry>();
    HashMap<Long, RegisteredProceduresMapEntry> registeredProceduresById = 
            new HashMap<Long, RegisteredProceduresMapEntry>();
    




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


	protected WampClientSession(BaseWebSocket ws, Realm realm) {
		m_realm = realm;
		//m_context.webSocket = ws;
	}


	private void handleMessage(SessionContext context, WampMessage msg) {
		if (welcomeDetails == null) {
			// We were not yet welcomed
			if (msg instanceof WelcomeMessage) {
				// Receive a welcome. Now the session is established!
				welcomeDetails = ((WelcomeMessage) msg).details;
				sessionId = ((WelcomeMessage) msg).sessionId;

				// Extract the roles of the remote side
				JsonNode roleNode = welcomeDetails.get("roles");
				if (roleNode == null || !roleNode.isObject()) {
					onProtocolError();
					return;
				}

				routerRoles = null;
				Set<WampRoles> rroles = new HashSet<WampRoles>();
				Iterator<String> roleKeys = roleNode.fieldNames();
				while (roleKeys.hasNext()) {
					WampRoles role = WampRoles.fromString(roleKeys.next());
					if (role != null) rroles.add(role);
				}
				routerRoles = new WampRoles[rroles.size()];
				int i = 0;
				for (WampRoles r : rroles) {
					routerRoles[i] = r; 
					i++;
				}

				status = Status.Connected;
				statusObservable.onNext(status);
			}
			else if (msg instanceof AbortMessage) {
				// The remote doesn't want us to connect :(
				AbortMessage abort = (AbortMessage) msg;
				onSessionError(new ApplicationError(abort.reason));
			}
		}
		else {
			// We were already welcomed
			if (msg instanceof WelcomeMessage) {
				onProtocolError();
			}
			else if (msg instanceof AbortMessage) {
				onProtocolError();
			}
			else if (msg instanceof GoodbyeMessage) {
				// Reply the goodbye
				//@@@MS channel.writeAndFlush(new GoodbyeMessage(null, ApplicationError.GOODBYE_AND_OUT));
				// We could also use the reason from the msg, but this would be harder
				// to determinate from a "real" error
				onSessionError(new ApplicationError(ApplicationError.GOODBYE_AND_OUT));
			}
			else if (msg instanceof ResultMessage) {
				ResultMessage r = (ResultMessage)msg;
				RequestMapEntry requestInfo = requestMap.get(r.requestId);
				if (requestInfo == null) return; // Ignore the result
				if (requestInfo.requestType != WampMessages.CallMessage.ID) {
					onProtocolError();
					return;
				}
				requestMap.remove(r.requestId);
				Reply reply = new Reply(r.arguments, r.argumentsKw);
				@SuppressWarnings("unchecked")
					AsyncSubject<Reply> subject = (AsyncSubject<Reply>)requestInfo.resultSubject;
				subject.onNext(reply);
				subject.onCompleted();
			}
			else if (msg instanceof ErrorMessage) {
				ErrorMessage r = (ErrorMessage)msg;
				if (r.requestType == WampMessages.CallMessage.ID
						|| r.requestType == WampMessages.SubscribeMessage.ID
						|| r.requestType == WampMessages.UnsubscribeMessage.ID
						|| r.requestType == WampMessages.PublishMessage.ID
						|| r.requestType == WampMessages.RegisterMessage.ID
						|| r.requestType == WampMessages.UnregisterMessage.ID)
				{
					RequestMapEntry requestInfo = requestMap.get(r.requestId);
					if (requestInfo == null) return; // Ignore the error
					// Check whether the request type we sent equals the
					// request type for the error we receive
					if (requestInfo.requestType != r.requestType) {
						onProtocolError();
						return;
					}
					requestMap.remove(r.requestId);
					ApplicationError err = new ApplicationError(r.error, r.arguments, r.argumentsKw);
					requestInfo.resultSubject.onError(err);
				}
			}
			else if (msg instanceof SubscribedMessage) {
				SubscribedMessage m = (SubscribedMessage)msg;
				RequestMapEntry requestInfo = requestMap.get(m.requestId);
				if (requestInfo == null) return; // Ignore the result
				if (requestInfo.requestType != WampMessages.SubscribeMessage.ID) {
					onProtocolError();
					return;
				}
				requestMap.remove(m.requestId);
				@SuppressWarnings("unchecked")
					AsyncSubject<Long> subject = (AsyncSubject<Long>)requestInfo.resultSubject;
				subject.onNext(m.subscriptionId);
				subject.onCompleted();
			}
			else if (msg instanceof UnsubscribedMessage) {
				UnsubscribedMessage m = (UnsubscribedMessage)msg;
				RequestMapEntry requestInfo = requestMap.get(m.requestId);
				if (requestInfo == null) return; // Ignore the result
				if (requestInfo.requestType != WampMessages.UnsubscribeMessage.ID) {
					onProtocolError();
					return;
				}
				requestMap.remove(m.requestId);
				@SuppressWarnings("unchecked")
					AsyncSubject<Void> subject = (AsyncSubject<Void>)requestInfo.resultSubject;
				subject.onNext(null);
				subject.onCompleted();
			}
			else if (msg instanceof EventMessage) {
				EventMessage ev = (EventMessage)msg;
				SubscriptionMapEntry entry = subscriptionsBySubscriptionId.get(ev.subscriptionId);
				if (entry == null || entry.state != PubSubState.Subscribed) return; // Ignore the result
				PubSubData evResult = new PubSubData(ev.details, ev.arguments, ev.argumentsKw);
				// publish the event
				for (Subscriber<? super PubSubData> s : entry.subscribers) {
					s.onNext(evResult);
				}
			}
			else if (msg instanceof PublishedMessage) {
				PublishedMessage m = (PublishedMessage)msg;
				RequestMapEntry requestInfo = requestMap.get(m.requestId);
				if (requestInfo == null) return; // Ignore the result
				if (requestInfo.requestType != WampMessages.PublishMessage.ID) {
					onProtocolError();
					return;
				}
				requestMap.remove(m.requestId);
				@SuppressWarnings("unchecked")
					AsyncSubject<Long> subject = (AsyncSubject<Long>)requestInfo.resultSubject;
				subject.onNext(m.publicationId);
				subject.onCompleted();
			}
			else if (msg instanceof RegisteredMessage) {
				RegisteredMessage m = (RegisteredMessage)msg;
				RequestMapEntry requestInfo = requestMap.get(m.requestId);
				if (requestInfo == null) return; // Ignore the result
				if (requestInfo.requestType != WampMessages.RegisterMessage.ID) {
					onProtocolError();
					return;
				}
				requestMap.remove(m.requestId);
				@SuppressWarnings("unchecked")
					AsyncSubject<Long> subject = (AsyncSubject<Long>)requestInfo.resultSubject;
				subject.onNext(m.registrationId);
				subject.onCompleted();
			}
			else if (msg instanceof UnregisteredMessage) {
				UnregisteredMessage m = (UnregisteredMessage)msg;
				RequestMapEntry requestInfo = requestMap.get(m.requestId);
				if (requestInfo == null) return; // Ignore the result
				if (requestInfo.requestType != WampMessages.UnregisterMessage.ID) {
					onProtocolError();
					return;
				}
				requestMap.remove(m.requestId);
				@SuppressWarnings("unchecked")
					AsyncSubject<Void> subject = (AsyncSubject<Void>)requestInfo.resultSubject;
				subject.onNext(null);
				subject.onCompleted();
			}
			else if (msg instanceof InvocationMessage) {
				InvocationMessage m = (InvocationMessage)msg;
				RegisteredProceduresMapEntry entry = registeredProceduresById.get(m.registrationId);
				if (entry == null || entry.state != RegistrationState.Registered) {
					// Send an error that we are no longer registered
					//@@@MS channel.writeAndFlush(new ErrorMessage(InvocationMessage.ID, m.requestId, null, ApplicationError.NO_SUCH_PROCEDURE, null, null));
				}
				else {
					// Send the request to the subscriber, which can then send responses
					//@@@MS Request request = new Request(this, channel, m.requestId, m.arguments, m.argumentsKw);
					//@@@MSentry.subscriber.onNext(request);
				}
			}
			else {
				// Unknown message
			}
		}
	}


	private void onProtocolError() {
		onSessionError(new ApplicationError(ApplicationError.PROTCOL_ERROR));
	}

	private void onSessionError(ApplicationError error) {
		// We move from connected to disconnected
		//closeCurrentTransport();
		statusObservable.onNext(status);

		if (closeClientOnErrors) {
			completeStatus(error);
		}
	}
	private void completeStatus(Exception e) {
		if (isCompleted) return;
		isCompleted = true;

		if (e != null)
			statusObservable.onError(e);
		else
			statusObservable.onCompleted();
	}

	public void onWebSocketConnect(Session sess) {
		debug("<-- SocketConnect");
		state = CONNECTED;
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
		//m_context.realm.removeSession(m_context, true);
		state = DISCONNECTED;
	}

	public void onWebSocketError(Throwable cause) {
		debug("<-- SocketError:" + cause);
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

	private static final Logger m_logger = LoggerFactory.getLogger(WampClientSession.class);
}
