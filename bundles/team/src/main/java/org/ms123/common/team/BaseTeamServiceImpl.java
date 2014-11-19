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
package org.ms123.common.team;

import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import org.apache.commons.beanutils.PropertyUtils;
import org.ms123.common.data.api.SessionContext;
import org.ms123.common.data.api.DataLayer;
import org.ms123.common.permission.api.PermissionException;
import org.ms123.common.permission.api.PermissionService;
import org.ms123.common.nucleus.api.NucleusService;
import org.ms123.common.entity.api.EntityService;
import org.ms123.common.git.GitService;
import org.ms123.common.auth.api.AuthService;
import org.ms123.common.libhelper.Inflector;
import org.ms123.common.utils.UtilsService;
import org.ms123.common.store.StoreDesc;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static java.text.MessageFormat.format;
import org.ms123.common.rpc.RpcException;
import javax.jdo.PersistenceManager;
import org.apache.commons.beanutils.BeanMap;
import javax.jdo.Extent;
import javax.jdo.Query;
import javax.transaction.UserTransaction;
import static org.ms123.common.rpc.JsonRpcServlet.ERROR_FROM_METHOD;
import static org.ms123.common.rpc.JsonRpcServlet.INTERNAL_SERVER_ERROR;
import static org.ms123.common.rpc.JsonRpcServlet.PERMISSION_DENIED;

/** BaseTeamService implementation
 */
@SuppressWarnings("unchecked")
public class BaseTeamServiceImpl {

	protected Inflector m_inflector = Inflector.getInstance();

	protected PermissionService m_permissionService;

	protected UtilsService m_utilsService;

	protected AuthService m_authService;

	protected GitService m_gitService;

	protected EntityService m_entityService;
	protected NucleusService m_nucleusService;

	protected DataLayer m_dataLayer;

	protected JSONDeserializer m_ds = new JSONDeserializer();

	protected JSONSerializer m_js = new JSONSerializer();

	protected Map<String, Map> m_adminTreeCache = new HashMap();

	protected static String GROUPS = "groups";

	protected static int READ = 1;

	protected static int WRITE = 2;

	protected static int NOACCESS = -1;

	protected static int INACTIVE = 1;

	protected static int ACTIVE = 2;

	protected static final String ADMINROLE = "admin";
	protected static final String TEAM_ENTITY = "team";
	protected static final String TEAM_ID = "teamid";
	protected static final String TEAM_NAME = "name";
	protected static final String DESCRIPTION = "description";
	protected static final String USER_READ = "userRead";
	protected static final String USER_MANAGE = "userManage";
	protected static final String USER_CREATE = "userCreate";
	protected static final String TEAMINTERN_ENTITY = "teamintern";
	protected static final String AUTOCREATE_TEAMS = ".etc/autocreate_teams.json";

	public BaseTeamServiceImpl() {
		m_js.prettyPrint(true);
	}

	public Map _createTeam(String namespace, String teamid, String name, String description, List<String> userRead, List<String> userManage, List<String> userCreate) throws Exception {
		if (!checkTeamid(teamid)) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "TeamService.createTeam:teamid wrong");
		}
		String parentTeamId = getParentTeamid(teamid);
		name = getTeamName(name, teamid);
		m_adminTreeCache = new HashMap();
		StoreDesc sdesc = StoreDesc.getNamespaceData(namespace);
		SessionContext sessionContext = m_dataLayer.getSessionContext(sdesc);
		String userName = sessionContext.getUserName();
		if ("root".equals(parentTeamId)) {
			Map userProps = m_authService.getUserProperties(userName);
			if (!getBoolean(userProps.get("team_manage"), false)) {
				throw new RpcException(ERROR_FROM_METHOD, PERMISSION_DENIED, "TeamService:createTeam not allowed");
			}
		} else {
			Class clazz = sessionContext.getClass(TEAMINTERN_ENTITY);
			PersistenceManager pm = sessionContext.getPM();
			Object objectParent = pm.getObjectById(clazz, parentTeamId);
			String[] permittedUser = getArray(objectParent, "userCreate");
			if (!contains(permittedUser, userName)) {
				throw new RpcException(ERROR_FROM_METHOD, PERMISSION_DENIED, "TeamService:createTeam2 not allowed");
			}
		}
		Map<String, Object> data = new HashMap();
		data.put(TEAM_NAME, name);
		data.put(TEAM_ID, teamid);
		data.put(DESCRIPTION, description);
		data.put(USER_READ, userRead);
		data.put(USER_CREATE, userCreate);
		data.put(USER_MANAGE, userManage);
		info("createTeam:"+m_js.deepSerialize(data));
		Map ret = m_dataLayer.insertObject(data, sdesc, "children", TEAMINTERN_ENTITY, parentTeamId);
		return ret;
	}

	public boolean canCreateTeam(String namespace, String teamid, String userName){
		return _canCreateManageTeam(namespace,teamid,userName, "userCreate");
	}
	public boolean canManageTeam(String namespace, String teamid, String userName){
		return _canCreateManageTeam(namespace,teamid,userName, "userManage");
	}
	private boolean _canCreateManageTeam(String namespace, String teamid, String userName, String perm){
		StoreDesc sdesc = StoreDesc.getNamespaceData(namespace);
		SessionContext sessionContext = m_dataLayer.getSessionContext(sdesc);
		if( userName==null){ 
			userName=sessionContext.getUserName();
		}
		String _teamid = perm.equals("userCreate") ? getParentTeamid(teamid) : teamid;
		Class clazz = sessionContext.getClass(TEAMINTERN_ENTITY);
		PersistenceManager pm = sessionContext.getPM();
		Object objectParent = pm.getObjectById(clazz, _teamid);
		String[] permittedUser = getArray(objectParent, perm);
		info("_canCreateManageTeam:"+perm+"/"+Arrays.asList(getArray(objectParent,perm))+"/"+userName);
		if (!contains(permittedUser, userName)) {
			return false;
		}
		return true;
	}
	protected void createSubTeams(String namespace, String teamid, String name, String description, List<String> userRead, List<String> userManage, List<String> userCreate) throws Exception {
		String ppid = getParentTeamid(teamid);
		List<Map<String,String>> autoCreateTeams = getAutoCreateTeams(namespace);
		if( autoCreateTeams == null) return;
		for( Map<String,String> tm : autoCreateTeams ){
			if( ppid.equalsIgnoreCase(tm.get("trigger"))){
				_createTeam(namespace,teamid+"."+tm.get("id") , tm.get("name"), tm.get("description") ,userRead,userManage,userCreate);
			}
		}
	}

	private List getAutoCreateTeams(String ns){
		try{
			String s = m_gitService.getContentRaw(ns, AUTOCREATE_TEAMS);
			info("getAutoCreateTeams:"+s);
			return (List)m_ds.deserialize(s);
		}catch(RpcException e){
			if( e.getErrorCode() == 101){
				return null;
			}
			throw e;
		}
	}
	private List<String> getChildTeamids(StoreDesc sdesc, String teamid) throws Exception {
		List<String> ret = new ArrayList();
		String filter = "teamid == '" + teamid + "'";
		Class clazz = m_nucleusService.getClass(sdesc, m_inflector.getClassName("teamintern"));
		PersistenceManager pm = m_nucleusService.getPersistenceManagerFactory(sdesc).getPersistenceManager();
		Extent e = pm.getExtent(clazz, true);
		Query q = pm.newQuery(e, filter);
		try {
			Collection coll = (Collection) q.execute();
			Iterator iter = coll.iterator();
			if (iter.hasNext()) {
				Object obj = iter.next();
				Set children = (Set)PropertyUtils.getProperty(obj, "children");
				PropertyUtils.setProperty(obj, "children", new HashSet());

				System.out.println("children:"+children);
				for( Object c : children){
					String id = (String)PropertyUtils.getProperty(c, "teamid");
				System.out.println("\tid:"+id);
					ret.add( id);
				}
			}
		} finally {
			q.closeAll();
			pm.close();
		}
		return ret;
	}
	protected void _deleteTeam(StoreDesc sdesc, String teamid) throws Exception {
		List<String> ret = new ArrayList();
		String filter = "teamid == '" + getParentTeamid(teamid) + "'";
		Class clazz = m_nucleusService.getClass(sdesc, m_inflector.getClassName("teamintern"));
		PersistenceManager pm = m_nucleusService.getPersistenceManagerFactory(sdesc).getPersistenceManager();
		Extent e = pm.getExtent(clazz, true);
		Query q = pm.newQuery(e, filter);
		UserTransaction ut = m_nucleusService.getUserTransaction();
		try {
			Collection coll = (Collection) q.execute();
			Iterator iter = coll.iterator();
			if (iter.hasNext()) {
				Object obj = iter.next();
				Set children = (Set)PropertyUtils.getProperty(obj, "children");
				ut.begin();
				for( Object c : children){
					String _teamid = (String)PropertyUtils.getProperty(c, "teamid");
					System.out.println("Team._teamid:"+_teamid);
					if( teamid.equals(_teamid)){
						System.out.println("\tfoundTeam:"+teamid);
						children.remove(c);
						pm.deletePersistent(c);
					}
				}
				ut.commit();
			}
		} finally {
			q.closeAll();
			pm.close();
		}
	}

	private boolean checkTeamid(String s) {
		if (s == null)
			return false;
		String path[] = s.split("\\.");
		for (String seg : path) {
			boolean ok = seg.matches("^[0-9A-Za-z_\\-]{1,32}$");
			if (!ok) {
				info("checkTeamid:" + s + " -> false");
				return false;
			}
		}
		info("checkTeamid:" + s + " -> true");
		return true;
	}

	private String getParentTeamid(String teamid) {
		int ind = teamid.lastIndexOf(".");
		return teamid.substring(0, ind);
	}

	private String getTeamName(String name, String teamid) {
		if (name != null)
			return name;
		int ind = teamid.lastIndexOf(".");
		return teamid.substring(ind + 1);
	}

	protected boolean contains(String[] arr, String s) {
		int len = arr.length;
		for (int i = 0; i < len; i++) {
			if (arr[i].equals(s))
				return true;
		}
		return false;
	}

	protected String[] getArray(Object data, String prop) {
		Object obj = null;
		if (data instanceof Map) {
			obj = (Object) ((Map) data).get(prop);
		} else {
			try {
				obj = PropertyUtils.getProperty(data, prop);
			} catch (Exception e) {
				e.printStackTrace();
				return new String[0];
			}
		}
		if (obj instanceof String) {
			if (obj != null) {
				return ((String) obj).split(",");
			}
		}
		if (obj instanceof List) {
			return (String[]) ((List) obj).toArray(new String[0]);
		}
		return new String[0];
	}

	protected boolean getBoolean(Map m, String key, boolean def) {
		return (Boolean) ((m.get(key) != null) ? m.get(key) : def);
	}

	protected boolean getBoolean(Object val, boolean def) {
		try {
			if (val instanceof Boolean) {
				return (Boolean) val;
			}
		} catch (Exception e) {
		}
		return def;
	}

	protected void debug(String msg) {
		//System.out.println(msg);
		m_logger.debug(msg);
	}

	protected void info(String msg) {
		System.out.println(msg);
		m_logger.info(msg);
	}

	private static final org.slf4j.Logger m_logger = org.slf4j.LoggerFactory.getLogger(TeamServiceImpl.class);
}
