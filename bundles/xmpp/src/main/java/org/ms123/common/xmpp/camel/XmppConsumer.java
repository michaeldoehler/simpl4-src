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
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.filter.ToContainsFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smackx.muc.DiscussionHistory;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;
import java.util.Collection;

/**
 * A {@link org.apache.camel.Consumer Consumer} which listens to XMPP packets
 *
 * @version 
 */
public class XmppConsumer extends DefaultConsumer implements PacketListener, MessageListener, ChatManagerListener {

	private static final Logger LOG = LoggerFactory.getLogger(XmppConsumer.class);

	private final XmppEndpoint endpoint;
	private MultiUserChat muc;
	private Chat privateChat;
	private ChatManager chatManager;
	private XMPPConnection connection;
	private ScheduledExecutorService scheduledExecutor;
	private XmppConnectionContext m_connectionContext;

	public XmppConsumer(XmppEndpoint endpoint, Processor processor, XmppConnectionContext cc) {
		super(endpoint, processor);
		this.endpoint = endpoint;
		this.m_connectionContext = cc;
	}

	@Override
	protected void doStart() throws Exception {
		connection = m_connectionContext.getConnection();
		chatManager = connection.getChatManager();
		chatManager.addChatListener(this);
		if (endpoint.getRoom() == null) {
			/*privateChat = chatManager.getThreadChat(m_connectionContext.getChatId());
			if (privateChat != null) {
				debug("Adding listener to existing chat opened to " + privateChat.getParticipant());
				privateChat.addMessageListener(this);
			} else {
				privateChat = connection.getChatManager().createChat(m_connectionContext.getParticipant(), m_connectionContext.getChatId(), this);
				debug("Opening private chat to " + privateChat.getParticipant());
			}*/
		} else {
			// add the presence packet listener to the connection so we only get packets that concerns us
			// we must add the listener before creating the muc
			final ToContainsFilter toFilter = new ToContainsFilter(m_connectionContext.getParticipant());
			final AndFilter packetFilter = new AndFilter(new PacketTypeFilter(Presence.class), toFilter);
			connection.addPacketListener(this, packetFilter);
			muc = new MultiUserChat(connection, endpoint.resolveRoom(connection));
			muc.addMessageListener(this);
			DiscussionHistory history = new DiscussionHistory();
			history.setMaxChars(0);
			// we do not want any historical messages
			muc.join(m_connectionContext.getNickname(), null, history, SmackConfiguration.getPacketReplyTimeout());
			info("Joined room: {} as: {}", muc.getRoom(), m_connectionContext.getNickname());
		}
		this.startRobustConnectionMonitor();
		super.doStart();
	}

	protected void scheduleDelayedStart() throws Exception {
		Runnable startRunnable = new Runnable() {

			@Override
			public void run() {
				try {
					doStart();
				} catch (Exception e) {
					warn("Ignoring an exception caught in the startup connection poller thread.", e);
				}
			}
		};
		info("Delaying XMPP consumer startup for endpoint {}. Trying again in {} seconds.", URISupport.sanitizeUri(endpoint.getEndpointUri()), endpoint.getConnectionPollDelay());
		getExecutor().schedule(startRunnable, endpoint.getConnectionPollDelay(), TimeUnit.SECONDS);
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
		if (!connection.isConnected()) {
			info("Attempting to reconnect to: {}", XmppEndpoint.getConnectionMessage(connection));
			try {
				connection.connect();
			} catch (XMPPException e) {
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
	protected void doStop() throws Exception {
		super.doStop();
		if (scheduledExecutor != null) {
			getEndpoint().getCamelContext().getExecutorServiceManager().shutdownNow(scheduledExecutor);
			scheduledExecutor = null;
		}
		if (muc != null) {
			info("Leaving room: {}", muc.getRoom());
			muc.removeMessageListener(this);
			muc.leave();
			muc = null;
		}
		debug("Consumer.doStop.isConnected:" + connection.isConnected());
		if (connection != null && connection.isConnected()) {
			connection.disconnect();
		}
	}

	public void chatCreated(Chat chat, boolean createdLocally) {
		debug("Consumer.chatCreated:" + chat + "," + m_connectionContext.getUsername()+"/local:"+createdLocally);
		if (!createdLocally) {
			chat.addMessageListener(this);
			debug("Accepting incoming chat session from " + chat.getParticipant());
		}
	}

	public void processPacket(Packet packet) {
		debug("Received XMPP packet:"+packet  );
		if (packet instanceof Message) {
			processMessage(null, (Message) packet);
		}
	}

	public void processMessage(Chat chat, Message message) {
		debug("Received XMPP message for {} from {} : {}",  m_connectionContext.getUsername(), m_connectionContext.getParticipant(), message.getBody() );
		Exchange exchange = endpoint.createExchange(message);
		try {
			getProcessor().process(exchange);
		} catch (Exception e) {
			exchange.setException(e);
		} finally {
			// must remove message from muc to avoid messages stacking up and causing OutOfMemoryError
			// pollMessage is a non blocking method
			// (see http://issues.igniterealtime.org/browse/SMACK-129)
			if (muc != null) {
				muc.pollMessage();
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
