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

import aQute.bnd.annotation.component.*;
import aQute.bnd.annotation.metatype.*;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.Date;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Collection;
import java.util.Collections;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.FetchPlan;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.ms123.common.permission.api.PermissionException;
import org.apache.commons.beanutils.PropertyUtils;
import org.ms123.common.utils.*;
import org.ms123.common.data.api.DataLayer;
import org.ms123.common.data.api.SessionContext;
import org.ms123.common.auth.api.AuthService;
import org.ms123.common.git.GitService;
import org.ms123.common.entity.api.EntityService;
import org.ms123.common.nucleus.api.NucleusService;
import org.ms123.common.libhelper.Bean2Map;
import org.ms123.common.rpc.PDefaultBool;
import org.ms123.common.rpc.PDefaultFloat;
import org.ms123.common.rpc.PDefaultInt;
import org.ms123.common.rpc.PDefaultLong;
import org.ms123.common.rpc.PDefaultString;
import org.ms123.common.rpc.PName;
import org.ms123.common.rpc.POptional;
import org.ms123.common.rpc.RpcException;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.permission.api.PermissionService;
import org.osgi.framework.BundleContext;
import org.apache.commons.beanutils.PropertyUtils;
import javax.jdo.PersistenceManager;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.Extent;
import javax.jdo.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.beanutils.BeanMap;
import static org.ms123.common.rpc.JsonRpcServlet.ERROR_FROM_METHOD;
import static org.ms123.common.rpc.JsonRpcServlet.INTERNAL_SERVER_ERROR;
import static org.ms123.common.rpc.JsonRpcServlet.PERMISSION_DENIED;

/** TeamService implementation
 */
@SuppressWarnings("unchecked")
@Component(enabled = true, configurationPolicy = ConfigurationPolicy.optional, immediate = true, properties = { "rpc.prefix=team" })
public class TeamServiceImpl extends BaseTeamServiceImpl implements org.ms123.common.team.api.TeamService {

	protected Inflector m_inflector = Inflector.getInstance();

	private Bean2Map m_b2m = new Bean2Map();

	public TeamServiceImpl() {
	}

	protected void activate(BundleContext bundleContext, Map<?, ?> props) {
		System.out.println("TeamServiceImpl.activate.props:" + props);
	}

	protected void deactivate() throws Exception {
		info("deactivate");
		System.out.println("TeamServiceImpl deactivate");
	}

	private Map getObjectGraph(StoreDesc sdesc, String entityName, String id, Map mapping) {
		System.out.println("getObjectGraph:entityName:" + entityName + ",id:" + id);
		Map retMap = new HashMap();
		SessionContext sessionContext = m_dataLayer.getSessionContext(sdesc);
		PersistenceManager pm = sessionContext.getPM();
		FetchPlan fp = pm.getFetchPlan();
		try {
			fp.setMaxFetchDepth(-1);
			String className = m_inflector.getClassName(entityName);
			Class clazz = sessionContext.getClass(className);
			Object objectMaster = pm.getObjectById(clazz, id);
			retMap = TeamVisitor.getObjectGraph(objectMaster, sessionContext, mapping);
		} catch (javax.jdo.JDOObjectNotFoundException e) {
			retMap.put("teamid", id);
			retMap.put("name", id);
			m_dataLayer.insertObject(retMap, sdesc, TEAMINTERN_ENTITY);
			return retMap;
		} catch (org.datanucleus.exceptions.NucleusObjectNotFoundException e) {
			return retMap;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			pm.close();
		}
		return retMap;
	}

	/* BEGIN JSON-RPC-API*/
	public Map getTeamTree(
			@PName(StoreDesc.NAMESPACE) String namespace, 
			@PName("mapping")          @POptional Map mapping) throws RpcException {
		Map ret = null;
		StoreDesc sdesc = StoreDesc.getNamespaceData(namespace);
		SessionContext sessionContext = m_dataLayer.getSessionContext(sdesc);
		try {
			if (sessionContext.hasAdminRole() ) {
				String mkey = mapping != null ? mapping.toString():"";
				Map tree = m_adminTreeCache.get(namespace+mkey);
				System.out.println("TreeCacheSize:"+m_adminTreeCache.keySet().size());
				if( tree != null) return tree;
			}
			ret = this.getObjectGraph(sdesc, TEAMINTERN_ENTITY, "root", mapping);
			if (sessionContext.hasAdminRole()) {
				String mkey = mapping != null ? mapping.toString():"";
				m_adminTreeCache.put(namespace+mkey,ret);
			}
			return ret;
		} catch (PermissionException e) {
			throw new RpcException(ERROR_FROM_METHOD, PERMISSION_DENIED, "TeamService.getTeamTree", e);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "TeamService.getTeamTree:", e);
		}
	}

	public Map getTeam(
			@PName(StoreDesc.NAMESPACE) String namespace, 
			@PName(TEAM_ID)            String teamid) throws RpcException {
		StoreDesc sdesc = StoreDesc.getNamespaceData(namespace);
		SessionContext sessionContext = m_dataLayer.getSessionContext(sdesc);
		try {
			Map ret = m_dataLayer.getObject(sdesc, TEAMINTERN_ENTITY, teamid);
			System.out.println("ret:" + ret);
			return ret;
		} catch (PermissionException e) {
			throw new RpcException(ERROR_FROM_METHOD, PERMISSION_DENIED, "TeamService.getTeam", e);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "TeamService.getTeam:", e);
		}
	}

	public Map createTeam(
			@PName(StoreDesc.NAMESPACE) String namespace, 
			@PName(TEAM_ID)         		String teamid, 
			@PName(TEAM_NAME)     			@POptional String name, 
			@PName(DESCRIPTION)     		String description, 
			@PName(USER_READ)       		List<String> userRead, 
			@PName(USER_MANAGE)     		List<String> userManage, 
			@PName(USER_CREATE)     		List<String> userCreate 
			) throws RpcException {

		try {
			Map ret = _createTeam(namespace,teamid,name,description,userRead,userManage,userCreate);
			createSubTeams(namespace,teamid,name,description,userRead,userManage,userCreate);
			return ret;
		} catch (Throwable e) {
			if( e instanceof RpcException){
				throw (RpcException)e;
			}
			if( e instanceof RuntimeException){
				//if( ((RuntimeException)e).getCause() instanceof javax.transaction.RollbackException){
					throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "TeamService.createTeam:team exists '"+teamid+"'");
				//}
			}
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "TeamService.createTeam:", e);
		} finally {
		}
	}

	public Map updateTeam(
			@PName(StoreDesc.NAMESPACE) String namespace, 
			@PName(TEAM_ID)            String teamid, 
			@PName(TEAM_NAME)     			@POptional String name, 
			@PName(DESCRIPTION)     		String description, 
			@PName(USER_READ)       		List<String> userRead, 
			@PName(USER_MANAGE)     		List<String> userManage, 
			@PName(USER_CREATE)     		List<String> userCreate) throws RpcException {
		try {
			m_adminTreeCache = new HashMap();
			boolean isAdmin = m_permissionService.hasRole(ADMINROLE);
			if(!isAdmin && !canManageTeam(namespace,teamid,null)){
				throw new RpcException(ERROR_FROM_METHOD, PERMISSION_DENIED, "TeamService:updateTeam not allowed");
			}
			StoreDesc sdesc = StoreDesc.getNamespaceData(namespace);
			Map<String,Object> data = new HashMap();
			//data.put(TEAM_NAME,name); Name darf nicht mehr geändert werden.
			data.put(TEAM_ID,teamid);
			data.put(DESCRIPTION,description);
			data.put(USER_READ,userRead);
			data.put(USER_CREATE,userCreate);
			data.put(USER_MANAGE,userManage);
			Map ret = m_dataLayer.updateObject(data, null, null, sdesc, TEAMINTERN_ENTITY, teamid, null, null);
			return ret;
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "TeamService.updateTeam:", e);
		} finally {
		}
	}

	@RequiresRoles("admin")
	public void deleteTeam(
			@PName(StoreDesc.NAMESPACE) String namespace, 
			@PName(TEAM_ID)            String teamid) throws RpcException {
		try {
			m_adminTreeCache = new HashMap();
			StoreDesc sdesc = StoreDesc.getNamespaceData(namespace);
			_deleteTeam(sdesc,teamid);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "TeamService.deleteTeam:", e);
		} finally {
		}
	}

	public List<Map> teamUsage(
			@PName(StoreDesc.NAMESPACE) String namespace, 
			@PName(TEAM_ID)            String teamid, 
			@PName("entityNameList")   List<String> entityNameList) throws RpcException {
		try {
			String user = org.ms123.common.system.thread.ThreadContext.getThreadContext().getUserName();
			return teamUsage(namespace, teamid, entityNameList, user);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "TeamService.teamUsage:", e);
		} finally {
		}
	}

	public List<Map> expiredTeams(
			@PName(StoreDesc.NAMESPACE) String namespace, 
			@PName("entityName")   String entityName) throws RpcException {
		try {
			String user = org.ms123.common.system.thread.ThreadContext.getThreadContext().getUserName();
			return expiredTeams(namespace, entityName, user);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "TeamService.expiredTeams:", e);
		} finally {
		}
	}

	public List<Map> teamUsageByUser(
			@PName(StoreDesc.NAMESPACE) String namespace, 
			@PName("userid")            String userid ) throws RpcException {
		try {
			StoreDesc sdesc = StoreDesc.getNamespaceData(namespace);
			SessionContext sessionContext = m_dataLayer.getSessionContext(sdesc);
			if (!sessionContext.hasAdminRole() ) {
				String user = org.ms123.common.system.thread.ThreadContext.getThreadContext().getUserName();
				if( !userid.equals(user)){
					throw new RuntimeException("no permittedUser");
				}
			}
			return teamUsageByUser(sdesc, userid);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "TeamService.teamUsage:", e);
		} finally {
		}
	}

	public boolean checkTeams(String namespace, String userName, Map userProperties, Collection<Object> teams) {
		try {
			boolean isAdmin = m_permissionService.hasRole(ADMINROLE);
			debug("checkTeams:" + ADMINROLE + "/" + userName + "/" + isAdmin);
			if (isAdmin) {
				return true;
			}
			if (teams == null || teams.size() == 0) {
				System.out.println("teams null");
				return true;
			}
			boolean inTeam = false;
			for (Object team : teams) {
				Map teamMap = null;
				if (team instanceof Map) {
					teamMap = (Map) team;
				} else {
					teamMap = new BeanMap(team);
				}
				String teamid = (String) teamMap.get(TEAM_ID);
				boolean disabled = getBoolean(teamMap, "disabled", false);
				if (disabled == false && teamid != null && teamid.trim().length() > 0) {
					Map		teamInternalMap = new BeanMap(teamMap.get("teamintern"));
					boolean ckdate = checkTeamDate(teamInternalMap, teamMap);
					if (!ckdate) {
						continue;
					}
					inTeam = true;
					boolean ur = contains(getArray(teamInternalMap, "userRead"), userName);
					boolean um = contains(getArray(teamInternalMap, "userManage"), userName);
					debug("TeamService.checkTeams.permissions(" + userName + "):ur->" + ur + "  um->" + um );
					if (ur || um) {
						return true;
					}
				}
			}
			debug("inTeam:"+inTeam);
			return !inTeam;
		} finally {
		}
	}

	private boolean checkTeamUser(String userName, Map teamInternalMap) {
		boolean ur = contains(getArray(teamInternalMap, "userRead"), userName);
		boolean um = contains(getArray(teamInternalMap, "userManage"), userName);
		debug("TeamService.checkTeamUser.permissions(" + userName + "):ur->" + ur + "  um->" + um);
		if (ur || um) {
			return true;
		}
		return false;
	}
	public int checkTeamUserPermission(String namespace, String teamid, String userName ) {
		boolean isAdmin = m_permissionService.hasRole(ADMINROLE);
		if (isAdmin) {
			return WRITE;
		}
		Map teamInternalMap = 	getTeamintern( namespace, teamid);
		boolean ur = contains(getArray(teamInternalMap, "userRead"), userName);
		boolean um = contains(getArray(teamInternalMap, "userManage"), userName);
		debug("TeamService.checkTeamUserPermissions(" + userName + "):ur->" + ur + "  um->" + um);
		if( um ) return WRITE;
		if (ur) return READ;
		return NOACCESS;
	}

	public boolean checkTeamDate(Map teamMap) {
		return checkTeamDate(null,teamMap);
	}
	private boolean checkTeamDate(Map teamInternalMap, Map teamMap) {
		Calendar today = Calendar.getInstance();
		today.set(Calendar.HOUR_OF_DAY, 0);
		today.set(Calendar.MINUTE, 0);
		today.set(Calendar.SECOND, 0);
		today.set(Calendar.MILLISECOND, 0);
		Calendar validFrom = null;
		if (teamMap != null && teamMap.get("validFrom") != null) {
			validFrom = getCalendar(teamMap, "validFrom", 0);
		} else {
			if (teamInternalMap != null) {
				validFrom = getCalendar(teamInternalMap, "validFrom", 0);
			}
		}
		Calendar validTo = null;
		if (teamMap != null && teamMap.get("validTo") != null) {
			validTo = getCalendar(teamMap, "validTo", Long.MAX_VALUE);
		} else {
			if (teamInternalMap != null) {
				validTo = getCalendar(teamInternalMap, "validTo", Long.MAX_VALUE);
			}
		}
		boolean startOk = true;
		boolean endOk = true;
		if (validFrom != null) {
			debug("checkTeamDate.validFrom:" + validFrom.getTime());
			debug("checkTeamDate.validFrom.before:" + today.before(validFrom));
			if (today.before(validFrom)) {
				startOk = false;
			}
		}
		if (validTo != null) {
			debug("checkTeamDate.validTo:" + validTo.getTime());
			debug("checkTeamDate.validTo.after:" + today.after(validTo));
			if (today.after(validTo)) {
				endOk = false;
			}
		}
		debug("checkTeamDate:" + startOk + "/" + endOk);
		if (!startOk || !endOk) {
			return false;
		}
		return true;
	}

	private Map getTeamintern(String namespace, String teamid) {
		StoreDesc sdesc = StoreDesc.getNamespaceData(namespace);
		SessionContext sessionContext = m_dataLayer.getSessionContext(sdesc);
		PersistenceManager pm = sessionContext.getPM();
		try {
			Class clazz = sessionContext.getClass(TEAMINTERN_ENTITY);
			Object obj = pm.getObjectById(clazz, teamid);
			BeanMap ret = new BeanMap(obj);
			return ret;
		} catch (Exception e) {
			throw new RuntimeException("TeamService.getTeamintern(" + teamid + ")", e);
		} finally {
		}
	}

	private Calendar getCalendar(Map m, String key, long deftime) {
		Calendar cal = Calendar.getInstance();
		try {
			cal.setTime((Date) m.get(key));
			return cal;
		} catch (Exception e) {
			try{
				cal.setTime(new Date((Long)m.get(key)));
				return cal;
			}catch(Exception e1){
				e.printStackTrace();
				cal.setTime(new Date(deftime));
				return cal;
			}
		}
	}

	private List<Map> teamUsage(String namespace, String teamid, List<String> entityNameList, String user) throws Exception {
		List ret = new ArrayList();
		for (String entityName : entityNameList) {
			Map m = teamUsageByEntity(namespace, teamid, entityName, user);
			ret.add(m);
		}
		return ret;
	}

	private Map<String, Object> teamUsageByEntity(String namespace, String teamid, String entityName, String user) throws Exception {
		Map<String, Object> statMap = new HashMap();
		statMap.put("inactive", 0);
		statMap.put("active", 0);
		statMap.put("entityName", entityName);
		statMap.put("teamid", teamid);
		StoreDesc sdesc = StoreDesc.getNamespaceData(namespace);
		Map<String, Object> info = m_entityService.getEntitytype(sdesc.getStoreId(), entityName);
		if (info.get("default_fields") == null || ((Boolean) info.get("default_fields")) != true) {
			return statMap;
		}
		int activeCount = 0;
		int inactiveCount = 0;
		Class clazz = m_nucleusService.getClass(sdesc, m_inflector.getClassName(entityName));
		String filter = "_team_list.size() > 0 && _team_list.contains (t) && t.teamid == '"+teamid+"'";
		PersistenceManager pm = m_nucleusService.getPersistenceManagerFactory(sdesc).getPersistenceManager();
		Extent e = pm.getExtent(clazz, true);
		Query q = pm.newQuery(e, filter);
		q.declareVariables("aid.Team t");
		try {
			Collection coll = (Collection) q.execute();
			Iterator iter = coll.iterator();
			while (iter.hasNext()) {
				Object obj = iter.next();
				Collection<Object> teamList = (Collection) PropertyUtils.getProperty(obj, "_team_list");
				for( Object team : teamList){	
					Map teamMap = new HashMap(new BeanMap(team));
					int status = getTeamStatus(sdesc.getNamespace(), teamMap, teamid, user);
					if (status == ACTIVE){
						activeCount++;
					}
					if (status == INACTIVE){
						inactiveCount++;
					}
				}
			}
		} finally {
			q.closeAll();
			pm.close();
		}
		statMap.put("inactive", inactiveCount);
		statMap.put("active", activeCount);
		statMap.put("total", (inactiveCount+activeCount));
		return statMap;
	}

	private List<Map> teamUsageByUser(StoreDesc sdesc, String userid) throws Exception {
		List<Map> ret = new ArrayList();
		Class clazz = m_nucleusService.getClass(sdesc, m_inflector.getClassName("teamintern"));
		String filter1 = "userCreate.contains ('"+userid+"') || userManage.contains('"+userid+"') || userRead.contains('"+userid+"')";
		String filter2 = "userCreate.size()>0 || userManage.size()>0 || userRead.size()>0";
		PersistenceManager pm = m_nucleusService.getPersistenceManagerFactory(sdesc).getPersistenceManager();
		Extent e = pm.getExtent(clazz, true);
		Query q = pm.newQuery(e, userid==null ? filter2 : filter1);
		try {
			Collection coll = (Collection) q.execute();
			Iterator iter = coll.iterator();
			while (iter.hasNext()) {
				Object obj = iter.next();
				Map		bMap = new HashMap(new BeanMap(obj));
				bMap.remove("children");
				bMap.put("userCreate",listToString((List)bMap.get("userCreate")));
				bMap.put("userRead",listToString((List)bMap.get("userRead")));
				bMap.put("userManage",listToString((List)bMap.get("userManage")));
				bMap.remove("validTo");
				bMap.remove("validFrom");
				ret.add( bMap);
			}
		} finally {
			q.closeAll();
			pm.close();
		}
		return ret;
	}

	private String listToString(List<String> list){
		if( list == null) return "";
		String ret="";
		String komma="";
		for( String s : list){
			ret += komma+s;
			komma=",";
		}
		return ret;
	}

	public int getTeamStatus(String namespace, Map teamMap, String teamid, String user) {
		System.out.println("getTeamStatus:"+teamMap+"/teamid:"+teamid+"/user:"+user);
		String _teamid = (String) teamMap.get("teamid");
		if (teamid!=null && !teamid.equals(_teamid)) {
			System.out.println("_teamid:" + _teamid + "|" + teamid + "| not equals");
			return -1;
		}
		Map		teamInternalMap = new BeanMap(teamMap.get("teamintern"));
		System.out.println("Team:" + new HashMap(teamMap));
		System.out.println("teamInternalMap:" + new HashMap(teamInternalMap));
		boolean isAdmin = m_permissionService.hasRole(ADMINROLE);
		if (!isAdmin && !checkTeamUser(user, teamInternalMap)) {
			return -1;
		}
		if (!checkTeamDate(null, teamMap)) {
			System.out.println("Team.INACTIVE");
			return INACTIVE;
		}
		System.out.println("Team.ACTIVE");
		return ACTIVE;
	}

	public List<Map> expiredTeams(String namespace, String entityName, String user) throws Exception {
		List ret = new ArrayList();
		StoreDesc sdesc = StoreDesc.getNamespaceData(namespace);
		Map<String, Object> info = m_entityService.getEntitytype(sdesc.getStoreId(), entityName);
		if (info.get("default_fields") == null || ((Boolean) info.get("default_fields")) != true) {
			return ret;
		}
		int activeCount = 0;
		int inactiveCount = 0;
		Class clazz = m_nucleusService.getClass(sdesc, m_inflector.getClassName(entityName));
		String filter = "_team_list.size() > 0";
		PersistenceManager pm = m_nucleusService.getPersistenceManagerFactory(sdesc).getPersistenceManager();
		Extent e = pm.getExtent(clazz, true);
		Query q = pm.newQuery(e, filter);
		Map existsMap = new HashMap();
		try {
			Collection coll = (Collection) q.execute();
			Iterator iter = coll.iterator();
			while (iter.hasNext()) {
				Object obj = iter.next();
				Collection<Object> teamList = (Collection) PropertyUtils.getProperty(obj, "_team_list");
				for( Object team : teamList){	
					Map teamMap = new HashMap(new BeanMap(team));
					int status = getTeamStatus(sdesc.getNamespace(), teamMap, null, user);
					if (status != INACTIVE){
						continue;
					}
					String teamid = (String) teamMap.get("teamid");
					if( existsMap.get(teamid) == null){
						ret.add( teamMap);
						existsMap.put(teamid,true);
					}
				}
			}
		} finally {
			q.closeAll();
			pm.close();
		}
		sortListToValidTo(ret);
		return ret;
	}

	private void sortListToValidTo(List list) {
		Collections.sort(list, new ListSortByValidTo());
	}

	private class ListSortByValidTo implements Comparator<Map> {
		public int compare(Map m1, Map m2) {
			long validTo1 = ((Date)m1.get("validTo")).getTime();
			long validTo2 = ((Date)m2.get("validTo")).getTime();
			return (int) (validTo1 - validTo2);
		}
	}

	/* END JSON-RPC-API*/
	@Reference(target = "(kind=jdo)", dynamic = true, optional = true)
	public void setDataLayer(DataLayer dataLayer) {
		System.out.println("TeamServiceImpl.setDataLayer:" + dataLayer);
		m_dataLayer = dataLayer;
	}

	@Reference(dynamic = true, optional = true)
	public void setAuthService(AuthService authService) {
		this.m_authService = authService;
		info("TeamServiceImpl.setAuthService:" + authService);
	}

	@Reference(dynamic = true, optional = true)
	public void setEntityService(EntityService paramEntityService) {
		this.m_entityService = paramEntityService;
		System.out.println("TeamServiceImpl.setEntityService:" + paramEntityService);
	}

	@Reference(dynamic = true, optional = true)
	public void setNucleusService(NucleusService paramNucleusService) {
		this.m_nucleusService = paramNucleusService;
		System.out.println("TeamServiceImpl.setNucleusService:" + paramNucleusService);
	}

	@Reference(dynamic = true, optional = true)
	public void setGitService(GitService gitService) {
		System.out.println("TeamServiceImpl.setGitService:" + gitService);
		this.m_gitService = gitService;
	}
	@Reference(dynamic = true)
	public void setPermissionService(PermissionService paramPermissionService) {
		this.m_permissionService = paramPermissionService;
		System.out.println("TeamServiceImpl.setPermissionService:" + paramPermissionService);
	}
}
