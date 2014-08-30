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
package org.ms123.common.data.scripting;

import org.mvel2.UnresolveablePropertyException;
import org.mvel2.integration.VariableResolver;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Date;
import java.util.Iterator;
import javax.jdo.PersistenceManager;
import org.apache.commons.beanutils.*;

@SuppressWarnings({ "unchecked" })
public class MyVariableResolverFactory extends MapVariableResolverFactory {

	Map<String, Class> m_module;

	PersistenceManager m_pm;

	public MyVariableResolverFactory(Map variables, Map<String, Class> entity, PersistenceManager pm) {
		this.variables = variables;
		m_pm = pm;
		m_module = entity;
	}

	public MyVariableResolverFactory(Map variables) {
		this.variables = variables;
	}

	public VariableResolver getVariableResolver(String name) {
		//System.out.println("=> MyVariableResolverFactory.getVariableResolver:" + name); 
		VariableResolver vr = variableResolvers.get(name);
		if (vr != null) {
			return vr;
		} else if (variables.containsKey(name)) {
			variableResolvers.put(name, vr = new MapVariableResolver(variables, name));
			return vr;
		} else if (nextFactory != null) {
			vr = nextFactory.getVariableResolver(name);
			return vr;
		}
		throw new UnresolveablePropertyException("unable to resolve variable '" + name + "'");
	}

	public boolean isResolveable(String name) {
		boolean b = (variableResolvers.containsKey(name)) || (variables != null && variables.containsKey(name)) || (nextFactory != null && nextFactory.isResolveable(name));
		if (b) {
			return true;
		}
		if (m_module == null)
			return false;
		long start = new Date().getTime();
		//		System.out.println("=> MyVariableResolverFactory.isResolveable:" + name + "," + m_module); 
		//		System.out.println("\tisResolveable:" + variables); 
		//		System.out.println("\tisResolveable:" + getProperClass(name)); 
		Class clazz = getProperClass(name);
		if (clazz != null) {
			String m = getEntityName(clazz);
			Long id = (Long) variables.get("id");
			if (id == null) {
				id = (Long) variables.get(m + ".id");
			}
			System.out.println("\tID:" + id);
			if (id != null) {
				try {
					Object o = m_pm.getObjectById(clazz, id);
					//					System.out.println("\to:" + o); 
					Object value = PropertyUtils.getProperty(o, name);
					//					System.out.println("\tisResolveable.value:" + value); 
					createVariable(name, value);
					long end = new Date().getTime();
					//					System.out.println("<= Dauer:" + (end - start)); 
					return true;
				} catch (Exception e) {
					e.printStackTrace();
					return false;
				}
			}
		}
		return false;
	}

	private String getEntityName(Class clazz) {
		Iterator<String> it = m_module.keySet().iterator();
		while (it.hasNext()) {
			String mod = it.next();
			Class c = m_module.get(mod);
			if (clazz.equals(c)) {
				return mod;
			}
		}
		return null;
	}

	private Class getProperClass(String var) {
		Iterator<String> it = m_module.keySet().iterator();
		while (it.hasNext()) {
			String mod = it.next();
			Class clazz = m_module.get(mod);
			if (hasProperty(clazz, var)) {
				return clazz;
			}
		}
		return null;
	}

	private boolean hasProperty(Class clazz, String prop) {
		try {
			return clazz.getDeclaredField(prop) != null;
		} catch (Exception e) {
			return false;
		}
	}

	public void clear() {
		variableResolvers.clear();
		variables.clear();
	}
}
