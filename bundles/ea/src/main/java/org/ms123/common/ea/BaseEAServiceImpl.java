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
import javax.transaction.UserTransaction;
import javax.xml.transform.stream.StreamSource;
import net.sf.sojo.common.*;
import net.sf.sojo.core.*;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.tika.Tika;
import org.milyn.container.*;
import org.milyn.payload.*;
import org.milyn.Smooks;
import org.milyn.SmooksFactory;
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
import org.osgi.framework.BundleContext;
import org.ms123.common.nucleus.api.NucleusService;
import org.ms123.common.data.api.LuceneService;
import org.ms123.common.data.api.LuceneSession;
import org.ms123.common.data.dupcheck.DublettenCheckService;
import org.ms123.common.data.dupcheck.DupCheckContext;
import org.ms123.common.reporting.ReportingService;
import org.ms123.common.git.GitService;
import org.apache.commons.beanutils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.jdo.PersistenceManager;
import javax.transaction.RollbackException;
import javax.transaction.UserTransaction;
import javax.transaction.Status;
import javax.jdo.Query;
import javax.jdo.Transaction;
import static org.apache.commons.io.FileUtils.readFileToString;

/** BaseEAService implementation
 */
@SuppressWarnings("unchecked")
public class BaseEAServiceImpl implements Constants {

	private static final Logger m_logger = LoggerFactory.getLogger(BaseEAServiceImpl.class);

	protected DataLayer m_dataLayer;

	protected NucleusService m_nucleusService;
	protected ReportingService m_reportingService;
	protected GitService m_gitService;

	protected LuceneService m_luceneService;
	protected DublettenCheckService m_dublettenCheckService;

	protected JSONDeserializer m_ds = new JSONDeserializer();

	private SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");

	public BaseEAServiceImpl() {
	}

	protected void _initialerImport(String storeId, String entity, String basedir) throws Exception{
		if ("contact".equals(entity)) {
			new EACompanyContactImporter( m_dataLayer, m_nucleusService, m_dublettenCheckService, storeId, basedir, initCompany(), initContacts());
		} else if ("activity".equals(entity)) {
			importActivities(storeId, basedir);
		} else if ("communication".equals(entity)) {
			importCommunications(storeId, basedir);
		} else if ("team".equals(entity)) {
			importTeams(storeId, basedir);
		} else if ("zipcode".equals(entity)) {
			importZipcodes(storeId, basedir);
		} else {
			throw new RuntimeException("EAServiceImpl.cannot import:" + entity);
		}
	}

	private void printList(String msg, List<Map> l, Map<String,String[]> fields){
		System.out.println(msg);

		for( Map m : l){
			for( String field : fields.keySet()){
				System.out.print("\t"+m.get(field));
			}
			System.out.println();
		}
	}

	private Map importActivities(String storeId, String basedir) throws Exception{
		String json = readFileToString(new File(basedir, "idmap.map"));
		Map<String,String> idmap = (Map)m_ds.deserialize(json);
		Calendar high_cal = Calendar.getInstance();
		high_cal.set(Calendar.YEAR, 2050);
		high_cal.set(Calendar.MONTH, 11);
		high_cal.set(Calendar.DAY_OF_MONTH, 31);

		Calendar low_cal = Calendar.getInstance();
		low_cal.set(Calendar.YEAR, 2012);
		low_cal.set(Calendar.MONTH, 11);
		low_cal.set(Calendar.DAY_OF_MONTH, 12);
		StoreDesc sdesc = StoreDesc.get(storeId);
		PersistenceManager pm = m_nucleusService.getPersistenceManagerFactory(sdesc).getPersistenceManager();
		UserTransaction ut = m_nucleusService.getUserTransaction();
		Map mapping = initActivities();
		try {
			LabeledCSVParser lp = new LabeledCSVParser(new ExcelCSVParser(new FileInputStream(new File(basedir,"Kontakte.csv"))));
			System.out.println("Persisting activities");
			int num = 0;
			Class _contact = m_nucleusService.getClass(sdesc, "Contact");
			Class _teamIntern = m_nucleusService.getClass(sdesc, "Teamintern");
			while (lp.getLine() != null) {
				String nummer = lp.getValueByLabel("Nummer");
				String merkmal = lp.getValueByLabel("Merkmal");
				Object c = getObject(pm, _contact, nummer);
				if (c == null) {
					continue;
				}
				if (ut.getStatus() != Status.STATUS_ACTIVE) {
					ut.begin();
				}
				String teamid = idmap.get(merkmal);
				Object activity = m_nucleusService.getClass(sdesc, "Activity").newInstance();
				if( teamid != null){
					Object team = m_nucleusService.getClass(sdesc, "Team").newInstance();
					Object teamintern = getTeamintern(pm, _teamIntern, teamid);
					PropertyUtils.setProperty(team, "teamintern", teamintern);
					PropertyUtils.setProperty(team, "teamid", teamid);
					PropertyUtils.setProperty(team, "description",PropertyUtils.getProperty(teamintern, "description"));
					PropertyUtils.setProperty(team, "validFrom", low_cal.getTime());
					PropertyUtils.setProperty(team, "validTo", high_cal.getTime());
					PropertyUtils.setProperty(team, "disabled", false);
					Collection l = (Collection) PropertyUtils.getProperty(activity, "_team_list");
					if( l == null){
						l = new HashSet();
						PropertyUtils.setProperty(activity, "_team_list", l);
					}
					l.add(team);
				}
				Iterator it = mapping.keySet().iterator();
				while (it.hasNext()) {
					String key = (String) it.next();
					if (key.equals("traits")) {
						continue;
					}
					String[] m1 = (String[]) mapping.get(key);
					String field = m1[0];
					String type = m1[1];
					String val = lp.getValueByLabel(key).trim();
					if (type.equals("date")) {
						Date d = getDate(val);
						if (d != null) {
							BeanUtils.setProperty(activity, field, d);
						}
					} else if (type.equals("boolean")) {
						Boolean b = false;
						if ("J".equals(val)) {
							b = true;
						}
						BeanUtils.setProperty(activity, field, b);
					} else {
						if (val != null) {
							BeanUtils.setProperty(activity, field, val);
						}
					}
				}
				Collection l = (Collection) PropertyUtils.getProperty(c, "activity_list");
				l.add(activity);
				PropertyUtils.setProperty(activity, "contact", c);
				pm.makePersistent(activity);
				//LuceneSession luceneSession = m_luceneService.createSession(sdesc);
				//m_luceneService.addToIndex(luceneSession, activity);
				//m_luceneService.commit(luceneSession);
				if ((num % 1000) == 1) {
					System.out.println(num + ":\t" + new Date().getTime());
					ut.commit();
				}
				num++;
			}
			if (ut.getStatus() == Status.STATUS_ACTIVE) {
				ut.commit();
			}
			System.out.println("Contact and Book have been persisted");
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} finally {
			pm.close();
		}
		return null;
	}
	private Map importTeams(String storeId, String basedir) throws Exception{
		Calendar high_cal = Calendar.getInstance();
		high_cal.set(Calendar.YEAR, 2050);
		high_cal.set(Calendar.MONTH, 11);
		high_cal.set(Calendar.DAY_OF_MONTH, 31);

		Calendar low_cal = Calendar.getInstance();
		low_cal.set(Calendar.YEAR, 2012);
		low_cal.set(Calendar.MONTH, 11);
		low_cal.set(Calendar.DAY_OF_MONTH, 12);

		String json = readFileToString(new File(basedir, "idmap.map"));
		Map<String,String> idmap = (Map)m_ds.deserialize(json);
		StoreDesc sdesc = StoreDesc.get(storeId);
		PersistenceManager pm = m_nucleusService.getPersistenceManagerFactory(sdesc).getPersistenceManager();
		UserTransaction ut = m_nucleusService.getUserTransaction();
		try {
			LabeledCSVParser lp = new LabeledCSVParser(new ExcelCSVParser(new FileInputStream(new File(basedir,"Merkmale.csv"))));
			System.out.println("Persisting teams");
			int num = 0;
			Class _contact = m_nucleusService.getClass(sdesc, "Contact");
			Class _company = m_nucleusService.getClass(sdesc, "Company");
			Class _teamIntern = m_nucleusService.getClass(sdesc, "Teamintern");
			while (lp.getLine() != null) {
				String nummer = lp.getValueByLabel("Nummer");
				String merkmal = lp.getValueByLabel("Merkmal");
				String beginn = lp.getValueByLabel("Beginn");
				String ende = lp.getValueByLabel("Ende");
				String status = lp.getValueByLabel("Status");
				String teamid = idmap.get(merkmal);
				if( teamid == null){
					System.out.println("Teamid not found:"+merkmal);
					continue;
				}
				Object c = getObject(pm, _contact, _company, nummer);
				if (c == null) {
					System.out.println("No contact/company:"+nummer);
					continue;
				}
				if (ut.getStatus() != Status.STATUS_ACTIVE) {
					ut.begin();
				}
				Object team = m_nucleusService.getClass(sdesc, "Team").newInstance();
				Object teamintern = getTeamintern(pm, _teamIntern, teamid);
				PropertyUtils.setProperty(team, "teamintern", teamintern);
				PropertyUtils.setProperty(team, "teamid", teamid);
				PropertyUtils.setProperty(team, "description",PropertyUtils.getProperty(teamintern, "description"));

				Boolean active = isActive(status);
				Date validFrom = getTeamDate(beginn);
				Date validTo = getTeamDate(ende);
				if( active!= null && validFrom != null && validTo != null){
					PropertyUtils.setProperty(team, "validFrom", validFrom);
					PropertyUtils.setProperty(team, "validTo", validTo);
					PropertyUtils.setProperty(team, "disabled", !active);
				}
				if( active== null && validFrom == null && validTo == null){
					PropertyUtils.setProperty(team, "validFrom", low_cal.getTime());
					PropertyUtils.setProperty(team, "validTo", high_cal.getTime());
					PropertyUtils.setProperty(team, "disabled", false);
				}
				if( active!=null && validFrom != null && validTo == null){
					PropertyUtils.setProperty(team, "validFrom", validFrom);
					PropertyUtils.setProperty(team, "validTo", high_cal.getTime());
					PropertyUtils.setProperty(team, "disabled", !active);
				}
				PropertyUtils.setProperty(team, "property1", lp.getValueByLabel("Nutzer"));
				PropertyUtils.setProperty(team, "property2", lp.getValueByLabel("Passwort"));
				
				Collection l = (Collection) PropertyUtils.getProperty(c, "_team_list");
				if( l == null){
					l = new HashSet();
					PropertyUtils.setProperty(c, "_team_list", l);
				}
				l.add(team);
				pm.makePersistent(team);
				if ((num % 1000) == 1) {
					System.out.println(num + ":\t" + new Date().getTime());
					ut.commit();
				}
				num++;
			}
			if (ut.getStatus() == Status.STATUS_ACTIVE) {
				ut.commit();
			}
			System.out.println("Contact and Book have been persisted");
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} finally {
			pm.close();
		}
		return null;
	}

	private Map importCommunications(String storeId, String basedir) {
		StoreDesc sdesc = StoreDesc.get(storeId);
		PersistenceManager pm = m_nucleusService.getPersistenceManagerFactory(sdesc).getPersistenceManager();
		UserTransaction ut = m_nucleusService.getUserTransaction();
		Map mapping = initCommunication();
		try {
			LabeledCSVParser lp = new LabeledCSVParser(new ExcelCSVParser(new FileInputStream(new File(basedir,"Kommunikation.csv"))));
			System.out.println("Persisting communication");
			int num = 0;
			Class _contact = m_nucleusService.getClass(sdesc, "Contact");
			Class _company = m_nucleusService.getClass(sdesc, "Company");
			while (lp.getLine() != null) {
				String nummer = lp.getValueByLabel("Nummer");
				Object c = getObject(pm, _contact, nummer);
				if (c == null) {
					c = getObject(pm, _company, nummer);
					if (c == null) {
						continue;
					}
				}
				if (ut.getStatus() != Status.STATUS_ACTIVE) {
					ut.begin();
				}
				String typ = lp.getValueByLabel("Typ");
				typ = typ.toLowerCase();
				String adresse = lp.getValueByLabel("Adresse");
				Object comm = PropertyUtils.getProperty(c, "communication");
				if (comm == null) {
					comm = m_nucleusService.getClass(sdesc, "Communication").newInstance();
					PropertyUtils.setProperty(c, "communication", comm);
					pm.makePersistent(comm);
				}
				String[] m1 = (String[]) mapping.get(typ);
				if (m1 == null) {
					System.out.println("typ(" + typ + "): not found");
					continue;
				}
				String field = m1[0];
				BeanUtils.setProperty(comm, field, adresse);
				pm.makePersistent(comm);
				if ((num % 1000) == 1) {
					System.out.println(num + ":\t" + new Date().getTime());
					ut.commit();
				}
				num++;
			}
			if (ut.getStatus() == Status.STATUS_ACTIVE) {
				ut.commit();
			}
			System.out.println("Communication have been persisted");
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} finally {
			pm.close();
		}
		return null;
	}

	private Map importZipcodes(String storeId, String basedir) {
		StoreDesc sdesc = StoreDesc.get(storeId);
		PersistenceManager pm = m_nucleusService.getPersistenceManagerFactory(sdesc).getPersistenceManager();
		UserTransaction ut = m_nucleusService.getUserTransaction();
		Class _company = m_nucleusService.getClass(sdesc, "Company");
		Class _contact = m_nucleusService.getClass(sdesc, "Contact");
		Map mapping = initZipcodes();
		try {
			LabeledCSVParser lp = new LabeledCSVParser(getCSVParser(new FileInputStream(new File(basedir,"zipcodes.csv"))));
			System.out.println("Persisting zipcodes");
			int num = 0;
			while (lp.getLine() != null) {
				if (ut.getStatus() != Status.STATUS_ACTIVE) {
					ut.begin();
				}
				// Zipcode zipcode = new Zipcode(); 
				Object zipcode = m_nucleusService.getClass(sdesc, "Zipcode").newInstance();

				String gemeindekennziffer = lp.getValueByLabel("Gemeindekennziffer");
				String ortname = lp.getValueByLabel("ORTNAME");
				String plz = lp.getValueByLabel("PLZ");
				String lkz = lp.getValueByLabel("LKZ");
				PropertyUtils.setProperty(zipcode, "lkz", lkz);
				PropertyUtils.setProperty(zipcode, "plz", plz);
				PropertyUtils.setProperty(zipcode, "ortname", ortname);
				PropertyUtils.setProperty(zipcode, "gemeindekennziffer", gemeindekennziffer);

				Collection cl = getContactList(pm, _company, plz);
				if( cl != null){
					for( Object cx : cl ){
						PropertyUtils.setProperty(cx, "zipcode", zipcode);
						PropertyUtils.setProperty(cx, "lkz", lkz);
					}
				}
				cl = getContactList(pm, _contact, plz);
				if( cl != null){
					for( Object cx : cl ){
						PropertyUtils.setProperty(cx, "zipcode", zipcode);
						PropertyUtils.setProperty(cx, "lkz", lkz);
					}
				}
				pm.makePersistent(zipcode);
				if ((num % 1000) == 1) {
					System.out.println(num + ":\t" + new Date().getTime());
					ut.commit();
				}
				num++;
			}
			if (ut.getStatus() == Status.STATUS_ACTIVE) {
				ut.commit();
			}
			System.out.println("Contact and Book have been persisted");
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} finally {
			pm.close();
		}
		return null;
	}

	public List<Map> _getUserListStatusChange( String dateStr, String state) throws Exception{ //12.10.2014
		StoreDesc sdesc = StoreDesc.get("ea_data");
		SessionContext sc = m_dataLayer.getSessionContext(sdesc);
		List fieldList = new ArrayList();
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
		Date date = dateFormat.parse(dateStr);
		long startTime = getStartOfDay(date).getTime();
		long endTime = getEndOfDay(date).getTime();

		String validField = "to".equals(state) ? "contact$_team_list.validTo" : "contact$_team_list.validFrom";
		System.out.println("startTime:"+startTime);
		System.out.println("endTime:"+endTime);

		Map field = new HashMap();
		field.put("field", "contact$_team_list.teamid");
		field.put("op", "bw");
		field.put("data", "root.ENPEDIA");
		field.put("children", new ArrayList());
		fieldList.add(field);

		field = new HashMap();
		field.put("field", validField);
		field.put("op", "gt");
		field.put("data", startTime-1);
		field.put("children", new ArrayList());
		fieldList.add(field);

		field = new HashMap();
		field.put("field", validField);
		field.put("op", "lt");
		field.put("data", endTime+1);
		field.put("children", new ArrayList());
		fieldList.add(field);

		Map filter = new HashMap();
		filter.put("connector", "and");
		filter.put("children", fieldList);

		String filterJson = m_gitService.searchContent( sdesc.getNamespace(), "contact.filter", "sw.filter" );
		Map contentMap = (Map) m_ds.deserialize(filterJson);
		contentMap.put("filter",filter);
		contentMap.put("pageSize", 0);
		Map ret = sc.executeFilter(contentMap,null);
		List<Map> rows = (List) ret.get("rows");
		if( rows!=null){
			List teams = new ArrayList();
			for( Map row : rows){
				Collection teamList = (Collection)row.get("_team_list");
				row.remove("_team_list");
				row.put("teams",toTeams(teamList));
			}
		}
		return rows;
	}
	private Collection toTeams(Collection<Map> teamList){
		Collection teams = new ArrayList();
		for( Map t : teamList){
			String teamid = (String)t.get("teamid");
			Map team = new HashMap();
			team.put(teamid,t);
			teams.add(team);
		}
		return teams;
	}
	private static String EMAIL = "email";
	protected void _syncWithEnpedia() throws Exception{
		StoreDesc sdesc = StoreDesc.get("ea_data");
		PersistenceManager pm = m_nucleusService.getPersistenceManagerFactory(sdesc).getPersistenceManager();
		Class _contact = m_nucleusService.getClass(sdesc, "Contact");
		Class _company = m_nucleusService.getClass(sdesc, "Company");
		List<Map<String,String>> userList = csvToListOfMap("user-complete.csv");
		try{
			for( Map<String,String> userMap : userList){
				String email = userMap.get(EMAIL);
				Object c = getContact(pm,_contact,email);
				if( c!= null){
					System.out.println("+++Email found:"+email);
				}else{
					c = getContact(pm,_company,email);
					if( c!= null){
						System.out.println("===Email found:"+email);
					}else{
						System.out.println("---Email not found:"+email);
					}
				}
			}
		} finally {
			pm.close();
		}
	}

	private Date getEndOfDay(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.set(Calendar.HOUR_OF_DAY, 23);
		calendar.set(Calendar.MINUTE, 59);
		calendar.set(Calendar.SECOND, 59);
		calendar.set(Calendar.MILLISECOND, 999);
		return calendar.getTime();
	}

	private Date getStartOfDay(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar.getTime();
	}

	private List<Map<String,String>> csvToListOfMap(String filename) throws Exception{
		String basedir = System.getProperty("workspace") + "/../etc/ea/data";
		LabeledCSVParser lp = new LabeledCSVParser(getCSVParser(new FileInputStream(new File(basedir,filename))));
		String[] labels = lp.getLabels();
		int num = 0;
		List<Map<String,String>> retList = new ArrayList();
		while (lp.getLine() != null) {
			Map<String,String> map = new HashMap();
			retList.add(map);
			for( String label : labels){
				map.put(label, lp.getValueByLabel(label));
			}
		}
		return retList;
	}
	private Object getContact(PersistenceManager pm, Class clazz, String email) {
		Extent e = pm.getExtent(clazz, true);
		Query q = pm.newQuery(e, "(_state=='ok' or _state is null) && communication.mail1 == '" + email + "'");
		Collection coll = (Collection) q.execute();
		Iterator iter = coll.iterator();
		if (iter.hasNext()) {
			Object c = iter.next();
			return c;
		} else {
			//System.out.println("number(" + nummer + ") not found");
		}
		return null;
	}

	private CSVParse getCSVParser(InputStream is) {
		char delimeter = ',';
		char quote = '"';
		CSVParse p = new CSVParser(is, delimeter);
		p.changeQuote(quote);
		return p;
	}
	public Map initCompany() {
		Map<String, String[]> mMap = new HashMap<String, String[]>();
		mMap.put("Nummer", new String[] { "number", "string" });
		mMap.put("HauptadressNummer", new String[] { "main_address_number", "string" });
		mMap.put("Ursprung", new String[] { "origin", "string" });
		mMap.put("KurznameFirma", new String[] { "shortname_company", "string" });
		mMap.put("Firma1", new String[] { "company1", "string" });
		mMap.put("Firma2", new String[] { "company2", "string" });
		mMap.put("Firma3", new String[] { "company3", "string" });
		mMap.put("Strasse", new String[] { "street", "string" });
		mMap.put("LKZ", new String[] { "lkz", "string" });
		mMap.put("PLZ", new String[] { "plz", "string" });
		mMap.put("Ort", new String[] { "ortname", "string" });
		mMap.put("Gemeindekennziffer", new String[] { "gemeindekennziffer", "string" });
		mMap.put("AlphaNummer", new String[] { "aplha_number", "string" });
		mMap.put("AdressStatus", new String[] { "address_status", "string" });
		mMap.put("AdressStatusSeit", new String[] { "address_status_since", "date" });
		mMap.put("Bemerkung", new String[] { "remark", "string" });
		mMap.put("Erfassungsdatum", new String[] { "_created_at", "date" });
		mMap.put("ErfasstVon", new String[] { "_created_by", "string" });
		mMap.put("Aenderungsdatum", new String[] { "_updated_at", "date" });
		mMap.put("GeaendertVon", new String[] { "_updated_by", "string" });
		return mMap;
	}
	public Map initContacts() {
		Map<String, String[]> mMap = new HashMap<String, String[]>();
		mMap.put("Nummer", new String[] { "number", "string" });
		mMap.put("HauptadressNummer", new String[] { "main_address_number", "string" });
		mMap.put("Ursprung", new String[] { "origin", "string" });
		mMap.put("KurznameFirma", new String[] { "shortname_company", "string" });
		mMap.put("KurznamePerson", new String[] { "shortname_person", "string" });
		mMap.put("Firma1", new String[] { "company1", "string" });
		mMap.put("Firma2", new String[] { "company2", "string" });
		mMap.put("Firma3", new String[] { "company3", "string" });
		mMap.put("Geburtsdatum", new String[] { "birthdate", "date" });
		mMap.put("BerufsSchluessel", new String[] { "job_key", "string" });
		mMap.put("Geschlecht", new String[] { "sex", "string" });
		mMap.put("AnredeSchluessel", new String[] { "salutation_key", "string" });
		mMap.put("Anredename", new String[] { "salutation_name", "string" });
		mMap.put("TitelSchluessel", new String[] { "title_key", "string" });
		mMap.put("Vorname", new String[] { "givenname", "string" });
		mMap.put("Name1", new String[] { "name1", "string" });
		mMap.put("Strasse", new String[] { "street", "string" });
		mMap.put("LKZ", new String[] { "lkz", "string" });
		mMap.put("PLZ", new String[] { "plz", "string" });
		mMap.put("Ort", new String[] { "ortname", "string" });
		mMap.put("Gemeindekennziffer", new String[] { "gemeindekennziffer", "string" });
		mMap.put("AlphaNummer", new String[] { "aplha_number", "string" });
		mMap.put("Briefanrede", new String[] { "letter_salutation", "string" });
		mMap.put("Robinson", new String[] { "robinson", "string" });
		mMap.put("AdressStatus", new String[] { "address_status", "string" });
		mMap.put("AdressStatusSeit", new String[] { "address_status_since", "date" });
		mMap.put("Bemerkung", new String[] { "remark", "string" });
		mMap.put("Erfassungsdatum", new String[] { "_created_at", "date" });
		mMap.put("ErfasstVon", new String[] { "_created_by", "string" });
		mMap.put("Aenderungsdatum", new String[] { "_updated_at", "date" });
		mMap.put("GeaendertVon", new String[] { "_updated_by", "string" });
		return mMap;
	}

	public Map initActivities() {
		Map<String, String[]> mMap = new HashMap<String, String[]>();
		mMap.put("Nummer", new String[] { "number", "number" });
		mMap.put("Kontakt", new String[] { "contact_type", "string" });
		mMap.put("Adresse", new String[] { "address", "string" });
		mMap.put("Datum", new String[] { "date", "date" });
		mMap.put("Notiz", new String[] { "note", "string" });
		mMap.put("Vorgang", new String[] { "process", "string" });
		mMap.put("erledigt", new String[] { "done", "boolean" });
		mMap.put("traits", new String[] { "traits", "string" });
		return mMap;
	}
	public Map initZipcodes() {
		Map<String, String[]> mMap = new HashMap<String, String[]>();
		mMap.put("LKZ", new String[] { "lkz", "string" });
		mMap.put("PLZ", new String[] { "plz", "string" });
		mMap.put("ORTNAME", new String[] { "ortname", "string" });
		mMap.put("Gemeindekennziffer", new String[] { "gemeindekennziffer", "string" });
		return mMap;
	}

	public Map initPersons() {
		Map<String, String[]> mMap = new HashMap<String, String[]>();
		mMap.put("Surname", new String[] { "surname", "string" });
		mMap.put("Givenname", new String[] { "givenname", "string" });
		return mMap;
	}

	public Map initTags() {
		Map<String, String[]> mMap = new HashMap<String, String[]>();
		mMap.put("name", new String[] { "name", "string" });
		mMap.put("disabled", new String[] { "disabled", "boolean" });
		mMap.put("valid_from", new String[] { "valid_from", "date" });
		mMap.put("valid_to", new String[] { "valid_to", "date" });
		return mMap;
	}

	public Map initCommunication() {
		Map<String, String[]> mMap = new HashMap<String, String[]>();
		mMap.put("nummer", new String[] { "number", "number" });
		mMap.put("pf", new String[] { "pf", "string" });
		mMap.put("fond1", new String[] { "fond1", "string" });
		mMap.put("faxd1", new String[] { "faxd1", "string" });
		mMap.put("fond2", new String[] { "fond2", "string" });
		mMap.put("fonp1", new String[] { "fonp1", "string" });
		mMap.put("fonp2", new String[] { "fonp2", "string" });
		mMap.put("faxd2", new String[] { "faxd2", "string" });
		mMap.put("faxp1", new String[] { "faxp1", "string" });
		mMap.put("fonm1", new String[] { "fonm1", "string" });
		mMap.put("fonm2", new String[] { "fonm2", "string" });
		mMap.put("sms1", new String[] { "sms1", "string" });
		mMap.put("mail1", new String[] { "mail1", "string" });
		mMap.put("mail2", new String[] { "mail2", "string" });
		mMap.put("mail3", new String[] { "mail3", "string" });
		mMap.put("mail4", new String[] { "mail4", "string" });
		mMap.put("tel3", new String[] { "tel3", "string" });
		mMap.put("tel4", new String[] { "tel4", "string" });
		mMap.put("tel5", new String[] { "tel5", "string" });
		mMap.put("tel6", new String[] { "tel6", "string" });
		mMap.put("tel7", new String[] { "tel7", "string" });
		mMap.put("www1", new String[] { "www1", "string" });
		mMap.put("www2", new String[] { "www2", "string" });
		return mMap;
	}

	private Object getObject(PersistenceManager pm, Class clazz1, Class clazz2,String nummer) {
		Object c = getObject(pm, clazz1, nummer);
		if( c==null){
			c = getObject(pm, clazz2, nummer);
		}
		return c;
	}

	private Object getObject(PersistenceManager pm, Class clazz, String nummer) {
		Extent e = pm.getExtent(clazz, true);
		Query q = pm.newQuery(e, "number == " + nummer + "");
		Collection coll = (Collection) q.execute();
		Iterator iter = coll.iterator();
		if (iter.hasNext()) {
			Object c = iter.next();
			return c;
		} else {
			//System.out.println("number(" + nummer + ") not found");
		}
		return null;
	}
	private Collection getContactList(PersistenceManager pm, Class clazz, String plz) {
		List ret = new ArrayList();
		Extent e = pm.getExtent(clazz, true);
		Query q = pm.newQuery(e, "plz == '" + plz + "'");
		Collection coll = (Collection) q.execute();
		return coll;
	}

	private Boolean isActive(String status){
		if(status == null || "".equals(status.trim())) return null;
		if("A".equals(status.trim())) return true;
		if("I".equals(status.trim())) return false;
		return null;
	}
	private Date getTeamDate(String dstr){
		if(dstr == null || "".equals(dstr.trim())) return null;
		return getDate(dstr);
	}

	private Object getTeamintern(PersistenceManager pm, Class clazz,String teamid) {
		try {
			Object obj = pm.getObjectById(clazz, teamid);
			return obj;
		} catch (Exception e) {
			throw new RuntimeException("getTeamintern(" + teamid + ")", e);
		} 
	}
	private String checkNull(Map m, String key, String msg) {
		if (m.get(key) != null) {
			return (String) m.get(key);
		}
		throw new RuntimeException(msg);
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
}
