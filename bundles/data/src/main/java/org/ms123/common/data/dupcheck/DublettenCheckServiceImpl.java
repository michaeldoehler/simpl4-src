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
import org.ms123.common.store.StoreDesc;
import org.ms123.common.setting.api.SettingService;
import aQute.bnd.annotation.metatype.*;
import aQute.bnd.annotation.component.*;

@SuppressWarnings("unchecked")
@Component(enabled = true, configurationPolicy = ConfigurationPolicy.optional, immediate = true, properties = { "impl=default" })
public class DublettenCheckServiceImpl implements DublettenCheckService {

	private static final Logger m_logger = LoggerFactory.getLogger(DublettenCheckServiceImpl.class);

	private final String ENTITY = "entity";

	protected Inflector m_inflector = Inflector.getInstance();

	private NucleusService m_nucleusService;

	private SettingService m_settingService;

	private DoubleMetaphone m_metaphone = new DoubleMetaphone();

	private ColognePhonetic m_cologn = new ColognePhonetic();

	private JaroWinkler m_jaro = new JaroWinkler();

	// OsgiActivate 
	public void activate() {
		System.out.println("DublettenCheckServiceImpl.activate");
		m_metaphone.setMaxCodeLen(128);
	}

	public DupCheckContext getContext(){
		return new DupCheckContext();
	}
	public DupCheckContext getContext(SessionContext sc, String entityName){
		return new DupCheckContext(sc, entityName);
	}
	public DupCheckContext getContext(SessionContext sc, String entityName, String idField){
		return new DupCheckContext(sc, entityName,idField);
	}

	public Map dublettenCheck(DupCheckContext dcc, Object dataObject) {
		List<Map> filterFields = dcc.getDupFields();
		if (filterFields.size() == 0) {
			return new HashMap();
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
		Map<String, String[]> fieldAlgoMap = dcc.getFieldAlgoMap();
		Map retMap = new HashMap();
		try {
			List compareList = getCompareList( dcc );
			for (Map dataMap : dataList) {
				_dublettenCheckOne(dcc, compareList, dataMap);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
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


	private List getCompareList(DupCheckContext dcc) throws Exception{
		SessionContext sc = dcc.getSessionContext();
		String entityName = dcc.getEntityName();
		StoreDesc sdesc = sc.getStoreDesc();
		String className = m_inflector.getClassName(entityName);
		String plural = m_inflector.pluralize(entityName).toLowerCase();
		String projection = "";
		String komma = "";
		Map<String,String[]> fieldAlgoMap = dcc.getFieldAlgoMap();
		for (String field : fieldAlgoMap.keySet()) {
			projection += komma + field;
			komma = ",";
		}
		String sql = "Select distinct " + projection + "," + sc.getPrimaryKey() + " from " + className + " " + plural;
		System.out.println("dublettenCheck.sql:" + sql);
		Query q = sc.getPM().newQuery("javax.jdo.query.JPQL", sql);
		q.declareImports(sdesc.getImports());
		List compareList = (List) q.execute();
		return compareList;
	}

	public void dublettenCheckOne(DupCheckContext dcc, List<Map> compareList, Map dataMap) {
		Map<String, String[]> fieldAlgoMap = dcc.getFieldAlgoMap();
		if( compareList.size() == 0){
			List okList = dcc.getOkList();
			okList.add(dataMap);
			return;
		}
		Map<String,String>  inputEncCache = dcc.getInputEncCache();
		String idField = dcc.getIdField();
		Iterator itr = compareList.iterator();
		boolean gdup = false;
		while (itr.hasNext()) {
			Map row = (Map) itr.next();
			int i = 0;
			boolean isDup = false;
			for (String fieldName : fieldAlgoMap.keySet()) {
				String compareValue = (String)row.get(fieldName);
				String[] algos = fieldAlgoMap.get(fieldName);
				isDup = compare(dcc, (String) dataMap.get(fieldName), compareValue,algos);
				if (isDup == false) {
					break;
				}
				i++;
			}
			if (isDup == true ) {
				gdup = true;
				List dupList = dcc.getDupList();
				dataMap.put("dupid", row.get(idField));
				dupList.add(dataMap);
				if( idField != null){
					Map<String,List> dupMap = dcc.getDupMap();
					String id = (String)row.get(idField);
					List idList = dupMap.get(id);	
					if( idList == null){
						idList = new ArrayList();
					}
					idList.add( dataMap.get(idField));
				}
			}else{
			}
		}
		if( !gdup ){
			List okList = dcc.getOkList();
			okList.add(dataMap);
		}
	}

	private void _dublettenCheckOne(DupCheckContext dcc, List compareList, Map dataMap) {
		Map<String, String[]> fieldAlgoMap = dcc.getFieldAlgoMap();
		List<Map> cvList = dcc.getCVList();
		List idHitList = dcc.getIdHitList();
		Map<String,String>  inputEncCache = dcc.getInputEncCache();
		String komma = "";
		Iterator itr = compareList.iterator();
		while (itr.hasNext()) {
			Object[] row = (Object[]) itr.next();
			int i = 0;
			boolean isDup = false;
			for (String fieldName : fieldAlgoMap.keySet()) {
				String[] algos = fieldAlgoMap.get(fieldName);
				isDup = compare(dcc,(String) dataMap.get(fieldName), (String) row[i], algos);
				if (isDup == false) {
					break;
				}
				i++;
			}
			if (isDup == true) {
				dataMap.put("_duplicated_", "ja");
				dataMap.put("_duplicated_id_", row[row.length - 1]);
				System.out.print("\t--->>>Treffer:");
				komma = "";
				for (i = 0; i < row.length; i++) {
					System.out.print(komma + row[i]);
					komma = "\t,";
				}
				System.out.println("");
				i = 0;
				for (String field : fieldAlgoMap.keySet()) {
					Map<String, String> mapCV = new HashMap();
					mapCV.put("message", "Duplicated:" + row[i]);
					mapCV.put("path", field);
					cvList.add(mapCV);
					i++;
				}
				idHitList.add(row[row.length - 1]);
			}
		}
	}

	public boolean compare(DupCheckContext dcc, String valueInput, String valueCompareTo,String[] algos) {
		Map<String,String>  inputEncCache = dcc.getInputEncCache();
		if ((valueInput == null || valueInput.equals("")) && (valueCompareTo == null || valueCompareTo.equals(""))) {
			return true;
		}
		if (valueCompareTo == null || valueCompareTo.equals("")) {
			return false;
		}
		if (valueInput == null || valueInput.equals("")) {
			return false;
		}
		boolean ret = false;
		//First is datatype
		for (int i = 1; i < algos.length; i++) {
			String algo = algos[i];
			if ("metaphone".equals(algo)) {
				String encInput = getInputValueEnc(valueInput, algo, inputEncCache);
				String encCompareTo = m_metaphone.encode(valueCompareTo);
				boolean ret1 = encCompareTo.equals(encInput);
				String encInputSorted = getInputValueEnc(sortString(valueInput), algo, inputEncCache);
				String encCompareToSorted = m_metaphone.encode(sortString(valueCompareTo));
				if (encCompareToSorted == null) {
					encCompareToSorted = "";
				}
				boolean ret2 = encCompareToSorted.equals(encInputSorted);
				ret = ret1 || ret2;
				if (ret) {
					//System.out.println("METAPHONE.valueInput:" + valueInput + ",valueCompareTo:" + valueCompareTo + " -> " + ret);
				}
			} else if ("jaro".equals(algo)) {
				double threshold = new Double(algos[++i]).doubleValue();
				double score = Math.max(m_jaro.score(sortString(valueInput.toLowerCase()), sortString(valueCompareTo.toLowerCase())), m_jaro.score(valueInput.toLowerCase(), valueCompareTo.toLowerCase()));
				ret = score > threshold;
				if (score > threshold) {
					//System.out.println("JARO.valueInp:" + valueInp + ",valueCompareTo:" + valueCompareTo + " -> " + score);
				}
			} else if ("notequal".equals(algo)) {
				ret =  !valueInput.equals( valueCompareTo);
			} else if ("equal".equals(algo)) {
				ret = valueInput.equals( valueCompareTo);
			}
			if (ret == true) {
				return true;
			}
		}
		return false;
	}

	private String getInputValueEnc(String inp, String algo, Map<String, String> inputEncCache) {
		if (inputEncCache.get(inp + algo) != null) {
			return inputEncCache.get(inp + algo);
		}
		String enc = null;
		if ("metaphone".equals(algo)) {
			enc = m_metaphone.encode(inp);
		}
		inputEncCache.put(inp + algo, enc);
		return enc;
	}

	private static int indexOfAny(String str, char[] searchChars) {
		if (str == null) {
			return -1;
		}
		for (int i = 0; i < str.length(); i++) {
			char ch = str.charAt(i);
			for (char searchChar : searchChars) {
				if (searchChar == ch) {
					return i;
				}
			}
		}
		return -1;
	}

	private char[] a = { ' ', '-', '+', ',' };

	private Map<String, String> m_sortBuffer = new HashMap();

	private String sortString(String s) {
		if (indexOfAny(s, a) == -1) {
			return s;
		}
		String ret = m_sortBuffer.get(s);
		if (ret != null) {
			return ret;
		}
		StringTokenizer st = new StringTokenizer(s, " +-,");
		String[] sp = new String[st.countTokens()];
		int i = 0;
		while (st.hasMoreTokens()) {
			sp[i++] = st.nextToken();
		}
		Arrays.sort(sp);
		ret = arrayToString(sp, " ");
		m_sortBuffer.put(s, ret);
		return ret;
	}

	private static String arrayToString(String[] a, String separator) {
		StringBuffer result = new StringBuffer();
		if (a.length > 0) {
			result.append(a[0]);
			for (int i = 1; i < a.length; i++) {
				result.append(separator);
				result.append(a[i]);
			}
		}
		return result.toString();
	}

	private String getString(Map m, String key, String _def) {
		try {
			if (m.get(key) != null) {
				return (String) m.get(key);
			}
		} catch (Exception e) {
		}
		return _def;
	}

	private String checkNull(Map m, String key, String msg) {
		if (m.get(key) != null) {
			return (String) m.get(key);
		}
		throw new RuntimeException(msg);
	}

	/************************************ C O N F I G ********************************************************/
	@Reference(dynamic = true)
	public void setNucleusService(NucleusService paramNucleusService) {
		this.m_nucleusService = paramNucleusService;
		System.out.println("DublettenCheckServiceImpl.setNucleusService:" + paramNucleusService);
	}

	@Reference(dynamic = true, optional = true)
	public void setSettingService(SettingService paramSettingService) {
		this.m_settingService = paramSettingService;
		System.out.println("DublettenCheckServiceImpl.setSettingService:" + paramSettingService);
	}
}
