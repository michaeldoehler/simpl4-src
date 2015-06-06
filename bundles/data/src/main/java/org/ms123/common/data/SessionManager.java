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

import java.util.Map;
import java.util.HashMap;
import javax.jdo.PersistenceManager;
import javax.transaction.UserTransaction;
import org.ms123.common.nucleus.api.NucleusService;
import org.ms123.common.store.StoreDesc;
import javax.transaction.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unchecked")
public class SessionManager implements org.ms123.common.system.thread.ThreadFinalizer{

	private NucleusService m_nucleusService;
	private Map m_userProperties;

	private Map<StoreDesc, PersistenceManager> m_pmMap = new HashMap();
	private Map<String, Map> m_permittedFieldsMap = new HashMap();

	public SessionManager(NucleusService ns){
		m_nucleusService = ns;
	}
	public void setUserProperties(Map data) {
		m_userProperties = data;
	}

	public Map getUserProperties() {
		return m_userProperties;
	}
	public String getUserName() {
		return getThreadContext().getUserName();
	}

	public void finalize(Throwable t) {
		if( t == null){
			handleFinish();
		}else{
		handleException(t);
		}
	}
	public synchronized void handleFinish() {
		info("-> SessionManager.handleFinish");
		UserTransaction ut = m_nucleusService.getUserTransaction();
		if (ut != null) {
			try {
				info("\tstatus:" + ut.getStatus());
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				if (ut.getStatus() == Status.STATUS_ACTIVE) {
					info("###Transaction aktive:rollback");
					ut.rollback();
				}
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
		for (StoreDesc key : m_pmMap.keySet()) {
			PersistenceManager pm = m_pmMap.get(key);
			System.err.println("\tclose:" + key);
			pm.close();
		}
		m_pmMap = new HashMap();
	}

	public void handleException(Throwable e) {
		handleException(null, e);
	}

	public void handleException(UserTransaction ut, Throwable e) {
		error("\n--> SessionManager.handleException:"+e.getClass()+"/"+getStatus(ut));
		if (ut != null) {
			if (!(e instanceof javax.transaction.RollbackException)) {
				try {
					error("\thandleException.status:" + ut);
					if (ut.getStatus() == Status.STATUS_ACTIVE) {
						ut.rollback();
					}
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		}
		String msg = e.getMessage();
		while (e.getCause() != null) {
			e = e.getCause();
			msg += "\n" +e.getMessage();
		}
		error("---------------------------ExecptionMessage------------------");
		error(msg);
		error("-------------------------------------------------------------\n");

		if (e instanceof RuntimeException) {
			throw (RuntimeException) e;
		} else {
			throw new RuntimeException(e);
		}
	}
	public Object getStatus(UserTransaction ut){
		try{
			if( ut != null) return ut.toString()+":status:"+ut.getStatus();
		}catch(Exception e){
		}
		return "No Transaction active";
	}

	public PersistenceManager getPM(StoreDesc sdesc) {
		PersistenceManager pm = m_pmMap.get(sdesc);
		if (pm == null) {
			pm = m_nucleusService.getPersistenceManagerFactory(sdesc).getPersistenceManager();
			m_pmMap.put(sdesc, pm);
		}
		return pm;
	}
	protected Map<String,Map> getPermittedFieldsMap(String entityName){
		return m_permittedFieldsMap.get(entityName);
	}

	protected void  setPermittedFieldsMap(String entityName, Map<String,Object> map){
		m_permittedFieldsMap.put(entityName,map);
	}
	private org.ms123.common.system.thread.ThreadContext getThreadContext() {
		return org.ms123.common.system.thread.ThreadContext.getThreadContext();
	}
	protected void info(String message) {
		m_logger.info(message);
		System.out.println(message);
	}
	protected void error(String message) {
		m_logger.error(message);
		System.out.println(message);
	}
	private static final Logger m_logger = LoggerFactory.getLogger(SessionManager.class);
}
