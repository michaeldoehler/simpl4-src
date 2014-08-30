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
package org.ms123.common.bhs;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import org.ms123.common.data.api.DataLayer;
import org.ms123.common.auth.api.AuthService;
import org.ms123.common.utils.UtilsService;
import org.ms123.common.data.api.SessionContext;
import org.ms123.common.git.GitService;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.nucleus.api.NucleusService;
import org.ms123.common.permission.api.PermissionService;
import org.ms123.common.libhelper.Inflector;

import aQute.bnd.annotation.metatype.*;
import aQute.bnd.annotation.component.*;

import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.FetchPlan;
import javax.jdo.PersistenceManager;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.Extent;
import javax.jdo.Query;

import org.ms123.common.rpc.PName;
import org.ms123.common.rpc.POptional;
import org.ms123.common.rpc.RpcException;

import static org.ms123.common.rpc.JsonRpcServlet.ERROR_FROM_METHOD;
import static org.ms123.common.rpc.JsonRpcServlet.INTERNAL_SERVER_ERROR;
import static org.ms123.common.rpc.JsonRpcServlet.PERMISSION_DENIED;
import org.ms123.common.permission.api.PermissionException;
import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;
import org.apache.commons.beanutils.PropertyUtils;


/** BhsService implementation
 */
@SuppressWarnings("unchecked")
@Component(enabled = true, configurationPolicy = ConfigurationPolicy.optional, immediate = true, properties = { "rpc.prefix=bhs" })
public class BhsServiceImpl extends BaseBhsServiceImpl implements BhsService{
	private static final Logger m_logger = LoggerFactory.getLogger(BhsServiceImpl.class);

	private static final String BOM_ENTITY = "bom";
	protected JSONSerializer m_js = new JSONSerializer();
	public BhsServiceImpl(){
		m_logger.info("BhsServiceImpl construct");
	}

	protected void activate(BundleContext bundleContext, Map props) {
		System.out.println("BhsServiceImpl.activate.props:" + props);
		try {
			m_logger.info("BhsServiceImpl.activate -->");
			Bundle b = bundleContext.getBundle();
		} catch (Exception e) {
			e.printStackTrace();
		}
		m_js.prettyPrint(true);
	}

	protected void deactivate() throws Exception {
		m_logger.info("deactivate");
		System.out.println("deactivate");
	}

	private List<Map> getAssemblyByPath(StoreDesc sdesc, String className, String path) throws Exception{
		SessionContext sessionContext = m_dataLayer.getSessionContext(sdesc);
		PersistenceManager pm = sessionContext.getPM();
		Class clazz = sessionContext.getClass(className);
		String filter = "SELECT b from "+clazz.getName()+" as b  where path.ltreeMatch('" + path + "')";
System.out.println("filter:"+filter);
		Query q = pm.newQuery("javax.jdo.query.JPQL", filter);
		Collection coll = (Collection) q.execute();
		Iterator iter = coll.iterator();
		List<Map> ret = new ArrayList();
		while (iter.hasNext()) {
			Object b = iter.next();
			Map m = new HashMap();
			Object md = PropertyUtils.getProperty(b,"masterdata");
			m.put("part", PropertyUtils.getProperty(b,"part"));
			m.put("name", PropertyUtils.getProperty(md,"name"));
			m.put("name2", PropertyUtils.getProperty(md,"name2"));
			m.put("qty", PropertyUtils.getProperty(b,"qty"));
			ret.add(m);
		} 
		return ret;
	}
	private Object getObjectByPath(PersistenceManager pm, Class clazz, String path) throws Exception{
		String filter = "path.ltreeMatch('" + path + "')";
		Extent e = pm.getExtent(clazz, true);
		Query q = pm.newQuery(e, filter);
		Collection coll = (Collection) q.execute();
		Iterator iter = coll.iterator();
		if (iter.hasNext()) {
			Object c = iter.next();
			return c;
		} else {
			throw new RuntimeException("getObjectByPath(" + path + ") not found");
		}
	}

	private Map getObjectGraph(StoreDesc sdesc, String entityName, String path, Map mapping) {
		System.out.println("getObjectGraph:entityName:" + entityName + ",path:" + path);
		Map retMap = new HashMap();
		SessionContext sessionContext = m_dataLayer.getSessionContext(sdesc);
		PersistenceManager pm = sessionContext.getPM();
		FetchPlan fp = pm.getFetchPlan();
		try {
			fp.setMaxFetchDepth(-1);
			String className = m_inflector.getClassName(entityName);
			Class clazz = sessionContext.getClass(className);
			Object objectMaster = getObjectByPath(pm, clazz, path);
System.out.println("objectMaster:"+m_js.serialize(objectMaster));
			retMap = BOMVisitor.getObjectGraph(objectMaster, sessionContext, mapping);
			retMap.put("name", "Maschine "+ path);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			pm.close();
		}
		return retMap;
	}

	/*BEGIN JSON-RPC-API*/
	public Map getBOMTree(
			@PName(StoreDesc.NAMESPACE) String namespace, 
			@PName("machine") 					String machine, 
			@PName("mapping")          @POptional Map mapping
			) throws RpcException {
		Map ret = null;
		StoreDesc sdesc = StoreDesc.getNamespaceData(namespace);
		SessionContext sessionContext = m_dataLayer.getSessionContext(sdesc);
		try {
			ret = this.getObjectGraph(sdesc, BOM_ENTITY, machine, mapping);
			return ret;
		} catch (PermissionException e) {
			throw new RpcException(ERROR_FROM_METHOD, PERMISSION_DENIED, "BhsService.getBOMTree", e);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "BhsTeamService.getBOMTree:", e);
		}
	}
	public List<Map> getAssembly(
			@PName(StoreDesc.NAMESPACE) String namespace, 
			@PName("path") 					String path 
			) throws RpcException {
		StoreDesc sdesc = StoreDesc.getNamespaceData(namespace);
		SessionContext sessionContext = m_dataLayer.getSessionContext(sdesc);
		try {
			return this.getAssemblyByPath(sdesc, BOM_ENTITY, path);
		} catch (PermissionException e) {
			throw new RpcException(ERROR_FROM_METHOD, PERMISSION_DENIED, "BhsService.getAssembly", e);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "BhsTeamService.getAssembly:", e);
		}
	}
	public void importBom(
			@PName("storeId")          String storeId,
			@PName("aukwnr")          String aukwnr
			) throws RpcException {
		try {
			_importBom(storeId, aukwnr);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "BHSService.importBom:", e);
		}
	}
	public void importMD(
			@PName("storeId")          String storeId
			) throws RpcException {
		try {
			_importMD(storeId);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "BHSService.importMD:", e);
		}
	}
	public void setTranslations(
			@PName("storeId")          String storeId
			) throws RpcException {
		try {
			_setTranslations(storeId);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "BHSService.setTranslations:", e);
		}
	}

	/*END JSON-RPC-API*/


	@Reference(target = "(kind=jdo)", dynamic = true, optional = true)
	public void setDataLayer(DataLayer dataLayer) {
		System.out.println("BhsServiceImpl.setDataLayer:" + dataLayer);
		m_dataLayer = dataLayer;
	}

	@Reference(dynamic = true, optional = true)
	public void setGitService(GitService gitService) {
		System.out.println("BhsServiceImpl.setGitService:" + gitService);
		m_gitService = gitService;
	}

	@Reference(dynamic = true)
	public void setPermissionService(PermissionService paramPermissionService) {
		this.m_permissionService = paramPermissionService;
		System.out.println("BhsServiceImpl.setPermissionService:" + paramPermissionService);
	}

	@Reference(dynamic = true)
	public void setAuthService(AuthService paramService) {
		this.m_authService = paramService;
		System.out.println("BhsServiceImpl.setAuthService:" + paramService);
	}

	@Reference(dynamic = true)
	public void setUtilsService(UtilsService paramUtilsService) {
		this.m_utilsService = paramUtilsService;
		System.out.println("BhsServiceImpl.setUtilsService:" + paramUtilsService);
	}

	@Reference(dynamic = true)
	public void setNucleusService(NucleusService paramService) {
		this.m_nucleusService = paramService;
		System.out.println("BhsServiceImpl.setNucleusService:" + paramService);
	}
}
