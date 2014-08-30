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
package org.ms123.common.workflow;


import java.beans.FeatureDescriptor;
import java.util.*;

import org.activiti.engine.impl.javax.el.ELContext;
import org.activiti.engine.impl.javax.el.ELResolver;
import org.osgi.framework.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 */
@SuppressWarnings("unchecked")
public class ImplicitContextElResolver extends ELResolver {
	private static final Logger m_logger = LoggerFactory.getLogger(ImplicitContextElResolver.class);
	protected static Map<String, Object> m_cache = new HashMap<String, Object>();

	public ImplicitContextElResolver() {
	}

	public Object getValue(ELContext context, Object base, Object property) {
		System.out.println("OsgiContextElResolver:+getValue:" + base + "/" + property);
		if (base == null) {
			String key = (String) property;
			if ("utils".equals(key)) {
				context.setPropertyResolved(true);
				return new Utils();
			}		
		}

		return null;
	}

	public boolean isReadOnly(ELContext context, Object base, Object property) {
		return true;
	}

	public void setValue(ELContext context, Object base, Object property, Object value) {
		if (base == null) {}
	}

	public Class< ? > getCommonPropertyType(ELContext context, Object arg) {
		return Object.class;
	}

	public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context, Object arg) {
		return null;
	}

	public Class< ? > getType(ELContext context, Object arg1, Object arg2) {
		return Object.class;
	}

	static class Utils {
		public boolean contains(Object o1, Object o2) {
			return contains(o1, o2, "id");
		}

		public boolean contains(Object o1, Object o2, String key) {
			System.out.println("contains:" + o1 + "/" + o2 + "/" + key);
			if (o1 instanceof List && o2 instanceof List) {
				List l1 = (List) o1;
				List l2 = (List) o2;
			
				if (l1.size() == 0) {
					return true;
				}		
				if (l2.size() == 0) {
					return false;
				}		
				if (l1.get(0) instanceof Map) {
					for (Map m1 : (List<Map>) l1) {
						Object val1 = (Object) m1.get(key);
						boolean ok = false;
						for (Map m2 : (List<Map>) l2) {
							Object val2 = (Object) m2.get(key);
							if (val1.equals(val2)) {
								ok = true;
							}	
						}
						if (!ok) {
							System.out.println("l2:" + l2 + "contains not " + l1);
							return false;
						}
					}
				}
				if (l1.get(0) instanceof String) {
					for (String s1 : (List<String>) l1) {
						boolean ok = false;
						for (String s2 : (List<String>) l2) {
							if (s1.equals(s2)) {
								ok = true;
							}	
						}
						if (!ok) {
							System.out.println("l2:" + l2 + "contains not " + l1);
							return false;
						}
					}
				}
				System.out.println("l2:" + l2 + "contains " + l1);
				return true;
			}
			return false;
		}
	}
}
