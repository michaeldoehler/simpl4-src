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

import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;
import java.io.PrintStream;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.*;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Element;
import javax.jdo.spi.PersistenceCapable;
import javax.script.ScriptEngineManager;
import javax.transaction.RollbackException;
import javax.transaction.UserTransaction;
import javax.transaction.Status;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import org.activiti.engine.delegate.DelegateExecution;
import org.apache.commons.beanutils.BeanMap;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.fileupload.FileItem;
import org.joda.time.DateTime;
import org.ms123.common.entity.api.EntityService;
import org.ms123.common.setting.api.SettingService;
import static org.ms123.common.setting.api.Constants.TITLEEXPRESSION;
import static org.ms123.common.setting.api.Constants.RECORDVALIDATION;
import static org.ms123.common.setting.api.Constants.NORESULTSETCOUNT;
import static org.ms123.common.setting.api.Constants.STATESELECT;
import static org.ms123.common.setting.api.Constants.GLOBAL_SETTINGS;
import static org.ms123.common.entity.api.Constants.STATE_OK;
import static org.ms123.common.entity.api.Constants.STATE_NEW;
import static org.ms123.common.entity.api.Constants.STATE_FIELD;
import static org.ms123.common.entity.api.Constants.DISABLE_STATESELECT;
import org.ms123.common.data.api.SessionContext;
import org.ms123.common.git.GitService;
import org.ms123.common.camel.api.CamelService;
import org.ms123.common.auth.api.AuthService;
import org.ms123.common.utils.annotations.RelatedTo;
import org.ms123.common.data.api.LuceneService;
import org.ms123.common.data.api.LuceneSession;
import org.ms123.common.data.query.QueryBuilder;
import org.ms123.common.data.scripting.MVELEvaluator;
import org.ms123.common.data.dupcheck.DublettenCheckService;
import org.ms123.common.nucleus.api.NucleusService;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.permission.api.PermissionService;
import org.ms123.common.team.api.TeamService;
import org.ms123.common.system.ThreadContext;
import org.ms123.common.permission.api.PermissionException;
import org.ms123.common.libhelper.Inflector;
import org.ms123.common.utils.IOUtils;
import org.ms123.common.utils.TypeUtils;
import org.ms123.common.utils.UtilsServiceImpl;
import org.mvel2.optimizers.OptimizerFactory;
import org.mvel2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.servlet.http.*;
import org.apache.tika.Tika;
import org.apache.tika.mime.MediaType;
import org.apache.tika.detect.Detector;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import java.io.*;
import org.ms123.common.libhelper.Base64;
import org.xml.sax.SAXException;
import aQute.bnd.annotation.metatype.*;
import aQute.bnd.annotation.component.*;

@SuppressWarnings("unchecked")
@Component(enabled = true, configurationPolicy = ConfigurationPolicy.optional, immediate = true, properties = { "kind=jdo,name=dataLayer" })
public class JdoLayerImpl implements org.ms123.common.data.api.DataLayer {

	protected SimpleDateFormat m_dateFormat = new SimpleDateFormat("dd.MM.yyyy");

	private static final Logger m_logger = LoggerFactory.getLogger(JdoLayerImpl.class);

	private EntityService m_entityService;

	private AuthService m_authService;

	private SettingService m_settingService;
	private GitService m_gitService;
	private CamelService m_camelService;

//	private LuceneService m_luceneService;

	private NucleusService m_nucleusService;

	private TeamService m_teamService;

	private PermissionService m_permissionService;

	private Map<String,DublettenCheckService> m_dublettenCheckService = new HashMap();

	private TriggerService m_triggerService;

	private JSONSerializer m_js = new JSONSerializer();

	private JSONDeserializer m_ds = new JSONDeserializer();

	private Validator m_validator;

	protected Inflector m_inflector = Inflector.getInstance();

	public void activate() {
		debug("JdoLayerImpl.activate");
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		m_validator = factory.getValidator();
		debug("++++++validator+++++++:" + m_validator);
		MVELEvaluator evalator = new MVELEvaluator(null);
		evalator.test();
		OptimizerFactory.setDefaultOptimizer("reflective");
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////// 
	//insert 
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////// 
	public void insertUpdate(DelegateExecution execution) {
		debug("JdoLayerImpl.insertUpdate:" + execution.getVariableNames());
		debug("JdoLayerImpl.insertUpdate:" + execution.getVariables());
	}

	public Map insertObject(Map dataMap, StoreDesc sdesc, String entityName) {
		return insertObject(dataMap, null, null, sdesc, entityName, null, null);
	}

	public Map insertObject(Map dataMap, StoreDesc sdesc, String entityName, String entityNameParent, String idParent) {
		return insertObject(dataMap, null, null, sdesc, entityName, entityNameParent, idParent);
	}

	public Map insertObject(Map dataMap, Map filterMap, Map hintsMap, StoreDesc sdesc, String entityName, String entityNameParent, String idParent) {
		debug("insertObject:" + dataMap + ",filterMap:" + filterMap + ",entity:" + entityName + "/entityNameParent:" + entityNameParent + "/idParent:" + idParent);
		Map retMap = new HashMap();
		// initialize 
		SessionContext sessionContext = getSessionContext(sdesc);
		UserTransaction ut = m_nucleusService.getUserTransaction();
		try {
			ut.begin();
			retMap = insertObject(sessionContext, dataMap, filterMap, hintsMap, entityName, entityNameParent, idParent);
			if (retMap.get("constraintViolations") == null) {
				ut.commit();
			}else{
				ut.rollback();
			}
		} catch (Throwable e) {
			sessionContext.handleException(ut, e);
		} finally {
			sessionContext.handleFinally(ut);
		}
		return retMap;
	}

	public Map insertObject(SessionContext sessionContext, Map dataMap, String entityName) throws Exception {
		return insertObject(sessionContext, dataMap, null, null, entityName, null, null);
	}

	public Map insertObject(SessionContext sessionContext, Map dataMap, String entityName, String entityNameParent, String idParent) throws Exception {
		return insertObject(sessionContext, dataMap, null, null, entityName, entityNameParent, idParent);
	}

	public Map insertObject(SessionContext sessionContext, Map dataMap, Map hintsMap, String entityName, String entityNameParent, String idParent) throws Exception {
		return insertObject(sessionContext, dataMap, null, hintsMap, entityName, entityNameParent, idParent);
	}

	public Map insertObject(SessionContext sessionContext, Map dataMap, Map filterMap, Map hintsMap, String entityName, String entityNameParent, String idParent) throws Exception {
		StoreDesc sdesc = sessionContext.getStoreDesc();
		String fieldName = entityName;
		if (entityNameParent != null) {
			entityName = constructEntityName(sessionContext, entityName, entityNameParent);
		}
		String user = sessionContext.getUserName();
		checkPermissions(sdesc, user, entityName, dataMap, "write");
		Map retMap = new HashMap();
		debug("fieldName:" + fieldName);
		debug("moduleName:" + entityName);
		String config = sessionContext.getConfigName();
		dataMap.put("_isnew", true);
		dataMap.put("_user", user);
		PersistenceManager pm = sessionContext.getPM();
		entityName = m_inflector.getEntityName(entityName);
		List ids = new ArrayList();
		// evaluate formulas 
		evaluteFormulas(sessionContext, entityName, dataMap, "in");
		Iterator rit = null;
		if (filterMap != null) {
			// query masterObjects 
			Map fieldSets = m_settingService.getFieldSets(config, sdesc.getNamespace(), m_inflector.getEntityName(entityNameParent));
			boolean hasTeamSecurity = hasTeamSecurity(sessionContext, entityName, null);
			QueryBuilder qb = new QueryBuilder("pg", sdesc, m_inflector.getEntityName(entityNameParent), hasTeamSecurity, config, sessionContext, null, filterMap, null, fieldSets);
			String whereClause = "where " + qb.getWhere();
			String from = qb.getFrom(null);
			whereClause = whereClause + getAddWhere(qb,entityName, null,null);
			String sql = "Select distinct id from " + from + " " + whereClause;
			debug("sql:" + sql);
			Query q = pm.newQuery("javax.jdo.query.JPQL", sql);
			q.declareImports(sdesc.getImports());
			List results = (List) q.executeWithMap(qb.getQueryParams());
			rit = results.iterator();
		} else {
			// no master 
			rit = new DummyIterator();
		}
		// get insertClass/masterClass 
		Class masterClazz = null;
		Class insertClazz = getClass(sdesc, m_inflector.getClassName(entityName));
		if (entityNameParent != null) {
			masterClazz = getClass(sdesc, m_inflector.getClassName(entityNameParent));
		}
		while (rit.hasNext()) {
			Object row = rit.next();
			//MasterId 
			Object masterId = null;
			if (masterClazz != null) {
				if (row == null) {
					Map permittedFields = sessionContext.getPermittedFields(entityNameParent);
					masterId = getIdObject(idParent, sdesc, permittedFields);
				} else {
					masterId = row;
					debug("masterId:" + masterId);
				}
			}
			// create insert object 
			Object objectInsert = insertClazz.newInstance();
			setDefaultValues(insertClazz, objectInsert);
			populate(sessionContext, dataMap, objectInsert, hintsMap);
			m_js.prettyPrint(true);
			debug("IO:"+m_js.deepSerialize(objectInsert));
			List constraintViolations = validateObject(sessionContext, objectInsert, entityName, true);
			if (constraintViolations != null) {
				retMap.put("constraintViolations", constraintViolations);
				break;
			}
			insertIntoMaster(sessionContext, objectInsert, entityName, masterClazz, fieldName, masterId);
			makePersistent(sessionContext, objectInsert);
			Boolean bypassTrigger = (Boolean)sessionContext.getProperty("bypassTrigger");
			if( bypassTrigger ==null || bypassTrigger==false){
				m_triggerService.applyInsertRules(sessionContext, entityName, objectInsert);
			}
			ids.add(PropertyUtils.getProperty(objectInsert, getPrimaryKey(objectInsert.getClass())));
		}
		if (retMap.get("constraintViolations") == null) {
			if (filterMap != null) {
				retMap.put("ids", ids);
			} else {
				if (ids.size() > 0) {
					retMap.put("id", ids.get(0));
				}
			}
		}
		return retMap;
	}
	
	public String constructEntityName(SessionContext sessionContext, String entityName, String entityNameParent) {
		debug("constructEntityName:" + entityName + ",parent:" + entityNameParent);
		try {
			Class t = TypeUtils.getTypeForField(newInstance(sessionContext.getStoreDesc(), entityNameParent), entityName);
			if( t == null) return entityName;
			String name = t.getName();
			int ind = name.lastIndexOf(".");
			entityName = m_inflector.getEntityName(name.substring(ind + 1));
		} catch (Exception e) {
			debug("insertObject.getEntityName:" + e);
		}
		debug(" >> " + entityName);
		return entityName;
	}

	public void makePersistent(SessionContext sessionContext, Object objectInsert) {
		PersistenceManager pm = sessionContext.getPM();
		pm.makePersistent(objectInsert);
		StoreDesc sdesc = sessionContext.getStoreDesc();
		String name = objectInsert.getClass().getSimpleName().toLowerCase();
		if (sdesc.isDataPack() && "teamintern".equals(name) == false) { //@@@MS Konfigurierbar machen
//			sessionContext.getLuceneSession().addToIndex(objectInsert);
		}
	}

	public List validateObject(SessionContext sessionContext, Object objectInsert) {
		if (objectInsert instanceof Map) {
			throw new RuntimeException("JdoLayerImpl.validateObject:objectInsert is a Map");
		}
		String entityName = m_inflector.getEntityName(getLastElement(objectInsert.getClass().getName(), "."));
		return validateObject(sessionContext, objectInsert, entityName, true);
	}
	public List validateObject(SessionContext sessionContext, Object objectInsert, String entityName) {
		return validateObject(sessionContext, objectInsert, entityName, true);
	}

	public List validateObject(SessionContext sessionContext, Object objectInsert, String entityName, boolean bInsert) {
		return validateObject(sessionContext,objectInsert, null, entityName, bInsert);
	}
	public List validateObject(SessionContext sessionContext, Object objectInsert, Object objectUpdatePre, String entityName, boolean bInsert) {
		Set cv = m_validator.validate(objectInsert);
		String recordValidation = getRecordValidation(sessionContext,entityName);
		if( recordValidation != null){
			Map<String,Object> properties = new HashMap();
			properties.put("constraintViolationList", constructConstraitViolationList(cv));
			properties.put("object", objectInsert);
			properties.put("teamChangedList", Utils.getTeamChangedList(m_teamService,objectInsert,objectUpdatePre));
			Object answer = m_camelService.camelSend(sessionContext.getStoreDesc().getNamespace(),recordValidation, null, null,properties);
			System.out.println("answer:"+answer);
			if ( answer != null && ((Collection)answer).size() > 0) {
				return new ArrayList((Collection)answer);
			}else{
				return null;
			}
		}else{
			if (cv.size() > 0) {
				return constructConstraitViolationList(cv);
			}
		}
		if( true) return null; //@@@ NO DublettenCheck online
		StoreDesc sdesc= sessionContext.getStoreDesc();
		String vendor = sdesc.getVendor();
		DublettenCheckService dcs = m_dublettenCheckService.get(vendor);
		if( dcs == null){
			dcs = m_dublettenCheckService.get("default");
		}
		debug("DublettenCheckService:"+dcs);
		Map ret = dcs.dublettenCheck(dcs.getContext(sessionContext,entityName), objectInsert);
		debug("ret:" + ret);
		String primKey = sessionContext.getPrimaryKey();
		List<Map> cvl = (List) ret.get("constraintViolations");
		List idHitList = (List) ret.get("idHitList");
		debug("idHitList:" + idHitList);
		if (idHitList != null && idHitList.size() == 1) {
			try {
				if (!(objectInsert instanceof Map)) {
					primKey = getPrimaryKey(objectInsert.getClass());
				}
				Object _hit = idHitList.get(0);
				if (_hit instanceof Long) {
					Long hit = (Long) idHitList.get(0);
					Long updateId = (Long) PropertyUtils.getProperty(objectInsert, primKey);
					if (updateId != null && hit == updateId) {
						debug("self bei update");
						return null;
					}
				}
				if (_hit instanceof String) {
					String hit = (String) idHitList.get(0);
					String updateId = (String) PropertyUtils.getProperty(objectInsert, primKey);
					if (!bInsert && updateId != null && updateId.equals(hit)) {
						debug("self bei update");
						return null;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if(idHitList != null && cvl!=null && cvl.size()>0){
			cvl.get(0).put("idHitList",idHitList);
		}
		return cvl;
	}

	public Object createObject(SessionContext sessionContext, String entityName) {
		try {
			Class clazz = getClass(sessionContext.getStoreDesc(), m_inflector.getClassName(entityName));
			Object object = clazz.newInstance();
			setDefaultValues(clazz, object);
			return object;
		} catch (Exception e) {
			throw new RuntimeException("JdoLayerImpl.createObject:", e);
		}
	}

	public void insertIntoMaster(SessionContext sc, Object objectInsert, String entityName, Class masterClazz, String fieldName, Object masterId) throws Exception {
		if (masterClazz == null){
			return;
		}
		PersistenceManager pm = sc.getPM();
		Object objectMaster = pm.getObjectById(masterClazz, masterId);
		insertIntoMaster(sc,objectInsert,entityName,objectMaster,fieldName);
	}

	public void insertIntoMaster(SessionContext sc, Object objectInsert, String entityName, Object objectMaster, String fieldName) throws Exception {
		debug("insertIntoMaster:"+objectMaster+"/"+entityName+"/"+fieldName);
		if (objectMaster == null)
			return;
		String propertyName = fieldName;
		if (fieldName.equals(entityName)) {
			propertyName = m_inflector.pluralize(entityName).toLowerCase();
		}
		Class clazz = PropertyUtils.getPropertyType(objectMaster, propertyName);
		if( clazz == null){
			clazz = PropertyUtils.getPropertyType(objectMaster, fieldName);
		}
		debug("\tinsertIntoMaster.class:"+clazz);
		if (clazz != null) {
			if (clazz.equals(java.util.List.class) || clazz.equals(java.util.Set.class)) {
				Collection l = (Collection) PropertyUtils.getProperty(objectMaster, propertyName);
				l.add(objectInsert);
				String relatedToFieldName = getRelatedTo(objectInsert, objectMaster.getClass(), propertyName);
				if (relatedToFieldName != null) {
					PropertyUtils.setProperty(objectInsert, relatedToFieldName, objectMaster);
				}
			} else {
				if (fieldName.equals(entityName)) {
					propertyName = m_inflector.singularize(entityName).toLowerCase();
				}
				PropertyUtils.setProperty(objectMaster, propertyName, objectInsert);
			}
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////// 
	//update 
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////// 
	public Map updateObject(Map dataMap, StoreDesc sdesc, String entityName, String id) {
		return updateObject(dataMap, null, null, sdesc, entityName, id, null, null);
	}

	public Map updateObject(Map dataMap, Map filterMap, Map hintsMap, StoreDesc sdesc, String entityName, String id, String entityNameParent, String idParent) {
		m_logger.info("updateObject:" + dataMap + ",filterMap:" + filterMap + ",module:" + entityName);
		debug("updateObject:" + dataMap + ",filterMap:" + filterMap + ",module:" + entityName + ",id:" + id + ",moduleNameParent:" + entityNameParent + ",idParent:" + idParent);
		Map retMap = new HashMap();
		SessionContext sessionContext = getSessionContext(sdesc);
		UserTransaction ut = m_nucleusService.getUserTransaction();
		try {
			ut.begin();
			retMap = updateObject(sessionContext, dataMap, filterMap, hintsMap, entityName, id, entityNameParent, idParent);
			if (retMap.get("constraintViolations") == null) {
				ut.commit();
			}else{
				ut.rollback();
			}
		} catch (Exception e) {
			sessionContext.handleException(ut, e);
		} finally {
			sessionContext.handleFinally(ut);
		}
		return retMap;
	}

	public Map updateObject(SessionContext sessionContext, Map dataMap, String entityName, String id) throws Exception {
		return updateObject(sessionContext, dataMap, null, null, entityName, id, null, null);
	}

	public Map updateObject(SessionContext sessionContext, Map dataMap, Map hintsMap, String entityName, String id) throws Exception {
		return updateObject(sessionContext, dataMap, null, hintsMap, entityName, id, null, null);
	}

	public Map updateObject(SessionContext sessionContext, Map dataMap, Map filterMap, Map hintsMap, String entityName, String id, String entityNameParent, String idParent) throws Exception {
		StoreDesc sdesc = sessionContext.getStoreDesc();
		String fieldName = entityName;
		if (entityNameParent != null) {
			entityName = constructEntityName(sessionContext, entityName, entityNameParent);
		}
		String user = sessionContext.getUserName();
		checkPermissions(sdesc, user, entityName, dataMap, "write");

		String config = sessionContext.getConfigName();
		Map retMap = new HashMap();
		String classNameUpdate = null;
		dataMap.put("_isnew", false);
		dataMap.put("_user", user);
		PersistenceManager pm = sessionContext.getPM();
		Iterator rit = null;
		Map permittedFields = null;
		if (filterMap != null) {
			// Query updateObject`s 
			classNameUpdate = m_inflector.getClassName(entityName);
			Class updateClazz = getClass(sdesc, classNameUpdate);
			Map fieldSets = m_settingService.getFieldSets(config, sdesc.getNamespace(), entityName);
			boolean hasTeamSecurity = hasTeamSecurity(sessionContext, entityName, null);
			QueryBuilder qb = new QueryBuilder("pg", sdesc, entityName, hasTeamSecurity, config, sessionContext, null, filterMap, hintsMap, fieldSets);
			String whereClause = "where " + qb.getWhere();
			String from = qb.getFrom(null);

			whereClause = whereClause + getAddWhere(qb,entityName, null,null);
			String sql = "Select distinct id from " + from + " " + whereClause;
			debug("sql:" + sql);
			Query q = pm.newQuery("javax.jdo.query.JPQL", sql);
			q.declareImports(sdesc.getImports());
			List results = (List) q.executeWithMap(qb.getQueryParams());
			rit = results.iterator();
		} else {
			Object updateId = null;
			// Get updateObject from parent over one-to-one Relation,Ex:data/contacts/30329/communication 
			if (idParent == null && entityNameParent != null) {
				String propertyName = fieldName;
				if (fieldName.equals(entityName)) {
					propertyName = m_inflector.singularize(entityName).toLowerCase();
				}
				Object object = null;
				String classNameMaster = m_inflector.getClassName(entityNameParent);
				Class masterClazz = getClass(sdesc, classNameMaster);
				Object masterId = getIdObject(id, sdesc, null);
				Object objectMaster = pm.getObjectById(masterClazz, masterId);
				try {
					object = PropertyUtils.getProperty(objectMaster, propertyName);
				} catch (Exception e) {
					debug("e:" + e);
				}
				if (object != null) {
					updateId = PropertyUtils.getProperty(object, "id");
				} else {
					String className = m_inflector.getClassName(entityName);
					Class updateClazz = getClass(sdesc, className);
					Object objectUpdate = updateClazz.newInstance();
					pm.makePersistent(objectUpdate);
					PropertyUtils.setProperty(objectMaster, propertyName, objectUpdate);
					updateId = (Long) PropertyUtils.getProperty(objectUpdate, "id");
				}
				classNameUpdate = m_inflector.getClassName(entityName);
			} else {
				permittedFields = sessionContext.getPermittedFields(entityName);
				updateId = getIdObject(id, sdesc, permittedFields);
				classNameUpdate = m_inflector.getClassName(entityName);
			}
			entityName = m_inflector.getEntityName(entityName);
			//evaluteFormulas(sessionContext, entityName, dataMap, "in");
			debug("moduleName:" + entityName);
			rit = new DummyIterator(updateId);
		}
		int counter=0;
		long start = new Date().getTime();
		Class updateClazz = getClass(sdesc, classNameUpdate);
		while (rit.hasNext()) {
			Object row = rit.next();
			counter++;
			Object updateId = row;
			Object objectUpdate = null;
			boolean fCreated = false;
			try {
				if (updateId == null){
					updateId = "dummy";
				}
				objectUpdate = pm.getObjectById(updateClazz, updateId);
			} catch (javax.jdo.JDOObjectNotFoundException e) {
				String pk = null;
				if (permittedFields != null) {
					pk = getPrimaryKey(updateClazz);
					debug("pk:" + pk);
				}
				debug("pk:" + pk);
				Boolean create=true;
				if( hintsMap != null && hintsMap.get("create")!=null){
					create = (Boolean)hintsMap.get("create");
				}
				fCreated = create;
				if( create){
					objectUpdate = createObject(sessionContext, entityName);
					PropertyUtils.setProperty(objectUpdate, pk, updateId);
					dataMap.put("_isnew", true);
				}
			}
			debug("objectUpdate:" + objectUpdate + "," + updateId+"/"+counter);
			if( objectUpdate != null){
				Object objectUpdatePre = null;
				if( filterMap == null){
					objectUpdatePre = UtilsServiceImpl.copyObject(objectUpdate);
				}
				Map<String,String> stateMap = getState(dataMap,objectUpdate);;
				evaluteFormulas(sessionContext, entityName, dataMap, "in");
				populate(sessionContext, dataMap, objectUpdate, hintsMap);
				setState(stateMap,objectUpdate);
				List constraintViolations = validateObject(sessionContext, objectUpdate, objectUpdatePre, entityName, fCreated);
				if (constraintViolations != null) {
					retMap.put("constraintViolations", constraintViolations);
					retMap.put("created", null);
					break;
				}
				if (fCreated) {
					pm.makePersistent(objectUpdate);
					retMap.put("id", updateId);
					retMap.put("created", true);
				}
				debug("SEDSC:" + sdesc);
				if (sdesc.isDataPack()) {
					//sessionContext.getLuceneSession().addToIndex(objectUpdate);
				}
				if( filterMap == null){
					Boolean bypassTrigger = (Boolean)sessionContext.getProperty("bypassTrigger");
					if( bypassTrigger == null || bypassTrigger == false){
						m_triggerService.applyUpdateRules(sessionContext, entityName, objectUpdate,objectUpdatePre);
					}
				}
				if((counter%100)==0){
					displayInfo("",start);
					start = new Date().getTime();
				}
			}else{
				retMap.put("id", null);
				retMap.put("created", false);
				retMap.put("notFound", true);
			}
		}
		return retMap;
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////// 
	//delete 
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////// 
	public Map deleteObject(Map dataMap, StoreDesc sdesc, String entityName, String id) {
		info("deleteObject:" + dataMap + ",module:" + entityName);
		debug("deleteObject:" + dataMap + ",module:" + entityName + ",id:" + id);
		Map retMap = new HashMap();
		SessionContext sessionContext = getSessionContext(sdesc);
		UserTransaction ut = m_nucleusService.getUserTransaction();
		try {
			ut.begin();
			retMap = deleteObject(sessionContext, dataMap, entityName, id);
			ut.commit();
		} catch (Exception e) {
			sessionContext.handleException(ut, e);
		} finally {
			sessionContext.handleFinally(ut);
		}
		return retMap;
	}

	public Map deleteObject(SessionContext sessionContext, Map dataMap, String entityName, String id) throws Exception {
		StoreDesc sdesc = sessionContext.getStoreDesc();
		String user = sessionContext.getUserName();
		checkPermissions(sdesc, user, entityName, dataMap, "write");
		String config = sessionContext.getConfigName();
		PersistenceManager pm = sessionContext.getPM();
		info("deleteObject:" + entityName + ",id:" + id);
		Map retMap = new HashMap();
		String classNameDelete = m_inflector.getClassName(entityName);
		Object objId = null;
		if (sdesc.isDataPack()) {
			Map permittedFields = sessionContext.getPermittedFields(m_inflector.getEntityName(entityName));
			objId = getIdObject(id, sdesc, permittedFields);
		} else {
			objId = getIdObject(id, sdesc, null);
		}
		Class deleteClazz = getClass(sdesc, classNameDelete);
		Object objectDelete = pm.getObjectById(deleteClazz, objId);
		Boolean bypassTrigger = (Boolean)sessionContext.getProperty("bypassTrigger");
		if( bypassTrigger == null || bypassTrigger == false){
			m_triggerService.applyDeleteRules(sessionContext, entityName, objectDelete);
		}
		//@@@MS should be reverse 
		pm.deletePersistent(objectDelete);
		if (sdesc.isDataPack() && !"teamintern".equals(entityName)) {
			//sessionContext.getLuceneSession().deleteFromIndex(objectDelete);
		}
		return retMap;
	}

	private void checkPermissions(StoreDesc sdesc, String user, String entityName, Map<String,Object> dataMap, String action) {
		entityName = m_inflector.getEntityName(entityName);
		debug("checkPermissions:"+entityName+"/"+action+"/"+dataMap);
		if( entityName.equals("team") && dataMap!=null && "write".equals(action)){
			String teamid = (String)dataMap.get("teamid");
			if( teamid == null){
				throw new PermissionException("JdoLayerImpl.checkTeamPermission.noTeamid");
			}
			int status = m_teamService.checkTeamUserPermission(sdesc.getNamespace(),teamid,user);
			debug("TeamStatus:"+status);
			if( status == -1){
				throw new PermissionException("JdoLayerImpl.noTeamPermission");
			}
		}
		boolean b = m_permissionService.hasEntityPermissions(sdesc, entityName, action);
		if( b ) return;
		throw new PermissionException("JdoLayerImpl.checkPermissions(" + sdesc+"/entityName:"+entityName+"/action:"+action + ") not allowed");
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////// 
	//populate 
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////// 
	public void populate(SessionContext sessionContext, Map from, Object to, Map hintsMap) {
		PersistenceManager pm = sessionContext.getPM();
		if (hintsMap == null) {
			hintsMap = new HashMap();
		}
		Map<String,String> expressions = (Map)hintsMap.get("__expressions");
		if( expressions==null) expressions=new HashMap();
		BeanMap beanMap = new BeanMap(to);
		String entityName = m_inflector.getEntityName(to.getClass().getSimpleName());
		debug("populate.from:" + from + ",to:" + to + ",BeanMap:" + beanMap + "/hintsMap:" + hintsMap+"/entityName:"+entityName);
		if (from == null){
			return;
		}
		Map permittedFields = sessionContext.getPermittedFields(entityName,"write");
		Iterator<String> it = from.keySet().iterator();
		while (it.hasNext()) {
			String key = it.next();
			Object oldValue = beanMap.get(key);
			boolean permitted = m_permissionService.hasAdminRole() || "team".equals(entityName) || sessionContext.isFieldPermitted( key, entityName, "write");
			if( !key.startsWith("_") && !permitted){
				debug("---->populate:field("+key+") no write permission");
				continue;
			}else{
				debug("++++>populate:field("+key+") write permitted");
			}
			String datatype = null;
			String edittype = null;
			if( !key.startsWith("_") ){
				Map config = (Map)permittedFields.get(key);
				if( config != null){
					datatype = (String)config.get("datatype");
					edittype = (String)config.get("edittype");
				}
			}

			if(key.equals(STATE_FIELD) && !m_permissionService.hasAdminRole()){
				continue;	
			}
			if("auto".equals(edittype))continue;
			String mode = null;
			Map hm = (Map) hintsMap.get(key);
			if (hm != null) {
				Object m = hm.get("mode");
				if (m != null && m instanceof String) {
					mode = (String) m;
				}
				if (mode == null) {
					m = hm.get("useit");
					if (m != null && m instanceof String) {
						mode = (String) m;
					}
				}
			}
			if (mode == null) {
				mode = "replace";
			}
			Class clazz = beanMap.getType(key);
			debug("\ttype:" + clazz + "(" + key + "=" + from.get(key)+")");
			if ("_ignore_".equals(from.get(key))) {
				continue;
			}
			if (clazz == null) {
				debug("\t--- Warning property not found:" + key);
			} else if (clazz.equals(java.util.Date.class)) {
				String value = Utils.getString(from.get(key), beanMap.get(key), mode);
				debug("\tDate found:" + key + "=>" + value);
				Date date = null;
				if( value != null){
					try {
						Long val = Long.valueOf(value);
						date = (Date) ConvertUtils.convert(val, Date.class);
						debug("\tdate1:" + date);
					} catch (Exception e) {
						try {
							DateTime dt = new DateTime(value);
							date = new Date(dt.getMillis());
							debug("\tdate2:" + date);
						} catch (Exception e1) {
							try {
								int space = value.indexOf(" ");
								if (space != -1) {
									value = value.substring(0, space) + "T" + value.substring(space + 1);
									DateTime dt = new DateTime(value);
									date = new Date(dt.getMillis());
								}
								debug("\tdate3:" + date);
							} catch (Exception e2) {
								debug("\terror setting date:" + e);
							}
						}
					}
				}
				debug("\tsetting date:" + date);
				beanMap.put(key, date);
			} else if (clazz.equals(java.util.Map.class) ) {
				info("!!!!!!!!!!!!!!!!!!!Map not implemented");
			} else if (clazz.equals(java.util.List.class) || clazz.equals(java.util.Set.class)) {
				boolean isList = clazz.equals(java.util.List.class);
				boolean isSimple = false;
				if( datatype != null && datatype.startsWith("list_")){
					isSimple = true;
				}
				try {
					Class type = TypeUtils.getTypeForField(to, key);
					debug("type:" + type + " fill with: " + from.get(key) + ",list:" + beanMap.get(key) + "/mode:" + mode);
					Collection valList = isList ? new ArrayList() : new HashSet();
					if (from.get(key) instanceof Collection) {
						valList = (Collection) from.get(key);
					}
					if (valList == null) {
						valList = isList ? new ArrayList() : new HashSet();
					}
					Collection toList = (Collection) PropertyUtils.getProperty(to, key);
					debug("toList:"+toList);
					debug("valList:"+valList);
					if (toList == null) {
						toList = isList ? new ArrayList() : new HashSet();
						PropertyUtils.setProperty(to, key, toList);
					}
					if ("replace".equals(mode)) {
						boolean isEqual = false;
						if( isSimple ){
							isEqual = Utils.isCollectionEqual(toList, valList);
							if( !isEqual ){
								toList.clear();
							}
							debug("\tisEqual:"+isEqual);
						}else{
							List deleteList= new ArrayList();
							String namespace = sessionContext.getStoreDesc().getNamespace();
							for (Object o : toList) {
								if( type.getName().endsWith(".Team")){
									int status = m_teamService.getTeamStatus(namespace,new BeanMap(o),null,sessionContext.getUserName());
									debug("populate.replace.teamStatus:"+status+"/"+new HashMap(new BeanMap(o)));
									if( status != -1){
										pm.deletePersistent(o);
										deleteList.add(o);
									}
								}else{
									pm.deletePersistent(o);
									deleteList.add(o);
								}
							}
							for(Object o: deleteList){
								toList.remove(o);
							}
						}
						debug("populate.replace.toList:"+toList+"/"+type.getName());
						if( isSimple ){
							if( !isEqual){
								for (Object o : valList) {
									toList.add(o);
								}
							}
						}else{
							for (Object o : valList) {
								Map valMap = (Map) o;
								Object n = type.newInstance();
								if( type.getName().endsWith(".Team")){
									valMap.remove("id");
									Object desc = valMap.get("description");
									Object name = valMap.get("name");
									Object dis = valMap.get("disabled");
									String teamid = (String)valMap.get("teamid");
									Object ti = Utils.getTeamintern(sessionContext,teamid);
									if( desc == null){
										valMap.put("description",PropertyUtils.getProperty(ti, "description"));
									}
									if( name == null){
										valMap.put("name",PropertyUtils.getProperty(ti, "name"));
									}
									if( dis == null){
										valMap.put("disabled",false);
									}
									pm.makePersistent(n);
									populate(sessionContext, valMap, n, null);
									PropertyUtils.setProperty(n, "teamintern", ti);
								}else{
									pm.makePersistent(n);
									populate(sessionContext, valMap, n, null);
								}
								debug("populated.add:"+new HashMap(new BeanMap(n)));
								toList.add(n);
							}
						}
					} else if ("remove".equals(mode)) {
						if( isSimple ){
							for (Object o : valList) {
								if( toList.contains(o)){
									toList.remove(o);
								}
							}
						}else{
							for (Object ol : valList) {
								Map map = (Map) ol;
								Object o = Utils.listContainsId(toList, map, "teamid");
								if (o != null) {
									toList.remove(o);
									pm.deletePersistent(o);
								}
							}
						}
					} else if ("add".equals(mode)) {
						if( isSimple ){
							for (Object o : valList) {
								toList.add(o);
							}
						}else{
							for (Object ol : valList) {
								Map map = (Map) ol;
								Object o = Utils.listContainsId(toList, map, "teamid");
								if (o != null) {
									populate(sessionContext, map, o, null);
								} else {
									o = type.newInstance();
									if( type.getName().endsWith(".Team")){
										Object desc = map.get("description");
										Object name = map.get("name");
										Object dis = map.get("disabled");
										String teamid = (String)map.get("teamid");
										Object ti = Utils.getTeamintern(sessionContext,teamid);
										if( desc == null){
											map.put("description",PropertyUtils.getProperty(ti, "description"));
										}
										if( name == null){
											map.put("name",PropertyUtils.getProperty(ti, "name"));
										}
										if( dis == null){
											map.put("disabled",false);
										}

										pm.makePersistent(o);
										populate(sessionContext, map, o, null);
										PropertyUtils.setProperty(o, "teamintern", ti);
									}else{
										pm.makePersistent(o);
										populate(sessionContext, map, o, null);
									}
									toList.add(o);
								}
							}
						}
					} else if ("assign".equals(mode)) {
						if( !isSimple ){
							for (Object ol : valList) {
								Map map = (Map) ol;
								Object o = Utils.listContainsId(toList, map);
								if (o != null) {
									debug("id:" + map + " already assigned");
								} else {
									Object id = map.get("id");
									Boolean assign = Utils.getBoolean(map.get("assign"));
									Object obj = pm.getObjectById(type, id);
									if (assign) {
										toList.add(obj);
									} else {
										toList.remove(obj);
									}
								}
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					debug("populate.list.failed:" + key + "=>" + from.get(key) + ";" + e);
				}
			} else if (clazz.equals(java.lang.Boolean.class)) {
				try {
					beanMap.put(key, ConvertUtils.convert(from.get(key), Boolean.class));
				} catch (Exception e) {
					debug("populate.boolean.failed:" + key + "=>" + from.get(key) + ";" + e);
				}
			} else if (clazz.equals(java.lang.Double.class)) {
				String value = Utils.getString(from.get(key), beanMap.get(key), mode);
				try {
					beanMap.put(key, Double.valueOf(value));
				} catch (Exception e) {
					debug("populate.double.failed:" + key + "=>" + value + ";" + e);
				}
			} else if (clazz.equals(java.lang.Long.class)) {
				try {
					beanMap.put(key, ConvertUtils.convert(from.get(key), Long.class));
				} catch (Exception e) {
					debug("populate.long.failed:" + key + "=>" + from.get(key) + ";" + e);
				}
			} else if (clazz.equals(java.lang.Integer.class)) {
				debug("Integer:" + ConvertUtils.convert(from.get(key), Integer.class));
				try {
					beanMap.put(key, ConvertUtils.convert(from.get(key), Integer.class));
				} catch (Exception e) {
					debug("populate.integer.failed:" + key + "=>" + from.get(key) + ";" + e);
				}
			} else if ("binary".equals(datatype) || clazz.equals(byte[].class)) {
				InputStream is = null;
				InputStream is2 = null;
				
				try {
					if (from.get(key) instanceof FileItem) {
						FileItem fi = (FileItem) from.get(key);
						String name = fi.getName();
						byte[] bytes = IOUtils.toByteArray(fi.getInputStream());
						if(bytes != null){
							debug("bytes:"+bytes.length);
						}
						beanMap.put(key, bytes);
						is = fi.getInputStream();
						is2 = fi.getInputStream();
					} else if (from.get(key) instanceof Map) {
						Map map = (Map) from.get(key);
						String storeLocation = (String) map.get("storeLocation");
						is = new FileInputStream(new File(storeLocation));
						is2 = new FileInputStream(new File(storeLocation));
						byte[] bytes = IOUtils.toByteArray(is);
						if(bytes != null){
							debug("bytes2:"+bytes.length);
						}
						is.close();
						beanMap.put(key, bytes);
						is = new FileInputStream(new File(storeLocation));
					} else if (from.get(key) instanceof String) {
						String value = (String)from.get(key);
						if( value.startsWith("data:")){
							int ind = value.indexOf(";base64,");
							byte b[] = Base64.decode(value.substring(ind+8));
							beanMap.put(key, b);
							is = new ByteArrayInputStream(b);
							is2 = new ByteArrayInputStream(b);
						}else{
						}
					} else {
						debug("populate.byte[].no a FileItem:" + key + "=>" + from.get(key));
						continue;
					}
					Tika tika = new Tika();
					TikaInputStream stream = TikaInputStream.get(is);
					TikaInputStream stream2 = TikaInputStream.get(is2);
					String text = tika.parseToString(is);
					debug("Text:" + text);
					try {
						beanMap.put("text", text);
					} catch (Exception e) {
						beanMap.put("text", text.getBytes());
					}
					//@@@MS Hardcoded 
					try {
						Detector detector = new DefaultDetector();
						MediaType mime = detector.detect(stream2, new Metadata());
						debug("Mime:" + mime.getType() + "|" + mime.getSubtype() + "|" + mime.toString());
						beanMap.put("type", mime.toString());
						from.put("type", mime.toString());
					} catch (Exception e) {
						e.printStackTrace();
					}
				} catch (Exception e) {
					e.printStackTrace();
					debug("populate.byte[].failed:" + key + "=>" + from.get(key) + ";" + e);
				}finally{
					try{
						is.close();
						is2.close();
					}catch(Exception e){
					}
				}
			} else {
				boolean ok = false;
				try {
					Class type = TypeUtils.getTypeForField(to, key);
					if (type != null) {
						Object o = type.newInstance();
						if (o instanceof javax.jdo.spi.PersistenceCapable) {
							Object id = null;
							try {
								Object _id = from.get(key);
								if (_id != null) {
									if( _id instanceof Map){
										id = ((Map)_id).get("id");
									}else{
										String s = String.valueOf(_id);
										if (s.indexOf("/") >= 0) {
											_id = Utils.extractId(s);
										}
										Class idClass = PropertyUtils.getPropertyType(o, "id");
										id = (idClass.equals(Long.class)) ? Long.valueOf(_id + "") : _id;
									}
								}
							} catch (Exception e) {
				
							}
							if (id != null && !"".equals(id) && !"null".equals(id)) {
								debug("\tId2:"+id);
								Object relatedObject = pm.getObjectById(type, id);
								List<Collection> candidates = TypeUtils.getCandidateLists(relatedObject, to, null);
								if (candidates.size() == 1) {
									Collection l = candidates.get(0);
									debug("list.contains:" + l.contains(to));
									if (!l.contains(to)) {
										l.add(to);
									}
								}
								beanMap.put(key, relatedObject);
							} else {
								Object relatedObject = beanMap.get(key);
								debug("\trelatedObject:"+relatedObject);
								if (relatedObject != null) {
									List<Collection> candidates = TypeUtils.getCandidateLists(relatedObject, to, null);
									if (candidates.size() == 1) {
										Collection l = candidates.get(0);
										debug("list.contains:" + l.contains(to));
										if (l.contains(to)) {
											l.remove(to);
										}
									}
								}
								beanMap.put(key, null);
							}
							ok = true;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (!ok) {
					String value = Utils.getString(from.get(key), beanMap.get(key), mode);
					// debug("populate:" + key + "=>" + value); 
					// debug("String:" + ConvertUtils.convert(from.get(key), String.class)); 
					try {
						beanMap.put(key, value);
					} catch (Exception e) {
						debug("populate.failed:" + key + "=>" + value + ";" + e);
					}
				}
			}
			String expression = expressions.get(key);
			if( !isEmpty(expression)){
				beanMap.put(key,oldValue);
				Map scriptCache = (Map)sessionContext.getProperty("scriptCache");
				if( scriptCache == null){
					scriptCache=new HashMap();
					sessionContext.setProperty("scriptCache",scriptCache);
				}
				Object result = Utils.eval( expression, beanMap, scriptCache);
				try{
					if("string".equals(datatype) && !(result instanceof String)){
						beanMap.put(key,ConvertUtils.convert(result,String.class));
					}else{
						beanMap.put(key,result);
					}
				}catch(Exception e){
					info("Cannot set value for("+key+"):"+result+"/"+e.getMessage());
				}
			}
		}
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////// 
	//get 
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////// 
	public Map getObject(StoreDesc sdesc, String entityName, String id) {
		return getObject(sdesc, entityName, id, null, null, null);
	}

	public Map getObject(StoreDesc sdesc, String entityName, String id, List fields) {
		return getObject(sdesc, entityName, id, null, fields, null);
	}

	public Map getObject(StoreDesc sdesc, String entityName, String id, String entityNameDetails, List fields) {
		return getObject(sdesc, entityName, id, entityNameDetails, fields, null);
	}

	public Map getObject(StoreDesc sdesc, String entityName, String id, String entityNameDetails, List fields, HttpServletResponse resp) {
		SessionContext sessionContext = getSessionContext(sdesc);
		checkPermissions(sdesc, sessionContext.getUserName(),entityName, null, "read");
		debug("getObject:fields:" + fields + ",entityName:" + entityName + ",entityNameDetails:" + entityNameDetails + ",id:" + id+"/resp:"+resp);
		Map retMap = new HashMap();
		PersistenceManager pm = sessionContext.getPM();
		boolean hasTeamSecurity = false;
		try {
			String className = m_inflector.getClassName(entityName);
			Map permittedFields = sessionContext.getPermittedFields(entityName);
			hasTeamSecurity = hasTeamSecurity(sessionContext,entityName,null);
			Object objId = getIdObject(id, sdesc, permittedFields);
			Class clazz = getClass(sdesc, className);
			Object objectMaster = pm.getObjectById(clazz, objId);
			debug("getObject.master1:" + objectMaster);
			String oldEntityName = entityName;
			if (entityNameDetails == null) {
				entityName = m_inflector.getEntityName(entityName);
			} else {
				//entityName = m_inflector.getEntityName(entityNameDetails);
				entityName = getEntityNameFromProperty(sdesc, entityName,entityNameDetails);
				String propertyName = m_inflector.singularize(entityNameDetails).toLowerCase();
				objectMaster = PropertyUtils.getProperty(objectMaster, propertyName);
				if (objectMaster == null) {
					return null;
				}
				debug("getObject.master2:" + objectMaster);
			}
			if (!oldEntityName.equals(entityName)) {
				hasTeamSecurity = hasTeamSecurity(sessionContext,entityName,null);
				permittedFields = sessionContext.getPermittedFields(entityName);
			}
			Map m = null;
			if (resp != null) {
				getBinary(sessionContext, objectMaster, hasTeamSecurity, permittedFields, resp);
			} else {
				m = getPropertyMap(sessionContext, objectMaster, hasTeamSecurity, permittedFields, fields);
			}
			if (m == null) {
				return null;
			}
			evaluteFormulas(sessionContext, entityName, m, "out", getInvolvedClasses(sdesc, entityName), pm);
			retMap.putAll(m);
		} catch (Exception e) {
			m_logger.error("getObject:", e);
			sessionContext.handleException(e);
			throw new RuntimeException(e);
		} finally {
			sessionContext.handleFinally();
		}
		return retMap;
	}


	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////// 
	//query 
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////// 
	public Map query(Map params, StoreDesc sdesc, String entityName) {
		return query(params, sdesc, entityName, null, null);
	}

	public Map query(Map params, StoreDesc sdesc, String entityName, String idParent, String entityNameDetails) {
		SessionContext sessionContext = getSessionContext(sdesc);
		try {
			return query(sessionContext, params, sdesc, entityName, idParent, entityNameDetails);
		} catch (Exception e) {
			sessionContext.handleException(e);
		} finally {
			sessionContext.handleFinally();
		}
		return null;
	}

	public Map query(SessionContext sessionContext, Map params, StoreDesc sdesc, String entityName) {
		return query(sessionContext, params, sdesc, entityName, null, null);
	}

	public Map query(SessionContext sessionContext, Map params, StoreDesc sdesc, String entityName, String idParent, String entityNameDetails) {
		debug("query:" + params + ",entityName:" + entityName + ",entityNameDetails:" + entityNameDetails + ",idParent:" + idParent ); 
		String config = sessionContext.getConfigName();
		PersistenceManager pm = sessionContext.getPM();
		long start = new Date().getTime();
		List<String> joinFields = new ArrayList<String>();
		entityName = m_inflector.getEntityName(entityName);
		String detailFieldName = entityNameDetails;
		if (entityNameDetails != null) {
			joinFields.add(entityNameDetails);
			entityNameDetails = getEntityNameFromProperty(sdesc, entityName, entityNameDetails);
		} else {
			entityNameDetails = null;
		}
		Map retMap = new HashMap();
		try {
			Map fieldSets = m_settingService.getFieldSets(config, sdesc.getNamespace(), entityName);
			boolean hasTeamSecurity = hasTeamSecurity(sessionContext, entityName, entityNameDetails);
			boolean hasStateSelect = hasStateSelect(sdesc, entityName, entityNameDetails);
			System.out.println("hasStateSelect:" + hasStateSelect + "," + params.get("state")); 
			String orderBy = getOrderBy(params, entityName, detailFieldName);
			Map filtersMap = null;
			if (params.get("filters") != null && "".equals(params.get("filters")) == false) {
				filtersMap = (Map) m_ds.deserialize((String) params.get("filters"));
			}
			if (params.get("filter") != null) {
				filtersMap = (Map) params.get("filter");
			}
			/*if (params.get("luceneQuery") != null && "".equals(params.get("luceneQuery")) == false) {
				filtersMap = createFiltersMapFromLuceneQuery(sessionContext, entityName, (String) params.get("luceneQuery"));
				if (filtersMap == null) {
					Map ret = new HashMap();
					ret.put("rows", new ArrayList());
					return ret;
				}
			}*/
			QueryBuilder qb = new QueryBuilder("pg", sdesc, entityName, hasTeamSecurity, sessionContext.getConfigName(), sessionContext, joinFields, filtersMap, (Map) params, fieldSets);
			qb.addSelector(entityName);
			List<String> fieldsArray = null;
			boolean fullyQualified = false;
			if (params.get("fields") != null) {
				if (params.get("fields") instanceof List) {
					fieldsArray = (List) params.get("fields");
				} else {
					if (((String) params.get("fields")).length() > 0) {
						fieldsArray = (List) m_ds.deserialize((String) params.get("fields"));
					}
				}
				if (fieldsArray != null) {
					for (String field : fieldsArray) {
						int dot = field.indexOf(".");
						if (dot != -1) {
							qb.addSelector(field);
							fullyQualified = true;
							String[] name = field.split("\\.");
							String selector = name[0];
						}
					}
				}
			}
			String andStr = "";
			String whereClause = "where";
			if (params.get("whereVal") != null && ((String) params.get("whereVal")).trim() != "") {
				whereClause += " " + params.get("whereVal");
				andStr = " and";
			}
			String w = qb.getWhere();
			if (w != null) {
				whereClause += andStr + " " + w;
				andStr = " and";
			}
			String jointype = null;
			if (entityNameDetails != null) {
				jointype = "join";
				Class mainClass = sessionContext.getClass(entityName);
				Map permittedFields = sessionContext.getPermittedFields(entityName);
				String pk = getPrimaryKey(mainClass);
				Map c = (Map) permittedFields.get(pk);
				if (c != null && c.get("datatype") != null && "string".equals(c.get("datatype"))) {
					whereClause += andStr + " (" + getAlias(entityName) + "." + pk + " = '" + idParent + "')";
				} else if (c != null && c.get("datatype") != null && "number".equals(c.get("datatype"))) {
					whereClause += andStr + " (" + getAlias(entityName) + "." + pk + " = " + idParent + ")";
				} else {
					//Default id = string 
					whereClause += andStr + " (" + getAlias(entityName) + "." + pk + " = '" + idParent + "')";
				}
				andStr = " and";
				if (fieldsArray == null) {
					String alias = entityNameDetails;
					if (joinFields.size() > 0) {
						alias = joinFields.get(0);
					}
					boolean isMap = TypeUtils.isMap(mainClass,detailFieldName);
					if(isMap){
						Class childClass = TypeUtils.getTypeForField(mainClass,detailFieldName);
						String varName = "v_"+childClass.getSimpleName().toLowerCase();
						qb.addVariable(childClass.getName()+" "+varName);
						fieldsArray = qb.getProjectionListEntity(entityNameDetails, varName);

						whereClause += andStr + " (" + getAlias(entityName) + "." + detailFieldName + ".containsValue("+varName+"))";
					}else{
						qb.addSelector(entityName);
						fieldsArray = qb.getProjectionListEntity(entityNameDetails, entityName + "$" + alias);
					}
				}
			}
			if ("where".equals(whereClause)) {
				andStr = "";
				whereClause = "";
			}
			List<String> projList = null;
			if (fieldsArray != null) {
				projList = fieldsArray;
				String pk = null;
				if (entityNameDetails != null) {
					pk = getPrimaryKey(sessionContext.getClass(entityNameDetails));
				} else {
					pk = getPrimaryKey(sessionContext.getClass(entityName));
				}
				if (!Utils.containsId(projList, pk)) {
					projList.add(pk);
				}
				qb.addSelectors(fieldsArray);
			} else {
				if (entityNameDetails != null) {
					projList = qb.getProjectionListAll(entityNameDetails);
				} else {
					projList = qb.getProjectionListAll(entityName);
				}
			}
			String projection = getProjectionString(projList);
			String from = qb.getFrom(jointype);
			if (hasTeamSecurity) {
				if (entityNameDetails != null) {
					projection = projection + "," + entityName + "$" + detailFieldName;
				} else {
					projection = projection + "," + entityName;
				}
				projList.add("_team_list");
			}
			List<String> involvedModule = qb.getInvolvedEntity();
			Map<String, Class> involvedClasses = getInvolvedClasses(sdesc, involvedModule);

			String teamUserWhere = qb.getTeamUserWhere();
			if( teamUserWhere != null){
				teamUserWhere = andStr + " "+ teamUserWhere;
				andStr = " and";
			}else{
				teamUserWhere = "";
			}

			String teamSecurityWhere = qb.getTeamSecurityWhere();
			if( teamSecurityWhere != null){
				teamSecurityWhere = andStr + " "+ teamSecurityWhere;
			}else{
				teamSecurityWhere = "";
			}

			String stateWhere="";
			if( hasStateSelect && !Utils.getBoolean(params, DISABLE_STATESELECT, false)){
				String state = qb.getRequestedState();
				String qualifier = null;
				if (entityNameDetails != null) {
					qualifier = entityName + "$" + detailFieldName;
				} else {
					qualifier = entityName;
				}
				stateWhere =  andStr+" "+ getStateWhere(qualifier,state);
				andStr = " and";
			}
			boolean noResultSetCount = noResultSetCount(sdesc, entityName, entityNameDetails);
			String sql = "Select distinct " + projection + " from " + from + " " + whereClause + " " + stateWhere + " " + teamUserWhere + " " + teamSecurityWhere + " "+ orderBy;
			info("=========================================================================================");
			info("sql:" + sql);
			info("=========================================================================================");
			info("Imports:"+sdesc.getImports());
			Query q = pm.newQuery("javax.jdo.query.JPQL", sql);
			q.declareImports(sdesc.getImports());
			int offset = getInt(params, "offset", 0);
			int pageSize = getInt(params, "pageSize", 30);
			if( noResultSetCount && pageSize >0){
				q.setRange(offset,offset+pageSize);
			}
			String vars = qb.getVariables();
			if( vars!= null) q.declareVariables(vars);
			List results = (List) q.executeWithMap(qb.getQueryParams());
			Iterator itr = results.iterator();
			int count = 0;
			boolean first = true;
			Field fields[] = null;
			String colNames[] = null;
			boolean isRelatedTo[] = null;
			int total = 0;
			List dataList = new ArrayList();
			String defaultClassName = m_inflector.getClassName(entityNameDetails != null ? entityNameDetails : entityName);
			Map<Class,String> primKeyNameMap = new HashMap();
			while (itr.hasNext()) {
				Object[] row = null;
				Object no = itr.next();
				if( !(no instanceof Object[] )){
					row = new Object[1]; 
					row[0] = no;
				}else{
					row = (Object[]) no;
				}
				if (noResultSetCount || pageSize == 0 || (total >= offset && count < pageSize)) {
					Map<String, Object> rowMap = new HashMap<String, Object>();
					if (first) {
						colNames = new String[row.length];
						isRelatedTo = new boolean[row.length];
						for (int col = 0; col < row.length; col++) {
							debug("\tColumn:"+row[col]);
							String colName = projList.get(col);
							Field field = getFieldForColName(sdesc, colName, defaultClassName);
							isRelatedTo[col] = field != null ? field.isAnnotationPresent(RelatedTo.class) : false;
							if (fullyQualified == false) {
								int dot = colName.indexOf(".");
								if (dot != -1) {
									colName = colName.substring(dot + 1);
								}
							}
							boolean isPermitted = true;
							if( field != null){
								String clazz = getLastElement(field.getDeclaringClass().getName());
								String name = field.getName();
								isPermitted = sessionContext.isFieldPermitted(name, clazz);
								debug("\tisPermitted("+clazz+","+name+"):"+isPermitted);
							}
							if (colName.equals("id") || isPermitted ) {
								colNames[col] = colName;
							} else {
								colNames[col] = null;
							}
						}
						first = false;
					}
					for (int col = 0; col < row.length; col++) {
						String colName = colNames[col];
						if (colName == null){
							continue;
						}
						if (isRelatedTo[col]) {
							if (row[col] != null) {
								Class clazz = row[col].getClass();
								if( primKeyNameMap.get(clazz) == null){
									primKeyNameMap.put( clazz, getPrimaryKey(clazz));
								}
								Object id = PropertyUtils.getProperty(row[col], primKeyNameMap.get(clazz));
								rowMap.put(colName, id + "");
								//rowMap.put(colName,getTitle(sessionContext, m_inflector.getEntityName(clazz.getSimpleName()),new BeanMap(row[col]), id));
							} else {
								rowMap.put(colName, "");
							}
						} else if (row[col] instanceof byte[]) {
							debug("Bytearray");
						} else if (row[col] instanceof java.util.Date) {
							rowMap.put(colName, String.valueOf(((Date) row[col]).getTime()));
						} else {
							rowMap.put(colName, row[col]);
						}
					}
					String _entityName = entityNameDetails != null ? entityNameDetails : entityName;
					evaluteFormulas(sessionContext, _entityName, rowMap, "out", involvedClasses, pm);
					if (hasTeamSecurity) {
						Object x = rowMap.get("_team_list");
						if (x != null) {
							Set<Object> _teams = null;
							if (PropertyUtils.isReadable(x, "_team_list")) {
								_teams = (Set<Object>) PropertyUtils.getProperty(x, "_team_list");
							} 
							rowMap.remove("_team_list");
							if (_teams != null && _teams.size() > 0) {
								debug("NOT Checking");
								/*if (hasTeamSecurity && !m_teamService.checkTeams(sessionContext.getStoreDesc().getNamespace(), sessionContext.getUserName(), sessionContext.getUserProperties(), _teams)) {
									debug("No teampermission:" + rowMap);
									continue;
								}*/
							}
						}
					}
					dataList.add(rowMap);
					count++;
				}
				total++;
			}
			long pagesTotal = (total == 0 || pageSize == 0) ? 0 : Math.round((total / pageSize) + 0.4999);
			if( noResultSetCount ){
				pagesTotal = total==pageSize ? -1 : 0;
				total = total==pageSize|| offset>0 ? -1 : total;
				
			}
			info("<== getTable.rows:" + dataList.size());
			info("<== getTable.recordsTotal:" + total);
			debug("getTable.pagesTotal:" + pagesTotal);
			debug("getTable.page:" + getInt(params, "page", 0));
			Map ret = new HashMap();
			ret.put("records", total);
			ret.put("page", params.get("page"));
			ret.put("total", pagesTotal);
			ret.put("rows", dataList);
			retMap = ret;
			long end = new Date().getTime();
			info("Dauer_query(rows:" + total + "):" + (end - start));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return retMap;
	}

	public Map querySql(SessionContext sessionContext, StoreDesc sdesc, Map params, String sql) {
		debug("query:" + params + ",sql:" + sql ); 
		PersistenceManager pm = sessionContext.getPM();
		Map retMap = new HashMap();
		try {
			debug("=========================================================================================");
			debug("sql:" + sql.replace("\n"," "));
			debug("=========================================================================================");
			debug("Imports:"+sdesc.getImports());
			Query q = pm.newQuery("javax.jdo.query.JPQL", sql.replace("\n"," "));
			q.declareImports(sdesc.getImports());
			List results = (List) q.executeWithMap(params);
			Iterator itr = results.iterator();
			int count = 0;
			int offset = getInt(params, "offset", 0);
			boolean first = true;
			Field fields[] = null;
			String colNames[] = null;
			int total = 0;
			List dataList = new ArrayList();
			int pageSize = getInt(params, "pageSize", 30);
			while (itr.hasNext()) {
				Object[] row = null;
				Object no = itr.next();
				if (pageSize == 0 || (total >= offset && count < pageSize)) {
					dataList.add(new HashMap(new BeanMap(no)));
					count++;
				}
				total++;
			}
			Map ret = new HashMap();
			ret.put("rows", dataList);
			retMap = ret;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		return retMap;
	}

/*	private Map createFiltersMapFromLuceneQuery(SessionContext sessionContext, String entityName, String query) {
		Map ret = new HashMap();
		ret.put("connector", "or");
		List<Map> children = new ArrayList();
		ret.put("children", children);
		List<Integer> idList = m_luceneService.queryIdList(sessionContext.getStoreDesc().getNamespace(), entityName, query);
		if (idList.size() == 0) {
			return null;
		}
		for (int id : idList) {
			Map cmap = new HashMap();
			cmap.put("field", "id");
			cmap.put("op", "eq");
			cmap.put("data", id);
			cmap.put("connector", null);
			children.add(cmap);
		}
		return ret;
	}*/

	private String getStateWhere(String qualifier, String state){
		String w =null;
		if( state.equals(STATE_OK) || state.equals(STATE_NEW)){
			w = "("+qualifier + "."+STATE_FIELD+"='"+state+"' or "+qualifier+"."+STATE_FIELD+" is null)";
		}else{
			w = "("+qualifier + "."+STATE_FIELD+"='"+state+"')";
		}
		return w;
	}

	private Field getFieldForColName(StoreDesc sdesc, String colName, String defaultClassName) {
		String fieldName = colName;
		String className = defaultClassName;
		int dot = colName.indexOf(".");
		if (dot != -1) {
			className = colName.substring(0, dot);
			fieldName = colName.substring(dot + 1);
		}
		Class clazz = null;
		try {
			int dollar = className.lastIndexOf("$");
			if (dollar != -1) {
				String[] path = className.split("\\$");
				className = m_inflector.getClassName(path[0]);
				clazz = getClass(sdesc, className);
				for (int i = 1; i < path.length; i++) {
					Field field = clazz.getDeclaredField(path[i]);
					clazz = TypeUtils.getTypeForField(field);
				}
			} else {
				className = m_inflector.getClassName(className);
				clazz = getClass(sdesc, className);
			}
			Field field = clazz.getDeclaredField(fieldName);
			return field;
		} catch (Exception e) {
			//e.printStackTrace(); 
			debug("getFieldForColName(" + colName + "," + defaultClassName + "):" + e);
			return null;
		}
	}

	private String getEntityNameFromProperty(StoreDesc sdesc, String entityName,String entityNameDetails){
		debug("getEntityNameFromProperty1:"+entityName+"|"+entityNameDetails);
		try {
			Class t = TypeUtils.getTypeForField(newInstance(sdesc, entityName), entityNameDetails);
			String name = t.getName();
			int ind = name.lastIndexOf(".");
			entityNameDetails = m_inflector.getEntityName(name.substring(ind + 1));
		} catch (Exception e) {
			new RuntimeException("JdoLayerImpl.getEntityNameFromProperty:"+e);
		}
		debug("getEntityNameFromProperty2:"+entityNameDetails);
		return entityNameDetails;
	}

	public Map getObjectGraph(StoreDesc sdesc, String entityName, String id) {
		SessionContext sessionContext = getSessionContext(sdesc);
		checkPermissions(sdesc, sessionContext.getUserName(),entityName, null, "read");
		debug("getObjectGraph:entityName:" + entityName + ",id:" + id);
		Map retMap = new HashMap();
		PersistenceManager pm = sessionContext.getPM();
		try {
			String className = m_inflector.getClassName(entityName);
			Map permittedFields = sessionContext.getPermittedFields(entityName);
			Object objId = getIdObject(id, sdesc, permittedFields);
			Class clazz = getClass(sdesc, className);
			Object objectMaster = pm.getObjectById(clazz, objId);
			debug("getObject.master1:" + objectMaster);
			String oldEntityName = entityName;
			entityName = m_inflector.getEntityName(entityName);
			boolean hasTeamSecurity = hasTeamSecurity(sessionContext, entityName, null);
			Object objGraph = SojoObjectFilter.getObjectGraph(objectMaster, sessionContext);
			//evaluteFormulas(sessionContext, entityName, m, "out", getInvolvedClasses(sdesc, entityName), pm);
			retMap.put("result", objGraph);
		} catch (Exception e) {
			m_logger.error("getObject:", e);
			sessionContext.handleException(e);
			throw new RuntimeException(e);
		} finally {
			sessionContext.handleFinally();
		}
		return retMap;
	}

	public void evaluteFormulas(SessionContext sessionContext, String entityName, Map<String, Object> map, String direction) {
		evaluteFormulas(sessionContext, entityName, map, direction, null, null);
	}

	private void evaluteFormulas(SessionContext sessionContext, String entityName, Map<String, Object> map, String direction, Map<String, Class> involvedClasses, PersistenceManager pm) {
		Map permittedFields = sessionContext.getPermittedFields(entityName);
		Iterator kit = permittedFields.keySet().iterator();
		while (kit.hasNext()) {
			String field = (String) kit.next();
			if (permittedFields.get(field) instanceof Map) {
				Map cm = (Map) permittedFields.get(field);
				if (cm != null && cm.get("formula_" + direction) != null && !"".equals(cm.get("formula_" + direction))) {
					if (!"out".equals(direction)) {
						if (map.get(field) == null) {
							map.put(field, "");
						}
					}
				}
			}
		}
		MVELEvaluator evalator = null;
		if (pm != null) {
			evalator = new MVELEvaluator((Map) permittedFields.get("_selListMap"), involvedClasses, pm);
		} else {
			evalator = new MVELEvaluator((Map) permittedFields.get("_selListMap"));
		}
		evalator.setLocalVars(map);
		evalator.setLocalVar("se", evalator);
		evalator.setLocalVar("inflector", m_inflector);
		Set<String> keyset = new HashSet(map.keySet());
		for (String field : keyset) {
			int dot = field.lastIndexOf(".");
			String f = field;
			if (dot != -1) {
				f = field.substring(dot + 1);
			}
			Map cm = (Map) permittedFields.get(f);
			if (cm != null && cm.get("formula_" + direction) != null && !"".equals(cm.get("formula_" + direction))) {
				String formula = (String) cm.get("formula_" + direction);
				Object r = "";
				try {
					r = evalator.eval(formula);
					debug("evalOk:"+formula+"/"+r);
				} catch (Exception e) {
					debug("evalError:"+formula+"/"+e);
				}
				map.put(field, r);
			}
		}
	}
	private String evaluteTitle(SessionContext sessionContext, String entityName, Map<String, Object> map, String titleExpression, String def) {
		Map permittedFields = sessionContext.getPermittedFields(entityName);
		Map vars = new HashMap();
		Iterator kit = map.keySet().iterator();
		while (kit.hasNext()) {
			String field = (String) kit.next();
			if (permittedFields.get(field)!= null) {
				vars.put(field, map.get(field));
			}
		}
		debug("evaluteTitle:"+titleExpression+"/"+vars);
		try {
			String ret = "" + MVEL.eval(titleExpression, vars);
			debug("evaluteTitle.ret:"+ret);
			return ret;
		} catch (Exception e) {
			e.printStackTrace();
			info("TitleEval:"+e);
		}
		return def;
	}

	private String getProjectionString(List<String> fieldsArray) {
		StringBuffer sb = new StringBuffer();
		String komma = "";
		for (String field : fieldsArray) {
			sb.append(komma + field);
			komma = ",";
		}
		return sb.toString();
	}

	private String getAlias(String mod) {
		String mn = m_inflector.getEntityName(mod);
		return mn;
	}

	public synchronized SessionContext getSessionContext(StoreDesc sdesc) {
		try {
			debug("-->getSessionContext:" + sdesc);
			SessionManager sessionManager = (SessionManager)ThreadContext.getThreadContext().get(ThreadContext.SESSION_MANAGER);
			if( sessionManager == null){
				sessionManager = createSessionManager();
			}
			SessionContext sessionContext = new SessionContextImpl(sessionManager);
			sessionContext.setNucleusService(m_nucleusService);
			sessionContext.setEntityService(m_entityService);
			sessionContext.setSettingService(m_settingService);
			sessionContext.setGitService(m_gitService);
			sessionContext.setPermissionService(m_permissionService);
			sessionContext.setTeamService(m_teamService);
			sessionContext.setStoreDesc(sdesc);
			if (sdesc.isDataPack()) {
				//LuceneSession ls = m_luceneService.createSession(sdesc);
				//ls.setSessionContext(sessionContext);
				//ls.setTransactionManager(m_nucleusService.getTransactionManager());
				//sessionContext.setLuceneSession(ls);
			}
			sessionContext.setDataLayer(this);
			sessionContext.setUserProperties(sessionManager.getUserProperties());
			return sessionContext;
		} catch (Exception e) {
			throw new RuntimeException("JdoLayerImpl.getSessionContext:", e);
		}
	}
	private SessionManager createSessionManager() {
		try {
			debug("-->getSessionManager" );
			SessionManager sessionManager = new SessionManager(m_nucleusService);
			ThreadContext.getThreadContext().put(ThreadContext.SESSION_MANAGER, sessionManager);
			if( noAuth()){
				sessionManager.setUserProperties(new HashMap());
			}else{
				Map<String, Object> userProperties = m_authService.getUserProperties(sessionManager.getUserName());
				sessionManager.setUserProperties(userProperties);
			}
			return sessionManager;
		} catch (Exception e) {
			throw new RuntimeException("JdoLayerImpl.getSessionManager:", e);
		}
	}


	private void getBinary(SessionContext sessionContext, Object obj, boolean hasTeamSecurity, Map permittedFields, HttpServletResponse resp) throws Exception {
		Map<String, Object> map = new HashMap();
		BeanMap beanMap = new BeanMap(obj);
		if (beanMap.getType("_team_list") != null) {
			Set<Object> _teams = (Set<Object>) beanMap.get("_team_list");
			if (hasTeamSecurity && !m_teamService.checkTeams(sessionContext.getStoreDesc().getNamespace(), sessionContext.getUserName(), sessionContext.getUserProperties(), _teams)) {
				info("getPropertyMap.notAllowed:" + _teams);
				resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
		}
		if (PropertyUtils.isReadable(obj, "content")) {
			byte[] c = (byte[]) PropertyUtils.getProperty(obj, "content");
			if (PropertyUtils.isReadable(obj, "type")) {
				String t = (String) PropertyUtils.getProperty(obj, "type");
				resp.setContentType(t);
			}
			if (PropertyUtils.isReadable(obj, "filename")) {
				String t = (String) PropertyUtils.getProperty(obj, "filename");
				resp.addHeader("Content-Disposition", "inline;filename=" + t);
			}
			if( c!=null){
				debug("getBinary:length:"+c.length);
				IOUtils.write(c, resp.getOutputStream());
			}
			resp.setStatus(HttpServletResponse.SC_OK);
			resp.getOutputStream().close();
		}
	}

	private Map<String, Object> getPropertyMap(SessionContext sessionContext, Object obj, boolean hasTeamSecurity, Map permittedFields, List<String> fieldsArray) throws Exception {
		Map<String, Object> map = new HashMap();
		BeanMap beanMap = new BeanMap(obj);
		if (beanMap.getType("_team_list") != null) {
			Set<Object> _teams = (Set<Object>) beanMap.get("_team_list");
			debug("getPropertyMap.hasTeamSecurity:" + hasTeamSecurity);
			if (hasTeamSecurity && !m_teamService.checkTeams(sessionContext.getStoreDesc().getNamespace(), sessionContext.getUserName(), sessionContext.getUserProperties(), _teams)) {
				debug("getPropertyMap.notAllowed:" + _teams);
				return null;
			}
		}
		map.put("id", beanMap.get("id"));
		if (fieldsArray != null) {
			for (String field : fieldsArray) {
				if (beanMap.getType(field) == null) {
					m_logger.error("getPropertyMap.property_not_found:" + field);
					continue;
				}
				Map cm = (Map) permittedFields.get(field);
				if (cm == null) {
					continue;
				}
				try {
					Class type = beanMap.getType(field);
					Object value = beanMap.get(field);
					map.put(field, prepareValue(sessionContext, beanMap, value, (String) cm.get("datatype")));
				} catch (Exception e) {
					m_logger.error("getPropertyMap.field:" + field + "," + e);
					e.printStackTrace();
				}
			}
		} else {
			Iterator itk = beanMap.keyIterator();
			while (itk.hasNext()) {
				String field = (String) itk.next();
				Map cm = (Map) permittedFields.get(field);
				if (cm == null) {
					continue;
				}

				if( STATE_FIELD.equals(cm.get("id")) && !m_permissionService.hasAdminRole()){
					continue;
				}
				try {
					Class type = beanMap.getType(field);
					debug("Type:"+type+"/"+cm.get("datatype"));
					Object value = beanMap.get(field);
					if ("binary".equals(cm.get("datatype"))){
						continue;
					}
					if ("fulltext".equals(cm.get("datatype"))){
						continue;
					}
					if ("set".equals(cm.get("datatype"))){
						continue;
					}
					map.put(field, prepareValue(sessionContext, beanMap, value, (String) cm.get("datatype")));
				} catch (Exception e) {
					m_logger.error("getPropertyMap.field:" + field + "," + e);
					e.printStackTrace();
				}
			}
		}
		return map;
	}

	private String getRecordTitle(Object obj) {
		String names[] = { "name", "title", "shortname", "shortname_company", "shortname_person", "name1" };
		for (int i = 0; i < names.length; i++) {
			String name = null;
			try {
				name = (String) PropertyUtils.getProperty(obj, names[i]);
			} catch (Exception e) {
				debug("ex:" + e);
			}
			if (name != null && name.length() > 0) {
				return name + "";
			}
		}
		return "Id";
	}

	private void setRelatedToFields(Map targetMap, String[] colNames, BeanMap beanMap, Object value) {
		BeanMap relatedTo = new BeanMap(value);
		Iterator<String> it = relatedTo.keySet().iterator();
		while (it.hasNext()) {
			String key = it.next();
			if (key.startsWith("_"))
				continue;
			if (key.equals("class"))
				continue;
			if (key.equals("id"))
				continue;
			if (key.equals("name"))
				continue;
			if (colNames != null && !arrayContains(colNames, key))
				continue;
			if (beanMap != null && !beanMap.containsKey(key))
				continue;
			Class type = relatedTo.getType(key);
			if (type.equals(List.class) || type.equals(Set.class))
				continue;
			debug("\tsetRelatedToFields.key:" + key + "=" + relatedTo.get(key) + "/" + relatedTo.getType(key));
			targetMap.put(key, relatedTo.get(key));
		}
	}

	private boolean arrayContains(String[] arr, String key) {
		for (String a : arr) {
			if (a != null && a.equals(key))
				return true;
		}
		return false;
	}

	private Object prepareValue(SessionContext sc, BeanMap beanMap, Object value, String dt) throws Exception {
		if (value == null) {
			if (dt.startsWith("array/string")) {
				return new ArrayList();
			} else {
				return null;
			}
		}
		if (value instanceof java.util.Date) {
			return String.valueOf(((Date) value).getTime());
		} else if (value instanceof java.lang.Boolean) {
			return value;
		} else if (value instanceof java.lang.Double) {
			return value;
		} else if (value instanceof java.lang.Integer) {
			return value;
		} else if (dt.startsWith("related")) {
			Object ret = "";
			try {
				Object id = (Object) PropertyUtils.getProperty(value, "id");
				ret = id;
				String name = getRecordTitle(value);
				ret = id + "/" + name;
				ret = getTitle(sc,m_inflector.getEntityName(value.getClass().getSimpleName()),new BeanMap(value), id, (String)ret);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return ret;
		} else if (dt.startsWith("array/string")) {
			List arr = new ArrayList();
			String[] vals = String.valueOf(value).split(",");
			for (int i = 0; i < vals.length; i++) {
				String v = vals[i];
				arr.add(v);
			}
			return arr;
		} else if (dt.startsWith("map_")) {
			return value;
		} else if (dt.startsWith("list_")) {
			return value;
		} else if (dt.startsWith("array/")) {
			String t = dt.substring(dt.indexOf("/") + 1);
			if ("team".equals(t)){
				t = "_team_list";
			}
			List arr = new ArrayList();
			Collection<Object> _teams = (Collection) beanMap.get(t);
			for (Object team : _teams) {
				BeanMap bm = new BeanMap(team);
				Iterator itk = bm.keyIterator();
				Map map = new HashMap();
				while (itk.hasNext()) {
					String field = (String) itk.next();
					if ("class".equals(field)) {
						continue;
					}
					Object val = bm.get(field);
					map.put(field, prepareValue(sc, bm, val, ""));
				}
				arr.add(map);
			}
			return arr;
		} else {
			return String.valueOf(value);
		}
	}


	public Class getClass(SessionContext sessionContext, String entityName) {
		return getClass(sessionContext.getStoreDesc(), m_inflector.getClassName(entityName));
	}

	private Class getClass(StoreDesc sdesc, String className) {
		return m_nucleusService.getClass(sdesc, className);
	}

	protected Object newInstance(StoreDesc sdesc, String entity) {
		Object o = null;
		String clazzName = m_inflector.getClassName(entity);
		Class clazz = m_nucleusService.getClass(sdesc, clazzName);
		try {
			o = clazz.newInstance();
		} catch (Exception e) {
		}
		return o;
	}

	private Map<String, Class> getInvolvedClasses(StoreDesc sdesc, String entity) {
		Map<String, Class> classes = new HashMap();
		classes.put(entity, getClass(sdesc, m_inflector.getClassName(entity)));
		return classes;
	}

	private Map<String, Class> getInvolvedClasses(StoreDesc sdesc, List<String> modules) {
		Map<String, Class> classes = new HashMap();
		try {
			for (String m : modules) {
				classes.put(m, getClass(sdesc, m_inflector.getClassName(m)));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return classes;
	}

	private class DummyIterator implements Iterator {

		private int next = 2;

		Object m_id = null;

		public DummyIterator() {
		}

		public DummyIterator(Object id) {
			m_id = id;
		}

		public boolean hasNext() {
			next--;
			return next > 0;
		}

		public Object next() {
			return m_id;
		}

		public void remove() {
		}
	}

	private void setDefaultValues(Class clazz, Object o) throws Exception {
		debug("----->setDefaultValues.clazz:" + clazz + "/" + o);
		Field[] fields = clazz.getDeclaredFields();
		for (int i = 0; i < fields.length; i++) {
			java.lang.annotation.Annotation[] anns = fields[i].getDeclaredAnnotations();
			for (int j = 0; j < anns.length; j++) {
				try {
					Class atype = anns[j].annotationType();
					if (!(anns[j] instanceof javax.jdo.annotations.Column)) {
						continue;
					}
					Method methDf = atype.getDeclaredMethod("defaultValue");
					//Method methAn = atype.getDeclaredMethod("allowsNull");//@@@MS ???? 
					String df = (String) methDf.invoke(anns[j], new Object[0]);
					//String al = (String) methAn.invoke(anns[j], new Object[0]); 
					if (df != null && df.length() > 0) {
						Class type = TypeUtils.getTypeForField(o, fields[i].getName());
						Object v = ConvertUtils.convert(df, type);
						debug("setDefaultValues:" + fields[i].getName() + ":" + v + "/" + type);
						PropertyUtils.setProperty(o, fields[i].getName(), v);
					}
				} catch (Exception e) {
					debug("setDefaultValues.e:" + e);
				}
			}
		}
	}

	private String getPrimaryKey(Class clazz) throws Exception {
		debug("----->getPrimaryKey.clazz:" + clazz + " -> ");
		Field[] fields = clazz.getDeclaredFields();
		for (int i = 0; i < fields.length; i++) {
			java.lang.annotation.Annotation[] anns = fields[i].getDeclaredAnnotations();
			for (int j = 0; j < anns.length; j++) {
				try {
					Class atype = anns[j].annotationType();
					if (!(anns[j] instanceof javax.jdo.annotations.PrimaryKey)) {
						continue;
					}
					debug(fields[i].getName());
					return fields[i].getName();
				} catch (Exception e) {
					debug("getPrimaryKey.e:" + e);
				}
			}
		}
		throw new RuntimeException("JdoLayerImpl.getPrimaryKey(" + clazz + "):no_primary_key");
	}

	private String getRecordValidation(SessionContext sc, String entityName) {
	  Map m = m_settingService.getPropertiesForEntityView( sc.getStoreDesc().getNamespace(), GLOBAL_SETTINGS, entityName, null);
		String val = (String)m.get(RECORDVALIDATION);
		return val;
	}

	private String getTitle(SessionContext sc, String entityName, Map<String,Object> map, Object id,String def) {
	  Map m = m_settingService.getPropertiesForEntityView( sc.getStoreDesc().getNamespace(), GLOBAL_SETTINGS, entityName, null);
		String te = (String)m.get(TITLEEXPRESSION);
		if( isEmpty(te)){
			 return def;
		}
		return evaluteTitle(sc,entityName, map, te, def)+"/"+id;
	}
	private boolean hasStateSelect(StoreDesc sdesc, String entityName, String entityNameDetails) {
		String en = entityName;
		if( entityNameDetails!=null){
			en = entityNameDetails;
		}
	  Map m = m_settingService.getPropertiesForEntityView( sdesc.getNamespace(), GLOBAL_SETTINGS, en, null);
		return m.get(STATESELECT) != null ? (Boolean)m.get(STATESELECT) : false;
	}
	private boolean noResultSetCount(StoreDesc sdesc, String entityName, String entityNameDetails) {
		String en = entityName;
		if( entityNameDetails!=null){
			en = entityNameDetails;
		}
	  Map m = m_settingService.getPropertiesForEntityView( sdesc.getNamespace(), GLOBAL_SETTINGS, en, null);
		return m.get(NORESULTSETCOUNT) != null ? (Boolean)m.get(NORESULTSETCOUNT) : false;
	}

	private boolean isEmpty(String s) {
		return (s == null || "".equals(s.trim()));
	}

	protected String getRelatedTo(Object insert, Class clazzMaster, String propNameMaster) throws Exception {
		debug("getRelatedTo:" + insert + ",clazzMaster:" + clazzMaster + "," + propNameMaster);
		Class clazzInsert = insert.getClass();
		Field[] fields = clazzInsert.getDeclaredFields();
		for (Field field : fields) {
			Annotation ann = field.getAnnotation(RelatedTo.class);
			if (ann != null) {
				Class t = field.getType();
				debug("t:" + t);
				if (t.equals(clazzMaster)) {
					Class atype = ann.annotationType();
					Method meth = atype.getDeclaredMethod("value");
					debug("atype:" + atype + ",meth:" + meth);
					String val = (String) meth.invoke(ann, new Object[0]);
					debug("val:" + val + "," + field.getName());
					if (val != null && val.length() > 0) {
						if (propNameMaster.equals(val)) {
							return field.getName();
						}
					} else {
						return field.getName();
					}
				}
			}
		}
		return null;
	}

	private String getAddWhere(QueryBuilder qb, String entityName, String entityNameDetails, String detailFieldName){
		String whereResult = "";
		String andStr = " and ";
		String teamUserWhere = qb.getTeamUserWhere();
		if( teamUserWhere != null){
			whereResult = andStr + teamUserWhere;
		}

		String teamSecurityWhere = qb.getTeamSecurityWhere();
		if( teamSecurityWhere != null){
			whereResult = andStr + " "+ teamSecurityWhere;
		}

		boolean hasStateSelect = hasStateSelect(qb.getSessionContext().getStoreDesc(), entityName, entityNameDetails);
		if( hasStateSelect){
			String state = qb.getRequestedState();
			String qualifier = null;
			if (entityNameDetails != null) {
				qualifier = entityName + "$" + detailFieldName;
			} else {
				qualifier = entityName;
			}
			whereResult =  andStr + getStateWhere(qualifier,state);
		}
		return whereResult;
	}

	private List<Map> constructConstraitViolationList(Set<ConstraintViolation> constraintViolations) {
		List<Map> cvList = new ArrayList();
		if( constraintViolations == null) return cvList;
		for (ConstraintViolation cv : constraintViolations) {
			Map<String, String> mapCV = new HashMap();
			mapCV.put("message", cv.getMessage());
			mapCV.put("path", cv.getPropertyPath() + "");
			cvList.add(mapCV);
		}
		debug("constraintViolations:" + cvList);
		return cvList;
	}

	private String getAppName(String appName) {
		if ("xaddress".equals(appName)) {
			return "xaddr";
		}
		return appName;
	}

	private boolean hasTeamSecurity(SessionContext sessionContext, String entityName, String entityNameDetails) {
		Map entityMap = sessionContext.getEntitytype(entityName);
		boolean hasTeamSecurity = Utils.getBoolean(entityMap, "team_security", false);
		debug("hasTeamSecurity:" + hasTeamSecurity);
		if (entityNameDetails != null) {
			Map em = sessionContext.getEntitytype(entityNameDetails);
			hasTeamSecurity = Utils.getBoolean(em, "team_security", false);
			debug("hasTeamSecurityDetails:" + hasTeamSecurity);
		}
		return hasTeamSecurity;
	}

	private String getOrderBy(Map params, String entityName, String entityNameDetails) {
		String orderBy = "";
		if (params.get("orderby") != null && !"".equals(params.get("orderby"))) {
			String alias = getAlias(entityNameDetails == null ? entityName : entityNameDetails);
			if (entityNameDetails != null && entityName != null) {
				alias = entityName + "$" + entityNameDetails;
			}
			if (((String) params.get("orderby")).indexOf(".") == -1) {
				orderBy = "order by " + alias + "." + params.get("orderby");
			} else {
				orderBy = "order by " + params.get("orderby");
			}
		}
		return orderBy;
	}

	private Object getIdObject(String id, StoreDesc sdesc, Map config) {
		if (sdesc.getIdType().equals("string")) {
			return id;
		} else if (sdesc.getIdType().equals("long")) {
			return Long.valueOf(id);
		}
		if (config != null) {
			Map c = (Map) config.get("id");
			if (c != null && c.get("datatype") != null && "string".equals(c.get("datatype"))) {
				return id;
			} else {
				return Long.valueOf(id);
			}
		}
		return id;
	}

	private String getLastElement(String path) {
		return getLastElement(path, ".");
	}

	private String getLastElement(String path, String sep) {
		int lastDot = path.lastIndexOf(sep);
		return path.substring(lastDot + 1);
	}


	protected int getInt(Map m, String key, int _def) {
		try {
			Object i= m.get(key);
			if( i==null) return _def;
			if( i instanceof Integer){
				return (Integer)i;
			}
			return Integer.parseInt(String.valueOf(m.get(key)));
		} catch (Exception e) {
		}
		return _def;
	}

	private Object copyObject(Object o) throws Exception{
		Map n = new HashMap();
		BeanMap beanMap = new BeanMap(o);
		Iterator itv = beanMap.keyIterator();
		while (itv.hasNext()) {
			String prop = (String) itv.next();
			if ("class".equals(prop)) {
				continue;
			}
			Object value = beanMap.get(prop);
			if ("_team_list".equals(prop)) {
				Set teamSet = new HashSet();	
				Set teams = (Set)beanMap.get(prop);
				for (Object team : teams) {
					Map t = new HashMap(new BeanMap(team));
					t.remove("teamintern");
					teamSet.add(t);
				}
				value = teamSet;
			}else if( value instanceof Collection){
				continue;
			}else{
				java.lang.reflect.Field field = o.getClass().getDeclaredField(prop);
				if (field != null) {
					if (field.isAnnotationPresent(Element.class) || field.isAnnotationPresent(Persistent.class)) {
						continue;
					}
				}
			}
			n.put(prop, value);
		}
		return n;
	}
	private boolean isStateEqual(String s1, String s2){
		if( s1 == null && s2 == null) return true;
		if( s1 == null && s2 != null) return false;
		if( s1 != null && s2 == null) return false;
		return s1.equals(s2);
	}


	private Map<String,String> getState(Map dataMap, Object objectUpdate){
		String stateNew = null;
		String stateOld = null;
		try{
			stateNew = (String)dataMap.get(STATE_FIELD);
			stateOld = (String)PropertyUtils.getProperty(objectUpdate, STATE_FIELD);
		}catch(Exception e){
			debug("getState:"+e);
		}
		debug("State:old:"+stateOld+"/new:"+stateNew);
		Map map = new HashMap();
		map.put("stateOld",stateOld);
		map.put("stateNew",stateNew);
		return map;
	}

	private void setState(Map<String,String> stateMap, Object objectUpdate){
		String stateOld = stateMap.get("stateOld");
		String stateNew = stateMap.get("stateNew");
		try{
			if( m_permissionService.hasAdminRole() /*&& !isStateEqual(stateOld,stateNew)*/){
				PropertyUtils.setProperty(objectUpdate, STATE_FIELD, stateNew);
			}else{
				PropertyUtils.setProperty(objectUpdate, STATE_FIELD, null);
			}
		}catch(Exception e){
			debug("setState:"+e);
		}
	}


	private boolean noAuth() {
		String sh = System.getProperty("workspace");
		try {
			String basedir = new File(sh).getCanonicalFile().getParent();
			debug("NOAUTH:" + new File(basedir, "noauth").exists());
			return new File(basedir, "noauth").exists();
		} catch (Exception e) {
		}
		return false;
	}
	/*public MediaType getContentType(InputStream is, String fileName,TikaConfig tikaConfig) {
		MediaType mediaType;
		Metadata md = new Metadata();
		md.set(Metadata.RESOURCE_NAME_KEY, fileName);
		Detector detector = new ContainerAwareDetector(tikaConfig.getMimeRepository());

		try {
			mediaType = detector.detect(is, md);
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return null;
		}
		return mediaType;
	}*/
	public static String replaceTokens(String text, Map<String, String> replacements) {
		Pattern pattern = Pattern.compile("[\\@\\$]\\{(.+?)\\}");
		Matcher matcher = pattern.matcher(text);
		StringBuilder builder = new StringBuilder();
		int i = 0;
		while (matcher.find()) {
			String replacement = replacements.get(matcher.group(1));
			builder.append(text.substring(i, matcher.start()));
			if (replacement == null)
				builder.append(matcher.group(0));
			else
				builder.append(replacement);
			i = matcher.end();
		}
		builder.append(text.substring(i, text.length()));
		return builder.toString();
	}
	private void displayInfo( String where, long startTime ) {
    long time = new Date().getTime() - startTime;
    long fm = Runtime.getRuntime().freeMemory() / ( 1024 * 1024 );
    long tm = Runtime.getRuntime().totalMemory() / ( 1024 * 1024 );
    debug( "Memory(" + where + "):free=" + fm + "M,total=" + tm + "M,time:" + time +" mSec");
  }

	protected void debug(String message) {
		m_logger.debug(message);
		//System.out.println(message);
	}
	protected void info(String message) {
		m_logger.info(message);
		System.out.println(message);
	}
	/************************************ C O N F I G ********************************************************/
	/*	public void setRulesService(RulesService rulesService) {
		this.m_rulesService = rulesService;
		System.out.println("JdoLayerImpl.setRulesService:" + rulesService);
	}*/
	//@Reference(dynamic = true)
	//public void setLuceneService(LuceneService paramLuceneService) {
	//	this.m_luceneService = paramLuceneService;
	//	info("JdoLayerImpl.setLuceneService:" + paramLuceneService);
	//}

	@Reference(dynamic = true)
	public void setTriggerService(TriggerService paramTriggerService) {
		this.m_triggerService = paramTriggerService;
		info("JdoLayerImpl.setTriggerService:" + paramTriggerService);
	}

	@Reference(dynamic = true, optional = true)
	public void setEntityService(EntityService paramEntityService) {
		this.m_entityService = paramEntityService;
		info("JdoLayerImpl.setEntityService:" + paramEntityService);
	}

	@Reference(dynamic = true)
	public void setNucleusService(NucleusService paramNucleusService) {
		this.m_nucleusService = paramNucleusService;
		info("JdoLayerImpl.setNucleusService:" + paramNucleusService);
	}

	@Reference(dynamic = true)
	public void setPermissionService(PermissionService paramPermissionService) {
		this.m_permissionService = paramPermissionService;
		info("JdoLayerImpl.setPermissionService:" + paramPermissionService);
	}

	@Reference(dynamic = true)
	public void setTeamService(TeamService paramService) {
		this.m_teamService = paramService;
		info("JdoLayerImpl.setTeamService:" + paramService);
	}

	@Reference(dynamic = true, optional = true)
	public void setSettingService(SettingService paramSettingService) {
		this.m_settingService = paramSettingService;
		info("JdoLayerImpl.setSettingService:" + paramSettingService);
	}

	@Reference(dynamic = true, optional = true)
	public void setGitService(GitService paramGitService) {
		this.m_gitService = paramGitService;
		info("JdoLayerImpl.setGitService:" + paramGitService);
	}
	@Reference(dynamic = true, optional = true)
	public void setCamelService(CamelService paramCamelService) {
		this.m_camelService = paramCamelService;
		info("JdoLayerImpl.setCamelService:" + paramCamelService);
	}

	@Reference(target = "(impl=default)", dynamic = true)
	public void setDublettenCheckService(DublettenCheckService paramDublettenCheckService) {
		m_dublettenCheckService.put("default", paramDublettenCheckService);
		info("JdoLayerImpl.setDublettenCheckService:" + paramDublettenCheckService);
	}
	@Reference(target = "(impl=pg)", dynamic = true)
	public void setDublettenCheckServicePG(DublettenCheckService paramDublettenCheckService) {
		m_dublettenCheckService.put("pg", paramDublettenCheckService);
		info("JdoLayerImpl.setDublettenCheckServicePG:" + paramDublettenCheckService);
	}

	@Reference(dynamic = true, optional = true)
	public void setAuthService(AuthService paramAuthService) {
		this.m_authService = paramAuthService;
		info("JdoLayerImpl.setAuthService:" + paramAuthService);
	}
}
