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
package org.ms123.common.importing;

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
import org.ms123.common.datamapper.DatamapperService;
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
import org.ms123.common.permission.api.PermissionException;
import org.ms123.common.permission.api.PermissionService;
import org.ms123.common.utils.UtilsService;
import org.ms123.common.entity.api.EntityService;
import org.ms123.common.libhelper.Inflector;
import org.ms123.common.data.dupcheck.DublettenCheckService;
import org.ms123.common.entity.api.EntityService;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.jdo.PersistenceManager;
import javax.jdo.Extent;
import javax.jdo.Query;
import javax.transaction.UserTransaction;
import org.apache.commons.beanutils.PropertyUtils;
import org.ms123.common.libhelper.Bean2Map;
import org.milyn.SmooksFactory;
import org.ms123.common.libhelper.Base64;
import static org.ms123.common.utils.IOUtils.toByteArray;
import static org.apache.commons.beanutils.PropertyUtils.setProperty;
import static org.apache.commons.beanutils.PropertyUtils.getProperty;
import static java.text.MessageFormat.format;
import static org.ms123.common.rpc.JsonRpcServlet.ERROR_FROM_METHOD;
import static org.ms123.common.rpc.JsonRpcServlet.INTERNAL_SERVER_ERROR;
import static org.ms123.common.rpc.JsonRpcServlet.PERMISSION_DENIED;

/** ImportingService implementation
 */
@SuppressWarnings("unchecked")
@Component(enabled = true, configurationPolicy = ConfigurationPolicy.optional, immediate = true, properties = { "rpc.prefix=importing" })
public class ImportingServiceImpl extends BaseImportingServiceImpl implements ImportingService, Constants {

	private static final Logger m_logger = LoggerFactory.getLogger(ImportingServiceImpl.class);

	public ImportingServiceImpl() {
	}

	protected void activate(BundleContext bundleContext, Map<?, ?> props) {
		System.out.println("ImportingServiceImpl.activate.props:" + m_dataLayer);
	}

	protected void deactivate() throws Exception {
		System.out.println("ImportingServiceImpl deactivate");
	}

	private StoreDesc getStoreDesc(String ns){
		StoreDesc sdesc = null;
		try{
			sdesc = StoreDesc.get(ns+"_config");
		}catch(Exception e){
		}
		if( sdesc == null){
			sdesc = StoreDesc.getNamespaceMeta(ns);
		}
		return sdesc;
	}

	/* BEGIN JSON-RPC-API*/
	public List<Map> getImportings(
			@PName(StoreDesc.NAMESPACE) String namespace, 
			@PName(PREFIX)          @POptional String prefix,
			@PName(MAPPING)          @POptional Map mapping) throws RpcException {
		StoreDesc sdesc = getStoreDesc(namespace);
		try {
			return _getImportings(sdesc, prefix, mapping);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "ImportingService.getImportings:", e);
		}
	}

	public Map createImporting(
			@PName(StoreDesc.NAMESPACE) String namespace, 
			@PName(IMPORTING_ID)       String importingid, 
			@PName(DESCRIPTION)        @POptional String description, 
			@PName(SETTINGS)           @POptional Map settings) throws RpcException {
		try {
			StoreDesc sdesc = getStoreDesc(namespace);
			Map data = new HashMap();
			data.put(IMPORTING_ID, importingid);
			data.put(DESCRIPTION, description);
			data.put(USER, getUserName());
			if (settings != null) {
				String jsonBody = m_js.deepSerialize(settings);
				data.put(JSON_BODY, jsonBody);
			}
			try {
				m_dataLayer.deleteObject(null, sdesc, IMPORTING_ENTITY, importingid);
			} catch (Exception e) {
			}
			Map ret = m_dataLayer.insertObject(data, sdesc, IMPORTING_ENTITY, null, null);
			return ret;
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "ImportingService.createImporting:", e);
		} finally {
		}
	}

	public Map updateImporting(
			@PName(StoreDesc.NAMESPACE) String namespace, 
			@PName(IMPORTING_ID)       String importingid, 
			@PName(DESCRIPTION)        @POptional String description, 
			@PName(SETTINGS)           @POptional Map settings) throws RpcException {
		try {
			StoreDesc sdesc = getStoreDesc(namespace);
			Map data = new HashMap();
			data.put(IMPORTING_ID, importingid);
			data.put(DESCRIPTION, description);
			if (settings != null) {
				String jsonBody = m_js.deepSerialize(settings);
				data.put("jsonBody", jsonBody);
			}
			Map ret = m_dataLayer.updateObject(data, null, null, sdesc, IMPORTING_ENTITY, importingid, null, null);
			return ret;
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "ImportingServiceImpl.updateImporting:", e);
		} finally {
		}
	}

	@RequiresRoles("admin")
	public Map deleteImporting(
			@PName(StoreDesc.NAMESPACE) String namespace, 
			@PName(IMPORTING_ID)       String importingid) throws RpcException {
		try {
			StoreDesc sdesc = getStoreDesc(namespace);
			Map ret = m_dataLayer.deleteObject(null, sdesc, IMPORTING_ENTITY, importingid);
			return ret;
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "ImportingServiceImpl.deleteImporting:", e);
		} finally {
		}
	}

	public Map getSettings(
			@PName(StoreDesc.NAMESPACE) String namespace, 
			@PName(IMPORTING_ID)       @POptional String importingid) throws RpcException {
		StoreDesc sdesc = getStoreDesc(namespace);
		SessionContext sessionContext = m_dataLayer.getSessionContext(sdesc);
		try {
			String className = m_inflector.getClassName(IMPORTING_ENTITY);
			Class clazz = sessionContext.getClass(className);
			Object obj = sessionContext.getObjectById(clazz, importingid);
			if (obj == null) {
				System.out.println("ImportingServiceImpl.getSettings:importingid:\"" + importingid + "\" not found");
				return new HashMap();
			}
			Map settings = (Map) m_ds.deserialize((String) getProperty(obj, JSON_BODY));
			return settings;
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "ImportingServiceImpl.getSettings:", e);
		} finally {
			sessionContext.handleFinally(null);
		}
	}

	public String getFileContent(
			@PName(StoreDesc.NAMESPACE) String namespace, 
			@PName(IMPORTING_ID)       @POptional String importingid) throws RpcException {
		StoreDesc sdesc = getStoreDesc(namespace);
		SessionContext sessionContext = m_dataLayer.getSessionContext(sdesc);
		try {
			String className = m_inflector.getClassName(IMPORTING_ENTITY);
			Class clazz = sessionContext.getClass(className);
			Object obj = sessionContext.getObjectById(clazz, importingid);
			if (obj == null) {
				System.out.println("ImportingServiceImpl.getFileContent:importingid:\"" + importingid + "\" not found");
				return null;
			}
			byte[] content = (byte[]) getProperty(obj, CONTENT);
			return new String(content);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "ImportingServiceImpl.getFileContent:", e);
		} finally {
			sessionContext.handleFinally(null);
		}
	}

	public Map getFileModel(
			@PName(StoreDesc.NAMESPACE) String namespace, 
			@PName(IMPORTING_ID)       @POptional String importingid) throws RpcException {
		StoreDesc sdesc = getStoreDesc(namespace);
		SessionContext sessionContext = m_dataLayer.getSessionContext(sdesc);
		try {
			String className = m_inflector.getClassName(IMPORTING_ENTITY);
			Class clazz = sessionContext.getClass(className);
			Object obj = sessionContext.getObjectById(clazz, importingid);
			if (obj == null) {
				System.out.println("ImportingServiceImpl.getFileModel:importingid:\"" + importingid + "\" not found");
				return null;
			}
			Map settings = (Map) m_ds.deserialize((String) getProperty(obj, JSON_BODY));
			byte[] content = (byte[]) getProperty(obj, CONTENT);
			return getFileModel(content, (Map) settings.get(SOURCE_SETUP));
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "ImportingServiceImpl.getFileModel:", e);
		} finally {
			sessionContext.handleFinally(null);
		}
	}

	public Object doImport(
			@PName(StoreDesc.STORE_ID) String storeId, 
			@PName(IMPORTING_ID)       @POptional String importingid, 
			@PName("withoutSave")      @POptional @PDefaultBool(false) Boolean withoutSave,
			@PName("max")      @POptional @PDefaultInt(-1) Integer max
					) throws RpcException {
		StoreDesc data_sdesc = StoreDesc.get(storeId);
		StoreDesc aid_sdesc = getStoreDesc(data_sdesc.getNamespace());
		SessionContext sessionContext = m_dataLayer.getSessionContext(aid_sdesc);
		try {
			String className = m_inflector.getClassName(IMPORTING_ENTITY);
			Class clazz = sessionContext.getClass(className);
			Object obj = sessionContext.getObjectById(clazz, importingid);
			if (obj == null) {
				throw new RuntimeException("ImportingServiceImpl.doImport:importingid:\"" + importingid + "\" not found");
			}
			Map settings = (Map) m_ds.deserialize((String) getProperty(obj, JSON_BODY));
			byte[] content = (byte[]) getProperty(obj, CONTENT);
			if( settings.get("input")!= null){
				System.out.println("doImport:"+settings);
				System.out.println("doImport:"+m_datamapper+"/"+data_sdesc+"/"+content);
				sessionContext = m_dataLayer.getSessionContext(data_sdesc);
				BeanFactory bf = new BeanFactory(sessionContext, settings);
				Object ret = m_datamapper.transform(data_sdesc.getNamespace(), settings, null, new String(content), bf);
				if( withoutSave) return ret;
				UserTransaction ut = sessionContext.getUserTransaction();
				try{
					ut.begin();
					Map outputTree = (Map)settings.get("output");
					Map<String,String> persistenceSpecification = (Map)outputTree.get("persistenceSpecification");
					Object o = org.ms123.common.data.MultiOperations.persistObjects(sessionContext,ret,persistenceSpecification, -1);
					ut.commit();
					return o;
				}catch(Exception e){
					ut.rollback();
					throw e;
				}
			}else{
				return doImport(data_sdesc, settings, content, withoutSave, max);
			}
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "ImportingServiceImpl.doImport:", e);
		} finally {
			sessionContext.handleFinally(null);
		}
	}

	public Map upload(
			@PName(StoreDesc.STORE_ID) String storeId, 
			@PName(IMPORTING_ID)       @POptional String importingid, 
			@PName(FILE_CONTENT)       @POptional String fileContent, 
			@PName(FILE_MAP)           @POptional Map fileMap, 
			@PName(SETTINGS)           @POptional Map settings, 
			@PName("withoutImport")    @POptional @PDefaultBool(false) Boolean withoutImport) throws RpcException {

		if( fileMap == null && fileContent == null){
				throw new RuntimeException("fileMap or fileContent is needed");
		}
		StoreDesc data_sdesc = StoreDesc.get(storeId);
		StoreDesc aid_sdesc = getStoreDesc(data_sdesc.getNamespace());
		SessionContext sessionContext = m_dataLayer.getSessionContext(aid_sdesc);
		PersistenceManager pm = sessionContext.getPM();
		UserTransaction ut = sessionContext.getUserTransaction();
		try {
			ut.begin();
			System.out.println("upload:" + data_sdesc + "/" + fileMap);
			String className = m_inflector.getClassName(IMPORTING_ENTITY);
			Class clazz = sessionContext.getClass(className);
			Object obj = sessionContext.getObjectById(clazz, importingid);
			if (obj == null) {
				obj = sessionContext.createObject(IMPORTING_ENTITY);
				setProperty(obj, IMPORTING_ID, importingid);
				sessionContext.makePersistent(obj);
			}
			byte[] bytes = null;
			if( fileMap != null){
				Map importFile = (Map) fileMap.get("importfile");
				String storeLocation = (String) importFile.get("storeLocation");
				InputStream is = new FileInputStream(new File(storeLocation));
				bytes = toByteArray(is);
				is.close();
			}else if (fileContent != null && fileContent.startsWith("data:")){
				int ind = fileContent.indexOf(";base64,");
				bytes = Base64.decode(fileContent.substring(ind+8));
			}
			bytes = convertToUTF8(bytes);
			setProperty(obj, USER, getUserName());
			setProperty(obj, CONTENT, bytes);
			if (settings != null) {
				setProperty(obj, JSON_BODY, m_js.deepSerialize(settings));
			}
			ut.commit();
			Map ret = null;
			if (settings != null && !withoutImport) {
				ret = doImport(data_sdesc, settings, bytes, false, -1);
			}
			return ret;
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "ImportingServiceImpl.upload:", e);
		} finally {
			sessionContext.handleFinally(ut);
		}
	}
	public Object dmUpload(
			@PName(StoreDesc.STORE_ID) String storeId, 
			@PName(IMPORTING_ID)       @POptional String importingid, 
			@PName("dmConfig")         @POptional Map config, 
			@PName(FILE_CONTENT)       @POptional String fileContent, 
			@PName(FILE_MAP)           @POptional Map fileMap ) throws RpcException {
			upload(storeId,importingid, fileContent, fileMap,null,true);
			if( fileContent == null){
				StoreDesc sdesc = StoreDesc.get(storeId);
				fileContent = getFileContent(sdesc.getNamespace(),importingid);
			}
		return m_datamapper.getMetaData2(config,fileContent);
	}

	/* END JSON-RPC-API*/
	@Reference(target = "(kind=jdo)", dynamic = true, optional = true)
	public void setDataLayer(DataLayer dataLayer) {
		System.out.println("ImportingServiceImpl.setDataLayer:" + dataLayer);
		m_dataLayer = dataLayer;
	}
	@Reference(dynamic = true, optional = true)
	public void setDatamapper(DatamapperService datamapper) {
		System.out.println("ImportingServiceImpl.setDatamapper:" + datamapper);
		m_datamapper = datamapper;
	}

	@Reference(dynamic = true)
	public void setPermissionService(PermissionService paramPermissionService) {
		this.m_permissionService = paramPermissionService;
		System.out.println("ImportingServiceImpl.setPermissionService:" + paramPermissionService);
	}

	@Reference(dynamic = true)
	public void setUtilsService(UtilsService paramUtilsService) {
		this.m_utilsService = paramUtilsService;
		System.out.println("ImportingServiceImpl.setUtilsService:" + paramUtilsService);
	}

	@Reference(dynamic = true)
	public void setSmooksFactory(SmooksFactory paramSmooksFactory) {
		m_smooksFactory = paramSmooksFactory;
		System.out.println("ImportingServiceImpl.setSmooksFactory:" + paramSmooksFactory);
	}

	@Reference(dynamic = true)
	public void setEntityService(EntityService paramEntityService) {
		m_entityService = paramEntityService;
		System.out.println("ImportingServiceImpl.setEntityService:" + paramEntityService);
	}
	@Reference(target = "(impl=default)", dynamic = true, optional=true)
	public void setDublettenCheckService(DublettenCheckService paramDublettenCheckService) {
		m_dublettenCheckService = paramDublettenCheckService;
		System.out.println("ImportingServiceImpl.setDublettenCheckService:" + paramDublettenCheckService);
	}
}
