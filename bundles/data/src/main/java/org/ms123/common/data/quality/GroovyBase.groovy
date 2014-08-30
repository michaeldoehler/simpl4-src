/*
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
import groovy.lang.*;



@groovy.transform.CompileStatic
@groovy.transform.TypeChecked
public abstract class GroovyBase extends Script implements Constants {
	private Map m_properties = [:];
	public boolean fuzzy(Object fields, double threshold){
		fuzzy(fields, threshold,0.0);
	}
	public boolean fuzzy(Object fields, double threshold, double inner){
		Compare fc = (Compare)((Map)m_properties.get("fieldset")).get(fields.toString()+threshold.toString());
		return fc.isEquals(getRefObj(), getCandidate());
	}

	public boolean equal(String field){
		Compare c = (Compare)((Map)m_properties.get("fieldset")).get(field);
		return c.isEquals(getRefObj(), getCandidate());
	}
	private Object getCandidate() {
		return m_properties.get("__candidate");
	}
	private Object getRefObj() {
		return m_properties.get("__refObj");
	}

	public void setProperty(String name,Object value){
		m_properties.put(name,value);
	}
	public Object getProperty(String name){
		Object o = m_properties.get(name);
		if( o != null ) return o;
		return super.getProperty(name);
	}
}


