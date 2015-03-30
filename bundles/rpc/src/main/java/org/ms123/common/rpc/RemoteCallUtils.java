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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.rmi.Remote;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

@SuppressWarnings("unchecked")
public class RemoteCallUtils {

	protected JavaSerializer serializer;

	public RemoteCallUtils(final JavaSerializer javaSerializer) {
		this.serializer = javaSerializer;
	}

	public Remote getServiceInstance(final String serviceClassName, Class requiredClass) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
		if (requiredClass == null) {
			requiredClass = java.rmi.Remote.class;
		}
		final Class serviceClass = Class.forName(serviceClassName);
		if (!requiredClass.isAssignableFrom(serviceClass)) {
			throw new ClassCastException("The requested service class " + serviceClassName + " is not from the required type " + requiredClass.getName());
		}
		return (Remote) serviceClass.newInstance();
	}

	/**
	 * Invokes a method compatible to the specified parameters.
	 *
	 * @param instance          the object on which to invoke the
	 *                          method (must not be
	 *                          <code>null</code>).
	 * @param methodName        the name of the method to invoke (must
	 *                          not be <code>null</code>).
	 * @param originalArguments the method parameters (as JSON
	 *                          objects - must not be
	 *                          <code>null</code>).
	 */
	public Object callCompatibleMethod(final Object instance, String methodName, final Object originalArguments, HttpServletRequest request, HttpServletResponse response) throws InvocationTargetException, IllegalAccessException, RpcException, NoSuchMethodException {
		int argsCount = 0;
		if (originalArguments instanceof List) {
			argsCount = ((List) originalArguments).size();
		}
		final Class clazz = instance.getClass();
		Method methodToCall = null;
		Class[] methodParameterTypes;
		List<Object> convertedArguments = null;
		// Search for the appropriate method among all public methods of the instance
		final Method[] methods = clazz.getMethods();
		boolean goodMethodNameFound = false;
		Class[] badExceptions = null;
		Exception argsConversionException = null;
		String currParameter = null;
		for (final Method candidateMethod : methods) {
			if (!candidateMethod.getName().equals(methodName)) {
				continue;
			}
			goodMethodNameFound = true;
			Class[] exceptions = candidateMethod.getExceptionTypes();
			if (!(exceptions.length == 1 && (exceptions[0] == RpcException.class))) {
				badExceptions = exceptions;
				continue;
			}
			methodParameterTypes = candidateMethod.getParameterTypes();
			if (originalArguments instanceof List) {
				if (methodParameterTypes.length != argsCount) {
					continue;
				}
				// try to convert the serialized arguments
				try {
					convertedArguments = unserializeArguments((List) originalArguments, candidateMethod, 0);
				} catch (UnserializationException e) {
					argsConversionException = e;
					continue;
				} catch (InstantiationException e) {
					argsConversionException = e;
					continue;
				} catch (IllegalAccessException e) {
					argsConversionException = e;
					continue;
				}
				// All tests passed (method name + arguments types), we've found our method!
				methodToCall = candidateMethod;
				break;
			} else {
				currParameter = null;
				try {
					convertedArguments = new ArrayList<Object>();
					if (originalArguments == null) {
						if (methodParameterTypes.length == 0 || allOptional(candidateMethod,methodParameterTypes)) {
							if( methodParameterTypes.length > 0){
								for( int x=0; x< methodParameterTypes.length; x++){
									convertedArguments.add(null);
								}
							}
							methodToCall = candidateMethod;
							break;
						} else {
							continue;
						}
					}
					Map<String, Object> mapArguments = (Map) originalArguments;
					//System.out.println("mapArguments:" + mapArguments + "/method:" + candidateMethod);
					final Class[] argsClasses = candidateMethod.getParameterTypes();
					final Type[] argsTypes = candidateMethod.getGenericParameterTypes();
					int index = 0;
					for (Class<?> cl : methodParameterTypes) {
						if (cl == HttpServletResponse.class) {
							convertedArguments.add(response);
						}else if (cl == HttpServletRequest.class) {
							convertedArguments.add(request);
						} else if (ParameterNameUtil.isMethodAnotatedByPName(candidateMethod)) {
							String value = ParameterNameUtil.getPName(candidateMethod, index).value();
							currParameter = value;
							Object defValue = null;
							if (!mapArguments.containsKey(value)) {
								if (ParameterNameUtil.isOptional(candidateMethod, index)) {
									if (cl == String.class) {
										defValue = ParameterNameUtil.getDefaultString(candidateMethod, index);
									}
									if (cl == Integer.class || cl == Integer.TYPE) {
										defValue = ParameterNameUtil.getDefaultInt(candidateMethod, index);
									}
									if (cl == Long.class || cl == Long.TYPE) {
										defValue = ParameterNameUtil.getDefaultLong(candidateMethod, index);
									}
									if (cl == Boolean.class || cl == Boolean.TYPE) {
										defValue = ParameterNameUtil.getDefaultBool(candidateMethod, index);
									}
									if (cl == Float.class || cl == Float.TYPE || cl == Double.class || cl == Double.TYPE) {
										defValue = ParameterNameUtil.getDefaultFloat(candidateMethod, index);
									}
								} else {
									throw new Exception("Parameter '" + value + "' not found:");
								}
							} else {
								defValue = mapArguments.get(value);
							}
							final Object arg = serializer.unserialize(defValue, argsClasses[index], argsTypes[index]);
							convertedArguments.add(arg);
						} else {
							throw new Exception("No Annotation found:" + candidateMethod);
						}
						index++;
					}
					methodToCall = candidateMethod;
					break;
				} catch (Exception e) {
					argsConversionException = e;
					error("EX:" , e);
				}
			}
		}
		if (methodToCall == null) {
			// No appropriate method has been found
			if (goodMethodNameFound) {
				if (argsConversionException != null) {
					if (currParameter != null) {
						methodName = methodName + "/" + currParameter;
					}
					throw new RpcException(JsonRpcServlet.ERROR_FROM_SERVER, JsonRpcServlet.PARAMETER_MISMATCH, "Bad arguments for method " + methodName, argsConversionException);
				} else if (badExceptions != null) {
					throw new RpcException(JsonRpcServlet.ERROR_FROM_SERVER, JsonRpcServlet.PARAMETER_MISMATCH, "The invoked method " + methodName + " must throw a java.rmi.Exception and only a java.rmi.RemoteException. " + getDeclaredExceptionsMessage(badExceptions));
				} else {
					throw new RpcException(JsonRpcServlet.ERROR_FROM_SERVER, JsonRpcServlet.PARAMETER_MISMATCH, "Bad arguments count (" + (argsCount) + ") for method " + methodName + ".");
				}
			} else {
				throw new NoSuchMethodException(methodName + "(...)");
			}
		}
		// Invoke method
		Object methodResult = null;
		try {
			debugCall(methodToCall, convertedArguments);
			methodResult = methodToCall.invoke(instance, convertedArguments.toArray());
			if( methodToCall.getReturnType().equals(Void.TYPE)){
				return Void.TYPE;
			}
			debugResult(methodResult);
		} catch (IllegalAccessException e) {
			error("ERROR IllegalAccessException while trying to call " + methodToCall.getName() + " on a " + instance.getClass().getName() + " with arguments : (");
			for (final Object arg : convertedArguments) {
				error("   " + arg.getClass().getName() + " : " + arg + ", ");
			}
			error(")");
			trace(e);
			throw e;
		} catch (InvocationTargetException e) {
			if (e.getCause() instanceof RpcException) {
				throw (RpcException) e.getCause();
			} else if (e.getCause() instanceof org.apache.shiro.authz.UnauthorizedException) {
				throw new RpcException( 0,0,e.getMessage(), e.getCause());
			} else {
				// Unwanted exception, like NullPointerException
				// TODO: raise a different type of exception
				trace(e);
				throw e;
			}
		}
		return methodResult;
	}
	private boolean allOptional(Method candidateMethod, Class[] methodParameterTypes){
		int index = 0;
		for (Class<?> cl : methodParameterTypes) {
			if (ParameterNameUtil.isOptional(candidateMethod, index++)) {
			}else{
				info("allOptional:false");
				return false;
			}
		}
		info("allOptional:true");
		return true;
	}

	private void debugResult(Object methodResult) {
		flexjson.JSONSerializer js = new flexjson.JSONSerializer();
		js.prettyPrint(true);
		String s = js.deepSerialize(methodResult);
		if( !m_logger.isDebugEnabled() && s.length() >9192){
			s = s.substring(0,9192) +" .....";
		}
		info(s);
		info("---------------------------------------\n");
	}

	private void debugCall(Method methodToCall, List convertedArguments) {
		info("--------------------------------------------");
		info("==========>>calling:" + methodToCall.getName());
		Class[] mPTypes = methodToCall.getParameterTypes();
		for (int i = 0; i < convertedArguments.size(); i++) {
			String value = null;
			try {
				value = ParameterNameUtil.getPName(methodToCall, i).value();
			} catch (Exception e) {
				value = "NONAME";
			}
			Object arg = convertedArguments.get(i);
			if (mPTypes[i] == java.lang.String.class /*&& arg != null*/) {
				arg = "\"" + arg + "\"";
			}
			if (arg != null)
				info("\t" + value + " : " + arg);
		}
		info("---------------------------------------\n");
	}

	protected List<Object> unserializeArguments(final List<Object> src, Method method, int startIndex) throws UnserializationException, InvocationTargetException, InstantiationException, IllegalAccessException {
		final List<Object> unserializedArgs = new ArrayList<Object>();
		final Class[] argsClasses = method.getParameterTypes();
		final Type[] argsTypes = method.getGenericParameterTypes();
		/* Number of arguments should have been tested before (inside callCompatibleMethod) so we should not worry about that */
		for (int i = startIndex; i < argsClasses.length; i++) {
			info("\targ:" + src.get(i - startIndex) + "/" + argsClasses[i] + "/" + argsTypes[i]);
			final Object arg = serializer.unserialize(src.get(i - startIndex), argsClasses[i], argsTypes[i]);
			unserializedArgs.add(arg);
		}
		return unserializedArgs;
	}

	protected boolean checkExtraArgs(final Method method, final Object... extraArgs) {
		boolean areArgsCompatible = true;
		if (extraArgs != null && extraArgs.length > 0) {
			final Class[] argsClasses = method.getParameterTypes();
			for (int i = 0; i < extraArgs.length; i++) {
				if (extraArgs[i] != null && !(argsClasses[i].isAssignableFrom(extraArgs[i].getClass()))) {
					areArgsCompatible = false;
					break;
				}
			}
		}
		return areArgsCompatible;
	}

	protected String getDeclaredExceptionsMessage(Class[] exceptions) {
		StringBuilder builder = new StringBuilder();
		if (exceptions == null || exceptions.length < 1) {
			builder.append("No exception is thrown by the declared method.");
		} else {
			builder.append("Declared exceptions of the method are: ");
			for (int i = 0; i < exceptions.length - 1; i++) {
				builder.append(exceptions[i]);
				builder.append(", ");
			}
			builder.append(exceptions[exceptions.length - 1]);
			builder.append('.');
		}
		return builder.toString();
	}

	protected void trace(Throwable e) {
		while (e != null) {
			error(e.getClass().getName());
			error(e.getMessage(),e);
			error("");
			e = e.getCause();
			if (e != null) {
				error("CAUSED BY: ");
				trace(e);
			}
		}
	}

	protected void debug(String message) {
		m_logger.debug(message);
		System.out.println(message);
	}
	protected void info(String message) {
		m_logger.info(message);
		System.out.println(message);
	}
	protected void error(String message,Throwable e) {
		m_logger.error(message,e);
		e.printStackTrace();
	}
	protected void error(String message) {
		m_logger.error(message);
		System.out.println(message);
	}
	private static final Logger m_logger = LoggerFactory.getLogger(RemoteCallUtils.class);
}
