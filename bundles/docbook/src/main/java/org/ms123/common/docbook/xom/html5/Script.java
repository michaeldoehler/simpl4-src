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

public class Script extends Base {
	private String m_script=null;
	private String m_src="";
	private String m_type="text/javascript";
	public Script() {
		super("script");
	}
	public Script(String src) {
		super("script");
		m_src=src;
	}
	public Script(String type, String src) {
		super("script");
		m_type=type;
		m_src=src;
	}

	public void setScr(String s) {
		m_src = s;
	}
	public void setScript(String s) {
		m_script = s;
	}
	public void setType(String t) {
		m_type = t;
	}

	public Element toXom() {
		Element e = null;
		m_attributes.add(new Attribute("type", m_type));
		if( m_script == null){
			m_attributes.add(new Attribute("src", m_src));
			e = super.toXom();
			e.appendChild(new Text(" "));
		}else{
			e = super.toXom();
			e.appendChild(new Text(m_script));
		}
		return e;
	}
}
