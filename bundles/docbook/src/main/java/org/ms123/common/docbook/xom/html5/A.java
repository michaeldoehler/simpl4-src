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
package org.ms123.common.docbook.xom.html5;

import nu.xom.*;
import java.util.*;

@SuppressWarnings("unchecked")
public class A extends Base {
	private String m_href=null;
	private String m_href2=null;
	private String m_href3=null;

	private String m_resid=null;
	private String m_label=null;
	private String m_target=null;
	private String m_target2=null;
	private String m_target3=null;
	public A() {
		super("a");
	}
	public A(String href) {
		super("a");
		m_href=href;
	}
	public A(String href,String label) {
		super("a");
		m_label=label;
		m_href=href;
	}

	public void setHref(String h) {
		m_href = h;
	}
	public void setHref2(String h) {
		m_href2 = h;
	}
	public void setHref3(String h) {
		m_href3 = h;
	}

	public void setLabel(String l) {
		m_label = l;
	}
	public void setTarget(String l) {
		m_target = l;
	}
	public void setTarget2(String l) {
		m_target2 = l;
	}
	public void setTarget3(String l) {
		m_target3 = l;
	}
	public void setResourceId(String l) {
		m_resid = l;
	}

	public Element toXom() {
		m_attributes.add(new Attribute("href", m_href));
		if( m_resid !=  null){
			m_attributes.add(new Attribute("resourceId", m_resid));
		}
		if( m_target !=  null){
			m_attributes.add(new Attribute("target", m_target));
		}
		Element e = super.toXom();
		e.appendChild(new Text(m_label));
		return e;
	}
	public Map<String,Object> toMap() {
		m_attributes.add(new Attribute("href", m_href));
		if( m_resid !=  null){
			m_attributes.add(new Attribute("resourceId", m_resid));
		}
		if( m_target !=  null){
			m_attributes.add(new Attribute("target", m_target));
		}
		Map m = super.toMap();
		m.put("label",m_label);
		return m;
	}
}
