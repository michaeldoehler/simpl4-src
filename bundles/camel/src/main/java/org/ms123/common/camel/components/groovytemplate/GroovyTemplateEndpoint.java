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
package org.ms123.common.camel.components.groovytemplate;

import java.io.StringWriter;
import java.util.Map;
import java.util.LinkedHashMap;
import org.apache.camel.Component;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Message;
import org.apache.camel.component.ResourceEndpoint;
import org.apache.camel.util.ExchangeHelper;
import groovy.text.GStringTemplateEngine;
import groovy.text.SimpleTemplateEngine;
import groovy.text.XmlTemplateEngine;
import groovy.text.Template;
import groovy.text.TemplateEngine;
import java.security.MessageDigest;

@SuppressWarnings("unchecked") 
public class GroovyTemplateEndpoint extends ResourceEndpoint {

	private Map<String, Template> m_templateCache = new LinkedHashMap();

	private TemplateEngine m_engine = new SimpleTemplateEngine();

	private String m_engineType = "simple";

	public GroovyTemplateEndpoint() {
	}

	public GroovyTemplateEndpoint(String endpointUri, Component component, String resourceUri) {
		super(endpointUri, component, resourceUri);
		info("GroovyTemplateEndpoint:endpointUri:"+endpointUri+"/resourceUri:"+resourceUri);
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
		m_engine = createTemplateEngine(et);
	}

	@Override
	protected void onExchange(Exchange exchange) throws Exception {
		Map<String, Object> variableMap = exchange.getIn().getHeader(GroovyTemplateConstants.GROOVYTEMPLATE_VARIABLE_MAP, Map.class);
		if (variableMap == null) {
			variableMap = ExchangeHelper.createVariableMap(exchange);
		}

		String text = exchange.getIn().getHeader(GroovyTemplateConstants.GROOVYTEMPLATE, String.class);
		if (text != null) {
			exchange.getIn().removeHeader(GroovyTemplateConstants.GROOVYTEMPLATE);
		}
		if( text == null){
			text = exchange.getContext().getTypeConverter().mandatoryConvertTo(String.class, getResourceAsInputStream());
		}

		String key = getMD5OfUTF8(text);
		Template template = m_templateCache.get(key);
		if (template == null) {
			template = m_engine.createTemplate(text);
			m_templateCache.put(key, template);
		}
		info("GroovyTemplate is writing using attributes:" + variableMap);
		String answer = template.make(variableMap).toString();
		// now lets output the results to the exchange
		Message out = exchange.getOut();
		out.setBody(answer);
		out.setHeaders(exchange.getIn().getHeaders());
		out.setAttachments(exchange.getIn().getAttachments());
	}

	private TemplateEngine createTemplateEngine(String type) {
		if ("gstring".equals(type)) {
			return new GStringTemplateEngine();
		}
		if ("xml".equals(type)) {
			try {
				return new XmlTemplateEngine();
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot create XmlTemplateEngine:" + e.getMessage());
			}
		}
		return new SimpleTemplateEngine();
	}

	private static String getMD5OfUTF8(String text) {
		try {
			MessageDigest msgDigest = MessageDigest.getInstance("MD5");
			byte[] mdbytes = msgDigest.digest(text.getBytes("UTF-8"));
			StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < mdbytes.length; i++) {
				String hex = Integer.toHexString(0xff & mdbytes[i]);
				if (hex.length() == 1)
					hexString.append('0');
				hexString.append(hex);
			}
			return hexString.toString();
		} catch (Exception ex) {
			throw new RuntimeException("GroovyTemplateEndpoint.getMD5OfUTF8");
		}
	}

	private void debug(String msg) {
		System.out.println(msg);
		m_logger.debug(msg);
	}

	private void info(String msg) {
		System.out.println(msg);
		m_logger.info(msg);
	}

	private static final org.slf4j.Logger m_logger = org.slf4j.LoggerFactory.getLogger(GroovyTemplateEndpoint.class);
}
