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

public class Html extends Base {
	private Head m_head;
	private Body m_body;
	
	public Html() {
		super("html");
	}
	public void setHead(Head head) {
		m_head = head;
	}

	public Head getHead() {
		return m_head;
	}

	public void setBody(Body body) {
		m_body = body;
	}

	public Body getBody() {
		return m_body;
	}

	public Element toXom() {
		Element e = super.toXom();
//		e.addAttribute(new Attribute("xml:lang", "http://www.w3.org/XML/1998/namespace","de"));
		e.addAttribute(new Attribute("lang", "de"));
		if( m_head != null){
			e.appendChild(m_head.toXom());
		}
		if( m_body != null){
			e.appendChild(m_body.toXom());
		}
		return e;
	}
}
