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
import java.lang.reflect.*;
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
import net.sf.sojo.core.filter.ClassPropertyFilterHandler;
import net.sf.sojo.core.filter.ClassPropertyFilter;
import flexjson.*;
import org.ms123.common.data.api.SessionContext;

@SuppressWarnings("unchecked")
public class SojoFilterInterceptor implements WalkerInterceptor {

	protected Inflector m_inflector = Inflector.getInstance();

	private SessionContext m_sessionContext;

	private List<String> m_aliasList = null;

	private Map<String, Integer> m_fieldMap = new HashMap();

	private Map<String, Integer> m_pathMap = new HashMap();

	private Map m_result = new HashMap();

	private Map m_lastCurrent = new HashMap();

	private Object m_current = null;

	private Stack<String> m_moduleNameStack = new Stack();

	private String m_currentModuleName = "ROOT";

	public Map getResult() {
		return m_result;
	}
	private static JSONSerializer m_js = new JSONSerializer();

	public void setSessionContext(SessionContext sess) {
		m_sessionContext = sess;
	}

	public static Map filterFields(Object o, SessionContext sc, List<String> fieldList, List<String> aliasList) {
		ObjectGraphWalker walker = new ObjectGraphWalker(new ClassPropertyFilterHandlerImpl());
		//ObjectGraphWalker walker = new ObjectGraphWalker();
		ReflectionHelper.addSimpleType(org.datanucleus.store.types.simple.Date.class);
		walker.setIgnoreNullValues(true);
		SojoFilterInterceptor interceptor = new SojoFilterInterceptor();
		interceptor.setFields(fieldList, aliasList);
		interceptor.setSessionContext(sc);
		walker.addInterceptor(interceptor);
//		m_js.prettyPrint(true);
//		System.out.println("fieldList:"+m_js.deepSerialize(o));
		walker.walk(o);
		return interceptor.getResult();
	}

	public boolean visitElement(Object pvKey, int pvIndex, Object pvValue, int pvType, String pvPath, int pvNumberOfRecursion) {
		pvPath = cleanPath(pvPath);
		if (pvType == Constants.TYPE_SIMPLE) {
			boolean isId = isId((String) pvKey);
			if (!isId && m_fieldMap.get(pvPath) == null) {
				return false;
			}
			if (pvKey != null && pvKey.getClass().equals(String.class)) {
				if (isId || m_sessionContext.isFieldPermitted((String) pvKey, m_currentModuleName)) {
					String fieldName = (String) pvKey;
					if (!isId) {
						int index = m_fieldMap.get(pvPath);
						String alias = m_aliasList.get(index);
						if (alias != null && alias.length() > 0) {
							fieldName = alias;
						}
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
				if (m_pathMap.get(pvPath) == null) {
					return true;
				}
			}
			if (pvType == Constants.TYPE_MAP) {
				Object teams = ((Map) pvValue).get("_team_list");
				if (teams != null && ((Collection) teams).size() > 0) {
					for( Map team : ((Collection<Map>) teams)){ //@@@MS Copy the "team.name" from "teamintern" to "team"
						if(team.get("name") == null){
							Map teamintern = (Map)team.get("teamintern");
							team.put("name", teamintern.get("name"));
							team.remove("teamintern");
						}
					}
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
			}
		}
	}

	public void startWalk(Object pvStartObject) {
	}

	public void endWalk() {
	}

	private void setFields(List<String> fieldList, List<String> aliasList) {
		m_aliasList = aliasList;
		m_fieldMap = new HashMap();
		m_pathMap = new HashMap();
		int i = 0;
		for (String fn : fieldList) {
			m_fieldMap.put(removeFirstSegment(replaceDollar(fn)), i++);
		}
		i = 0;
		for (String fn : fieldList) {
			m_pathMap.put(removeLastSegment(removeFirstSegment(replaceDollar(fn))), i++);
		}
	}

	public String removeLastPointOnPath(String pvPath) {
		String lvPath = pvPath;
		if (lvPath.endsWith(".")) {
			lvPath = lvPath.substring(0, lvPath.length() - 1);
		}
		return lvPath;
	}

	private String replaceDollar(String s) {
		return s.replaceAll("\\$", ".");
	}

	private String removeFirstSegment(String s) {
		int i = s.indexOf(".");
		if (i == -1) {
			return "";
		}
		return s.substring(i + 1);
	}

	private String removeLastSegment(String s) {
		int i = s.lastIndexOf(".");
		if (i == -1) {
			return "";
		}
		return s.substring(0, i);
	}

	private String cleanPath(String pvPath) {
		pvPath = pvPath.replaceAll("\\[[0-9]*\\]", "");
		pvPath = pvPath.replaceAll("\\.\\(\\)", "");
		pvPath = pvPath.replaceAll("\\(\\)", "");
		pvPath = pvPath.replaceAll("\\.\\.", "");
		return pvPath;
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
	private static  class ClassPropertyFilterHandlerImpl implements ClassPropertyFilterHandler {
		public ClassPropertyFilter getClassPropertyFilterByClass(Class pvFilterClass) {
			return new ClassPropertyFilterImpl(pvFilterClass);
		}
	}

	private static class ClassPropertyFilterImpl extends ClassPropertyFilter{
		Class clazz;
		Map<String,Boolean> cache = new HashMap();
		public ClassPropertyFilterImpl(Class pvClass) {
			super(pvClass);
			clazz = pvClass;
		}
		public boolean isKnownProperty(String pvProperty) {
			if("class".equals(pvProperty)) return false;
		//	if("teamintern".equals(pvProperty)) return true;
			if("~unique-id~".equals(pvProperty)) return true;
			if( clazz.equals(Set.class) || clazz.equals(List.class) ) return false;
			boolean inc = isIncluded(pvProperty);

			//System.out.println("isKnownProperty("+clazz.getSimpleName()+"):"+pvProperty+"="+inc);
			return !inc;		
		}
		private boolean isIncluded( String pvProperty ) {
			try{
				Boolean b = cache.get(pvProperty);
				if( b!= null) return b;
				Field field = clazz.getDeclaredField(pvProperty);
				if( field.isAnnotationPresent( JSON.class ) ) {
					boolean isInc = field.getAnnotation( JSON.class ).include();
					cache.put(pvProperty,isInc);
					return isInc;
				}
			}catch(Exception e){
			}
			return true;
		}
	}
}
