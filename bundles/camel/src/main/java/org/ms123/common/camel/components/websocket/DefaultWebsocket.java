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
package org.ms123.common.camel.components.websocket;

import java.io.Serializable;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.api.CloseStatus;
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
	private volatile Session m_session;
	private Map<String, String> m_parameterMap;
	private Map<String, String> m_headers = new HashMap();
	private JSONDeserializer m_ds = new JSONDeserializer();
	private JSONSerializer m_js = new JSONSerializer();

	public DefaultWebsocket(Map<String, String> parameterMap, NodeSynchronization sync, WebsocketConsumer consumer) {
		this.m_parameterMap = parameterMap;
		this.m_sync = sync;
		this.m_consumer = consumer;
		extractHeaders();
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
		/*@@@MS*/
		Map body = new HashMap();
		body.put("command", "close");
		this.m_consumer.sendMessage(getConnectionKey(), body, m_headers,m_session);
		m_sync.removeSocket(this);
	}

	@Override
	public void onWebSocketConnect(Session sess) {
		this.m_session = sess;
		debug("onOpen {}", sess);
		m_sync.addSocket(this);
	}

	@Override
	public void onWebSocketError(Throwable cause) {
		cause.printStackTrace(System.err);
	}

	@Override
	public void onWebSocketText(String message) {
		debug("onMessage: {}", message);
		debug("\theaders: {}", m_headers);
		Object body = null;
		try {
			body = m_ds.deserialize(message);
			if (body instanceof Map) {
				/*@@@MS*/
				String command = (String) ((Map) body).get("command");
				if (command != null && "close".equals(command)) {
					CloseStatus cs = new CloseStatus(4001, "close on  client demand");
					m_session.close(cs);
					return;
				}
			}
		} catch (Exception e) {
			body = message;
		}
		if (this.m_consumer != null) {
			this.m_consumer.sendMessage(getConnectionKey(), body, m_headers,m_session);
		} else {
			debug("No consumer to handle message received: {}", message);
		}
	}

	public void sendMessage(String message) {
		debug("-> Websocket:" + message);
		m_session.getRemote().sendStringByFuture(message);
	}

	public String getConnectionKey() {
		return m_parameterMap.get("connectionKey");
	}

	private void extractHeaders() {
		for (Map.Entry<String, String> e : m_parameterMap.entrySet()) {
			String key = e.getKey();
			if (key.startsWith("Camel")) {
				m_headers.put(key, e.getValue());
			}
		}
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
