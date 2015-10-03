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
package org.ms123.common.workflow.tasks;

import java.util.*;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.activiti.engine.impl.el.Expression;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.scripting.ScriptingEngines;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.ms123.common.data.api.DataLayer;
import javax.transaction.UserTransaction;
import org.ms123.common.data.api.SessionContext;
import org.apache.commons.beanutils.*;
import org.ms123.common.store.StoreDesc;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.history.HistoricProcessInstance;
import org.apache.commons.beanutils.PropertyUtils;
import flexjson.*;
import static org.ms123.common.system.history.HistoryService.HISTORY_ACTIVITI_PROCESS_KEY;
import static org.ms123.common.system.history.HistoryService.HISTORY_ACTIVITI_ACTIVITY_KEY;
import static org.ms123.common.rpc.CallService.ACTIVITI_CAMEL_PROPERTIES;

import static org.ms123.common.workflow.api.WorkflowService.WORKFLOW_ACTIVITY_ID;
import static org.ms123.common.workflow.api.WorkflowService.WORKFLOW_EXECUTION_ID;
import static org.ms123.common.workflow.api.WorkflowService.WORKFLOW_PROCESS_BUSINESS_KEY;
import static org.ms123.common.workflow.api.WorkflowService.WORKFLOW_PROCESS_DEFINITION_ID;
import static org.ms123.common.workflow.api.WorkflowService.WORKFLOW_PROCESS_DEFINITION_NAME;
import static org.ms123.common.workflow.api.WorkflowService.WORKFLOW_PROCESS_INSTANCE_ID;

public class TaskCamelExecutor extends TaskBaseExecutor implements JavaDelegate {

	private Expression namespace;
	private Expression routename;
	private Expression routevarname;

	private Expression listname;

	private Expression variablesmapping;

	@Override
	public void execute(DelegateExecution execution) {
		TaskContext tc = new TaskContext();
		tc.setExecution(execution);
		setCategory(tc);
		showVariablenNames(tc);

		Object rno =  routename != null ? routename.getValue(execution) : null;
		Object rvo =  routevarname != null ? routevarname.getValue(execution) : null;

		String rn = null;
		String rv = null;
		if( !isEmpty(rno)){
			rn = getName(rno.toString());
		}else if( !isEmpty(rvo)){
			rv = getName(rvo.toString());
		}else{
			throw new RuntimeException(getExceptionInfo(tc)+"\nTaskCamelExecutor.routename and routevarname  is null");
		}

		try {
			String methodname = null;
			Map<String, Object> fparams = getParams(execution, variablesmapping, "routevar");
			if( rn != null){
				methodname = rn;
			}
			if( rv != null){
				methodname = (String)execution.getVariable(rv);
				if( methodname == null ){
					throw new RuntimeException(getExceptionInfo(tc)+"\nTaskCamelExecutor.routename is null");
				}
			}
			
			Map<String,String> activitiProperties = new TreeMap<String,String>();
			activitiProperties.put(HISTORY_ACTIVITI_PROCESS_KEY, tc.getCategory() +"/"+tc.getProcessDefinitionName()+"/"+execution.getProcessInstanceId());
			activitiProperties.put(HISTORY_ACTIVITI_ACTIVITY_KEY, tc.getCategory() +"/"+tc.getProcessDefinitionName()+"/"+execution.getId()+"/"+execution.getCurrentActivityId());
			activitiProperties.put(WORKFLOW_ACTIVITY_ID, execution.getCurrentActivityId());
			activitiProperties.put(WORKFLOW_EXECUTION_ID, execution.getId());
			activitiProperties.put(WORKFLOW_PROCESS_BUSINESS_KEY, execution.getProcessBusinessKey());
			activitiProperties.put(WORKFLOW_PROCESS_DEFINITION_ID, execution.getProcessDefinitionId());
			activitiProperties.put(WORKFLOW_PROCESS_DEFINITION_NAME, tc.getProcessDefinitionName());
			activitiProperties.put(WORKFLOW_PROCESS_INSTANCE_ID, execution.getProcessInstanceId());
			fparams.put(ACTIVITI_CAMEL_PROPERTIES, activitiProperties);

			Object answer = getCallService().callCamel( namespace.getValue(execution)+"."+methodname, fparams);
			log("TaskCamelExecutor.answer:"+ answer);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	private boolean isEmpty(Object s) {
		if (s == null || "".equals(((String)s).trim())) {
			return true;
		}
		return false;
	}

	private String getName(String s) {
		if (s == null) {
			throw new RuntimeException("TaskCamelExecutor.routename is null");
		}
		return s;
	}
}

