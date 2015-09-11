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
package org.ms123.common.rpc;

import org.json.JSONException;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.ms123.common.libhelper.Inflector;

@SuppressWarnings("unchecked")
public class JsonRpc {

	protected Inflector m_inflector = Inflector.getInstance();
	private flexjson.JSONSerializer m_js = new flexjson.JSONSerializer();
	private flexjson.JSONDeserializer m_ds = new flexjson.JSONDeserializer();

	/*
	 * Error codes
	 */
	// origin of error
	public static final int ERROR_FROM_SERVER = 1;
	public static final int ERROR_FROM_METHOD = 2;
	public static final int METHOD_NOT_FOUND = 4;
	public static final int PARAMETER_MISMATCH = 5;
	public static final int PERMISSION_DENIED = 6;
	public static final int INTERNAL_SERVER_ERROR = 500;
	private JSONSerializer m_jsonSerializer = null;
	private JavaSerializer m_javaSerializer = null;
	private BundleContext m_bundleContext;
	private RemoteCallUtils m_remoteCallUtils = null;
	private CallService m_callService;

	public JsonRpc(BundleContext bc) {
		m_bundleContext = bc;
		m_jsonSerializer = new JSONSerializer();
		m_javaSerializer = new JavaSerializer();
		m_remoteCallUtils = new RemoteCallUtils(m_javaSerializer);
	}

	public String handleRPC(Object obj, String requestString) throws Exception {
		final Map<String, Object> requestMap = extractRequestMap(requestString);
		return handleRPC(obj, requestMap);
	}

	public Map<String,Object> handleRPC(String serviceAndMethod, String requestString) {
		String serviceName = null;
		String methodName = null;
		String[] x = serviceAndMethod.split("\\.");
		if (x.length > 0) {
			serviceName = x[0];
		}
		if (x.length > 1) {
			methodName = x[1];
		}
		Object service = getService(serviceName);
		Map<String, Object> responseIntermediateObject;
		final Map<String, Object> parameterMap = extractRequestMap(requestString);
		
		final Map<String, Object> requestMap = new HashMap();
		requestMap.put("service", serviceName);
		requestMap.put("method", methodName);
		requestMap.put("params", parameterMap);
		try {
			final Object methodResult = executeRequest(service, requestMap);
			responseIntermediateObject = buildResponse(requestMap, methodResult, null);
		} catch (RpcException e) {
			responseIntermediateObject = buildResponse(requestMap, e);
		}
		return responseIntermediateObject;
	}

	public String handleRPC(Object obj, Map<String, Object> requestMap) throws Exception {
		Map<String, Object> responseIntermediateObject;
		try {
			final Object methodResult = executeRequest(obj, requestMap);
			responseIntermediateObject = buildResponse(requestMap, methodResult, null);
		} catch (RpcException e) {
			responseIntermediateObject = buildResponse(requestMap, e);
		}
		return m_js.deepSerialize(responseIntermediateObject);
	}

	public Map<String, Object> extractRequestMap(final String requestString) {
		Object requestIntermediateObject;
		try {
			debug("extractRequestMap:" + requestString);
			requestIntermediateObject = m_jsonSerializer.unserialize(requestString);
		} catch (JSONException e) {
			throw new RuntimeException("Unable to read request", e);
		}
		final Map<String, Object> requestMap = (Map<String, Object>) requestIntermediateObject;
		return requestMap;
	}

	private Object executeRequest(Object obj, final Map<String, Object> requestIntermediateObject) throws RpcException {
		final String serviceName = (String) requestIntermediateObject.get("service");
		final String methodName = (String) requestIntermediateObject.get("method");
		final Object methodParams = requestIntermediateObject.get("params");
		if (CallService.CAMELSERVICENAME.equals(serviceName) ||  CallService.CAMELSERVICENAME2.equals(serviceName)){
			return getCallService().callCamel(methodName, methodParams, null, null);
		}
		return callProcedure(obj, serviceName, methodName, methodParams);
	}

	private Object callProcedure(Object obj, String service, String method, final Object args) throws RpcException {
		debug("callProcedure:" + service + "/method:" + method + "/args:" + args);
		Object methodResult;
		try {
			methodResult = m_remoteCallUtils.callCompatibleMethod(obj, method, args, null, null);
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

	private Map<String, Object> buildResponse(final Map<String, Object> request, final Object methodResult, final Map<Class, List<String>> wantedFields) {
		Map<String, Object> response;
//		try {
			//final Object result = m_javaSerializer.serialize(methodResult, wantedFields, 0, 10);
			response = new HashMap<String, Object>(3);
			response.put("id", request.get("id"));
			response.put("error", null);
			response.put("result", methodResult);
//		} catch (SerializationException e) {
		//	response = buildResponse(request, new RpcException(ERROR_FROM_SERVER, INTERNAL_SERVER_ERROR, "Unable to serialize method result.", e));
		//}
		return response;
	}

	private Map<String, Object> buildResponse(final Map<String, Object> request, final RpcException exception) {
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

	private String constructMessage(String m1, String m2) {
		if (m2 == null) {
			return m1;
		}
		if (m1 != null && !m1.endsWith(":")) {
			return m1 + ":" + m2;
		}
		return m1 + m2;
	}

	private String makePretty(String s) {
		m_js.prettyPrint(true);
		return m_js.deepSerialize(m_ds.deserialize(s));
	}

	private CallService getCallService() {
		if (m_callService != null)
			return m_callService;
		ServiceReference ref = m_bundleContext.getServiceReference(CallService.class.getName());
		if (ref != null) {
			m_callService = (CallService) m_bundleContext.getService(ref);
		}
		return m_callService;
	}

	private Object getService(String serviceName) {
		String serviceClassName = null;
		int dot = serviceName.lastIndexOf(".");
		if (dot != -1) {
			String part1 = serviceName.substring(0, dot);
			String part2 = serviceName.substring(dot + 1);
			System.out.println("serviceName:" + serviceName);
			serviceClassName = "org.ms123.common." + part1 + "." + m_inflector.upperCamelCase(part2, '-') + "Service";
		} else {
			String s = m_inflector.upperCamelCase(serviceName, '-');
			serviceClassName = "org.ms123.common." + s.toLowerCase() + "." + s + "Service";
		}
		System.out.println("ServiceClassName:" + serviceClassName);
		Object service = null;
		ServiceReference sr = m_bundleContext.getServiceReference(serviceClassName);
		if (sr != null) {
			service = m_bundleContext.getService(sr);
		}
		if (service == null) {
			throw new RuntimeException("JsonRpc.Cannot resolve service:" + serviceClassName);
		}
		return service;
	}

	private void debug(String msg) {
		System.out.println(msg);
		m_logger.debug(msg);
	}

	private void info(String msg) {
		System.out.println(msg);
		m_logger.info(msg);
	}

	private void error(String msg) {
		System.out.println(msg);
		m_logger.error(msg);
	}

	private static final org.slf4j.Logger m_logger = org.slf4j.LoggerFactory.getLogger(JsonRpc.class);
}
