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

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import org.apache.lucene.index.IndexWriter;
import javax.transaction.TransactionManager;
import javax.transaction.Transaction;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.data.api.SessionContext;

@SuppressWarnings("unchecked")
public class LuceneSessionImpl implements XAResource, org.ms123.common.data.api.LuceneSession {

	private IndexWriter m_iw;

	private StoreDesc m_sdesc;

	private boolean m_enlisted = false;

	private TransactionManager m_tm;

	private org.ms123.common.data.api.LuceneService m_luceneService;

	private List m_deleteList;

	private List<String[]> m_idList;

	private int m_status = XAResource.XA_OK;

	private SessionContext m_sc = null;

	public LuceneSessionImpl(StoreDesc sdesc,org.ms123.common.data.api. LuceneService ls) {
		//this.m_iw = iw; 
		this.m_sdesc = sdesc;
		m_idList = new ArrayList();
		m_deleteList = new ArrayList();
		m_luceneService = ls;
	}

	public void setTransactionManager(TransactionManager tm) {
		this.m_tm = tm;
	}

	public void addToIndex(Object obj) {
		enlistResource();
		m_luceneService.addToIndex(this, obj);
	}

	public void deleteFromIndex(Object obj) {
		enlistResource();
		m_deleteList.add(obj);
	}

	public void setSessionContext(SessionContext sc) {
		m_sc = sc;
	}

	public String getPrimaryKey() {
		if (m_sc != null)
			return m_sc.getPrimaryKey();
		return "id";
	}

	public IndexWriter getIndexWriter() {
		if (m_iw == null) {
			try {
				start(null, 0);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return m_iw;
	}

	public StoreDesc getStoreDesc() {
		return m_sdesc;
	}

	public void addId(String[] doc) {
		m_idList.add(doc);
	}

	public List<String[]> getIdList() {
		return m_idList;
	}

	private synchronized void enlistResource() {
		if (m_enlisted)
			return;
		try {
			Transaction tx = m_tm.getTransaction();
			//System.out.println("LuceneSession.enlistResource:" + m_tm + "," + tx); 
			if (tx != null) {
				tx.enlistResource(this);
				m_enlisted = true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static final Xid[] XIDS = new Xid[0];

	public synchronized void commit(Xid xid, boolean b) throws XAException {
		//System.out.println("LuceneSession.commit:" + xid+"/"+m_deleteList); 
		try {
			for (int i = 0; i < m_deleteList.size(); i++) {
				m_luceneService.deleteFromIndex(this, m_deleteList.get(i));
			}
			m_luceneService.commit(this);
			Transaction tx = m_tm.getTransaction();
			//			System.out.println("LuceneSession.delistResource:" + m_tm + "," + tx); 
			tx.delistResource(this, TMSUCCESS);
			m_enlisted = false;
		} catch (Exception e) {
			//@@@MS TODO 
			System.out.println("LuceneSession.commit:" + e.getMessage());
			throw new XAException(e.getMessage());
		}
	}

	public void rollback(Xid xid) throws XAException {
		System.out.println("LuceneSession.rollback:" + xid);
		m_luceneService.rollback(this);
	}

	public int prepare(Xid xid) throws XAException {
		//System.out.println("LuceneSession.prepare:" + xid + "," + XAResource.XA_OK); 
		return XAResource.XA_OK;
	}

	public void end(Xid xid, int i) throws XAException {
	}

	public void forget(Xid xid) throws XAException {
		System.out.println("LuceneSession.forget:" + xid);
	}

	public int getTransactionTimeout() throws XAException {
		return 0;
	}

	public boolean isSameRM(XAResource xaResource) throws XAException {
		return false;
	}

	public Xid[] recover(int i) throws XAException {
		return XIDS;
	}

	public boolean setTransactionTimeout(int i) throws XAException {
		return false;
	}

	public void start(Xid xid, int i) throws XAException {
		m_iw = m_luceneService.createTmpIndexWriter();
		//		System.out.println("LuceneSession.start:" + m_iw); 
		m_idList = new ArrayList();
		m_deleteList = new ArrayList();
	}
}
