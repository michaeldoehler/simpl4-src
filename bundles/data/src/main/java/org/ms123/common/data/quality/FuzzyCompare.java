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
import com.wcohen.ss.*;
import com.wcohen.ss.lookup.*;
import com.wcohen.ss.api.*;
import com.wcohen.ss.tokens.*;
import org.apache.commons.beanutils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unchecked")
public class FuzzyCompare implements Compare {

	private SoftTFIDF m_cmp = null;

	double m_ot;

	double m_lastScore;
	double m_maxScore;

	List m_corbus = new ArrayList();

	List m_fields;

	public FuzzyCompare(String field, double innerThreshold, double outerThreshhold) {
		this(toList(field), innerThreshold, outerThreshhold);
	}

	public FuzzyCompare(List fields, double innerThreshold, double outerThreshhold) {
		info("FuzzyCompare.create:"+fields+"/"+outerThreshhold+"/"+innerThreshold);
		m_fields = fields;
		double it = innerThreshold;
		m_ot = outerThreshhold;
		SimpleTokenizer tokenizer = new SimpleTokenizer(true, true);
		m_cmp = new SoftTFIDF(tokenizer, new JaroWinkler(), it);
	}

	public void addTrainValue(String value) {
		if (value == null || value.length() < 2) {
			return;
		}
		m_corbus.add(m_cmp.prepare(value.toLowerCase()));
	}
	public void reset() {
		m_maxScore=0.0;
	}

	public void init() {
		info(toString() + ":" + m_corbus.size());
		m_cmp.train(new BasicStringWrapperIterator(m_corbus.iterator()));
	}

	public boolean isEquals(Object o1, Object o2) {
		String s1 = getValue(o1);
		String s2 = getValue(o2);
		if (s1.equals(s2)) {
			debug("\tF(" + s1 + " | " + s2 + "):equal");
			m_lastScore = 1.0;
			m_maxScore = 1.0;
			return true;
		} else {
			double d = m_cmp.score(m_cmp.prepare(s1), m_cmp.prepare(s2));
			m_lastScore = d;
			if( d > m_maxScore){
				debug("\tF(" + s1 + " | " + s2 + "):" + d);
			}
			m_maxScore = Math.max( m_maxScore, d);
			if (d > m_ot) {
				return true;
			}
		}
		return false;
	}

	private static List toList(String s) {
		ArrayList list = new ArrayList();
		list.add(s);
		return list;
	}

	public String toString() {
		return "FuzzyCompare(" + m_fields + "," + m_ot + ")";
	}

	private String getValue(Object obj) {
		List<String> fields = m_fields;
		String value = "";
		String blank = "";
		for (String field : fields) {
			Object v = null;
			try {
				v = PropertyUtils.getProperty(obj, field);
			} catch (Exception e) {
				v = null;
			}
			if (v != null) {
				value += blank + v;
				blank = " ";
			}
		}
		return value.toLowerCase().trim();
	}

	public double getLastScore() {
		return m_lastScore;
	}

	private void debug(String message) {
		m_logger.debug(message);
		System.out.println(message);
	}

	private void info(String message) {
		m_logger.info(message);
		System.out.println(message);
	}

	private static final Logger m_logger = LoggerFactory.getLogger(FuzzyCompare.class);
}
