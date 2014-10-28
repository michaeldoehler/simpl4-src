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
package org.ms123.common.entity;

import flexjson.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.jdo.annotations.Element;
import javax.jdo.annotations.Key;
import javax.jdo.annotations.Value;
import javax.jdo.annotations.Persistent;
import java.lang.annotation.Annotation;
import org.apache.commons.beanutils.BeanMap;
import org.ms123.common.utils.annotations.RelatedTo;
import org.ms123.common.data.api.DataLayer;
import org.ms123.common.git.GitService;
import org.ms123.common.auth.api.AuthService;
import org.ms123.common.utils.UtilsService;
import org.ms123.common.data.api.SessionContext;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.nucleus.api.NucleusService;
import org.ms123.common.enumeration.EnumerationService;
import org.ms123.common.permission.api.PermissionService;
import org.ms123.common.libhelper.Inflector;
import org.ms123.common.utils.ParameterParser;
import org.ms123.common.utils.TypeUtils;
import org.mvel2.MVEL;
import flexjson.JSONDeserializer;

/**
 *
 */
@SuppressWarnings("unchecked")
class BaseEntityServiceImpl implements org.ms123.common.entity.api.Constants {

	protected Inflector m_inflector = Inflector.getInstance();

	private static final String ENTITY = "entity";

	private static final String FIELDS = "fields";

	private static final String FIELD = "field";

	protected DataLayer m_dataLayer;

	protected JSONDeserializer m_ds = new JSONDeserializer();

	protected AuthService m_authService;

	protected PermissionService m_permissionService;

	protected EnumerationService m_enumerationService;

	protected NucleusService m_nucleusService;

	protected GitService m_gitService;

	protected UtilsService m_utilsService;

	protected MetaData m_gitMetaData;

	public List getEntities(StoreDesc sdesc, Boolean withChilds, String mappingstr) throws Exception {
		Map mapping = null;
		if (mappingstr != null) {
			ParameterParser p = new ParameterParser();
			mapping = p.parse(mappingstr, ',', ':');
		}
		return getEntities(sdesc, withChilds, false, mapping, null, null);
	}

	public List getEntities(StoreDesc sdesc, Boolean withChilds, Boolean withTeam, Map mapping, String filter, String sortField) throws Exception {
		SessionContext sc = m_dataLayer.getSessionContext(sdesc);
		String namespace = sdesc.getNamespace();
		try {
			List<Map> modList = m_gitMetaData.getEntitytypes(sdesc.getStoreId());
			if( withTeam){
				Map team = new HashMap();
				team.put("enabled",true);
				team.put("index",200);
				team.put("default_fields",false);
				team.put("type","sw.entitytype");
				team.put("team_security",false);
				team.put("name","team");
				team.put("multi_add",false);
				team.put("multiple_tabs",false);
				team.put("exclusion_list",false);
				modList.add(team);
			}

			Map<String, Map> modMaps = toMap(modList, "name");
			List<Map> retList = new ArrayList();
			for (Map map : modList) {
				Object enabled = map.get("enabled");
				String mn = m_inflector.getEntityName(map.get("name"));
				String _pack = getString(map, StoreDesc.PACK, StoreDesc.PACK_DATA);
				if (enabled != null && sdesc.isSamePack(_pack) && ((Boolean) enabled) == true && m_permissionService.hasEntityPermissions(sdesc, mn, "read")) {
					map.put("write", m_permissionService.hasEntityPermissions(sdesc, mn, "write"));
					if (withChilds != null && withChilds) {
						Class clazz = null;
						try {
							clazz = sc.getClass(m_inflector.getClassName((String) map.get("name")));
						} catch (Exception e) {
							e.printStackTrace();
							continue;
						}
						List childList = new ArrayList();
						List<Object[]> childs = getSubEntityFromClass(sdesc, clazz);
						for (int i = 0; i < childs.size(); i++) {
							Object[] o = childs.get(i);
							Map childMap = new HashMap();
							childMap.put("name", o[1]);
							String className = ((Class) o[0]).getName();
System.out.println("className:"+className);
							int dot = className.lastIndexOf(".");
							String entityName = m_inflector.getEntityName(className.substring(dot + 1));
							if (!m_permissionService.hasEntityPermissions(sdesc, entityName, "read")) {
								continue;
							}
							childMap.put("write", m_permissionService.hasEntityPermissions(sdesc, entityName, "write"));
							childMap.put("modulename", entityName);
							Map modMap = modMaps.get(entityName);
							if (modMap != null) {
								childMap.put("index", modMap.get("index"));
								childMap.put("multi_add", modMap.get("multi_add"));
							}
							childMap.put("dependent", o[3]);
							if (o[2].equals(java.util.List.class) 
										|| o[2].equals(java.util.Set.class)
										|| o[2].equals(java.util.Map.class)
									) {
								childMap.put("datatype", "list/xxx");
							} else {
								childMap.put("datatype", "one/xxx");
							}
							childList.add(childMap);
						}
						if (childList.size() > 0) {
							if (sortField != null) {
								m_utilsService.sortListByField(childList, sortField);
							} else {
								sortListToIndex(childList);
							}
							if (mapping != null || filter != null) {
								childList = m_utilsService.listToList(childList, mapping, filter);
							}
						}
						map.put("childs", childList);
					}
					retList.add(map);
				}
			}
			if (sortField != null) {
				m_utilsService.sortListByField(retList, sortField);
			} else {
				sortListToIndex(retList);
			}
			if (mapping != null || filter != null) {
				retList = m_utilsService.listToList(retList, mapping, filter);
			}
			return retList;
		} finally {
			sc.handleFinally(null);
		}
	}

	protected List<Object[]> getSubEntityFromClass(StoreDesc sdesc, Class clazz) {
		List<Object[]> list = new ArrayList<Object[]>();
		if (clazz == null) {
			return list;
		}
		try {
			Object o = clazz.newInstance();
			BeanMap beanMap = new BeanMap(o);
			Iterator itv = beanMap.keyIterator();
			while (itv.hasNext()) {
				String prop = (String) itv.next();
				if ("class".equals(prop)) {
					continue;
				}
				boolean isRelatedTo = false;
				boolean isDependent = false;
				try {
					java.lang.reflect.Field field = o.getClass().getDeclaredField(prop);
					if (field != null) {
						isRelatedTo = field.isAnnotationPresent(RelatedTo.class);
						if (field.isAnnotationPresent(Element.class) || field.isAnnotationPresent(Persistent.class)) {
							isDependent = isDependent(field);
						}
					}
				} catch (Exception e) {
				}
				debug("getSubEntityFromClass:" + prop);
				if (!TypeUtils.isPrimitiveType(beanMap.getType(prop)) && !"teamintern".equals(prop)) {
					Class type = TypeUtils.getTypeForField(o, prop);
					if (type != null && !TypeUtils.isPrimitiveType(type)) {
						Object[] s = new Object[4];
						s[0] = type;
						s[1] = prop;
						s[2] = beanMap.getType(prop);
						s[3] = isDependent;
						list.add(s);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}

	public Map getEntityTree(StoreDesc sdesc, String mainEntity, int maxlevel, Boolean _pathid, String _type, Boolean _listResolved) throws Exception {
		SessionContext sc = m_dataLayer.getSessionContext(sdesc);
		try {
			Map userData = m_authService.getUserProperties(sc.getUserName());
			mainEntity = m_inflector.getClassName(mainEntity);
			boolean pathid = _pathid != null && _pathid;
			boolean listResolved = _listResolved != null && _listResolved;
			String type = _type != null ? _type : "all";
			Class mainClazz = sc.getClass(mainEntity);
			Object[] member = new Object[3];
			member[0] = mainClazz;
			member[1] = m_inflector.getEntityName(mainEntity).toLowerCase();
			member[2] = mainClazz;
			Map tree = _getEntitySubTree(sdesc, (String) member[1], member, 0, maxlevel, pathid, listResolved, type, userData);
			return tree;
		} catch (Exception e) {
			sc.handleException(e);
			throw e;
		} finally {
			sc.handleFinally();
		}
	}

	protected Map _getEntitySubTree(StoreDesc sdesc, String path, Object[] member, int level, int maxlevel, boolean pathid, boolean listResolved, String type, Map userData) {
		Class clazz = (Class) member[0];
		String entityName = m_inflector.getEntityName(clazz.getName().substring(clazz.getName().lastIndexOf(".") + 1));
		if (level == maxlevel) {
			return null;
		}
		if (!m_permissionService.hasEntityPermissions(sdesc, entityName, "read")) {
			return null;
		}
		Map node = new HashMap();
		node.put("level", level);
		List<Object[]> childs = getSubEntityFromClass(sdesc, clazz);
		Object t = member[2];
		boolean collection = (t.equals(java.util.List.class) || t.equals(java.util.Set.class) || t.equals(java.util.Map.class));
		if (type.equals("one") && collection) {
			return null;
		}
		if (type.equals("collection") && !collection && level > 0) {
			return null;
		}
		if (pathid) {
			node.put("id", path);
		} else {
			node.put("id", member[1]);
		}
		node.put("datatype", (collection || (level == 20 && listResolved)) ? "list" : "object");
		node.put(ENTITY, entityName);
		node.put("name", member[1]);
		node.put("write", m_permissionService.hasEntityPermissions(sdesc, entityName, "write"));
		node.put("title", ("data." + m_inflector.getEntityName(member[1])).toLowerCase());
		Map objNode = null;
		if ((collection || level == 20) && listResolved) {
			objNode = getNodeWithSingularNames(node, entityName, path, pathid);
		}
		for (int i = 0; i < childs.size(); i++) {
			Object[] cmem = childs.get(i);
			Map m = _getEntitySubTree(sdesc, path + "/" + cmem[1], cmem, level + 1, maxlevel, pathid, listResolved, type, userData);
			if (m != null) {
				List<Map> childList = (objNode != null) ? (List) objNode.get("children") : (List) node.get("children");
				if (childList == null) {
					childList = new ArrayList<Map>();
					if (objNode != null) {
						objNode.put("children", childList);
					} else {
						node.put("children", childList);
					}
				}
				childList.add(m);
			}
		}
		return node;
	}

	public Map getPermittedFields(StoreDesc sdesc, String entityName) {
		return getPermittedFields(sdesc, entityName, "read");
	}

	public Map getPermittedFields(StoreDesc sdesc, String entityName, String actions) {
		Map ret = null;
		try {
			ret = new HashMap();
			List<Map> allFields = getFields(sdesc, entityName, true, true);
			allFields = m_permissionService.permissionFieldListFilter(sdesc, entityName, allFields, "name", actions);
			ret = toMap(allFields, "name");
			ret.put("_selListMap", getSelectionMap(sdesc.getNamespace(), allFields));
		} catch (Exception e) {
			throw new RuntimeException("BaseEntityServiceImpl._getPermittedFields", e);
		} finally {
		}
		boolean b = m_permissionService.hasRole("admin");
		printMaps("getPermittedFields(" + entityName + "," + actions + "):roleAdmin:" + b, ret);
		return ret;
	}


	private Map getSelectionMap(String namespace, List<Map> allFields){
		Map selListMap = new HashMap();
		for (Map f : allFields) {
			Object si = f.get("selectable_items");
			if (si == null || !(si instanceof String)){
				continue;
			}
			try {
				Map _enum = getEnumeration(namespace, (String) si);
				if (_enum != null) {
					selListMap.put(f.get("name") + "_enum", _enum);
				}
			} catch (Exception e) {
				debug("BaseEntityServiceImpl.getSelectionMap:Cannot conv:" + f.get("name") + "/" + si + "/" + e);
			}
		}
		return selListMap;
	}
	private Map getEnumeration(String namespace, String si) {
		Map selItem = (Map) m_ds.deserialize((String) si);
		String enumDescription = (String) selItem.get("enumDescription");
		List<Map> items = (List) selItem.get("items");
		debug("getEnumeration:"+enumDescription);
		debug("items:"+items);
		if( items==null || enumDescription.startsWith("sw.filter")){
			return new HashMap();
		}
		try {
			Map mapping = new HashMap();
			for (Map<String, String> item : items) {
				String m = item.get("mapping");
				String c = item.get("colname");
				if (m == null)
					m = c;
				mapping.put(m, c);
			}
			List<Map> enumList = m_enumerationService.get(namespace, enumDescription.substring(8), mapping, null);
			return _list2map(enumList);
		} catch (Exception e2) {
			e2.printStackTrace();
		}
		return null;
	}

	private Map _list2map(List<Map> list) {
		Map ret = new HashMap();
		if (list.size() == 0) {
			return ret;
		}
		for (int i = 0; i < list.size(); i++) {
			Map o = list.get(i);
			if( o.get("value") == null){
				ret.put("", o);
			}else{
				ret.put(o.get("value"), o);
			}
		}
		return ret;
	}

	public List<Map> getRelations(StoreDesc sdesc) throws Exception {
		try {
			List<Map> relList = m_gitMetaData.getRelations(sdesc.getStoreId());
			return relList;
		} catch (Exception e) {
			throw e;
		} finally {
		}
	}

	public String getIdField(StoreDesc sdesc, String entityName) throws Exception {
		try {
			List<Map> metaFields = m_gitMetaData.getFields(sdesc.getStoreId(), entityName);
			if (metaFields == null) {
				return "id";
			}
			for (Map f : metaFields) {
				Object o = f.get("primary_key");
				debug("getIdField:" + f);
				boolean pk = (o != null && o instanceof Boolean) ? (Boolean) o : false;
				if (pk)
					return (String) f.get("name");
			}
			return "id";
		} catch (Exception e) {
			throw e;
		} finally {
		}
	}

	public List<Map> getFields(StoreDesc sdesc, String entityName, Boolean withAutoGen) throws Exception {
		return getFields(sdesc, entityName, withAutoGen, false);
	}

	public List<Map> getFields(StoreDesc sdesc, String entityName, Boolean withAutoGen, Boolean withAllRelations) throws Exception {
		try {
			List<Map> metaFields = m_gitMetaData.getFields(sdesc.getStoreId(), entityName);
			if (metaFields == null) {
				return new ArrayList();
			}
			sortListToName(metaFields);
			printList("Plain:", metaFields);
			if (withAutoGen) {
				List<Map> autoGenList = getAutoGenFields(sdesc, metaFields, entityName);
				printList("Autogen:", autoGenList);
				metaFields.addAll(autoGenList);
			}
			List relList = getFieldsFromRelations(sdesc, entityName, withAllRelations);
			if (relList.size() > 0) {
				printList("Rellist:", relList);
				metaFields.addAll(relList);
			}
			printList("-> entity.getFields:", metaFields);
			return metaFields;
		} catch (Exception e) {
			throw e;
		} finally {
		}
	}

	private List<Map> getAutoGenFields(StoreDesc sdesc, List<Map> metaFields, String entityName) throws Exception {
		String namespace = sdesc.getNamespace();
		debug("getAutoGenFields:" + sdesc + "/" + entityName);
		Map<String, Map> fieldMap = toMap(metaFields, "name");
		String clazz = m_inflector.getClassName(entityName);
		Class c = null;
		try {
			c = m_nucleusService.getClass(sdesc, clazz);
		} catch (Exception e) {
		}
		debug("c:" + c);
		List<Map> retList = new ArrayList();
		if (c == null) {
			return retList;
		}
		BeanMap beanMap = new BeanMap(c.newInstance());
		Iterator itv = beanMap.keyIterator();
		while (itv.hasNext()) {
			String prop = (String) itv.next();
			if ("class".equals(prop)) {
				continue;
			}
			Map<String, Object> map = null;
			if (fieldMap.get(prop) != null) {
				continue;
			} else {
				// Autogen field  
				map = getDefaultField(prop);
			}
			if (map == null) {
				continue;
			}
			retList.add(map);
		}
		return retList;
	}

	private Map getDefaultField(String prop) {
		for (Map m : m_defaultFields) {
			if (m.get("id").equals(prop)) {
				return new HashMap(m);
			}
		}
		for (Map m : m_stateFields) {
			if (m.get("internal") == null && m.get("id").equals(prop)) {
				return new HashMap(m);
			}
		}
		return null;
	}

	private String getBaseName(String name) {
		if (name == null || name.trim().equals(""))
			return null;
		int lindex = name.lastIndexOf(".");
		if (lindex == -1)
			return name;
		return name.substring(lindex + 1).toLowerCase();
	}

	private List<Map> getFieldsFromRelations(StoreDesc sdesc, String entityName, boolean all) throws Exception {
		debug("--->getFieldsFromRelations:" + sdesc + "/" + entityName);
		List<Map> retList = new ArrayList();
		SessionContext sc = m_dataLayer.getSessionContext(sdesc);
		String namespace = sdesc.getNamespace();
		String pack = sdesc.getPack();
		String className = null;
		if (StoreDesc.isAidPack(pack)) {
			className = pack + "." + m_inflector.getClassName(entityName);
		} else {
			className = namespace + "." + pack + "." + m_inflector.getClassName(entityName);
		}
		List<Map> relList = m_gitMetaData.getRelations(sdesc.getStoreId());
		if (relList == null) {
			return retList;
		}
		Iterator<Map> it = relList.iterator();
		String simpleClassName = m_inflector.getClassName(entityName);
		while (it.hasNext()) {
			Map relMap = it.next();
			String rel = (String) relMap.get(RELATION);
			if ("one-to-many-bi".equals(rel)) {
				String rightClass = sdesc.insertJavaPackage((String) relMap.get(RIGHT_ENTITY));
				String leftClass = (String) relMap.get(LEFT_ENTITY);
				String id = m_inflector.singularize(removePackName((String) relMap.get(LEFT_ENTITY))).toLowerCase();
				if (className.equalsIgnoreCase(rightClass)) {
					Map field = new HashMap();
					String fieldName = !isEmpty((String)relMap.get(RIGHT_FIELD)) ? (String)relMap.get(RIGHT_FIELD) : removePackName(leftClass.toLowerCase());
					field.put("id", fieldName);
					field.put("name", fieldName);
					field.put("datatype", "relatedto/" + sdesc.insertJavaPackage(leftClass) + "/" + fieldName);
					retList.add(field);
					continue;
				}
			}
			if (all) {
				Map f = __getField(entityName, relMap, sdesc);
				if (f != null) {
					retList.add(f);
				}
			}
		}
		return retList;
	}

	private Map __getField(String entity, Map rel, StoreDesc sdesc) {
		String relation = (String) rel.get(RELATION);
		String leftmodule = sdesc.insertJavaPackage((String) rel.get(LEFT_ENTITY));
		String leftfield = (String) rel.get(LEFT_FIELD);
		String rightmodule = sdesc.insertJavaPackage((String) rel.get(RIGHT_ENTITY));
		String rightfield = (String) rel.get(RIGHT_FIELD);
		boolean manyToMany = "many-to-many".equals(relation);
		boolean oneToMany = "one-to-many".equals(relation) || "one-to-many-map".equals(relation);
		boolean oneToManyBi = "one-to-many-bi".equals(relation);
		boolean oneToOne = "one-to-one".equals(relation);
		boolean oneToOneBi = "one-to-one-bi".equals(relation);
		if (leftfield == null || "".equals(leftfield)) {
			if (manyToMany) {
				leftfield = m_inflector.pluralize(rightmodule).toLowerCase();
			}
			if (oneToMany || oneToManyBi || oneToOne || oneToOneBi) {
				leftfield = m_inflector.pluralize(rightmodule).toLowerCase();
			}
		}
		if (oneToOne || oneToOneBi) {
			leftfield = m_inflector.singularize(leftfield).toLowerCase();
		}
		if (rightfield == null || "".equals(rightfield)) {
			if (manyToMany) {
				rightfield = m_inflector.pluralize(leftmodule).toLowerCase();
			}
			if (oneToMany || oneToOne) {
			}
			if (oneToManyBi || oneToOneBi) {
				rightfield = m_inflector.singularize(leftmodule).toLowerCase();
			}
		}
		rightfield = removePackName(rightfield);
		leftfield = removePackName(leftfield);
		String datatype = null;
		if (oneToManyBi || manyToMany || oneToOneBi) {
			datatype = "set";
			if (!entity.equals(getBaseName(leftmodule))) {
				String tmp = rightfield;
				rightfield = leftfield;
				leftfield = tmp;
				tmp = rightmodule;
				rightmodule = leftmodule;
				leftmodule = tmp;
				if (oneToManyBi || oneToOneBi) {
					datatype = "object";
				}
			}
		} else {
			datatype = "object";
		}
		if (!entity.equals(getBaseName(leftmodule))) {
			debug("\tNOREL.entity:" + entity + "/" + getBaseName(leftmodule));
			return null;
		}
		debug("__getField(" + entity + "):lm:" + leftmodule + "/rm:" + rightmodule + "/lf:" + leftfield + "/rf:" + rightfield + "/datatype:" + datatype);
		Map field = new HashMap();
		field.put("id", leftfield);
		field.put("name", leftfield);
		field.put("datatype", datatype);
		return field;
	}

	private String removePackName(String s) {
		if (s == null)
			return null;
		int index = s.lastIndexOf('.');
		if (index == -1)
			return s;
		return s.substring(index + 1);
	}

	public List<Map> getDefaultFields() {
		List<Map> ret = new ArrayList();
		for (Map m : m_defaultFields) {
			ret.add(new HashMap(m));
		}
		return ret;
	}
	public List<Map> getTeamFields() {
		List<Map> ret = new ArrayList();
		for (Map m : m_teamFields) {
			ret.add(new HashMap(m));
		}
		return ret;
	}
	public List<Map> getStateFields() {
		List<Map> ret = new ArrayList();
		for (Map m : m_stateFields) {
			ret.add(new HashMap(m));
		}
		return ret;
	}

	private String getRelatedTo(Class c, String prop) {
		try {
			java.lang.reflect.Field field = c.getDeclaredField(prop);
			if (field != null) {
				Annotation ann = field.getAnnotation(RelatedTo.class);
				if (ann == null) {
					return null;
				}
				Class atype = ann.annotationType();
				Method meth = atype.getDeclaredMethod("value");
				String val = (String) meth.invoke(ann, new Object[0]);
				return val;
			}
		} catch (Exception e) {
			debug("getRelatedTo:" + e);
		}
		return null;
	}

	private Map getNodeWithSingularNames(Map node, String entityName, String path, boolean pathid) {
		Map objNode = new HashMap();
		objNode.put("datatype", "object");
		objNode.put(ENTITY, m_inflector.singularize(entityName));
		objNode.put("name", m_inflector.singularize(entityName));
		if (pathid) {
			path = path + "/" + m_inflector.singularize(entityName);
			objNode.put("id", path);
		} else {
			objNode.put("id", m_inflector.singularize(entityName));
		}
		objNode.put("title", ("data." + m_inflector.singularize(entityName).toLowerCase()));
		List<Map> childList = new ArrayList<Map>();
		childList.add(objNode);
		node.put("children", childList);
		return objNode;
	}

	private void sortListToIndex(List list) {
		Collections.sort(list, new ListSortByIndex());
	}

	private void sortListToName(List list) {
		Collections.sort(list, new ListSortByName());
	}

	private class ListSortByIndex implements Comparator<Map> {

		public int compare(Map o1, Map o2) {
			if (o1.get("index") == null || o2.get("index") == null) {
				return 0;
			}
			int index1 = getIntFromObject(o1.get("index"));
			int index2 = getIntFromObject(o2.get("index"));
			return (int) (index1 - index2);
		}
	}

	private class ListSortByName implements Comparator<Map> {

		public int compare(Map o1, Map o2) {
			if (o1.get("name") == null || o2.get("name") == null) {
				return 0;
			}
			String s1 = (String) o1.get("name");
			String s2 = (String) o2.get("name");
			return s1.compareTo(s2);
		}
	}

	private int getIntFromObject(Object object) {
		if (object instanceof Long) {
			return ((Long) object).intValue();
		}
		if (object instanceof Integer) {
			return ((Integer) object).intValue();
		}
		return -1;
	}

	private boolean isDependent(Field field) throws Exception {
		try {
			java.lang.annotation.Annotation ann = field.getAnnotation(Element.class);
			if (ann == null) {
				ann = field.getAnnotation(Persistent.class);
				if (ann == null) {
					return false;
				}
			}
			Class atype = ann.annotationType();
			Method methDependent = atype.getDeclaredMethod("dependent");
			if (methDependent != null) {
				String dep = (String) methDependent.invoke(ann, new Object[0]);
				if (dep != null && "true".equals(dep)) {
					return true;
				}
			}
			return false;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	private void applyMapping(List<Map> list, Map<String, String> mapping) {
		for (Map<String, Object> rec : list) {
			Iterator<String> it = mapping.keySet().iterator();
			rec.put("inflector", m_inflector);
			while (it.hasNext()) {
				String key = it.next();
				String val = mapping.get(key);
				try {
					//debug("eval:"+val);  
					val = MVEL.evalToString(val, rec);
				} catch (Exception e) {
					debug("eval.error:" + e);
				}
				rec.put(key, val);
			}
			rec.remove("inflector");
		}
	}

	private Map getMapping(String mappingstr) {
		Map mapping = null;
		if (mappingstr != null) {
			ParameterParser p = new ParameterParser();
			mapping = p.parse(mappingstr, ',', ':');
		}
		return mapping;
	}

	private void printList(String header, List<Map> list) {
		if( true) return ;
		debug("----->" + header);
		if (list != null) {
			String komma = "";
			debug("\t");
			Iterator it = list.iterator();
			while (it.hasNext()) {
				Map map = (Map) it.next();
				debug(komma + map.get("name"));
				komma = ", ";
			}
		}
		debug("");
	}

	private void printMaps(String header, Map map) {
		if( true) return ;
		debug("----->" + header);
		if (map != null) {
			String komma = "";
			debug("\t");
			Iterator it = map.keySet().iterator();
			while (it.hasNext()) {
				String key = (String) it.next();
				debug(komma + key);
				komma = ", ";
			}
		}
		debug("");
	}

	private void printMap(String header, Map map) {
		if( true) return ;
		debug("----->" + header);
		if (map != null) {
			String komma = "";
			debug("\t");
			Iterator it = map.keySet().iterator();
			while (it.hasNext()) {
				String key = (String) it.next();
				Object val = map.get(key);
				debug(komma + key + "=" + val);
				komma = ", ";
			}
		}
		debug("");
	}

	private <T extends Map> Map<String, T> toMap(List<T> list, String key) {
		Map<String, T> retMap = new HashMap();
		if (list == null) {
			return retMap;
		}
		for (T m : list) {
			retMap.put((String) m.get(key), m);
		}
		return retMap;
	}

	private boolean isEmpty(String s){
		if( s == null|| s.trim().length()==0) return true;
		return false;
	}

	private String getString(Map m, String key, String _def) {
		try {
			if (m.get(key) != null) {
				return (String) m.get(key);
			}
		} catch (Exception e) {
		}
		return _def;
	}

	protected void debug(String msg) {
		//System.out.println(msg);
		m_logger.debug(msg);
	}
	protected void info(String msg) {
		System.out.println(msg);
		m_logger.info(msg);
	}
	private static final org.slf4j.Logger m_logger = org.slf4j.LoggerFactory.getLogger(BaseEntityServiceImpl.class);
}
