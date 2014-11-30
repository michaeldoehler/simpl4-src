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

import java.io.Serializable;
import java.util.Date;
import java.util.*;
import java.net.*;
import com.google.gson.*;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.RouteNode;
import org.apache.camel.model.FromDefinition;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.spi.TracedRouteNodes;
import org.apache.camel.util.MessageHelper;
import flexjson.*;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

/**
 * Default {@link TraceEventMessage}.
 */
public final class TraceEventMessage implements Serializable, org.apache.camel.processor.interceptor.TraceEventMessage {

	private static final long serialVersionUID = -4549012920528941203L;

	protected JSONSerializer m_js = new JSONSerializer();

	private Gson m_gson = new GsonBuilder().setExclusionStrategies(new MyExclusionStrategy(String.class)).setPrettyPrinting().create();

	private Date timestamp;

	private String fromEndpointUri;

	private String previousNode;

	private String toNode;

	private String exchangeId;

	private String shortExchangeId;

	private String exchangePattern;

	private String properties;

	private String headers;

	private String body;

	private String bodyType;

	private String outHeaders;

	private String outBody;

	private String outBodyType;

	private String causedByException;

	private String routeId;
	private String resourceId;
	private String resourceIdTo;

	private final transient Exchange tracedExchange;

	/**
     * Creates a {@link TraceEventMessage} based on the given node it was traced while processing
     * the current {@link Exchange}
     *
     * @param toNode the node where this trace is intercepted
     * @param exchange the current {@link Exchange}
     */
	public TraceEventMessage(final Date timestamp, final ProcessorDefinition<?> toNode, final Exchange exchange) {
		m_js.prettyPrint(true);
		this.tracedExchange = exchange;
		Message in = exchange.getIn();
		// need to use defensive copies to avoid Exchange altering after the point of interception
		this.timestamp = timestamp;
		this.fromEndpointUri = exchange.getFromEndpoint() != null ? exchange.getFromEndpoint().getEndpointUri() : null;
		this.previousNode = extractFromNode(exchange);
		this.toNode = extractToNode(exchange);
		this.exchangeId = exchange.getExchangeId();
		this.routeId = exchange.getFromRouteId();
		this.shortExchangeId = extractShortExchangeId(exchange);
		this.exchangePattern = exchange.getPattern().toString();
		this.properties = exchange.getProperties().isEmpty() ? null : toJsonString(exchange.getProperties());
		this.headers = in.getHeaders().isEmpty() ? null : toJsonString(in.getHeaders());
		this.body = MessageHelper.extractBodyAsString(in);
		this.bodyType = MessageHelper.getBodyTypeName(in);
		if (exchange.hasOut()) {
			Message out = exchange.getOut();
			this.outHeaders = out.getHeaders().isEmpty() ? null : toJsonString(out.getHeaders());
			this.outBody = MessageHelper.extractBodyAsString(out);
			this.outBodyType = MessageHelper.getBodyTypeName(out);
		}
		this.causedByException = extractCausedByException(exchange);
		this.resourceId = extractFromId(exchange);
		if( this.resourceId == null){
			this.resourceId = getResourceIdFromRouteDefinition(exchange, this.routeId);
		}
		this.resourceIdTo = extractToId(exchange);
	}

	// Implementation
	//---------------------------------------------------------------
	private static String extractCausedByException(Exchange exchange) {
		Throwable cause = exchange.getException();
		if (cause == null) {
			cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class);
		}
		if (cause != null) {
			return getStackTrace(cause);
		} else {
			return null;
		}
	}

	private String getResourceIdFromRouteDefinition(Exchange exchange, String routeId){
		List<FromDefinition> inputs = exchange.getContext().getRouteDefinition(routeId).getInputs();
		if( inputs.size()==0){
			return "ROUTID("+routeId+") has no \"from\"";
		}
		FromDefinition from = 	inputs.get(0);
		return from.getId();
	}

	private static String extractShortExchangeId(Exchange exchange) {
		return exchange.getExchangeId().substring(exchange.getExchangeId().indexOf("/") + 1);
	}

	private static String extractFromNode(Exchange exchange) {
		if (exchange.getUnitOfWork() != null) {
			TracedRouteNodes traced = exchange.getUnitOfWork().getTracedRouteNodes();
			RouteNode last = traced.getSecondLastNode();
			return last != null ? decode(last.getLabel(exchange)) : null;
		}
		return null;
	}

	private static String extractToNode(Exchange exchange) {
		if (exchange.getUnitOfWork() != null) {
			TracedRouteNodes traced = exchange.getUnitOfWork().getTracedRouteNodes();
			RouteNode last = traced.getLastNode();
			return last != null ? decode(last.getLabel(exchange)) : null;
		}
		return null;
	}

	private static String extractFromId(Exchange exchange) {
		try{
			if (exchange.getUnitOfWork() != null) {
				TracedRouteNodes traced = exchange.getUnitOfWork().getTracedRouteNodes();
				RouteNode last = traced.getSecondLastNode();
				return last != null ? last.getProcessorDefinition().getId() : null;
			}
		}catch(Throwable t){
		}
		return null;
	}

	private static String extractToId(Exchange exchange) {
		if (exchange.getUnitOfWork() != null) {
			TracedRouteNodes traced = exchange.getUnitOfWork().getTracedRouteNodes();
			RouteNode last = traced.getLastNode();
			try{
				return last != null ? last.getProcessorDefinition().getId() : null;
			}catch(Exception e){
			}
		}
		return null;
	}


	private static String decode(String uri){
		try{
			return URLDecoder.decode(uri.replace("+", "%2B"), "UTF-8").replace("%2B", "+");
		}catch(Exception e){
			return uri;
		}
	}
	private String toJsonString(Object o) {
		try{
			return m_gson.toJson(o);
		}catch(Exception e){
		}
		return m_js.serialize(o);
	}

	// Properties
	//---------------------------------------------------------------

	public String getResourceId() {
		return resourceId;
	}
	public String getResourceIdTo() {
		return resourceIdTo;
	}
	public Date getTimestamp() {
		return timestamp;
	}

	public String getFromEndpointUri() {
		return fromEndpointUri;
	}

	public String getPreviousNode() {
		return previousNode;
	}

	public String getToNode() {
		return toNode;
	}

	public String getExchangeId() {
		return exchangeId;
	}

	public String getRouteId() {
		return routeId;
	}

	public String getShortExchangeId() {
		return shortExchangeId;
	}

	public String getExchangePattern() {
		return exchangePattern;
	}

	public String getProperties() {
		return properties;
	}

	public String getHeaders() {
		return headers;
	}

	public String getBody() {
		return body;
	}

	public String getBodyType() {
		return bodyType;
	}

	public String getOutBody() {
		return outBody;
	}

	public String getOutBodyType() {
		return outBodyType;
	}

	public String getOutHeaders() {
		return outHeaders;
	}

	public String getCausedByException() {
		return causedByException;
	}

	public void setResourceId(String resid) {
		this.resourceId = resid;
	}

	public void setResourceIdTo(String resid) {
		this.resourceIdTo = resid;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public void setFromEndpointUri(String fromEndpointUri) {
		this.fromEndpointUri = fromEndpointUri;
	}

	public void setPreviousNode(String previousNode) {
		this.previousNode = previousNode;
	}

	public void setToNode(String toNode) {
		this.toNode = toNode;
	}

	public void setExchangeId(String exchangeId) {
		this.exchangeId = exchangeId;
	}

	public void setRouteId(String routeId) {
		this.routeId = routeId;
	}

	public void setShortExchangeId(String shortExchangeId) {
		this.shortExchangeId = shortExchangeId;
	}

	public void setExchangePattern(String exchangePattern) {
		this.exchangePattern = exchangePattern;
	}

	public void setProperties(String properties) {
		this.properties = properties;
	}

	public void setHeaders(String headers) {
		this.headers = headers;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public void setBodyType(String bodyType) {
		this.bodyType = bodyType;
	}

	public void setOutBody(String outBody) {
		this.outBody = outBody;
	}

	public void setOutBodyType(String outBodyType) {
		this.outBodyType = outBodyType;
	}

	public void setOutHeaders(String outHeaders) {
		this.outHeaders = outHeaders;
	}

	public void setCausedByException(String causedByException) {
		this.causedByException = causedByException;
	}

	public Exchange getTracedExchange() {
		return tracedExchange;
	}

	@Override
	public String toString() {
		return "TraceEventMessage[" + exchangeId + "] on node: " + toNode;
	}

	private static class MyExclusionStrategy implements ExclusionStrategy {

		private final Class<?> typeToSkip;

		private MyExclusionStrategy(Class<?> typeToSkip) {
			this.typeToSkip = typeToSkip;
		}

		public boolean shouldSkipClass(Class<?> clazz) {
			if (clazz.getName().startsWith("org.apache.camel.com")){
				return true;
			}
			if (clazz.getName().startsWith("org.apache.camel.impl.DefaultMessageHistory")){
				return true;
			}
			if (clazz.getName().startsWith("java.io.Fil")){
				return true;
			}
			if (clazz.getName().startsWith("java.util.concurrent.Callable")){
				return true;
			}
			if (clazz.toString().startsWith("interface")){
				return true;
			}
			
			if( Throwable.class.isAssignableFrom(clazz)){
				return true;
			}
//			System.out.println("shouldSkipClass:"+clazz);
			return false;
		}

		public boolean shouldSkipField(FieldAttributes f) {
			//System.out.println("shouldSkipField:" + f);
			return false;
		}
	}
}
