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
import org.ms123.common.store.StoreDesc;
import org.ms123.common.datamapper.DatamapperService;
import org.ms123.common.permission.api.PermissionService;
import org.ms123.common.camel.api.CamelService;
import org.ms123.common.system.LogService;
import org.ms123.common.utils.Inflector;
import groovy.lang.GroovyShell;
import groovy.lang.Binding;

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
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "CamelServiceImpl.createRoutesFromShape:"+msg);
		}
	}

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
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "CamelServiceImpl.saveRouteShape:"+msg);
		}
	}
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
	public List<String> getContextNames(
			@PName(StoreDesc.NAMESPACE) @POptional String namespace ) throws RpcException {
		try {
			return _getContextNames(namespace);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "CamelServiceImpl.getCamelContextList:", e);
		}
	}
	public List<Map> getRouteInfoList(
			@PName("contextKey") String contextKey
			 ) throws RpcException {
		try {
			return _getRouteInfoList(contextKey);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "CamelServiceImpl.getRouteInfoList:", e);
		}
	}

	public List<Map> getRouteInstances(
			@PName("contextKey") String contextKey,
			@PName("routeId") String routeId,
			@PName("startTime") @POptional Long startTime,
			@PName("endTime") @POptional Long endTime
			 ) throws RpcException {
		try {
			return _getRouteInstances(contextKey, routeId, startTime,endTime);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "CamelServiceImpl.getRouteInstances:", e);
		}
	}

	public List<Map> getRouteInstance(
			@PName("contextKey") String contextKey,
			@PName("routeId") String routeId,
			@PName("exchangeId") String exchangeId
			 ) throws RpcException {
		try {
			return _getRouteInstance(contextKey, routeId, exchangeId);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "CamelServiceImpl.getRouteInstance:", e);
		}
	}

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

	private void info(String msg) {
		System.out.println(msg);
		m_logger.info(msg);
	}
	private static final org.slf4j.Logger m_logger = org.slf4j.LoggerFactory.getLogger(CamelServiceImpl.class);
	/*END JSON-RPC-API*/

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
	public void setDatamapperService(DatamapperService paramService) {
		this.m_datamapperService = paramService;
		info("CamelServiceImpl.setDatamapperService:" + paramService);
	}
	@Reference(dynamic = true,optional=true)
	public void setEventAdmin(EventAdmin paramEventAdmin) {
		System.out.println("CamelServiceImpl.setEventAdmin:" + paramEventAdmin);
		this.m_eventAdmin = paramEventAdmin;
	}
	@Reference(dynamic = true,optional=true)
	public void setLogService(LogService param) {
		System.out.println("CamelServiceImpl.setLogService:" + param);
		this.m_logService = param;
	}
}
