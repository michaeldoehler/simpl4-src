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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.jdo.PersistenceManager;
import org.apache.commons.beanutils.BeanMap;
import org.apache.commons.beanutils.PropertyUtils;
import org.ms123.common.data.api.SessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unchecked")
public class Utils {

	private static final String TEAMINTERN_ENTITY = "teamintern";

	public static String getString(Object newValue, Object oldValue, String mode) {
		if (newValue == null) {
			return null;
		}
		String ret = "";
		if (newValue instanceof List) {
			debug("newValue:" + newValue);
			debug("oldvalue:" + oldValue);
			debug("mode:" + mode);
			if ("replace".equals(mode) || oldValue == null || "".equals(oldValue)) {
				List list = (List) newValue;
				String komma = "";
				for (int i = 0; i < list.size(); i++) {
					ret += komma + list.get(i);
					komma = ",";
				}
			} else if ("add".equals(mode) || "remove".equals(mode)) {
				String[] oValues = ((String) oldValue).split(",");
				List<String> nValues = new ArrayList();
				for (int i = 0; i < oValues.length; i++) {
					nValues.add(oValues[i]);
				}
				List list = (List) newValue;
				for (int i = 0; i < list.size(); i++) {
					if ("add".equals(mode)) {
						if (!nValues.contains(list.get(i))) {
							nValues.add((String) list.get(i));
						}
					} else {
						if (nValues.contains(list.get(i))) {
							nValues.remove(list.get(i));
						}
					}
				}
				String komma = "";
				for (int i = 0; i < nValues.size(); i++) {
					ret += komma + nValues.get(i);
					komma = ",";
				}
			}
			debug("newvalue:" + ret);
		} else {
			ret = String.valueOf(newValue);
		}
		return ret;
	}

	public static Object listContainsId(Collection list, Map map, String idField) throws Exception {
		for (Object o : list) {
			String id1 = (String) PropertyUtils.getProperty(o, idField);
			if (map.get(idField) != null) {
				String id2 = String.valueOf(map.get(idField));
				if (id1.equals(id2)) {
					debug("\treturn:" + o);
					return o;
				}
			}
		}
		return null;
	}

	public static Object listContainsId(Collection list, Map map) throws Exception {
		for (Object o : list) {
			Object id = PropertyUtils.getProperty(o, "id");
			if (id instanceof Long) {
				Long id1 = (Long) PropertyUtils.getProperty(o, "id");
				if (map.get("id") != null) {
					Long id2 = getLong(map.get("id"));
					if (id1 == id2) {
						return o;
					}
				}
			}
			if (id instanceof String) {
				String id1 = (String) PropertyUtils.getProperty(o, "id");
				if (map.get("id") != null) {
					String id2 = (String) map.get("id");
					if (id1 == id2) {
						return o;
					}
				}
			}
		}
		return null;
	}

	public static <T> boolean isCollectionEqual(Collection<T> lhs, Collection<T> rhs) {
		return lhs.size() == rhs.size() && lhs.containsAll(rhs) && rhs.containsAll(lhs);
	}
	public static  boolean isEmptyObj(Object o) {
		if( o instanceof String){
			String s=(String)o;
			return (s == null || "".equals(s.trim()));
		}
		if( o==null) return true;
		return false;
	}

	public static  boolean isEmpty(String s) {
		return (s == null || "".equals(s.trim()));
	}
	public static boolean containsId(List<String> list, String pk) {
		Iterator<String> it = list.iterator();
		while (it.hasNext()) {
			if (it.next().endsWith("." + pk)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isNum(String s) {
		try {
			Double.parseDouble(s);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}

	public static boolean getBoolean(Object value) {
		try {
			return (Boolean) value;
		} catch (Exception e) {
		}
		return false;
	}

	public static boolean getBoolean(Map m, String key, boolean _def) {
		try {
			return (Boolean) m.get(key);
		} catch (Exception e) {
		}
		return _def;
	}

	public static Long getLong(Object o) {
		try {
			if (o instanceof Long) {
				return (Long) o;
			}
			return Long.parseLong(String.valueOf(o));
		} catch (Exception e) {
			return -1L;
		}
	}

	public static String extractId(String s) {
		for (String part : s.split("/")) {
			if (isaId(part))
				return part;
		}
		return null;
	}

	public static boolean isaId(String s) {
		if (s == null || s.length() != 32)
			return false;
		boolean isNumeric = s.matches("\\p{XDigit}+");
		info("isAid:" + s + " -> " + isNumeric);
		return isNumeric;
	}

	public static Object getTeamintern(SessionContext sc, String teamid) {
		PersistenceManager pm = sc.getPM();
		try {
			Class clazz = sc.getClass(TEAMINTERN_ENTITY);
			Object obj = pm.getObjectById(clazz, teamid);
			debug("Teamintern:" + new HashMap(new BeanMap(obj)));
			return obj;
		} catch (Exception e) {
			throw new RuntimeException("TeamService.getTeamintern(" + teamid + ")", e);
		}
	}
	public static  String getBaseName(String name) {
		if (name == null || name.trim().equals(""))
			return null;
		int lindex = name.lastIndexOf(".");
		if (lindex == -1)
			return name;
		return name.substring(lindex + 1).toLowerCase();
	}

	protected static void debug(String message) {
		m_logger.debug(message);
	}

	protected static void info(String message) {
		m_logger.info(message);
		System.out.println(message);
	}

	private static final Logger m_logger = LoggerFactory.getLogger(Utils.class);
}
