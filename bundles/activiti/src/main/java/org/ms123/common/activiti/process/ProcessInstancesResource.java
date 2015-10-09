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
import org.activiti.engine.history.HistoricProcessInstanceQuery;
import org.activiti.engine.impl.HistoricProcessInstanceQueryProperty;
import org.activiti.engine.query.QueryProperty;
import org.ms123.common.activiti.ActivitiService;
import org.ms123.common.activiti.BaseResource;

/**
 */
public class ProcessInstancesResource extends BaseResource {

	private Map<String, QueryProperty> properties = new HashMap<String, QueryProperty>();

	private String m_processDefinitionId;
	private String m_processDefinitionKey;

	private String m_businessKey;
	private String m_namespace;

	private Boolean m_unfinished;

	private Boolean m_finished;

	public ProcessInstancesResource(ActivitiService as, Map<String, Object> listParams, String processDefinitionId, String processDefinitionKey,String businessKey, Boolean unfinished, Boolean finished, String namespace) {
		super(as, listParams);
		m_processDefinitionId = processDefinitionId;
		m_processDefinitionKey = processDefinitionKey;
		m_businessKey = businessKey;
		m_unfinished = unfinished;
		m_finished = finished;
		m_namespace = namespace;
		properties.put("id", HistoricProcessInstanceQueryProperty.PROCESS_INSTANCE_ID_);
		properties.put("processDefinitionId", HistoricProcessInstanceQueryProperty.PROCESS_DEFINITION_ID);
		properties.put("businessKey", HistoricProcessInstanceQueryProperty.BUSINESS_KEY);
		properties.put("startTime", HistoricProcessInstanceQueryProperty.START_TIME);
		properties.put("endTime", HistoricProcessInstanceQueryProperty.END_TIME);
		properties.put("duration", HistoricProcessInstanceQueryProperty.DURATION);
	}

	public Map getProcessInstances() {
		HistoricProcessInstanceQuery query = getPE().getHistoryService().createHistoricProcessInstanceQuery();
		query = query.processInstanceTenantId(m_namespace);
		if (m_unfinished != null) {
			if (m_unfinished) {
				query = query.unfinished();
			} else {
				query = query.finished();
			}
		}
		if (m_finished != null) {
			if (m_finished) {
				query = query.finished();
			} else {
				query = query.unfinished();
			}
		}
		String processDefinitionId = m_processDefinitionId;
		String processDefinitionKey = m_processDefinitionKey;
		String processInstanceKey = m_businessKey;
		query = processDefinitionId == null ? query : query.processDefinitionId(processDefinitionId);
		query = processDefinitionKey == null ? query : query.processDefinitionKey(processDefinitionKey);
		query = processInstanceKey == null ? query : query.processInstanceBusinessKey(processInstanceKey);
		Map response = new ProcessInstancesPaginateList(this).paginateList(m_listParams, query, "id", properties);
		return response;
	}
}
