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

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Bundle;
import aQute.bnd.annotation.metatype.*;
import aQute.bnd.annotation.component.*;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.ms123.common.nucleus.api.NucleusService;
import org.ms123.common.store.StoreDesc;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;
import javax.transaction.UserTransaction;
import javax.transaction.Status;
import static org.apache.commons.beanutils.PropertyUtils.setProperty;
import org.ms123.common.rpc.PName;
import org.ms123.common.rpc.POptional;
import org.ms123.common.rpc.RpcException;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.ms123.common.rpc.JsonRpcServlet.ERROR_FROM_METHOD;
import static org.ms123.common.rpc.JsonRpcServlet.INTERNAL_SERVER_ERROR;
import static org.ms123.common.rpc.JsonRpcServlet.PERMISSION_DENIED;

/** LogService implementation
 */
@SuppressWarnings("unchecked")
@Component(enabled = true, configurationPolicy = ConfigurationPolicy.optional, immediate = true, properties = { "rpc.prefix=log" })
public class LogServiceImpl extends BaseLogServiceImpl implements LogService, EventHandler {

	private static final Logger m_logger = LoggerFactory.getLogger(LogServiceImpl.class);

	private EventAdmin m_eventAdmin;

	public LogServiceImpl() {
	}

	static final String[] topics = new String[] { "log" };

	protected void activate(BundleContext bundleContext, Map<?, ?> props) {
		System.out.println("LogEventHandlerService.activate.props:" + props);
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

	public void handleEvent(Event event) {
		info("LogServiceImpl.handleEvent: " + event + ",key:" + event.getProperty(LOG_KEY) + ",type:" + event.getProperty(LOG_TYPE));
		StoreDesc sdesc = StoreDesc.getGlobalData();
		PersistenceManager pm = m_nucleusService.getPersistenceManagerFactory(sdesc).getPersistenceManager();
		UserTransaction ut = m_nucleusService.getUserTransaction();
		try {
			ut.begin();
			Object logMessage = m_nucleusService.getClass(sdesc, "logmessage").newInstance();
			setProperty(logMessage, LOG_KEY, event.getProperty(LOG_KEY));
			if (event.getProperty(LOG_HINT) != null) {
				setProperty(logMessage, LOG_HINT, event.getProperty(LOG_HINT));
			}
			if (event.getProperty(LOG_TYPE) != null) {
				setProperty(logMessage, LOG_TYPE, event.getProperty(LOG_TYPE));
			}
			setProperty(logMessage, LOG_MSG, event.getProperty(LOG_MSG));
			setProperty(logMessage, LOG_TIME, new Date());
			pm.makePersistent(logMessage);
			ut.commit();
		} catch (Exception e) {
			try {
				ut.rollback();
			} catch (Exception e1) {
			}
			e.printStackTrace();
		} finally {
			pm.close();
		}
	}

	public void update(Map<String, Object> props) {
		info("LogServiceImpl.updated:" + props);
	}

	protected void deactivate() throws Exception {
		info("LogServiceImpl.deactivate");
	}

	public Map<String, List<Map>> getLogKeyList(
			@PName("keyList")          List<String> keyList, 
			@PName(LOG_TYPE)             @POptional String type) throws RpcException {
		try {
			Map<String, List<Map>> retMap = new HashMap();
			for (String key : keyList) {
				List<Map> logs = _getLog(key, type,null,null,null,null);
				if (logs.size() > 0) {
					retMap.put(key, logs);
				}
			}
			return retMap;
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "LogServiceImpl.getLogKeyList:", e);
		}
	}

	public List<Map> getLog(
			@PName(LOG_KEY)              String key, 
			@PName(LOG_TYPE)             @POptional String type, 
			@PName("projection")       		@POptional String projection, 
			@PName("orderby")       		@POptional String orderby, 
			@PName("startTime")        @POptional Long startTime, 
			@PName("endTime")          @POptional Long endTime) throws RpcException {
		try {
			return _getLog(key, type, projection, orderby,startTime, endTime);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "LogServiceImpl.getLog:", e);
		}
	}

	@Reference(dynamic = true)
	public void setEventAdmin(EventAdmin paramEventAdmin) {
		System.out.println("LogServiceImpl.setEventAdmin:" + paramEventAdmin);
		this.m_eventAdmin = paramEventAdmin;
	}

	@Reference
	public void setNucleusService(NucleusService paramNucleusService) {
		this.m_nucleusService = paramNucleusService;
		System.out.println("LogServiceImpl.setNucleusService:" + paramNucleusService);
	}
}
