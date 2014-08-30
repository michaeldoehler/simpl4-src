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
import java.io.StringWriter;
import java.io.Writer;
import de.fau.cs.osr.ptk.common.ast.*;
import java.io.File;
import java.util.*;
import org.sweble.wikitext.engine.Page;
import org.sweble.wikitext.lazy.parser.Bold;
import org.sweble.wikitext.lazy.parser.Whitespace;
import org.sweble.wikitext.lazy.parser.Italics;
import org.sweble.wikitext.lazy.parser.Paragraph;
//import org.sweble.wikitext.lazy.parser.Section;
import org.sweble.wikitext.lazy.parser.SemiPreLine;
import org.sweble.wikitext.lazy.parser.SemiPre;
import org.sweble.wikitext.lazy.parser.Itemization;
import org.sweble.wikitext.lazy.parser.ItemizationItem;
import org.sweble.wikitext.lazy.parser.Enumeration;
import org.sweble.wikitext.lazy.parser.EnumerationItem;
//import org.sweble.wikitext.lazy.parser.Table;
import org.sweble.wikitext.lazy.parser.TableRow;
import org.sweble.wikitext.lazy.parser.TableHeader;
import org.sweble.wikitext.lazy.parser.TableCell;
import org.sweble.wikitext.lazy.parser.TableCaption;
import org.sweble.wikitext.lazy.parser.DefinitionList;
import org.sweble.wikitext.lazy.parser.DefinitionTerm;
import org.sweble.wikitext.lazy.parser.DefinitionDefinition;
import org.sweble.wikitext.lazy.parser.Url;
import org.sweble.wikitext.lazy.parser.ExternalLink;
import org.sweble.wikitext.lazy.parser.InternalLink;
import org.sweble.wikitext.lazy.parser.HorizontalRule;
import org.sweble.wikitext.lazy.parser.XmlElement;
import org.sweble.wikitext.lazy.parser.XmlElementOpen;
import org.sweble.wikitext.lazy.parser.XmlElementClose;
import org.sweble.wikitext.lazy.utils.*;
import org.ms123.common.docbook.xom.db.*;
import org.apache.commons.beanutils.PropertyUtils;
import nu.xom.Element;

@SuppressWarnings("unchecked")
public class SwebleDocbook extends de.fau.cs.osr.ptk.common.PrinterBase {

	protected Map<Integer, Section> m_sectionMap = new HashMap();

	protected Section m_currentSection = null;

	protected int m_sectionLevel = 0;

	protected List<Object> m_firstLevelList = new ArrayList();

	protected List<Object> m_currentList;

	protected Stack<Informaltable> m_tableStack = new Stack();

	protected int m_tableState;

	protected Map<String, String> m_currentParam;

	protected String m_currentValue;
	protected boolean m_inWrongElement=false;

	private static final int INHEADER = 1;

	private static final int INBODY = 2;

	public static String print(AstNode node, String articleTitle) {
		StringWriter writer = new StringWriter();
		new SwebleDocbook(writer, articleTitle).go(node);
		return writer.toString();
	}

	public static Writer print(Writer writer, AstNode node, String articleTitle) {
		new SwebleDocbook(writer, articleTitle).go(node);
		return writer;
	}

	public List<Object> getDocbookObjects() {
		System.out.println("m_firstLevelList:" + m_firstLevelList);
		return m_firstLevelList;
	}

	// =========================================================================
	public void visit(AstNode astNode) throws IOException {
		println("AstNode:" + astNode);
	}

	public void visit(NodeList l) throws IOException {
		println("NodeList:" + l);
		iterate(l);
	}

	public void visit(Page page) throws IOException {
		println("Page");
		m_currentList = m_firstLevelList;
		iterate(page.getContent());
	}

	public void visit(XmlElementOpen e) throws IOException {
		println("XmlElementOpen:" + e.getName());
		m_inWrongElement = true;
	}
	public void visit(XmlElementClose e) throws IOException {
		m_inWrongElement = false;
	}

	public void visit(XmlElement e) throws IOException {
		println("XmlElement:" + e.getName());
		//iterate(e.getXmlAttributes());
		Map<String, String> params = iterateAttributes(e.getXmlAttributes());
		System.out.println("Params:" + params);
		if (e.getEmpty()) {
		} else {
			println("XmlElement.name:" + e.getName());
			Object obj = null;
			if( "blockquote".equals( e.getName())){
				Quote q = new Quote();
				m_currentList.add(q);
				List<Object> save = m_currentList;
				m_currentList = q.getContent();
				iterate(e.getBody());
				m_currentList = save;
				obj = q;
			}
			if( "sub".equals( e.getName()) ){
				Subscript q = new Subscript();
				m_currentList.add(q);
				List<Object> save = m_currentList;
				m_currentList = q.getContent();
				iterate(e.getBody());
				m_currentList = save;
				obj = q;
			}
			if( "span".equals( e.getName()) ){
				Phrase q = new Phrase();
				m_currentList.add(q);
				List<Object> save = m_currentList;
				m_currentList = q.getContent();
				iterate(e.getBody());
				m_currentList = save;
				obj = q;
			}
			if( "p".equals( e.getName()) ){
				Para q = new Para();
				m_currentList.add(q);
				List<Object> save = m_currentList;
				m_currentList = q.getContent();
				iterate(e.getBody());
				m_currentList = save;
				obj = q;
			}
			if( "sup".equals( e.getName()) ){
				Superscript q = new Superscript();
				m_currentList.add(q);
				List<Object> save = m_currentList;
				m_currentList = q.getContent();
				iterate(e.getBody());
				m_currentList = save;
				obj = q;
			}
			if( "del".equals( e.getName()) ){
				Emphasis emp = new Emphasis();
				emp.setRole("strikethrough");
				m_currentList.add(emp);
				List<Object> save = m_currentList;
				m_currentList = emp.getContent();
				iterate(e.getBody());
				m_currentList = save;
				obj = emp;
			}
			if( "ins".equals(e.getName())){
				Emphasis emp = new Emphasis();
				emp.setRole("underline");
				m_currentList.add(emp);
				List<Object> save = m_currentList;
				m_currentList = emp.getContent();
				iterate(e.getBody());
				m_currentList = save;
				obj = emp;
			}
			if( obj != null){
				for (String key : params.keySet()) {
					if (params.get(key) != null) {
						setAttribute(obj, key, params.get(key));
					}
				}
			}
		}
	}

	public void visit(XmlAttribute a) throws IOException {
		println("XmlAttribute:" + a.getName());
		iterate(a.getValue());
		if (m_currentParam != null) {
			m_currentParam.put(a.getName(), m_currentValue);
		}
	}

	public void visit(XmlEntityRef ref) throws IOException {
		if (m_currentParam != null) {
			m_currentValue = "&" + ref.getName() + ";";
		} else {
			m_currentList.add("&" + ref.getName() + ";");
		}
	}

	public void visit(Text text) throws IOException {
		println("Text:" + text.getContent());
		if( m_inWrongElement ) return;
		if (m_currentParam != null) {
			m_currentValue = text.getContent();
		} else {
			m_currentList.add(text.getContent());
		}
	}
	public void visit(SemiPreLine n) throws IOException {
		println("SemiPreLine:" + n.getContent());
		iterate(n.getContent());
		m_currentList.add( "\n");	
	}

	public void visit(SemiPre n) throws IOException {
		println("SemiPre:" + n.getContent());
		Programlisting spre = new Programlisting();
		m_currentList.add(spre);
		List<Object> save = m_currentList;
		m_currentList = spre.getContent();
		iterate(n.getContent());
		m_currentList = save;
	}

	public void visit(Italics n) throws IOException {
		println("Italics:" + n.getContent());
		Emphasis emp = new Emphasis();
		emp.setRole("italic");
		m_currentList.add(emp);
		List<Object> save = m_currentList;
		m_currentList = emp.getContent();
		iterate(n.getContent());
		m_currentList = save;
	}

	public void visit(Bold n) throws IOException {
		println("Bold:" + n.getContent());
		Emphasis emp = new Emphasis();
		emp.setRole("bold");
		m_currentList.add(emp);
		List<Object> save = m_currentList;
		m_currentList = emp.getContent();
		iterate(n.getContent());
		m_currentList = save;
	}

	public void visit(Whitespace n) throws IOException {
		println("Whitespace:" + n.getContent());
	}

	public void visit(Paragraph p) throws IOException {
		println("Paragraph");
		Para para = new Para();
		m_currentList.add(para);
		List<Object> save = m_currentList;
		m_currentList = para.getContent();
		renderBlockLevelElementsFirst(p);
		if (!isParagraphEmpty(p)) {
			iterate(p.getContent());
		}
		m_currentList = save;
	}

	public void visit(org.sweble.wikitext.lazy.parser.Section s) throws IOException {
		println("Section:" + s.getLevel() + "/" + s.getTitle());
		handleSectionStart(s.getLevel());
		Title title = new Title();
		m_currentList.add(title);
		List<Object> save = m_currentList;
		m_currentList = title.getContent();
		iterate(s.getTitle());
		m_currentList = save;
		iterate(s.getBody());
		m_currentList = save;
	}

	public void visit(DefinitionList n) throws IOException {
		println("DefinitionList:" + n);
		iterate(n.getContent());
	}

	public void visit(DefinitionTerm n) throws IOException {
		iterate(n.getContent());
	}

	public void visit(DefinitionDefinition n) throws IOException {
		iterate(n.getContent());
	}

	public void visit(Enumeration n) throws IOException {
		println("Enumeration:" + n);
		Orderedlist ol = new Orderedlist();
		ol.setSpacing("compact");
		m_currentList.add(ol);
		List<Object> save = m_currentList;
		m_currentList = ol.getContent();
		iterate(n.getContent());
		m_currentList = save;
	}

	public void visit(EnumerationItem n) throws IOException {
		println("EnumerationItem:" + n);
		Listitem li = new Listitem();
		m_currentList.add(li);
		List<Object> save = m_currentList;
		Simpara sp = new Simpara();
		li.add(sp);
		m_currentList = sp.getContent();
		iterate(n.getContent());
		m_currentList = save;
	}

	public void visit(Itemization n) throws IOException {
		println("Itemization:" + n);
		Itemizedlist il = new Itemizedlist();
		il.setSpacing("compact");
		m_currentList.add(il);
		List<Object> save = m_currentList;
		m_currentList = il.getContent();
		iterate(n.getContent());
		m_currentList = save;
	}

	public void visit(ItemizationItem n) throws IOException {
		println("ItemizationItem:" + n);
		Listitem li = new Listitem();
		m_currentList.add(li);
		List<Object> save = m_currentList;
		Simpara sp = new Simpara();
		li.add(sp);
		m_currentList = sp.getContent();
		iterate(n.getContent());
		m_currentList = save;
	}

	public void visit(ExternalLink link) throws IOException {
		println("ExternalLink:" + link);
		if (!link.getTitle().isEmpty()) {
			iterate(link.getTitle());
		} else {
		}
	}

	public void visit(Url url) throws IOException {
		println("Url:" + url);
	}

	public void visit(InternalLink n) throws IOException {
		println("InternalLink:" + n);
		if (n.getTitle().getContent().isEmpty()) {
		} else {
			iterate(n.getTitle().getContent());
		}
	}

	private Map<String, String> iterateAttributes(NodeList a) {
		m_currentParam = new HashMap();
		iterate(a);
		Map params = m_currentParam;
		m_currentParam = null;
		return params;
	}

	public void visit(org.sweble.wikitext.lazy.parser.Table ptable) throws IOException {
		println("Table:");
		Map<String, String> params = iterateAttributes(ptable.getXmlAttributes());
		System.out.println("Params:" + params);


		Informaltable table;
		List tb = ptable.getBody();
		if ((tb.size()> 0 && (tb.get(0) instanceof TableCaption)) || params.get("title") != null) {
			table = new Table();
		} else {
			table = new Informaltable();
		}
		if( params.get("font") != null){
			Para para = new Para();
			para.setRole( params.get("font"));
			para.getContent().add(table);		
			m_currentList.add(para);
		}else{
			m_currentList.add(table);
		}
		Tgroup tgroup = new Tgroup();
		table.setTgroup(tgroup);
		Tbody tbody = new Tbody();
		tgroup.setTbody(tbody);
		Thead thead = new Thead();
		tgroup.setThead(thead);
		m_tableStack.push(table);

		List<Colspec> csList = tgroup.getColspecs();
		for (String key : params.keySet()) {
			if (params.get(key) != null) {
				if (key.startsWith("colspec") || key.startsWith("cs")) {
					csList.add(getColspec(key, params.get(key)));
				} else if (key.equals("font")) {
					continue;
				} else if( key.startsWith("title") && table instanceof Table){
					Title title = new Title();
					title.getContent().add( params.get(key));
					((Table)table).setTitle( title );
				} else {
					setAttribute(table, key, params.get(key));
				}
			}
		}
		Collections.sort(csList, new ColspecSortByNum());
		iterate(ptable.getBody());
		int maxcols = 0;
		if (thead != null) {
			List<Row> rows = thead.getRows();
			if (rows.size() > 0) {
				Row row = rows.get(0);
				List l = row.getEntries();
				maxcols = l.size();
			} else {
				tgroup.setThead(null);
			}
		}
		List<Row> rows = tbody.getRows();
		if( rows.size() > 0){
			for (Row row : rows) {
				List l = row.getEntries();
				maxcols = Math.max(maxcols, l.size());
			}
		}else{
			Row row = new Row();
			Entry entry = new Entry();
			row.getEntries().add(entry);
			rows.add( row);
		}
		tgroup.setCols(maxcols + "");
		m_tableStack.pop();
	}

	public void visit(TableCaption caption) throws IOException {
		println("TableCaption:" + caption + "/" + m_tableStack.peek().getClass());
		if (m_tableStack.peek() instanceof Table) {
			Map<String, String> params = iterateAttributes(caption.getXmlAttributes());
			System.out.println("Params:" + params);
			if (((Table) m_tableStack.peek()).getTitle() != null) {
				Caption cap = new Caption();
				((Table) m_tableStack.peek()).setCaption(cap);
				List<Object> save = m_currentList;
				m_currentList = cap.getContent();
				iterate(caption.getBody());
				m_currentList = save;
			} else {
				Title title = new Title();
				((Table) m_tableStack.peek()).setTitle(title);
				List<Object> save = m_currentList;
				m_currentList = title.getContent();
				iterate(caption.getBody());
				m_currentList = save;
			}
		}
	}

	public void visit(TableRow prow) throws IOException {
		println("TableRow");
		m_tableState = INBODY;
		List tr = prow.getBody();
		if (tr.size()>0 && (tr.get(0) instanceof TableHeader)) {
			m_tableState = INHEADER;
		}
		List<Row> rows = getTableRows();
		Row row = new Row();
		rows.add(row);
		Map<String, String> params = iterateAttributes(prow.getXmlAttributes());
		;
		System.out.println("Params:" + params);
		for (String key : params.keySet()) {
			if (params.get(key) != null) {
				setAttribute(row, key, params.get(key));
			}
		}
		iterate(prow.getBody());
	}

	public void visit(TableHeader header) throws IOException {
		println("TableHeader");
		m_tableState = INHEADER;
		List<Row> rows = getTableRows();
		if (rows.size() == 0) {
			Row row = new Row();
			rows.add(row);
		}
		Row row = rows.get(rows.size() - 1);
		Entry entry = new Entry();
		row.getEntries().add(entry);
		List<Object> save = m_currentList;
		m_currentList = entry.getContent();
		Map<String, String> params = iterateAttributes(header.getXmlAttributes());
		System.out.println("Params:" + params);
		for (String key : params.keySet()) {
			if (params.get(key) != null) {
				if( key.equals("font")){
					Para para = new Para();
					para.setRole( params.get(key));
					m_currentList = para.getContent();
					entry.getContent().add(para);
				}else{
					setAttribute(entry, key, params.get(key));
				}
			}
		}
		iterate(header.getBody());
		m_currentList = save;
	}

	public void visit(TableCell cell) throws IOException {
		println("TableCell");
		List<Row> rows = getTableRows();
		if (rows.size() == 0) {
			Row row = new Row();
			rows.add(row);
		}
		Row row = rows.get(rows.size() - 1);
		Entry entry = new Entry();
		row.getEntries().add(entry);
		List<Object> save = m_currentList;
		m_currentList = entry.getContent();
		Map<String, String> params = iterateAttributes(cell.getXmlAttributes());
		System.out.println("Params:" + params);
		for (String key : params.keySet()) {
			if (params.get(key) != null) {
				if( key.equals("font")){
					Para para = new Para();
					para.setRole( params.get(key));
					m_currentList = para.getContent();
					entry.getContent().add(para);
				}else{
					setAttribute(entry, key, params.get(key));
				}
			}
		}
		iterate(cell.getBody());
		m_currentList = save;
	}

	public void visit(HorizontalRule rule) throws IOException {
		println("HorizontalRule:" + rule);
	}

	// =========================================================================
	private String articleTitle = "";

	// ===========================================================================
	public SwebleDocbook(Writer writer, String articleTitle) {
		super(writer);
		this.articleTitle = articleTitle;
	}

	// ===========================================================================
	/*
	private void iterate(List<? extends AstNode> list)
	{
		for (AstNode n : list)
			dispatch(n);
	}
	*/
	private String asXmlCharRefs(String codePoint) {
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < codePoint.length(); ++i) {
			b.append("&#");
			b.append((int) codePoint.charAt(i));
			b.append(";");
		}
		return b.toString();
	}

	@SuppressWarnings("unchecked")
	private void renderBlockLevelElementsFirst(Paragraph p) {
		List<AstNode> l = (List<AstNode>) p.getAttribute("blockLevelElements");
		if (l == null)
			return;
		for (AstNode n : l) dispatch(n);
	}

	@SuppressWarnings("unchecked")
	private boolean isParagraphEmpty(Paragraph p) {
		if (!p.isEmpty()) {
			List<AstNode> l = (List<AstNode>) p.getAttribute("blockLevelElements");
			if (l == null || p.size() - l.size() > 0)
				return false;
		}
		return true;
	}

	private void println(String s) {
		System.out.println(s);
	}

	private void handleSectionStart(int newLevel) {
		System.out.println("handleSectionStart:" + newLevel);
		closeOpenSections(newLevel);
		openMissingSections(newLevel);
		this.m_sectionLevel = newLevel;
		m_currentList = m_currentSection.getContent();
	}

	/**
     * Close open sections.
     *
     * @param newLevel the new section level, all upper levels have to be closed.
     * @param sink the sink to receive the events.
     */
	private void closeOpenSections(int newLevel) {
		while (this.m_sectionLevel >= newLevel) {
			System.out.println("\tcloseSection:" + m_sectionLevel);
			m_sectionMap.put(m_sectionLevel, null);
			this.m_sectionLevel--;
		}
	}

	/**
     * Open missing sections.
     *
     * @param newLevel the new section level, all lower levels have to be opened.
     * @param sink the sink to receive the events.
     */
	private void openMissingSections(int newLevel) {
		System.out.println("openMissingSections:" + m_sectionMap);
		m_currentSection = m_sectionMap.get(newLevel - 1);
		while (this.m_sectionLevel < newLevel) {
			m_sectionLevel++;
			System.out.println("\topenSection:" + m_sectionLevel);
			Section newSection = new Section();
			if (m_currentSection == null) {
				m_firstLevelList.add(newSection);
				m_sectionMap.put(m_sectionLevel, newSection);
			} else {
				m_currentSection.getContent().add(newSection);
				m_sectionMap.put(m_sectionLevel, newSection);
			}
			m_currentSection = newSection;
		}
	}

	private Tgroup getTableTgroup() {
		Tgroup tgroup = m_tableStack.peek().getTgroup();
System.out.println("getTableTgroup:"+tgroup+"/"+m_tableStack.peek());
		return tgroup;
	}

	private List<Row> getTableRows() {
		Tgroup tgroup = getTableTgroup();
		List<Row> rows = null;
		if (m_tableState == INHEADER) {
			Thead thead = tgroup.getThead();
			rows = thead.getRows();
		} else {
			Tbody tbody = tgroup.getTbody();
			rows = tbody.getRows();
		}
		return rows;
	}

	private Colspec getColspec(String key, String value) {
		Colspec colspec = new Colspec();
		try {
			String[] s = value.split(",");
			int len = key.startsWith("cs") ? 2 : 7;
			String colnum = key.substring(len);
			colspec.setColnum(colnum);
			if (s.length > 0 && s[0].length() > 0) {
				colspec.setAlign(getAlign(s[0]));
			}
			if (s.length > 1 && s[1].length() > 0) {
				colspec.setColwidth(s[1]);
			}
			if (s.length > 2 && s[2].length() > 0) {
				colspec.setColsep(getSep(s[2]));
			}
			if (s.length > 3 && s[3].length() > 0) {
				colspec.setRowsep(getSep(s[3]));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return colspec;
	}
	private String getAlign(String s){
		try{
			s = s.toLowerCase();
			if( s.equals("left") || s.equals("right")|| s.equals("center")) return s;
			if( s.equals("l") ) return "left";
			if( s.equals("c")) return "center";
			if( s.equals("r")  ) return "right";
		}catch(Exception e){
		}
		return "left";
	}

	private String getSep(String s){
		try{
			s = s.toLowerCase();
			if( s.equals("true") ) return "1";
			if( s.equals("false")) return "0";
			if( s.equals("y") || s.equals("j") || s.equals("t")) return "1";
			if( s.equals("yes") || s.equals("ja")) return "1";
			if( s.equals("n") || s.equals("f") ) return "0";
			if( s.equals("no") || s.equals("nein")) return "0";
		}catch(Exception e){
		}
		return "0";
	}
	private void setAttribute(Object o, String attr, String value) {
		try {
			PropertyUtils.setProperty(o, attr, value);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private class ColspecSortByNum implements Comparator<Colspec> {

		public int compare(Colspec c1, Colspec c2) {
			int i1 = Integer.parseInt(c1.getColnum());
			int i2 = Integer.parseInt(c2.getColnum());
			return i1-i2;
		}
	}
}
