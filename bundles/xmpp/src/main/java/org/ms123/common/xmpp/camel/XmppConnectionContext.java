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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.camel.Exchange;
import org.apache.camel.Consumer;
import org.jivesoftware.smack.XMPPConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class XmppConnectionContext {

	private XMPPConnection m_connection;
	private XmppConsumer m_consumer;
	private String m_participant;
	private String m_username;
	private String m_nickname;

	public XmppConnectionContext() {
	}

	public void setConnection(XMPPConnection conn) {
		m_connection = conn;
	}

	public XMPPConnection getConnection() {
		return m_connection;
	}

	public void setParticipant(String participant) {
		m_participant = participant;
	}

	public String getParticipant() {
		return m_participant;
	}

	public void setUsername(String username) {
		m_username = username;
	}

	public String getUsername() {
		return m_username;
	}

	public void setNickname(String nickname) {
		m_nickname = nickname;
	}

	public String getNickname() {
		return m_nickname;
	}

	public void setConsumer(XmppConsumer consumer) {
		m_consumer = consumer;
	}

	public XmppConsumer getConsumer() {
		return m_consumer;
	}

	public String getChatId() {
		return "Chat:" + getParticipant() + ":" + getUsername();
	}

	public String toString() {
		return "Context[Username:" + getUsername() + "/Participant:" + getParticipant() + "]";
	}
}
