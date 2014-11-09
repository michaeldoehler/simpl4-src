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
import org.ms123.common.libhelper.Inflector;
import org.ms123.common.permission.api.PermissionService;
import org.ms123.common.team.api.TeamService;
import org.ms123.common.utils.IOUtils;
import org.ms123.common.utils.TypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.ms123.common.entity.api.Constants.STATE_FIELD;
import org.ms123.common.libhelper.Base64;

@SuppressWarnings("unchecked")
public class MultiOperations {

	protected static Inflector m_inflector = Inflector.getInstance();

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////// 
	//populate 
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////// 
	public static void populate(SessionContext sessionContext, Map from, Object to, Map hintsMap) {
		PersistenceManager pm = sessionContext.getPM();
		if (hintsMap == null) {
			hintsMap = new HashMap();
		}
		BeanMap beanMap = new BeanMap(to);
		String entityName = m_inflector.getEntityName(to.getClass().getSimpleName());
		debug("populate.from:" + from + ",to:" + to + ",BeanMap:" + beanMap + "/hintsMap:" + hintsMap + "/entityName:" + entityName);
		if (from == null) {
			return;
		}
		Map permittedFields = sessionContext.getPermittedFields(entityName, "write");
		Iterator<String> it = from.keySet().iterator();
		while (it.hasNext()) {
			String key = it.next();
			boolean permitted = sessionContext.getPermissionService().hasAdminRole() || "team".equals(entityName) || sessionContext.isFieldPermitted(key, entityName, "write");
			if (!key.startsWith("_") && !permitted) {
				debug("---->populate:field(" + key + ") no write permission");
				continue;
			} else {
				debug("++++>populate:field(" + key + ") write permitted");
			}
			String datatype = null;
			if (!key.startsWith("_")) {
				Map config = (Map) permittedFields.get(key);
				if (config != null) {
					datatype = (String) config.get("datatype");
				}
			}
			if (key.equals(STATE_FIELD) && !sessionContext.getPermissionService().hasAdminRole()) {
				continue;
			}
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
			debug("\ttype:" + clazz + "(" + key + "=" + from.get(key) + ")");
			if ("_ignore_".equals(from.get(key))) {
				continue;
			}
			if (clazz == null) {
				debug("\t--- Warning property not found:" + key);
			} else if (clazz.equals(java.util.Date.class)) {
				String value = Utils.getString(from.get(key), beanMap.get(key), mode);
				debug("\tDate found:" + key + "=>" + value);
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
				beanMap.put(key, date);
			} else if (clazz.equals(java.util.Map.class)) {
				info("!!!!!!!!!!!!!!!!!!!Map not implemented");
			} else if (clazz.equals(java.util.List.class) || clazz.equals(java.util.Set.class)) {
				boolean isList = clazz.equals(java.util.List.class);
				boolean isSimple = false;
				if (datatype != null && datatype.startsWith("list_")) {
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
					debug("toList:" + toList);
					debug("valList:" + valList);
					if (toList == null) {
						toList = isList ? new ArrayList() : new HashSet();
						PropertyUtils.setProperty(to, key, toList);
					}
					if ("replace".equals(mode)) {
						boolean isEqual = false;
						if (isSimple) {
							isEqual = Utils.isCollectionEqual(toList, valList);
							if (!isEqual) {
								toList.clear();
							}
							debug("\tisEqual:" + isEqual);
						} else {
							List deleteList = new ArrayList();
							String namespace = sessionContext.getStoreDesc().getNamespace();
							for (Object o : toList) {
								if (type.getName().endsWith(".Team")) {
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
								toList.remove(o);
							}
						}
						debug("populate.replace.toList:" + toList + "/" + type.getName());
						if (isSimple) {
							if (!isEqual) {
								for (Object o : valList) {
									toList.add(o);
								}
							}
						} else {
							for (Object o : valList) {
								Map valMap = (Map) o;
								Object n = type.newInstance();
								if (type.getName().endsWith(".Team")) {
									valMap.remove("id");
									Object desc = valMap.get("description");
									Object name = valMap.get("name");
									Object dis = valMap.get("disabled");
									String teamid = (String) valMap.get("teamid");
									Object ti = Utils.getTeamintern(sessionContext, teamid);
									if (desc == null) {
										valMap.put("description", PropertyUtils.getProperty(ti, "description"));
									}
									if (name == null) {
										valMap.put("name", PropertyUtils.getProperty(ti, "name"));
									}
									if (dis == null) {
										valMap.put("disabled", false);
									}
									pm.makePersistent(n);
									populate(sessionContext, valMap, n, null);
									PropertyUtils.setProperty(n, "teamintern", ti);
								} else {
									pm.makePersistent(n);
									populate(sessionContext, valMap, n, null);
								}
								debug("populated.add:" + new HashMap(new BeanMap(n)));
								toList.add(n);
							}
						}
					} else if ("remove".equals(mode)) {
						if (isSimple) {
							for (Object o : valList) {
								if (toList.contains(o)) {
									toList.remove(o);
								}
							}
						} else {
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
						if (isSimple) {
							for (Object o : valList) {
								toList.add(o);
							}
						} else {
							for (Object ol : valList) {
								Map map = (Map) ol;
								Object o = Utils.listContainsId(toList, map, "teamid");
								if (o != null) {
									populate(sessionContext, map, o, null);
								} else {
									o = type.newInstance();
									if (type.getName().endsWith(".Team")) {
										Object desc = map.get("description");
										Object name = map.get("name");
										Object dis = map.get("disabled");
										String teamid = (String) map.get("teamid");
										Object ti = Utils.getTeamintern(sessionContext, teamid);
										if (desc == null) {
											map.put("description", PropertyUtils.getProperty(ti, "description"));
										}
										if (name == null) {
											map.put("name", PropertyUtils.getProperty(ti, "name"));
										}
										if (dis == null) {
											map.put("disabled", false);
										}
										pm.makePersistent(o);
										populate(sessionContext, map, o, null);
										PropertyUtils.setProperty(o, "teamintern", ti);
									} else {
										pm.makePersistent(o);
										populate(sessionContext, map, o, null);
									}
									toList.add(o);
								}
							}
						}
					} else if ("assign".equals(mode)) {
						if (!isSimple) {
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
						if (bytes != null) {
							debug("bytes:" + bytes.length);
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
						if (bytes != null) {
							debug("bytes2:" + bytes.length);
						}
						is.close();
						beanMap.put(key, bytes);
						is = new FileInputStream(new File(storeLocation));
					} else if (from.get(key) instanceof String) {
						String value = (String) from.get(key);
						if (value.startsWith("data:")) {
							int ind = value.indexOf(";base64,");
							byte b[] = Base64.decode(value.substring(ind + 8));
							beanMap.put(key, b);
							is = new ByteArrayInputStream(b);
							is2 = new ByteArrayInputStream(b);
						} else {
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
					Class type = TypeUtils.getTypeForField(to, key);
					if (type != null) {
						Object o = type.newInstance();
						if (o instanceof javax.jdo.spi.PersistenceCapable) {
							Object id = null;
							try {
								Object _id = from.get(key);
								if (_id != null) {
									if (_id instanceof Map) {
										id = ((Map) _id).get("id");
									} else {
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
							debug("FromX:" + from.get(key) + "/" + from.get(key).getClass());
							if (id != null && !"".equals(id) && !"null".equals(id)) {
								debug("\tId2:" + id);
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
								debug("\trelatedObject:" + relatedObject);
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
