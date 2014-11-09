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
package org.ms123.common.data.quality;

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
import java.math.*;
import java.text.DecimalFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Extent;
import org.ms123.common.utils.*;
import org.ms123.common.data.*;
import org.apache.commons.beanutils.*;
import flexjson.*;
import org.ms123.common.data.api.SessionContext;
import org.ms123.common.data.api.DataLayer;
import org.ms123.common.nucleus.api.NucleusService;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.setting.api.SettingService;
import org.apache.commons.beanutils.PropertyUtils;
import com.wcohen.ss.*;
import com.wcohen.ss.lookup.*;
import com.wcohen.ss.api.*;
import com.wcohen.ss.tokens.*;
import javax.transaction.RollbackException;
import javax.transaction.UserTransaction;
import javax.transaction.Status;
import static org.ms123.common.entity.api.Constants.STATE_FIELD;
import static org.ms123.common.entity.api.Constants.STATE_OK;
import static org.ms123.common.entity.api.Constants.STATE_DUP;
import static org.ms123.common.entity.api.Constants.STATE_NEW;
import static org.ms123.common.entity.api.Constants.STATE_REFID;
import static org.ms123.common.setting.api.Constants.GLOBAL_SETTINGS;

@SuppressWarnings({"unchecked","deprecation"})
public class QualityBatch implements Constants {

	protected Inflector m_inflector = Inflector.getInstance();

	protected SettingService m_settingService;

	protected NucleusService m_nucleusService;

	protected StoreDesc m_sdesc;

	protected String m_entityName;

	private DecimalFormat m_df = new DecimalFormat("#.0000");

	protected DataLayer m_dataLayer;

	private int m_count = 0;

	private int m_dups = 0;

	private Map<String, Object> m_permittedFields;

	private Object m_null = new Object();

	private List<Map> m_fields;

	private List<Map> m_fieldsets;

	private List<Map> m_prevFields;

	private List<Map> m_prevFieldsets;

	private JSONSerializer m_js = new JSONSerializer();

	public QualityBatch(String namespace, String entityName, DataLayer dl, SettingService ss, NucleusService ns) {
		m_entityName = entityName;
		m_settingService = ss;
		m_dataLayer = dl;
		m_nucleusService = ns;
		m_sdesc = StoreDesc.getNamespaceData(namespace);
		m_js.prettyPrint(true);
	}

	protected synchronized List<Map> doCheckFromDb(String state, String id, boolean dry) throws Exception {
		SessionContext sc = getSessionContext();
		m_permittedFields = sc.getPermittedFields(m_entityName, "write");
		init();
		m_count = 0;
		m_dups = 0;
		UserTransaction ut = m_nucleusService.getUserTransaction();
		List<Map> dupList = new ArrayList();
		try {
			ut.begin();
			Class clazz = sc.getClass(getEntityName());
			Extent e = sc.getPM().getExtent(clazz, true);
			String filter = constructFilter(state, id);
			Query q = sc.getPM().newQuery(e, filter);
			q.setFilter(filter);
			List results = (List) q.execute();
			Iterator iter = results.iterator();
			while (iter.hasNext()) {
				Object cand = iter.next();
				Object refObj = compareToOKList(sc, cand, dry);
				if (refObj != null) {
					dupList.add(getOneResult(refObj, cand));
				}
			}
			ut.commit();
		} catch (Throwable e) {
			sc.handleException(ut, e);
		} finally {
			sc.handleFinally(ut);
		}
		info("dupList:" + m_js.deepSerialize(dupList));
		return dupList;
	}

	protected synchronized List<Map> doCheckFromData(List<Map> candidateData) throws Exception {
		SessionContext sc = getSessionContext();
		m_permittedFields = sc.getPermittedFields(m_entityName, "write");
		init();
		m_count = 0;
		UserTransaction ut = m_nucleusService.getUserTransaction();
		List<Map> dupList = new ArrayList();
		try {
			ut.begin();
			Iterator<Map> iter = candidateData.iterator();
			while (iter.hasNext()) {
				Map cand = iter.next();
				setRelatedObjects(sc, cand);
				debug("candidateData:" + cand);
				Object refObj = compareToOKList(sc, cand, true);
				if (refObj != null) {
					dupList.add(getOneResult(refObj, cand));
				}
			}
			ut.commit();
		} catch (Throwable e) {
			sc.handleException(ut, e);
		} finally {
			sc.handleFinally(ut);
		}
		return dupList;
	}

	private String constructFilter(String state, String id) {
		String filter = "";
		if (state.equals(STATE_NEW)) {
			filter = STATE_FIELD + " ==  null || " + STATE_FIELD + " == \"" + STATE_NEW + "\"";
		} else if (state.equals(STATE_DUP)) {
			filter = STATE_FIELD + " == \"" + STATE_DUP + "\"";
		}
		if (id != null) {
			filter = "(" + filter + ") && " + "id == \"" + id + "\"";
		}
		debug("filter:" + filter);
		return filter;
	}

	private Object compareToOKList(SessionContext sc, Object candidate, boolean dry) throws Exception {
		reset();
		info("record.num:" + m_count++);
		Object candId = getProperty(candidate,ID);
		String entityName = getEntityName();
		Class clazz = sc.getClass(entityName);
		String filter = STATE_FIELD + " == \"" + STATE_OK + "\"";
		Extent e = sc.getPM().getExtent(clazz, true);
		Query q = sc.getPM().newQuery(e, filter);
		q.addExtension("datanucleus.rdbms.query.resultSetConcurrency", "read-only");
		q.addExtension("datanucleus.rdbms.query.fetchDirection", "forward");
		q.declareImports(m_sdesc.getImports());
		try {
			Collection coll = (Collection) q.execute();
			Iterator iter = coll.iterator();
			while (iter.hasNext()) {
				Object refObj = iter.next();
				if( candId != null){
					Object refId = getProperty(refObj,ID);
					if( candId.equals(refId)){
					 	continue;
					}
				}
				boolean b = compareOne(refObj, candidate);
				if (b == true) {
					m_dups++;
					if (!dry) {
						String refid = (String) PropertyUtils.getProperty(refObj, ID);
						PropertyUtils.setProperty(candidate, STATE_FIELD, STATE_DUP);
						PropertyUtils.setProperty(candidate, STATE_REFID, refid);
					}
					return refObj;
				}
			}
		} finally {
			q.closeAll();
		}
		if (!dry) {
			PropertyUtils.setProperty(candidate, STATE_FIELD, STATE_OK);
		}
		return null;
	}

	private boolean compareOne(Object refObj, Object candidate) throws Exception {
		List<Map> fields = getFields();
		for (Map field : fields) {
			Compare c = (Compare) field.get(COMPARE);
			boolean b = c.isEquals(refObj, candidate);
			if (b == false)
				return false;
		}
		List<Map> fieldsets = getFieldsets();
		for (Map fieldset : fieldsets) {
			if (!isEmpty(fieldset.get(EXPRESSION))) {
				GroovyEval ge = (GroovyEval) fieldset.get(GROOVY_EVAL);
				boolean b = (Boolean)ge.eval(refObj, candidate);
				if (b == false) {
					return false;
				}
			} else {
				Compare c = (Compare) fieldset.get(COMPARE);
				boolean b = c.isEquals(refObj, candidate);
				if (b == false) {
					return false;
				}
			}
		}
		return true;
	}

	private Object getProperty(Object o, String name) throws Exception {
		if (o instanceof Map) {
			Map m = (Map) o;
			return m.get(name);
		} else {
			return PropertyUtils.getProperty(o, name);
		}
	}

	private String getEntityName() {
		return m_entityName;
	}

	private synchronized List<Map> getFields() {
		if (m_fields == null) {
			m_fields = _getFields(getEntityName());
		}
		return m_fields;
	}

	private synchronized List<Map> getFieldsets() {
		if (m_fieldsets == null) {
			m_fieldsets = _getFieldsets(getEntityName());
		}
		return m_fieldsets;
	}

	private Map getOneResult(Object refObj, Object candidate) throws Exception {
		Map retMap = new HashMap();
		List<Map> fields = getFields();
		List<Map> fieldsets = getFieldsets();
		List<Map> cvList = new ArrayList();
		Object refid = getProperty(refObj, ID);
		Object id = getProperty(candidate, ID);
		for (Map<String, Object> field : fields) {
			Map<String, Object> mapCV = new HashMap();
			String fieldName = (String) field.get(NAME);
			Object content = getProperty(candidate, fieldName);
			Compare c = (Compare) field.get(COMPARE);
			if (c instanceof RelatedCompare) {
				if( content != null){
					mapCV.put("message", (String) getProperty(content, ID));
				}else{
					mapCV.put("message", "---");
				}
			} else if (c instanceof FuzzyCompare) {
				FuzzyCompare fc = (FuzzyCompare) c;
				Object contentRef = getProperty(refObj, fieldName);
				mapCV.put("message", content + "(" + m_df.format(fc.getLastScore()) + ")");
				mapCV.put("messageRef", contentRef);
				mapCV.put("score", m_df.format(fc.getLastScore()));
			} else {
				mapCV.put("message", content);
			}
			mapCV.put("path", fieldName);
			cvList.add(mapCV);
		}
		for (Map fieldset : fieldsets) {
			if (!isEmpty(fieldset.get(EXPRESSION))) {
				for (Map cmpUnit : (List<Map>) fieldset.get(COMPARE_UNITS)) {
					Compare c = (Compare) cmpUnit.get(COMPARE);
					if (c instanceof FuzzyCompare) {
						FuzzyCompare fc = (FuzzyCompare) c;
					}
					List<String> fs = (List) cmpUnit.get(FIELDS);
					for (String fieldName : fs) {
						Map<String, Object> mapCV = new HashMap();
						Object content = getProperty(candidate, fieldName);
						Object contentRef = getProperty(refObj, fieldName);
						String score = null;
						if (c instanceof RelatedCompare) {
							if( content != null){
								content = getProperty(content, ID);
								contentRef = getProperty(contentRef, ID);
							}else{
								content = "---";
								contentRef = "---";
							}
						}
						if (c instanceof FuzzyCompare) {
							FuzzyCompare fc = (FuzzyCompare) c;
							score = m_df.format(fc.getLastScore());
						}
						mapCV.put("message", content);
						mapCV.put("messageRef", contentRef);
						mapCV.put("path", fieldName);
						mapCV.put("score", score);
						cvList.add(mapCV);
					}
				}
			} else {
				String scoreMsg = "";
				String score = null;
				Compare c = (Compare) fieldset.get(COMPARE);
				if (c instanceof FuzzyCompare) {
					FuzzyCompare fc = (FuzzyCompare) c;
					scoreMsg = "(" + m_df.format(fc.getLastScore()) + ")";
					score = m_df.format(fc.getLastScore());
				}
				List<String> fieldNames = (List) fieldset.get(FIELDS);
				for (String fieldName : fieldNames) {
					Map<String, Object> mapCV = new HashMap();
					Object content = getProperty(candidate, fieldName);
					Object contentRef = getProperty(refObj, fieldName);
					mapCV.put("message", content + scoreMsg);
					mapCV.put("messageRef", contentRef);
					mapCV.put("score", score);
					mapCV.put("path", fieldName);
					cvList.add(mapCV);
				}
			}
		}
		retMap.put("refid", refid);
		retMap.put("id", id);
		retMap.put("cvList", cvList);
		return retMap;
	}

	private SessionContext getSessionContext() {
		SessionContext sc = m_dataLayer.getSessionContext(m_sdesc);
		return sc;
	}

	private void init() throws Exception {
		m_prevFieldsets = m_fieldsets;
		m_prevFields = m_fields;
		m_fieldsets = null;
		m_fields = null;
		if (!isConfigUpdated()) {
			info("Config is not updated");
			m_fieldsets = m_prevFieldsets;
			m_fields = m_prevFields;
			return;
		}
		info("Config is updated");
		m_fieldsets = null;
		m_fields = null;
		train();
		List<Map> fields = getFields();
		List<Map> fieldsets = getFieldsets();
		for (Map field : fields) {
			Compare c = (Compare) field.get(COMPARE);
			c.init();
		}
		for (Map fieldset : fieldsets) {
			if (!isEmpty(fieldset.get(EXPRESSION))) {
				for (Map cmpUnit : (List<Map>) fieldset.get(COMPARE_UNITS)) {
					Compare c = (Compare) cmpUnit.get(COMPARE);
					c.init();
				}
			} else {
				Compare c = (Compare) fieldset.get(COMPARE);
				c.init();
			}
		}
	}

	private void reset() throws Exception {
		List<Map> fields = getFields();
		List<Map> fieldsets = getFieldsets();
		for (Map field : fields) {
			Compare c = (Compare) field.get(COMPARE);
			c.reset();
		}
		for (Map fieldset : fieldsets) {
			if (!isEmpty(fieldset.get(EXPRESSION))) {
				for (Map cmpUnit : (List<Map>) fieldset.get(COMPARE_UNITS)) {
					Compare c = (Compare) cmpUnit.get(COMPARE);
					c.reset();
				}
			} else {
				Compare c = (Compare) fieldset.get(COMPARE);
				c.reset();
			}
		}
	}

	private void train() {
		UserTransaction ut = m_nucleusService.getUserTransaction();
		SessionContext sc = getSessionContext();
		List corbus = new ArrayList();
		try {
			ut.begin();
			Class clazz = sc.getClass(getEntityName());
			Extent e = sc.getPM().getExtent(clazz, true);
			Query q = sc.getPM().newQuery(e);
			List results = (List) q.execute();
			Iterator iter = results.iterator();
			List<Map> fields = getFields();
			List<Map> fieldsets = getFieldsets();
			int count = 0;
			while (iter.hasNext()) {
				Object obj = iter.next();
				if ((count++ % 1000) == 1) {
					info("train.num:" + count);
				}
				for (Map field : fields) {
					if (field.get(COMPARE) instanceof FuzzyCompare) {
						FuzzyCompare fc = (FuzzyCompare) field.get(COMPARE);
						fc.addTrainValue((String) PropertyUtils.getProperty(obj, (String) field.get(NAME)));
					}
				}
				for (Map fieldset : fieldsets) {
					if (!isEmpty(fieldset.get(EXPRESSION))) {
						for (Map cmpUnit : (List<Map>) fieldset.get(COMPARE_UNITS)) {
							Compare c = (Compare) cmpUnit.get(COMPARE);
							if (c instanceof FuzzyCompare) {
								FuzzyCompare fc = (FuzzyCompare) c;
								List fs = (List) cmpUnit.get(FIELDS);
								String value = getFieldsetValue(obj, fs);
								fc.addTrainValue(value);
							}
						}
					} else {
						FuzzyCompare fc = (FuzzyCompare) fieldset.get(COMPARE);
						String value = getFieldsetValue(obj, fieldset);
						fc.addTrainValue(value);
					}
				}
			}
			ut.commit();
		} catch (Throwable e) {
			sc.handleException(ut, e);
		} finally {
			sc.handleFinally(ut);
		}
	}

	private String getFieldsetValue(Object obj, Object fieldset) throws Exception {
		List<String> fields = null;
		if (fieldset instanceof Map) {
			fields = (List) ((Map) fieldset).get(FIELDS);
		} else {
			fields = (List) fieldset;
		}
		String value = "";
		String blank = "";
		for (String field : fields) {
			Object v = PropertyUtils.getProperty(obj, field);
			if (v != null) {
				value += blank + v;
				blank = " ";
			}
		}
		return value;
	}

	private List<Map> _getFieldsets(String entityName) {
		try {
			Map props = m_settingService.getPropertiesForEntityView(m_sdesc.getNamespace(), GLOBAL_SETTINGS, entityName, "duplicate-check");
			List<Map> fieldsets = (List) props.get("fieldsets");
			if( fieldsets == null){
				fieldsets = new ArrayList();
			}
			for (Map fieldset : fieldsets) {
				String expr = (String) getString(fieldset.get(EXPRESSION), null);
				info("EXPRESSION:" + expr);
				List<Map> compareUnits = new ArrayList();
				if (expr != null) {
					List<Map> funcList = FunctionCallVisitor.getFunctionCalls(expr);
					info("funcList:" + funcList);
					for (Map func : funcList) {
						Map compareUnit = new HashMap();
						List args = (List) func.get(ARGS);
						String method = (String) func.get(METHOD);
						String check_type = FUZZY;
						String datatype = STRING;
						String signature = null;
						if (FUZZY.equals(method)) {
							List<String> fields = new ArrayList();
							if (args.get(0) instanceof String) {
								fields.add((String) args.get(0));
							} else {
								fields.addAll((List) args.get(0));
							}
							compareUnit.put(FIELDS, fields);
							double threshold = getDouble(args.get(1), 0.85);
							compareUnit.put(THRESHOLD, threshold);
							if( args.size()>2){
								compareUnit.put(INNERTHRESHOLD, getDouble(args.get(2),threshold));
							}else{
								compareUnit.put(INNERTHRESHOLD, threshold);
							}
							signature = args.get(0).toString() + args.get(1).toString();
						} else if (EQUAL.equals(method)) {
							check_type = EQUAL;
							if (!(args.get(0) instanceof String)) {
								throw new RuntimeException("Expression(" + expr + "):field not a string:" + method + "(" + args.get(0) + ")");
							}
							List<String> fields = new ArrayList();
							String field = (String) args.get(0);
							fields.add(field);
							compareUnit.put(FIELDS, fields);
							compareUnit.put(NAME, field);
							Map config = (Map) m_permittedFields.get(field);
							if (config == null) {
								throw new RuntimeException("Expression(" + expr + "):field not exists:" + method + "(" + field + ")");
							}
							datatype = (String) config.get(DATATYPE);
							signature = args.get(0).toString();
						}
						compareUnit.put(CHECKTYPE, check_type);
						compareUnit.put(DATATYPE, datatype);
						createComparator(compareUnit);
						info("signature:" + signature + "|");
						fieldset.put(signature, compareUnit.get(COMPARE));
						compareUnits.add(compareUnit);
					}
					fieldset.put(COMPARE_UNITS, compareUnits);
					GroovyEval ge = new GroovyEval(fieldset);
					for (String key : m_permittedFields.keySet()) {
						ge.setProperty(key, key);
					}
					fieldset.put(GROOVY_EVAL, ge);
				} else {
					Double outer = (Double) getDouble(fieldset.get(THRESHOLD), 0.85);
					Double inner = (Double) getDouble(fieldset.get(INNERTHRESHOLD), outer);
					fieldset.put(COMPARE, new FuzzyCompare((List) fieldset.get(FIELDS), inner, outer));
				}
				if (fieldset.get(FIELDS) == null) {
					fieldset.put(FIELDS, new ArrayList());
				}
			}
			info("fieldsets:" + fieldsets);
			return fieldsets;
		} catch (Exception e) {
			if (e instanceof groovy.lang.GroovyRuntimeException) {
				throw new RuntimeException(org.ms123.common.utils.Utils.formatGroovyException(e, "xxxx"));
			}
			if (e instanceof RuntimeException) {
				throw (RuntimeException)e;
			}
			e.printStackTrace();
			return new ArrayList();
		}
	}

	private List<Map> _getFields(String entityName) {
		try {
			List<Map> fields = m_settingService.getFieldsForEntityView(m_sdesc.getNamespace(), entityName, "duplicate-check");
			return setDefaults(fields);
		} catch (Exception e) {
			return new ArrayList();
		}
	}

	private boolean isConfigUpdated() throws Exception {
		if (m_prevFields == null || m_prevFieldsets == null) {
			return true;
		}
		List<Map> fields = getFields();
		if (m_prevFields.size() != fields.size()) {
			return true;
		}
		for (int i = 0; i < fields.size(); i++) {
			Map prevF = m_prevFields.get(i);
			Map currF = fields.get(i);
			if (!prevF.get(THRESHOLD).equals(currF.get(THRESHOLD))) {
				return true;
			}
			if (!prevF.get(INNERTHRESHOLD).equals(currF.get(INNERTHRESHOLD))) {
				return true;
			}
			if (!prevF.get(DATATYPE).equals(currF.get(DATATYPE))) {
				return true;
			}
			if (!prevF.get(CHECKTYPE).equals(currF.get(CHECKTYPE))) {
				return true;
			}
			if (!prevF.get(NAME).equals(currF.get(NAME))) {
				return true;
			}
		}
		List<Map> fieldsets = getFieldsets();
		if (fieldsets == null) {
			fieldsets = new ArrayList();
		}
		if (m_prevFieldsets.size() != fieldsets.size()) {
			info("step8");
			return true;
		}
		for (int i = 0; i < fieldsets.size(); i++) {
			Map prevFS = m_prevFieldsets.get(i);
			Map currFS = fieldsets.get(i);
			if (!checkNull(prevFS.get(THRESHOLD)).equals(checkNull(currFS.get(THRESHOLD)))) {
				return true;
			}
			if (!checkNull(prevFS.get(INNERTHRESHOLD)).equals(checkNull(currFS.get(INNERTHRESHOLD)))) {
				return true;
			}
			if (!checkNull(prevFS.get(EXPRESSION)).equals(checkNull(currFS.get(EXPRESSION)))) {
				return true;
			}
			List<String> pf = (List) prevFS.get(FIELDS);
			List<String> cf = (List) currFS.get(FIELDS);
			if (pf.size() != cf.size()) {
				return true;
			}
			for (int j = 0; j < pf.size(); j++) {
				if (!pf.get(j).equals(cf.get(j))) {
					return true;
				}
			}
		}
		return false;
	}

	private Object checkNull(Object o) {
		if (o == null)
			return m_null;
		return o;
	}

	private List<Map> setDefaults(List<Map> fields) {
		Map<String, String> fieldMap = new HashMap();
		for (Map<String, Object> compareUnit : fields) {
			if (compareUnit.get(CHECKTYPE).equals("both")) {
				compareUnit.put(CHECKTYPE, "fuzzy");
			}
			compareUnit.put(DATATYPE, getString(compareUnit.get(DATATYPE), "string"));
			compareUnit.put(CHECKTYPE, getString(compareUnit.get(CHECKTYPE), "equal"));
			compareUnit.put(THRESHOLD, getDouble(compareUnit.get(THRESHOLD), 0.85));
			compareUnit.put(INNERTHRESHOLD, getDouble(compareUnit.get(INNERTHRESHOLD), (Double) compareUnit.get(THRESHOLD)));
			createComparator(compareUnit);
		}
		info("Fields:" + fields);
		return fields;
	}

	private void createComparator(Map compareUnit) {
		String datatype = (String) compareUnit.get(DATATYPE);
		String checktype = (String) compareUnit.get(CHECKTYPE);
		if (datatype.equals(STRING) && checktype.equals(FUZZY)) {
			Double outer = (Double) compareUnit.get(THRESHOLD);
			Double inner = (Double) compareUnit.get(INNERTHRESHOLD);
			if (compareUnit.get(FIELDS) != null) {
				compareUnit.put(COMPARE, new FuzzyCompare((List) compareUnit.get(FIELDS), inner, outer));
			} else {
				compareUnit.put(COMPARE, new FuzzyCompare((String) compareUnit.get(NAME), inner, outer));
			}
		}
		if (datatype.startsWith("related") && checktype.equals(EQUAL)) {
			compareUnit.put(COMPARE, new RelatedCompare((String) compareUnit.get(NAME)));
		} else if (checktype.equals(EQUAL)) {
			compareUnit.put(COMPARE, new EqualCompare((String) compareUnit.get(NAME)));
		}
	}

	private void setRelatedObjects(SessionContext sc, Map<String, Object> data) throws Exception {
		for (String field : data.keySet()) {
			Map<String, String> config = (Map) m_permittedFields.get(field);
			if (config == null) {
				continue;
			}
			String datatype = config.get(DATATYPE);
			if (datatype.startsWith("related")) {
				Object o = data.get(field);
				if (o == null){
					continue;
				}
				String id = null;
				if (o instanceof String) {
					id = (String)o;
					if( ((String)o).indexOf("/")!=-1){
						id = extractId((String)o);
					}
				}
				if (o instanceof Map) {
					id = (String)((Map) o).get(ID);
				}
				if( id != null){
					Object r = sc.getObjectById(sc.getClass(getEntityName(datatype)), id);
					data.put(field, r);
				}
			}
		}
	}

	private String getEntityName(String dt) {
		String className = dt.split("/")[1];
		int dot = className.lastIndexOf(".");
		return className.substring(dot + 1);
	}

	private boolean isEmpty(Object o) {
		if (o instanceof String) {
			String s = (String) o;
			return (s == null || "".equals(s.trim()));
		}
		return o == null;
	}
	private String extractId(String s){
		for( String part : s.split("/")){
			if( isaId(part)) return part;
		}
		return null;
	}

	private boolean isaId(String s){
		if( s==null || s.length() != 32) return false;
		boolean isNumeric = s.matches("\\p{XDigit}+");
		info("isAid:"+s+" -> "+isNumeric);
		return isNumeric;
	}

	private String getString(Object value, String def) {
		if (value != null && (value instanceof String) && ((String) value).length() > 0) {
			return (String) value;
		}
		return def;
	}

	private Double getDouble(Object value, Double def) {
		try {
			if (value instanceof BigDecimal) {
				return ((BigDecimal) value).doubleValue();
			}
			if (value instanceof Double) {
				return (Double) value;
			}
			if (value instanceof Number) {
				return ((Number) value).doubleValue();
			}
			if (value instanceof String) {
				return Double.parseDouble((String) value);
			}
		} catch (Exception e) {
		}
		return def;
	}

	protected void debug(String message) {
		m_logger.debug(message);
		System.out.println(message);
	}

	protected void info(String message) {
		m_logger.info(message);
		System.out.println(message);
	}

	private static final Logger m_logger = LoggerFactory.getLogger(QualityBatch.class);
}
