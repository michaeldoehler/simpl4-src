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
package org.ms123.common.camel.components.eventbus;

import org.apache.camel.AsyncCallback;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Processor;
import org.apache.camel.impl.DefaultConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.apache.camel.Message;
import org.osgi.framework.ServiceRegistration;
import java.util.Dictionary;
import java.util.Hashtable;
import org.slf4j.helpers.MessageFormatter;
import java.util.Map;

public class EventBusConsumer extends DefaultConsumer implements EventHandler {

	private static final Logger LOG = LoggerFactory.getLogger(EventBusConsumer.class);

	private final EventBusEndpoint endpoint;
	private ServiceRegistration m_register;

	public EventBusConsumer(EventBusEndpoint endpoint, Processor processor) {
		super(endpoint, processor);
		this.endpoint = endpoint;
	}

	public void handleEvent(Event event) {
		debug("handleEvent.onEvent {}", event);
		final boolean reply = false;
		final Exchange exchange = endpoint.createExchange(reply ? ExchangePattern.InOut : ExchangePattern.InOnly);
		exchange.setIn((Message)event.getProperty("msg"));
		try {
			getAsyncProcessor().process(exchange, new AsyncCallback() {

				@Override
				public void done(boolean doneSync) {
				}
			});
		} catch (Exception e) {
			getExceptionHandler().handleException("Error processing Vertx event: " + event, exchange, e);
		}
	}

	protected void doStart() throws Exception {
		debug("Registering EventHandler handler on address {}", endpoint.getAddress());
		String[] topics = new String[] { "eventbus/" + endpoint.getAddress() };
		Dictionary d = new Hashtable();
		d.put(EventConstants.EVENT_TOPIC, topics);
		m_register = endpoint.getBundleContext().registerService(EventHandler.class.getName(), this, d);
		super.doStart();
	}

	protected void doStop() throws Exception {
		debug("Unregistering EventBus handler on address {}", endpoint.getAddress());
		m_register.unregister();
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

	private Object[] varargsToArray(Object...args){
	 Object[] ret = new Object[args.length];
    for (int i = 0; i < args.length; i++) {
      ret[i] = args[i];
    }
		return ret;
	}
}
