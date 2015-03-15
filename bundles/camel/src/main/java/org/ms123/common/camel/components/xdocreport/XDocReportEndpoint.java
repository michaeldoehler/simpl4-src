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
package org.ms123.common.camel.components.xdocreport;

import java.io.StringWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;
import org.apache.camel.Component;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Message;
import org.apache.camel.component.ResourceEndpoint;
import org.apache.camel.util.ExchangeHelper;
import fr.opensagres.xdocreport.core.XDocReportException;
import fr.opensagres.xdocreport.document.IXDocReport;
import fr.opensagres.xdocreport.document.registry.XDocReportRegistry;
import fr.opensagres.xdocreport.template.IContext;
import fr.opensagres.xdocreport.converter.Options;
import fr.opensagres.xdocreport.converter.ConverterTypeTo;
import fr.opensagres.xdocreport.core.document.DocumentKind;
import fr.opensagres.xdocreport.template.TemplateEngineKind;
import fr.opensagres.xdocreport.template.formatter.FieldsMetadata;
import java.security.MessageDigest;

@SuppressWarnings("unchecked")
public class XDocReportEndpoint extends ResourceEndpoint {


	private TemplateEngineKind m_templateEngineKind = TemplateEngineKind.Freemarker;
	private String m_headerFields;
	private String m_outputformat;

	public XDocReportEndpoint() {
	}

	public XDocReportEndpoint(String endpointUri, Component component, String resourceUri) {
		super(endpointUri, component, resourceUri);
		info("XDocReportEndpoint:endpointUri:" + endpointUri + "/resourceUri:" + resourceUri);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public ExchangePattern getExchangePattern() {
		return ExchangePattern.InOut;
	}

	public TemplateEngineKind getTemplateEngineKind() {
		return m_templateEngineKind;
	}
	public void setTemplateEngineKind(TemplateEngineKind t) {
		m_templateEngineKind = t;
	}

	public void setHeaderfields(String t) {
		m_headerFields = t;
	}

	public String getHeaderfields() {
		return m_headerFields;
	}

	public void setOutputformat(String t) {
		m_outputformat = t;
	}

	public String getOutputformat() {
		return m_outputformat;
	}


	@Override
	protected void onExchange(Exchange exchange) throws Exception {
		List<String> headerList=null;	
		if( m_headerFields!=null){
			headerList = Arrays.asList(m_headerFields.split(","));
		}else{
			headerList = new ArrayList();
		}
		Map<String, Object> variableMap = exchange.getIn().getHeader(XDocReportConstants.XDOCREPORT_DATA, Map.class);
		if (variableMap == null) {
			//variableMap = ExchangeHelper.createVariableMap(exchange);
			variableMap = new HashMap();
			for (Map.Entry<String, Object> header : exchange.getIn().getHeaders().entrySet()) {
				if( headerList.size()==0 || headerList.contains( header.getKey())){
					variableMap.put(header.getKey(), header.getValue());
				}
			}
		}
		byte[] bytes = exchange.getIn().getHeader(XDocReportConstants.XDOCREPORT_ODT, byte[].class);
		if (bytes != null) {
			exchange.getIn().removeHeader(XDocReportConstants.XDOCREPORT_ODT);
		}
		if (bytes == null) {
			bytes = exchange.getIn().getBody(byte[].class);
		}
		info("variableMap:"+ variableMap);

		InputStream in = new ByteArrayInputStream(bytes);
		IXDocReport report = XDocReportRegistry.getRegistry().loadReport(in, m_templateEngineKind);
		IContext context = report.createContext();
		context.putMap(variableMap);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		if( "pdf".equals(getOutputformat() )){
			Options options = Options.getTo(ConverterTypeTo.PDF);
			report.convert(context, options, out);
		}else{
			report.process(context, out);
		}
		Message mout = exchange.getOut();
		out.close();
		mout.setBody(out.toByteArray());
		mout.setHeaders(exchange.getIn().getHeaders());
		mout.setAttachments(exchange.getIn().getAttachments());
	}

	private void debug(String msg) {
		System.out.println(msg);
		m_logger.debug(msg);
	}

	private void info(String msg) {
		System.out.println(msg);
		m_logger.info(msg);
	}

	private static final org.slf4j.Logger m_logger = org.slf4j.LoggerFactory.getLogger(XDocReportEndpoint.class);
}
