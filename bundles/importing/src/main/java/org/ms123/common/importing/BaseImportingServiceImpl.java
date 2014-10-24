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
package org.ms123.common.importing;

import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;
import java.io.*;
import java.nio.charset.*;
import java.nio.*;
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
import org.ms123.common.datamapper.DatamapperService;
import org.ms123.common.data.api.SessionContext;
import org.ms123.common.data.SojoObjectFilter;
import org.ms123.common.data.dupcheck.DublettenCheckService;
import org.ms123.common.entity.api.EntityService;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.permission.api.PermissionException;
import org.ms123.common.permission.api.PermissionService;
import org.ms123.common.libhelper.Bean2Map;
import org.ms123.common.libhelper.Inflector;
import org.ms123.common.utils.Utils;
import org.apache.commons.lang3.StringUtils;
import org.ms123.common.utils.UtilsService;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import groovy.lang.*;
import org.codehaus.groovy.control.*;
import org.apache.commons.beanutils.BeanMap;

/** BaseImportingService implementation
 */
@SuppressWarnings("unchecked")
public class BaseImportingServiceImpl implements Constants {

	private static final Logger m_logger = LoggerFactory.getLogger(BaseImportingServiceImpl.class);

	protected static Inflector m_inflector = Inflector.getInstance();

	private Bean2Map m_b2m = new Bean2Map();

	private String FIELDNAME_REGEX = "[a-zA-Z0-9_]{2,64}";
	protected DataLayer m_dataLayer;
	protected DatamapperService m_datamapper;

	protected PermissionService m_permissionService;

	protected UtilsService m_utilsService;

	protected EntityService m_entityService;
	protected DublettenCheckService m_dublettenCheckService;

	protected SmooksFactory m_smooksFactory;

	protected JSONDeserializer m_ds = new JSONDeserializer();

	private ObjectUtil m_objUtils = new ObjectUtil();

	protected JSONSerializer m_js = new JSONSerializer();

	public BaseImportingServiceImpl() {
		m_js.prettyPrint(true);
	}

	/*NEWIMPORT*/
	protected List<Map> persistObjects(SessionContext sc, Object obj, Map settings, boolean withoutSave, int max){
		List<Map> retList = new ArrayList();
		UserTransaction ut = sc.getUserTransaction();
		String mainEntity = null;
		Collection<Object> resultList = null;
		if( obj instanceof Collection ){
			resultList = (Collection)obj;
		}else{
			resultList = new ArrayList();
			resultList.add(obj);
		}
		Map outputTree = (Map)settings.get("output");
		Map<String,String> parentSpec = (Map)outputTree.get("parentSpec");
		System.out.println("persistObjects:"+resultList+",parentSpec:"+parentSpec);
		String parentFieldName=null;
		Class parentClazz = null;
		String parentQuery = null;
		PersistenceManager pm = sc.getPM();
		GroovyShell groovyShell=null;
		if( parentSpec != null){
			String parentLookup = parentSpec.get("lookup");
			String relation = parentSpec.get("relation");
			if(!isEmpty(parentLookup) && !isEmpty(relation)){
				String s[] = relation.split(",");
				parentClazz = sc.getClass(getBaseName(s[0]));
				parentFieldName = s[1];
				if( parentLookup.matches(FIELDNAME_REGEX)){
					String q= isString(parentClazz,parentLookup) ? "'" : "";
					parentQuery = parentLookup+ " == "+q+"${"+parentLookup+"}"+q;
				}else if( parentLookup.matches(FIELDNAME_REGEX+","+FIELDNAME_REGEX)){
					s = parentLookup.split(",");
					String q= isString(parentClazz,s[1]) ? "'" : "";
					parentQuery = s[0]+ " == "+q+"${"+s[1]+"}"+q;
				}else{
					parentQuery = parentLookup;
				}
				groovyShell = new GroovyShell(this.getClass().getClassLoader(), new Binding(), new CompilerConfiguration());
			}
		}
		try {
			int num = 0;
			if( resultList.size() > 0){
				Class clazz = resultList.iterator().next().getClass();
				mainEntity = m_inflector.getEntityName(clazz.getSimpleName());
				String pk = getPrimaryKey(clazz);
				sc.setPrimaryKey(pk);
			}
			for (Object object : resultList) {
				if (max != -1 && num >= max){
					break;
				}
				Map m = SojoObjectFilter.getObjectGraph(object, sc,2);
				retList.add(m);
				List cv = sc.validateObject(m, mainEntity);
				System.out.println("cv:"+cv+"/"+m);
				if (cv == null && m.get("_duplicated_id_") == null) {
					if(!withoutSave){
						ut.begin();
						if( parentClazz != null){
							Object parentObject = getParentObject(groovyShell, pm,parentClazz,object,parentQuery);
							m_dataLayer.insertIntoMaster(sc, object, mainEntity,parentObject, parentFieldName);
						}
						m_dataLayer.makePersistent(sc, object);
						System.out.println("\tpersist:"+m_js.serialize(object));
						ut.commit();
					}
				} else {
					m.put("constraintViolations", cv);
				}
				num++;
				if ((num % 1000) == 0) {
					System.out.println("commit1++++++++:" + num);
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
			sc.handleException(ut, e);
		} finally {
			sc.handleFinally(ut);
		}
		return retList;
	}

	private Object getParentObject(GroovyShell shell, PersistenceManager pm, Class clazz, Object child, String queryString) throws Exception {
		String filter = expandString(shell,queryString, new BeanMap(child));
		System.out.println("getParentObject:"+filter);
		Extent e = pm.getExtent(clazz, true);
		Query q = pm.newQuery(e, filter);
		try {
			Collection coll = (Collection) q.execute();
			Iterator iter = coll.iterator();
			if (iter.hasNext()) {
				Object obj = iter.next();
				return obj;
			}
		} finally {
			q.closeAll();
		}
		return null;
	}

	protected static class BeanFactory implements org.ms123.common.datamapper.BeanFactory{
		private SessionContext m_sc;
		private Map m_settings;
		private List<Map> m_defaults;
		public BeanFactory(SessionContext sc, Map settings){
			m_sc = sc;
			m_settings = settings;
			m_defaults = (List)settings.get("defaults");
		}
		public Object create(Class clazz){
			try{
				System.out.println("BeanFactory:"+clazz);
				Object bean = clazz.newInstance();
				if( m_defaults != null){
					Map defaults = getDefaults(clazz.getSimpleName(),m_defaults);
					if (defaults != null) {
						m_sc.populate(defaults, bean);
					}
				}
				return bean;
			}catch(Exception e){
				throw new RuntimeException("BeanFactory.create", e);
			}
		}
	}

	private static Map<String, Object> getDefaults(String entityName, List<Map> defaults) {
		entityName = m_inflector.getEntityName(entityName);
		for (Map m : defaults) {
			if (entityName.toLowerCase().equals((m_inflector.getEntityName((String) m.get("name"))).toLowerCase())) {
				return (Map) m.get("content");
			}
		}
		return null;
	}
	private String getBaseName(String name) {
		if (name == null || name.trim().equals(""))
			return null;
		int lindex = name.lastIndexOf(".");
		if (lindex == -1)
			return name;
		return name.substring(lindex + 1).toLowerCase();
	}

	private boolean isString(Class c, String fieldName){
		try{
			Field f = c.getDeclaredField(fieldName);
			return f.getType().isAssignableFrom(String.class);
		}catch(Exception e){
			return false;
		}
	}

	private String getPrimaryKey(Class clazz) throws Exception {
		System.out.print("----->getPrimaryKey.clazz:" + clazz +" -> ");
		Field[] fields = clazz.getDeclaredFields();
		for (int i = 0; i < fields.length; i++) {
			java.lang.annotation.Annotation[] anns = fields[i].getDeclaredAnnotations();
			for (int j = 0; j < anns.length; j++) {
				try {
					Class atype = anns[j].annotationType();
					if (!(anns[j] instanceof javax.jdo.annotations.PrimaryKey)) {
						continue;
					}
					System.out.println(fields[i].getName());
					return fields[i].getName();
				} catch (Exception e) {
					System.out.println("getPrimaryKey.e:" + e);
				}
			}
		}
		throw new RuntimeException("JdoLayerImpl.getPrimaryKey("+clazz+"):no_primary_key");
	}

	protected byte[] convertToUTF8(byte bytes[] ) throws Exception{
		String sch = detectCharset(bytes);
		if( "UTF-8".equals(sch)) return bytes;

		CharsetDecoder chDecoder = Charset.forName(sch).newDecoder();  
		ByteBuffer bbuf = ByteBuffer.wrap(bytes);  
		CharBuffer cbuf = chDecoder.decode(bbuf);  

		CharsetEncoder utf8encoder = Charset.forName("UTF-8").newEncoder();  
		return utf8encoder.encode(cbuf).array();
	}

	protected synchronized String detectCharset(byte[] content) throws Exception {
		if( content == null || content.length==0) return null;
		ByteArrayInputStream bis = new ByteArrayInputStream(content);
		try {
			org.apache.tika.parser.txt.UniversalEncodingDetector ued = new org.apache.tika.parser.txt.UniversalEncodingDetector();
			Charset charset = ued.detect(bis,new org.apache.tika.metadata.Metadata());
			System.out.println("detectCharset:" + charset);
			return charset.toString();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			bis.close();
		}
		return "unknown";
	}

	protected synchronized String detectFileType(byte[] content) throws Exception {
		if( content == null || content.length==0) return null;
		ByteArrayInputStream bis = new ByteArrayInputStream(content);
		try {
			Tika tika = new Tika();
			String ftype = tika.detect(bis);
			System.out.println("getFileModel.ftype:" + ftype);
			return ftype;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			bis.close();
		}
		return "unknown";
	}

	private synchronized String expandString(GroovyShell shell, String str, Map<String,String> vars) {
		String newString = "";
		int openBrackets = 0;
		int first = 0;
		for (int i = 0; i < str.length(); i++) {
			if (i < str.length() - 2 && str.substring(i, i + 2).compareTo("${") == 0) {
				if (openBrackets == 0) {
					first = i + 2;
				}
				openBrackets++;
			} else if (str.charAt(i) == '}' && openBrackets > 0) {
				openBrackets -= 1;
				if (openBrackets == 0) {
					newString += eval(shell, str.substring(first, i), vars);
				}
			} else if (openBrackets == 0) {
				newString += str.charAt(i);
			}
		}
		return newString;
	}

	private Object eval(GroovyShell shell,String expr, Map<String,String> vars) {
		try {
			Script script = shell.parse(expr);
			Binding binding = new Binding(vars);
			script.setBinding(binding);
			return script.run();
		} catch (Throwable e) {
			e.printStackTrace();
			String msg = Utils.formatGroovyException(e, expr);
			throw new RuntimeException(msg);
		}
	}

	private boolean isEmpty(String s) {
		return (s == null || "".equals(s.trim()));
	}


	/*OLDIMPORT*/
	public List _getImportings(StoreDesc sdesc, String prefix, Map mapping) throws Exception {
		Map filtersMap = new HashMap();
		Map field1 = new HashMap();
		field1.put("field", IMPORTING_ID);
		field1.put("op", "bw");
		if( prefix == null ) prefix = "alluser/";
		field1.put("data", prefix);
		field1.put("connector", null);
		field1.put("children", new ArrayList());
		List fieldList = new ArrayList();
		fieldList.add(field1);
		filtersMap.put("children", fieldList);
		Map data = new HashMap();
		data.put("filter", filtersMap);
		List fields = new ArrayList();
		fields.add(IMPORTING_ID);
		fields.add(DESCRIPTION);
		fields.add(JSON_BODY);
		data.put("fields", fields);
		Map result = m_dataLayer.query(data, sdesc, IMPORTING_ENTITY);
		List<Map> resultList = (List) result.get("rows");
		for (int i = 0; i < resultList.size(); i++) {
			Map m = resultList.get(i);
			m.put(SETTINGS, m_ds.deserialize((String) m.get(JSON_BODY)));
			m.remove(JSON_BODY);
		}
		return m_utilsService.listToList(resultList, mapping, null);
	}


	public Map getFileModel(byte[] content, Map sourceSetup) throws Exception {
		String ftype = detectFileType(content);
		if( ftype == null ) return null;
		if ("text/plain".equals(ftype)) {
			return getCsvModel(content,ftype, sourceSetup);
		} else {
			return getXmlModel(content);
		}
	}

	private  Map getXmlModel(byte[] content) throws Exception {
		System.out.println("ImportingServiceImpl.activate:" + m_smooksFactory.createInstance());
		Smooks smooks = m_smooksFactory.createInstance();
		try {
			XmlTreeBuilder xtb = new XmlTreeBuilder();
			smooks.addVisitor(xtb, "*");
			ExecutionContext executionContext = smooks.createExecutionContext();
			ByteArrayInputStream is = new ByteArrayInputStream(content);
			JavaResult result = new JavaResult();
			smooks.filterSource(executionContext, new StreamSource(is), result);
			System.out.println("Smooks:" + xtb);
			return xtb.getTreeMap();
		} finally {
			smooks.close();
		}
	}

	private Map getCsvModel(byte[] content, String ftype, Map sourceSetup) throws Exception {
		JSONDeserializer ds = new JSONDeserializer();
		try {
			ByteArrayInputStream is = new ByteArrayInputStream(content);
			CSVParse parser = getCSVParser(is, sourceSetup);
			String[] line = parser.getLine();
			Map rootmap = new HashMap();
			rootmap.put("value", "csv-record");
			rootmap.put("title", "csv-record");
			rootmap.put("type", "element");
			List<Map> childs = new ArrayList();
			rootmap.put("children", childs);
			for (int i = 0; i < line.length; i++) {
				Map map = new HashMap();
				childs.add(map);
				map.put("value", line[i]);
				map.put("title", line[i]);
				map.put("type", "element");
				map.put("children", new ArrayList());
			}
			System.out.println("getCsvModel:" + rootmap);
			return rootmap;
		} finally {
		}
	}
	protected  Map doImport(StoreDesc data_sdesc, Map settings, byte[] content, boolean withoutSave, int max) throws Exception {
		List<Map> mappings = null;
		List<Map> defaults = null;
		Map sourceSetup = null;
		String mainEntity = null;
		Smooks smooks = m_smooksFactory.createInstance();
		try {
			mappings = getListParameter(settings, MAPPING, false);
			defaults = getListParameter(settings, DEFAULTS, true);
			sourceSetup = getMapParameter(settings, SOURCE_SETUP, false);
			mainEntity = getStringParameter(settings, MAIN_ENTITY, false);
			SessionContext sessionContext = m_dataLayer.getSessionContext(data_sdesc);
			UserTransaction ut = sessionContext.getUserTransaction();
			try {
				String ftype = detectFileType(content);
				if( ftype == null){
					throw new RuntimeException("BaseImportingServiceImpl.doImport:no Filecontent");
				}

				boolean isCsv = false;
				if ("text/plain".equals(ftype)) {
					isCsv = true;
				}
				Collection<Object> resultList = null;
				if (isCsv) {
					Map result = csvImport(sessionContext, mappings, defaults, sourceSetup, mainEntity, content);
					resultList = (Set) result.get("result");
				} else {
					Map entityTree = m_entityService.getEntityTree(data_sdesc, mainEntity, 3, null, null, true);
					XmlImporter xim = new XmlImporter();
					xim.setUserName(getUserName());
					xim.setModuleTree(entityTree);
					xim.setMax(max);
					xim.setDefaults(defaults);
					xim.setMainEntityName(mainEntity);
					xim.setSessionContext(sessionContext);
					Map<String, String> shortestMapping = getShortestMapping(mappings);
					if (shortestMapping == null) {
						throw new RuntimeException("ImportingServiceImpl.xmlImport:invalid mapping");
					}
					removeSelectorPrefixFromMappings(mappings, shortestMapping.get("source"));
					xim.setMappings(mappings);
					String target = shortestMapping.get("target");
					if (!m_inflector.getClassName(target).equals(m_inflector.getClassName(mainEntity))) {
						throw new RuntimeException("ImportingServiceImpl.xmlImport:wrong main mapping:" + shortestMapping + "/mainEntity:" + mainEntity);
					}
					System.out.println("shortest_mapping:" + shortestMapping);
					smooks.addVisitor(xim, shortestMapping.get("source"));
					ExecutionContext executionContext = smooks.createExecutionContext();
					ByteArrayInputStream is = new ByteArrayInputStream(content);
					JavaResult result = new JavaResult();
					smooks.filterSource(executionContext, new StreamSource(is), result);
					resultList = xim.getResultList();
				}
				List<Map> retList = new ArrayList();
				int num = 0;
				if( resultList.size() > 0){
					String pk = getPrimaryKey(resultList.iterator().next().getClass());
					sessionContext.setPrimaryKey(pk);
				}
				for (Object o : resultList) {
					if (max != -1 && num >= max)
						break;
					Map m = SojoObjectFilter.getObjectGraph(o, sessionContext,2);
					retList.add(m);
					List cv = sessionContext.validateObject(m, mainEntity);
					if (cv == null && m.get("_duplicated_id_") == null) {
						if (!withoutSave) {
							ut.begin();
							m_dataLayer.makePersistent(sessionContext, o);
							ut.commit();
						}
					} else {
						m.put("constraintViolations", cv);
					}
					num++;
					if ((num % 1000) == 0) {
						System.out.println("commit1++++++++:" + num);
					}
				}
				String[] dbfields = getFieldsFromMapping(mappings);
				Map ret = new HashMap();
				ret.put("fields", dbfields);
				ret.put("result", retList);
				return ret;
			} catch (Throwable e) {
				e.printStackTrace();
				sessionContext.handleException(ut, e);
			} finally {
				sessionContext.handleFinally(ut);
			}
		} finally {
			smooks.close();
		}
		return new HashMap();
	}

	protected  Map csvImport(SessionContext sc,List<Map> _mappings,List<Map> defaults, Map options, String mainEntity,  byte[] content) throws Exception {
		String entity = m_inflector.getEntityName((String) mainEntity);
		Class clazz =  sc.getClass(m_inflector.getClassName(mainEntity));
		Map<String, Object> defaultsMap = null;
		if (defaults != null && defaults.size() > 0) {
			defaultsMap = (Map) defaults.get(0).get("content");
		}
		List<Map<String, String>> mappings = new ArrayList();
		for (Map<String, String> mapping : _mappings) {
			String source = mapping.get("source");
			String target = mapping.get("target");
			String targetType = mapping.get("targetType");
			if (!("list".equals(targetType) || "object".equals(targetType))) {
				Map<String, String> mappingNew = new HashMap();
				mappingNew.put("source", getLastElement(source, "/"));
				mappingNew.put("target", getLastElement(target, "."));
				mappings.add(mappingNew);
			}
		}
System.out.println("Mapping:"+mappings);
		ByteArrayInputStream is = new ByteArrayInputStream(content);
		CSVParse parser = getCSVParser(is, options);
		LabeledCSVParser lparser = new LabeledCSVParser(parser);
		boolean withHeader = getBoolean(options, "header", false);
		System.out.println("withHeader:" + withHeader);
		if (withHeader) {
		}
		Set resultList = new HashSet();
		while (true) {
			String row[] = lparser.getLine();
			if (row == null) {
				break;
			}
			Object o = clazz.newInstance();
			Map rowMap = new HashMap();
			if (defaultsMap != null) {
				sc.populate(defaultsMap, o);
			}
			for (Map<String, String> mapping : mappings) {
				String source = mapping.get("source");
				String target = mapping.get("target");
				String value = lparser.getValueByLabel(source);
				rowMap.put(target, value);
			}
			rowMap.put("_isnew", true);
			rowMap.put("_user", getUserName());
			sc.evaluteFormulas(entity, rowMap);
			//sc.populate(rowMap, o);  
			m_objUtils.makeComplex(rowMap, o);
			resultList.add(o);
		}
		Map ret = new HashMap();
		ret.put("result", resultList);
		return ret;
	}
	protected String getUserName() {
		return org.ms123.common.system.ThreadContext.getThreadContext().getUserName();
	}
	private List getListParameter(Map map, String key, boolean optional) {
		Object o = map.get(key);
		if (o instanceof String) {
			JSONDeserializer ds = new JSONDeserializer();
			return (List) ds.deserialize((String) o);
		}
		if (o instanceof List) {
			return (List) o;
		}
		if (!optional) {
			throw new RuntimeException("ImportingServiceImpl.wrong or missing parameter:" + key);
		}
		return null;
	}

	private Map getMapParameter(Map map, String key, boolean optional) {
		Object o = map.get(key);
		if (o instanceof String) {
			JSONDeserializer ds = new JSONDeserializer();
			return (Map) ds.deserialize((String) o);
		}
		if (o instanceof Map) {
			return (Map) o;
		}
		if (!optional) {
			throw new RuntimeException("ImportingServiceImpl.wrong or missing parameter:" + key);
		}
		return null;
	}

	private String getStringParameter(Map map, String key, boolean optional) {
		Object o = map.get(key);
		if (o instanceof String) {
			return ((String) o);
		}
		if (!optional) {
			throw new RuntimeException("ImportingServiceImpl.wrong or missing parameter:" + key);
		}
		return null;
	}

	private CSVParse getCSVParser(InputStream is, Map options) {
		char delimeter = ',';
		String columnDelim = (String) options.get("columnDelim");
		System.out.println("options:" + options);
		if (columnDelim == null) {
			throw new RuntimeException("ImportingServiceImpl.getCSVParser:not initialized");
		}
		if (columnDelim.toLowerCase().indexOf("tab") != -1) {
			delimeter = '\t';
		}
		if (columnDelim.length() > 0) {
			delimeter = columnDelim.charAt(0);
		}
		char quote = '"';
		if (((String) options.get("quote")).length() > 0) {
			quote = ((String) options.get("quote")).charAt(0);
		}
		CSVParse p;
		if (options.get("excel") != null) {
			p = new ExcelCSVParser(is, delimeter);
		} else {
			p = new CSVParser(is, delimeter);
		}
		p.changeQuote(quote);
		return p;
	}

	private String getLastElement(String path) {
		return getLastElement(path, ".");
	}

	private String getLastElement(String path, String sep) {
		int lastDot = path.lastIndexOf(sep);
		return path.substring(lastDot + 1);
	}

	private String[] getFieldsFromMapping(List<Map> mappings) {
		List<String> fields = new ArrayList<String>();
		for (Map<String, String> mapping : mappings) {
			String source = mapping.get("source");
			String target = mapping.get("target");
			String targetType = mapping.get("targetType");
			if (!("list".equals(targetType) || "object".equals(targetType))) {
				if (StringUtils.countMatches(target, ".") == 1) {
					fields.add(getLastElement(target, "."));
				}
			}
		}
		System.out.println("getFieldsFromMapping2:" + fields);
		String sa[] = new String[fields.size()];
		return fields.toArray(sa);
	}

	private void removeSelectorPrefixFromMappings(List<Map> mappings, String prefix) {
		Map<String, String> shortest_mapping = null;
		int plen = prefix.length();
		for (Map<String, String> mapping : mappings) {
			String source = mapping.get("source");
			if ((plen + 1) < source.length()) {
				mapping.put("source", source.substring(plen + 1));
			}
		}
	}

	private Map<String, String> getShortestMapping(List<Map> mappings) {
		Map<String, String> shortest_mapping = null;
		for (Map<String, String> mapping : mappings) {
			String targetType = mapping.get("targetType");
			if ("object".equals(targetType)) {
				if (shortest_mapping == null) {
					shortest_mapping = mapping;
				} else {
					String source = mapping.get("source");
					String shortest_source = shortest_mapping.get("source");
					if (source.length() < shortest_source.length()) {
						shortest_mapping = mapping;
					}
				}
			}
		}
		return shortest_mapping;
	}
	private boolean getBoolean(Map map, String key, boolean def) {
		try {
			return ((Boolean) map.get(key)).booleanValue();
		} catch (Exception e) {
			return def;
		}
	}

	/*OLDIMPORT END*/

}
