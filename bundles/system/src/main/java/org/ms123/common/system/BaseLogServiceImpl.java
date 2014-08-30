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
package org.ms123.common.system;

import flexjson.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.nucleus.api.NucleusService;
import org.ms123.common.libhelper.Inflector;
import org.osgi.framework.BundleContext;
import javax.jdo.PersistenceManager;
import javax.jdo.Extent;
import javax.jdo.Query;
import javax.transaction.UserTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.camel.util.IntrospectionSupport;
import static org.ms123.common.system.LogService.LOG_TIME;

/**
 *
 */
@SuppressWarnings("unchecked")
abstract class BaseLogServiceImpl {

	protected NucleusService m_nucleusService;

	protected Inflector m_inflector = Inflector.getInstance();

	protected BundleContext m_bundleContext;

	protected JSONDeserializer m_ds = new JSONDeserializer();

	protected JSONSerializer m_js = new JSONSerializer();

	public List<Map> getLog(String key, String type) throws Exception {
		return _getLog(key, type, null, null, null,null);
	}
	public List<Map> getLog(String key, String type,String projection,Long startTime, Long endTime) throws Exception {
		return _getLog(key, type, projection, null, startTime,endTime);
	}

	protected List<Map> _getLog(String key, String type, String projection, String orderby, Long startTime, Long endTime) throws Exception {
		List<Map> retList = new ArrayList();
		String filter = "key.startsWith(:_key)";
		Map varMap = new HashMap();
		varMap.put("_key", key);
		if (type != null) {
			filter += " && type == :_type";
			varMap.put("_type", type);
		}
		if (startTime != null) {
			filter += " && time >= :_startTime";
			varMap.put("_startTime", new Date(startTime));
		}
		if (endTime != null) {
			filter += " && time <= :_endTime";
			varMap.put("_endTime", new Date(endTime));
		}
		StoreDesc sdesc = StoreDesc.getGlobalData();
		PersistenceManager pm = m_nucleusService.getPersistenceManagerFactory(sdesc).getPersistenceManager();
		Class clazz = m_nucleusService.getClass(sdesc, m_inflector.getClassName("logmessage"));
		Extent e = pm.getExtent(clazz, true);
		Query q = pm.newQuery(e, filter);
		if( projection == null || !projection.startsWith("distinct")){
			if( orderby != null){
				q.setOrdering(orderby);
			}else{
				q.setOrdering(LOG_TIME);
			}
		}
		q.setResult(projection);
		try {
			Collection coll = (Collection) q.executeWithMap(varMap);
			Iterator iter = coll.iterator();
			while (iter.hasNext()) {
				Object obj = iter.next();
				Map props = null;
				if (projection != null) {
					props = getProps(projection, obj);
				} else {
					props = new HashMap();
					IntrospectionSupport.getProperties(obj, props, null);
				}
				retList.add(props);
			}
		} finally {
			q.closeAll();
		}
		return retList;
	}

	private Map getProps(String projection, Object obj) {
		Map retMap = new HashMap();
		projection = projection.replaceAll(" ", "");
		if (projection.startsWith("distinct")){
			projection = projection.substring(8);
		}
		String[] fields = projection.split(",");
		if (obj instanceof Object[]) {
			int i = 0;
			for (Object o : (Object[]) obj) {
				retMap.put(fields[i++], o);
			}
		} else {
			retMap.put(fields[0], obj);
		}
		return retMap;
	}

	protected static void debug(String msg) {
		System.out.println(msg);
		m_logger.debug(msg);
	}

	protected static void info(String msg) {
		System.out.println(msg);
		m_logger.info(msg);
	}

	private static final Logger m_logger = LoggerFactory.getLogger(BaseLogServiceImpl.class);
}
