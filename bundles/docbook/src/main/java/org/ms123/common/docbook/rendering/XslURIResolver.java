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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.util.*;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.sax.SAXSource;
import org.xml.sax.InputSource;
import org.osgi.framework.BundleContext;
import org.xml.sax.XMLReader;
import javax.xml.parsers.SAXParserFactory;
import com.ctc.wstx.sax.*;
import com.ctc.wstx.api.ReaderConfig;

public class XslURIResolver implements URIResolver, javax.xml.stream.XMLResolver {

	protected BundleContext m_bundleContect;

	protected SAXParserFactory m_saxParserFactory;

	private static final String defaultXslStylesheetBase = "/xsl/docbook/fo/";

	private static final String defaultXslStylesheetBase2 = "/xsl/docbook/common/";

	private static final String defaultXslStylesheetBase3 = "/xsl/";

	public XslURIResolver(BundleContext bc, SAXParserFactory f) {
		m_bundleContect = bc;
		m_saxParserFactory = f;
	}

	private static final Logger log = LoggerFactory.getLogger(XslURIResolver.class);

	private String docbookXslBase;

	public Source resolve(String href, String base) throws TransformerException {
		System.out.println("[resolve: href=" + href + " for base=" + base + "]");
		if (href == null || href.trim().length() == 0) {
			throw new TransformerException("href is null");
		}
		try {
			String aname = defaultXslStylesheetBase + href;
			if (aname.indexOf("..") != -1) {
				aname = new File(aname).getCanonicalPath().toString();
			}
			if (m_bundleContect.getBundle().getEntry(aname) == null) {
				aname = defaultXslStylesheetBase2 + href;
			}
			if (m_bundleContect.getBundle().getEntry(aname) == null) {
				aname = defaultXslStylesheetBase3 + href;
			}
			System.out.println("\tresolved:" + (aname) + "|" + m_bundleContect.getBundle().getEntry(aname));
			SAXSource ss = new SAXSource(new InputSource(m_bundleContect.getBundle().getEntry(aname).openStream()));
			XMLReader reader = m_saxParserFactory.newSAXParser().getXMLReader();
			ReaderConfig rc = ((WstxSAXParser) reader).getStaxConfig();
			rc.setXMLResolver(new XslURIResolver(m_bundleContect, m_saxParserFactory));
			ss.setXMLReader(reader);
			return ss;
		} catch (Exception e) {
			throw new RuntimeException("XslURIResolver.resolve", e);
		}
	}

	public Object resolveEntity(String publicID, String systemID, String baseURI, String namespace) {
		System.err.println("[resolveEntity: pub->'" + publicID + "', sys->'" + systemID + "', base '" + baseURI + "', ns '" + namespace + "']");
		try {
			String aname = defaultXslStylesheetBase + systemID;
			if (aname.indexOf("..") != -1) {
				aname = new File(aname).getCanonicalPath().toString();
			}
			if (m_bundleContect.getBundle().getEntry(aname) == null) {
				aname = defaultXslStylesheetBase2 + systemID;
			}
			if (m_bundleContect.getBundle().getEntry(aname) == null) {
				aname = defaultXslStylesheetBase3 + systemID;
			}
			System.out.println("\tresolved:" + (aname) + "|" + m_bundleContect.getBundle().getEntry(aname));
			StreamSource ss = new StreamSource(m_bundleContect.getBundle().getEntry(aname).openStream());
			return ss;
		} catch (Exception e) {
			throw new RuntimeException("XslURIResolver.resolveEntity", e);
		}
	}
}
