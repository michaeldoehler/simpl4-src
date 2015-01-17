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
package org.ms123.common.jetty;

import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.Writer;
import java.io.File;
import java.io.Reader;
import java.io.StringWriter;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.service.component.ComponentContext;
import aQute.bnd.annotation.metatype.*;
import aQute.bnd.annotation.component.*;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.util.resource.*;
import org.eclipse.jetty.util.*;
import org.eclipse.jetty.server.*;
import javax.servlet.http.*;
import javax.servlet.*;
import org.eclipse.jetty.servlet.ServletHolder;
import java.net.*;
import org.ms123.common.docbook.DocbookService;
import org.ms123.common.git.GitService;
import org.ms123.common.permission.api.PermissionService;
import org.ms123.common.rpc.JsonRpcServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.FilterMapping;
import org.ms123.common.libhelper.Inflector;
import java.lang.reflect.*;
import org.ms123.common.utils.IOUtils;
import flexjson.*;

/** JettyService implementation
 */
@SuppressWarnings("unchecked")
@Component(enabled = true, configurationPolicy = ConfigurationPolicy.optional, immediate = true)
public class JettyServiceImpl implements JettyService, ServiceListener {

	private final String NAMESPACE = "namespace";

	protected Inflector m_inflector = Inflector.getInstance();

	private PermissionService m_permissionService;
	private DocbookService m_docbookService;
	private GitService m_gitService;

//	private MiltonService m_miltonService;

	private JsonRpcServlet m_rpcServlet;

	private BundleContext m_bundleContext;

	private Server m_server;

	private String m_basedir;

	public Map<String,String> FILETYPES = createFiletypeMap();

	private static final String HEADER_IFMODSINCE = "If-Modified-Since";

	private static final String HEADER_LASTMOD = "Last-Modified";

	public JettyServiceImpl() {
	}

	private static Map<String, String> createFiletypeMap() {
		Map<String, String> result = new HashMap<String, String>();
		result.put("js", "text/javascript");
		result.put("js.gz", "text/javascript");
		result.put("css", "text/css");
		result.put("css.gz", "text/css");
		result.put("adoc", "text/x-asciidoc");
		result.put("html", "text/html");
		result.put("html.gz", "text/html");
		result.put("gif", "image/gif");
		result.put("png", "image/png");
		result.put("jpg", "image/jpg");
		result.put("jepg", "image/jpg");
		result.put("woff", "application/x-font-woff");
		result.put("woff.gz", "application/x-font-woff");
		result.put("svg", "image/svg+xml");
		return Collections.unmodifiableMap(result);
	}

	//protected void activate(ComponentContext context) {
	protected void __activate() {
		System.out.println("JettyServiceImpl.activate");
		try {
			Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
			initJetty();
		} catch (Exception e) {
			e.printStackTrace();
		}
		BundleContext bc = m_bundleContext;
		bc.addServiceListener(this);
		try {
			ServiceReference[] sr = bc.getServiceReferences((String)null, "(rpc.prefix=*)");
			for (int i = 0; i < sr.length; i++) {
				String rpc_prefix = (String) sr[i].getProperty("rpc.prefix");
				Object o = bc.getService(sr[i]);
				if (o != null) {
					String[] objectClass = (String[]) sr[i].getProperty("objectClass");
					m_rpcServlet.putServiceMapping(rpc_prefix, objectClass[0]);
					System.out.println("Jetty.ServiceName:" + objectClass[0] + "/rpc_prefix:" + rpc_prefix);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void activate(BundleContext bundleContext, Map<?, ?> props) {
		System.out.println("JettyServiceImpl.activate2");
		m_bundleContext = bundleContext;
		__activate();
	}

	private void initJetty() throws Exception {
		String port = System.getProperty("jetty.port");
		int p = getInt(port, 8075);
		m_server = new Server(p);
		String sh = System.getProperty("workspace");
		m_basedir = new File(sh).getCanonicalFile().getParent();
		ContextHandlerCollection contexts = new ContextHandlerCollection();
		if (new File(sh + "/webapps/guvnor").exists()) {
			WebAppContext webapp = new WebAppContext(contexts, sh + "/webapps/guvnor", "/guvnor");
		}
		// WebAppContext webapp = new WebAppContext(contexts, sh + "/webapps/e", "/e");
		// webapp.setWar(sh+"/webapps/guvnor-webapp-5.2.0.Final.war");
		// webapp.setExtractWAR(true);
		// webapp.setResourceBase(sh + "/webapps/guvnor");
		LoginFilter loginFilter = new LoginFilter(m_permissionService);
		FilterHolder loginFilterHolder = new FilterHolder(loginFilter);
		loginFilterHolder.setName("LoginFilter");
		ServletContextHandler context0 = new ServletContextHandler(contexts, "/", ServletContextHandler.SESSIONS);
		ServletHandler servletHandler = context0.getServletHandler();
		servletHandler.addFilter(loginFilterHolder, createFilterMapping("/*", loginFilterHolder));
		BundleContext bc = m_bundleContext;
		m_rpcServlet = new JsonRpcServlet(bc);
		context0.addServlet(new ServletHolder(m_rpcServlet), "/rpc/*");
		context0.addServlet(new ServletHolder(new DefaultServlet() {


			public String getInitParameter(String name){
				System.out.println("getInitParameter:"+name+"="+super.getInitParameter(name));
				if("resourceBase".equals(name)) return m_basedir;
				if("acceptRanges".equals(name)) return "true";
				if("etags".equals(name)) return "true";
				if("cacheControl".equals(name)) return "max-age=604800";
				return super.getInitParameter(name);
			}

			public void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				String method = req.getMethod().toUpperCase();
				if (method.equals("GET") || method.equals("HEAD")) {
					doGet(req, resp);
				} else {
					unknownRequest(req, resp);
				}
			}

			public void doGet(HttpServletRequest req, HttpServletResponse response) {
				try {
					if( req.getPathInfo().endsWith(".__pdf")){
						super.doGet(req,response);
						return;
					}
					info("Repo Request:"+req.getPathInfo());
					if( req.getPathInfo().startsWith("/repo/")){
						if(!handleRepo(req,response)){
							unknownRequest(req, response);
						}	
					}else{
						boolean handled = handleStatic(req, response);
						if (!handled) {
							unknownRequest(req, response);
						}
					}
				} catch (Exception e) {
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					e.printStackTrace();
				}
			}

			public void doOptions(HttpServletRequest req, HttpServletResponse response) {
				String origin = req.getHeader("Origin");
				debug("doOptions:" + origin);
				if (origin != null) {
					response.setHeader("Access-Control-Allow-Origin", origin);
					response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
					response.setContentType("text/plain");
					response.setHeader("Connection", "Keep-Alive");
					debug("setHeader");
				} else {
					String allow = "OPTIONS, GET, POST, TRACE, PROPFIND, PROPPATCH, MKCOL, COPY, PUT, DELETE, MOVE, LOCK, UNLOCK, BIND, REBIND, UNBIND, VERSION-CONTROL";
					response.setHeader("Allow", allow);
					response.setHeader("DAV", "1,2,3,bind,bind");
				}
			}
		}), "/*");
		contexts.setHandlers(new Handler[] { context0 });
		m_server.setHandler(contexts);
		m_server.start();
		info("initJetty.ok");
	}

	private void unknownRequest(HttpServletRequest request, HttpServletResponse response) {
		info("unknown request:" + request.getPathInfo() + "/" + request.getMethod());
		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
	}

	private boolean handleRepo(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String segs[] = request.getPathInfo().split("/");
		if( segs.length != 4 ){
			//throw new RuntimeException("Bad request");
		}
		String namespace = segs[2];
		String fileName = segs[segs.length-1];
		String ext = getExtension(fileName);
		if( segs.length>4){
			int i = request.getPathInfo().indexOf( segs[3]);
			fileName = request.getPathInfo().substring(i);
			System.out.println("Filename:"+fileName);
		}
		String mime =  FILETYPES.get(ext);
		System.out.println("Mime:"+mime);
		if( mime == null){
			throw new RuntimeException("Unknown Filetype");
		}
		getAsset(namespace,fileName,mime, ext, request, response);
		return true;
	}

	public static String getExtension(String name){
		if( name.lastIndexOf("/")!=-1){
			name = name.substring(name.lastIndexOf("/"));
		}
		String segs[] = name.split("\\.");
		int len = segs.length;
		String ext = segs[len-1];
		if(len < 2 ){
			throw new RuntimeException("Bad filename");
		}
		if( "gz".equals(ext)){
			if(len < 3 ){
				throw new RuntimeException("Bad filename");
			}
			return segs[len-2] + "."+ segs[len-1];
		}
		return segs[len-1];	
	}

	private boolean handleStatic(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String target = request.getPathInfo();
		String namespace = request.getPathInfo().split("/")[1];
		boolean handled = true;
		if (target.startsWith("/robots.txt")){
			return true;
		}
		if (!target.startsWith("/" + namespace + "/")){
			return false;
		}
		target = target.substring(("/" + namespace + "/").length());
		debug("m_basedir:"+m_basedir+"|target:"+target);
		if (target.endsWith("mobile.html")) {
			FileResource fr = new FileResource(new URL("file:" + m_basedir + "/client/mobile.html"));
			response.setContentType("text/html;charset=UTF-8");
			response.addDateHeader("Date", new java.util.Date().getTime());
			response.addDateHeader("Expires", new java.util.Date().getTime() + 1000000000);
			fr.writeTo(response.getOutputStream(), 0, -1);
		}else if (target.endsWith("start.html")) {
			FileResource fr = new FileResource(new URL("file:" + m_basedir + "/client/start.html"));
			response.setContentType("text/html;charset=UTF-8");
			response.addDateHeader("Date", new java.util.Date().getTime());
			response.addDateHeader("Expires", new java.util.Date().getTime() + 1000000000);
			fr.writeTo(response.getOutputStream(), 0, -1);
		}else if (!target.startsWith("surface") && target.endsWith(".html")) {
			int slash = target.indexOf("/");
			if( slash != -1){
				String ns = target.substring(0,slash);	
				String ws = target.substring(slash+1, target.length()-5);	
				debug("ns:"+ns+"|ws:"+ws);
				m_docbookService.website(ns, ws, request, response);
			}else{
				m_docbookService.website("testapp1", "firstweb", request, response);
			}
		}else if ( isAssetRequest(target)) {
			String ns = getNamespace(target);	
			String assetName = getAssetName(target);	
			String ext = getFiletype(assetName);	
			debug("ns:"+ns+"|assetName:"+assetName+"|"+ext);
			m_docbookService.getAsset(ns, assetName, "image/"+ext, request, response);
		} else if (target.endsWith(".html")) {
			target = removeFirstSegmentInCaseWebsite(target);
			FileResource fr = getFileResource2(m_basedir, target);
			response.setContentType("text/html;charset=UTF-8");
			response.addDateHeader("Date", new java.util.Date().getTime());
			response.addDateHeader("Expires", new java.util.Date().getTime() + 1000000000);
			fr.writeTo(response.getOutputStream(), 0, -1);
		} else if (target.endsWith(".html.gz")) {
			target = removeFirstSegmentInCaseWebsite(target);
			FileResource fr = getFileResource2(m_basedir, target);
			response.setContentType("text/html;charset=UTF-8");
			response.setHeader("Content-Encoding","gzip");
			response.addDateHeader("Date", new java.util.Date().getTime());
			response.addDateHeader("Expires", new java.util.Date().getTime() + 1000000000);
			fr.writeTo(response.getOutputStream(), 0, -1);
		} else if (target.endsWith(".css")) {
			target = removeFirstSegmentInCaseWebsite(target);
			FileResource fr = getFileResource2(m_basedir, target);
			response.setContentType("text/css;charset=UTF-8");
			response.addDateHeader("Date", new java.util.Date().getTime());
			response.addDateHeader("Expires", new java.util.Date().getTime() + 1000000000);
			fr.writeTo(response.getOutputStream(), 0, -1);
		} else if (target.endsWith(".css.gz")) {
			target = removeFirstSegmentInCaseWebsite(target);
			FileResource fr = getFileResource2(m_basedir, target);
			response.setContentType("text/css;charset=UTF-8");
			response.setHeader("Content-Encoding","gzip");
			response.addDateHeader("Date", new java.util.Date().getTime());
			response.addDateHeader("Expires", new java.util.Date().getTime() + 1000000000);
			fr.writeTo(response.getOutputStream(), 0, -1);
		} else if (target.endsWith(".js.gz")) {
			target = removeFirstSegmentInCaseWebsite(target);
			FileResource fr = getFileResource2(m_basedir, target);
			response.setContentType("text/javascript;charset=UTF-8");
			response.setHeader("Content-Encoding","gzip");
			response.addDateHeader("Date", new java.util.Date().getTime());
			response.addDateHeader("Expires", new java.util.Date().getTime() + 1000000000);
			fr.writeTo(response.getOutputStream(), 0, -1);
		} else if (target.endsWith(".js")) {
			target = removeFirstSegmentInCaseWebsite(target);
			FileResource fr = getFileResource2(m_basedir, target);
			response.setContentType("text/javascript;charset=UTF-8");
			response.addDateHeader("Date", new java.util.Date().getTime());
			response.addDateHeader("Expires", new java.util.Date().getTime() + 1000000000);
			fr.writeTo(response.getOutputStream(), 0, -1);
		} else if (target.endsWith(".gif")) {
			target = removeFirstSegmentInCaseWebsite(target);
			FileResource fr = getFileResource(m_basedir, target);
			response.setContentType("image/gif;charset=UTF-8");
			response.addDateHeader("Date", new java.util.Date().getTime());
			response.addDateHeader("Expires", new java.util.Date().getTime() + 1000000000);
			fr.writeTo(response.getOutputStream(), 0, -1);
		} else if (target.endsWith(".jpg")) {
			target = removeFirstSegmentInCaseWebsite(target);
			FileResource fr = getFileResource(m_basedir, target);
			response.setContentType("image/jpeg;charset=UTF-8");
			response.addDateHeader("Date", new java.util.Date().getTime());
			response.addDateHeader("Expires", new java.util.Date().getTime() + 1000000000);
			fr.writeTo(response.getOutputStream(), 0, -1);
		} else if (target.endsWith(".png")) {
			target = removeFirstSegmentInCaseWebsite(target);
			FileResource fr = getFileResource(m_basedir, target);
			response.setContentType("image/png;charset=UTF-8");
			response.addDateHeader("Date", new java.util.Date().getTime());
			response.addDateHeader("Expires", new java.util.Date().getTime() + 1000000000);
			fr.writeTo(response.getOutputStream(), 0, -1);
		} else if (target.endsWith(".woff")) {
			target = removeFirstSegmentInCaseWebsite(target);
			FileResource fr = getFileResource(m_basedir, target);
			response.setContentType("application/x-font-woff");
			response.addDateHeader("Date", new java.util.Date().getTime());
			response.addDateHeader("Expires", new java.util.Date().getTime() + 1000000000);
			fr.writeTo(response.getOutputStream(), 0, -1);
		} else if (target.endsWith(".ttf")) {
			target = removeFirstSegmentInCaseWebsite(target);
			FileResource fr = getFileResource(m_basedir, target);
			response.setContentType("application/x-font-ttf");
			response.addDateHeader("Date", new java.util.Date().getTime());
			response.addDateHeader("Expires", new java.util.Date().getTime() + 1000000000);
			fr.writeTo(response.getOutputStream(), 0, -1);
		} else if (target.endsWith(".pdf")) {
			target = removeFirstSegmentInCaseWebsite(target);
			FileResource fr = getFileResource(m_basedir, target);
			if(!isModified(fr, request, response)){
				return true;
			}
			response.setContentLength((int)fr.length());
			response.setContentType("application/pdf;charset=UTF-8");
			response.addDateHeader("Date", new java.util.Date().getTime());
			fr.writeTo(response.getOutputStream(), 0, -1);
		} else if (target.endsWith(".xml")) {
			target = removeFirstSegmentInCaseWebsite(target);
			FileResource fr = getFileResource(m_basedir, target);
			if(!isModified(fr, request, response)){
				return true;
			}
			response.setContentLength((int)fr.length());
			response.setContentType("application/xml;charset=UTF-8");
			response.addDateHeader("Date", new java.util.Date().getTime());
			fr.writeTo(response.getOutputStream(), 0, -1);
		} else if (target.endsWith(".svg")) {
			target = removeFirstSegmentInCaseWebsite(target);
			FileResource fr = getFileResource(m_basedir, target);
			response.setContentType("image/svg+xml;charset=UTF-8");
			response.addDateHeader("Date", new java.util.Date().getTime());
			response.addDateHeader("Expires", new java.util.Date().getTime() + 1000000000);
			fr.writeTo(response.getOutputStream(), 0, -1);
		} else {
			handled = false;
		}
		return handled;
	}

	private String removeFirstSegmentInCaseWebsite(String s){
		debug("removeFirstSegmentInCaseWebsite1:"+s);
		String segs[] = s.split("/");
		if( s.charAt(0) == '/'){
			if( segs.length > 1 && segs[1].equals("website")){
				return s.substring(s.substring(1).indexOf("/")+1);
			}
		}else{
			if( segs.length > 1 && segs[0].equals("website")){
				return s.substring(s.indexOf("/")+1);
			}
		}
		debug("removeFirstSegmentInCaseWebsite2:"+s);
		return s;
	}
	private FileResource getFileResource(String basedir, String target) throws Exception {
		FileResource fr = new FileResource(new URL("file:" + basedir + "/client/" + target));
		if (fr.exists()) {
			return fr;
		}
		fr = new FileResource(new URL("file:" + basedir + "/client/common/build/" + target));
		if (fr.exists()) {
			return fr;
		}
		fr = new FileResource(new URL("file:" + basedir + "/client/website/build/" + target));
		if (fr.exists()) {
			return fr;
		}
		fr = new FileResource(new URL("file:" + basedir + "/client/mobile/build/" + target));
		if (fr.exists()) {
			return fr;
		}
		fr = new FileResource(new URL("file:" + basedir + "/client/" + target));
		debug("getFileResource:"+fr);
		return fr;
	}
	private FileResource getFileResource2(String basedir, String target) throws Exception {
		FileResource fr = new FileResource(new URL("file:" + basedir + "/client/" + target));
		if (fr.exists()) {
			return fr;
		}
		fr = new FileResource(new URL("file:" + basedir + "/client/mobile/build/" + target));
		debug("getFileResource:"+fr);
		return fr;
	}

	private void handleService(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String _meth = request.getMethod().toLowerCase();
		if (!"post".equals(_meth)) {
			throw new RuntimeException("JettyServiceImpl.handleService:only method \"post\" allowed");
		}
		String pathInfo = request.getPathInfo();
		BundleContext bc = m_bundleContext;
		String[] segs = pathInfo.split("/");
		String namespace = segs[1];
		String service = segs[3];
		String method = segs[4];
		int dot = service.lastIndexOf(".");
		String clazzName = null;
		if (dot != -1) {
			String part1 = service.substring(0, dot);
			String part2 = service.substring(dot + 1);
			info("service:" + service);
			clazzName = "org.ms123." + namespace + "." + part1 + "." + m_inflector.upperCamelCase(part2, '-') + "Service";
		} else {
			String s = m_inflector.upperCamelCase(service, '-');
			clazzName = "org.ms123." + namespace + "." + s.toLowerCase() + "." + s + "Service";
		}
		info("=> " + clazzName + "/" + method);
		ServiceReference sr = bc.getServiceReference(clazzName);
		if (sr == null) {
			if (dot != -1) {
				String part1 = service.substring(0, dot);
				String part2 = service.substring(dot + 1);
				clazzName = "org.ms123.common." + part1 + "." + m_inflector.upperCamelCase(part2, '-') + "Service";
			} else {
				String s = m_inflector.upperCamelCase(service, '-');
				clazzName = "org.ms123.common." + s.toLowerCase() + "." + s + "Service";
			}
			info("=> " + clazzName);
			sr = bc.getServiceReference(clazzName);
		}
		if (sr == null) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		String contentType = request.getContentType();
		Map paramMap = null;
		try {
			String postdata = convertStreamToString(request.getInputStream());
			if (postdata != null && postdata.startsWith("{")) {
				info("\tPostdata(" + contentType + "):" + postdata);
				JSONDeserializer ds = new JSONDeserializer();
				paramMap = (Map) ds.deserialize(postdata);
			} else {
				paramMap = new HashMap();
				Map<String, String[]> map = request.getParameterMap();
				Iterator<String> it = map.keySet().iterator();
				while (it.hasNext()) {
					String key = it.next();
					String[] val = map.get(key);
					paramMap.put(key, val.length > 1 ? val : val[0]);
				}
			}
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			info("json-error:" + e);
			return;
		}
		info("\tparamMap:" + paramMap);
		Object o = bc.getService(sr);
		Class[] cargs = new Class[2];
		cargs[0] = Map.class;
		cargs[1] = Map.class;
		if (method.indexOf("-") != -1) {
			method = m_inflector.lowerCamelCase(method, '-');
			info("\tmethodname:" + method);
		}
		Method meth = o.getClass().getDeclaredMethod(method, cargs);
		info("\tDeclaredMethot:" + meth);
		info("\tNamespace:" + namespace);
		Map params = new HashMap();
		Object[] args = new Object[2];
		Map sysMap = new HashMap();
		sysMap.put("username", request.getAttribute("username"));
		sysMap.put(NAMESPACE, namespace);
		sysMap.put("request", request);
		sysMap.put("response", response);
		args[0] = paramMap;
		args[1] = sysMap;
		try {
			Object _ret = meth.invoke(o, args);
			if (_ret != null && !response.isCommitted()) {
				Map retMap = _ret != null ? (Map) _ret : new HashMap();
				boolean pretty = paramMap.get("_prettyPrint") == null ? false : (Boolean) paramMap.get("_prettyPrint");
				JSONSerializer serializer = new JSONSerializer();
				serializer.prettyPrint(pretty);
				Object jsonObject = serializer.deepSerialize(retMap);
				response.setContentType("application/json;charset=UTF-8");
				response.getWriter().print(jsonObject);
				info("<= " + jsonObject);
			}
		} catch (Throwable e) {
			info("doPost:"+ e);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.setContentType("application/json;charset=UTF-8");
			Map retMap = new HashMap();
			while (e.getCause() != null) {
				e = e.getCause();
				info("\t:cause:" + e.getMessage());
			}
			retMap.put("msg", e.toString());
			JSONSerializer serializer = new JSONSerializer();
			Object jsonObject = serializer.deepSerialize(retMap);
			response.getWriter().print(jsonObject);
			info("<= " + jsonObject);
		}
	}

	private FilterMapping createFilterMapping(String pathSpec, FilterHolder filterHolder) {
		FilterMapping filterMapping = new FilterMapping();
		filterMapping.setPathSpec(pathSpec);
		filterMapping.setFilterName(filterHolder.getName());
		return filterMapping;
	}

	public void serviceChanged(ServiceEvent event) {
		String[] objectClass = (String[]) event.getServiceReference().getProperty("objectClass");
		ServiceReference sr = event.getServiceReference();
		String rpc_prefix = (String) sr.getProperty("rpc.prefix");
		if (rpc_prefix == null) {
			info("JettyServiceImpl.serviceChanged is null:"+objectClass+"/"+sr);
			return;
		}
		if (event.getType() == ServiceEvent.REGISTERED) {
			debug("Ex1: Service of type " + objectClass[0] + " registered.rpc_prefix:" + rpc_prefix);
			m_rpcServlet.putServiceMapping(rpc_prefix, objectClass[0]);
		} else if (event.getType() == ServiceEvent.UNREGISTERING) {
			m_rpcServlet.putServiceMapping(rpc_prefix, null);
			debug("Ex1: Service of type " + objectClass[0] + " unregistered.rpc_prefix:" + rpc_prefix);
		} else if (event.getType() == ServiceEvent.MODIFIED) {
			m_rpcServlet.putServiceMapping(rpc_prefix, objectClass[0]);
			debug("Ex1: Service of type " + objectClass[0] + " modified.rpc_prefix:" + rpc_prefix);
		}
	}

	protected void deactivate() throws Exception {
		info("JettyServiceImpl deactivate");
		m_server.stop();
	}

	public void destroy() throws Exception {
		info("deactivate");
		m_server.stop();
	}

	@Reference(dynamic = true)
	public void setPermissionService(PermissionService shiroService) {
		info("shiroService:" + shiroService);
		this.m_permissionService = shiroService;
	}
	@Reference(dynamic = true,optional=true)
	public void setDocbookService(DocbookService dbService) {
		info("dbService:" + dbService);
		this.m_docbookService = dbService;
	}
	@Reference(dynamic = true,optional=true)
	public void setGitService(GitService dbService) {
		info("gitService:" + dbService);
		this.m_gitService = dbService;
	}

/*	@Reference(dynamic = true)
	public void setMiltonService(MiltonService miltonService) {
		info("miltonService:" + miltonService);
		this.m_miltonService = miltonService;
	}*/

	private boolean isAssetRequest(String target){
		try{
			String p[] = target.split("/");
			if( p.length!=2) return false;
			if( p[1].startsWith("image:") || p[1].startsWith("repo")){
				debug("isAssetRequest:true");
				return true;
			}
			return false;
		}catch(Exception e){
			return false;
		}
	}
	private String getFiletype(String asset){
		try{
			int dot = asset.lastIndexOf(".");
			String ext = asset.substring(dot+1);	
			if( "jpeg".equals(ext)) return "jpg";
			return ext;
		}catch(Exception e){
			return "unknown_ext";
		}
	}
	private String getNamespace(String target){
		try{
			int slash = target.indexOf("/");
			return target.substring(0,slash);	
		}catch(Exception e){
			return "unknown_ns";
		}
	}
	private String getAssetName(String target){
		try{
			int slash = target.indexOf("/");
			if( target.substring(slash+1).startsWith("image")){
				return target.substring(slash+1+("image:".length()), target.length());	
			}else if( target.substring(slash+1).startsWith("repo")){
				return target.substring(slash+1+("repo:".length()), target.length());	
			}else{
				return "unknown_ns";
			}
		}catch(Exception e){
			return "unknown_ns";
		}
	}

	private String convertStreamToString(InputStream is) throws IOException {
		if (is != null) {
			Writer writer = new StringWriter();
			char[] buffer = new char[1024];
			try {
				Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
				int n;
				while ((n = reader.read(buffer)) != -1) {
					writer.write(buffer, 0, n);
				}
			} finally {
				is.close();
			}
			return writer.toString();
		} else {
			return "";
		}
	}

	private boolean isModified(FileResource f, HttpServletRequest request, HttpServletResponse response){
		Date sinceDate = new Date(request.getDateHeader("If-Modified-Since"));
		long modTime = f.lastModified( ); 
		if( modTime < sinceDate.getTime() ){
			response.setStatus(304);
			return false;
		}else{
			response.setDateHeader("Last-Modified", modTime + 10000 );
			return true;
		}
	}

	private void getAsset(String namespace, String name, String type, String ext, HttpServletRequest request, HttpServletResponse response) throws Exception {
		File asset=null;
		String contentType = type;
		try{
			if( "image/svg".equals(type)){
				type = "image/svg+xml";
				contentType = "image/svg+xml";
			}
			if( "image/swf".equals(type)){
				contentType = "application/x-shockwave-flash";
			}
			if( "image/pdf".equals(type)){
				contentType = "application/pdf";
			}
			info("getAsset.FileName:"+name+"/contentType:"+type);
			asset = m_gitService.searchFile(namespace, name, type);
		}catch(Exception e){
			e.printStackTrace();
			response.setStatus(404);
			return;
		}
		
		String rpc = request.getParameter("rpc");
		if( rpc == null){
			Date sinceDate = new Date(request.getDateHeader("If-Modified-Since")+1000);
			long modTime = asset.lastModified( ); 
			if( modTime < sinceDate.getTime() ){
				response.setStatus(304);
				return;
			}else{
				if( name.endsWith(".gz")){
					response.setHeader("Content-Encoding","gzip");
				}
				response.setDateHeader("Last-Modified", modTime + 10000 );
				response.setStatus(HttpServletResponse.SC_OK);
				if( "adoc".equals(ext)){
					response.setContentType( "text/html" );
					Writer w  = response.getWriter();
					m_docbookService.adocToHtml(asset, w );
					w.close();
				}else{
					response.setContentType( contentType );
					response.setContentLength( (int)asset.length() );
					OutputStream os = response.getOutputStream();
					IOUtils.copy( new FileInputStream(asset), os );
					os.close();
				}
			}
		}else{
			final Map<String, Object> rpcMap = m_rpcServlet.extractRequestMap(rpc);
			rpcMap.put("_ASSET", asset);
			String result = m_rpcServlet.handleRPC(request,rpcMap,response);
			if (!response.isCommitted()) {
				String origin = request.getHeader("Origin");
				if (origin != null) {
					response.setHeader("Access-Control-Allow-Origin", origin);
				}
				response.setContentType(JsonRpcServlet.DOPOST_RESPONSE_CONTENTTYPE);
				try {
					response.getWriter().write(result);
				} catch (IOException e) {
					throw new ServletException("Cannot write response", e);
				}
			}
		}
	}

	private int getInt(String s, int def) {
		try {
			return Integer.parseInt(s);
		} catch (Exception e) {
		}
		return def;
	}

	protected void debug(String msg) {
		System.out.println(msg);
		m_logger.debug(msg);
	}
	protected void info(String msg) {
		System.out.println(msg);
		m_logger.info(msg);
	}
	private static final org.slf4j.Logger m_logger = org.slf4j.LoggerFactory.getLogger(JettyServiceImpl.class);
}
