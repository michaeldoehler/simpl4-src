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
package org.ms123.common.data.api;

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

public interface LuceneSession {

	public void setTransactionManager(TransactionManager tm);

	public void addToIndex(Object obj);

	public void deleteFromIndex(Object obj);

	public void setSessionContext(SessionContext sc);

	public String getPrimaryKey();

	public IndexWriter getIndexWriter();

	public StoreDesc getStoreDesc();

	public void addId(String[] doc);

	public List<String[]> getIdList();


	public void commit(Xid xid, boolean b) throws XAException;

	public void rollback(Xid xid) throws XAException;

	public int prepare(Xid xid) throws XAException;

	public void end(Xid xid, int i) throws XAException;

	public void forget(Xid xid) throws XAException;

	public int getTransactionTimeout() throws XAException;

	public boolean isSameRM(XAResource xaResource) throws XAException;

	public Xid[] recover(int i) throws XAException;

	public boolean setTransactionTimeout(int i) throws XAException;

	public void start(Xid xid, int i) throws XAException;
}
