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

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.impl.DefaultConsumer;
import org.apache.camel.util.URISupport;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterListener;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.PresenceListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smackx.muc.DiscussionHistory;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * A {@link org.apache.camel.Consumer Consumer} which listens to XMPP packets
 *
 * @version 
 */
public class XmppConsumer extends DefaultConsumer implements PresenceListener, RosterListener, PacketListener, MessageListener, ChatMessageListener, ChatManagerListener {

	private static final Logger LOG = LoggerFactory.getLogger(XmppConsumer.class);

	private final XmppEndpoint endpoint;
	private XMPPTCPConnection m_connection;
	private ScheduledExecutorService scheduledExecutor;
	private XmppConnectionContext m_connectionContext;

	public XmppConsumer(XmppEndpoint endpoint, Processor processor, XmppConnectionContext cc) {
		super(endpoint, processor);
		this.endpoint = endpoint;
		this.m_connectionContext = cc;
	}

	@Override
	protected void doStart() throws Exception {
		m_connection = m_connectionContext.getConnection();
		ChatManager chatManager = ChatManager.getInstanceFor(m_connection);
		chatManager.addChatListener(this);
		Roster roster = Roster.getInstanceFor(m_connection);
		roster.addRosterListener( this);
		this.startRobustConnectionMonitor();
		super.doStart();
		//sendRoster();
	}

	private void startRobustConnectionMonitor() throws Exception {
		Runnable connectionCheckRunnable = new Runnable() {

			@Override
			public void run() {
				try {
					checkConnection();
				} catch (Exception e) {
					warn("Ignoring an exception caught in the connection poller thread.", e);
				}
			}
		};
		// background thread to detect and repair lost connections
		getExecutor().scheduleAtFixedRate(connectionCheckRunnable, endpoint.getConnectionPollDelay(), endpoint.getConnectionPollDelay(), TimeUnit.SECONDS);
	}

	private void checkConnection() throws Exception {
		if (!m_connection.isConnected()) {
			info("Attempting to reconnect to: {}", XmppEndpoint.getConnectionMessage(m_connection));
			try {
				m_connection.connect();
			} catch (XMPPException.XMPPErrorException e) {
				warn(XmppEndpoint.getXmppExceptionLogMessage(e), null);
			}
		}
	}

	private ScheduledExecutorService getExecutor() {
		if (this.scheduledExecutor == null) {
			scheduledExecutor = getEndpoint().getCamelContext().getExecutorServiceManager().newSingleThreadScheduledExecutor(this, "connectionPoll");
		}
		return scheduledExecutor;
	}

	@Override
	protected synchronized void doStop() throws Exception {
		if (scheduledExecutor != null) {
			getEndpoint().getCamelContext().getExecutorServiceManager().shutdownNow(scheduledExecutor);
			scheduledExecutor = null;
		}
		Map<String,MultiUserChat> mucs = m_connectionContext.getMUCs();
		for (MultiUserChat muc : mucs.values()) {
			info("Leaving room: {}", muc.getRoom());
			muc.leave();
			muc.removeMessageListener(this);
			muc.removeParticipantListener(this);
		}
		mucs.clear();
		if (m_connection != null && m_connection.isConnected()) {
			try{
				m_connection.disconnect();
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		debug("Consumer.doStop.isConnected:" + m_connection.isConnected());
	}

	public void chatCreated(Chat chat, boolean createdLocally) {
		debug("Consumer.chatCreated:" + chat + "," + m_connectionContext.getSessionId()+"/local:"+createdLocally);
		if (!createdLocally) {
			chat.addMessageListener(this);
			debug("Accepting incoming chat session from " + chat.getParticipant());
		}
	}

	private void sleep(Long time){
		try{
			Thread.sleep(time);
		}catch(Exception e){
		}
	}
	public void processPacket(Stanza packet) {
		debug("Received XMPP packet:"+packet  );
		if (packet instanceof Message) {
			processMessage( (Message) packet);
		}
	}

	public void processPresence(Presence presence) {
		debug("Received XMPP presence:"+presence  );
		String fqFrom = presence.getFrom();
		String from = fqFrom.split("@")[0];
		debug("processPresence.from2:"+fqFrom+"/sess:"+m_connectionContext.getSessionId());
		MultiUserChat muc = m_connectionContext.getMUC(from);
		if( muc != null){
			List<String>	occupants = muc.getOccupants();
			sendPresence(fqFrom, occupants);
		}
	}
	public void processMessage(Message message) {
		processMessage(null,message);
	}

	public void processMessage(Chat chat,  Message message) {
		debug("Received XMPP message for session:{} to {}  from {} : {}",  m_connectionContext.getSessionId(),message.getTo() , message.getFrom(), message.getBody() );
		Exchange exchange = endpoint.createExchange(message);
		try {
			exchange.getIn().setHeader(XmppConstants.SESSIONID, m_connectionContext.getSessionId());
			getProcessor().process(exchange);
		} catch (Exception e) {
			exchange.setException(e);
		} finally {
			Map<String,MultiUserChat> mucs = m_connectionContext.getMUCs();
			try{
				for (MultiUserChat muc : mucs.values()) {
					muc.pollMessage();
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}

	public void processMessage(Map<String,Object> body) {
		debug("processMessage:"+body+" -> "+m_connectionContext.getSessionId());
		Exchange exchange = endpoint.createExchange(body);
		exchange.getIn().setHeader(XmppConstants.SESSIONID, m_connectionContext.getSessionId());
		try {
			getProcessor().process(exchange);
		} catch (Exception e) {
			exchange.setException(e);
		}
	}
	/* RosterListener Start ================================ */
	private void sendRoster(){
		Roster roster = Roster.getInstanceFor(m_connectionContext.getConnection());
		Collection<RosterEntry> entries =  roster.getEntries();
		debug("Entries:"+entries);
		debug("UEntries:"+roster.getUnfiledEntries());
		List<Map<String,String>> retList = new ArrayList();
		for( RosterEntry entry : entries){
			Map<String,String> m = new HashMap();
			m.put("username", entry.getUser());
			retList.add(m);
		}
		Map<String,Object> body = new HashMap();
		body.put("rosterEntries", retList);
		processMessage( body);
	}

	private List<String> removeRoom(List<String> in){
		List<String> out = new ArrayList();
		for( String one : in ){
			out.add(one.split("/")[1]);
		}
		return out;
	}

	protected void sendPresence(String from, List<String> occupants){
		Map<String,Object> body = new HashMap();
		body.put("from", from);
		body.put("presence", removeRoom(occupants));
		processMessage( body);
	}

	public void	entriesAdded(Collection<String> addresses){
		debug("Roster.entriesAdded("+m_connectionContext.getSessionId()+"):"+addresses);
		sendRoster();
	}
	public void	entriesDeleted(Collection<String> addresses){
		debug("Roster.entriesDeleted("+m_connectionContext.getSessionId()+"):"+addresses);
		sendRoster();
	}
	public void	entriesUpdated(Collection<String> addresses){
		debug("Roster.entriesUpdated("+m_connectionContext.getSessionId()+"):"+addresses);
		sendRoster();
	}
	public void	presenceChanged(Presence presence){
		debug("Roster.presenceChanged(" + presence.getFrom()+" -> "+presence.getTo()+"):"+presence);
		if( Presence.Type.available.equals(presence.getType())){
//			sendRoster();
		}
	}
	/* RosterListener End ================================== */

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
