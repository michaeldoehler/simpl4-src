/*
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
package org.ms123.common.bhs;

import flexjson.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.ms123.common.data.api.DataLayer;
import org.ms123.common.auth.api.AuthService;
import org.ms123.common.utils.UtilsService;
import org.ms123.common.git.GitService;
import org.ms123.common.data.api.SessionContext;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.nucleus.api.NucleusService;
import org.ms123.common.permission.api.PermissionService;
import org.ms123.common.libhelper.Inflector;

import com.Ostermiller.util.*;
import javax.transaction.RollbackException;
import javax.transaction.UserTransaction;
import javax.transaction.Status;
import javax.jdo.Query;
import javax.jdo.Transaction;
import javax.jdo.Extent;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.*;
import javax.transaction.UserTransaction;
import org.apache.commons.beanutils.PropertyUtils;
/**
 *
 */
@groovy.transform.CompileStatic
@groovy.transform.TypeChecked
abstract class BaseBhsServiceImpl implements Constants,BhsService {

	private List<String> m_fields;

	protected Inflector m_inflector = Inflector.getInstance();

	protected DataLayer m_dataLayer;

	protected AuthService m_authService;

	protected GitService m_gitService;

	protected PermissionService m_permissionService;

	protected NucleusService m_nucleusService;

	protected UtilsService m_utilsService;
	private int m_mach=0;
	private int m_lines=0;

	protected Map _importBom(String storeId, String aukwnr) throws Exception{
		StoreDesc sdesc = StoreDesc.get(storeId);
		PersistenceManager pm = m_nucleusService.getPersistenceManagerFactory(sdesc).getPersistenceManager();
		try{
			Class _b = m_nucleusService.getClass(sdesc, "bom");
			if( aukwnr != null){
				importOneBom(pm, storeId, aukwnr);
			}else{
				List<String> al = readMachFile(storeId);
				for(String a : al){
					Object m = getObject(pm, _b, trim(a));
					if( m==null){
						importOneBom(pm,storeId, a);
					}else{
						System.out.println("Masch:"+a);
					}
				}
			}
		} finally {
			try{
				pm.close();
			}catch(Exception x){}
		}
	}
	protected Map importOneBom(PersistenceManager pm, String storeId, String aukwnr) throws Exception{
		StoreDesc sdesc = StoreDesc.get(storeId);
		UserTransaction ut = m_nucleusService.getUserTransaction();
		String currentpath = null;
		try {

			Map<String,Object> cache = new HashMap();
			String prefix = aukwnr.substring(0,2);
			File f = new File("/opt/sw/gitrepos/"+storeId +"/import/bom/"+prefix+"/"+aukwnr);
			info("aukwnr:"+aukwnr+"/"+m_mach+++"/"+m_lines);

			Class _md = m_nucleusService.getClass(sdesc, "masterdata");
			CSVParse p = new ExcelCSVParser(new FileInputStream(f));
			int num = 0;
			String[] line = null;
			while ((line = p.getLine()) != null) {
				if (ut.getStatus() != Status.STATUS_ACTIVE) {
					ut.begin();
				}
				List<String> paList = getPathList(line);
				String dot="";
				String path = "";
				int elems = line.length;
				String _qty = line[line.length-1];
				for( int i=0; i< paList.size(); i++ ){
					String part = paList.get(i);
					part = part.replaceAll("[^0-9a-zA-Z]","_");

					String qty = null;
					if( i == paList.size()-1)
						qty = _qty;

					path += dot + part;
					dot=".";
					Object b = cache.get(path);
					currentpath = path;
					if( b == null){
						b =  m_nucleusService.getClass(sdesc, "Bom").newInstance();
						PropertyUtils.setProperty(b, "path", path);
						PropertyUtils.setProperty(b, "part", trim(part));
						PropertyUtils.setProperty(b, "qty", getDouble(qty));
						setMD(pm,_md,b,path,part);
						cache.put(path, b);
					}
					if( path.indexOf(".") != -1){
						Object parent = cache.get(getParentPath(path));
						if( parent == null){
							info("parent is null:"+path+"/"+getParentPath(path));
							break;
						}
						Collection l = (Collection) PropertyUtils.getProperty(parent, "children");
						if( l == null){
							l = new HashSet();
							try{
								PropertyUtils.setProperty(parent, "children", l);
							}catch(Exception x){
								info("Path:"+path);
								info("Ex:"+x);
								x.printStackTrace();
							}
						}
						l.add(b);
					}
					pm.makePersistent(b);
				}

				if ((num % 10000) == 1) {
					//System.out.println(num + ":\t" + new Date().getTime());
					ut.commit();
				}
				num++;
			}
			if (ut.getStatus() == Status.STATUS_ACTIVE) {
				ut.commit();
			}
			m_lines +=num;
		} catch (Exception e) {
			info("CurrentPath:"+currentpath);
			if (ut.getStatus() == Status.STATUS_ACTIVE) {
				ut.rollback();
			}
			throw new RuntimeException(e);
		} finally {
			try{
				//pm.close();
			}catch(Exception x){}
		}
		return null;
	}


	private Map m_mdCache = new HashMap();

	private void setMD(PersistenceManager pm, Class _md, Object b, String path, String part)throws Exception{
		if( path.indexOf(".")==-1){
			part = "#"+part;
		}	
		part = part.replaceAll("[_]",".").trim();
		Object t = m_mdCache.get(part);
		if(t== null){
			t = getObject(pm, _md, part);
			if (t == null) {
				System.out.println("No TTS:"+part+"|");
				return;
			}
			m_mdCache.put(part, t);
		}
		PropertyUtils.setProperty(b, "masterdata", t);
	}

	protected Map _importMD(String storeId) throws Exception{
		StoreDesc sdesc = StoreDesc.get(storeId);
		PersistenceManager pm = m_nucleusService.getPersistenceManagerFactory(sdesc).getPersistenceManager();
		UserTransaction ut = m_nucleusService.getUserTransaction();
		try {

			Class _bom = m_nucleusService.getClass(sdesc, "Bom");
			File f = new File("/opt/sw/gitrepos/"+storeId +"/import/tsspf.csv");
			CSVParse p = new ExcelCSVParser(new FileInputStream(f));
			LabeledCSVParser lp = new LabeledCSVParser(getCSVParser(new FileInputStream(f)));
			int num = 0;
			while (lp.getLine() != null) {
				if (ut.getStatus() != Status.STATUS_ACTIVE) {
					ut.begin();
				}
				Object md =  m_nucleusService.getClass(sdesc, "Masterdata").newInstance();

				String part = trim(lp.getValueByLabel("ARTNR"));
				String name = trim(lp.getValueByLabel("ARTBZ"));
				String name2 = trim(lp.getValueByLabel("ARTB2"));
				String invba = trim(lp.getValueByLabel("INVBA"));
				String dispo = trim(lp.getValueByLabel("DISPO"));
				PropertyUtils.setProperty(md, "part", part);
				PropertyUtils.setProperty(md, "name", name);
				PropertyUtils.setProperty(md, "name2", name2);
				PropertyUtils.setProperty(md, "invba", invba);
				PropertyUtils.setProperty(md, "dispo", dispo);

				pm.makePersistent(md);
				if ((num % 100000) == 1) {
					System.out.println(num);
					ut.commit();
				}
				num++;
			}
			if (ut.getStatus() == Status.STATUS_ACTIVE) {
				ut.commit();
			}
			m_lines +=num;
		} catch (Exception e) {
			if (ut.getStatus() == Status.STATUS_ACTIVE) {
				ut.rollback();
			}
			throw new RuntimeException(e);
		} finally {
			try{
				pm.close();
			}catch(Exception x){}
		}
		return null;
	}

	private Map<String,Map> m_trCache = new HashMap();
	protected void _setTranslations(String storeId) {

		StoreDesc sdesc = StoreDesc.get(storeId);
		Class _md =  m_nucleusService.getClass(sdesc, "Masterdata");
		Class _tr =  m_nucleusService.getClass(sdesc, "Translation");
		//readUebFile(storeId);
		System.out.println("UEB gelesen");
		PersistenceManager pm = m_nucleusService.getPersistenceManagerFactory(sdesc).getPersistenceManager();
		UserTransaction ut = m_nucleusService.getUserTransaction();
		int num = 0;
		int fromCache=0;
		int newEntry=0;
		try {
			int chunk = 100000;
			int from=0,to=chunk;
			while(true){
				ut.begin();
				Extent e = pm.getExtent(_md, true);
				Query q = pm.newQuery(e);
				q.addExtension("datanucleus.rdbms.query.resultSetType", "scroll-insensitive");
				q.addExtension("datanucleus.query.resultCacheType", "none");
				System.out.println("--> doing from:"+from+" -> "+ to );
				q.setRange(from, to);
				Collection coll = (Collection) q.execute();
				Iterator iter = coll.iterator();
				int c=0;
				while (iter.hasNext()) {
					Object md = iter.next();
					Map m = (Map)PropertyUtils.getProperty(md, "translation_list");
					if( m==null || m.size()== 0){
						String name = PropertyUtils.getProperty(md, "name");

						Map tr = m_trCache.get(name);
						if( tr != null){
							tr = new HashMap(tr);
							fromCache++;
						}else{
							Collection col = getTrList(pm, _tr, name);
							tr = new HashMap();
							for( Object t : col ){
								String lang = PropertyUtils.getProperty(t, "lang");
								tr.put( lang, t );
							}
							m_trCache.put(name,tr );
							if( col.size() >0) newEntry++;
						}
						PropertyUtils.setProperty(md, "translation_list", tr);
					}
					if ((num++ % 100) == 1) {
						System.err.print(num);
						System.err.print("\r");
					}
					c++;
				} 
				ut.commit();
				System.out.println("<<<finished from:"+from+" -> "+ c +"/fromCache:"+fromCache+"/newEntry:"+newEntry);
				if( (from+c) < to){
					break;
				}
				to += chunk;
				from += chunk;
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	private Collection getTrList(PersistenceManager pm, Class clazz, String msgid) {
		try{
			List ret = new ArrayList();
			Extent e = pm.getExtent(clazz, true);
			Query q = pm.newQuery(e, "msgid == '" + trim(msgid) + "'");
			Collection coll = (Collection) q.execute();
			if( coll.size() == 0){
				//System.out.println("Msgid:"+trim(msgid));
			}
			return coll;
		}catch(Exception e){
			System.out.println("Msgid:"+msgid);
			return new ArrayList();
		}
	}

	private Object getObject(PersistenceManager pm, Class clazz, String part) {
		String filter = "part == '" + part + "'";
		if( part.startsWith("#")){
			filter += " || part == '" + part.substring(1) + "'";
		}
		try{
			Extent e = pm.getExtent(clazz, true);
			Query q = pm.newQuery(e, filter);
			Collection coll = (Collection) q.execute();
			Iterator iter = coll.iterator();
			if (iter.hasNext()) {
				Object c = iter.next();
				return c;
			} else {
				//System.out.println("number(" + nummer + ") not found");
			}
			return null;
		}catch(Exception e){
			System.out.println("getObject(" + part + ") not found");
		}
	}

	private List<String> getPathList(String[] line){
		List<String> paList = new ArrayList();
		paList.add( line[0]);
	  if( isEmpty(line[2]) ){
			paList.add( line[3]);
			paList.add( line[5]);
			paList.add( line[6]);
		}else{
			paList.add( line[2]);
			paList.add( line[4]);
			paList.add( line[5]);
			paList.add( line[6]);
		} 
		return paList;
	}
	protected List<String> readMachFile(String storeId) throws Exception {
		File f = new File("/opt/sw/gitrepos/"+storeId +"/import/bom/mach.csv");
		List<String> retList = new ArrayList();
		try {
			LabeledCSVParser lp = new LabeledCSVParser(new ExcelCSVParser(new FileInputStream(f)));
			while (lp.getLine() != null) {
				String a = lp.getValueByLabel("AUGWNR");
				if( a!= null && !a.trim().equals("")){
					retList.add(a.trim());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}
		return retList;
	}

	protected void readUebFile(String storeId) throws Exception {
		StoreDesc sdesc = StoreDesc.get(storeId);
		PersistenceManager pm = m_nucleusService.getPersistenceManagerFactory(sdesc).getPersistenceManager();
		UserTransaction ut = m_nucleusService.getUserTransaction();
		Class _tr =  m_nucleusService.getClass(sdesc, "Translation");
		int num = 0;
		File f = new File("/opt/sw/gitrepos/"+storeId +"/import/uebsepf.csv");
		try {
			LabeledCSVParser lp = new LabeledCSVParser(new ExcelCSVParser(new FileInputStream(f)));
			ut.begin();
			while (lp.getLine() != null) {

				String lang = lp.getValueByLabel("SPRACH");
				String msgid = lp.getValueByLabel("ARTBZS");
				String msgid2 = lp.getValueByLabel("BEZD");
				String msg = lp.getValueByLabel("BEZUEB");

				if( isEmpty(lang) || isEmpty(msgid) || isEmpty(msg)){
					continue;
				}

				Object t = _tr.newInstance();
				PropertyUtils.setProperty(t, "message", trim(msg));
				PropertyUtils.setProperty(t, "lang", trim(lang));
				PropertyUtils.setProperty(t, "msgid", trim(msgid));
				pm.makePersistent(t);

			}
			ut.commit();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}
	}

	private CSVParse getCSVParser(InputStream is) {
		char delimeter = ',';
		char quote = '"';
		CSVParse p = new CSVParser(is, delimeter);
		p.changeQuote(quote);
		return p;
	}

	private String getParentPath(String p){
		int last = p.lastIndexOf(".");
		return p.substring(0,last);
	}

	private boolean isEmpty(String s){
		if(s==null || s.trim().equals("")) return true;
		return false;
	}
	private String trim(String s){
		if(s==null ) return s;
		return s.trim();
	}
	private Double getDouble(String s){
		if(s==null ) return null;
		try{
			return new Double(s);
		}catch(Exception e){
			return null;
		}
	}

	protected void info(String msg) {
		System.out.println(msg);
	}
}
