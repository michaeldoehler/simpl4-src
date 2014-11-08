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
package org.ms123.common.data.lucene;

import java.util.*;
import java.io.*;
import org.apache.lucene.search.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.index.*;
import org.apache.lucene.util.Version;
import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.standard.*;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.AbstractField;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.NumericField;
import org.apache.lucene.queryParser.*;
import org.ms123.common.store.StoreDesc;
// import org.slf4j.Logger; 
// import org.slf4j.LoggerFactory; 
import org.apache.commons.beanutils.*;
import org.ms123.common.utils.*;
import org.ms123.common.data.api.DataLayer;
import org.ms123.common.data.api.SessionContext;
import org.ms123.common.data.api.LuceneSession;
import org.ms123.common.setting.api.SettingService;
import aQute.bnd.annotation.metatype.*;
import aQute.bnd.annotation.component.*;
import static org.apache.commons.io.FileUtils.forceDelete;
import org.ms123.common.rpc.PDefaultBool;
import org.ms123.common.rpc.PDefaultFloat;
import org.ms123.common.rpc.PDefaultInt;
import org.ms123.common.rpc.PDefaultLong;
import org.ms123.common.rpc.PDefaultString;
import org.ms123.common.rpc.PName;
import org.ms123.common.rpc.POptional;
import org.ms123.common.rpc.RpcException;
import static org.ms123.common.rpc.JsonRpcServlet.ERROR_FROM_METHOD;
import static org.ms123.common.rpc.JsonRpcServlet.INTERNAL_SERVER_ERROR;
import static org.ms123.common.rpc.JsonRpcServlet.PERMISSION_DENIED;

@SuppressWarnings({"unchecked","deprecation"})
@Component(enabled = true, configurationPolicy = ConfigurationPolicy.optional, immediate = true, properties = { "rpc.prefix=lucene" })
public class LuceneServiceImpl implements org.ms123.common.data.api.LuceneService {

	// private static final Logger m_logger = LoggerFactory.getLogger(LuceneServiceImpl.class); 
	protected Inflector m_inflector = Inflector.getInstance();

	private Map<String, IndexWriter> m_indexWriters = new HashMap();

	private Map<String, IndexReader> m_indexReaders = new HashMap();

	private SettingService m_settingService;

	private DataLayer m_dataLayer;

	private final String ENTITY = "entity";

	private int MAX = 100;

	public void activate() {
		System.out.println("LuceneServiceImpl.activate");
	}

	public void deactivate() {
		System.out.println("LuceneServiceImpl.deactivate");
	}

	public Map query(
			@PName("namespace")        String namespace, 
			@PName("query")            String query) throws RpcException {
		try {
			return _doQuery(namespace, query);
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "LuceneService.query:", e);
		}
	}

	public void removeIndex(
			@PName("namespace")        String namespace) throws RpcException {
		try {
			File luceneDir = new File(System.getProperty("workspace") + "/lucene", namespace);
			if (luceneDir.exists()) {
				for (IndexWriter iw : m_indexWriters.values()) {
					iw.close();
				}
				for (IndexReader ir : m_indexReaders.values()) {
					ir.close();
				}
				forceDelete(luceneDir);
				m_indexWriters = new HashMap();
				m_indexReaders = new HashMap();
			}
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "LuceneService.removeIndex:", e);
		}
	}

	public void createIndex(
			@PName("namespace")        String namespace, 
			@PName("entityList")       List<String> entityList) throws RpcException {
		try {
			_createIndex(namespace, entityList);
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "LuceneService.createIndex:", e);
		}
	}

	private void _createIndex(String namespace, List<String> entityList) throws Exception {
		for (String e : entityList) {
			_createIndex(namespace, e);
		}
	}

	private void _createIndex(String namespace, String entityName) throws Exception {
		StoreDesc sdesc = StoreDesc.getNamespaceData(namespace);
		SessionContext sc = m_dataLayer.getSessionContext(sdesc);
		String className = m_inflector.getClassName(entityName);
		String plural = m_inflector.pluralize(entityName).toLowerCase();
		LuceneSession ls = createSession(sdesc);
		String sql = "Select " + plural + " from " + className + " " + plural;
		javax.jdo.Query q = sc.getPM().newQuery("javax.jdo.query.JPQL", sql);
		q.declareImports(sdesc.getImports());
		List list = (List) q.execute();
		Iterator itr = list.iterator();
		int counter = 0;
		while (itr.hasNext()) {
			Object row = (Object) itr.next();
			addToIndex(ls, row);
			counter++;
			if ((counter % 1000) == 0) {
				System.out.println("counter:" + counter);
			}
		}
		commit(ls);
	}

	private Map _doQuery(String namespace, String queryString) throws Exception {
		System.out.println("LuceneServiceImpl.query:" + queryString + "/" + namespace);
		try {
			IndexSearcher searcher = getIndexSearcher(namespace);
			StoreDesc sdesc = StoreDesc.getNamespaceData(namespace);
			StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_33);
			QueryParser parser = new QueryParser(Version.LUCENE_33, "fulltext", analyzer);
			parser.setAllowLeadingWildcard(true);
			parser.setDefaultOperator(QueryParser.AND_OPERATOR);
			Query query = parser.parse(queryString);
			System.out.println("queryParsed:" + query);
			TopScoreDocCollector collector = TopScoreDocCollector.create(100000, true);
			searcher.search(query, collector);
			ScoreDoc[] hits = collector.topDocs().scoreDocs;
			System.out.println("Hits:" + hits.length);
			Map<String, List> retMap = new HashMap();
			Map<String, List> viewMap = new HashMap();
			for (int i = 0; i < hits.length && i < MAX; i++) {
				Document hitDoc = searcher.doc(hits[i].doc);
				System.out.println(hitDoc.get("id") + ",module:" + hitDoc.get(ENTITY));
				String entity = hitDoc.get(ENTITY);
				List viewFields = viewMap.get(entity);
				if (viewFields == null) {
					//viewFields = m_settingService.getEntityViewConfigFields(p, sysMap);
					//@@@MS viewFields = m_settingService.getFieldsForEntityView(sdesc,entity,"global-search");
					viewFields = m_settingService.getFieldsForEntityView(sdesc.getNamespace(), entity, "global-search");
					viewMap.put(entity, viewFields);
				}
				List hitList = retMap.get(entity);
				if (hitList == null) {
					hitList = new ArrayList();
					retMap.put(entity, hitList);
				}
				fillHitList(hitDoc, hitList, viewFields);
			}
			System.out.println("retMap:" + retMap);
			return retMap;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("LuceneServiceImpl.queryIdList", e);
		}
	}

	private void fillHitList(Document hitDoc, List hitList, List<Map> viewFields) {
		Map fmap = new HashMap();
		fmap.put("id", hitDoc.get("id"));
		fmap.put(ENTITY, hitDoc.get(ENTITY));
		for (Map f : viewFields) {
			String fname = (String) f.get("name");
			String value = hitDoc.get(fname);
			fmap.put(fname, value);
		}
		hitList.add(fmap);
		System.out.println("hitList:" + hitList);
	}

	public List<Integer> queryIdList(String namespace, String entityName, String queryString) {
		List<Integer> idList = new ArrayList();
		try {
			IndexSearcher searcher = getIndexSearcher(namespace);
			StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_33);
			QueryParser parser = new QueryParser(Version.LUCENE_33, "fulltext", analyzer);
			parser.setAllowLeadingWildcard(true);
			parser.setDefaultOperator(QueryParser.AND_OPERATOR);
			Query query = parser.parse(queryString + " AND module:" + entityName);
			System.out.println("queryParsed:" + query);
			TopScoreDocCollector collector = TopScoreDocCollector.create(100000, true);
			searcher.search(query, collector);
			System.out.println("searcher.maxDocs:" + searcher.maxDoc());
			ScoreDoc[] hits = collector.topDocs().scoreDocs;
			System.out.println("Hits:" + hits.length);
			for (int i = 0; i < hits.length; i++) {
				Document hitDoc = searcher.doc(hits[i].doc);
				// getting actual document 
				System.out.println(hitDoc.get("id"));
				idList.add(Integer.parseInt(hitDoc.get("id")));
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("LuceneServiceImpl.queryIdList", e);
		}
		System.out.println("idList:" + idList);
		return idList;
	}

	public LuceneSession createSession(StoreDesc sdesc) {
		//IndexWriter iw = createTmpIndexWriter(); 
		LuceneSession ls = new LuceneSessionImpl(sdesc, this);
		return ls;
	}

	public synchronized void commit(LuceneSession session) {
		IndexWriter tiw = session.getIndexWriter();
		String namespace = session.getStoreDesc().getNamespace();
		try {
			IndexReader ir = IndexReader.open(tiw, true);
			IndexWriter riw = getRealIndexWriter(namespace);
			deleteExistingDocs(session, riw);
			riw.addIndexes(ir);
			riw.maybeMerge();
			riw.commit();
			tiw.getDirectory().close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void rollback(LuceneSession session) {
		IndexWriter tiw = session.getIndexWriter();
		try {
			tiw.getDirectory().close();
			tiw.close();
		} catch (Exception e) {
			//e.printStackTrace(); 
			System.out.println("LuceneService.rollback:" + e);
		}
	}

	public synchronized void addToIndex(LuceneSession session, Object obj) {
		IndexWriter iw = session.getIndexWriter();
		StoreDesc sdesc = session.getStoreDesc();
		String namespace = sdesc.getNamespace();
		try {
			String entityName = getEntityName(obj);
			Object id = getId(obj, session.getPrimaryKey());
			//System.out.println("addToIndex:"+id+"/iw:"+iw); 
			Map p = new HashMap();
			p.put("view", "global-search");
			p.put(ENTITY, entityName);
			p.put(StoreDesc.STORE_ID, sdesc.getStoreId());
			p.put(StoreDesc.PACK, sdesc.getPack());
			List<Map> searchFields = m_settingService.getFieldsForEntityView(sdesc.getNamespace(), entityName, "global-search");
			Document doc = new Document();
			Field field = new Field(ENTITY, entityName, Field.Store.YES, Field.Index.NOT_ANALYZED);
			doc.add(field);
			if (id instanceof Long) {
				NumericField nfield = new NumericField("id_numeric", Field.Store.YES, true);
				nfield = nfield.setLongValue((Long) id);
				doc.add(nfield);
			}
			field = new Field("id", id.toString(), Field.Store.YES, Field.Index.NOT_ANALYZED);
			doc.add(field);
			populate(obj, searchFields, doc);
			String[] _id = new String[2];
			_id[0] = id.toString();
			_id[1] = entityName;
			session.addId(_id);
			iw.addDocument(doc);
			iw.commit();
		} catch (Exception e) {
			try {
				iw.rollback();
			} catch (Exception x) {
			}
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public synchronized void deleteFromIndex(LuceneSession session, Object obj) {
		StoreDesc sdesc = session.getStoreDesc();
		String namespace = sdesc.getNamespace();
		IndexWriter iw = getRealIndexWriter(namespace);
		try {
			String entityName = getEntityName(obj);
			Object id = PropertyUtils.getProperty(obj, session.getPrimaryKey());
			Term term1 = new Term("id", id.toString());
			Term term2 = new Term(ENTITY, entityName);
			TermQuery query1 = new TermQuery(term1);
			TermQuery query2 = new TermQuery(term2);
			BooleanQuery and = new BooleanQuery();
			and.add(query1, BooleanClause.Occur.MUST);
			and.add(query2, BooleanClause.Occur.MUST);
			IndexSearcher is = getIndexSearcher(namespace, iw);
			TopDocs hits = is.search(and, 10);
			if (hits.scoreDocs.length == 0) {
				System.out.println("LuceneServiceImpl.deleteFromIndex:id(" + id + ") not found");
			} else if (hits.scoreDocs.length == 1) {
				System.out.println("LuceneServiceImpl.deleting:" + and);
				iw.deleteDocuments(and);
			} else if (hits.scoreDocs.length > 1) {
				throw new IllegalArgumentException("LuceneServiceImpl.delete:Term (" + and + ") matches more than 1 document in the index.");
			}
			iw.commit();
		} catch (Exception e) {
			try {
				iw.rollback();
			} catch (Exception x) {
			}
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private IndexWriter getRealIndexWriter(String namespace) {
		IndexWriter iw = m_indexWriters.get(namespace);
		if (iw == null) {
			iw = createRealIndexWriter(namespace);
			m_indexWriters.put(namespace, iw);
		}
		return iw;
	}

	private synchronized IndexSearcher getIndexSearcher(String namespace) {
		IndexWriter iw = getRealIndexWriter(namespace);
		return getIndexSearcher(namespace, iw);
	}

	private synchronized IndexSearcher getIndexSearcher(String namespace, IndexWriter iw) {
		IndexSearcher searcher = null;
		IndexReader ir = m_indexReaders.get(namespace);
		try {
			if (ir == null) {
				ir = IndexReader.open(iw, true);
				m_indexReaders.put(namespace, ir);
				searcher = new IndexSearcher(ir);
			} else {
				IndexReader newReader = IndexReader.openIfChanged(ir, true);
				System.out.println("newReader:" + newReader);
				if (newReader != null) {
					ir.close();
					ir = newReader;
					m_indexReaders.put(namespace, ir);
				}
				searcher = new IndexSearcher(ir);
			}
			return searcher;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("LuceneServiceImpl.createIndexSearcher", e);
		}
	}

	private IndexWriter createRealIndexWriter(String namespace) {
		try {
			IndexWriterConfig.OpenMode mode = IndexWriterConfig.OpenMode.CREATE;
			File luceneDir = new File(System.getProperty("workspace") + "/lucene", namespace);
			if (!luceneDir.exists()) {
				luceneDir.mkdirs();
			} else {
				mode = IndexWriterConfig.OpenMode.APPEND;
			}
			Directory dir = new SimpleFSDirectory(luceneDir);
			IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_33, new StandardAnalyzer(Version.LUCENE_33));
			iwc.setOpenMode(mode);
			IndexWriter iw = new IndexWriter(dir, iwc);
			return iw;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("LuceneServiceImpl.createRealIndexWriter", e);
		}
	}

	public IndexWriter createTmpIndexWriter() {
		try {
			IndexWriterConfig.OpenMode mode = IndexWriterConfig.OpenMode.CREATE;
			Directory dir = new RAMDirectory();
			IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_33, new StandardAnalyzer(Version.LUCENE_33));
			iwc.setOpenMode(mode);
			IndexWriter iw = new IndexWriter(dir, iwc);
			return iw;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("LuceneServiceImpl.createTmpIndexWriter", e);
		}
	}

	private synchronized void deleteExistingDocs(LuceneSession session, IndexWriter iw) throws Exception {
		List<String[]> idlist = session.getIdList();
		IndexSearcher is = getIndexSearcher(session.getStoreDesc().getNamespace(), iw);
		for (int i = 0; i < idlist.size(); i++) {
			String[] _id = idlist.get(i);
			Term term1 = new Term("id", _id[0]);
			Term term2 = new Term(ENTITY, _id[1]);
			TermQuery query1 = new TermQuery(term1);
			TermQuery query2 = new TermQuery(term2);
			BooleanQuery and = new BooleanQuery();
			and.add(query1, BooleanClause.Occur.MUST);
			and.add(query2, BooleanClause.Occur.MUST);
			TopDocs hits = is.search(and, 10);
			if (hits.scoreDocs.length == 1) {
				iw.deleteDocuments(and);
			} else if (hits.scoreDocs.length > 1) {
				throw new IllegalArgumentException("LuceneServiceImpl.add:Term (" + and + ") matches more than 1 document in the index.");
			}
		}
	}

	private Object getId(Object obj, String primaryKey) throws Exception {
		if (obj instanceof Map) {
			return ((Map) obj).get(primaryKey);
		} else {
			return PropertyUtils.getProperty(obj, primaryKey);
		}
	}

	private String getEntityName(Object obj) {
		if (obj instanceof Map) {
			return (String) ((Map) obj).get("__entityName");
		} else {
			String className = obj.getClass().getName();
			int dot = className.lastIndexOf(".");
			String entityName = m_inflector.getEntityName(className.substring(dot + 1));
			return entityName;
		}
	}

	private void populate(Object obj, List<Map> searchFields, Document doc) {
		Map inMap = null;
		if (obj instanceof Map) {
			inMap = (Map) obj;
		} else {
			inMap = new BeanMap(obj);
		}
		String fulltext = "fulltext";
		for (Map m : searchFields) {
			String name = (String) m.get("name");
			Object v = inMap.get(name);
			if (v == null) {
				continue;
			}
			AbstractField field = null;
			AbstractField ft_field = null;
			String dt = (String) m.get("datatype");
			if( "decimal".equals(dt)) dt = "double";
			if( "number".equals(dt)) dt = "integer";
			if ("date".equals(dt)) {
				Date date = (Date) inMap.get(name);
				String value = DateTools.dateToString(date, DateTools.Resolution.DAY);
				field = new Field(name, value, Field.Store.YES, Field.Index.NOT_ANALYZED);
				ft_field = new Field(fulltext, value, Field.Store.NO, Field.Index.NOT_ANALYZED);
			}
			if ("number".equals(dt) || "double".equals(dt) || "long".equals(dt) || "integer".equals(dt)) {
				field = new NumericField(name, Field.Store.YES, true);
				ft_field = new NumericField(fulltext, Field.Store.NO, true);
				if ("long".equals(dt)) {
					Long value = (Long) inMap.get(name);
					((NumericField) field).setLongValue(value);
					((NumericField) ft_field).setLongValue(value);
				}
				if ("number".equals(dt)) {
					Integer value = (Integer) inMap.get(name);
					((NumericField) field).setIntValue(value);
					((NumericField) ft_field).setIntValue(value);
				}
				if ("integer".equals(dt)) {
					Integer value = (Integer) inMap.get(name);
					((NumericField) field).setIntValue(value);
					((NumericField) ft_field).setIntValue(value);
				}
				if ("double".equals(dt)) {
					Double value = (Double) inMap.get(name);
					((NumericField) field).setDoubleValue(value);
					((NumericField) ft_field).setDoubleValue(value);
				}
			}
			if ("string".equals(dt) || "text".equals(dt)) {
				String value = (String) inMap.get(name);
				if ("".equals(value)) {
					continue;
				}
				field = new Field(name, value, Field.Store.YES, Field.Index.ANALYZED);
				ft_field = new Field(fulltext, value, Field.Store.NO, Field.Index.ANALYZED);
			}
			if (field != null) {
				doc.add(field);
				doc.add(ft_field);
			}
		}
	}

	/************************************ C O N F I G ********************************************************/
	@Reference(dynamic = true, optional = true)
	public void setSettingService(SettingService settingService) {
		this.m_settingService = settingService;
		System.out.println("LuceneServiceImpl.setSettingService:" + settingService);
	}

	@Reference(target = "(kind=jdo)", dynamic = true, optional = true)
	public void setDataLayer(DataLayer paramDataLayer) {
		this.m_dataLayer = paramDataLayer;
		System.out.println("LuceneServiceImpl.setDataLayer:" + paramDataLayer);
	}
}
