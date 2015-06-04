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
package org.ms123.common.nucleus;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import javax.jdo.PersistenceManagerFactory;
import java.util.*;
import java.io.File;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Extent;
import javax.jdo.Query;
import javax.jdo.JDOHelper;
import javax.jdo.JDOEnhancer;
import javax.jdo.Transaction;
import org.ms123.common.libhelper.FileSystemClassLoader;
import javax.jdo.spi.*;
import org.datanucleus.store.rdbms.datasource.dbcp.managed.*;
import org.hsqldb.jdbc.pool.JDBCXADataSource;
import org.hsqldb.jdbc.JDBCDataSource;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.system.TransactionService;

/**
 */
@SuppressWarnings("unchecked")
public class HsqldbPersistenceManagerLoader extends AbstractPersistenceManagerLoader {

	private String m_baseDir;

	public HsqldbPersistenceManagerLoader(BundleContext bundleContext, StoreDesc sdesc, File[] baseDirs, ClassLoader aidClassLoader, Map props, TransactionService ts) {
		super(bundleContext, sdesc, baseDirs, aidClassLoader,props, ts);
	}

	protected void init() {
		String bd = m_sdesc.getStoreBaseDir();
		File fstore = new File(bd);
		if (!fstore.exists()) {
			fstore.mkdirs();
		}
		m_baseDir = fstore.toString();
	}

	protected void setProperties() {
		m_props.put("javax.jdo.PersistenceManagerFactoryClass", "org.datanucleus.api.jdo.JDOPersistenceManagerFactory");
		m_props.put("datanucleus.rdbms.dynamicSchemaUpdates ", "true");
		m_props.put("datanucleus.storeManagerType", m_sdesc.getStore());
		m_props.put("datanucleus.metadata.validate", "false");
		m_props.put("datanucleus.schema.autoCreateAll", "true");
		m_props.put("datanucleus.schema.validateTables", "false");
		m_props.put("datanucleus.TransactionType", "JTA");
		m_props.put("datanucleus.connection.resourceType", "JTA");
		m_props.put("datanucleus.jtaLocator", m_transactionService.getJtaLocator());
		m_props.put("datanucleus.schema.validateConstraints", "false");
		//		m_props.put("datanucleus.identifier.case", "MixedCase");
		m_props.put("datanucleus.plugin.pluginRegistryClassName", "org.ms123.common.nucleus.OsgiPluginRegistry");
	}

	protected void setDataSources() {
		JDBCXADataSource xa = null;
		try {
			xa = new JDBCXADataSource();
		} catch (java.sql.SQLException e) {
			throw new RuntimeException("HsqldbPersistenceManagerLoader.setDataSources:", e);
		}
		xa.setUser("SA");
		xa.setUrl("jdbc:hsqldb:file:" + m_baseDir + "/db;hsqldb.write_delay_millis=0");
		//xa.setDatabaseName(getDbName(null));
		BasicManagedDataSource b = new BasicManagedDataSource();
		b.setTransactionManager(m_transactionService.getTransactionManager());
		b.setXaDataSourceInstance(xa);
		m_props.put("datanucleus.ConnectionFactory", b);
		JDBCDataSource pd = new JDBCDataSource();
		// nontx
		pd.setUser("SA");
		xa.setUrl("jdbc:hsqldb:file:" + m_baseDir + "/db;hsqldb.write_delay_millis=0");
		//pd.setDatabaseName(getDbName(null));
		m_props.put("datanucleus.ConnectionFactory2", pd);
	}
}
