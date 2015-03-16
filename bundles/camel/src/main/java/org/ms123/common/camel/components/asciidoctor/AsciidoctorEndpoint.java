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
package org.ms123.common.camel.components.asciidoctor;

import java.io.StringWriter;
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
import org.ms123.common.docbook.DocbookService;

@SuppressWarnings("unchecked")
public class AsciidoctorEndpoint extends ResourceEndpoint {


	public AsciidoctorEndpoint() {
	}

	public AsciidoctorEndpoint(String endpointUri, Component component, String resourceUri) {
		super(endpointUri, component, resourceUri);
		info("AsciidoctorEndpoint:endpointUri:" + endpointUri + "/resourceUri:" + resourceUri);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public ExchangePattern getExchangePattern() {
		return ExchangePattern.InOut;
	}

	@Override
	protected void onExchange(Exchange exchange) throws Exception {
		String text = exchange.getIn().getHeader(AsciidoctorConstants.ASCIIDOCTOR_SRC, String.class);
		if (text != null) {
			exchange.getIn().removeHeader(AsciidoctorConstants.ASCIIDOCTOR_SRC);
		}
		if( text == null){
			text = exchange.getIn().getBody(String.class);
		}

		DocbookService ds = getDocbookService();
		String html = ds.adocToHtml( text);
		Message mout = exchange.getOut();
		mout.setBody(html);
		mout.setHeaders(exchange.getIn().getHeaders());
		mout.setAttachments(exchange.getIn().getAttachments());
	}

	public DocbookService getDocbookService() {
		return getByType(DocbookService.class);
	}

	private <T> T getByType(Class<T> kls) {
		return kls.cast(getCamelContext().getRegistry().lookupByName(kls.getName()));
	}

	private void debug(String msg) {
		System.out.println(msg);
		m_logger.debug(msg);
	}

	private void info(String msg) {
		System.out.println(msg);
		m_logger.info(msg);
	}

	private static final org.slf4j.Logger m_logger = org.slf4j.LoggerFactory.getLogger(AsciidoctorEndpoint.class);
}
