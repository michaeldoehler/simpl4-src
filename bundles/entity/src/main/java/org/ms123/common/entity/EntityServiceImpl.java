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
package org.ms123.common.entity;

import aQute.bnd.annotation.component.*;
import aQute.bnd.annotation.metatype.*;
import com.Ostermiller.util.*;
import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import java.util.Map;
import java.util.List;
import org.ms123.common.data.api.DataLayer;
import org.ms123.common.auth.api.AuthService;
import org.ms123.common.nucleus.api.NucleusService;
import org.ms123.common.enumeration.EnumerationService;
import org.ms123.common.git.GitService;
import org.ms123.common.rpc.PDefaultBool;
import org.ms123.common.rpc.PDefaultFloat;
import org.ms123.common.rpc.PDefaultInt;
import org.ms123.common.rpc.PDefaultLong;
import org.ms123.common.rpc.PDefaultString;
import org.ms123.common.rpc.PName;
import org.ms123.common.rpc.POptional;
import org.ms123.common.rpc.RpcException;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.permission.api.PermissionException;
import org.ms123.common.permission.api.PermissionService;
import org.ms123.common.libhelper.Inflector;
import org.ms123.common.utils.ParameterParser;
import org.ms123.common.utils.UtilsService;
import org.ms123.common.data.api.SessionContext;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.ms123.common.rpc.JsonRpcServlet.ERROR_FROM_METHOD;
import static org.ms123.common.rpc.JsonRpcServlet.INTERNAL_SERVER_ERROR;
import static org.ms123.common.rpc.JsonRpcServlet.PERMISSION_DENIED;

/** EntityService implementation
 */
@SuppressWarnings("unchecked")
@Component(enabled = true, configurationPolicy = ConfigurationPolicy.optional, immediate = true, properties = { "rpc.prefix=entity" })
public class EntityServiceImpl extends BaseEntityServiceImpl implements org.ms123.common.entity.api.EntityService {

	protected Inflector m_inflector = Inflector.getInstance();

	private static final Logger m_logger = LoggerFactory.getLogger(EntityServiceImpl.class);

	protected JSONDeserializer m_ds = new JSONDeserializer();

	protected JSONSerializer m_js = new JSONSerializer();


	public EntityServiceImpl() {
	}

	protected void activate(BundleContext bundleContext, Map<?, ?> props) {
		System.out.println("EntityServiceImpl.activate.props:" + m_dataLayer);
	}

	protected void deactivate() throws Exception {
		System.out.println("EntityServiceImpl deactivate");
	}

	/* BEGIN JSON-RPC-API*/
	public List<Map> getEntitytypes(
			@PName(StoreDesc.STORE_ID) String storeId) throws RpcException {
		try {
			return m_gitMetaData.getEntitytypes(storeId);
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "EntityService.getEntitytypes:", e);
		}
	}

	public List<Map> getEntitytypeInfo(
			@PName(StoreDesc.STORE_ID) String storeId,
			@PName("names") List<String> names
				) throws RpcException {
		try {
			return m_gitMetaData.getEntitytypeInfo(storeId,names);
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "EntityService.getEntitytypeInfo:", e);
		}
	}

	public List<Map> getRelations(
			@PName(StoreDesc.STORE_ID) String storeId) throws RpcException {
		try {
			return m_gitMetaData.getRelations(storeId);
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "EntityService.getRelations:", e);
		}
	}

	public void saveRelations(
			@PName(StoreDesc.STORE_ID) String storeId, 
			@PName("relations")        List<Map> relations) throws RpcException {
		try {
			m_gitMetaData.saveRelations(storeId, relations);
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "EntityService.saveRelations:", e);
		}
	}

	public void saveEntitytype(
			@PName(StoreDesc.STORE_ID) String storeId, 
			@PName("name")             String name, 
			@PName("data")             Map<String, Object> data) throws RpcException {
		try {
			m_gitMetaData.saveEntitytype(storeId, name, data);
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "EntityService.saveEntitytype:", e);
		}
	}

	public Map createEntitytypes(
			@PName(StoreDesc.STORE_ID) String storeId, 
			@PName("datamapperConfigName")        @POptional     String datamapperConfigName, 
			@PName("datamapperConfig")          @POptional   Map datamapperConfig, 
			@PName("strategy")          @POptional  List<Map> strategy,
			@PName("side")            String side,
			@PName("infoOnly")           @POptional @PDefaultBool(false) Boolean infoOnly
				) throws RpcException {
		try {
			GraphCreator gc = new GraphCreator(this);
			if( datamapperConfig == null && datamapperConfigName == null){
				throw new RpcException(ERROR_FROM_METHOD, 100, "EntityService.createEntitytypes:no datamapperConfig");
			}
			return gc.createEntitytypes(storeId, datamapperConfigName, datamapperConfig, strategy,side,infoOnly);
		} catch (Exception e) {
			if( e instanceof RpcException) throw (RpcException)e;
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "EntityService.saveEntitytype:", e);
		}
	}

	public void deleteEntitytype(
			@PName(StoreDesc.STORE_ID) String storeId, 
			@PName("name")             String name) throws RpcException {
		try {
			m_gitMetaData.deleteEntitytype(storeId, name);
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "EntityService.deleteEntitytype:", e);
		}
	}

	public void deleteEntitytypeField(
			@PName(StoreDesc.STORE_ID) String storeId, 
			@PName("entitytype")       String entitytype, 
			@PName("name")             String name) throws RpcException {
		try {
			m_gitMetaData.deleteEntitytypeField(storeId, entitytype, name);
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "EntityService.deleteEntitytypeField:", e);
		}
	}

	public void deleteEntitytypes(
			@PName(StoreDesc.STORE_ID) String storeId) throws RpcException {
		try {
			m_gitMetaData.deleteEntitytypes(storeId);
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "EntityService.deleteEntitytypes:", e);
		}
	}

	public void saveEntitytypeField(
			@PName(StoreDesc.STORE_ID) String storeId, 
			@PName("entitytype")       String entitytype, 
			@PName("name")             String name, 
			@PName("data")             Map<String, Object> data) throws RpcException {
		try {
			m_gitMetaData.saveEntitytypeField(storeId, entitytype, name, data);
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "EntityService.saveEntitytypeField:", e);
		}
	}

	public Map<String, Object> getEntitytypeField(
			@PName(StoreDesc.STORE_ID) String storeId, 
			@PName("entitytype")       String entitytype, 
			@PName("name")             String name) throws RpcException {
		try {
			return m_gitMetaData.getEntitytypeField(storeId, entitytype, name);
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "EntityService.getEntitytypeField:", e);
		}
	}

	public Map<String, Object> getEntitytype(
			@PName(StoreDesc.STORE_ID) String storeId, 
			@PName("name")             String name) throws RpcException {
		try {
			return m_gitMetaData.getEntitytype(storeId, name);
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "EntityService.getEntitytype:", e);
		}
	}

	public Object getEntities(
			@PName(StoreDesc.STORE_ID) String storeId, 
			@PName("withChilds")       @POptional @PDefaultBool(true) Boolean withChilds, 
			@PName("withTeam")       @POptional @PDefaultBool(false) Boolean withTeam, 
			@PName("filter")           @POptional String filter, 
			@PName("sortField")        @POptional String sortField, 
			@PName("mapping")          @POptional Map mapping) throws RpcException {
		try {
			StoreDesc sdesc = StoreDesc.get(storeId);
			return getEntities(sdesc, withChilds, withTeam, mapping, filter, sortField);
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "EntityService.getEntities:", e);
		}
	}

	public Map getEntityTree(
			@PName(StoreDesc.STORE_ID) String storeId, 
			@PName("main")             String main, 
			@PName("maxlevel")         int maxlevel, 
			@PName("pathid")           @POptional Boolean pathid, 
			@PName("type")             @POptional String type, 
			@PName("listResolved")     @POptional Boolean listResolved) throws RpcException {
		try {
			StoreDesc sdesc = StoreDesc.get(storeId);
			return getEntityTree(sdesc, main, maxlevel, pathid, type, listResolved);
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "EntityService.getEntityTree:", e);
		}
	}

	public String getIdField(
			@PName(StoreDesc.STORE_ID) String storeId, 
			@PName("entity")           String entity ) throws RpcException {
		try {
			StoreDesc sdesc = StoreDesc.get(storeId);
			return getIdField(sdesc, entity);
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "EntityService.getIdField:", e);
		}
	}

	public List<Map> getFields(
			@PName(StoreDesc.STORE_ID) String storeId, 
			@PName("entity")           String entity, 
			@PName("mapping")          @POptional Map mapping, 
			@PName("filter")           @POptional String filter, 
			@PName("withAutoGen")      @POptional @PDefaultBool(true) Boolean withAutoGen,
			@PName("withRelations")      @POptional @PDefaultBool(false) Boolean withRelations
			) throws RpcException {
		try {
			StoreDesc sdesc = StoreDesc.get(storeId);
			List<Map> result = getFields(sdesc, entity, withAutoGen,withRelations);
			if (mapping == null && filter == null)
				return result;
			return m_utilsService.listToList(result, mapping, filter);
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "EntityService.getFields:", e);
		}
	}

	@RequiresRoles("admin")
	public Map getPermittedFields(
			@PName(StoreDesc.STORE_ID) String storeId, 
			@PName("entity")           String entity) throws RpcException {
		try {
			StoreDesc sdesc = StoreDesc.get(storeId);
			return getPermittedFields(sdesc, entity);
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "EntityService.getPermittedFields:", e);
		}
	}

	/* END JSON-RPC-API*/
	@Reference(target = "(kind=jdo)", dynamic = true, optional = true)
	public void setDataLayer(DataLayer dataLayer) {
		System.out.println("EntityServiceImpl.setDataLayer:" + dataLayer);
		m_dataLayer = dataLayer;
	}

	@Reference(dynamic = true, optional = true)
	public void setGitService(GitService gitService) {
		System.out.println("EntityServiceImpl.setGitService:" + gitService);
		m_gitMetaData = new GitMetaDataImpl(gitService);
		this.m_gitService = gitService;
	}

	@Reference(dynamic = true)
	public void setPermissionService(PermissionService paramPermissionService) {
		this.m_permissionService = paramPermissionService;
		System.out.println("EntityServiceImpl.setPermissionService:" + paramPermissionService);
	}

	@Reference(dynamic = true)
	public void setAuthService(AuthService paramService) {
		this.m_authService = paramService;
		System.out.println("EntityServiceImpl.setAuthService:" + paramService);
	}

	@Reference(dynamic = true)
	public void setUtilsService(UtilsService paramUtilsService) {
		this.m_utilsService = paramUtilsService;
		System.out.println("EntityServiceImpl.setUtilsService:" + paramUtilsService);
	}

	@Reference(dynamic = true, optional=true)
	public void setEnumerationService(EnumerationService param) {
		this.m_enumerationService = param;
		System.out.println("EntityServiceImpl.setEnumerationService:" + param);
	}

	@Reference(dynamic = true)
	public void setNucleusService(NucleusService paramService) {
		this.m_nucleusService = paramService;
		System.out.println("EntityServiceImpl.setNucleusService:" + paramService);
	}
}
