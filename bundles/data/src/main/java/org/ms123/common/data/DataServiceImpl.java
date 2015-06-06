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
package org.ms123.common.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Calendar;
import java.util.Iterator;
import flexjson.*;
import java.io.File;
import java.io.FileOutputStream;
import org.ms123.common.reporting.ReportingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.Ostermiller.util.*;
import org.activiti.engine.delegate.JavaDelegate;
import org.activiti.engine.delegate.DelegateExecution;
//import javax.naming.*; 
import javax.servlet.http.*;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.ms123.common.utils.ParameterParser;
import org.mvel2.MVEL;
import org.ms123.common.utils.UtilsService;
import org.ms123.common.utils.TypeUtils;
import org.ms123.common.nucleus.api.NucleusService;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.entity.api.EntityService;
import org.ms123.common.data.api.DataLayer;
import org.ms123.common.data.api.SessionContext;
import java.lang.reflect.*;
import org.ms123.common.permission.api.PermissionException;
import java.rmi.RemoteException;
import org.ms123.common.rpc.PName;
import org.ms123.common.rpc.POptional;
import org.ms123.common.rpc.PDefaultString;
import org.ms123.common.rpc.PDefaultInt;
import org.ms123.common.rpc.PDefaultBool;
import org.ms123.common.rpc.PDefaultFloat;
import org.ms123.common.rpc.RpcException;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.ms123.common.rpc.JsonRpcServlet.ERROR_FROM_METHOD;
import static org.ms123.common.rpc.JsonRpcServlet.INTERNAL_SERVER_ERROR;
import static org.ms123.common.rpc.JsonRpcServlet.PERMISSION_DENIED;
import aQute.bnd.annotation.metatype.*;
import aQute.bnd.annotation.component.*;

/** DataService implementation
 */
@SuppressWarnings("unchecked")
@Component(enabled = true, configurationPolicy = ConfigurationPolicy.optional, immediate = true, properties = { "rpc.prefix=data" })
public class DataServiceImpl implements DataService, JavaDelegate {

	private static final Logger m_logger = LoggerFactory.getLogger(DataServiceImpl.class);
	private final String ENTITY = "entity";

	private ReportingService m_reportingService;

	private NucleusService m_nucleusService;

	private DataLayer m_dataLayer;

	protected EntityService m_entityService;
	protected UtilsService m_utilsService;

	private Map m_configMapCache = new HashMap();

	private JSONSerializer m_js = new JSONSerializer();

	private JSONDeserializer m_ds = new JSONDeserializer();

	public DataServiceImpl() {
		System.out.println("DataServiceImpl construct");
	}

	public void execute(DelegateExecution execution) {
		System.out.println("DataServiceImpl.execute:" + execution.getClass());
		System.out.println("DataServiceImpl.execute:" + execution.getVariable("var2"));
		System.out.println("DataServiceImpl.execute:" + execution.getVariableNames());
		System.out.println("DataServiceImpl.execute:" + execution.getVariables());
		execution.setVariable("var1", "jjjjj");
		try {
			System.getProperties().remove("java.naming.factory.initial");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void activate() {
		System.out.println("DataServiceImpl.activate");
		m_logger.info("DataServiceImpl.activate");
	}
	public void deactivate() {
		System.out.println("DataServiceImpl.deactivate");
		m_logger.info("DataServiceImpl.deactivate");
	}

	protected Map getConfigForFieldsArray(StoreDesc sdesc, String entityName, List<String> fieldsArray) {
		Map retMap = new HashMap();
		for (String field : fieldsArray) {
			int dot = field.indexOf(".");
			if (dot != -1) {
				String[] name = field.split("\\.");
				String mname = name[0];
				String fname = name[1];
				mname = TypeUtils.getEntityForPath(m_nucleusService, sdesc, mname);
				Map _configMap = m_entityService.getPermittedFields(sdesc, mname);
				retMap.put(field, _configMap.get(fname));
			} else {
				Map _configMap = m_entityService.getPermittedFields(sdesc, entityName);
				retMap.put(field, _configMap.get(field));
			}
		}
		return retMap;
	}

	protected boolean getBoolean(Object val, boolean _def) {
		try {
			if (val instanceof String) {
				if ("false".equals(((String) val).toLowerCase())) {
					return false;
				}
				if ("true".equals(((String) val).toLowerCase())) {
					return true;
				}
			}
			if (val instanceof Boolean) {
				return (Boolean) val;
			}
		} catch (Exception e) {
		}
		return _def;
	}

	private void applyMapping(List<Map> list, Map<String, String> mapping) {
		for (Map<String, Object> rec : list) {
			Iterator<String> it = mapping.keySet().iterator();
			while (it.hasNext()) {
				String key = it.next();
				String val = mapping.get(key);
				try {
					System.out.println("eval:" + val + "/rec:" + rec);
					val = MVEL.evalToString(val, rec);
				} catch (Exception e) {
					System.out.println("eval.error:" + e);
				}
				rec.put(key, val);
			}
		}
	}

	/* BEGIN JSON-RPC-API*/
	public Map getObjectGraph(
			@PName(StoreDesc.STORE_ID) String storeId, 
			@PName("entity")           String entity, 
			@PName("id")               @POptional String id, 
			@PName("depth")            Integer depth) throws RpcException {
		Map ret = null;
		StoreDesc sdesc = StoreDesc.get(storeId);
		try {
					ret = m_dataLayer.getObjectGraph(sdesc, entity, id);
			return ret;
		} catch (PermissionException e) {
			throw new RpcException(ERROR_FROM_METHOD, PERMISSION_DENIED, "DataService.getOldTree", e);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "DataService.getOldTree:", e);
		}
	}

	public Map query(
			@PName(StoreDesc.STORE_ID) String storeId, 
			@PName("entity")           String entity, 
			@PName("entityChild")      @POptional String entityChild, 
			@PName("id")               @POptional String id, 
			@PName("filter")           @POptional Map filter, 
			@PName("fields")           @POptional List fields, 
			@PName("page")             @POptional @PDefaultInt(1) Integer page, 
			@PName("pageSize")         @POptional @PDefaultInt(30) Integer pageSize, 
			@PName("where")            @POptional String whereVal, 
			@PName("nosql")            @POptional @PDefaultBool(false) Boolean nosql, 
			@PName("join")             @POptional @PDefaultBool(false) Boolean join, 
			@PName("sql")              @POptional String sql, 
			@PName("orderby")          @POptional String orderby, 
			@PName("luceneQuery")      @POptional String luceneQuery, 
			@PName("format")           @POptional String format, 
			@PName("options")          @POptional String options, 
			@PName("state")     	     @POptional String state, 
			@PName("aliases")          @POptional String aliases) throws RpcException {
		Map ret = null;
		StoreDesc sdesc = StoreDesc.get(storeId);
		try {
			Map params = new HashMap();
			params.put("offset", (page - 1) * pageSize);
			params.put("pageSize", pageSize);
			params.put("page", page);
			params.put("whereVal", whereVal);
			params.put("nosql", nosql);
			params.put("sql", sql);
			params.put("join", join);
			params.put("state", state);
			params.put("filter", filter);
			params.put("luceneQuery", luceneQuery);
			params.put("fields", fields);
			params.put("orderby", orderby);
			ret = m_dataLayer.query(params, sdesc, entity, id, entityChild);
			if (ret == null) {
				Map emptyRet = new HashMap();
				emptyRet.put("records", "0");
				return emptyRet;
			}
			m_logger.info("format:" + format);
			if (format != null && (format.equals("csv") || format.equals("pdf") || format.equals("xls") || format.equals("html"))) {
				String user = org.ms123.common.system.thread.ThreadContext.getThreadContext().getUserName();
				String tmpFile = createDownloadFile((List) ret.get("rows"), sdesc, entity, format, options, fields, aliases, user);
				Map tmpRet = new HashMap();
				tmpRet.put("tmpFile", tmpFile);
				return tmpRet;
			} else {
				return ret;
			}
		} catch (PermissionException e) {
			throw new RpcException(ERROR_FROM_METHOD, PERMISSION_DENIED, "DataService.query:", e);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "DataService.query:", e);
		}
	}

	public Map queryOne(
			@PName(StoreDesc.STORE_ID) String storeId, 
			@PName("entity")           String entity, 
			@PName("id")               @POptional String id, 
			@PName("entityChild")      @POptional String entityChild, 
			@PName("fields")           @POptional List fields, 
			@PName("getContent")       @POptional @PDefaultBool(false) Boolean getContent, 
							HttpServletResponse response) throws RpcException {
		StoreDesc sdesc = StoreDesc.get(storeId);
		Map ret = null;
		try {
			if (getContent) {
				ret = m_dataLayer.getObject(sdesc, entity, id, entityChild, fields, response);
				return null;
			} else {
				ret = m_dataLayer.getObject(sdesc, entity, id, entityChild, fields);
			}
			if (ret == null) {
				ret = new HashMap();
				ret.put("records", "0");
			}
			return ret;
		} catch (PermissionException e) {
			throw new RpcException(ERROR_FROM_METHOD, PERMISSION_DENIED, "DataService.queryOne:", e);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "DataService.queryOne:", e);
		}
	}

	public Map insert(
			@PName(StoreDesc.STORE_ID) String storeId, 
			@PName("entity")           String entity, 
			@PName("id")               @POptional String id, 
			@PName("entityChild")      @POptional String entityChild, 
			@PName("data")             Map data, 
			@PName("filter")           @POptional Map filter, 
			@PName("state")     	     @POptional String state, 
			@PName("hints")            @POptional Map hints
				) throws RpcException {
		StoreDesc sdesc = StoreDesc.get(storeId);
		try {
			Map retMap = null;
			String idParent = null;
			String entityParent = null;
			if (entityChild != null) {
				entityParent = entity;
				entity = entityChild;
				idParent = id;
			}
			if( hints == null){
				hints = new HashMap();
			}
			hints.put("state", state);
			retMap = m_dataLayer.insertObject(data, filter, hints, sdesc, entity, entityParent, idParent);
			System.out.println("insert:" + retMap);
			return retMap;
		} catch (PermissionException e) {
			throw new RpcException(ERROR_FROM_METHOD, PERMISSION_DENIED, "DataService.insert:", e);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "DataService.insert:", e);
		}
	}

	public Map update(
			@PName(StoreDesc.STORE_ID) String storeId, 
			@PName("entity")           String entity, 
			@PName("entityChild")      @POptional String entityChild, 
			@PName("id")               @POptional String id, 
			@PName("idChild")          @POptional String idChild, 
			@PName("data")             Map data, 
			@PName("filter")           @POptional Map filter, 
			@PName("state")     	     @POptional String state, 
			@PName("hints")            @POptional Map hints
				) throws RpcException {
		StoreDesc sdesc = StoreDesc.get(storeId);
		try {
			Map retMap = null;
			String idParent = null;
			String entityParent = null;
			if (entityChild != null) {
				entityParent = entity;
				entity = entityChild;
				idParent = id;
				id = idChild;
			}
			if( hints == null){
				hints = new HashMap();
			}
			hints.put("state", state);
			retMap = m_dataLayer.updateObject(data, filter, hints, sdesc, entity, id, entityParent, idParent);
			System.out.println("update:" + retMap);
			return retMap;
		} catch (PermissionException e) {
			throw new RpcException(ERROR_FROM_METHOD, PERMISSION_DENIED, "DataService.update:", e);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "DataService.update:", e);
		}
	}

	public Map delete(
			@PName(StoreDesc.STORE_ID) String storeId, 
			@PName("entity")           String entity, 
			@PName("entityChild")      @POptional String entityChild, 
			@PName("id")               String id, 
			@PName("idChild")          @POptional String idChild, 
			@PName("data")             @POptional Map data, 
			@PName("filter")           @POptional Map filter, 
			@PName("hints")            @POptional Map hints
			) throws RpcException {
		StoreDesc sdesc = StoreDesc.get(storeId);
		try {
			Map retMap = null;
			if (entityChild != null) {
				entity = entityChild;
				id = idChild;
			}
			retMap = m_dataLayer.deleteObject(data, sdesc, entity, id);
			System.out.println("delete:" + retMap);
			return retMap;
		} catch (PermissionException e) {
			throw new RpcException(ERROR_FROM_METHOD, PERMISSION_DENIED, "DataService.delete:", e);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "DataService.delete:", e);
		}
	}

	public Map upload(
			@PName(StoreDesc.STORE_ID) String storeId, 
			@PName("entity")           String entity, 
			@PName("id")               @POptional String id, 
			@PName("fileMap")          Map fileMap) throws RpcException {
		StoreDesc sdesc = StoreDesc.get(storeId);
		try {
			System.out.println("upload:" + storeId + "/" + sdesc + "/" + fileMap);
			fileMap.put("id",id);
			return m_dataLayer.updateObject(fileMap, sdesc, entity, id);
		} catch (PermissionException e) {
			throw new RpcException(ERROR_FROM_METHOD, PERMISSION_DENIED, "DataService.upload:", e);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "DataService.upload:", e);
		}
	}
	public Map executeFilter(
			@PName(StoreDesc.STORE_ID) String storeId, 
			@PName("desc")             Map filterDesc, 
			@PName("params")             @POptional Map params, 
			@PName("options")             @POptional Map options, 
			@PName("checkParams")       @POptional  @PDefaultBool(false) Boolean checkParams, 
			@PName("mapping")          @POptional Map mapping) throws RpcException {
		StoreDesc sdesc = StoreDesc.get(storeId);
		SessionContext sc = m_dataLayer.getSessionContext(sdesc);
		try {
			if( params ==null) params = new HashMap();
			if( options == null){
				options = new HashMap();
			}
			options.put(SessionContext.CHECK_PARAMS, checkParams);
			Map ret = sc.executeFilter(filterDesc,params,options);
			if (ret != null && ret.get("rows") != null) {
				List retList = (List) ret.get("rows");
				if (mapping != null) {
					retList = m_utilsService.listToList(retList, mapping,null);
				}
				ret.put("rows", retList);
				return ret;
			}
			if( ret == null){
				ret = new HashMap();
			}
			ret.put("rows", new ArrayList());
			return ret;
		} catch (PermissionException e) {
			sc.handleException(e);
			throw new RpcException(ERROR_FROM_METHOD, PERMISSION_DENIED, "DataService.executeFilter", e);
		} catch (Throwable e) {
			sc.handleException(e);
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "DataService.executeFilter:", e);
		}finally{
			sc.handleFinally();
		}
	}

	public Map executeFilterByName(
			@PName(StoreDesc.STORE_ID) String storeId, 
			@PName("name")             String name, 
			@PName("params")             @POptional Map params, 
			@PName("checkParams")       @POptional  @PDefaultBool(false) Boolean checkParams, 
			@PName("withMeta")       @POptional  @PDefaultBool(false) Boolean withMeta, 
			@PName("pageSize")       @POptional  @PDefaultInt(0) Integer pageSize, 
			@PName("offset")       @POptional  @PDefaultInt(0) Integer offset, 
			@PName("mapping")          @POptional Map mapping) throws RpcException {
		StoreDesc sdesc = StoreDesc.get(storeId);
		SessionContext sc = m_dataLayer.getSessionContext(sdesc);
		try {
			Map options = new HashMap();
			options.put(SessionContext.CHECK_PARAMS, checkParams);
			options.put("pageSize", pageSize);
			options.put("offset", offset);
			options.put("withMeta", withMeta);
			Map ret = sc.executeNamedFilter(name,params,options);
			System.out.println("ret:" + ret);
			if (ret != null && ret.get("rows") != null) {
				List retList = (List) ret.get("rows");
				if (mapping != null) {
					retList = m_utilsService.listToList(retList, mapping,null);
				}
				ret.put("rows", retList);
				return ret;
			}
			if( ret == null){
				ret = new HashMap();
			}
			ret.put("rows", new ArrayList());
			return ret;
		} catch (PermissionException e) {
			sc.handleException(e);
			throw new RpcException(ERROR_FROM_METHOD, PERMISSION_DENIED, "DataService.executeFilterByName", e);
		} catch (Throwable e) {
			sc.handleException(e);
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "DataService.executeFilterByName:", e);
		}finally{
			sc.handleFinally();
		}
	}

	@RequiresRoles("admin")
	public Map executeQuery(
			@PName(StoreDesc.STORE_ID) String storeId, 
			@PName("params")             @POptional Map params, 
			@PName("fileMap")          @POptional Map fileMap, 
			@PName("sql")           @POptional String sql) throws RpcException {

		StoreDesc sdesc = StoreDesc.get(storeId);
		SessionContext sc = m_dataLayer.getSessionContext(sdesc);
		try {
			if (fileMap != null) {
				Map map = (Map) fileMap.get("importfile");
				sql = readFileToString(new File((String) map.get("storeLocation")));
				System.out.println("SQL:"+sql);
			}
			Map ret = m_dataLayer.querySql(sc,sdesc, params, sql);
			return ret;
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "DataService.executeQuery:", e);
		}finally{
			sc.handleFinally();
		}
	}
	/* END JSON-RPC-API*/
	private String createDownloadFile(List rows, StoreDesc sdesc, String entity, String format, String soptions, List fields, String aliases, String user) throws Exception {
		Map options = null;
		if (soptions != null) {
			options = (Map) m_ds.deserialize(soptions);
		} else {
			options = new HashMap();
			options.put("rowDelim", "UNIX");
			options.put("columnDelim", ",");
			options.put("quote", "\"");
			options.put("alwaysQuote", false);
		}
		List aliasesArray = null;
		if (aliases != null && aliases.length() > 0) {
			aliasesArray = (List) m_ds.deserialize(aliases);
		} else {
			aliasesArray = new ArrayList();
		}
		List fieldsArray = null;
		if (fields != null && fields.size() > 0) {
			fieldsArray = fields;
		} else {
			throw new Exception("DataService.createDownloadFile:fieldsArray_is_empty");
		}
		Map configForFieldsArray = getConfigForFieldsArray(sdesc, entity, fieldsArray);
		File outFile = File.createTempFile("simplewf", "." + format);
		FileOutputStream fos = new FileOutputStream(outFile);
		try {
			if (!format.equals("csv")) {
				m_reportingService.generateReport(rows, fieldsArray, aliasesArray, configForFieldsArray, format, options, fos);
			} else {
				m_reportingService.createCSV(rows, fieldsArray, aliasesArray, configForFieldsArray, options, fos);
			}
		} finally {
			fos.close();
		}
		return outFile.toString();
	}

	private String getString(Map m, String key, String _def) {
		try {
			if (m.get(key) != null) {
				return (String) m.get(key);
			}
		} catch (Exception e) {
		}
		return _def;
	}

	private String checkNull(Map m, String key, String msg) {
		if (m.get(key) != null) {
			return (String) m.get(key);
		}
		throw new RuntimeException(msg);
	}

	/************************************ C O N F I G ********************************************************/
	@Reference(dynamic = true)
	public void setReportingService(ReportingService paramReportingService) {
		this.m_reportingService = paramReportingService;
		System.out.println("DataServiceImpl.setReportingService:" + paramReportingService);
	}

	@Reference(dynamic = true)
	public void setEntityService(EntityService paramEntityService) {
		this.m_entityService = paramEntityService;
		System.out.println("DataServiceImpl.setEntityService:" + paramEntityService);
	}
	@Reference(dynamic = true)
	public void setUtilsService(UtilsService paramUtilsService) {
		this.m_utilsService = paramUtilsService;
		System.out.println("DataServiceImpl.setUtilsService:" + paramUtilsService);
	}

	@Reference(dynamic = true)
	public void setNucleusService(NucleusService paramNucleusService) {
		this.m_nucleusService = paramNucleusService;
		System.out.println("DataServiceImpl.setNucleusService:" + paramNucleusService);
	}

	@Reference(target = "(kind=jdo)", dynamic = true)
	public void setDataLayer(DataLayer paramDataLayer) {
		this.m_dataLayer = paramDataLayer;
		System.out.println("DataServiceImpl.setDataLayer:" + paramDataLayer);
	}

}
