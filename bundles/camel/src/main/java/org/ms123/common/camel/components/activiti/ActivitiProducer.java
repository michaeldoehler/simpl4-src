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

@SuppressWarnings("unchecked")
public class ActivitiProducer extends org.activiti.camel.ActivitiProducer {

	private RuntimeService m_runtimeService;

	protected JSONDeserializer m_ds = new JSONDeserializer();
	protected JSONSerializer m_js = new JSONSerializer();
	private PermissionService m_permissionService;
	private WorkflowService m_workflowService;

	public static final String PROCESS_KEY_PROPERTY = "PROCESS_KEY_PROPERTY";

	public static final String PROCESS_ID_PROPERTY = "PROCESS_ID_PROPERTY";

	private String m_processKey = null;

	private String m_activity = null;
	private Map m_options;
	private String m_namespace;
	private String m_activitiKey;

	public ActivitiProducer(ActivitiEndpoint endpoint, WorkflowService workflowService, PermissionService permissionService) {
		super(endpoint, -1,100);
		m_permissionService = permissionService;
		m_workflowService = workflowService;
		m_runtimeService = workflowService.getProcessEngine().getRuntimeService();
		setRuntimeService(m_runtimeService);
		info("ActivitiProducer:"+m_runtimeService+"/"+m_permissionService);
		String[] path = endpoint.getEndpointKey().split(":");
		m_processKey = path[1].replace("//", "");
		if (path.length > 2) {
			m_activity = path[2];
		}
		int slash = m_processKey.indexOf("/");
		if( slash !=-1){
			m_namespace= m_processKey.substring(0, slash);
			m_processKey= m_processKey.substring(slash+1);
		}else{
			ProcessDefinition pd = getProcessDefinition();
			m_namespace = pd.getCategory();
		}
		m_activitiKey = m_namespace+"/"+m_processKey;
		m_options = endpoint.getOptions();
		info("activity:" + m_activity + "|processKey:" + m_processKey+"|options:"+m_options);
	}

	public void process(Exchange exchange) throws Exception {
		ProcessDefinition pd = getProcessDefinition();
		String ns = (String) exchange.getContext().getRegistry().lookupByName("namespace");
		info("process:" + shouldStartProcess()+"/properties:"+exchange.getProperties()+"/"+ns+"/ProcessDefinition:"+pd);
		try{
			if (shouldStartProcess()) {
				ProcessInstance pi = startProcess(exchange);
				info("ProcessInstance:"+pi);
				if( pi != null){
					m_activitiKey += "/"+pi.getId();
					info("m_activitiKey:"+m_activitiKey);
					exchange.setProperty(PROCESS_ID_PROPERTY, pi.getProcessInstanceId());
					exchange.getOut().setBody(pi.getId());
				}
			} else {
				signal(exchange,pd);
			}
		}catch(Exception e){
			info("createLogEntry.ActivitiProducer.process");
			createLogEntry(exchange,pd, e);	
			if( e instanceof RuntimeException){
				throw e;
			}else{
				throw new RuntimeException(e);
			}
		}
		final CamelService camelService = (CamelService)exchange.getContext().getRegistry().lookupByName(CamelService.class.getName());
		info("activitikey:"+m_activitiKey);
		exchange.setProperty("activitikey", m_activitiKey);
		info("createLogEntry.saveHistory.ActivitiProducer.process");
		camelService.saveHistory(exchange);
		printHistory(exchange);
	}

	private   void printHistory(Exchange exchange){
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
	private void createLogEntry(Exchange exchange, ProcessDefinition pd, Exception e){
		EventAdmin eventAdmin = (EventAdmin)exchange.getContext().getRegistry().lookupByName(EventAdmin.class.getName());
		Map props = new HashMap();
		if( shouldStartProcess()){
			String key = pd.getCategory()+"/"+pd.getId();
			props.put(HISTORY_KEY, key);
			props.put(HISTORY_TYPE, HISTORY_ACTIVITI_START_PROCESS_EXCEPTION);
			Map hint = new HashMap();
			hint.put("processDefinitionId", pd.getId());
			hint.put("processDefinitionKey", pd.getKey());
			hint.put("processDefinitionName", pd.getName());
			hint.put("processDeploymentId", pd.getDeploymentId());
			props.put(HISTORY_HINT, m_js.deepSerialize(hint));
		}else{
			props.put(HISTORY_KEY, m_activitiKey);
			props.put(HISTORY_TYPE, HISTORY_ACTIVITI_JOB_EXCEPTION);
		}
		info("props:" + props);
		Throwable r = getRootCause(e);
		props.put(HISTORY_MSG, r != null ? getStackTrace(r) : getStackTrace(e));
		eventAdmin.postEvent(new Event(HISTORY_TOPIC, props));
	}
	protected boolean shouldStartProcess() {
		return m_activity == null;
	}

	private void signal(Exchange exchange, ProcessDefinition pd) {
		String ns = (String) exchange.getContext().getRegistry().lookupByName("namespace");
		String processInstanceId = findProcessInstanceId(exchange);
		info("signal:"+"m_processKey:"+m_processKey+"/m_activity:"+m_activity+"/processInstanceId:"+processInstanceId);
		ProcessInstance execution = (ProcessInstance)m_runtimeService.createExecutionQuery().processDefinitionKey(m_processKey).processInstanceId(processInstanceId).activityId(m_activity).singleResult();
		m_activitiKey += "/"+processInstanceId+"/"+m_activity;
		if (execution == null) {
			throw new RuntimeException("Couldn't find activity " + m_activity + " for processId " + processInstanceId);
		}
		Map vars = execution.getProcessVariables();
		//Map exVars = ExchangeUtils.prepareVariables(exchange, getActivitiEndpoint().isCopyVariablesFromHeader(), getActivitiEndpoint().isCopyVariablesFromProperties(),getActivitiEndpoint().isCopyCamelBodyToBodyAsString());
		Map exVars = ExchangeUtils.prepareVariables(exchange,true,true ,getActivitiEndpoint().isCopyCamelBodyToBodyAsString());
		Map props = exchange.getProperties();
		Map headers = exchange.getIn().getHeaders();
		logCamel(exchange);
		m_runtimeService.setVariables(execution.getId(), getCamelVariablenMap(exchange));
		new SignalThread(ns, pd, execution, exchange).start();
	}

	private Map getCamelVariablenMap(Exchange exchange){
		m_js.prettyPrint(true);
		Map camelMap = new HashMap();
		//Map exVars = ExchangeUtils.prepareVariables(exchange, getActivitiEndpoint().isCopyVariablesFromHeader(),getActivitiEndpoint().isCopyVariablesFromProperties(),getActivitiEndpoint().isCopyCamelBodyToBodyAsString());
		Map exVars = ExchangeUtils.prepareVariables(exchange,true,true ,getActivitiEndpoint().isCopyCamelBodyToBodyAsString());
		//String e1 = m_js.deepSerialize(exVars);
		//System.out.println("e1:"+e1);
		camelMap.putAll(exVars);
		//Map props = exchange.getProperties();
		//String e2 = m_js.deepSerialize(props);
		//System.out.println("e2:"+e2);
		//camelMap.put("exchangeProperties", props);
		//Map headers = exchange.getIn().getHeaders();
		//camelMap.putAll(headers);
		return camelMap;
	}
	private void logCamel(Exchange exchange){
		//Map exVars = ExchangeUtils.prepareVariables(exchange, getActivitiEndpoint().isCopyVariablesFromHeader(),getActivitiEndpoint().isCopyVariablesFromProperties(),getActivitiEndpoint().isCopyCamelBodyToBodyAsString());
		Map exVars = ExchangeUtils.prepareVariables(exchange,true,true ,getActivitiEndpoint().isCopyCamelBodyToBodyAsString());
		Map props = exchange.getProperties();
		Map headers = exchange.getIn().getHeaders();
		info("exchangeVars:"+exVars);
		info("exchangeProps:"+props);
		info("exchangeHeader:"+headers);
	}

	private class SignalThread extends Thread {

		Execution execution;
		Exchange exchange;
		ProcessDefinition pd;

		String ns;

		public SignalThread(String ns, ProcessDefinition pd, Execution execution,Exchange exchange) {
			this.execution = execution;
			this.exchange = exchange;
			this.ns = ns;
			this.pd = pd;
		}

		public void run() {
			ThreadContext.loadThreadContext(ns, "admin");
			m_permissionService.loginInternal(ns);
			while (true) {
				try {
					info("run.sending signal to:" + execution.getId() + "/ns:" + ns);
					m_runtimeService.signal(execution.getId(), m_options);
				} catch (org.activiti.engine.ActivitiOptimisticLockingException e) {
					info("run:" + e);
					try {
						Thread.sleep(100L);
					} catch (Exception x) {
					}
					continue;
				} catch (Exception e) {
		info("createLogEntry.ActivitiProducer.run");
					createLogEntry(exchange,pd, e);	
					if( e instanceof RuntimeException){
						throw (RuntimeException)e;
					}else{
						throw new RuntimeException(e);
					}
				}finally{
					ThreadContext.getThreadContext().finalize(null);
				}
				break;
			}
		}
	}

	protected String findProcessInstanceId(Exchange exchange) {
		String processInstanceId = exchange.getProperty(PROCESS_ID_PROPERTY, String.class);
		info("findProcessInstanceId:" + processInstanceId);
		if (processInstanceId != null) {
			return processInstanceId;
		}
		String processInstanceKey = exchange.getProperty(PROCESS_KEY_PROPERTY, String.class);
		ProcessInstance processInstance = m_runtimeService.createProcessInstanceQuery().processInstanceBusinessKey(processInstanceKey).singleResult();
		if (processInstance == null) {
			throw new RuntimeException("ActivitiProducer:Could not find activiti with key " + processInstanceKey);
		}
		return processInstance.getId();
	}

	protected ProcessInstance startProcess(Exchange exchange) {
		logCamel(exchange);
		Map vars = getCamelVariablenMap(exchange);
		info("startProcess:"+m_processKey);
		String ns=m_namespace;
		ThreadContext.loadThreadContext(ns, "admin");
		m_permissionService.loginInternal(ns);
		try {
			String key = exchange.getProperty(PROCESS_KEY_PROPERTY, String.class);
			if (key == null) {
				return m_runtimeService.startProcessInstanceByKey(m_processKey, vars);
			} else {
				return m_runtimeService.startProcessInstanceByKey(m_processKey, key, vars);
			}
		}finally{
			ThreadContext.getThreadContext().finalize(null);
			info("endProcess:"+ns+"/"+m_processKey);
		}
	}

	private ProcessDefinition getProcessDefinition() {
		ProcessEngine  pe = m_workflowService.getProcessEngine();
		RepositoryService repositoryService = pe.getRepositoryService();
		ProcessDefinition  processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey(m_processKey).latestVersion().singleResult();
		if (processDefinition == null) {
			throw new RuntimeException("ActivitiProducer:getCategory:processDefinition not found:" + m_processKey);
		}
		info("getProcessDefinition:"+processDefinition+"/"+processDefinition.getCategory());
		return processDefinition;
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
