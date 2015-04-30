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
import org.eclipse.jetty.websocket.api.CloseStatus;
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
import org.ms123.common.xmpp.camel.XmppMessage;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import org.apache.camel.rx.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.camel.ProducerTemplate;
import org.jivesoftware.smackx.packet.MessageEvent;
import static org.ms123.common.camel.api.CamelService.PROPERTIES;
import static org.ms123.common.xmpp.camel.XmppConstants.USERNAME;
import static org.ms123.common.xmpp.camel.XmppConstants.PASSWORD;
import static org.ms123.common.xmpp.camel.XmppConstants.COMMAND;
import static org.ms123.common.xmpp.camel.XmppConstants.COMMAND_OPEN;
import static org.ms123.common.xmpp.camel.XmppConstants.COMMAND_CLOSE;
import static org.ms123.common.xmpp.camel.XmppConstants.TO;


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
		JSONDeserializer m_ds = new JSONDeserializer();
		JSONSerializer m_js = new JSONSerializer();
		private Map<String, String> m_params;
		private ProducerTemplate m_outTemplate;
		private CamelContext m_context;
		private Session m_session;
		private Endpoint sendEndpoint;

		public WebSocket(Map<String, Object> config, Map<String, List<String>> parameterMap) {
			m_js.prettyPrint(true);
			m_config = config;
			m_params = convertMap(parameterMap);
			String namespace = m_params.get("namespace");
			String routesName = m_params.get("routes");
			m_context = m_camelService.getCamelContext(namespace, "default");
			m_outTemplate = m_context.createProducerTemplate();


			Map shape = getCamelShape(namespace, routesName);
			String recvEndpointUri = getString(shape, "recvEndpoint",null);
			String sendEndpointUri = getString(shape, "sendEndpoint",null);
			if( recvEndpointUri == null){
				throw new RuntimeException("XmppServiceImpl:Missing \"ReceiveEndpoint\" in:"+namespace+"/"+routesName);
			}
			if( sendEndpointUri == null){
				throw new RuntimeException("XmppServiceImpl:Missing \"SendEndpoint\" in:"+namespace+"/"+routesName);
			}

			sendEndpoint = m_context.getEndpoint( sendEndpointUri);
			Endpoint recvEndpoint = m_context.getEndpoint( recvEndpointUri);

			Action1 action = new Action1<Message>() {

        @Override
        public void call(Message _message) {
          org.jivesoftware.smack.packet.Message message =  ((XmppMessage)_message).getXmppMessage();;
          Map<String,Object>  sendMap = new HashMap();
          Collection<String> propertyNames = message.getPropertyNames();
          for( String name : propertyNames){
            sendMap.put( name, message.getProperty(name));
          }
          String defaultSubject = message.getSubject();

          List<String> subList = new ArrayList();
          for (org.jivesoftware.smack.packet.Message.Subject subject : message.getSubjects()) {
            String sub = subject.getSubject();
            if(defaultSubject.equals(sub)) continue;
            subList.add(sub);
          }

          sendMap.put("from", message.getFrom());
          sendMap.put("to", message.getTo());
          sendMap.put("packetId", message.getPacketID());
          if( defaultSubject != null){
            sendMap.put("subject", defaultSubject);
          }
          if(subList.size() >0){
            sendMap.put("subjects", subList);
          }
          sendMap.put("thread", message.getThread());
          sendMap.put("type", message.getType());
          sendMap.put("language", message.getLanguage());
          sendMap.put("body", message.getBody());

          List<String> extensionList = new ArrayList();
          for( org.jivesoftware.smack.packet.PacketExtension ep : message.getExtensions()){
            if( ep instanceof org.jivesoftware.smackx.packet.ChatStateExtension){
              sendMap.put("chatState", ep.getElementName());
            }
            if( ep instanceof MessageEvent){
              MessageEvent me = (MessageEvent)ep;
              sendMap.put("isComposing", me.isComposing());
              sendMap.put("isOffline", me.isOffline());
            }
          }

          String sendString = m_js.deepSerialize(sendMap);
          System.out.println("\nXMPPMessage:"+sendString);
          m_session.getRemote().sendStringByFuture(sendString);
        }
      };
 
			ReactiveCamel reactiveCamel = new ReactiveCamel(m_context);
			Observable<Message> observable = reactiveCamel.toObservable(recvEndpoint);

			observable.filter(new Func1<Message, Boolean>() {

				@Override
				public Boolean call(Message _message) {
					org.jivesoftware.smack.packet.Message message =  ((XmppMessage)_message).getXmppMessage();;
					String to = message.getTo();
					String username = m_params.get("username") + "@";
					System.out.print("filter:"+username);
					if( to.startsWith( username)){
						System.out.println(" -> ok");
						return true;
					}
					System.out.println(" -> not ok");
					return false;
				}
			}).subscribe(action);
		}

		@Override
		public void onWebSocketConnect(Session sess) {
			super.onWebSocketConnect(sess);
			m_session = sess;
			System.out.println("Socket Connected: " + sess);
			Map<String,Object> headers = getHeaders("dummy", COMMAND_OPEN);
			try{
				m_outTemplate.sendBodyAndHeaders(sendEndpoint, null,headers);
			}catch(Throwable e){
				e.printStackTrace();
				String msg = e.getMessage();
				while (e.getCause() != null) {
					e = e.getCause();
					msg = e.getMessage();
				}
				System.out.println("Msg:"+msg);
				CloseStatus cs = new CloseStatus(4000, msg);
				m_session.close(cs);
			}
		}

		@Override
		public void onWebSocketText(String message) {
			super.onWebSocketText(message);
			Map<String, Object> map = (Map) m_ds.deserialize(message);
			String body = (String)map.get("body");
			String participant = (String)map.get("participant");
			System.out.println("FromSocket(" + hashCode() + ") message: " + map);
			if( body != null){
				Map<String,Object> headers = getHeaders(participant, null);
				m_outTemplate.sendBodyAndHeaders(sendEndpoint, body,headers);
			}
		}

		@Override
		public void onWebSocketClose(int statusCode, String reason) {
			super.onWebSocketClose(statusCode, reason);
			System.out.println("Socket Closed: [" + statusCode + "] " + reason);
			Map<String,Object> headers = getHeaders("dummy", COMMAND_CLOSE);
			m_outTemplate.sendBodyAndHeaders(sendEndpoint, null,headers);
		}

		@Override
		public void onWebSocketError(Throwable cause) {
			super.onWebSocketError(cause);
			cause.printStackTrace(System.err);
		}

		private Map<String, String> convertMap(Map<String, List<String>> inMap) {
			Map<String, String> outMap = new HashMap();
			for (Map.Entry<String, List<String>> entry : inMap.entrySet()) {
				outMap.put(entry.getKey(), StringUtils.join(entry.getValue(), ","));
			}
			System.out.println("convertMap:" + outMap);
			return outMap;
		}

		protected Map getCamelShape(String ns, String name) {
			Map shape = m_camelService.getShapeByRouteId(ns, name);
			if (shape == null) {
				if( !name.endsWith(".camel")){
					shape = m_camelService.getShapeByRouteId(ns, name+".camel");
				}
			}
			if (shape == null) {
				throw new RuntimeException("XmppServiceImpl.routeShape: " + name + "(.camel) not found");
			}
			return shape;
		}
		protected String getString(Map shape, String name,String _default) {
			Map properties = (Map) shape.get(PROPERTIES);

			Object value  = properties.get(name);
			if( value == null) return _default;
			return (String)value;
		}
		private Map<String,Object> getHeaders(String to, String cmd){
			Map<String,Object> headers = new HashMap();
			headers.put(USERNAME,m_params.get("username"));
			headers.put(PASSWORD,m_params.get("password"));
			headers.put(TO,to);
			if( cmd!=null){
				headers.put(COMMAND,cmd);
			}
System.out.println("Header:"+headers);
			return headers;
		}
	}

	@Reference(dynamic = true, optional = true)
	public void setCamelService(CamelService paramService) {
		this.m_camelService = paramService;
		info("XmppServiceImpl.setCamelService:" + paramService);
	}

}
