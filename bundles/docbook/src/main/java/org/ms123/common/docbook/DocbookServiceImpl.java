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
package org.ms123.common.docbook;

import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.metatype.*;
import java.io.*;
import java.util.*;
import javax.servlet.http.*;
import javax.servlet.ServletOutputStream;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.ms123.common.data.api.DataLayer;
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
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.ms123.common.rpc.JsonRpcServlet.ERROR_FROM_METHOD;
import static org.ms123.common.rpc.JsonRpcServlet.INTERNAL_SERVER_ERROR;
import static org.ms123.common.rpc.JsonRpcServlet.PERMISSION_DENIED;


/** DocbookService implementation
 */
@SuppressWarnings("unchecked")
@Component(enabled = true, configurationPolicy = ConfigurationPolicy.optional, immediate = true, properties = { "rpc.prefix=docbook" })
public class DocbookServiceImpl extends BaseDocbookServiceImpl implements DocbookService {

	private static final Logger m_logger = LoggerFactory.getLogger(DocbookServiceImpl.class);

//	protected MetaData m_gitMetaData;

	public DocbookServiceImpl() {
	}

	protected void activate(BundleContext bundleContext, Map<?, ?> props) {
		System.out.println("DocbookServiceImpl.activate.props:" + props);
		m_bc = bundleContext;
	}

	protected void deactivate() throws Exception {
		System.out.println("DocbookServiceImpl deactivate");
	}

	public void jsonToDocbookPdf(String namespace, String json, Map<String, Object> paramsIn, OutputStream os) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Map paramsOut = new HashMap();
		oryxToDocbook(namespace, json, out, paramsIn, paramsOut);
		out.close();
		InputStream is = new ByteArrayInputStream(out.toByteArray());
		renderToPdf(namespace,is, os, paramsOut);
	}

	/* BEGIN JSON-RPC-API*/
	public void markdownToHtml(
			@PName("markdown")         String markdown, 
			@PName("fileMap")          @POptional Map fileMap, HttpServletResponse response) throws RpcException {
		try {
			if (fileMap != null) {
				Map map = (Map) fileMap.get("importfile");
				markdown = readFileToString(new File((String) map.get("storeLocation")));
			}
			markdownToHtml(markdown, response.getWriter());
			response.setContentType("application/html;charset=utf-8");
			response.addHeader("Content-Disposition", "attachment;filename=\"markdown.html\"");
			response.setStatus(HttpServletResponse.SC_OK);
			response.getWriter().close();
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "DocbookServiceImpl.markdownToHtml:", e);
		}
	}
	public void website(
			@PName(StoreDesc.NAMESPACE) String namespace, 
			@PName("name")         String name, 
				 HttpServletRequest request,
				 HttpServletResponse response
			) throws RpcException {
		try {
			long starttime= new java.util.Date().getTime();
			response.setContentType("application/xhtml+xml; charset=utf-8");
			String serverName = request.getServerName();

			//response.setContentType("text/html; charset=utf-8");
			websiteStart(serverName,namespace, name, response.getOutputStream(), request.getRequestURI());
			response.setStatus(HttpServletResponse.SC_OK);
			response.getOutputStream().close();
			System.out.println("website.dauer:"+( new java.util.Date().getTime()-starttime));
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "DocbookServiceImpl.website:", e);
		}
	}
	public Map websiteMain(
			@PName(StoreDesc.NAMESPACE) String namespace, 
			@PName("name")         String name
			) throws RpcException {
		try {
			long starttime= new java.util.Date().getTime();
			Map ret = _websiteMain(namespace, name);
			System.out.println("websiteMain.dauer:"+( new java.util.Date().getTime()-starttime));
			return ret;
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "DocbookServiceImpl.website:", e);
		}
	}
	public Map websiteFragment(
			@PName(StoreDesc.NAMESPACE) String namespace, 
			@PName("name")         String name, 
			@PName("resourceId")   @POptional   String resourceId,
			@PName("shapeId")      @POptional   String shapeId
			) throws RpcException {
		try {
			long starttime= new java.util.Date().getTime();
			Map ret = _websiteFragment(namespace, name, shapeId, resourceId);
			System.out.println("websiteFragment:"+( new java.util.Date().getTime()-starttime));
			return ret;
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "DocbookServiceImpl.website:", e);
		}
	}
	public Map websitePage(
			@PName(StoreDesc.NAMESPACE) String namespace, 
			@PName("name")         String name 
			) throws RpcException {
		try {
			Map ret = _websitePage(namespace, name);
			return ret;
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "DocbookServiceImpl.website:", e);
		}
	}
	public List shapePropertiesList(
			@PName(StoreDesc.NAMESPACE) String namespace, 
			@PName("criteria")         Map criteria,
			@PName("propertyNames")         List propertyNames
			) throws RpcException {
		try {
			return _shapePropertiesList(namespace, criteria,propertyNames);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "DocbookServiceImpl.selectShapes:", e);
		}
	}
	public void getAsset(
			@PName(StoreDesc.NAMESPACE) String namespace, 
			@PName("name")         String name, 
			@PName("type")         String type, 
				 HttpServletRequest request,
				 HttpServletResponse response
			) throws RpcException {
		try {
			_getAsset(namespace, name, type, request, response);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "DocbookServiceImpl.getAsset:", e);
		}
	}
	public void markdownToDocbookXmlOld(
			@PName("markdown")         String markdown, 
			@PName("fileMap")          @POptional Map fileMap, HttpServletResponse response) throws RpcException {
		try {
			if (fileMap != null) {
				Map map = (Map) fileMap.get("importfile");
				markdown = readFileToString(new File((String) map.get("storeLocation")));
			}
			markdownToDocbookOld(markdown, response.getOutputStream());
			response.setContentType("application/xml;charset=UTF-8");
			response.addHeader("Content-Disposition", "attachment;filename=\"docbook.xml\"");
			response.setStatus(HttpServletResponse.SC_OK);
			response.getOutputStream().close();
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "DocbookServiceImpl.markdownToDocbookXml:", e);
		}
	}

	public void markdownToDocbookXml(
			@PName("markdown")         String markdown, 
			@PName("fileMap")          @POptional Map fileMap, HttpServletResponse response) throws RpcException {
		try {
			if (fileMap != null) {
				Map map = (Map) fileMap.get("importfile");
				markdown = readFileToString(new File((String) map.get("storeLocation")));
			}
			markdownToDocbook(markdown, response.getOutputStream());
			response.setContentType("application/xml;charset=UTF-8");
			response.addHeader("Content-Disposition", "attachment;filename=\"docbook.xml\"");
			response.setStatus(HttpServletResponse.SC_OK);
			response.getOutputStream().close();
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "DocbookServiceImpl.markdownToDocbookXml:", e);
		}
	}

	public void markdownToDocbookPdf(
			@PName("markdown")         String markdown, 
			@PName("fileMap")          @POptional Map fileMap, HttpServletResponse response) throws RpcException {
		try {
			if (fileMap != null) {
				Map map = (Map) fileMap.get("importfile");
				markdown = readFileToString(new File((String) map.get("storeLocation")));
			}
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			markdownToDocbook(markdown, out);
			out.close();
			InputStream is = new ByteArrayInputStream(out.toByteArray());
			renderToPdf(null,is, response.getOutputStream(), new HashMap());
			response.setContentType("application/pdf;charset=UTF-8");
			response.addHeader("Content-Disposition", "attachment;filename=\"docbook.pdf\"");
			response.setStatus(HttpServletResponse.SC_OK);
			response.getOutputStream().close();
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "DocbookServiceImpl.markdownToDocbookPdf:", e);
		}
	}

	public String jsonToDocbookXml(
			@PName(StoreDesc.NAMESPACE) String namespace, 
			@PName("json")             String json, 
			@PName("params")           @POptional Map<String, Object> params, 
			@PName("fileMap")          @POptional Map fileMap, HttpServletResponse response) throws RpcException {
		try {
			if (fileMap != null) {
				System.out.println("fileMap:" + fileMap);
				Map map = (Map) fileMap.get("importfile");
				json = readFileToString(new File((String) map.get("storeLocation")));
			}
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			oryxToDocbook(namespace, json, out, params, new HashMap());
			System.out.println("oryxToDocbook:" + new String(out.toByteArray()));
			return new String(out.toByteArray());
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "DocbookServiceImpl.jsonToDocbookXml:", e);
		}
	}

	public void jsonToDocbookPdf(
			@PName(StoreDesc.NAMESPACE) String namespace, 
			@PName("json")             String json, 
			@PName("params")           @POptional Map<String, Object> params, 
			@PName("fileMap")          @POptional Map fileMap, HttpServletResponse response) throws RpcException {
		try {
			if (fileMap != null) {
				Map map = (Map) fileMap.get("importfile");
				json = readFileToString(new File((String) map.get("storeLocation")));
			}
			jsonToDocbookPdf(namespace, json, params, response.getOutputStream());
			response.setContentType("application/pdf;charset=UTF-8");
			response.addHeader("Content-Disposition", "attachment;filename=\"docbook.pdf\"");
			response.setStatus(HttpServletResponse.SC_OK);
			response.getOutputStream().close();
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "DocbookServiceImpl.jsonToDocbookPdf:", e);
		}
	}
	public String getHtml(
			@PName(StoreDesc.NAMESPACE) String namespace, 
			@PName("name")         String name 
			) throws RpcException {
		try {
			File indexFile = new File(System.getProperty("workspace"),name); 
				return readFileToString(indexFile);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "DocbookServiceImpl.getAsset:", e);
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
		System.out.println("DocbookServiceImpl.setGitService:" + gitService);
		m_gitService = gitService;
	}

	@Reference(dynamic = true)
	public void setPermissionService(PermissionService paramPermissionService) {
		this.m_permissionService = paramPermissionService;
		System.out.println("DocbookServiceImpl.setPermissionService:" + paramPermissionService);
	}

	@Reference(dynamic = true)
	public void setUtilsService(UtilsService paramUtilsService) {
		this.m_utilsService = paramUtilsService;
		System.out.println("DocbookServiceImpl.setUtilsService:" + paramUtilsService);
	}
}
