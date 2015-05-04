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
import org.slf4j.helpers.MessageFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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

	private XmppConnectionContext handleCommand(Exchange exchange, String command, Map<String,Object> parameter, String username, String password, String resourceId, String participant) throws Exception {
		if (command == null) {
			return endpoint.getOrCreateConnectionContext(username, password, resourceId, participant);
		}
		exchange.setProperty(Exchange.ROUTE_STOP, Boolean.TRUE);
		if (command.equals(XmppConstants.COMMAND_OPEN)) {
			return endpoint.getOrCreateConnectionContext(username, password, resourceId, participant);
		}
		if (command.equals(XmppConstants.COMMAND_CLOSE)) {
			String sessionId = username+"/"+resourceId;
			debug("HandleCommand(close):" + endpoint.hasConnectionContext(sessionId)+ "-> " + sessionId);
			if (endpoint.hasConnectionContext(sessionId)) {
				XmppConnectionContext cc = endpoint.getOrCreateConnectionContext(username, password, resourceId, participant);
				endpoint.removeConnectionContext(cc);
				if (cc.getConsumer() != null) {
					cc.getConsumer().doStop();
				}
			}
		}
		if (command.equals(XmppConstants.COMMAND_ADDUSER)) {
			XmppConnectionContext cc = endpoint.getOrCreateConnectionContext(username, password, resourceId, participant);
			List<String> groupList = (List)parameter.get("groups");
			String[] groups = null;
			if( groupList!=null){
				groups = groupList.toArray(new String[groupList.size()]);
			}
			cc.getConnection().getRoster().createEntry((String)parameter.get("username"), (String)parameter.get("nickname"),groups);
		}
		return null;
	}

	public void process(Exchange exchange) {
		XmppConnectionContext cc = null;
		XMPPConnection connection = null;
		String username = exchange.getIn().getHeader(XmppConstants.USERNAME, String.class);
		String resourceId = exchange.getIn().getHeader(XmppConstants.RESOURCEID, String.class);
		String password = exchange.getIn().getHeader(XmppConstants.PASSWORD, String.class);
		String nickname = exchange.getIn().getHeader(XmppConstants.NICKNAME, String.class);
		String participant = exchange.getIn().getHeader(XmppConstants.TO, String.class);
		String command = exchange.getIn().getHeader(XmppConstants.COMMAND, String.class);
		Map<String,Object> parameter = exchange.getIn().getHeader(XmppConstants.PARAMETER, Map.class);
		if( command != null){
			String sessionId = username+"/"+resourceId;
			debug("Command:" + command + "/hasConnectionContext:" + endpoint.hasConnectionContext(sessionId));
		}
		try {
			cc = handleCommand(exchange, command, parameter, username, password, resourceId, participant);
			if (cc == null) {
				return;
			}
			connection = cc.getConnection();
			if (!connection.isConnected()) {
				this.reconnect();
			}
		} catch (Exception e) {
			debug("username:" + username);
			debug("password:" + password);
			if (command == null) {
				throw new RuntimeException("XmppPrivateChatProducer.Could not connect to XMPP server.", e);
			} else {
				throw new RuntimeException("XmppPrivateChatProducer.Could not handle command:" + command, e);
			}
		}
		if( "dummy".equals(participant)){
			return;
		}
		String thread = "Chat:" + participant + ":" + username;
		cc.setParticipant(participant);
		cc.setUsername(username);
		ChatManager chatManager = connection.getChatManager();
		Chat chat = getOrCreateChat(chatManager, cc.getConsumer(),participant, thread);
		Message message = null;
		try {
			message = new Message();
			message.setTo(participant);
			message.setThread(thread);
			message.setType(Message.Type.normal);
			endpoint.getBinding().populateXmppMessage(message, exchange);
			debug("Sending XmppMessage from {} to {} : {}", cc.getUsername(), participant, message.getBody() );
			chat.sendMessage(message);
		} catch (XMPPException xmppe) {
			throw new RuntimeExchangeException("Could not send XMPP message: to " + participant + " from " + cc.getUsername() + " : " + message + " to: " + XmppEndpoint.getConnectionMessage(connection), exchange, xmppe);
		} catch (Exception e) {
			throw new RuntimeExchangeException("Could not send XMPP message to " + participant + " from " + cc.getUsername() + " : " + message + " to: " + XmppEndpoint.getConnectionMessage(connection), exchange, e);
		}
	}

	private synchronized Chat getOrCreateChat(ChatManager chatManager, XmppConsumer consumer, final String participant, String thread) {
		debug("Looking chat instance with thread:"+ thread);
		Chat chat = chatManager.getThreadChat(thread);
		if (chat != null) {
			debug("\tThreadChat:" + chat+"/msL:"+chat.getListeners());
		}else{ 
			chat = chatManager.createChat(participant, thread, consumer );
		}
		return chat;
	}

	private synchronized void reconnect() throws XMPPException {
		if (!connection.isConnected()) {
			debug("Reconnecting to: {}", XmppEndpoint.getConnectionMessage(connection));
			connection.connect();
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

	protected void warn(String msg, Exception e) {
		System.out.println(msg);
		if (e != null)
			e.printStackTrace();
		LOG.warn(msg, e);
	}
	private Object[] varargsToArray(Object...args){
	 Object[] ret = new Object[args.length];
    for (int i = 0; i < args.length; i++) {
      ret[i] = args[i];
    }
		return ret;
	}
}
