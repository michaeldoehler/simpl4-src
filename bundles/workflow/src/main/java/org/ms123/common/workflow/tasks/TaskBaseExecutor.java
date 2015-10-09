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
import java.io.File;
import javax.transaction.UserTransaction;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.VariableScope;
import org.activiti.engine.delegate.JavaDelegate;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.el.Expression;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.scripting.ScriptingEngines;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RuntimeService;
import org.apache.commons.beanutils.*;
import org.ms123.common.data.api.DataLayer;
import org.ms123.common.workflow.api.WorkflowService;
import org.ms123.common.workflow.GroovyTaskDsl;
import org.ms123.common.data.api.SessionContext;
import org.ms123.common.docbook.DocbookService;
import org.ms123.common.rpc.CallService;
import org.ms123.common.git.GitService;
import org.ms123.common.system.tm.TransactionService;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.ProcessEngine;
import org.ms123.common.store.StoreDesc;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import flexjson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unchecked")
public abstract class TaskBaseExecutor {
	private static final Logger m_logger = LoggerFactory.getLogger(TaskBaseExecutor.class);

	protected JSONDeserializer m_ds = new JSONDeserializer();
	protected JSONSerializer m_js = new JSONSerializer();
	protected DataLayer m_dataLayer;
	protected WorkflowService m_workflowService;
	protected BundleContext m_bundleContext;
	protected CallService m_callService;

	protected File getProcessBasedir(DelegateExecution execution) {
		String ws = System.getProperty("workspace");
		File file = new File(ws, "activiti/" + execution.getProcessInstanceId());
		if (!file.exists()) {
			file.mkdirs();
		}
		return file;
	}

	protected File getProcessDocBasedir(DelegateExecution execution) {
		String ws = System.getProperty("workspace");
		File file = new File(ws, "activiti/" + execution.getProcessInstanceId() + "/documents");
		if (!file.exists()) {
			file.mkdirs();
		}
		return file;
	}

	protected void printInfo(TaskContext tc) {
		log(tc, getInfo(tc));
	}

	protected String getExceptionInfo(TaskContext tc){
		String		hint = "\n-----------------------------\n"+getInfo(tc)+"\n";
		hint += "------------------------------\n";
		return hint;
	}
	protected String getInfo(TaskContext tc) {
		VariableScope execution = tc.getExecution();
		if (!(execution instanceof DelegateExecution)) {
			return "";
		}
		DelegateExecution d = (DelegateExecution) execution;
		String processDefinitionId = ((ExecutionEntity) d).getProcessDefinitionId();
		StringBuffer sb = new StringBuffer();
		sb.append("Namespace:" + tc.getTenantId());
		sb.append("\nCurrentActivityId:" + d.getCurrentActivityId());
		sb.append("\nCurrentActivityName:" + d.getCurrentActivityName());
		sb.append("\nEventName:" + d.getEventName());
		sb.append("\nId:" + d.getId());
		sb.append("\nParentId:" + d.getParentId());
		sb.append("\nProcessDefinitionId:" + processDefinitionId);
		sb.append("\nProcessInstanceId:" + d.getProcessInstanceId());
		return sb.toString();
	}

	protected SessionContext getSessionContext(TaskContext tc) {
		VariableScope execution = tc.getExecution();
		SessionContext sc = null;
		if (m_dataLayer != null) {
			StoreDesc sdesc = StoreDesc.getNamespaceData(tc.getTenantId());
			sc = m_dataLayer.getSessionContext(sdesc);
		} else {
			Map beans = Context.getProcessEngineConfiguration().getBeans();
			DataLayer dataLayer = (DataLayer) beans.get(DataLayer.DATA_LAYER);
			log(tc, "Category:"+tc.getTenantId());
			StoreDesc sdesc = StoreDesc.getNamespaceData(tc.getTenantId());
			log(tc, "Sdesc:"+sdesc);
			sc = dataLayer.getSessionContext(sdesc);
		}
		return sc;
	}

	protected WorkflowService getWorkflowService() {
		SessionContext sc = null;
		if (m_workflowService != null) {
			return m_workflowService;
		} else {
			Map beans = Context.getProcessEngineConfiguration().getBeans();
			return (WorkflowService) beans.get(WorkflowService.WORKFLOW_SERVICE);
		}
	}

	protected Object getValue(DelegateExecution execution, String processvar) throws Exception {
		if (processvar.indexOf(".") == -1) {
			//log("\tProcessvar.getValue:" + processvar + " = " + execution.getVariable(processvar));
			return execution.getVariable(processvar);
		}
		String[] parts = processvar.split("\\.");
		Object o = execution.getVariable(parts[0]);
		for (int i = 1; i < parts.length; i++) {
			String part = parts[i];
			o = PropertyUtils.getProperty(o, part);
		}
		//log("\tProcessvar.getValue:" + processvar + " = " + o);
		return o;
	}

	protected String getFileContentFromGit(DelegateExecution execution, String namespace, String name, String type) {
		return getGitService(execution).searchContent(namespace, name, type);
	}

	protected DocbookService getDocbookService(DelegateExecution execution) {
		Map beans = Context.getProcessEngineConfiguration().getBeans();
		DocbookService ds = (DocbookService) beans.get("docbookService");
		return ds;
	}

	protected EventAdmin getEventAdmin(VariableScope execution) {
		Map beans = Context.getProcessEngineConfiguration().getBeans();
		EventAdmin ea = (EventAdmin) beans.get("eventAdmin");
		return ea;
	}

	protected GitService getGitService(VariableScope execution) {
		Map beans = Context.getProcessEngineConfiguration().getBeans();
		GitService gs = (GitService) beans.get("gitService");
		return gs;
	}

	protected TransactionService getTransactionService() {
		if( m_workflowService != null){
			TransactionService ts = (TransactionService)m_workflowService.lookupServiceByName("org.ms123.common.system.TransactionService");
			return ts;
		}else{
			Map beans = Context.getProcessEngineConfiguration().getBeans();
			TransactionService ts = (TransactionService) beans.get("transactionService");
			return ts;
		}
	}

	protected CallService getCallService(){
		if( m_callService != null ) return m_callService;
		if( m_bundleContext == null){
			Map beans = Context.getProcessEngineConfiguration().getBeans();
			m_bundleContext = (BundleContext) beans.get("bundleContext");
		}
		log("TaskBaseExecutor.m_bundleContext:"+m_bundleContext);
	  ServiceReference ref = m_bundleContext.getServiceReference(CallService.class.getName());	
		if( ref != null){
			m_callService = (CallService) m_bundleContext.getService(ref);
		}
		log("TaskBaseExecutor.m_callService:"+m_callService);
		return m_callService;
	}
	protected void setValue(DelegateExecution execution, String processvar, Object value) throws Exception {
		if (processvar.indexOf(".") == -1) {
			//log("\tProcessvar.setValue:" + processvar + " = " + value);
			execution.setVariable(processvar, value);
		}
		String[] parts = processvar.split("\\.");
		Object o = execution.getVariable(parts[0]);
		//log("\tProcessvar.setValue1:" + processvar + "(" + o + "/" + parts[0] + "/" + value);
		if (o == null) {
			o = new HashMap();
			execution.setVariable(parts[0], o);
		}
		for (int i = 1; i < parts.length; i++) {
			String part = parts[i];
			if (i < (parts.length - 1)) {
				Object o1 = PropertyUtils.getProperty(o, part);
				if (o1 == null) {
					o1 = new HashMap();
					PropertyUtils.setProperty(o, part, o1);
				}
				o = o1;
			} else {
				//log("\tProcessvar.setValue2:" + processvar + "(" + o + "/" + part + "/" + value);
				PropertyUtils.setProperty(o, part, value);
			}
		}
	}

	protected void showVariablenNames(TaskContext tc) {
		VariableScope execution = tc.getExecution();
		log(tc, "VarNames(" + this.getClass().getSimpleName() + "):" + execution.getVariableNames());
		for (String x : execution.getVariableNames()) {
			log(tc, "\tx:" + x + "=" + execution.getVariable(x));
		}
	}

	protected GroovyTaskDsl getGroovyTaskDsl(TaskContext tc, SessionContext sc, Map<String, Object> vars) {
		GroovyTaskDsl dsl = null;
		WorkflowService ws = getWorkflowService();
		VariableScope _execution = tc.getExecution();
		if (_execution instanceof DelegateExecution) {
			DelegateExecution execution = (DelegateExecution) _execution;
			String processDefinitionId = ((ExecutionEntity) execution).getProcessDefinitionId();
			dsl = new GroovyTaskDsl(sc, getEventAdmin(execution), ws, tc.getTenantId(), tc.getProcessDefinitionKey(),
					execution.getProcessInstanceId(), getInfo(tc), vars);
		} else {
			dsl = new GroovyTaskDsl(sc, null, ws, tc.getTenantId(), tc.getProcessDefinitionKey(), tc.getPid(),
					tc.getHint(), vars);
		}
		return dsl;
	}

	protected Map<String, Object> getParams(DelegateExecution execution, Expression variablesmapping, String taskVarName) throws Exception {
		if (variablesmapping == null) {
			return new HashMap();
		}
		String vm = variablesmapping.getValue(execution).toString();
		if( vm.trim().length() == 0) return new HashMap();
		Map map = (Map) m_ds.deserialize(vm);
		List<Map> varmap = (List<Map>) map.get("items");
		Map<String, Object> values = new HashMap();
		for (Map<String, String> m : varmap) {
			String processvar = m.get("processvar");
			Object o = getValue(execution, processvar);
			String pvar = m.get(taskVarName);
			values.put(pvar, o);
		}
		return values;
	}
	protected void setProcessDefinition(TaskContext tc, VariableScope execution) {
		if (execution instanceof DelegateExecution) {
			Map beans = Context.getProcessEngineConfiguration().getBeans();
			ProcessEngine pe = (ProcessEngine) beans.get(WorkflowService.PROCESS_ENGINE);
			String processDefinitionId = ((ExecutionEntity) execution).getProcessDefinitionId();
			RepositoryService repositoryService = pe.getRepositoryService();
			ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(processDefinitionId).singleResult();
			tc.setProcessDefinitionKey(processDefinition.getKey());
			tc.setProcessDefinitionName(processDefinition.getName());
			tc.setTenantId(processDefinition.getTenantId());
		}
	}

	protected  class TaskContext {
		protected VariableScope m_execution;
		protected String m_tenantId;
		protected String m_processDefinitionKey;
		protected String m_processDefinitionName;
		protected String m_hint;
		protected String m_pid;
		protected String m_script;

		public TaskContext(){
		}

		public TaskContext(DelegateExecution execution){
			m_execution = execution;
			setProcessDefinition( this, execution);
		}
		public void setExecution(VariableScope vs) {
			m_execution = vs;
		}

		public void setScript(String s) {
			m_script = s;
		}

		public void setPid(String pid) {
			m_pid = pid;
		}

		public void setHint(String hint) {
			m_hint = hint;
		}

		public void setProcessDefinitionName(String pd) {
			m_processDefinitionName = pd;
		}

		public void setProcessDefinitionKey(String pd) {
			m_processDefinitionKey = pd;
		}

		public void setTenantId(String c) {
			m_tenantId = c;
		}

		public VariableScope getExecution() {
			return m_execution;
		}

		public String getScript() {
			return m_script;
		}

		public String getPid() {
			return m_pid;
		}

		public String getHint() {
			return m_hint;
		}

		public String getProcessDefinitionKey() {
			return m_processDefinitionKey;
		}
		public String getProcessDefinitionName() {
			return m_processDefinitionName;
		}

		public String getTenantId() {
			return m_tenantId;
		}
	}
	protected void log(String message) {
		m_logger.info(message);
		System.err.println(message);
	}

	protected void log(TaskContext tc, String message) {
		VariableScope execution = tc.getExecution();
		if ((execution instanceof DelegateExecution)) {
			DelegateExecution d = (DelegateExecution) execution;
			message = "(" + d.getProcessInstanceId() + "," + hashCode() + "):" + message;
		}
		m_logger.info(message);
		System.err.println(message);
	}
}

