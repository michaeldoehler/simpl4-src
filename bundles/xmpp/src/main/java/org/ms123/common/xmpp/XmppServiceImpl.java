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
import org.ms123.common.camel.api.CamelService;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.Producer;
import org.apache.camel.Endpoint;
import org.apache.camel.Route;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import rx.Observable;
import rx.functions.Action1;
import org.apache.camel.rx.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.camel.ProducerTemplate;

/** XmppService implementation
 */
@SuppressWarnings("unchecked")
@Component(enabled = true, configurationPolicy = ConfigurationPolicy.optional, immediate = true, properties = { "rpc.prefix=xmpp" })
public class XmppServiceImpl extends BaseXmppServiceImpl implements XmppService {

	private static final Logger m_logger = LoggerFactory.getLogger(XmppServiceImpl.class);

	public XmppServiceImpl() {
	}

	protected void activate(BundleContext bundleContext, Map<?, ?> props) {
		org.apache.taglibs.standard.tag.common.fmt.BundleSupport.registerClassLoader("openfire_i18n", XMPPServer.class.getClassLoader());
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
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "Exception create the room:" + roomName + "/" + e.getMessage());
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
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "Exception update the room:" + roomName + "/" + e.getMessage());
		}
	}

	@RequiresRoles("admin")
	public void deleteRoom(
			@PName("serviceName")      String serviceName, 
			@PName("roomName")         String roomName) throws RpcException {
		try {
			MUCRoom room = XMPPServer.getInstance().getMultiUserChatManager().getMultiUserChatService(serviceName).getChatRoom(roomName.toLowerCase());
			if (room == null) {
				throw new Exception("Room not exists.");
			}
			room.destroyRoom(null, null);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "deleteRoom:" + roomName + "/" + e.getMessage());
		}
	}

	public List<Map> getRooms(
			@PName("serviceName")      String serviceName, 
			@PName("channelType")      @PDefaultString("all") @POptional String channelType, 
			@PName("roomSearch")       @POptional String roomSearch) throws RpcException {
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
			@PName("serviceName")      String serviceName, 
			@PName("roomName")         String roomName) throws RpcException {
		try {
			MUCRoom room = XMPPServer.getInstance().getMultiUserChatManager().getMultiUserChatService(serviceName).getChatRoom(roomName);
			if (room == null) {
				throw new Exception("room could be not found.");
			}
			return convertToRoomSpec(room);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "getRoom:" + roomName + "/" + e.getMessage());
		}
	}

	public WebSocketAdapter createWebSocket(Map<String, Object> config, Map<String, List<String>> parameterMap) {
		return new WebSocket(config, parameterMap);
	}

	public class WebSocket extends WebSocketAdapter {

		private Map<String, Object> m_config = null;

		private Map<String, List<String>> m_params = null;

		JSONDeserializer m_ds = new JSONDeserializer();

		int num = 0;
		private Route m_routeIn;
//		private Route m_routeOut;
		private CamelContext m_context;
//		private	Endpoint m_endpointOut;
		private ProducerTemplate m_outTemplate;
		private Endpoint m_epIn;

		public WebSocket(Map<String, Object> config, Map<String, List<String>> parameterMap) {
			m_config = config;
			m_params = parameterMap;
			System.out.println("parameterMap:" + m_params);
			Map<String,String> env = convertMap(parameterMap);
			String namespace = env.get("namespace");
			String routeIn = env.get("routeIn");
			String routeOut = env.get("routeOut");
			m_context = m_camelService.getCamelContext(namespace, "default");
info("Vor createProducer");
			m_outTemplate = m_context.createProducerTemplate();
info("Nach createProducer");
			try {
				List<Route> routes = m_camelService.createRoutes(namespace, routeIn, "admin", env, namespace+"/"+routeIn);
				m_routeIn = routes.get(0);
				info("routeIn:" + m_routeIn + "/" + m_routeIn.getId()+"/"+m_routeIn.getClass());
				//m_routeOut = m_camelService.createRoute(namespace, routeOut, "admin", env, namespace+"/"+routeOut); info("routeOut:" + m_routeOut + "/" + m_routeOut.getId()); m_endpointOut = m_routeOut.getEndpoint();
			} catch (Exception e) {
				e.printStackTrace();
			}
			ReactiveCamel reactiveCamel = new ReactiveCamel(m_context);
			Endpoint epOut = m_routeIn.getRouteContext().resolveEndpoint("direct:out");
			m_epIn = m_routeIn.getEndpoint();
info("EndpointIn:"+m_routeIn.getEndpoint());
info("EndpointOut:"+epOut);
			Observable<Message> observable = reactiveCamel.toObservable(epOut);
			//observable.toBlocking().forEach(new Action1<Message>() {
			observable.subscribe(new Action1<Message>() {

				@Override
				public void call(Message message) {
					String body = "Processing message headers " + message.getHeaders();
					System.out.print(body);
					System.out.println("\t" + message.getBody());
					if (message.getBody() != null) {
					}
				}
			});
		}

		@Override
		public void onWebSocketConnect(Session sess) {
			super.onWebSocketConnect(sess);
			System.out.println("Socket Connected: " + sess);
			try{
				m_context.startRoute(m_routeIn.getId());
				//m_context.startRoute(m_routeOut.getId());
			}catch(Exception e){
				e.printStackTrace();
				throw new RuntimeException("WebSocket.onWebSocketConnect:"+e);
			}
		}

		@Override
		public void onWebSocketText(String message) {
			super.onWebSocketText(message);
			Map<String,Object> event = (Map)m_ds.deserialize(message);
			Map<String,Object> data = (Map)event.get("data");
			System.out.println("Received(" + hashCode() + ")TEXT message: " + event);
			
			m_outTemplate.sendBody( m_epIn, data.get("content"));
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

	@Reference(dynamic = true, optional = true)
	public void setCamelService(CamelService paramService) {
		this.m_camelService = paramService;
		info("XmppServiceImpl.setCamelService:" + paramService);
	}

	private Map<String,String> convertMap( Map<String,List<String>> inMap){
		Map<String,String> outMap = new HashMap();
		for (Map.Entry<String, List<String>> entry : inMap.entrySet()) {
			outMap.put( entry.getKey() , StringUtils.join( entry.getValue(),","));
		}
System.out.println("convertMap:"+outMap);
		return outMap;
	}
}
