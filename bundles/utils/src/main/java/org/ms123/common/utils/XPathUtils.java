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
package org.ms123.common.utils;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class XPathUtils {
	private static final XPathFactory FACTORY = XPathFactory.newInstance();
	private XPath xpath;

	public XPathUtils() {
		xpath = FACTORY.newXPath();
	}

	public XPathUtils(final Map<String, String> ns) {
		this();

		if (ns != null) {
			xpath.setNamespaceContext(new MapNamespaceContext(ns));
		}
	}

	public XPathUtils(final NamespaceContext ctx) {
		this();
		xpath.setNamespaceContext(ctx);
	}

	public Object getValue(String xpathExpression, Node node, QName type) {
		try {
			return xpath.evaluate(xpathExpression, node, type);
		} catch (Exception e) {
			return null;
		}
	}
	public List getList(String xpathExpression, Node node) {
		return filterNodeListElements(getValueList(xpathExpression, node));
	}

	public NodeList getValueList(String xpathExpression, Node node) {
		return (NodeList) getValue(xpathExpression, node, XPathConstants.NODESET);
	}

	public String getValueString(String xpathExpression, Node node) {
		return (String) getValue(xpathExpression, node, XPathConstants.STRING);
	}

	public Node getValueNode(String xpathExpression, Node node) {
		return (Node) getValue(xpathExpression, node, XPathConstants.NODE);
	}

	public boolean isExist(String xpathExpression, Node node, QName type) {
		return getValue(xpathExpression, node, type) != null;
	}

	public static XPathFactory getFactory() {
		return FACTORY;
	}

	/* ----------------------------------------------------------------------------------------*/
	
	/* DomStuff
	 /*----------------------------------------------------------------------------------------*/

	/**
	 * Returns a map of all node's attributes. All non-attribute nodes are ignored.
	 */
	public static Map<String, String> getAllAttributes(Node node) {
		HashMap<String, String> attrs = new HashMap<String, String>();
		NamedNodeMap nmm = node.getAttributes();
		for (int j = 0; j < nmm.getLength(); j++) {
			Node attribute = nmm.item(j);
			if (attribute.getNodeType() != Node.ATTRIBUTE_NODE) {
				continue;
			}
			attrs.put(attribute.getNodeName(), attribute.getNodeValue());
		}
		return attrs;
	}

	/**
	 * Returns attribute value of a node or <code>null</code> if attribute name not found.
	 * Specified attribute is searched on every call.
	 * Consider {@link #getAllAttributes(org.w3c.dom.Node)} for better performances.
	 */
	public static String getAttributeValue(Node node, String attrName) {
		NamedNodeMap nmm = node.getAttributes();
		for (int j = 0; j < nmm.getLength(); j++) {
			Node attribute = nmm.item(j);
			if (attribute.getNodeType() != Node.ATTRIBUTE_NODE) {
				continue;
			}
			String nodeName = attribute.getNodeName();
			if (nodeName.equals(attrName)) {
				return attribute.getNodeValue();
			}
		}
		return null;
	}

	/**
	 * Get element's attribute value or <code>null</code> if attribute not found or empty.
	 */
	public static String getAttributeValue(Element element, String name) {
		String value = element.getAttribute(name);
		if (value.length() == 0) {
			value = null;
		}
		return value;
	}

	// ---------------------------------------------------------------- nodelist

	/**
	 * Filters node list by keeping nodes of specified type.
	 */
	public static List filterNodeList(NodeList nodeList, short keepNodeType) {
		return filterNodeList(nodeList, keepNodeType, null);
	}

	/**
	 * Filters node list by keeping nodes of specified type and node name.
	 */
	public static List<Node> filterNodeList(NodeList nodeList, short keepNodeType, String nodeName) {
		List<Node> nodes = new ArrayList<Node>();
		for (int k = 0; k < nodeList.getLength(); k++) {
			Node node = nodeList.item(k);
			if (node.getNodeType() != keepNodeType) {
				continue;
			}
			if (nodeName != null && (node.getNodeName().equals(nodeName) == false)) {
				continue;
			}
			nodes.add(node);
		}
		return nodes;
	}

	/**
	 * Filter node list for all Element nodes.
	 */
	public static List filterNodeListElements(NodeList nodeList) {
		return filterNodeListElements(nodeList, null);
	}

	/**
	 * Filter node list for Element nodes of specified name.
	 */
	public static List<Node> filterNodeListElements(NodeList nodeList, String nodeName) {
		List<Node> nodes = new ArrayList<Node>();
		for (int k = 0; k < nodeList.getLength(); k++) {
			Node node = nodeList.item(k);
			if (node.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			if (nodeName != null && (node.getNodeName().equals(nodeName) == false)) {
				continue;
			}
			nodes.add(node);
		}
		return nodes;
	}

	/**
	 * Returns a list of all child Elements,
	 */
	public static List getChildElements(Node node) {
		return getChildElements(node, null);
	}

	/**
	 * Returns a list of child Elements of specified name.
	 */
	public static List getChildElements(Node node, String nodeName) {
		NodeList childs = node.getChildNodes();
		return filterNodeListElements(childs, nodeName);
	}

	// ---------------------------------------------------------------- node


	/**
	 * Returns value of first available child text node or <code>null</code> if not found.
	 */
	public static String getFirstChildTextNodeValue(Node node) {
		NodeList children = node.getChildNodes();
		int len = children.getLength();
		for (int i = 0; i < len; i++) {
			Node n = children.item(i);
			if (n.getNodeType() == Node.TEXT_NODE) {
				return n.getNodeValue();
			}
		}
		return null;
	}

	/**
	 * Returns value of single child text node or <code>null</code>.
	 */
	public static String getChildTextNodeValue(Node node) {
		if (node.getChildNodes().getLength() != 1) {
			return null;
		}
		Node item0 = node.getChildNodes().item(0);
		if (item0.getNodeType() != Node.TEXT_NODE) {
			return null;
		}
		return item0.getNodeValue();
	}

}

