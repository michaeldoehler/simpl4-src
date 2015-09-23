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

import java.io.FileInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Collection;
//import org.activiti.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.activiti.spring.SpringProcessEngineConfiguration;
import org.activiti.engine.impl.scripting.BeansResolverFactory;
import org.activiti.engine.impl.scripting.ResolverFactory;
import org.activiti.engine.impl.scripting.ScriptBindingsFactory;
import org.activiti.engine.impl.scripting.ScriptingEngines;
import org.activiti.engine.impl.scripting.VariableScopeResolverFactory;
import org.activiti.engine.impl.interceptor.SessionFactory;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.delegate.VariableScope;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.ProcessEngines;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.repository.Deployment;
import org.ms123.common.workflow.processengine.ProcessEngineFactory;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.script.ScriptEngineManager;
import org.ms123.common.data.api.DataLayer;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import aQute.bnd.annotation.metatype.*;
import aQute.bnd.annotation.component.*;
import org.ms123.common.utils.ScriptEngineService;
import org.ms123.common.libhelper.FileSystemClassLoader;
import org.ms123.common.utils.Utils;
import org.ms123.common.git.GitService;
import org.ms123.common.system.tm.TransactionService;
import org.ms123.common.stencil.api.StencilService;
import org.ms123.common.workflow.converter.Simpl4BpmnJsonConverter;
import javax.servlet.http.*;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.ms123.common.permission.api.PermissionService;
import org.ms123.common.activiti.ActivitiService;
import org.ms123.common.activiti.process.ProcessDefinitionResponse;
import org.ms123.common.docbook.DocbookService;
import org.ms123.common.auth.api.AuthService;
import org.ms123.common.utils.IOUtils;
import org.ms123.common.rpc.PName;
import org.ms123.common.rpc.POptional;
import org.ms123.common.rpc.PDefaultString;
import org.ms123.common.rpc.PDefaultInt;
import org.ms123.common.rpc.PDefaultLong;
import org.ms123.common.rpc.PDefaultBool;
import org.ms123.common.rpc.PDefaultFloat;
import org.ms123.common.rpc.RpcException;
import static org.ms123.common.rpc.JsonRpcServlet.ERROR_FROM_METHOD;
import static org.ms123.common.rpc.JsonRpcServlet.INTERNAL_SERVER_ERROR;
import static org.ms123.common.rpc.JsonRpcServlet.PERMISSION_DENIED;
import flexjson.*;
import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.ActivitiListener;
import org.activiti.bpmn.model.ImplementationType;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.activiti.bpmn.model.Process;
import org.apache.camel.CamelContext;
import javax.sql.DataSource;
import org.ms123.common.workflow.api.WorkflowService;
import org.activiti.engine.impl.interceptor.CommandContextFactory;
import bitronix.tm.resource.jdbc.PoolingDataSource;

/** WorkflowService implementation
 */
@SuppressWarnings("unchecked")
@Component(enabled = true, configurationPolicy = ConfigurationPolicy.optional, immediate = true, properties = { "rpc.prefix=workflow" })
public class WorkflowServiceImpl implements org.ms123.common.workflow.api.WorkflowService,EventHandler {

	private static final Logger m_logger = LoggerFactory.getLogger(WorkflowServiceImpl.class);

	protected JSONSerializer m_js = new JSONSerializer();

	protected JSONDeserializer m_ds = new JSONDeserializer();
	protected ProcessEngineFactory m_processEngineFactory;
	private DataSource m_dataSource;

	private String m_namespace;

	private String m_workspace;

	private ProcessEngine m_processEngine = null;
	private ShiroJobExecutor m_shiroJobExecutor;

	private EventAdmin m_eventAdmin;

	private SpringProcessEngineConfiguration m_processEngineConfiguration = new SpringProcessEngineConfiguration();

	private ScriptEngineService m_scriptEngineService;

	protected PermissionService m_permissionService;

	protected ActivitiService m_activitiService;
	protected TransactionService m_transactionService;

	protected AuthService m_authService;

	protected GitService m_gitService;
	protected StencilService m_stencilService;

	private DataLayer m_dataLayer;

	private BundleContext m_bundleContext;

	final static String[] topics = new String[] {
		"task/classes_generated"
	};
	public void handleEvent(Event event) {
		System.out.println("WorkflowServiceImpl1.Event: " + event);
		try{
			if( "task/classes_generated".equals(event.getTopic())){
				System.out.println("WorkflowServiceImpl2.Event: " + event);
				m_processEngineFactory.setFsClassLoader(createFsClassLoader2());
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
		}
	}
	public WorkflowServiceImpl() {
		m_processEngineConfiguration.setBeans(new HashMap());
		m_js.prettyPrint(true);
	}

	protected void activate(BundleContext bundleContext, Map<?, ?> props) {
		System.out.println("WorkflowServiceImpl.activate");
		//initProcessEngine(bundleContext);
		m_bundleContext = bundleContext;
		Dictionary d = new Hashtable();
		d.put(EventConstants.EVENT_TOPIC, topics);
		m_bundleContext.registerService(EventHandler.class.getName(), this, d);
	}

	protected void deactivate() {
		System.out.println("WorkflowServiceImpl.deactivate");
		m_shiroJobExecutor.shutdown();
		if( m_processEngine != null){
			m_processEngine.close();
		}
		((DataSourceWrapper)m_dataSource).destroy();
		h2Close(m_dataSource);
		m_dataSource = null;
	}

	private ClassLoader createFsClassLoader1(){
		String sh = System.getProperty("workspace");
		File[] locations = new File[1];
		locations[0] = new File(sh + "/java", "classes");
		return new FileSystemClassLoader(org.activiti.engine.impl.javax.el.ExpressionFactory.class.getClassLoader(), locations);
	}
	private ClassLoader createFsClassLoader2(){
		String sh = System.getProperty("workspace");
		File[] locations = new File[1];
		locations[0] = new File(sh + "/java", "classes");
		return new FileSystemClassLoader(locations);
	}
	public ProcessEngine getProcessEngine() {
		if (m_processEngine == null) {
			initProcessEngine(m_bundleContext);
		}
		return m_processEngine;
	}

	private DataSource getDataSource(String url){
		if( m_dataSource!=null) return m_dataSource;

		DataSource _ds = null;
		if( m_transactionService.getJtaLocator().equals("bitronix")){
			_ds = getPoolingDataSource(url);
		}else{
			org.h2.jdbcx.JdbcDataSource ds = new org.h2.jdbcx.JdbcDataSource();
			ds.setUser("sa");
			ds.setPassword("");
			ds.setURL(url);
			_ds = ds;
		}
		m_dataSource = new DataSourceWrapper(_ds);
		return m_dataSource;
	}
	private PoolingDataSource getPoolingDataSource(String url){
		PoolingDataSource ds = new PoolingDataSource();
		ds.setClassName("org.h2.jdbcx.JdbcDataSource");
		ds.setUniqueName("activiti");
		ds.setMaxPoolSize(15);
		ds.setAllowLocalTransactions(true);
		ds.setTestQuery("SELECT 1");
		ds.getDriverProperties().setProperty("user", "sa");
		ds.getDriverProperties().setProperty("password", "");
		ds.getDriverProperties().setProperty("URL", url);    
		return ds;
	}
	private SpringProcessEngineConfiguration initProcessEngine(BundleContext bundleContext) {
		SpringProcessEngineConfiguration c = m_processEngineConfiguration;
		ScriptingEngines se = new ScriptingEngines(m_scriptEngineService.getScriptEngineManager());
		List resolverFactories = new ArrayList<ResolverFactory>();
		resolverFactories.add(new VariableScopeResolverFactory());
		resolverFactories.add(new BeansResolverFactory());
		se.setScriptBindingsFactory(new ScriptBindingsFactory(resolverFactories));
		c.setScriptingEngines(se);
		CommandContextFactory ccf = createDefaultCommandContextFactory();
		ccf.setProcessEngineConfiguration(c);
		c.setCommandContextFactory(ccf);

		c.setDatabaseType("h2");
		String sh = System.getProperty("workspace");
		c.setDataSource(getDataSource("jdbc:h2:file:" + sh + "/activiti/h2;DB_CLOSE_DELAY=1000"));
		c.setDatabaseSchemaUpdate("true");
		c.setJdbcMaxActiveConnections(100);
		c.setJdbcMaxIdleConnections(25);
		m_shiroJobExecutor = new ShiroJobExecutor(c.getBeans());
		c.setJobExecutor(m_shiroJobExecutor);

		c.setClassLoader(createFsClassLoader1());
		c.setJobExecutorActivate(true);
		c.setTransactionManager( m_transactionService.getPlatformTransactionManager());
		c.setTransactionsExternallyManaged(true);
		c.setHistory("full");
		c.setMailServerHost("127.0.0.1");
		GroovyExpressionManager exManager = new GroovyExpressionManager();
		c.setExpressionManager(exManager);
		c.setIdentityService(new IdentityServiceImpl());
		c.getBeans().put("bundleContext", bundleContext);
		List<SessionFactory> customSessionFactories = c.getCustomSessionFactories();
		if (customSessionFactories == null) {
			customSessionFactories = new ArrayList<SessionFactory>();
		}
		customSessionFactories.add(new Simpl4GroupManagerFactory(m_authService, m_permissionService));
		customSessionFactories.add(new Simpl4UserManagerFactory(m_authService, m_permissionService));
		c.setCustomSessionFactories(customSessionFactories);
		ProcessEngineFactory pef = new ProcessEngineFactory();
		pef.setBundle(bundleContext.getBundle());
		pef.setProcessEngineConfiguration(c);
		try {
			pef.init(createFsClassLoader2());
			m_processEngine = pef.getObject();
			c.getBeans().put(PROCESS_ENGINE, m_processEngine);
			c.getBeans().put(WORKFLOW_SERVICE, this);
			//c.getBeans().put(CamelService.CAMEL_SERVICE, m_camelService);
			c.getBeans().put(PermissionService.PERMISSION_SERVICE, m_permissionService);
			exManager.setProcessEngine(m_processEngine);
			m_shiroJobExecutor.setProcessEngine(m_processEngine);
		} catch (Exception e) {
			m_logger.error("WorkflowServiceImpl.activate.initProcessEngine", e);
			e.printStackTrace();
		}
		m_processEngineFactory = pef;
		return c;
	}

 	private CommandContextFactory createDefaultCommandContextFactory() {
    return new CommandContextFactory();
  }

	public CamelContext getCamelContextForProcess(String namespace, String processname) {
		throw new RuntimeException("WorkflowServiceImpl.getCamelContextForProcess not allowed");
	}

	public Object lookupServiceByName(String name) {
		BundleContext bc = m_bundleContext;
		Object service = null;
		ServiceReference sr = bc.getServiceReference(name);
		if (sr != null) {
			service = bc.getService(sr);
		}
		if (service == null) {
			throw new RuntimeException("CamelBehaviorDefaultImpl.Cannot resolve service:" + name);
		}
		return service;
	}

	public void executeScriptTask( String executionId, String category, String processDefinitionKey, String pid, String script, Map newVariables, String taskName ){
		TaskScriptExecutor sce = new TaskScriptExecutor();
		VariableScope vs = new RuntimeVariableScope(m_processEngine.getRuntimeService(), executionId);
		sce.execute(category,processDefinitionKey, pid, script, newVariables, vs,taskName, m_dataLayer,(WorkflowService)this);
	}

	private ActivitiListener createListener(String event, String clazz) {
		ActivitiListener listener = new ActivitiListener();
		listener.setEvent(event);
		listener.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_CLASS);
		listener.setImplementation(clazz);
		return listener;
	}

	private byte[] getBpmnXML(String processJson, String ns, String path) throws Exception {
		Simpl4BpmnJsonConverter jsonConverter = new Simpl4BpmnJsonConverter(ns,m_stencilService);
		JsonNode editorNode = new ObjectMapper().readTree(processJson);
		BpmnModel bpmnModel = jsonConverter.convertToBpmnModel(editorNode);
		bpmnModel.setTargetNamespace(ns);
		for (Process process : bpmnModel.getProcesses()) {
			if (process.getId() == null) {
				process.setId(getBasename(path));
			}
			process.getExecutionListeners().add(createListener("start", "org.ms123.common.workflow.ProcessStartExecutionListener"));
			process.getExecutionListeners().add(createListener("end", "org.ms123.common.workflow.ProcessEndExecutionListener"));
			//Collection<FlowElement> flowElements = process.getFlowElements();
			//System.out.println("flowElements:"+flowElements);
			//for( FlowElement fe : flowElements){
				//fe.getExecutionListeners().add(createListener("end", "org.ms123.common.workflow.ProcessEndExecutionListener"));
			//}
		}
		BpmnXMLConverter xmlConverter = new BpmnXMLConverter();
		//System.out.println("WorkflowServiceImpl.bpmnModel:"+m_js.deepSerialize(bpmnModel));
		byte[] bpmnBytes = xmlConverter.convertToXML(bpmnModel);
		return bpmnBytes;
	}

	public Map testRules(
			@PName("namespace")        String namespace, 
			@PName("name")             String name, 
			@PName("values")           Map values) throws RpcException {
		try {
			Map rules = getRules(name, namespace);
			RulesProcessor rp = new RulesProcessor(rules, values);
			Map ret = rp.execute();
			return ret;
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "WorkflowService.testRules:", e);
		}
	}

	private Map getRules(String name, String namespace) {
		String filterJson = m_gitService.searchContent(namespace, name, "sw.rule");
		Map contentMap = (Map) m_ds.deserialize(filterJson);
		return contentMap;
	}

	@RequiresRoles("admin")
	public void getBpmn(
			@PName("namespace")        String namespace, 
			@PName("path")             String path, HttpServletResponse response) throws RpcException {
		try {
			String processJson = m_gitService.getFileContent(namespace, path);
			byte[] bpmnBytes = getBpmnXML(processJson, namespace, path);
			response.setContentType("application/xml");
			response.addHeader("Content-Disposition", "inline;filename=xxx.bpmn20.xml");
			IOUtils.write(bpmnBytes, response.getOutputStream());
			response.setStatus(HttpServletResponse.SC_OK);
			response.getOutputStream().close();
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "WorkflowService.getBpmn:", e);
		}
	}

	@RequiresRoles("admin")
	private Object deployProcess(String namespace, String path, boolean deploy, boolean all) throws Exception {
		String processJson = m_gitService.getFileContent(namespace, path);
		String deploymentId = null;
		String basename = getBasename(path);
		RepositoryService rs = getProcessEngine().getRepositoryService();
		List<Deployment> dl = null;
		if (all) {
			dl = rs.createDeploymentQuery().deploymentName(basename).list();
		} else {
			dl = rs.createDeploymentQuery().deploymentName(basename).deploymentCategory(namespace).list();
		}
		System.out.println("Deployment:" + dl);
	/*	if (dl != null && dl.size() > 0) {
			for (Deployment dm : dl) {
				System.out.println("Deployment:" + dm.getName() + "/" + dm.getId());
				rs.deleteDeployment(dm.getId(), true);
			}
		}*/
		if (deploy) {
			Map shape = (Map) m_ds.deserialize(processJson);
			Map<String, Object> properties = (Map) shape.get("properties");
			Object m = properties.get("initialparameter");
			String initialParameter = null;
			if (m instanceof Map) {
				initialParameter = m_js.deepSerialize(m);
			} else {
				initialParameter = (String) properties.get("initialparameter");
				if (initialParameter == null) {
					initialParameter = m_js.deepSerialize(new HashMap());
				}
			}
			byte[] bpmnBytes = getBpmnXML(processJson, namespace, path);
			InputStream bais = new ByteArrayInputStream(bpmnBytes);
			DeploymentBuilder deployment = rs.createDeployment();
			deployment.name(basename);
			deployment.category(namespace);
			deployment.addString("initialParameter", initialParameter);
			deploymentId = deployment.addInputStream(basename + ".bpmn20.xml", bais).deploy().getId();
			System.out.println("deploymentId:" + deploymentId);
			Map pdefs = m_activitiService.getProcessDefinitions(namespace, basename, null, -1, null, null, null);
			List<ProcessDefinitionResponse> pList = (List) pdefs.get("data");
			m_js.prettyPrint(true);
			System.out.println("PList:" + m_js.deepSerialize(pList));
			if (pList.size() != 1) {
				throw new RuntimeException("WorkflowService.deployProcess(" + namespace + "," + basename + "):not " + (pList.size() == 0 ? "found" : "uniqe"));
			}
			String processdefinitionId = pList.get(0).getId();
			String groups = (String) properties.get("startablegroups");
			List groupList = null;
			if (groups != null && groups.trim().length() > 0) {
				groupList = new ArrayList<String>(Arrays.asList(groups.split(",")));
			}
			List userList = null;
			String users = (String) properties.get("startableusers");
			if (users != null && users.trim().length() > 0) {
				userList = new ArrayList<String>(Arrays.asList(users.split(",")));
			}
			System.out.println("userList:" + userList + "/grList:" + groupList);
			m_activitiService.setProcessDefinitionCandidates(processdefinitionId, userList, groupList);
		} else {
			deploymentId = null;
		}
		Map map = new HashMap();
		map.put("deploymentId", deploymentId);
		return map;
	}

	private String getBasename(String path) {
		String e[] = path.split("/");
		return e[e.length - 1];
	}

	protected Object checkNull(Object o, String msg) {
		if (o == null) {
			throw new RuntimeException(msg);
		}
		return o;
	}

	public synchronized void h2Close(DataSource ds) {
		if( m_transactionService.getJtaLocator().equals("bitronix")){
			((PoolingDataSource)((DataSourceWrapper)m_dataSource).getDataSource()).close();
		}else{
			java.sql.Connection conn = null;
			try {
				conn = ds.getConnection();
				java.sql.Statement stat = conn.createStatement();
				stat.execute("shutdown compact");
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					conn.close();
				} catch (Exception e) {
				}
			}
		}
	}

	/* BEGIN JSON-RPC-API*/
	@RequiresRoles("admin")
	public Object deployProcess(
			@PName("namespace")        String namespace, 
			@PName("path")             String path) throws RpcException {
		try {
			return deployProcess(namespace, path, true, false);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "WorkflowService.deployProcess:", e);
		} finally {
		}
	}

	@RequiresRoles("admin")
	public Object undeployProcess(
			@PName("namespace")        String namespace, 
			@PName("path")             String path, 
			@PName("all")              @PDefaultBool(false) @POptional Boolean all) throws RpcException {
		try {
			return deployProcess(namespace, path, false, all);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "WorkflowService.undeployProcess:", e);
		} finally {
		}
	}

	@RequiresRoles("admin")
	public Object saveProcess(
			@PName("namespace")        String namespace, 
			@PName("path")             String path, 
			@PName("data")             Map data) throws RpcException {
		try {
			m_gitService.putContent(namespace, path, "sw.process", m_js.deepSerialize(data));
			return deployProcess(namespace, path, true, false);
		} catch (Throwable e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "WorkflowService.saveProcess:", e);
		} finally {
		}
	}

	/* END JSON-RPC-API*/
	@Reference(dynamic = true, optional = true)
	public void setGitService(GitService gitService) {
		this.m_gitService = gitService;
		System.out.println("WorkflowServiceImpl.setGitService:" + gitService);
		m_processEngineConfiguration.getBeans().put("gitService", gitService);
	}

	@Reference(target = "(kind=jdo)", dynamic = true, optional = true)
	public void setDataLayer(DataLayer dataLayer) {
		System.out.println("WorkflowServiceImpl.setDataLayer:" + dataLayer);
		m_dataLayer = dataLayer;
		m_processEngineConfiguration.getBeans().put(DataLayer.DATA_LAYER, dataLayer);
	}

	@Reference(dynamic = true)
	public void setEventAdmin(EventAdmin paramEventAdmin) {
		System.out.println("WorkflowServiceImpl.setEventAdmin:" + paramEventAdmin);
		this.m_eventAdmin = paramEventAdmin;
		m_processEngineConfiguration.getBeans().put("eventAdmin", paramEventAdmin);
	}

	@Reference
	public void setScriptEngineService(ScriptEngineService paramService) {
		m_scriptEngineService = paramService;
		System.out.println("WorkflowServiceImpl.setScriptEngineService:" + paramService);
	}

	@Reference(multiple = false, dynamic = true, optional = true)
	public void setAuthService(AuthService paramAuthService) {
		this.m_authService = paramAuthService;
		System.out.println("WorkflowServiceImpl.setAuthService:" + paramAuthService);
	}

	@Reference(multiple = false, dynamic = true, optional = true)
	public void setDocbookService(DocbookService paramDocbookService) {
		System.out.println("WorkflowServiceImpl.setDocbookService:" + paramDocbookService);
		m_processEngineConfiguration.getBeans().put("docbookService", paramDocbookService);
	}

	@Reference(multiple = false, dynamic = true, optional=true)
	public void setPermissionService(PermissionService paramPermissionService) {
		this.m_permissionService = paramPermissionService;
		System.out.println("WorkflowServiceImpl.setPermissionService:" + paramPermissionService);
	}
	@Reference(multiple = false, dynamic = true, optional = true)
	public void setActivitiService(ActivitiService paramActivitiService) {
		this.m_activitiService = paramActivitiService;
		System.out.println("WorkflowServiceImpl.setActivitiService:" + paramActivitiService);
	}
	@Reference(multiple = false, dynamic = true, optional = true)
	public void setStencilService(StencilService paramActivitiService) {
		this.m_stencilService = paramActivitiService;
		System.out.println("WorkflowServiceImpl.setStencilService:" + paramActivitiService);
	}
	@Reference(multiple = false, dynamic = true, optional = true)
	public void setTransactionService(TransactionService paramActivitiService) {
		this.m_transactionService = paramActivitiService;
		System.out.println("WorkflowServiceImpl.setTransactionService:" + paramActivitiService);
		m_processEngineConfiguration.getBeans().put("transactionService", paramActivitiService);
	}
}
