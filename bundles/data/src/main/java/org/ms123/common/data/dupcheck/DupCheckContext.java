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
package org.ms123.common.data.dupcheck;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collection;
import java.util.Date;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.data.api.SessionContext;
import org.ms123.common.libhelper.Inflector;

@SuppressWarnings("unchecked")
public class DupCheckContext {

	private SessionContext m_sc;

	private String m_entityName;

	private List<Map> m_dupFields;

	private Map<String, String[]> m_algoMap;

	private List<Map> m_cvList = new ArrayList();

	private List m_idHitList = new ArrayList();

	private List m_dupList = new ArrayList();

	private List m_okList = new ArrayList();
	private Map<String,List> m_dupMap = new HashMap();

	private String m_idField = null;

	private Map<String, String> m_inputEncCache = new HashMap();

	protected int m_paramCount = 1;
	protected Map m_queryParams = new HashMap();

	public DupCheckContext() {
		this(null, null, null);
	}

	public DupCheckContext(SessionContext sc, String entityName) {
		this(sc, entityName, null);
	}

	public DupCheckContext(SessionContext sc, String entityName, String idField) {
		m_sc = sc;
		m_idField = idField;
		m_entityName = entityName;
		if( entityName != null){
			m_dupFields = getDupFields(entityName);
			m_algoMap = getFieldAlgoMap(m_dupFields);
		}
	}

	public String getEntityName() {
		return m_entityName;
	}

	public List<Map> getDupFields() {
		return m_dupFields;
	}

	public Map<String, String[]> getFieldAlgoMap() {
		return m_algoMap;
	}

	public SessionContext getSessionContext() {
		return m_sc;
	}

	public List<Map> getCVList() {
		return m_cvList;
	}

	public List getIdHitList() {
		return m_idHitList;
	}

	public List getDupList() {
		return m_dupList;
	}

	public Map<String,List> getDupMap() {
		return m_dupMap;
	}

	public List getOkList() {
		return m_okList;
	}

	public String getIdField() {
		return m_idField;
	}

	public Map getQueryParams() {
		return m_queryParams;
	}

	public int getParamCount() {
		return m_paramCount;
	}

	public void incParamCount() {
		m_paramCount++;
	}

	public Map<String, String> getInputEncCache() {
		return m_inputEncCache;
	}

	private List<Map> getDupFields(String entityName) {
		try {
			return m_sc.getSettingService().getFieldsForEntityView(m_sc.getStoreDesc().getNamespace(), entityName, "duplicate-check");
		} catch (Exception e) {
			return new ArrayList();
		}
	}

	private Map<String, String[]> getFieldAlgoMap(List<Map> filterFields) {
		Map<String, String[]> fieldAlgoMap = new HashMap();
		for (Map field : filterFields) {
			String name = (String) field.get("name");
			String datatype = (String) field.get("datatype");
			String threshold = String.valueOf( field.get("threshold"));
			if (datatype == null){
				datatype = "string";
			}
			if (threshold == null || "null".equals(threshold)){
				threshold = "0.85";
			}
			String checkType = (String) field.get("check_type");
			if (checkType == null){
				checkType = "both";
			}
			if ("string".equals(datatype) || "text".equals(datatype)) {
				if (checkType.equals("both")) {
					fieldAlgoMap.put(name, new String[] { datatype, "jaro", threshold, "metaphone" });
				}
				if (checkType.equals("fuzzy")) {
					fieldAlgoMap.put(name, new String[] { datatype, "jaro", threshold, "metaphone" });
				}
				if (checkType.equals("phonetic")) {
					fieldAlgoMap.put(name, new String[] { datatype, "metaphone" });
				}
				if (checkType.equals("distance")) {
					fieldAlgoMap.put(name, new String[] { datatype, "jaro", threshold });
				}
			}
			if (checkType.equals("equal")) {
				fieldAlgoMap.put(name, new String[] { datatype, "equal" });
			}
		}
		return fieldAlgoMap;
	}
}
