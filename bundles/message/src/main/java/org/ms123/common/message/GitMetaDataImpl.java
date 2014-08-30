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
package org.ms123.common.message;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Date;
import java.io.StringReader;
import java.io.Reader;
import flexjson.*;
import org.ms123.common.data.api.SessionContext;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.utils.ParameterParser;
import org.ms123.common.git.GitService;
import org.ms123.common.libhelper.Inflector;
import org.ms123.common.store.StoreDesc;
import static java.text.MessageFormat.format;
import org.ms123.common.data.query.QueryBuilder;
import org.mvel2.MVEL;
import com.Ostermiller.util.*;

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

	public List<Map> getLanguages(String namespace) throws Exception {
		List<String> types = new ArrayList();
		types.add(MESSAGESLANG_TYPE);
		Map map = m_gitService.getWorkingTree(namespace, MESSAGES_PATH, 100, types, null, null,null);
		List<Map> childList = (List) map.get("children");
		for (Map child : childList) {
			String name = (String) child.get("name");
			Map m = new HashMap();
			m.put("name", name);
			child.putAll(m);
		}
		return childList;
	}

	public List<Map> getMessages(String namespace, String lang, Map filter) throws Exception {
		String content = m_gitService.getContent(namespace, format(MESSAGESLANG_PATH, lang));
		List messages = (List) m_ds.deserialize(content);
		return filterList(namespace, messages, filter);
	}

	public Map<String, String> getMessage(String namespace, String lang, String id) throws Exception {
		String ret = m_gitService.getContent(namespace, format(MESSAGESLANG_PATH, lang));
		return getMessageById((List) m_ds.deserialize(ret), id);
	}

	public void saveMessage(String namespace, String lang, Map msg,boolean overwrite) throws Exception {
		String id = (String) msg.get("msgid");
		String ret = m_gitService.getContent(namespace, format(MESSAGESLANG_PATH, lang));
		List<Map> msgs = (List) m_ds.deserialize(ret);
		Map _msg = getMessageById(msgs, id);
		if (_msg != null) {
			if( overwrite){
				_msg.putAll(msg);
			}
		} else {
			msgs.add(msg);
		}
		m_gitService.putContent(namespace, format(MESSAGESLANG_PATH, lang), MESSAGESLANG_TYPE, m_js.deepSerialize(msgs));
	}

	public void addMessages(String namespace, String lang, List<Map> msgList,boolean overwrite) throws Exception {
		String ret = m_gitService.getContent(namespace, format(MESSAGESLANG_PATH, lang));
		List<Map> msgs = (List) m_ds.deserialize(ret);
		for( Map msg : msgList){
			String id = (String) msg.get("msgid");
			Map _msg = getMessageById(msgs, id);
			if (_msg != null) {
				if( overwrite){
					_msg.putAll(msg);
				}
			} else {
				msgs.add(msg);
			}
		}
		m_gitService.putContent(namespace, format(MESSAGESLANG_PATH, lang), MESSAGESLANG_TYPE, m_js.deepSerialize(msgs));
	}

	public void saveMessages(String namespace, String lang, List<Map> msgs) throws Exception {
		m_gitService.putContent(namespace, format(MESSAGESLANG_PATH, lang), MESSAGESLANG_TYPE, m_js.deepSerialize(msgs));
	}

	public void deleteMessages(String namespace, String lang,List<String> msgIds) throws Exception {
		if( msgIds == null){
			m_gitService.deleteObject(namespace, format(MESSAGESLANG_PATH, lang));
		}else{
			String ret = m_gitService.getContent(namespace, format(MESSAGESLANG_PATH, lang));
			List<Map> msgs = (List) m_ds.deserialize(ret);
			for( String id : msgIds){
				Map _msg = getMessageById(msgs, id);
				if (_msg != null) {
					msgs.remove(_msg);
				}
			}
			m_gitService.putContent(namespace, format(MESSAGESLANG_PATH, lang), MESSAGESLANG_TYPE, m_js.deepSerialize(msgs));
		}
	}

	public void deleteMessage(String namespace, String lang, String id) throws Exception {
		String ret = m_gitService.getContent(namespace, format(MESSAGESLANG_PATH, lang));
		List<Map> msgs = (List) m_ds.deserialize(ret);
		Map _msg = getMessageById(msgs, id);
		if (_msg != null) {
			msgs.remove(_msg);
		}
		m_gitService.putContent(namespace, format(MESSAGESLANG_PATH, lang), MESSAGESLANG_TYPE, m_js.deepSerialize(msgs));
	}

	private Map getMessageById(List<Map> msgs, String id) {
		for (Map msg : msgs) {
			if (msg.get("msgid").equals(id)) {
				return msg;
			}
		}
		return null;
	}

	public List<Map> parseCSV(Reader reader) throws Exception {
		CSVParse parser = new ExcelCSVParser(reader, ',');
		parser.changeQuote('"');
		LabeledCSVParser lparser = new LabeledCSVParser(parser);
		List<Map> resultList = new ArrayList();
		while (lparser.getLine() != null) {
			Map<String, String> mMap = new HashMap();
			mMap.put("msgid", lparser.getValueByLabel("msgid"));
			mMap.put("msgstr", lparser.getValueByLabel("msgstr"));
			resultList.add(mMap);
		}
		return resultList;
	}

	private List<Map> getMessageList(String namespace, String lang) throws Exception {
		Map map = m_gitService.getContentCheckRaw(namespace, format(MESSAGESLANG_PATH, lang));
		if (((Boolean) map.get("raw")) == false) {
			return (List) m_ds.deserialize((String) map.get("content"));
		}
		List<Map> retList = parseCSV(new StringReader((String) map.get("content")));
		m_gitService.putContent(namespace, format(MESSAGESLANG_PATH, lang), MESSAGESLANG_TYPE, m_js.deepSerialize(retList));
		return retList;
	}

	private List<Map> filterList(String namespace, List<Map> msgs, Map filter) {
		if (filter == null)
			return msgs;
		StoreDesc sdesc = StoreDesc.getNamespaceMeta(namespace);
		QueryBuilder qb = new QueryBuilder("mvel", sdesc, "message", false, null,null, null, filter, null, null);
		String evalString = qb.getWhere();
		System.out.println("condition:" + evalString);
		List<Map> retList = new ArrayList();
		for (Map msg : msgs) {
			if (MVEL.evalToBoolean(evalString, msg)) {
				retList.add(msg);
			}
		}
		return retList;
	}
}
