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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.OutputStream;
import java.io.InputStream;
import org.ms123.common.nucleus.api.NucleusService;
import org.ms123.common.permission.api.PermissionService;
import flexjson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import au.com.ds.ef.*;
import au.com.ds.ef.call.StateHandler;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.CloseStatus;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import static au.com.ds.ef.FlowBuilder.from;
import static au.com.ds.ef.FlowBuilder.fromTransitions;
import static au.com.ds.ef.FlowBuilder.on;
import au.com.ds.ef.call.ContextHandler;
import static org.ms123.common.wamp.WampRouterSession.Events.*;
import static org.ms123.common.wamp.WampRouterSession.States.*;
import org.ms123.common.wamp.WampMessages.WampMessage;

/**
 *
 */
class WampRouterSession {

	protected PermissionService m_permissionService;
	protected NucleusService m_nucleusService;
	protected JSONDeserializer m_ds = new JSONDeserializer();
	protected JSONSerializer m_js = new JSONSerializer();
	private WampService m_wampService;
	private WebSocketListener m_wsListener;
	private EasyFlow<FlowContext> m_flow;
	private FlowContext m_context = new FlowContext();

	private static class FlowContext extends StatefulContext {
	}

	enum States implements StateEnum {
		WEBSOCKET_CREATED, WEBSOCKET_CONNECTED, WAMP_SESSION_OPEN
	}

	enum Events implements EventEnum {
		websocketConnection,hello
	}

	protected WampRouterSession(WampService wampService, WebSocketListener ws) {
		m_wampService = wampService;
		m_wsListener = ws;
     initFlow();

     m_flow.start(m_context);
	}


	public void initFlow() {
		if (m_flow != null) {
			return;
		}
		from(WEBSOCKET_CREATED).transit(
				on(websocketConnection).to(WEBSOCKET_CONNECTED).transit(
						on(hello).to(WAMP_SESSION_OPEN)));

		m_flow.executor(new SyncExecutor())

		.whenEnter(WEBSOCKET_CONNECTED, new ContextHandler<FlowContext>() {
			@Override
			public void call(FlowContext context) throws Exception {
			}
		})

		.whenEnter(WAMP_SESSION_OPEN, new ContextHandler<FlowContext>() {
			@Override
			public void call(FlowContext context) throws Exception {
			}
		});
	}

	public void wsConnect(Session sess) {
		m_context.safeTrigger(websocketConnection);
	}

	public void wsMessage(String message) {
		try{
			WampMessage msg = WampDecode.decode(message.getBytes());
			debug("Message.recveived:"+msg);
		}catch(Exception e){
			e.printStackTrace();
		
		}
	}

	public void wsClose(int statusCode, String reason) {
	}

	public void wsError(Throwable cause) {
	}

	protected static void debug(String msg) {
		System.err.println(msg);
		m_logger.debug(msg);
	}

	protected static void info(String msg) {
		System.err.println(msg);
		m_logger.info(msg);
	}

	private static final Logger m_logger = LoggerFactory.getLogger(WampRouterSession.class);
}

