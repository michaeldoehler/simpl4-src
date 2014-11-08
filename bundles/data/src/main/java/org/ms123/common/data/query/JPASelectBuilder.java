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

@SuppressWarnings({"unchecked","deprecation"})
public class JPASelectBuilder extends BasicSelectBuilder implements SelectBuilder {

	private static final Logger m_logger = LoggerFactory.getLogger(JPASelectBuilder.class);

	private static Map<String, Map<String, Object>> m_connectors;

	static {
		m_connectors = new HashMap<String, Map<String, Object>>();
		initConectors();
	}

	public JPASelectBuilder(QueryBuilder qb, StoreDesc sdesc, String entityName, List<String> joinFields, Map filters, Map fieldSets) {
		super(qb, sdesc, entityName, joinFields, filters, fieldSets);
	}

	protected Map getConnector(Object con) {
		return m_connectors.get(con);
	}

	protected String getOp(String field, String op, Object _data, Map<String, String> c) {
		String data = String.valueOf(_data);
		String dt = c.get("datatype");
		if (!dt.equals("array/string") && dt.startsWith("array/")) {
			field = field.replace('.', '$');
			addSelector(field);
		}
		System.out.println("field:" + field + ",op:" + op + ",data:" + data+",dt:"+dt);
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
			return getBegin(field, data, dt);
		}
		if ("cn".equals(op)) {
			return getContains(field, data, dt);
		}
		if ("in".equals(op)) {
			if( field.indexOf("_team_list.valid") != -1){
				return m_entityName+"$_team_list is not null and "+ field + " is null";
			}else{
				return field + " is null or "+field+" = ''";
			}
		}
		if ("inn".equals(op)) {
			if( field.indexOf("_team_list.valid") != -1){
				return m_entityName+"$_team_list is not null and "+ field + " is not null";
			}else{
				return field + "is not null";
			}
		}
		return "op not found";
	}

	private String getBegin(String f, String d, String dt) {
		d = d.toUpperCase();
		if (dt.startsWith("array/string")) {
			String ret = null;
			ret = "(UPPER(" + f + ") like '" + d + "%'";
			ret += " or UPPER(" + f + ") like '," + d + "%')";
			return ret;
		} else if (dt.startsWith("array/team")) {
			return getTeamString(f, d, dt, "begin");
		} else {
			return "UPPER(" + f + ") like '" + d + "%'";
		}
	}

	private String getContains(String f, String d, String dt) {
		d = d.toUpperCase();
		if( "".equals(d)){
			return f +" is null or UPPER(" + f + ") like '%" + d + "%'";
		}else{
			return "UPPER(" + f + ") like '%" + d + "%'";
		}
	}

	private String getRegexp(String f, String d, String dt) {
		d = d.toUpperCase();
		return "UPPER(" + f + ") regexp '.*" + d + ".*'";
	}

	private String getNotEqual(String f, String d, String dt) {
		d = d.toUpperCase();
		if (("string".equals(dt) || "text".equals(dt))) {
			return "UPPER(" + f + ") <> '" + d + "'";
		} else if (dt.startsWith("array/string")) {
			d = d.toUpperCase();
			String ret = null;
			ret = "( NOT (UPPER(" + f + ") like '" + d + ",%'";
			ret += " or UPPER(" + f + ") like '%," + d + ",%'";
			ret += " or UPPER(" + f + ") like '" + d + "'";
			ret += " or UPPER(" + f + ") like '%," + d + "'))";
			return ret;
		} else if (dt.startsWith("array/team")) {
			return getTeamString(f, d, dt, "notequal");
		} else if ("date".equals(dt)) {
			return f + " <> " + d;
		} else if ("boolean".equals(dt)) {
			return f + " <> " + d;
		} else {
			return f + " <> " + d;
		}
	}

	private String getCaseEqual(String f, String d, String dt) {
		if (("string".equals(dt) || "text".equals(dt))) {
			return f + " = '" + d + "'";
		} else if ("boolean".equals(dt)) {
			return f + " = " + d;
		} else if (dt.startsWith("array/string")) {
			String ret = null;
			ret = "(" + f + " like '" + d + ",%'";
			ret += " or " + f + " like '%," + d + ",%'";
			ret += " or " + f + " like '" + d + "'";
			ret += " or " + f + " like '%," + d + "')";
			return ret;
		} else if (dt.startsWith("array/team")) {
			return getTeamString(f, d, dt, "caseequal");
		} else if ("date".equals(dt)) {
			return f + " = " + d;
		} else if ("boolean".equals(dt)) {
			return f + " = " + d;
		} else {
			return f + " = " + d;
		}
	}

	private String getEqual(String f, String d, String dt) {
		m_logger.info("\tgetEqual:" + f + "," + d + "," + dt);
		if (("string".equals(dt) || "text".equals(dt))) {
			d = d.toUpperCase();
			if( "".equals(d)){
				return f +" is null or UPPER(" + f + ") = ''";
			}else{
				return "UPPER(" + f + ") = '" + d + "'";
			}
		} else if (dt.startsWith("list_string")) {
			return f + ".contains('" + d + "')";
		} else if (dt.startsWith("array/string")) {
			d = d.toUpperCase();
			String ret = null;
			ret = "(UPPER(" + f + ") like '" + d + ",%'";
			ret += " or UPPER(" + f + ") like '%," + d + ",%'";
			ret += " or UPPER(" + f + ") like '" + d + "'";
			ret += " or UPPER(" + f + ") like '%," + d + "')";
			return ret;
		} else if (dt.startsWith("array/team")) {
			d = d.toUpperCase();
			return getTeamString(f, d, dt, "equal");
		} else if ("date".equals(dt)) {
			return f + " = " + d;
		} else if ("boolean".equals(dt)) {
			return f + " = " + d;
		} else {
			d = d.toUpperCase();
			return f + " = " + d;
		}
	}

	protected String getTeamString(String f, String d, String dt, String op) {
		int paramCount = m_queryBuilder.getParamCount();
		String today = ":param" + paramCount;
		m_queryBuilder.getQueryParams().put("param" + paramCount, new Date());
		m_queryBuilder.incParamCount();
		int slash = dt.indexOf("/");
		String id = "id";
		if (slash != -1) {
			id = dt.substring(slash + 1) + "id";
		}
		String ret = "";
		if ("equal".equals(op)) {
			ret = "UPPER(" + f + "." + id + ") = '" + d + "'";
		}
		if ("caseequal".equals(op)) {
			ret = f + "." + id + " = '" + d + "'";
		}
		if ("notequal".equals(op)) {
			ret = "UPPER(" + f + "." + id + ") <> '" + d + "'";
		}
		if ("begin".equals(op)) {
			ret = "UPPER(" + f + "." + id + ") like '" + d + "%'";
		}
		ret +=  "and (" + f + ".disabled is null or " + f + ".disabled=false) and ( " + f + ".validFrom is null or " + f + ".validFrom < " + today + " ) and ( " + f + ".validTo is null or " + f + ".validTo > " + today + " )";
		return ret;
	}
	protected String getTeamSecurityWhere(String sel){
		String userName = m_queryBuilder.getSessionContext().getUserName();
		int paramCount = m_queryBuilder.getParamCount();
		String today = ":param" + paramCount;
		m_queryBuilder.getQueryParams().put("param" + paramCount, new Date());
		m_queryBuilder.incParamCount();

		return  "("+
			"     "+sel+"$_team_list.teamid IS NULL"+
			" OR  ( ("+
			"           "+sel+"$_team_list.disabled IS NULL OR  "+sel+"$_team_list.disabled=false )"+
			"     AND ( "+sel+"$_team_list.validFrom IS NULL OR  "+sel+"$_team_list.validFrom < "+today+" )"+
			"     AND ( "+sel+"$_team_list.validTo IS NULL OR  "+sel+"$_team_list.validTo > "+today+" )"+
			"     AND ( "+sel+"$_team_list.teamintern.userRead.contains('"+userName+"') OR  "+sel+"$_team_list.teamintern.userManage.contains('"+userName+"') ) ))";
	}

	protected String getTeamUserWhere(String f){
		String userName = m_queryBuilder.getSessionContext().getUserName();
		String ur = getEqual(f+".teamintern.userRead" ,userName,"list_string");
		String um = getEqual(f+".teamintern.userManage" ,userName,"list_string");
		return  "("+ur +" or " + um + ")";
	}

	private static void initConectors() {
		Map<String, Object> map = new HashMap();
		map.put("not_all", false);
		map.put("not_except", false);
		map.put("c_i1", "and");
		map.put("c_in", "and");
		m_connectors.put("and", map);
		map = new HashMap();
		map.put("not_all", false);
		map.put("not_except", false);
		map.put("c_i1", "or");
		map.put("c_in", "or");
		m_connectors.put("or", map);
		map = new HashMap();
		map.put("not_all", true);
		map.put("not_except", false);
		map.put("c_i1", "and");
		map.put("c_in", "and");
		m_connectors.put("and_not", map);
		map = new HashMap();
		map.put("not_all", true);
		map.put("not_except", false);
		map.put("c_i1", "or");
		map.put("c_in", "or");
		m_connectors.put("not", map);
		map = new HashMap();
		map.put("not_all", false);
		map.put("not_except", true);
		map.put("c_i1", "and");
		map.put("c_in", "or");
		m_connectors.put("except", map);
		map = new HashMap();
		map.put("not_all", false);
		map.put("not_except", false);
		map.put("c_i1", "or");
		map.put("c_in", "or");
		m_connectors.put("union", map);
		map = new HashMap();
		map.put("not_all", false);
		map.put("not_except", false);
		map.put("c_i1", "and");
		map.put("c_in", "and");
		m_connectors.put("intersect", map);
	}
}
