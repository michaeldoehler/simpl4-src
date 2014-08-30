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
package org.ms123.common.bhs;

import metamodel.*;
import metamodel.visitor.ObjectGraphIterator;
import metamodel.visitor.ObjectGraphVisitor;
import metamodel.coreservices.*;
import metamodel.parser.*;
import java.util.*;
import org.ms123.common.utils.BaseObjectGraphVisitor;
import org.apache.commons.beanutils.PropertyUtils;
import org.ms123.common.data.api.SessionContext;
import antlr.TokenStreamException;
import antlr.RecognitionException;

import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;
/**
 */
@SuppressWarnings("unchecked")
public class BOMVisitor extends BaseObjectGraphVisitor {

	protected JSONSerializer m_js = new JSONSerializer();

	private static ClassNode createClassNode(SessionContext sc) throws RecognitionException, TokenStreamException {
		ClassNode classNode = TraversalUtils.parse(
		 "Bom as bo(" +
		 "               part,path,masterdata," + 
		 "               children:collection(reference(bo)))");
		classNode.setResolver(new SCClassResolver(sc));
		return classNode;
	}

	public static Map getObjectGraph(Object root, SessionContext sc, Map mapping) {
		try {
			ClassNode classNode = createClassNode(sc);
			BOMVisitor tf = new BOMVisitor(classNode, sc,mapping);
			tf.serialize(root);
			return tf.getRoot();
		} catch (Exception e) {
			throw new RuntimeException("BOMVisitor.getObjectGraph:", e);
		}
	}

	public BOMVisitor(ClassNode cn, SessionContext sc, Map mapping) {
		m_classNode = cn;
		m_sc = sc;
		m_mapping = mapping;
	}

	public void endCollection(CollectionRef collRef, Collection data, Object parent, boolean hasNext) {
		level = level.substring(0, level.length() - 1);
		Collections.sort( m_currentList, new TComparable() );
		m_currentList = m_listStack.pop();
	}

	public void visitFlatProperty(FlatProperty property, Object value, Object parent, boolean hasNext) {
		debug("visitFlatProperty:" + property.getName() + "/" + value + "/" + hasNext+"/"+property.getTypeName());
		if( value instanceof Date ){
			m_currentMap.put(property.getName(), ((Date)value).getTime());
		}else if( property.getName().equals("masterdata") ){
			String name = (String)getProperty(value,"name");
			String name2 = (String)getProperty(value,"name2");
			m_currentMap.put("name", isEmpty(name2) ? name : name2);
		}else{
			m_currentMap.put(property.getName(), value);
		}
	}
	private Object getProperty(Object o, String attr){
		try{
			return PropertyUtils.getProperty(o,attr);
		}catch(Exception e){
			return "NoValue";
		}
	}
	public boolean isOk(ClassNode classNode, DeepProperty dp, Object data, Object parent) {
		try {
			String path = (String)getProperty(data,"path");
			if( path.startsWith("2100097.141028500") || path.startsWith("2100097.141028600")) return false;
			Object qty = getProperty(data,"qty");
			if( qty == null ) return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	private boolean isEmpty(String s){
		if(s==null || s.trim().equals("")) return true;
		return false;
	}

	public class TComparable implements Comparator<Map>{
		@Override
		public int compare(Map m1, Map m2) {
			String s1 = (String)m1.get(NAME);
			String s2 = (String)m2.get(NAME);
			if( s1 == null) s1 = "X";
			if( s2 == null) s2 = "X";
			return s1.compareTo(s2);
		}
	}
}
