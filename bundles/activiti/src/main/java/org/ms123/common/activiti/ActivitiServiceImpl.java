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
package org.ms123.common.activiti;

import aQute.bnd.annotation.component.*;
import aQute.bnd.annotation.metatype.*;
import com.Ostermiller.util.*;
import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import java.util.Map;
import java.util.List;
import org.ms123.common.data.api.DataLayer;
import org.ms123.common.auth.api.AuthService;
import org.ms123.common.nucleus.api.NucleusService;
import org.ms123.common.rpc.PDefaultBool;
import org.ms123.common.rpc.PDefaultFloat;
import org.ms123.common.rpc.PDefaultInt;
import org.ms123.common.rpc.PDefaultLong;
import org.ms123.common.rpc.PDefaultString;
import org.ms123.common.rpc.PName;
import org.ms123.common.rpc.POptional;
import org.ms123.common.rpc.RpcException;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.git.GitService;
import org.ms123.common.libhelper.Inflector;
import org.ms123.common.utils.ParameterParser;
import org.ms123.common.utils.UtilsService;
import org.ms123.common.form.FormService;
import org.ms123.common.data.api.SessionContext;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.activiti.engine.ProcessEngine;
import org.osgi.service.event.EventAdmin;
import org.ms123.common.workflow.api.WorkflowService;
import org.ms123.common.activiti.process.ProcessDefinitionsResource;
import org.ms123.common.activiti.process.ProcessInstanceResource;
import org.ms123.common.activiti.process.ProcessInstancesResource;
import org.ms123.common.activiti.process.StartProcessInstanceResource;
import org.ms123.common.activiti.process.ProcessDefinitionDiagramResource;
import org.ms123.common.activiti.process.ProcessDefinitionCandidateResource;
import org.ms123.common.activiti.process.ProcessInstanceDiagramResource;
import org.ms123.common.activiti.task.TasksResource;
import org.ms123.common.activiti.task.TaskFormPropertiesResource;
import org.ms123.common.activiti.task.TaskOperationResource;
import org.ms123.common.activiti.repository.DeploymentsDeleteResource;
import static org.ms123.common.rpc.JsonRpcServlet.ERROR_FROM_METHOD;
import static org.ms123.common.rpc.JsonRpcServlet.INTERNAL_SERVER_ERROR;
import static org.ms123.common.rpc.JsonRpcServlet.PERMISSION_DENIED;

/** ActivitiService implementation
 */
@Component(enabled = true, configurationPolicy = ConfigurationPolicy.optional, immediate = true, properties = { "rpc.prefix=activiti" })
public class ActivitiServiceImpl extends BaseActivitiServiceImpl implements ActivitiService {

	protected Inflector m_inflector = Inflector.getInstance();


	private static final Logger m_logger = LoggerFactory.getLogger(ActivitiServiceImpl.class);


	public ActivitiServiceImpl() {
	}

	protected void activate(BundleContext bundleContext, Map<?, ?> props) {
		System.out.println("ActivitiServiceImpl.activate.props:" + m_dataLayer);
	}

	protected void deactivate() throws Exception {
		System.out.println("ActivitiServiceImpl.deactivate");
	}

	public ProcessEngine getPE() {
		return m_processEngine;
	}

	public FormService getFormService(){
		return m_formService;
	}
	public DataLayer getDataLayer(){
		return m_dataLayer;
	}
	public WorkflowService getWorkflowService(){
		return m_workflowService;
	}
	public EventAdmin getEventAdmin(){
		return m_eventAdmin;
	}
	/* BEGIN JSON-RPC-API*/
	public Map getProcessDefinitions(
			@PName("namespace")  @POptional String namespace, 
			@PName("key")  @POptional String key, 
			@PName("name")  @POptional String name, 
			@PName("version")  @POptional Integer version, 
			@PName("startableByUser")  @POptional String user, 
			@PName("startableByGroup")  @POptional String group, 
			@PName("listParams")       @POptional Map<String, Object> listParams) throws RpcException {
		try {
			ProcessDefinitionsResource pdr = new ProcessDefinitionsResource(this, listParams, namespace,key, name,version, user,group);
			return pdr.getProcessDefinitions();
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "ActivitiService.getProcessDefinitions:", e);
		}
	}

	public void setProcessDefinitionCandidates(
			@PName("processDefinitionId")  @POptional String processDefinitionId, 
			@PName("candidateUsers")  @POptional List<String> userList, 
			@PName("candidateGroups")  @POptional List<String> groupList
			) throws RpcException {
		try {
			ProcessDefinitionCandidateResource pdc = new ProcessDefinitionCandidateResource(this,processDefinitionId, userList,groupList);
			pdc.execute();
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "ActivitiService.setProcessDefinitionCandidates:", e);
		}
	}

	public Map getProcessInstance(
			@PName("processInstanceId") String processInstanceId) throws RpcException {
		ClassLoader saveCl = _setContextClassLoader( processInstanceId);
		try {
			ProcessInstanceResource pir = new ProcessInstanceResource(this, processInstanceId);
			return pir.getProcessInstance();
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "ActivitiService.getProcessInstance:", e);
		}finally{
			Thread.currentThread().setContextClassLoader(saveCl);
		}
	}

	@RequiresRoles("admin")
	public Map deleteProcessInstance(
			@PName("processInstanceId") String processInstanceId,
			@PName("reason")  @POptional String reason 
				) throws RpcException {
		ClassLoader saveCl = _setContextClassLoader( processInstanceId);
		try {
			ProcessInstanceResource pir = new ProcessInstanceResource(this, processInstanceId, reason);
			return pir.deleteProcessInstance();
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "ActivitiService.deleteProcessInstance:", e);
		}finally{
			Thread.currentThread().setContextClassLoader(saveCl);
		}
	}

	public Map getProcessInstances(
			@PName("namespace") String namespace, 
			@PName("processDefinitionId") @POptional String processDefinitionId, 
			@PName("processDefinitionKey") @POptional String processDefinitionKey, 
			@PName("businessKey")      @POptional String businessKey, 
			@PName("unfinished")       @POptional Boolean unfinished, 
			@PName("finished")         @POptional Boolean finished, 
			@PName("listParams")       @POptional Map<String, Object> listParams) throws RpcException {
		try {
			if (processDefinitionId == null && processDefinitionKey == null) {
				throw new RuntimeException("getProcessInstance.no processDefinition{Id,Key}");
			}
			ProcessInstancesResource pir = new ProcessInstancesResource(this, listParams, processDefinitionId, processDefinitionKey, businessKey, unfinished, finished,namespace);
			return pir.getProcessInstances();
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "ActivitiService.getProcessInstances:", e);
		}
	}

	public Map startProcessInstance(
			//@PName(StoreDesc.STORE_ID) @POptional String storeId, 
			@PName("namespace") @POptional String namespace, 
			@PName("version") @POptional Integer version, 
			@PName("processDefinitionId") @POptional String processDefinitionId, 
			@PName("processDefinitionKey") @POptional String processDefinitionKey, 
			@PName("processDefinitionName") @POptional String processDefinitionName, 
			@PName("messageName") @POptional String messageName, 
			@PName("businessKey")      @POptional String businessKey, 
			@PName("startParams")      @POptional Map<String, Object> startParams) throws RpcException {
		try {
			if (processDefinitionId == null && processDefinitionKey == null && processDefinitionName == null && messageName == null) {
				throw new RuntimeException("startProcessInstance.no processDefinition{Id,Key,Name,MessageName}");
			}
			StartProcessInstanceResource spir = new StartProcessInstanceResource(this, namespace,version, processDefinitionId, processDefinitionKey, processDefinitionName,messageName, businessKey, startParams);
			return spir.startProcessInstance();
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "ActivitiService.startProcessInstance:", e);
		}
	}

	public Map getVariables(
			@PName("executionId")      String executionId) throws RpcException {
		ClassLoader saveCl = _setContextClassLoader( executionId);
		try {
			return m_processEngine.getRuntimeService().getVariables(executionId);
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "ActivitiService.getVariables:", e);
		}finally{
			Thread.currentThread().setContextClassLoader(saveCl);
		}
	}

	public Map getTasks(
			@PName("queryParams")      @POptional Map<String, Object> queryParams, 
			@PName("listParams")       @POptional Map<String, Object> listParams) throws RpcException {
		//ClassLoader saveCl = _setContextClassLoader( (String)queryParams.get("processInstanceId"));
		try {
			TasksResource tr = new TasksResource(this, listParams, queryParams);
			return tr.getTasks();
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "ActivitiService.getTasks:", e);
		}finally{
			//Thread.currentThread().setContextClassLoader(saveCl);
		}
	}

	public Map getTaskFormProperties(
			@PName("executionId")      String executionId, 
			@PName("taskId")           String taskId) throws RpcException {
		ClassLoader saveCl = _setContextClassLoader( executionId);
		try {
			TaskFormPropertiesResource tr = new TaskFormPropertiesResource(this, executionId, taskId);
			return tr.getTaskFormProperties();
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "ActivitiService.getTaskFormProperties:", e);
		}finally{
			Thread.currentThread().setContextClassLoader(saveCl);
		}
	}

	public Map executeTaskOperation(
			@PName("taskId")           String taskId, 
			@PName("operation")        String operation, 
			@PName("startParams")      @POptional Map<String, Object> startParams) throws RpcException {
		try {
			TaskOperationResource tr = new TaskOperationResource(this, taskId, operation, startParams);
			return tr.executeTaskOperation();
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "ActivitiService.executeTaskOperation:", e);
		}
	}

	public String getDefinitionDiagram(
			@PName("processDefinitionId") String processDefinitionId) throws RpcException {
		try {
			ProcessDefinitionDiagramResource pir = new ProcessDefinitionDiagramResource(this, processDefinitionId);
			return pir.getDiagram();
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "ActivitiService.getDefintionDiagram:", e);
		}
	}

	public String getInstanceDiagram(
			@PName("processInstanceId") String processInstanceId) throws RpcException {
		try {
			ProcessInstanceDiagramResource pir = new ProcessInstanceDiagramResource(this, processInstanceId);
			return pir.getDiagram();
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "ActivitiService.getInstanceDiagram:", e);
		}
	}

	@RequiresRoles("admin")
	public Map deleteDeployments(
			@PName("deploymentIds") List<String> deploymentIds,
			@PName("cascade")         @POptional @PDefaultBool(false) Boolean cascade 
				) throws RpcException {
		try {
			DeploymentsDeleteResource ddr = new DeploymentsDeleteResource(this, deploymentIds, cascade);
			return ddr.execute();
		} catch (Exception e) {
			throw new RpcException(ERROR_FROM_METHOD, INTERNAL_SERVER_ERROR, "ActivitiService.deleteDeployments:", e);
		}
	}

	/* END JSON-RPC-API*/
	@Reference(dynamic = true)
	public void setWorkflowService(WorkflowService paramWorkflowService) {
		m_workflowService = paramWorkflowService;
		m_processEngine = paramWorkflowService.getProcessEngine();
	}

	@Reference(target = "(kind=jdo)", dynamic = true, optional = true)
	public void setDataLayer(DataLayer dataLayer) {
		System.out.println("ActivitiServiceImpl.setDataLayer:" + dataLayer);
		m_dataLayer = dataLayer;
	}

	@Reference(dynamic = true,optional=true)
	public void setFormService(FormService paramFormService) {
		this.m_formService = paramFormService;
		System.out.println("ActivitiServiceImpl.setFormService:" + paramFormService);
	}

	@Reference(dynamic = true,optional=true)
	public void setEventAdmin(EventAdmin paramEventAdmin) {
		System.out.println("ActivitiServiceImpl.setEventAdmin:" + paramEventAdmin);
		this.m_eventAdmin = paramEventAdmin;
	}

	@Reference(dynamic = true)
	public void setNucleusService(NucleusService paramService) {
		this.m_nucleusService = paramService;
		System.out.println("ActivitiServiceImpl.setNucleusService:" + paramService);
	}
}
