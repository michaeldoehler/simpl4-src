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
import org.ms123.common.libhelper.BundleDelegatingClassLoader;
import org.ms123.common.libhelper.ClassLoaderWrapper;
import org.datanucleus.store.schema.SchemaAwareStoreManager;
import javax.jdo.spi.*;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.system.tm.TransactionService;

/**
 */
@SuppressWarnings("unchecked")
public abstract class AbstractPersistenceManagerLoader {

	protected PersistenceManagerFactory m_pmf;

	protected JDOEnhancer m_enhancer;

	protected ClassLoader m_classLoader;

	protected TransactionService m_transactionService;

	protected StoreDesc m_sdesc;

	protected BundleContext m_bundleContext;
	protected ClassLoader  m_aidClassLoader;

	protected File[] m_baseDirs;

	protected Map m_props;

	public AbstractPersistenceManagerLoader(BundleContext bundleContext, StoreDesc sdesc, File[] baseDirs, ClassLoader aidClassLoader, Map props, TransactionService ts) {
		m_transactionService = ts;
		m_sdesc = sdesc;
		m_bundleContext = bundleContext;
		m_baseDirs = baseDirs;
		m_aidClassLoader = aidClassLoader;
		m_props = props;
		init();
		setProperties();
		setDataSources();
		initFactory();
		dbSpecific();
	}

	protected abstract void setProperties();

	protected void init() {
	}

	protected void setDataSources() {
	}

	protected void initFactory() {
		org.ms123.common.nucleus.OsgiPluginRegistry.setBundleContext(m_bundleContext);
		ClassLoader previous = Thread.currentThread().getContextClassLoader();
		try {
			ClassLoader bundleDelegatingCL = new BundleDelegatingClassLoader(m_bundleContext.getBundle());

			ClassLoaderWrapper wrapCL = new ClassLoaderWrapper(m_aidClassLoader, bundleDelegatingCL);

			File[] locations = new File[1];
			locations[0] = new File(m_baseDirs[0], "classes");
			String[] includePattern = new String[1];
			includePattern[0] = "^"+m_sdesc.getNamespace()+"\\..*";
			FileSystemClassLoader filesystemCL = new FileSystemClassLoader(wrapCL, locations, includePattern);

			m_classLoader = filesystemCL;
			ClassLoaderWrapper contextWrappweCL = new ClassLoaderWrapper(bundleDelegatingCL, PostgresqlPersistenceManagerLoader.class.getClassLoader(), PersistenceManagerFactory.class.getClassLoader(), previous);
			Thread.currentThread().setContextClassLoader(contextWrappweCL);
			try {
				m_pmf = JDOHelper.getPersistenceManagerFactory(m_props, bundleDelegatingCL);
			} catch (Throwable e) {
				throw new RuntimeException("Cannot load PersistenceManagerFactory", e);
			}
			((org.datanucleus.api.jdo.JDOPersistenceManagerFactory) m_pmf).setPrimaryClassLoader(filesystemCL);
			Properties p = new Properties();
			p.setProperty("datanucleus.plugin.pluginRegistryClassName", "org.ms123.common.nucleus.OsgiPluginRegistry");
			//m_enhancer = new org.datanucleus.jdo.JDODataNucleusEnhancer(p);
		} finally {
			Thread.currentThread().setContextClassLoader(previous);
		}
	}

	public void dbSpecific(){
	}

	public JDOEnhancer getEnhancer() {
		ClassLoader previous = Thread.currentThread().getContextClassLoader();
		try {
			ClassLoader bundleDelegatingCL = new BundleDelegatingClassLoader(m_bundleContext.getBundle());
			Thread.currentThread().setContextClassLoader(bundleDelegatingCL);
			Properties p = new Properties();
			p.setProperty("datanucleus.plugin.pluginRegistryClassName", "org.ms123.common.nucleus.OsgiPluginRegistry");
			return new org.datanucleus.api.jdo.JDOEnhancer(p);
		} finally {
			Thread.currentThread().setContextClassLoader(previous);
		}
	}

	protected void deleteSchema(SchemaAwareStoreManager ssm,Set<String> classes,Properties props) {
		ClassLoader previous = Thread.currentThread().getContextClassLoader();
		try {
System.out.println("deleteSchema:"+classes+"/"+props);
			ClassLoader bundleDelegatingCL = new BundleDelegatingClassLoader(m_bundleContext.getBundle());
			File[] locations = new File[1];
			locations[0] = new File(m_baseDirs[0], "classes");
			FileSystemClassLoader filesystemCL = new FileSystemClassLoader(bundleDelegatingCL, locations);
			Thread.currentThread().setContextClassLoader(filesystemCL);
			try {
				ssm.deleteSchemaForClasses(classes,props);	
				//ssm.createSchema(classes,props);	
			} catch (Throwable e) {
				throw new RuntimeException("Cannot delete Schema", e);
			}
		} finally {
			Thread.currentThread().setContextClassLoader(previous);
		}
	}

	public ClassLoader getClassLoader() {
		return m_classLoader;
	}

	//public JDOEnhancer getEnhancer() {
	//	return m_enhancer;
	//}

	public synchronized void deregister() {
		if (m_classLoader != null) {
			JDOImplHelper implHelper = JDOImplHelper.getInstance();
			Collection registeredClasses = implHelper.getRegisteredClasses();
			implHelper.unregisterClasses(m_classLoader);
			registeredClasses = implHelper.getRegisteredClasses();
		}
	}

	public synchronized void close() {
		m_pmf.close();
		if (m_classLoader != null) {
			JDOImplHelper implHelper = JDOImplHelper.getInstance();
			Collection registeredClasses = implHelper.getRegisteredClasses();
			implHelper.unregisterClasses(m_classLoader);
			registeredClasses = implHelper.getRegisteredClasses();
			((FileSystemClassLoader) m_classLoader).setInvalid();
		}
	}

	public PersistenceManagerFactory getPersistenceManagerFactory() {
		return m_pmf;
	}
}
