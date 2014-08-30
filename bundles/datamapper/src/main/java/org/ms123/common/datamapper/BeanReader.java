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
package org.ms123.common.datamapper;

import org.milyn.xml.*;
import org.milyn.cdr.SmooksConfigurationException;
import org.milyn.container.ExecutionContext;
import org.milyn.delivery.VisitorAppender;
import org.milyn.delivery.VisitorConfigMap;
import org.milyn.delivery.annotation.Initialize;
import org.milyn.delivery.java.JavaXMLReader;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import net.sf.sojo.common.ObjectGraphWalker;
import flexjson.*;

public class BeanReader implements JavaXMLReader, VisitorAppender {

	private ExecutionContext m_executionContext;
	private JSONSerializer m_js = new JSONSerializer();

	private List<Object> m_sourceObjects;

	private Boolean m_withNullValues;
	private String m_rootTag;

	private ContentHandler m_contentHandler;

	public void setContentHandler(ContentHandler contentHandler) {
		this.m_contentHandler = contentHandler;
	}

	public ContentHandler getContentHandler() {
		return m_contentHandler;
	}

	public void setSourceObjects(List<Object> sourceObjects) throws SmooksConfigurationException {
		m_sourceObjects = sourceObjects;
	}
	
	public void parse(InputSource src) throws IOException, SAXException {
		m_js.prettyPrint(true);
		m_contentHandler.startDocument();
		ObjectGraphWalker walker = new ObjectGraphWalker();
		walker.setIgnoreNullValues(!m_withNullValues);
		BeanWalker interceptor = new BeanWalker();
		interceptor.setContentHandler(m_contentHandler);
		walker.addInterceptor(interceptor);
		interceptor.setRootTag(m_rootTag);
		walker.walk(m_sourceObjects.get(0));
		m_contentHandler.endDocument();
	}

	public void setExecutionContext(ExecutionContext request) {
		this.m_executionContext = request;
		m_rootTag = (String) m_executionContext.getAttribute("rootTag");
		m_withNullValues = true;
	}

	@Initialize
	public void initialize() {
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
