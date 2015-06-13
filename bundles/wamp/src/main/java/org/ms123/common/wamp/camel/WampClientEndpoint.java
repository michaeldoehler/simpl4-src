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
import org.ms123.common.wamp.WampService;
import org.ms123.common.wamp.WampClientSession;
import org.ms123.common.wamp.WampServiceImpl;
import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;


/**
 */
@UriEndpoint(scheme = "wampclient", title = "WampClient", syntax = "wampclient:topic", consumerClass = WampClientConsumer.class)
public class WampClientEndpoint extends DefaultEndpoint {


	private JSONDeserializer ds = new JSONDeserializer();
	// common options
	@UriParam
	private String mode;


	// producer(publisher) options
	//@UriParam(label = "producer")
	//private String procedure;

	// consumer(rpc) options
	@UriParam
	private String procedure;
	private String startableGroups;
	private String startableUsers;
	private String rpcReturn;
	private String rpcParameter;
	private String rpcReturnHeaders;

	private BundleContext m_bundleContext;
	private WampService wampService;

	public WampClientEndpoint(WampClientComponent component, String uri, String remaining) {
		super(uri, component);
		this.mode = remaining;
	}

	@Override
	public WampClientComponent getComponent() {
		return (WampClientComponent) super.getComponent();
	}

	public Producer createProducer() throws Exception {
		return new WampClientProducer(this);
	}

	public Consumer createConsumer(Processor processor) throws Exception {
		WampClientConsumer consumer = new WampClientConsumer(this, processor);
		System.out.println("createConsumer");
		configureConsumer(consumer);
		return consumer;
	}

	public boolean isSingleton() {
		return true;
	}

	public WampClientSession createWampClientSession(String realm) {
		return WampServiceImpl.createWampClientSession(realm);
	}

	public BundleContext getBundleContext() {
		return m_bundleContext;
	}

	public String getProcedure() {
		return this.procedure;
	}

	public void setProcedure(String procedure) {
		this.procedure = procedure;
	}

	public String getStartableUsers() {
		return this.startableUsers;
	}

	public void setStartableUsers(String s) {
		this.startableUsers = s;
	}

	public String getStartableGroups() {
		return this.startableGroups;
	}

	public void setStartableGroups(String s) {
		this.startableGroups = s;
	}

	public String getRpcParameter() {
		return this.rpcParameter;
	}

	public void setRpcParameter(String s) {
		this.rpcParameter = s;
	}

	public String getRpcReturn() {
		return this.rpcReturn;
	}

	public void setRpcReturn(String s) {
		this.rpcReturn = s;
	}

	public String getRpcReturnHeaders() {
		return this.rpcReturnHeaders;
	}

	public void setRpcReturnHeaders(String s) {
		this.rpcReturnHeaders = s;
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public List<String> getPermittedUsers() {
		return getStringList(this.startableUsers);
	}
	public List<String> getPermittedRoles() {
		return getStringList(this.startableGroups);
	}
	public List<Map> getParamList() {
		if( this.rpcParameter==null) return new ArrayList<Map>();
		Map  res =  (Map)ds.deserialize(this.rpcParameter);
		return (List<Map>)res.get("items");
	}
	public List<Map> getReturnHeaders() {
		if( this.rpcReturnHeaders==null) return new ArrayList<Map>();
		Map  res =  (Map)ds.deserialize(this.rpcReturnHeaders);
		return (List<Map>)res.get("items");
	}
	protected List<String> getStringList(String s) {
		if( s==null) return new ArrayList<String>();
		return Arrays.asList(s.split(","));
	}

}

