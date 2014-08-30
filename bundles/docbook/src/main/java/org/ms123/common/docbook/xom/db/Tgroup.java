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
import java.util.*;

@SuppressWarnings("unchecked")
public class Tgroup extends Base {

	private Thead m_thead;

	private Tbody m_tbody;

	private List<Colspec> m_colspecs = new ArrayList();

	public Tgroup() {
		super("tgroup");
	}

	public void setCols(String cols) {
		m_attributes.add(new Attribute("cols", cols));
	}

	public void setColspecs(List<Colspec> csList) {
		m_colspecs = csList;
	}

	public List<Colspec> getColspecs() {
		return m_colspecs;
	}

	public void setThead(Thead thead) {
		m_thead = thead;
	}

	public Thead getThead() {
		return m_thead;
	}

	public void setTbody(Tbody tbody) {
		m_tbody = tbody;
	}

	public Tbody getTbody() {
		return m_tbody;
	}

	public Element toXom() {
		Element e = super.toXom();
		for (Colspec cs : m_colspecs) {
			e.appendChild(cs.toXom());
		}
		if( m_thead != null){
			e.appendChild(m_thead.toXom());
		}
		if( m_tbody != null){
			e.appendChild(m_tbody.toXom());
		}
		return e;
	}
}
