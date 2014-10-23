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
import org.ms123.common.store.StoreDesc;
import javax.servlet.http.*;

public interface DataLayer {
	public final String DATA_LAYER = "dataLayer";
	public Map insertObject(Map dataMap, StoreDesc sdesc, String entityName);

	public Map insertObject(Map dataMap, StoreDesc sdesc, String entityName, String entityNameParent, String idParent);

	public Map insertObject(Map dataMap, Map filterMap, Map hintsMap, StoreDesc sdesc, String entityName, String entityNameParent, String idParent);

	public Map insertObject(SessionContext sessionContext, Map dataMap, String entityName) throws Exception;

	public Map insertObject(SessionContext sessionContext, Map dataMap, String entityName, String entityNameParent, String idParent) throws Exception;

	public Map insertObject(SessionContext sessionContext, Map dataMap, Map hintsMap, String entityName, String entityNameParent, String idParent) throws Exception;

	public Map insertObject(SessionContext sessionContext, Map dataMap, Map filterMap, Map hintsMap, String entityName, String entityNameParent, String idParent) throws Exception;

	public Map updateObject(Map dataMap, StoreDesc sdesc, String entityName, String id);

	public Map updateObject(Map dataMap, Map filterMap, Map hintsMap, StoreDesc sdesc, String entityName, String id, String entityNameParent, String parentId);

	public Map updateObject(SessionContext sessionContext, Map dataMap, String entityName, String id) throws Exception;

	public Map updateObject(SessionContext sessionContext, Map dataMap, Map hintsMap, String entityName, String id) throws Exception;

	public Map updateObject(SessionContext sessionContext, Map dataMap, Map filterMap, Map hintsMap, String entityName, String id, String entityNameParent, String idParent) throws Exception;

	public Map deleteObject(Map dataMap, StoreDesc sdesc, String entityName, String id);

	public Map deleteObject(SessionContext sessionContext, Map dataMap, String entityName, String id) throws Exception;

	public Map getObjectGraph(StoreDesc sdesc, String entityName, String id);

	public Map getObject(StoreDesc sdesc, String entityName, String id);

	public Map getObject(StoreDesc sdesc, String entityName, String id, List fields);

	public Map getObject(StoreDesc sdesc, String entityName, String id, String entityNameDetails, List fields);

	public Map getObject(StoreDesc sdesc, String entityName, String id, String entityNameDetails, List fields, HttpServletResponse response);

	public Map querySql(SessionContext sessionContext, StoreDesc sdesc, Map params, String sql);

	public Map query(Map params, StoreDesc sdesc, String entityName);

	public Map query(Map params, StoreDesc sdesc, String entityName, String idParent, String entityNameDetails);

	public Map query(SessionContext sessionContext, Map params, StoreDesc sdesc, String entityName);

	public Map query(SessionContext sessionContext, Map params, StoreDesc sdesc, String entityName, String idParent, String entityNameDetails);

	public SessionContext getSessionContext(StoreDesc sdesc);

	public void populate(SessionContext sessionContext, Map from, Object to, Map hintsMap);

	public List validateObject(SessionContext sessionContext, Object objectInsert, String entityName, boolean bInsert);
	public List validateObject(SessionContext sessionContext, Object objectInsert, String entityName);

	public List validateObject(SessionContext sessionContext, Object objectInsert);

	public Object createObject(SessionContext sessionContext, String entityName);

	public void insertIntoMaster(SessionContext sc, Object objectInsert, String entityName, Class masterClazz, String fieldName, Object masterId) throws Exception;

	public void insertIntoMaster(SessionContext sc, Object objectInsert, String entityName, Object objectMaster, String fieldName) throws Exception;

	public void makePersistent(SessionContext sessionContext, Object objectInsert);

	public String constructEntityName(SessionContext sessionContext, String entityName, String entityNameParent);

	public Class getClass(SessionContext sessionContext, String entityName);

	public void evaluteFormulas(SessionContext sessionContext, String entityName, Map<String, Object> map, String direction);
}
