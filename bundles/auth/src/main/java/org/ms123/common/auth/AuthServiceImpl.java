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
package org.ms123.common.auth;

import aQute.bnd.annotation.component.*;
import aQute.bnd.annotation.metatype.*;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.Collection;
import java.util.Set;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.Extent;
import javax.jdo.Query;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.ms123.common.namespace.NamespaceService;
import org.ms123.common.libhelper.Bean2Map;
import org.ms123.common.libhelper.Inflector;
import org.ms123.common.data.api.DataLayer;
import org.ms123.common.data.api.SessionContext;
import org.ms123.common.rpc.PDefaultBool;
import org.ms123.common.rpc.PDefaultFloat;
import org.ms123.common.rpc.PDefaultInt;
import org.ms123.common.rpc.PDefaultLong;
import org.ms123.common.rpc.PDefaultString;
import org.ms123.common.rpc.PName;
import org.ms123.common.rpc.POptional;
import org.ms123.common.rpc.RpcException;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.nucleus.api.NucleusService;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.ms123.common.rpc.JsonRpcServlet.ERROR_FROM_METHOD;
import static org.ms123.common.rpc.JsonRpcServlet.INTERNAL_SERVER_ERROR;
import static org.ms123.common.rpc.JsonRpcServlet.PERMISSION_DENIED;
import org.ms123.common.auth.user.*;

/** AuthService implementation
 */
@SuppressWarnings("unchecked")
@Component(enabled = true, configurationPolicy = ConfigurationPolicy.optional, immediate = true, properties = { "rpc.prefix=auth" })
public class AuthServiceImpl implements org.ms123.common.auth.api.AuthService, Constants {

	protected Inflector m_inflector = Inflector.getInstance();

	private DataLayer m_dataLayer;

	private NamespaceService m_namespaceService;
	private String m_mainNamepace;

	private NucleusService m_nucleusService;

	public AuthServiceImpl() {
	}

	protected void activate(BundleContext bundleContext, Map<?, ?> props) {
		System.out.println("AuthServiceImpl.activate.props:" + props);
	}

	protected void deactivate() throws Exception {
		m_logger.info("deactivate");
		System.out.println("AuthServiceImpl deactivate");
	}

	private StoreDesc getStoreDesc(){
		if( m_mainNamepace == null){
			Map<String,String> branding = m_namespaceService.getBranding();
			m_mainNamepace = branding.get(NamespaceService.MAIN_NAMESPACE);
		}
		StoreDesc sdesc = null;
		if( m_mainNamepace != null){
			sdesc = StoreDesc.get(m_mainNamepace+"_user");
		}
		if( sdesc == null){
			sdesc = StoreDesc.getGlobalData();
		}
		return sdesc;
	}

	public String getAdminUser() {
		String adminuser = System.getProperty("admin_user");
		if (adminuser == null) {
			adminuser = ADMIN_USER;
		}
		return adminuser;
	}

	public Map getUserProperties( String userid) {
		try {
			debug("getUserProperties.userid:" + userid);
			StoreDesc sdesc = getStoreDesc();
			Map ret = getUserByUserid(sdesc, userid);
			debug("getUserProperties.ret:" + ret);
			return ret;
		} catch (RuntimeException e) {
			if (e.getCause() instanceof javax.jdo.JDOObjectNotFoundException) {
				throw new RuntimeException("user_not_exists:" + userid);
			} else {
				throw e;
			}
		} catch (Throwable e) {
			throw new RuntimeException(e);
		} finally {
		}
	}

	private Map getUserByUserid(StoreDesc sdesc, String id) throws Exception {
		String filter = "userid == '" + id + "'";
		debug("getUserByUserid:" + filter);
		PersistenceManager pm = m_nucleusService.getPersistenceManagerFactory(sdesc).getPersistenceManager();
		Class clazz = m_nucleusService.getClass(sdesc, m_inflector.getClassName(USER_ENTITY));
		Extent e = pm.getExtent(clazz, true);
		Query q = pm.newQuery(e, filter);
		try {
			Collection coll = (Collection) q.execute();
			Iterator iter = coll.iterator();
			if (iter.hasNext()) {
				Object obj = iter.next();
				Bean2Map b2m = new Bean2Map();
				return b2m.transform(obj, new HashMap());
			}
		} finally {
			q.closeAll();
		}
		return null;
	}

	private Map _createUser(String userid, Map data) throws Exception {
		StoreDesc sdesc = getStoreDesc();
		data.put(USER_ID, userid);
		Map ret = m_dataLayer.updateObject(data, null, null, sdesc, USER_ENTITY, userid, null, null);
		debug("RET:" + ret);
		userid = (String) data.get(USER_ID);
		String pw = (String) data.get(PASSWD);
		List groups = new ArrayList();
		Boolean created = (Boolean) ret.get("created");
		if (created != null && created) {
		} else {
		}
		return ret;
	}

	private Map _createGroup(String groupid, Map data) throws Exception {
		StoreDesc sdesc = getStoreDesc();
		Map ret = m_dataLayer.updateObject(data, null, null, sdesc, GROUP_ENTITY, groupid, null, null);
		Boolean created = (Boolean) ret.get("created");
		if (created != null && created) {
		}
		return ret;
	}

	/* BEGIN JSON-RPC-API*/
	//@RequiresRoles("admin")
	public Map getUsers(
			@PName("filter")           @POptional Map filter) throws RpcException {
		try {
			StoreDesc sdesc = getStoreDesc();
			if (filter == null) {
				filter = new HashMap();
				Map field1 = new HashMap();
				field1.put("field", USER_ID);
				field1.put("op", "cn");
				field1.put("data", "");
				field1.put("connector", null);
				field1.put("children", new ArrayList());
				List fieldList = new ArrayList();
				fieldList.add(field1);
				filter.put("children", fieldList);
			}
			SessionContext sessionContext = m_dataLayer.getSessionContext(sdesc);
			Map params = new HashMap();
			params.put("filter", filter);
			params.put("pageSize", 0);
			Map ret = m_dataLayer.query(sessionContext, params, sdesc, USER_ENTITY);
			if (sessionContext.hasAdminRole())
				return ret;
			List<Map> rows = (List) ret.get("rows");
			List f = new ArrayList();
			for (Map row : rows) {
				Map m = new HashMap();
				m.put("userid", row.get("userid"));
				f.add(m);
			}
			ret.put("rows", f);
			return ret;
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "AuthServiceImpl.getUsers:", e);
		} finally {
		}
	}


	@RequiresRoles("admin")
	public Map createUser(
			@PName(USER_ID)            String userid, 
			@PName("data")             Map data) throws RpcException {
		try {
			return _createUser(userid, data);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "AuthServiceImpl.createUser:", e);
		} finally {
		}
	}

	@RequiresRoles("admin")
	public Map getUser(
			@PName(USER_ID)            String userid) throws RpcException {
		try {
			StoreDesc sdesc = getStoreDesc();
			return getUserByUserid(sdesc, userid);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "AuthServiceImpl.createUser:", e);
		} finally {
		}
	}

	@RequiresRoles("admin")
	public Map updateUser(
			@PName(USER_ID)            String userid, 
			@PName("data")             Map data) throws RpcException {
		try {
			return _createUser(userid, data);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "AuthServiceImpl.updateUser:", e);
		} finally {
		}
	}

	@RequiresRoles("admin")
	public Map deleteUser(
			@PName(USER_ID)            String userid) throws RpcException {
		try {
			StoreDesc sdesc = getStoreDesc();
			Map ret = m_dataLayer.deleteObject(null, sdesc, USER_ENTITY, userid);
			return ret;
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "AuthServiceImpl.deleteUser:", e);
		} finally {
		}
	}
	public Map getUserProperties() throws RpcException {
		try {
			String userid=null;
			try {
				userid = org.ms123.common.system.ThreadContext.getThreadContext().getUserName();
			} catch (Exception e) {
				throw new RuntimeException("No userid");
			}
			return getUserProperties(userid);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "AuthServiceImpl.getUserProperties:", e);
		} finally {
		}
	}


	/* END JSON-RPC-API*/
	@Reference(target = "(kind=jdo)", dynamic = true, optional = true)
	public void setDataLayer(DataLayer dataLayer) {
		System.out.println("AuthServiceImpl.setDataLayer:" + dataLayer);
		m_dataLayer = dataLayer;
	}
	@Reference(dynamic = true, optional = true)
	public void setNamespaceService(NamespaceService ns) {
		System.out.println("AuthServiceImpl.setNamespaceService:" + ns);
		m_namespaceService = ns;
	}

	@Reference(dynamic = true)
	public void setNucleusService(NucleusService paramNucleusService) {
		this.m_nucleusService = paramNucleusService;
		System.out.println("AuthServiceImpl.setNucleusService:" + paramNucleusService);
	}

	protected void debug(String msg) {
		//System.out.println(msg);
		m_logger.debug(msg);
	}
	protected void info(String msg) {
		System.out.println(msg);
		m_logger.info(msg);
	}
	private static final org.slf4j.Logger m_logger = org.slf4j.LoggerFactory.getLogger(AuthServiceImpl.class);
}
