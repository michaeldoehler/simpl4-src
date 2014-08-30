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
package org.ms123.common.entity;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import flexjson.*;
import org.ms123.common.data.api.SessionContext;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.utils.ParameterParser;
import org.ms123.common.data.api.DataLayer;
import org.apache.commons.beanutils.PropertyUtils;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Extent;
import javax.jdo.Query;
import javax.jdo.JDOHelper;
import javax.jdo.Transaction;
import org.ms123.common.libhelper.Inflector;
import org.ms123.common.libhelper.Bean2Map;
import org.ms123.common.store.StoreDesc;

/**
 *
 */
interface  MetaData {

	public final String ENTITY = "entity";
	public final String ENTITYTYPES_PATH = "data_description/{0}/entitytypes";
	public final String ENTITYTYPE_PATH = "data_description/{0}/entitytypes/{1}";
	public final String RELATIONS_PATH = "data_description/{0}/relations";
	public final String ENTITYTYPE_TYPE = "sw.entitytype";
	public final String RELATIONS_TYPE = "sw.relations";

	public final String FIELD = "field";

	public final String RELATION = "relation";

	public List<Map> getEntitytypes(String storeId) throws Exception;
	public List<Map> getEntitytypeInfo(String storeId,List<String> names) throws Exception;
	public List<Map> getFields(String storeId, String entityType) throws Exception;
	public List<Map> getRelations(String storeId) throws Exception;
	public void saveRelations(String storeId, List<Map> relations) throws Exception;
	public void saveEntitytype(String storeId, String name, Map<String,Object> desc) throws Exception;
	public void deleteEntitytype(String storeId, String name) throws Exception;
	public void deleteEntitytypeField(String storeId, String entitytype, String name) throws Exception;
	public void deleteEntitytypes(String storeId) throws Exception;
	public Map<String,Object>  getEntitytype(String storeId, String name) throws Exception;
	public void saveEntitytypeField(String storeId, String entitytype, String name, Map<String,Object> data) throws Exception;
	public Map<String,Object> getEntitytypeField(String storeId, String entitytype, String name) throws Exception;

}
