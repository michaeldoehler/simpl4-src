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
package org.ms123.common.data.quality;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.beanutils.PropertyUtils;

public class EqualCompare implements Compare {

	private String m_field;

	private Object m_null = new Object();

	public EqualCompare(String field) {
		m_field = field;
	}

	public void init() {
	}
	public void reset() {
	}

	public boolean isEquals(Object o1, Object o2) {
		Object s1 = getValue(o1);
		Object s2 = getValue(o2);
		if (s1.equals(s2)) {
			//debug("\tE(" + s1 + "," + s2 + "):true");
			return true;
		}
		//debug("\tE(" + s1 + "," + s2 + "):false");
		return false;
	}

	private Object getValue(Object obj) {
		Object v = null;
		try {
			v = PropertyUtils.getProperty(obj, m_field);
		} catch (Exception e) {
			return "ECError:" + m_field + "/" + e.getMessage();
		}
		if (v instanceof String) {
			v = ((String) v).toLowerCase();
		}
		if (v == null)
			return m_null;
		return v;
	}

	private void debug(String message) {
		m_logger.debug(message);
		System.out.println(message);
	}

	private void info(String message) {
		m_logger.info(message);
		System.out.println(message);
	}

	private static final Logger m_logger = LoggerFactory.getLogger(EqualCompare.class);
}
