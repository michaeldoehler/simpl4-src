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

public class Link extends Base {
	private String m_rel="stylesheet";
	private String m_href="";
	private String m_type=null;
	public Link() {
		super("link");
	}
	public Link(String rel, String href) {
		super("link");
		if( rel != null){
			m_rel=rel;
		}
		m_href=href;
	}

	public void setRel(String r) {
		m_rel = r;
	}
	public void setHref(String h) {
		m_href = h;
	}
	public void setType(String h) {
		m_type = h;
	}


	public Element toXom() {
		m_attributes.add(new Attribute("rel", m_rel));
		m_attributes.add(new Attribute("href", m_href));
		if( m_type != null){
			m_attributes.add(new Attribute("type", m_type));
		}
		Element e = super.toXom();
		return e;
	}
}
