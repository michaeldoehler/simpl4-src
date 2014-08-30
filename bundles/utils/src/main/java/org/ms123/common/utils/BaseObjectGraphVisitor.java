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

import metamodel.*;
import metamodel.visitor.ObjectGraphIterator;
import metamodel.visitor.ObjectGraphVisitor;
import metamodel.coreservices.*;
import metamodel.parser.*;
import org.apache.commons.beanutils.PropertyUtils;
import org.ms123.common.utils.Inflector;
import java.util.*;
import org.ms123.common.data.api.SessionContext;
import org.mvel2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
@SuppressWarnings("unchecked")
public abstract class BaseObjectGraphVisitor implements ObjectGraphVisitor {
	private Inflector m_inflector = Inflector.getInstance();
	protected static String CHILDREN = "children";
	protected static String TITLE = "title";
	protected static String DESCRIPTION = "description";
	protected static String NAME = "name";
	protected static String VALUE = "value";

	protected ClassNode m_classNode;

	protected SessionContext m_sc;

	protected String level = "\t";

	protected Map m_rootMap = null;

	protected Map m_currentMap = null;
	protected Map<String,String> m_mapping = null;

	protected List m_currentList = null;

	protected Stack<List> m_listStack = new Stack();

	protected Stack<Map> m_mapStack = new Stack();

	protected Map getRoot() {
		return m_rootMap;
	}

	public void serialize(Object data) {
		debug("serialize:"+data );
		ObjectGraphIterator iter = new ObjectGraphIterator(m_classNode, this, ObjectGraphIterator.TraversalMode.NOTIFY_CYCLES);
		if (data instanceof Collection) {
			Collection collection = (Collection) data;
			iter.iterateCollection(collection);
		} else {
			iter.iterate(data);
		}
	}

	public void visitRoot(ClassNode classNode, Object data) {
		debug("visitRoot" );
		m_rootMap = new HashMap();
		m_rootMap.put(TITLE, "root");
		m_rootMap.put(DESCRIPTION, "root");
		m_currentMap = m_rootMap;
	}

	public void visitObject(ClassNode classNode, DeepProperty property, Object value, Object parent) {
		debug("visitObject:" + property.getName() );
		level += "\t";
		m_mapStack.push(m_currentMap);
		m_currentMap = new HashMap();
		m_currentList.add(m_currentMap);
	}

	public void visitCollection(CollectionRef property, Object value, Object parent) {
		debug("visitCollection:" + property.getName() );
		m_listStack.push(m_currentList);
		List newList = new ArrayList();
		m_currentMap.put(property.getName(), newList);
		m_currentList = newList;
		level += "\t";
	}

	public void visitFlatProperty(FlatProperty property, Object value, Object parent, boolean hasNext) {
		debug("visitFlatProperty:" + property.getName() + "/" + value + "/" + hasNext+"/"+property.getTypeName());
		if( value instanceof Date ){
			m_currentMap.put(property.getName(), ((Date)value).getTime());
		}else{
			m_currentMap.put(property.getName(), value);
		}
	}

	public void visitNull(Property property, Object parent, boolean hasNext) {
		m_currentMap.put(property.getName(), null);
	}

	public void visitCyclic(ClassNode classNode, DeepProperty property, Object value, Object parent, boolean hasNext) {
	}

	public void endRoot(ClassNode classNode, Object data) {
		if( m_mapping != null){
			applyMapping(m_currentMap);
		}
	}

	public void endObject(ClassNode classNode, DeepProperty dp, Object data, Object parent, boolean hasNext) {
		level = level.substring(0, level.length() - 1);
		debug("endObject:" + dp.getName() +"/"+m_mapping);
		if( m_currentMap.get(CHILDREN) == null){
			m_currentMap.put(CHILDREN, new ArrayList());
		}
		if( m_mapping != null){
			applyMapping(m_currentMap);
		}
		m_currentMap = m_mapStack.pop();
	}

	public void endCollection(CollectionRef collRef, Collection data, Object parent, boolean hasNext) {
		level = level.substring(0, level.length() - 1);
		debug("endCollection:" + collRef.getName() );
		m_currentList = m_listStack.pop();
	}


	public boolean isOk(ClassNode classNode, DeepProperty dp, Object data, Object parent) {
		return true;
	}

	protected void applyMapping(Map props){
		Map retMap = new HashMap();
		Iterator<String> it = m_mapping.keySet().iterator();
		props.put("inflector", m_inflector);
		while (it.hasNext()) {
			String key = it.next();
			String val = m_mapping.get(key);
			if ("_all_".equals(key)) {
				props.remove("inflector");
				retMap.putAll(props);
			} else if (val == null) {
				retMap.put(key, props.get(key));
			} else {
				if (val.startsWith("(")) {
					int len = val.length();
					val = val.substring(1, len - 1);
					try {
						val = MVEL.evalToString(val, props);
					} catch (Exception e) {
						debug("TreeNodeVisitor.getNodeMap.MVEL(" + val + "):" + e);
					}
					retMap.put(key, val);
				} else {
					retMap.put(key, props.get(val));
				}
			}
		}
		Object c = props.get(CHILDREN);
		props.clear();
		props.putAll(retMap);
		props.put(CHILDREN, c );
	}
	protected boolean contains(String[] arr, String s) {
		int len = arr.length;
		for (int i = 0; i < len; i++) {
			if (arr[i].equals(s))
				return true;
		}
		return false;
	}

	protected String[] getArray(Object data, String prop) throws Exception {
		List l = (List) PropertyUtils.getProperty(data, prop);
		if (l != null){
			String[] s = new String[l.size()];
			l.toArray(s);
			return s;
		}
		return new String[0];
	}
	
	protected String getLastSegment(String s){
		if( s == null) return "NOTHING";
		String sa[] = s.split("\\.");
		if( sa.length>0){
			return sa[sa.length-1];
		}
			return s;
	}

	protected static class SCClassResolver implements ClassResolver {

		private SessionContext m_sc;

		public SCClassResolver(SessionContext sc) {
			this.m_sc = sc;
		}

		public Class resolveClass(String name) {
			try {
				return m_sc.getClass(name);
			} catch (Exception e) {
				throw new RuntimeException("SettingsVisitor.resolveClass:", e);
			}
		}
	}
	protected void debug(String msg) {
		//System.out.println(msg);
		m_logger.debug(msg);
	}
	protected void info(String msg) {
		System.out.println(msg);
		m_logger.info(msg);
	}
	private static final org.slf4j.Logger m_logger = org.slf4j.LoggerFactory.getLogger(BaseObjectGraphVisitor.class);
}
