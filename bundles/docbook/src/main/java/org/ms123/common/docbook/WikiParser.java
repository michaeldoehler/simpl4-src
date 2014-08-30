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
package org.ms123.common.docbook;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.io.StringReader;
import java.io.FileNotFoundException;
import org.pegdown.Extensions;
import org.pegdown.PegDownProcessor;
import org.pegdown.ast.RootNode;
import org.pegdown.ToHtmlSerializer;
import java.util.List;
import java.util.ArrayList;
import javax.xml.bind.JAXBException;
import org.osgi.framework.Bundle;
import org.sweble.wikitext.engine.CompiledPage;
import org.sweble.wikitext.engine.Compiler;
import org.sweble.wikitext.engine.CompilerException;
import org.sweble.wikitext.engine.PageId;
import org.sweble.wikitext.engine.PageTitle;
import org.sweble.wikitext.engine.utils.HtmlPrinter;
import org.sweble.wikitext.engine.utils.SimpleWikiConfiguration;
import org.sweble.wikitext.lazy.LinkTargetException;
import org.sweble.wikitext.lazy.LinkTargetParser;
import org.sweble.wikitext.engine.config.Namespace;
/**
 *
 */
@SuppressWarnings("unchecked")
public class WikiParser {

	private static SimpleWikiConfiguration m_config;

	private Bundle m_bundle;

	public WikiParser() {
	}

	public WikiParser(Bundle bundle) {
		m_bundle = bundle;
	}

	public void parseToHtml(String source, Writer w) {
		long starttime = new java.util.Date().getTime();
		ClassLoader previous = null;
		if (m_bundle != null) {
			previous = Thread.currentThread().getContextClassLoader();
			Thread.currentThread().setContextClassLoader(m_bundle.getClass().getClassLoader());
		}
		try {
			if (m_config == null) {
				m_config = new MyWikiConfiguration("classpath:/org/sweble/wikitext/engine/SimpleWikiConfiguration.xml");
			}
			Compiler compiler = new Compiler(m_config);
			PageTitle pageTitle = PageTitle.make(m_config, "dummy");
			PageId pageId = new PageId(pageTitle, -1);
			CompiledPage cp = compiler.postprocess(pageId, source, null);
			SwebleHtml.print(w, cp.getPage(), pageTitle.getFullTitle());
			long time1 = new java.util.Date().getTime();
			System.out.println("WikiParser.time1:" + (time1 - starttime));
		} catch (Exception e) {
			throw new RuntimeException("WikiParser:Failed reading MediaWiki source document", e);
		} finally {
			if (previous != null) {
				Thread.currentThread().setContextClassLoader(previous);
			}
		}
	}

	public List<Object> parse(String source) {
		ClassLoader previous = null;
		if (m_bundle != null) {
			previous = Thread.currentThread().getContextClassLoader();
			Thread.currentThread().setContextClassLoader(m_bundle.getClass().getClassLoader());
		}
		try {
			if (m_config == null) {
				m_config = new MyWikiConfiguration("classpath:/org/sweble/wikitext/engine/SimpleWikiConfiguration.xml");
			}
			Compiler compiler = new Compiler(m_config);
			PageTitle pageTitle = PageTitle.make(m_config, "dummy");
			PageId pageId = new PageId(pageTitle, -1);
			CompiledPage cp = compiler.postprocess(pageId, source, null);
			// Render the compiled page as Docbook
			StringWriter w = new StringWriter();
			SwebleDocbook p = new SwebleDocbook(w, pageTitle.getFullTitle());
			p.go(cp.getPage());
			return p.getDocbookObjects();
		} catch (Exception e) {
			throw new RuntimeException("WikiParser:Failed reading MediaWiki source document", e);
		} finally {
			if (previous != null) {
				Thread.currentThread().setContextClassLoader(previous);
			}
		}
	}
	class MyWikiConfiguration extends SimpleWikiConfiguration{
		MyWikiConfiguration(String config)throws FileNotFoundException,JAXBException{
			super(config);
			addNamespace(new Namespace(511,"repo", "repo",false, true,new ArrayList()));
		}
		@Override
		public TargetType classifyTarget(String target) {
			System.out.println("MyWikiConfiguration:classifyTarget:"+target);
			LinkTargetParser ltp = new LinkTargetParser();
			try {
				ltp.parse(this, target);
			}
			catch (LinkTargetException e) {
				return TargetType.INVALID;
			}

			String ns = ltp.getNamespace();
			System.out.println("NS:"+ns);
			if ("file".equalsIgnoreCase(ns) || 
					"repo".equalsIgnoreCase(ns) ||
					"image".equalsIgnoreCase(ns)
				 ){
				System.out.println("MyWikiConfiguration:return :"+TargetType.IMAGE);
				return TargetType.IMAGE;
			}

			return TargetType.PAGE;
		}
	}
}
