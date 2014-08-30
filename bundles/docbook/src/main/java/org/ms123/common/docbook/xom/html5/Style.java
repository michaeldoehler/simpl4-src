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

public class Style extends Base {
	private String m_style="";	
	private String m_type="text/css";	
	public Style() {
		super("style");
	}

	public void setStyle(String style){
		m_style = style;
	}
	public Element toXom() {
		Element e = super.toXom();
		e.addAttribute(new Attribute("type",m_type));
		//e.addAttribute(new Attribute("xml:space", "http://www.w3.org/XML/1998/namespace","preserve"));
		e.appendChild(new Text(m_style));
		return e;
	}
}
