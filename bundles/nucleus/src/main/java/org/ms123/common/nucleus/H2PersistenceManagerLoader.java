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
import java.sql.Statement;
import java.sql.Connection;
import javax.sql.DataSource;
import org.datanucleus.store.rdbms.datasource.dbcp.managed.*;
import org.h2.jdbcx.JdbcDataSource;
import org.h2.jdbcx.JdbcConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.system.TransactionService;
import bitronix.tm.resource.jdbc.PoolingDataSource;

/**
 */
@SuppressWarnings("unchecked")
public class H2PersistenceManagerLoader extends AbstractPersistenceManagerLoader {

	private String m_baseDir;

	private BasicManagedDataSource m_basicMangedDataSource;
	private PoolingDataSource m_poolingDataSource;

	private JdbcConnectionPool m_nonTxPool;

	public H2PersistenceManagerLoader(BundleContext bundleContext, StoreDesc sdesc, File[] baseDirs, Map props, TransactionService ts) {
		super(bundleContext, sdesc, baseDirs, props, ts);
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
		m_props.put("datanucleus.autoCreateSchema", "true");
		m_props.put("datanucleus.validateTables", "false");
		m_props.put("datanucleus.TransactionType", "JTA");
		m_props.put("datanucleus.connection.resourceType", "JTA");
		m_props.put("datanucleus.jtaLocator", m_transactionService.getJtaLocator());
		m_props.put("datanucleus.validateConstraints", "false");
		//		m_props.put("datanucleus.identifier.case", "PreserveCase");
		m_props.put("datanucleus.plugin.pluginRegistryClassName", "org.ms123.common.nucleus.OsgiPluginRegistry");
	}

	protected synchronized void setDataSources() {
		debug("OpenH2:"+m_sdesc);
		boolean b_script = false;
		File fDB1 = new File(m_baseDir,"dbh2.h2.db");
		File fDB2 = new File(m_baseDir,"dbh2.mv.db");
		File fLock = new File(m_baseDir,"swlock");
		if( !fLock.exists() || (!fDB1.exists() && !fDB2.exists()) ){
			File fScript = new File(m_baseDir,"script.sql");
			if( fScript.exists()){
				b_script=true;
				if( fDB1.exists()){
					fDB1.delete();
				}
				if( fDB2.exists()){
					fDB2.delete();
				}
			}
		}
		if( fLock.exists()) fLock.delete();
		debug("setDataSources:"+m_baseDir+","+b_script);

		Object b = null;
		if( m_transactionService.getJtaLocator().equals("jotm")){
			b = getBasicManagedDatasource();
		}else{
			b = getPoolingDataSource();
		}
		m_props.put("datanucleus.ConnectionFactory", b);

		// nontx
		JdbcDataSource pd = new JdbcDataSource();
		pd.setUser("sa");
		pd.setPassword("sa");
		pd.setURL("jdbc:h2:file:" + m_baseDir + "/dbh2;TRACE_LEVEL_FILE=2;TRACE_LEVEL_SYSTEM_OUT=1;CACHE_SIZE=33107;MV_STORE=TRUE;MVCC=TRUE;DB_CLOSE_ON_EXIT=false;");
		JdbcConnectionPool pool = JdbcConnectionPool.create(pd);
		m_nonTxPool = pool;
		m_props.put("datanucleus.ConnectionFactory2", pool);
		if(b_script){
			createDBFromScript(pool);
		}else{
			createScript(pool);
		}
	}
	private BasicManagedDataSource getBasicManagedDatasource(){
		JdbcDataSource xa = new JdbcDataSource();
		xa.setUser("sa");
		xa.setPassword("sa");
		xa.setURL("jdbc:h2:file:" + m_baseDir + "/dbh2;TRACE_LEVEL_FILE=2;TRACE_LEVEL_SYSTEM_OUT=1;CACHE_SIZE=33107");
		BasicManagedDataSource b = new BasicManagedDataSource();
		m_basicMangedDataSource = b;
		b.setTransactionManager(m_transactionService.getTransactionManager());
		b.setXaDataSourceInstance(xa);
		return b;
	}

	private PoolingDataSource getPoolingDataSource(){
		PoolingDataSource ds = new PoolingDataSource();
		ds.setClassName("org.h2.jdbcx.JdbcDataSource");
		ds.setUniqueName(m_sdesc.toString());
		ds.setMaxPoolSize(15);
		ds.setAllowLocalTransactions(true);
		ds.setTestQuery("SELECT 1");
		ds.getDriverProperties().setProperty("user", "sa");
		ds.getDriverProperties().setProperty("password", "sa");
		ds.getDriverProperties().setProperty("URL", "jdbc:h2:file:" + m_baseDir + "/dbh2;TRACE_LEVEL_FILE=2;TRACE_LEVEL_SYSTEM_OUT=1;MV_STORE=TRUE;MVCC=TRUE;CACHE_SIZE=33107;DB_CLOSE_ON_EXIT=FALSE");    
		m_poolingDataSource = ds;
		return ds;
	}

	private void createScript(JdbcConnectionPool pd){
		debug("createScript:"+m_baseDir);
		Connection	conn = null;
		try{
			conn = pd.getConnection();
			Statement stat = conn.createStatement();
			stat.execute("SCRIPT NOPASSWORDS TO '"+m_baseDir+"/script.sql'");
			File f = new File(m_baseDir,"swlock");
			f.createNewFile();
		}catch(Exception e){
			e.printStackTrace();
		} finally {
			try {
				conn.close();
			} catch (Exception e) {
			}
		}
	}
	private void createDBFromScript(JdbcConnectionPool pool){
		debug("createDBFromScript:"+m_baseDir);
		Connection	conn = null;
		try{
			conn = pool.getConnection();
			Statement stat = conn.createStatement();
			stat.execute("RUNSCRIPT FROM '"+m_baseDir+"/script.sql'");
			File f = new File(m_baseDir,"swlock");
			f.createNewFile();
		}catch(Exception e){
			e.printStackTrace();
		} finally {
			try {
				conn.close();
			} catch (Exception e) {
			}
		}
	}

	public synchronized void close() {
		debug("CloseH2:"+m_baseDir);
		super.close();
		Connection conn = null;
		try {
			conn = m_nonTxPool.getConnection();
			Statement stat = conn.createStatement();
			stat.execute("SCRIPT NOPASSWORDS TO '"+m_baseDir+"/script.sql'");
			stat.execute("shutdown compact");
			new File(m_baseDir,"swlock").delete();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if(m_nonTxPool != null){
					m_nonTxPool.dispose();
				}
				if(m_basicMangedDataSource != null){
					m_basicMangedDataSource.close();
				}
				if(m_poolingDataSource != null){
					m_poolingDataSource.close();
				}
			} catch (Exception e) {
			}
		}
	}
	public String toString(){
		return "[H2PersistenceManagerLoader:"+m_baseDir+"]";
	}
	protected void debug(String msg) {
		//System.out.println(msg);
		m_logger.debug(msg);
	}
	protected void info(String msg) {
		System.out.println(msg);
		m_logger.info(msg);
	}
	private static final org.slf4j.Logger m_logger = org.slf4j.LoggerFactory.getLogger(H2PersistenceManagerLoader.class);
}
