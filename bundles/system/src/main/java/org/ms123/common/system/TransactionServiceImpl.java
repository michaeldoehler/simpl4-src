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

import java.util.ArrayList;
import java.util.HashMap;
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
import static org.apache.commons.beanutils.PropertyUtils.setProperty;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.ms123.common.rpc.PName;
import org.ms123.common.rpc.POptional;
import org.ms123.common.rpc.RpcException;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.ms123.common.rpc.JsonRpcServlet.ERROR_FROM_METHOD;
import static org.ms123.common.rpc.JsonRpcServlet.INTERNAL_SERVER_ERROR;
import static org.ms123.common.rpc.JsonRpcServlet.PERMISSION_DENIED;

/** TransactionService implementation
 */
@Component(enabled = true, configurationPolicy = ConfigurationPolicy.optional, immediate = true, properties = { "rpc.prefix=transaction" })
public class TransactionServiceImpl extends BaseTransactionServiceImpl implements TransactionService {

	private static final Logger m_logger = LoggerFactory.getLogger(TransactionServiceImpl.class);

	public TransactionServiceImpl() {
	}

	protected void activate(BundleContext bundleContext, Map<?, ?> props) {
		info("TransactionEventHandlerService.activate.props:" + props);
		try {
			Bundle b = bundleContext.getBundle();
			m_bundleContext = bundleContext;
			m_jotm = new org.objectweb.jotm.Jotm(true, false);
			m_jta = new JtaTransactionManager(this.getTransactionManager());
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
}
