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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.eclipse.jetty.websocket.api.Session;
import org.ms123.common.wamp.WampMessages.*;
import org.ms123.common.wamp.WampServiceImpl.WampClientWebSocket;
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
import rx.subscriptions.Subscriptions;

/**
 *
 */
public class WampClientSession {

	/**
	 * Possible states for a WAMP session between client and router
	 */
	public static enum Status {
		Disconnected, /** The session is not connected */
		Connecting, /** The session is trying to connect to the router */
		Connected
		/** The session is connected to the router */
	}

	/** The current status */
	private Status status = Status.Disconnected;

	private BehaviorSubject<Status> statusObservable = BehaviorSubject.create();

	private ObjectMapper m_objectMapper = new ObjectMapper();
	private ObjectNode welcomeDetails = null;

	private final boolean useStrictUriValidation = true;
	private long sessionId;

	private WampRoles[] routerRoles;

	private final ObjectMapper objectMapper = new ObjectMapper();
	protected final Scheduler scheduler;

	private final boolean closeClientOnErrors = false;

	private long lastRequestId = IdValidator.MIN_VALID_ID;
	private WampRouterSession m_wampRouterSession;
	private boolean isCompleted = false;

	private enum PubSubState {
		Subscribing, Subscribed, Unsubscribing, Unsubscribed
	}

	private enum RegistrationState {
		Registering, Registered, Unregistering, Unregistered
	}

	private enum PublishFlags {
		DontExcludeMe;
	}

	private static class RequestMapEntry {
		public final int requestType;
		public final AsyncSubject<?> resultSubject;

		public RequestMapEntry(int requestType, AsyncSubject<?> resultSubject) {
			this.requestType = requestType;
			this.resultSubject = resultSubject;
		}
	}

	private static class SubscriptionMapEntry {

		public PubSubState state;
		public final SubscriptionFlags flags;
		public long subscriptionId = 0;
		public final List<Subscriber<? super PubSubData>> subscribers = new ArrayList<Subscriber<? super PubSubData>>();

		public SubscriptionMapEntry(SubscriptionFlags flags, PubSubState state) {
			this.flags = flags;
			this.state = state;
		}
	}

	private static class RegisteredProceduresMapEntry {
		public RegistrationState state;
		public long registrationId = 0;
		public final Subscriber<? super Request> subscriber;

		public RegisteredProceduresMapEntry(Subscriber<? super Request> subscriber, RegistrationState state) {
			this.subscriber = subscriber;
			this.state = state;
		}
	}

	private HashMap<Long, RequestMapEntry> requestMap = new HashMap<>();
	private EnumMap<SubscriptionFlags, HashMap<String, SubscriptionMapEntry>> subscriptionsByFlags = new EnumMap<>(SubscriptionFlags.class);
	private HashMap<Long, SubscriptionMapEntry> subscriptionsBySubscriptionId = new HashMap<>();
	private HashMap<String, RegisteredProceduresMapEntry> registeredProceduresByUri = new HashMap<>();
	private HashMap<Long, RegisteredProceduresMapEntry> registeredProceduresById = new HashMap<>();

	protected WampClientWebSocket webSocket;

	protected WampClientSession(WampClientWebSocket ws, String realmName, Map<String, Realm> realms) {
		Set<WampRoles> roles = new HashSet<>();
		roles.add(WampRoles.Caller);
		roles.add(WampRoles.Callee);
		roles.add(WampRoles.Publisher);
		roles.add(WampRoles.Subscriber);
		RealmConfig realmConfig = new RealmConfig(roles, false);
		this.webSocket = ws;
		ExecutorService executor = Executors.newSingleThreadExecutor();
		this.scheduler = Schedulers.from(executor);
		m_wampRouterSession = new WampRouterSession(ws, realms);
		ws.setWampRouterSession(m_wampRouterSession);
		executor = Executors.newSingleThreadExecutor();
		executor.submit(() -> {
			ws.onWebSocketConnect(null);
			ws.onWebSocketText(WampCodec.encode(new HelloMessage(realmName, null)));
		});
	}

	public void close() {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.submit(() -> {
			if (!isCompleted) {
				if (this.status == Status.Connected) {
					// Send goodbye to the remote
					GoodbyeMessage msg = new GoodbyeMessage(null, ApplicationError.SYSTEM_SHUTDOWN);
					WampClientSession.this.webSocket.onWebSocketText(WampCodec.encode(msg));
				}

				if (this.status != Status.Disconnected) {
					// Close the connection (or connection attempt)
					closeCurrentTransport();
					statusObservable.onNext(this.status);
				}

				// Normal close without an error
				completeStatus(null);
			}

		});
	}

	public Observable<Long> publish(final String topic, Object... args) {
		return publish(topic, buildArgumentsArray(args), null);
	}

	public Observable<Long> publish(final String topic, PubSubData event) {
		if (event != null)
			return publish(topic, event.arguments, event.keywordArguments);
		else
			return publish(topic, null, null);
	}

	public Observable<Long> publish(final String topic, final ArrayNode arguments, final ObjectNode argumentsKw) {
		return publish(topic, null, arguments, argumentsKw);
	}

	public Observable<Long> publish(final String topic, final PublishFlags flags, final ArrayNode arguments, final ObjectNode argumentsKw) {
		final AsyncSubject<Long> resultSubject = AsyncSubject.create();

		try {
			UriValidator.validate(topic, useStrictUriValidation);
		} catch (WampError e) {
			resultSubject.onError(e);
			return resultSubject;
		}

		ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.submit(() -> {
			if (this.status != Status.Connected) {
				resultSubject.onError(new ApplicationError(ApplicationError.NOT_CONNECTED));
				return;
			}

			final long requestId = IdGenerator.newLinearId(lastRequestId, requestMap);
			lastRequestId = requestId;

			ObjectNode options = null;
			if (flags == PublishFlags.DontExcludeMe) {
				options = objectMapper.createObjectNode();
				options.put("exclude_me", false);
			}
			final WampMessages.PublishMessage msg = new WampMessages.PublishMessage(requestId, options, topic, arguments, argumentsKw);
System.out.println("WampMessages.PublishMessage:"+msg);

			requestMap.put(requestId, new RequestMapEntry(WampMessages.PublishMessage.ID, resultSubject));
			WampClientSession.this.webSocket.onWebSocketText(WampCodec.encode(msg));
		});
		return resultSubject;
	}

	public Observable<Request> registerProcedure(final String topic) {
		return Observable.create(new OnSubscribe<Request>() {
			@Override
			public void call(final Subscriber<? super Request> subscriber) {
				try {
					UriValidator.validate(topic, useStrictUriValidation);
				} catch (WampError e) {
					subscriber.onError(e);
					return;
				}

				ExecutorService executor = Executors.newSingleThreadExecutor();
				executor.submit(() -> {
					// If the Subscriber unsubscribed in the meantime we return early
					if (subscriber.isUnsubscribed())
						return;
					// Set subscription to completed if we are not connected
					if (status != Status.Connected) {
						subscriber.onCompleted();
						return;
					}

					final RegisteredProceduresMapEntry entry = registeredProceduresByUri.get(topic);
					// Check if we have already registered a function with the same name
					if (entry != null) {
						subscriber.onError(new ApplicationError(ApplicationError.PROCEDURE_ALREADY_EXISTS));
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
					registerFuture.observeOn(WampClientSession.this.scheduler).subscribe(new Action1<Long>() {
						@Override
						public void call(Long t1) {
							// Check if we were unsubscribed (through transport close)
							if (newEntry.state != RegistrationState.Registering)
								return;
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
							if (newEntry.state != RegistrationState.Registering)
								return;
							// Remark: Actually noone can't unregister until this Future completes because
							// the unregister functionality is only added in the success case
							// However a transport close event could set us to Unregistered early
							newEntry.state = RegistrationState.Unregistered;

							boolean isClosed = false;
							if (t1 instanceof ApplicationError && ((ApplicationError) t1).uri.equals(ApplicationError.TRANSPORT_CLOSED))
								isClosed = true;

							if (isClosed)
								subscriber.onCompleted();
							else
								subscriber.onError(t1);

							registeredProceduresByUri.remove(topic);
						}
					});

					requestMap.put(requestId, new RequestMapEntry(RegisterMessage.ID, registerFuture));
					WampClientSession.this.webSocket.onWebSocketText(WampCodec.encode(msg));
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
				executor.submit(() -> {
					if (mapEntry.state != RegistrationState.Registered)
						return;

					mapEntry.state = RegistrationState.Unregistering;
					registeredProceduresByUri.remove(topic);
					registeredProceduresById.remove(mapEntry.registrationId);

					// Make the unregister call
					final long requestId = IdGenerator.newLinearId(lastRequestId, requestMap);
					lastRequestId = requestId;
					final UnregisterMessage msg = new UnregisterMessage(requestId, mapEntry.registrationId);

					final AsyncSubject<Void> unregisterFuture = AsyncSubject.create();
					unregisterFuture.observeOn(WampClientSession.this.scheduler).subscribe(new Action1<Void>() {
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

					requestMap.put(requestId, new RequestMapEntry(UnregisterMessage.ID, unregisterFuture));
					WampClientSession.this.webSocket.onWebSocketText(WampCodec.encode(msg));
				});
			}
		}));
	}

	public <T> Observable<T> makeSubscription(final String topic, final Class<T> eventClass) {
		return makeSubscription(topic, SubscriptionFlags.Exact, eventClass);
	}

	public <T> Observable<T> makeSubscription(final String topic, SubscriptionFlags flags, final Class<T> eventClass) {
		return makeSubscription(topic, flags).map(new Func1<PubSubData, T>() {
			@Override
			public T call(PubSubData ev) {
				if (eventClass == null || eventClass == Void.class) {
					// We don't need a value
					return null;
				}

				if (ev.arguments == null || ev.arguments.size() < 1)
					throw OnErrorThrowable.from(new ApplicationError(ApplicationError.MISSING_VALUE));

				JsonNode eventNode = ev.arguments.get(0);
				if (eventNode.isNull())
					return null;

				T eventValue;
				try {
					eventValue = objectMapper.convertValue(eventNode, eventClass);
				} catch (IllegalArgumentException e) {
					throw OnErrorThrowable.from(new ApplicationError(ApplicationError.INVALID_VALUE_TYPE));
				}
				return eventValue;
			}
		});
	}

	public Observable<PubSubData> makeSubscription(final String topic) {
		return makeSubscription(topic, SubscriptionFlags.Exact);
	}

	public Observable<PubSubData> makeSubscription(final String topic, final SubscriptionFlags flags) {
		return Observable.create(new OnSubscribe<PubSubData>() {
			@Override
			public void call(final Subscriber<? super PubSubData> subscriber) {
				try {
					UriValidator.validate(topic, useStrictUriValidation, flags == SubscriptionFlags.Wildcard);
				} catch (WampError e) {
					subscriber.onError(e);
					return;
				}

				ExecutorService executor = Executors.newSingleThreadExecutor();
				executor.submit(() -> {
					if (subscriber.isUnsubscribed()) {
						return;
					}
					if (status != Status.Connected) {
						subscriber.onCompleted();
						return;
					}

					final SubscriptionMapEntry entry = subscriptionsByFlags.get(flags).get(topic);

					if (entry != null) { // We are already subscribed at the dealer
						entry.subscribers.add(subscriber);
						if (entry.state == PubSubState.Subscribed) {
							attachPubSubCancelationAction(subscriber, entry, topic);
						}
					} else { // need to subscribe
						final SubscriptionMapEntry newEntry = new SubscriptionMapEntry(flags, PubSubState.Subscribing);
						newEntry.subscribers.add(subscriber);
						subscriptionsByFlags.get(flags).put(topic, newEntry);

						final long requestId = IdGenerator.newLinearId(lastRequestId, requestMap);
						lastRequestId = requestId;

						ObjectNode options = null;
						if (flags != SubscriptionFlags.Exact) {
							options = objectMapper.createObjectNode();
							options.put("match", flags.name().toLowerCase());
						}
						final SubscribeMessage msg = new SubscribeMessage(requestId, options, topic);

						final AsyncSubject<Long> subscribeFuture = AsyncSubject.create();
						subscribeFuture.observeOn(WampClientSession.this.scheduler).subscribe(new Action1<Long>() {
							@Override
							public void call(Long t1) {
								// Check if we were unsubscribed (through transport close)
								if (newEntry.state != PubSubState.Subscribing) {
									return;
								}
								// Subscription at the broker was successful
								newEntry.state = PubSubState.Subscribed;
								newEntry.subscriptionId = t1;
								subscriptionsBySubscriptionId.put(t1, newEntry);
								// Add the cancellation functionality to all subscribers
								// If one is already unsubscribed this will immediately call
								// the cancellation function for this subscriber
								for (Subscriber<? super PubSubData> s : newEntry.subscribers) {
									attachPubSubCancelationAction(s, newEntry, topic);
								}
							}
						}, new Action1<Throwable>() {
							@Override
							public void call(Throwable t1) {
								// Error on subscription
								if (newEntry.state != PubSubState.Subscribing)
									return;
								// Remark: Actually noone can't unsubscribe until this Future completes because
								// the unsubscription functionality is only added in the success case
								// However a transport close event could set us to Unsubscribed early
								newEntry.state = PubSubState.Unsubscribed;

								boolean isClosed = false;
								if (t1 instanceof ApplicationError && ((ApplicationError) t1).uri.equals(ApplicationError.TRANSPORT_CLOSED))
									isClosed = true;

								for (Subscriber<? super PubSubData> s : newEntry.subscribers) {
									if (isClosed)
										s.onCompleted();
									else
										s.onError(t1);
								}

								newEntry.subscribers.clear();
								subscriptionsByFlags.get(flags).remove(topic);
							}
						});

						requestMap.put(requestId, new RequestMapEntry(SubscribeMessage.ID, subscribeFuture));
						WampClientSession.this.webSocket.onWebSocketText(WampCodec.encode(msg));
					}
				});
			}
		});
	}

	private void attachPubSubCancelationAction(final Subscriber<? super PubSubData> subscriber, final SubscriptionMapEntry mapEntry, final String topic) {
		subscriber.add(Subscriptions.create(new Action0() {
			@Override
			public void call() {
				ExecutorService executor = Executors.newSingleThreadExecutor();
				executor.submit(() -> {
					mapEntry.subscribers.remove(subscriber);
					if (mapEntry.state == PubSubState.Subscribed && mapEntry.subscribers.size() == 0) {
						// We removed the last subscriber and can therefore unsubscribe from the dealer
						mapEntry.state = PubSubState.Unsubscribing;
						subscriptionsByFlags.get(mapEntry.flags).remove(topic);
						subscriptionsBySubscriptionId.remove(mapEntry.subscriptionId);

						final long requestId = IdGenerator.newLinearId(lastRequestId, requestMap);
						lastRequestId = requestId;
						final UnsubscribeMessage msg = new UnsubscribeMessage(requestId, mapEntry.subscriptionId);

						final AsyncSubject<Void> unsubscribeFuture = AsyncSubject.create();
						unsubscribeFuture.observeOn(WampClientSession.this.scheduler).subscribe(new Action1<Void>() {
							@Override
							public void call(Void t1) {
								mapEntry.state = PubSubState.Unsubscribed;
							}
						}, new Action1<Throwable>() {
							@Override
							public void call(Throwable t1) {
								// Error on unsubscription
							}
						});

						requestMap.put(requestId, new RequestMapEntry(UnsubscribeMessage.ID, unsubscribeFuture));

						WampClientSession.this.webSocket.onWebSocketText(WampCodec.encode(msg));
					}
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
		executor.submit(() -> {
			if (this.status != Status.Connected) {
				resultSubject.onError(new ApplicationError(ApplicationError.NOT_CONNECTED));
				return;
			}

			final long requestId = IdGenerator.newLinearId(lastRequestId, requestMap);
			lastRequestId = requestId;
			final CallMessage callMsg = new CallMessage(requestId, null, procedure, arguments, argumentsKw);

			requestMap.put(requestId, new RequestMapEntry(CallMessage.ID, resultSubject));
			this.webSocket.onWebSocketText(WampCodec.encode(callMsg));
		});
		return resultSubject;
	}

	public Observable<Reply> call(final String procedure, Object... args) {
		return call(procedure, buildArgumentsArray(args), null);
	}

	public <T> Observable<T> call(final String procedure, final Class<T> returnValueClass, Object... args) {
		return call(procedure, buildArgumentsArray(args), null).map(new Func1<Reply, T>() {
			@Override
			public T call(Reply reply) {
				if (returnValueClass == null || returnValueClass == Void.class) {
					// We don't need a return value
					return null;
				}

				if (reply.arguments == null || reply.arguments.size() < 1)
					throw OnErrorThrowable.from(new ApplicationError(ApplicationError.MISSING_RESULT));

				JsonNode resultNode = reply.arguments.get(0);
				if (resultNode.isNull())
					return null;

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
			if (msg instanceof WelcomeMessage) {
				welcomeDetails = ((WelcomeMessage) msg).details;
				sessionId = ((WelcomeMessage) msg).sessionId;
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
				this.status = Status.Connected;
				info("handleMessage:" + this.status);
				statusObservable.onNext(this.status);
			} else if (msg instanceof AbortMessage) {
				AbortMessage abort = (AbortMessage) msg;
				onSessionError(new ApplicationError(abort.reason));
			}
		} else {
			if (msg instanceof WelcomeMessage) {
				onProtocolError();
			} else if (msg instanceof AbortMessage) {
				onProtocolError();
			} else if (msg instanceof GoodbyeMessage) {
				this.webSocket.onWebSocketText(WampCodec.encode(new GoodbyeMessage(null, ApplicationError.GOODBYE_AND_OUT)));
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
				@SuppressWarnings("unchecked")
				AsyncSubject<Reply> subject = (AsyncSubject<Reply>) requestInfo.resultSubject;
				subject.onNext(reply);
				subject.onCompleted();
			} else if (msg instanceof ErrorMessage) {
				ErrorMessage r = (ErrorMessage) msg;
				if (r.requestType == WampMessages.CallMessage.ID || r.requestType == WampMessages.SubscribeMessage.ID || r.requestType == WampMessages.UnsubscribeMessage.ID || r.requestType == WampMessages.PublishMessage.ID || r.requestType == WampMessages.RegisterMessage.ID || r.requestType == WampMessages.UnregisterMessage.ID) {
					RequestMapEntry requestInfo = requestMap.get(r.requestId);
					if (requestInfo == null) {
						return;
					}
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
				@SuppressWarnings("unchecked")
				AsyncSubject<Long> subject = (AsyncSubject<Long>) requestInfo.resultSubject;
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
				@SuppressWarnings("unchecked")
				AsyncSubject<Void> subject = (AsyncSubject<Void>) requestInfo.resultSubject;
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
				@SuppressWarnings("unchecked")
				AsyncSubject<Long> subject = (AsyncSubject<Long>) requestInfo.resultSubject;
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
				@SuppressWarnings("unchecked")
				AsyncSubject<Long> subject = (AsyncSubject<Long>) requestInfo.resultSubject;
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
				@SuppressWarnings("unchecked")
				AsyncSubject<Void> subject = (AsyncSubject<Void>) requestInfo.resultSubject;
				subject.onNext(null);
				subject.onCompleted();
			} else if (msg instanceof InvocationMessage) {
				InvocationMessage m = (InvocationMessage) msg;
				RegisteredProceduresMapEntry entry = registeredProceduresById.get(m.registrationId);
				if (entry == null || entry.state != RegistrationState.Registered) {
					// Send an error that we are no longer registered
					String errMsg = WampCodec.encode(new ErrorMessage(InvocationMessage.ID, m.requestId, null, ApplicationError.NO_SUCH_PROCEDURE, null, null));
					this.webSocket.onWebSocketText(errMsg);
				} else {
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
		closeCurrentTransport();
		statusObservable.onNext(this.status);
		if (closeClientOnErrors) {
			completeStatus(error);
		}
	}

	private void completeStatus(Exception e) {
		if (isCompleted) {
			return;
		}
		isCompleted = true;
		if (e != null) {
			statusObservable.onError(e);
		} else {
			statusObservable.onCompleted();
		}
	}

	public Observable<Status> statusChanged() {
		return statusObservable;
	}

	protected ArrayNode buildArgumentsArray(Object... args) {
		if (args.length == 0)
			return null;
		// Build the arguments array and serialize the arguments
		final ArrayNode argArray = objectMapper.createArrayNode();
		for (Object arg : args) {
			argArray.addPOJO(arg);
		}
		return argArray;
	}

	private void clearPendingRequests(Throwable e) {
		for (Entry<Long, RequestMapEntry> entry : requestMap.entrySet()) {
			entry.getValue().resultSubject.onError(e);
		}
		requestMap.clear();
	}

	private void clearAllSubscriptions(Throwable e) {
		for (HashMap<String, SubscriptionMapEntry> subscriptionByUri : subscriptionsByFlags.values()) {
			for (Entry<String, SubscriptionMapEntry> entry : subscriptionByUri.entrySet()) {
				for (Subscriber<? super PubSubData> s : entry.getValue().subscribers) {
					if (e == null)
						s.onCompleted();
					else
						s.onError(e);
				}
				entry.getValue().state = PubSubState.Unsubscribed;
			}
			subscriptionByUri.clear();
		}
		subscriptionsBySubscriptionId.clear();
	}

	private void clearAllRegisteredProcedures(Throwable e) {
		for (Entry<String, RegisteredProceduresMapEntry> entry : registeredProceduresByUri.entrySet()) {
			if (e == null)
				entry.getValue().subscriber.onCompleted();
			else
				entry.getValue().subscriber.onError(e);
			entry.getValue().state = RegistrationState.Unregistered;
		}
		registeredProceduresByUri.clear();
		registeredProceduresById.clear();
	}

	private void closeCurrentTransport() {
		if (this.status == Status.Disconnected) {
			return;
		}

		this.welcomeDetails = null;
		this.sessionId = 0;

		WampClientSession.this.webSocket.onWebSocketClose(-1, "");
		this.status = Status.Disconnected;

		clearPendingRequests(new ApplicationError(ApplicationError.TRANSPORT_CLOSED));
		clearAllSubscriptions(null);
		clearAllRegisteredProcedures(null);
	}

	public void onWebSocketConnect(Session sess) {
		debug("<-- WampClientSession.SocketConnect");
		this.status = Status.Connecting;
	}

	public void onWebSocketText(String message) {
		info("WampClientSession.onWebSocketText:" + message);
		try {
			WampMessage msg = WampCodec.decode(message.getBytes());
			if (msg instanceof ErrorMessage) {
				ErrorMessage errMsg = (ErrorMessage) msg;
				debug("<-- WampClientSession.ReceiveMessage(error):" + errMsg.error + "/requestId:" + errMsg.requestId + "/requestType:" + errMsg.requestType);
			} else {
				debug("<-- WampClientSession.ReceiveMessage(" + getMessageName(msg) + "):" + message);
			}
			handleMessage(msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void onWebSocketClose(int statusCode, String reason) {
		debug("<-- SocketClose:" + statusCode + "/" + reason);
		//m_context.realm.removeSession(m_context, true);
		this.status = Status.Disconnected;
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

