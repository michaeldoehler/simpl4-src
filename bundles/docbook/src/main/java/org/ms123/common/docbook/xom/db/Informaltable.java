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

public class Informaltable extends Base {

	private String m_keepTogether = null;
	private Tgroup m_tgroup;

	private Caption m_caption;

	public Informaltable() {
		super("informaltable");
	}

	public Informaltable(String s) {
		super(s);
	}


	public void setKeepTogether(String x){
		if( "auto".equals(x) || "always".equals(x)){
			m_keepTogether = x;
		}else{
			m_keepTogether = "always";
		}
	}

	public void setFrame(String frame) {
		this.m_attributes.add(new Attribute("frame", frame));
	}

	public void setRowsep(String rowsep) {
		this.m_attributes.add(new Attribute("rowsep", rowsep));
	}

	public void setColsep(String colsep) {
		this.m_attributes.add(new Attribute("colsep", colsep));
	}

	public void setTgroup(Tgroup tgroup) {
		m_tgroup = tgroup;
	}

	public Tgroup getTgroup() {
		return m_tgroup;
	}

	public void setCaption(Caption caption) {
		m_caption = caption;
	}

	public Caption getCaption() {
		return m_caption;
	}

	public Element toXom() {
		Element e = super.toXom();
		if( m_keepTogether != null){
			e.appendChild( new ProcessingInstruction("dbfo", "keep-together=\""+m_keepTogether+"\""));
		}
		if( m_caption != null){
			e.appendChild(m_caption.toXom());
		}
		e.appendChild(m_tgroup.toXom());
		return e;
	}
}
