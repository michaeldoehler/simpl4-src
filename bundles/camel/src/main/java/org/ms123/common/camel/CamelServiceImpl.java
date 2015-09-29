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
package org.ms123.common.camel;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import org.ms123.common.data.api.DataLayer;
import org.ms123.common.auth.api.AuthService;
import org.ms123.common.utils.UtilsService;
import org.ms123.common.data.api.SessionContext;
import org.ms123.common.git.GitService;
import org.ms123.common.wamp.WampService;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.datamapper.DatamapperService;
import org.ms123.common.permission.api.PermissionService;
import org.ms123.common.namespace.NamespaceService;
import org.ms123.common.camel.api.CamelService;
import org.ms123.common.utils.Inflector;
import groovy.lang.GroovyShell;
import groovy.lang.Binding;
import org.apache.camel.Endpoint;
import org.apache.camel.Route;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.util.ExchangeHelper;
import org.apache.camel.ProducerTemplate;

import aQute.bnd.annotation.metatype.*;
import aQute.bnd.annotation.component.*;

import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import org.ms123.common.rpc.PName;
import org.ms123.common.rpc.POptional;
import org.ms123.common.rpc.RpcException;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.apache.commons.io.FilenameUtils.getName;

import static org.ms123.common.rpc.JsonRpcServlet.ERROR_FROM_METHOD;
import static org.ms123.common.rpc.JsonRpcServlet.INTERNAL_SERVER_ERROR;
import static org.ms123.common.rpc.JsonRpcServlet.PERMISSION_DENIED;
import org.apache.shiro.authz.annotation.RequiresRoles;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/** CamelService implementation
 */
@Component(enabled = true, configurationPolicy = ConfigurationPolicy.optional, immediate = true, properties = { "rpc.prefix=camel" })
public class CamelServiceImpl extends BaseCamelServiceImpl implements org.ms123.common.camel.api.CamelService{

	public CamelServiceImpl(){
		info("CamelServiceImpl construct");
	}

	protected void activate(BundleContext bundleContext, Map props) {
		info("CamelServiceImpl.activate.props:" + props);
		try {
			m_bundleContext = bundleContext;
			_createRoutesFromShape();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void deactivate() throws Exception {
		info("CamelServiceImpl deactivate");
	}


	/*BEGIN JSON-RPC-API*/
	@RequiresRoles("admin")
	public void  createRoutesFromShape(
			@PName(StoreDesc.NAMESPACE) @POptional String namespace
		 ) throws RpcException {
		try {
			if( namespace == null){
				_createRoutesFromShape();
			}else{
				_createRoutesFromShape( namespace);
			}
		} catch (Throwable e) {
			String msg = e.getMessage();
			while (e.getCause() != null) {
				e = e.getCause();
				msg += "\n"+e.getMessage();
			}
			e.printStackTrace();
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "CamelServiceImpl.createRoutesFromShape:"+msg);
		}
	}
	@RequiresRoles("admin")
	public List<Map<String,Object>> getProcedureShapesForPrefix( @PName("prefix") String prefix) throws RpcException {
		try {
			return _getProcedureShapesForPrefix(prefix);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "CamelServiceImpl.getProcedureShapesForPrefix:", e);
		}
	}

	@RequiresRoles("admin")
	public void  saveRouteShape(
			@PName(StoreDesc.NAMESPACE) String namespace,
			@PName("path") String path,
			@PName("content") String content
		 ) throws RpcException {
		try {
			m_gitService.putContent(namespace,path, CAMEL_TYPE, content);
			_createRoutesFromShape(namespace,path);
		} catch (Throwable e) {
			String msg = e.getMessage();
			while (e.getCause() != null) {
				e = e.getCause();
				msg += "\n"+e.getMessage();
			}
			e.printStackTrace();
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "CamelServiceImpl.saveRouteShape:"+msg);
		}
	}

	@RequiresRoles("admin")
	public void  saveGroovyScript(
			@PName(StoreDesc.NAMESPACE) String namespace,
			@PName("path") String path,
			@PName("content") String content
		 ) throws RpcException {
		try {
			m_gitService.putContent(namespace,path, GROOVY_TYPE, content);
			_compileGroovyScripts(namespace, path,content);
		} catch (Throwable e) {
			e.printStackTrace();
			String msg = e.getMessage();
			while (e.getCause() != null) {
				e = e.getCause();
				msg += "\n"+e.getMessage();
			}
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "CamelServiceImpl.saveGroovyScript:"+msg);
		}
	}

	@RequiresRoles("admin")
	public void  saveJava(
			@PName(StoreDesc.NAMESPACE) String namespace,
			@PName("path") String path,
			@PName("content") String content
		 ) throws RpcException {
		try {
			m_gitService.putContent(namespace,path, JAVA_TYPE, content);
			_compileJava(namespace, path,content);
		} catch (Throwable e) {
			e.printStackTrace();
			String msg = e.getMessage();
			while (e.getCause() != null) {
				e = e.getCause();
				msg += "\n"+e.getMessage();
			}
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "CamelServiceImpl.saveJava:"+msg);
		}
	}

	@RequiresRoles("admin")
	public List<String> getContextNames(
			@PName(StoreDesc.NAMESPACE) @POptional String namespace ) throws RpcException {
		try {
			return _getContextNames(namespace);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "CamelServiceImpl.getCamelContextList:", e);
		}
	}

	@RequiresRoles("admin")
	public List<Map> getRouteInfoList(
			@PName("contextKey") String contextKey
			 ) throws RpcException {
		try {
			return _getRouteInfoList(contextKey);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "CamelServiceImpl.getRouteInfoList:", e);
		}
	}

	@RequiresRoles("admin")
	public Map<String,List> getRouteVisGraph(
			@PName("contextKey") String contextKey,
			@PName("routeId") String routeId
			 ) throws RpcException {
		try {
			return _getRouteVisGraph(contextKey, routeId);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "CamelServiceImpl.getRouteVisGraph:", e);
		}
	}
	/*END JSON-RPC-API*/

	public Object camelSend(String epUri, final Map<String, Object> properties) {
		return camelSend(epUri, null, null, properties);
	}

	public Object camelSend(String epUri, final Object body, final Map<String, Object> properties) {
		return camelSend(epUri, body, null, properties);
	}

	public Object camelSend(String epUri, final Object body, final Map<String, Object> headers, final Map<String, Object> properties) {
		Processor p = new Processor() {

			public void process(Exchange exchange) {
				if (properties != null) {
					for (String key : properties.keySet()) {
						exchange.setProperty(key, properties.get(key));
					}
				}
				Message in = exchange.getIn();
				if (headers != null) {
					for (String key : headers.keySet()) {
						in.setHeader(key, headers.get(key));
					}
				}
				in.setBody(body);
			}
		};
		ProducerTemplate template = getCamelContext((String) properties.get("namespace"), CamelService.DEFAULT_CONTEXT).createProducerTemplate();
		Exchange result = template.send(epUri, p);
		return ExchangeHelper.extractResultBody(result, null);
	}

	public Object camelSend(String ns, Endpoint endpoint, final Object body, final Map<String, Object> headers, final Map<String, Object> properties) {
		Processor p = new Processor() {

			public void process(Exchange exchange) {
				if (properties != null) {
					for (String key : properties.keySet()) {
						exchange.setProperty(key, properties.get(key));
					}
				}
				Message in = exchange.getIn();
				if (headers != null) {
					for (String key : headers.keySet()) {
						in.setHeader(key, headers.get(key));
					}
				}
				in.setBody(body);
			}
		};
		//String ns = (String) properties.get("namespace");
		ProducerTemplate template = getCamelContext(ns, CamelService.DEFAULT_CONTEXT).createProducerTemplate();
		Exchange result = template.send(endpoint, p);
		return ExchangeHelper.extractResultBody(result, null);
	}

	public Object camelSend(String ns, String routeName,Map<String, Object> properties){
		return camelSend(ns,routeName,null,null,properties);
	}
	public Object camelSend(String ns, String routeName,Object body, Map<String, Object> headers, Map<String, Object> properties){
		Route route = getCamelContext(ns, DEFAULT_CONTEXT).getRoute(routeName);
		if( route == null){
			throw new RuntimeException("CamelServiceImpl:route '"+routeName+"' not found");
		}
		info("Endpoint(Id:"+routeName+"):" + route.getEndpoint());
		Object answer = camelSend(ns, route.getEndpoint(), body, headers, properties);
		return answer;
	}

	public Object camelSend(String ns, Endpoint endpoint, final Object body, final Map<String, Object> headers, final Map<String, Object> properties,String returnSpec, List<String> returnHeaderList) {
		Processor p = new Processor() {

			public void process(Exchange exchange) {
				if (properties != null) {
					for (String key : properties.keySet()) {
						exchange.setProperty(key, properties.get(key));
					}
				}
				Message in = exchange.getIn();
				if (headers != null) {
					for (String key : headers.keySet()) {
						in.setHeader(key, headers.get(key));
					}
				}
				in.setBody(body);
			}
		};
		ProducerTemplate template = getCamelContext(ns, CamelService.DEFAULT_CONTEXT).createProducerTemplate();
		Exchange exchange = template.send(endpoint, p);

		Object camelBody = ExchangeHelper.extractResultBody(exchange, null);
		if( "body".equals(returnSpec) || "bodyWithMime".equals(returnSpec)){
			return ExchangeHelper.extractResultBody(exchange, null);
		}else if( "headers".equals(returnSpec)){
			Map<String, Object> camelVarMap = new HashMap();
			for (Map.Entry<String, Object> header : exchange.getIn().getHeaders().entrySet()) {
				if( returnHeaderList.size()==0 || returnHeaderList.contains( header.getKey())){
					camelVarMap.put(header.getKey(), header.getValue());
				}
			}
			return camelVarMap;
		}else if( "bodyAndHeaders".equals(returnSpec)){
			Map<String, Object> camelVarMap = new HashMap();
			if (camelBody instanceof Map<?, ?>) {
				Map<?, ?> camelBodyMap = (Map<?, ?>) camelBody;
				for (@SuppressWarnings("rawtypes") Map.Entry e : camelBodyMap.entrySet()) {
					if (e.getKey() instanceof String) {
						camelVarMap.put((String) e.getKey(), e.getValue());
					}
				}
			} else {
				camelVarMap.put("body", camelBody);
			}
			for (Map.Entry<String, Object> header : exchange.getIn().getHeaders().entrySet()) {
				if( returnHeaderList.size()==0 || returnHeaderList.contains( header.getKey())){
					camelVarMap.put(header.getKey(), header.getValue());
				}
			}
			return camelVarMap;
		}
		return null;
	}

	@Reference(target = "(kind=jdo)", dynamic = true, optional = true)
	public void setDataLayer(DataLayer dataLayer) {
		info("CamelServiceImpl.setDataLayer:" + dataLayer);
		m_dataLayer = dataLayer;
	}

	@Reference(dynamic = true, optional = false)
	public void setGitService(GitService gitService) {
		info("CamelServiceImpl.setGitService:" + gitService);
		m_gitService = gitService;
	}

	@Reference(dynamic = true, optional = false)
	public void setWampService(WampService wampService) {
		info("CamelServiceImpl.setWampService:" + wampService);
	}

	@Reference(dynamic = true, optional=true)
	public void setPermissionService(PermissionService paramPermissionService) {
		this.m_permissionService = paramPermissionService;
		info("CamelServiceImpl.setPermissionService:" + paramPermissionService);
	}

	@Reference(dynamic = true, optional=true)
	public void setAuthService(AuthService paramService) {
		this.m_authService = paramService;
		info("CamelServiceImpl.setAuthService:" + paramService);
	}

	@Reference(dynamic = true, optional=true)
	public void setUtilsService(UtilsService paramUtilsService) {
		this.m_utilsService = paramUtilsService;
		info("CamelServiceImpl.setUtilsService:" + paramUtilsService);
	}
	@Reference(dynamic = true, optional=true)
	public void setNamespaceService(NamespaceService paramNamespaceService) {
		this.m_namespaceService = paramNamespaceService;
		info("CamelServiceImpl.setNamespaceService:" + paramNamespaceService);
	}

	@Reference(dynamic = true, optional=true)
	public void setDatamapperService(DatamapperService paramService) {
		this.m_datamapperService = paramService;
		info("CamelServiceImpl.setDatamapperService:" + paramService);
	}
	@Reference(dynamic = true,optional=true)
	public void setEventAdmin(EventAdmin paramEventAdmin) {
		System.out.println("CamelServiceImpl.setEventAdmin:" + paramEventAdmin);
		this.m_eventAdmin = paramEventAdmin;
	}
}
