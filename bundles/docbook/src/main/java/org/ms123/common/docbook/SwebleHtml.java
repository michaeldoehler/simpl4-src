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
import java.util.ArrayList;
import java.util.List;
import org.sweble.wikitext.engine.Page;
import org.sweble.wikitext.lazy.encval.*;
import org.sweble.wikitext.lazy.parser.*;
import org.sweble.wikitext.lazy.preprocessor.*;
import org.sweble.wikitext.lazy.utils.*;

public class SwebleHtml extends de.fau.cs.osr.ptk.common.PrinterBase {

	public static String print(AstNode node, String articleTitle) {
		StringWriter writer = new StringWriter();
		new SwebleHtml(writer, articleTitle).go(node);
		return writer.toString();
	}

	public static Writer print(Writer writer, AstNode node, String articleTitle) {
		new SwebleHtml(writer, articleTitle).go(node);
		return writer;
	}

	// =========================================================================
	public void visit(AstNode astNode) throws IOException {
		print("<span class=\"");
		print(classPrefix);
		print("unknown-node\">");
		print(astNode.getClass().getSimpleName());
		print("</span>");
	}

	public void visit(NodeList l) throws IOException {
		iterate(l);
	}

	public void visit(Page page) throws IOException {
		printNewline(false);
		print("<div class=\"");
		print(classPrefix);
		print("content\">");
		printNewline(true);
		incIndent("\t\t");
		iterate(page.getContent());
		decIndent();
		printNewline(true);
		print("</div>");
		printNewline(false);
		printNewline(false);
	}

	public void visit(Text text) throws IOException {
		print(escHtml(text.getContent()));
	}

	public void visit(Italics n) throws IOException {
		print("<i>");
		iterate(n.getContent());
		print("</i>");
	}

	public void visit(Bold n) throws IOException {
		print("<b>");
		iterate(n.getContent());
		print("</b>");
	}

	public void visit(Whitespace n) throws IOException {
		iterate(n.getContent());
	}

	public void visit(Paragraph p) throws IOException {
		printNewline(false);
		renderBlockLevelElementsFirst(p);
		printNewline(false);
		if (!isParagraphEmpty(p)) {
			print("<p>");
			printNewline(false);
			incIndent("\t");
			iterate(p.getContent());
			decIndent();
			printNewline(false);
			print("</p>");
		}
		printNewline(false);
		printNewline(false);
	}

	public void visit(SemiPre sp) throws IOException {
		printNewline(false);
		print("<pre>");
		iterate(sp.getContent());
		print("</pre>");
		printNewline(false);
	}

	public void visit(SemiPreLine line) throws IOException {
		iterate(line.getContent());
		print("\n");
	}

	public void visit(Section s) throws IOException {
		printNewline(false);
		print("<div class=\"");
		print(classPrefix);
		print("section\">");
		printNewline(true);
		print("\t<h");
		print(s.getLevel());
		print(">");
		iterate(s.getTitle());
		print("</h");
		print(s.getLevel());
		print(">");
		printNewline(true);
		print("\t<div class=\"");
		print(classPrefix);
		print("section-body\">");
		printNewline(false);
		incIndent("\t\t");
		iterate(s.getBody());
		decIndent();
		printNewline(false);
		print("\t</div>");
		printNewline(true);
		print("</div>");
		printNewline(false);
	}

	public void visit(XmlComment e) throws IOException {
	}

	public void visit(XmlElement e) throws IOException {
		print("<");
		print(e.getName());
		iterate(e.getXmlAttributes());
		if (e.getEmpty()) {
			print(" />");
		} else {
			print(">");
			iterate(e.getBody());
			print("</");
			print(e.getName());
			print(">");
		}
	}

	public void visit(XmlAttribute a) throws IOException {
		print(" ");
		print(a.getName());
		print("=\"");
		iterate(a.getValue());
		print("\"");
	}

	public void visit(XmlAttributeGarbage g) throws IOException {
	}

	public void visit(XmlCharRef ref) throws IOException {
		print("&#");
		print(ref.getCodePoint());
		print(";");
	}

	public void visit(XmlEntityRef ref) throws IOException {
		print("&");
		print(ref.getName());
		print(";");
	}

	public void visit(DefinitionList n) throws IOException {
		printNewline(false);
		print("<dl>");
		printNewline(false);
		incIndent("\t");
		iterate(n.getContent());
		decIndent();
		printNewline(false);
		print("</dl>");
		printNewline(false);
	}

	public void visit(DefinitionTerm n) throws IOException {
		printNewline(false);
		print("<dt>");
		iterate(n.getContent());
		print("</dt>");
		printNewline(false);
	}

	public void visit(DefinitionDefinition n) throws IOException {
		printNewline(false);
		print("<dd>");
		iterate(n.getContent());
		print("</dd>");
		printNewline(false);
	}

	public void visit(Enumeration n) throws IOException {
		printNewline(false);
		print("<ol>");
		printNewline(false);
		incIndent("\t");
		iterate(n.getContent());
		decIndent();
		printNewline(false);
		print("</ol>");
		printNewline(false);
	}

	public void visit(EnumerationItem n) throws IOException {
		printNewline(false);
		print("<li>");
		iterate(n.getContent());
		print("</li>");
		printNewline(false);
	}

	public void visit(Itemization n) throws IOException {
		printNewline(false);
		print("<ul>");
		printNewline(false);
		incIndent("\t");
		iterate(n.getContent());
		decIndent();
		printNewline(false);
		print("</ul>");
		printNewline(false);
	}

	public void visit(ItemizationItem n) throws IOException {
		printNewline(false);
		print("<li>");
		iterate(n.getContent());
		print("</li>");
		printNewline(false);
	}

	public void visit(ExternalLink link) throws IOException {
System.out.println("ExternalLink:"+link);
		print("<a href=\"");
		print(link.getTarget().getProtocol());
		print(":");
		print(link.getTarget().getPath());
		print("\">");
		if (!link.getTitle().isEmpty()) {
			iterate(link.getTitle());
		} else {
			printExternalLinkNumber(link);
		}
		print("</a>");
	}

	public void visit(Url url) throws IOException {
		print("<a href=\"");
		print(url.getProtocol());
		print(":");
		print(url.getPath());
		print("\">");
		print(url.getProtocol());
		print(":");
		print(url.getPath());
		print("</a>");
	}

	public void visit(InternalLink n) throws IOException { //@@@MS muss noch sauber gemacht werden
System.out.println("InternalLink:"+n.getTarget());
		print("<a href=\"");
		String linkTarget = makeLinkTarget(n);
		if( linkTarget.indexOf(":") == -1){
			if( linkTarget.indexOf("#") == -1){
				linkTarget = "loc:"+linkTarget;
			}
		}
		String s[] = linkTarget.split(",");
		if( s.length == 2){
			print(s[0]+"\" ");
			print("target=\""+s[1]);
		}else{
			print(linkTarget);
		}
		print("\">");
		print(n.getPrefix());
		if (n.getTitle().getContent().isEmpty()) {
			print(makeLinkTitle(n));
		} else {
			iterate(n.getTitle().getContent());
		}
		print(n.getPostfix());
		print("</a>");
	}

	private String getVAlign(ImageLink n){
		if( n.getVAlign() == null) return null;
		if( "none".equals(n.getVAlign().asKeyword().toLowerCase())) return null;
		if( "".equals(n.getVAlign().asKeyword().toLowerCase())) return null;
		return n.getVAlign().asKeyword().toLowerCase();
	}
	private String getHAlign(ImageLink n){
		if( n.getHAlign() == null) return null;
		if( "none".equals(n.getHAlign().asKeyword().toLowerCase())) return null;
		if( "".equals(n.getHAlign().asKeyword().toLowerCase())) return null;
		return n.getHAlign().asKeyword().toLowerCase();
	}
	public void visit(ImageLink n) throws IOException {
		System.out.println("ImageLink.target1:"+n.getTarget()+"/"+getHAlign(n)+"/"+getVAlign(n));
		print("<img src=\"");
		print(n.getTarget().replace(":","%3a"));
		print("\"");
		String halign = getHAlign(n);
		String valign = getVAlign(n);
		if (halign != null || valign != null){
			print(" style=\"");
			if (halign != null){
				print("float:"+halign+";");
			}
			if (valign != null){
				print("vertical-align:"+valign+";");
			}
			print("\"");
		}
		if (n.getWidth() >= 0 || n.getHeight() >= 0){
			if (n.getWidth() >= 0){
				print(" width=\"");
				print(n.getWidth());
				print("\"");
			}
			if (n.getHeight() >= 0){
				print(" height=\"");
				print(n.getHeight());
				print("\"");
			}
		}
		if (!n.getAlt().isEmpty()) {
			print(" alt=\"");
			iterate(n.getAlt());
			print("\"");
		}
		print("/>");
	}

	public void visit(Table table) throws IOException {
		printNewline(false);
		print("<table");
		iterate(table.getXmlAttributes());
		print(">");
		printNewline(false);
		incIndent("\t");
		iterate(table.getBody());
		decIndent();
		printNewline(false);
		printNewline(true);
		print("</table>");
		printNewline(false);
	}

	public void visit(TableCaption caption) throws IOException {
		printNewline(false);
		print("<caption");
		iterate(caption.getXmlAttributes());
		print(">");
		printNewline(false);
		incIndent("\t");
		iterate(caption.getBody());
		decIndent();
		printNewline(false);
		print("</caption>");
		printNewline(false);
	}

	public void visit(TableRow row) throws IOException {
		printNewline(false);
		print("<tr");
		iterate(row.getXmlAttributes());
		print(">");
		printNewline(false);
		incIndent("\t");
		iterate(row.getBody());
		decIndent();
		printNewline(false);
		print("</tr>");
		printNewline(false);
	}

	public void visit(TableHeader header) throws IOException {
		printNewline(false);
		print("<th");
		iterate(header.getXmlAttributes());
		print(">");
		printNewline(false);
		incIndent("\t");
		iterate(header.getBody());
		decIndent();
		printNewline(false);
		print("</th>");
		printNewline(false);
	}

	public void visit(TableCell cell) throws IOException {
		printNewline(false);
		print("<td");
		iterate(cell.getXmlAttributes());
		print(">");
		printNewline(false);
		incIndent("\t");
		iterate(cell.getBody());
		decIndent();
		printNewline(false);
		print("</td>");
		printNewline(false);
	}

	public void visit(HorizontalRule rule) throws IOException {
		printNewline(false);
		print("<hr />");
		printNewline(false);
	}

	public void visit(Signature sig) throws IOException {
		print("<span class=\"");
		print(classPrefix);
		print("signature\">");
		print(makeSignature(sig));
		print("</span>");
	}

	public void visit(Redirect n) throws IOException {
		print("<span class=\"");
		print(classPrefix);
		print("redirect\">&#x21B3; ");
		print(n.getTarget());
		print("</span>");
	}

	public void visit(IllegalCodePoint n) throws IOException {
		print("<span class=\"");
		print(classPrefix);
		print("illegal\">");
		print(asXmlCharRefs(n.getCodePoint()));
		print("</span>");
	}

	public void visit(MagicWord n) throws IOException {
		print("<span class=\"");
		print(classPrefix);
		print("magic-word\">__");
		print(n.getWord());
		print("__</span>");
	}

	public void visit(TagExtension n) throws IOException {
		print("<span class=\"");
		print(classPrefix);
		print("unknown-node\">");
		if (n.getBody().isEmpty()) {
			print("&lt;");
			print(n.getName());
			iterate(n.getXmlAttributes());
			print(" />");
		} else {
			print("&lt;");
			print(n.getName());
			iterate(n.getXmlAttributes());
			print(">");
			print(escHtml(n.getBody()));
			print("&lt;/");
			print(n.getName());
			print(">");
		}
		print("</span>");
	}

	public void visit(XmlElementEmpty e) throws IOException {
		print("<span class=\"");
		print(classPrefix);
		print("unknown-node\">");
		print("&lt;");
		print(e.getName());
		iterate(e.getXmlAttributes());
		print(" />");
		print("</span>");
	}

	public void visit(XmlElementOpen e) throws IOException {
		print("<span class=\"");
		print(classPrefix);
		print("unknown-node\">");
		print("&lt;");
		print(e.getName());
		iterate(e.getXmlAttributes());
		print(">");
		print("</span>");
	}

	public void visit(XmlElementClose e) throws IOException {
		print("<span class=\"");
		print(classPrefix);
		print("unknown-node\">");
		print("&lt;/");
		print(e.getName());
		print(">");
		print("</span>");
	}

	public void visit(Template tmpl) throws IOException {
		if( ((Text)tmpl.getName().get(0)).getContent().equals("anchor")){
			TemplateArgument[] args  = tmpl.getArgs().toArray(new TemplateArgument[0]);
			for(int i=0;i< args.length;i++){
				TemplateArgument arg = args[i];
				print("<span id=\"");
				iterate(arg);
				print("\">");
				print("</span>");
			}
		}else{
			print("<span class=\"");
			print(classPrefix);
			print("unknown-node\">");
			print("{");
			print("{");
			iterate(tmpl.getName());
			iterate(tmpl.getArgs());
			print("}}");
			print("</span>");
		}
	}

	public void visit(TemplateArgument arg) throws IOException {
		print("|");
		if (!arg.getHasName()) {
			iterate(arg.getValue());
		} else {
			iterate(arg.getName());
			print("=");
			iterate(arg.getValue());
		}
	}

	public void visit(TemplateParameter param) throws IOException {
		print("<span class=\"");
		print(classPrefix);
		print("unknown-node\">");
		if (renderTemplates) {
			print("{");
			print("{");
			print("{");
			iterate(param.getName());
			dispatch(param.getDefaultValue());
			iterate(param.getGarbage());
			print("}}}");
		} else {
			if (param.getDefaultValue() == null) {
				print("{");
				print("{");
				print("{");
				iterate(param.getName());
				print("}}}");
			} else {
				print("{");
				print("{");
				print("{");
				iterate(param.getName());
				print("|...}}}");
			}
		}
		print("</span>");
	}


	// =========================================================================
	private String classPrefix;

	private String articleTitle = "";

	private boolean renderTemplates = true;

	private boolean renderTagExtensions = true;

	private List<ExternalLink> numberedLinks = new ArrayList<ExternalLink>();

	private String cssLink;

	private File cssFile;

	private String cssResource;

	// ===========================================================================
	public SwebleHtml(Writer writer, String articleTitle) {
		super(writer);
		this.articleTitle = articleTitle;
		setCssResource("SwebleHtml.css", "");
	}

	// ===========================================================================
	private void setClassPrefix(String classPrefix) {
		if (classPrefix != null) {
			this.classPrefix = classPrefix;
			if (!classPrefix.isEmpty())
				this.classPrefix += '-';
		}
	}


	public String getCssLink() {
		return cssLink;
	}

	public void setCssLink(String cssLink, String classPrefix) {
		this.cssFile = null;
		this.cssResource = null;
		this.cssLink = cssLink;
		setClassPrefix(classPrefix);
	}

	public File getCssFile() {
		return cssFile;
	}

	public void setCssFile(File cssFile, String classPrefix) {
		this.cssResource = null;
		this.cssLink = null;
		this.cssFile = cssFile;
		setClassPrefix(classPrefix);
	}

	public String getCssResource() {
		return cssResource;
	}

	public void setCssResource(String cssResource, String classPrefix) {
		this.cssFile = null;
		this.cssLink = null;
		this.cssResource = cssResource;
		setClassPrefix(classPrefix);
	}

	public void setRenderTemplates(boolean renderTemplates) {
		this.renderTemplates = renderTemplates;
	}

	public void setRenderTagExtensions(boolean renderTagExtensions) {
		this.renderTagExtensions = renderTagExtensions;
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

	private void printExternalLinkNumber(ExternalLink link) {
		numberedLinks.add(link);
		print(numberedLinks.size());
	}

	private String makeLinkTitle(InternalLink n) {
		return n.getTarget();
	}

	private String makeLinkTarget(InternalLink n) {
		return n.getTarget();
	}

	private String makeSignature(Signature sig) {
		return "[SIG]";
	}
}
