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
package org.ms123.common.docbook.xom.db;

import nu.xom.*;

public class Colspec extends Base {

	private String m_align;

	private String m_colwidth;

	private String m_colsep;

	private String m_rowsep;

	private String m_colnum;

	public Colspec() {
		super("colspec");
	}

	public void setAlign(String a) {
		m_align = a;
		m_attributes.add(new Attribute("align", a));
	}

	public void setColwidth(String w) {
		m_colwidth = w;
		m_attributes.add(new Attribute("colwidth", w));
	}

	public void setColsep(String s) {
		m_colsep = s;
		m_attributes.add(new Attribute("colsep", s));
	}

	public void setRowsep(String r) {
		m_rowsep = r;
		m_attributes.add(new Attribute("rowsep", r));
	}

	public void setColnum(String s) {
		m_colnum = s;
		m_attributes.add(new Attribute("colnum", s));
	}

	public String getColnum() {
		return m_colnum;
	}
}
