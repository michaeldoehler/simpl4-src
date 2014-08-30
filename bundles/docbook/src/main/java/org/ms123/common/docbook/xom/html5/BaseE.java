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
public class BaseE extends Base {
	private String m_href=null;

	private String m_target=null;
	public BaseE() {
		super("base");
	}
	public BaseE(String href) {
		super("base");
		m_href=href;
	}
	public BaseE(String href,String target) {
		super("base");
		m_href=href;
		m_target=target;
	}

	public void setHref(String h) {
		m_href = h;
	}

	public void setTarget(String l) {
		m_target = l;
	}

	public Element toXom() {
		m_attributes.add(new Attribute("href", m_href));
		if( m_target !=  null){
			m_attributes.add(new Attribute("target", m_target));
		}
		Element e = super.toXom();
		return e;
	}
	public Map<String,Object> toMap() {
		m_attributes.add(new Attribute("href", m_href));
		if( m_target !=  null){
			m_attributes.add(new Attribute("target", m_target));
		}
		Map m = super.toMap();
		return m;
	}
}
