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
package org.ms123.common.store;

import java.util.*;
import java.io.File;
import java.io.FileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Bundle;
import aQute.bnd.annotation.metatype.*;
import aQute.bnd.annotation.component.*;
import org.ms123.common.rpc.PName;
import org.ms123.common.git.*;
import org.eclipse.jgit.storage.file.*;
import org.eclipse.jgit.util.*;
import org.osgi.framework.BundleContext;
import org.ms123.common.rpc.POptional;
import org.ms123.common.rpc.RpcException;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.ms123.common.rpc.PDefaultBool;
import static org.ms123.common.rpc.JsonRpcServlet.ERROR_FROM_METHOD;
import static org.ms123.common.rpc.JsonRpcServlet.INTERNAL_SERVER_ERROR;
import static org.ms123.common.rpc.JsonRpcServlet.PERMISSION_DENIED;

@SuppressWarnings("unchecked")
@Component(enabled = true, configurationPolicy = ConfigurationPolicy.optional, immediate = true, properties = { "rpc.prefix=store" })
public class StoreServiceImpl implements StoreService {

	private BundleContext m_bc;

	protected void activate(BundleContext bundleContext, Map<?, ?> props) {
	}

	protected void deactivate() throws Exception {
		System.out.println("StoreServiceImpl.deactivate");
	}
	/*BEGIN JSON-RPC-API*/
	public Map getStoreDescriptions(
			@PName(StoreDesc.NAMESPACE) String namespace) throws RpcException {
		try {
			return _getStoreDescriptions(namespace);
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "StoreServiceImpl.getStoreDescription", e);
		}
	}
	/*BEGIN JSON-RPC-API*/
	protected static List<String> _getNamespaces() throws Exception {
		FileFilter directoryFilter = new FileFilter() {

			public boolean accept(File file) {
				return file.isDirectory();
			}
		};
		List<String> retList = new ArrayList();
		String gitSpace = System.getProperty("git.repos");
		File gitSpaceDir = new File(gitSpace);
		File[] dirs = gitSpaceDir.listFiles(directoryFilter);
		for (File dir : dirs) {
			if( org.ms123.common.git.GitServiceImpl.hasStoreCfg(dir)){
				retList.add(dir.getName());
			}
		}
		return retList;
	}

	protected static Map _getStoreDescriptions(String namespace) throws Exception {
		String gitSpace = System.getProperty("git.repos");
		File dir = new File(gitSpace);
		String storeFileName = "store.cfg";
		File storeCfgFile = new File(gitSpace + "/" + namespace, storeFileName);
		if (!storeCfgFile.exists()) {
			throw new RuntimeException("StoreServiceImpl.storeCfg not exists:" + storeCfgFile);
		}
		FS fs = FS.detect();
		FileBasedConfig fbc = new FileBasedConfig(storeCfgFile, fs);
		fbc.load();
		Set<String> subList = fbc.getSubsections("store");
		Map storeMap = new HashMap();
		for (String storeId : subList) {
			String pack = fbc.getString("store", storeId, "pack");
			String ns = fbc.getString("store", storeId, "namespace");
			String db = fbc.getString("store", storeId, "database");
			String dbn = fbc.getString("store", storeId, "databasename");
			String dbh = fbc.getString("store", storeId, "databasehost");
			String repo = fbc.getString("store", storeId, "repository");
			if (repo == null){
				repo = ns;
			}
			Map m = new HashMap();
			m.put(StoreDesc.PACK, pack);
			m.put(StoreDesc.STORE, db);
			m.put(StoreDesc.NAMESPACE, ns);
			m.put(StoreDesc.REPOSITORY, repo);
			m.put(StoreDesc.DATABASENAME, dbn);
			m.put(StoreDesc.DATABASEHOST, dbh);
			m.put(StoreDesc.STORE_ID, namespace + "_" + storeId);
			storeMap.put(namespace + "_" + storeId, m);
		}
		debug("storeMap:" + storeMap);
		return storeMap;
	}

	protected static void debug(String msg) {
		//System.out.println(msg);
		m_logger.debug(msg);
	}
	protected void info(String msg) {
		System.out.println(msg);
		m_logger.info(msg);
	}
	private static final org.slf4j.Logger m_logger = org.slf4j.LoggerFactory.getLogger(StoreServiceImpl.class);
}
