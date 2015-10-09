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
package org.ms123.common.workflow;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.impl.jobexecutor.*;
import java.util.concurrent.RejectedExecutionException;
import org.ms123.common.permission.api.PermissionService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ManagementService;
import org.activiti.engine.runtime.Job;
import org.activiti.engine.runtime.*;
import org.activiti.engine.history.*;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.ProcessDefinition;
import org.osgi.service.event.EventAdmin;

/**
 * 
 */
@SuppressWarnings("unchecked")
public class ShiroJobExecutor extends DefaultJobExecutor {

	ProcessEngine m_pe;
	Map m_beans;

	public ShiroJobExecutor(Map  beans) {
		m_beans = beans;
	}

	public PermissionService getPermissionService() {
		return (PermissionService)m_beans.get(PermissionService.PERMISSION_SERVICE);
	}

	public EventAdmin getEventAdmin() {
		return (EventAdmin)m_beans.get("eventAdmin");
	}

	public void setProcessEngine(ProcessEngine pe) {
		m_pe = pe;
	}

	public ProcessEngine getProcessEngine() {
		return m_pe;
	}

	public void executeJobs(List<String> jobIds) {
		ManagementService ms = m_pe.getManagementService();
		Job job = ms.createJobQuery().jobId(jobIds.get(0)).singleResult();
		log("------>executeJobs:" + jobIds+"/"+job.getProcessInstanceId()+"/"+job.getProcessDefinitionId()+"/"+job.getTenantId());
		Map<String,String> info = getInfo(job.getProcessInstanceId(),job.getProcessDefinitionId(),job.getTenantId());
		try {
			threadPoolExecutor.execute(new ShiroExecuteJobsRunnable(this, info, jobIds));
		} catch (RejectedExecutionException e) {
			rejectedJobsHandler.jobsRejected(this, jobIds);
		}
	}

	protected Map getInfo(String processInstanceId,String processDefinitionId, String tenantId) {
		Map<String,String> info = new HashMap();
		if( processInstanceId != null){
			ProcessInstance processInstance = m_pe.getRuntimeService().createProcessInstanceQuery().processInstanceId(processInstanceId).processInstanceTenantId(tenantId).singleResult();
			Map<String,Object> vars = m_pe.getRuntimeService().getVariables(processInstanceId);
			log("getInfo.vars:"+vars);
			info.put("user",(String)vars.get("__currentUser"));
			if (processInstance == null) {
				HistoricProcessInstance instance = m_pe.getHistoryService().createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).processInstanceTenantId(tenantId).singleResult();
				if (instance == null) {
					throw new RuntimeException("ShiroJobExecutor.getInfo:processInstance not found:" + processInstanceId);
				}
				processDefinitionId = instance.getProcessDefinitionId();
			} else {
				processDefinitionId = processInstance.getProcessDefinitionId();
			}
		}else{
			info.put("user","admin");
		}
		RepositoryService repositoryService = m_pe.getRepositoryService();
		ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(processDefinitionId).processDefinitionTenantId(tenantId).singleResult();
		log("getInfo.namespace:"+processDefinition.getTenantId());
		info.put("namespace", processDefinition.getTenantId());
		return info;
	}
	private void log(String message) {
		m_logger.info(message);
		System.out.println(message);
	}
	private static final org.slf4j.Logger m_logger = org.slf4j.LoggerFactory.getLogger(ShiroJobExecutor.class);
}
