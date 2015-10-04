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

import aQute.bnd.annotation.component.*;
import aQute.bnd.annotation.metatype.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.Date;
import java.util.Dictionary;
import org.ms123.common.cassandra.CassandraService;
import org.ms123.common.rpc.PName;
import org.ms123.common.rpc.POptional;
import org.ms123.common.rpc.RpcException;
import org.ms123.common.store.StoreDesc;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.apache.shiro.authz.annotation.RequiresRoles;

import static org.ms123.common.rpc.JsonRpcServlet.ERROR_FROM_METHOD;
import static org.ms123.common.rpc.JsonRpcServlet.INTERNAL_SERVER_ERROR;

/** HistoryService implementation
 */
@SuppressWarnings("unchecked")
@Component(enabled = true, configurationPolicy = ConfigurationPolicy.optional, immediate = true, properties = { "rpc.prefix=history" })
public class HistoryServiceImpl extends BaseHistoryServiceImpl implements HistoryService, EventHandler {

	private EventAdmin m_eventAdmin;
	private static final String[] topics = new String[] { HISTORY_TOPIC };


	public HistoryServiceImpl() {
	}

	protected void activate(BundleContext bundleContext, Map<?, ?> props) {
		System.out.println("HistoryEventHandlerService.activate.props:" + props);
		try {
			Bundle b = bundleContext.getBundle();
			Dictionary d = new Hashtable();
			d.put(EventConstants.EVENT_TOPIC, topics);
			b.getBundleContext().registerService(EventHandler.class.getName(), this, d);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void update(Map<String, Object> props) {
		info("HistoryServiceImpl.updated:" + props);
	}

	protected void deactivate() throws Exception {
		info("HistoryServiceImpl.deactivate");
	}

	public void handleEvent(Event event) {
		info("HistoryServiceImpl.handleEvent: " + event + "/"+ event.getProperty(HISTORY_TYPE));
		try {
			String type = (String) event.getProperty(HISTORY_TYPE);
			if( ACTIVITI_CAMEL_CORRELATION_TYPE.equals(type)){
				String activitiId = (String) event.getProperty(ACC_ACTIVITI_ID);
				String routeInstanceId = (String) event.getProperty(ACC_ROUTE_INSTANCE_ID);
				upsertAcc(activitiId, routeInstanceId);
			}else{
				String key = (String) event.getProperty(HISTORY_KEY);
				Date time = new Date();
				String hint = (String) event.getProperty(HISTORY_HINT);
				String msg = (String) event.getProperty(HISTORY_MSG);
				upsertHistory(key, time, type, hint, msg);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}
		info("HistoryServiceImpl.end");
	}


	@RequiresRoles("admin")
	public Map<String, List<Map>> getHistoryByKeyList(
			@PName("keyList") List<String> keyList, 
			@PName(HISTORY_TYPE) @POptional String type) throws RpcException {
		try {
			Map<String, List<Map>> retMap = new HashMap();
			for (String key : keyList) {
				List<Map> logs = _getHistory(key, type, null, null);
				if (logs.size() > 0) {
					retMap.put(key, logs);
				}
			}
			return retMap;
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "LogServiceImpl.getHistoryByKeyList:", e);
		}
	}

	@RequiresRoles("admin")
	public List<Map> getHistory(
			@PName(HISTORY_KEY) String key, 
			@PName(HISTORY_TYPE) @POptional String type, 
			@PName("startTime") @POptional Long startTime, 
			@PName("endTime") @POptional Long endTime) throws RpcException {
		try {
			return _getHistory(key, type, startTime, endTime);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "LogServiceImpl.getHistory:", e);
		}
	}

	@RequiresRoles("admin")
	public List<Map> getRouteInstances(
			@PName("contextKey") String contextKey,
			@PName("routeId") String routeId,
			@PName("startTime") @POptional Long startTime,
			@PName("endTime") @POptional Long endTime
			 ) throws RpcException {
		try {
			return _getRouteInstances(contextKey, routeId, startTime,endTime);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "CamelServiceImpl.getRouteInstances:", e);
		}
	}

	@RequiresRoles("admin")
	public List<Map> getRouteInstance(
			@PName("contextKey") String contextKey,
			@PName("routeId") String routeId,
			@PName("exchangeId") String exchangeId
			 ) throws RpcException {
		try {
			return _getRouteInstance(contextKey, routeId, exchangeId);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "CamelServiceImpl.getRouteInstance:", e);
		}
	}
	@RequiresRoles("admin")
	public Map<String,List<Map>> getRouteInstancesByActivitiId(
			@PName("activitiId") String activitiId
			 ) throws RpcException {
		try {
			Map<String,List<Map>> ret = new LinkedHashMap<String,List<Map>>();
			info("getRouteInstance.activitiId:"+activitiId);
			Set<String> routeDefIds = _getActivitiCamelCorrelation(activitiId);
			info("getRouteInstance.routeDefIds:"+routeDefIds);
			if( routeDefIds.size() == 0) return null;
			for( String routeDefId : routeDefIds){	
				String x[] = routeDefId.split("\\|");
				String exchangeId = x[1];
				int slash = x[0].lastIndexOf("/");
				String contextKey = x[0].substring(0, slash);
				String routeId = x[0].substring(slash+1);
				ret.put(routeDefId, _getRouteInstance(contextKey, routeId, exchangeId));
			}
			return ret;
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "CamelServiceImpl.getRouteInstance:", e);
		}
	}
	@Reference(dynamic = true, optional = true)
	public void setCassandraService(CassandraService cassandraService) {
		System.out.println("HistoryServiceImpl.setCassandraService:" + cassandraService);
		this.m_cassandraService = cassandraService;
	}
}

