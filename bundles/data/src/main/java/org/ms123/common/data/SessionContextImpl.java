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
package org.ms123.common.data;

import flexjson.JSONSerializer;
import flexjson.JSONDeserializer;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collection;
import java.util.Date;
import javax.jdo.PersistenceManager;
import javax.jdo.Extent;
import javax.jdo.Query;
import javax.jdo.JDOHelper;
import org.ms123.common.data.api.LuceneSession;
import javax.transaction.UserTransaction;
import org.ms123.common.nucleus.api.NucleusService;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.entity.api.EntityService;
import org.ms123.common.permission.api.PermissionService;
import org.ms123.common.setting.api.SettingService;
import org.ms123.common.git.GitService;
import org.ms123.common.team.api.TeamService;
import org.ms123.common.data.query.QueryBuilder;
import org.ms123.common.data.query.SelectBuilder;
import org.ms123.common.data.query.JPASelectBuilderPostgresql;
import javax.transaction.Status;
import org.ms123.common.libhelper.Inflector;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.beanutils.BeanMap;
import org.ms123.common.data.api.DataLayer;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unchecked")
public class SessionContextImpl implements org.ms123.common.data.api.SessionContext{

	protected Inflector m_inflector = Inflector.getInstance();

	private JSONSerializer m_js = new JSONSerializer();
	private SessionManager m_sessionManager;

	public SessionContextImpl( SessionManager sm){
		m_sessionManager = sm;
	}

	private JSONDeserializer m_ds = new JSONDeserializer();

	private String m_configName;

	private StoreDesc m_sdesc;

	private Map m_userProperties;

	private Map<String,Object> m_propertyMap = new HashMap();

	private LuceneSession m_luceneSession;

	private DataLayer m_dataLayer;

	private NucleusService m_nucleusService;

	private EntityService m_entityService;
	private GitService m_gitService;

	private SettingService m_settingService;

	private PermissionService m_permissionService;

	private TeamService m_teamService;

	private String m_primaryKey = "id";
	private String m_filterPath = null;

	public void setUserProperties(Map data) {
		m_userProperties = data;
	}

	public Map getUserProperties() {
		return m_userProperties;
	}

	public void setDataLayer(DataLayer data) {
		m_dataLayer = data;
	}

	public DataLayer getDataLayer() {
		return m_dataLayer;
	}

	public StoreDesc getStoreDesc() {
		return m_sdesc;
	}

	public void setStoreDesc(StoreDesc data) {
		m_sdesc = data;
	}

	public void setEntityService(EntityService data) {
		m_entityService = data;
	}

	public void setSettingService(SettingService data) {
		m_settingService = data;
	}
	public void setGitService(GitService data) {
		m_gitService = data;
	}

	public void setPermissionService(PermissionService data) {
		m_permissionService = data;
	}
	public PermissionService getPermissionService() {
		return m_permissionService;
	}
	public TeamService getTeamService() {
		return m_teamService;
	}

	public void setTeamService(TeamService data) {
		m_teamService = data;
	}

	public EntityService getEntityService() {
		return m_entityService;
	}

	public SettingService getSettingService() {
		return m_settingService;
	}

	public GitService getGitService() {
		return m_gitService;
	}

	public void setNucleusService(NucleusService data) {
		m_nucleusService = data;
	}

	public NucleusService getNucleusService() {
		return m_nucleusService;
	}

	public UserTransaction getUserTransaction() {
		return m_nucleusService.getUserTransaction();
	}

	public ClassLoader getClassLoader() {
		return m_nucleusService.getClassLoader(m_sdesc);
	}

	public Class getClass(StoreDesc sdesc, String className) {
		return m_nucleusService.getClass(sdesc, className);
	}

	public Class getClass(String className) {
		return m_dataLayer.getClass(this, className);
	}

	public Object createObject(String entityName) {
		return m_dataLayer.createObject(this, entityName);
	}

	public List validateObject(Object objectInsert) {
		return m_dataLayer.validateObject(this, objectInsert);
	}

	public List validateObject(Object objectInsert, String entityName) {
		return m_dataLayer.validateObject(this, objectInsert, entityName);
	}

	public void insertIntoMaster(Object objectInsert, String entityName, Class masterClazz, String fieldName, Object masterId) throws Exception {
		m_dataLayer.insertIntoMaster(this, objectInsert, entityName, masterClazz, fieldName, masterId);
	}

	public void makePersistent(Object objectInsert) {
		m_dataLayer.makePersistent(this, objectInsert);
	}

	public void populate(Map from, Object to) {
		m_dataLayer.populate(this, from, to, null);
	}

	public void populate(Map from, Object to, Map hintsMap) {
		m_dataLayer.populate(this, from, to, hintsMap);
	}

	public void evaluteFormulas(String entityName, Map<String, Object> mapInsert) {
		m_dataLayer.evaluteFormulas(this, entityName, mapInsert, "in");
	}

	public void deleteObject(String entityName, Object id) throws Exception {
		m_dataLayer.deleteObject(this, null, entityName, String.valueOf(id));
	}

	public Object getObjectById(Class clazz, Object id) {
		return getObjectById(m_sdesc, clazz, id);
	}

	public Object getObjectById(StoreDesc sdesc, Class clazz, Object id) {
		try {
			return getPM(sdesc).getObjectById(clazz, id);
		} catch (javax.jdo.JDOObjectNotFoundException e) {
		}
		return null;
	}
	public List<Object> persistObjects(Object obj, Map<String,Object> persistenceSpecification){
		return MultiOperations.persistObjects(this,obj,persistenceSpecification,-1);
	}
	public Object getObjectByAttr(Class clazz, String attr, Object value) {
		String filter = null;
		if( value instanceof String){
			filter = attr + "==" + "\""+ value + "\"";
		}else{
			filter = attr + "=" + value ;
		}
		debug("SessionManager.getObjectByAttr.filter:"+filter);
		Extent e = getPM().getExtent(clazz, true);
		Query q = getPM().newQuery(e, filter);
		try {
			Collection coll = (Collection) q.execute();
			Iterator iter = coll.iterator();
			if (iter.hasNext()) {
				Object c = iter.next();
				debug("\tfound:"+c);
				return c;
			}
		} finally {
			q.closeAll();
		}
		debug("\tnot found");
		return null;
	}

	public Object getObjectByFilter(Class clazz, String filter) {
		Extent e = getPM().getExtent(clazz, true);
		Query q = getPM().newQuery(e, filter);
		try {
			Collection coll = (Collection) q.execute();
			Iterator iter = coll.iterator();
			if (iter.hasNext()) {
				Object c = iter.next();
				return c;
			}
		} finally {
			q.closeAll();
		}
		return null;
	}

	public List getListByFilter(Class clazz, String filter) {
		List retList = new ArrayList();
		Extent e = getPM().getExtent(clazz, true);
		Query q = getPM().newQuery(e, filter);
		try {
			Collection coll = (Collection) q.execute();
			Iterator iter = coll.iterator();
			if (iter.hasNext()) {
				Object c = iter.next();
				retList.add(c);
			}
		} finally {
			q.closeAll();
		}
		return retList;
	}

	public Map deleteObjectById(String entityName, String id) throws Exception{
		return m_dataLayer.deleteObject(this, null, entityName, id);
	}

	public Map getObjectMapById(String entityName, String id) {
		return m_dataLayer.getObject(m_sdesc, entityName, id);
	}
	public Map insertObjectMap(Map data, String entityName) throws Exception{
		return m_dataLayer.insertObject(this,data, entityName);
	}
	public Map updateObjectMap(Map data, String entityName, String id) throws Exception{
		Map hints = new HashMap();
		hints.put("create", false);
		return m_dataLayer.updateObject(this,data, hints, entityName,id);
	}

	public void retrieve(Object o) {
		getPM().retrieve(o);
	}

	public void handleFinally() {
		handleFinally(null);
	}

	public void handleFinally(UserTransaction ut) {
	}

	public void handleException(Throwable e) {
		handleException(null, e);
	}

	public void handleException(UserTransaction ut, Throwable e) {
		m_sessionManager.handleException(ut,e);
	}

	public Map executeNamedFilter(String name) {
		return executeNamedFilter(name, new HashMap());
	}

	public Map getNamedFilter(String name) {
		name = getName(name);
		String filterJson = m_gitService.searchContent( m_sdesc.getNamespace(), name, "sw.filter" );
		Map contentMap = (Map) m_ds.deserialize(filterJson);
		return contentMap;
	}

	private boolean isEmpty(String s) {
		return (s == null || "".equals(s.trim()));
	}
	private List<String> getFilterParameter(Map filter) {
		String label = (String) filter.get("label");
		List<String> params = new ArrayList();
		if (filter.get("connector") == null && label != null) {
			if (label.matches("^[a-zA-Z].*")) {
				params.add(label);
			}
		}
		List<Map> children = (List) filter.get("children");
		if( children != null){
			for (Map<String,Object> c : children) {
				params.addAll(getFilterParameter(c));
			}
		}
		return params;
	}
	private void getMissingFilterParameter(Map<String, Object> filter, List<String> missingParamList,Map params) {
		String label = (String) filter.get("label");
		if (filter.get("connector") == null && label != null) {
			if (label.matches("^[a-zA-Z].*")) {
				if(params.get(label) == null){
					missingParamList.add(label);
				}
			}
		}
		List<Map> children = (List) filter.get("children");
		if( children != null){
			for (Map<String,Object> c : children) {
				getMissingFilterParameter(c, missingParamList,params);
			}
		}
	}

	public Map executeNamedFilter(String name, Map<String, Object> fparams) {
		return executeNamedFilter(name,fparams,new HashMap());
	}
	public Map executeNamedFilter(String name, Map<String, Object> fparams, Map<String, Object> options) {
		name = getName(name);
		String filterJson = m_gitService.searchContent( m_sdesc.getNamespace(), name, "sw.filter" );
		Map contentMap = (Map) m_ds.deserialize(filterJson);
		return executeFilter(contentMap,fparams,options);
	}

	public Map executeFilter(Map filterDesc, Map<String, Object> fparams) {
		return executeFilter(filterDesc,fparams, new HashMap());
	}
	public Map executeFilter(Map filterDesc, Map<String, Object> fparams,  Map<String, Object> options) {
		List<String> missingParamList = new ArrayList();
		if( getBoolean(options, CHECK_PARAMS, false )){
			getMissingFilterParameter((Map)filterDesc.get("filter"), missingParamList,fparams);
			if( missingParamList.size() > 0){
				Map ret = new HashMap();
				ret.put("missingParamList", missingParamList);
				return ret;
			}
		}
		String entityName = (String)filterDesc.get("modulename");
		m_js.prettyPrint(true);
		debug("executeFilter:"+m_js.deepSerialize(filterDesc));

		List<String> aliasList = new ArrayList();
		List<String> fieldList = new ArrayList();
		List<Map> fieldsArray = (List)filterDesc.get("fields");
		if( fieldsArray == null){
			List<Map> rfields = getReportFields(entityName);
			for (Map f : rfields) {
				fieldList.add((String) entityName+"."+f.get("name"));
				aliasList.add((String) f.get("name"));
			}
		}else{
			for (Map f : fieldsArray) {
				if ((Boolean) f.get("display")) {
					fieldList.add((String) f.get("path") + "." + (String) f.get("id"));
					aliasList.add((String) f.get("mapping"));
				}
			}
		}
		String orderby=null; //Needs a better solution
		for( String field : fieldList){
			if( StringUtils.countMatches(field,"$") ==0){
				orderby=field;
				break;
			}
		}
		debug("orderby:"+orderby);
		debug("fieldList:"+fieldList);
		List moduleList = new ArrayList();
		String clazzName = m_inflector.getClassName(entityName);
		moduleList.add(clazzName);
		Map<String, Object> params = new HashMap(options);
		params.put("fields", m_js.serialize(moduleList));
		Map filter = (Map)filterDesc.get("filter");
		filter = addExclusionFilter(filter,(List)filterDesc.get("exclusion"));
		debug("FilterWith:"+m_js.deepSerialize(filter));
		params.put("filter", filter);
		params.put("orderby", orderby);
		params.put("filterParams", fparams);
		if( params.get("pageSize") == null){
			params.put("pageSize", "0");
		}
		Map<String, Object> ret = m_dataLayer.query(this, params, m_sdesc, entityName);
		List<Map> rows = (List) ret.get("rows");
		List<Map> retList = new ArrayList();
		boolean isAdmin = m_permissionService.hasAdminRole();
		if( !isAdmin || !getBoolean(options, GET_OBJECT, false) ){
			for (Map row : rows) {
				Object obj = row.get(clazzName);
				retList.add(SojoFilterInterceptor.filterFields(obj, this, fieldList, aliasList));
			}
		}
		boolean withMeta = getBoolean(options, "withMeta", false );
		if( withMeta){
			Map meta = new HashMap();
			meta.put("params", getFilterParameter(filterDesc));
			meta.put("fields", Utils.prepareFields(fieldList,aliasList));
			meta.put("aliases", aliasList);
			ret.put("meta",meta);
		}
		ret.put("rows", retList);
		return ret;
	}
	private List<Map> getReportFields(String entityName) {
		try {
			return getSettingService().getFieldsForEntityView(getStoreDesc().getNamespace(), entityName, "report");
		} catch (Exception e) {
			return new ArrayList();
		}
	}

	public List query(String entityName, Map filtersMap) {
		Map params = new HashMap();
		params.put("filter", filtersMap);
		params.put("pageSize", 0);
		Map ret = m_dataLayer.query(this, params, m_sdesc, entityName);
		return (List) ret.get("rows");
	}

	public List<Object> query(String entityName, String filter) {
		Map params = new HashMap();
		params.put("filters", filter);
		params.put("pageSize", 0);
		Map ret = m_dataLayer.query(this, params, m_sdesc, entityName);
		return (List) ret.get("rows");
	}
	public Map getEntitytype(String name) {
		return m_entityService.getEntitytype(getStoreDesc().getStoreId(), name);
	}

	public boolean hasTeamPermission(Object o) {
		long startTime = new Date().getTime();
		Object teams = null;
		try {
			if (!(o instanceof Collection)) {
				if (PropertyUtils.isReadable(o, "_team_list")) {
					teams = PropertyUtils.getProperty(o, "_team_list");
				}
			}
			if (teams != null) {
				boolean b = m_teamService.checkTeams(getStoreDesc().getNamespace(), getUserName(), getUserProperties(), (Collection<Object>) teams);
				long endTime = new Date().getTime();
				System.err.println("hasTeamPermission.time:" + (endTime - startTime) + "/allowed:" + b);
				return b;
			}
		} catch (Exception e) {
			throw new RuntimeException("SessionContext.hasTeamPermission:", e);
		}
		return true;
	}

	public Map getPermittedFields(String entityName) {
		return getPermittedFields(entityName, "read");
	}
	public Map getPermittedFields(String entityName,String actions) {
		Map permittedFields = m_sessionManager.getPermittedFieldsMap(entityName.toLowerCase()+"/"+actions);
		if (permittedFields == null) {
			permittedFields = m_entityService.getPermittedFields(getStoreDesc(), entityName.toLowerCase(),actions);
			setPrimaryKey(permittedFields);
			m_sessionManager.setPermittedFieldsMap(entityName.toLowerCase()+"/"+actions, permittedFields);
		}
		return permittedFields;
	}

	public boolean isFieldPermitted(String fieldName, String entityName) {
		return isFieldPermitted(fieldName, entityName, "read");
	}
	public boolean isFieldPermitted(String fieldName, String entityName,String actions) {
		Map permittedFields = m_sessionManager.getPermittedFieldsMap(entityName.toLowerCase()+"/"+actions);
		if (permittedFields == null) {
			permittedFields = m_entityService.getPermittedFields(getStoreDesc(), entityName.toLowerCase(), actions);
			m_sessionManager.setPermittedFieldsMap(entityName.toLowerCase()+"/"+actions, permittedFields);
		}
		return permittedFields.get(fieldName) == null ? false : true;
	}

	public PersistenceManager getPM() {
		return m_sessionManager.getPM(m_sdesc);
	}

	public PersistenceManager getPM(StoreDesc sdesc) {
		return m_sessionManager.getPM(sdesc);
	}

	public LuceneSession getLuceneSession() {
		return m_luceneSession;
	}

	public void setLuceneSession(LuceneSession data) {
		m_luceneSession = data;
	}

	public String getUserName() {
		return getThreadContext().getUserName();
	}

	public boolean hasAdminRole() {
		return m_permissionService.hasRole("admin");
	}

	public String getConfigName() {
		return "global";
	}

	public void setConfigName(String data) {
		m_configName = data;
	}

	public String getPrimaryKey() {
		return m_primaryKey;
	}

	public void setPrimaryKey(String data) {
		m_primaryKey = data;
	}

	private void setPrimaryKey(Map<String, Map> configMap) {
		for (String key : configMap.keySet()) {
			Map cm = configMap.get(key);
			if (cm != null && cm.get("primary_key") != null &&  ((Boolean)cm.get("primary_key"))) {
				setPrimaryKey(key);
			}
		}
	}
	private String getName(String s){
		int i = s.lastIndexOf("/");
		if( i == -1){
			return s;
		}
		return s.substring(i+1);
	}
	private boolean getBoolean(Map m, String key, boolean _def) {
		try {
			return (Boolean) m.get(key);
		} catch (Exception e) {
		}
		return _def;
	}
	private Map addExclusionFilter(Map filter, List<Map> exclusion){
		if (exclusion != null && exclusion.size() > 0) {
			Map idFilter = createIdFilter(exclusion);
			Map newFilter = new HashMap();
			newFilter.put("connector", "except");
			List<Map> children = new ArrayList();
			newFilter.put("children", children);

			children.add(filter);
			children.add(idFilter);
			return newFilter;
		}else{
			return filter;
		}
	}
	private Map createIdFilter(List<Map> exclusion) {
		Map ret = new HashMap();
		ret.put("connector", "or");
		List<Map> children = new ArrayList();
		ret.put("children", children);
		if (exclusion.size() == 0) {
			return null;
		}
		for (Map ex : exclusion) {
			String id = (String)ex.get("id");
			Map cmap = new HashMap();
			cmap.put("field", "id");
			cmap.put("op", "eq");
			cmap.put("data", id);
			cmap.put("connector", null);
			children.add(cmap);
		}
		return ret;
	}

	public void setProperty(String key, Object value){
		m_propertyMap.put(key,value);
	}
	public Object getProperty(String key){
		return m_propertyMap.get(key);
	}
	private org.ms123.common.system.ThreadContext getThreadContext() {
		return org.ms123.common.system.ThreadContext.getThreadContext();
	}
	protected static void debug(String message) {
		m_logger.debug(message);
	}

	protected static void info(String message) {
		m_logger.info(message);
		System.out.println(message);
	}

	private static final Logger m_logger = LoggerFactory.getLogger(SessionContextImpl.class);
}
