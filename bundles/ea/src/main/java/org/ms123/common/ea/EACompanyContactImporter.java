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
package org.ms123.common.ea;

import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.jdo.Extent;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import org.apache.commons.beanutils.PropertyUtils;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import com.Ostermiller.util.*;
import org.ms123.common.data.api.DataLayer;
import org.ms123.common.data.api.SessionContext;
import org.ms123.common.entity.api.EntityService;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.libhelper.Bean2Map;
import org.ms123.common.libhelper.Inflector;
import org.ms123.common.utils.UtilsService;
import org.ms123.common.nucleus.api.NucleusService;
import org.ms123.common.data.api.LuceneService;
import org.ms123.common.data.api.LuceneSession;
import org.ms123.common.data.dupcheck.DublettenCheckService;
import org.ms123.common.data.dupcheck.DupCheckContext;
import org.apache.commons.beanutils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.jdo.PersistenceManager;
import javax.transaction.RollbackException;
import javax.transaction.UserTransaction;
import javax.transaction.Status;
import javax.jdo.Query;
import javax.jdo.Transaction;
import static org.apache.commons.lang.StringUtils.trim;
import static org.apache.commons.lang.StringUtils.leftPad;
import static org.ms123.common.entity.api.Constants.STATE_OK;

/** EACompanyContactImporter implementation
 */
@SuppressWarnings("unchecked")
public class EACompanyContactImporter implements Constants {

	private static final Logger m_logger = LoggerFactory.getLogger(BaseEAServiceImpl.class);

	protected DataLayer m_dataLayer;

	protected NucleusService m_nucleusService;

	protected LuceneService m_luceneService;

	protected String m_basedir;

	protected StoreDesc m_storeDesc;
	protected Map m_companyMapping;
	protected Map m_contactMapping;

	protected Map<String, Map> m_mappings = new HashMap();

	protected JSONDeserializer m_ds = new JSONDeserializer();

	private SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");

	private SessionContext m_sessionContext;

	private Class m_contactClazz;

	public EACompanyContactImporter(DataLayer dl, NucleusService ns, DublettenCheckService dcs, String storeId, String basedir, Map cyf, Map cof) throws Exception{
		m_dataLayer = dl;
		m_nucleusService = ns;
		m_storeDesc = StoreDesc.get(storeId);
		m_sessionContext = m_dataLayer.getSessionContext(m_storeDesc);
		m_basedir = basedir;
		m_contactClazz = m_nucleusService.getClass(m_storeDesc, "contact");
		m_contactMapping = initContact();
		m_companyMapping = initCompany();
		doImport();
	}

	private void doImport() throws Exception {
		LabeledCSVParser lp = new LabeledCSVParser(new ExcelCSVParser(new FileInputStream(new File(m_basedir, "ea.csv"))));
		int status;
		PersistenceManager pm = m_nucleusService.getPersistenceManagerFactory(m_storeDesc).getPersistenceManager();
		UserTransaction ut = m_nucleusService.getUserTransaction();
		int num=0;
		Object company =null;
		String lastCompanyId = null;
		while (lp.getLine() != null) {
			String type = lp.getValueByLabel("type");
			String companyId = lp.getValueByLabel("companyId");
			if( type.startsWith("nok")) continue;
			if (ut.getStatus() != Status.STATUS_ACTIVE) {
				ut.begin();
			}
			String s[] = getStateAndEntity(type);
			Object obj = populate(lp,s[0], s[1]);

			if( !isEmpty(companyId)){	
				if( !companyId.equals(lastCompanyId) ){
					company = obj;
					lastCompanyId = companyId;
				}
				if( s[0].equals("contact")){
					Set cl = (Set) PropertyUtils.getProperty(company, "contact_list");
					if (cl == null) {
						cl = new HashSet();
						PropertyUtils.setProperty(company, "contact_list", cl);
					}
					cl.add(obj);
				}
			}
			pm.makePersistent(obj);
			if ((num % 1000) == 1) {
				System.out.println(num + ":\t" + new Date().getTime());
				ut.commit();
			}
			num++;
		}
		if (ut.getStatus() == Status.STATUS_ACTIVE) {
			ut.commit();
		}
	}

	private String[] getStateAndEntity(String type){
		int ul = type.indexOf("_");
		String entity=null;
		String state=STATE_OK;
		if( ul == -1 ){
			entity = type;
		}else{
			entity = type.substring(0,ul);	
			state = type.substring(ul+1);	
			if( "fc".equals(state) || "single".equals(state)){
				state = STATE_OK;
			}
		}
		String ret[] = new String[2];
		ret[0]=entity;
		ret[1]=state;
		return ret;
	}
	private Object populate(LabeledCSVParser lp, String entity,String state) throws Exception {
		Object obj = m_nucleusService.getClass(m_storeDesc, entity).newInstance();
		Map mapping = entity.equals("company") ? m_companyMapping : m_contactMapping;
		Iterator it = mapping.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			String[] m1 = (String[]) mapping.get(key);
			String field = m1[0];
			String dtype = m1[1];
			String val = lp.getValueByLabel(key);
			if( val != null){
				val = val.trim();
			}else{
				System.err.println("\tkeynull:"+key);
			}
			if (dtype.equals("date")) {
				Date d = getDate(val);
				if (d != null) {
					BeanUtils.setProperty(obj, field, d);
				}
			} else if (dtype.equals("boolean")) {
				Boolean b = false;
				if ("J".equals(val)) {
					b = true;
				}
				BeanUtils.setProperty(obj, field, b);
			} else {
				if (val != null) {
					if ("plz".equals(field)) {
						val = leftPad(val, 5, '0');
					}
					BeanUtils.setProperty(obj, field, trim(val));
				}else{
					BeanUtils.setProperty(obj, field, "");
				}
			}
		}
		if( entity.equals("contact") ){
			BeanUtils.setProperty(obj, "country", "DEU");
		}
		BeanUtils.setProperty(obj, "_state", state);
		return obj;
	}


	private void persistObject(Object o) throws Exception {
		UserTransaction ut = m_nucleusService.getUserTransaction();
		PersistenceManager pm = m_sessionContext.getPM();
		try {
			ut.begin();
			pm.makePersistent(o);
			//m_sessionContext.makePersistent(o);
			ut.commit();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}
	}

	
	private boolean isEmpty(String s) {
		return (s == null || "".equals(s.trim()));
	}


	private Date getDate(String val) {
		Date ret = null;
		try {
			ret = dateFormat.parse(val);
		} catch (Exception e) {
			ret = null;
		}
		return ret;
	}
	public Map initCompany() {
		Map<String, String[]> mMap = new HashMap<String, String[]>();
		mMap.put("number", new String[] { "number", "string" });
		mMap.put("main_address_number", new String[] { "main_address_number", "string" });
		mMap.put("origin", new String[] { "origin", "string" });
		mMap.put("shortname_company", new String[] { "shortname_company", "string" });
		mMap.put("company1", new String[] { "company1", "string" });
		mMap.put("company2", new String[] { "company2", "string" });
		mMap.put("company3", new String[] { "company3", "string" });
		mMap.put("street", new String[] { "street", "string" });
		mMap.put("lkz", new String[] { "lkz", "string" });
		mMap.put("plz", new String[] { "plz", "string" });
		mMap.put("ortname", new String[] { "ortname", "string" });
		mMap.put("gemeindekennziffer", new String[] { "gemeindekennziffer", "string" });
		mMap.put("aplha_number", new String[] { "aplha_number", "string" });
		mMap.put("address_status", new String[] { "address_status", "string" });
		mMap.put("address_status_since", new String[] { "address_status_since", "date" });
		mMap.put("remark", new String[] { "remark", "string" });
		mMap.put("_created_at", new String[] { "_created_at", "date" });
		mMap.put("_created_by", new String[] { "_created_by", "string" });
		mMap.put("_updated_at", new String[] { "_updated_at", "date" });
		mMap.put("_updated_by", new String[] { "_updated_by", "string" });
		return mMap;
	}
	public Map initContact() {
		Map<String, String[]> mMap = new HashMap<String, String[]>();
		mMap.put("number", new String[] { "number", "string" });
		mMap.put("main_address_number", new String[] { "main_address_number", "string" });
		mMap.put("origin", new String[] { "origin", "string" });
		mMap.put("shortname_company", new String[] { "shortname_company", "string" });
		mMap.put("shortname_person", new String[] { "shortname_person", "string" });
		mMap.put("company1", new String[] { "company1", "string" });
		mMap.put("company2", new String[] { "company2", "string" });
		mMap.put("company3", new String[] { "company3", "string" });
		mMap.put("birthdate", new String[] { "birthdate", "date" });
		mMap.put("job_key", new String[] { "job_key", "string" });
		mMap.put("sex", new String[] { "sex", "string" });
		mMap.put("salutation_key", new String[] { "salutation_key", "string" });
		mMap.put("salutation_name", new String[] { "salutation_name", "string" });
		mMap.put("title_key", new String[] { "title_key", "string" });
		mMap.put("givenname", new String[] { "givenname", "string" });
		mMap.put("name1", new String[] { "name1", "string" });
		mMap.put("street", new String[] { "street", "string" });
		mMap.put("lkz", new String[] { "lkz", "string" });
		mMap.put("plz", new String[] { "plz", "string" });
		mMap.put("ortname", new String[] { "ortname", "string" });
		mMap.put("gemeindekennziffer", new String[] { "gemeindekennziffer", "string" });
		mMap.put("aplha_number", new String[] { "aplha_number", "string" });
		mMap.put("letter_salutation", new String[] { "letter_salutation", "string" });
		mMap.put("robinson", new String[] { "robinson", "string" });
		mMap.put("address_status", new String[] { "address_status", "string" });
		mMap.put("address_status_since", new String[] { "address_status_since", "date" });
		mMap.put("remark", new String[] { "remark", "string" });
		mMap.put("_created_at", new String[] { "_created_at", "date" });
		mMap.put("_created_by", new String[] { "_created_by", "string" });
		mMap.put("_updated_at", new String[] { "_updated_at", "date" });
		mMap.put("_updated_by", new String[] { "_updated_by", "string" });
		return mMap;
	}


}
