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
package org.ms123.common.data;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collection;
import java.util.Date;
import java.util.Stack;
import org.ms123.common.utils.*;
import net.sf.sojo.common.WalkerInterceptor;
import net.sf.sojo.core.Constants;
import net.sf.sojo.interchange.SerializerException;
import net.sf.sojo.util.Util;
import net.sf.sojo.interchange.json.JsonWalkerInterceptor;
import net.sf.sojo.common.*;
import net.sf.sojo.core.*;
import net.sf.sojo.core.conversion.*;
import net.sf.sojo.core.reflect.*;
import net.sf.sojo.navigation.*;
import org.ms123.common.data.api.SessionContext;

@SuppressWarnings("unchecked")
public class SojoObjectFilter implements WalkerInterceptor {

	protected Inflector m_inflector = Inflector.getInstance();

	private UniqueIdGenerator uniqueIdGenerator = null;

	private SessionContext m_sessionContext;

	private Map m_result = new HashMap();

	private Map m_lastCurrent = new HashMap();

	private Object m_current = null;

	private Stack<String> m_moduleNameStack = new Stack();

	private String m_currentModuleName = "ROOT";

	private List m_uidList = new ArrayList();
	private int m_level=0;
	private int m_maxLevel=5;
	private boolean m_useBeanMap=false;

	public Map getResult() {
		return m_result;
	}

	public void setSessionContext(SessionContext sess) {
		m_sessionContext = sess;
	}
	public void setMaxLevel(int ml) {
		m_maxLevel = ml;
	}
	public void setUseBeanMap(boolean u) {
		m_useBeanMap = u;
	}

	public static Map getObjectGraph(Object o, SessionContext sc) {
		return getObjectGraph(o,sc,5);
	}
	public static Map getObjectGraph(Object o, SessionContext sc, int maxLevel) {
		return getObjectGraph(o,sc,false,5);
	}
	public static Map getObjectGraph(Object o, SessionContext sc,boolean useBeanMap, int maxLevel) {
		ObjectGraphWalker walker = new ObjectGraphWalker();
		walker.setUseBeanMap(useBeanMap);
		ReflectionHelper.addSimpleType(org.datanucleus.store.types.simple.Date.class);
		walker.setIgnoreNullValues(true);
		SojoObjectFilter interceptor = new SojoObjectFilter();
		interceptor.setUseBeanMap(useBeanMap);
		interceptor.setSessionContext(sc);
		interceptor.setMaxLevel(maxLevel);
		walker.addInterceptor(interceptor);
		walker.walk(o);
		return interceptor.getResult();
	}

	public boolean visitElement(Object pvKey, int pvIndex, Object pvValue, int pvType, String pvPath, int pvNumberOfRecursion) {
		if (pvType == Constants.TYPE_SIMPLE) {
			//System.out.println("pvValue:"+pvValue+"/"+pvKey+"/"+pvValue.getClass());
			boolean isId = isId((String) pvKey);
			if (pvKey != null && pvKey.getClass().equals(String.class)) {
				if (isId || m_sessionContext.isFieldPermitted((String) pvKey, m_currentModuleName)) {
					String fieldName = (String) pvKey;
					if (!isId) {
					}
					if( pvValue instanceof java.util.Date){
						pvValue = ((java.util.Date)pvValue).getTime();
					}
					((Map) m_current).put(fieldName, pvValue);
				}
			}
		} else if (pvType == Constants.TYPE_NULL) {
		} else if (pvValue != null) {
			if (pvKey != null) {
			}
			if (pvType == Constants.TYPE_ITERATEABLE) {
				return false;
			}
			if (pvType == Constants.TYPE_MAP) {
				//System.out.println("Max:"+m_maxLevel+"/"+m_level+"/"+pvNumberOfRecursion+"/"+pvPath+"/"+printType(pvType));
				if(m_level >= m_maxLevel){
					return true;
				}
				Object uid = ((Map) pvValue).get(UniqueIdGenerator.UNIQUE_ID_PROPERTY);
				if (m_uidList.indexOf(uid) != -1){
					return true;
				}
				Object teams = ((Map) pvValue).get("_team_list");
				if (teams != null && ((Collection) teams).size() > 0) {
					System.out.println("teams:" + teams);
					if (!m_sessionContext.hasTeamPermission(teams)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public void visitIterateableElement(Object pvValue, int pvType, String pvPath, int pvBeginEnd) {
		pvPath = removeLastPointOnPath(pvPath);
		if (pvBeginEnd == Constants.ITERATOR_BEGIN) {
			m_lastCurrent.put(pvPath, m_current);
			if (pvType == Constants.TYPE_ITERATEABLE) {
				List newList = new ArrayList();
				if (m_current instanceof List) {
					((List) m_current).add(newList);
				}
				if (m_current instanceof Map) {
					((Map) m_current).put(getLastSegment(pvPath), newList);
				}
				m_current = newList;
			} else if (pvType == Constants.TYPE_MAP) {
				Object uid = ((Map) pvValue).get(UniqueIdGenerator.UNIQUE_ID_PROPERTY);
				if( uid!=null){
					m_uidList.add(uid);
				}
				m_level++;
				m_moduleNameStack.push(m_currentModuleName);
				m_currentModuleName = getEntityName(((Map) pvValue).get("class"));
				Map newMap = m_current != null ? new HashMap() : m_result;
				if (m_current instanceof List) {
					((List) m_current).add(newMap);
				}
				if (m_current instanceof Map) {
					((Map) m_current).put(getLastSegment(pvPath), newMap);
				}
				m_current = newMap;
			}
		} else if (pvBeginEnd == Constants.ITERATOR_END) {
			m_current = m_lastCurrent.get(pvPath);
			if (pvType == Constants.TYPE_ITERATEABLE) {
			} else if (pvType == Constants.TYPE_MAP) {
				m_currentModuleName = m_moduleNameStack.pop();
				m_level--;
			}
		}
	}

	public void startWalk(Object pvStartObject) {
	}

	public void endWalk() {
	}

	private String removeLastPointOnPath(String pvPath) {
		String lvPath = pvPath;
		if (lvPath.endsWith(".")) {
			lvPath = lvPath.substring(0, lvPath.length() - 1);
		}
		return lvPath;
	}


	private boolean isId(String pvKey) {
		return pvKey != null && pvKey.toLowerCase().equals("id");
	}

	private String getLastSegment(String path) {
		return getLastSegment(path, ".");
	}

	private String getLastSegment(String path, String sep) {
		int lastDot = path.lastIndexOf(sep);
		return path.substring(lastDot + 1);
	}

	private String getEntityName(Object clazz) {
		return m_inflector.getEntityName(getLastSegment((String) clazz.toString())).toLowerCase();
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
