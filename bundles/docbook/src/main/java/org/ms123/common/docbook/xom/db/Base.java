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
public class Base {

	private String m_name;

	private List<Object> m_content = new ArrayList();

	protected List<Attribute> m_attributes = new ArrayList();

	public Base(String name) {
		m_name = name;
	}

	public List<Object> getContent() {
		return m_content;
	}

	public void add(Base e) {
		m_content.add(e);
	}

	public void add(String e) {
		m_content.add(e);
	}

	public void setRole(String role) {
		this.m_attributes.add(new Attribute("role", role));
	}

	public Element toXom() {
		Element e = new Element(m_name, "http://docbook.org/ns/docbook");
		for (Attribute a : m_attributes) {
			e.addAttribute(a);
		}
		for (Object o : m_content) {
			if (o instanceof String) {
				e.appendChild(new Text((String) o));
			}else if (o instanceof Element) {
				e.appendChild((Element)o);
			}else if (o instanceof ProcessingInstruction) {
				e.appendChild((ProcessingInstruction)o);
			} else {
				Base b = (Base) o;
				e.appendChild(b.toXom());
			}
		}
		return e;
	}
}
