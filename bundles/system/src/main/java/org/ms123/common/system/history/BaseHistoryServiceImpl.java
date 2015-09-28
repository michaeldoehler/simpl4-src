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
package org.ms123.common.system.history;

import flexjson.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Date;
import java.util.Collection;
import org.ms123.common.cassandra.CassandraService;
import org.ms123.common.libhelper.Inflector;
import org.ms123.common.store.StoreDesc;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.datastax.driver.core.Session;
import com.noorq.casser.core.Casser;
import com.noorq.casser.core.CasserSession;
import static com.noorq.casser.core.Query.eq;
import static com.noorq.casser.core.Query.asc;

/**
 *
 */
@SuppressWarnings("unchecked")
abstract class BaseHistoryServiceImpl implements HistoryService{
	protected BundleContext m_bundleContext;
	protected JSONDeserializer m_ds = new JSONDeserializer();
	protected JSONSerializer m_js = new JSONSerializer();
	protected CassandraService m_cassandraService;
	private CasserSession m_session;
	protected History history;
	protected HistoryRoute historyRoute;
	private String GLOBAL_KEYSPACE = "global";

	protected void upsert(String key, Date time, String type, String hint, String msg) {
		initHistory();
		String key1 = null;
		String key2 = null;
		if (type != null && "camel/trace".equals(type)) {
			int pipe = key.lastIndexOf("|");
			if (pipe > 0) {
				key1 = key.substring(0, pipe);
				key2 = key.substring(pipe + 1);
			}
		}
		m_session.upsert().value(history::key, key).value(history::time, time).value(history::type, type).value(history::hint, hint).value(history::msg, msg).sync();
		if (key1 != null) {
			m_session.upsert().value(historyRoute::routeId, key1).value(historyRoute::instanceId, key2).value(historyRoute::time, time).sync();
		}
	}

	protected List<Map> _getHistory(String key, String type, Long startTime, Long endTime) throws Exception {
		final List<Map> retList = new ArrayList();
		initHistory();

		if (type != null && "camel/trace".equals(type) && key.indexOf("|") < 0 ) {
			m_session.select(historyRoute::routeId, historyRoute::instanceId, historyRoute::time).where(historyRoute::routeId, eq(key)).orderBy(asc(history::time)).sync()
			.forEach(h -> {
				Map m = new HashMap();
				String _key = h._1 + "|"+ h._2;
				List<Map> lm = _getOneInstance( _key, type, startTime, endTime);
				retList.addAll(lm);
			});
		}else{
			retList.addAll(_getOneInstance( key, type, startTime, endTime));
		}
		return retList;
	}

	protected List<Map> _getOneInstance(String key, String type, Long startTime, Long endTime) {
		List<Map> retList = new ArrayList();
		m_session.select(history::key, history::time, history::type, history::hint, history::msg).where(history::key, eq(key)).and(history::type, eq(type)).orderBy(asc(history::time)).sync()
		.forEach(h -> {
			Map m = new HashMap();
			m.put(LOG_KEY, h._1);
			m.put(LOG_TIME, h._2);
			m.put(LOG_TYPE, h._3);
			m.put(LOG_HINT, h._4);
			m.put(LOG_MSG, h._5);
			retList.add(m);
		});
		return retList;
	}

	protected void initHistory() {
		if (m_session != null){
			return;
		}
		try{
			Session session = m_cassandraService.getSession(GLOBAL_KEYSPACE);
			history = Casser.dsl(History.class);
			historyRoute = Casser.dsl(HistoryRoute.class);
			m_session = Casser.init(session).showCql().add(history).add(historyRoute).autoCreateDrop().get();
			info("history:" + history);
		}catch(Exception e){
			info("BaseHistoryServiceImpl.initHistory:" +e.getMessage());
			m_session = null;
			e.printStackTrace();
		}
	}


	protected static void info(String msg) {
		System.err.println(msg);
		m_logger.info(msg);
	}
	protected static void debug(String msg) {
		m_logger.debug(msg);
	}

	private static final Logger m_logger = LoggerFactory.getLogger(HistoryServiceImpl.class);
}
