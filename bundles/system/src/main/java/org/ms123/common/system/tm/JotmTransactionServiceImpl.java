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

/**
 *
 */
public class JotmTransactionServiceImpl implements TransactionService{

	protected TransactionTemplate transactionTemplate;

	protected Jotm m_jotm;

	protected JtaTransactionManager m_jta;

	public JotmTransactionServiceImpl() throws Exception{
			m_jotm = new org.objectweb.jotm.Jotm(true, false);
			m_jta = new JtaTransactionManager(m_jotm.getTransactionManager());
	}
	public UserTransaction getUserTransaction() {
		UserTransaction utx = m_jotm.getUserTransaction();
		return utx;
	}
	public String getJtaLocator(){
		return "jotm";
	}

	public TransactionManager getTransactionManager() {
		TransactionManager tm = m_jotm.getTransactionManager();
		return tm;
	}

	public PlatformTransactionManager getPlatformTransactionManager() {
		return m_jta;
	}

	public Jotm getJotm() {
		return m_jotm;
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

	private static final Logger m_logger = LoggerFactory.getLogger(JotmTransactionServiceImpl.class);
}
