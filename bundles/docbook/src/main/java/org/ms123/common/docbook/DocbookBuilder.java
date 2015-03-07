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
import java.lang.reflect.Method;
import nu.xom.*;
import org.ms123.common.docbook.xom.db.*;
import org.ms123.common.data.api.DataLayer;
import org.ms123.common.utils.UtilsService;
import org.ms123.common.data.api.SessionContext;
import org.ms123.common.store.StoreDesc;
import org.osgi.framework.BundleContext;
import org.dbdoclet.trafo.html.docbook.*;
import org.asciidoctor.Asciidoctor;

/**
 *
 */
@SuppressWarnings("unchecked")
class DocbookBuilder extends BaseBuilder {
	List<String> m_pageMasterParam = new ArrayList<String>() {{
		add("page.margin.top");
		add("page.margin.bottom");
		add("body.margin.top");
		add("body.margin.bottom");
		add("region.after.extent");
		add("region.before.extent");
	}};
	protected Asciidoctor m_asciidoctor;

	DocbookBuilder(DataLayer dataLayer, BundleContext bc,Asciidoctor ad) {
		super(dataLayer, bc);
		m_asciidoctor=ad;
	}

	protected void toDocbook(String namespace, String json, OutputStream out, Map<String, Object> paramsIn, Map<String, String> paramsOut)  throws Exception{
		Article article = new Article();
		Map m = (Map) m_ds.deserialize(json);
		m_js.prettyPrint(true);
		List<Map> childShapes = (List) m.get("childShapes");
		getParamsOut(m, paramsOut);
		sortListByPosition(childShapes);
		int size = childShapes.size();
		List<List<Map>> lines = getRows(childShapes);
		for (List<Map> line : lines) {
			if (line.size() == 0) {
				continue;
			}
			if (line.size() == 1 && isBodyRowGroup(line.get(0))) {
				handleRowGroup(namespace, article, line.get(0), paramsIn, paramsOut);
			} else {
				sortListByX(line);
				handleLine(namespace, article.getContent(), line, null, "0", null, paramsIn, paramsOut);
			}
		}
		List<Map> headerFooterRowGroupList = getHeaderFooterRowGroupList(childShapes);
		printlist("3", headerFooterRowGroupList);
		for (Map headerFooterRowGroup : headerFooterRowGroupList) {
			handleRowGroup(namespace, article, headerFooterRowGroup, paramsIn, paramsOut);
		}
		marshallObjectXom(article, out);
	}


	private void handleLine(String namespace, List parent, List<Map> line, String frame, String colsep, String keepTogether, Map<String, Object> paramsIn, Map<String, String> paramsOut) throws Exception {
System.out.println("handleLine:"+line.size());
		if (line.size() == 0) {
			return;
		}
		int cols = 0;
		for (Map m : line) {
			Map properties = (Map) m.get("properties");
			boolean xf_enabled = getBoolean(properties, "xf_enabled", true);
			if (xf_enabled){
				cols++;
			}
		}
		if (cols==0){
			return;
		}
		Informaltable table = new Informaltable();
		if (!withFrame(line)) {
			table.setFrame("none");
			if (frame != null) {
				table.setFrame(frame);
			}
		}
		if (keepTogether != null) {
			table.setKeepTogether(keepTogether);
		}
		table.setColsep(colsep);
		table.setRowsep("0");
		parent.add(table);
		Tgroup tgroup = new Tgroup();
		tgroup.setCols(cols+"");
		table.setTgroup(tgroup);

		List<Colspec> csList = tgroup.getColspecs();
		int colnum = 1;
		for (Map m : line) {
			Map properties = (Map) m.get("properties");
			boolean xf_enabled = getBoolean(properties, "xf_enabled", true);
			if (!xf_enabled){
				continue;
			}
			Colspec colspec = new Colspec();
			colspec.setColnum((colnum++) + "");
			colspec.setAlign(getColAlign(properties.get("xf_align")));
			colspec.setColwidth(getColWidth(properties.get("xf_width")));
			colspec.setColsep(getColsep(properties.get("xf_colsep")));
			csList.add(colspec);
		}
		Tbody tbody = new Tbody();
		tgroup.setTbody(tbody);
		List<Row> rows = tbody.getRows();
		Row row = new Row();
		rows.add(row);
		for (Map m : line) {
			Map properties = (Map) m.get("properties");
			boolean xf_enabled = getBoolean(properties, "xf_enabled", true);
			if (!xf_enabled){
				continue;
			}
			Entry entry = new Entry();
			entry.setValign(getValign(properties.get("xf_valign")));
			row.getEntries().add(entry);
			handleElement(namespace, entry.getContent(), m, paramsIn, paramsOut);
		}
	}

	private void handleRowGroup(String namespace, Article article, Map rowGroup, Map<String, Object> paramsIn, Map<String, String> paramsOut)  throws Exception{
System.out.println("handleRowGroup");
		boolean isBodyRowGroup = isBodyRowGroup(rowGroup);
		List<Map> childShapes = (List) rowGroup.get("childShapes");
		sortListByX(childShapes);
		Para p = new Para();
		Map properties = (Map) rowGroup.get("properties");
		String xf_frame = (String) properties.get("xf_frame");
		String xf_colsep = getColsep(properties.get("xf_colsep"));
		String xf_keepTogether = getKeepTogether(properties, "xf_keeptogether");
		String xf_need = getString(properties, "xf_need", null);
		boolean xf_enabled = getBoolean(properties, "xf_enabled", true);
		if (!xf_enabled)
			return;
		if (!isBodyRowGroup) {
			String xf_position = (String) properties.get("xf_position");
			String xf_pages = (String) properties.get("xf_pages");
			p.setRole("hf_" + xf_position + "_" + xf_pages);
			article.getContent().add(p);
			handleLine(namespace, p.getContent(), childShapes, xf_frame, xf_colsep, null, paramsIn, paramsOut);
		} else {
			if (xf_need != null && !"".equals(xf_need.trim())) {
				article.getContent().add(new ProcessingInstruction("dbfo-need", "height=\"" + xf_need + "\""));
			}
			handleLine(namespace, article.getContent(), childShapes, xf_frame, xf_colsep, xf_keepTogether, paramsIn, paramsOut);
		}
	}

	private boolean withFrame(List<Map> line) {
		for (Map m : line) {
			Map<String, Object> properties = (Map) m.get("properties");
			if (getBoolean(properties, "xf_frame", false))
				return true;
		}
		return false;
	}

	private String getKeepTogether(Map properties, String key) {
		try {
			if (properties.get(key) == null)
				return null;
			String v = (String) properties.get(key);
			if ("none".equals(v))
				return null;
			return v;
		} catch (Exception e) {
			return null;
		}
	}

	private String getColsep(Object o) {
		try {
			if (o == null)
				return "0";
			if (o instanceof Boolean) {
				Boolean b = (Boolean) o;
				System.out.println("getColsep:" + b);
				return b ? "1" : "0";
			}
			return "1".equals(o.toString()) ? "1" : "0";
		} catch (Exception e) {
			return "0";
		}
	}

	private String getColAlign(Object o) {
		try {
			return o != null ? (String) o : "left";
		} catch (Exception e) {
			return "left";
		}
	}

	private String getValign(Object o) {
		try {
			return o != null ? (String) o : "top";
		} catch (Exception e) {
			return "top";
		}
	}

	private String getColWidth(Object o) {
		try {
			return o != null ? (String) o : "1*";
		} catch (Exception e) {
			return "1*";
		}
	}


	private void getParamsOut(Map element, Map<String, String> paramsOut) {
		Map<String, Object> properties = (Map) element.get("properties");
		for (String prop : properties.keySet()) {
			if (prop.startsWith("db_")) {
				String val = (String) properties.get(prop);
				String key = prop.substring(3).replace("_", ".");
				if( m_pageMasterParam.indexOf( key) >=0){
					getTripleValues( paramsOut, key, val);
				}else{
					paramsOut.put(key, val);
				}
			}
		}
		System.out.println("getParamsOut.paramsOut:" + paramsOut);
	}

	private void getTripleValues(Map<String, String> paramsOut, String key, String val){
		if( val == null || val.trim().equals("")){
			paramsOut.put(key, val);
			return;
		}
		val = val.trim();
		val = val.replace(" ", "");
		String[] vals = val.split(",");
		if( vals.length == 1){
			paramsOut.put(key, vals[0]);
			paramsOut.put(key+".odd", vals[0]);
			paramsOut.put(key+".even", vals[0]);
			return;
		}
		if( vals.length == 2){
			paramsOut.put(key, vals[0]);
			paramsOut.put(key+".odd", vals[1]);
			paramsOut.put(key+".even", vals[1]);
			return;
		}
		paramsOut.put(key, vals[0]);
		paramsOut.put(key+".odd", vals[1]);
		paramsOut.put(key+".even", vals[2]);
	}

	private void handleElement(String namespace, List content, Map element, Map paramsIn, Map paramsOut) throws Exception{
		Map<String, Object> properties = (Map) element.get("properties");
		String id = getStencilId(element);
		getParamsOut(element, paramsOut);
		if ("autogentable".equals(id)) {
			String filter = (String) properties.get("xf_filter");
			List<Map> columns = (List) ((Map) properties.get("xf_columns")).get("items");
			handleAutoGenTable(namespace, filter, columns, content);
		} else if ("textarea".equals(id)) {
			String xf_type = getString(properties, "xf_type", null);
			String xf_des = getString(properties, "description", null);
			System.out.println("handleElement.textarea:"+xf_type+"/"+xf_des);
			long startTime = new Date().getTime();
			if( "markdown".equals(xf_type)){
				String markdown = (String) properties.get("xf_markdown");
				String filter = (String) properties.get("xf_filter");
				String syntax = (String) properties.get("xf_syntax");
				System.out.println("markdownPreroovy:" + markdown);
				markdown = applyGroovy(namespace, markdown, filter, paramsIn, true);
				System.out.println("markdownAfterGroovy:" + markdown);
				if( "mediawiki".equals(syntax)){
					WikiParser mp = new WikiParser(m_bc.getBundle());
					List<Object> ol = mp.parse(markdown);
					for (Object o : ol) {
						content.add(o);
					}
				}else if( "asciidoctor".equals(syntax)){
					String html = adocToHtml(markdown);
					System.out.println("AfterAdoc:"+html);
					DocBookTransformer tf = new DocBookTransformer();
					String db = tf.transformFragment(html);
					System.out.println("AfterDB:"+db);
					addDBChunkToContent(content, db );
				}else{
					Context ctx = new Context(null, null, false, new HashMap(), new HashMap());
					String html = xwikiToHtml(ctx, markdown);
					System.out.println("Html:"+html);
					DocBookTransformer tf = new DocBookTransformer();
					String db = tf.transformFragment(html);
					System.out.println("DB:"+html);
					addDBChunkToContent(content, db );
				}
			}
			if( "html".equals(xf_type)){
				String html = (String) properties.get("xf_html");
				System.out.println("htmlPreGroovy:" + html);
				html = applyGroovy(namespace, html, null, paramsIn, true);
				System.out.println("htmlAfterGroovy:" + html);
				DocBookTransformer tf = new DocBookTransformer();
				String db = tf.transformFragment(html);
				System.out.println("dbAfter:" + db);
				addDBChunkToContent(content, db );
			}
			long endTime = new Date().getTime();
			System.out.println("WikiParser.etime:" + (endTime - startTime) );
		}
	}
	private String adocToHtml( String adoc) throws Exception {
		Map<String, Object> options = new HashMap();
		Map<String, Object> attributes = new HashMap();
		attributes.put("icons", org.asciidoctor.Attributes.FONT_ICONS);
		options.put("attributes", attributes);
		options.put("safe", 0);
		return m_asciidoctor.convert( adoc, options);
	}
	private void addDBChunkToContent(List content, String db) throws Exception{
		if( db == null || db.trim().equals("")) return;
		Document doc = new Builder().build( "<div xmlns:xl=\"http://www.w3.org/1999/xlink\">"+db+"</div>", null);
		printXom(doc);
		Element rootElement = (Element) doc.getRootElement().getChild(0);
		System.out.println("childs:"+rootElement.getChildElements().size());
		int childSize = rootElement.getChildElements().size();
		List<Element> childs = new ArrayList();
		for( int i=0; i< childSize; i++){
			Element e = rootElement.getChildElements().get(i);	
			Utils.setNamespace(e,"http://docbook.org/ns/docbook");
			content.add(e);
			childs.add(e);
		}
		for( Element child : childs){
			child.detach();
		}
		rootElement.detach();
	}

	private void handleAutoGenTable(String namespace, String filter, List<Map> columns, List content) {
		StoreDesc sdesc = StoreDesc.getNamespaceData(namespace);
		SessionContext sc = m_dataLayer.getSessionContext(sdesc);
		Map retMap = sc.executeNamedFilter(filter, new HashMap());
		List<Map> result = null;
		if (retMap == null) {
			result = new ArrayList();
		} else {
			result = (List) retMap.get("rows");
		}
		Informaltable table = new Informaltable();
		content.add(table);
		Tgroup tgroup = new Tgroup();
		tgroup.setCols(columns.size() + "");
		table.setTgroup(tgroup);
		List<Colspec> csList = tgroup.getColspecs();
		for (Map m : columns) {
			Colspec colspec = new Colspec();
			csList.add(colspec);
		}
		Thead thead = new Thead();
		tgroup.setThead(thead);
		List<Row> rows = thead.getRows();
		Row row = new Row();
		rows.add(row);
		for (Map m : columns) {
			Entry entry = new Entry();
			row.getEntries().add(entry);
			Simpara sm = new Simpara();
			entry.getContent().add(sm);
			sm.getContent().add(m.get("colname"));
		}
		Tbody tbody = new Tbody();
		tgroup.setTbody(tbody);
		rows = tbody.getRows();
		for (Map record : result) {
			row = new Row();
			rows.add(row);
			for (Map m : columns) {
				Entry entry = new Entry();
				row.getEntries().add(entry);
				Simpara sm = new Simpara();
				entry.getContent().add(sm);
				sm.getContent().add(record.get(m.get("colname")));
			}
		}
	}

	protected List<List<Map>> getRows(List<Map> childShapes) {
		List lines = new ArrayList();
		int size = childShapes.size();
		if (size == 0) {
			return lines;
		}
		List line = new ArrayList();
		lines.add(line);
		for (int i = 0; i < size; i++) {
			Map child = (Map) childShapes.get(i);
			if (isHeaderFooterGroup(child)) {
				continue;
			}
			if (isBodyRowGroup(child)) {
				line = new ArrayList();
				line.add(child);
				lines.add(line);
				line = new ArrayList();
				continue;
			}
			line.add(child);
			if (getLineBreak(childShapes, i)) {
				if ((i + 1) < size) {
					line = new ArrayList();
					lines.add(line);
				}
			}
		}
		return lines;
	}

	private List<Map> getHeaderFooterRowGroupList(List<Map> childShapes) {
		List<Map> headerFooter = new ArrayList();
		int size = childShapes.size();
		if (size == 0) {
			return headerFooter;
		}
		for (int i = 0; i < size; i++) {
			Map child = (Map) childShapes.get(i);
			if (!isHeaderFooterGroup(child)) {
				continue;
			}
			headerFooter.add(child);
		}
		return headerFooter;
	}

	private boolean isBodyRowGroup(Map element) {
		Map properties = (Map) element.get("properties");
		String position = (String) properties.get("xf_position");
		return "body".equals(position);
	}

	private boolean isHeaderFooterGroup(Map element) {
		Map properties = (Map) element.get("properties");
		String position = (String) properties.get("xf_position");
		System.out.println("isHeaderFooterGroup:" + position);
		return "header".equals(position) || "footer".equals(position);
	}
	protected static void printXom(Document doc) {
		try {
			Serializer ser = new Serializer(System.out);
			ser.setIndent(2);
			ser.setLineSeparator("\n");
			System.out.println("Docbook.toXML:\n");
			ser.write(doc);
		} catch (Exception e) {
			throw new RuntimeException("DocbookServiceImpl.marshallObjectXom:", e);
		} finally {
		}
	}

	protected static void marshallObjectXom(Article article, OutputStream outputStream) {
		try {
			Document doc = new Document(article.toXom());
			Serializer ser = new Serializer(System.out);
			ser.setIndent(2);
			ser.setLineSeparator("\n");
			System.out.println("Docbook.toXML:\n");
			ser.write(doc);
			ser = new Serializer(outputStream);
			ser.write(doc);
		} catch (Exception e) {
			throw new RuntimeException("DocbookServiceImpl.marshallObjectXom:", e);
		} finally {
		}
	}

}
