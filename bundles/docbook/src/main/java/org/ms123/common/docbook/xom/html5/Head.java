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
public class Head extends Base {
	private Title m_title;
	protected List<Meta> m_metas = new ArrayList();
	protected List<Link> m_links = new ArrayList();
	protected List<Script> m_scripts = new ArrayList();
	
	public Head() {
		super("head");
	}
	public void setTitle(Title title) {
		m_title = title;
	}

	public Title getTitle() {
		return m_title;
	}

	public List<Meta> getMetas() {
		return m_metas;
	}

	public List<Link> getLinks() {
		return m_links;
	}
	public List<Script> getScripts() {
		return m_scripts;
	}

	public Element toXom() {
		Element e = super.toXom();
		for (Meta m : m_metas) {
			e.appendChild(m.toXom());
		}
		if( m_title != null){
			e.appendChild(m_title.toXom());
		}
		for (Link l : m_links) {
			e.appendChild(l.toXom());
		}
		for (Script s : m_scripts) {
			e.appendChild(s.toXom());
		}
		return e;
	}
}
