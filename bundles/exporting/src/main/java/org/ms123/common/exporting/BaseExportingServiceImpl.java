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

import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.jdo.Extent;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.transaction.UserTransaction;
import javax.xml.transform.stream.StreamSource;
import net.sf.sojo.common.*;
import net.sf.sojo.core.*;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.tika.Tika;
import org.milyn.container.*;
import org.milyn.*;
import org.milyn.payload.*;
import org.milyn.Smooks;
import org.milyn.SmooksFactory;
import com.Ostermiller.util.*;
import javax.servlet.http.*;
import javax.servlet.ServletOutputStream;
import javax.xml.transform.stream.*;
import org.ms123.common.data.api.DataLayer;
import org.ms123.common.data.api.SessionContext;
import org.ms123.common.entity.api.EntityService;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.permission.api.PermissionException;
import org.ms123.common.libhelper.Inflector;
import org.ms123.common.permission.api.PermissionService;
import org.ms123.common.nucleus.api.NucleusService;
import org.ms123.common.reporting.ReportingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** BaseExportingService implementation
 */
@SuppressWarnings("unchecked")
public class BaseExportingServiceImpl implements Constants {

	private static final Logger m_logger = LoggerFactory.getLogger(BaseExportingServiceImpl.class);

	protected Inflector m_inflector = Inflector.getInstance();

	protected DataLayer m_dataLayer;

	protected PermissionService m_permissionService;

	protected EntityService m_entityService;

	protected SmooksFactory m_smooksFactory;

	protected NucleusService m_nucleusService;

	protected ReportingService m_reportingService;

	protected JSONDeserializer m_ds = new JSONDeserializer();

	private ObjectUtil m_objUtils = new ObjectUtil();

	protected JSONSerializer m_js = new JSONSerializer();

	public BaseExportingServiceImpl() {
	}

	public Map smooksExport(String storeId, String entityName, Map filters, Map options, HttpServletResponse response) throws Exception {
		StoreDesc sdesc = StoreDesc.get(storeId);
		response.setContentType("application/xml;charset=UTF-8");
		Map filtersMap = filters;
		response.addHeader("Content-Disposition", "attachment;filename=\"" + options.get("filename") + "\"");
		Smooks smooks = m_smooksFactory.createInstance();
		ClassLoader previous = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(m_nucleusService.getClassLoader(sdesc));
		System.out.println("filtersMap:" + filtersMap);
		System.out.println("mainEntity:" + entityName);
		System.out.println("options:" + options);
		try {
			SessionContext sessionContext = m_dataLayer.getSessionContext(sdesc);
			List result = sessionContext.query(entityName, filtersMap);
			System.out.println("result:" + result);
			Class clazz = m_nucleusService.getClass(sdesc, m_inflector.getClassName(entityName));
			List nList = new ArrayList();
			nList.addAll(result);
			GenericReaderConfigurator grc = new GenericReaderConfigurator(BeanReader.class);
			smooks.setReaderConfig(grc);
			ExecutionContext executionContext = smooks.createExecutionContext();
			executionContext.setAttribute("sessionContext", sessionContext);
			executionContext.setAttribute("moduleName", entityName);
			executionContext.setAttribute("withNullValues", options.get("withNullValues"));
			JavaSource source = new JavaSource("result", nList);
			ServletOutputStream outputStream = response.getOutputStream();
			outputStream.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			smooks.filterSource(executionContext, source, new StreamResult(outputStream));
			return new HashMap();
		} finally {
			smooks.close();
			Thread.currentThread().setContextClassLoader(previous);
		}
	}

	protected String createReport(List rows, StoreDesc sdesc, String entity, String format, Map soptions, List fields, List aliases, String user, HttpServletResponse response) throws Exception {
		Map options = null;
		if (soptions != null) {
			options = soptions;
		} else {
			options = new HashMap();
			options.put("rowDelim", "UNIX");
			options.put("columnDelim", ",");
			options.put("quote", "\"");
			options.put("alwaysQuote", false);
		}
		List aliasesArray = null;
		if (aliases != null && aliases.size() > 0) {
			aliasesArray = aliases;
		} else {
			aliasesArray = new ArrayList();
		}
		List fieldsArray = null;
		if (fields != null && fields.size() > 0) {
			fieldsArray = fields;
		} else {
			throw new Exception("DataService.createDownloadFile:fieldsArray_is_empty");
		}
		System.out.println("fields:" + fields);
		System.out.println("aliases:" + aliases);
		System.out.println("fieldsArray:" + fieldsArray);
		System.out.println("aliasesArray:" + aliasesArray);
		Map configForFieldsArray = getConfigForFieldsArray(sdesc, entity, fieldsArray);
		if (!format.equals("csv")) {
			String filename = (String) options.get("filename");
			if (format.equals("pdf")) {
				response.setContentType("application/x-pdf");
				response.addHeader("Content-Disposition", "inline;filename=" + filename);
			} else if (format.equals("xls")) {
				response.setContentType("application/msexcel");
				response.addHeader("Content-Disposition", "inline;filename=" + filename);
			} else if (format.equals("html")) {
				response.setContentType("text/html");
				response.addHeader("Content-Disposition", "inline;filename=" + filename);
			}
			String retx = m_reportingService.generateReport(rows, fieldsArray, aliasesArray, configForFieldsArray, format, options, response.getOutputStream());
			response.flushBuffer();
			return retx;
		} else {
			response.setContentType("text/csv;charset=UTF-8");
			response.addHeader("Content-Disposition", "inline;filename=" + options.get("filename"));
			return m_reportingService.createCSV(rows, fieldsArray, aliasesArray, configForFieldsArray, options, response.getOutputStream());
		}
	}

	protected Map getConfigForFieldsArray(StoreDesc sdesc, String entityName, List<String> fieldsArray) {
		Map retMap = new HashMap();
		for (String field : fieldsArray) {
			int dot = field.indexOf(".");
			if (dot != -1) {
				String[] name = field.split("\\.");
				String mname = name[0];
				String fname = name[1];
				mname = org.ms123.common.utils.TypeUtils.getEntityForPath(m_nucleusService, sdesc, mname);
				Map _configMap = m_entityService.getPermittedFields(sdesc, mname);
				retMap.put(field, _configMap.get(fname));
			} else {
				Map _configMap = m_entityService.getPermittedFields(sdesc, entityName);
				retMap.put(field, _configMap.get(field));
			}
		}
		return retMap;
	}

	protected String getUserName() {
		return org.ms123.common.system.ThreadContext.getThreadContext().getUserName();
	}
}
