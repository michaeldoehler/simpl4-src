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
package org.ms123.common.camel.components.activiti;

import org.activiti.engine.RuntimeService;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.ExecutionQuery;
import org.activiti.engine.runtime.ProcessInstanceBuilder;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.ProcessDefinition;
import org.apache.camel.Exchange;
import org.apache.camel.MessageHistory;
import org.apache.camel.impl.DefaultProducer;
import org.ms123.common.permission.api.PermissionService;
import org.ms123.common.camel.api.CamelService;
import org.ms123.common.workflow.api.WorkflowService;
import org.ms123.common.system.thread.ThreadContext;
import org.ms123.common.camel.components.ExchangeUtils;
import org.ms123.common.camel.trace.ExchangeFormatter;
import org.ms123.common.camel.trace.MessageHelper;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import flexjson.*;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCause;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import static org.ms123.common.system.history.HistoryService.HISTORY_MSG;
import static org.ms123.common.system.history.HistoryService.HISTORY_KEY;
import static org.ms123.common.system.history.HistoryService.HISTORY_TYPE;
import static org.ms123.common.system.history.HistoryService.HISTORY_HINT;
import static org.ms123.common.system.history.HistoryService.HISTORY_ACTIVITI_START_PROCESS_EXCEPTION;
import static org.ms123.common.system.history.HistoryService.HISTORY_ACTIVITI_JOB_EXCEPTION;
import static org.ms123.common.system.history.HistoryService.HISTORY_TOPIC;

import static org.ms123.common.system.history.HistoryService.ACTIVITI_CAMEL_CORRELATION_TYPE;
import static org.ms123.common.system.history.HistoryService.ACC_ACTIVITI_ID;
import static org.ms123.common.system.history.HistoryService.ACC_ROUTE_INSTANCE_ID;
import static org.ms123.common.system.history.HistoryService.HISTORY_ACTIVITI_ACTIVITY_KEY;
import static org.ms123.common.system.history.HistoryService.CAMEL_ROUTE_DEFINITION_KEY;

import static org.ms123.common.workflow.api.WorkflowService.WORKFLOW_ACTIVITY_ID;
import static org.ms123.common.workflow.api.WorkflowService.WORKFLOW_EXECUTION_ID;
import static org.ms123.common.workflow.api.WorkflowService.WORKFLOW_PROCESS_BUSINESS_KEY;
import static org.ms123.common.workflow.api.WorkflowService.WORKFLOW_PROCESS_INSTANCE_ID;
import static org.ms123.common.workflow.api.WorkflowService.WORKFLOW_ACTIVITY_NAME;

@SuppressWarnings("unchecked")
public class ActivitiProducer extends org.activiti.camel.ActivitiProducer implements ActivitiConstants {

	protected JSONSerializer js = new JSONSerializer();
	private RuntimeService runtimeService;
	private RepositoryService repositoryService;
	private PermissionService permissionService;
	private WorkflowService workflowService;

	private ActivitiOperation operation;
	private ActivitiEndpoint endpoint;

	private Map<String, String> processCriteria;
	private String activityId;
	private String namespace;

	private Map options;
	private String activitiKey;

	public ActivitiProducer(ActivitiEndpoint endpoint, WorkflowService workflowService, PermissionService permissionService) {
		super(endpoint, -1, 100);
		this.endpoint = endpoint;
		this.permissionService = permissionService;
		this.workflowService = workflowService;
		this.runtimeService = workflowService.getProcessEngine().getRuntimeService();
		this.repositoryService = workflowService.getProcessEngine().getRepositoryService();
		setRuntimeService(this.runtimeService);
		String[] path = endpoint.getEndpointKey().split(":");
		this.operation = ActivitiOperation.valueOf(path[1].replace("//", ""));
		this.namespace = endpoint.getNamespace();
		this.processCriteria = endpoint.getProcessCriteria();
		this.options = endpoint.getOptions();
	}

	public void process(Exchange exchange) throws Exception {
		this.activityId = getString(exchange, ACTIVITY_ID, this.activityId);
		this.namespace = getString(exchange, NAMESPACE, this.namespace);
		if (this.namespace == null) {
			this.namespace = (String) exchange.getContext().getRegistry().lookupByName("namespace");
		}
		info("ActivitiProducer.operation:" + this.operation);
		invokeOperation(this.operation, exchange);
		/* final CamelService camelService = (CamelService) exchange.getContext().getRegistry().lookupByName(CamelService.class.getName());
		 exchange.setProperty(WORKFLOW_ACTIVITY_NAME, this.activityId);
		 camelService.saveHistory(exchange);
		 saveActivitiCamelCorrelationHistory(exchange);
		 printHistory(exchange);*/
	}

	/**
	 * Entry method that selects the appropriate operation and executes it
	 * @param operation
	 * @param exchange
	 * @throws Exception
	 */
	private void invokeOperation(ActivitiOperation operation, Exchange exchange) throws Exception {
		switch (operation) {
		case sendMessageEvent:
			doSendMessageEvent(exchange);
			break;
		case sendSignalEvent:
			doSendSignalEvent(exchange);
			break;
		case sendSignalToReceiveTask:
			doSendSignalToReceiveTask(exchange);
			break;
		case startProcess:
			doStartProcess(exchange);
			break;
		default:
			throw new RuntimeException("ActivitiProducer.Operation not supported. Value: " + operation);
		}
	}

	private void doSendMessageEvent(Exchange exchange) {
	}

	private void doSendSignalEvent(Exchange exchange) {
		//		ProcessDefinition processDefinition = getProcessDefinition();
	}

	private void doSendSignalToReceiveTask(Exchange exchange) {
		List<ProcessInstance> processInstanceiList = getProcessInstances(exchange);
		for( ProcessInstance pi : processInstanceiList){
			signal(exchange, pi);
		}
	}

	private void doStartProcess(Exchange exchange) {
		ProcessInstance pi = executeProcess(exchange);
		info("ProcessInstance:" + pi);
		if (pi != null) {
			this.activitiKey += "/" + pi.getId();
			info("m_activitiKey:" + this.activitiKey);
			exchange.getOut().setBody(pi.getId());
		}
	}

	private void signal(Exchange exchange, ProcessInstance execution) {
		String ns = (String) exchange.getContext().getRegistry().lookupByName("namespace");
		this.activitiKey += "/" + execution.getId() + "/" + this.activityId;
		if (execution == null) {
			throw new RuntimeException("Couldn't find activityId " + this.activityId + " for processId " + execution.getId());
		}
		Map vars = execution.getProcessVariables();
		Map exVars = ExchangeUtils.prepareVariables(exchange, true, true, true);
		Map props = exchange.getProperties();
		Map headers = exchange.getIn().getHeaders();
		this.runtimeService.setVariables(execution.getId(), getCamelVariablenMap(exchange));
		new SignalThread(ns, getProcessDefinition(execution), execution, exchange).start();
	}

	private class SignalThread extends Thread {
		Execution execution;
		ProcessDefinition processDefinition;
		Exchange exchange;
		Map options;

		String ns;

		public SignalThread(String ns, ProcessDefinition pd, ProcessInstance exec, Exchange exchange) {
			this.execution = exec;
			this.exchange = exchange;
			this.options = options;
			this.ns = ns;
			this.processDefinition = pd;
		}

		public void run() {
			ThreadContext.loadThreadContext(ns, "admin");
			permissionService.loginInternal(ns);
			while (true) {
				try {
					info("SignalThread.sending signal to:" + execution.getId());
					runtimeService.signal(execution.getId(), this.options);
				} catch (org.activiti.engine.ActivitiOptimisticLockingException e) {
					info("SignalThread:" + e);
					try {
						Thread.sleep(100L);
					} catch (Exception x) {
					}
					continue;
				} catch (Exception e) {
					createLogEntry(exchange, this.processDefinition, e);
					if (e instanceof RuntimeException) {
						throw (RuntimeException) e;
					} else {
						throw new RuntimeException(e);
					}
				} finally {
					ThreadContext.getThreadContext().finalize(null);
				}
				break;
			}
		}
	}

	private ProcessInstance executeProcess(Exchange exchange) {
		Map<String, Object> vars = getCamelVariablenMap(exchange);
		info("ExecuteProcess:criteria:" + this.processCriteria);
		ThreadContext.loadThreadContext(this.namespace, "admin");
		this.permissionService.loginInternal(this.namespace);
		try {
			ProcessInstanceBuilder pib = this.runtimeService.createProcessInstanceBuilder();
			String processDefinitionId = getString(exchange, PROCESS_DEFINITION_ID, this.processCriteria.get(PROCESS_DEFINITION_ID));
			if (processDefinitionId != null) {
				pib.processDefinitionId(processDefinitionId);
			}
			String processDefinitionKey = getString(exchange, PROCESS_DEFINITION_KEY, this.processCriteria.get(PROCESS_DEFINITION_KEY));
			if (processDefinitionKey != null) {
				pib.processDefinitionKey(processDefinitionKey);
			}
			String businessKey = getString(exchange, BUSINESS_KEY, this.processCriteria.get(BUSINESS_KEY));
			if (businessKey != null) {
				pib.businessKey(businessKey);
			}

			for (Map.Entry<String, Object> entry : vars.entrySet()) {
				pib.addVariable(entry.getKey(), entry.getValue());
			}
			pib.tenantId(this.namespace);
			return pib.start();
		} finally {
			ThreadContext.getThreadContext().finalize(null);
			info("EndProcess:" + this.processCriteria + "/" + this.namespace);
		}
	}

	private List<ProcessInstance> getProcessInstances(Exchange exchange) {
		Map<String, Object> vars = getCamelVariablenMap(exchange);
		info("findExecution:processCriteria:" + this.processCriteria + "/activityId:" + this.activityId);
		ExecutionQuery eq = this.runtimeService.createExecutionQuery();
		String processDefinitionId = getString(exchange, PROCESS_DEFINITION_ID, this.processCriteria.get(PROCESS_DEFINITION_ID));
		if (processDefinitionId != null) {
			eq.processDefinitionId(processDefinitionId);
		}
		String processDefinitionKey = getString(exchange, PROCESS_DEFINITION_KEY, this.processCriteria.get(PROCESS_DEFINITION_KEY));
		if (processDefinitionKey != null) {
			eq.processDefinitionKey(processDefinitionKey);
		}
		String processInstanceId = getString(exchange, PROCESS_INSTANCE_ID, this.processCriteria.get(PROCESS_INSTANCE_ID));
		if (processInstanceId != null) {
			eq.processInstanceId(processInstanceId);
		}
		String businessKey = getString(exchange, BUSINESS_KEY, this.processCriteria.get(BUSINESS_KEY));
		if (businessKey != null) {
			eq.processInstanceBusinessKey(businessKey);
		}

		String activityId = getString(exchange, ACTIVITY_ID, this.processCriteria.get(ACTIVITY_ID));
		if (activityId != null) {
			eq.activityId(activityId);
		}
		String executionId = getString(exchange, EXECUTION_ID, this.processCriteria.get(EXECUTION_ID));
		if (executionId != null) {
			eq.executionId(executionId);
		}

		eq.executionTenantId(this.namespace);
		List<ProcessInstance> executionList = (List) eq.list();
		info("getProcessInstances:" + executionList);
		if (executionList == null || executionList.size() == 0) {
			throw new RuntimeException("ActivitiProducer.findProcessInstance:Could not find processInstance with criteria " + processCriteria);
		}
		return executionList;
	}

	private ProcessDefinition getProcessDefinition(ProcessInstance processInstance) {
		info("getProcessDefinition:" + processInstance.getProcessDefinitionId() + "/ns:" + this.namespace);

		ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(processInstance.getProcessDefinitionId()).singleResult();
		if (processDefinition == null) {
			throw new RuntimeException("ActivitiProducer:getProcessDefinition:processDefinition not found:" + processInstance);
		}
		info("getProcessDefinition:" + processDefinition + "/" + processDefinition.getTenantId());
		return processDefinition;
	}

	private void saveActivitiCamelCorrelationHistory(Exchange exchange, ProcessInstance processInstance) {
		EventAdmin eventAdmin = (EventAdmin) exchange.getContext().getRegistry().lookupByName(EventAdmin.class.getName());

		String aci = this.namespace + "/" + processInstance.getProcessDefinitionKey() + "/" + processInstance.getId() + "/" + this.activityId;
		exchange.setProperty(HISTORY_ACTIVITI_ACTIVITY_KEY, aci);

		String bc = (String) exchange.getIn().getHeader(Exchange.BREADCRUMB_ID);
		String routeDef = (String) exchange.getProperty(CAMEL_ROUTE_DEFINITION_KEY);
		Map props = new HashMap();
		props.put(HISTORY_TYPE, ACTIVITI_CAMEL_CORRELATION_TYPE);
		props.put(ACC_ACTIVITI_ID, aci);
		props.put(ACC_ROUTE_INSTANCE_ID, routeDef + "|" + bc);
		eventAdmin.postEvent(new Event(HISTORY_TOPIC, props));
	}

	private void printHistory(Exchange exchange) {
		List<MessageHistory> list = exchange.getProperty(Exchange.MESSAGE_HISTORY, List.class);
		ExchangeFormatter formatter = new ExchangeFormatter();
		formatter.setShowExchangeId(true);
		formatter.setMultiline(true);
		formatter.setShowHeaders(true);
		formatter.setStyle(ExchangeFormatter.OutputStyle.Fixed);
		String routeStackTrace = MessageHelper.dumpMessageHistoryStacktrace(exchange, formatter, true);
		info(routeStackTrace);
	}

	private void createLogEntry(Exchange exchange, ProcessDefinition processDefinition, Exception e) {
		EventAdmin eventAdmin = (EventAdmin) exchange.getContext().getRegistry().lookupByName(EventAdmin.class.getName());
		Map props = new HashMap();
		if (this.operation == ActivitiOperation.startProcess) {
			String key = processDefinition.getTenantId() + "/" + processDefinition.getId();
			props.put(HISTORY_KEY, key);
			props.put(HISTORY_TYPE, HISTORY_ACTIVITI_START_PROCESS_EXCEPTION);
			Map hint = new HashMap();
			hint.put("processDefinitionId", processDefinition.getId());
			hint.put("processDefinitionKey", processDefinition.getKey());
			hint.put("processDefinitionName", processDefinition.getName());
			hint.put("processDeploymentId", processDefinition.getDeploymentId());
			props.put(HISTORY_HINT, this.js.deepSerialize(hint));
		} else {
			props.put(HISTORY_KEY, this.activitiKey);
			props.put(HISTORY_TYPE, HISTORY_ACTIVITI_JOB_EXCEPTION);
		}
		Throwable r = getRootCause(e);
		props.put(HISTORY_MSG, r != null ? getStackTrace(r) : getStackTrace(e));
		eventAdmin.postEvent(new Event(HISTORY_TOPIC, props));
	}

	protected boolean isProcessStart() {
		return this.activityId == null;
	}

	private Map getCamelVariablenMap(Exchange exchange) {
		this.js.prettyPrint(true);
		Map camelMap = new HashMap();
		Map exVars = ExchangeUtils.prepareVariables(exchange, true, true, true);
		camelMap.putAll(exVars);
		return camelMap;
	}

	private String getStringCheck(Exchange e, String key, String def) {
		String value = e.getIn().getHeader(key, String.class);
		info("getStringCheck:" + key + "=" + value + "/def:" + def);
		if (value == null) {
			value = e.getProperty(key, String.class);
		}
		if (value == null && def == null) {
			throw new RuntimeException("ActivitiProducer." + key + "_is_null");
		}
		return value != null ? value : def;
	}

	private String getString(Exchange e, String key, String def) {
		String value = e.getIn().getHeader(key, String.class);
		if (value == null) {
			value = e.getProperty(key, String.class);
		}
		info("getString:" + key + "=" + value + "/def:" + def);
		return value != null ? value : def;
	}

	private boolean getBoolean(Exchange e, String key, boolean def) {
		Boolean value = e.getIn().getHeader(key, Boolean.class);
		if (value == null) {
			value = e.getProperty(key, Boolean.class);
		}
		info("getString:" + key + "=" + value + "/def:" + def);
		return value != null ? value : def;
	}

	protected ActivitiEndpoint getActivitiEndpoint() {
		return (ActivitiEndpoint) getEndpoint();
	}

	private void info(String msg) {
		System.err.println(msg);
		m_logger.info(msg);
	}

	private static final org.slf4j.Logger m_logger = org.slf4j.LoggerFactory.getLogger(ActivitiProducer.class);
}

