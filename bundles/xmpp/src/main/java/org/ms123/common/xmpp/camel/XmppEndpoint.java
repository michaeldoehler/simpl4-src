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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
		private Map<String,XmppConnectionContext> m_connectionContextMap = new HashMap();
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
        //XmppConsumer answer = new XmppConsumer(this, processor);
        //configureConsumer(answer);
        //return answer;
				return null;
    }

    private Consumer createConsumer(XmppConnectionContext cc) throws Exception {
        XmppConsumer answer = new XmppConsumer(this, m_processor,cc);
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

    @Override
    protected String createEndpointUri() {
        return "xmpp://" + host + ":" + port + "?serviceName=" + serviceName;
    }

    public boolean isSingleton() {
        return true;
    }

    public synchronized XmppConnectionContext createConnectionContext(String username, String password) throws XMPPException {
				XmppConnectionContext  connectionContext = m_connectionContextMap.get(username);

				XMPPConnection connection = null;
				if( connectionContext!= null){
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
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Logging in to XMPP as user: {} on connection: {}", username, getConnectionMessage(connection));
                }
                if (password == null) {
                    LOG.warn("No password configured for user: {} on connection: {}", username, getConnectionMessage(connection));
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
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Logging in anonymously to XMPP on connection: {}", getConnectionMessage(connection));
                }
                connection.loginAnonymously();
            }

            // presence is not needed to be sent after login
        }

				XmppConnectionContext cc = new XmppConnectionContext();
				cc.setConnection(connection);
				try{
					cc.setConsumer( createConsumer(cc));
        } catch (Exception e) {
            throw new RuntimeException("XmppEndpoint:Could not create Consumer.", e);
        }
				m_connectionContextMap.put( username, cc);
        return cc;
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
        LOG.debug("Detected chat server: {}", chatServer);

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
            strBuff.append("[ ").append(xmppError.getCode()).append(" ] ")
                .append(xmppError.getCondition()).append(" : ")
                .append(xmppError.getMessage());
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
        /*if (connection != null) {
            connection.disconnect();
        }
        connection = null;
        binding = null;*/
    }

}
