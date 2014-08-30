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
package org.ms123.common.setting;

import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.jdo.Extent;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.transaction.UserTransaction;
import org.apache.commons.beanutils.PropertyUtils;
import org.ms123.common.data.api.SessionContext;
import org.ms123.common.permission.api.PermissionException;
import org.ms123.common.permission.api.PermissionService;
import org.ms123.common.entity.api.EntityService;
import org.ms123.common.libhelper.Bean2Map;
import org.ms123.common.libhelper.Inflector;
import org.ms123.common.utils.UtilsService;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static java.text.MessageFormat.format;

/** BaseSettingService implementation
 */
@SuppressWarnings("unchecked")
public class BaseSettingServiceImpl implements Constants {

	protected Inflector m_inflector = Inflector.getInstance();

	private Bean2Map m_b2m = new Bean2Map();
	protected boolean m_isRuntimeSystem;

	protected PermissionService m_permissionService;
	protected UtilsService m_utilsService;
	protected EntityService m_entityService;

	protected JSONDeserializer m_ds = new JSONDeserializer();

	protected JSONSerializer m_js = new JSONSerializer();
	protected MetaData m_gitMetaData;

	protected Map<String, Boolean> m_initMap = new HashMap();

	public BaseSettingServiceImpl() {
	}

	/* P U B L I C - A P I  */
	public Map getFieldSets(String settingsid, String namespace, String entityName) throws Exception {
		debug("getFieldSets:" + settingsid + "/" + entityName + "/" + namespace);
		List<Map> res = getFieldsetsForEntity(namespace, settingsid, entityName);
		debug("\t:" + res);
		return _listToMap(res, "fsname");
	}

	public List getFieldsForEntityView(String namespace, String entity, String view) throws Exception {
		return getFieldsForEntityView(namespace, GLOBAL_SETTINGS, entity, view);
	}

	public List getFieldsForEntityView(String namespace, String settingsid, String entity, String view) throws Exception {
		return getFieldsForEntityView(namespace, settingsid, entity, view, null, null, null);
	}

	public List<Map> getFieldsForEntityView(String namespace, String settingsid, String entity, String view, Map mapping, String filter, String sortField) throws Exception {
		return m_gitMetaData.getFieldsForEntityView(namespace,settingsid,entity,view,mapping,filter,sortField);
	}

	public List getFieldsetsForEntity(String namespace, String entity) throws Exception {
		return getFieldsetsForEntity(namespace, GLOBAL_SETTINGS, entity);
	}

	public List getFieldsetsForEntity(String namespace, String settingsid, String entity) throws Exception {
		return m_gitMetaData.getFieldsetsForEntity(namespace,settingsid,entity);
	}

	public void setResourceSetting(String namespace, String settingsid, String resourceid, Map settings) throws Exception {
		m_gitMetaData.setResourceSetting(namespace,settingsid,resourceid,settings,true);
	}

	public Map getResourceSetting(String namespace, String settingsid, String resourceid) throws Exception {
		return m_gitMetaData.getResourceSetting(namespace,settingsid,resourceid);
	}

	public void deleteResourceSetting(String namespace, String settingsid, String resourceid) throws Exception {
		m_gitMetaData.deleteResourceSetting(namespace,settingsid,resourceid);
	}


	private String getFilter(String settingsid, String resourceid) {
		String filter = SETTINGS_ENTITY + "." + SETTINGS_ID + " == '" + settingsid + "' && " + RESOURCE_ID + " == '" + resourceid + "'";
		return filter;
	}

	private String getContainerFilter(String settingsid) {
		String filter = SETTINGS_ID + " == '" + settingsid + "'";
		return filter;
	}

	private Object resourceLookup(String resourceid, Collection c) throws Exception {
		Iterator it = c.iterator();
		while (it.hasNext()) {
			Object o = it.next();
			String rid = (String) PropertyUtils.getProperty(o, RESOURCE_ID);
			if (resourceid.equals(rid)) {
				return o;
			}
		}
		return null;
	}

	protected List<Map> _mergeProperties(List<Map> l, Map<String, Map> mapMerge) {
		List<Map> resList = new ArrayList<Map>();
		for (Map<String, Object> m : l) {
			Map<String, Object> merge = mapMerge.get(m.get("name"));
			if (merge == null) {
				continue;
			}
			merge.putAll(m);
			resList.add(merge);
		}
		return resList;
	}

	protected Map<String, Map> _listToMap(List<Map> list, String key) {
		Map<String, Map> retMap = new HashMap();
		Iterator it = list.iterator();
		for (Map m : list) {
			String k = (String) m.get(key);
			retMap.put(k, m);
		}
		return retMap;
	}
	protected void debug(String msg) {
		//System.out.println(msg);
		m_logger.debug(msg);
	}
	protected void info(String msg) {
		System.out.println(msg);
		m_logger.info(msg);
	}
	private static final org.slf4j.Logger m_logger = org.slf4j.LoggerFactory.getLogger(BaseSettingServiceImpl.class);
}
