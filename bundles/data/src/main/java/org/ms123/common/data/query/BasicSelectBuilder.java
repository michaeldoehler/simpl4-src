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

import java.util.*;
import org.apache.commons.beanutils.*;
import java.lang.reflect.*;
import java.lang.annotation.*;
import com.google.common.base.*;
import com.google.common.collect.*;
import org.ms123.common.libhelper.Inflector;
import org.ms123.common.utils.annotations.RelatedTo;
import org.ms123.common.utils.TypeUtils;
import java.lang.annotation.*;
import javax.jdo.annotations.Element;
import org.ms123.common.libhelper.Inflector;
import org.ms123.common.data.api.SessionContext;
import static org.ms123.common.entity.api.Constants.STATE_OK;
import static org.ms123.common.entity.api.Constants.STATE_NEW;
import static org.ms123.common.entity.api.Constants.STATE_FIELD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;
import org.ms123.common.store.StoreDesc;

@SuppressWarnings("unchecked")
public abstract class BasicSelectBuilder {

	private JSONDeserializer m_ds = new JSONDeserializer();

	protected Inflector m_inflector = Inflector.getInstance();

	private List<String> m_selectorList = new ArrayList<String>();

	protected StoreDesc m_sdesc;

	private List<String> m_joinFields = new ArrayList<String>();
	private List<String> m_sortedUniqueList = null;

	protected String m_entityName;

	protected Map m_filters;

	protected Map m_fieldSets;

	protected QueryBuilder m_queryBuilder;

	public BasicSelectBuilder(QueryBuilder qb, StoreDesc sdesc, String entityName, List<String> joinFields, Map filters, Map fieldSets) {
		m_queryBuilder = qb;
		m_sdesc = sdesc;
		m_entityName = entityName;
		m_filters = filters;
		m_fieldSets = fieldSets;
		m_joinFields = joinFields;
	}

	public String getWhere() {
		debug("getWhere.filter:" + m_filters);
		if (m_filters == null) {
			return null;
		}
		List children = (List) m_filters.get("children");
		if( children.size() == 0 && m_filters.get("field") == null){
			return "1=1";
		}
		
		String where = processFilterNode(m_filters);
		debug("getWhere.where:" + where);
		return where;
	}

	public String getTeamSecurityWhere(){
		if(isAdmin()){
			return null;
		}
		String where = null;
		String and = " and ";
		for( String x : m_sortedUniqueList){
			debug("getTeamSecurityWhere:"+x);
			if( hasTeamSecurity(x) ){
				if( where == null){
					where ="";
					and = "";
				}
				where += and +getTeamSecurityWhere(x);
				and = " and ";
			}
		}
		return where;
	}

	public String getTeamUserWhere(){
		if(isAdmin()){
			return null;
		}
		if( m_queryBuilder.hasTeamSecurity()){
			return null;
		}
		String where = null;
		List<String> uniqueList = uniqueList(m_selectorList);
		String and = " and ";
		for(String sel : uniqueList){
			debug("getTeamUserWhere:"+sel);
			if( sel.indexOf("$_team_list")!=-1){
				if( where == null){
				  where ="";
					and = "";
				}
debug("\t:"+getTeamUserWhere(sel));
				where += and +getTeamUserWhere(sel);
				and = " and ";
			}
		}
		return where;
	}

	protected abstract Map getConnector(Object con);

	protected abstract String getTeamSecurityWhere(String sel);
	protected abstract String getTeamUserWhere(String field);

	protected String getOr() {
		return "or";
	}

	protected String getNot() {
		return "not";
	}

	private String processFilterNode(Map filter) {
		String result = "";
		Map cons = getConnector(filter.get("connector"));
		debug("> processFilterNode.filter:" + filter + ":" + cons);
		List children = (List) filter.get("children");
		int csize = (children != null) ? children.size() : 0;
		if (csize == 0) {
			result = "(";
			children = new ArrayList();
			children.add(filter);
			csize = 1;
			cons = getConnector("and");
		} else {
			result = getBoolean(cons, "not_all", false) ? "NOT (" : "(";
		}
		for (int i = 0; i < csize; i++) {
			Map c = (Map) children.get(i);
			if (i == 0) {
			} else if (i == 1) {
				result += cons.get("c_i1") + " ";
				if (getBoolean(cons, "not_except", false)) {
					result += " " + getNot() + " (";
				}
			} else {
				result += cons.get("c_in") + " ";
			}
			if (c.get("connector") == null) {
				String fieldname = "";
				Object o = c.get("field");
				if (o instanceof String) {
					fieldname = (String) c.get("field");
				}
				if (o instanceof Map) {
					fieldname = (String) ((Map) c.get("field")).get("id");
					if (fieldname == null) {
						fieldname = (String) ((Map) c.get("field")).get("value");
					}
				}
				Map fs = null;
				int dot = fieldname.indexOf(".");
				if (dot != -1 && fieldname.indexOf("$") != -1) {
					String fm[] = getFieldAndEntityName(fieldname, dot);
					Map fsets = m_queryBuilder.getFieldSets(fm[1]);
					fs = (Map) ((fsets != null) ? fsets.get(fm[0]) : null);
				} else {
					fs = (Map) ((m_fieldSets != null) ? m_fieldSets.get(fieldname) : null);
				}
				String cond = "";
				if (fs != null) {
					cond = "(";
					String or = "";
					String mname = null;
					if (fieldname.indexOf("$") != -1) {
						mname = (dot != -1) ? fieldname.substring(0, dot) : null;
					}
					for (Object f : (List) fs.get("fields")) {
						Map m = new HashMap();
						if (mname != null) {
							m.put("field", mname + "." + f);
						} else {
							m.put("field", f);
						}
						m.put("op", c.get("op"));
						m.put("data", c.get("data"));
						cond += or + getCondition(m);
						or = " " + getOr() + " ";
					}
					cond += ")";
				} else {
					cond = getCondition(c);
				}
				result += cond;
			} else {
				result += processFilterNode(c);
			}
			if (i == (csize - 1)) {
				if (getBoolean(cons, "not_except", false)) {
					result += ")";
				}
			}
		}
		if (csize > 0) {
			result += (getBoolean(cons, "not_all", false)) ? ")" : ")";
		}
		return result;
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
			data = ":param" + m_queryBuilder.getParamCount();
			m_queryBuilder.getQueryParams().put("param" + m_queryBuilder.getParamCount(), d);
			m_queryBuilder.incParamCount();
		} catch (Exception e) {
			e.printStackTrace();
			data = "''";
		}
		return data;
	}

	protected abstract String getOp(String field, String op, Object data, Map<String, String> c);

	protected String getCondition(Map<String, Object> rule) {
		Object o = rule.get("field");
		String fullfieldname = "";
		if (o instanceof String) {
			fullfieldname = (String) rule.get("field");
		}
		if (o instanceof Map) {
			fullfieldname = (String) ((Map) rule.get("field")).get("id");
		}
		String[] f = fullfieldname.split("\\.");
		String entityName = (f.length == 2) ? f[0] : m_entityName;
		String selector = entityName;
		debug("getCondition:"+fullfieldname+"|"+selector);
		addSelector(entityName);
		entityName = m_queryBuilder.getEntityForPath(entityName);
		Map configMap = m_queryBuilder.getPermittedFields(entityName);
		String fieldname = (f.length == 2) ? f[1] : fullfieldname;
		Map c = (Map) configMap.get(fieldname);
		if (fieldname.equals("id")) {
			c = new HashMap();
			c.put("datatype", "string");
		}
		if (fieldname.startsWith("_exists_")) {
			String filterName = (String) rule.get("op");
			Map filterObject = m_queryBuilder.getSessionContext().getNamedFilter(filterName);
			if (filterObject == null) {
				throw new RuntimeException("BasicSelectBuilder.getCondition:Filter(" + filterName + ") not found");
			}
			String subEntityName = null;
			Map  filterMap = null;
			try {
				subEntityName = (String)filterObject.get("modulename");
				filterMap = (Map)filterObject.get("filter");
			} catch (Exception e) {
				throw new RuntimeException("SessionContext.executeNamedFilter:", e);
			}
			m_queryBuilder.insertFilterParams(filterMap);
			SelectBuilder qb = m_queryBuilder.getSelectBuilder(subEntityName, filterMap);
			String whereClause = "where " + qb.getWhere();
			String from = qb.getFrom(null);
			String select = "Select id from " + from + " " + whereClause + " and " + rule.get("data");
			String exists = fieldname.equals("_exists_not_subselect") ? "NOT EXISTS" : "EXISTS";
			return exists + " (" + select + ")";
		}
		if (c == null) {
			throw new RuntimeException("Query:Field \"" + fieldname + "\" not found in " + m_sdesc + "/" + entityName);
		}
		Object data = rule.get("data");
		if (c.get("datatype").equals("date")) {
			data = getDate(data, rule);
		}
		String op = getOp(selector + "." + fieldname, (String) rule.get("op"), data, c);
		return "(" + op + ")";
	}

	protected boolean isAdmin(){
		return m_queryBuilder.getSessionContext().getPermissionService().hasAdminRole();
	}

	protected boolean getBoolean(Map m, String key, boolean def) {
		try {
			return (Boolean) m.get(key);
		} catch (Exception e) {
		}
		return def;
	}

	public List<String> getProjectionListEntity(String entity, String parent) {
		List<String> list = new ArrayList<String>();
		String mn = m_inflector.getEntityName(entity);
		String clazz = m_inflector.getClassName(mn);
		list.addAll(getProjectionFromClass(clazz, parent));
		return list;
	}

	public List<String> getProjectionListAll(String entity) {
		List<String> list = new ArrayList<String>();
		String clazz = m_inflector.getClassName(entity);
		list.addAll(getProjectionFromClass(clazz, entity));
		return list;
	}

	public List<String> getProjectionFromClass(String clazz, String alias) {
		clazz = m_inflector.getClassName(clazz);
		List<String> list = new ArrayList<String>();
		Object o = null;
		try {
			Class c = m_queryBuilder.getClass(clazz);
			o = c.newInstance();
			BeanMap beanMap = new BeanMap(o);
			Iterator itv = beanMap.keyIterator();
			String komma = "";
			while (itv.hasNext()) {
				String prop = (String) itv.next();
				boolean isRelatedTo = false;
				try {
					Field field = o.getClass().getDeclaredField(prop);
					if (field != null) {
						isRelatedTo = field.isAnnotationPresent(RelatedTo.class);
					}
				} catch (Exception e) {
				}
				debug("\tName:" + prop + " -> " + beanMap.getType(prop) + "," + isRelatedTo); 
				if (clazz.equals("Document") && prop.equals("text")) {
					continue;
				}
				if( !isAdmin() && STATE_FIELD.equals(prop)){
					continue;
				}
				Class type = beanMap.getType(prop);				
				if ((TypeUtils.isPrimitiveType(type) && !type.equals(byte[].class)) || isRelatedTo ) {
					if (alias != null) {
						list.add(alias + "." + prop);
					} else {
						list.add(prop);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (alias != null && !alias.startsWith("v_")) {
			m_selectorList.add(alias);
		}
		debug("Projection for " + clazz + ":" + list);
		debug("Projection.m_selectorList:" + m_selectorList);
		return list;
	}

	public void addSelector(String selector) {
		if(selector.startsWith("v_")) return;
		int ind = selector.indexOf('.');
		if (ind != -1) {
			selector = selector.substring(0, ind);
		}
		m_selectorList.add(selector);
	}

	public void addSelectors(List<String> fields) {
		for (String field : fields) {
			int ind = field.indexOf('.');
			if (ind != -1) {
				addSelector(field.substring(0, ind));
			}
		}
	}

	public List<String> getInvolvedEntity() {
		List<String> uniqueList = uniqueList(m_selectorList);
		debug("uniqueList:"+uniqueList);
		List<String> entityList = new ArrayList();
		for (String part : uniqueList) {
			entityList.add(m_queryBuilder.getEntityForPath(part));
		}
		debug("entityList:"+entityList);
		return entityList;
	}

	public String getFrom(String jointype) {
		if (jointype == null) {
			jointype = "left outer join";
		}
		List<String> uniqueList = uniqueList(m_selectorList);
		Ordering<String> orderByCountDollars = new Ordering<String>() {

			public int compare(final String s1, final String s2) {
				return CharMatcher.is('$').countIn(s1) - CharMatcher.is('$').countIn(s2);
			}
		};
		List<String> sortedUniqueList = orderByCountDollars.sortedCopy(uniqueList);
		m_sortedUniqueList = sortedUniqueList;
		sortedUniqueList = checkAndCorrectList(sortedUniqueList);
		String main = sortedUniqueList.get(0);
		String clazz = m_inflector.getClassName(main);
		String from = "\"" + clazz + "\" " + main;
		for (int i = 1; i < sortedUniqueList.size(); i++) {
			String e = sortedUniqueList.get(i);
			int ind = e.lastIndexOf("$");
			String alias = null;
			if (ind != -1) {
				alias = e.substring(ind + 1);
				String join = e.substring(0, ind) + "." + e.substring(ind + 1);
				from += " " + jointype + " " + join + " " + e;
			} else {
				alias = e;
				String join = main + "." + e;
				from += " " + jointype + " " + join + " " + e;
			}
			if(hasStateSelect(alias) && !stateSelectDisabled()){
				String state = getRequestedState();
				if( state.equals(STATE_OK) || state.equals(STATE_NEW)){
					from += " on ("+alias+"."+STATE_FIELD+" is null or "+alias+"."+STATE_FIELD+"='"+state+"')";
				}else{
					from += " on ("+alias+"."+STATE_FIELD+"='"+state+"')";
				}
			}
		}
		for( String x : sortedUniqueList){
			debug("From:"+x);
			if( hasTeamSecurity(x) ){
				debug("BasicSelectBuilder.getFrom.selector:"+x);
				if( from.indexOf(x+"$_team_list") == -1){
					from += " left outer join "+x+"._team_list "+x+"$_team_list";
				}
			}
		}
		return from;
	}

	protected static List<String> checkAndCorrectList(List<String> inList) {
		List<String> outList = new ArrayList();
		int lastCount = -1;
		for (String elem : inList) {
			String sub = "";
			StringTokenizer st = new StringTokenizer(elem, "$");
			while (st.hasMoreTokens()) {
				int sl = sub.length();
				sub += ((sl > 0) ? "$" : "") + st.nextToken();
				if (!outList.contains(sub)) {
					outList.add(sub);
				}
			}
		}
		return outList;
	}

	protected String[] getFieldAndEntityName(String fullfieldname, int dot) {
		String entityName = fullfieldname.substring(0, dot);
		entityName = m_queryBuilder.getEntityForPath(entityName);
		String fieldname = fullfieldname.substring(dot + 1);
		String[] ret = new String[2];
		ret[0] = fieldname;
		ret[1] = entityName;
		return ret;
	}

	private List<String> uniqueList(List<String> list) {
		List<String> uniqueList = Lists.newArrayList(Iterables.filter(list, new Predicate<String>() {

			private Map<String, String> m = new HashMap<String, String>();

			public boolean apply(final String s) {
				if (m.get(s) != null) {
					return false;
				} else {
					m.put(s, "");
					return true;
				}
			}
		}));
		return uniqueList;
	}
	public boolean stateSelectDisabled(){
		return m_queryBuilder.stateSelectDisabled();
	}

	public String getRequestedState(){
		return m_queryBuilder.getRequestedState();
	}

	private boolean hasStateSelect(String selector) {
		String entityName = m_queryBuilder.getEntityForPath(selector);
		return m_queryBuilder.hasStateSelect(entityName);
	}
	private boolean hasTeamSecurity(String selector) {
		String entityName = m_queryBuilder.getEntityForPath(selector);
		if( "team".equals(entityName)) return false;
		SessionContext sc = m_queryBuilder.getSessionContext();
		Map entityMap = sc.getEntitytype(entityName);
		boolean hasTeamSecurity = getBoolean(entityMap, "team_security", false);
		debug("hasTeamSecurity"+entityName+"):" + hasTeamSecurity);
		return hasTeamSecurity;
	}
	protected void debug(String msg) {
		//System.out.println(msg);
		m_logger.debug(msg);
	}
	protected void info(String msg) {
		System.out.println(msg);
		m_logger.info(msg);
	}
	private static final org.slf4j.Logger m_logger = org.slf4j.LoggerFactory.getLogger(BasicSelectBuilder.class);
}
