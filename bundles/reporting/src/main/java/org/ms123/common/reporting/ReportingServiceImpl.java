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
package org.ms123.common.reporting;

import aQute.bnd.annotation.component.*;
import aQute.bnd.annotation.metatype.*;
import ar.com.fdvs.dj.core.DynamicJasperHelper;
import ar.com.fdvs.dj.core.layout.LayoutManager;
import ar.com.fdvs.dj.core.layout.ListLayoutManager;
import ar.com.fdvs.dj.domain.*;
import ar.com.fdvs.dj.domain.builders.*;
import ar.com.fdvs.dj.domain.constants.*;
import ar.com.fdvs.dj.domain.entities.columns.*;
import com.Ostermiller.util.*;
import java.awt.Color;
import java.io.*;
import java.text.*;
import java.util.*;
import javax.script.ScriptEngineManager;
import javax.servlet.http.*;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.*;
import net.sf.jasperreports.engine.export.*;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
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
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.ms123.common.rpc.JsonRpcServlet.ERROR_FROM_METHOD;
import static org.ms123.common.rpc.JsonRpcServlet.INTERNAL_SERVER_ERROR;
import static org.ms123.common.rpc.JsonRpcServlet.PERMISSION_DENIED;

/** ReportingService implementation
 */
@SuppressWarnings("unchecked")
@Component(enabled = true, configurationPolicy = ConfigurationPolicy.optional, immediate = true, properties = { "rpc.prefix=report" })
public class ReportingServiceImpl implements ReportingService {

	private static final Logger m_logger = LoggerFactory.getLogger(ReportingServiceImpl.class);

	private String REPORT_ENTITY = "report";

	protected MetaData m_gitMetaData;

	private PermissionService m_permissionService;

	protected void activate(final ComponentContext context) {
	}

	protected void deactivate(ComponentContext context) {
	}

	public String generateReport(Collection<Map> data, List<String> fieldsArray, List<String> aliasesArray, Map configMap, String type, Map options, OutputStream outputStream) throws Exception {
		Map params = new HashMap();
		System.out.println("xgenerateReport.options:" + options);
		System.out.println("xgenerateReport.datasize:" + data.size());
		System.out.println("xgenerateReport.data:" + data);
		System.out.println("xgenerateReport.type:" + type + "," + fieldsArray);
		Style detailStyle = new Style();
		Style headerStyle = new Style();
		headerStyle.setFont(Font.ARIAL_BIG_BOLD);
		//		headerStyle.setBorderBottom(Border.PEN_2_POINT);
		headerStyle.setHorizontalAlign(HorizontalAlign.CENTER);
		headerStyle.setVerticalAlign(VerticalAlign.MIDDLE);
		headerStyle.setBackgroundColor(Color.decode("#2891eb"));
		headerStyle.setTextColor(Color.WHITE);
		headerStyle.setTransparency(Transparency.OPAQUE);
		int percentEach = 100 / fieldsArray.size();
		System.out.println("xgenerateReport.percentEachColumn:" + percentEach);
		DynamicReportBuilder drb = new DynamicReportBuilder();
		int i = 0;
		for (String field : fieldsArray) {
			boolean isDate = false;
			ColumnBuilder col = ColumnBuilder.getNew();
			String dt = String.class.getName();
			if( configMap.get(field) instanceof Map){
				Map<String, String> cm = (Map) configMap.get(field);
				dt = getDatatype(cm);
			}else{
				dt = (String) configMap.get(field);
			}
			if( dt.toLowerCase().endsWith("date")){
				isDate = true;
			}
			if (aliasesArray != null && aliasesArray.get(i) != null && aliasesArray.get(i).length() > 0 && !"null".equals(aliasesArray.get(i).toString())) {
				//				col.setColumnProperty(aliasesArray[i].toString(), dt);
				col.setTitle(aliasesArray.get(i).toString());
			} else {
				col.setTitle(field.replace('$', '!'));
			}
			//Replace $-Sign in Vars, Jasper don't like it
System.out.println("Field:"+field+"|"+dt+"|"+isDate);
			col.setColumnProperty(field.replace('$', '!'), dt);
			List<Map> data_new = new ArrayList<Map>();
			for (Map m : data) {
				Map nm = new HashMap();
				Iterator<String> it = m.keySet().iterator();
				while (it.hasNext()) {
					String key = it.next();
					nm.put(key.replace('$', '!'), m.get(key));
				}
				data_new.add(nm);
			}
			data = data_new;
			if (isDate) {
				col.setCustomExpression(new DateExpression(field));
			}
			col.setWidth(new Integer(percentEach));
			col.setStyle(detailStyle);
			col.setHeaderStyle(headerStyle);
			drb.addColumn(col.build());
			i++;
		}
		drb.setPrintBackgroundOnOddRows(true);
		if (type == "xls" || type == "csv") {
			drb.setPrintBackgroundOnOddRows(false);
			drb.setIgnorePagination(true);
			drb.setMargins(0, 0, 0, 0);
		}
		if (type == "html") {
			//drb.setPrintBackgroundOnOddRows(false)			
			drb.setIgnorePagination(true);
			drb.setMargins(0, 0, 0, 0);
		}
		if (options.get("header") == null || ((Boolean)options.get("header")) == false) {
			drb.setPrintColumnNames(false);
		}
		drb.setUseFullPageWidth(true);
		drb.setPageSizeAndOrientation(Page.Page_A4_Portrait());
		if (type == "pdf" && options.get("landscape") != null) {
			drb.setPageSizeAndOrientation(Page.Page_A4_Landscape());
		}
		DynamicReport dr = drb.build();
		JasperReport jr = DynamicJasperHelper.generateJasperReport(dr, new ListLayoutManager(), params);
		m_logger.info("generateReport.jr:" + jr);
		System.out.println("generateReport.jr:" + jr);
		JasperPrint jp = net.sf.jasperreports.engine.JasperFillManager.fillReport(jr, params, new JRMapCollectionDataSource(data));
		m_logger.info("activate.jp:" + jp);
		System.out.println("activate.jp:" + jp);
		StringBuffer sb = new StringBuffer();
		if ("pdf".equals(type)) {
			JRPdfExporter exporter = new JRPdfExporter();
			exporter.setParameter(JRExporterParameter.JASPER_PRINT, jp);
			exporter.setParameter(JRExporterParameter.OUTPUT_STREAM, outputStream);
			exporter.exportReport();
			return "";
		}
		if ("xls".equals(type)) {
			JRXlsExporter exporter = new JRXlsExporter();
			exporter.setParameter(JRExporterParameter.JASPER_PRINT, jp);
			exporter.setParameter(JRExporterParameter.OUTPUT_STREAM, outputStream);
			exporter.setParameter(JRXlsExporterParameter.IS_ONE_PAGE_PER_SHEET, Boolean.FALSE);
			exporter.setParameter(JRXlsExporterParameter.IS_REMOVE_EMPTY_SPACE_BETWEEN_ROWS, Boolean.TRUE);
			exporter.setParameter(JRXlsExporterParameter.IS_WHITE_PAGE_BACKGROUND, Boolean.FALSE);
			exporter.exportReport();
			//response.flushBuffer();
			return "";
		}
		if ("csv".equals(type)) {
			JRCsvExporter exporter = new JRCsvExporter();
			exporter.setParameter(JRExporterParameter.JASPER_PRINT, jp);
			exporter.setParameter(JRExporterParameter.OUTPUT_STREAM, outputStream);
			exporter.exportReport();
			return "";
		}
		if ("html".equals(type)) {
			JRHtmlExporter exporter = new JRHtmlExporter();
			exporter.setParameter(JRExporterParameter.JASPER_PRINT, jp);
			exporter.setParameter(JRHtmlExporterParameter.IS_USING_IMAGES_TO_ALIGN, Boolean.FALSE);
			exporter.setParameter(JRExporterParameter.OUTPUT_STREAM, outputStream);
			exporter.exportReport();
			return "";
		}
		return "";
	}
	private String getDatatype(Map<String,String> cm){
		boolean isDate = false;
		String dt = String.class.getName();
		if (cm != null) {
			if (cm.get("datatype").startsWith("boolean")) {
				dt = Boolean.class.getName();
			}
			if (cm.get("datatype").startsWith("number")) {
				dt = Number.class.getName();
			}
			if (cm.get("datatype").startsWith("integer")) {
				dt = Integer.class.getName();
			}
			if (cm.get("datatype").startsWith("decimal")) {
				dt = Double.class.getName();
			}
			if (cm.get("datatype").startsWith("long")) {
				dt = Long.class.getName();
			}
			if (cm.get("datatype").startsWith("date")) {
				dt = Date.class.getName();
			}
		}
		return dt;
	}

	private static class DateExpression implements CustomExpression {

		String field;

		public DateExpression(String field) {
			this.field = field.replace('$', '!');
		}

		public Object evaluate(Map fields, Map variables, Map parameters) {
			Calendar cal = Calendar.getInstance();
			try {
				if( fields.get(field) == null) return "";
				long millis = Long.parseLong((String) (fields.get(field)));
				cal.setTimeInMillis(millis);
				DateFormat formatter = java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT, new Locale("de", "DE"));
				Object result = formatter.format(cal.getTime());
				return result;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return "";
		}

		public String getClassName() {
			return String.class.getName();
		}
	}

	public String createCSV(Collection<Map> data, List<String> fieldsArray, List<String> aliasesArray, Map configMap, Map options, OutputStream outputStream) {
		m_logger.info("_createCSV.aliases:" + aliasesArray);
		m_logger.info("_createCSV.fields:" + fieldsArray);
		m_logger.info("_createCSV.options:" + options);
		OutputStreamWriter ow = null;
		try{
			ow = new OutputStreamWriter(outputStream, "UTF-8");
		}catch(Exception e){
		}
		CSVPrint ep = null;
		String lineEnding = (String) options.get("rowDelim");
		if (lineEnding.indexOf("CR/LF") != -1) {
			lineEnding = "\r\n";
		}
		if (lineEnding.indexOf("UNIX") != -1) {
			lineEnding = "\n";
		}
		String columnDelim = (String) options.get("columnDelim");
		char _columnDelim = columnDelim.charAt(0);
		if (columnDelim.toLowerCase().indexOf("tab") != -1) {
			_columnDelim = '\t';
		}
		if (columnDelim.length() > 1) {
			_columnDelim = columnDelim.charAt(0);
		}
		char quote = '"';
		if (((String) options.get("quote")).length() > 0) {
			quote = ((String) options.get("quote")).charAt(0);
		}
		if (options.get("excel") != null) {
			ep = new ExcelCSVPrinter(ow, (char) quote, _columnDelim, lineEnding, (Boolean) options.get("alwaysQuote"), true);
		} else {
			ep = new CSVPrinter(ow, '#', (char) quote, _columnDelim, lineEnding, (Boolean) options.get("alwaysQuote"), true);
		}
		if (options.get("header") != null) {
			int i = 0;
			for (String field : fieldsArray) {
				if (aliasesArray != null && aliasesArray.get(i) != null && aliasesArray.get(i).toString().length() > 0 && !"null".equals(aliasesArray.get(i).toString())) {
					ep.print(aliasesArray.get(i).toString());
				} else {
					ep.print(field);
				}
				i++;
			}
			ep.println();
		}
		Iterator it = data.iterator();
		while (it.hasNext()) {
			Map line = (Map) it.next();
			for (String field : fieldsArray) {
				Object value = line.get(field);
				String dt = "";
				boolean isDate = false;
				if( configMap.get(field) instanceof Map){
					Map<String, String> cm = (Map) configMap.get(field);
					dt = getDatatype(cm);
				}else{
					dt = (String) configMap.get(field);
				}
				if( dt.toLowerCase().startsWith("date") || dt.toLowerCase().equals("java.util.date")){
					isDate = true;
				}
				if (isDate) {
					value = _formatDate(value);
				}
				if (value == null) {
					ep.print("");
				} else {
					ep.print(String.valueOf(value));
				}
			}
			ep.println();
		}
		try {
			ep.close();
			ow.close();
		} catch (Exception e) {
			throw new RuntimeException("ReportingServiceImpl..createCSV", e);
		}
		return "";
	}

	private String _formatDate(Object value) {
		Calendar cal = Calendar.getInstance();
		try {
			long millis = Long.parseLong(String.valueOf(value));
			cal.setTimeInMillis(millis);
			java.text.DateFormat formatter = java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT, new Locale("de", "DE"));
			String result = formatter.format(cal.getTime());
			return result;
		} catch (Exception e) {
		}
		return "";
	}

	/* BEGIN JSON-RPC-API*/
	public List getReports(
			@PName("namespace")        String namespace) throws RpcException {
		try {
			return m_gitMetaData.getReports(namespace);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "ReportingServiceImpl.getReports:", e);
		} finally {
		}
	}
	public Map getReport(
			@PName("namespace")        String namespace, 
			@PName("name")             String name ) throws RpcException {
		try {
			return m_gitMetaData.getReport(namespace,name);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "ReportingServiceImpl.getReport:", e);
		} finally {
		}
	}

	public void saveReport(
			@PName("namespace")        String namespace, 
			@PName("name")             String name, 
			@PName("report")         @POptional Map reportData) throws RpcException {
		try {
			m_gitMetaData.saveReport(namespace,name,reportData);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "ReportingServiceImpl.saveReport:", e);
		} finally {
		}
	}

	public void deleteReport(
			@PName("namespace")        String namespace, 
			@PName("name")               String name) throws RpcException {
		try {
			m_gitMetaData.deleteReport(namespace,name);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "ReportingServiceImpl.deleteReport:", e);
		} finally {
		}
	}

	/* END JSON-RPC-API*/
	@Reference(dynamic = true, optional = true)
	public void setGitService(GitService gitService) {
		System.out.println("ReportingServiceImpl.setGitService:" + gitService);
		m_gitMetaData = new GitMetaDataImpl(gitService);
	}
	@Reference(dynamic = true)
	public void setPermissionService(PermissionService paramPermissionService) {
		this.m_permissionService = paramPermissionService;
		System.out.println("ReportingServiceImpl.setPermissionService:" + paramPermissionService);
	}
}
