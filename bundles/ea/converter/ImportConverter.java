import com.wcohen.ss.*;
import com.wcohen.ss.lookup.*;
import com.wcohen.ss.api.*;
import com.wcohen.ss.tokens.*;
import java.io.IOException;
import java.util.Collection;
import java.util.*;
import java.io.*;
import java.math.*;
import org.apache.commons.codec.language.*;
import com.Ostermiller.util.*;
import com.wcohen.ss.abbvGapsHmm.Acronym;
import org.apache.commons.validator.routines.EmailValidator;
import com.wcohen.ss.abbvGapsHmm.AlignmentPredictionModel;
import static org.apache.commons.lang.StringUtils.trim;
import static org.apache.commons.lang.StringUtils.leftPad;

public class ImportConverter {

	public List<String> m_dupFieldsCompany = new ArrayList();

	public List<String> m_dupFieldsContact = new ArrayList();

	List<Map<String, String>> m_result = new ArrayList();

	List<Map<String, String>> m_dups = new ArrayList();

	static SoftTFIDF m_companyCmp = null;

	static SoftTFIDF m_ortCmp = null;

	static SoftTFIDF m_contactCmp = null;

	Map<String, String> m_aliases = new HashMap();

	static double m_outerThresholdCompany = 0.85;
	static double m_outerThresholdContact = 0.85;

	static double m_innerThresholdCompany = 0.85;
	static double m_innerThresholdContact = 0.85;

	String m_dupFieldCompany = "dupCompany";

	String m_dupFieldContact = "dupContact";

	List<String> m_regexExeptionList = new ArrayList();

	int m_maybeaEqual = 0;

	int m_complettEqual = 0;

	int m_invalidPlzs = 0;
	EmailValidator m_ev = EmailValidator.getInstance();

	Map m_companyMapping = initCompany();

	Map m_contactMapping = initContacts();
	List<Map<String, String>> m_notCorrectList = new ArrayList();

	Integer m_numberCounter = 2000000;

	int m_companyId = 0;

	public static void main(String[] args) throws Exception {
		m_outerThresholdCompany = new BigDecimal(args[0]).doubleValue();
		m_innerThresholdCompany = new BigDecimal(args[1]).doubleValue();
		m_outerThresholdContact = new BigDecimal(args[2]).doubleValue();
		m_innerThresholdContact = new BigDecimal(args[3]).doubleValue();
		SimpleTokenizer tokenizer = new SimpleTokenizer(true, true);
		m_companyCmp = new SoftTFIDF(tokenizer, new JaroWinkler(), m_innerThresholdCompany);
		m_contactCmp = new SoftTFIDF(tokenizer, new JaroWinkler(), m_innerThresholdContact);
		m_ortCmp = new SoftTFIDF(tokenizer, new JaroWinkler(), 0.97);
		//org.apache.log4j.BasicConfigurator.configure();
		new ImportConverter().doIt();
		//new ImportConverter().test();
	}

	private void test()throws Exception{
		AbbreviationAlignment a = new AbbreviationAlignment();
		String score = a.explainScore("M Sattler","Manfred Sattler");
		System.out.println("score:\n"+score);
		List<Map<String, String>> allList = readFile("Adressen.csv");
		int len = allList.size();
		for (int i = 0; i < len; i++) {
			if ((i % 100) == 1) {
System.out.println("i:"+i);
			}
			for (int j = i+1; j < len; j++) {
				Map<String,String> m1 = allList.get(i);
				Map<String,String> m2 = allList.get(j);
				String name1 = m1.get("shortname_company");// + " " +m1.get("name1");
				String name2 = m2.get("shortname_company");// + " " +m2.get("name1");
				if(name1.equals(name2)){
					continue;
				}
				if( name1.indexOf(".") == -1 /*&& name2.indexOf(".") == -1*/){
					continue;
				}
				double score1 = a.score(name1,name2);
				if( score1!= 0){
					String e = a.explainScore(name1,name2);
					System.out.println(name1+"/"+name2+":\n"+e);
				}
			}
		}
	}

	public void doIt() throws Exception {
		m_dupFieldsCompany.add("shortname_company");
		m_dupFieldsContact.add("name1");
		m_dupFieldsContact.add("givenname");
		m_dupFieldsContact.add("street");
		Map<String, String> emailMap = readCommFile("/tmp/Kommunikation.csv");
		List<Map<String, String>> allList = readFile("/tmp/Adressen.csv");
		//m_aliases.put("vhs", "volkshochschule");
		//m_aliases.put("evangelischer", "ev.");
		//m_aliases.put("evangelische", "ev.");
		m_regexExeptionList.add("^stadt \\w.*$");
		m_regexExeptionList.add("^stadtwerke \\w.*$");
		m_regexExeptionList.add("^stadt \\w.*$");
		m_regexExeptionList.add("^volkshochschule \\w.*$");
		m_regexExeptionList.add("^stadt \\w.*$");
		m_regexExeptionList.add("^.*stadtwerke.*$");
		train(allList);
		setExtraToken(m_contactCmp);
		setExtraToken(m_companyCmp);
		int num = 0;
		List<Map<String, String>> singleContactList = new ArrayList();
		List<Map<String, String>> resultList = new ArrayList();
		List<Map<String, String>> foundList = new ArrayList();
		Map<String,String> valEmailMap=new HashMap();
		for (Map<String, String> candidat : allList) {
			if (isNotCorrect(candidat.get("shortname_company"), candidat.get("shortname_person"))) {
				candidat.put("type", "nok_sn");
				m_notCorrectList.add(candidat);
				continue;
			}
			String stat = isNotCorrectEmail(candidat.get("number"), emailMap,valEmailMap);
			boolean isCompany = isCompany(candidat.get("shortname_company"), candidat.get("shortname_person"));
			if (!isCompany && !"ok".equals(stat)) {
				candidat.put("type", stat);
				m_notCorrectList.add(candidat);
				continue;
			}
			if (isSingleContact(candidat.get("shortname_person"), candidat.get("shortname_company"))) {
				candidat.put("type", "contact_single");
				singleContactList.add(candidat);
				continue;
			}
			String id = isDupCompany(resultList, candidat);
			if (id == null) {
				candidat.put("companyId", newCompayId());
				resultList.add(candidat);
			} else {
				candidat.put("companyId", id);
				foundList.add(candidat);
			}
			if ((num % 1000) == 1) {
				System.out.println("NUM:" + num + "/" + resultList.size() + "/" + foundList.size() + "/schrott:" + m_notCorrectList.size() + "/" + m_companyId);
			}
			//if( num == 2000) break;
			num++;
		}
		resultList.addAll(foundList);
		setOccurence(resultList);
		sortListById(resultList);
		sortListByOccurence(resultList);
		List filtered = filterList(resultList);
		markDupContacts(singleContactList);
		filtered.addAll(singleContactList);
		filtered.addAll(m_notCorrectList);
		createCSV(filtered);
	}

	private List filterList(List<Map<String, String>> inList) {
		String lastId = null;
		List group = null;
		List filtered = new ArrayList();
		for (Map<String, String> current : inList) {
			String curId = current.get("companyId");
			if (!curId.equals(lastId)) {
				if (group != null) {
					cleanCompany(group);
					filtered.addAll(group);
				}
				lastId = curId;
				group = new ArrayList();
			}
			group.add(current);
		}
		cleanCompany(group);
		filtered.addAll(group);
		return filtered;
	}

	private void cleanCompany(List<Map<String, String>> group) {
		String companyName = getMost(group, "shortname_company");
		List<Map<String, String>> comList = new ArrayList();
		List<Map<String, String>> conList = new ArrayList();
		for (Map<String, String> current : group) {
			if (isCompany(current.get("shortname_company"), current.get("shortname_person"))) {
				comList.add(current);
				current.put("type", "company");
			} else {
				current.put("type", "contact");
				current.put("shortname_company", companyName);
				conList.add(current);
			}
		}
		if (comList.size() == 0) {
			if (conList.size() > 0) {
				comList.add(createCompanyFromContact(conList.get(0)));
			}
		}
		if (comList.size() > 1) {
			int i = 0;
			for (Map<String, String> com : comList) {
				if (i > 0) {
					com.put("type", "company_dup");
				}
				i++;
			}
			comList.get(0).put("shortname_company", companyName);
		}
		markDupContacts(conList);
		group.clear();
		group.addAll(comList);
		group.addAll(conList);
	}

	private void markDupContacts(List<Map<String, String>> conList) {
		List<Map<String, String>> compareList = new ArrayList();
		for (Map<String, String> candidat : conList) {
			boolean b = isDupContactList(compareList, candidat);
			if (b) {
				candidat.put("type", "contact_dup");
			} else {
				compareList.add(candidat);
			}
		}
	}

	private boolean isDupContactList(List<Map<String, String>> compareList, Map<String, String> candidat) {
		for (Map current : compareList) {
			boolean b = isDupContact(current, candidat);
			if (b) {
				return true;
			}
		}
		return false;
	}

	private boolean isDupContact(Map<String, String> currentMap, Map<String, String> candidatMap) {
		String current = currentMap.get(m_dupFieldContact);
		String candidat = candidatMap.get(m_dupFieldContact);
		String s1 = current.toLowerCase();
		String s2 = candidat.toLowerCase();
		if (s1.equals(s2)) {
			candidatMap.put("score","100%");
			return true;
		} else {
			double d = m_contactCmp.score(m_contactCmp.prepare(s1), m_contactCmp.prepare(s2));
			if (d > m_outerThresholdContact) {
				String s = m_contactCmp.explainScore(m_contactCmp.prepare(s1), m_contactCmp.prepare(s2));
				candidatMap.put("score",s);
				candidatMap.put("dupOther",current);
				return true;
			}
		}
		return false;
	}
	protected Map<String, String> readCommFile(String file) throws Exception {
		Map<String, String> retMap = new HashMap();
		try {
			LabeledCSVParser lp = new LabeledCSVParser(new ExcelCSVParser(new FileInputStream(new File(file))));
			while (lp.getLine() != null) {
				String typ = lp.getValueByLabel("Typ");
				if(!"Mail1".equals(typ)) continue;
				String number = lp.getValueByLabel("Nummer");
				String mail = lp.getValueByLabel("Adresse");
				retMap.put(number, mail);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}
		return retMap;
	}

	protected List<Map<String, String>> readFile(String file) throws Exception {
		Map mapping = m_contactMapping;
		List<Map<String, String>> retList = new ArrayList();
		try {
			LabeledCSVParser lp = new LabeledCSVParser(new ExcelCSVParser(new FileInputStream(new File(file))));
			while (lp.getLine() != null) {
				String astat = lp.getValueByLabel("AdressStatus");
				if (astat != null && astat.trim().equals("L")) {
					continue;
				}

				Map<String, String> contact = new HashMap();
				Iterator it = mapping.keySet().iterator();
				String dupFieldCompany = "";
				String dupFieldContact = "";
				String delimCompany = "";
				String delimContact = "";
				String plz = lp.getValueByLabel("PLZ");
				String shortname_company = lp.getValueByLabel("KurznameFirma");
				//if( ".".equals(shortname_company)) shortname_company = "";
				Map<String,String> dupMap = new HashMap();
				while (it.hasNext()) {
					String key = (String) it.next();
					String[] m1 = (String[]) mapping.get(key);
					String field = m1[0];
					String val = lp.getValueByLabel(key).trim();
					contact.put(field, val);
					if (m_dupFieldsCompany.contains(field)) {
						dupFieldCompany += delimCompany + val;
						delimCompany = " ";
					}
					if (m_dupFieldsContact.contains(field)) {
						dupMap.put(field,val);
					}
				}
				for(String dField : m_dupFieldsContact){
					dupFieldContact += delimContact + dupMap.get(dField);
					delimContact = " ";
				}
				contact.put(m_dupFieldCompany, dupFieldCompany);
				contact.put(m_dupFieldContact, dupFieldContact);
				contact.put("sc_orig", shortname_company);
				//contact.put("shortname_company", shortname_company);
				if (!isValidPlz(plz)) {
					m_invalidPlzs++;
					contact.put("type", "nok_plz");
					m_notCorrectList.add(contact);
					continue;
				}
				retList.add(contact);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}
		return retList;
	}


	private boolean compareOrtPlz(String ortCandidat,String plzCandidat,String ortCurrent, String plzCurrent){
		boolean ort = false;
		boolean plz = false;
		
		if( ortCandidat == null && ortCurrent == null){
			ort = true;
		}else if( ortCandidat != null && ortCandidat.equals(ortCurrent)){
			ort = true;
		}
		if( plzCandidat == null && plzCurrent == null){
			plz = true;
		}else if( plzCandidat != null && plzCandidat.equals(plzCurrent)){
			plz = true;
		}
		return ort && plz;
	}

	private String isDupCompany(List<Map<String, String>> allList, Map<String, String> candidat) {
		for (Map current : allList) {
			String id = isDupCompany(current, candidat);
			if (id != null) {
				return id;
			}
		}
		return null;
	}

	private String isDupCompany(Map<String, String> currentMap, Map<String, String> candidatMap) {
		String plzCurrent = currentMap.get("plz");
		String companyId = currentMap.get("companyId");
		String currentcompanyId = candidatMap.get("companyId");
		String plzCandidat = candidatMap.get("plz");
		String current = currentMap.get(m_dupFieldCompany);
		String candidat = candidatMap.get(m_dupFieldCompany);
		String ortCurrent = currentMap.get("ortname").toLowerCase();
		String ortCandidat = candidatMap.get("ortname").toLowerCase();
		if (ortCandidat != null && ortCurrent != null) {
			if (ortCurrent.equals(ortCandidat) && !plzCandidat.equals(plzCurrent)) {
			}
		}
		if (current != null && candidat != null) {
			double od = 0;
			if(compareOrtPlz( ortCandidat,plzCandidat,ortCurrent,plzCurrent)){
				od = 1.0;
			} else {
				od = m_ortCmp.score(m_companyCmp.prepare(ortCurrent), m_companyCmp.prepare(ortCandidat));
			}
			if (od < 0.97) {
				return null;
			}
			String s1 = current.toLowerCase();
			String s2 = candidat.toLowerCase();
			if (isRegexException(s1, s2)) {
				return null;
			}
			if (s1.equals(s2)) {
				return companyId;
			} else {
				double d = m_companyCmp.score(m_companyCmp.prepare(s1), m_companyCmp.prepare(s2));
				if (d > m_outerThresholdCompany) {
					return companyId;
				}
			}
		}
		return null;
	}

	private String newNumber() {
		m_numberCounter++;
		return "" + m_numberCounter;
	}

	private String newCompayId() {
		m_companyId++;
		return "" + m_companyId;
	}

	private void train(List<Map<String, String>> allList) {
		List corbusCom = new ArrayList();
		List corbusO = new ArrayList();
		List corbusCon = new ArrayList();
		for (Map<String, String> current : allList) {
			String s = replaceAliases(current.get(m_dupFieldCompany).toLowerCase());
			corbusCom.add(m_companyCmp.prepare(s));
			String ort = current.get("ortname");
			corbusO.add(m_ortCmp.prepare(ort));
			s = current.get(m_dupFieldContact).toLowerCase();
			corbusCon.add(m_contactCmp.prepare(s));
		}
		m_companyCmp.train(new BasicStringWrapperIterator(corbusCom.iterator()));
		m_ortCmp.train(new BasicStringWrapperIterator(corbusO.iterator()));
		m_contactCmp.train(new BasicStringWrapperIterator(corbusCon.iterator()));
	}

	private void setExtraToken(SoftTFIDF cmp) {
		Iterator<Token> it = cmp.tokenIterator();
		int i = highestTindex(it);
		cmp.setDocumentFrequency(new BasicToken(i++, "auf"), 10000);
		cmp.setDocumentFrequency(new BasicToken(i++, "dem"), 10000);
		cmp.setDocumentFrequency(new BasicToken(i++, "an"), 10000);
		cmp.setDocumentFrequency(new BasicToken(i++, "am"), 10000);
		cmp.setDocumentFrequency(new BasicToken(i++, "in"), 10000);
		cmp.setDocumentFrequency(new BasicToken(i++, "zum"), 10000);
		cmp.setDocumentFrequency(new BasicToken(i++, "der"), 10000);
		cmp.setDocumentFrequency(new BasicToken(i++, "die"), 10000);
		cmp.setDocumentFrequency(new BasicToken(i++, "das"), 10000);
	}

	private int highestTindex(Iterator<Token> it) {
		int h = 0;
		while (it.hasNext()) {
			Token key = it.next();
			h = Math.max(h, key.getIndex());
		}
		return h;
	}

	private boolean isEmpty(String s) {
		return (s == null || "".equals(s.trim()));
	}

	private boolean isRegexException(String s1, String s2) {
		Iterator<String> it = m_regexExeptionList.iterator();
		while (it.hasNext()) {
			String regex1 = it.next();
			String regex2 = it.next();
			if ((s1.matches(regex1) && s2.matches(regex2)) || (s2.matches(regex1) && s1.matches(regex2))) {
				return true;
			}
		}
		return false;
	}

	private String replaceAliases(String s) {
		Iterator<String> it = m_aliases.keySet().iterator();
		s = s.toLowerCase();
		while (it.hasNext()) {
			String key = it.next();
			String val = m_aliases.get(key);
			s = s.replace(key, val);
		}
		return s;
	}

	private boolean isValidPlz(String s) {
		if (s == null)
			return false;
		try {
			Integer x = Integer.parseInt(trim(s));
			if (x <= 1000)
				return false;
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private boolean isNotCorrect(String shortname_company, String shortname_person) {
		return ".".equals(shortname_company) || (isEmpty(shortname_company) && isEmpty(shortname_person));
	}

	private String isNotCorrectEmail(String number, Map<String,String> emailMap, Map<String,String> valEmailMap) {
		String email = emailMap.get(number);
		if( isEmpty(email)){
		 return "nok_emailempty";
		}
		boolean valid = m_ev.isValid(email);
		if( !valid ){
			return "nok_emailinvalid";
		}
		if( valEmailMap.get(email.toLowerCase()) != null){
			return "nok_emaildup";
		}
		valEmailMap.put(email.toLowerCase(),"xx");
		return "ok";
	}
	private boolean isSingleContact(String shortname_person, String shortname_company) {
		boolean b = !isEmpty(shortname_person) && isEmpty(shortname_company);
		return b;
	}

	private boolean isContact(String shortname_person) {
		return !isEmpty(shortname_person);
	}

	private boolean hasCompanyField(String shortname_company) {
		return !isEmpty(shortname_company);
	}

	private boolean isCompany(String shortname_company, String shortname_person) {
		return !isEmpty(shortname_company) && isEmpty(shortname_person);
	}

	private void setOccurence(List<Map<String, String>> resultList) {
		Map<String, Integer> companyIdMap = new HashMap();
		for (Map<String, String> current : resultList) {
			String companyId = current.get("companyId");
			if (companyIdMap.get(companyId) == null) {
				companyIdMap.put(companyId, 1);
			} else {
				int occur = companyIdMap.get(companyId);
				companyIdMap.put(companyId, ++occur);
			}
		}
		for (Map<String, String> current : resultList) {
			String id = current.get("companyId");
			int occur = (companyIdMap.get(id) != null) ? companyIdMap.get(id) : 1;
			current.put("occur", occur + "");
		}
	}

	private void sortListById(List<Map<String, String>> list) {
		Collections.sort(list, new ListSortById());
	}

	private class ListSortById implements Comparator<Map<String, String>> {

		public int compare(Map<String, String> c1, Map<String, String> c2) {
			int id1 = Integer.parseInt(c1.get("companyId"));
			int id2 = Integer.parseInt(c2.get("companyId"));
			return (id1) - (id2);
		}
	}

	private void sortListByOccurence(List<Map<String, String>> list) {
		Collections.sort(list, new ListSortByOccurence());
	}

	private class ListSortByOccurence implements Comparator<Map<String, String>> {

		public int compare(Map<String, String> c1, Map<String, String> c2) {
			int o1 = Integer.parseInt(c1.get("occur"));
			int o2 = Integer.parseInt(c2.get("occur"));
			return (o2) - (o1);
		}
	}

	private String getMost(List<Map<String, String>> list, String key) {
		Map<String, Integer> map = new HashMap<String, Integer>();
		for (Map<String, String> m : list) {
			String s = m.get(key);
			if (map.containsKey(s)) {
				map.put(s, map.get(s) + 1);
			} else {
				map.put(s, 1);
			}
		}
		ValueComparator<String, Integer> comparator = new ValueComparator<String, Integer>(map);
		Map<String, Integer> sortedMap = new TreeMap<String, Integer>(comparator);
		sortedMap.putAll(map);
		List<String> sortedList = new ArrayList<String>(sortedMap.keySet());
		if (sortedList.size() > 1) {
			//System.out.println("\tso:" + sortedMap);
		}
		return sortedList.get(0);
	}

	private class ValueComparator<K, V extends Comparable<V>> implements Comparator<K> {

		Map<K, V> map;

		public ValueComparator(Map<K, V> base) {
			this.map = base;
		}

		@Override
		public int compare(K o1, K o2) {
			return map.get(o2).compareTo(map.get(o1));
		}
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

	private Map createCompanyFromContact(Map<String, String> contact) {
		Map mapping = m_companyMapping;
		Map company = new HashMap();
		Iterator it = mapping.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			String[] m1 = (String[]) mapping.get(key);
			String field = m1[0];
			Object val = contact.get(field);
			company.put(field, val);
		}
		company.put("number", newNumber());
		company.put("type", "company_fc");
		company.put("companyId", contact.get("companyId"));
		company.put("sc_orig", contact.get("sc_orig"));
		return company;
	}

	private String createCSV(List<Map<String, String>> data) throws Exception {
		FileWriter fw = new FileWriter("ea.csv");
		CSVPrint ep = new ExcelCSVPrinter(fw);
		Map<String, String> firstLine = data.get(0);
		List<String> firstFields = new ArrayList();
		firstFields.add("companyId");
		firstFields.add("type");
		firstFields.add("shortname_company");
		firstFields.add("sc_orig");
		firstFields.add("dupContact");
		firstFields.add("score");
		firstFields.add("dupOther");
		firstFields.add("ortname");
		firstFields.add("street");
		firstFields.add("plz");
		firstFields.add("name1");
		firstFields.add("givenname");
		firstFields.add( "number");
		firstFields.add( "main_address_number");
		firstFields.add( "origin");
		firstFields.add( "shortname_person");
		firstFields.add( "company1");
		firstFields.add( "company2");
		firstFields.add( "company3");
		firstFields.add( "birthdate");
		firstFields.add( "job_key");
		firstFields.add( "sex");
		firstFields.add( "salutation_key");
		firstFields.add( "salutation_name");
		firstFields.add( "title_key");
		firstFields.add( "lkz");
		firstFields.add( "gemeindekennziffer");
		firstFields.add( "aplha_number");
		firstFields.add( "letter_salutation");
		firstFields.add( "robinson");
		firstFields.add( "address_status");
		firstFields.add( "address_status_since");
		firstFields.add( "remark");
		firstFields.add( "_created_at");
		firstFields.add( "_created_by");
		firstFields.add( "_updated_at");
		firstFields.add( "_updated_by");
		for (String h : firstFields) {
			ep.print(h);
		}
		ep.println();
		Iterator<Map<String, String>> it = data.iterator();
		while (it.hasNext()) {
			Map<String, String> rowMap = it.next();
			for (String h : firstFields) {
				String value = checkNull(rowMap.get(h));
				ep.print(value);
			}
			ep.println();
		}
		try {
			ep.close();
			fw.close();
		} catch (Exception e) {
			throw new RuntimeException("ReportingServiceImpl..createCSV", e);
		}
		return "";
	}
	private String checkNull(String s){
		if(s==null) return "";
		return s;
	}
}
