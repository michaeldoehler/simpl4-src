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
import java.util.ArrayList;
import org.ms123.common.docbook.Context;
import org.apache.commons.lang.RandomStringUtils;

@SuppressWarnings("unchecked")
public class TabsMacroRenderer extends AbstractMacroRenderer {

	String m_tabsId = null;

	List<String> m_idList = null;

	public TabsMacroRenderer(Context ctx, WikiParameters params, String content) {
		super(ctx, params, content);
	}

	public String render() {
		try {
			String ids = getString(m_params.getParameter("ids"), null);
			if( ids == null){
				return parse("{{warning}}Parameter ids not found{{/warning}}");
			}
			String[] tabs = ids.split(",");
			m_tabsId = "ID" + RandomStringUtils.randomAlphanumeric(10);
			String html = "<div id=\"" + m_tabsId + "\">" + "<ul>";
			m_idList = new ArrayList();
			for (String tab : tabs) {
				String[] values = tab.split("=");
				String id = values[0];
				m_idList.add(id);
				String label = values[1];
				html += "<li><a href=\"#" + id + "\">" + label + "</a></li>";
			}
			html += "</ul></div>";
			System.out.println("Html:" + html);
			return html;
		} catch (Exception e) {
			e.printStackTrace();
			return parse("{{warning}}TabsParse error:" + e.getMessage() + "{{/warning}}");
		}
	}

	public String getJS() {
		if( m_idList == null) return null;
		String js = "var tabs = jQuery( \"#" + m_tabsId + "\" ).tabs();var ul = tabs.find( 'ul' );";
		for (String id : m_idList) {
			js += "jQuery('#" + id + "').appendTo(ul);";
		}
		js += "tabs.tabs( 'refresh' );";
		return js;
	}
}
