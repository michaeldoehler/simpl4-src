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
package org.ms123.common.setting;

import metamodel.*;
import metamodel.visitor.ObjectGraphIterator;
import metamodel.visitor.ObjectGraphVisitor;
import metamodel.coreservices.*;
import metamodel.parser.*;
import java.util.*;
import org.ms123.common.utils.BaseObjectGraphVisitor;
import org.ms123.common.data.api.SessionContext;
import antlr.TokenStreamException;
import antlr.RecognitionException;

/**
 */
@SuppressWarnings("unchecked")
public class SettingsVisitor extends BaseObjectGraphVisitor {

	private static String SETTINGSID = "settingsid";

	private static ClassNode createClassNode(SessionContext sc) throws RecognitionException, TokenStreamException {
		ClassNode classNode = TraversalUtils.parse(
		 "Settingscontainer(" +
		 "               settingsid,description," + 
		 "               children:collection(Settingscontainer(" +
		 "                   settingsid,description,"+
		 "                     children:collection(Settingscontainer("+
		 "                         settingsid,description,"+
		 "                         children:collection(Settingscontainer("+
		 "														settingsid,description)"+
		 "												 )"+
		 "                     )))))");
		classNode.setResolver(new SCClassResolver(sc));
		return classNode;
	}

	public static Map getObjectGraph(Object root, SessionContext sc, Map mapping) {
		try {
			ClassNode classNode = createClassNode(sc);
			SettingsVisitor tf = new SettingsVisitor(classNode, sc, mapping);
			tf.serialize(root);
			return tf.getRoot();
		} catch (Exception e) {
			throw new RuntimeException("SettingsVisitor.getObjectGraph:", e);
		}
	}

	public SettingsVisitor(ClassNode cn, SessionContext sc, Map mapping) {
		m_classNode = cn;
		m_sc = sc;
		m_mapping = mapping;
	}

	public void endCollection(CollectionRef collRef, Collection data, Object parent, boolean hasNext) {
		level = level.substring(0, level.length() - 1);
		Collections.sort(m_currentList, new TComparable());
		m_currentList = m_listStack.pop();
	}

	public class TComparable implements Comparator<Map> {
		@Override
		public int compare(Map m1, Map m2) {
			String s1 = (String) m1.get(SETTINGSID);
			String s2 = (String) m2.get(SETTINGSID);
			if (s1 == null)
				s1 = "X";
			if (s2 == null)
				s2 = "X";
			return s1.compareTo(s2);
		}
	}
}
