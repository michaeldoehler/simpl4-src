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

import java.util.*;
import java.io.File;
import java.io.FileFilter;
import java.sql.Statement;
import java.sql.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Bundle;
import org.ms123.common.nucleus.PostgresqlPersistenceManagerLoader;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.JDOEnhancer;
import flexjson.*;
import org.datanucleus.*;
import org.datanucleus.api.jdo.*;
import org.ms123.common.libhelper.Inflector;
import javax.transaction.UserTransaction;
import javax.transaction.TransactionManager;
import javax.transaction.Status;
import aQute.bnd.annotation.metatype.*;
import aQute.bnd.annotation.component.*;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.ms123.common.rpc.PName;
import org.ms123.common.rpc.POptional;
import org.ms123.common.rpc.RpcException;
import org.eclipse.jgit.storage.file.*;
import org.eclipse.jgit.util.*;
import org.datanucleus.store.schema.SchemaTool;
import org.datanucleus.store.schema.SchemaAwareStoreManager;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.ms123.common.system.TransactionService;
import org.ms123.common.git.GitService;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.libhelper.FileSystemClassLoader;
import org.ms123.common.libhelper.BundleDelegatingClassLoader;
import static org.apache.commons.io.FileUtils.readFileToString;
import org.ms123.common.rpc.PDefaultBool;
import static org.ms123.common.rpc.JsonRpcServlet.ERROR_FROM_METHOD;
import static org.ms123.common.rpc.JsonRpcServlet.INTERNAL_SERVER_ERROR;
import static org.ms123.common.rpc.JsonRpcServlet.PERMISSION_DENIED;

@SuppressWarnings("unchecked")
@Component(enabled = true, configurationPolicy = ConfigurationPolicy.optional, immediate = true, properties = { "rpc.prefix=nucleus" })
public class NucleusServiceImpl implements org.ms123.common.nucleus.api.NucleusService,EventHandler {

	protected Inflector m_inflector = Inflector.getInstance();

	private Map<StoreDesc, AbstractPersistenceManagerLoader> m_loaders = new HashMap<StoreDesc, AbstractPersistenceManagerLoader>();

	private BundleContext m_bc;
	private TransactionService m_transactionService;
	private ClassLoader m_aidClassLoader;

	private List<AbstractPersistenceManagerLoader> m_openList = new ArrayList();

	final static String[] topics = new String[] {
		"namespace/installed",
		"namespace/created",
		"namespace/preCommit",
		"namespace/preUpdate",
		"namespace/postUpdate",
		"namespace/preGet",
		"namespace/pull",
		"namespace/deleted"
	};
	protected void activate(BundleContext bundleContext, Map<?, ?> props) {
		try {
			m_bc = bundleContext;
			debug("NucleusServiceImpl.activate:" + bundleContext);
			Dictionary d = new Hashtable();
			d.put(EventConstants.EVENT_TOPIC, topics);
			m_bc.registerService(EventHandler.class.getName(), this, d);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void deactivate() throws Exception {
		System.out.println("NucleusServiceImpl.deactivate");
		closeAll();
	}
	public void handleEvent(Event event) {
		debug("NucleusServiceImpl.Event: " + event);
		try{
			String ns = (String)event.getProperty("namespace");
			if( "namespace/deleted".equals(event.getTopic())){
				if( ns.startsWith("global")){
					close( StoreDesc.getGlobalData());
				}else{
					close( StoreDesc.getNamespaceData(ns));
					close( StoreDesc.getNamespaceMeta(ns));
				}
			}
			if( "namespace/preCommit".equals(event.getTopic())){
				if( ns.startsWith("global")){
					close( StoreDesc.getGlobalData());
				}else{
					close( StoreDesc.getNamespaceData(ns));
					close( StoreDesc.getNamespaceMeta(ns));
				}
			}
			if( "namespace/preUpdate".equals(event.getTopic())){
				if( ns.startsWith("global")){
					close( StoreDesc.getGlobalData());
				}else{
					close( StoreDesc.getNamespaceData(ns));
					close( StoreDesc.getNamespaceMeta(ns));
				}
			}
			if( "namespace/preGet".equals(event.getTopic())){
				closeAll();
			}
			StoreDesc.init();
		}catch(Exception e){
			e.printStackTrace();
		}finally{
		}
	}
	public UserTransaction getUserTransaction() {
		UserTransaction utx = m_transactionService.getUserTransaction();
		return utx;
	}

	public TransactionManager getTransactionManager() {
		TransactionManager tm = m_transactionService.getTransactionManager();
		return tm;
	}

	private synchronized void createFactory(StoreDesc sdesc) {
		debug("createFactory:" + sdesc+"/"+m_loaders);
		if( sdesc == null) return;
		try {
			if (m_openList.size() > 0) {
				for (AbstractPersistenceManagerLoader p : m_openList) {
					p.close();
				}
				m_openList.clear();
			}
			if( m_aidClassLoader == null){
				createAidClassLoader(sdesc.getBaseDir());
			}
			Map<String, Object> props = new HashMap<String, Object>();
			File[] baseDirs = new File[1];
			baseDirs[0] = sdesc.getBaseDir();
			AbstractPersistenceManagerLoader pml = null;
			if (sdesc.getStore().equals(StoreDesc.STORE_RDBMS)) {
				if (sdesc.getVendor().equals(StoreDesc.VENDOR_PG)) {
					pml = new PostgresqlPersistenceManagerLoader(m_bc, sdesc, baseDirs, m_aidClassLoader,props, m_transactionService);
				} else if (sdesc.getVendor().equals(StoreDesc.VENDOR_HSQL)) {
					pml = new HsqldbPersistenceManagerLoader(m_bc, sdesc, baseDirs, m_aidClassLoader,props, m_transactionService);
				} else if (sdesc.getVendor().equals(StoreDesc.VENDOR_H2)) {
					pml = new H2PersistenceManagerLoader(m_bc, sdesc, baseDirs, m_aidClassLoader,props, m_transactionService);
				}
			}
			if (sdesc.getStore().equals(StoreDesc.STORE_FILE)) {
				pml = new FilePersistenceManagerLoader(m_bc, sdesc, baseDirs, m_aidClassLoader,props, m_transactionService);
			}
			debug("createFactory.pml:" + pml);
			if (pml == null)
				throw new RuntimeException("NucleusServiceImpl.no_loader_for:" + sdesc + " found");
			m_loaders.put(sdesc, pml);
		} catch (Throwable e) {
			throw new RuntimeException("NucleusServiceImpl.createFactory:Cannot create Factory", e);
		}
	}

	public synchronized void closeAll() {
		debug("CloseAll");
		Iterator<StoreDesc> it = m_loaders.keySet().iterator();
		while (it.hasNext()) {
			StoreDesc sd = it.next();
			debug("\tCloseAll:sd:"+sd);
			try{
					_close(sd);
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}

	public synchronized void close(StoreDesc sdesc) {
		debug("CLOSE:sd1:"+sdesc);
		Iterator<StoreDesc> it = m_loaders.keySet().iterator();
		while (it.hasNext()) {
			StoreDesc sd = it.next();
			debug("\tCLOSE:sd2:"+sd);
			try{
				if (sdesc.getNamespace().equals(sd.getNamespace()) && sdesc.getPack().equals(sd.getPack())) {
					_close(sd);
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}

	private void _close(StoreDesc sdesc) {
		debug("Nucleus._close:" + sdesc);
		AbstractPersistenceManagerLoader pml = m_loaders.get(sdesc);
		if (pml != null) {
			try {
				int count=0;
				while(true){	
					debug("NucleusServiceImpl._close:status:"+getUserTransaction().getStatus()+"/"+count);
					if (getUserTransaction().getStatus() != Status.STATUS_ACTIVE) {
						pml.close();
						pml = null;
						break;
					} 
					count++;
					Thread.sleep(2000L);
					if( count>20) break;
				}
				if(pml != null){
					m_openList.add(pml);
					debug("close:in tx:"+m_openList);
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				m_loaders.put(sdesc, null);
			}
		}
	}

	public Class getClass(StoreDesc sdesc, String className) {
		try {
			className = m_inflector.getClassName(className);
			String pack = sdesc.getJavaPackage();
			try {
				ClassLoader cl = getClassLoader(sdesc);
				//System.out.println("GetClass:("+sdesc+"),"+cl+"/"+pack+"."+className);				
				return cl.loadClass(pack + "." + className);
			} catch (Exception e1) {
				debug("NucleusServiceImpl.getClass:" + sdesc + "/pack:" + pack + "/cn:" + className + " not found, trying common_ns");
				try{
					return getClassLoader(sdesc).loadClass(StoreDesc.PACK_AID + "." + className);
				}catch(Exception e2){
					throw new RuntimeException("NucleusServiceImpl.getClass(:"+className+")",e1);
				}
			}
		} catch (Exception e) {
			if (e instanceof RuntimeException)
				throw (RuntimeException) e;
			throw new RuntimeException("NucleusServiceImpl.getClass(" + sdesc + "/" + className + "):", e);
		}
	}

	public java.sql.Connection getJdbcConnection(StoreDesc sdesc) {
		if (m_loaders.get(sdesc) == null) {
			createFactory(sdesc);
		}
		AbstractPersistenceManagerLoader pml = m_loaders.get(sdesc);
		javax.jdo.PersistenceManager pm = pml.getPersistenceManagerFactory().getPersistenceManager();
		return (java.sql.Connection) pm.getDataStoreConnection().getNativeConnection();
	}

	public synchronized PersistenceManagerFactory getPersistenceManagerFactory(StoreDesc sdesc) {
		if (m_loaders.get(sdesc) == null) {
			createFactory(sdesc);
		}
		return m_loaders.get(sdesc).getPersistenceManagerFactory();
	}

	public ClassLoader getClassLoader(StoreDesc sdesc) {
		if (m_loaders.get(sdesc) == null) {
			createFactory(sdesc);
		}
		return m_loaders.get(sdesc).getClassLoader();
	}

	public JDOEnhancer getEnhancer(StoreDesc sdesc) {
		AbstractPersistenceManagerLoader pml = m_loaders.get(sdesc);
		if (pml == null) {
			createFactory(sdesc);
			pml = m_loaders.get(sdesc);
		}
		return pml.getEnhancer();
	}

	private void createAidClassLoader(File baseDir){
		File[] locations = new File[1];
		locations[0] = new File(baseDir, "classes");
		String[] includePattern = new String[1];
		includePattern[0] = "^aid\\..*";
		ClassLoader bundleDelegatingCL = new BundleDelegatingClassLoader(m_bc.getBundle());
		m_aidClassLoader = new FileSystemClassLoader(bundleDelegatingCL, locations, includePattern);
	}

	private void printMap(String header, Map map) {
		debug("----->" + header);
		if (map != null) {
			Iterator it = map.keySet().iterator();
			while (it.hasNext()) {
				Object key = it.next();
				debug("\tkey=" + key + "=" + map.get(key));
			}
		}
		debug("--------------------------------------------------------");
	}

	/*BEGIN JSON-RPC-API*/
	@RequiresRoles("admin")
	public String schemaTool(
			@PName(StoreDesc.STORE_ID) String storeId,
			@PName("op") String op,
			@PName("classes") @POptional Set<String> classes,
			@PName("dry") @POptional @PDefaultBool(false)  Boolean dry
		) throws RpcException {
		try {
			closeAll();
			StoreDesc sdesc = StoreDesc.get(storeId);
			if (m_loaders.get(sdesc) == null) {
				createFactory(sdesc);
			}
			return _schemaOp(sdesc,op,classes,dry);
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "NucleusServiceImpl.schemaTool", e);
		}
	}
	/*BEGIN JSON-RPC-API*/
	private synchronized String _schemaOp(StoreDesc sdesc,String op,Set<String> classes, boolean dry) throws Exception{
		debug("\t_schemaOp:sd:"+sdesc+" -> "+op+"|classes:"+classes);
		AbstractPersistenceManagerLoader pml = m_loaders.get(sdesc);
		if (pml != null) {
			SchemaTool schemaTool = new SchemaTool();	
			JDOPersistenceManagerFactory pmf = (JDOPersistenceManagerFactory)pml.getPersistenceManagerFactory();
			debug("_schemaOp.pmf:"+pmf+"/"+pml);
			NucleusContext ctx = pmf.getNucleusContext();
			SchemaAwareStoreManager ssm = (SchemaAwareStoreManager)ctx.getStoreManager();
			if(op.equals("delete")){
				Properties props = new Properties();
				props.setProperty("completeDdl", "true");
        props.setProperty("autoStartTable", "true");
				File tempFile = null;
				if( dry ){
					tempFile = File.createTempFile("delete", ".ddl");
					props.setProperty("ddlFilename", tempFile.toString());
					props.setProperty("completeDdl", "true");
				}
				if( classes == null){
					classes = sdesc.getClasses();
				}else{
					Set<String> newClasses = new HashSet();
					for( String cl : classes){
						newClasses.add( sdesc.getFQN( cl ));	
					}
					classes = newClasses;
				}
				debug("\tDELETE:"+classes+"/"+tempFile+"|"+props+"|"+ssm);
				pml.deleteSchema(ssm,classes,props);
				if( dry ){
					String ddl = readFileToString(tempFile);
					debug(ddl);
					return ddl;
				}else{
					close( sdesc );
				}
			}else if( op.equals("validate")){
				schemaTool.validateSchema(ssm,classes);	
			}else if( op.equals("create")){
				Properties props = new Properties();
				File tempFile = null;
				if( dry ){
					tempFile = File.createTempFile("create", ".ddl");
					props.setProperty("ddlFilename", tempFile.toString());
					props.setProperty("completeDdl", "true");
				}
				schemaTool.createSchema(ssm,classes);	
				if( dry ){
					return readFileToString(tempFile);
				}else{
					close( sdesc );
				}
			}
		}
		return null;
	}


	public void close(
			@PName(StoreDesc.STORE_ID) String storeid) throws RpcException {
		try {
			StoreDesc sdesc = StoreDesc.get(storeid);
			close(sdesc);
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "NucleusServiceImpl.close:", e);
		}
	}

	@Reference(dynamic = true,optional=true)
	public void setTransactionService(TransactionService paramTransactionService) {
		this.m_transactionService = paramTransactionService;
		System.out.println("TransactionServiceImpl.setTransactionService:" + paramTransactionService);
	}

	protected static void debug(String msg) {
		//System.out.println(msg);
		m_logger.debug(msg);
	}
	protected void info(String msg) {
		System.out.println(msg);
		m_logger.info(msg);
	}
	private static final org.slf4j.Logger m_logger = org.slf4j.LoggerFactory.getLogger(NucleusServiceImpl.class);
}
