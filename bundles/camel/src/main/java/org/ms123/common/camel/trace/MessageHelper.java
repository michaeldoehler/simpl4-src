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
package org.ms123.common.camel.trace;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.xml.transform.Source;
import org.apache.camel.BytesSource;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.MessageHistory;
import org.apache.camel.StreamCache;
import org.apache.camel.StringSource;
import org.apache.camel.WrappedFile;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.StringHelper;
import org.apache.camel.util.URISupport;
import org.apache.camel.util.StopWatch;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

/**
 * Some helper methods when working with {@link org.apache.camel.Message}.
 * 
 * @version
 */
public final class MessageHelper {

	private static final String MESSAGE_HISTORY_HEADER = "%-20s %-20s %-50s %-12s";

	private static final String MESSAGE_HISTORY_OUTPUT = "[%-18.18s] [%-18.18s] [%-48.48s] [%10.10s]";

	/**
     * Utility classes should not have a public constructor.
     */
	private MessageHelper() {
	}

	/**
     * Extracts the given body and returns it as a String, that can be used for
     * logging etc.
     * <p/>
     * Will handle stream based bodies wrapped in StreamCache.
     * 
     * @param message the message with the body
     * @return the body as String, can return <tt>null</null> if no body
     */
	public static String extractBodyAsString(Message message) {
		if (message == null) {
			return null;
		}
		StreamCache newBody = message.getBody(StreamCache.class);
		if (newBody != null) {
			message.setBody(newBody);
		}
		Object answer = message.getBody(String.class);
		if (answer == null) {
			answer = message.getBody();
		}
		if (newBody != null) {
			// Reset the InputStreamCache
			newBody.reset();
		}
		return answer != null ? answer.toString() : null;
	}

	/**
     * Gets the given body class type name as a String.
     * <p/>
     * Will skip java.lang. for the build in Java types.
     * 
     * @param message the message with the body
     * @return the body type name as String, can return
     *         <tt>null</null> if no body
     */
	public static String getBodyTypeName(Message message) {
		if (message == null) {
			return null;
		}
		String answer = ObjectHelper.classCanonicalName(message.getBody());
		if (answer != null && answer.startsWith("java.lang.")) {
			return answer.substring(10);
		}
		return answer;
	}

	/**
     * If the message body contains a {@link StreamCache} instance, reset the
     * cache to enable reading from it again.
     * 
     * @param message the message for which to reset the body
     */
	public static void resetStreamCache(Message message) {
		if (message == null) {
			return;
		}
		Object body = message.getBody();
		if (body != null && body instanceof StreamCache) {
			((StreamCache) body).reset();
		}
	}

	/**
     * Returns the MIME content type on the message or <tt>null</tt> if none
     * defined
     */
	public static String getContentType(Message message) {
		return message.getHeader(Exchange.CONTENT_TYPE, String.class);
	}

	/**
     * Returns the MIME content encoding on the message or <tt>null</tt> if none
     * defined
     */
	public static String getContentEncoding(Message message) {
		return message.getHeader(Exchange.CONTENT_ENCODING, String.class);
	}

	/**
     * Extracts the body for logging purpose.
     * <p/>
     * Will clip the body if its too big for logging. Will prepend the message
     * with <tt>Message: </tt>
     * 
     * @see org.apache.camel.Exchange#LOG_DEBUG_BODY_STREAMS
     * @see org.apache.camel.Exchange#LOG_DEBUG_BODY_MAX_CHARS
     * @param message the message
     * @return the logging message
     */
	public static String extractBodyForLogging(Message message) {
		return extractBodyForLogging(message, "Message: ");
	}

	/**
     * Extracts the body for logging purpose.
     * <p/>
     * Will clip the body if its too big for logging.
     * 
     * @see org.apache.camel.Exchange#LOG_DEBUG_BODY_STREAMS
     * @see org.apache.camel.Exchange#LOG_DEBUG_BODY_MAX_CHARS
     * @param message the message
     * @param prepend a message to prepend
     * @return the logging message
     */
	public static String extractBodyForLogging(Message message, String prepend) {
		boolean streams = false;
		if (message.getExchange() != null) {
			String property = message.getExchange().getContext().getProperty(Exchange.LOG_DEBUG_BODY_STREAMS);
			if (property != null) {
				streams = message.getExchange().getContext().getTypeConverter().convertTo(Boolean.class, message.getExchange(), property);
			}
		}
		// default to 1000 chars
		int maxChars = 1000;
		if (message.getExchange() != null) {
			String property = message.getExchange().getContext().getProperty(Exchange.LOG_DEBUG_BODY_MAX_CHARS);
			if (property != null) {
				maxChars = message.getExchange().getContext().getTypeConverter().convertTo(Integer.class, property);
			}
		}
		return extractBodyForLogging(message, prepend, streams, false, maxChars);
	}

	/**
     * Extracts the body for logging purpose.
     * <p/>
     * Will clip the body if its too big for logging.
     * 
     * @see org.apache.camel.Exchange#LOG_DEBUG_BODY_MAX_CHARS
     * @param message the message
     * @param prepend a message to prepend
     * @param allowStreams whether or not streams is allowed
     * @param allowFiles whether or not files is allowed (currently not in use)
     * @param maxChars limit to maximum number of chars. Use 0 for not limit, and -1 for turning logging message body off.
     * @return the logging message
     */
	public static String extractBodyForLogging(Message message, String prepend, boolean allowStreams, boolean allowFiles, int maxChars) {
		if (maxChars < 0) {
			return prepend + "[Body is not logged]";
		}
		Object obj = message.getBody();
		if (obj == null) {
			return prepend + "[Body is null]";
		}
		if (!allowStreams) {
			if (obj instanceof Source && !(obj instanceof StringSource || obj instanceof BytesSource)) {
				// for Source its only StringSource or BytesSource that is okay as they are memory based
				// all other kinds we should not touch the body
				return prepend + "[Body is instance of java.xml.transform.Source]";
			} else if (obj instanceof StreamCache) {
				return prepend + "[Body is instance of org.apache.camel.StreamCache]";
			} else if (obj instanceof InputStream) {
				return prepend + "[Body is instance of java.io.InputStream]";
			} else if (obj instanceof OutputStream) {
				return prepend + "[Body is instance of java.io.OutputStream]";
			} else if (obj instanceof Reader) {
				return prepend + "[Body is instance of java.io.Reader]";
			} else if (obj instanceof Writer) {
				return prepend + "[Body is instance of java.io.Writer]";
			} else if (obj instanceof WrappedFile || obj instanceof File) {
				return prepend + "[Body is file based: " + obj + "]";
			}
		}
		if (!allowFiles) {
			if (obj instanceof WrappedFile || obj instanceof File) {
				return prepend + "[Body is file based: " + obj + "]";
			}
		}
		// is the body a stream cache
		StreamCache cache;
		if (obj instanceof StreamCache) {
			cache = (StreamCache) obj;
		} else {
			cache = null;
		}
		// grab the message body as a string
		String body = null;
		if (message.getExchange() != null) {
			try {
				if (obj instanceof InputStream) {
					//@@@MS Problem with LOG_DEBUG_BODY_STREAMS
					((InputStream) obj).mark(100000);
				}
				body = message.getExchange().getContext().getTypeConverter().convertTo(String.class, message.getExchange(), obj);
				try {
					//@@@MS LOG_DEBUG_BODY_STREAMS
					if (obj instanceof InputStream) {
						((InputStream) obj).reset();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			} catch (Exception e) {
			}
		}
		if (body == null) {
			body = obj.toString();
		}
		// reset stream cache after use
		if (cache != null) {
			cache.reset();
		}
		if (body == null) {
			return prepend + "[Body is null]";
		}
		// clip body if length enabled and the body is too big
		if (maxChars > 0 && body.length() > maxChars) {
			body = body.substring(0, maxChars) + "... [Body clipped after " + maxChars + " chars, total length is " + body.length() + "]";
		}
		return prepend + body;
	}

	/**
     * Copies the headers from the source to the target message.
     * 
     * @param source the source message
     * @param target the target message
     * @param override whether to override existing headers
     */
	public static void copyHeaders(Message source, Message target, boolean override) {
		if (!source.hasHeaders()) {
			return;
		}
		for (Map.Entry<String, Object> entry : source.getHeaders().entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if (target.getHeader(key) == null || override) {
				target.setHeader(key, value);
			}
		}
	}

	/**
     * Dumps the {@link MessageHistory} from the {@link Exchange} in a human readable format.
     *
     * @param exchange           the exchange
     * @param exchangeFormatter  if provided then information about the exchange is included in the dump
     * @param logStackTrace      whether to include a header for the stacktrace, to be added (not included in this dump).
     * @return a human readable message history as a table
     */
	public static String dumpMessageHistoryStacktrace(Exchange exchange, ExchangeFormatter exchangeFormatter, boolean logStackTrace) {
		// must not cause new exceptions so run this in a try catch block
		try {
			return doDumpMessageHistoryStacktrace(exchange, exchangeFormatter, logStackTrace);
		} catch (Exception e) {
			return "";
		}
	}

	@SuppressWarnings("unchecked")
	public static String doDumpMessageHistoryStacktrace(Exchange exchange, ExchangeFormatter exchangeFormatter, boolean logStackTrace) {
		List<MessageHistory> list = exchange.getProperty(Exchange.MESSAGE_HISTORY, List.class);
		if (list == null || list.isEmpty()) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		sb.append("\n");
		sb.append("Message History\n");
		sb.append("---------------------------------------------------------------------------------------------------------\n");
		sb.append(String.format(MESSAGE_HISTORY_HEADER, "RouteId", "ProcessorId", "Processor", "Elapsed (ms)"));
		sb.append("\n");
		// add incoming origin of message on the top
		String routeId = exchange.getFromRouteId();
		String id = routeId;
		String label = "";
		if (exchange.getFromEndpoint() != null) {
			label = URISupport.sanitizeUri(exchange.getFromEndpoint().getEndpointUri());
		}
		long elapsed = 0;
		Date created = exchange.getProperty(Exchange.CREATED_TIMESTAMP, Date.class);
		if (created != null) {
			elapsed = new StopWatch(created).stop();
		}
		sb.append(String.format(MESSAGE_HISTORY_OUTPUT, routeId, id, label, elapsed));
		sb.append("\n");
		// and then each history
		for (MessageHistory history : list) {
			routeId = history.getRouteId();
			id = history.getNode().getId();
			label = history.getNode().getLabel();
			elapsed = history.getElapsed();
			sb.append(String.format(MESSAGE_HISTORY_OUTPUT, routeId, id, label, elapsed));
			sb.append("\n");
		}
		if (exchangeFormatter != null) {
			sb.append("\nExchange\n");
			sb.append("---------------------------------------------------------------------------------------------------------\n");
			sb.append(exchangeFormatter.format(exchange));
			sb.append("\n");
		}
		Exception e = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
		if (e != null && logStackTrace) {
			sb.append("\nStacktrace\n");
			sb.append("---------------------------------------------------------------------------------------------------------\n");
			sb.append(getStackTrace(e));
		}
		return sb.toString();
	}
}
