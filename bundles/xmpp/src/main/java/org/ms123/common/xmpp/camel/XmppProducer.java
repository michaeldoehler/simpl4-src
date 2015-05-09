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
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smackx.muc.DiscussionHistory;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.chatstates.packet.ChatStateExtension;
import org.jivesoftware.smackx.chatstates.ChatState;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smackx.disco.packet.DiscoverItems;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smack.filter.ToFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @version 
 */
public class XmppProducer extends DefaultProducer implements XmppConstants {

	private static final Logger LOG = LoggerFactory.getLogger(XmppProducer.class);

	private final XmppEndpoint endpoint;

	public XmppProducer(XmppEndpoint endpoint) {
		super(endpoint);
		this.endpoint = endpoint;
	}

	private XmppConnectionContext handleCommand(Exchange exchange, String command, Map<String,Object> parameter, String username, String password, String resourceId, String participant,String room) throws Exception {
		if (command == null) {
			return endpoint.getOrCreateConnectionContext(username, password, resourceId, participant);
		}
		exchange.setProperty(Exchange.ROUTE_STOP, Boolean.TRUE);
		if (command.equals(COMMAND_OPEN)) {
			return endpoint.getOrCreateConnectionContext(username, password, resourceId, participant);
		}
		if (command.equals(COMMAND_CLOSE)) {
			String sessionId = username+"/"+resourceId;
			if (endpoint.hasConnectionContext(sessionId)) {
				XmppConnectionContext cc = endpoint.getOrCreateConnectionContext(username, password, resourceId, participant);
				endpoint.removeConnectionContext(cc);
				if (cc.getConsumer() != null) {
					cc.getConsumer().doStop();
				}
			}
		}
		if (command.equals(COMMAND_ADDUSER)) {
			XmppConnectionContext cc = endpoint.getOrCreateConnectionContext(username, password, resourceId, participant);
			List<String> groupList = (List)parameter.get("groups");
			String[] groups = null;
			if( groupList!=null){
				groups = groupList.toArray(new String[groupList.size()]);
			}
			Roster.getInstanceFor(cc.getConnection()).createEntry((String)parameter.get("username"), (String)parameter.get("nickname"),groups);
		}
		if (command.equals(COMMAND_CHATSTATE)) {
			XmppConnectionContext cc = endpoint.getOrCreateConnectionContext(username, password, resourceId, participant);
			String state = (String)parameter.get("state");
			Message message=new Message();
			ChatStateExtension extension=new ChatStateExtension(ChatState.valueOf(state));
			message.addExtension(extension);
			ChatManager chatManager = ChatManager.getInstanceFor(cc.getConnection());
			String thread = "Chat:" + participant + ":" + username;
			Chat chat = getOrCreateChat(chatManager, cc.getConsumer(),participant, thread);
			chat.sendMessage(message);
		}
		return null;
	}

	public void process(Exchange exchange) {
		XmppConnectionContext cc = null;
		XMPPTCPConnection connection = null;
		String username = exchange.getIn().getHeader(USERNAME, String.class);
		String resourceId = exchange.getIn().getHeader(RESOURCEID, String.class);
		String password = exchange.getIn().getHeader(PASSWORD, String.class);
		String nickname = exchange.getIn().getHeader(NICKNAME, String.class);
		String participant = exchange.getIn().getHeader(TO, String.class);
		String roomname = exchange.getIn().getHeader(ROOM, String.class);
		String command = exchange.getIn().getHeader(COMMAND, String.class);
		Map<String,Object> parameter = exchange.getIn().getHeader(PARAMETER, Map.class);
		debugCommand( command, username,resourceId, roomname, participant);
		try {
			cc = handleCommand(exchange, command, parameter, username, password, resourceId, participant,roomname);
			if (cc == null) {
				return;
			}
			connection = cc.getConnection();
			if (!connection.isConnected()) {
				this.reconnect(connection);
			}
			createMaybeMUC(cc, roomname);
		} catch (Exception e) {
			if (command == null) {
				throw new RuntimeException("XmppProducer.Could not connect to XMPP server.", e);
			} else {
				throw new RuntimeException("XmppProducer.Could not handle command:" + command, e);
			}
		}
		if( "dummy".equals(participant)){
			return;
		}


		if( roomname != null){
			Message message = cc.getMUC(roomname).createMessage();
			message.setTo(cc.getFQRoomname(roomname));
			message.setFrom(cc.getUsername());

			endpoint.getBinding().populateXmppMessage(message, exchange);
			try {
				debug("Sending XMPP message toMUC({}): {}", cc.getFQRoomname(roomname), message.getBody());
				cc.getMUC(roomname).sendMessage(message);
				cc.getMUC(roomname).pollMessage();
			} catch (Exception e) {
				throw new RuntimeExchangeException("Could not send XMPP message: " + message, exchange, e);
			}
		}else{
			String thread = "Chat:" + participant + ":" + username;
			cc.setParticipant(participant);
			cc.setUsername(username);
			ChatManager chatManager = ChatManager.getInstanceFor(connection);
			Chat chat = getOrCreateChat(chatManager, cc.getConsumer(),participant, thread);
			Message message = null;
			try {
				message = new Message();
				message.setTo(participant);
				message.setThread(thread);
				message.setType(Message.Type.normal);
				endpoint.getBinding().populateXmppMessage(message, exchange);
				debug("Sending XmppMessage from {} to {} : {}", cc.getSessionId(), participant, message.getBody() );
				chat.sendMessage(message);
			} catch (Exception e) {
				throw new RuntimeExchangeException("Could not send XMPP message to " + participant + " from " + cc.getUsername() + " : " + message + " to: " + XmppEndpoint.getConnectionMessage(connection), exchange, e);
			}
		}
	}

	private synchronized Chat getOrCreateChat(ChatManager chatManager, XmppConsumer consumer, final String participant, String thread) {
		Chat chat = chatManager.getThreadChat(thread);
		if (chat != null) {
			debug("ThreadChat(" + chat+") found");
		}else{ 
			chat = chatManager.createChat(participant, thread, consumer );
		}
		return chat;
	}

	private synchronized void createMaybeMUC(XmppConnectionContext cc, String roomname) throws Exception {
		if( roomname == null || cc.getMUC(roomname) != null){
			return;
		}
		XMPPTCPConnection connection = cc.getConnection();

		// add the presence packet listener to the connection so we only get packets that concerns us
		// we must add the listener before creating the muc
		final ToFilter toFilter = new ToFilter(cc.getParticipant());
		final AndFilter packetFilter = new AndFilter(new PacketTypeFilter(Presence.class), toFilter);
		connection.addPacketListener(cc.getConsumer(), packetFilter);

		String fqRoomname = endpoint.resolveRoom(connection,roomname);
		MultiUserChat muc = MultiUserChatManager.getInstanceFor(connection).getMultiUserChat(fqRoomname);

		muc.addMessageListener(cc.getConsumer());
		DiscussionHistory history = new DiscussionHistory();
		history.setMaxChars(0); // we do not want any historical messages
		muc.join(cc.getNickname(), null, history, 5000L);
		muc.addParticipantListener(cc.getConsumer());
		info("Joined room: {} as: {}", fqRoomname, cc.getNickname());
		try{
		//	info("Joined room: {} members: {}", muc.getOccupants());
		//	info("Joined room: {} members: {}", muc.getParticipants());
			// Discover the list of items (i.e. occupants in this case) related to a room
			DiscoverItems result = ServiceDiscoveryManager.getInstanceFor(connection).discoverItems(fqRoomname);
			debug("DiscoverItems:"+result);
			for (DiscoverItems.Item item : result.getItems()) {
				//debug("\tMember:"+item);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		List<String>	occupants = muc.getOccupants();
		cc.getConsumer().sendPresence(fqRoomname,occupants);
		cc.putMUC(roomname,muc);
	}

	private synchronized void reconnect(XMPPTCPConnection connection) throws Exception {
		if (!connection.isConnected()) {
			debug("Reconnecting to: {}", XmppEndpoint.getConnectionMessage(connection));
			connection.connect();
		}
	}
	private void debugCommand(String command, String username, String resourceId,String roomname, String participant){
		String sessionId = username+"/"+resourceId;
		if( command != null){
			debug("Command(:" + command + "):hasConnectionContext("+sessionId+") -> " + endpoint.hasConnectionContext(sessionId));
		}else{
			debug("Username(:" + username + "):hasConnectionContext("+sessionId+") -> " + endpoint.hasConnectionContext(sessionId)+"/roomname:"+roomname+"/participant:"+participant);
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
