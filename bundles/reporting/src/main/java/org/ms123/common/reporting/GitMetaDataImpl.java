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

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Date;
import flexjson.*;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.utils.ParameterParser;
import org.ms123.common.git.GitService;
import org.ms123.common.libhelper.Inflector;
import org.ms123.common.store.StoreDesc;
import static java.text.MessageFormat.format;

/**
 *
 */
@SuppressWarnings("unchecked")
class GitMetaDataImpl implements MetaData {

	protected Inflector m_inflector = Inflector.getInstance();

	protected JSONDeserializer m_ds = new JSONDeserializer();

	protected JSONSerializer m_js = new JSONSerializer();
	private GitService m_gitService;

	/**
	 */
	public GitMetaDataImpl(GitService gs) {
		m_gitService = gs;
		m_js.prettyPrint(true);
	}

	public List<Map> getReports(String namespace) throws Exception{
		StoreDesc sdesc = StoreDesc.getNamespaceData(namespace);
		List<String> types = new ArrayList();
		types.add( REPORT_TYPE );
		String user = getUserName();
		Map map= m_gitService.getWorkingTree(sdesc.getRepository(), format(REPORTS_USER_PATH,user), 100, types, null, null,null);
		List<Map> childList = (List)map.get("children");
		List<Map> retList = new ArrayList();
		for( Map child : childList){
			String name = (String)child.get("name");
			String content= m_gitService.getContent(sdesc.getRepository(), format(REPORT_USER_PATH,user,name));
			Map m = new HashMap();
			if( content != null && !content.trim().equals("")){
				m = (Map)m_ds.deserialize(content);
			}
			m.put("name", name);
			retList.add(m);
		}
		return retList;
	}

	public Map<String,List>  getReport(String namespace, String name) throws Exception{
		StoreDesc sdesc = StoreDesc.getNamespaceData(namespace);
		String user = getUserName();
		String ret = m_gitService.getContent(sdesc.getRepository(), format(REPORT_USER_PATH,user,name));
		return (Map)m_ds.deserialize(ret );
	}

	public void saveReport(String namespace, String name, Map<String,List> desc) throws Exception{
		StoreDesc sdesc = StoreDesc.getNamespaceData(namespace);
		String user = getUserName();
		m_gitService.putContentInternal(sdesc.getRepository(), format(REPORT_USER_PATH,user,name), REPORT_TYPE, m_js.deepSerialize(desc));
	}

	public void deleteReport(String namespace, String name) throws Exception{
		StoreDesc sdesc = StoreDesc.getNamespaceData(namespace);
		String user = getUserName();
		m_gitService.deleteObjectInternal(sdesc.getRepository(), format(REPORT_USER_PATH,user,name));
	}

	public String getUserName() {
		return org.ms123.common.system.thread.ThreadContext.getThreadContext().getUserName();
	}
}
