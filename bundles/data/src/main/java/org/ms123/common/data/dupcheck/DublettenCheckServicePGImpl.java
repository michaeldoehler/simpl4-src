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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.StringTokenizer;
import java.util.regex.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import org.ms123.common.utils.*;
import org.ms123.common.data.*;
import org.apache.commons.beanutils.*;
import org.apache.commons.codec.language.DoubleMetaphone;
import org.apache.commons.codec.language.Metaphone;
import org.apache.commons.codec.language.RefinedSoundex;
import org.apache.commons.codec.language.Soundex;
import org.apache.commons.codec.language.ColognePhonetic;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.StringEncoder;
import com.wcohen.ss.Jaro;
import com.wcohen.ss.JaroWinkler;
import com.wcohen.ss.NeedlemanWunsch;
import org.apache.commons.lang.StringUtils;
import com.google.common.collect.*;
import java.lang.reflect.*;
import java.lang.annotation.*;
import javax.jdo.annotations.Element;
import flexjson.*;
import org.ms123.common.data.api.SessionContext;
import org.ms123.common.nucleus.api.NucleusService;
import org.ms123.common.setting.api.SettingService;
import org.ms123.common.store.StoreDesc;
import aQute.bnd.annotation.metatype.*;
import aQute.bnd.annotation.component.*;

@SuppressWarnings("unchecked")
@Component(enabled = true, configurationPolicy = ConfigurationPolicy.optional, immediate = true, properties = { "impl=pg" })
public class DublettenCheckServicePGImpl implements DublettenCheckService {

	private static final Logger m_logger = LoggerFactory.getLogger(DublettenCheckServicePGImpl.class);

	protected Inflector m_inflector = Inflector.getInstance();

	// OsgiActivate 
	public void activate() {
		System.out.println("DublettenCheckServicePGImpl.activate");
	}

	public DupCheckContext getContext() {
		return new DupCheckContext();
	}

	public DupCheckContext getContext(SessionContext sc, String entityName) {
		return new DupCheckContext(sc, entityName);
	}

	public DupCheckContext getContext(SessionContext sc, String entityName, String idField) {
		return new DupCheckContext(sc, entityName, idField);
	}

	public Map dublettenCheck(DupCheckContext dcc, Object dataObject) {
		List<Map> filterFields = dcc.getDupFields();
		if (dataObject instanceof Map) {
			return dublettenCheck(dcc, (Map) dataObject);
		}
		Map<String, Object> dataMap = new HashMap();
		for (Map field : filterFields) {
			String name = (String) field.get("name");
			try {
				dataMap.put(name, PropertyUtils.getProperty(dataObject, name));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		List<Map> dataList = new ArrayList();
		dataList.add(dataMap);
		return dublettenCheck(dcc, dataList);
	}

	public Map dublettenCheck(DupCheckContext dcc, Map dataMap) {
		List<Map> dataList = new ArrayList();
		dataList.add(dataMap);
		return dublettenCheck(dcc, dataList);
	}

	public Map dublettenCheck(DupCheckContext dcc, List<Map> dataList) {
		Map retMap = new HashMap();
		try {
			for (Map dataMap : dataList) {
				dublettenCheckOne(dcc, dataMap);
			}
		} finally {
			List<Map> cvList = dcc.getCVList();
			List idHitList = dcc.getIdHitList();
			if (cvList.size() > 0) {
				retMap.put("constraintViolations", cvList);
				retMap.put("idHitList", idHitList);
			}
		}
		return retMap;
	}

	public void dublettenCheckOne(DupCheckContext dcc, List<Map> compareList, Map dataMap) {
		throw new RuntimeException("DublettenCheckServicePGImpl.dublettenCheckOne not implemented");
	}

	public boolean compare(DupCheckContext dcc, String valueInput, String valueCompareTo, String[] algos) {
		throw new RuntimeException("DublettenCheckServicePGImpl.compare not implemented");
	}

	private void dublettenCheckOne(DupCheckContext dcc, Map dataMap) {
		List<Map> cvList = dcc.getCVList();
		List idHitList = dcc.getIdHitList();
		SessionContext sc = dcc.getSessionContext();
		String entityName = dcc.getEntityName();
		Map<String, String[]> fieldAlgoMap = dcc.getFieldAlgoMap();
		String komma = "";
		String className = m_inflector.getClassName(entityName);
		Query q = null;
		try {
			int i = 0;
			boolean isDup = false;
			String where = getWhereClause(dcc, fieldAlgoMap, dataMap);
			if (where.length() == 0) {
				return;
			}
			String sql = "select c from " + className + " as c where " + where;
			System.out.println("dupcheck.sql:" + sql);
			System.out.println("dupcheck.app:" + sc.getStoreDesc());
			q = sc.getPM().newQuery("javax.jdo.query.JPQL", sql);
			q.declareImports(sc.getStoreDesc().getImports());
			System.out.println("dupcheck.queryParams:" + dcc.getQueryParams());
			List results = (List) q.executeWithMap(dcc.getQueryParams());
			System.out.println("dupcheck.resultSize:" + results);
			for (Object o : results) {
				Map map = UtilsServiceImpl.copyObject(o);
				dataMap.put("_duplicated_", "ja");
				dataMap.put("_duplicated_id_", map.get("id"));
				for (String field : fieldAlgoMap.keySet()) {
					Map<String, Object> mapCV = new HashMap();
					mapCV.put("message", (String) map.get(field));
					mapCV.put("path", field);
					if (i == 0) {
						mapCV.put("obj", map);
					}
					cvList.add(mapCV);
					i++;
				}
				idHitList.add(map.get("id"));
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} finally {
			if (q != null) {
				q.closeAll();
			}
		}
	}

	private String getWhereClause(DupCheckContext dcc, Map<String, String[]> fieldAlgoMap, Map dataMap) {
		String where = "";
		String and = "";
		for (String field : fieldAlgoMap.keySet()) {
			String[] algos = fieldAlgoMap.get(field);
			String datatype = algos[0];
			where += and + "(";
			String or = "";
			Object value = getValue(dataMap.get(field), datatype);
			/*if (value == null || "".equals(value)) {
				if ("string".equals(datatype) || "text".equals(datatype)) {
					where += or + getEqualOrIsNullClause(dcc, field, datatype, "");
					or = " or ";
					where += ")";
					and = " and ";
				}
				continue;
			}*/
			for (int i = 1; i < algos.length; i++) {
				String algo = algos[i];
				if (value == null) {
					where += or + getIsNullClause(dcc, field, datatype, dataMap.get(field));
					or = " or ";
					continue;
				}
				if ("metaphone".equals(algo)) {
					where += or + getMetaphoneClause(dcc, field, (String) value);
					or = " or ";
				}
				if ("jaro".equals(algo)) {
					String threshold = algos[++i];
					where += or + getJaroClause(dcc, threshold, field, (String) value);
					or = " or ";
				}
				if ("equal".equals(algo)) {
					where += or + getEqualClause(dcc, field, datatype, dataMap.get(field));
					or = " or ";
				}
			}
			where += ")";
			and = " and ";
		}
		return where;
	}

	private String getEqualOrIsNullClause(DupCheckContext dcc, String field, String datatype, Object value) {
		String pn = ":param" + dcc.getParamCount();
		Map qp = dcc.getQueryParams();
		dcc.incParamCount();
		if ("string".equals(datatype) || "text".equals(datatype)) {
			qp.put(pn.substring(1), ((String) value).toLowerCase());
			return "(lower(" + field + ") = " + pn + " or " + field + " is null)";
		}
		qp.put(pn.substring(1), value);
		return "(" + field + " = " + pn + ")";
	}
	private String getIsNullClause(DupCheckContext dcc, String field, String datatype, Object value) {
		return "(" + field + " is null)";
	}

	private String getEqualClause(DupCheckContext dcc, String field, String datatype, Object value) {
		String pn = ":param" + dcc.getParamCount();
		Map qp = dcc.getQueryParams();
		dcc.incParamCount();
		if (value != null && ("string".equals(datatype) || "text".equals(datatype))) {
			qp.put(pn.substring(1), ((String) value).toLowerCase());
			return "(lower(" + field + ") = " + pn + ")";
		}
		qp.put(pn.substring(1), value);
		return "(" + field + " = " + pn + ")";
	}

	private String getMetaphoneClause(DupCheckContext dcc, String field, String value) {
		String pn = ":param" + dcc.getParamCount();
		Map qp = dcc.getQueryParams();
		dcc.incParamCount();
		qp.put(pn.substring(1), value);
		return "(" + field + ".metaphone(" + pn + "))";
	}

	private String getJaroClause(DupCheckContext dcc, String threshold, String field, String value) {
		String pn = ":param" + dcc.getParamCount();
		Map qp = dcc.getQueryParams();
		dcc.incParamCount();
		qp.put(pn.substring(1), value);
		return "(" + field + ".jarowinkler(" + pn + ")>" + threshold + ")";
	}

	private Object getValue(Object value, String datatype) {
		return value;
	}
}
