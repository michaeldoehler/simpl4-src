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

import aQute.bnd.annotation.component.*;
import aQute.bnd.annotation.metatype.*;
import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.jdo.Extent;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import org.apache.commons.beanutils.BeanMap;
import org.apache.commons.beanutils.PropertyUtils;
import org.ms123.common.activiti.ActivitiService;
import org.ms123.common.data.api.DataLayer;
import org.ms123.common.data.api.SessionContext;
import org.ms123.common.data.query.QueryBuilder;
import org.ms123.common.data.scripting.MVELEvaluator;
import org.ms123.common.libhelper.Bean2Map;
import org.ms123.common.libhelper.Inflector;
import org.ms123.common.nucleus.api.NucleusService;
import org.ms123.common.permission.api.PermissionService;
import org.ms123.common.setting.api.SettingService;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.system.ThreadContext;
import org.ms123.common.team.api.TeamService;
import org.ms123.common.utils.UtilsServiceImpl;
import org.ms123.common.camel.api.CamelService;
import org.mvel2.MVEL;
import org.mvel2.optimizers.OptimizerFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.apache.commons.beanutils.PropertyUtils.getProperty;
import static org.apache.commons.beanutils.PropertyUtils.setProperty;
import org.apache.camel.ProducerTemplate;

@SuppressWarnings("unchecked")
@Component(enabled = true, configurationPolicy = ConfigurationPolicy.optional, immediate = true)
public class TriggerServiceImpl implements TriggerService {

	private BlueprintContainer m_blueprintContainer;
	private BundleContext m_bundleContext;

	private SettingService m_settingService;
	private TeamService m_teamService;

	private NucleusService m_nucleusService;

	private ActivitiService m_activitiService;
	private CamelService m_camelService;

	private PermissionService m_permissionService;

	private final String ENTITY = "entity";

	private static JSONSerializer m_js = new JSONSerializer();

	private static JSONDeserializer m_ds = new JSONDeserializer();

	protected static Inflector m_inflector = Inflector.getInstance();

	public void activate() {
		MVELEvaluator evalator = new MVELEvaluator(null);
		evalator.test();
		OptimizerFactory.setDefaultOptimizer("reflective");
		m_js.prettyPrint(true);
	}

	protected void activate(BundleContext bundleContext, Map<?, ?> props) {
		info("TriggerService.activate:");
		m_bundleContext = bundleContext;
	}
	public Map applyUpdateRules(SessionContext sessionContext, String entityName, Object update, Object preUpdate) throws Exception {
		return applyRules(sessionContext, entityName, update, preUpdate, UPDATE);
	}

	public Map applyInsertRules(SessionContext sessionContext, String entityName, Object insert) throws Exception {
		return applyRules(sessionContext, entityName, insert, new HashMap(), INSERT);
	}

	public Map applyDeleteRules(SessionContext sessionContext, String entityName, Object insert) throws Exception {
//		Map preUpdate = new HashMap(new BeanMap(insert));
		Map preUpdate = UtilsServiceImpl.copyObject(insert);
		if (PropertyUtils.isReadable(insert, "_team_list")) {
			PropertyUtils.setProperty(insert, "_team_list", new HashSet());
		}
		return applyRules(sessionContext, entityName, insert, preUpdate, DELETE);
	}

	public Map applyRules(SessionContext sessionContext, String entityName, Object insert, Object preUpdate, int operation) throws Exception {
		String user = sessionContext.getUserName();
		StoreDesc sdesc = sessionContext.getStoreDesc();
		String entity = entityName;
		String config = sessionContext.getConfigName();
		Map params = new HashMap();
		params.put(ENTITY, entity);
		String applies_to = (operation == INSERT) ? "new" : "updated";
		Map system = new HashMap();
		system.put("username", user);
		system.put(StoreDesc.NAMESPACE, sdesc.getNamespace());
		params.put(StoreDesc.PACK, sdesc.getPack());
		params.put(StoreDesc.STORE, sdesc.getStore());
		List<Map> triggerList = null;
		try {
			triggerList = getTriggers(sessionContext.getDataLayer(), sdesc, entity, applies_to);
		} catch (Exception e) {
			throw e;
		}
		m_js.prettyPrint(true);
		for (Map triggerMap : triggerList) {
			if(isDebug()){
				debug("TriggerMap:" + m_js.deepSerialize(triggerMap));
			}
			Map triggerInfo = new HashMap();
			triggerInfo.put("operation", operation);
			boolean conditionsOk = testTriggerConditions(sessionContext, triggerMap, sdesc, entity, config, insert, preUpdate,triggerInfo);
			debug("Trigger.conditionsOk:" + conditionsOk);
			if (conditionsOk) {
				executeTriggerActions(triggerMap, sessionContext, entity, insert,triggerInfo);
			}
		}
		return null;
	}

	private boolean testTriggerConditions(SessionContext sessionContext, Map triggerMap, StoreDesc sdesc, String entity, String config, Object insert, Object preUpdate, Map triggerInfo) throws Exception {
		int operation = (Integer)triggerInfo.get("operation");
		List<Map> conditions = (List) triggerMap.get("conditions");
		Map fieldSets = m_settingService.getFieldSets(config, sdesc.getNamespace(), m_inflector.getEntityName(entity));
		boolean ok = false;
		if (conditions.size() == 0){
			return true;
		}
		for (Map cond : conditions) {
			if (cond.size() == 0) {
				ok = true;
				continue;
			}
			QueryBuilder qb = new QueryBuilder("mvel", sdesc, entity, false, config, sessionContext, null, cond, null, fieldSets);
			String evalString = qb.getWhere();
			Map props = new HashMap();
			props.putAll(qb.getQueryParams());
			props.put(m_inflector.getEntityName(entity), insert);
			if( PropertyUtils.isReadable(insert,"_team_list")){
				Set nowList = (Set) PropertyUtils.getProperty(insert, "_team_list");
				Set preList = (Set) ((Map) preUpdate).get("_team_list");
				if (nowList != null || preList != null) {
					Map tc = _getTeamChangedFlags(preList, nowList);
					props.put("team_changed", tc);
					triggerInfo.put("teamsChanged", _getTeamChangedList(tc, preList, nowList));
				}
			}
			props.put(m_inflector.getEntityName(entity) + "_pre", preUpdate);
			debug("TestTrigger.evalString:" + evalString);
			if(isDebug()){
				debug("TestTrigger.properties:" + m_js.deepSerialize(props));
			}
			ok = MVEL.evalToBoolean(evalString, props);
			debug("\tTestTrigger.ok:" + ok);
			if (!ok) {
				return ok;
			}
		}
		return ok;
	}

	private boolean executeTriggerActions(Map triggerMap, SessionContext sessionContext, String entity, Object insert, Map triggerInfo) throws Exception {
		List<Map> actions = (List) triggerMap.get("actions");
		int operation = (Integer)triggerInfo.get("operation");
		debug("executeTriggerActions:" + actions);
		for (Map action : actions) {
			debug("action1:" + action);
			boolean fieldActive = getBoolean(action.get("fieldactive"));
			boolean serviceActive = getBoolean(action.get("serviceactive"));
			boolean processActive = getBoolean(action.get("processactive"));
			boolean camelActive = getBoolean(action.get("camelactive"));
			if (fieldActive && operation != DELETE) {
				fieldAction(sessionContext, action, insert, operation);
			}
			if (serviceActive) {
				serviceAction(sessionContext, action, entity, insert, operation);
			}
			if (processActive) {
				processAction(sessionContext, action, entity, insert, triggerInfo);
			}
			if (camelActive) {
				camelAction(sessionContext, action, entity, insert, triggerInfo);
			}
		}
		return false;
	}

	private void serviceAction(SessionContext sessionContext, Map action, String entity, Object insert, int operation) throws Exception {
		PersistenceManager pm = sessionContext.getPM();
		pm.flush();
		BundleContext bc = m_bundleContext;
		String serviceCall = (String) action.get("servicecall");
		int dot = serviceCall.lastIndexOf(".");
		if (dot == -1) {
			throw new RuntimeException("TriggerServiceImpl.serviceAction:wrong servicecall:" + serviceCall);
		}
		String method = serviceCall.substring(dot + 1);
		String clazzName = serviceCall.substring(0, dot);
		ServiceReference sr = bc.getServiceReference(clazzName);
		if (sr == null) {
			throw new RuntimeException("TriggerServiceImpl.serviceAction:service not found:" + clazzName);
		}
		Object o = bc.getService(sr);
		Class[] cargs = new Class[5];
		cargs[0] = String.class;
		cargs[1] = String.class;
		cargs[2] = Object.class;
		cargs[3] = Integer.class;
		cargs[4] = PersistenceManager.class;
		Method meth = o.getClass().getDeclaredMethod(method, cargs);
		debug("\tDeclaredMethot:" + meth);
		Map paramMap = new HashMap();
		paramMap.put("targetModule", entity);
		paramMap.put("operation", operation);
		paramMap.put("pm", sessionContext.getPM());
		paramMap.put("targetModuleObject", insert);
		Object[] args = new Object[5];
		Map sysMap = new HashMap();
		StoreDesc sdesc = sessionContext.getStoreDesc();
		sysMap.put(StoreDesc.NAMESPACE, sdesc.getNamespace());
		paramMap.put(StoreDesc.STORE, sdesc.getStore());
		paramMap.put(StoreDesc.PACK, sdesc.getPack());
		sysMap.put("username", sessionContext.getUserName());
		args[0] = sdesc.getNamespace();
		args[1] = entity;
		args[2] = insert;
		args[3] = new Integer(operation);
		args[4] = pm;
		Object ret = meth.invoke(o, args);
	}
	private void camelAction(SessionContext sessionContext, Map action, String entity, Object insert, Map triggerInfo) throws Exception {
		PersistenceManager pm = sessionContext.getPM();
		pm.flush();
		BundleContext bc = m_bundleContext;
		String camelCall = (String) action.get("camelcall");
		String startCamelUser = (String) action.get("startCamelUser");
		debug("camelAction:" + action + "/" + entity + "/" + insert );
		StoreDesc sdesc = sessionContext.getStoreDesc();
		String user = "admin";
		if (startCamelUser.equals("user")) {
			user = ThreadContext.getThreadContext().getUserName();
		}
		insert = sessionContext.getPM().detachCopy(insert);
		Map paramMap = new HashMap();
		triggerInfo.put("namespace", sdesc.getNamespace());
		triggerInfo.put("targetEntity", entity);
		triggerInfo.put("targetObject", SojoObjectFilter.getObjectGraph(insert, sessionContext));
		paramMap.put("triggerInfo", triggerInfo);
		if(isDebug()){
			m_js.prettyPrint(true);
			debug("paramMap:"+m_js.deepSerialize(paramMap));
		}
		new CamelThread(sdesc.getNamespace(), camelCall, user, paramMap).start();
	}

	private class CamelThread extends Thread {
		String endpoint;
		String ns;
		String user;
		Map paramMap;

		public CamelThread(String ns, String endpoint, String user, Map paramMap) {
			this.endpoint = endpoint;
			this.ns = ns;
			this.user = user;
			this.paramMap = paramMap;
		}

		public void run() {
			try {
				ThreadContext.loadThreadContext(ns, user);
				m_permissionService.loginInternal(ns);
				ProducerTemplate template = m_camelService.getCamelContext(ns,CamelService.DEFAULT_CONTEXT).createProducerTemplate();
				template.sendBody(endpoint, paramMap);
				debug("calling cameltemplate:" + endpoint + "/ns:" + ns + "/user:" + user);
			} catch (Exception e) {
				e.printStackTrace();
				ThreadContext.getThreadContext().finalize(e);
				m_logger.error("TriggerServiceImpl.CamelThread:", e);
			} finally {
				ThreadContext.getThreadContext().finalize(null);
			}
		}
	}

	private void processAction(SessionContext sessionContext, Map action, String entity, Object insert, Map triggerInfo) throws Exception {
		PersistenceManager pm = sessionContext.getPM();
		pm.flush();
		BundleContext bc = m_bundleContext;
		String processCall = (String) action.get("processcall");
		String startProcessUser = (String) action.get("startProcessUser");
		debug("processAction:" + action + "/" + entity + "/" + insert );
		StoreDesc sdesc = sessionContext.getStoreDesc();
		String user = "admin";
		if (startProcessUser.equals("user")) {
			user = ThreadContext.getThreadContext().getUserName();
		}
		insert = sessionContext.getPM().detachCopy(insert);
		Map paramMap = new HashMap();
		triggerInfo.put("namespace", sdesc.getNamespace());
		triggerInfo.put("targetEntity", entity);
		triggerInfo.put("targetObject", SojoObjectFilter.getObjectGraph(insert, sessionContext));
		paramMap.put("triggerInfo", triggerInfo);
		m_js.prettyPrint(true);
		System.out.println("paramMap:"+m_js.deepSerialize(paramMap));
		new ProcessThread(sdesc.getNamespace(), processCall, user, paramMap).start();
	}

	private class ProcessThread extends Thread {

		String processName;

		String ns;

		String user;

		Map paramMap;

		public ProcessThread(String ns, String processName, String user, Map paramMap) {
			this.processName = processName;
			this.ns = ns;
			this.user = user;
			this.paramMap = paramMap;
		}

		public void run() {
			try {
				ThreadContext.loadThreadContext(ns, user);
				m_permissionService.loginInternal(ns);
				debug("starting process:" + processName + "/ns:" + ns + "/user:" + user);
				Map result = m_activitiService.startProcessInstance(this.ns, -1, null, this.processName, null, null, null, paramMap);
				debug("processAction:" + result);
			} catch (Exception e) {
				ThreadContext.getThreadContext().finalize(e);
				m_logger.error("TriggerServiceImpl.ProcessThread:", e);
			} finally {
				ThreadContext.getThreadContext().finalize(null);
			}
		}
	}

	private void fieldAction(SessionContext sessionContext, Map action, Object insert, int operation) throws Exception {
		String type = (String) action.get("action");
		Map fields = (Map) action.get("fields");
		StoreDesc sdesc = sessionContext.getStoreDesc();
		String config = sessionContext.getConfigName();
		PersistenceManager pm = sessionContext.getPM();
		DataLayer dl = sessionContext.getDataLayer();
		Map hints = (Map) action.get("hints");
		if ("update-target".equals(type)) {
			debug("\tfields:" + fields);
			debug("\thints:" + hints);
			dl.populate(sessionContext, fields, insert, hints);
			pm.flush();
			debug("insertaction:" + m_js.deepSerialize(insert));
		} else if ("update-related".equals(type)) {
			String targetmodule = (String) action.get("targetmodule");
			String m[] = targetmodule.split("/");
			if (operation == INSERT) {
				pm.makePersistent(insert);
			}
			String entityNameParent = m[0];
			String entityName = m[1];
			try {
				Object updateObject = PropertyUtils.getProperty(insert, entityName);
				debug("updateObject:" + updateObject);
				if (updateObject == null) {
					String mid = (String) PropertyUtils.getProperty(insert, "id");
					insertObject(sessionContext, fields, hints, entityName, entityNameParent, mid);
				} else {
					dl.populate(sessionContext, fields, updateObject, hints);
					debug("updateObject:" + m_js.deepSerialize(updateObject));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if ("create-record".equals(type)) {
			String targetmodule = (String) action.get("targetmodule");
			String m[] = targetmodule.split("/");
			pm.makePersistent(insert);
			String mid = (String) PropertyUtils.getProperty(insert, "id");
			String entityName = m[1];
			String entityNameParent = m[0];
			debug("mid:" + mid);
			debug("moduleName:" + entityName);
			debug("moduleNameParent:" + entityNameParent);
			insertObject(sessionContext, fields, hints, entityName, entityNameParent, mid);
		}
	}

	private Map insertObject(SessionContext sessionContext, Map fields, Map hints, String entityName, String entityNameParent, String mid) throws Exception {
		DataLayer dl = sessionContext.getDataLayer();
		Object insertObject = dl.createObject(sessionContext, entityName);
		debug("insertObject:" + insertObject);
		dl.populate(sessionContext, fields, insertObject, hints);
		List constraintViolations = dl.validateObject(sessionContext, insertObject, entityName);
		debug("ConstraintViolation:" + constraintViolations);
		if (constraintViolations != null) {
			throw new RuntimeException("ConstraintViolation in Trigger");
		}
		Class masterClazz = dl.getClass(sessionContext, entityNameParent);
		String fieldName = entityName;
		entityName = dl.constructEntityName(sessionContext, entityName, entityNameParent);
		dl.insertIntoMaster(sessionContext, insertObject, entityName, masterClazz, fieldName, mid);
		dl.makePersistent(sessionContext, insertObject);
		Map ret = new HashMap();
		debug("insertObject:" + m_js.deepSerialize(insertObject));
		return ret;
	}

	private boolean getBoolean(Object value) {
		if (value == null) {
			return false;
		}
		boolean v = (Boolean) value;
		return v;
	}

	private static List getTriggers(DataLayer dl, StoreDesc sdesc, String entity, String appliesTo) throws Exception {
		StoreDesc csdesc = StoreDesc.getNamespaceMeta(sdesc.getNamespace());
		String pack = sdesc.getPack();
		String clazzName = pack + "." + m_inflector.getClassName(entity);
		SessionContext sc = dl.getSessionContext(csdesc);
		List<Map> ret = new ArrayList();
		try {
			String filter = "(active==true) && (targetmodule == '" + clazzName + "') && (applies_to.regexCI(\"" + appliesTo + "\"))";
			debug("GetTriggers.filter:" + filter);
			List tl = sc.getListByFilter(sc.getClass("trigger"), filter);
			for (Object o : tl) {
				Map trigger = new HashMap();
				List<Map> cList = new ArrayList();
				List<Map> aList = new ArrayList();
				trigger.put("conditions", cList);
				trigger.put("actions", aList);
				ret.add(trigger);
				Collection tcondList = (Collection) getProperty(o, "tcondition_list");
				Iterator cit = tcondList.iterator();
				while (cit.hasNext()) {
					Object c = cit.next();
					boolean active = (Boolean) getProperty(c, "active");
					if (active) {
						String value = (String) getProperty(c, "value");
						Map condition = null;
						if (value == null || "".equals(value)) {
							condition = new HashMap();
						} else {
							condition = (Map) m_ds.deserialize(value);
						}
						cList.add(condition);
					}
				}
				Collection tactionList = (Collection) getProperty(o, "taction_list");
				Iterator ait = tactionList.iterator();
				while (ait.hasNext()) {
					Object a = ait.next();
					boolean active = (Boolean) getProperty(a, "active");
					if (active) {
						String value = (String) getProperty(a, "value");
						Map actions = (Map) m_ds.deserialize(value);
						aList.add(actions);
					}
				}
			}
		} finally {
		}
		return ret;
	}

	//TEAM-Helper
	private Map _getTeamChangedFlags(Set<Map> preList, Set<Object> nowList) {
		Map flags = new HashMap();
		Map<String, Map> nowMap = _toTeamNowMap(nowList);
		if (preList != null) {
			for (Map<String, Object> pre : preList) {
				String teamid = (String) pre.get("teamid");
				Map now = nowMap.get(teamid);
				if (now != null) {
					flags.put(teamid, _teamsEqual(pre, now) ? null : "update");
					nowMap.put(teamid, null);
				} else {
					flags.put(teamid, "delete");
				}
			}
		}
		Set<String> idSet = nowMap.keySet();
		for (String teamId : idSet) {
			if (nowMap.get(teamId) != null) {
				flags.put(teamId, "add");
			}
		}
		return flags;
	}

	private boolean _teamsEqual(Map<String, Object> pre, Map<String, Object> now) {
		long validFromPre = getLong(pre.get("validFrom"));
		long validFromNow = getLong(now.get("validFrom"));
		long validToPre = getLong(pre.get("validTo"));
		long validToNow = getLong(now.get("validTo"));
		Boolean disabledPre = _getBoolean(pre.get("disabled"));
		Boolean disabledNow = _getBoolean(now.get("disabled"));
		boolean b = validFromPre == validFromNow && validToPre == validToNow && disabledPre == disabledNow;
		return b;
	}

	private List<Map> _getTeamChangedList(Map<String,String> tc, Set<Map> preList, Set<Object> nowList) {
		Map<String, Map> nowMap = _toTeamNowMap(nowList);
		List<Map> teams = new ArrayList();
		for (String key : tc.keySet()) {
			if(tc.get(key) == null){
				 continue;
			}
			String op = tc.get(key);
			Map team = null;
			if( nowMap.get(key)!=null){
				team = nowMap.get(key);	
			}else{
				for (Map pre : preList) {
					if( pre.get("teamid").equals(key) ){
						team = pre;	
						break;
					}
				}
			}
			boolean valid = m_teamService.checkTeamDate(team) && !getBoolean(team, "disabled", false);
			team.put("operation", op);
			team.put("valid", valid);
			teams.add(team);	
		}
		
		return teams;
	}

	private Map<String, Map> _toTeamNowMap(Set<Object> nowList) {
		Map<String, Map> retMap = new HashMap();
		if (nowList == null){
			return retMap;
		}
		for (Object t : nowList) {
			Map team = new HashMap(new BeanMap(t));
			team.remove("teamintern");
			retMap.put((String) team.get("teamid"), team);
		}
		return retMap;
	}

	private long getLong(Object l) {
		try {
			if (l == null)
				return -1;
			if (l instanceof Date) {
				return ((Date) l).getTime();
			}
			Number n = (Number) l;
			return n.longValue();
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
	}

	private boolean getBoolean(Map m, String key, boolean def) {
		return (Boolean) ((m.get(key) != null) ? m.get(key) : def);
	}
	private Boolean _getBoolean(Object b) {
		try {
			if (b == null){
				return null;
			}
			return (Boolean) b;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	protected static boolean isDebug() {
		return m_logger.isDebugEnabled();
	}
	protected static void debug(String message) {
		m_logger.debug(message);
		System.out.println(message);
	}
	protected static void info(String message) {
		m_logger.info(message);
		System.out.println(message);
	}
	private static final Logger m_logger = LoggerFactory.getLogger(TriggerServiceImpl.class);

	/************************************ C O N F I G ********************************************************/
	@Reference(dynamic = true, optional = true)
	public void setTeamService(TeamService paramTeamService) {
		this.m_teamService = paramTeamService;
		System.out.println("TriggerServiceImpl.setTeamService:" + paramTeamService);
	}
	@Reference(dynamic = true, optional = true)
	public void setSettingService(SettingService paramSettingService) {
		this.m_settingService = paramSettingService;
		System.out.println("TriggerServiceImpl.setSettingService:" + paramSettingService);
	}

	@Reference(dynamic = true)
	public void setNucleusService(NucleusService paramNucleusService) {
		this.m_nucleusService = paramNucleusService;
		System.out.println("TriggerServiceImpl.setNucleusService:" + paramNucleusService);
	}

	@Reference(dynamic = true, optional = true)
	public void setActivitiService(ActivitiService paramActivitiService) {
		this.m_activitiService = paramActivitiService;
		System.out.println("TriggerServiceImpl.setActivitiService:" + paramActivitiService);
	}

	@Reference(dynamic = true, optional = true)
	public void setCamelService(CamelService paramCamelService) {
		this.m_camelService = paramCamelService;
		System.out.println("TriggerServiceImpl.setCamelService:" + paramCamelService);
	}

	@Reference(dynamic = true, optional = true)
	public void setPermissionService(PermissionService paramPermissionService) {
		this.m_permissionService = paramPermissionService;
		System.out.println("TriggerServiceImpl.setPermissionService:" + paramPermissionService);
	}

}
