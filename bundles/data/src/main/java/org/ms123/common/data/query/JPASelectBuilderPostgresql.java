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
public class JPASelectBuilderPostgresql extends JPASelectBuilder implements SelectBuilder {

	private static final Logger m_logger = LoggerFactory.getLogger(JPASelectBuilderPostgresql.class);

	public JPASelectBuilderPostgresql(QueryBuilder qb, StoreDesc sdesc, String entityName, List<String> joinFields, Map filters, Map fieldSets) {
		super(qb, sdesc, entityName, joinFields, filters, fieldSets);
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
				String year = (d.getYear() + 1900) + "";
				data = day + "/" + month + "/" + year;
			}else if (op.equals("in") || op.equals("inn")) {
			} else {
				int paramCount = m_queryBuilder.getParamCount();
				data = ":param" + paramCount;
				System.out.println("param"+paramCount+":"+d);
				m_queryBuilder.getQueryParams().put("param" + paramCount, d);
				m_queryBuilder.incParamCount();
			}
		} catch (Exception e) {
			e.printStackTrace();
			data = "''";
		}
		return data;
	}

	protected String getList(List list) {
		String komma = "";
		String ret = "(";
		for (Object o : list) {
			if (o instanceof String) {
				ret += komma + "'" + o + "'";
			} else {
				ret += komma + o;
			}
			komma = ",";
		}
		ret += ")";
		System.out.println("getList:" + ret);
		return ret;
	}

	protected String getOp(String field, String op, Object _data, Map<String, String> c) {
		if (_data instanceof List) {
			return field + " in " + getList((List) _data);
		}
		String data = String.valueOf(_data);
		String dt = c.get("datatype");
		debug("field:" + field + ",op:" + op + ",data:" + data+",dt:"+dt);
		if (!dt.equals("array/string") && dt.startsWith("array/")) {
			field = field.replace('.', '$');
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
			if( field.indexOf("_team_list.valid") != -1){
				return m_entityName+"$_team_list is not null and "+ field + " is null";
			}else{
				if (("string".equals(dt) || "text".equals(dt))) {
					return field + " is null or "+field+" = ''";
				}else{
					return field + " is null";
				}
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

	private String getBeginsWith(String f, String d, String dt) {
		if (dt.startsWith("array/string")) {
			d = d.toUpperCase();
			String ret = null;
			ret = "(UPPER(" + f + ") like '" + d + "%'";
			ret += " or UPPER(" + f + ") like '," + d + "%')";
			return ret;
		} else if (dt.startsWith("array/team")) {
			d = d.toUpperCase();
			return getTeamString(f, d, dt, "begin");
		} else {
			return f + ".regexCI(\"^" + d + "\")";
		}
	}

	private String getContains(String f, String d, String dt) {
		if ("fulltext".equals(dt)) {
			return f + ".fulltext(\"" + substWildcard(d.toLowerCase(),false) + "\")";
		} else {
			if( "".equals(d)){
				return f +" is null or " +f + ".regexCI(\"" + d + "\")";
			}else{
				return f + ".regexCI(\"" + d + "\")";
			}
		}
	}

	private String getNotEqual(String f, String d, String dt) {
		if (("string".equals(dt) || "text".equals(dt))) {
			return f + ".regexNCS(\"^" + d + "$\")";
		}
		d = d.toUpperCase();
		if (dt.startsWith("array/string")) {
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
			String x[] = d.split("/");
			return f + ".getDay()" + " <> " + x[0] + " or " + f + ".getMonth()" + " <> " + x[1] + " or " + f + ".getYear()" + " <> " + x[2];
		} else if ("boolean".equals(dt)) {
			return f + " <> " + d;
		} else {
			return f + " <> " + d;
		}
	}

	private String getCaseEqual(String f, String d, String dt) {
		if (("string".equals(dt) || "text".equals(dt))) {
			return f + ".regexCS(\"^" + d + "$\")";
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
			String x[] = d.split("/");
			return f + ".getDay()" + " = " + x[0] + " and " + f + ".getMonth()" + " = " + x[1] + " and " + f + ".getYear()" + " = " + x[2];
		} else if ("boolean".equals(dt)) {
			return f + " = " + d;
		} else {
			return f + " = " + d;
		}
	}

	private String getEqual(String f, String d, String dt) {
		m_logger.info("\tgetEqual:" + f + "," + d + "," + dt);
		if (("string".equals(dt) || "text".equals(dt))) {
			if( "".equals(d)){
				return f +" is null or " +f + ".regexCI(\"^" + d + "$\")";
			}else{
				return f + ".regexCI(\"^" + d + "$\")";
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
			String x[] = d.split("/");
			return f + ".getDay()" + " = " + x[0] + " and " + f + ".getMonth()" + " = " + x[1] + " and " + f + ".getYear()" + " = " + x[2];
		} else if ("boolean".equals(dt)) {
			return f + " = " + d;
		} else {
			d = d.toUpperCase();
			return f + " = " + d;
		}
	}
	private String substWildcard( String ss, boolean autoWildcard ) {
    String s = "";
    for ( int i = 0;i < ss.length();i++ ) {
      if ( ss.charAt( i ) == '*' ) {
        s += ":*";
        continue;
      }
      s += ss.charAt( i );
    }
    if ( !s.endsWith( "*" ) && autoWildcard ) {
      s += ":*";
    }
    return s;
  }

}
