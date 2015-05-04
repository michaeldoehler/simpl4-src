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
package org.ms123.common.xmpp.camel;

import java.util.Iterator;
import org.apache.camel.Consumer;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.impl.DefaultHeaderFilterStrategy;
import org.apache.camel.spi.HeaderFilterStrategy;
import org.apache.camel.spi.HeaderFilterStrategyAware;
import org.apache.camel.util.ObjectHelper;
import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smackx.muc.MultiUserChat;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

/**
 * A XMPP Endpoint
 *
 * @version 
 */
public class XmppEndpoint extends DefaultEndpoint implements HeaderFilterStrategyAware {

	private static final Logger LOG = LoggerFactory.getLogger(XmppEndpoint.class);

	private HeaderFilterStrategy headerFilterStrategy = new DefaultHeaderFilterStrategy();
	private XmppBinding binding;
	private String host;
	private int port;
	private String resource = "Camel";
	private boolean login = true;
	private boolean createAccount;
	private String room;
	private String serviceName;
	private Map<String, XmppConnectionContext> m_connectionContextMap = new ConcurrentHashMap();
	private Processor m_processor;
	private boolean testConnectionOnStartup = true;
	private int connectionPollDelay = 10;

	public XmppEndpoint() {
	}

	public XmppEndpoint(String uri, XmppComponent component) {
		super(uri, component);
	}

	@Deprecated
	public XmppEndpoint(String endpointUri) {
		super(endpointUri);
	}

	public Producer createProducer() throws Exception {
		if (room != null) {
			return createGroupChatProducer();
		} else {
			return createPrivateChatProducer();
		}
	}

	public Producer createGroupChatProducer() throws Exception {
		return new XmppGroupChatProducer(this);
	}

	public Producer createPrivateChatProducer() throws Exception {
		return new XmppPrivateChatProducer(this);
	}

	public Consumer createConsumer(Processor processor) throws Exception {
		m_processor = processor;
		return null;
	}

	private XmppConsumer createConsumer(XmppConnectionContext cc) throws Exception {
		XmppConsumer answer = new XmppConsumer(this, m_processor, cc);
		configureConsumer(answer);
		answer.doStart();
		return answer;
	}

	@Override
	public Exchange createExchange(ExchangePattern pattern) {
		return createExchange(pattern, null);
	}

	public Exchange createExchange(Message message) {
		return createExchange(getExchangePattern(), message);
	}

	private Exchange createExchange(ExchangePattern pattern, Message message) {
		Exchange exchange = new DefaultExchange(this, getExchangePattern());
		exchange.setProperty(Exchange.BINDING, getBinding());
		exchange.setIn(new XmppMessage(message));
		return exchange;
	}

	public Exchange createExchange(Map<String,Object> body) {
		org.apache.camel.Message message = new org.apache.camel.impl.DefaultMessage();
		message.setBody(body);
		Exchange exchange = new DefaultExchange(this, getExchangePattern());
		exchange.setIn(message);
		return exchange;
	}

	@Override
	protected String createEndpointUri() {
		return "xmpp://" + host + ":" + port + "?serviceName=" + serviceName;
	}

	public boolean isSingleton() {
		return true;
	}

	public boolean hasConnectionContext(String username) {
		return m_connectionContextMap.get(username) != null;
	}

	public synchronized XmppConnectionContext getOrCreateConnectionContext(String username, String password, String participant) throws XMPPException {
		XmppConnectionContext connectionContext = m_connectionContextMap.get(username);
		debug("GetOrCreateConnectionContext:connectionContext:" + connectionContext + "/" + username);
		XMPPConnection connection = null;
		if (connectionContext != null) {
			connection = connectionContext.getConnection();
		}
		if (connection != null && connection.isConnected()) {
			return connectionContext;
		}
		if (connection == null) {
			if (port > 0) {
				if (getServiceName() == null) {
					connection = new XMPPConnection(new ConnectionConfiguration(host, port));
				} else {
					connection = new XMPPConnection(new ConnectionConfiguration(host, port, serviceName));
				}
			} else {
				connection = new XMPPConnection(host);
			}
		}
		connection.connect();
		debug("GetOrCreateConnectionContext:connection created and/or connected");
		connection.addPacketListener(new XmppLogger("INBOUND"), new PacketFilter() {

			public boolean accept(Packet packet) {
				return true;
			}
		});
		connection.addPacketSendingListener(new XmppLogger("OUTBOUND"), new PacketFilter() {

			public boolean accept(Packet packet) {
				return true;
			}
		});
		if (!connection.isAuthenticated()) {
			if (username != null) {
				debug("Logging in to XMPP as user: {} on connection: {}", username, getConnectionMessage(connection));
				if (password == null) {
					warn("No password configured for user: {} on connection: {}", username, getConnectionMessage(connection));
				}
				if (createAccount) {
					AccountManager accountManager = new AccountManager(connection);
					accountManager.createAccount(username, password);
				}
				if (login) {
					if (resource != null) {
						connection.login(username, password, resource);
					} else {
						connection.login(username, password);
					}
				}
			} else {
				debug("Logging in anonymously to XMPP on connection: {}", getConnectionMessage(connection));
				connection.loginAnonymously();
			}
		}
		XmppConnectionContext cc = new XmppConnectionContext();
		cc.setUsername(username);
		cc.setUsername(password);
		cc.setParticipant(participant);
		cc.setConnection(connection);
		try {
			cc.setConsumer(createConsumer(cc));
		} catch (Exception e) {
			throw new RuntimeException("XmppEndpoint:Could not create Consumer.", e);
		}
		m_connectionContextMap.put(username, cc);
		return cc;
	}

	protected void removeConnectionContext(XmppConnectionContext cc) {
		m_connectionContextMap.remove(cc.getUsername());
		debug("XmppEndpoint.removeConnectionContext." + m_connectionContextMap);
	}

	/*
     * If there is no "@" symbol in the room, find the chat service JID and
     * return fully qualified JID for the room as room@conference.server.domain
     */
	public String resolveRoom(XMPPConnection connection) throws XMPPException {
		ObjectHelper.notEmpty(room, "room");
		if (room.indexOf('@', 0) != -1) {
			return room;
		}
		Iterator<String> iterator = MultiUserChat.getServiceNames(connection).iterator();
		if (!iterator.hasNext()) {
			throw new XMPPException("Cannot find Multi User Chat service on connection: " + getConnectionMessage(connection));
		}
		String chatServer = iterator.next();
		debug("Detected chat server: {}", chatServer);
		return room + "@" + chatServer;
	}

	public String getConnectionDescription() {
		return host + ":" + port + "/" + serviceName;
	}

	public static String getConnectionMessage(XMPPConnection connection) {
		return connection.getHost() + ":" + connection.getPort() + "/" + connection.getServiceName();
	}

	public static String getXmppExceptionLogMessage(XMPPException e) {
		XMPPError xmppError = e.getXMPPError();
		Throwable t = e.getWrappedThrowable();
		StringBuilder strBuff = new StringBuilder();
		if (xmppError != null) {
			strBuff.append("[ ").append(xmppError.getCode()).append(" ] ").append(xmppError.getCondition()).append(" : ").append(xmppError.getMessage());
		}
		if (t != null) {
			strBuff.append(" ( ").append(e.getWrappedThrowable().getMessage()).append(" )");
		}
		return strBuff.toString();
	}

	// Properties
	// -------------------------------------------------------------------------
	public XmppBinding getBinding() {
		if (binding == null) {
			binding = new XmppBinding(headerFilterStrategy);
		}
		return binding;
	}

	/**
     * Sets the binding used to convert from a Camel message to and from an XMPP
     * message
     */
	public void setBinding(XmppBinding binding) {
		this.binding = binding;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getResource() {
		return resource;
	}

	public void setResource(String resource) {
		this.resource = resource;
	}

	public boolean isLogin() {
		return login;
	}

	public void setLogin(boolean login) {
		this.login = login;
	}

	public boolean isCreateAccount() {
		return createAccount;
	}

	public void setCreateAccount(boolean createAccount) {
		this.createAccount = createAccount;
	}

	public String getRoom() {
		return room;
	}

	public void setRoom(String room) {
		this.room = room;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getServiceName() {
		return serviceName;
	}

	public HeaderFilterStrategy getHeaderFilterStrategy() {
		return headerFilterStrategy;
	}

	public void setHeaderFilterStrategy(HeaderFilterStrategy headerFilterStrategy) {
		this.headerFilterStrategy = headerFilterStrategy;
	}

	public boolean isTestConnectionOnStartup() {
		return testConnectionOnStartup;
	}

	public void setTestConnectionOnStartup(boolean testConnectionOnStartup) {
		this.testConnectionOnStartup = testConnectionOnStartup;
	}

	public int getConnectionPollDelay() {
		return connectionPollDelay;
	}

	public void setConnectionPollDelay(int connectionPollDelay) {
		this.connectionPollDelay = connectionPollDelay;
	}

	// Implementation methods
	// -------------------------------------------------------------------------
	@Override
	protected void doStop() throws Exception {
		for (Map.Entry<String, XmppConnectionContext> entry : m_connectionContextMap.entrySet()) {
			XmppConnectionContext cc = entry.getValue();
			cc.getConsumer().doStop();
		}
		m_connectionContextMap = new ConcurrentHashMap();
		binding = null;
	}

	protected void debug(String msg, Object... args) {
		System.out.println(MessageFormatter.arrayFormat(msg, varargsToArray(args)).getMessage());
		LOG.debug(msg, args);
	}

	protected void info(String msg, Object... args) {
		System.out.println(MessageFormatter.arrayFormat(msg, varargsToArray(args)).getMessage());
		LOG.info(msg, args);
	}

	protected void warn(String msg, Object... args) {
		System.out.println(MessageFormatter.arrayFormat(msg, varargsToArray(args)).getMessage());
		LOG.warn(msg, args);
	}

	private Object[] varargsToArray(Object... args) {
		Object[] ret = new Object[args.length];
		for (int i = 0; i < args.length; i++) {
			ret[i] = args[i];
		}
		return ret;
	}

	protected void warn(String msg, Exception e) {
		System.out.println(msg);
		if (e != null)
			e.printStackTrace();
		LOG.warn(msg, e);
	}
}
