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
package org.ms123.common.data.query;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.beanutils.*;
import java.lang.reflect.*;
import java.lang.annotation.*;
import org.ms123.common.libhelper.Inflector;
import org.ms123.common.nucleus.api.NucleusService;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.data.api.SessionContext;

@SuppressWarnings("unchecked")
public class MVELSelectBuilder extends BasicSelectBuilder implements SelectBuilder {

	private static final Logger m_logger = LoggerFactory.getLogger(MVELSelectBuilder.class);

	protected Inflector m_inflector = Inflector.getInstance();

	private static Map<String, Map<String, Object>> m_connectors;

	static {
		m_connectors = new HashMap<String, Map<String, Object>>();
		initConectors();
	}

	public MVELSelectBuilder(QueryBuilder qb, StoreDesc sdesc, String entityName, List<String> joinFields, Map filters, Map fieldSets) {
		super(qb, sdesc, entityName, joinFields, filters, fieldSets);
	}

	protected Map getConnector(Object con) {
		return m_connectors.get(con);
	}
	protected String getTeamSecurityWhere(String f){
		return "";
	}
	protected String getTeamUserWhere(String f){
		return "";
	}

	protected String getOr() {
		return "||";
	}

	protected String getNot() {
		return "!";
	}

	protected String getOp(String field, String op, Object _data, Map<String, String> c) {
		String data = String.valueOf(_data);
		String dt = c.get("datatype");
		if (dt.equals("array/team")) {
			// field = field.replace('.', '$'); 
			addSelector(field);
		}
		if ("ge".equals(op)) {
			return field + " >= " + data;
		}
		if ("le".equals(op)) {
			return field + " <= " + data;
		}
		if ("gt".equals(op)) {
			return field + " > " + data;
		}
		if ("lt".equals(op)) {
			return field + " < " + data;
		}
		if ("eq".equals(op)) {
			return getEqual(field, data, dt);
		}
		if ("ceq".equals(op)) {
			return getCaseEqual(field, data, dt);
		}
		if ("ne".equals(op)) {
			return getNotEqual(field, data, dt);
		}
		if ("bw".equals(op)) {
			return getBeginsWith(field, data, dt);
		}
		if ("cn".equals(op)) {
			return getContains(field, data, dt);
		}
		if ("in".equals(op)) {
			return field + " is null";
		}
		if ("inn".equals(op)) {
			return field + "is not null";
		}
		if ("team_changed".equals(op)) {
			return "team_changed!=null && team_changed['"+data + "'] != null && team_changed['"+data+"']!=null";
		}
		if ("team_hierarchy_changed".equals(op)) {
			return "team_changed!=null && ($ in team_changed.keySet() if $.startsWith('"+data+"') && team_changed[$]!=null).size() >0";
		}
		return "op not found";
	}

	protected Object getDate(Object data, Map<String, Object> rule) {
		try {
			Date d = new Date();
			try {
				if (data instanceof Long) {
					d = new Date((Long) data);
				} else {
					d = new Date(Long.valueOf((String) rule.get("data")));
				}
			} catch (Exception e) {
			}
			String op = (String) rule.get("op");
			if (op.equals("eq") || op.equals("ceq") || op.equals("neq")) {
				String day = d.getDate() + "";
				String month = d.getMonth() + "";
				String year = (d.getYear()) + "";
				data = day + "/" + month + "/" + year;
			} else {
				int paramCount = m_queryBuilder.getParamCount();
				data = "param" + paramCount;
				m_queryBuilder.getQueryParams().put("param" + paramCount, d);
				m_queryBuilder.incParamCount();
			}
		} catch (Exception e) {
			e.printStackTrace();
			data = "''";
		}
		return data;
	}

	private String getBeginsWith(String f, String d, String dt) {
		if (dt.startsWith("array/string")) {
			d = d.toUpperCase();
			String ret = null;
			ret = "(" + f + " ~= '(?i)" + d + ".*'";
			ret += " || " + f + " ~= '(?i)," + d + ".*')";
			return ret;
		} else if (dt.startsWith("array/")) {
			d = d.toUpperCase();
			return getArrayString(f, d, dt, "begin");
		} else {
			return f + " ~= (\"(?i)" + d + ".*\")";
		}
	}

	private String getContains(String f, String d, String dt) {
		return f + " ~= \"(?i).*" + d + ".*\"";
	}

	private String getNotEqual(String f, String d, String dt) {
		if (("string".equals(dt) || "text".equals(dt))) {
			return "!(" + f + " ~= (\"(?i)" + d + "\"))";
		}
		d = d.toUpperCase();
		if (dt.startsWith("array/string")) {
			d = d.toUpperCase();
			String ret = null;
			ret = "( ! (" + f + " ~= '(?i)" + d + ",.*'";
			ret += " || " + f + " ~= '(?i).*," + d + ",.*'";
			ret += " || " + f + " ~= '(?i)" + d + "'";
			ret += " || " + f + " ~= '(?i).*," + d + "'))";
			return ret;
		} else if (dt.startsWith("array/")) {
			return getArrayString(f, d, dt, "notequal");
		} else if ("date".equals(dt)) {
			String x[] = d.split("/");
			return f + ".getDate()" + " != " + x[0] + " || " + f + ".getMonth()" + " != " + x[1] + " || " + f + ".getYear()" + " != " + x[2];
		} else if ("boolean".equals(dt)) {
			return f + " != " + d;
		} else {
			return f + " != " + d;
		}
	}

	private String getCaseEqual(String f, String d, String dt) {
		if (("string".equals(dt) || "text".equals(dt))) {
			return f + " ~= (\"" + d + "\")";
		} else if ("boolean".equals(dt)) {
			return f + " == " + d;
		} else if (dt.startsWith("array/string")) {
			String ret = null;
			ret = "(" + f + " ~= '" + d + ",.*'";
			ret += " || " + f + " ~= '.*," + d + ",.*'";
			ret += " || " + f + " ~= '" + d + "'";
			ret += " || " + f + " ~= '.*," + d + "')";
			return ret;
		} else if (dt.startsWith("array/")) {
			return getArrayString(f, d, dt, "caseequal");
		} else if ("date".equals(dt)) {
			String x[] = d.split("/");
			return f + ".getDate()" + " == " + x[0] + " && " + f + ".getMonth()" + " == " + x[1] + " && " + f + ".getYear()" + " == " + x[2];
		} else if ("boolean".equals(dt)) {
			return f + " == " + d;
		} else {
			return f + " == " + d;
		}
	}

	private String getEqual(String f, String d, String dt) {
		if (("string".equals(dt) || "text".equals(dt))) {
			return f + " ~= (\"(?i)" + d + "\")";
		} else if (dt.startsWith("array/string")) {
			d = d.toUpperCase();
			String ret = null;
			ret = "(" + f + " ~= '(?i)" + d + ",.*'";
			ret += " || " + f + " ~= '(?i).*," + d + ",*'";
			ret += " || " + f + " ~= '(?i)" + d + "'";
			ret += " || " + f + " ~= '(?i).*," + d + "')";
			return ret;
		} else if (dt.startsWith("array/")) {
			d = d.toUpperCase();
			return getArrayString(f, d, dt, "equal");
		} else if ("date".equals(dt)) {
			String x[] = d.split("/");
			return f + ".getDate()" + " == " + x[0] + " && " + f + ".getMonth()" + " == " + x[1] + " && " + f + ".getYear()" + " == " + x[2];
		} else if ("boolean".equals(dt)) {
			return f + " == " + d;
		} else {
			d = d.toUpperCase();
			return f + " == " + d;
		}
	}

	protected String getArrayString(String f, String d, String dt, String op) {
		int paramCount = m_queryBuilder.getParamCount();
		String today = "param" + paramCount;
		m_queryBuilder.getQueryParams().put(today, new Date());
		m_queryBuilder.incParamCount();
		String id = dt.substring(dt.indexOf("/") + 1) + "id";
		String ret = "";
		if ("equal".equals(op)) {
			ret = "($ in " + f + " if $." + id + " ~= '(?i)" + d + "'";
		}
		if ("caseequal".equals(op)) {
			ret = "($ in " + f + " if $." + id + " ~= '" + d + "'";
		}
		if ("notequal".equals(op)) {
			ret = "($ in " + f + " if !($." + id + " ~= '(?i)" + d + "')";
		}
		if ("begin".equals(op)) {
			ret = "($ in " + f + " if $." + id + " ~= '(?i)" + d + ".*'";
		}
		return ret + " && ($.disabled==null || $.disabled==false ) && ($.validFrom == null || $.validFrom < " + today + " ) && ($.validTo == null || $.validTo > " + today + " ) ).size() >0";
	}

	private static void initConectors() {
		Map<String, Object> map = new HashMap();
		map.put("not_all", false);
		map.put("not_except", false);
		map.put("c_i1", "&&");
		map.put("c_in", "&&");
		m_connectors.put("and", map);
		map = new HashMap();
		map.put("not_all", false);
		map.put("not_except", false);
		map.put("c_i1", "||");
		map.put("c_in", "||");
		m_connectors.put("or", map);
		map = new HashMap();
		map.put("not_all", true);
		map.put("not_except", false);
		map.put("c_i1", "&&");
		map.put("c_in", "&&");
		m_connectors.put("and_not", map);
		map = new HashMap();
		map.put("not_all", true);
		map.put("not_except", false);
		map.put("c_i1", "||");
		map.put("c_in", "||");
		m_connectors.put("not", map);
		map = new HashMap();
		map.put("not_all", false);
		map.put("not_except", true);
		map.put("c_i1", "&&");
		map.put("c_in", "||");
		m_connectors.put("except", map);
		map = new HashMap();
		map.put("not_all", false);
		map.put("not_except", false);
		map.put("c_i1", "||");
		map.put("c_in", "||");
		m_connectors.put("union", map);
		map = new HashMap();
		map.put("not_all", false);
		map.put("not_except", false);
		map.put("c_i1", "&&");
		map.put("c_in", "&&");
		m_connectors.put("intersect", map);
	}
}
