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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.system.tm.TransactionService;
import bitronix.tm.resource.jdbc.PoolingDataSource;
import org.datanucleus.store.schema.SchemaAwareStoreManager;
import org.datanucleus.PersistenceNucleusContext;
import org.datanucleus.store.cassandra.CassandraSchemaHandler;
import org.datanucleus.store.cassandra.CassandraStoreManager;
import javax.jdo.PersistenceManagerFactory;
import org.datanucleus.api.jdo.JDOPersistenceManagerFactory;
/**
 */
@SuppressWarnings("unchecked")
public class CassandraPersistenceManagerLoader extends AbstractPersistenceManagerLoader {

	private String m_baseDir;

	public CassandraPersistenceManagerLoader(BundleContext bundleContext, StoreDesc sdesc, File[] baseDirs, ClassLoader aidClassLoader,Map props, TransactionService ts) {
		super(bundleContext, sdesc, baseDirs, aidClassLoader, props, ts);
	}

	protected void init() {
	}

	protected void setProperties() {
		setTxProperties();
	}
	protected void setTxProperties() {
		m_props.put("javax.jdo.PersistenceManagerFactoryClass", "org.datanucleus.api.jdo.JDOPersistenceManagerFactory");
		m_props.put("datanucleus.rdbms.dynamicSchemaUpdates ", "true");
		m_props.put("datanucleus.metadata.validate", "false");
		m_props.put("datanucleus.schema.autoCreateAll", "true");
		m_props.put("datanucleus.schema.validateConstraints", "false");
		m_props.put("datanucleus.plugin.pluginRegistryClassName", "org.ms123.common.nucleus.OsgiPluginRegistry");
		m_props.put("datanucleus.ConnectionURL", "cassandra");
System.out.println("SettingSchema:"+m_sdesc.getNamespace());
		m_props.put("datanucleus.mapping.Schema", m_sdesc.getNamespace());
	}

	public void dbSpecific( ){
			JDOPersistenceManagerFactory pmf = (JDOPersistenceManagerFactory)getPersistenceManagerFactory();
			PersistenceNucleusContext ctx = pmf.getNucleusContext();
			CassandraStoreManager csm = (CassandraStoreManager)ctx.getStoreManager();
			CassandraSchemaHandler csh = new CassandraSchemaHandler(csm);
			try{
				csh.createSchema(m_sdesc.getNamespace(), null, null);
			}catch(Exception e){
				e.printStackTrace();
			}
	}

	protected synchronized void setDataSources() {
	}

	public synchronized void close() {
		super.close();
	}
	public String toString(){
		return "[CassandraPersistenceManagerLoader:"+m_baseDir+"]";
	}
	protected void debug(String msg) {
		//System.out.println(msg);
		m_logger.debug(msg);
	}
	protected void info(String msg) {
		System.out.println(msg);
		m_logger.info(msg);
	}
	private static final org.slf4j.Logger m_logger = org.slf4j.LoggerFactory.getLogger(CassandraPersistenceManagerLoader.class);
}
