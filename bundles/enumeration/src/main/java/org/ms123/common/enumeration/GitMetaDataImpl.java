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
package org.ms123.common.enumeration;

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

	public List<Map> getEnumerations(String namespace) throws Exception{
		List<String> types = new ArrayList();
		types.add( ENUMERATION_TYPE );
		Map map= m_gitService.getWorkingTree(namespace, ENUMERATIONS_PATH, 100, types, null, null,null);
		List<Map> childList = (List)map.get("children");
		for( Map child : childList){
			String name = (String)child.get("name");
			//String content= m_gitService.getContent(namespace, format(ENTITYTYPE_PATH,name));
			//Map m = (Map)m_ds.deserialize(content);
			Map m = new HashMap();
			m.put("name", name);
			child.putAll(m);
		}
		return childList;
	}

	public Map<String,List>  getEnumeration(String namespace, String name) throws Exception{
		String ret = m_gitService.getContent(namespace, format(ENUMERATION_PATH,name));
		return (Map)m_ds.deserialize(ret );
	}

	public void saveEnumeration(String namespace, String name, Map<String,List> desc) throws Exception{
		m_gitService.putContent(namespace, format(ENUMERATION_PATH,name), ENUMERATION_TYPE, m_js.deepSerialize(desc));
	}


	public void deleteEnumeration(String namespace, String name) throws Exception{
		m_gitService.deleteObject(namespace, format(ENUMERATION_PATH,name));
	}

}
