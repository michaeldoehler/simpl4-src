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

import flexjson.*;
import java.util.*;
import javax.transaction.UserTransaction;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.activiti.engine.delegate.VariableScope;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.el.Expression;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.apache.commons.beanutils.*;
import org.ms123.common.data.api.DataLayer;
import org.ms123.common.workflow.api.WorkflowService;
import org.ms123.common.data.api.SessionContext;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.workflow.GroovyTaskDsl;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.TransactionStatus;

@SuppressWarnings("unchecked")
public class TaskScriptExecutor extends TaskBaseExecutor implements JavaDelegate {

	private Expression script;
	private boolean m_ownTransaction = true;

	public TaskScriptExecutor() {
		m_js.prettyPrint(true);
	}

	@Override
	public void execute(DelegateExecution execution) {
		final TaskContext tc = new TaskContext();
		tc.setExecution(execution);
		setCategory(tc);
		if (script == null){
			return;
		}
		tc.setScript(script.getValue(execution).toString());

		if( m_ownTransaction ){
			TransactionTemplate tt = getTransactionService().getTransactionTemplate(true);
			tt.execute(new TransactionCallback<Object>() {
				public Object doInTransaction(TransactionStatus paramTransactionStatus) {
					_execute(tc,null);
					return null;
			  }
			});
		}else{
			_execute(tc, null);
		}
	}

	public void execute(String namespace, String processDefinitionKey, String pid, String script, final Map addVars,
			VariableScope variableScope, String hint, DataLayer dataLayer, WorkflowService ws) {
		if (script == null) {
			return;
		}
		final TaskContext tc = new TaskContext();
		tc.setCategory(namespace);
		tc.setProcessDefinitionKey(processDefinitionKey);
		tc.setHint(hint);
		tc.setPid(pid);
		tc.setScript(script);
		tc.setExecution(variableScope);
		m_dataLayer = dataLayer;
		m_workflowService = ws;

		if( m_ownTransaction ){
			TransactionTemplate tt = getTransactionService().getTransactionTemplate(true);
			tt.execute(new TransactionCallback<Object>() {
				public Object doInTransaction(TransactionStatus paramTransactionStatus) {
					_execute(tc,addVars);
					return null;
			  }
			});
		}else{
			_execute(tc, addVars);
		}
	}

	private void _execute(TaskContext tc, Map addVars) {
		m_js.prettyPrint(true);
		log(tc, "TaskScriptExecutor._execute:" + tc.getScript());
		printInfo(tc);
		showVariablenNames(tc);
		SessionContext sc = getSessionContext(tc);
		Map<String, Object> vars = new HashMap(tc.getExecution().getVariables());
		log(tc, "TaskScriptExecutor.vars:" + m_js.deepSerialize(vars));
		if (addVars != null) {
			vars.putAll(addVars);
		}
		Map<String, Object> lvars = new HashMap();
		Map<String, Object> gvars = new HashMap();
		vars.put("lvars", lvars);
		vars.put("gvars", gvars);
		vars.put("execution", tc.getExecution());
		GroovyTaskDsl dsl = getGroovyTaskDsl(tc, sc, vars);
		UserTransaction tx = sc.getUserTransaction();
		Object ret = null;
		try {
			log(tc, "transaction.status:" + tx.getStatus() + "/" + org.ms123.common.system.thread.ThreadContext.getThreadContext());
			ret = dsl.eval(tc.getScript());
			log(tc, "TaskScriptExecutor.gvars:" + m_js.deepSerialize(gvars));
			tc.getExecution().setVariables(gvars);
			tc.getExecution().setVariablesLocal(lvars);
			for (Object o : dsl.getCreatedObjects()) {
				log(tc, "createdObject:" + o);
				sc.retrieve(o);
			}
			for (Object o : dsl.getQueriedObjects()) {
				log(tc, "queriedObject:" + o);
				sc.retrieve(o);
			}
		} catch (Exception e) {
			sc.handleException(tx, e);
		} finally {
			sc.handleFinally(tx);
		}
	}
}

