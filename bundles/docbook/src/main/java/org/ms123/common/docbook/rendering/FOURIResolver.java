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
import java.io.*;
import java.util.*;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.sax.SAXSource;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import javax.xml.parsers.SAXParserFactory;
import com.ctc.wstx.sax.*;
import com.ctc.wstx.api.ReaderConfig;
import org.ms123.common.git.GitService;

public class FOURIResolver implements URIResolver {

	protected GitService m_gitService;

	protected String m_namespace;

	public FOURIResolver(GitService gs, String namespace) {
		m_gitService = gs;
		m_namespace = namespace;
	}

	private static final Logger log = LoggerFactory.getLogger(FOURIResolver.class);

	private String docbookXslBase;

	public Source resolve(String href, String base) throws TransformerException {
		System.out.println("[FOURIResolver.resolve: href=" + href + " for base=" + base + "]");
		String tmp = href.toLowerCase();
		if (tmp.startsWith("repo:")) {
			String file = href.substring(5);
			String type = null;
			if (tmp.endsWith(".svg")) {
				type = "image/svg+xml";
			}
			if (tmp.endsWith(".png")) {
				type = "image/png";
			}
			if (tmp.endsWith(".jpg")) {
				type = "image/jpg";
			}
			if (tmp.endsWith(".jepg")) {
				type = "image/jpg";
			}
			if (tmp.endsWith(".swf")) {
				type = "image/swf";
			}
			if (tmp.endsWith(".pdf")) {
				type = "image/pdf";
			}
			try {
				File _file = m_gitService.searchFile(m_namespace, file, type);
				return new StreamSource(new FileInputStream(_file));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}
}
