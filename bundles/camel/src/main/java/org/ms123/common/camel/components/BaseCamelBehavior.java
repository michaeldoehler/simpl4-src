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
package org.ms123.common.camel.components;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.lang.reflect.Field;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.impl.bpmn.behavior.BpmnActivityBehavior;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.pvm.PvmProcessDefinition;
import org.activiti.engine.impl.pvm.delegate.ActivityBehavior;
import org.activiti.engine.impl.pvm.delegate.ActivityExecution;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.camel.ExchangeUtils;
import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.MessageHistory;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.spi.Registry;
import org.apache.camel.core.osgi.OsgiDefaultCamelContext;
import org.apache.camel.core.osgi.OsgiServiceRegistry;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.apache.camel.impl.CompositeRegistry;
import org.apache.camel.processor.interceptor.Tracer;
import org.apache.camel.processor.interceptor.Tracer;
import org.ms123.common.permission.api.PermissionService;
import org.ms123.common.datamapper.DatamapperService;
import org.ms123.common.data.api.DataLayer;
import org.ms123.common.camel.api.CamelService;
import org.ms123.common.camel.CamelContextBuilder;
import org.ms123.common.camel.trace.*;
import org.ms123.common.camel.components.activiti.ActivitiEndpoint;
import org.ms123.common.camel.components.activiti.ActivitiProducer;
import org.ms123.common.camel.GroovyRegistry;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import groovy.lang.GroovyShell;
import flexjson.*;
import static org.apache.commons.lang3.StringEscapeUtils.unescapeJava;
import org.apache.commons.beanutils.*;

/**
*/
@SuppressWarnings("unchecked")
public abstract class BaseCamelBehavior extends BpmnActivityBehavior implements ActivityBehavior {

	private Expression variablesmapping;
	private Expression resultvar;

	public static String PROCESSVAR = "processvar";
	public static String CAMELVAR = "camelvar";
	public static String DESTINATION = "destination";
	public static String DESTINATION_BODY = "body";
	public static String DESTINATION_PROP = "property";
	public static String DESTINATION_HEADER = "header";
	public static String DESTINATION_ROUTING = "routing";
	public static String ITEMS = "items";
	public static String DOT = "\\.";
	String m_category, m_processDefinitionKey;

	public Class m_routeBuilderClass = null;

	private static final long serialVersionUID = 1L;

	protected JSONDeserializer m_ds = new JSONDeserializer();

	protected JSONSerializer m_js = new JSONSerializer();

	protected String m_camelActivitiKey;

	protected CamelContext m_camelContext;
	protected ActivityExecution m_execution;
	protected Map m_mapRouting;

	public void execute(ActivityExecution execution) throws Exception {
		m_js.prettyPrint(true);
		m_execution = execution;
		debug("BaseCamelBehavior.execution:" + isASync(execution));

		Map mapBody = getVarMapping(execution, variablesmapping,DESTINATION_BODY);
		debug("MappingBody:" + m_js.deepSerialize(mapBody));
		Map mapProp = getVarMapping(execution, variablesmapping,DESTINATION_PROP);
		debug("MappingProp:" + m_js.deepSerialize(mapProp));
		Map mapHeader = getVarMapping(execution, variablesmapping,DESTINATION_HEADER);
		debug("MappingHeader:" + m_js.deepSerialize(mapHeader));
		m_mapRouting = getVarMapping(execution, variablesmapping,DESTINATION_ROUTING);
		debug("MappingRouting:" + m_js.deepSerialize(m_mapRouting));

		setCategoryAndName(execution);
		m_camelContext = createCamelContext(m_category);
		createRouteBuilderClazz();
		addRoute(m_camelContext, execution);
		m_camelContext.start();
		final ActivitiEndpoint endpoint = createEndpoint(execution);
		final Exchange exchange = createExchange(execution, endpoint);
		exchange.setProperty("activitikey", m_camelActivitiKey);
		copyVariablesToProperties( mapProp, exchange);
		copyVariablesToHeader( mapHeader, exchange);
		copyVariablesToBodyAsMap( mapBody, exchange);
		final CamelService camelService = (CamelService)m_camelContext.getRegistry().lookupByName(CamelService.class.getName());
		if (isASync(execution)) {
			FutureTask<Void> future = new FutureTask<Void>(new Callable<Void>() {

				public Void call() {
					try {
						endpoint.process(exchange);
					} catch (Exception e) {
						throw new RuntimeException("Unable to process camel endpoint asynchronously.");
					}finally{
						Exception camelException = exchange.getException();
						info("camelException:"+camelException);
						printHistory(exchange);
						camelService.saveHistory(exchange);
						try{
							info("Context stop");
							m_camelContext.stop();
						}catch(Exception e){
							e.printStackTrace();
						}	
					}
					return null;
				}
			});
			ExecutorService executor = Executors.newSingleThreadExecutor();
			executor.submit(future);
			handleCamelException(exchange);
		} else {
			try{
				endpoint.process(exchange);
				handleCamelException(exchange);
				debug(ExchangeUtils.prepareVariables(exchange, endpoint)+"");
				String rs = null;
				if( resultvar != null){
					 rs = resultvar.getValue(execution).toString();
				}
				if(isEmpty(rs)){
					execution.setVariables(ExchangeUtils.prepareVariables(exchange, endpoint));
				}else{
					execution.setVariable(rs, ExchangeUtils.prepareVariables(exchange, endpoint));
				}
			}finally{
				printHistory(exchange);
				camelService.saveHistory(exchange);
				m_camelContext.stop();
			}
		}
		performDefaultOutgoingBehavior(execution);
	}


	public static  void printHistory(Exchange exchange){
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

	private Map<String, Object> getVarMapping(DelegateExecution execution, Expression variablesmapping, String destination) throws Exception {
		if (variablesmapping == null) {
			return new HashMap();
		}
		String vm = variablesmapping.getValue(execution).toString();
		debug("getVarMapping:"+vm);
		if (vm.trim().length() == 0){
			return new HashMap();
		}
		Map map = (Map) m_ds.deserialize(vm);
		debug("getVarMapping.Map:"+map);
		List<Map> varmap = (List<Map>) map.get(ITEMS);
		Map<String, Object> values = new HashMap();
		for (Map<String, String> m : varmap) {
			String processVar = m.get(PROCESSVAR);
			if( isEmpty(processVar)){
				continue;
			}
			String dest = m.get(DESTINATION);
			if(!destination.equals(dest)){
				 continue;
			}
			Object o = null;
			if( processVar.startsWith("#")){
				o = processVar.substring(1);
			}else{
				o = getValue(execution, processVar);
			}
			String camelVar = m.get(CAMELVAR);
			if( isEmpty(camelVar)){
			 camelVar = processVar;
			}
			values.put(camelVar, o);
		}
		return values;
	}

	private boolean isEmpty(String s) {
		return (s == null || "".equals(s.trim()));
	}
	private Object getValue(DelegateExecution execution, String processVar) throws Exception {
		if (processVar.indexOf(".") == -1) {
			return execution.getVariable(processVar);
		}
		String[] parts = processVar.split(DOT);
		Object o = execution.getVariable(parts[0]);
		for (int i = 1; i < parts.length; i++) {
			String part = parts[i];
			o = PropertyUtils.getProperty(o, part);
		}
		return o;
	}
	protected Map getRoutingVariables(){
		info("getRoutingVariables:"+m_mapRouting);
		return m_mapRouting;
	}


	protected ActivitiEndpoint createEndpoint(ActivityExecution execution) {
		String uri = "activiti://" + getProcessDefinitionKey(execution) + ":" + execution.getActivity().getId();
		info("createEndpoint:" + uri);
		return getEndpoint(uri);
	}

	protected ActivitiEndpoint getEndpoint(String key) {
		for (Endpoint e : m_camelContext.getEndpoints()) {
			info("Endpoint:" + e + "|" + e.getEndpointKey());
			//if (e.getEndpointKey().equals(key) && (e instanceof ActivitiEndpoint)) {
			if (e instanceof ActivitiEndpoint) {
				return (ActivitiEndpoint) e;
			}
		}
		throw new RuntimeException("Activiti endpoint not defined for " + key);
	}

	protected Exchange createExchange(ActivityExecution activityExecution, ActivitiEndpoint endpoint) {
		Exchange ex = new DefaultExchange(m_camelContext);
		ex.setProperty(ActivitiProducer.PROCESS_ID_PROPERTY, activityExecution.getProcessInstanceId());
		return ex;
	}

	protected void handleCamelException(Exchange exchange) {
		Exception camelException = exchange.getException();
		boolean notHandledByCamel = exchange.isFailed() && camelException != null;
		if (notHandledByCamel) {
			//throw new ActivitiException("Unhandled exception on camel route", camelException);
			info("Unhandled exception on camel route:"+ camelException);
		}
	}

	protected String getProcessDefinitionKey(ActivityExecution execution) {
		PvmProcessDefinition processDefinition = execution.getActivity().getProcessDefinition();
		return processDefinition.getKey();
	}

	protected boolean isASync(ActivityExecution execution) {
		return execution.getActivity().isAsync();
	}

	protected String getStringFromField(Expression expression, DelegateExecution execution) {
		if (expression != null) {
			Object value = expression.getValue(execution);
			if (value != null) {
				return value.toString();
			}
		}
		return null;
	}

  public boolean isCopyCamelBodyToBodyAsString() {
    return true;
  }
  public boolean isCopyVariablesFromHeader() {
    return true;
  }
  protected void copyVariablesToProperties(Map<String, Object> variables, Exchange exchange) {
		if( variables.entrySet().size() == 0) return;
    for (Map.Entry<String, Object> var : variables.entrySet()) {
      exchange.setProperty(var.getKey(), var.getValue());
    }
  }
  protected void copyVariablesToHeader(Map<String, Object> variables, Exchange exchange) {
		if( variables.entrySet().size() == 0) return;
    for (Map.Entry<String, Object> var : variables.entrySet()) {
      exchange.getIn().setHeader(var.getKey(), var.getValue());
    }
  }
  
  protected void copyVariablesToBodyAsMap(Map<String, Object> variables, Exchange exchange) {
		if( variables.entrySet().size() == 0) return;
    exchange.getIn().setBody(new HashMap<String,Object>(variables));
  }
  
  protected void copyVariablesToBody(Map<String, Object> variables, Exchange exchange) {
		if( variables.entrySet().size() == 0) return;
    Object camelBody = variables.get(ExchangeUtils.CAMELBODY);
    if(camelBody != null) {
      exchange.getIn().setBody(camelBody);
    }
  }
	protected synchronized void createRouteBuilderClazz() {
		if (m_routeBuilderClass != null)
			return;
		String routing = unescapeJava(getRoutingDSL());
		List<String> paramNames = getParameterNames();
		paramNames.addAll(getRoutingVariables().keySet());
		String script = buildScript(routing, paramNames);
		System.out.println("m_routeBuilderScript:" + script);
		GroovyShell gs = new GroovyShell();
		m_routeBuilderClass = (Class) gs.evaluate(script);
		System.out.println("m_routeBuilderClass:" + m_routeBuilderClass);
	}

	private String buildScript(String camelDSL, List<String> paramNames) {
		String script = "import org.apache.camel.*\n";
		script += "import org.apache.camel.impl.*\n";
		script += "import org.apache.camel.builder.*\n";
		script += "import org.apache.camel.model.dataformat.*\n";
		script += "import java.util.*\n";
		script += "class DynRouteBuilder extends org.apache.camel.builder.RouteBuilder implements org.ms123.common.workflow.stencil.StencilRouteBuilder{\n";
		for (String fName : paramNames) {
			script += "String " + fName + ";";
		}
		script += "def void init(Map params) {\n";
		for (String fName : paramNames) {
			script += fName + "=params." + fName + ";\n";
		}
		script += "}\n";
		script += "def void configure() {\n";
		script += camelDSL;
		script += "}}\n";
		script += "return DynRouteBuilder.class\n";
		return script;
	}

	protected RouteBuilder newRouteBuilder() {
		try {
			RouteBuilder rb = (RouteBuilder) m_routeBuilderClass.newInstance();
			System.out.println("RouteBuilder:" + rb);
			return rb;
		} catch (Exception e) {
			throw new RuntimeException("BaseCamelBehavior.newRouteBuilder", e);
		}
	}

	public abstract void addRoute(CamelContext cc, ActivityExecution execution) throws Exception;

	public abstract String getRoutingDSL();

	public abstract List<String> getParameterNames();

	protected synchronized CamelContext createCamelContext(String namespace) throws Exception {
		Map beans = Context.getProcessEngineConfiguration().getBeans();
		BundleContext bc = (BundleContext) beans.get("bundleContext");
		Registry gr = new GroovyRegistry( BaseCamelBehavior.class.getClassLoader(), bc, namespace);
		CamelContext camelContext = CamelContextBuilder.createCamelContext(namespace,gr, bc,false);
		return camelContext;
	}

	protected void setCategoryAndName(ActivityExecution execution) {
		Map beans = Context.getProcessEngineConfiguration().getBeans();
		ProcessEngine pe = (ProcessEngine) beans.get("processEngine");
		String processDefinitionId = ((ExecutionEntity) execution).getProcessDefinitionId();
		RepositoryService repositoryService = pe.getRepositoryService();
		ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(processDefinitionId).singleResult();
		m_category = processDefinition.getCategory();
		m_processDefinitionKey = processDefinition.getKey();
		info("NS:" + m_category);
		info("EID:" + execution.getId());
		info("PID:" + execution.getProcessInstanceId());
		info("AID:" + execution.getCurrentActivityId());
		info("ANAME:" + execution.getCurrentActivityName());
		m_camelActivitiKey = m_category +"/"+processDefinition.getName()+"/"+execution.getId()+"/"+execution.getCurrentActivityId();
		info("m_camelActivitiKey:" + m_camelActivitiKey);
	}

	protected void debug(String msg) {
		System.out.println(msg);
		m_logger.debug(msg);
	}

	protected static void info(String msg) {
		System.err.println(msg);
		m_logger.info(msg);
	}

	private static final org.slf4j.Logger m_logger = org.slf4j.LoggerFactory.getLogger(BaseCamelBehavior.class);
}
