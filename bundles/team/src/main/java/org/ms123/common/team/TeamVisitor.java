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
package org.ms123.common.team;

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
public class TeamVisitor extends BaseObjectGraphVisitor {

	private static String TEAMID = "teamid";
	protected JSONSerializer m_js = new JSONSerializer();

	private static ClassNode createClassNode(SessionContext sc) throws RecognitionException, TokenStreamException {
		ClassNode classNode = TraversalUtils.parse(
		 "Teamintern as ti(" +
		 "               teamid,name,description,validFrom,validTo," + 
		 "               children:collection(reference(ti)))");
		classNode.setResolver(new SCClassResolver(sc));
		return classNode;
	}

	public static Map getObjectGraph(Object root, SessionContext sc, Map mapping) {
		try {
			ClassNode classNode = createClassNode(sc);
			TeamVisitor tf = new TeamVisitor(classNode, sc,mapping);
			tf.serialize(root);
			return tf.getRoot();
		} catch (Exception e) {
			throw new RuntimeException("TeamVisitor.getObjectGraph:", e);
		}
	}

	public TeamVisitor(ClassNode cn, SessionContext sc, Map mapping) {
		m_classNode = cn;
		m_sc = sc;
		m_mapping = mapping;
	}

	public void endCollection(CollectionRef collRef, Collection data, Object parent, boolean hasNext) {
		level = level.substring(0, level.length() - 1);
		Collections.sort( m_currentList, new TComparable() );
		m_currentList = m_listStack.pop();
	}

	public boolean isOk(ClassNode classNode, DeepProperty dp, Object data, Object parent) {
		try {
			if (m_sc.hasAdminRole()) {
				return true;
			}
			String s = (String) PropertyUtils.getProperty(data, "teamid");
			if( s!= null && "root".equals(s)) return true;
			String[] user_read = getArray(data, "userRead");
			String[] user_manage = getArray(data, "userManage");
			String[] user_create = getArray(data, "userCreate");
			String userName = m_sc.getUserName();
			if (contains(user_read, userName)) {
				System.out.println("\t==> item.read"); 
				return true;
			}
			if (contains(user_manage, userName)) {
				System.out.println("\t==> item.manage"); 
				return true;
			}
			if (contains(user_create, userName)) {
				System.out.println("\t==> item.create"); 
				return true;
			}
			if (m_sc.getUserProperties().get("groups") == null) {
				return false;
			}
			/*List<String> groups = (List) m_sc.getUserProperties().get("groups");
			for (String group : groups) {
				if (group != null && group.length() > 0) {
					if (contains(group_read, group)) {
						//System.out.println("\t==> item.group_read:"+item+","+m.get("group_read") +",group:"+group); 
						return true;
					}
					if (contains(group_manage, group)) {
						//System.out.println("\t==> item.group_manage:"+item); 
						return true;
					}
					if (contains(group_create, group)) {
						//System.out.println("\t==> item.group_create:"+item); 
						return true;
					}
				}
			}*/
		} catch (Exception e) {
			e.printStackTrace();
		}
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
