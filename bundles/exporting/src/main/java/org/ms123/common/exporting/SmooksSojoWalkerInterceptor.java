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
package org.ms123.common.exporting;

import java.util.Map;
import java.util.TreeMap;
import java.util.*;
import java.io.*;
import org.ms123.common.utils.*;
import net.sf.sojo.core.Constants;
import net.sf.sojo.core.UniqueIdGenerator;
import net.sf.sojo.common.WalkerInterceptor;
import org.xml.sax.ContentHandler;
import javax.xml.XMLConstants;
import org.xml.sax.helpers.AttributesImpl;
import org.ms123.common.data.api.SessionContext;

/**
 * 
 */
@SuppressWarnings("unchecked")
public class SmooksSojoWalkerInterceptor implements WalkerInterceptor {

	protected Inflector m_inflector = Inflector.getInstance();

	private boolean filterUniqueIdProperty = false;

	private boolean onlySimpleProperties = false;

	private Object rootObject;

	private String rootTag;

	private ContentHandler m_contentHandler;

	private SessionContext m_sessionContext;

	private String m_currentEntityName = "ROOT";

	private Stack<String> m_entityNameStack = new Stack();

	public SmooksSojoWalkerInterceptor() {
	}

	public void setContentHandler(ContentHandler out) {
		m_contentHandler = out;
	}

	public void setSessionContext(SessionContext sess) {
		m_sessionContext = sess;
	}

	public void setRootTag(String tag) {
		rootTag = tag;
	}

	public void endWalk() {
	}

	public void startWalk(Object pvStartObject) {
		rootObject = pvStartObject;
	}

	public boolean visitElement(Object pvKey, int pvIndex, Object pvValue, int pvType, String pvPath, int pvNumberOfRecursion) {
		if (pvType == Constants.TYPE_SIMPLE) {
			if (pvKey != null && !pvKey.equals("~unique-id~") && !pvKey.equals("class") && pvKey.getClass().equals(String.class)) {
				String tag = (String) pvKey;
				if (((String) pvKey).startsWith("_")) {
					return false;
				}
				if (m_sessionContext.isFieldPermitted(tag, m_currentEntityName)) {
					startElement(tag);
					text(pvValue + "");
					endElement(tag);
				}
			}
		} else if (pvType == Constants.TYPE_NULL) {
			String tag = (String) pvKey;
			startElement(tag);
			endElement(tag);
		} else if (pvType == Constants.TYPE_ITERATEABLE) {
		} else if (pvType == Constants.TYPE_MAP) {
			Object teams = ((Map) pvValue).get("_team_list");
			if (teams != null && ((Collection) teams).size() > 0) {
				System.out.println("teams:" + teams);
				if (!m_sessionContext.hasTeamPermission(teams)) {
					System.out.println("no teamsperm:" + pvValue);
					return true;
				}
			}
		} else if (pvKey != null && pvValue != null) {
			if (pvKey != null && pvKey.getClass().equals(String.class)) {
			}
		}
		return false;
	}

	public void visitIterateableElement(Object pvValue, int pvType, String pvPath, int pvBeginEnd) {
		if (pvBeginEnd == Constants.ITERATOR_BEGIN) {
			if (pvValue.equals(rootObject)) {
				startElement(rootTag);
			} else if (pvType == Constants.TYPE_ITERATEABLE) {
				startElement(getLastSegment(pvPath).toLowerCase());
			} else if (pvType == Constants.TYPE_MAP) {
				m_entityNameStack.push(m_currentEntityName);
				m_currentEntityName = getEntityName(((Map) pvValue).get("class"));
				startElement(getClassName(((Map) pvValue).get("class")));
			}
		}
		if (pvBeginEnd == Constants.ITERATOR_END) {
			if (pvValue.equals(rootObject)) {
				endElement(rootTag);
			} else if (pvType == Constants.TYPE_ITERATEABLE) {
				endElement(getLastSegment(pvPath).toLowerCase());
			} else if (pvType == Constants.TYPE_MAP) {
				m_currentEntityName = m_entityNameStack.pop();
				endElement(getClassName(((Map) pvValue).get("class")));
			}
		}
	}

	private void startElement(String name) {
		try {
			m_contentHandler.startElement(XMLConstants.NULL_NS_URI, name, "", new AttributesImpl());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void endElement(String name) {
		try {
			m_contentHandler.endElement(XMLConstants.NULL_NS_URI, name, "");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void text(String text) {
		try {
			m_contentHandler.characters(text.toCharArray(), 0, text.length());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String getLastSegment(String path) {
		return getLastSegment(path, ".");
	}

	private String getLastSegment(String path, String sep) {
		int lastDot = path.lastIndexOf(sep);
		return path.substring(lastDot + 1);
	}

	private String getClassName(Object clazz) {
		if( clazz == null){
			return m_inflector.getClassName(rootTag.toLowerCase());
		}
		return m_inflector.getClassName(getLastSegment((String) clazz)).toLowerCase();
	}

	private String getEntityName(Object clazz) {
		if( clazz == null){
			return m_inflector.getEntityName(rootTag.toLowerCase());
		}
		return m_inflector.getEntityName(getLastSegment((String) clazz)).toLowerCase();
	}

	private String printBeginEnd(int num) {
		switch(num) {
			case Constants.ITERATOR_BEGIN:
				return "BEGIN";
			case Constants.ITERATOR_END:
				return "END";
			default:
				return "";
		}
	}

	private Class _getClass(Object type) {
		if (type == null)
			return null;
		return type.getClass();
	}

	private String printType(int num) {
		switch(num) {
			case Constants.TYPE_NULL:
				return "NULL";
			case Constants.TYPE_SIMPLE:
				return "SIMPLE";
			case Constants.TYPE_ITERATEABLE:
				return "ITERATEABLE";
			case Constants.TYPE_MAP:
				return "MAP";
			default:
				return "";
		}
	}
}
