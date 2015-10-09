/*
 Java JSON RPC
 RPC Java POJO by Novlog
 http://www.novlog.com

 This library is dual-licensed under the GNU Lesser General Public License (LGPL) and the Eclipse Public License (EPL).
 Check http://qooxdoo.org/license

 This library is also licensed under the Apache license.
 Check http://www.apache.org/licenses/LICENSE-2.0

 Contribution:
 This contribution is provided by Novlog company.
 http://www.novlog.com
 */
package org.ms123.common.rpc;

import org.json.JSONException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import static org.apache.commons.io.FileUtils.copyFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unchecked")
public class JsonRpcServlet extends HttpServlet {

	private flexjson.JSONSerializer m_js = new flexjson.JSONSerializer();

	private flexjson.JSONDeserializer m_ds = new flexjson.JSONDeserializer();

	private BundleContext m_bundleContext;
	private CallService m_callService;

	private Map<String, String> m_serviceMapping;

	public static final String DOPOST_RESPONSE_CONTENTTYPE = "text/plain; charset=UTF-8";

	private static final String CONTENT_TYPE = "Content-Type";

	private static final String DOPOST_REQUEST_CONTENTTYPE = "application/json";

	private static final String DOGET_RESPONSE_CONTENTTYPE = "text/javascript; charset=UTF-8";

	private static final String SCRIPT_TRANSPORT_ID = "_ScriptTransport_id";

	private static final String SCRIPT_TRANSPORT_DATA = "_ScriptTransport_data";

	private static final int READ_BUFFER_SIZE = 8192;

	/*
	 * Error codes
	 */
	// origin of error
	public static final int ERROR_FROM_SERVER = 1;

	public static final int ERROR_FROM_METHOD = 2;

	// error codes for errors that come from server (origin = 1 = ERROR_FROM_SERVER)
	public static final int ILLEGAL_SERVICE = 1;

	public static final int SERVICE_NOT_FOUND = 2;

	public static final int METHOD_NOT_FOUND = 4;

	public static final int PARAMETER_MISMATCH = 5;

	public static final int PERMISSION_DENIED = 6;

	public static final int INTERNAL_SERVER_ERROR = 500;

	protected static final String ACCESS_DENIED_RESULT = "alert('Access denied. Please make sure that your browser sends correct referer headers.')";

	/**
	 * The referrer checking method used for RPC calls.
	 */
	protected static int referrerCheck;

	protected static final int REFERRER_CHECK_STRICT = 0;

	protected static final int REFERRER_CHECK_DOMAIN = 1;

	protected static final int REFERRER_CHECK_SESSION = 2;

	protected static final int REFERRER_CHECK_PUBLIC = 3;

	protected static final int REFERRER_CHECK_FAIL = 4;

	protected static final String SESSION_REFERRER_KEY = "_qooxdoo_rpc_referrer";

	protected JSONSerializer m_jsonSerializer = null;

	protected JavaSerializer m_javaSerializer = null;

	protected RemoteCallUtils remoteCallUtils = null;

	private static final String UTF_8 = "UTF-8";

	public JsonRpcServlet(BundleContext bc) {
		m_bundleContext = bc;
		m_serviceMapping = new HashMap();
	}

	/**
	 * Initializes this servlet.
	 *
	 * @param config the servlet config.
	 */
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		this.m_jsonSerializer = doGetJsonSerializer();
		this.m_javaSerializer = doGetJavaSerializer();
		this.remoteCallUtils = new RemoteCallUtils(m_javaSerializer);
		final String referrerCheckName = config.getInitParameter("referrerCheck");
		if ("strict".equals(referrerCheckName)) {
			referrerCheck = REFERRER_CHECK_STRICT;
		} else if ("domain".equals(referrerCheckName)) {
			referrerCheck = REFERRER_CHECK_DOMAIN;
		} else if ("session".equals(referrerCheckName)) {
			referrerCheck = REFERRER_CHECK_SESSION;
		} else if ("public".equals(referrerCheckName)) {
			referrerCheck = REFERRER_CHECK_PUBLIC;
		} else if ("fail".equals(referrerCheckName)) {
			referrerCheck = REFERRER_CHECK_FAIL;
		} else {
			referrerCheck = REFERRER_CHECK_PUBLIC;
			// @@@MS
			log("No referrer checking configuration found. Using strict checking as the default.");
		}
	}

	protected JSONSerializer doGetJsonSerializer() {
		return new JSONSerializer();
	}

	protected JavaSerializer doGetJavaSerializer() {
		return new JavaSerializer();
	}

	public void putServiceMapping(String rpc_prefix, String service) {
		m_serviceMapping.put(rpc_prefix, service);
	}

	/**
	 * Possibility to modify here the map produced from the JSON request.
	 * The modification must be done in the same map instance since it's the one that will be used.
	 *
	 * @param request
	 */
	protected void doModifyRequestMap(Map<String, Object> request) {
	}

	/**
	 * Possibility to modify here the result of the invoked method.
	 * The modification must be done in the same instance since it's the one that will be used.
	 *
	 * @param result The result of the invoked method, that has been prepared for JSON serialization
	 *               result can be :
	 *               - a Map
	 *               - a List
	 *               - a String
	 *               - a primitive wrapped into an object (Integer, Boolean, Character, etc.)
	 *               - null
	 *               Only Map and List results should be modified
	 */
	protected void doModifyMethodResult(Object result) {
	}

	public String handleRPC(final HttpServletRequest httpRequest, String requestString, HttpServletResponse response) throws ServletException {
		final Map<String, Object> requestMap = extractRequestMap(requestString);
		return handleRPC(httpRequest,requestMap,response);
	}
	public String handleRPC(final HttpServletRequest httpRequest,  Map<String, Object> requestMap, HttpServletResponse response) throws ServletException {
		Map<String, Object> responseIntermediateObject;
		try {
			beforeCallService(httpRequest, requestMap);
			debug("httpRequest.pathInfo:" + httpRequest.getPathInfo());
			final Object methodResult = executeRequest(httpRequest.getPathInfo(), requestMap, httpRequest, response);
			afterCallService(httpRequest,methodResult, requestMap);
			responseIntermediateObject = buildResponse(requestMap, methodResult, null);
		} catch (RpcException e) {
			responseIntermediateObject = buildResponse(requestMap, e);
		} catch (RemoteException e) {
			responseIntermediateObject = buildResponse(requestMap, e);
		}
		// TODO: what does happen in case of other exception?
		return m_js.deepSerialize(responseIntermediateObject);
	}

	protected final void beforeCallService(final HttpServletRequest httpRequest, final Map<String, Object> requestMap) throws RpcException {
		final String serviceName = (String) requestMap.get("service");
		final String methodName = (String) requestMap.get("method");
		final Object methodParams = requestMap.get("params");
		if (methodParams instanceof Map) {
			doBeforeCallService(httpRequest, serviceName, methodName, (Map) methodParams);
		}
		if (methodParams instanceof List) {
			doBeforeCallService(httpRequest, serviceName, methodName, (List) methodParams);
		}
		callHooks(httpRequest,serviceName, methodName, methodParams,null,true);
	}
	protected final void afterCallService(HttpServletRequest httpRequest, Object result, final Map<String, Object> requestMap) throws RpcException {
		final String serviceName = (String) requestMap.get("service");
		final String methodName = (String) requestMap.get("method");
		final Object methodParams = requestMap.get("params");
		callHooks(httpRequest,serviceName, methodName, methodParams,result, false);
	}

	private void callHooks(HttpServletRequest httpRequest, String serviceName,String methodName, Object methodParams, Object result, boolean before){
		CallService hs = getCallService();
		if( hs != null){
			String[] x = httpRequest.getPathInfo().split("/");
			if (serviceName == null && x.length > 1) {
				serviceName = x[1];
			}
			if (methodName == null && x.length > 2) {
				methodName = x[2];
			}
			Map props = new HashMap();
			props.put("service", serviceName);
			props.put("method", methodName);
			props.put("params", methodParams);
			props.put("result", result);
			props.put("at", before ? "before" : "after");
			hs.callHooks(props);
		}else{
			error("CallServicce not available");
		}
	}
	protected void doBeforeCallService(final HttpServletRequest httpRequest, final String serviceName, final String methodName, final List<Object> methodParams) throws RpcException {
	}

	protected void doBeforeCallService(final HttpServletRequest httpRequest, final String serviceName, final String methodName, final Map<String, Object> methodParams) throws RpcException {
	}

	protected String getRequestString(final HttpServletRequest request) throws Exception {
		String requestString = null;
		InputStream is = null;
		Reader reader = null;
		boolean 	rpcForm = false;
		if( request.getPathInfo().indexOf("__rpcForm__") != -1){
			rpcForm = true;
		}else if( request.getPathInfo().indexOf("/get") != -1){
			rpcForm = true;
		}
		final String contentType = request.getHeader(CONTENT_TYPE);
		boolean isMultipart = ServletFileUpload.isMultipartContent(request);
		debug("getRequestString:isMultipart:"+isMultipart+"/rpcForm:"+rpcForm);
		if (isMultipart) {
			DiskFileItemFactory factory = new DiskFileItemFactory();
			factory.setSizeThreshold(0);
			ServletFileUpload upload = new ServletFileUpload(factory);
			List<FileItem> items = upload.parseRequest(request);
			if (items.size() < 1) {
				throw new RuntimeException("insert:No Uploadfile");
			}
			Map dataMap = new HashMap();
			for (FileItem fi : items) {
				if (fi.isFormField()) {
					String fieldname = fi.getFieldName();
					String fieldvalue = fi.getString();
					debug("ItemValue:" + fieldname + "=" + fieldvalue);
					dataMap.put(fieldname, fieldvalue);
				} else {
					debug("FileItem:" + fi.getFieldName());
					dataMap.put(fi.getFieldName(), unpackFileItem((DiskFileItem) fi));
				}
			}
			debug("dataMap:" + dataMap);
			requestString = (String) dataMap.get("__rpc__");
			if( requestString == null){
				requestString = (String) dataMap.get("rpc");
			}
			debug("rpcParams:" + requestString);
			dataMap.remove("__rpc__");
			dataMap.remove("rpc");
			dataMap.remove("__hints__");
			Map p = (Map) m_ds.deserialize(requestString);
			Map params = (Map) p.get("params");
			params.put("fileMap", dataMap);
			params.put("docid", dataMap.get("docid"));
			flexjson.JSONSerializer js = new flexjson.JSONSerializer();
			requestString = js.deepSerialize(p);
		} else if(rpcForm){
			requestString = request.getParameter("__rpc__");
			if( requestString == null){
				requestString = request.getParameter("rpc");
			}
		} else {
			try {
				is = request.getInputStream();
				reader = new InputStreamReader(is, UTF_8);
				final StringBuilder sb = new StringBuilder(READ_BUFFER_SIZE);
				char[] readBuffer = new char[READ_BUFFER_SIZE];
				int length;
				while ((length = reader.read(readBuffer)) != -1) {
					sb.append(readBuffer, 0, length);
				}
				requestString = sb.toString();
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e) {
					}
				}
				if (is != null) {
					try {
						is.close();
					} catch (IOException e) {
					}
				}
			}
		}
		return requestString;
	}

	private Map unpackFileItem(DiskFileItem fi) {
		Map ret = new HashMap();
		try {
			String storeLocation = fi.getStoreLocation().toString();
			File newFile = File.createTempFile("upload", ".file");
			File origFile =new File(storeLocation);
			//org.ms123.common.utils.FileUtils.copy(new File(storeLocation), newFile);
			copyFile(new File(storeLocation), newFile);
			ret.put("storeLocation", newFile.toString());
			ret.put("fileName", fi.getName());
			origFile.delete();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}

	public Map<String, Object> extractRequestMap(final String requestString) throws ServletException {
		Object requestIntermediateObject;
		try {
			debug("extractRequestMap:" + requestString);
			requestIntermediateObject = m_jsonSerializer.unserialize(requestString);
		} catch (JSONException e) {
			throw new ServletException("Unable to read request", e);
		}
		final Map<String, Object> requestMap = (Map<String, Object>) requestIntermediateObject;
		doModifyRequestMap(requestMap);
		return requestMap;
	}

	protected Object executeRequest(String pathInfo, final Map<String, Object> requestIntermediateObject, HttpServletRequest request,HttpServletResponse response) throws RpcException, RemoteException {
		debug("executeRequest:" + requestIntermediateObject);
		final String serviceName = (String) requestIntermediateObject.get("service");
		final String methodName = (String) requestIntermediateObject.get("method");
		final Object methodParams = requestIntermediateObject.get("params");
		if( CallService.CAMELSERVICENAME.equals(serviceName) ||  CallService.CAMELSERVICENAME2.equals(serviceName)){
			return m_callService.callCamel(methodName, methodParams, request, response);
		}
		return callProcedure(pathInfo, serviceName, methodName, methodParams, request, response);
	}

	protected Object callProcedure(String pathInfo, String service, String method, final Object args, HttpServletRequest request, HttpServletResponse response) throws RpcException, RemoteException {
		Remote serviceInstance;
		debug("callProcedure:" + service + "/method:" + method + "/args:" + args);
		debug("ServiceMapping:" + m_serviceMapping);
		String[] x = pathInfo.split("/");
		if (service == null && x.length > 1) {
			service = x[1];
		}
		if (method == null && x.length > 2) {
			method = x[2];
		}
		String _service = m_serviceMapping.get(service);
		if (_service != null) {
			service = _service;
		}
		ServiceReference sr = m_bundleContext.getServiceReference(service);
		debug("sr:" + sr);
		if (sr == null) {
			throw new RpcException(ERROR_FROM_SERVER, SERVICE_NOT_FOUND, "Service " + service + " not found");
		}
		Object methodResult;
		try {
			Object o = m_bundleContext.getService(sr);
			methodResult = remoteCallUtils.callCompatibleMethod(o, method, args != null ? args : new HashMap(), request, response);
		} catch (NoSuchMethodException e) {
			throw new RpcException(ERROR_FROM_SERVER, METHOD_NOT_FOUND, "Method " + method + " not found", e);
		} catch (IllegalAccessException e) {
			throw new RpcException(ERROR_FROM_SERVER, METHOD_NOT_FOUND, "Method " + method + " not found", e);
		} catch (InvocationTargetException e) {
			final Throwable raisedException = e.getCause();
			throw new RpcException(ERROR_FROM_METHOD, null, "An exception (" + raisedException.getClass() + ") was raised by the call to method " + method + "(...) : " + raisedException.getMessage(), raisedException);
		}
		return methodResult;
	}

	protected Map<String, Object> buildResponse(final Map<String, Object> request, final Object methodResult, final Map<Class, List<String>> wantedFields) {
		Map<String, Object> response;
		try {
			final Object result = m_javaSerializer.serialize(methodResult, wantedFields,0,10);
			doModifyMethodResult(result);
			response = new HashMap<String, Object>(3);
			response.put("id", request.get("id"));
			response.put("error", null);
			response.put("result", methodResult);
		} catch (SerializationException e) {
			response = buildResponse(request, new RpcException(ERROR_FROM_SERVER, INTERNAL_SERVER_ERROR, "Unable to serialize method result.", e));
		}
		return response;
	}

	protected Map<String, Object> buildResponse(final Map<String, Object> request, final RpcException exception) {
		final Map<String, Object> response = new HashMap<String, Object>(3);
		final Map<String, Object> error = new HashMap<String, Object>(6);
		error.put("origin", exception.getOrigin());
		error.put("code", exception.getErrorCode());
		String cmessage = null;
		Throwable cause = exception.getCause();
		if (cause != null) {
			while (cause.getCause() != null) {
				cause = cause.getCause();
			}
			cmessage = cause.toString();
			if (cmessage == null) {
				cmessage = cause.toString();
			}
		}
		error.put("message", constructMessage(exception.getMessage(), cmessage));
		error.put("class", exception.getClass().getName());
		response.put("id", request.get("id"));
		response.put("error", error);
		response.put("result", null);
		exception.printStackTrace();
		return response;
	}

	protected Map<String, Object> buildResponse(final Map<String, Object> request, final RemoteException exception) {
		final Map<String, Object> response = new HashMap<String, Object>(3);
		final Map<String, Object> error = new HashMap<String, Object>(6);
		error.put("origin", ERROR_FROM_METHOD);
		error.put("code", null);
		error.put("message", exception.getMessage());
		error.put("class", exception.getClass().getName());
		final Throwable cause = exception.getCause();
		if (cause != null) {
		}
		response.put("id", request.get("id"));
		response.put("error", error);
		response.put("result", null);
		return response;
	}

	protected Map<String, Object> convertException(Throwable exception) {
		final Map<String, Object> error = new HashMap<String, Object>(4);
		error.put("message", exception.getClass().getName() + ": " + exception.getMessage());
		error.put("origMessage", exception.getMessage());
		error.put("class", exception.getClass().getName());
		final Throwable cause = exception.getCause();
		if (cause != null) {
			error.put("cause", convertException(cause));
		}
		return error;
	}

	private String constructMessage(String m1, String m2) {
		if (m2 == null) {
			return m1;
		}
		if (m1 != null && !m1.endsWith(":")) {
			return m1 + ":" + m2;
		}
		return m1 + m2;
	}

	/**
	 * Checks the referrer based on the configured strategy
	 *
	 * @param request the incoming request.
	 * @return whether or not the request should be granted.
	 */
	protected boolean checkReferrer(HttpServletRequest request) {
		if (referrerCheck == REFERRER_CHECK_PUBLIC) {
			return true;
		}
		if (referrerCheck == REFERRER_CHECK_FAIL) {
			return false;
		}
		String referrer = request.getHeader("Referer");
		debug("Referer:" + referrer);
		if (referrer == null) {
			return false;
		}
		if (referrerCheck == REFERRER_CHECK_STRICT) {
			String contextURL = getContextURL(request);
			return referrer.startsWith(contextURL);
		}
		if (referrerCheck == REFERRER_CHECK_DOMAIN) {
			String domainURL = getDomainURL(request);
			return referrer.startsWith(domainURL);
		}
		if (referrerCheck == REFERRER_CHECK_SESSION) {
			// find the domain part of the referrer
			int colonIndex = referrer.indexOf(":");
			if (colonIndex == -1) {
				return false;
			}
			int referrerLength = referrer.length();
			int i;
			for (i = colonIndex + 1; ; ++i) {
				if (i >= referrerLength) {
					return false;
				}
				if (referrer.charAt(i) != '/') {
					break;
				}
			}
			int slashIndex = referrer.indexOf("/", i + 1);
			if (slashIndex == -1) {
				return false;
			}
			String referrerDomain = referrer.substring(0, slashIndex + 1);
			HttpSession session = request.getSession();
			String oldReferrerDomain = (String) session.getAttribute(SESSION_REFERRER_KEY);
			if (oldReferrerDomain == null) {
				session.setAttribute(SESSION_REFERRER_KEY, referrerDomain);
			} else {
				if (!oldReferrerDomain.equals(referrerDomain)) {
					return false;
				}
			}
			return true;
		}
		throw new IllegalStateException("Internal error: unknown referrer checking configuration");
	}

	protected String getContextURL(HttpServletRequest request) {
		// reconstruct the start of the URL
		StringBuffer contextURL = new StringBuffer();
		String scheme = request.getScheme();
		int port = request.getServerPort();
		contextURL.append(scheme);
		contextURL.append("://");
		contextURL.append(request.getServerName());
		if ((scheme.equals("http") && port != 80) || (scheme.equals("https") && port != 443)) {
			contextURL.append(':');
			contextURL.append(request.getServerPort());
		}
		contextURL.append(request.getContextPath());
		return contextURL.toString();
	}

	protected String getDomainURL(HttpServletRequest request) {
		// reconstruct the start of the URL
		StringBuffer domainURL = new StringBuffer();
		String scheme = request.getScheme();
		int port = request.getServerPort();
		domainURL.append(scheme);
		domainURL.append("://");
		domainURL.append(request.getServerName());
		if ((scheme.equals("http") && port != 80) || (scheme.equals("https") && port != 443)) {
			domainURL.append(':');
			domainURL.append(request.getServerPort());
		}
		domainURL.append('/');
		return domainURL.toString();
	}

	/**
	 * @param request  the servlet request.
	 * @param response the servlet response.
	 * @throws ServletException thrown when executing the method or writing the response fails.
	 */
	@Override
	public void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		debug("RPC.doOptions:" + request);
		String origin = request.getHeader("Origin");
		debug("doOptions:" + origin);
		if (origin != null) {
			response.setHeader("Access-Control-Allow-Origin", origin);
			response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
			response.setHeader("Access-Control-Allow-Headers", "X-PINGOTHER,Authorization,Content-Type");
			//response.setHeader("Access-Control-Max-Age", "1728000");
			response.setHeader("Keep-Alive", "timeout=2,max=100");
			response.setHeader("Connection", "Keep-Alive");
		}
	}

	/**
	 * Remote method execution. The method name and parameters are expected
	 * in JSON format in the request body.
	 *
	 * @param request  the servlet request.
	 * @param response the servlet response.
	 * @throws ServletException thrown when executing the method or writing the response fails.
	 */
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		String result = null;
		String requestString = null;
		try {
			requestString = getRequestString(request);
		} catch (Exception e) {
			throw new ServletException("Cannot read request", e);
		}
		result = handleRPC(request, requestString, response);
		if (!response.isCommitted()) {
			String origin = request.getHeader("Origin");
			if (origin != null) {
				response.setHeader("Access-Control-Allow-Origin", origin);
			}
			response.setContentType(DOPOST_RESPONSE_CONTENTTYPE);
			try {
				final Writer responseWriter = response.getWriter();
				if (request.getParameter("pretty") != null) {
					result = makePretty(result);
				}
				responseWriter.write(result);
			} catch (IOException e) {
				throw new ServletException("Cannot write response", e);
			}
		}
	}

	private String makePretty(String s) {
		m_js.prettyPrint(true);
		return m_js.deepSerialize(m_ds.deserialize(s));
	}

	/**
	 * Returns context path information to the client (as a JavaScript hash
	 * map).
	 *
	 * @param request  the servlet request.
	 * @param response the servlet response.
	 * @throws ServletException thrown when writing the response fails.
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		if( request.getPathInfo().indexOf("__rpcForm__") != -1){
			doPost(request,response);
		}else if( request.getPathInfo().indexOf("/get") != -1){
			doPost(request,response);
		}
	}

	private CallService getCallService(){
		if( m_callService != null ) return m_callService;
	  ServiceReference ref = m_bundleContext.getServiceReference(CallService.class.getName());	
		if( ref != null){
			m_callService = (CallService) m_bundleContext.getService(ref);
		}
		return m_callService;
	}
	protected void debug(String msg) {
		//System.out.println(msg);
		m_logger.debug(msg);
	}
	protected void info(String msg) {
		System.out.println(msg);
		m_logger.info(msg);
	}
	protected void error(String msg) {
		System.out.println(msg);
		m_logger.error(msg);
	}
	private static final org.slf4j.Logger m_logger = org.slf4j.LoggerFactory.getLogger(JsonRpcServlet.class);
}
