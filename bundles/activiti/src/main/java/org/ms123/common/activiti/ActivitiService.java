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
package org.ms123.common.activiti;

import java.util.Map;
import java.util.List;
import org.ms123.common.store.StoreDesc;
import org.activiti.engine.ProcessEngine;
import org.ms123.common.rpc.RpcException;
import org.ms123.common.form.FormService;
import org.ms123.common.workflow.api.WorkflowService;
import org.ms123.common.data.api.DataLayer;
import org.osgi.service.event.EventAdmin;

public interface ActivitiService {

	public ProcessEngine getPE();
	public FormService getFormService();
	public WorkflowService getWorkflowService();
	public DataLayer getDataLayer();
	public EventAdmin getEventAdmin();
	public Map startProcessInstance(
			String namespace, 
			Integer  version,
			String processDefinitionId, 
			String processDefinitionKey, 
			String processDefinitionName, 
			String messageName, 
			String businessKey, 
			Map<String, Object> startParams) throws RpcException;

	public Map getProcessDefinitions(
			String namespace, 
			String key, 
			String name, 
			Integer version, 
			String user, 
			String group, 
			Map<String, Object> listParams) throws RpcException;

	public void setProcessDefinitionCandidates(
			String processDefinitionId, 
			List<String> userList, 
			List<String> groupList) throws RpcException;
}
