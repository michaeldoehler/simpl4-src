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
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.task.Task;
import org.apache.commons.lang.StringUtils;
import org.ms123.common.activiti.ActivitiService;
import org.ms123.common.activiti.BaseResource;
import org.ms123.common.activiti.Util;
import org.activiti.engine.form.TaskFormData;
import org.ms123.common.form.FormService;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.repository.ProcessDefinition;

/**
 */
@SuppressWarnings("unchecked")
public class TaskOperationResource extends BaseResource {

	private String m_taskId;

	private String m_operation;

	private Map<String, Object> m_startParams;

	public TaskOperationResource(ActivitiService as, String taskId, String operation, Map<String, Object> startParams) {
		super(as, null);
		m_taskId = taskId;
		m_operation = operation;
		m_startParams = startParams;
	}

	public Map executeTaskOperation() {
		if ("claim".equals(m_operation)) {
			String userId = org.ms123.common.system.thread.ThreadContext.getThreadContext().getUserName();
			getPE().getTaskService().claim(m_taskId, userId);
		} else if ("unclaim".equals(m_operation)) {
			getPE().getTaskService().claim(m_taskId, null);
		} else if ("complete".equals(m_operation)) {
			if( m_startParams==null) m_startParams= new HashMap();
			Map<String, Object> variables = m_startParams;
			variables.remove("taskId");
			//getPE().getFormService().submitTaskFormData(m_taskId, variables);

			TaskFormData taskFormData = getPE().getFormService().getTaskFormData(m_taskId);
			List<FormProperty> userProperties = taskFormData.getFormProperties();
			String formVar = null;
			for(FormProperty fp : userProperties){
				System.out.println("TaskOperationResource.FormProperty:"+fp.getId()+"="+fp.getValue());
				if( "formvarname".equals(fp.getId())){
					formVar = fp.getValue();
				}
			}
			Map<String, Object> newVariables = new HashMap();
			if (taskFormData != null) {
				String formKey = taskFormData.getFormKey();


				Task task = getPE().getTaskService().createTaskQuery().taskId(m_taskId).singleResult();
				String taskName = task.getName();
				String executionId = task.getExecutionId();
				String pid = task.getProcessInstanceId();
				String processDefinitionId = task.getProcessDefinitionId();
				ProcessDefinition processDefinition = getPE().getRepositoryService().createProcessDefinitionQuery().processDefinitionId(processDefinitionId).singleResult();
				String tenantId = processDefinition.getTenantId();
				String processDefinitionKey = processDefinition.getKey();

				String namespace=tenantId;
				System.out.println("TaskOperationResource.formVar:"+formVar);
				if( formVar == null || formVar.length()==0 ){
					formVar = getFormVar(namespace,formKey);
				}
				Map data = (Map)variables.get(formVar);
				System.out.println("formKey:"+formVar+"/"+data+"/"+taskName);
				if( data != null){
					Map ret  = getFormService().validateForm(namespace,formKey,data,true);				
					List<Map> errors = (List)ret.get("errors");
					if( errors.size()>0){
						Map successNode = new HashMap();
						successNode.put("success", false);
						successNode.put("errors", errors);
						return successNode;
					}else{
						data = (Map)ret.get("cleanData");
						newVariables.put(formVar,data);
						String script = (String)ret.get("postProcess");
						if( script!=null && script.trim().length()> 2){
							getWorkflowService().executeScriptTask( executionId, tenantId, processDefinitionKey, pid, script, newVariables, taskName );
							if( data.get("errors") != null ){
								Object _errors = data.get("errors");
								Map successNode = new HashMap();
								successNode.put("success", false);
								if( _errors instanceof List){
									successNode.put("errors", _errors);
								}else{
									List errorList = new ArrayList();
									Map error = new HashMap();
									if( _errors instanceof String){
										error.put("message", _errors);
									}else{
										error.put("message", "Unknown error");
									}
									errorList.add(error);
									successNode.put("errors", errorList);
								}
								return successNode;
							}
						}
					}
				}
			}

			getPE().getTaskService().complete(m_taskId, newVariables);
		} else if ("assign".equals(m_operation)) {
			String userId = org.ms123.common.system.thread.ThreadContext.getThreadContext().getUserName();
			getPE().getTaskService().setAssignee(m_taskId, userId);
		} else {
			throw new RuntimeException("'" + m_operation + "' is not a valid operation");
		}
		Map successNode = new HashMap();
		successNode.put("success", true);
		return successNode;
	}
	private String getFormVar(String namespace,String formKey){
		String formVar=null;
		try{
			formVar = getFormService().getFormName(namespace,formKey);				
		}catch(Exception e){
			throw new RuntimeException("TaskOperationResource:cannot get formVar:",e);
		}
		System.out.println("TaskOperationResource:"+formVar);
		if( formVar == null){
			throw new RuntimeException("TaskOperationResource:formVar is null");
		}
		return formVar;
	}
}
