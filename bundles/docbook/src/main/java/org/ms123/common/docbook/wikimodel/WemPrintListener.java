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
package org.ms123.common.docbook.wikimodel;

import flexjson.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.Writer;
import java.io.Reader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.StringWriter;
import java.io.StringReader;
import java.lang.reflect.*;
import org.ms123.common.docbook.Context;
import org.ms123.common.docbook.Utils;
import org.wikimodel.wem.*;
import org.wikimodel.wem.xwiki.XWikiParser;
import org.wikimodel.wem.xhtml.*;

/**
 *
 */
@SuppressWarnings("unchecked")
public class WemPrintListener extends PrintListener {

	protected JSONDeserializer m_ds = new JSONDeserializer();

	protected JSONSerializer m_js = new JSONSerializer();

	protected IWikiPrinter m_printer;

	protected Map m_macros = new HashMap();

	protected Context m_ctx = null;

	protected StringBuilder m_headerTxt = null;

	final IWikiParser parser = new XWikiParser();

	public WemPrintListener(Context ctx, IWikiPrinter printer) {
		super(printer);
		m_ctx = ctx;
		m_printer = printer;
	}

	public void onImage(String ref) {
		System.out.println("onImage0:" + ref);
		print("<img");
		print(" src='" + ref + "'");
		print(" class='wikimodel-freestanding'/>");
	}

	public void onImage(WikiReference ref) {
		print("<img");
		String link = ref.getLink();
		System.out.println("onImage1:" + link);
		link = WikiPageUtil.escapeXmlAttribute(link);
		link = link.replace(":", "%3a");
		System.out.println("onImage2:" + link);
		print(" src='" + link + "'");
		WikiParameters params = ref.getParameters();
		String label = ref.getLabel();
		if (label != null) {
			if (params.getParameter("title") == null) {
				params = params.addParameter("title", label);
			}
		}
		print(params + "/>");
	}

	protected ReferenceHandler newReferenceHandler() {
		return new ReferenceHandler(isSupportImage(), isSupportDownload()) {

			protected void handleImage(String ref, String label, WikiParameters params) {
				System.out.println("newhandleImage:" + ref + "/" + label);
				print("<img src='" + WikiPageUtil.escapeXmlAttribute(ref) + "'" + params + "/>");
			}

			protected void handleReference(String ref, String label, WikiParameters params) {
				System.out.println("WemPrintListener.newReference1:" + ref + "/" + label);
				if (m_headerTxt != null) {
					m_headerTxt.append(label);
				}
				try {
					WikiSink ws = new WikiSink();
					parser.parse(new StringReader(label), new PrintInlineListener(ws));
					System.out.println("WemPrintListener.newReference2:" + ref + "/" + ws.toString());
					print("<a href='" + WikiPageUtil.escapeXmlAttribute(ref) + "'" + params + ">" + ws.toString() + "</a>");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
	}

	public void onMacroBlock(String macroName, WikiParameters params, String content) {
		IWikiMacroRenderer wmr = createMacroInstance(macroName, params, content);
		if (wmr != null) {
			print(wmr.render());
			String js = wmr.getJS();
			if (js != null) {
				((List) m_ctx.get("jsList")).add(js);
			}
		} else {
			print("<span>Macro:" + macroName + " not found</span>");
		}
	}

	public void onMacroInline(String macroName, WikiParameters params, String content) {
		IWikiMacroRenderer wmr = createMacroInstance(macroName, params, content);
		if (wmr != null) {
			print(wmr.render());
			String js = wmr.getJS();
			if (js != null) {
				((List) m_ctx.get("jsList")).add(js);
			}
		} else {
			print("<span>Macro:" + macroName + " not found</span>");
		}
	}

	public void beginHeader(int headerLevel, WikiParameters params) {
		print("<h" + headerLevel + ">");
		m_headerTxt = new StringBuilder();
	}

	public void beginSection(int docLevel, int headerLevel, WikiParameters params) {
		System.out.println("Section.docLevel:" + docLevel + "/" + headerLevel + "/" + params);
		if ((docLevel == 1 && headerLevel > 0) || (docLevel > 1 && headerLevel != 1)) {
			print("<div class=\"section\" " + params + ">");
		}
	}

	public void endSection(int docLevel, int headerLevel, WikiParameters params) {
		if ((docLevel == 1 && headerLevel > 0) || (docLevel > 1 && headerLevel != 1)) {
			print("</div>");
		}
	}

	public void endHeader(int headerLevel, WikiParameters params) {
		String id = "H" + Utils.clearName(m_headerTxt.toString(), true, true);
		println("<span id=\"" + id + "\"/></h" + headerLevel + ">");
		m_headerTxt = null;
	}

  public void beginTable(WikiParameters params) {
    println("<table" + params + "><tbody>");
  }
	public void onWord(String str) {
		super.onWord(str);
		if (m_headerTxt != null) {
			m_headerTxt.append(str);
		}
	}

	public void onSpace(String str) {
		super.onSpace(str);
		if (m_headerTxt != null) {
			m_headerTxt.append(str);
		}
	}
  public void onNewLine() {
    println("<br />");
  }
	protected boolean isHtmlEntities() {
		return false;
	}


	public void print(String s) {
		m_printer.print(s);
	}

	private IWikiMacroRenderer createMacroInstance(String name, WikiParameters params, String content) {
		try {
			Class clazz = Class.forName("org.ms123.common.docbook.wikimodel." + firstToUpper(name) + "MacroRenderer");
			Constructor constructor = clazz.getConstructor(new Class[] { Context.class, WikiParameters.class, String.class });
			IWikiMacroRenderer wmr = (IWikiMacroRenderer) constructor.newInstance(new Object[] { m_ctx, params, content });
			return wmr;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private String firstToUpper(String s) {
		String fc = s.substring(0, 1);
		return fc.toUpperCase() + s.substring(1);
	}

	public static class WikiSink implements IWikiPrinter {

		private StringBuffer m_sb = new StringBuffer();

		public void print(String str) {
			m_sb.append(str);
		}

		public void println(String str) {
			m_sb.append(str + "\n");
		}

		public String toString() {
			return m_sb.toString();
		}
	}
}
