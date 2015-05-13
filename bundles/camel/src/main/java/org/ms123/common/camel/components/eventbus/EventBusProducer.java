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
import org.apache.camel.InvalidPayloadRuntimeException;
import org.apache.camel.impl.DefaultAsyncProducer;
import org.apache.camel.util.ExchangeHelper;
import org.apache.camel.util.MessageHelper;
import org.apache.camel.Message;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.helpers.MessageFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;

public class EventBusProducer extends DefaultAsyncProducer {

	private static final Logger LOG = LoggerFactory.getLogger(EventBusProducer.class);

	public EventBusProducer(EventBusEndpoint endpoint) {
		super(endpoint);
	}

	@Override
	public EventBusEndpoint getEndpoint() {
		return (EventBusEndpoint) super.getEndpoint();
	}

	@Override
	public boolean process(Exchange exchange, AsyncCallback callback) {
		String address = getEndpoint().getAddress();
		boolean reply = ExchangeHelper.isOutCapable(exchange);
		Message msg = exchange.hasOut() ? exchange.getOut() : exchange.getIn();
		Map properties = new HashMap();
		properties.put("msg", msg);
debug("EventBusProducer.process:"+address);
debug("EventBusProducer.Headers:"+msg.getHeaders());
debug("EventBusProducer.Body:"+msg.getBody());
debug("EventBusProducer.process:"+properties);
		Event event = new Event("eventbus/" + address, properties);
		getEndpoint().getEventAdmin().postEvent(event);
		return true;
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
