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
package org.ms123.common.exporting;

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
import org.ms123.common.nucleus.api.NucleusService;
import org.ms123.common.reporting.ReportingService;
import org.ms123.common.libhelper.Inflector;
import org.ms123.common.entity.api.EntityService;
import org.osgi.framework.BundleContext;
import javax.servlet.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.jdo.PersistenceManager;
import javax.jdo.Extent;
import javax.jdo.Query;
import javax.transaction.UserTransaction;
import org.apache.commons.beanutils.PropertyUtils;
import org.ms123.common.libhelper.Bean2Map;
import org.milyn.SmooksFactory;
import static org.apache.commons.beanutils.PropertyUtils.setProperty;
import static org.apache.commons.beanutils.PropertyUtils.getProperty;
import static java.text.MessageFormat.format;
import static org.ms123.common.rpc.JsonRpcServlet.ERROR_FROM_METHOD;
import static org.ms123.common.rpc.JsonRpcServlet.INTERNAL_SERVER_ERROR;
import static org.ms123.common.rpc.JsonRpcServlet.PERMISSION_DENIED;

/** ExportingService implementation
 */
@SuppressWarnings("unchecked")
@Component(enabled = true, configurationPolicy = ConfigurationPolicy.optional, immediate = true, properties = { "rpc.prefix=exporting" })
public class ExportingServiceImpl extends BaseExportingServiceImpl implements ExportingService, Constants {

	private static final Logger m_logger = LoggerFactory.getLogger(ExportingServiceImpl.class);

	public ExportingServiceImpl() {
	}

	protected void activate(BundleContext bundleContext, Map<?, ?> props) {
		System.out.println("ExportingServiceImpl.activate.props:" + m_dataLayer);
	}

	protected void deactivate() throws Exception {
		System.out.println("ExportingServiceImpl deactivate");
	}

	/* BEGIN JSON-RPC-API*/
	public Map exportData(
			@PName(StoreDesc.STORE_ID) String storeId, 
			@PName(MAIN_ENTITY)        String mainEntity, 
			@PName(FORMAT)             String format, 
			@PName(FILTERS)            @POptional Map filters, 
			@PName(OPTIONS)            @POptional Map options, 
			@PName(FIELDS)             @POptional List fields, 
			@PName(ORDERBY)            @POptional String orderby, 
			@PName(ALIASES)            @POptional List aliases, HttpServletResponse response) throws RpcException {
		try {
			if ("xml".equals(format)) {
				return smooksExport(storeId, mainEntity, filters, options, response);
			} else {
				List _fields = new ArrayList();
				_fields.addAll(fields);
				Map params = new HashMap();
				params.put("offset", 0);
				params.put("pageSize", 0);
				params.put("join", true);
				params.put("filter", filters);
				params.put("fields", _fields);
				params.put("orderby", orderby);
				Map ret = m_dataLayer.query(params, StoreDesc.get(storeId), mainEntity, null, null);
				String user = getUserName();
				String tmp = createReport((List) ret.get("rows"), StoreDesc.get(storeId), mainEntity, format, options, fields, aliases, user, response);
				Map tmpRet = new HashMap();
				tmpRet.put("result", tmp);
				return tmpRet;
			}
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "ExportingService.exportData:", e);
		}
	}

	public void exportJSON(
			@PName(StoreDesc.STORE_ID) String storeId, 
			@PName("filename") String filename, 
			@PName(FILTERDESC)             Map filterDesc, 
			HttpServletResponse response) throws RpcException {
		try {
			SessionContext sc = m_dataLayer.getSessionContext(StoreDesc.get(storeId));
			Map result = sc.executeFilter( filterDesc,null);
			response.setContentType("application/json; charset=utf-8");
			JSONSerializer js = new JSONSerializer();
			js.prettyPrint(true);
			String ret = js.deepSerialize(result.get("rows"));

			response.addHeader("Content-Disposition", "attachment;filename=\""+filename+"\"");
			response.setStatus(HttpServletResponse.SC_OK);
			response.getWriter().write(ret);
			response.getWriter().flush();
			response.getWriter().close();
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "ExportingService.exportJSON:", e);
		}
	}

	/* END JSON-RPC-API*/
	@Reference(target = "(kind=jdo)", dynamic = true, optional = true)
	public void setDataLayer(DataLayer dataLayer) {
		System.out.println("ExportingServiceImpl.setDataLayer:" + dataLayer);
		m_dataLayer = dataLayer;
	}

	@Reference(dynamic = true)
	public void setPermissionService(PermissionService paramPermissionService) {
		this.m_permissionService = paramPermissionService;
		System.out.println("ExportingServiceImpl.setPermissionService:" + paramPermissionService);
	}

	@Reference(dynamic = true)
	public void setSmooksFactory(SmooksFactory paramSmooksFactory) {
		m_smooksFactory = paramSmooksFactory;
		System.out.println("ExportingServiceImpl.setSmooksFactory:" + paramSmooksFactory);
	}

	@Reference(dynamic = true)
	public void setEntityService(EntityService paramEntityService) {
		m_entityService = paramEntityService;
		System.out.println("ExportingServiceImpl.setEntityService:" + paramEntityService);
	}

	@Reference(dynamic = true)
	public void setNucleusService(NucleusService paramNucleusService) {
		this.m_nucleusService = paramNucleusService;
		System.out.println("ExportingServiceImpl.setNucleusService:" + paramNucleusService);
	}

	@Reference(dynamic = true)
	public void setReportingService(ReportingService paramReportingService) {
		this.m_reportingService = paramReportingService;
		System.out.println("DataServiceImpl.setReportingService:" + paramReportingService);
	}
}
