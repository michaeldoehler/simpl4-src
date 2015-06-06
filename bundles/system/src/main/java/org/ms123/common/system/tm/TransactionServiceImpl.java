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
package org.ms123.common.system.tm;

import aQute.bnd.annotation.component.*;
import aQute.bnd.annotation.metatype.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Enumeration;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.ms123.common.system.tm.BitronixTransactionServiceImpl;
import org.ms123.common.system.tm.JotmTransactionServiceImpl;
import javax.naming.*;
import bitronix.tm.TransactionManagerServices;

/** TransactionService implementation
 */
@Component(enabled = true, configurationPolicy = ConfigurationPolicy.optional, immediate = true, properties = { "rpc.prefix=transaction" })
public class TransactionServiceImpl implements TransactionService {

	private static final Logger m_logger = LoggerFactory.getLogger(TransactionServiceImpl.class);
	TransactionService m_ts =null;
	public TransactionServiceImpl() {
	}

	protected void activate(BundleContext bundleContext, Map<?, ?> props) {
		info("TransactionEventHandlerService.activate.props:" + props);
		try {
			m_ts = new BitronixTransactionServiceImpl();

			Context ctx = new InitialContext();
			Context javaCompCtx = ctx.createSubcontext("java:comp");	
			javaCompCtx.bind("UserTransaction",m_ts.getTransactionManager() );
			javaCompCtx.bind("TransactionSynchronizationRegistry",TransactionManagerServices.getTransactionSynchronizationRegistry() );

			TransactionManager btm = (TransactionManager)ctx.lookup("java:comp/UserTransaction");
			System.out.println("BTM_LOOKUP:"+btm);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void update(Map<String, Object> props) {
		info("TransactionServiceImpl.updated:" + props);
	}

	protected void deactivate() throws Exception {
		info("TransactionServiceImpl.deactivate");
	}

	public UserTransaction getUserTransaction(){
		return m_ts.getUserTransaction();
	}

	public TransactionManager getTransactionManager(){
		return m_ts.getTransactionManager();
	}

	public PlatformTransactionManager getPlatformTransactionManager(){
		return m_ts.getPlatformTransactionManager();
	}

	public String getJtaLocator(){	
		return m_ts.getJtaLocator();
	}

	public TransactionTemplate getTransactionTemplate(){
		return m_ts.getTransactionTemplate();
	}

	public TransactionTemplate getTransactionTemplate(boolean propagation_requires_new){
		return m_ts.getTransactionTemplate(propagation_requires_new);
	}
	protected static void info(String msg) {
		System.out.println(msg);
		m_logger.info(msg);
	}
}
