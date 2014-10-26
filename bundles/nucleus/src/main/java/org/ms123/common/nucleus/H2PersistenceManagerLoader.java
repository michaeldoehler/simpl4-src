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
import org.datanucleus.store.rdbms.datasource.dbcp.managed.*;
import org.h2.jdbcx.JdbcDataSource;
import org.h2.jdbcx.JdbcConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.system.TransactionService;

/**
 */
@SuppressWarnings("unchecked")
public class H2PersistenceManagerLoader extends AbstractPersistenceManagerLoader {

	private String m_baseDir;

	private BasicManagedDataSource m_ds1;

	private JdbcDataSource m_ds2;

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
		boolean b_script = false;
		JdbcDataSource xa = new JdbcDataSource();
		File fDB = new File(m_baseDir,"dbh2.h2.db");
		File fLock = new File(m_baseDir,"swlock");
		if( !fLock.exists() || !fDB.exists() ){
			File fScript = new File(m_baseDir,"script.sql");
			if( fScript.exists()){
				b_script=true;
				if( fDB.exists()){
					fDB.delete();
				}
			}
		}
		if( fLock.exists()) fLock.delete();
		debug("setDataSources:"+m_baseDir+","+b_script);
		xa.setUser("sa");
		xa.setPassword("sa");
		xa.setURL("jdbc:h2:file:" + m_baseDir + "/dbh2;TRACE_LEVEL_FILE=2;TRACE_LEVEL_SYSTEM_OUT=1;CACHE_SIZE=33107");
		BasicManagedDataSource b = new BasicManagedDataSource();
		m_ds1 = b;
		b.setTransactionManager(m_transactionService.getTransactionManager());
		b.setXaDataSourceInstance(xa);
		m_props.put("datanucleus.ConnectionFactory", b);
		JdbcDataSource pd = new JdbcDataSource();
		// nontx
		m_ds2 = pd;
		pd.setUser("sa");
		pd.setPassword("sa");
		pd.setURL("jdbc:h2:file:" + m_baseDir + "/dbh2;TRACE_LEVEL_FILE=2;TRACE_LEVEL_SYSTEM_OUT=1;CACHE_SIZE=33107");
		JdbcConnectionPool pool = JdbcConnectionPool.create(pd);
		m_props.put("datanucleus.ConnectionFactory2", pool);
		if(b_script){
			createDBFromScript(pd);
		}else{
			createScript(pd);
		}
	}

	private void createScript(JdbcDataSource pd){
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
	private void createDBFromScript(JdbcDataSource pd){
		debug("createDBFromScript:"+m_baseDir);
		Connection	conn = null;
		try{
			conn = pd.getConnection();
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
			conn = m_ds2.getConnection();
			Statement stat = conn.createStatement();
			stat.execute("SCRIPT NOPASSWORDS TO '"+m_baseDir+"/script.sql'");
			m_ds1.close();
			stat.execute("shutdown compact");
			new File(m_baseDir,"swlock").delete();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				conn.close();
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
