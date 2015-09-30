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
import java.util.List;
import java.util.Map;
import java.util.Date;
import java.util.Collection;
import java.util.Comparator;
import java.util.Collections;
import org.ms123.common.cassandra.CassandraService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.datastax.driver.core.Session;
import com.noorq.casser.core.Casser;
import com.noorq.casser.core.CasserSession;
import static com.noorq.casser.core.Query.eq;
import static com.noorq.casser.core.Query.asc;
import static com.noorq.casser.core.Query.lt;
import static com.noorq.casser.core.Query.gt;

/**
 *
 */
@SuppressWarnings("unchecked")
abstract class BaseHistoryServiceImpl implements HistoryService {
	protected JSONDeserializer m_ds = new JSONDeserializer();
	protected JSONSerializer m_js = new JSONSerializer();
	protected CassandraService m_cassandraService;
	private CasserSession m_session;
	protected History history;
	protected HistoryRoute historyRoute;
	protected ActivitiCamelCorrelation activitiCamel;

	private static String GLOBAL_KEYSPACE = "global";
	private static String STARTTIME = "startTime";
	private static String ENDTIME = "endTime";
	private static String STATUS = "status";

	protected void upsertHistory(String key, Date time, String type, String hint, String msg) {
		initHistory();
		String key1 = null;
		String key2 = null;
		if (type != null && HISTORY_CAMEL_TRACE.equals(type)) {
			int pipe = key.lastIndexOf("|");
			if (pipe > 0) {
				key1 = key.substring(0, pipe);
				key2 = key.substring(pipe + 1);
			}
		}
		m_session.upsert()
							.value(history::key, key)
							.value(history::time, time)
							.value(history::type, type)
							.value(history::hint, hint)
							.value(history::msg, msg)
								.sync();
		if (key1 != null) {
			m_session.upsert()
							.value(historyRoute::routeId, key1)
							.value(historyRoute::instanceId, key2)
							.value(historyRoute::time, time)
								.sync();
		}
	}

	protected void upsertAcc(String activitiId, String routeInstanceId) {
		initHistory();
			m_session.upsert()
							.value(activitiCamel::activitiId, activitiId)
							.value(activitiCamel::routeInstanceId, routeInstanceId)
								.sync();
	}

	protected List<Map> _getHistory(String key, String type, Long startTime, Long endTime) throws Exception {
		final List<Map> retList = new ArrayList();
		initHistory();
		if (startTime == null) {
			startTime = new Date().getTime() - (long) 1 * 1000 * 60 * 60 * 24;
		}
		if (endTime == null) {
			endTime = new Date().getTime() + 1000000;
		}
		if (type != null && HISTORY_CAMEL_TRACE.equals(type) && key.indexOf("|") < 0) {
			m_session.select(historyRoute::routeId, historyRoute::instanceId, historyRoute::time)
							.where(historyRoute::routeId, eq(key))
								.and(historyRoute::time, gt(new Date(startTime)))
								.and(historyRoute::time, lt(new Date(endTime)))
									.orderBy(asc(history::time)).sync().forEach(h -> {
				Map m = new HashMap();
				String _key = h._1 + "|" + h._2;
				List<Map> lm = _getOneEntry(_key, type);
				retList.addAll(lm);
			});
		} else {
			retList.addAll(_getOneEntry(key, type));
		}
		return retList;
	}

	private List<Map> _getOneEntry(String key, String type) {
		List<Map> retList = new ArrayList();
		m_session.select(history::key, history::time, history::type, history::hint, history::msg)
							.where(history::key, eq(key))
								.and(history::type, eq(type))
									.orderBy(asc(history::time)).sync().forEach(h -> {
			Map m = new HashMap();
			m.put(HISTORY_KEY, h._1);
			m.put(HISTORY_TIME, h._2);
			m.put(HISTORY_TYPE, h._3);
			m.put(HISTORY_HINT, h._4);
			m.put(HISTORY_MSG, h._5);
			retList.add(m);
		});
		return retList;
	}

	protected List<Map> _getRouteInstances(String contextKey, String routeId, java.lang.Long _startTime, java.lang.Long endTime) throws Exception {
		List<Map> retList = new ArrayList();
		List<Map> historyEntries = _getHistory(contextKey + "/" + routeId, HISTORY_CAMEL_TRACE, _startTime, endTime);
		String currentKey = null;
		Date startTime = null;
		Date prevTime = null;
		boolean hasError = false;
		for (Map entry : historyEntries) {
			String key = (String) entry.get(HISTORY_KEY);
			if ("error".equals(entry.get(HISTORY_HINT))) {
				hasError = true;
			}
			if (!key.equals(currentKey)) {
				if (startTime != null) {
					retList.add(createRecord(startTime, prevTime, currentKey, hasError));
					hasError = false;
				}
				startTime = (Date) entry.get(HISTORY_TIME);
				currentKey = key;
			}
			prevTime = (Date) entry.get(HISTORY_TIME);
		}
		if (startTime != null) {
			retList.add(createRecord(startTime, prevTime, currentKey, hasError));
		}
		sortListByStartTime(retList);
		return retList;
	}

	protected List<Map> _getRouteInstance(String contextKey, String routeId, String exchangeId) throws Exception {
		List<Map> historyEntries = _getHistory(contextKey + "/" + routeId + "|" + exchangeId, HISTORY_CAMEL_TRACE, null, null);
		return historyEntries;
	}

	private Map createRecord(Date startTime, Date endTime, String key, boolean hasError) {
		Map retMap = new HashMap();
		retMap.put(STARTTIME, startTime);
		retMap.put(ENDTIME, endTime);
		retMap.put(STATUS, hasError ? "error" : "ok");
		int lastSlash = key.lastIndexOf("|");
		retMap.put("exchangeId", key.substring(lastSlash + 1));
		return retMap;
	}

	private void sortListByStartTime(List<Map> list) {
		Collections.sort(list, new TComparable());
	}

	private static class TComparable implements Comparator<Map> {
		@Override
		public int compare(Map m1, Map m2) {
			Date l1 = (Date) m1.get(STARTTIME);
			Date l2 = (Date) m2.get(STARTTIME);
			return l2.compareTo(l1);
		}
	}

	private void initHistory() {
		if (m_session != null) {
			return;
		}
		try {
			Session session = m_cassandraService.getSession(GLOBAL_KEYSPACE);
			history = Casser.dsl(History.class);
			historyRoute = Casser.dsl(HistoryRoute.class);
			activitiCamel = Casser.dsl(ActivitiCamelCorrelation.class);
			m_session = Casser.init(session).showCql()
				.add(history)
				.add(historyRoute)
				.add(activitiCamel)
					.autoCreateDrop().get();
			info("history:" + history);
		} catch (Exception e) {
			info("BaseHistoryServiceImpl.initHistory:" + e.getMessage());
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

