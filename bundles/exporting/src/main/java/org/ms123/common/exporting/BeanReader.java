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
package org.ms123.common.exporting;

import org.milyn.xml.*;
import org.milyn.cdr.SmooksConfigurationException;
import org.milyn.cdr.annotation.ConfigParam;
import org.milyn.container.ExecutionContext;
import org.milyn.delivery.VisitorAppender;
import org.milyn.delivery.VisitorConfigMap;
import org.milyn.delivery.annotation.Initialize;
import org.milyn.delivery.dom.DOMVisitAfter;
import org.milyn.delivery.ordering.Consumer;
import org.milyn.delivery.sax.SAXElement;
import org.milyn.delivery.sax.SAXVisitAfter;
import org.milyn.expression.MVELExpressionEvaluator;
import org.milyn.function.StringFunctionExecutor;
import org.milyn.javabean.Bean;
import org.milyn.javabean.context.BeanContext;
import org.milyn.delivery.java.JavaXMLReader;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.helpers.AttributesImpl;
import javax.xml.XMLConstants;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import org.ms123.common.utils.*;
import org.ms123.common.data.api.SessionContext;
import net.sf.sojo.common.*;
import net.sf.sojo.core.*;
import net.sf.sojo.core.conversion.*;
import net.sf.sojo.core.reflect.*;
import net.sf.sojo.navigation.*;

public class BeanReader implements JavaXMLReader, VisitorAppender {

	protected Inflector m_inflector = Inflector.getInstance();

	private SessionContext m_sessionContext;

	private ExecutionContext execContext;

	private List<Object> m_sourceObjects;

	private String m_moduleName;

	private Boolean m_withNullValues;

	private ContentHandler m_contentHandler;

	public void setContentHandler(ContentHandler contentHandler) {
		this.m_contentHandler = contentHandler;
	}

	public ContentHandler getContentHandler() {
		return m_contentHandler;
	}

	public void setSourceObjects(List<Object> sourceObjects) throws SmooksConfigurationException {
		System.out.println("sourceObjects:" + sourceObjects);
		m_sourceObjects = sourceObjects;
	}

	public void parse(InputSource src) throws IOException, SAXException {
		System.out.println("parse:" + src);
		m_contentHandler.startDocument();
		ObjectGraphWalker walker = new ObjectGraphWalker();
		ReflectionHelper.addSimpleType(org.datanucleus.store.types.simple.Date.class);
		walker.setIgnoreNullValues(!m_withNullValues);
		SmooksSojoWalkerInterceptor interceptor = new SmooksSojoWalkerInterceptor();
		interceptor.setContentHandler(m_contentHandler);
		interceptor.setSessionContext(m_sessionContext);
		interceptor.setRootTag(m_inflector.getEntityName(m_moduleName));
		walker.addInterceptor(interceptor);
		walker.walk((List) (m_sourceObjects.get(0)));
		m_contentHandler.endDocument();
	}

	public void setExecutionContext(ExecutionContext request) {
		this.execContext = request;
		m_sessionContext = (SessionContext) execContext.getAttribute("sessionContext");
		m_moduleName = (String) execContext.getAttribute("moduleName");
		m_withNullValues = (Boolean) execContext.getAttribute("withNullValues");
		System.out.println("setExecutionContext:" + m_sessionContext);
	}

	@Initialize
	public void initialize() {
		System.out.println("initialize");
	}

	public void addVisitors(VisitorConfigMap visitorMap) {
	}

	/****************************************************************************
	 *
	 * The following methods are currently unimplemnted...
	 *
	 ****************************************************************************/
	public void parse(String systemId) throws IOException, SAXException {
		throw new UnsupportedOperationException("Operation not supports by this reader.");
	}

	public boolean getFeature(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
		return false;
	}

	public void setFeature(String name, boolean value) throws SAXNotRecognizedException, SAXNotSupportedException {
	}

	public DTDHandler getDTDHandler() {
		return null;
	}

	public void setDTDHandler(DTDHandler arg0) {
	}

	public EntityResolver getEntityResolver() {
		return null;
	}

	public void setEntityResolver(EntityResolver arg0) {
	}

	public ErrorHandler getErrorHandler() {
		return null;
	}

	public void setErrorHandler(ErrorHandler arg0) {
	}

	public Object getProperty(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
		return null;
	}

	public void setProperty(String name, Object value) throws SAXNotRecognizedException, SAXNotSupportedException {
	}
}
