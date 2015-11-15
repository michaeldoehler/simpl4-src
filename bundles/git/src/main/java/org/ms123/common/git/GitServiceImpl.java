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
package org.ms123.common.git;

import java.io.FileInputStream;
import java.io.File;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Date;
import java.util.Locale;
import java.util.Calendar;
import java.util.Iterator;
import java.util.TimeZone;
import java.text.SimpleDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.Event;
import aQute.bnd.annotation.metatype.*;
import aQute.bnd.annotation.component.*;
import static org.apache.commons.io.FileUtils.forceDelete;
import static org.apache.commons.io.FileUtils.moveDirectory;
import static org.apache.commons.io.FileUtils.moveDirectoryToDirectory;
import static org.apache.commons.io.FileUtils.moveFileToDirectory;
import static org.apache.commons.io.FileUtils.moveToDirectory;
import static org.apache.commons.io.FileUtils.moveFile;
import static org.apache.commons.io.FileUtils.copyFile;
import static org.apache.commons.io.FileUtils.copyFileToDirectory;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.apache.commons.io.FileUtils.write;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.ms123.common.rpc.PName;
import org.ms123.common.rpc.POptional;
import org.ms123.common.rpc.PDefaultBool;
import org.ms123.common.rpc.PDefaultInt;
import org.ms123.common.rpc.RpcException;
import org.apache.commons.lang.StringUtils;
import static org.ms123.common.rpc.JsonRpcServlet.ERROR_FROM_METHOD;
import static org.ms123.common.rpc.JsonRpcServlet.INTERNAL_SERVER_ERROR;
import static org.ms123.common.rpc.JsonRpcServlet.PERMISSION_DENIED;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.util.*;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.treewalk.*;
import org.eclipse.jgit.treewalk.filter.*;
import org.eclipse.jgit.revwalk.*;
import org.eclipse.jgit.storage.file.*;
import org.eclipse.jgit.util.*;
import flexjson.JSONSerializer;
import org.mvel2.*;
import java.util.Scanner;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import org.eclipse.jgit.transport.*;
import org.apache.tika.Tika;

/** GitService implementation
 */
@SuppressWarnings("unchecked")
@Component(enabled = true, configurationPolicy = ConfigurationPolicy.optional, immediate = true, properties = { "rpc.prefix=git" })
public class GitServiceImpl implements GitService {

	private static JSONSerializer m_js = new JSONSerializer();

	private Map<String,File> m_fileCache = new LinkedHashMap<String,File>();
	private EventAdmin m_eventAdmin;

	private static final String REF_PREFIX = "refs/heads/";
	private static final String STORE_CFG = "store.cfg";

	private String m_datePattern = "dd.MM.yyyy HH:mm";

	Map<String, String> m_typeMap = new HashMap<String, String>() {

		{
			put("wf", "sw.workflow");
			put("form", "sw.form");
			put("rule", "sw.rule");
			put("fil", "sw.filter");
			put("camel", "sw.camel");
			put("doc", "sw.document");
			put("ws", "sw.website");
			put("wp", "sw.webpage");
		}
	};

	List<String> m_rawList = new ArrayList<String>() {
		{
			add("sw.groovy");
			add("sw.java");
			add("image/svg+xml");
			add("text/html");
			add("text/xml");
			add("text/css");
			add("text/x-asciidoc");
			add("text/x-yaml");
			add("text/javascript");
			add("application/json");
			add("application/vnd.oasis.opendocument.text");
		}
	};

	public GitServiceImpl() {
		CustomJschConfigSessionFactory jschConfigSessionFactory = new CustomJschConfigSessionFactory();
		SshSessionFactory.setInstance(jschConfigSessionFactory);
	}

	protected void activate(BundleContext bundleContext, Map<?, ?> props) {
		info("GitServiceImpl.activate.props:" + props);
	}

	public void update(Map<String, Object> props) {
		System.out.println("GitServiceImpl.updated:" + props);
	}

	protected void deactivate() throws Exception {
		info("deactivate");
		System.out.println("GitServiceImpl deactivate");
	}

	public String getFileContent(String repoName, String path) throws Exception {
		String gitSpace = System.getProperty("git.repos");
		File gitDir = new File(gitSpace, repoName);
		if (!gitDir.exists()) {
			throw new RuntimeException("GitService.getContent:Repo(" + repoName + ") not exists");
		}
		File file = new File(gitDir, path);
		if (!file.exists()) {
			throw new RuntimeException("GitService.getContent:File(" + repoName + "/" + path + ") not exists");
		}
		FileHolder fr = new FileHolder(file);
		return fr.getContent();
	}

	public FileHolderApi getFileHolder( String repoName, String path) {
		try {
			String gitSpace = System.getProperty("git.repos");
			File gitDir = new File(gitSpace, repoName);
			if (!gitDir.exists()) {
				throw new RuntimeException("GitService.getContent:Repo(" + repoName + ") not exists");
			}
			File file = new File(gitDir, path);
			if (!file.exists()) {
				throw new RuntimeException("GitService.getContent:File(" + repoName + "/" + path + ") not exists");
			}
			FileHolder fr = new FileHolder(file);
			return fr;
		} catch (Exception e) {
			if( e instanceof RuntimeException) throw (RuntimeException)e;
			throw new RuntimeException("GitService.getContent:", e);
		} finally {
		}
	}

	/* BEGIN JSON-RPC-API*/
	@RequiresRoles("admin")
	public void createRepository(
			@PName("name")             String name) throws RpcException {
		try {
			String gitSpace = System.getProperty("git.repos");
			File dir = new File(gitSpace, name);
			if (dir.exists()) {
				throw new RpcException(ERROR_FROM_METHOD, 100, "GitService.createRepository:Repo(" + name + ") exists");
			}
			InitCommand ic = Git.init();
			ic.setDirectory(dir);
			ic.call();
			FS fs = FS.detect();
			FileBasedConfig fbc = new FileBasedConfig(new File(gitSpace + "/" + name + "/.git/config"), fs);
			fbc.load();
			SimpleDateFormat sdf = new SimpleDateFormat(m_datePattern);
			sdf.setTimeZone(TimeZone.getTimeZone("Europe/Berlin"));
			String created = sdf.format(new Date());
			fbc.setString("sw", null, "created", created);
			fbc.save();
			if (!isDataRepo(name)) {
				createStoreFile(gitSpace, name);
			}
			return;
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "GitService.createRepository:", e);
		} finally {
		}
	}

	public static boolean isDataRepo(String name){
		return name.endsWith("_data");
	}
	public static String getDataRepoName(String name){
		return name+"_data";
	}
	public static boolean hasStoreCfg(File file){
		File storeCfgFile = new File(file, STORE_CFG);
		if (storeCfgFile.exists()) {
			File disabledFile = new File(file, "disabled");
			if (!disabledFile.exists()) {
				return true;
			}
		}
		return false;
	}

	@RequiresRoles("admin")
	public void cloneRepository(
			@PName("name")             String name,
			@PName("fromUri")             String fromUri
			) throws RpcException {
		File dir = null;
		try {
			String gitSpace = System.getProperty("git.repos");
			dir = new File(gitSpace, name);
			if (dir.exists()) {
				throw new RpcException(ERROR_FROM_METHOD, 100, "GitService.cloneRepository:Repo(" + name + ") exists");
			}
			CloneCommand ic = Git.cloneRepository();
			ic.setDirectory(dir);
			ic.setURI(fromUri);
			ic.call();
			FS fs = FS.detect();
			FileBasedConfig fbc = new FileBasedConfig(new File(gitSpace + "/" + name + "/.git/config"), fs);
			fbc.load();
			SimpleDateFormat sdf = new SimpleDateFormat(m_datePattern);
			sdf.setTimeZone(TimeZone.getTimeZone("Europe/Berlin"));
			String created = sdf.format(new Date());
			fbc.setString("sw", null, "cloned", created);
			fbc.save();
			if (!isDataRepo(name)) {
				createStoreFile(gitSpace, name);
			}
			return;
		} catch (Exception e) {
			if (dir != null && dir.exists()) {
				try{
					forceDelete(dir);
				}catch(Exception ex){
					e.printStackTrace();
				}
			}
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "GitService.createRepository:", e);
		} finally {
		}
	}

	@RequiresRoles("admin")
	public void deleteRepository(
			@PName("name")             String name) throws RpcException {
		try {
			String gitSpace = System.getProperty("git.repos");
			File dir = new File(gitSpace, name);
			if (!dir.exists()) {
				throw new RpcException(ERROR_FROM_METHOD, 100, "GitService.deleteRepository:Repo(" + name + ") not exists");
			}
			forceDelete(dir);
			return;
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "GitService.deleteRepository:", e);
		} finally {
		}
	}

	public List getRepositories(
		@PName("flags")  @POptional List<String> flags, 
		@PName("all")        @PDefaultBool(false) @POptional Boolean all) throws RpcException {
		System.out.println("getRepositories;"+flags+"/"+all);
		try {
			String gitSpace = System.getProperty("git.repos");
			File dir = new File(gitSpace);
			if (!dir.exists()) {
				throw new RpcException(ERROR_FROM_METHOD, 100, "GitService.gitSpace not exists");
			}
			List<Map> resList = new ArrayList<Map>();
			File[] chld = dir.listFiles();
			FS fs = FS.detect();
			for (int i = 0; i < chld.length; i++) {
				File file = chld[i];
				String fileName = file.getName();
				if (!all && hasStoreCfg(file)==false) {
					continue;
				}
				System.out.println("\nupdateavail---------------->" + fileName);
				Map map = new HashMap();
				if (flags!=null && flags.contains("updateAvailable")) {
					Git gitObject = Git.open(new File(gitSpace, fileName));
					map.put("updateAvailable", updateAvailable(gitObject));
				}
				if (flags!=null && flags.contains("isModified")) {
					Git gitObject = Git.open(new File(gitSpace, fileName));
					map.put("isModified", isModified(gitObject));
				}
				debug(fileName);
				FileBasedConfig fbc = new FileBasedConfig(new File(gitSpace + "/" + fileName + "/.git/config"), fs);
				fbc.load();
				debug("FBC:" + fbc);
				debug("FBC:" + fbc.getSections());
				debug("FBC:" + fbc.toText());
				String created = fbc.getString("sw", null, "created");
				SimpleDateFormat sdf = new SimpleDateFormat(m_datePattern, Locale.GERMAN);
				map.put("name", fileName);
				if (created != null) {
					map.put("created", sdf.parse(created).getTime());
				}
				resList.add(map);
			}
			return resList;
		} catch (Exception e) {
			if( e instanceof RpcException) throw (RpcException)e;
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "GitService.getRepositories:", e);
		} finally {
		}
	}

	public Map getWorkingTree(
			@PName("name")             String repoName, 
			@PName("path")             @POptional String path, 
			@PName("depth")            @POptional @PDefaultInt(100) Integer depth, 
			@PName("includeTypeList")  @POptional List<String> includeTypeList, 
			@PName("includePathList")  @POptional List<String> includePathList, 
			@PName("excludePathList")  @POptional List<String> excludePathList, 
			@PName("mapping")          @POptional Map mapping) throws RpcException {
		try {
			if (depth == null)
				depth = 100;
			String gitSpace = System.getProperty("git.repos");
			File gitDir = new File(gitSpace, repoName + "/.git");
			if (!gitDir.exists()) {
				throw new RpcException(ERROR_FROM_METHOD, 100, "GitService.getWorkingTree:Repo(" + repoName + ") not exists");
			}
			File repoDir = new File(gitSpace, repoName);
			Git gitObject = Git.open(new File(gitSpace, repoName));
			TreeWalk treeWalk = new TreeWalk(gitObject.getRepository());
			FileTreeIterator newTree = null;
			String rootPath = "root";
			String type = "sw.project";
			File basePath = repoDir;
			String title = repoName;
			if (path == null) {
				newTree = new FileTreeIterator(gitObject.getRepository());
			} else {
				File f = new File(gitObject.getRepository().getDirectory().getParentFile(), path);
				debug("f:" + f);
				newTree = new FileTreeIterator(f, FS.detect(), gitObject.getRepository().getConfig().get(WorkingTreeOptions.KEY));
				rootPath = path;
				type = "sw.directory";
				title += "/" + path;
				basePath = new File(basePath, path);
			}
			treeWalk.addTree(newTree);
			treeWalk.setRecursive(true);
			Collection<TreeFilter> filterList = new HashSet();
			TreeFilter pathFilter = PathPatternFilter.create("^[a-zA-Z].*$", includePathList, excludePathList, 0);
			filterList.add(pathFilter);
			if (includeTypeList != null && includeTypeList.size() > 0) {
				filterList.add(TypeFilter.create(basePath, includeTypeList));
			}
			if (filterList.size() > 1) {
				TreeFilter andFilter = AndTreeFilter.create(filterList);
				treeWalk.setFilter(andFilter);
			} else {
				treeWalk.setFilter(pathFilter);
			}
			treeWalk.setPostOrderTraversal(true);
			Map<String, Map> parentMap = new HashMap();
			Map root = new HashMap();
			root.put("path", rootPath);
			root.put("title", repoName);
			root.put("value", rootPath);
			root.put("type", type);
			root.put("children", new ArrayList());
			parentMap.put("root", root);
			while (true) {
				if (!treeWalk.next()) {
					break;
				}
				String pathString = new String(treeWalk.getRawPath());
				// String pathString = treeWalk.getPathString();
				Node[] nodes = getNodes(pathString);
				for (int i = 0; i < nodes.length && i < depth; i++) {
					if (parentMap.get(nodes[i].path) == null) {
						Map map = getNodeMap(nodes[i], i < (nodes.length - 1), basePath, mapping);
						map.put("children", new ArrayList());
						parentMap.put(nodes[i].path, map);
						Map pmap = parentMap.get(nodes[i].parent);
						if (pmap != null) {
							List<Map> children = (List) pmap.get("children");
							children.add(map);
						}
					}
				}
			}
			// m_js.prettyPrint(true);
			// String ser = m_js.deepSerialize(parentMap.get("root"));
			// debug("Tree" + ser);
			return parentMap.get("root");
		} catch (Exception e) {
			if( e instanceof RpcException) throw (RpcException)e;
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "GitService.getWorkingTree:", e);
		} finally {
		}
	}

	public File searchFile(
			@PName("reponame")         String repoName, 
			@PName("name")             String name, 
			@PName("type")             String type) throws RpcException {
		try {
			long startTime = new Date().getTime();
			String gitSpace = System.getProperty("git.repos");
			File gitDir = new File(gitSpace, repoName);
			if (!gitDir.exists()) {
				throw new RpcException(ERROR_FROM_METHOD, 100, "GitService.searchFile:Repo(" + repoName + ") not exists");
			}
			List<String> pathList = null;
			File f = new File(gitDir,name);
			info("searchFile.name:"+name+"|wantedType:"+type);
			if( f.exists() && (isEmpty(type) || getFileType(f).equals(type))){
				info("searchFile.found:"+f);
				return f;
			}
			File cachedFile = m_fileCache.get(repoName+"/"+name+"/"+type);
			if( cachedFile != null && cachedFile.exists()){
				info("searchFile.cachedFile("+repoName+"/"+name+") exists.searchTime"+(new Date().getTime() - startTime));
				return cachedFile;
			}
			pathList = assetList(repoName, name, type, true);
			info("searchFile.searchTime:" + (new Date().getTime() - startTime));
			if (pathList.size() == 0) {
				throw new RpcException(ERROR_FROM_METHOD, 100, "GitService.searchFile:File \"" + name + "\" exists not in " + repoName);
			}
			if (pathList.size() > 1) {
				throw new RpcException(ERROR_FROM_METHOD, 100, "GitService.searchFile:File \"" + name + "\" exists more as one times in " + repoName);
			}
			File file = new File(gitDir, pathList.get(0));
			m_fileCache.put(repoName+"/"+name+"/"+type, file);
			return file;
		} catch (Exception e) {
			if( e instanceof RpcException) throw (RpcException)e;
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "GitService.searchFile:", e);
		} finally {
		}
	}

	public String searchContent(
			@PName("reponame")         String repoName, 
			@PName("name")             String name, 
			@PName("type")             String type) throws RpcException {
		try {
			long startTime = new Date().getTime();
			String gitSpace = System.getProperty("git.repos");
			File gitDir = new File(gitSpace, repoName);
			if (!gitDir.exists()) {
				throw new RpcException(ERROR_FROM_METHOD, 100, "GitService.searchContent:Repo(" + repoName + ") not exists");
			}

			List<String> pathList = null;
			File f = new File(gitDir,name);
			if( f.exists() && (isEmpty(type) || getFileType(f).equals(type))){
				info("searchContent.found:"+f);
				FileHolder fr = new FileHolder(f);
				return fr.getContent();
			}
			File cachedFile = m_fileCache.get(repoName+"/"+name+"/"+type);
			if( cachedFile != null && cachedFile.exists()){
				info("searchContent.cachedFile("+repoName+"/"+name+") exists.searchTime:"+(new Date().getTime() - startTime));
				long _startTime = new Date().getTime();
				FileHolder fr = new FileHolder(cachedFile);
				String c = fr.getContent();
				info("searchContent.loadtime("+repoName+"/"+name+"):"+(new Date().getTime() - _startTime));
				return c;
			}

			pathList = assetList(repoName, name, type, true);
			info("searchContent.searchTime:" + (new Date().getTime() - startTime));
			if (pathList.size() == 0) {
				throw new RpcException(ERROR_FROM_METHOD, 100, "GitService.searchContent:File \"" + name + "\" exists not in " + repoName);
			}
			if (pathList.size() > 1) {
				throw new RpcException(ERROR_FROM_METHOD, 100, "GitService.searchContent:File \"" + name + "\" exists more as one times in " + repoName);
			}
			File file = new File(gitDir, pathList.get(0));
			m_fileCache.put(repoName+"/"+name+"/"+type, file);
			FileHolder fr = new FileHolder(file);
			return fr.getContent();
		} catch (Exception e) {
			if( e instanceof RpcException) throw (RpcException)e;
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "GitService.searchContent:", e);
		} finally {
		}
	}

	public String getContent(
			@PName("reponame")         String repoName, 
			@PName("path")             String path) throws RpcException {
		try {
			String gitSpace = System.getProperty("git.repos");
			debug("getContent.gitSpace:"+gitSpace+"/repo:"+repoName+"/path:"+path);
			File gitDir = new File(gitSpace, repoName);
			if (!gitDir.exists()) {
				throw new RpcException(ERROR_FROM_METHOD, 100, "GitService.getContent:Repo(" + repoName + ") not exists");
			}
			File file = new File(gitDir, path);
			debug("getContent.file:"+file.toString()+"/exists:"+file.exists()+"/gitDir:"+gitDir);
			if (!file.exists()) {
				throw new RpcException(ERROR_FROM_METHOD, 101, "GitService.getContent:File(" + repoName + "/" + path + ") not exists");
			}
			FileHolder fr = new FileHolder(file);
			return fr.getContent();
		} catch (Exception e) {
			if( e instanceof RpcException) throw (RpcException)e;
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "GitService.getContent:", e);
		} finally {
		}
	}

	public String getContentRaw(
			@PName("reponame")         String repoName, 
			@PName("path")             String path) throws RpcException {
		try {
			String gitSpace = System.getProperty("git.repos");
			debug("getContent.gitSpace:"+gitSpace+"/repo:"+repoName+"/path:"+path);
			File gitDir = new File(gitSpace, repoName);
			if (!gitDir.exists()) {
				throw new RpcException(ERROR_FROM_METHOD, 100, "GitService.getContent:Repo(" + repoName + ") not exists");
			}
			File file = new File(gitDir, path);
			debug("getContent.file:"+file.toString()+"/exists:"+file.exists()+"/gitDir:"+gitDir);
			if (!file.exists()) {
				throw new RpcException(ERROR_FROM_METHOD, 101, "GitService.getContent:File(" + repoName + "/" + path + ") not exists");
			}
			return readFileToString(file);
		} catch (Exception e) {
			if( e instanceof RpcException) throw (RpcException)e;
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "GitService.getContentRaw:", e);
		} finally {
		}
	}

	public Map getContentCheckRaw(
			@PName("reponame")         String repoName, 
			@PName("path")             String path) throws RpcException {
		try {
			String gitSpace = System.getProperty("git.repos");
			File gitDir = new File(gitSpace, repoName);
			if (!gitDir.exists()) {
				throw new RpcException(ERROR_FROM_METHOD, 100, "GitService.getContent:Repo(" + repoName + ") not exists");
			}
			File file = new File(gitDir, path);
			if (!file.exists()) {
				throw new RpcException(ERROR_FROM_METHOD, 100, "GitService.getContent:File(" + repoName + "/" + path + ") not exists");
			}
			Map map = new HashMap();
			try {
				FileHolder fr = new FileHolder(file);
				map.put("raw", false);
				map.put("content", fr.getContent());
				return map;
			} catch (org.eclipse.jgit.errors.ConfigInvalidException e) {
				map.put("raw", true);
				map.put("content", readFileToString(file));
				return map;
			}
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "GitService.getContentCheckRaw:", e);
		} finally {
		}
	}

	public boolean exists(
			@PName("reponame")         String repoName, 
			@PName("path")             String path) throws RpcException {
		try {
			String gitSpace = System.getProperty("git.repos");
			File gitDir = new File(gitSpace, repoName);
			if (!gitDir.exists()) {
				throw new RpcException(ERROR_FROM_METHOD, 100, "GitService.exists:Repo(" + repoName + ") not exists");
			}
			File file = new File(gitDir, path);
			return file.exists();
		} catch (Exception e) {
			if( e instanceof RpcException) throw (RpcException)e;
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "GitService.exists:", e);
		} finally {
		}
	}

	@RequiresRoles("admin")
	public synchronized void putContent(
			@PName("reponame")         String repoName, 
			@PName("path")             String path, 
			@PName("type")             @POptional String type, 
			@PName("content")          String content) throws RpcException {
		putContentInternal(repoName,path,content,type);
	}

	@RequiresRoles("admin")
	public void createObject(
			@PName("reponame")         String repoName, 
			@PName("path")             String path, 
			@PName("overwrite")        @PDefaultBool(false) @POptional Boolean overwrite, 
			@PName("content")          @POptional String content, 
			@PName("type")             String type) throws RpcException {
		createObjectInternal(repoName,path,overwrite,content,type);
	}

	@RequiresRoles("admin")
	public void moveObject(
			@PName("reponame")         String repoName, 
			@PName("oldPath")          String oldPath, 
			@PName("newPath")          String newPath) throws RpcException {
		try {
			String gitSpace = System.getProperty("git.repos");
			File gitDir = new File(gitSpace, repoName);
			if (!gitDir.exists()) {
				throw new RpcException(ERROR_FROM_METHOD, 100, "GitService.moveObject:Repo(" + repoName + ") not exists");
			}
			File ofile = new File(gitDir, oldPath);
			if (!ofile.exists()) {
				throw new RuntimeException("moveObject(" + oldPath + "):not exists");
			}
			File nfile = new File(gitDir, newPath);
			if (ofile.isDirectory()) {
				if (nfile.exists()) {
					if (nfile.isDirectory()) {
						moveDirectoryToDirectory(ofile, nfile, true);
					} else {
						throw new RuntimeException("moveObject:cannot mv Directory to File");
					}
				} else {
					moveDirectory(ofile, nfile);
				}
			} else {
				if (nfile.exists()) {
					if (nfile.isDirectory()) {
						moveFileToDirectory(ofile, nfile, true);
					} else {
						if (ofile.isDirectory()) {
							throw new RuntimeException("moveObject:cannot mv Directory to File");
						} else {
							throw new RuntimeException("moveObject:cannot mv File to itsself");
						}
					}
				} else {
					moveFile(ofile, nfile);
				}
			}
			add(repoName, newPath);
			rm(repoName, oldPath);
		} catch (Exception e) {
			if( e instanceof RpcException) throw (RpcException)e;
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "GitService.moveObject:", e);
		} finally {
		}
	}

	@RequiresRoles("admin")
	public void copyObject(
			@PName("reponame")         String repoName, 
			@PName("origPath")         String origPath, 
			@PName("newPath")          String newPath) throws RpcException {
		try {
			String gitSpace = System.getProperty("git.repos");
			File gitDir = new File(gitSpace, repoName);
			if (!gitDir.exists()) {
				throw new RpcException(ERROR_FROM_METHOD, 100, "GitService.copyObject:Repo(" + repoName + ") not exists");
			}
			File ofile = new File(gitDir, origPath);
			if (!ofile.exists()) {
				throw new RuntimeException("copyObject(" + origPath + "):not exists");
			}
			File nfile = new File(gitDir, newPath);
			if (ofile.isDirectory()) {
				throw new RuntimeException("copyObject:cannot copy Directory");
			} else {
				if (nfile.exists()) {
					if (nfile.isDirectory()) {
						copyFileToDirectory(ofile, nfile, true);
					} else {
						throw new RuntimeException("copyObject:File exists");
					}
				} else {
					copyFile(ofile, nfile);
				}
			}
			add(repoName, newPath);
		} catch (Exception e) {
			if( e instanceof RpcException) throw (RpcException)e;
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "GitService.copyObject:", e);
		} finally {
		}
	}

	@RequiresRoles("admin")
	public void deleteObject(
			@PName("reponame")         String repoName, 
			@PName("path")             String path) throws RpcException {
		deleteObjectInternal(repoName,path);
	}

	@RequiresRoles("admin")
	public void deleteObjects(
			@PName("reponame")         String repoName, 
			@PName("directory")        String directory,
			@PName("regex")             String regex
			) throws RpcException {
		try {
			debug("Git.deleteObjects:"+repoName+","+directory+","+regex);
			String gitSpace = System.getProperty("git.repos");
			File gitDir = new File(gitSpace, repoName);
			if (!gitDir.exists()) {
				throw new RpcException(ERROR_FROM_METHOD, 100, "GitService.deleteObjects:Repo(" + repoName + ") not exists");
			}
			File dir = new File(gitDir, directory);
			if( !dir.exists()){
				throw new RpcException(ERROR_FROM_METHOD, 100, "GitService.deleteObjects:Directory(" + directory + ") not exists");
			}
			FileFilter fileFilter = new RegexFileFilter(regex);
			File[] files = dir.listFiles(fileFilter);
			debug("Git.deleteObjects.files:"+files.length);
			for (int i = 0; i < files.length; i++) {
				File file = files[i];
				deleteQuietly(file);
				String[] segs = file.toString().split("/");
				debug("Git.deleteObject:"+file+","+directory+"|"+segs[segs.length-1]);
				rm(repoName, directory+segs[segs.length-1]);
			} 
		} catch (Exception e) {
			if( e instanceof RpcException) throw (RpcException)e;
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "GitService.deleteObjects:", e);
		} finally {
		}
	}

	public List<String> assetList(
			@PName("reponame")         String repoName, 
			@PName("name")             String name, 
			@PName("type")             String type, 
			@PName("onlyFirst")        @POptional @PDefaultBool(false) Boolean onlyFirst) throws RpcException {
		try {
			String gitSpace = System.getProperty("git.repos");
			File gitDir = new File(gitSpace, repoName);
			if (!gitDir.exists()) {
				throw new RpcException(ERROR_FROM_METHOD, 100, "GitService.getContent:Repo(" + repoName + ") not exists");
			}
			List<String> typeList = new ArrayList();
			List<String> hitList = new ArrayList();
			typeList.add(type);
			Git gitObject = Git.open(new File(gitSpace, repoName));
			TreeWalk treeWalk = new TreeWalk(gitObject.getRepository());
			FileTreeIterator newTree = new FileTreeIterator(gitObject.getRepository());
			treeWalk.addTree(newTree);
			treeWalk.setRecursive(true);
			treeWalk.setPostOrderTraversal(true);
			File basePath = new File(gitSpace, repoName);
			while (true) {
				if (!treeWalk.next()) {
					break;
				}
				String pathString = new String(treeWalk.getRawPath());
				File file = new File(basePath, pathString);
				if (file.isDirectory() || pathString.startsWith("store") || pathString.startsWith(".git") ) {
					continue;
				}
				if (typeList.contains("all") || typeList.contains(getFileType(file))) {
					if (name == null || name.equals(getBasename(pathString))) {
						debug("\tTreffer:" + pathString);
						hitList.add(pathString);
						if (onlyFirst) {
							return hitList;
						}
					}
				}
			}
			return hitList;
		} catch (Exception e) {
			if( e instanceof RpcException) throw (RpcException)e;
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "GitService.assetList:", e);
		} finally {
		}
	}

	public void commitAll(
			@PName("name")             String name, 
			@PName("message")          String message) throws RpcException {
		try {
			String gitSpace = System.getProperty("git.repos");
			File dir = new File(gitSpace, name);
			if (!dir.exists()) {
				throw new RpcException(ERROR_FROM_METHOD, 100, "GitService.CommitAll:Repo(" + name + ") not exists");
			}
			Git git = Git.open(dir);
			CommitCommand ic = git.commit();
			ic.setAll(true);
			ic.setMessage(message);
			ic.call();
			return;
		} catch (Exception e) {
			if( e instanceof RpcException) throw (RpcException)e;
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "GitService.commitAll:", e);
		} finally {
		}
	}

	public void push(
			@PName("name")             String name) throws RpcException {
		try {
			String gitSpace = System.getProperty("git.repos");
			File dir = new File(gitSpace, name);
			if (!dir.exists()) {
				throw new RpcException(ERROR_FROM_METHOD, 100, "GitService.push:Repo(" + name + ") not exists");
			}
			Git git = Git.open(dir);
			PushCommand push = git.push();
			push.setRefSpecs(new RefSpec(REF_PREFIX + "master")).setRemote("origin");
			//ic.setPushAll(true);
			Iterable<PushResult> result = null;
			try{
				result = push.call();
			}catch(org.eclipse.jgit.api.errors.TransportException e){
				if( e.getCause() instanceof org.eclipse.jgit.errors.NoRemoteRepositoryException){
					info("Push:"+e.getCause().getMessage());
					return;
				}else{
					throw e;
				}
			}
			for (PushResult pushResult : result) {
				if (StringUtils.isNotBlank(pushResult.getMessages())) {
					debug(pushResult.getMessages());
				}
			}
			return;
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "GitService.push:", e);
		} finally {
		}
	}

	public void pull(
			@PName("name")             String name) throws RpcException {
		try {
			String gitSpace = System.getProperty("git.repos");
			File dir = new File(gitSpace, name);
			if (!dir.exists()) {
				throw new RpcException(ERROR_FROM_METHOD, 100, "GitService.pull:Repo(" + name + ") not exists");
			}
			Git git = Git.open(dir);
			PullCommand pull = git.pull();
			PullResult result = pull.call();
			debug(result.toString());
			return;
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "GitService.pull:", e);
		} finally {
		}
	}

	public void add(
			@PName("name")             String name, 
			@PName("pattern")          String pattern) throws RpcException {
		try {
			String gitSpace = System.getProperty("git.repos");
			File dir = new File(gitSpace, name);
			if (!dir.exists()) {
				throw new RpcException(ERROR_FROM_METHOD, 100, "GitService.add:Repo(" + name + ") not exists");
			}
			Git git = Git.open(dir);
			AddCommand add = git.add();
			add.addFilepattern(pattern);
			add.call();
			return;
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "GitService.add:", e);
		} finally {
		}
	}

	public void rm(
			@PName("name")             String name, 
			@PName("pattern")          String pattern) throws RpcException {
		try {
			String gitSpace = System.getProperty("git.repos");
			File dir = new File(gitSpace, name);
			if (!dir.exists()) {
				throw new RpcException(ERROR_FROM_METHOD, 100, "GitService.rm:Repo(" + name + ") not exists");
			}
			Git git = Git.open(dir);
			RmCommand rm = git.rm();
			rm.addFilepattern(pattern);
			rm.call();
			return;
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "GitService.rm:", e);
		} finally {
		}
	}


	public void addRemoteOrigin(
			@PName("name") 		String name, 
			@PName("url") 		String url){
		try {
			String gitSpace = System.getProperty("git.repos");
			File dir = new File(gitSpace, name);
			if (!dir.exists()) {
				throw new RpcException(ERROR_FROM_METHOD, 100, "GitService.setAddRemoteOrigin:Repo(" + name + ") not exists");
			}
			Git git = Git.open(dir);
			StoredConfig config = git.getRepository().getConfig();
			config.setString("remote", "origin", "url", url);
			config.save();
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "GitService.addRemoteOrigin:", e);
		} finally {
		}
	}
	/* END JSON-RPC-API*/


	/*Unrestricted access for internal use,public not accessible*/
	public void putContentInternal( String repoName, String path, String content,String type) {
		try {
			String gitSpace = System.getProperty("git.repos");
			File gitDir = new File(gitSpace, repoName);
			if (!gitDir.exists()) {
				throw new RpcException(ERROR_FROM_METHOD, 100, "GitService.putContent:Repo(" + repoName + ") not exists");
			}
			File file = new File(gitDir, path);
			if (!file.exists()) {
				if (type != null) {
					createObjectInternal(repoName, path, true, content, type);
				} else {
					throw new RpcException(ERROR_FROM_METHOD, 100, "GitService.putContent:File(" + repoName + "/" + path + ") not exists");
				}
			}else{
				if( type != null && m_rawList.contains(type) ){
					write(file, content, "UTF-8");
				}else{
					FileHolder fr = new FileHolder(file);
					fr.putContent(content);
				}
			}
		} catch (Exception e) {
			if( e instanceof RpcException) throw (RpcException)e;
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "GitService.putContent:", e);
		} finally {
		}
	}

	public void createObjectInternal( String repoName, String path, Boolean overwrite, String content, String type) {
		try {
			String gitSpace = System.getProperty("git.repos");
			File gitDir = new File(gitSpace, repoName);
			if (!gitDir.exists()) {
				throw new RpcException(ERROR_FROM_METHOD, 100, "GitService.createObject:Repo(" + repoName + ") not exists");
			}
			File file = new File(gitDir, path);
			if(!overwrite && file.exists()){
				throw new RpcException(ERROR_FROM_METHOD, 100, "GitService.createObject:Repo(" + repoName + ") path("+path+") exists");
			}
			if (type.equals("sw.directory")) {
				FileUtils.mkdirs(file, true);
			} else {
				if (!file.getParentFile().exists()) {
					file.getParentFile().mkdirs();
				}
				if( m_rawList.contains(type) ){
					file = addExtentions(file,type);	
					file.createNewFile();
					if( content != null){
						write(file, content, "UTF-8");
					}
				}else{
					FileHolder fr = new FileHolder(file);
					fr.putContent(type, content);
				}
			}
			add(repoName, ".");
		} catch (Exception e) {
			if( e instanceof RpcException) throw (RpcException)e;
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "GitService.createObject:", e);
		} finally {
		}
	}

	public void deleteObjectInternal( String repoName, String path) {
		try{
			String gitSpace = System.getProperty("git.repos");
			File gitDir = new File(gitSpace, repoName);
			if (!gitDir.exists()) {
				throw new RpcException(ERROR_FROM_METHOD, 100, "GitService.deleteObject:Repo(" + repoName + ") not exists");
			}
			File file = new File(gitDir, path);
			deleteQuietly(file);
			rm(repoName, path);
		} catch (Exception e) {
			if( e instanceof RpcException) throw (RpcException)e;
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "GitService.deleteObject:", e);
		} finally {
		}
	}

	/* Private Stuff*/
	private Map getNodeMap(Node n, boolean isSubtree, File repoDir, Map<String, String> mapping) throws Exception {
		Map<String, Object> nodeMap = new HashMap();
		Map props = new HashMap();
		props.put("path", n.path);
		props.put("name", n.name);
		File file = new File(repoDir, n.path);
		if (!isSubtree && !file.isDirectory()) {
			props.put("type", getFileType(file));
		}
		if (mapping == null) {
			nodeMap.putAll(props);
			return nodeMap;
		}
		Iterator<String> it = mapping.keySet().iterator();
		while (it.hasNext()) {
			String key = it.next();
			String val = mapping.get(key);
			if ("_all_".equals(key)) {
				nodeMap.putAll(props);
			} else if ("uuid".equals(val)) {
				nodeMap.put(key, n.id);
			} else {
				if (val.startsWith("(")) {
					int len = val.length();
					val = val.substring(1, len - 1);
					try {
						val = MVEL.evalToString(val, props);
					} catch (Exception e) {
						debug("TreeNodeVisitor.getNodeMap.MVEL(" + val + "):" + e);
					}
					nodeMap.put(key, val);
				} else {
					nodeMap.put(key, props.get(val));
				}
			}
		}
		return nodeMap;
	}

	private Boolean updateAvailable(Git git) throws Exception {
		FetchCommand fc = git.fetch();
		fc.setDryRun(true);
		fc.setRemote("origin");
		FetchResult fr = null;
		try{
			fr = fc.call();
		}catch(org.eclipse.jgit.api.errors.InvalidRemoteException e){
			return false;
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}
		debug("getAdvertisedRefs:" + fr.getAdvertisedRefs());
		debug("getTrackingRefUpdates:" + fr.getTrackingRefUpdates());
		return !fr.getTrackingRefUpdates().isEmpty();
	}

	private Boolean isModified(Git git) throws Exception {
		StatusCommand sc = git.status();
		Status st = sc.call();
		debug("Status.getModified:" + st.getModified());
		debug("Status.getAdded:" + st.getAdded());
		debug("Status.getChanged:" + st.getChanged());
		debug("Status.getMissing:" + st.getMissing());
		debug("Status.getUntracked:" + st.getUntracked());
		boolean clean = st.getAdded().isEmpty() && st.getChanged().isEmpty() && st.getRemoved().isEmpty() && st.getMissing().isEmpty() && st.getModified().isEmpty() && st.getConflicting().isEmpty();
		debug("Status.clean:" + clean + "/" + st.isClean());
		return !clean;
	}

	private String getBasename(String path) {
		String e[] = path.split("/");
		return e[e.length - 1];
	}

	private static Node[] getNodes(String pathString) {
		String[] p = pathString.split("/");
		Node[] nodes = new Node[p.length];
		String lastpart = null;
		for (int i = 0; i < p.length; i++) {
			Node node = new Node();
			if (i == 0) {
				node.parent = "root";
			} else {
				node.parent = lastpart;
			}
			node.name = p[i];
			lastpart = node.path = (lastpart == null ? "" : lastpart + "/") + p[i];
			nodes[i] = node;
		}
		return nodes;
	}

	private static class Node {

		public String parent;

		public String path;

		public String name;

		public String id;

		public String toString() {
			return "Path:" + path + ",parent:" + parent;
		}
	}

	protected static String getFileType(File file) {
		if (file.isDirectory()){
			return "sw.directory";
		}else if( file.toString().endsWith(".txt")){
			return "text/plain";
		}else if( file.toString().endsWith(".jpeg") || file.toString().endsWith(".jpg")){
			return "image/jpeg";
		}else if( file.toString().endsWith(".png")){
			return "image/png";
		}else if( file.toString().endsWith(".svg")){
			return "image/svg+xml";
		}else if( file.toString().endsWith(".xml")){
			return "text/xml";
		}else if( file.toString().endsWith(".woff") || file.toString().endsWith(".woff.gz")){
			return "application/x-font-woff";
		}else if( file.toString().endsWith(".js") || file.toString().endsWith(".js.gz")){
			return "text/javascript";
		}else if( file.toString().endsWith(".adoc") || file.toString().endsWith(".adoc.gz")){
			return "text/x-asciidoc";
		}else if( file.toString().endsWith(".html") || file.toString().endsWith(".html.gz")){
			return "text/html";
		}else if( file.toString().endsWith(".css") || file.toString().endsWith(".css.gz")){
			return "text/css";
		}else if( file.toString().endsWith(".yaml") || file.toString().endsWith(".yml")){
			return "text/x-yaml";
		}else if( file.toString().endsWith(".json") || file.toString().endsWith(".json.gz")){
			return "application/json";
		}else if( file.toString().endsWith(".odt")){
			return "application/vnd.oasis.opendocument.text";
		}else if( file.toString().endsWith(".groovy")){
			return "sw.groovy";
		}else if( file.toString().endsWith(".java")){
			return "sw.java";
		}
		RandomAccessFile r = null;
		try {
			int lnr = 0;
			r = new RandomAccessFile(file, "r");
			int i = r.readInt();
			if (i == 1534293853) {
				r.seek(0);
				while (true) {
					String line = r.readLine();
					if (line == null) {
						break;
					}
					if (lnr == 0 && !line.startsWith("[sw]"))
						break;
					if (line.trim().startsWith("type")) {
						int eq = line.indexOf("=");
						if (eq != -1) {
							return line.substring(eq + 1).trim();
						}
					}
					lnr++;
					if (lnr > 20)
						break;
				}
			} else if (i == -2555936) {
				return "image/jpeg";
			} else if (i == -1991225785) {
				return "image/png";
			}
		} catch (Exception e) {
			return "sw.unknown";
		} finally {
			closeFile(r);
		}
		return detectFileType(file);
	}

	protected static synchronized String detectFileType(File file) {
		FileInputStream bis = null;
		try {
			bis = new FileInputStream(file);
			Tika tika = new Tika();
			String ftype = null;
			try{
				ftype = tika.detect(bis);
			}catch(Throwable t){
				return "sw.unknown";
			}
			if( ftype.equals("application/x-shockwave-flash")){
				return "image/swf";
			}
			if( ftype.equals("application/pdf")){
				return "image/pdf";
			}
			return ftype;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (bis != null)
					bis.close();
			} catch (Exception e) {
			}
		}
		return "sw.unknown";
	}

	private static void closeFile(RandomAccessFile r) {
		if (r != null) {
			try {
				r.close();
			} catch (Exception e) {
			}
		}
	}


	private File addExtentions(File file, String type){
		String ext = type.substring(2);
		if( file.getName().endsWith(ext)){
			return file;
		}
		return new File(file+ ext);
	}
	private void createStoreFile(String gitSpace, String name) throws Exception {
		File file = new File(gitSpace + "/" + name + "/store.cfg");
		if (file.exists())
			return;
		FS fs = FS.detect();
		FileBasedConfig fbc = new FileBasedConfig(file, fs);
		fbc.setString("store", "meta", "pack", "data");
		fbc.setString("store", "meta", "namespace", name);
		fbc.setString("store", "meta", "database", "rdbms:h2");
		fbc.setString("store", "data", "pack", "data");
		fbc.setString("store", "data", "namespace", name);
		fbc.setString("store", "data", "repository", getDataRepoName(name));
		fbc.setString("store", "data", "database", "rdbms:h2");
		fbc.save();
	}

	private String getStoreHost() {
		return "swstore.ms123.org";
	}

	private boolean isEmpty(Object o) {
		if (o instanceof String) {
			String s = (String) o;
			return (s == null || "".equals(s.trim()) || "all".equals(s.trim().toLowerCase()));
		}
		return o == null;
	}
	private static class CustomJschConfigSessionFactory extends JschConfigSessionFactory {

		@Override
		protected void configure(OpenSshConfig.Host host, Session session) {
			session.setConfig("StrictHostKeyChecking", "no");
		}

		protected JSch createDefaultJSch(FS fs) throws JSchException {
			JSch jsch = new JSch();
			try {
				jsch.addIdentity("/root/.ssh/id_rsa");
				jsch.setKnownHosts("/root/.ssh/known_hosts");
			} catch (JSchException e) {
				e.printStackTrace();
			}
			return jsch;
		}
	}

	private void sendEvent(String topic, String repo) {
		Map props = new HashMap();
		props.put("repository", repo);
		debug("GitService.sendEvent.postEvent:" + m_eventAdmin);
		m_eventAdmin.postEvent(new Event("git/" + topic, props));
	}

	@Reference(dynamic = true)
	public void setEventAdmin(EventAdmin paramEventAdmin) {
		debug("GitServiceImpl.setEventAdmin:" + paramEventAdmin);
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
	private static final org.slf4j.Logger m_logger = org.slf4j.LoggerFactory.getLogger(GitServiceImpl.class);
}
