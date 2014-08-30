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
import java.util.Stack;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.Writer;
import java.io.Reader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.StringWriter;
import java.io.StringReader;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import nu.xom.*;
import org.zkoss.zuss.Zuss;
import org.zkoss.zuss.Resolver;
import org.zkoss.zuss.Locator;
import org.zkoss.zuss.metainfo.ZussDefinition;
import org.zkoss.zuss.impl.in.Parser;
import org.zkoss.zuss.impl.out.Translator;
import org.ms123.common.docbook.xom.html5.*;
import org.ms123.common.data.api.DataLayer;
import org.osgi.framework.BundleContext;
import eu.bitwalker.useragentutils.*;
import org.apache.commons.beanutils.PropertyUtils;

/**
 *
 */
@SuppressWarnings("unchecked")
class WebsiteBuilder extends BaseBuilder {

	WebsiteBuilder(DataLayer dataLayer, BundleContext bc) {
		super(dataLayer, bc);
	}

	protected void getWebsiteStart(String serverName, String namespace, String name, String json, OutputStream out, Map<String, Object> paramsIn, Map<String, String> paramsOut) throws Exception {
		try{
			Html html = new Html();
			Head head = new Head();
			html.setHead(head);
			Title title = new Title("XXX");
			head.add(title);
			head.add(new Meta());
			//head.add(new Meta("viewport", "width=device-width,initial-scale=2"));
			head.add(new BaseE( "http://"+serverName+"/sw/"+namespace+"/"));
			Script script = new Script("/sw/"+namespace+"/legacy/js/jquery-2.0.3.js.gz");
			head.getScripts().add(script);
			script = new Script("/sw/"+namespace+"/legacy/js/etc.js.gz");
			head.getScripts().add(script);
			script = new Script("/sw/"+namespace+"/website/build/script/website.js.gz");
			head.getScripts().add(script);
			Link link = new Link(null, "/sw/"+namespace+"/legacy/css/style.css");
			head.getLinks().add(link);
			Style style = new Style();
			Map rootShape = (Map) m_ds.deserialize(json);
			Map properties = (Map) rootShape.get("properties");
			title.setTitle(getString(properties, "ws_title", name));
			Link lfi = new Link("shortcut icon", "repo%3Afavicon.png");
			lfi.setType("image/png");
			head.add(lfi);
			Body body = new Body();
			Div div = new Div();
			div.setId("ms123id");
			body.add(div);
			div.addAttribute("space", namespace);
			div.addAttribute("pagename", name);
			html.setBody(body);
			WebContext ctx = new WebContext(namespace, name, true, paramsIn, paramsOut);
			traverseElement(ctx, rootShape);
			for (Style s : ctx.styleList) {
				head.add(s);
			}
			for (Script s : ctx.scriptList) {
				head.getScripts().add(s);
			}
			marshall(html, out);
		}catch(Exception e){
			e.printStackTrace();
			String msg = e.getMessage();
			Html html = new Html();
			Head head = new Head();
			html.setHead(head);
			Title title = new Title("Error");
			head.add(title);
			Body body = new Body();
			html.setBody(body);
			body.add(msg);	
			marshall(html, out);
		}
	}

	protected Map getWebsiteMain(String namespace, String name, String json, Map<String, Object> paramsIn, Map<String, String> paramsOut) throws Exception {
		Map rootShape = (Map) m_ds.deserialize(json);
		Map properties = (Map) rootShape.get("properties");
		properties.put("ws_css", null);
		properties.put("ws_js", null);
		List<Map> childShapes = (List) rootShape.get("childShapes");
		getParamsOut(rootShape, paramsOut);
		sortListByPosition(childShapes);
		int size = childShapes.size();
		List<Map> pageElements = getPageElements(childShapes);
		WebContext ctx = new WebContext(namespace, name, true, paramsIn, paramsOut);
		handleElement(ctx, rootShape);
		removeSecondaryGroups(ctx, rootShape);
		return rootShape;
	}

	protected Map getWebsiteFragment(String namespace, String name, String json, String shapeId, String resourceId, Map<String, Object> paramsIn, Map<String, String> paramsOut) throws Exception {
		System.out.println("getWebsiteFragment");
		Map rootShape = (Map) m_ds.deserialize(json);
		WebContext ctx = new WebContext(namespace, name, false, paramsIn, paramsOut);
		Map fragmentShape = (resourceId == null) ? getElementByShapeId(ctx, rootShape, shapeId) : getElementByResourceId(ctx, rootShape, resourceId);
		String id = getString((Map) fragmentShape.get("properties"), "ws_id", null);
		String resId = (String) fragmentShape.get("resourceId");
		System.out.println("getWebsiteFragment.id:" + resId);
		if (fragmentShape == null) {
			throw new RuntimeException("WebsiteBuilder.getWebsiteFragment:" + name + "." + shapeId + " not found");
		}
		handleElement(ctx, fragmentShape);
		//for (Style s : ctx.styleList) {
		//	root.add(s);
		//}
		return fragmentShape;
	}

	protected Map getWebsitePage(String namespace, String name, String content) throws Exception {
		Context ctx = new Context(namespace, name, false, new HashMap(), new HashMap());
		Map properties = new HashMap();
		properties.put("ws_markdown",content);
		return createHtmlFromMarkdown(ctx, properties);
	}

	private void removeSecondaryGroups(Context ctx, Map shape) throws Exception {
		List<Map> childShapes = (List) shape.get("childShapes");
		List rmList = new ArrayList();
		for (Map child : childShapes) {
			if ("secondarygroup".equals(getStencilId(child))) {
				rmList.add(child);
			}
		}
		for (Object rm : rmList) {
			childShapes.remove(rm);
		}
	}

	private void traverseElement(WebContext ctx, Map shape) throws Exception {
		List<Map> childShapes = (List) shape.get("childShapes");
		String sid = getStencilId(shape);
		Map properties = (Map) shape.get("properties");
		ctx.propertyStack.push(properties);
		ctx.addCss(properties);
		if( "xwebsite".equals(sid)){
			ctx.addJs(properties);
		}
		boolean atStartpage = getBoolean(properties, "ws_atstartpage", true);
		if (ctx.isStartpage && !atStartpage) {
			return;
		}
		for (Map child : childShapes) {
			properties = (Map) child.get("properties");
			atStartpage = getBoolean(properties, "ws_atstartpage", true);
			if (!atStartpage) {
				continue;
			}
			traverseElement(ctx, child);
		}
		ctx.propertyStack.pop();
	}

	private void handleElement(WebContext ctx, Map shape) throws Exception {
		Map properties = (Map) shape.get("properties");
		boolean atStartpage = getBoolean(properties, "ws_atstartpage", true);
		if (ctx.isStartpage && !atStartpage) {
			properties.clear();
			return;
		}
		ctx.addCss(properties);
		ctx.addJs(properties);
		String sid = getStencilId(shape);
		System.out.println("handleElement:"+sid);
		String id = getString(properties, "ws_id", null);
		if (isEmpty(id)) {
			id = ctx.nextId();
		}
		properties.put("ws_id", id);
		getParamsOut(shape, ctx.paramsOut);
		if ("autogentable".equals(sid)) {
			String filter = (String) properties.get("ws_filter");
			List<Map> columns = (List) ((Map) properties.get("ws_columns")).get("items");
		} else if ("menu".equals(sid)) {
			String menuType = (String) properties.get("ws_menutype");
			if( "html".equals(menuType)){
				Base ret = createMenuHtml(ctx, shape, true);
				if (ret != null) {
					String html = Utils.xomToString(ret.toXom());
					System.out.println("MenuHtml:"+html);
					properties.put("ws_html", "<div id=\"" + id + "\">" + html + "</div>");
				}
			}
			if( "tree".equals(menuType)){
				Map menu = new HashMap();
				menu.put("label","root");
				createMenuJson(menu, ctx, shape);
				m_js.prettyPrint(true);
				String tree = m_js.deepSerialize(menu);
				System.out.println("MenuJson:"+tree);
				properties.put("ws_tree", tree);
			}
		} else if ("htmlarea".equals(sid)) {
			String html = createHtmlFromHtml(ctx, properties);
			properties.put("ws_html", html);
		} else if ("textarea".equals(sid) || "menuitem".equals(sid) || "submenu".equals(sid)) {
			Map ret = createHtmlFromMarkdown(ctx, properties);
			properties.put("ws_html", ret.get("html"));
			properties.put("ws_markdown", "");
		} else if ("pageelement".equals(sid)) {
		}
		List<Map> childShapes = (List) shape.get("childShapes");
		sortListByX(childShapes);
		for (Map child : childShapes) {
			properties = (Map) child.get("properties");
			atStartpage = getBoolean(properties, "ws_atstartpage", true);
			if (!atStartpage) {
				properties.clear();
				continue;
			}
			handleElement(ctx, child);
		}
	}

	private Map createHtmlFromMarkdown(Context ctx, Map properties) throws Exception {
		long starttime = new java.util.Date().getTime();
		String markdown = (String) properties.get("ws_markdown");
		String filter = (String) properties.get("ws_filter");
		markdown = applyGroovy(ctx.namespace, markdown, filter, ctx.paramsIn, false);
		System.out.println("createHtmlFromMarkdown.after_groovy:"+markdown);
		String html = xwikiToHtml(ctx,markdown);
		Map ret = new HashMap();
		ret.put("html",html);
		ret.put("toc",ctx.get("toc"));
		ret.put("js",ctx.get("jsList"));
		System.out.println("createHtmlFromMarkdown:"+html);
		return ret;
	}

	private String createHtmlFromHtml(Context ctx, Map properties) throws Exception {
		String html = (String) properties.get("ws_html");
		Element rootElement = null;
		try {
			html = applyGroovy(ctx.namespace, html, null, ctx.paramsIn, false);
			Document doc = new Builder().build("<div><div>" + html + "</div></div>", null);
			rootElement = (Element) doc.getRootElement().getChild(0);
			rootElement.detach();
			Utils.escImageSrc(rootElement);
		} catch (Throwable t) {
			t.printStackTrace();
			rootElement = new Element("div");
			while (t instanceof RuntimeException) {
				if (t.getCause() == null) {
					break;
				}
				t = t.getCause();
			}
			rootElement.appendChild(t.getMessage());
		}
		return Utils.xomToString(rootElement);
	}

	private Base createMenuHtml(Context ctx, Map shape, boolean isMainMenu) {
		if (shape == null) {
			return null;
		}
		List<Map> childShapes = (List) shape.get("childShapes");
		List<String> types = new ArrayList();
		types.add("menuitem");
		types.add("submenu");
		List<Map> menuitemShapes = getShapeList(childShapes, types);
		sortListByPosition(menuitemShapes);
		if (menuitemShapes.size() == 0) {
			return null;
		}
		Ul ul = new Ul();
		if (isMainMenu) {
			ul.setClass("main");
		} else {
			ul.setClass("sub");
		}
		for (Map menuitemShape : menuitemShapes) {
			Map properties = (Map)menuitemShape.get("properties");
			if(!getBoolean(properties, "ws_enabled", true)) continue;
			Li li = new Li();
			ul.add(li);
			if ("submenu".equals(getStencilId(menuitemShape))) {
				List<Map> subMenushapes = (List) menuitemShape.get("childShapes");
				if (subMenushapes.size() > 0) {
					String subText = getString((Map) menuitemShape.get("properties"), "ws_name", null);
					if (!isEmpty(subText)) {
						A a = new A("#", subText);
//						setHrefAndTarget(a, ctx,menuitemShape,"");
						li.add(a);
					}
					Base ret = createMenuHtml(ctx, menuitemShape, false);
					if (ret != null) {
						li.add(ret);
					}
				}
			} else {
				A a = new A("","");
				li.add(a);
				String name = getString((Map) menuitemShape.get("properties"), "ws_name", "");
				a.setLabel(name);

				setHrefAndTarget(a, ctx,menuitemShape,"");
				setHrefAndTarget(a, ctx,menuitemShape,"1");
				setHrefAndTarget(a, ctx,menuitemShape,"2");

			}
			li = null;
		}
		return ul;
	}
	private void createMenuJson(Map root, Context ctx, Map shape) {
		if (shape == null) {
			return;
		}
		List<Map> childShapes = (List) shape.get("childShapes");
		List<String> types = new ArrayList();
		types.add("menuitem");
		types.add("submenu");
		List<Map> menuitemShapes = getShapeList(childShapes, types);
		sortListByPosition(menuitemShapes);

		List childs = (List)root.get("children");
		if( childs == null){
			childs = new ArrayList();
			root.put("children", childs);
		}
		for (Map menuitemShape : menuitemShapes) {
			Map properties = (Map)menuitemShape.get("properties");
			if(!getBoolean(properties, "ws_enabled", true)) continue;
			if ("submenu".equals(getStencilId(menuitemShape))) {
				List<Map> subMenushapes = (List) menuitemShape.get("childShapes");
				Map sub = new HashMap();
				sub.put("children", new ArrayList());

				String name = getString((Map) menuitemShape.get("properties"), "ws_name", null);
				if (!isEmpty(name)) {
					sub.put("name", name);
					sub.put("label", name);
					setHrefAndTarget(sub, ctx,menuitemShape,"");
				}else{
					sub.put("name", "");
					sub.put("label", "");
				}
				childs.add( sub );
				createMenuJson(sub, ctx, menuitemShape);
			} else {
				Map child = new HashMap();
				String name = getString((Map) menuitemShape.get("properties"), "ws_name", "");
				Boolean sb = getBoolean((Map) menuitemShape.get("properties"), "ws_scrollbar", true);
				child.put("name", name);
				child.put("label", name);
				child.put("scrollbar", sb);

				setHrefAndTarget(child, ctx,menuitemShape,"");
				setHrefAndTarget(child, ctx,menuitemShape,"1");
				setHrefAndTarget(child, ctx,menuitemShape,"2");
				childs.add( child);
			}
		}
	}
	private void setHrefAndTarget(Object a, Context ctx, Map menuitemShape, String num){
		String linkType = getString((Map) menuitemShape.get("properties"), "ws_linktype"+num, null);
		if( linkType == null ) return;
		linkType = linkType.toLowerCase();
		String locationId = getString((Map) menuitemShape.get("properties"), "ws_locationid"+num, null);
		String target = getString((Map) menuitemShape.get("properties"), "ws_target"+num, null);
		if( locationId == null && target == null ) return;
		String contentid="";
		if( "contentid".equals(linkType)){
			String contentId = getString((Map) menuitemShape.get("properties"), "ws_contentid"+num, "");
			if (contentId.indexOf(".") == -1 && contentId.indexOf(":") == -1){
				contentId = ctx.pageName + "." + contentId;
			}else{
				contentId = contentId.replace(':','.');
			}
			setAttribute(a, "href"+num, "loc:"+contentId);
		}else if( "content".equals(linkType)){
			String resourceId = getString((Map) menuitemShape, "resourceId", "");
			setAttribute(a, "href"+num, "res:"+resourceId);
			//a.setResourceId(resourceId);
		}else if( "workflow".equals(linkType)){
			String wf = getString((Map) menuitemShape.get("properties"), "ws_workflow"+num, "");
			setAttribute(a, "href"+num, "wf:"+wf);
		}else if( "js".equals(linkType)){
			String js = getString((Map) menuitemShape.get("properties"), "ws_js"+num, "");
			setAttribute(a, "href"+num, "js:"+js);
		}else if( "swf".equals(linkType)){
			String swf = getString((Map) menuitemShape.get("properties"), "ws_swf"+num, "");
			setAttribute(a, "href"+num, "swf:"+swf);
		}else if( "pdf".equals(linkType)){
			String pdf = getString((Map) menuitemShape.get("properties"), "ws_pdf"+num, "");
			setAttribute(a, "href"+num, "pdf:"+pdf);
		}else if( "webpage".equals(linkType)){
			String wp = getString((Map) menuitemShape.get("properties"), "ws_webpage"+num, "");
			setAttribute(a, "href"+num, "wp:"+wp);
		}else if( "internet".equals(linkType)){
			String url = getString((Map) menuitemShape.get("properties"), "ws_url"+num, "");
			setAttribute(a, "href"+num, url);
		}
		setAttribute(a, "target"+num, locationId != null ? locationId : target);
	}

	private void setAttribute(Object o, String attr, String value) {
		try {
			if( o instanceof Map){
				((Map)o).put(attr,value);
			}else{
				PropertyUtils.setProperty(o, attr, value);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private Map getElementByShapeId(Context ctx, Map<String, Object> shape, String id) throws Exception {
		System.out.println("getElementByShapeId:" + id);
		Map properties = (Map) shape.get("properties");
		if (id.equals(getString(properties, "ws_id", null))) {
			return shape;
		}
		List<Map> childShapes = (List) shape.get("childShapes");
		for (Map child : childShapes) {
			Map b = getElementByShapeId(ctx, child, id);
			if (b != null)
				return b;
		}
		return null;
	}

	private Map getElementByResourceId(Context ctx, Map<String, Object> shape, String id) throws Exception {
		String resId = (String) shape.get("resourceId");
		System.out.println("getElementByResourceId:" + id + "/" + resId);
		if (id.equals(resId)) {
			return shape;
		}
		List<Map> childShapes = (List) shape.get("childShapes");
		for (Map child : childShapes) {
			Map b = getElementByResourceId(ctx, child, id);
			if (b != null)
				return b;
		}
		return null;
	}

	private void marshall(Base root, OutputStream out) {
		marshall(root, out, true);
	}

	private void marshall(Base element, OutputStream out, boolean withDoctype) {
		try {
			Document doc = new Document(element.toXom());
			if (withDoctype) {
				DocType doctype = new DocType("html");
				doc.insertChild(doctype, 0);
			}
			Serializer ser = new Serializer(out) {
				protected void writeXMLDeclaration() {
					System.out.println("writeXMLDeclaration");
				}
			};
			//ser.setIndent(2);
			//ser.setLineSeparator("\n");
			ser.write(doc);

			/*ser = new Serializer(System.out) {
				protected void writeXMLDeclaration() {
					System.out.println("writeXMLDeclaration");
				}
			};
			ser.setIndent(2);
			ser.setLineSeparator("\n");
			ser.write(doc);*/
		} catch (Exception e) {
			throw new RuntimeException("WebsiteBuilder.toHtml:", e);
		} finally {
		}
	}


	private void getParamsOut(Map shape, Map<String, String> paramsOut) {
		Map<String, Object> properties = (Map) shape.get("properties");
		for (String prop : properties.keySet()) {
			if (prop.startsWith("db_")) {
				String val = (String) properties.get(prop);
				paramsOut.put(prop.substring(3).replace("_", "."), val);
			}
		}
		System.out.println("getParamsOut.paramsOut:" + paramsOut);
	}

	protected List<Map> getPageElements(List<Map> childShapes) {
		List lines = new ArrayList();
		int size = childShapes.size();
		if (size == 0) {
			return lines;
		}
		for (int i = 0; i < size; i++) {
			Map child = (Map) childShapes.get(i);
			lines.add(child);
		}
		return lines;
	}



}
