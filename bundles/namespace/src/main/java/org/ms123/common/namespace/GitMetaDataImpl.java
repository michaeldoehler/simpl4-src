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

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Date;
import flexjson.*;
import org.ms123.common.data.api.SessionContext;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.git.GitService;
import org.ms123.common.store.StoreDesc;
import static java.text.MessageFormat.format;
import static org.apache.commons.io.FileUtils.readFileToString;

/**
 *
 */
@SuppressWarnings("unchecked")
class GitMetaDataImpl implements MetaData {

	protected JSONDeserializer m_ds = new JSONDeserializer();

	protected JSONSerializer m_js = new JSONSerializer();

	private GitService m_gitService;

	/**
	 */
	public GitMetaDataImpl(GitService gs) {
		m_gitService = gs;
		m_js.prettyPrint(true);
	}

	public List<Map> getNamespaces() throws Exception {
		StoreDesc sdesc = StoreDesc.getGlobalData();
		List<String> types = new ArrayList();
		types.add(NAMESPACE_TYPE);
		Map map = m_gitService.getWorkingTree(sdesc.getRepository(), NAMESPACES_PATH, 100, types, null, null, null);
		List<Map> childList = (List) map.get("children");
		for (Map child : childList) {
			String name = (String) child.get("name");
			Map m = new HashMap();
			m.put("name", name);
			child.putAll(m);
			child.remove("children");
			child.remove("path");
		}
		return childList;
	}

	public Map<String, String> getBranding() throws Exception {
		String swDir = System.getProperty("simpl4.dir");
		String ret = readFileToString(new File(swDir, BRANDING_PATH));
		return (Map) m_ds.deserialize(ret);
	}

	public void saveBranding(Map<String, String> desc) throws Exception {
		StoreDesc sdesc = StoreDesc.getGlobalData();
		m_gitService.putContent(sdesc.getRepository(), BRANDING_PATH, "sw.setting", m_js.deepSerialize(desc));
	}

	public Map<String, List> getNamespace(String name) throws Exception {
		StoreDesc sdesc = StoreDesc.getGlobalData();
		String ret = m_gitService.getContent(sdesc.getRepository(), format(NAMESPACE_PATH, name));
		return (Map) m_ds.deserialize(ret);
	}

	public void saveNamespace(String name, Map<String, List> desc) throws Exception {
		StoreDesc sdesc = StoreDesc.getGlobalData();
		m_gitService.putContent(sdesc.getRepository(), format(NAMESPACE_PATH, name), NAMESPACE_TYPE, m_js.deepSerialize(desc));
	}

	public void deleteNamespace(String name) throws Exception {
		StoreDesc sdesc = StoreDesc.getGlobalData();
		m_gitService.deleteObject(sdesc.getRepository(), format(NAMESPACE_PATH, name));
	}
}
