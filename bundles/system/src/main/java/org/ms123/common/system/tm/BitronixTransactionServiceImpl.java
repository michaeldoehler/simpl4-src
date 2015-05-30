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

import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import org.objectweb.jotm.Jotm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.TransactionDefinition;
import org.ms123.common.system.TransactionService;
import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.BitronixTransactionManager;
import java.util.Hashtable;
import javax.naming.*;

/**
 *
 */
public class BitronixTransactionServiceImpl implements TransactionService{

	protected TransactionTemplate transactionTemplate;

	public static  BitronixTransactionManager m_btm;

	protected JtaTransactionManager m_jta;

  @SuppressWarnings("unchecked")
	public BitronixTransactionServiceImpl() throws Exception{
		TransactionManagerServices.getConfiguration().setDefaultTransactionTimeout(36000);;
		TransactionManagerServices.getConfiguration().setWarnAboutZeroResourceTransaction(false);
		m_btm = new BitronixTransactionManager();
		m_jta = new JtaTransactionManager((UserTransaction)m_btm,(TransactionManager)m_btm);
	}

	public UserTransaction getUserTransaction() {
		UserTransaction utx = m_btm;
		return utx;
	}
	public String getJtaLocator(){
		return "bitronix";
	}

	public TransactionManager getTransactionManager() {
		TransactionManager tm = m_btm;
		return tm;
	}

	public PlatformTransactionManager getPlatformTransactionManager() {
		return m_jta;
	}

	public TransactionTemplate getTransactionTemplate() {
		return getTransactionTemplate(false);
	}

	public TransactionTemplate getTransactionTemplate(boolean _new) {
		DefaultTransactionDefinition d = _new ? new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW) : new DefaultTransactionDefinition();
		System.err.println("TransactionTemplate:" + d);
		return new TransactionTemplate(m_jta, d);
	}

	protected static void debug(String msg) {
		System.out.println(msg);
		m_logger.debug(msg);
	}

	protected static void info(String msg) {
		System.out.println(msg);
		m_logger.info(msg);
	}

	private static final Logger m_logger = LoggerFactory.getLogger(BitronixTransactionServiceImpl.class);
}
