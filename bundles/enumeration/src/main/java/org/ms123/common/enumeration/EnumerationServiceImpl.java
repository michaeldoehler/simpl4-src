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
package org.ms123.common.enumeration;

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
import org.ms123.common.utils.UtilsService;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.ms123.common.rpc.JsonRpcServlet.ERROR_FROM_METHOD;
import static org.ms123.common.rpc.JsonRpcServlet.INTERNAL_SERVER_ERROR;
import static org.ms123.common.rpc.JsonRpcServlet.PERMISSION_DENIED;

/** EnumerationService implementation
 */
@SuppressWarnings("unchecked")
@Component(enabled = true, configurationPolicy = ConfigurationPolicy.optional, immediate = true, properties = { "rpc.prefix=enumeration" })
public class EnumerationServiceImpl implements EnumerationService {

	private static final Logger m_logger = LoggerFactory.getLogger(EnumerationServiceImpl.class);

	private static final String ENUMERATION_ENTITY = "enumeration";

	private static final String NAME = "name";

	protected MetaData m_gitMetaData;

	private PermissionService m_permissionService;

	private UtilsService m_utilsService;

	protected JSONDeserializer m_ds = new JSONDeserializer();

	protected JSONSerializer m_js = new JSONSerializer();

	public EnumerationServiceImpl() {
	}

	protected void activate(BundleContext bundleContext, Map<?, ?> props) {
		System.out.println("EnumerationServiceImpl.activate.props:" + props);
	}

	protected void deactivate() throws Exception {
	}

	private Map read(InputStream is,Map options) throws Exception {
		CSVParse p = getCSVParser(is, options);
		String headerRow[] = p.getLine();
		Map headerMap = createFields(headerRow);
		String[] keys = (String[]) headerMap.get("keys");
		List<Map> dataList = new ArrayList();
		while (true) {
			String values[] = p.getLine();
			if (values == null) {
				break;
			}
			String typ = values[0];
			Map<String, Object> properties = new HashMap();
			properties.put("key", typ);
			for (int c = 1; c < values.length; c++) {
				String val = values[c];
				String key = keys[c];
				if ("invalid_from".equals(key)) {
					properties.put(key, getCalendar(val));
				} else if ("invalid".equals(key)) {
					Boolean v = getBoolean(val);
					properties.put(key, v);
				} else {
					properties.put(key, val);
				}
			}
			if (properties.get("invalid") == null) {
				properties.put("invalid", false);
			}
			if (properties.get("invalid_from") == null) {
				properties.put("invalid_from", getCalendar(null));
			}
			if (properties.get("sort") == null) {
				properties.put("sort", 0);
			}
			dataList.add(properties);
		}
		Map result = new HashMap();
		result.put("fieldList", headerMap.get("fields"));
		result.put("dataList", dataList);
		return result;
	}

	private CSVParse getCSVParser(InputStream is, Map options) {
		char delimeter = ',';
		String columnDelim = (String) options.get("columnDelim");
		System.out.println("options:" + options);
		if (columnDelim == null) {
			columnDelim=";";
		}
		if (columnDelim.toLowerCase().indexOf("tab") != -1) {
			delimeter = '\t';
		}
		if (columnDelim.length() > 0) {
			delimeter = columnDelim.charAt(0);
		}
		char quote = '"';
		if( options.get("quote") == null){
			options.put("quote", "");
		}
		if (((String) options.get("quote")).length() > 0) {
			quote = ((String) options.get("quote")).charAt(0);
		}
		CSVParse p;
		if (options.get("excel") != null) {
			p = new ExcelCSVParser(is, delimeter);
		} else {
			p = new CSVParser(is, delimeter);
		}
		p.changeQuote(quote);
		return p;
	}
	private Map createFields(String[] headerRow) throws Exception {
		String keys[] = new String[headerRow.length];
		Map hMap = new HashMap();
		hMap.put("keys", keys);
		List fieldList = new ArrayList();
		for (int c = 0; c < headerRow.length; c++) {
			String[] names = headerRow[c].split(":");
			String field = names[0];
			String description = null;
			if (names.length == 1) {
				description = names[0];
			} else {
				description = names[1];
			}
			keys[c] = field;
			if (c == 0 || c >= (headerRow.length - 3))
				continue;
			if (field == "sort") {
				continue;
			}
			if (field == "key") {
				continue;
			}
			if (field == "invalid") {
				continue;
			}
			if (field == "invalid_from") {
				continue;
			}
			Map<String, Object> properties = new HashMap();
			properties.put("description", description);
			properties.put("fieldname", field);
			fieldList.add(properties);
		}
		hMap.put("fields", fieldList);
		return hMap;
	}

	private Date getCalendar(String val) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
		Calendar cal = null;
		try {
			Date date = dateFormat.parse(val);
			cal = Calendar.getInstance();
			cal.setTime(date);
		} catch (Exception e) {
			cal = Calendar.getInstance();
			cal.set(Calendar.YEAR, 2100);
			cal.set(Calendar.DAY_OF_MONTH, 31);
			cal.set(Calendar.MONTH, 11);
		}
		return cal.getTime();
	}

	private Boolean getBoolean(String val) {
		if (val == null || val.length() == 0) {
			return true;
		}
		val = val.toLowerCase();
		if ("n".equals(val)) {
			return false;
		}
		if ("f".equals(val)) {
			return false;
		}
		if ("false".equals(val)) {
			return false;
		}
		return true;
	}

	/* BEGIN JSON-RPC-API*/
	@RequiresRoles("admin")
	public void createEnumeration(
			@PName(StoreDesc.NAMESPACE) String namespace, 
			@PName(NAME)               String name, 
			@PName("options")         @POptional     Map options, 
			@PName("fileMap")          @POptional Map fileMap) throws RpcException {
		try {
			System.out.println("fileMap:" + fileMap);
			Map data = null;
			if (fileMap != null) {
				Map map = (Map) fileMap.get("importfile");
				InputStream is = new FileInputStream(new File((String) map.get("storeLocation")));
				if( options==null)options=new HashMap();
				data = read(is,options);
				is.close();
			} else {
				data = new HashMap();
				data.put("fieldList", new ArrayList());
				data.put("dataList", new ArrayList());
			}
			try {
				m_gitMetaData.saveEnumeration(namespace, name, data);
			} catch (Exception e) {
			}
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "EnumerationServiceImpl.createEnumeration:", e);
		} finally {
		}
	}

	public List getEnumerations(
			@PName(StoreDesc.NAMESPACE) String namespace) throws RpcException {
		try {
			return m_gitMetaData.getEnumerations(namespace);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "EnumerationServiceImpl.getEnumerations:", e);
		} finally {
		}
	}

	public Map<String, List> getEnumeration(
			@PName(StoreDesc.NAMESPACE) String namespace, 
			@PName(NAME)               String name) throws RpcException {
		try {
			return m_gitMetaData.getEnumeration(namespace, name);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "EnumerationServiceImpl.getEnumeration:", e);
		}
	}

	public List<Map> get(
			@PName(StoreDesc.NAMESPACE) String namespace, 
			@PName(NAME)               String name, 
			@PName("mapping")                          @POptional Map mapping, 
			@PName("filter")                             @POptional String filter) throws RpcException {
		try {
			Map m = m_gitMetaData.getEnumeration(namespace, name);
			List list = (List) m.get("dataList");
			return m_utilsService.listToList(list, mapping, filter,true);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "EnumerationServiceImpl.get:", e);
		}
	}

	public void saveEnumeration(
			@PName(StoreDesc.NAMESPACE) String namespace, 
			@PName(NAME)               String name, 
			@PName("data")             Map data) throws RpcException {
		try {
			m_gitMetaData.saveEnumeration(namespace, name, data);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "EnumerationServiceImpl.saveEnumeration:", e);
		} finally {
		}
	}

	@RequiresRoles("admin")
	public void deleteEnumeration(
			@PName(StoreDesc.NAMESPACE) String namespace, 
			@PName(NAME)               String name) throws RpcException {
		try {
			m_gitMetaData.deleteEnumeration(namespace, name);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "EnumerationServiceImpl.deleteEnumeration:", e);
		} finally {
		}
	}

	/* END JSON-RPC-API*/
	@Reference(dynamic = true, optional = true)
	public void setGitService(GitService gitService) {
		System.out.println("EnumerationServiceImpl.setGitService:" + gitService);
		m_gitMetaData = new GitMetaDataImpl(gitService);
	}

	@Reference(dynamic = true)
	public void setPermissionService(PermissionService paramPermissionService) {
		this.m_permissionService = paramPermissionService;
		System.out.println("EnumerationServiceImpl.setPermissionService:" + paramPermissionService);
	}

	@Reference(dynamic = true)
	public void setUtilsService(UtilsService paramUtilsService) {
		this.m_utilsService = paramUtilsService;
		System.out.println("EnumerationServiceImpl.setUtilsService:" + paramUtilsService);
	}
}
