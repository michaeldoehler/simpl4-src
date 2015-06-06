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

import bitronix.tm.resource.jdbc.PoolingDataSource;
import java.io.File;
import java.util.*;
import javax.jdo.Extent;
import javax.jdo.JDOEnhancer;
import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.jdo.spi.*;
import javax.jdo.Transaction;
import org.datanucleus.store.rdbms.datasource.dbcp.managed.*;
import org.ms123.common.libhelper.FileSystemClassLoader;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.system.tm.TransactionService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.postgresql.ds.PGPoolingDataSource;
import org.postgresql.xa.PGXADataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
@SuppressWarnings("unchecked")
public class PostgresqlPersistenceManagerLoader extends AbstractPersistenceManagerLoader {
	private PoolingDataSource m_poolingDataSource;

	public PostgresqlPersistenceManagerLoader(BundleContext bundleContext, StoreDesc sdesc, File[] baseDirs, ClassLoader aidClassLoader,Map props, TransactionService ts) {
		super(bundleContext, sdesc, baseDirs, aidClassLoader,props, ts);
	}

	protected void setProperties() {
		m_props.put("javax.jdo.PersistenceManagerFactoryClass", "org.datanucleus.api.jdo.JDOPersistenceManagerFactory");
		m_props.put("datanucleus.rdbms.dynamicSchemaUpdates ", "true");
		m_props.put("datanucleus.storeManagerType", m_sdesc.getStore());
		m_props.put("datanucleus.metadata.validate", "true");
		m_props.put("datanucleus.schema.autoCreateAll", "true");
		m_props.put("datanucleus.schema.validateTables", "true");
		m_props.put("datanucleus.TransactionType", "JTA");
		m_props.put("datanucleus.connection.resourceType", "JTA");
		m_props.put("datanucleus.jtaLocator", m_transactionService.getJtaLocator());
		m_props.put("datanucleus.schema.validateConstraints", "false");
		m_props.put("datanucleus.plugin.pluginRegistryClassName", "org.ms123.common.nucleus.OsgiPluginRegistry");
	}

	protected void setDataSources() {
		debug("OpenPostgresql:"+m_sdesc);
		if( m_transactionService.getJtaLocator().equals("jotm")){
			PGXADataSource xa = new PGXADataSource();
			xa.setUser("postgres");
			xa.setServerName(m_sdesc.getDatabaseHost());
			xa.setDatabaseName(m_sdesc.getDatabaseName());
			BasicManagedDataSource b = new BasicManagedDataSource();
			b.setTransactionManager(m_transactionService.getTransactionManager());
			b.setXaDataSourceInstance(xa);
			m_props.put("datanucleus.ConnectionFactory", b);
		}else{
			PoolingDataSource ds = new PoolingDataSource();
			ds.setClassName("org.postgresql.xa.PGXADataSource");
			ds.setUniqueName(m_sdesc.toString());
			ds.setMaxPoolSize(25);
			ds.setAllowLocalTransactions(true);
			ds.setEnableJdbc4ConnectionTest(true);
			ds.setTestQuery(null);
			ds.getDriverProperties().put("databaseName", m_sdesc.getDatabaseName());
			ds.getDriverProperties().setProperty("user", "postgres");
			ds.getDriverProperties().setProperty("serverName", m_sdesc.getDatabaseHost());    
			m_props.put("datanucleus.ConnectionFactory", ds);
			m_poolingDataSource=ds;
		}

		// nontx
		PGPoolingDataSource pd = new PGPoolingDataSource();
		pd.setUser("postgres");
		pd.setServerName(m_sdesc.getDatabaseHost());
		pd.setDatabaseName(m_sdesc.getDatabaseName());
		m_props.put("datanucleus.ConnectionFactory2", pd);
	}
	public synchronized void close() {
		debug("ClosePostgresql:"+m_sdesc);
		super.close();
		if(m_poolingDataSource != null){
			m_poolingDataSource.close();
		}
	}
	public String toString(){
		return "[PostgresqlPersistenceManagerLoader:"+m_sdesc+"]";
	}
	protected void debug(String msg) {
		m_logger.debug(msg);
	}
	protected void info(String msg) {
		System.out.println(msg);
		m_logger.info(msg);
	}
	private static final org.slf4j.Logger m_logger = org.slf4j.LoggerFactory.getLogger(PostgresqlPersistenceManagerLoader.class);
}
