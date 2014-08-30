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
package org.ms123.common.importing;

import flexjson.JSONSerializer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import org.milyn.container.ExecutionContext;
import org.milyn.delivery.sax.DefaultSAXElementSerializer;
import org.milyn.delivery.sax.SAXElement;
import org.ms123.common.libhelper.Inflector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;

@SuppressWarnings("unchecked")
public class XmlTreeBuilder extends DefaultSAXElementSerializer {

	private static final Logger m_logger = LoggerFactory.getLogger(XmlTreeBuilder.class);

	protected Inflector m_inflector = Inflector.getInstance();

	private JSONSerializer m_js = new JSONSerializer();

	private Map<SAXElement, Map> m_parentMap = new HashMap();

	private SAXElement m_rootNode = null;

	private List<String> m_visitedList = new ArrayList();

	public void visitBefore(SAXElement element, ExecutionContext executionContext) {
		String path = getPath(element);
		if (isVisited(path)) {
			return;
		}
		m_visitedList.add(path);
		System.out.println("PAth:" + path);
		String etag = element.getName().toString();
		Map parentMap = null;
		if (m_rootNode == null) {
			m_rootNode = element;
		} else {
			parentMap = m_parentMap.get(element.getParent());
		}
		Map map = new HashMap();
		map.put("value", etag);
		map.put("title", etag);
		map.put("type", "element");
		List<Map> childs = new ArrayList();
		map.put("children", childs);
		m_parentMap.put(element, map);
		Attributes attrs = element.getAttributes();
		for (int i = 0; i < attrs.getLength(); i++) {
			Map amap = new HashMap();
			amap.put("value", attrs.getQName(i).toString());
			amap.put("title", attrs.getQName(i).toString());
			amap.put("type", "attr");
			amap.put("children", new ArrayList());
			childs.add(amap);
		}
		if (parentMap != null) {
			List<Map> children = (List) parentMap.get("children");
			children.add(map);
		}
	}

	public void visitAfter(SAXElement element, ExecutionContext executionContext) {
	}

	private boolean isVisited(String path) {
		for (String p : m_visitedList) {
			if (p.startsWith(path)) {
				return true;
			}
		}
		return false;
	}

	private String getPath(SAXElement element) {
		List<SAXElement> list = new ArrayList();
		SAXElement p = element;
		while (p != null) {
			list.add(p);
			p = p.getParent();
		}
		Collections.reverse(list);
		String path = "";
		for (SAXElement e : list) {
			path += "/" + e.getName().toString();
		}
		return path;
	}

	public Map getTreeMap() {
		return m_parentMap.get(m_rootNode);
	}
}
