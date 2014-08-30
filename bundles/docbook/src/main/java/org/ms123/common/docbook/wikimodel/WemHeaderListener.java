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
public class WemHeaderListener extends PrintListener{
	protected IWikiPrinter m_printer;
	protected Context m_ctx = null;
	private StringBuilder m_headerTxtId=null;
	private StringBuilder m_headerTxtLabel=null;
	private Map m_headerMap = null;
	final IWikiParser parser = new XWikiParser();

	public WemHeaderListener(Context ctx, IWikiPrinter printer) {
		super(printer);
		m_ctx = ctx;
		m_printer = printer;
	}

	public void beginHeader(int headerLevel, WikiParameters params) {
		m_headerTxtId =  new StringBuilder();
		m_headerTxtLabel =  new StringBuilder();
		m_headerMap = new HashMap();
		m_headerMap.put("level", headerLevel);
	}

	public void endHeader(int headerLevel, WikiParameters params) {
		List headerList = (List)m_ctx.get("headerList");
		m_headerMap.put("label", m_headerTxtLabel.toString());
		m_headerMap.put("id", "H"+Utils.clearName(m_headerTxtId.toString(),true,true));
		headerList.add(m_headerMap);
		m_headerTxtId=null;
		m_headerTxtLabel=null;
		m_headerMap = null;
	}
	public void onImage(String link) {
		if( m_headerMap!=null ){
			m_headerMap.put("icon", link);
		}
	}

	public void onImage(WikiReference ref) {
		String link = ref.getLink();
		if( m_headerMap!=null ){
			m_headerMap.put("icon", link);
		}
	}
	public void onReference(WikiReference ref) {
		if( m_headerTxtId!=null ){
			String label=null;
			m_headerTxtId.append(ref.getLabel());
			try{
			System.out.println("WemHeaderListener.newReference1:" + ref.getLabel());
				WemPrintListener.WikiSink ws = new WemPrintListener.WikiSink();
				parser.parse(new StringReader(ref.getLabel()), new PrintInlineListener(ws));
				label = ws.toString();
				m_headerTxtLabel.append(label);
			}catch(Exception e){
				e.printStackTrace();
				label= e.getMessage();
			}
			System.out.println("WemHeaderListener.newReference2:"  + label);
		}
	}

	public void onWord(String str) {
		if( m_headerTxtId!=null ){
			System.out.println("WemHeaderListener.word:"  + str);
			m_headerTxtId.append(str);
			m_headerTxtLabel.append(str);
		}
	}
	public void onSpace(String str) {
		if( m_headerTxtId!=null ){
			m_headerTxtLabel.append(str);
		}
	}
}
