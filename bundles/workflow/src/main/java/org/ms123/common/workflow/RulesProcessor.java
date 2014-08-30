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
package org.ms123.common.workflow;


import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collection;
import java.util.Date;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.mvel2.MVEL;
import flexjson.*;


/**
 *
 */
@SuppressWarnings("unchecked")
public class RulesProcessor {

	protected JSONSerializer m_js = new JSONSerializer();
	private Map m_rules;
	private Map m_values;
	private Map m_evalParams = new HashMap();
	private int m_paramCount = 0;

	/**
	 */
	public RulesProcessor(Map rules, Map values) {
		m_rules = rules;
		m_values = values;
		m_js.prettyPrint(true);
	}

	public Map execute() throws Exception {
		Map ret = new HashMap();
		System.out.println("RulesProcessor.execute:"+m_js.deepSerialize(m_rules));
		Map variables = (Map) m_rules.get("variables");
		List<Map> inputVars = (List) variables.get("input");
		List<Map> outputVars = (List) variables.get("output");

		convertInput(m_values, toMap(inputVars));

		Map columns = (Map) m_rules.get("columns");
		List<Map> conditionColumns = (List) columns.get("conditions");
		List<Map> actionColumns = (List) columns.get("actions");

		int countRules = getCountRules(conditionColumns);

		for (int i = 0; i < countRules; i++) {
			m_evalParams = new HashMap();
			m_paramCount = 0;
			String exprStr = "";
			String andStr = "";
			for (Map condition : conditionColumns) {
				String varName = (String) condition.get("variableName");
				String varType = (String) condition.get("variableType");
				String op = (String) condition.get("operation");
				List<Object> data = (List) condition.get("data");
				if (i >= data.size()) {
					continue;
				}
				Object value = data.get(i);
				if (value == null) {
					continue;
				}
				if (varType.equals("date")) {
					value = getDate(value, op);
				}
				exprStr += andStr + getOp(varName, op, value + "", varType);
				andStr = " && ";
			}

			Map values = new HashMap(m_evalParams);
			values.putAll(m_values);

			System.out.println("\n\tVariables:" + values);
			System.out.println("\texprStr:" + exprStr);
			boolean ok = true;
			if( exprStr != null && exprStr.length() > 0 ){
				ok = MVEL.evalToBoolean(exprStr, values);
			}
			System.out.println("ok:" + ok);
			if (ok) {
				for (Map action : actionColumns) {
					String varName = (String) action.get("variableName");
					List<Object> data = (List) action.get("data");
					if (i >= data.size()) {
						continue;
					}
					Object value = data.get(i);
					if (value == null) {
						continue;
					}
					ret.put(varName, value);
				}
				return ret;
			}
		}
		return new HashMap();
	}

	private void  convertInput(Map<String, Object> values, Map<String, String> varMeta) {
		System.out.println("convertInput:" + values);
		System.out.println("convertInput:" + varMeta);
		for (String key : values.keySet()) {
			Object value = values.get(key);
			if (value instanceof Long && varMeta.get(key).equals("date")) {
				values.put(key, new Date((Long) value));
			}
		}
	}

	private Map<String, String>  toMap(List<Map> varMeta) {
		Map<String, String> ret = new HashMap();
		for (Map<String, String> m : varMeta) {
			ret.put(m.get("variable"), m.get("vartype"));
		}
		return ret;
	}
	
	private int getCountRules(List<Map> conditions) {
		int count = 0;
		for (Map cond : conditions) {
			List<Object> data = (List) cond.get("data");
			count = Math.max(data.size(), count);
		}
		return count;
	}

	private String getOp(String field, String op, String data, String varType) {

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
			return getEqual(field, data, varType);
		}
		if ("ceq".equals(op)) {
			return getCaseEqual(field, data, varType);
		}
		if ("ne".equals(op)) {
			return getNotEqual(field, data, varType);
		}
		if ("bw".equals(op)) {
			return getBeginsWith(field, data, varType);
		}
		if ("cn".equals(op)) {
			return getContains(field, data, varType);
		}
		if ("in".equals(op)) {
			return field + " is null";
		}
		if ("inn".equals(op)) {
			return field + "is not null";
		}
		return "op not found";
	}

	protected Object getDate(Object data, String op) {
		try {
			Date d = new Date();
			try {
				if (data instanceof Long) {
					d = new Date((Long) data);
				} else {
					d = new Date(Long.valueOf((String) data));
				}
			} catch (Exception e) {}
			if (op.equals("eq") || op.equals("ceq") || op.equals("neq")) {
				String day = d.getDate() + "";
				String month = d.getMonth() + "";
				String year = (d.getYear()) + "";
				data = day + "/" + month + "/" + year;
			} else {
				data = "__param" + m_paramCount;
				m_evalParams.put("__param" + m_paramCount, d);
				m_paramCount++;
			}
		} catch (Exception  e) {
			e.printStackTrace();
			data = "''";
		}
		return data;
	}

	private String  getBeginsWith(String f, String d, String varType) {
		return  f + " ~= (\"(?i)" + d + ".*\")";
	}

	private String  getContains(String f, String d, String varType) {
		return  f + " ~= \"(?i).*" + d + ".*\"";
	}

	private String  getNotEqual(String f, String d, String varType) {
		if (("string".equals(varType) || "text".equals(varType))) {
			return  "!(" + f + " ~= (\"(?i)" + d + "\"))";
		} 
		d = d.toUpperCase();
		if ("date".equals(varType)) {
			String x[] = d.split("/");
			return f + ".getDate()" + " != " + x[0] + " || " + f + ".getMonth()" + " != " + x[1] + " || " + f + ".getYear()" + " != " + x[2];
		} else if ("boolean".equals(varType)) {
			return f + " != " + d;
		} else {
			return f + " != " + d;
		}
	}

	private String  getCaseEqual(String f, String d, String varType) {
		if (("string".equals(varType) || "text".equals(varType))) {
			return  f + " ~= (\"" + d + "\")";
		} else if ("boolean".equals(varType)) {
			return f + " == " + d;
		} else if ("date".equals(varType)) {
			String x[] = d.split("/");
			return f + ".getDate()" + " == " + x[0] + " && " + f + ".getMonth()" + " == " + x[1] + " && " + f + ".getYear()" + " == " + x[2];
		} else if ("boolean".equals(varType)) {
			return f + " == " + d;
		} else {
			return f + " == " + d;
		}
	}

	private String  getEqual(String f, String d, String varType) {
		if (("string".equals(varType) || "text".equals(varType))) {
			return  f + " ~= (\"(?i)" + d + "\")";
		} else if ("date".equals(varType)) {
			String x[] = d.split("/");
			return f + ".getDate()" + " == " + x[0] + " && " + f + ".getMonth()" + " == " + x[1] + " && " + f + ".getYear()" + " == " + x[2];
		} else if ("boolean".equals(varType)) {
			return f + " == " + d;
		} else {
			d = d.toUpperCase();
			return f + " == " + d;
		}
	}

}
