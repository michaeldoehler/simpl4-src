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

import java.util.List;
import java.util.Map;
import org.apache.camel.CamelContext;
import org.apache.camel.ComponentConfiguration;
import org.apache.camel.Endpoint;
import org.apache.camel.impl.UriEndpointComponent;
import org.apache.camel.spi.EndpointCompleter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class EventBusComponent extends UriEndpointComponent implements EndpointCompleter {

	private static final Logger LOG = LoggerFactory.getLogger(EventBusComponent.class);

	private String host;
	private int port;

	public EventBusComponent() {
		super(EventBusEndpoint.class);
	}

	public EventBusComponent(CamelContext context) {
		super(context, EventBusEndpoint.class);
	}

	protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
		EventBusEndpoint endpoint = new EventBusEndpoint(uri, this, remaining);
		setProperties(endpoint, parameters);
		return endpoint;
	}

	public List<String> completeEndpointPath(ComponentConfiguration componentConfiguration, String text) {
		return null;
	}

	@Override
	protected void doStart() throws Exception {
		super.doStart();
	}

	@Override
	protected void doStop() throws Exception {
		super.doStop();
	}
}
