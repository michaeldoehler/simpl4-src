/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ms123.common.camel.components.websocket;

import java.io.Serializable;
import java.util.UUID;
import java.util.Map;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;
import org.apache.camel.Exchange;
import flexjson.*;

/**
 */
public class DefaultWebsocket implements WebSocketListener {

	private static final Logger LOG = LoggerFactory.getLogger(DefaultWebsocket.class);

	private final WebsocketConsumer m_consumer;
	private final NodeSynchronization m_sync;
	private String m_connectionKey;
	private volatile Session m_session;
	private Map<String,String> m_parameterMap;
	private JSONDeserializer m_ds = new JSONDeserializer();
	private JSONSerializer m_js = new JSONSerializer();

	public DefaultWebsocket(Map<String,String> parameterMap, NodeSynchronization sync, WebsocketConsumer consumer) {
		this.m_parameterMap = parameterMap;
		this.m_sync = sync;
		this.m_consumer = consumer;
		m_js.prettyPrint(true);
	}

	public RemoteEndpoint getRemote() {
		Session sess = this.m_session;
		return sess == null ? null : m_session.getRemote();
	}

	public Session getSession() {
		return m_session;
	}

	public boolean isConnected() {
		Session sess = this.m_session;
		return (sess != null) && (sess.isOpen());
	}

	public boolean isNotConnected() {
		Session sess = this.m_session;
		return (sess == null) || (!sess.isOpen());
	}

	@Override
	public void onWebSocketBinary(byte[] payload, int offset, int len) {
	}

	@Override
	public void onWebSocketClose(int statusCode, String reason) {
		this.m_session = null;
		debug("onClose {} {}", statusCode, reason);
		m_sync.removeSocket(this);
	}

	@Override
	public void onWebSocketConnect(Session sess) {
		this.m_session = sess;
		debug("onOpen {}", sess);
		this.m_connectionKey = getConnectionKey();
		m_sync.addSocket(this);
	}

	@Override
	public void onWebSocketError(Throwable cause) {
		cause.printStackTrace(System.err);
	}

	@Override
	public void onWebSocketText(String message) {
		debug("onMessage: {}", message);
		Map<String,Object> headers = null;
		Map<String,Object> properties = null;
		Map<String,Object> map = null;
		Object body = null;
		Object o = null;
		try{
			o  = m_ds.deserialize(message);
			if( o instanceof Map){
				map = (Map)o;
			}else{
				body = o;	
			}
		}catch(Exception e){
			body = o;	
		}
		if( map != null){
			headers = (Map)map.get(WebsocketConstants.HEADERS);
			properties = (Map)map.get(WebsocketConstants.PROPERTIES);
			body = map.get(WebsocketConstants.BODY);
			if(body==null && headers==null && properties==null){
				body = map;
			}
		}

		if (this.m_consumer != null) {
			this.m_consumer.sendMessage(getConnectionKey(), headers, properties, body);
		} else {
			debug("No consumer to handle message received: {}", message);
		}
	}

	public void sendMessage(Object _message) {
		String message = m_js.deepSerialize(_message);
		debug("-> Websocket:"+message);
		m_session.getRemote().sendStringByFuture(message);
	}

	public String getConnectionKey() {
			return m_parameterMap.get("connectionKey");
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
