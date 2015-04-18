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
package org.ms123.common.xmpp;

import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.metatype.*;
import java.io.*;
import java.util.*;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.ms123.common.rpc.PDefaultBool;
import org.ms123.common.rpc.PDefaultFloat;
import org.ms123.common.rpc.PDefaultInt;
import org.ms123.common.rpc.PDefaultLong;
import org.ms123.common.rpc.PDefaultString;
import org.ms123.common.rpc.PName;
import org.ms123.common.rpc.POptional;
import org.ms123.common.rpc.RpcException;
import org.ms123.common.permission.api.PermissionException;
import org.ms123.common.permission.api.PermissionService;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.ms123.common.rpc.JsonRpcServlet.ERROR_FROM_METHOD;
import static org.ms123.common.rpc.JsonRpcServlet.INTERNAL_SERVER_ERROR;
import static org.ms123.common.rpc.JsonRpcServlet.PERMISSION_DENIED;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.openfire.user.UserManager;
import org.jivesoftware.openfire.group.GroupManager;
import org.jivesoftware.openfire.auth.AuthFactory;
import org.jivesoftware.openfire.admin.AdminManager;
import org.jivesoftware.openfire.muc.ConflictException;
import org.jivesoftware.openfire.muc.ForbiddenException;
import org.jivesoftware.openfire.muc.MUCRole;
import org.jivesoftware.openfire.muc.MUCRoom;
import org.jivesoftware.openfire.muc.NotAllowedException;
import flexjson.*;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import java.util.Map;

/** XmppService implementation
 */
@SuppressWarnings("unchecked")
@Component(enabled = true, configurationPolicy = ConfigurationPolicy.optional, immediate = true, properties = { "rpc.prefix=xmpp" })
public class XmppServiceImpl extends BaseXmppServiceImpl implements XmppService {

	private static final Logger m_logger = LoggerFactory.getLogger(XmppServiceImpl.class);

	public XmppServiceImpl() {
	}

	protected void activate(BundleContext bundleContext, Map<?, ?> props) {
	 org.apache.taglibs.standard.tag.common.fmt.BundleSupport.registerClassLoader("openfire_i18n",  XMPPServer.class.getClassLoader());

		Simpl4Manager.setBundleContext(bundleContext);
		UserManager.setUserProvider(new Simpl4UserProvider());
		GroupManager.setGroupProvider(new Simpl4GroupProvider());
		AuthFactory.setAuthProvider(new Simpl4AuthProvider());
		AdminManager.setAdminProvider(new Simpl4AdminProvider());
		new XMPPServer();
		DbConnectionManager.setConnectionProvider(new Simpl4ConnectionProvider());
	}

	protected void deactivate() throws Exception {
	}

	@RequiresRoles("admin")
	public void createRoom(
			@PName("serviceName")      String serviceName, 
			@PName("roomSpec")         Map<String, Object> roomSpec) throws RpcException {
		String roomName = (String) roomSpec.get("roomName");
		try {
			createRoom(roomSpec, serviceName);
		} catch (NotAllowedException e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "Not allowed to create the room:" + roomName);
		} catch (ForbiddenException e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "It's forbitten to create the room:" + roomName);
		} catch (ConflictException e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "It'ss a conflict to create the room:" + roomName);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "Exception create the room:" + roomName+"/"+e.getMessage());
		}
	}

	@RequiresRoles("admin")
	public void updateRoom(
			@PName("serviceName")      String serviceName, 
			@PName("roomSpec")         Map<String, Object> roomSpec) throws RpcException {
		String roomName = (String) roomSpec.get("roomName");
		try {
			createRoom(roomSpec, serviceName);
		} catch (NotAllowedException e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "Not allowed to update the room:" + roomName);
		} catch (ForbiddenException e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "It's forbitten to update the room:" + roomName);
		} catch (ConflictException e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "It'ss a conflict to update the room:" + roomName);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "Exception update the room:" + roomName+"/"+e.getMessage());
		}
	}

	@RequiresRoles("admin")
	public void deleteRoom(
			@PName("serviceName") String serviceName, 
			@PName("roomName") String roomName) throws RpcException {
		try {
			MUCRoom room = XMPPServer.getInstance().getMultiUserChatManager().getMultiUserChatService(serviceName).getChatRoom(roomName.toLowerCase());
			if (room == null) {
				throw new Exception("Room not exists.");
			}
			room.destroyRoom(null, null);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "deleteRoom:" + roomName+"/"+e.getMessage());
		}
	}

	public List<Map> getRooms(
			@PName("serviceName") String serviceName, 
			@PName("channelType") @PDefaultString("all") @POptional String channelType, 
			@PName("roomSearch") @POptional String roomSearch) throws RpcException {
		try {
			List<MUCRoom> rooms = XMPPServer.getInstance().getMultiUserChatManager().getMultiUserChatService(serviceName).getChatRooms();
			List<Map> mucRoomList = new ArrayList<Map>();
			for (MUCRoom room : rooms) {
				if (roomSearch != null) {
					if (!room.getName().contains(roomSearch)) {
						continue;
					}
				}
				Map roomSpec = convertToRoomSpec(room);
				if (channelType.equals("all")) {
					mucRoomList.add(roomSpec);
				} else if (channelType.equals("public") && room.isPublicRoom()) {
					mucRoomList.add(roomSpec);
				}
			}
			return mucRoomList;
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "getRooms");
		}
	}

	public Map getRoom(
			@PName("serviceName") String serviceName, 
			@PName("roomName") String roomName) throws RpcException {
		try {
			MUCRoom room = XMPPServer.getInstance().getMultiUserChatManager().getMultiUserChatService(serviceName).getChatRoom(roomName);
			if (room == null) {
				throw new Exception("room could be not found." );
			}
			return convertToRoomSpec(room);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "getRoom:" + roomName+"/"+e.getMessage());
		}
	}

	public WebSocketAdapter createWebSocket(Map<String,Object> config){
		return new WebSocket(config);
	}

	public class WebSocket extends WebSocketAdapter {
		private Map<String,Object> m_config = null;
		int num = 0;
		public WebSocket(Map<String,Object> config){
			m_config = config;
		}

		@Override
		public void onWebSocketConnect(Session sess) {
			super.onWebSocketConnect(sess);
			System.out.println("Socket Connected: " + sess);
			System.out.println("S4WebSocket: " + hashCode());
		}

		@Override
		public void onWebSocketText(String message) {
			super.onWebSocketText(message);
			System.out.println("Received("+hashCode()+ ")TEXT message: " + message);
			getSession().getRemote().sendStringByFuture(message);
			num++;
		}

		@Override
		public void onWebSocketClose(int statusCode, String reason) {
			super.onWebSocketClose(statusCode, reason);
			System.out.println("Socket Closed: [" + statusCode + "] " + reason);
		}

		@Override
		public void onWebSocketError(Throwable cause) {
			super.onWebSocketError(cause);
			cause.printStackTrace(System.err);
		}
	}
}
