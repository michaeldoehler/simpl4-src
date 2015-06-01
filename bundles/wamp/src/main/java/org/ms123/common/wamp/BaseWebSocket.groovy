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
package org.ms123.common.wamp;

import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;

/**
 */
@groovy.transform.CompileStatic
@groovy.transform.TypeChecked
public class BaseWebSocket implements WebSocketListener {

	protected Session m_session;

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
	}

	@Override
	public void onWebSocketConnect(Session sess) {
		this.m_session = sess;
	}

	@Override
	public void onWebSocketError(Throwable cause) {
	}

	@Override
	public void onWebSocketText(String message) {
	}
}
