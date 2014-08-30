/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.ms123.common.utils;
 

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
 
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.*;
 

public  final class MapNamespaceContext implements NamespaceContext {
	private Map<String, String> namespaces = new HashMap<String, String>();
	private Node targetNode;
	
	private static final String XMLNAMESPACE = "xmlns";
 
	public MapNamespaceContext() {
		super();
	}
 
	public MapNamespaceContext(final Map<String, String> ns) {
		this();
		this.namespaces = ns;
	}
 
	public MapNamespaceContext(final Node node) {
		this();
		this.targetNode = node;
	}
 
	public void setTargetNode(final Node node) {
		this.targetNode = node;
	}
 
	public void addNamespace(final String prefix, final String namespaceURI) {
		this.namespaces.put(prefix, namespaceURI);
	}
 
	public void addNamespaces(final Map<String, String> ns) {
		this.namespaces.putAll(ns);
	}
 
	public String getNamespaceURI(String prefix) {
		if (null == prefix) {
			throw new IllegalArgumentException("Null prefix to getNamespaceURI");
		}
		if (XMLConstants.XML_NS_PREFIX.equals(prefix)) {
			return XMLConstants.XML_NS_URI;
		}
		if (XMLConstants.XMLNS_ATTRIBUTE.equals(prefix)) {
			return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
		}
		// if we have a target node, facts-on-the-ground in its parent tree take precedence.
		if (targetNode != null) {
			String uri = getNamespace(targetNode, prefix);
			if (uri != null) {
				return uri;
			}
             
		}
		return namespaces.get(prefix);
	}
 
	public String getPrefix(String namespaceURI) {
		if (namespaceURI == null) {
			throw new IllegalArgumentException("Null namespace to getPrefix");
		}
		if (XMLConstants.XML_NS_URI.equals(namespaceURI)) {
			return XMLConstants.XML_NS_PREFIX;
		}
		if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(namespaceURI)) {
			return XMLConstants.XMLNS_ATTRIBUTE;
		}
 
		for (Map.Entry<String, String> e : namespaces.entrySet()) {
			if (e.getValue().equals(namespaceURI)) {
				return e.getKey();
			}
		}
		return null;
	}
 
	public Iterator getPrefixes(String namespaceURI) {
		return null;
	}
 
	public Map<String, String> getUsedNamespaces() {
		return namespaces;
	}

	/**
	 * Starting from a node, find the namespace declaration for a prefix. for a matching namespace
	 * declaration.
	 *
	 * @param node search up from here to search for namespace definitions
	 * @param searchPrefix the prefix we are searching for
	 * @return the namespace if found.
	 */
	public static String getNamespace(Node node, String searchPrefix) {
 
		Element el;
		while (!(node instanceof Element)) {
			node = node.getParentNode();
		}
		el = (Element) node;
 
		NamedNodeMap atts = el.getAttributes();
		for (int i = 0; i < atts.getLength(); i++) {
			Node currentAttribute = atts.item(i);
			String currentLocalName = currentAttribute.getLocalName();
			String currentPrefix = currentAttribute.getPrefix();
			if (searchPrefix.equals(currentLocalName) && XMLNAMESPACE.equals(currentPrefix)) {
				return currentAttribute.getNodeValue();
			} else if (isEmpty(searchPrefix) && XMLNAMESPACE.equals(currentLocalName) && isEmpty(currentPrefix)) {
				return currentAttribute.getNodeValue();
			}
		}
 
		Node parent = el.getParentNode();
		if (parent instanceof Element) {
			return getNamespace((Element) parent, searchPrefix);
		}
 
		return null;
	}

	private static boolean isEmpty(String s) {
		if (s == null || s.length() == 0) {
			return true;
		}
		return false;
	}
}

