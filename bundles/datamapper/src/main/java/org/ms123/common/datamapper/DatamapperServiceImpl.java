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
package org.ms123.common.datamapper;

import java.io.FileInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.ms123.common.data.api.DataLayer;
import org.ms123.common.auth.api.AuthService;
import org.ms123.common.utils.UtilsService;
import org.ms123.common.data.api.SessionContext;
import org.ms123.common.git.GitService;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.nucleus.api.NucleusService;
import org.ms123.common.permission.api.PermissionService;
import org.ms123.common.reporting.ReportingService;
import org.ms123.common.libhelper.Inflector;
import aQute.bnd.annotation.metatype.*;
import aQute.bnd.annotation.component.*;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.ms123.common.rpc.PName;
import org.ms123.common.rpc.POptional;
import org.ms123.common.rpc.RpcException;
import static org.ms123.common.rpc.JsonRpcServlet.ERROR_FROM_METHOD;
import static org.ms123.common.rpc.JsonRpcServlet.INTERNAL_SERVER_ERROR;
import static org.ms123.common.rpc.JsonRpcServlet.PERMISSION_DENIED;
import javax.servlet.http.*;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.apache.commons.io.FileUtils.copyInputStreamToFile;
import org.ms123.common.libhelper.Base64;

/** DatamapperService implementation
 */
@Component(enabled = true, configurationPolicy = ConfigurationPolicy.optional, immediate = true, properties = { "rpc.prefix=datamapper" })
public class DatamapperServiceImpl extends BaseDatamapperServiceImpl implements DatamapperService {

	private static final Logger m_logger = LoggerFactory.getLogger(DatamapperServiceImpl.class);

	public DatamapperServiceImpl() {
		m_logger.info("DatamapperServiceImpl construct");
	}

	protected void activate(BundleContext bundleContext, Map props) {
		System.out.println("DatamapperServiceImpl.activate.props:" + props);
		try {
			m_logger.info("DatamapperServiceImpl.activate -->");
			Bundle b = bundleContext.getBundle();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void deactivate() throws Exception {
		m_logger.info("deactivate");
		System.out.println("DatamapperServiceImpl deactivate");
	}

	/*BEGIN JSON-RPC-API*/
	public Object transform(
			@PName(StoreDesc.NAMESPACE) @POptional String namespace, 
			@PName("config")           @POptional Map config, 
			@PName("configName")       @POptional String configName, 
			@PName("fileId")           @POptional String fileId, 
			@PName("data")             @POptional String data) throws RpcException {
		try {
			if (data == null && fileId != null) {
				File ws = new File(System.getProperty("workspace"), "datamapper");
				File fileIn = new File(ws, fileId);
				data = readFileToString(fileIn);
			}
			if (data == null && fileId == null) {
				throw new RuntimeException("data or fileId needed");
			}
			if (config == null && configName == null) {
				throw new RuntimeException("config or configName needed");
			}
			return transform(namespace, config, configName, data);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "DatamapperService.transform:", e);
		}
	}
	public Object getMetaData2( 
			@PName("config")           Map config,
			@PName("fileContent")      @POptional String fileContent ) throws RpcException {
		return getMetaData(config,null,fileContent, null);
	}

	public Object getMetaData(
			@PName("config")           Map config,
			@PName("fileId")           @POptional String fileId,
			@PName("fileContent")      @POptional String fileContent,
			@PName("fileMap")          @POptional Map fileMap) throws RpcException {
		try {
			if (fileContent == null && fileId == null && fileMap==null) {
				throw new RuntimeException("fileContent,fileId or fileMap needed");
			}
			if( fileContent == null){
				if( fileMap != null){
					Map importFile = (Map) fileMap.get("importfile");
					String storeLocation = (String) importFile.get("storeLocation");
					fileContent = readFileToString(new File(storeLocation));
				} else if (fileId != null) {
					File ws = new File(System.getProperty("workspace"), "datamapper");
					File fileIn = new File(ws, fileId);
					fileContent = readFileToString(fileIn);
				}
			}else if (fileContent.startsWith("data:")){
				int ind = fileContent.indexOf(";base64,");
				fileContent = new String(Base64.decode(fileContent.substring(ind+8)));
			}
			MetaData jm = null;
			String format = (String)config.get("format");
			if( format.equals(Constants.FORMAT_JSON ) ||  format.equals(Constants.FORMAT_MAP)){
				jm = new JsonMetaData();
			}else if( format.equals(Constants.FORMAT_XML)){
				jm = new XmlMetaData();
			}else if( format.equals(Constants.FORMAT_CSV)){
				jm = new CsvMetaData(config);
			}
			return jm.generateMetadata(new StringReader(fileContent));
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "DatamapperService.getMetaData:", e);
		}
	}

	public void upload(
			@PName("fileId")           String fileId, 
			@PName("fileContent")      @POptional String fileContent, 
			@PName("fileMap")          @POptional Map fileMap) throws RpcException {
		try {
			if( fileMap == null && fileContent == null){
					throw new RuntimeException("fileMap or fileContent is needed");
			}
			File ws = new File(System.getProperty("workspace"), "datamapper");
			if (!ws.exists()) {
				ws.mkdirs();
			}
			File outFile = new File(ws, fileId);
			byte[] bytes = null;
			if( fileMap != null){
				Map importFile = (Map) fileMap.get("importfile");
				String storeLocation = (String) importFile.get("storeLocation");
				InputStream is = new FileInputStream(new File(storeLocation));
				copyInputStreamToFile(is, outFile);
			}else if (fileContent != null && fileContent.startsWith("data:")){
				int ind = fileContent.indexOf(";base64,");
				Base64.decodeToFile(fileContent.substring(ind+8),outFile.toString());
			}
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "DatamapperService.upload:", e);
		}
	}

	/*END JSON-RPC-API*/
	@Reference(dynamic = true, optional = true)
	public void setReportingService(ReportingService service) {
		System.out.println("DatamapperServiceImpl.setReportingService:" + service);
		m_reportingService = service;
	}
	@Reference(dynamic = true, optional = true)
	public void setNucleusService(NucleusService service) {
		System.out.println("DatamapperServiceImpl.setNucleusService:" + service);
		m_nucleusService = service;
	}
	@Reference(dynamic = true, optional = true)
	public void setGitService(GitService gitService) {
		System.out.println("DatamapperServiceImpl.setGitService:" + gitService);
		m_gitService = gitService;
	}
	@Reference(target = "(kind=jdo)", dynamic = true, optional = true)
	public void setDataLayer(DataLayer dataLayer) {
		System.out.println("DatamapperServiceImpl.setDataLayer:" + dataLayer);
		m_dataLayer = dataLayer;
	}
}
