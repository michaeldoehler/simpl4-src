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
package org.ms123.common.setting;

import aQute.bnd.annotation.component.*;
import aQute.bnd.annotation.metatype.*;
import com.Ostermiller.util.*;
import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.jdo.JDOObjectNotFoundException;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.ms123.common.data.api.DataLayer;
import org.ms123.common.data.api.SessionContext;
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
import org.ms123.common.namespace.NamespaceService;
import org.ms123.common.utils.UtilsService;
import org.ms123.common.entity.api.EntityService;
import org.ms123.common.data.api.DataLayer;
import org.ms123.common.libhelper.Inflector;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.jdo.PersistenceManager;
import javax.jdo.Extent;
import javax.jdo.Query;
import javax.transaction.UserTransaction;
import org.apache.commons.beanutils.PropertyUtils;
import org.ms123.common.libhelper.Bean2Map;
import static java.text.MessageFormat.format;
import static org.ms123.common.rpc.JsonRpcServlet.ERROR_FROM_METHOD;
import static org.ms123.common.rpc.JsonRpcServlet.INTERNAL_SERVER_ERROR;
import static org.ms123.common.rpc.JsonRpcServlet.PERMISSION_DENIED;

/** SettingService implementation
 */
@SuppressWarnings("unchecked")
@Component(enabled = true, configurationPolicy = ConfigurationPolicy.optional, immediate = true, properties = { "rpc.prefix=setting" })
public class SettingServiceImpl extends BaseSettingServiceImpl implements org.ms123.common.setting.api.SettingService,Constants {

	private static final Logger m_logger = LoggerFactory.getLogger(SettingServiceImpl.class);

	protected GitService m_gitService;
	protected DataLayer m_dataLayer;

	public SettingServiceImpl() {
	}

	protected void activate(BundleContext bundleContext, Map<?, ?> props) {
		System.out.println("SettingServiceImpl.activate.props:" + m_dataLayer);
	}

	protected void deactivate() throws Exception {
	}

	private StoreDesc getStoreDesc(String namespace){
		return StoreDesc.getNamespaceData(namespace);
	}

	/* BEGIN JSON-RPC-API*/
	@RequiresRoles("admin")
	public void setResourceSetting(
			@PName(StoreDesc.NAMESPACE) String namespace, 
			@PName(SETTINGS_ID)        String settingsid, 
			@PName(RESOURCE_ID)        String resourceid, 
			@PName(SETTING_MAP)       @POptional Map settings,
			@PName("overwrite")       @POptional @PDefaultBool(true) Boolean overwrite
			) throws RpcException {
		try {
			m_gitMetaData.setResourceSetting( namespace, settingsid,resourceid, settings,overwrite);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "SettingServiceImpl.setResourceSetting:", e);
		} finally {
		}
	}

	public Map getResourceSetting(
			@PName(StoreDesc.NAMESPACE) String namespace, 
			@PName(SETTINGS_ID)        String settingsid,
			@PName(RESOURCE_ID)        String resourceid) throws RpcException {
		try {
			return m_gitMetaData.getResourceSetting(namespace, settingsid, resourceid);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "SettingServiceImpl.getSetting:", e);
		}
	}

	@RequiresRoles("admin")
	public void deleteResourceSetting(
			@PName(StoreDesc.NAMESPACE) String namespace, 
			@PName(SETTINGS_ID)        String settingsid,
			@PName(RESOURCE_ID)        String resourceid ) throws RpcException {
		try {
			m_gitMetaData.deleteResourceSetting(namespace, settingsid, resourceid);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "SettingServiceImpl.deleteSetting:", e);
		} finally {
		}
	}


	/* HighLevel-Api*/
	public List getFieldsForEntityView(
			@PName(StoreDesc.NAMESPACE) String namespace, 
			@PName(SETTINGS_ID)        String settingsid, 
			@PName(ENTITY)             String entity, 
			@PName(VIEW)          		 @POptional String view,
			@PName("filter")           @POptional String filter,
			@PName("sortField")        @POptional String sortField,
			@PName("mapping")          @POptional Map mapping) throws RpcException {

		try {
			return m_gitMetaData.getFieldsForEntityView(namespace,settingsid, entity, view,mapping, filter, sortField );
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "SettingServiceImpl.getFieldsForEntityView:", e);
		}
	}

	public Map getPropertiesForEntityView(
			@PName(StoreDesc.NAMESPACE) String namespace, 
			@PName(SETTINGS_ID)        String settingsid, 
			@PName(ENTITY)             String entity, 
			@PName(VIEW)            @POptional   String view) throws RpcException {

		try {
			return m_gitMetaData.getPropertiesForEntityView(namespace,settingsid, entity, view );
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "SettingServiceImpl.getPropertiesForEntityView:", e);
		}
	}

	public List getFieldsetsForEntity(
			@PName(StoreDesc.NAMESPACE) String namespace, 
			@PName(SETTINGS_ID)        String settingsid, 
			@PName(ENTITY)             String entity 
			) throws RpcException {

		try {
			return m_gitMetaData.getFieldsetsForEntity(namespace,settingsid, entity );
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "SettingServiceImpl.getFieldsetsForEntity:", e);
		}
	}


	public List getAllSettingsForEntityList(
			@PName(StoreDesc.NAMESPACE) String namespace, 
			@PName(SETTINGS_ID)        String settingsid, 
			@PName("entities")         List<String> entityList ) throws RpcException {

		try {
			List ret = new ArrayList();
			for( String entity : entityList){			
				Map entityMap = new HashMap();
				List<String> viewList = new ArrayList();
				viewList.add("all");
				viewList.add("main-form");
				viewList.add("main-grid");
				viewList.add("search");
				viewList.add("report");
				viewList.add("export");

				List fieldSets = m_gitMetaData.getFieldsetsForEntity(namespace, settingsid, entity);
				entityMap.put("fieldSets",fieldSets);

				Map properties = m_gitMetaData.getPropertiesForEntity(namespace, settingsid, entity);
				entityMap.put("properties",properties);

				Map viewFields = new HashMap();
				for( String view : viewList){
					try{
						List fields = m_gitMetaData.getFieldsForEntityView(namespace,settingsid, entity, view,null, null, null );
						viewFields.put( view,fields);
					}catch(Exception e){
						System.out.println("getAllSettingsForEntity:Entity not found:"+entity);
					}
				}
				entityMap.put("viewFields",viewFields);

				Map viewProps = new HashMap();
				for( String view : viewList){
					try{
						Map props = m_gitMetaData.getPropertiesForEntityView(namespace,settingsid, entity, view );
						viewProps.put( view,props);
					}catch(Exception e){
						System.out.println("getAllSettingsForEntity2:Entity not found:"+entity);
					}
				}
				entityMap.put("viewProps",viewProps);
				ret.add(entityMap);
			}
			return ret;
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "SettingServiceImpl.getAllSettingsForEntity:", e);
		}
	}


	/* END JSON-RPC-API*/
	@Reference(target = "(kind=jdo)", dynamic = true, optional = true)
	public void setDataLayer(DataLayer dataLayer) {
		System.out.println("SettingServiceImpl.setDataLayer:" + dataLayer);
		m_dataLayer = dataLayer;
	}

	@Reference(dynamic = true, optional = true)
	public void setGitService(GitService gitService) {
		System.out.println("SettingServiceImpl.setGitService:" + gitService);
		m_gitService = gitService;
		m_gitMetaData = new GitMetaDataImpl(gitService,this);
	}

	@Reference(dynamic = true)
	public void setPermissionService(PermissionService paramPermissionService) {
		this.m_permissionService = paramPermissionService;
		System.out.println("SettingServiceImpl.setPermissionService:" + paramPermissionService);
	}
	@Reference(dynamic = true)
	public void setNamespaceService(NamespaceService nss) {
		System.out.println("SettingServiceImpl.setNamespaceService:" + nss);
		m_isRuntimeSystem = nss.isRuntimeSystem();
	}

	@Reference(dynamic = true)
	public void setUtilsService(UtilsService paramUtilsService) {
		this.m_utilsService = paramUtilsService;
		System.out.println("SettingServiceImpl.setUtilsService:" + paramUtilsService);
	}
	@Reference(dynamic = true)
	public void setEntityService(EntityService paramEntityService) {
		this.m_entityService = paramEntityService;
		System.out.println("SettingServiceImpl.setEntityService:" + paramEntityService);
	}
}
