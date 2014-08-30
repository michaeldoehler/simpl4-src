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

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import net.sf.sojo.common.ObjectUtil;
import net.sf.sojo.interchange.json.JsonSerializer;
import org.apache.commons.beanutils.PropertyUtils;
import org.milyn.container.ExecutionContext;
import org.milyn.delivery.DomModelCreator;
import org.milyn.delivery.DOMModel;
import org.milyn.delivery.sax.DefaultSAXElementSerializer;
import org.milyn.delivery.sax.SAXElement;
import org.ms123.common.data.api.SessionContext;
import org.ms123.common.libhelper.Inflector;
import org.apache.commons.lang3.StringUtils;
import org.ms123.common.utils.XPathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Element;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;

@SuppressWarnings("unchecked")
public class XmlImporter extends DomModelCreator {

	private static final Logger m_logger = LoggerFactory.getLogger(XmlImporter.class);

	protected Inflector m_inflector = Inflector.getInstance();

	private final String ENTITY = "entity";

	private Document m_document;

	private JsonSerializer m_sojoJson;

	private List<Map> m_defaults;

	private List<Map> m_mappings;

	private int m_max;

	private List<Object> m_resultList;

	private Map m_moduleTree;

	private ObjectUtil m_objUtils;

	private SessionContext m_sessionContext;

	private String m_mainEntityName;

	private String m_userName;

	private XPathUtils m_xpathUtils;

	private int m_num = 0;

	public XmlImporter() throws ParserConfigurationException {
		DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		m_document = documentBuilder.newDocument();
		m_xpathUtils = new XPathUtils();
		m_resultList = new ArrayList();
		m_sojoJson = new JsonSerializer();
		m_objUtils = new ObjectUtil();
	}

	public List<Object> getResultList() {
		return m_resultList;
	}

	public void setMappings(List<Map> mappings) {
		m_mappings = mappings;
	}

	public void setMax(int max) {
		m_max = max;
	}

	public void setModuleTree(Map moduleTree) {
		m_moduleTree = moduleTree;
	}

	public void setDefaults(List<Map> defaults) {
		m_defaults = defaults;
	}

	public void setMainEntityName(String mainEntityName) {
		m_mainEntityName = mainEntityName;
	}

	public void setUserName(String userName) {
		m_userName = userName;
	}

	public void setSessionContext(SessionContext sessionContext) {
		m_sessionContext = sessionContext;
	}

	public void visitAfter(SAXElement element, ExecutionContext executionContext) throws IOException {
		super.visitAfter(element, executionContext);
		if (m_max != -1 && m_num >= m_max)
			return;
		m_num++;
		DOMModel nodeModel = DOMModel.getModel(executionContext);
		Element domElement = null;
		Iterator it = nodeModel.getModels().keySet().iterator();
		if (it.hasNext()) {
			domElement = (Element) nodeModel.getModels().get(it.next());
		}
		if (domElement == null) {
			throw new RuntimeException("XmlImporter.visitAfter:rootElement is null");
		}
		try {
			Object o = createBeanGraph(m_sessionContext, m_moduleTree, domElement, m_mappings, m_defaults);
			System.out.println("=====>>>>beanGraph:" + m_sojoJson.serialize(o));
			m_resultList.add(o);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("XmlImporter.visitAfter:", e);
		}
	}

	private Object createBeanGraph(SessionContext sessionContext, Map<String, Object> moduleTree, Element rootElement, List<Map> mappings, List<Map> defaults) throws Exception {
		Stack<Element> parentElementStack = new Stack();
		parentElementStack.push(rootElement);
		return traverseModuleTree(null, parentElementStack, (String) moduleTree.get("name"), moduleTree);
	}

	private Object traverseModuleTree(Object parentBean, Stack<Element> parentElementStack, String path, Map<String, Object> moduleMap) throws Exception {
		String entityName = (String) moduleMap.get(ENTITY);
		System.out.println("===> traverseModuleTree.parentElementStack:" + parentElementStack + "\t\t|path:" + path + "\t\t|parent:" + parentBean);
		Map<String, String> wiring = getWiringForPath(path);
		System.out.println("wiring:" + path + "/" + wiring);
		if (wiring == null) {
			Map<String, String> w = getWiringForPathFromBinding(path);
			if (w == null) {
				return null;
			}
			wiring = w;
		}
		String targetType = wiring.get("targetType");
		System.out.println("targetType:" + targetType);
		Object newBean = null;
		if ("list".equals(targetType)) {
			List<Map> children = (List) moduleMap.get("children");
			System.out.println("\tchildren:" + children);
			if (children.size() > 0) {
				Map<String, Object> listType = children.get(0);
				List<Map> lchildren = (List) listType.get("children");
				Map<String, String> typeMapping = getWiringForPath(path + "." + listType.get("name"));
				List<Element> childElementList = getChildElements(parentElementStack, wiring);
				System.out.println("childElementList:" + childElementList);
				if (childElementList.size() > 0) {
					Set<Object> beanList = new HashSet();
					List<Map> bindings = getBindingsForPath(path + "." + listType.get("name"));
					System.out.println("\tbindings:" + bindings);
					for (Element childElement : childElementList) {
						parentElementStack.push(childElement);
						Map<String, Object> tmpMap = new HashMap();
						for (Map<String, String> binding : bindings) {
							String value = getSourceValue(parentElementStack, binding);
							tmpMap.put(getLastSegment(binding.get("target")), value);
						}
						evaluteFormulas((String) listType.get(ENTITY), tmpMap);
						Object bean = createBean((String) listType.get(ENTITY));
						m_objUtils.makeComplex(tmpMap, bean);
						beanList.add(bean);
						if (lchildren != null) {
							for (Map child : lchildren) {
								traverseModuleTree(bean, parentElementStack, path + "." + listType.get("name") + "." + child.get("name"), child);
							}
						}
						parentElementStack.pop();
					}
					PropertyUtils.setProperty(parentBean, (String) moduleMap.get("name"), beanList);
				}
			}
		} else {
			List<Map> bindings = getBindingsForPath(path);
			Map<String, Object> tmpMap = new HashMap();
			for (Map<String, String> binding : bindings) {
				String value = getSourceValue(parentElementStack, binding);
				tmpMap.put(getLastSegment(binding.get("target")), value);
			}
			evaluteFormulas((String) moduleMap.get(ENTITY), tmpMap);
			newBean = createBean((String) moduleMap.get(ENTITY));
			m_objUtils.makeComplex(tmpMap, newBean);
			if (parentBean != null) {
				PropertyUtils.setProperty(parentBean, (String) moduleMap.get("name"), newBean);
			}
			List<Map> children = (List) moduleMap.get("children");
			if (children != null) {
				for (Map child : children) {
					traverseModuleTree(newBean, parentElementStack, path + "." + child.get("name"), child);
				}
			}
		}
		return newBean;
	}

	private void evaluteFormulas(String entityName, Map data) throws Exception {
		data.put("_isnew", true);
		data.put("_user", m_userName);
		entityName = m_inflector.getEntityName(entityName);
		m_sessionContext.evaluteFormulas(entityName, data);
	}

	private Object createBean(String entityName) throws Exception {
		Class clazz = m_sessionContext.getClass(m_inflector.getClassName(entityName));
		Object bean = clazz.newInstance();
		Map defaults = getDefaults(entityName);
		if (defaults != null) {
			m_sessionContext.populate(defaults, bean);
		}
		return bean;
	}

	private String getElementPath(Element e) {
		StringBuilder sb = new StringBuilder();
		sb.append(e.getNodeName());
		Node n = e.getParentNode();
		while (n != null) {
			String s = n.getNodeName();
			if ("#document".equals(s)) {
				break;
			}
			sb.insert(0, s + "/");
			n = n.getParentNode();
		}
		String ret = sb.toString();
		int slash = ret.indexOf("/");
		if (slash != -1) {
			return ret.substring(slash + 1);
		}
		return ret;
	}

	private String removePart(String s1, String s2) {
		if (s1.startsWith(s2) && s1.length() > s2.length()) {
			return s1.substring(s2.length() + 1);
		}
		return s1;
	}

	private String removeLastDotSegment(String s1) {
		int lastDot = s1.lastIndexOf(".");
		if (lastDot != -1) {
			return s1.substring(0, lastDot);
		}
		return s1;
	}

	private List<Element> getChildElements(Stack<Element> parentElementStack, Map<String, String> wiring) {
		int size = parentElementStack.size();
		Element bestElement = null;
		String bestSource = null;
		for (int i = size - 1; i >= 0; i--) {
			Element element = parentElementStack.get(i);
			String p = getElementPath(element);
			String s = wiring.get("source");
			String source = removePart(s, p);
			if (bestSource == null || (countSegments(bestSource) > countSegments(source))) {
				if (m_xpathUtils.isExist(source, element, XPathConstants.NODESET)) {
					bestSource = source;
					bestElement = element;
				}
			}
		}
		System.out.println("\tgetChildElements.bestSource:" + bestSource);
		System.out.println("\tgetChildElements.bestElement:" + bestElement);
		return m_xpathUtils.getChildElements(m_xpathUtils.getValueNode(bestSource, bestElement));
	}

	private String getSourceValue(Stack<Element> parentElementStack, Map<String, String> binding) {
		int size = parentElementStack.size();
		Element bestElement = null;
		String bestSource = null;
		for (int i = size - 1; i >= 0; i--) {
			Element element = parentElementStack.get(i);
			String p = getElementPath(element);
			String s = binding.get("source");
			String source = removePart(s, p);
			if (bestSource == null || (countSegments(bestSource) > countSegments(source))) {
				if (m_xpathUtils.isExist(source, element, XPathConstants.NODE)) {
					bestSource = source;
					bestElement = element;
				}
			}
		}
		System.out.println("\tgetSourceValue.bestSource:" + bestSource);
		System.out.println("\tgetSourceValue.bestElement:" + bestElement);
		String value = m_xpathUtils.getValueString(bestSource, bestElement);
		System.out.println("getSourceValue.value:" + value);
		return value;
	}

	private Map<String, Object> getDefaults(String entityName) {
		entityName = m_inflector.getEntityName(entityName);
		for (Map m : m_defaults) {
			if (entityName.toLowerCase().equals((m_inflector.getEntityName((String) m.get("name"))).toLowerCase())) {
				return (Map) m.get("content");
			}
		}
		return null;
	}

	private int countDotSegments(String toCheck) {
		return StringUtils.countMatches(toCheck, ".");
	}

	private int countSegments(String toCheck) {
		return StringUtils.countMatches(toCheck, "/");
	}

	private Map<String, String> getWiringForPath(String path) throws Exception {
		for (Map<String, String> mapping : m_mappings) {
			String target = mapping.get("target");
			String targetType = mapping.get("targetType");
			if ("list".equals(targetType) || "object".equals(targetType)) {
				if (path.compareTo(target) == 0) {
					return mapping;
				}
			}
		}
		return null;
	}

	private List<Map> getBindingsForPath(String path) {
		System.out.println("\t\tgb.path:" + path + "/" + countDotSegments(path));
		int pathSegments = countDotSegments(path);
		int len = path.length();
		List<Map> ret = new ArrayList();
		for (Map<String, String> mapping : m_mappings) {
			String source = mapping.get("source");
			String target = mapping.get("target");
			String targetType = mapping.get("targetType");
			if (!("list".equals(targetType) || "object".equals(targetType))) {
				System.out.println("\t\t\tgb.target:" + target + "/" + countDotSegments(target));
				int targetSegments = countDotSegments(target);
				if ((targetSegments - pathSegments) == 1) {
					String x = removeLastDotSegment(target);
					System.out.println("\t\tx:" + x + "/s:" + m_inflector.singularize(x));
					if (path.compareTo(x) == 0) {
						ret.add(mapping);
					}
				}
			}
		}
		return ret;
	}

	private Map<String, String> getWiringForPathFromBinding(String path) {
		int len = path.length();
		for (Map<String, String> mapping : m_mappings) {
			String source = mapping.get("source");
			String target = mapping.get("target");
			String targetType = mapping.get("targetType");
			if (!("list".equals(targetType) || "object".equals(targetType))) {
				if (target.length() > len) {
					if (target.startsWith(path)) {
						String s = target.substring(path.length() + 1);
						System.out.println("s:" + s);
						if (s.indexOf(".") == -1) {
							Map<String, String> b = new HashMap();
							b.put("target", path);
							b.put("targetType", "object");
							return b;
						}
					}
				}
			}
		}
		return null;
	}

	private String getLastSegment(String path) {
		return getLastSegment(path, ".");
	}

	private String getLastSegment(String path, String sep) {
		int lastDot = path.lastIndexOf(sep);
		return path.substring(lastDot + 1);
	}

	private String prettyPrint(Element e) {
		DOMImplementation domImplementation = m_document.getImplementation();
		if (domImplementation.hasFeature("LS", "3.0") && domImplementation.hasFeature("Core", "2.0")) {
			DOMImplementationLS domImplementationLS = (DOMImplementationLS) domImplementation.getFeature("LS", "3.0");
			LSSerializer lsSerializer = domImplementationLS.createLSSerializer();
			DOMConfiguration domConfiguration = lsSerializer.getDomConfig();
			if (domConfiguration.canSetParameter("format-pretty-print", Boolean.TRUE)) {
				lsSerializer.getDomConfig().setParameter("format-pretty-print", Boolean.TRUE);
				LSOutput lsOutput = domImplementationLS.createLSOutput();
				lsOutput.setEncoding("UTF-8");
				StringWriter stringWriter = new StringWriter();
				lsOutput.setCharacterStream(stringWriter);
				lsSerializer.write(e, lsOutput);
				return stringWriter.toString();
			} else {
				throw new RuntimeException("DOMConfiguration 'format-pretty-print' parameter isn't settable.");
			}
		} else {
			throw new RuntimeException("DOM 3.0 LS and/or DOM 2.0 Core not supported.");
		}
	}
}
