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
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.jdo.PersistenceManager;
import javax.jdo.spi.PersistenceCapable;
import org.apache.commons.beanutils.BeanMap;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.fileupload.FileItem;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.Tika;
import org.joda.time.DateTime;
import org.ms123.common.data.api.SessionContext;
import org.ms123.common.data.scripting.MVELEvaluator;
import org.ms123.common.libhelper.Inflector;
import org.ms123.common.permission.api.PermissionService;
import org.ms123.common.team.api.TeamService;
import org.ms123.common.utils.IOUtils;
import org.ms123.common.utils.TypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.ms123.common.entity.api.Constants.STATE_FIELD;
import org.ms123.common.libhelper.Base64;
import groovy.lang.*;
import org.codehaus.groovy.control.*;
import javax.transaction.UserTransaction;
import javax.jdo.Extent;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import java.lang.reflect.Field;

@SuppressWarnings("unchecked")
public class MultiOperations {

	protected static Inflector m_inflector = Inflector.getInstance();

	protected static JSONSerializer m_js = new JSONSerializer();

	private static String FIELDNAME_REGEX = "[a-zA-Z0-9_]{2,64}";

	public static List<Object> persistObjects(SessionContext sc, Object obj, Map<String, Object> persistenceSpecification, int max) {
		List<Object> retList = new ArrayList();
		UserTransaction ut = sc.getUserTransaction();
		String mainEntity = null;
		Collection<Object> resultList = null;
		if (obj instanceof Collection) {
			resultList = (Collection) obj;
		} else {
			resultList = new ArrayList();
			resultList.add(obj);
		}
		debug("persistObjects:" + resultList + ",persistenceSpecification:" + persistenceSpecification);
		String parentFieldName = null;
		Class parentClazz = null;
		String parentQuery = null;
		String updateQuery = null;
		Boolean noUpdate = false;
		PersistenceManager pm = sc.getPM();
		GroovyShell groovyShell = new GroovyShell(MultiOperations.class.getClassLoader(), new Binding(), new CompilerConfiguration());
		if (persistenceSpecification != null) {
			String parentLookup = (String)persistenceSpecification.get("lookupRelationObjectExpr");
			String relation = (String)persistenceSpecification.get("relation");
			if (!Utils.isEmpty(parentLookup) && !Utils.isEmpty(relation)) {
				String s[] = relation.split(",");
				parentClazz = sc.getClass(Utils.getBaseName(s[0]));
				parentFieldName = s[1];
				if (parentLookup.matches(FIELDNAME_REGEX)) {
					String q = isString(parentClazz, parentLookup) ? "'" : "";
					parentQuery = parentLookup + " == " + q + "${" + parentLookup + "}" + q;
				} else if (parentLookup.matches(FIELDNAME_REGEX + "," + FIELDNAME_REGEX)) {
					s = parentLookup.split(",");
					String q = isString(parentClazz, s[1]) ? "'" : "";
					parentQuery = s[0] + " == " + q + "${" + s[1] + "}" + q;
				} else {
					parentQuery = parentLookup;
				}
			}
			String updateLookup = (String)persistenceSpecification.get("lookupUpdateObjectExpr");
			Class mainClass = null;
			if (resultList.size() > 0) {
				mainClass = resultList.iterator().next().getClass();
			}
			if (!Utils.isEmpty(updateLookup) && mainClass != null) {
				if (updateLookup.matches(FIELDNAME_REGEX)) {
					String q = isString(mainClass, updateLookup) ? "'" : "";
					updateQuery = updateLookup + " == " + q + "${" + updateLookup + "}" + q;
				} else {
					updateQuery = updateLookup;
				}
				noUpdate = (Boolean)persistenceSpecification.get("noUpdate");
				if( noUpdate == null) noUpdate=false;
			}
		}
		debug("noUpdate:"+noUpdate);
		try {
			int num = 0;
			if (resultList.size() > 0) {
				Class clazz = resultList.iterator().next().getClass();
				mainEntity = m_inflector.getEntityName(clazz.getSimpleName());
				String pk = TypeUtils.getPrimaryKey(clazz);
				sc.setPrimaryKey(pk);
			}
			for (Object object : resultList) {
				if (max != -1 && num >= max) {
					break;
				}
				Map m = SojoObjectFilter.getObjectGraph(object, sc, 2);
				//retList.add(m);
				//ut.begin();
				Object origObject = null;
				boolean isNew = true;
				if (updateQuery != null) {
					origObject = getObjectByFilter(groovyShell, pm, object.getClass(), object, updateQuery);
					debug("origObject:" + origObject);
					if (origObject != null) {
						if(noUpdate){
							 continue;
						}
						populate(sc, m, origObject, null);
						object = origObject;
						isNew = false;
					}
				}
				if (origObject == null && parentClazz != null) {
					Object parentObject = getObjectByFilter(groovyShell, pm, parentClazz, object, parentQuery);
					origObject = getOrigObjectFromParent(mainEntity,parentObject,parentFieldName);
					if(origObject == null || !origObject.getClass().equals( object.getClass())){
						sc.getDataLayer().insertIntoMaster(sc, object, mainEntity, parentObject, parentFieldName);
					}else{
						populate(sc, m, origObject, null);
						object = origObject;
						isNew = false;
					}
				}
				evaluteFormulas(sc, mainEntity, object, "in", isNew);
				//Muss eigentlich über alle Object gehen
				sc.getDataLayer().makePersistent(sc, object);
				retList.add(object);
				debug("\tpersist:" + m_js.serialize(object));
				//ut.commit();
				num++;
			}
		} catch (Throwable e) {
			e.printStackTrace();
			sc.handleException(ut, e);
		} finally {
			sc.handleFinally(ut);
		}
		return retList;
	}

	private static Object getObjectByFilter(GroovyShell shell, PersistenceManager pm, Class clazz, Object child, String queryString) throws Exception {
		String filter = expandString(shell, queryString, new BeanMap(child));
		debug("getObjectByFilter:" + filter+"/clazz:"+clazz);
		Extent e = pm.getExtent(clazz, true);
		Query q = pm.newQuery(e, filter);
		try {
			Collection coll = (Collection) q.execute();
			Iterator iter = coll.iterator();
			if (iter.hasNext()) {
				Object obj = iter.next();
				debug("getObjectByFilter:found");
				return obj;
			}
		} finally {
			q.closeAll();
		}
		debug("getObjectByFilter:not found");
		return null;
	}

	public static Object getOrigObjectFromParent(String entityName, Object objectMaster, String fieldName) throws Exception {
		debug("getOrigObjectFromParent:"+objectMaster+"/"+entityName+"/"+fieldName);
		if( objectMaster==null) return null;
		String propertyName = fieldName;
		if (fieldName.equals(entityName)) {
			propertyName = m_inflector.pluralize(entityName).toLowerCase();
		}
		Class clazz = PropertyUtils.getPropertyType(objectMaster, propertyName);
		if( clazz == null){
			clazz = PropertyUtils.getPropertyType(objectMaster, fieldName);
		}
		if (clazz != null) {
			if (clazz.equals(java.util.List.class) || clazz.equals(java.util.Set.class)) {
				return null;
			} else {
				if (fieldName.equals(entityName)) {
					propertyName = m_inflector.singularize(entityName).toLowerCase();
				}
				return PropertyUtils.getProperty(objectMaster, propertyName);
			}
		}
		return null;
	}
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////// 
	//populate 
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////// 
	public static void populate(SessionContext sessionContext, Map sourceMap, Object destinationObj, Map hintsMap) {
		PersistenceManager pm = sessionContext.getPM();
		if (hintsMap == null) {
			hintsMap = new HashMap();
		}
		BeanMap destinationMap = new BeanMap(destinationObj);
		String entityName = m_inflector.getEntityName(destinationObj.getClass().getSimpleName());
		debug("populate.sourceMap:" + sourceMap + ",destinationObj:" + destinationObj + ",destinationMap:" + destinationMap + "/hintsMap:" + hintsMap + "/entityName:" + entityName);
		if (sourceMap == null) {
			return;
		}
		debug("populate(" + entityName + ") is a persistObject:" + javax.jdo.JDOHelper.isPersistent(destinationObj) + "/" + javax.jdo.JDOHelper.isNew(destinationObj));
		if (sourceMap.get("id") != null) {
			debug("populate(" + entityName + ") has id:" + sourceMap.get("id"));
			return;
		}
		Map permittedFields = sessionContext.getPermittedFields(entityName, "write");
		Iterator<String> it = sourceMap.keySet().iterator();
		while (it.hasNext()) {
			String propertyName = it.next();
			boolean permitted = sessionContext.getPermissionService().hasAdminRole() || "team".equals(entityName) || sessionContext.isFieldPermitted(propertyName, entityName, "write");
			if (!propertyName.startsWith("_") && !permitted) {
				debug("---->populate:field(" + propertyName + ") no write permission");
				continue;
			} else {
				debug("++++>populate:field(" + propertyName + ") write permitted");
			}
			String datatype = null;
			if (!propertyName.startsWith("_")) {
				Map config = (Map) permittedFields.get(propertyName);
				if (config != null) {
					datatype = (String) config.get("datatype");
				}
			}
			if (propertyName.equals(STATE_FIELD) && !sessionContext.getPermissionService().hasAdminRole()) {
				continue;
			}
			String mode = null;
			Map hm = (Map) hintsMap.get(propertyName);
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
			Class propertyClass = destinationMap.getType(propertyName);
			debug("\ttype:" + propertyClass + "(" + propertyName + "=" + sourceMap.get(propertyName) + ")");
			if ("_ignore_".equals(sourceMap.get(propertyName))) {
				continue;
			}
			if (propertyClass == null) {
				debug("\t--- Warning property not found:" + propertyName);
			} else if (propertyClass.equals(java.util.Date.class)) {
				String value = Utils.getString(sourceMap.get(propertyName), destinationMap.get(propertyName), mode);
				debug("\tDate found:" + propertyName + "=>" + value);
				Date date = null;
				if (value != null) {
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
				destinationMap.put(propertyName, date);
			} else if (propertyClass.equals(java.util.Map.class)) {
				info("!!!!!!!!!!!!!!!!!!!Map not implemented");
			} else if (propertyClass.equals(java.util.List.class) || propertyClass.equals(java.util.Set.class)) {
				boolean isList = propertyClass.equals(java.util.List.class);
				boolean isSimple = false;
				if (datatype != null && datatype.startsWith("list_")) {
					isSimple = true;
				}
				try {
					Class propertyType = TypeUtils.getTypeForField(destinationObj, propertyName);
					debug("propertyType:" + propertyType + " fill with: " + sourceMap.get(propertyName) + ",list:" + destinationMap.get(propertyName) + "/mode:" + mode);
					Collection sourceList = isList ? new ArrayList() : new HashSet();
					if (sourceMap.get(propertyName) instanceof Collection) {
						sourceList = (Collection) sourceMap.get(propertyName);
					}
					if (sourceList == null) {
						sourceList = isList ? new ArrayList() : new HashSet();
					}
					Collection destinationList = (Collection) PropertyUtils.getProperty(destinationObj, propertyName);
					debug("destinationList:" + destinationList);
					debug("sourceList:" + sourceList);
					if (destinationList == null) {
						destinationList = isList ? new ArrayList() : new HashSet();
						PropertyUtils.setProperty(destinationObj, propertyName, destinationList);
					}
					if ("replace".equals(mode)) {
						boolean isEqual = false;
						if (isSimple) {
							isEqual = Utils.isCollectionEqual(destinationList, sourceList);
							if (!isEqual) {
								destinationList.clear();
							}
							debug("\tisEqual:" + isEqual);
						} else {
							List deleteList = new ArrayList();
							String namespace = sessionContext.getStoreDesc().getNamespace();
							for (Object o : destinationList) {
								if (propertyType.getName().endsWith(".Team")) {
									int status = sessionContext.getTeamService().getTeamStatus(namespace, new BeanMap(o), null, sessionContext.getUserName());
									debug("populate.replace.teamStatus:" + status + "/" + new HashMap(new BeanMap(o)));
									if (status != -1) {
										pm.deletePersistent(o);
										deleteList.add(o);
									}
								} else {
									pm.deletePersistent(o);
									deleteList.add(o);
								}
							}
							for (Object o : deleteList) {
								destinationList.remove(o);
							}
						}
						debug("populate.replace.destinationList:" + destinationList + "/" + propertyType.getName());
						if (isSimple) {
							if (!isEqual) {
								for (Object o : sourceList) {
									destinationList.add(o);
								}
							}
						} else {
							for (Object o : sourceList) {
								Map childSourceMap = (Map) o;
								Object childDestinationObj = propertyType.newInstance();
								if (propertyType.getName().endsWith(".Team")) {
									childSourceMap.remove("id");
									Object desc = childSourceMap.get("description");
									Object name = childSourceMap.get("name");
									Object dis = childSourceMap.get("disabled");
									String teamid = (String) childSourceMap.get("teamid");
									Object ti = Utils.getTeamintern(sessionContext, teamid);
									if (desc == null) {
										childSourceMap.put("description", PropertyUtils.getProperty(ti, "description"));
									}
									if (name == null) {
										childSourceMap.put("name", PropertyUtils.getProperty(ti, "name"));
									}
									if (dis == null) {
										childSourceMap.put("disabled", false);
									}
									pm.makePersistent(childDestinationObj);
									populate(sessionContext, childSourceMap, childDestinationObj, null);
									PropertyUtils.setProperty(childDestinationObj, "teamintern", ti);
								} else {
									pm.makePersistent(childDestinationObj);
									populate(sessionContext, childSourceMap, childDestinationObj, null);
								}
								debug("populated.add:" + new HashMap(new BeanMap(childDestinationObj)));
								destinationList.add(childDestinationObj);
							}
						}
					} else if ("remove".equals(mode)) {
						if (isSimple) {
							for (Object o : sourceList) {
								if (destinationList.contains(o)) {
									destinationList.remove(o);
								}
							}
						} else {
							for (Object ol : sourceList) {
								Map childSourceMap = (Map) ol;
								Object o = Utils.listContainsId(destinationList, childSourceMap, "teamid");
								if (o != null) {
									destinationList.remove(o);
									pm.deletePersistent(o);
								}
							}
						}
					} else if ("add".equals(mode)) {
						if (isSimple) {
							for (Object o : sourceList) {
								destinationList.add(o);
							}
						} else {
							for (Object ol : sourceList) {
								Map childSourceMap = (Map) ol;
								Object childDestinationObj = Utils.listContainsId(destinationList, childSourceMap, "teamid");
								if (childDestinationObj != null) {
									populate(sessionContext, childSourceMap, childDestinationObj, null);
								} else {
									childDestinationObj = propertyType.newInstance();
									if (propertyType.getName().endsWith(".Team")) {
										Object desc = childSourceMap.get("description");
										Object name = childSourceMap.get("name");
										Object dis = childSourceMap.get("disabled");
										String teamid = (String) childSourceMap.get("teamid");
										Object ti = Utils.getTeamintern(sessionContext, teamid);
										if (desc == null) {
											childSourceMap.put("description", PropertyUtils.getProperty(ti, "description"));
										}
										if (name == null) {
											childSourceMap.put("name", PropertyUtils.getProperty(ti, "name"));
										}
										if (dis == null) {
											childSourceMap.put("disabled", false);
										}
										pm.makePersistent(childDestinationObj);
										populate(sessionContext, childSourceMap, childDestinationObj, null);
										PropertyUtils.setProperty(childDestinationObj, "teamintern", ti);
									} else {
										pm.makePersistent(childDestinationObj);
										populate(sessionContext, childSourceMap, childDestinationObj, null);
									}
									destinationList.add(childDestinationObj);
								}
							}
						}
					} else if ("assign".equals(mode)) {
						if (!isSimple) {
							for (Object ol : sourceList) {
								Map childSourceMap = (Map) ol;
								Object childDestinationObj = Utils.listContainsId(destinationList, childSourceMap);
								if (childDestinationObj != null) {
									debug("id:" + childSourceMap + " already assigned");
								} else {
									Object id = childSourceMap.get("id");
									Boolean assign = Utils.getBoolean(childSourceMap.get("assign"));
									Object obj = pm.getObjectById(propertyType, id);
									if (assign) {
										destinationList.add(obj);
									} else {
										destinationList.remove(obj);
									}
								}
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					debug("populate.list.failed:" + propertyName + "=>" + sourceMap.get(propertyName) + ";" + e);
				}
			} else if (propertyClass.equals(java.lang.Boolean.class)) {
				try {
					destinationMap.put(propertyName, ConvertUtils.convert(sourceMap.get(propertyName), Boolean.class));
				} catch (Exception e) {
					debug("populate.boolean.failed:" + propertyName + "=>" + sourceMap.get(propertyName) + ";" + e);
				}
			} else if (propertyClass.equals(java.lang.Double.class)) {
				String value = Utils.getString(sourceMap.get(propertyName), destinationMap.get(propertyName), mode);
				try {
					destinationMap.put(propertyName, Double.valueOf(value));
				} catch (Exception e) {
					debug("populate.double.failed:" + propertyName + "=>" + value + ";" + e);
				}
			} else if (propertyClass.equals(java.lang.Long.class)) {
				try {
					destinationMap.put(propertyName, ConvertUtils.convert(sourceMap.get(propertyName), Long.class));
				} catch (Exception e) {
					debug("populate.long.failed:" + propertyName + "=>" + sourceMap.get(propertyName) + ";" + e);
				}
			} else if (propertyClass.equals(java.lang.Integer.class)) {
				debug("Integer:" + ConvertUtils.convert(sourceMap.get(propertyName), Integer.class));
				try {
					destinationMap.put(propertyName, ConvertUtils.convert(sourceMap.get(propertyName), Integer.class));
				} catch (Exception e) {
					debug("populate.integer.failed:" + propertyName + "=>" + sourceMap.get(propertyName) + ";" + e);
				}
			} else if ("binary".equals(datatype) || propertyClass.equals(byte[].class)) {
				InputStream is = null;
				InputStream is2 = null;
				try {
					if (sourceMap.get(propertyName) instanceof FileItem) {
						FileItem fi = (FileItem) sourceMap.get(propertyName);
						String name = fi.getName();
						byte[] bytes = IOUtils.toByteArray(fi.getInputStream());
						if (bytes != null) {
							debug("bytes:" + bytes.length);
						}
						destinationMap.put(propertyName, bytes);
						is = fi.getInputStream();
						is2 = fi.getInputStream();
					} else if (sourceMap.get(propertyName) instanceof Map) {
						Map map = (Map) sourceMap.get(propertyName);
						String storeLocation = (String) map.get("storeLocation");
						is = new FileInputStream(new File(storeLocation));
						is2 = new FileInputStream(new File(storeLocation));
						byte[] bytes = IOUtils.toByteArray(is);
						if (bytes != null) {
							debug("bytes2:" + bytes.length);
						}
						is.close();
						destinationMap.put(propertyName, bytes);
						is = new FileInputStream(new File(storeLocation));
					} else if (sourceMap.get(propertyName) instanceof String) {
						String value = (String) sourceMap.get(propertyName);
						if (value.startsWith("data:")) {
							int ind = value.indexOf(";base64,");
							byte b[] = Base64.decode(value.substring(ind + 8));
							destinationMap.put(propertyName, b);
							is = new ByteArrayInputStream(b);
							is2 = new ByteArrayInputStream(b);
						} else {
						}
					} else {
						debug("populate.byte[].no a FileItem:" + propertyName + "=>" + sourceMap.get(propertyName));
						continue;
					}
					Tika tika = new Tika();
					TikaInputStream stream = TikaInputStream.get(is);
					TikaInputStream stream2 = TikaInputStream.get(is2);
					String text = tika.parseToString(is);
					debug("Text:" + text);
					try {
						destinationMap.put("text", text);
					} catch (Exception e) {
						destinationMap.put("text", text.getBytes());
					}
					//@@@MS Hardcoded 
					try {
						Detector detector = new DefaultDetector();
						MediaType mime = detector.detect(stream2, new Metadata());
						debug("Mime:" + mime.getType() + "|" + mime.getSubtype() + "|" + mime.toString());
						destinationMap.put("type", mime.toString());
						sourceMap.put("type", mime.toString());
					} catch (Exception e) {
						e.printStackTrace();
					}
				} catch (Exception e) {
					e.printStackTrace();
					debug("populate.byte[].failed:" + propertyName + "=>" + sourceMap.get(propertyName) + ";" + e);
				} finally {
					try {
						is.close();
						is2.close();
					} catch (Exception e) {
					}
				}
			} else {
				boolean ok = false;
				try {
					Class propertyType = TypeUtils.getTypeForField(destinationObj, propertyName);
					debug("propertyType:" + propertyType + "/" + propertyName);
					if (propertyType != null) {
						if (propertyType.newInstance() instanceof javax.jdo.spi.PersistenceCapable) {
							//handleRelatedTo(sessionContext, sourceMap,propertyName, destinationMap, destinationObj, propertyType);
							Object obj = sourceMap.get(propertyName);
							if (obj != null && obj instanceof Map) {
								Map childSourceMap = (Map) obj;
								Object childDestinationObj = destinationMap.get(propertyName);
								if (childDestinationObj == null) {
									childDestinationObj = propertyType.newInstance();
									destinationMap.put(propertyName, childDestinationObj);
								}
								populate(sessionContext, childSourceMap, childDestinationObj, null);
							} else {
								if (obj == null) {
									destinationMap.put(propertyName, null);
								}
							}
							ok = true;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (!ok) {
					String value = Utils.getString(sourceMap.get(propertyName), destinationMap.get(propertyName), mode);
					try {
						destinationMap.put(propertyName, value);
					} catch (Exception e) {
						debug("populate.failed:" + propertyName + "=>" + value + ";" + e);
					}
				}
			}
		}
	}

	private static void handleRelatedTo(SessionContext sc, Map sourceMap, String propertyName, BeanMap destinationMap, Object destinationObj, Class propertyType) {
		Object id = null;
		try {
			Object _id = sourceMap.get(propertyName);
			if (_id != null) {
				if (_id instanceof Map) {
					id = ((Map) _id).get("id");
				} else {
					String s = String.valueOf(_id);
					if (s.indexOf("/") >= 0) {
						_id = Utils.extractId(s);
					}
					Class idClass = PropertyUtils.getPropertyType(propertyType.newInstance(), "id");
					id = (idClass.equals(Long.class)) ? Long.valueOf(_id + "") : _id;
				}
			}
		} catch (Exception e) {
		}
		if (id != null && !"".equals(id) && !"null".equals(id)) {
			debug("\tid.found:" + id);
			Object relatedObject = sc.getPM().getObjectById(propertyType, id);
			List<Collection> candidates = TypeUtils.getCandidateLists(relatedObject, destinationObj, null);
			if (candidates.size() == 1) {
				Collection l = candidates.get(0);
				debug("list.contains:" + l.contains(destinationObj));
				if (!l.contains(destinationObj)) {
					l.add(destinationObj);
				}
			}
			destinationMap.put(propertyName, relatedObject);
		} else {
			Object relatedObject = destinationMap.get(propertyName);
			debug("\trelatedObject:" + relatedObject);
			if (relatedObject != null) {
				List<Collection> candidates = TypeUtils.getCandidateLists(relatedObject, destinationObj, null);
				if (candidates.size() == 1) {
					Collection l = candidates.get(0);
					debug("list.contains:" + l.contains(destinationObj));
					if (l.contains(destinationObj)) {
						l.remove(destinationObj);
					}
				}
			}
			destinationMap.put(propertyName, null);
		}
	}

	private static synchronized String expandString(GroovyShell shell, String str, Map<String, String> vars) {
		String newString = "";
		int openBrackets = 0;
		int first = 0;
		for (int i = 0; i < str.length(); i++) {
			if (i < str.length() - 2 && str.substring(i, i + 2).compareTo("${") == 0) {
				if (openBrackets == 0) {
					first = i + 2;
				}
				openBrackets++;
			} else if (str.charAt(i) == '}' && openBrackets > 0) {
				openBrackets -= 1;
				if (openBrackets == 0) {
					newString += eval(shell, str.substring(first, i), vars);
				}
			} else if (openBrackets == 0) {
				newString += str.charAt(i);
			}
		}
		return newString;
	}

	private static void evaluteFormulas(SessionContext sessionContext, String entityName, Object obj, String direction, boolean isNew) {
		evaluteFormulas(sessionContext, entityName, obj, direction, isNew, null, null);
	}

	private static void evaluteFormulas(SessionContext sessionContext, String entityName, Object obj, String direction, boolean isNew, Map<String, Class> involvedClasses, PersistenceManager pm) {
		Map<String, Object> map = new BeanMap(obj);
		Map permittedFields = sessionContext.getPermittedFields(entityName);
		Map<String, Object> result = new HashMap();
		Iterator kit = permittedFields.keySet().iterator();
		while (kit.hasNext()) {
			String field = (String) kit.next();
			if (permittedFields.get(field) instanceof Map) {
				Map cm = (Map) permittedFields.get(field);
				if (cm != null && cm.get("formula_" + direction) != null && !"".equals(cm.get("formula_" + direction))) {
					if (!"out".equals(direction)) {
						if (map.get(field) == null) {
							result.put(field, "");
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
		evalator.setLocalVars(result);
		evalator.setLocalVars(map);
		evalator.setLocalVar("se", evalator);
		evalator.setLocalVar("inflector", m_inflector);
		evalator.setLocalVar("_isnew", isNew);
		evalator.setLocalVar("_user", "admin");
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
					debug("evalOk:" + formula + "/" + r);
				} catch (Exception e) {
					debug("evalError:" + formula + "/" + e);
				}
				result.put(field, r);
			}
		}
		populate(sessionContext, result, obj, null);
	}

	private static Object eval(GroovyShell shell, String expr, Map<String, String> vars) {
		try {
			Script script = shell.parse(expr);
			Binding binding = new Binding(vars);
			script.setBinding(binding);
			return script.run();
		} catch (Throwable e) {
			e.printStackTrace();
			String msg = org.ms123.common.utils.Utils.formatGroovyException(e, expr);
			throw new RuntimeException(msg);
		}
	}

	public static boolean isString(Class c, String fieldName) {
		try {
			Field f = c.getDeclaredField(fieldName);
			return f.getType().isAssignableFrom(String.class);
		} catch (Exception e) {
			return false;
		}
	}

	protected static void debug(String message) {
		m_logger.debug(message);
	}

	protected static void info(String message) {
		m_logger.info(message);
		System.out.println(message);
	}

	private static final Logger m_logger = LoggerFactory.getLogger(MultiOperations.class);
}
