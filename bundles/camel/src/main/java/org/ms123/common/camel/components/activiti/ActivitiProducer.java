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
public class ActivitiProducer extends org.activiti.camel.ActivitiProducer implements ActivitiConstants{

	protected JSONSerializer js = new JSONSerializer();
	private RuntimeService runtimeService;
	private PermissionService permissionService;
	private WorkflowService workflowService;

	private ActivitiOperation operation;
	private ActivitiEndpoint endpoint;

	private String processName;
	private String activityName;
	private String namespace;

	private Map options;
	private String activitiKey;

	public static final String PROCESS_KEY_PROPERTY = "PROCESS_KEY_PROPERTY";
	public static final String PROCESS_ID_PROPERTY = "PROCESS_ID_PROPERTY";

	public ActivitiProducer(ActivitiEndpoint endpoint, WorkflowService workflowService, PermissionService permissionService) {
		super(endpoint, -1, 100);
		this.endpoint = endpoint;
		this.permissionService = permissionService;
		this.workflowService = workflowService;
		this.runtimeService = workflowService.getProcessEngine().getRuntimeService();
		setRuntimeService(this.runtimeService);
		String[] path = endpoint.getEndpointKey().split(":");
		this.operation = ActivitiOperation.valueOf(path[1].replace("//", ""));
		this.namespace = endpoint.getNamespace();
		this.processName = endpoint.getProcessName();
		this.activityName = endpoint.getActivityName();
		this.options = endpoint.getOptions();
	}

	public void process(Exchange exchange) throws Exception {
		this.processName = getString(exchange, PROCESS_NAME, this.processName);
		this.activityName = getString(exchange, ACTIVITY_NAME, this.activityName);
		this.namespace = getString(exchange, NAMESPACE, this.namespace);
		if( this.namespace == null){
		 this.namespace = (String) exchange.getContext().getRegistry().lookupByName("namespace");
		}
		info("ActivitiProducer.process:" + this.operation);
		invokeOperation(this.operation, exchange);
/*		try {
			if (shouldStartProcess()) {
				ProcessInstance pi = startProcess(exchange);
				info("ProcessInstance:" + pi);
				if (pi != null) {
					this.activitiKey += "/" + pi.getId();
					info("m_activitiKey:" + this.activitiKey);
					exchange.setProperty(PROCESS_ID_PROPERTY, pi.getProcessInstanceId());
					exchange.getOut().setBody(pi.getId());
				}
			} else {
				signal(exchange, pd);
			}
		} catch (Exception e) {
			info("createLogEntry.ActivitiProducer.process");
			createLogEntry(exchange, pd, e);
			if (e instanceof RuntimeException) {
				throw e;
			} else {
				throw new RuntimeException(e);
			}
		}
		final CamelService camelService = (CamelService) exchange.getContext().getRegistry().lookupByName(CamelService.class.getName());
		exchange.setProperty(WORKFLOW_ACTIVITY_NAME, this.activityName);
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
	protected void invokeOperation(ActivitiOperation operation, Exchange exchange) throws Exception {
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
		ProcessDefinition processDefinition = getProcessDefinition();
	}
	private void doSendSignalToReceiveTask(Exchange exchange) {
		ProcessDefinition processDefinition = getProcessDefinition();
		this.processName = getStringCheck(exchange, PROCESS_NAME, this.processName);
		this.activityName = getStringCheck(exchange, ACTIVITY_NAME, this.activityName);
		signal(exchange, processDefinition);
	}

	private void doStartProcess(Exchange exchange) {
		this.processName = getStringCheck(exchange, PROCESS_NAME, this.processName);
		ProcessDefinition processDefinition = getProcessDefinition();
		ProcessInstance pi = startProcess(processDefinition, exchange);
		info("ProcessInstance:" + pi);
		if (pi != null) {
			this.activitiKey += "/" + pi.getId();
			info("m_activitiKey:" + this.activitiKey);
			exchange.setProperty(PROCESS_ID_PROPERTY, pi.getProcessInstanceId());
			exchange.getOut().setBody(pi.getId());
		}
	}

	private void signal(Exchange exchange, ProcessDefinition processDefinition) {
		String ns = (String) exchange.getContext().getRegistry().lookupByName("namespace");
		String processInstanceId = findProcessInstanceId(processDefinition, exchange);
		info("signal:" + "this.processName:" + this.processName + "/this.activityName:" + this.activityName + "/processInstanceId:" + processInstanceId);
		ProcessInstance execution = (ProcessInstance) this.runtimeService.createExecutionQuery().processDefinitionKey(processDefinition.getKey()).processInstanceId(processInstanceId).activityId(this.activityName).singleResult();
		info("ProcessInstance.execution:"+execution);
		this.activitiKey += "/" + processInstanceId + "/" + this.activityName;
		if (execution == null) {
			throw new RuntimeException("Couldn't find activityName " + this.activityName + " for processId " + processInstanceId);
		}
		Map vars = execution.getProcessVariables();
		Map exVars = ExchangeUtils.prepareVariables(exchange, true, true, true);
		Map props = exchange.getProperties();
		Map headers = exchange.getIn().getHeaders();
		logCamel(exchange);
		this.runtimeService.setVariables(execution.getId(), getCamelVariablenMap(exchange));
		new SignalThread(ns, processDefinition, execution, exchange).start();
	}

	private void saveActivitiCamelCorrelationHistory(Exchange exchange) {
		EventAdmin eventAdmin = (EventAdmin) exchange.getContext().getRegistry().lookupByName(EventAdmin.class.getName());

		String processInstanceId = findProcessInstanceId(null, exchange); //@@@MS
		ProcessInstance execution = (ProcessInstance) this.runtimeService.createExecutionQuery().processDefinitionKey(this.processName).processInstanceId(processInstanceId).activityId(this.activityName).singleResult();
		String aci = this.namespace + "/" + this.processName + "/" + execution.getId() + "/" + this.activityName;
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
		info("printHistoryX");
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
		if (shouldStartProcess()) {
			String key = processDefinition.getCategory() + "/" + processDefinition.getId();
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
		info("props:" + props);
		Throwable r = getRootCause(e);
		props.put(HISTORY_MSG, r != null ? getStackTrace(r) : getStackTrace(e));
		eventAdmin.postEvent(new Event(HISTORY_TOPIC, props));
	}

	protected boolean shouldStartProcess() {
		return this.activityName == null;
	}

	private Map getCamelVariablenMap(Exchange exchange) {
		this.js.prettyPrint(true);
		Map camelMap = new HashMap();
		Map exVars = ExchangeUtils.prepareVariables(exchange, true, true, true);
		//String e1 = this.js.deepSerialize(exVars);
		//System.out.println("e1:"+e1);
		camelMap.putAll(exVars);
		//Map props = exchange.getProperties();
		//String e2 = this.js.deepSerialize(props);
		//System.out.println("e2:"+e2);
		//camelMap.put("exchangeProperties", props);
		//Map headers = exchange.getIn().getHeaders();
		//camelMap.putAll(headers);
		return camelMap;
	}

	private void logCamel(Exchange exchange) {
		Map exVars = ExchangeUtils.prepareVariables(exchange, true, true, true);
		Map props = exchange.getProperties();
		Map headers = exchange.getIn().getHeaders();
		info("exchangeVars:" + exVars);
		info("exchangeProps:" + props);
		info("exchangeHeader:" + headers);
	}

	private class SignalThread extends Thread {
		Execution execution;
		Exchange exchange;
		ProcessDefinition processDefinition;
		Map options;

		String ns;

		public SignalThread(String ns, ProcessDefinition pd, Execution execution, Exchange exchange) {
			this.execution = execution;
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
					info("SignalThread.sending signal to:" + execution.getId() + "/ns:" + ns);
					runtimeService.signal(execution.getId(), this.options);
				} catch (org.activiti.engine.ActivitiOptimisticLockingException e) {
					info("SignalThread:" + e);
					try {
						Thread.sleep(100L);
					} catch (Exception x) {
					}
					continue;
				} catch (Exception e) {
					createLogEntry(exchange, processDefinition, e);
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

	protected ProcessInstance startProcess(ProcessDefinition processDefinition, Exchange exchange) {
		//logCamel(exchange);
		Map vars = getCamelVariablenMap(exchange);
		info("startProcess:Name:" + this.processName+"/key:"+processDefinition.getKey());
		ThreadContext.loadThreadContext(this.namespace, "admin");
		this.permissionService.loginInternal(this.namespace);
		try {
			return this.runtimeService.startProcessInstanceByKey(processDefinition.getKey(), vars);
		/*	String key = exchange.getProperty(PROCESS_KEY_PROPERTY, String.class);
			if (key == null) {
				return this.runtimeService.startProcessInstanceByKey(this.processName, vars);
			} else {
				return this.runtimeService.startProcessInstanceByKey(this.processName, key, vars);
			}*/
		} finally {
			ThreadContext.getThreadContext().finalize(null);
			info("endProcess:" + this.namespace + "/" + this.processName);
		}
	}

	protected String findProcessInstanceId(ProcessDefinition processDefinition, Exchange exchange) {
		info("findProcessInstanceId:Name:" + this.processName+"/key:"+processDefinition.getKey()+"/name:"+processDefinition.getName()+"/Id:"+processDefinition.getId());
		String processInstanceId = exchange.getProperty(WORKFLOW_PROCESS_INSTANCE_ID, String.class);
		if (processInstanceId != null) {
			System.out.println("processInstanceId2:" + processInstanceId);
			return processInstanceId;
		}
		String processInstanceKey = exchange.getProperty(WORKFLOW_PROCESS_BUSINESS_KEY, String.class);
		ProcessInstance processInstance = null;//this.runtimeService.createProcessInstanceQuery().processInstanceBusinessKey(processInstanceKey).singleResult();
		if (processInstance == null) {
			processInstance = this.runtimeService.createProcessInstanceQuery().processDefinitionId(processDefinition.getId()).
							//	processInstanceBusinessKey(processInstanceKey).
								singleResult();
		}
		if (processInstance == null) {
			throw new RuntimeException("ActivitiProducer:Could not find activiti with key " + processInstanceKey);
		}
		return processInstance.getId();
	}

	private ProcessDefinition getProcessDefinition() {
		ProcessEngine pe = this.workflowService.getProcessEngine();
		RepositoryService repositoryService = pe.getRepositoryService();
		info("getProcessDefinition:"+this.processName+"/ns:"+this.namespace);
		ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey(this.processName).processDefinitionCategory(this.namespace).latestVersion().singleResult();
		if (processDefinition == null) {
			throw new RuntimeException("ActivitiProducer:getProcessDefinition:processDefinition not found:" + this.namespace+"/"+this.processName);
		}
		info("getProcessDefinition:" + processDefinition + "/" + processDefinition.getCategory());
		return processDefinition;
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

