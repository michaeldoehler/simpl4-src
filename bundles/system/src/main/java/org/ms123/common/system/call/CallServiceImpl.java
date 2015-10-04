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
package org.ms123.common.system.call;

import aQute.bnd.annotation.component.*;
import aQute.bnd.annotation.metatype.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.Writer;
import java.io.OutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileInputStream;
import java.io.ByteArrayInputStream;
import org.apache.camel.Route;
import org.apache.camel.CamelContext;
import org.ms123.common.rpc.PName;
import org.ms123.common.rpc.POptional;
import org.ms123.common.rpc.RpcException;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.permission.api.PermissionService;
import org.ms123.common.camel.api.CamelService;
import org.ms123.common.git.GitService;
import org.osgi.framework.BundleContext;
import org.ms123.common.rpc.RpcException;
import org.ms123.common.rpc.JsonRpcServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import static org.apache.commons.io.IOUtils.copy;
import static org.ms123.common.rpc.JsonRpcServlet.ERROR_FROM_METHOD;
import static org.ms123.common.rpc.JsonRpcServlet.INTERNAL_SERVER_ERROR;
import static org.ms123.common.rpc.JsonRpcServlet.PERMISSION_DENIED;
import static org.ms123.common.system.history.HistoryService.HISTORY_ACTIVITI_PROCESS_KEY;
import static org.ms123.common.system.history.HistoryService.HISTORY_ACTIVITI_ACTIVITY_KEY;
import static org.ms123.common.system.history.HistoryService.CAMEL_ROUTE_DEFINITION_KEY;

/** CallService implementation
 */
@SuppressWarnings("unchecked")
@Component(enabled = true, configurationPolicy = ConfigurationPolicy.optional, immediate = true, properties = { "rpc.prefix=call" })
public class CallServiceImpl extends BaseCallServiceImpl implements org.ms123.common.rpc.CallService {

	public CallServiceImpl() {
	}

	protected void activate(BundleContext bundleContext, Map<?, ?> props) {
	}

	public void update(Map<String, Object> props) {
	}

	protected void deactivate() throws Exception {
	}

	public Object callCamel(String methodName, Object _methodParams) {
			return callCamel(methodName,_methodParams,null,null);
	}

	public Object callCamel(String methodName, Object _methodParams, HttpServletRequest request, HttpServletResponse response) {
		Map methodParams = (Map) _methodParams;
		int dot = methodName.indexOf(".");
		String namespace = null;
		if( dot > 0){
			String a[] = methodName.split("\\.");
			namespace = a[0];
			methodName = a[1];
		}
		if( namespace == null){
			namespace = getNamespace(methodParams);
			if (namespace == null) {
				throw new RpcException(JsonRpcServlet.ERROR_FROM_SERVER, JsonRpcServlet.PARAMETER_MISMATCH, "Method("+methodName+"):Namespace not found");
			}
		}
		info("Namespace/Procedure:"+namespace+"/"+methodName);
		String fqMethodName = namespace+"."+methodName;
		Map shape  = this.getProcedureShape(namespace,methodName );
		if( shape == null){
			info("getProcedureShape is null:"+fqMethodName);
			shape = getRootShape(namespace, methodName);
		}
		if (shape == null) {
			throw new RpcException(JsonRpcServlet.ERROR_FROM_SERVER, JsonRpcServlet.METHOD_NOT_FOUND, "Method \"" + fqMethodName + "\" not found");
		}
		if(!isRPC(shape)){
			info("Shape.isRPC:"+shape);
			throw new RpcException(JsonRpcServlet.ERROR_FROM_SERVER, JsonRpcServlet.METHOD_NOT_FOUND, "RPC in \"" + fqMethodName + "\" not enabled");
		}
		List<String> permittedRoleList = getStringList(shape, "startableGroups");
		List<String> permittedUserList = getStringList(shape, "startableUsers");
		String userName = getUserName();
		List<String> userRoleList = getUserRoles(userName);
		debug("userName:" + userName);
		info("userRoleList:" + userRoleList);
		info("permittedRoleList:" + permittedRoleList);
		info("permittedUserList:" + permittedUserList);
		if (!isPermitted(userName, userRoleList, permittedUserList, permittedRoleList)) {
			throw new RpcException(JsonRpcServlet.ERROR_FROM_METHOD, JsonRpcServlet.PERMISSION_DENIED, "Method("+fqMethodName+"):User(" + userName + ") has no permission");
		}

		Map<String, Object> properties = new TreeMap();
		Map<String, Object> headers = new HashMap();
		Map<String, Object> bodyMap = new HashMap();
		Object bodyObj=null;
		List<Map> paramList = getItemList(shape, "rpcParameter");
		int bodyCount = countBodyParams(paramList);
		for (Map param : paramList) {
			String destination = (String) param.get("destination");
			String name = (String) param.get("name");
			String destname = (String) param.get("destname");
			if( isEmpty(destname)){
				destname = name;
			}	
			Object def = param.get("defaultvalue");
			Class type = m_types.get((String) param.get("type"));
			Boolean opt = (Boolean) param.get("optional");
			if ("property".equals(destination)) {
				properties.put(destname, getValue(name, methodParams.get(name), def, opt, type,fqMethodName));
			} else if ("header".equals(destination)) {
				headers.put(destname, getValue(name, methodParams.get(name), def, opt, type,fqMethodName));
			} else if ("body".equals(destination)) {
				bodyObj = getValue(name, methodParams.get(name), def, opt, type,fqMethodName);
				bodyMap.put(destname, bodyObj);
			}
		}

		Map acp =(Map) methodParams.get(ACTIVITI_CAMEL_PROPERTIES);
		if( acp != null){
				properties.putAll(acp);
		}
		if( bodyCount != 1){
			if( bodyMap.keySet().size()>0){
				bodyObj = bodyMap;
			}else{
				bodyObj = null;
			}
		}
		properties.put("__logExceptionsOnly", getBoolean(shape, "logExceptionsOnly", false));
		info("methodParams:" + methodParams);
		debug("paramList:" + paramList);
		debug("properties:" + properties);
		debug("headers:" + headers);


		String returnSpec = getString(shape, "rpcReturn", "body");
		List<String> returnHeaderList = new ArrayList();
		List<Map> rh = getItemList(shape, "rpcReturnHeaders");
		if( rh!=null){
			for( Map<String,String> m : rh){
				returnHeaderList.add( m.get("name"));
			}
		}

		String routeId = getId(shape);
		CamelContext cc = m_camelService.getCamelContext(namespace);
		Route route = cc.getRoute(routeId);
		if( route == null){ //Maybe multiple routes
			route= getRouteWithDirectConsumer(cc, routeId);
			if( route == null){
				throw new RpcException(JsonRpcServlet.ERROR_FROM_METHOD, JsonRpcServlet.INTERNAL_SERVER_ERROR, "CamelRouteService:route for '"+routeId+"' not found");
			}
		}
		properties.put(CAMEL_ROUTE_DEFINITION_KEY, namespace+"/"+routeId );
		debug("Endpoint:" + route.getEndpoint());
		Object answer = null;
		try {
			answer = m_camelService.camelSend(namespace, route.getEndpoint(), bodyObj, headers, properties,returnSpec, returnHeaderList);
			debug("CallServiceImpl.Answer:" + answer);
			if( answer != null){
				debug("CallServiceImpl.Answer.type:" + answer.getClass());
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RpcException(JsonRpcServlet.ERROR_FROM_METHOD, JsonRpcServlet.INTERNAL_SERVER_ERROR, "CamelRouteService", e);
		}
		if( "bodyWithMime".equals(returnSpec)){
			if( response == null){
				throw new RpcException(JsonRpcServlet.ERROR_FROM_METHOD, JsonRpcServlet.INTERNAL_SERVER_ERROR, "CamelRouteService:returnSpec is \"bodyWithMime\" and response is null");
			}
			String mime = getString(shape, "mimetype", "text/html");
			response.setContentType(mime);
			boolean bytes=false;
			if( answer instanceof byte[] ){
				bytes=true;
			}
			try {
				if( bytes || mime.startsWith("image/") || mime.endsWith("pdf")){
					OutputStream os = response.getOutputStream();
					if( bytes ){
						copy( new ByteArrayInputStream((byte[])answer) , os );
						os.close();
					}else{
						copy( new FileInputStream((File)answer) , os );
						os.close();
					}
				}else{
					response.setCharacterEncoding( "UTF-8" );
					final Writer responseWriter = response.getWriter();
					response.setStatus(HttpServletResponse.SC_OK);
					if( answer instanceof String ){
						responseWriter.write(String.valueOf(answer));
					}else{
						copy( new FileReader((File)answer), responseWriter); 
					}
					responseWriter.close();
				}
			} catch (Exception e) {
				throw new RpcException(JsonRpcServlet.ERROR_FROM_METHOD, JsonRpcServlet.INTERNAL_SERVER_ERROR, "CamelRouteService:response to method \"" + methodName + "\":",e);
			}
			return null;
		}else{
			return answer;
		}
	}

	public void callHooks(Map props) {
		String serviceName = (String) props.get(SERVICENAME);
		String methodName = (String) props.get(METHODNAME);
		Object methodParams = props.get(METHODPARAMS);
		Object at = props.get(AT);
		Object result = props.get(METHODRESULT);
		String ns = getNamespace(methodParams);
		if (ns == null) {
			return;
		}
		List<Map> hookList = getHooks(ns);
		if (hookList == null)
			return;
		for (Map<String, Object> call : hookList) {
			if (at.equals(call.get(AT)) && ns.equals(call.get(StoreDesc.NAMESPACE)) && serviceName.equals(call.get(SERVICENAME)) && methodName.equals(call.get(METHODNAME))) {
				String preCondition = (String) call.get(PRECONDITION);
				if (preCondition != null) {
					boolean isok = isHookPreConditionOk(preCondition, methodParams);
					info("preCondition:" + preCondition + ":" + isok);
					if (!isok)
						return;
				}
				String action = (String) call.get(ACTION);
				Boolean sync = (Boolean) call.get(SYNC);
				info("CallServiceImpl.camelAction: service:" + serviceName + ",Method:" + methodName + "/params:" + methodParams);
				try {
					camelHook(ns, getUserName(), serviceName, methodName, action, sync, methodParams, result);
				} catch (Exception e) {
					System.out.println("callHooks:" + e);
					throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "CallRemote:", e);
				}
			}
		}
	}

	@Reference(dynamic = true, optional = true)
	public void setCamelService(CamelService paramCamelService) {
		this.m_camelService = paramCamelService;
		System.out.println("CallServiceImpl.setCamelService:" + paramCamelService);
	}

	@Reference(dynamic = true, optional = true)
	public void setGitService(GitService gitService) {
		System.out.println("CallServiceImpl.setGitService:" + gitService);
		this.m_gitService = gitService;
	}

	@Reference(dynamic = true, optional = true)
	public void setPermissionService(PermissionService paramPermissionService) {
		this.m_permissionService = paramPermissionService;
		System.out.println("CallServiceImpl.setPermissionService:" + paramPermissionService);
	}
}
