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
package org.ms123.common.entity.api;

import java.util.Map;
import java.util.List;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.rpc.RpcException;

public interface EntityService {
	public Map getPermittedFields(StoreDesc sdesc, String entityName, String actions);
	public Map getPermittedFields(StoreDesc sdesc, String entityName);
	public List getEntities(StoreDesc sdesc, Boolean withChilds, String mappingstr) throws Exception;
	public Map getEntityTree( StoreDesc sdesc, String mainEntity, int maxlevel, Boolean pathid, String type, Boolean listResolved) throws Exception;
	public List<Map> getFields( StoreDesc  sdesc, String entityName,Boolean withAutoGen) throws Exception;
	public List<Map> getFields( StoreDesc  sdesc, String entityName,Boolean withAutoGen,Boolean withRelations) throws Exception;
	public List<Map> getRelations(StoreDesc sdesc) throws Exception;
	public List<Map> getDefaultFields();
	public List<Map> getTeamFields();
	public List<Map> getStateFields();
	public void saveEntitytype( String storeId, String name, Map<String, Object> data) throws RpcException;
	public void deleteEntitytype( String storeId, String name) throws RpcException;
	public void deleteEntitytypes( String storeId) throws RpcException;
	public List<Map> getEntitytypes(String storeId) throws RpcException;
	public Map<String, Object> getEntitytype(String storeId,  String name) throws RpcException;
	public Map createEntitytypes( String storeId, String dataMapperConfigName, Map dataMapperConfig, String side,Boolean infoOnly) throws RpcException;
}
