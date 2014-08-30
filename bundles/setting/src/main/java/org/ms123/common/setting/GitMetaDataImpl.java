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

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Date;
import flexjson.*;
import org.ms123.common.data.api.SessionContext;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.utils.ParameterParser;
import org.ms123.common.git.GitService;
import org.ms123.common.libhelper.Inflector;
import static java.text.MessageFormat.format;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
@SuppressWarnings("unchecked")
class GitMetaDataImpl implements MetaData, Constants {

	protected Inflector m_inflector = Inflector.getInstance();

	protected JSONDeserializer m_ds = new JSONDeserializer();

	protected JSONSerializer m_js = new JSONSerializer();

	private GitService m_gitService;

	private SettingServiceImpl m_ssi;

	private String USERSETTINGS_NAMESPACE = "local";

	/**
	 */
	public GitMetaDataImpl(GitService gs, SettingServiceImpl ssi) {
		m_gitService = gs;
		m_ssi = ssi;
		m_js.prettyPrint(true);
	}

	public Map getResourceSetting(String namespace, String settingsid, String resourceid) throws Exception {
		try {
			boolean isRuntime = m_ssi.m_isRuntimeSystem;		
			String ret = null;
			String path = format(SETTING_PATH, resourceid);
			if( isRuntime){
				if( m_gitService.exists( getRepo(namespace,true), path)){
					ret = m_gitService.getContent(getRepo(namespace,true), path);
				}else{
					ret = m_gitService.getContent(getRepo(namespace,false), path);
				}
			}else{
				ret = m_gitService.getContent(getRepo(namespace), path);
			}
			return (Map) m_ds.deserialize(ret);
		} catch (Exception e) {
			return null;
		}
	}

	public void setResourceSetting(String namespace, String settingsid, String resourceid, Map settings, boolean overwrite) throws Exception {
		if (!overwrite && m_gitService.exists(getRepo(namespace), format(SETTING_PATH, resourceid))) {
			debug("setResourceSetting:" + format(SETTING_PATH, resourceid) + "exists");
			return;
		}
		m_gitService.putContent(getRepo(namespace), format(SETTING_PATH, resourceid), SETTING_TYPE, m_js.deepSerialize(settings));
	}

	public void deleteResourceSetting(String namespace, String settingsid, String resourceid) throws Exception {
		debug("deleteResourceSetting:"+getRepo(namespace)+","+settingsid+","+resourceid);
		m_gitService.deleteObjects(getRepo(namespace), SETTINGS_PATH, resourceid);
	}

	public Map getFieldSets(String settingsid, String namespace, String entityName) throws Exception {
		debug("getFieldSets:" + settingsid + "/" + entityName + "/" + namespace);
		List<Map> res = getFieldsetsForEntity(namespace, settingsid, entityName);
		debug("\t:" + res);
		return _listToMap(res, "fsname");
	}

	public List<Map> getFieldsetsForEntity(String namespace, String settingsid, String entity) throws Exception {
		String resourceid = format("settings/entities.{0}.fieldsets", entity);
		String settingStr = getResource(namespace, settingsid, resourceid);
		if (settingStr != null) {
			Map m = (Map) m_ds.deserialize(settingStr);
			debug("m:" + m);
			return filterFieldSetFields((List) m.get(FIELDSETS),namespace, entity);
		} else {
			return new ArrayList();
		}
	}

	public List<Map> getFieldsForEntityView(String namespace, String settingsid, String entity, String view) throws Exception {
		return getFieldsForEntityView(namespace, settingsid, entity, view, null, null, null);
	}

	public List<Map> getFieldsForEntityView(String namespace, String settingsid, String entity, String view, Map mapping, String filter, String sortField) throws Exception {
		List<Map> retList = new ArrayList();
		String resourceid = format("settings/entities.{0}.views.{1}.fields", entity, view);
		String settingStr = getResource(namespace, settingsid, resourceid);
		StoreDesc sdesc = StoreDesc.getNamespaceData(namespace);
		if (settingStr != null) {
			Map m = (Map) m_ds.deserialize(settingStr);
			debug("m:" + m);
			Map<String, Map> allFields = _listToMap(m_ssi.m_entityService.getFields(sdesc, entity, true), "name");
			retList = _mergeProperties((List) m.get(FIELDS), allFields);
			printList("\tAftermerge:", retList);
		} else {
			if (!"duplicate-check".equals(view)) {
				retList = m_ssi.m_entityService.getFields(sdesc, entity, false);
			}
		}
		retList = m_ssi.m_permissionService.permissionFieldListFilter(sdesc, entity, retList, "name", "read");
		SessionContext sc = m_ssi.m_dataLayer.getSessionContext(sdesc);
		for (Map m : retList) {
			boolean rd = getBoolean(m,"readonly",false);
			boolean readonly = !sc.isFieldPermitted((String) m.get("name"), entity, "write");
			if( rd) readonly = true;
			debug("\tisReadOnly(" + m.get("name") + "," + entity + ")" + readonly);
			m.put("readonly", readonly);
		}
		if (sortField != null) {
			m_ssi.m_utilsService.sortListByField(retList, sortField);
		}
		if (mapping != null || filter != null) {
			retList = m_ssi.m_utilsService.listToList(retList, mapping, filter);
		}
		return retList;
	}

	public Map getPropertiesForEntityView(String namespace, String settingsid, String entity, String view) throws Exception {
		String resourceid = format("settings/entities.{0}.views.{1}.properties", entity, view);
		String settingStr = getResource(namespace, settingsid, resourceid);
		if (settingStr != null) {
			Map m = (Map) m_ds.deserialize(settingStr);
			return m;
		} else {
			return getPropertiesForEntity(namespace, settingsid, entity);
		}
	}

	public Map getPropertiesForEntity(String namespace, String settingsid, String entity) throws Exception {
		String resourceid = format("settings/entities.{0}.properties", entity);
		String settingStr = getResource(namespace, settingsid, resourceid);
		if (settingStr != null) {
			Map m = (Map) m_ds.deserialize(settingStr);
			return m;
		} else {
			return new HashMap();
		}
	}

	private List<Map> filterFieldSetFields(List<Map> fsList, String namespace,String entity){
		StoreDesc sdesc = StoreDesc.getNamespaceData(namespace);
		SessionContext sc = m_ssi.m_dataLayer.getSessionContext(sdesc);
		for(Map fs : fsList){
			List<String> fList = (List<String>)fs.get(FIELDS);
			List<String> nList = new ArrayList();
			fs.put(FIELDS,nList);
			for( String f : fList ){
				if(sc.isFieldPermitted(f, entity, "read")){
					nList.add(f);
				}
			}
		}
		return fsList;
	}

	private String getResource(String namespace, String settingsid, String resourceid) {
		debug("GetResource:" + resourceid+"/"+namespace+"/"+settingsid);
		String ret = null;
		try {
			StoreDesc sdesc = StoreDesc.getNamespaceData(namespace);
			String repo = sdesc.getRepository();
			if( repo == null) repo = sdesc.getNamespace();
			ret = m_gitService.getContent(repo, resourceid);
		} catch (Exception e) {
			try {
				StoreDesc sdesc = StoreDesc.getNamespaceMeta(namespace);
				String repo = sdesc.getRepository();
				if( repo == null) repo = sdesc.getNamespace();
				ret = m_gitService.getContent(repo, resourceid);
			} catch (Exception e1) {
				try {
					ret = m_gitService.getContent("global", resourceid);
				} catch (Exception e2) {}
			}
		}
		debug("GetResource.ret:" + ret);
		return ret;
	}

	private String getRepo(String namespace, boolean dataRepo){
		StoreDesc sdesc = dataRepo ? StoreDesc.getNamespaceData(namespace) : StoreDesc.getNamespaceMeta(namespace);
		String repo = sdesc.getRepository();
		if( repo == null){
			repo = sdesc.getNamespace();
		}
		return repo;
	}

	private String getRepo(String namespace){
		boolean isRuntime = m_ssi.m_isRuntimeSystem;		
		StoreDesc sdesc = isRuntime ? StoreDesc.getNamespaceData(namespace) : StoreDesc.getNamespaceMeta(namespace);
		String repo = sdesc.getRepository();
		if( repo == null){
			repo = sdesc.getNamespace();
		}
		return repo;
	}

	private void printList(String header, List list) {
		debug("----->" + header);
		if (list != null) {
			String komma = "";
			debug("\t");
			Iterator it = list.iterator();
			while (it.hasNext()) {
				Map obj = (Map) it.next();
				debug(komma + obj.get("name"));
				komma = ", ";
			}
		}
		debug("");
	}

	protected List<Map> _mergeProperties(List<Map> l, Map<String, Map> mapMerge) {
		List<Map> resList = new ArrayList<Map>();
		for (Map<String, Object> m : l) {
			if (getBoolean(m, "enabled", true) == false) {
				debug("Not enabled:" + m.get("name"));
				continue;
			}
			Map<String, Object> merge = mapMerge.get(m.get("name"));
			if (merge == null) {
				continue;
			}
			merge.putAll(m);
			resList.add(merge);
		}
		return resList;
	}

	protected boolean getBoolean(Map m, String key, boolean def) {
		try {
			Object val = m.get(key);
			if (val == null){
				return def;
			}
			if (val instanceof String) {
				if ("true".equals(val)){
					return true;
				}
				if ("false".equals(val)){
					return false;
				}
			}
			return (Boolean) val;
		} catch (Exception e) {
		}
		return def;
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
	private static final org.slf4j.Logger m_logger = org.slf4j.LoggerFactory.getLogger(GitMetaDataImpl.class);
}
