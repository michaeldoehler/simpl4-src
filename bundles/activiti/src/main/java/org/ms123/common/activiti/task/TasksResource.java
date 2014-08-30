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
package org.ms123.common.activiti.task;

import java.util.HashMap;
import java.util.Map;
import org.activiti.engine.impl.TaskQueryProperty;
import org.activiti.engine.query.QueryProperty;
import org.activiti.engine.task.TaskQuery;
import org.ms123.common.activiti.ActivitiService;
import org.ms123.common.activiti.BaseResource;
import org.ms123.common.activiti.Util;

/**
 */
@SuppressWarnings("unchecked")
public class TasksResource extends BaseResource {

	Map<String, QueryProperty> properties = new HashMap<String, QueryProperty>();

	private Map<String, Object> m_queryParams = new HashMap();

	public TasksResource(ActivitiService as, Map<String, Object> listParams, Map<String, Object> queryParams) {
		super(as, listParams);
		m_queryParams = queryParams;
		properties.put("id", TaskQueryProperty.TASK_ID);
		properties.put("name", TaskQueryProperty.NAME);
		properties.put("description", TaskQueryProperty.DESCRIPTION);
		properties.put("priority", TaskQueryProperty.PRIORITY);
		properties.put("assignee", TaskQueryProperty.ASSIGNEE);
		properties.put("executionId", TaskQueryProperty.EXECUTION_ID);
		properties.put("processInstanceId", TaskQueryProperty.PROCESS_INSTANCE_ID);
	}

	public Map getTasks() {
		String personalTaskUserId = Util.getString(m_queryParams, "assignee");
		String ownerTaskUserId = Util.getString(m_queryParams, "owner");
		String involvedTaskUserId = Util.getString(m_queryParams, "involved");
		String candidateTaskUserId = Util.getString(m_queryParams, "candidate");
		String candidateGroupId = Util.getString(m_queryParams, "candidate-group");
		String strPriority = Util.getString(m_queryParams, "priority");
		String strMinPriority = Util.getString(m_queryParams, "minPriority");
		String strMaxPriority = Util.getString(m_queryParams, "maxPriority");
		String strDueDate = Util.getString(m_queryParams, "dueDate");
		String strMinDueDate = Util.getString(m_queryParams, "minDueDate");
		String strMaxDueDate = Util.getString(m_queryParams, "maxDueDate");
		TaskQuery taskQuery = getPE().getTaskService().createTaskQuery();
		if (personalTaskUserId != null) {
			taskQuery.taskAssignee(personalTaskUserId);
		} else if (ownerTaskUserId != null) {
			taskQuery.taskOwner(ownerTaskUserId);
		} else if (involvedTaskUserId != null) {
			taskQuery.taskInvolvedUser(involvedTaskUserId);
		} else if (candidateTaskUserId != null) {
			taskQuery.taskCandidateUser(candidateTaskUserId);
		} else if (candidateGroupId != null) {
			taskQuery.taskCandidateGroup(candidateGroupId);
		} else {
			throw new RuntimeException("Tasks must be filtered with 'assignee', 'owner', 'involved', 'candidate' or 'candidate-group'");
		}
		String processInstanceId = Util.getString(m_queryParams, "processInstanceId");
		if (processInstanceId != null) {
			taskQuery.processInstanceId(processInstanceId);
		}
		if (strPriority != null) {
			taskQuery.taskPriority(Util.parseToInteger(strPriority));
		} else if (strMinPriority != null) {
			taskQuery.taskMinPriority(Util.parseToInteger(strMinPriority));
		} else if (strMaxPriority != null) {
			taskQuery.taskMaxPriority(Util.parseToInteger(strMaxPriority));
		}
		if (strDueDate != null) {
			taskQuery.dueDate(Util.parseToDate(strDueDate));
		} else if (strMinDueDate != null) {
			taskQuery.dueAfter(Util.parseToDate(strMinDueDate));
		} else if (strMaxDueDate != null) {
			taskQuery.dueBefore(Util.parseToDate(strMaxDueDate));
		}
		Map dataResponse = new TasksPaginateList(this).paginateList(m_listParams, taskQuery, "id", properties);
		return dataResponse;
	}
}
