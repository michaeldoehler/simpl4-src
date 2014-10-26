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
import org.postgresql.xa.PGXADataSource;
import org.postgresql.ds.PGPoolingDataSource;
import org.datanucleus.store.rdbms.datasource.dbcp.managed.*;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.system.TransactionService;
import bitronix.tm.resource.jdbc.PoolingDataSource;

/**
 */
@SuppressWarnings("unchecked")
public class PostgresqlPersistenceManagerLoader extends AbstractPersistenceManagerLoader {

	public PostgresqlPersistenceManagerLoader(BundleContext bundleContext, StoreDesc sdesc, File[] baseDirs, Map props, TransactionService ts) {
		super(bundleContext, sdesc, baseDirs, props, ts);
	}

	protected void setProperties() {
		m_props.put("javax.jdo.PersistenceManagerFactoryClass", "org.datanucleus.api.jdo.JDOPersistenceManagerFactory");
		m_props.put("datanucleus.rdbms.dynamicSchemaUpdates ", "true");
		m_props.put("datanucleus.storeManagerType", m_sdesc.getStore());
		m_props.put("datanucleus.metadata.validate", "true");
		m_props.put("datanucleus.autoCreateSchema", "true");
		m_props.put("datanucleus.validateTables", "true");
		m_props.put("datanucleus.TransactionType", "JTA");
		m_props.put("datanucleus.connection.resourceType", "JTA");
		m_props.put("datanucleus.jtaLocator", m_transactionService.getJtaLocator());
		m_props.put("datanucleus.validateConstraints", "false");
		m_props.put("datanucleus.plugin.pluginRegistryClassName", "org.ms123.common.nucleus.OsgiPluginRegistry");
	}

	protected void setDataSources() {
		if( m_transactionService.getJtaLocator().equals("jotm")){
			PGXADataSource xa = new PGXADataSource();
			xa.setUser("postgres");
			xa.setServerName("localhost");
			xa.setDatabaseName(m_sdesc.getDatabaseName());
			BasicManagedDataSource b = new BasicManagedDataSource();
			b.setTransactionManager(m_transactionService.getTransactionManager());
			b.setXaDataSourceInstance(xa);
			m_props.put("datanucleus.ConnectionFactory", b);
		}else{
			PoolingDataSource ds = new PoolingDataSource();
			ds.setClassName("org.postgresql.xa.PGXADataSource");
			ds.setUniqueName(m_sdesc.toString());
			ds.setMaxPoolSize(15);
			ds.setAllowLocalTransactions(true);
			ds.setTestQuery("SELECT 1");
			ds.getDriverProperties().setProperty("user", "postgres");
			ds.getDriverProperties().setProperty("serverName", "localhost");    
			m_props.put("datanucleus.ConnectionFactory", ds);
		}

		// nontx
		PGPoolingDataSource pd = new PGPoolingDataSource();
		pd.setUser("postgres");
		pd.setServerName("localhost");
		pd.setDatabaseName(m_sdesc.getDatabaseName());
		m_props.put("datanucleus.ConnectionFactory2", pd);
	}
}
