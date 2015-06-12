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
package org.ms123.common.wamp.camel;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultConsumer;
import org.apache.camel.Processor;
import org.slf4j.helpers.MessageFormatter;
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
import org.ms123.common.wamp.WampClientSession;
import org.ms123.common.wamp.ApplicationError;
import org.apache.camel.AsyncCallback;
import org.apache.camel.ExchangePattern;

public class WampClientConsumer extends DefaultConsumer {

	private static final Logger LOG = LoggerFactory.getLogger(WampClientConsumer.class);
	private final WampClientEndpoint endpoint;
	private WampClientSession clientSession;
	private Processor processor;

	public WampClientConsumer(WampClientEndpoint endpoint, Processor processor) {
		super(endpoint, processor);
		this.endpoint = endpoint;
		this.processor = processor;
	}

	private void wampClientConnected() {
		info("register:" + endpoint.getProcedure());
		Subscription addProcSubscription = this.clientSession.registerProcedure(endpoint.getProcedure()).subscribe((request) -> {

			info("Request:" + request);
			final boolean reply = false;
			final Exchange exchange = endpoint.createExchange(reply ? ExchangePattern.InOut : ExchangePattern.InOnly);
			exchange.getIn().setBody("Hello from Endpoint");
			try {
				getAsyncProcessor().process(exchange, new AsyncCallback() {

					@Override
					public void done(boolean doneSync) {
						info("Body:" + exchange.getOut().getBody());
						request.reply(exchange.getOut().getBody());
					}
				});
			} catch (Exception e) {
				getExceptionHandler().handleException("Error processing Wamp event: " + exchange, e);
			}

			/*if (request.arguments() == null || request.arguments().size() != 2 || !request.arguments().get(0).canConvertToLong() || !request.arguments().get(1).canConvertToLong()) {
				try {
					request.replyError(new ApplicationError(ApplicationError.INVALID_PARAMETER));
				} catch (ApplicationError e) {
					e.printStackTrace();
				}
			} else {
				long a = request.arguments().get(0).asLong();
				long b = request.arguments().get(1).asLong();
				request.reply(a + b);
			}*/
		});

		Observable<Long> result1 = this.clientSession.call("com.myapp.add2", Long.class, 33, 66);
		result1.subscribe((t2) -> {
			System.out.println("Completed add with result " + t2);
		}, (t3) -> {
			System.out.println("Completed add with error " + t3);
		});
	}

	protected void doStart() throws Exception {
		this.clientSession = endpoint.createWampClientSession("realm1");
		this.clientSession.statusChanged().subscribe((state) -> {
			info("ClientSession:status changed to " + state);
			if (state == WampClientSession.Status.Connected) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
				wampClientConnected();
			}
			if (state == WampClientSession.Status.Disconnected) {
				try {
					this.doStop();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}, (t) -> {
			debug("ClientSession ended with error " + t);
		}, () -> {
			debug("clientSession ended normally");
		});
		super.doStart();
	}

	protected void doStop() throws Exception {
		debug("Stopping WampClientConsumer " + endpoint.getProcedure());
		this.clientSession.close();
		this.clientSession = null;
		super.doStop();
	}

	protected void debug(String msg, Object... args) {
		System.out.println(MessageFormatter.arrayFormat(msg, varargsToArray(args)).getMessage());
		LOG.debug(msg, args);
	}

	protected void info(String msg, Object... args) {
		System.out.println(MessageFormatter.arrayFormat(msg, varargsToArray(args)).getMessage());
		LOG.info(msg, args);
	}

	private Object[] varargsToArray(Object... args) {
		Object[] ret = new Object[args.length];
		for (int i = 0; i < args.length; i++) {
			ret[i] = args[i];
		}
		return ret;
	}
}

