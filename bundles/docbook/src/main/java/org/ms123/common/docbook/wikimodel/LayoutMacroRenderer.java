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
package org.ms123.common.docbook.wikimodel;

import org.wikimodel.wem.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import org.ms123.common.docbook.Context;
import org.apache.commons.lang.RandomStringUtils;

@SuppressWarnings("unchecked")
public class LayoutMacroRenderer extends AbstractMacroRenderer {

	String m_id = null;

	String m_type = null;

	String m_center = null;

	String m_west = null;

	String m_south = null;

	String m_east = null;

	String m_north = null;

	String m_align = null;

	String m_fill = null;

	Integer m_rows = null;

	Integer m_columns = null;

	Integer m_vgap = null;

	Integer m_hgap = null;
	Boolean m_resize = null;

	List<String> m_items = null;

	List<String> m_borderStrings = null;

	Map<String, String> m_borderMap = null;

	public LayoutMacroRenderer(Context ctx, WikiParameters params, String content) {
		super(ctx, params, content);
		m_borderStrings = new ArrayList();
		m_borderStrings.add("west");
		m_borderStrings.add("east");
		m_borderStrings.add("south");
		m_borderStrings.add("center");
		m_borderStrings.add("north");
	}

	public String render() {
		try {
			m_type = getString(m_params.getParameter("type"), "flexGrid");
			if ("flexgrid".equals(m_type))
				m_type = "flexGrid";
			m_rows = getInteger(m_params.getParameter("rows"), -1);
			m_columns = getInteger(m_params.getParameter("columns"), -1);
			m_align = getString(m_params.getParameter("align"), "left");
			m_fill = getString(m_params.getParameter("fill"), "horizontal");
			m_resize = getBoolean(m_params.getParameter("resize"), true);
			m_vgap = getInteger(m_params.getParameter("vgap"), 5);
			m_hgap = getInteger(m_params.getParameter("hgap"), 5);
			if ("border".equals(m_type)) {
				m_borderMap = new HashMap();
				for (String item : m_borderStrings) {
					String val = getString(m_params.getParameter(item), null);
					if (val != null) {
						m_borderMap.put(item, val);
					}
				}
			} else {
				String items = getString(m_params.getParameter("items"), null);
				if (items == null) {
					return parse("{{warning}}Parameter items not found{{/warning}}");
				}
				String[] _items = items.split(",");
				m_items = new ArrayList();
				if (!"border".equals(m_type)) {
					for (String item : _items) {
						m_items.add(item.trim());
					}
				}
			}
			m_id = "ID" + RandomStringUtils.randomAlphanumeric(10);
			String html = "<div id=\"" + m_id + "\"></div>";
			System.out.println("Html:" + html);
			return html;
		} catch (Exception e) {
			e.printStackTrace();
			return parse("{{warning}}LayoutParse error:" + e.getMessage() + "{{/warning}}");
		}
	}

	public String getJS() {
		String js = "var main = jQuery('#" + m_id + "');\n";
		if ("border".equals(m_type)) {
			for (String item : m_borderStrings) {
				if (m_borderMap.get(item) != null) {
					js += "jQuery('#" + m_borderMap.get(item) + "').appendTo(main);\n";
				}
			}
		} else {
			if (m_items == null) {
				return null;
			}
			for (String item : m_items) {
				js += "jQuery('#" + item + "').appendTo(main);\n";
			}
		}
		js += "jQuery('#" + m_id + "').layout({\n";
		js += "type: '" + m_type + "',\n";
		js += "resize: "+m_resize+",\n";
		js += "vgap: " + m_vgap + ",\n";
		js += "hgap: " + m_hgap + ",\n";

		if (m_rows != -1) {
			js += "rows: " + m_rows + ",\n";
		}
		if (m_columns != -1) {
			js += "columns: " + m_columns + ",\n";
		}
		if ("flow".equals(m_type)) {
			js += "alignment:'" + m_align + "',\n";
		}
		if ("grid".equals(m_type)) {
			js += "fill:'" + m_fill + "',\n";
		}
		if ("border".equals(m_type)) {
			for (String item : m_borderStrings) {
				if (item != null) {
					String val = m_borderMap.get(item);
					if (val != null)
						js += item + ":jQuery('#" + val + "'),\n";
				}
			}
		} else {
			js += "items: [\n";
			String komma = "";
			for (String item : m_items) {
				js += komma + "jQuery('#" + item + "')";
				komma = ",\n";
			}
			js += "]\n";
		}
		js += "});\n";
		System.out.println("js:" + js);
		return js;
	}
}
