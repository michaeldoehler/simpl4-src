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
import java.util.Date;
import java.util.Dictionary;
import java.util.NoSuchElementException;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.ms123.common.cassandra.CassandraService;
import org.ms123.common.git.GitService;
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

import static org.apache.commons.io.FileUtils.readFileToString;
import static org.ms123.common.rpc.JsonRpcServlet.ERROR_FROM_METHOD;
import static org.ms123.common.rpc.JsonRpcServlet.INTERNAL_SERVER_ERROR;
import static org.ms123.common.rpc.JsonRpcServlet.PERMISSION_DENIED;
import static org.apache.commons.beanutils.PropertyUtils.setProperty;

/** HistoryService implementation
 */
@SuppressWarnings("unchecked")
@Component(enabled = true, configurationPolicy = ConfigurationPolicy.optional, immediate = true, properties = { "rpc.prefix=history" })
public class HistoryServiceImpl extends BaseHistoryServiceImpl implements HistoryService, EventHandler {

	private EventAdmin m_eventAdmin;
	private static final String[] topics = new String[] { "history" };


	public HistoryServiceImpl() {
	}

	protected void activate(BundleContext bundleContext, Map<?, ?> props) {
		System.out.println("HistoryEventHandlerService.activate.props:" + props);
		try {
			Bundle b = bundleContext.getBundle();
			m_bundleContext = bundleContext;
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
		info("HistoryServiceImpl.handleEvent: " + event + ",key:" + event.getProperty(LOG_KEY) + ",type:" + event.getProperty(LOG_TYPE));
		try {
			String key = (String) event.getProperty(LOG_KEY);
			Date time = new Date();
			String type = (String) event.getProperty(LOG_TYPE);
			String hint = (String) event.getProperty(LOG_HINT);
			String msg = (String) event.getProperty(LOG_MSG);
			upsert(key, time, type, hint, msg);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}
		info("HistoryServiceImpl.end");
	}


	public Map<String, List<Map>> getHistoryByKeyList(
			@PName("keyList") List<String> keyList, 
			@PName(LOG_TYPE) @POptional String type) throws RpcException {
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

	public List<Map> getHistory(
			@PName(LOG_KEY) String key, 
			@PName(LOG_TYPE) @POptional String type, 
			@PName("startTime") @POptional Long startTime, 
			@PName("endTime") @POptional Long endTime) throws RpcException {
		try {
			return _getHistory(key, type, startTime, endTime);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "LogServiceImpl.getHistory:", e);
		}
	}

	public List<Map> getHistory(String key, String type) throws Exception {
		return _getHistory(key, type,  null,null);
	}

	@Reference(dynamic = true, optional = true)
	public void setCassandraService(CassandraService cassandraService) {
		System.out.println("HistoryServiceImpl.setCassandraService:" + cassandraService);
		this.m_cassandraService = cassandraService;
	}
}

