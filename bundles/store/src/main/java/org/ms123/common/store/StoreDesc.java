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
import java.io.FilenameFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ms123.common.libhelper.Inflector;

@SuppressWarnings("unchecked")
public class StoreDesc {

	private static final Logger m_logger = LoggerFactory.getLogger(StoreDesc.class);

	protected Inflector m_inflector = Inflector.getInstance();

	public static final String NAMESPACE = "namespace";

	public static final String NAMESPACE_GLOBAL = "global";

	public static final String PACK = "pack";

	public static final String STORE = "store";

	public static final String STORE_ID = "storeId";

	public static final String REPOSITORY = "repository";
	public static final String DATABASENAME = "databasename";
	public static final String DATABASEHOST = "databasehost";

	public static final String PACK_DATA = "data";

	public static final String PACK_AID = "aid";

	public static final String STORE_FILE = "file";

	public static final String STORE_RDBMS = "rdbms";

	public static final String VENDOR_PG = "pg";

	public static final String VENDOR_H2 = "h2";

	public static final String VENDOR_HSQL = "hsql";

	public static final String RDBMS_H2 = "rdbms:h2";

	public static final String RDBMS_PG = "rdbms:pg";

	private String m_namespace;
	private String m_databaseName;
	private String m_databaseHost;

	private String m_package;

	private String m_repository;

	private String m_store;

	private String m_vendor;

	private String m_storeId;

	private StoreDesc(String storeId, String namespace, String pack, String store, String repo, String dbName, String dbHost) {
		m_package = assign(pack, PACK_DATA);
		m_namespace = namespace;
		int colon = store.indexOf(":");
		if (colon > -1) {
			m_store = store.substring(0, colon);
			m_vendor = store.substring(colon + 1);
		} else {
			m_store = store;
		}
		m_storeId = storeId;
		m_repository = repo != null ? repo : m_namespace;
		m_databaseName = dbName != null ? dbName : m_namespace;
		m_databaseHost = dbHost != null ? dbHost : "localhost";
	}

	public String getNamespace() {
		return m_namespace;
	}

	public String getRepository() {
		return m_repository;
	}

	public String getDatabaseName() {
		return m_databaseName;
	}

	public String getDatabaseHost() {
		return m_databaseHost;
	}

	public String getStore() {
		return m_store;
	}

	public String getStoreId() {
		return m_storeId;
	}

	public String getPack() {
		return m_package;
	}

	public String getDBPack() {
		return m_package;
	}

	public String getJavaPackage() {
		if (isAidPack()) {
			return PACK_AID;
		}
		return getNamespace() + "." + getPack();
	}

	public String getVendor() {
		return m_vendor;
	}

	public String getImports() {
		return "import " + getJavaPackage() + ".*;import aid.*";
	}

	public File getBaseDir() {
		return new File(System.getProperty("workspace"), "java");
	}
	private String getBasename(String base) {
		int index = base.lastIndexOf('.');

		if (index != -1) {
			base = base.substring(0, index);
		}
		return base;
	}
	public Set<String> getClasses(){
		File dir = new File(getBaseDir()+"/classes/"+getNamespace()+"/"+getPack());
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File directory, String fileName) {
				return fileName.endsWith(".class");
			}
		};
		String[] files = dir.list(filter);
		Set<String> ret = new HashSet();
		for( String f : files ){
			ret.add( getNamespace()+"."+getPack()+"."+getBasename(f));	
		}
		return ret;
	}

	public String getFQN(String entityname) {
		String className = m_inflector.getClassName(entityname);
		if( className.indexOf(".") == -1){
			return getJavaPackage() + "." + className;
		}
		return className;
	}

	public String insertJavaPackage(String entityname) {
		String s = entityname;
		if (!s.startsWith(PACK_AID) && !s.startsWith(getNamespace())) {
			int dot = s.lastIndexOf(".");
			if (dot != -1) {
				s = getJavaPackage() + "." + m_inflector.getClassName(s.substring(dot + 1));
			}
		} else {
			int dot = s.lastIndexOf(".");
			if (dot != -1) {
				s = s.substring(0, dot) + "." + m_inflector.getClassName(s.substring(dot + 1));
			}
		}
		int dot = s.lastIndexOf(".");
		if (dot == -1) {
			s = m_inflector.getClassName(s.substring(dot + 1));
		}
		return s;
	}

	public boolean isAidPack() {
		if (getPack().equals(StoreDesc.PACK_AID)) {
			return true;
		}
		return false;
	}

	public static boolean isAidPack(String pack) {
		if (pack.equals(StoreDesc.PACK_AID)) {
			return true;
		}
		return false;
	}

	public boolean isDataPack(String pack) {
		return getStoreId().endsWith("_data");
		//return !(isAidPack(pack));
	}

	public boolean isDataPack() {
		return getStoreId().endsWith("_data");
		//return !(isAidPack(getPack()));
	}

	public boolean isSamePack(String pack) {
		if (m_package == null && pack == null)
			return true;
		return pack != null && pack.equals(m_package);
	}

	public String getIdType() {
		return "string";
	}

	public String getStoreBaseDir() {
		String gr = System.getProperty("git.repos");
		if (this.getStore().equals(STORE_RDBMS)) {
			if (this.getVendor().equals(VENDOR_H2) || this.getVendor().equals(VENDOR_HSQL)) {
				return gr + "/" + getRepository() + "/store/" + getDBPack();
			}
		} else if (this.getStore().equals(STORE_FILE)) {
			return gr + "/" + getRepository() + "/store/" + getDBPack();
		}
		throw new RuntimeException("StoreDesc.getStoreBaseDir.no_basedir:" + toString());
	}

	public String getString() {
		if (this.getStore().equals(STORE_RDBMS)) {
			return m_storeId + "/" + m_namespace + "," + m_package + "," + m_store + "," + m_vendor;
		} else {
			return m_storeId + "/" + m_namespace + "," + m_package + "," + m_store;
		}
	}

	public String toString() {
		return "[" + getString() + "]";
	}

	private String assign(String s1, String s2) {
		if (s1 == null) {
			return s2;
		}
		return s1;
	}

	@Override
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		}
		if (!(other instanceof StoreDesc)) {
			return false;
		}
		return getStoreId().equals(((StoreDesc) other).getStoreId());
	}

	public static StoreDesc get(String id) {
		if (m_storeIds == null || m_storeIds.get(id) ==null) {
			init();
		}
		StoreDesc sd = m_storeIds.get(id);
		if (sd == null) {
			throw new RuntimeException("StoreDesc.get:id \"" + id + "\" not found");
		}
		return sd;
	}

	public static StoreDesc getNamespaceMeta(String namespace) {
		if (m_storeIds == null || (m_storeIds.get(namespace + "_meta") == null)) {
			init();
		}
		StoreDesc s = m_storeIds.get(namespace + "_meta");
		if (s != null) {
			return s;
		}
		return m_storeIds.get("global_meta");
	}

	public static StoreDesc getNamespaceData(String namespace) {
		if (m_storeIds == null || (m_storeIds.get(namespace + "_data") == null)) {
			init();
		}
		return m_storeIds.get(namespace + "_data");
	}

	public static StoreDesc getGlobalData() {
		if (m_storeIds == null) {
			init();
		}
		return m_storeIds.get("global_data");
	}

	public static StoreDesc getGlobalMeta() {
		if (m_storeIds == null) {
			init();
		}
		return m_storeIds.get("global_meta");
	}

	public static void init() {
		try {
			m_storeIds = new HashMap();
			List<String> namespaces = StoreServiceImpl._getNamespaces();
			for (String ns : namespaces) {
				Map<String, Map> x = StoreServiceImpl._getStoreDescriptions(ns);
				if (x == null){
					continue;
				}
				for (String key : x.keySet()) {
					Map<String, String> m = x.get(key);
					StoreDesc sdesc = new StoreDesc(m.get(STORE_ID), m.get(NAMESPACE), m.get(PACK), 
							m.get(STORE), 
							m.get(REPOSITORY),
							m.get(DATABASENAME),
							m.get(DATABASEHOST)
							);
					m_storeIds.put(m.get(STORE_ID), sdesc);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static Map<String, StoreDesc> m_storeIds = null;

	@Override
	public int hashCode() {
		return 9999;
	}
}
