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

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.core.osgi.utils.BundleContextUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.EventAdmin;

/**
 */
@UriEndpoint(scheme = "eventbus", consumerClass = EventBusConsumer.class)
public class EventBusEndpoint extends DefaultEndpoint {

	@UriParam
	private String address;

	private BundleContext m_bundleContext;

	private EventAdmin m_eventAdmin;

	public EventBusEndpoint(String uri, EventBusComponent component, String address) {
		super(uri, component);
		this.address = address;
		m_bundleContext = BundleContextUtils.getBundleContext(EventBusEndpoint.class);
		m_eventAdmin = lookupServiceByClass(EventAdmin.class);
	}

	@Override
	public EventBusComponent getComponent() {
		return (EventBusComponent) super.getComponent();
	}

	public Producer createProducer() throws Exception {
		return new EventBusProducer(this);
	}

	public Consumer createConsumer(Processor processor) throws Exception {
		EventBusConsumer consumer = new EventBusConsumer(this, processor);
		configureConsumer(consumer);
		return consumer;
	}

	public <T> T lookupServiceByClass(Class<T> clazz) {
		T service = null;
		ServiceReference sr = m_bundleContext.getServiceReference(clazz);
		if (sr != null) {
			service = (T) m_bundleContext.getService(sr);
		}
		if (service == null) {
			throw new RuntimeException("EventBusEndpoint.Cannot resolve service:" + clazz);
		}
		return service;
	}

	public boolean isSingleton() {
		return true;
	}

	public EventAdmin getEventAdmin() {
		return m_eventAdmin;
	}

	public BundleContext getBundleContext() {
		return m_bundleContext;
	}

	public String getAddress() {
		return address;
	}

	/**
     * Sets the event bus address used to communicate
     */
	public void setAddress(String address) {
		this.address = address;
	}
}
