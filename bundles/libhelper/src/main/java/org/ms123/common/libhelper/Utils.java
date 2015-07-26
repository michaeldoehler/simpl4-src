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
package org.ms123.common.libhelper;

import groovy.lang.*;
import java.io.*;
import java.io.BufferedReader;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.*;
import java.util.Set;
import javax.jdo.annotations.Element;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import org.apache.commons.beanutils.BeanMap;
import org.mvel2.MVEL;

@SuppressWarnings("unchecked")
public class Utils {
	public List<Map> mapToList(Map<String, Map> map, Map mapping, String filter) {
		List<Map> retList = new ArrayList();
		Iterator<Map> it = map.values().iterator();
		while (it.hasNext()) {
			Map m = it.next();
			if (filter != null && filter.length() > 0) {
				boolean ok = _isOk(m, filter);
				if (!ok) {
					continue;
				}
			}
			if (mapping != null) {
				retList.add(mapValues(m, mapping));
			} else {
				retList.add(m);
			}
		}
		return retList;
	}

	public void sortListByField(List<Map> list, String sortField) {
		Collections.sort(list, new ListSortByField(sortField));
	}

	private class ListSortByField implements Comparator<Map> {

		String[] m_field;

		public ListSortByField(String field) {
			m_field = field.split(",");
		}

		public int compare(Map o1, Map o2) {
			if (m_field.length == 1) {
				String field = m_field[0];
				return compareTo(o1.get(field), o2.get(field));
			} else {
				String field1 = m_field[0];
				int res = compareTo(o1.get(field1), o2.get(field1));
				if (res == 0) {
					String field2 = m_field[1];
					return compareTo(o1.get(field2), o2.get(field2));
				} else {
					return res;
				}
			}
		}
	}

	private int compareTo(Object o1, Object o2) {
		if (o1 == null || o2 == null) {
			return 0;
		}
		if (o1 instanceof Integer) {
			int s1 = (Integer) o1;
			int s2 = (Integer) o2;
			return s1 - s2;
		} else if (o1 instanceof Boolean) {
			boolean s1 = (Boolean) o1;
			boolean s2 = (Boolean) o2;
			return s1 == s2 ? 0 : (s1 ? 1 : -1);
		} else if (o1 instanceof String) {
			String s1 = (String) o1;
			String s2 = (String) o2;
			return s1.compareTo(s2);
		}
		return 0;
	}

	public static List<Map> listToList(List list, Map mapping, String filter) {
		return listToList(list,mapping,filter,false);
	}
	public static List<Map> listToList(List list, Map mapping, String filter,boolean checkValid) {
		List<Map> retList = new ArrayList();
		Iterator it = list.iterator();
		while (it.hasNext()) {
			Map m = (Map) it.next();
			if(checkValid){
				boolean isValid = _isValid(m);
				if (!isValid) {
					continue;
				}
			}
			if (filter != null && filter.length() > 0) {
				boolean ok = _isOk(m, filter);
				if (!ok) {
					continue;
				}
			}
			if (mapping != null) {
				retList.add(mapValues(m, mapping));
			} else {
				retList.add(m);
			}
		}
		return retList;
	}

	protected static boolean _isOk(Map<String, Object> props, String filter) {
		try {
			return MVEL.evalToBoolean(filter, props);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	protected static boolean _isValid(Map<String, Object> props) {
		return !getBoolean(props.get("invalid")) && dateValid(props.get("invalid_from"));
	}

	public static Map mapValues(Map props, Map<String, String> mapping) {
		Iterator<String> it = mapping.keySet().iterator();
		Map retMap = new HashMap();
		while (it.hasNext()) {
			String key = it.next();
			String val = mapping.get(key);
			if ("_all_".equals(key)) {
				retMap.putAll(props);
			} else if (val == null) {
				retMap.put(key, props.get(key));
			} else {
				if (val.startsWith("(")) {
					int len = val.length();
					val = val.substring(1, len - 1);
					try {
						props.put("inflector", Inflector.getInstance());
						val = MVEL.evalToString(val, props);
						props.remove("inflector");
					} catch (Exception e) {
						e.printStackTrace();
					}
					retMap.put(key, val);
				} else {
					if( val.indexOf("${") != -1){
					retMap.put(key, expandString(val,props));
					}else{
						retMap.put(key, props.get(val));
					}
				}
			}
		}
		return retMap;
	}

	public static  Map copyObject(Object o) throws Exception{
		Map n = new HashMap();
		BeanMap beanMap = new BeanMap(o);
		Iterator itv = beanMap.keyIterator();
		while (itv.hasNext()) {
			String prop = (String) itv.next();
			if ("class".equals(prop)) {
				continue;
			}
			Object value = beanMap.get(prop);
			if ("_team_list".equals(prop)) {
				Set teamSet = new HashSet();	
				Set teams = (Set)beanMap.get(prop);
				if( teams!= null){
					for (Object team : teams) {
						Map t = new HashMap(new BeanMap(team));
						t.remove("teamintern");
						teamSet.add(t);
					}
				}
				value = teamSet;
			}else if( value instanceof Collection){
				continue;
			}else{
				java.lang.reflect.Field field = o.getClass().getDeclaredField(prop);
				if (field != null) {
					if (!field.isAnnotationPresent(PrimaryKey.class) && (field.isAnnotationPresent(Element.class) || field.isAnnotationPresent(Persistent.class))) {
						continue;
					}
				}
			}
			n.put(prop, value);
		}
		return n;
	}
	protected static  boolean getBoolean(Object value) {
		try {
			return (Boolean) value;
		} catch (Exception e) {
		}
		return false;
	}
	protected static boolean dateValid(Object value) {
		if( value == null) return true;
		long today = new java.util.Date().getTime();
		try {
			return today < (Long) value;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}
	private static Object expandString(String str, Map binding) {
		if( str.startsWith("~" )){
			return str.substring(1);
		}
		int countRepl = 0;
		int countPlainStr = 0;
		Object replacement = null;
		String   newString = "";
		int      openBrackets = 0;
		int      first = 0;
		for (int i = 0; i < str.length(); i++) {
			if (i < str.length() - 2 && str.substring(i, i + 2).compareTo("${") == 0) {
				if (openBrackets == 0) {
					first = i + 2;
				}
				openBrackets++;
			} else if (str.charAt(i) == '}' && openBrackets > 0) {
				openBrackets -= 1;
				if (openBrackets == 0) {
					countRepl++;
					replacement = MVEL.evalToString(str.substring(first, i), binding);
					newString += replacement;
				}
			} else if (openBrackets == 0) {
				newString += str.charAt(i);
				countPlainStr++;
			}
		}
		if (countRepl == 1 && countPlainStr == 0) {
			return replacement;
		} else {
			return newString;
		}
	}
	public static String __formatGroovyException(Throwable e) {
		String msg = e.getMessage();
		System.out.println("--->");
		System.out.println(msg);
		System.out.println("<---");
		int index = msg.indexOf("org.codehaus.groovy.syntax.SyntaxException");
		if (index > 0) {
			msg = msg.substring(0, index - 1);
		} else {
			StackTrace st = new StackTrace(e);
			Iterator it = st.iterator();
			String lineNumber = null;
			while (it.hasNext()) {
				StackTrace.Entry en = (StackTrace.Entry) it.next();
				System.out.println("Entry:" + en);
				String sf = en.getSourceFileName();
				if (sf != null && sf.startsWith("Script") && sf.endsWith(".groovy")) {
					lineNumber = en.getLineNumber();
					break;
				}
			}
			if (lineNumber != null) {
				msg = "Line:" + lineNumber + ": " + msg;
			}
		}
		return msg;
	}

	public static String formatGroovyException(Throwable e, String script) {
		if( e instanceof org.apache.camel.CamelExecutionException){
			e = e.getCause() != null ? e.getCause() : e;
		}

		Object lnMsg[] = getLineNumberFromMsg(e.getMessage());
		String lineNumber = null;
		String msg=null;
		String secondMsg = "";
		String delim="";
		String lnBreak="";
		if( lnMsg == null){
			msg = "<b>"+e.getMessage()+"</b>";
			Throwable t = e.getCause();
			while(t != null){
				secondMsg += delim+t.getMessage();
				t = t.getCause();
				delim = "\n-------------------------------\n";
			}

			if( msg == null){
				 StackTrace st = new StackTrace(e);
				 Iterator it = st.iterator();
				 msg = e.toString() +"/"+ it.next();
			}
			int index = msg.indexOf("org.codehaus.groovy.syntax.SyntaxException");
			if (index != -1) {
				msg = msg.substring(0, index - 1);
				Object[] res = getLineNumberFromMsg(msg);
				if (res != null) {
					lineNumber = (String) res[0];
					msg = (String) res[1];
				}
			} else {
				StackTrace st = new StackTrace(e);
				Iterator it = st.iterator();
				while (it.hasNext()) {
					StackTrace.Entry en = (StackTrace.Entry) it.next();
					System.out.println("Entry:" + en);
					String sf = en.getSourceFileName();
					if (sf != null && sf.startsWith("Script") && sf.endsWith(".groovy")) {
						lineNumber = en.getLineNumber();
						try{
							Integer.parseInt(lineNumber);
							break;
						}catch(Exception x){
						}
					}
				}
			}
		}else{
			lineNumber=(String)lnMsg[0];
			msg = "<b>"+lnMsg[1]+"</b>";
			lnBreak="<br/>";
		}
		String ret = insertLineNumbers(script, lineNumber);
		if (lineNumber != null) {
			msg = lnBreak+"Line:" + lineNumber + ": "+ msg;
		}
		delim = "\n-------------------------------\n";
		return msg + 
				delim + 
				ret +
				("".equals(secondMsg) ? "" : (delim + secondMsg));
	}

	private static Object[] getLineNumberFromMsg(String msg) {
		try{
			Pattern p = Pattern.compile(".*Script\\d{1,5}.groovy: (\\d{1,5}):(.*)", Pattern.DOTALL);
			Matcher m = p.matcher(msg);
			Object[] ret = new Object[2];
			if (m.find()) {
				ret[0] = m.group(1);
				ret[1] = m.group(2);
				return ret;
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}

	private static String insertLineNumbers(String lines, String lineNumber) {
		if( lines == null ) return "";
		int lnr = -1;
		int end = 100000;
		int start = 0;
		if (lineNumber != null) {
			try{
				lnr = Integer.parseInt(lineNumber);
				start = lnr - 7;
				end = lnr + 7;
			}catch(Exception e){
			}
		}
		if( start < 0) start = 0;
		BufferedReader br = new BufferedReader(new StringReader(lines));
		StringBuilder sb = new StringBuilder();
		try {
			int count = 1;
			String line = br.readLine();
			while (line != null) {
				if( count<start || count > end ){
					line = br.readLine();
					count++;
					continue;
				}
				if (lnr == count) {
					sb.append("->");
				} else {
					sb.append("  ");
				}
				sb.append(count++);
				sb.append(" ");
				sb.append(line);
				sb.append("\n");
				line = br.readLine();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (Exception e) {
			}
		}
		return sb.toString();
	}
}
