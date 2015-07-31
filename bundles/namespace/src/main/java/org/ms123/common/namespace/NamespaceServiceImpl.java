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
package org.ms123.common.namespace;

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
import org.ms123.common.git.GitServiceImpl;
import org.ms123.common.rpc.PDefaultBool;
import org.ms123.common.rpc.PDefaultFloat;
import org.ms123.common.rpc.PDefaultInt;
import org.ms123.common.rpc.PDefaultLong;
import org.ms123.common.rpc.PDefaultString;
import org.ms123.common.rpc.PName;
import org.ms123.common.rpc.POptional;
import org.ms123.common.rpc.RpcException;
import org.ms123.common.store.StoreDesc;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.ms123.common.rpc.JsonRpcServlet.ERROR_FROM_METHOD;
import static org.ms123.common.rpc.JsonRpcServlet.INTERNAL_SERVER_ERROR;
import static org.ms123.common.rpc.JsonRpcServlet.PERMISSION_DENIED;
import static org.apache.commons.io.FileUtils.copyDirectoryToDirectory;
import static org.apache.commons.io.FileUtils.copyDirectory;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.apache.commons.io.FileUtils.readFileToString;

/** NamespaceService implementation
 */
@SuppressWarnings("unchecked")
@Component(enabled = true, configurationPolicy = ConfigurationPolicy.optional, immediate = true, properties = { "rpc.prefix=namespace" })
public class NamespaceServiceImpl implements NamespaceService {

	private static final String NAMESPACE_ENTITY = "namespace";

	private static final String NAME = "name";
	private static final String URLMETA = "url_meta";

	private static final String URLDATA = "url_data";

	protected JSONSerializer m_js = new JSONSerializer();
	protected JSONDeserializer m_ds = new JSONDeserializer();

	private EventAdmin m_eventAdmin;

	protected GitService m_gitService;

	private MetaData m_gitMetaData;
	private String m_gitHost=null;
	private String m_apiKey=null;
	private boolean m_localOnly = false;


	public NamespaceServiceImpl() {
		m_js.prettyPrint(true);
	}

	protected void activate(BundleContext bundleContext, Map<?, ?> props) {
		try{
			String simpl4Dir = System.getProperty("simpl4.dir");
			File file = new File(simpl4Dir+"/etc", "branding");
			if( !file.exists()){
				file = new File(simpl4Dir+"/etc", "branding.example");
			}
			String json = readFileToString(file);
			Map<String,String> b = (Map) m_ds.deserialize(json);
			m_apiKey = b.get("apikey");
			m_gitHost = b.get("githost");
			if( m_gitHost == null){
				m_localOnly = true;
				return;
			}
			if( m_gitHost.indexOf(":") == -1){
				m_gitHost += ":3000";
			}
			m_localOnly=false;
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}

	protected void deactivate() throws Exception {
		System.out.println("NamespaceServiceImpl.deactivate");
	}

	private List<String> getStringList(List<Map> mInstList, String key) {
		List<String> ret = new ArrayList();
		for (Map in : mInstList) {
			ret.add((String) in.get(key));
		}
		return ret;
	}

	private List<Map> prepareList(List<Map> avail, List<Map> mInstList) {
		info("Installed:" + m_js.deepSerialize(mInstList));
		List<String> sInstList = getStringList(mInstList, "name");
		List<Map> ret = new ArrayList();
		if( avail == null) avail = mInstList;
		for (Map av : avail) {
			setFlags(av, sInstList, mInstList);
			if ("global".equals(av.get("name"))) {
				ret.add(0, av);
			} else {
				ret.add(av);
			}
		}
		return ret;
	}

	private String getStatus(String name, List<Map> list, String key) {
		for (Map m : list) {
			if (m.get("name").equals(name)) {
				return (Boolean) m.get(key) ? "yes" : "no";
			}
		}
		return "";
	}

	private void initRepo(String name, String templateName) throws Exception {
		String gitSpace = System.getProperty("git.repos");
		String simpl4Dir = System.getProperty("simpl4.dir");
		File dest = new File(gitSpace, name);
		File testFile = new File(dest,".gitignore");
		File src = new File(simpl4Dir + "/etc", "gittemplate/" + templateName);
		if (dest.exists() && !testFile.exists() && src.exists()) {
			copyDirectory(src, dest);
			m_gitService.add(name, ".");
		}
	}

	private void deleteDir(String name) throws Exception {
		String gitSpace = System.getProperty("git.repos");
		File dest = new File(gitSpace, name);
		if (dest.exists()) {
			deleteDirectory(dest);
		}
	}

	private void setFlags(Map entry, List<String> sInstList, List<Map> mInstList) {
		String name = (String) entry.get("name");
		if (sInstList.contains(name)) {
			entry.put("isInstalled", "yes");
			entry.put("isModified", getStatus(name, mInstList, "isModified"));
			entry.put("updateAvailable", getStatus(name, mInstList, "updateAvailable"));
		} else {
			entry.put("isInstalled", "no");
			entry.put("isModified", "");
			entry.put("updateAvailable", "");
		}
	}

	/* BEGIN JSON-RPC-API*/
	@RequiresRoles("admin")
	public void installNamespace(
			@PName(NAME) String name,
			@PName(URLMETA)            @POptional String url_meta, 
			@PName(URLDATA)            @POptional String url_data
			) throws RpcException {
		try {
			if(isEmpty(url_meta)){
				m_gitService.createRepository(name);
			}else{
				m_gitService.cloneRepository(name, url_meta);
			}
			initRepo(name, "meta");
			boolean dataRepoCreated=false;
			if( url_data != null){
				try{
					m_gitService.cloneRepository(GitServiceImpl.getDataRepoName(name), url_data);
					dataRepoCreated=true;
				}catch(Exception e){
					info("installNamespace.dataRepoClone:"+e.getMessage());
				}
			}
			if( !dataRepoCreated){
				try{
					m_gitService.createRepository(GitServiceImpl.getDataRepoName(name));
				}catch(Exception e){
					info("installNamespace.dataRepoCreate:"+e.getMessage());
				}
			}
			
			initRepo(GitServiceImpl.getDataRepoName(name), "data");
			sendEvent("installed", name);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "NamespaceServiceImpl.installNamespace:", e);
		} finally {
		}
	}

	@RequiresRoles("admin")
	public void updateRepository(
			@PName(NAME)  String name) throws RpcException {
		try {
			sendEvent("preUpdate", name);
			//if( !m_localOnly){
				m_gitService.pull(name);
			//}
			sendEvent("postUpdate", name);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "NamespaceServiceImpl.updateRepository:", e);
		} finally {
		}
	}

	@RequiresRoles("admin")
	public void commitAndPushRepository(
			@PName(NAME)  String name,
			@PName("message")  String message
			) throws RpcException {
		try {
			sendEvent("preCommit", name);
			m_gitService.commitAll(name,message);
			//if( !m_localOnly){
				m_gitService.push(name);
			//}
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "NamespaceServiceImpl.commitAndPushRepository:", e);
		} finally {
		}
	}

	public List getNamespaces() throws RpcException {
		try {
			info("getNamespaces");
			sendEvent("preGet", "all");
			if( m_localOnly){
				List<String> options = new ArrayList();
				options.add("updateAvailable");
				options.add("isModified");
				List repos = m_gitService.getRepositories(options, false);
				return prepareList(null,repos);	
			}
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "NamespaceServiceImpl.getNamespaces:", e);
		} finally {
		}
		return new ArrayList();
	}

	public Map getBranding() throws RpcException {
		try {
			Map m = m_gitMetaData.getBranding();
			m.remove("storeApiKey");
			return m;
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "NamespaceServiceImpl.getBranding:", e);
		} finally {
		}
	}
	public boolean isRuntimeSystem() {
		try {
			String u =  m_gitMetaData.getBranding().get(USAGE);
			return RUNTIME.equals(u);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "NamespaceServiceImpl.isRuntimeSystem:", e);
		} finally {
		}
	}

	public String getSystemUsage() throws RpcException {
		try {
			return m_gitMetaData.getBranding().get(USAGE);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "NamespaceServiceImpl.getSystemUsage:", e);
		} finally {
		}
	}

	private boolean isEmpty(String s) {
		return (s == null || "".equals(s.trim()));
	}
	@RequiresRoles("admin")
	public void deleteNamespace(
			@PName(NAME)               String name, 
			@PName("withRepository")   @POptional @PDefaultBool(false) Boolean withRepository, 
			@PName("withData")         @POptional @PDefaultBool(false) Boolean withData) throws RpcException {
		try {
			m_gitService.deleteRepository(name);
			if (withData) {
				m_gitService.deleteRepository(GitServiceImpl.getDataRepoName(name));
				deleteDir(GitServiceImpl.getDataRepoName(name));
			}
			sendEvent("deleted", name);
			deleteDir(name);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "NamespaceServiceImpl.deleteNamespace:", e);
		} finally {
		}
	}

	private void sendEvent(String what, String namespace) {
		Map props = new HashMap();
		props.put("namespace", namespace);
		info("GitService.sendEvent.postEvent:" + m_eventAdmin);
		m_eventAdmin.sendEvent(new Event("namespace/" + what, props));
	}

	/* END JSON-RPC-API*/
	@Reference(dynamic = true, optional = true)
	public void setGitService(GitService gitService) {
		info("NamespaceServiceImpl.setGitService:" + gitService);
		m_gitService = gitService;
		m_gitMetaData = new GitMetaDataImpl(m_gitService);
	}

	@Reference(dynamic = true)
	public void setEventAdmin(EventAdmin paramEventAdmin) {
		info("NamespaceServiceImpl.setEventAdmin:" + paramEventAdmin);
		this.m_eventAdmin = paramEventAdmin;
	}

	protected void debug(String msg) {
		//System.out.println(msg);
		m_logger.debug(msg);
	}
	protected void info(String msg) {
		System.out.println(msg);
		m_logger.info(msg);
	}
	private static final org.slf4j.Logger m_logger = org.slf4j.LoggerFactory.getLogger(NamespaceServiceImpl.class);
}
