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
package org.ms123.common.entity;

import flexjson.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.jdo.annotations.Element;
import javax.jdo.annotations.Persistent;
import java.lang.annotation.Annotation;
import org.apache.commons.beanutils.BeanMap;
import org.ms123.common.utils.annotations.RelatedTo;
import org.ms123.common.data.api.DataLayer;
import org.ms123.common.auth.api.AuthService;
import org.ms123.common.utils.UtilsService;
import org.ms123.common.git.GitService;
import org.ms123.common.data.api.SessionContext;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.nucleus.api.NucleusService;
import org.ms123.common.permission.api.PermissionService;
import org.ms123.common.libhelper.Inflector;
import org.ms123.common.utils.ParameterParser;
import org.mvel2.MVEL;

/**
 *
 */
@groovy.transform.CompileStatic
@groovy.transform.TypeChecked
class GraphCreator implements org.ms123.common.entity.api.Constants, org.ms123.common.datamapper.Constants {

	protected Inflector m_inflector = Inflector.getInstance();
	protected JSONDeserializer m_ds = new JSONDeserializer();

	protected JSONSerializer m_js = new JSONSerializer();


	protected List<Map> m_entityList = [];
	protected List<Map> m_relationList = [];
	protected NucleusService m_nucleusService;

	protected UtilsService m_utilsService;
	protected GitService m_gitService;
	protected MetaData m_gitMetaData;
	protected List<Map> m_strategy;

	protected GraphCreator(EntityServiceImpl esi){
		m_gitService = esi.m_gitService
		m_gitMetaData = esi.m_gitMetaData
		m_js.prettyPrint(true);
	}
	public Map createEntitytypes(String storeId, String datamapperConfigName, Map datamapperConfig, List<Map> strategy, String side, boolean infoOnly){
		StoreDesc sdesc = StoreDesc.get(storeId);
		m_strategy = strategy;
		if( datamapperConfigName != null){
		String json = m_gitService.searchContent(sdesc.getNamespace(), datamapperConfigName, "sw.datamapper");
			datamapperConfig = (Map)m_ds.deserialize(json);
		}
		Map inputTree =  side == INPUT ? datamapperConfig.input as Map : datamapperConfig.output as Map;
		println("InputTree:"+m_js.deepSerialize(inputTree));
		traverseTree( inputTree);
		if( !infoOnly){
			for(Map et : m_entityList ){
				if( isCreateEnabled( (String)et.get("name"))){
					m_gitMetaData.saveEntitytype(storeId, (String)et.get("name"), et);
				}
			}

			List<Map> relations = m_gitMetaData.getRelations(storeId);
			relations = removeExtingRelations( relations, m_entityList);
			relations.addAll( m_relationList );
			m_gitMetaData.saveRelations(storeId, relations);
		}
		return [ entityList: m_entityList, relationList : m_relationList ];
	}

	private boolean isCreateEnabled(String name){
		if( m_strategy==null) return true;
		for( Map emap : m_strategy){
			if( name.equals(emap.get("entityname")) && (Boolean)emap.get("create")){
				return true;
			}
		}
		return false;
	}
	private List<Map> removeExtingRelations(List<Map> relations, List<Map> entityList){
		List<Map> newRelations = new ArrayList();
		for( Map relation : relations ){
			if( !relationContainsEntity(relation, entityList)){
				newRelations.add(relation);
			}
		}
		return newRelations;
	}
	private boolean relationContainsEntity(Map<String,String> r, List<Map> etList){
		boolean leftFound = false;
		boolean rightFound = false;
		for( Map<String,String> et : etList){
			if( r.rightmodule == "data."+et.name) rightFound=true;
			if( r.leftmodule == "data."+et.name) leftFound=true;;
		}
		return leftFound && rightFound;
	}

	private void traverseTree(Map tree){
		Map entityMap = [:];
		m_entityList.add(entityMap);
		initEntityMap(entityMap,tree)
		traverseChildren(entityMap, tree.children as List);
	}

	private void traverseChildren(Map entityMap, List<Map> children){
		for(Map child in children){
			if( child.type == NODETYPE_ATTRIBUTE){
				addField( entityMap, child);
			}else if( child.type == NODETYPE_ELEMENT){
				Map newEntity = [:];
				m_entityList.add(newEntity);
				initEntityMap(newEntity, child);
				createRelation(entityMap, newEntity,"one-to-one");
				traverseChildren(newEntity, child.children as List);
			}else if( child.type == NODETYPE_COLLECTION){
				Map newEntity = [:];
				m_entityList.add(newEntity);
				initEntityMap(newEntity, child);
				createRelation(entityMap, newEntity,"one-to-many");
				traverseChildren(newEntity, child.children as List);
			}
		}	
	}
	private String getEditType(String datatype){
		if("date".equals(datatype))return "date";
		if("boolean".equals(datatype))return "checkbox";
		return "text";
	}
	private void addField(Map entityMap,Map treeNode){
		Map field = [:];
		field.name = treeNode.name;
		def type = treeNode.fieldType;
		if( type == "byte") type = "binary";
		if( type == "double") type = "decimal";
		if( type == "integer") type = "number";
		if( type == "long") type = "number";
		field.datatype = type;
		field.edittype = getEditType(type as String);
		field.enabled = true;
		Map fields = entityMap.fields as Map;
		fields[field.name as String] = field;
	}

	private String getBasename(Map<String,String> entity){
		String entityName = entity.get("name");
		int dot = entityName.lastIndexOf(".");
		if( dot == -1) return entityName;
		return entityName.substring(dot+1);
	}
	private void createRelation(Map leftEntity,Map rightEntity,String type){
		Map relation = [:];
		if( !isCreateEnabled( getBasename(leftEntity))) return;
		if( !isCreateEnabled( getBasename(rightEntity))) return;
		relation.rightmodule= "data."+(rightEntity.name as String);
    relation.leftfield= null;
    relation.leftmodule= "data."+(leftEntity.name as String);
    relation.rightfield= null;
    relation.dependent= true;
    relation.relation= type;
		m_relationList.add(relation);
	}

	private void initEntityMap(Map entityMap,Map treeNode){
		entityMap.name = (treeNode.name as String).toLowerCase();
		entityMap.description = "";
		entityMap.default_fields = false;
		entityMap.team_security = false;
		entityMap.enabled = true;
		entityMap.fields = [:];
	}
}
