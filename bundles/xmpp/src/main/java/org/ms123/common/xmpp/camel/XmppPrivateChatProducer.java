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

import org.apache.camel.Exchange;
import org.apache.camel.RuntimeExchangeException;
import org.apache.camel.impl.DefaultProducer;
import org.apache.camel.util.ObjectHelper;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version 
 */
public class XmppPrivateChatProducer extends DefaultProducer {

	private static final Logger LOG = LoggerFactory.getLogger(XmppPrivateChatProducer.class);

	private final XmppEndpoint endpoint;
	private XMPPConnection connection;

	public XmppPrivateChatProducer(XmppEndpoint endpoint) {
		super(endpoint);
		this.endpoint = endpoint;
	}


	private XmppConnectionContext  handleCommand(Exchange exchange, String command, String username, String password, String participant) throws Exception{
		if (command == null) {
			return endpoint.createConnectionContext(username, password, participant);
		}
		exchange.setProperty(Exchange.ROUTE_STOP, Boolean.TRUE);
		if (command.equals(XmppConstants.COMMAND_OPEN)) {
			return endpoint.createConnectionContext(username, password, participant);
		}
		if (command.equals(XmppConstants.COMMAND_CLOSE)) {
			if( endpoint.hasConnectionContext(username)){
				XmppConnectionContext cc = endpoint.createConnectionContext(username, password, participant);
				if( cc.getConsumer() != null){
					cc.getConsumer().doStop();
				}
				endpoint.removeConnectionContext(cc);	
			}
		}
		return null;
	}

	public void process(Exchange exchange) {
		XmppConnectionContext cc = null;
		XMPPConnection connection = null;
		String username = exchange.getIn().getHeader(XmppConstants.USERNAME, String.class);
		String password = exchange.getIn().getHeader(XmppConstants.PASSWORD, String.class);
		String nickname = exchange.getIn().getHeader(XmppConstants.NICKNAME, String.class);
		String participant = exchange.getIn().getHeader(XmppConstants.TO, String.class);
		String command = exchange.getIn().getHeader(XmppConstants.COMMAND, String.class);
			System.out.println("command:"+command);
		try {
			cc = handleCommand(exchange, command, username,password, participant);
			if( cc == null){
				return;
			}	
			connection = cc.getConnection();
			if (!connection.isConnected()) {
				this.reconnect();
			}
		} catch (Exception e) {
			System.out.println("username:"+username);
			System.out.println("password:"+password);
			if( command == null){
				throw new RuntimeException("XmppPrivateChatProducer.Could not connect to XMPP server.", e);
			}else{
				throw new RuntimeException("XmppPrivateChatProducer.Could not handle command:"+command, e);
			}
		}
		String thread = "Chat:" + participant + ":" + username;
		cc.setParticipant(participant);
		cc.setUsername(username);
		ChatManager chatManager = connection.getChatManager();
		Chat chat = getOrCreateChat(chatManager, participant, thread, cc.getChatId());
		Message message = null;
		try {
			message = new Message();
			message.setTo(participant);
			message.setThread(thread);
			message.setType(Message.Type.normal);
			endpoint.getBinding().populateXmppMessage(message, exchange);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Sending XMPP message to {} from {} : {}", new Object[] { participant, cc.getUsername(), message.getBody() });
			}
			chat.sendMessage(message);
		} catch (XMPPException xmppe) {
			throw new RuntimeExchangeException("Could not send XMPP message: to " + participant + " from " + cc.getUsername() + " : " + message + " to: " + XmppEndpoint.getConnectionMessage(connection), exchange, xmppe);
		} catch (Exception e) {
			throw new RuntimeExchangeException("Could not send XMPP message to " + participant + " from " + cc.getUsername() + " : " + message + " to: " + XmppEndpoint.getConnectionMessage(connection), exchange, e);
		}
	}

	private synchronized Chat getOrCreateChat(ChatManager chatManager, final String participant, String thread, String chatid) {
		if (LOG.isTraceEnabled()) {
			LOG.trace("Looking for existing chat instance with thread ID {}", chatid);
		}
		Chat chat = chatManager.getThreadChat(thread);
		System.out.println("getThreadChat:" + chat);
		if (chat == null) {
			if (LOG.isTraceEnabled()) {
				LOG.trace("Creating new chat instance with thread ID {}", thread);
			}
			chat = chatManager.createChat(participant, thread, new MessageListener() {

				public void processMessage(Chat chat, Message message) {
					System.out.println("getThreadChat.processMessage:" + message);
					// not here to do conversation
					if (LOG.isDebugEnabled()) {
						LOG.debug("Received and discarding message from {} : {}", participant, message.getBody());
					}
				}
			});
		}
		return chat;
	}

	private synchronized void reconnect() throws XMPPException {
		if (!connection.isConnected()) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Reconnecting to: {}", XmppEndpoint.getConnectionMessage(connection));
			}
			connection.connect();
		}
	}

	@Override
	protected void doStart() throws Exception {
		/* if (connection == null) {
            try {
                connection = endpoint.createConnection();
            } catch (XMPPException e) {
                if (endpoint.isTestConnectionOnStartup()) {
                    throw new RuntimeException("Could not establish connection to XMPP server:  " + endpoint.getConnectionDescription(), e);
                } else {
                    LOG.warn("Could not connect to XMPP server. {}  Producer will attempt lazy connection when needed.", XmppEndpoint.getXmppExceptionLogMessage(e));
                }
            }
        }*/
		super.doStart();
	}

	@Override
	protected void doStop() throws Exception {
		/* if (connection != null && connection.isConnected()) {
            connection.disconnect();
        }
        connection = null;*/
		super.doStop();
	}
}
