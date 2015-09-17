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
package org.ms123.common.camel.components.template;

import java.io.StringWriter;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import org.apache.camel.Component;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Message;
import org.apache.camel.component.ResourceEndpoint;
import org.apache.camel.util.ExchangeHelper;
import java.security.MessageDigest;

@SuppressWarnings("unchecked")
@groovy.transform.CompileStatic
public class TemplateEndpoint extends ResourceEndpoint {

	private Map<String, Engine> m_engineCache = new LinkedHashMap();
	private Engine m_engine;

	private String m_engineType = "groovy";
	private String m_headerFields;

	public TemplateEndpoint() {
	}

	public TemplateEndpoint(String endpointUri, Component component, String resourceUri) {
		super(endpointUri, component, resourceUri);
		info("TemplateEndpoint:endpointUri:"+endpointUri+"/resourceUri:"+resourceUri);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public ExchangePattern getExchangePattern() {
		return ExchangePattern.InOut;
	}

	public String getEngineType() {
		return m_engineType;
	}

	public void setEngineType(String et) {
		m_engineType = et;
		m_engine = m_engineCache.get(et);
		if( m_engine == null){
			if( "groovy".equals(et)){
				m_engine = new GroovyEngine();
				m_engineCache.put("groovy", m_engine);
			}
			if( "freemarker".equals(et)){
				m_engine = new FreemarkerEngine(getCamelContext());
				m_engineCache.put("freemarker", m_engine);
			}
		}
	}

	public void setHeaderfields(String t) {
		m_headerFields = t;
	}

	public String getHeaderfields() {
		return m_headerFields;
	}

	@Override
	protected void onExchange(Exchange exchange) throws Exception {
		List<String> headerList=null;
		if( m_headerFields!=null){
			headerList = Arrays.asList(m_headerFields.split(","));
		}else{
			headerList = new ArrayList();
		}
		Map<String, Object> variableMap = exchange.getIn().getHeader(TemplateConstants.TEMPLATE_DATA, Map.class);
		if (variableMap == null) {
			//variableMap = ExchangeHelper.createVariableMap(exchange);
			variableMap = new HashMap();
			for (Map.Entry<String, Object> header : exchange.getIn().getHeaders().entrySet()) {
				if( headerList.size()==0 || headerList.contains( header.getKey())){
					if( header.getValue() instanceof Map){
						variableMap.putAll((Map)header.getValue());
					}else{
						variableMap.put(header.getKey(), header.getValue());
					}
				}
			}
		}

		String text = exchange.getIn().getBody(String.class);

		String answer = m_engine.convert(text,variableMap);

		Message out = exchange.getOut();
		out.setBody(answer);
		out.setHeaders(exchange.getIn().getHeaders());
		out.setAttachments(exchange.getIn().getAttachments());
	}

	private void debug(String msg) {
		System.out.println(msg);
		m_logger.debug(msg);
	}

	private void info(String msg) {
		System.out.println(msg);
		m_logger.info(msg);
	}
	private static final org.slf4j.Logger m_logger = org.slf4j.LoggerFactory.getLogger(TemplateEndpoint.class);
}

