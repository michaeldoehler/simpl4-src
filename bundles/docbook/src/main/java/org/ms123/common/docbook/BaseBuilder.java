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
import java.io.BufferedReader;
import java.lang.reflect.Method;
import org.ms123.common.data.api.DataLayer;
import org.ms123.common.workflow.api.GroovyTaskDslBase;
import org.osgi.framework.BundleContext;
import org.ms123.common.data.api.SessionContext;
import org.ms123.common.store.StoreDesc;
import groovy.lang.*;
import java.util.regex.*;
import org.codehaus.groovy.control.*;
import nu.xom.*;
import org.wikimodel.wem.*;
import org.wikimodel.wem.xwiki.XWikiParser;
import org.wikimodel.wem.xhtml.PrintListener;
import org.ms123.common.docbook.wikimodel.WemPrintListener;
import org.ms123.common.docbook.wikimodel.WemHeaderListener;
import org.ms123.common.utils.StackTrace;
import org.apache.commons.lang.StringUtils;

/**
 *
 */
@SuppressWarnings("unchecked")
public class BaseBuilder {

	protected JSONDeserializer m_ds = new JSONDeserializer();

	protected JSONSerializer m_js = new JSONSerializer();

	protected BundleContext m_bc;

	protected DataLayer m_dataLayer;

	BaseBuilder(DataLayer dataLayer, BundleContext bc) {
		m_dataLayer = dataLayer;
		m_bc = bc;
	}

	protected String applyGroovy(String namespace, String markdown, String filter, Map<String, Object> paramsIn, boolean throwException) {
		if (markdown == null || markdown.trim().length() == 0)
			return "";
		Map binding = paramsIn != null ? new HashMap(paramsIn) : new HashMap();
		StoreDesc sdesc = StoreDesc.getNamespaceData(namespace);
		SessionContext sc = m_dataLayer.getSessionContext(sdesc);
		if (filter != null && filter.trim().length() > 0) {
			Map retMap = sc.executeNamedFilter(filter, new HashMap());
			List<Map> result = null;
			if (retMap == null) {
				result = new ArrayList();
			} else {
				result = (List) retMap.get("rows");
			}
			binding.put("list", result);
		}
		try {
			Map<String, Object> vars = new HashMap();
			CompilerConfiguration config = new CompilerConfiguration();
			config.setDebug(true);
			config.setScriptBaseClass(GroovyTaskDslBase.class.getName());
			//vars.put("__sessionContext", sc);
			binding.put("__sessionContext", sc);
			binding.put("__queredObjects", new ArrayList());
			binding.put("__createdObjects", new ArrayList());
			Binding xbinding = new Binding(vars);
			GroovyShell shell = new GroovyShell(this.getClass().getClassLoader(), xbinding, config);
			TemplateEvaluator te = new TemplateEvaluator(shell);
			return te.render(markdown, binding);
		} catch (Throwable e) {
			String msg = e.getMessage();

			int hit = -1;
			int index = msg.indexOf("org.codehaus.groovy.syntax.SyntaxException");
			String lineNumber = null;
			if (index != -1) {
				msg = msg.substring(0, index-1 );
				Object[] res  = getLineNumberFromMsg(msg);
				if( res != null){
					lineNumber = (String)res[0];
					msg = (String)res[1];
				}
			} else {
				if( e instanceof groovy.lang.MissingPropertyException){
					hit = searchProperty(markdown,((groovy.lang.MissingPropertyException)e).getProperty());
				}

				if( hit == -1){
					StackTrace st = new StackTrace(e);
					Iterator it = st.iterator();
						
					while (it.hasNext()) {
						StackTrace.Entry en = (StackTrace.Entry) it.next();
						if (en.getSourceFileName().startsWith("SimpleTemplateScript")) {
							lineNumber = en.getLineNumber();
							break;
						}
					}
				}
			}
			Object[] ret = insertLineNumbers(markdown,lineNumber,hit);
			if( lineNumber != null){
				msg = "Line:" + ret[0] + ": " + msg;
			}else if( hit != -1){
				msg = "Line:" + hit + ": " + msg;
			}
			if( throwException){
				throw new RuntimeException(msg+"\n"+ret[1]);
			}else{
				return "{{{"+msg + "\n"+ret[1]+"}}}";
			}
		}
	}

	private Object[] getLineNumberFromMsg(String msg){
		Pattern p = Pattern.compile(".*SimpleTemplateScript\\d{1,5}.groovy: (\\d{1,5}):(.*)", Pattern.DOTALL);
		Matcher m = p.matcher(msg);
		Object[] ret = new Object[2];
		if (m.find()) {
			ret[0] = m.group(1);
			ret[1] = m.group(2);
			return ret;
		}
		return null;
	}
	private int searchProperty(String lines, String property){
		BufferedReader br = new BufferedReader(new StringReader(lines));
		try {
			int count = 1;  
			String line = br.readLine();            
			while (line != null) {
				boolean b1 = line.matches(".*\\$"+property+"\\W.*");
				boolean b2 = line.matches(".*\\$"+property+"$");
				boolean b3 = line.matches(".*\\$"+property+"\\.\\w.*");
				if( b1||b2||b3 ){
					return count;
				}
				line = br.readLine();    
				count++;
			}
		}catch(Exception e){
			e.printStackTrace();
		} finally {
			try{
				br.close();
			}catch(Exception e){
			}
		}
		return -1;
	}

	private Object[] insertLineNumbers(String lines, String lineNumber, int hit){
		int lnr = -1;
		if( lineNumber != null){
			lnr = Integer.parseInt(lineNumber);
		}
		BufferedReader br = new BufferedReader(new StringReader(lines));
		StringBuilder sb = new StringBuilder();
		int totalScriptStart=0;
		int matchedAt=-1;
		try {
			int count = 1;  
			String line = br.readLine();            
			while (line != null) {
				if( hit == -1 && matchedAt==-1){
					totalScriptStart += StringUtils.countMatches(line, "<%");
				}
				if( matchedAt==-1 && ((lnr != -1 && lnr == (count+(totalScriptStart-1))) || hit == count)){
					sb.append("+ ");
					matchedAt=count;
				}else{
					sb.append("  ");
				}
				sb.append(count++);
				sb.append(" ");
				sb.append(line);
				sb.append("\n");
				line = br.readLine();    
			}
		}catch(Exception e){
			e.printStackTrace();
		} finally {
			try{
				br.close();
			}catch(Exception e){
			}
		}
		Object[] ret = new Object[2];
		ret[0] = matchedAt;
		ret[1] = sb.toString();
		return ret;
	}
	/* End Error Handling */


	protected boolean getLineBreak(List<Map> childShapes, int i) {
		int size = childShapes.size();
		Map<String, Map<String, Map>> child = childShapes.get(i);
		Map<String, Number> UL = child.get("bounds").get("upperLeft");
		boolean lineBreak = false;
		Map<String, Map<String, Map>> cc = null;
		if ((i + 1) < size) {
			Map<String, Map<String, Map>> child2 = childShapes.get(i + 1);
			cc = child2;
			Map<String, Number> nextUL = child2.get("bounds").get("upperLeft");
			if (Math.abs(UL.get("y").intValue() - nextUL.get("y").intValue()) >= 30) {
				lineBreak = true;
			}
		}
		return lineBreak;
	}

	protected String getStencilId(Map element) {
		Map stencil = (Map) element.get("stencil");
		String id = ((String) stencil.get("id")).toLowerCase();
		return id;
	}

	protected List<Map> getShapeList(List<Map> list, List<String> types) {
		List<Map> retList = new ArrayList();
		for (Map<String, Map> e : list) {
			String sid = getStencilId(e);
			if (types.contains(sid)) {
				retList.add(e);
			}
		}
		return retList;
	}

	protected List<Map> getShapeList(List<Map> list, String type) {
		List<Map> retList = new ArrayList();
		for (Map<String, Map> e : list) {
			String sid = getStencilId(e);
			if (type.toLowerCase().equals(sid)) {
				retList.add(e);
			}
		}
		return retList;
	}

	protected Map getShapeSingle(List<Map> list, String type) {
		for (Map<String, Map> e : list) {
			String sid = getStencilId(e);
			if (type.toLowerCase().equals(sid)) {
				return e;
			}
		}
		return null;
	}

	protected void sortListByPosition(List<Map> list) {
		Collections.sort(list, new ListSortByPosition());
	}

	protected class ListSortByPosition implements Comparator<Map> {

		public int compare(Map c1, Map c2) {
			Map<String, Number> u1 = (Map) ((Map) c1.get("bounds")).get("upperLeft");
			Map<String, Number> u2 = (Map) ((Map) c2.get("bounds")).get("upperLeft");
			return (u1.get("y").intValue() * 10000 + u1.get("x").intValue()) - (u2.get("y").intValue() * 10000 + u2.get("x").intValue());
		}
	}

	protected void sortListByX(List<Map> list) {
		Collections.sort(list, new ListSortByX());
	}

	protected class ListSortByX implements Comparator<Map> {

		public int compare(Map c1, Map c2) {
			Map<String, Number> u1 = (Map) ((Map) c1.get("bounds")).get("upperLeft");
			Map<String, Number> u2 = (Map) ((Map) c2.get("bounds")).get("upperLeft");
			return (u1.get("x").intValue() - u2.get("x").intValue());
		}
	}

	protected boolean getBoolean(Map properties, String key, boolean def) {
		try {
			if (properties.get(key) == null)
				return def;
			return (Boolean) properties.get(key);
		} catch (Exception e) {
			return def;
		}
	}

	protected int getInteger(Map properties, String key, int def) {
		try {
			if (properties.get(key) == null)
				return def;
			return ((Integer) properties.get(key)).intValue();
		} catch (Exception e) {
			return def;
		}
	}

	protected String getString(Map properties, String key, String def) {
		try {
			String val = (String) properties.get(key);
			if (val == null || "".equals(val.trim())) {
				return def;
			}
			return val;
		} catch (Exception e) {
			return def;
		}
	}

	protected boolean isEmpty(String str) {
		if (str == null || str.length() == 0)
			return true;
		return false;
	}

	protected void printlist(String header, List<Map> list) {
		System.out.println("----->" + header);
		for (Map m : list) {
			Map properties = (Map) m.get("properties");
			System.out.println("\t" + m.get("bounds") + "/" + properties.get("xf_id"));
		}
	}

	protected static String xwikiToHtml(Context ctx, String wikiText) throws Exception {
		IWikiParser parser = new XWikiParser();
		long start = new Date().getTime();
		ctx.set("headerList", new ArrayList());
		ctx.set("jsList", new ArrayList());
		getHeaderList(ctx,wikiText);
		System.out.println("HeaderList:"+ctx.get("headerList"));
		long end = new Date().getTime();
		System.out.println("getHeaderList.time:"+(end-start));
		IWikiPrinter printer = new IWikiPrinter() {
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
		};

		IWemListener serializer = new WemPrintListener(ctx, printer);
		Reader is = new StringReader(wikiText);
		parser.parse(is, serializer);
		return "<div class=\"content\">" + printer.toString() + "</div>";
	}
	private static void getHeaderList(Context ctx, String wikiText)throws Exception{
		IWikiPrinter printer = new IWikiPrinter() {
			public void print(String str) {
			}
			public void println(String str) {
			}
		};
		IWemListener serializer = new WemHeaderListener(ctx, printer);
		Reader is = new StringReader(wikiText);
		IWikiParser parser = new XWikiParser();
		parser.parse(is, serializer);
	}
}
