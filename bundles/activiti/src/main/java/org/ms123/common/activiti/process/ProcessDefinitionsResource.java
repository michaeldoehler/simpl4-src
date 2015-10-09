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
package org.ms123.common.activiti.process;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import org.ms123.common.activiti.ActivitiService;
import org.ms123.common.activiti.BaseResource;
import org.activiti.engine.impl.ProcessDefinitionQueryProperty;
import org.activiti.engine.impl.ProcessDefinitionQueryImpl;
import org.activiti.engine.query.QueryProperty;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.apache.commons.lang.StringUtils;

/**
 */
public class ProcessDefinitionsResource extends BaseResource {

	private String m_startableByUser;
	private String m_startableByGroup;
	private String m_namespace;
	private String m_name;
	private String m_key;
	private Integer m_version;

	private Map<String, QueryProperty> properties = new HashMap<String, QueryProperty>();

	public ProcessDefinitionsResource(ActivitiService as, Map<String, Object> listParams, String namespace, String key, String name, Integer version,String startableByUser,String startableByGroup) {
		super(as, listParams);
		m_startableByUser = startableByUser;
		m_startableByGroup = startableByGroup;
		m_namespace = namespace;
		m_name = name;
		m_key = key;
		m_version = version;
		properties.put("id", ProcessDefinitionQueryProperty.PROCESS_DEFINITION_ID);
		properties.put("key", ProcessDefinitionQueryProperty.PROCESS_DEFINITION_KEY);
		properties.put("version", ProcessDefinitionQueryProperty.PROCESS_DEFINITION_VERSION);
		properties.put("deploymentId", ProcessDefinitionQueryProperty.DEPLOYMENT_ID);
		properties.put("name", ProcessDefinitionQueryProperty.PROCESS_DEFINITION_NAME);
		properties.put("tenantId", ProcessDefinitionQueryProperty.PROCESS_DEFINITION_TENANT_ID);
	}

	public Map getProcessDefinitions() {
		ProcessDefinitionQuery query = getPE().getRepositoryService().createProcessDefinitionQuery();
		if( m_namespace != null){
			query = query.processDefinitionTenantId(this.m_namespace);
		}
		if( m_name != null){
			query = query.processDefinitionName(this.m_name);
		}
		if( m_key != null){
			query = query.processDefinitionKey(this.m_key);
		}
		if( m_version != null){
			if( m_version != -1){
				query = query.processDefinitionVersion(this.m_version);
			}else{
				query = query.latestVersion();
			}
		}
		if (StringUtils.isNotEmpty(m_startableByUser)) {
			query = query.startableByUser(m_startableByUser);
		}
		if( m_namespace == null){
			query = query.orderByTenantId();
		}
		if( m_key == null){
			query = query.orderByProcessDefinitionKey();
		}
		if( m_version == null){
			query = query.orderByProcessDefinitionVersion();
		}
		Map response = new ProcessDefinitionsPaginateList(this,m_startableByGroup).paginateList(m_listParams, query, "id", properties);
		return response;
	}
}
