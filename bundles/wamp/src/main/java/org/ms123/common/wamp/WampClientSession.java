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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
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
		Disconnected, /** The session is trying to connect to the router */
		Connecting, /** The session is connected to the router */
		Connected
	}

	/** The current status */
	Status status = Status.Disconnected;

	BehaviorSubject<Status> statusObservable = BehaviorSubject.create(Status.Disconnected);

	private ObjectMapper m_objectMapper = new ObjectMapper();
	private Realm m_realm;
	private State state = DISCONNECTED;
	ObjectNode welcomeDetails = null;

	final boolean useStrictUriValidation = true;
	long sessionId;

	//    final WampRoles[] clientRoles;
	WampRoles[] routerRoles;

	final ObjectMapper objectMapper = new ObjectMapper();
    final Scheduler scheduler;

	/** Returns the name of the realm on the router */
	final boolean closeClientOnErrors = false;

	long lastRequestId = IdValidator.MIN_VALID_ID;
	WampRouterSession m_wampRouterSession;
	boolean isCompleted = false;

	enum PubSubState {
		Subscribing, Subscribed, Unsubscribing, Unsubscribed
	}

	enum RegistrationState {
		Registering, Registered, Unregistering, Unregistered
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
		public final List<Subscriber<? super PubSubData>> subscribers = new ArrayList<Subscriber<? super PubSubData>>();

		public SubscriptionMapEntry(SubscriptionFlags flags, PubSubState state) {
			this.flags = flags;
			this.state = state;
		}
	}

	static class RegisteredProceduresMapEntry {
		public RegistrationState state;
		public long registrationId = 0;
		public final Subscriber<? super Request> subscriber;

		public RegisteredProceduresMapEntry(Subscriber<? super Request> subscriber, RegistrationState state) {
			this.subscriber = subscriber;
			this.state = state;
		}
	}
	HashMap<Long, RequestMapEntry> requestMap = new HashMap<Long, WampClientSession.RequestMapEntry>();
	EnumMap<SubscriptionFlags, HashMap<String, SubscriptionMapEntry>> subscriptionsByFlags = new EnumMap<SubscriptionFlags, HashMap<String, SubscriptionMapEntry>>(SubscriptionFlags.class);
	HashMap<Long, SubscriptionMapEntry> subscriptionsBySubscriptionId = new HashMap<Long, SubscriptionMapEntry>();
	HashMap<String, RegisteredProceduresMapEntry> registeredProceduresByUri = new HashMap<String, RegisteredProceduresMapEntry>();
	HashMap<Long, RegisteredProceduresMapEntry> registeredProceduresById = new HashMap<Long, RegisteredProceduresMapEntry>();

	BaseWebSocket m_webSocket;
	protected WampClientSession(WampServiceImpl.WampClientWebSocket ws, String realmName, Map<String,Realm> m_realms) {
		Set<WampRoles> roles = new HashSet<>();
		roles.add(WampRoles.Caller);
		roles.add(WampRoles.Callee);
		roles.add(WampRoles.Publisher);
		roles.add(WampRoles.Subscriber);
		RealmConfig realmConfig = new RealmConfig(roles, false);
		m_realm = new Realm(realmConfig);
		m_webSocket = ws;
		info("WampClientSession:"+ws);

		ExecutorService executor = Executors.newSingleThreadExecutor();
    this.scheduler = Schedulers.from(executor);
	  m_wampRouterSession = new WampRouterSession(ws, m_realms);
		ws.setWampRouterSession(m_wampRouterSession);
		m_wampRouterSession.onWebSocketConnect(null);
		m_wampRouterSession.onWebSocketText(WampCodec.encode(new HelloMessage(realmName, null)));
	}

	/**
	 * Registers a procedure at the router which will afterwards be available
	 * for remote procedure calls from other clients.<br>
	 * The actual registration will only happen after the user subscribes on
	 * the returned Observable. This guarantees that no RPC requests get lost.
	 * Incoming RPC requests will be pushed to the Subscriber via it's
	 * onNext method. The Subscriber can send responses through the methods on
	 * the {@link Request}.<br>
	 * If the client no longer wants to provide the method it can call
	 * unsubscribe() on the Subscription to unregister the procedure.<br>
	 * If the connection closes onCompleted will be called.<br>
	 * In case of errors during subscription onError will be called.
	 * @param topic The name of the procedure which this client wants to
	 * provide.<br>
	 * Must be valid WAMP URI.
	 * @return An observable that can be used to provide a procedure.
	 */
	public Observable<Request> registerProcedure(final String topic) {
			return Observable.create(new OnSubscribe<Request>() {
					@Override
					public void call(final Subscriber<? super Request> subscriber) {
							try {
									UriValidator.validate(topic, useStrictUriValidation);
							}
							catch (WampError e) {
									subscriber.onError(e);
									return;
							}

							ExecutorService executor = Executors.newSingleThreadExecutor();
							executor.submit(() ->  {
								// If the Subscriber unsubscribed in the meantime we return early
								if (subscriber.isUnsubscribed()) return;
								// Set subscription to completed if we are not connected
								if (status != Status.Connected) {
										subscriber.onCompleted();
										return;
								}

								final RegisteredProceduresMapEntry entry = registeredProceduresByUri.get(topic);
								// Check if we have already registered a function with the same name
								if (entry != null) {
										subscriber.onError( new ApplicationError(ApplicationError.PROCEDURE_ALREADY_EXISTS));
										return;
								}
								
								// Insert a new entry in the subscription map
								final RegisteredProceduresMapEntry newEntry = new RegisteredProceduresMapEntry(subscriber, RegistrationState.Registering);
								registeredProceduresByUri.put(topic, newEntry);

								// Make the subscribe call
								final long requestId = IdGenerator.newLinearId(lastRequestId, requestMap);
								lastRequestId = requestId;
								final RegisterMessage msg = new RegisterMessage(requestId, null, topic);

								final AsyncSubject<Long> registerFuture = AsyncSubject.create();
								registerFuture
								.observeOn(WampClientSession.this.scheduler)
								.subscribe(new Action1<Long>() {
										@Override
										public void call(Long t1) {
												// Check if we were unsubscribed (through transport close)
												if (newEntry.state != RegistrationState.Registering) return;
												// Registration at the broker was successful
												newEntry.state = RegistrationState.Registered;
												newEntry.registrationId = t1;
												registeredProceduresById.put(t1, newEntry);
												// Add the cancellation functionality to the subscriber
												attachCancelRegistrationAction(subscriber, newEntry, topic);
										}
								}, new Action1<Throwable>() {
										@Override
										public void call(Throwable t1) {
												// Error on registering
												if (newEntry.state != RegistrationState.Registering) return;
												// Remark: Actually noone can't unregister until this Future completes because
												// the unregister functionality is only added in the success case
												// However a transport close event could set us to Unregistered early
												newEntry.state = RegistrationState.Unregistered;

												boolean isClosed = false;
												if (t1 instanceof ApplicationError && ((ApplicationError)t1).uri.equals(ApplicationError.TRANSPORT_CLOSED))
														isClosed = true;
												
												if (isClosed) subscriber.onCompleted();
												else subscriber.onError(t1);

												registeredProceduresByUri.remove(topic);
										}
								});

								requestMap.put(requestId, new RequestMapEntry(RegisterMessage.ID, registerFuture));
                m_webSocket.onWebSocketText(WampCodec.encode(msg));
						});
					}
			});
	}

    /**
     * Add an action that is added to the subscriber which is executed
     * if unsubscribe is called on a registered procedure.<br>
     * This action will lead to unregistering a provided function at the dealer.
     */
	private void attachCancelRegistrationAction(final Subscriber<? super Request> subscriber, final RegisteredProceduresMapEntry mapEntry, final String topic) {
		subscriber.add(Subscriptions.create(new Action0() {
			@Override
			public void call() {
				ExecutorService executor = Executors.newSingleThreadExecutor();
				executor.submit(() ->  {
					if (mapEntry.state != RegistrationState.Registered) return;
					
					mapEntry.state = RegistrationState.Unregistering;
					registeredProceduresByUri.remove(topic);
					registeredProceduresById.remove(mapEntry.registrationId);
					
					// Make the unregister call
					final long requestId = IdGenerator.newLinearId(lastRequestId, requestMap);
					lastRequestId = requestId;
					final UnregisterMessage msg = new UnregisterMessage(requestId, mapEntry.registrationId);
					
					final AsyncSubject<Void> unregisterFuture = AsyncSubject.create();
					unregisterFuture .observeOn(WampClientSession.this.scheduler).subscribe(new Action1<Void>() {
							@Override
							public void call(Void t1) {
									// Unregistration at the broker was successful
									mapEntry.state = RegistrationState.Unregistered;
							}
					}, new Action1<Throwable>() {
							@Override
							public void call(Throwable t1) {
									// Error on unregister
							}
					});
					
					requestMap.put(requestId, new RequestMapEntry( UnregisterMessage.ID, unregisterFuture));
					m_webSocket.onWebSocketText(WampCodec.encode(msg));
				});
			}
		}));
	}
	/**
	 * Performs a remote procedure call through the router.<br>
	 * The function will return immediately, as the actual call will happen
	 * asynchronously.
	 * @param procedure The name of the procedure to call. Must be a valid WAMP
	 * Uri.
	 * @param arguments A list of all positional arguments for the procedure call
	 * @param argumentsKw All named arguments for the procedure call
	 * @return An observable that provides a notification whether the call was
	 * was successful and the return value. If the call is successful the
	 * returned observable will be completed with a single value (the return value).
	 * If the remote procedure call yields an error the observable will be completed
	 * with an error.
	 */
	public Observable<Reply> call(final String procedure, final ArrayNode arguments, final ObjectNode argumentsKw) {
		final AsyncSubject<Reply> resultSubject = AsyncSubject.create();

		try {
			UriValidator.validate(procedure, useStrictUriValidation);
		} catch (WampError e) {
			resultSubject.onError(e);
			return resultSubject;
		}

		ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.submit(() ->  {
			info("Call:"+status);
			if (status != Status.Connected) {
				resultSubject.onError(new ApplicationError(ApplicationError.NOT_CONNECTED));
				return;
			}

			final long requestId = IdGenerator.newLinearId(lastRequestId, requestMap);
			lastRequestId = requestId;
			final CallMessage callMsg = new CallMessage(requestId, null, procedure, arguments, argumentsKw);

			requestMap.put(requestId, new RequestMapEntry(CallMessage.ID, resultSubject));
			m_webSocket.onWebSocketText(WampCodec.encode(callMsg));
		});
		return resultSubject;
	}

	/**
	 * Performs a remote procedure call through the router.<br>
	 * The function will return immediately, as the actual call will happen
	 * asynchronously.
	 * @param procedure The name of the procedure to call. Must be a valid WAMP
	 * Uri.
	 * @param args The list of positional arguments for the remote procedure call.
	 * These will be get serialized according to the Jackson library serializing
	 * behavior.
	 * @return An observable that provides a notification whether the call was
	 * was successful and the return value. If the call is successful the
	 * returned observable will be completed with a single value (the return value).
	 * If the remote procedure call yields an error the observable will be completed
	 * with an error.
	 */
	public Observable<Reply> call(final String procedure, Object... args) {
		// Build the arguments array and serialize the arguments
		return call(procedure, buildArgumentsArray(args), null);
	}

	public <T> Observable<T> call(final String procedure, final Class<T> returnValueClass, Object... args) {
			return call(procedure, buildArgumentsArray(args), null).map(new Func1<Reply,T>() {
					@Override
					public T call(Reply reply) {
							if (returnValueClass == null || returnValueClass == Void.class) {
									// We don't need a return value
									return null;
							}
							
							if (reply.arguments == null || reply.arguments.size() < 1)
									throw OnErrorThrowable.from(new ApplicationError(ApplicationError.MISSING_RESULT));
									
							JsonNode resultNode = reply.arguments.get(0);
							if (resultNode.isNull()) return null;
							
							T result;
							try {
									result = objectMapper.convertValue(resultNode, returnValueClass);
							} catch (IllegalArgumentException e) {
									// The returned exception is an aggregate one. That's not too nice :(
									throw OnErrorThrowable.from(new ApplicationError(ApplicationError.INVALID_VALUE_TYPE));
							}
							return result;
					}
			});
	}

	private void handleMessage(WampMessage msg) {
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
					if (role != null)
						rroles.add(role);
				}
				routerRoles = new WampRoles[rroles.size()];
				int i = 0;
				for (WampRoles r : rroles) {
					routerRoles[i] = r;
					i++;
				}
				status = Status.Connected;
				info("handleMessage:"+status);
				statusObservable.onNext(status);
			} else if (msg instanceof AbortMessage) {
				// The remote doesn't want us to connect :(
				AbortMessage abort = (AbortMessage) msg;
				onSessionError(new ApplicationError(abort.reason));
			}
		} else {
			// We were already welcomed
			if (msg instanceof WelcomeMessage) {
				onProtocolError();
			} else if (msg instanceof AbortMessage) {
				onProtocolError();
			} else if (msg instanceof GoodbyeMessage) {
				// Reply the goodbye
				//@@@MS channel.writeAndFlush(new GoodbyeMessage(null, ApplicationError.GOODBYE_AND_OUT));
				// We could also use the reason from the msg, but this would be harder
				// to determinate from a "real" error
				onSessionError(new ApplicationError(ApplicationError.GOODBYE_AND_OUT));
			} else if (msg instanceof ResultMessage) {
				ResultMessage r = (ResultMessage) msg;
				RequestMapEntry requestInfo = requestMap.get(r.requestId);
				if (requestInfo == null)
					return;
				// Ignore the result
				if (requestInfo.requestType != WampMessages.CallMessage.ID) {
					onProtocolError();
					return;
				}
				requestMap.remove(r.requestId);
				Reply reply = new Reply(r.arguments, r.argumentsKw);
				@SuppressWarnings("unchecked") AsyncSubject<Reply> subject = (AsyncSubject<Reply>) requestInfo.resultSubject;
				subject.onNext(reply);
				subject.onCompleted();
			} else if (msg instanceof ErrorMessage) {
				ErrorMessage r = (ErrorMessage) msg;
				if (r.requestType == WampMessages.CallMessage.ID || r.requestType == WampMessages.SubscribeMessage.ID || r.requestType == WampMessages.UnsubscribeMessage.ID || r.requestType == WampMessages.PublishMessage.ID || r.requestType == WampMessages.RegisterMessage.ID || r.requestType == WampMessages.UnregisterMessage.ID) {
					RequestMapEntry requestInfo = requestMap.get(r.requestId);
					if (requestInfo == null)
						return;
					// Ignore the error
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
			} else if (msg instanceof SubscribedMessage) {
				SubscribedMessage m = (SubscribedMessage) msg;
				RequestMapEntry requestInfo = requestMap.get(m.requestId);
				if (requestInfo == null)
					return;
				// Ignore the result
				if (requestInfo.requestType != WampMessages.SubscribeMessage.ID) {
					onProtocolError();
					return;
				}
				requestMap.remove(m.requestId);
				@SuppressWarnings("unchecked") AsyncSubject<Long> subject = (AsyncSubject<Long>) requestInfo.resultSubject;
				subject.onNext(m.subscriptionId);
				subject.onCompleted();
			} else if (msg instanceof UnsubscribedMessage) {
				UnsubscribedMessage m = (UnsubscribedMessage) msg;
				RequestMapEntry requestInfo = requestMap.get(m.requestId);
				if (requestInfo == null)
					return;
				// Ignore the result
				if (requestInfo.requestType != WampMessages.UnsubscribeMessage.ID) {
					onProtocolError();
					return;
				}
				requestMap.remove(m.requestId);
				@SuppressWarnings("unchecked") AsyncSubject<Void> subject = (AsyncSubject<Void>) requestInfo.resultSubject;
				subject.onNext(null);
				subject.onCompleted();
			} else if (msg instanceof EventMessage) {
				EventMessage ev = (EventMessage) msg;
				SubscriptionMapEntry entry = subscriptionsBySubscriptionId.get(ev.subscriptionId);
				if (entry == null || entry.state != PubSubState.Subscribed)
					return;
				// Ignore the result
				PubSubData evResult = new PubSubData(ev.details, ev.arguments, ev.argumentsKw);
				// publish the event
				for (Subscriber<? super PubSubData> s : entry.subscribers) {
					s.onNext(evResult);
				}
			} else if (msg instanceof PublishedMessage) {
				PublishedMessage m = (PublishedMessage) msg;
				RequestMapEntry requestInfo = requestMap.get(m.requestId);
				if (requestInfo == null)
					return;
				// Ignore the result
				if (requestInfo.requestType != WampMessages.PublishMessage.ID) {
					onProtocolError();
					return;
				}
				requestMap.remove(m.requestId);
				@SuppressWarnings("unchecked") AsyncSubject<Long> subject = (AsyncSubject<Long>) requestInfo.resultSubject;
				subject.onNext(m.publicationId);
				subject.onCompleted();
			} else if (msg instanceof RegisteredMessage) {
				RegisteredMessage m = (RegisteredMessage) msg;
				RequestMapEntry requestInfo = requestMap.get(m.requestId);
				if (requestInfo == null)
					return;
				// Ignore the result
				if (requestInfo.requestType != WampMessages.RegisterMessage.ID) {
					onProtocolError();
					return;
				}
				requestMap.remove(m.requestId);
				@SuppressWarnings("unchecked") AsyncSubject<Long> subject = (AsyncSubject<Long>) requestInfo.resultSubject;
				subject.onNext(m.registrationId);
				subject.onCompleted();
			} else if (msg instanceof UnregisteredMessage) {
				UnregisteredMessage m = (UnregisteredMessage) msg;
				RequestMapEntry requestInfo = requestMap.get(m.requestId);
				if (requestInfo == null)
					return;
				// Ignore the result
				if (requestInfo.requestType != WampMessages.UnregisterMessage.ID) {
					onProtocolError();
					return;
				}
				requestMap.remove(m.requestId);
				@SuppressWarnings("unchecked") AsyncSubject<Void> subject = (AsyncSubject<Void>) requestInfo.resultSubject;
				subject.onNext(null);
				subject.onCompleted();
			} else if (msg instanceof InvocationMessage) {
				InvocationMessage m = (InvocationMessage)msg;
				RegisteredProceduresMapEntry entry = registeredProceduresById.get(m.registrationId);
				if (entry == null || entry.state != RegistrationState.Registered) {
					// Send an error that we are no longer registered
					//channel.writeAndFlush(new ErrorMessage(InvocationMessage.ID, m.requestId, null, ApplicationError.NO_SUCH_PROCEDURE, null, null));
				}
				else {
					// Send the request to the subscriber, which can then send responses
					Request request = new Request(this, m.requestId, m.arguments, m.argumentsKw);
					entry.subscriber.onNext(request);
				}
			} else {
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
		if (isCompleted){
			return;
		}
		isCompleted = true;
		if (e != null){
			statusObservable.onError(e);
		}else{
			statusObservable.onCompleted();
		}
	}

	public Observable<Status> statusChanged() {
		return statusObservable;
	}

	ArrayNode buildArgumentsArray(Object... args) {
		if (args.length == 0) return null;
		// Build the arguments array and serialize the arguments
		final ArrayNode argArray = objectMapper.createArrayNode();
		for (Object arg : args) {
			argArray.addPOJO(arg);
		}
		return argArray;
	}

	public void onWebSocketConnect(Session sess) {
		debug("<-- SocketConnect");
		state = CONNECTED;
	}

	public void onWebSocketText(String message) {
info("onWebSocketText:"+message);
		try {
			WampMessage msg = WampCodec.decode(message.getBytes());
			if (msg instanceof ErrorMessage) {
				ErrorMessage errMsg = (ErrorMessage) msg;
				debug("<-- CReceiveMessage(error):" + errMsg.error + "/requestId:" + errMsg.requestId + "/requestType:" + errMsg.requestType);
			} else {
				debug("<-- CReceiveMessage(" + getMessageName(msg) + "):" + message);
			}
			handleMessage(msg);
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
