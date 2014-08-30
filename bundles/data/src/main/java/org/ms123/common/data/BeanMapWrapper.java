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

import org.apache.commons.beanutils.BeanMap;

public class BeanMapWrapper {

	private BeanMap m_beanmap;

	public BeanMapWrapper(Object o) {
		m_beanmap = new BeanMap(o);
	}

	public Class getType(String key) {
		return m_beanmap.getType(key);
	}

	public void put(String key, Object val) {
		m_beanmap.put(key, val);
	}
}
