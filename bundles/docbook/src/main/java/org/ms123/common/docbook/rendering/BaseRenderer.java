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
package org.ms123.common.docbook.rendering;

import org.ms123.common.docbook.rendering.Docbook4JException;
import org.ms123.common.docbook.rendering.ExpressionEvaluatingXMLReader;
import org.ms123.common.docbook.rendering.XslURIResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import com.ctc.wstx.sax.*;
import com.ctc.wstx.api.ReaderConfig;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.*;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.stream.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import org.osgi.framework.BundleContext;
import org.ms123.common.git.GitService;

abstract class BaseRenderer<T extends BaseRenderer<T>> implements Renderer<T> {

	protected BundleContext m_bundleContect;

	protected GitService m_gitService;

	protected String m_namespace;

	private static Templates m_templates;

	private static int m_useConter = 0;

	private static final Logger log = LoggerFactory.getLogger(BaseRenderer.class);

	protected InputStream xmlResource;

	protected Map<String, String> params = new HashMap<String, String>();

	protected Map<String, Object> vars = new HashMap<String, Object>();

	@SuppressWarnings("unchecked")
	public T xml(InputStream xmlResource) {
		this.xmlResource = xmlResource;
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T parameter(String name, String value) {
		this.params.put(name, value);
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T parameters(Map<String, String> parameters) {
		if (parameters != null)
			this.params.putAll(parameters);
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T variable(String name, Object value) {
		this.vars.put(name, value);
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T variables(Map<String, Object> values) {
		if (values != null)
			this.vars.putAll(values);
		return (T) this;
	}

	public void render(OutputStream os) throws Exception {
		assertNotNull(xmlResource, "Value of the xml source should be not null!");
		ByteArrayOutputStream xsltResult = new ByteArrayOutputStream();
		ByteArrayOutputStream headerFooterResult = new ByteArrayOutputStream();
		InputStream xmlInputStream = null;
		try {
			SAXParserFactory factory = createParserFactory();
			final XMLReader reader = factory.newSAXParser().getXMLReader();
			ReaderConfig rc = ((WstxSAXParser) reader).getStaxConfig();
			rc.setXMLResolver(new XslURIResolver(m_bundleContect, factory));
			// prepare xml sax source
			ExpressionEvaluatingXMLReader piReader = new ExpressionEvaluatingXMLReader(reader, vars);
			SAXSource source = new SAXSource(reader, new InputSource(xmlResource));
			// create transofrmer and do transformation
			final Transformer transformer = createTransformer(factory, xmlResource);
			transformer.transform(source, new StreamResult(xsltResult));
			headerFooterProcess(new ByteArrayInputStream(xsltResult.toByteArray()), headerFooterResult);
			// do post processing
			postProcess(xmlInputStream, new ByteArrayInputStream(headerFooterResult.toByteArray()), os);
			xsltResult.close();
		} catch (SAXException e) {
			throw new Docbook4JException("Error transofrming xml!", e);
		} catch (ParserConfigurationException e) {
			throw new Docbook4JException("Error transofrming xml!", e);
		} catch (TransformerException e) {
			throw new Docbook4JException("Error transofrming xml!", e);
		} catch (IOException e) {
			throw new Docbook4JException("Error transofrming xml !", e);
		} finally {
		}
	}

	protected void headerFooterProcess(InputStream xsltResult, OutputStream result) throws Exception {
	}

	protected void postProcess(InputStream xmlSource, InputStream xsltResult, OutputStream fopResult) throws Docbook4JException {
	}

	protected SAXParserFactory createParserFactory() {
		SAXParserFactory factory = SAXParserFactory.newInstance("com.ctc.wstx.sax.WstxSAXParserFactory", null);
		factory.setNamespaceAware(true);
		return factory;
	}

	protected TransformerFactory createTransformerFactory() {
		return TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl", null);
	}

	protected Transformer createTransformer(SAXParserFactory f, InputStream xmlSource) throws TransformerConfigurationException, Exception {
		long startTime = new java.util.Date().getTime();
		if (m_useConter > 50) {
			m_templates = null;
			m_useConter = 0;
		}
		m_useConter++;
		if (m_templates == null) {
			TransformerFactory transformerFactory = createTransformerFactory();
			transformerFactory.setURIResolver(new XslURIResolver(m_bundleContect, f));
			InputStream xsl = getDefaultXslStylesheet();
			Source source = new StreamSource(xsl);
			m_templates = transformerFactory.newTemplates(source);
		}
		long endTime = new java.util.Date().getTime();
		Transformer transformer = m_templates.newTransformer();
		//transformer.setParameter("use.extensions", "1");
		transformer.setParameter("callout.graphics", "0");
		transformer.setParameter("callout.unicode", "1");
		transformer.setParameter("fop1.extensions", "1");
		//@@@MS needs maybe more investigations
		transformer.setParameter("callouts.extension", "1");
		System.out.println("createTransformer:" + params);
		for (Map.Entry<String, String> entry : this.params.entrySet()) {
			transformer.setParameter(entry.getKey(), entry.getValue());
		}
		return transformer;
	}

	protected abstract InputStream getDefaultXslStylesheet() throws Exception;

	protected InputStream resolveXslStylesheet(String location) {
		return null;
	}

	private void assertNotNull(Object value, String message) {
		if (value == null)
			throw new IllegalArgumentException(message);
	}
}
