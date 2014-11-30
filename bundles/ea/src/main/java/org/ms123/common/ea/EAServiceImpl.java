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
package org.ms123.common.ea;

import aQute.bnd.annotation.component.*;
import aQute.bnd.annotation.metatype.*;
import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;
import java.util.*;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import javax.jdo.PersistenceManager;
import javax.jdo.Extent;
import javax.jdo.Query;
import javax.transaction.UserTransaction;
import org.ms123.common.libhelper.Bean2Map;
import org.ms123.common.data.api.DataLayer;
import org.ms123.common.rpc.PDefaultBool;
import org.ms123.common.rpc.PDefaultFloat;
import org.ms123.common.rpc.PDefaultInt;
import org.ms123.common.rpc.PDefaultLong;
import org.ms123.common.rpc.PDefaultString;
import org.ms123.common.rpc.PName;
import org.ms123.common.rpc.POptional;
import org.ms123.common.rpc.RpcException;
import org.osgi.framework.BundleContext;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.data.api.SessionContext;
import org.ms123.common.libhelper.Inflector;
import org.ms123.common.git.GitService;
import org.ms123.common.nucleus.api.NucleusService;
import org.ms123.common.reporting.ReportingService;
import org.ms123.common.data.api.LuceneService;
import org.ms123.common.data.dupcheck.DublettenCheckService;
import org.apache.commons.beanutils.PropertyUtils;
import static org.ms123.common.rpc.JsonRpcServlet.ERROR_FROM_METHOD;
import static org.ms123.common.rpc.JsonRpcServlet.INTERNAL_SERVER_ERROR;
import static org.ms123.common.rpc.JsonRpcServlet.PERMISSION_DENIED;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** EAService implementation
 */
@SuppressWarnings("unchecked")
@Component(enabled = true, configurationPolicy = ConfigurationPolicy.optional, immediate = true, properties = { "rpc.prefix=ea" })
public class EAServiceImpl extends BaseEAServiceImpl implements EAService, Constants {

	protected Inflector m_inflector = Inflector.getInstance();
	private JSONDeserializer m_ds = new JSONDeserializer();

	public EAServiceImpl() {
	}

	protected void activate(BundleContext bundleContext, Map<?, ?> props) {
		System.out.println("EAServiceImpl.activate.props:" + m_dataLayer);
	}

	protected void deactivate() throws Exception {
		info("EAServiceImpl deactivate");
	}

	/* BEGIN JSON-RPC-API*/
	public void InitialerImport(
			@PName("storeId")          String storeId, 
			@PName("entity")           String entity, 
			@PName("basedir")          String basedir) throws RpcException {
		try {
			_initialerImport(storeId, entity, basedir);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "EAService.getEAs:", e);
		}
	}

	public List getUsers(
			@PName("teams")            List<String> teams) throws RpcException {
		try {
			StoreDesc sdesc = StoreDesc.get("ea_data");
			SessionContext sc = m_dataLayer.getSessionContext(sdesc);
			List fieldList = new ArrayList();
			for (String tn : teams) {
				if( tn == null) throw new RuntimeException("getUsers:(teamid:"+tn+")");
				String teamid = tn;//.toUpperCase();
				Map field = new HashMap();
				field.put("field", "contact._team_list");
				field.put("op", "bw");
				field.put("data", teamid);
				field.put("connector", null);
				field.put("children", new ArrayList());
				fieldList.add(field);
			}
			Map filter = new HashMap();
			filter.put("connector", "or");
			filter.put("children", fieldList);
			Map params = new HashMap();
			params.put("filter", filter);
			params.put("pageSize", 0);
			String filterJson = m_gitService.searchContent( sdesc.getNamespace(), "contact.filter", "sw.filter" );
			Map contentMap = (Map) m_ds.deserialize(filterJson);
			contentMap.put("filter",filter);
			Map ret = sc.executeFilter(contentMap,null);
			List<Map> rows = (List) ret.get("rows");
			return rows;
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "EAServiceImpl.getUsers:", e);
		} finally {
		}
	}

	public Map getUser(
			@PName(EMAIL)              String email) throws RpcException {
		try {
			StoreDesc sdesc = StoreDesc.get("ea_data");
			SessionContext sc = m_dataLayer.getSessionContext(sdesc);
			List fieldList = new ArrayList();

			Map field = new HashMap();
			field.put("field", "email");
			field.put("op", "eq");
			field.put("data", email);
			field.put("connector", null);
			field.put("children", new ArrayList());
			fieldList.add(field);

			Map filter = new HashMap();
			filter.put("connector", "and");
			filter.put("children", fieldList);
			Map params = new HashMap();
			params.put("filter", filter);
			params.put("pageSize", 0);
			String filterJson = m_gitService.searchContent( sdesc.getNamespace(), "contact.filter", "sw.filter" );
			Map contentMap = (Map) m_ds.deserialize(filterJson);
			contentMap.put("filter",filter);
			Map ret = sc.executeFilter(contentMap,null);
			List<Map> rows = (List) ret.get("rows");
			if( rows.size() == 0){
				throw new RuntimeException("getUser:(email:"+email+") not found");
			}
			return (Map)rows.get(0);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "EAServiceImpl.getUser:", e);
		} finally {
		}
	}

	public Map createOrUpdateUser(
			@PName(EMAIL)              String email, 
			@PName("data")             Map data,
			@PName("communication")    @POptional Map communication,
			@PName("teams")            List<String> teams) 
			throws RpcException {
		try {
			return _createOrUpdateContact(email, data,communication, teams);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "EAServiceImpl.createUser:", e);
		} finally {
		}
	}

	public List<Map> getUserListValidTo(
			@PName("date")             String date) //12.10.2014
			throws RpcException {
		try {
			return _getUserListStatusChange(date,"to");
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "EAServiceImpl.getUserListValidTo:", e);
		} finally {
		}
	}

	public List<Map> getUserListValidFrom(
			@PName("date")             String date) //12.10.2014
			throws RpcException {
		try {
			return _getUserListStatusChange(date,"from");
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "EAServiceImpl.getUserListValidFrom:", e);
		} finally {
		}
	}

	public void syncWithEnpedia() throws RpcException {
		try {
			_syncWithEnpedia();
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "EAServiceImpl.syncWithEnpedia:", e);
		} finally {
		}
	}
	private Map _createOrUpdateContact(String email, Map data,Map communication, List<String> teams) throws Exception {
		Map ret = null;
		StoreDesc sdesc = StoreDesc.get("ea_data");
		SessionContext sc = m_dataLayer.getSessionContext(sdesc);
		sc.setProperty("bypassTrigger", true );
		Map c = _getContactByEmail(sc, email);
		List<Map> teamList = new ArrayList();

		for( String tn : teams){
			String teamid = teamIdExists(sc, tn);
			if( teamid == null) throw new RuntimeException("createOrUpdateUser:(teamid:"+tn+") not found");
			Map m = new HashMap();
			m.put("teamid", teamid);
			m.put("value", teamid);
			teamList.add(m);
		}
		data.put("_team_list", teamList);
		data.put("email", email);

		if( communication == null){
			communication = new HashMap();
		}

		UserTransaction ut = m_nucleusService.getUserTransaction();
		ut.begin();
		try{
			if (c != null) {
				c.putAll(data);
				Object comm = c.remove(COMMUNICATION_ENTITY);
				String commId = (String)PropertyUtils.getProperty(comm, "id");
				System.out.println("commId:"+commId);
				ret = m_dataLayer.updateObject(sc, c, CONTACT_ENTITY, (String) c.get("id"));
				if( communication.size() >0){
					m_dataLayer.updateObject(sc, communication, COMMUNICATION_ENTITY, commId );
				}
			} else {
				ret = m_dataLayer.insertObject(sc, data, CONTACT_ENTITY);
//				communication.put("mail1", email);
				Map r = m_dataLayer.insertObject(sc, communication, COMMUNICATION_ENTITY, CONTACT_ENTITY, (String) ret.get("id"));
			}
			ut.commit();
		}catch(Exception e){
			sc.handleException(e);
		}
		return ret;
	}

	private Map _getContactByEmail(SessionContext sc, String mail) throws Exception {
		String filter = "communication.mail1 == '" + mail + "'";
		debug("_getContactByEmail:" + filter);
		PersistenceManager pm = sc.getPM();
		Class clazz = sc.getClass(m_inflector.getClassName(CONTACT_ENTITY));
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

	private String teamIdExists(SessionContext sc, String id) throws Exception {
		id = id.toUpperCase();
		String filter = "teamid.toUpperCase() == '" + id + "'";
		debug("_getTeamName:" + filter);
		PersistenceManager pm = sc.getPM();
		Class clazz = sc.getClass(m_inflector.getClassName(TEAMINTERN_ENTITY));
		Extent e = pm.getExtent(clazz, true);
		Query q = pm.newQuery(e, filter);
		try {
			Collection coll = (Collection) q.execute();
			Iterator iter = coll.iterator();
			if (iter.hasNext()) {
				Object obj = iter.next();
				return (String) PropertyUtils.getProperty(obj, "teamid");
			}
		} finally {
			q.closeAll();
		}
		return null;
	}

	/* END JSON-RPC-API*/
	@Reference(target = "(kind=jdo)", dynamic = true, optional = true)
	public void setDataLayer(DataLayer dataLayer) {
		System.out.println("EAServiceImpl.setDataLayer:" + dataLayer);
		m_dataLayer = dataLayer;
	}

	@Reference(dynamic = true)
	public void setNucleusService(NucleusService paramNucleusService) {
		this.m_nucleusService = paramNucleusService;
		System.out.println("EAServiceImpl.setNucleusService:" + paramNucleusService);
	}

	@Reference(dynamic = true)
	public void setLuceneService(LuceneService paramLuceneService) {
		this.m_luceneService = paramLuceneService;
		System.out.println("EAServiceImpl.setLuceneService:" + paramLuceneService);
	}

	@Reference(dynamic = true, optional = true)
	public void setReportingService(ReportingService paramReportService) {
		m_reportingService = paramReportService;
		System.out.println("EAServiceImpl.setReportingService:" + paramReportService);
	}

	@Reference(target = "(impl=default)", dynamic = true, optional = true)
	public void setDublettenCheckService(DublettenCheckService paramDublettenCheckService) {
		m_dublettenCheckService = paramDublettenCheckService;
		System.out.println("EAServiceImpl.setDublettenCheckService:" + paramDublettenCheckService);
	}

	@Reference(dynamic = true, optional = true)
	public void setGitService(GitService paramGitService) {
		this.m_gitService = paramGitService;
		info("EaServiceImpl.setGitService:" + paramGitService);
	}
	protected void debug(String msg) {
		System.out.println(msg);
		m_logger.debug(msg);
	}

	protected void info(String msg) {
		System.out.println(msg);
		m_logger.info(msg);
	}

	private static final org.slf4j.Logger m_logger = org.slf4j.LoggerFactory.getLogger(EAServiceImpl.class);
}
