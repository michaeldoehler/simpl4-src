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
package org.ms123.common.activiti.process;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.io.InputStream;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.runtime.ProcessInstance;
import org.ms123.common.activiti.ActivitiService;
import org.ms123.common.activiti.BaseResource;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.task.IdentityLink;
import org.apache.commons.io.IOUtils;
import flexjson.*;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.Event;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCause;

/**
 */
@SuppressWarnings("unchecked")
public class StartProcessInstanceResource extends BaseResource {

	private String m_processDefinitionId;

	private String m_processDefinitionKey;

	private String m_processDefinitionName;

	private String m_businessKey;

	private String m_messageName;

	private String m_namespace;

	private Integer m_version;

	private Map<String, Object> m_startParams;

	public StartProcessInstanceResource(ActivitiService as, String namespace, Integer version, String processDefinitionId, String processDefinitionKey, String processDefinitionName, String messageName, String businessKey, Map<String, Object> startParams) {
		super(as, null);
		m_namespace = namespace;
		m_processDefinitionId = processDefinitionId;
		m_processDefinitionKey = processDefinitionKey;
		m_processDefinitionName = processDefinitionName;
		m_businessKey = businessKey;
		m_messageName = messageName;
		m_startParams = startParams;
		m_version = version;
	}

	public Map startProcessInstance() {
		ProcessDefinitionEntity processDefinition=null;
		String uid=null;
		try {
			Map<String, Object> variables = m_startParams;
			if (variables != null) {
				variables.remove("processDefinitionId");
				variables.remove("processDefinitionKey");
				variables.remove("processDefinitionName");
				variables.remove("businessKey");
			} else {
				variables = new HashMap();
			}
			uid = org.ms123.common.system.ThreadContext.getThreadContext().getUserName();
			getPE().getIdentityService().setAuthenticatedUserId(uid);
			System.out.println("StartProcessInstanceResource:");
			System.out.println("\tm_processDefinitionId:" + m_processDefinitionId);
			System.out.println("\tm_processDefinitionKey:" + m_processDefinitionKey);
			System.out.println("\tm_processDefinitionName:" + m_processDefinitionName);
			System.out.println("\tm_messageName:" + m_messageName);
			if (m_processDefinitionKey != null) {
				ProcessDefinitionQuery query = getPE().getRepositoryService().createProcessDefinitionQuery();
				query = addVersion(query);
				processDefinition = (ProcessDefinitionEntity) query.
					processDefinitionKey(m_processDefinitionKey).
					processDefinitionCategory(m_namespace).
					singleResult();
				if (processDefinition == null) {
					throw new RuntimeException("No process with processDefinitionKey(" + m_processDefinitionKey + ") in namespace(" + m_namespace + ")");
				}
			} else if (m_messageName != null) {
				ProcessDefinitionQuery query =  getPE().getRepositoryService().createProcessDefinitionQuery();
				query = addVersion(query);
				processDefinition = (ProcessDefinitionEntity)query.
					messageEventSubscriptionName(m_messageName).
					processDefinitionCategory(m_namespace).
					singleResult();
				if (processDefinition == null) {
					throw new RuntimeException("No process with messageName(" + m_messageName + ") in namespace(" + m_namespace + ")");
				}
			} else if (m_processDefinitionName != null) {
				processDefinition = (ProcessDefinitionEntity) getPE().getRepositoryService().createProcessDefinitionQuery().
					processDefinitionName(m_processDefinitionName).
					processDefinitionCategory(m_namespace).
					singleResult();
				if (processDefinition == null) {
					throw new RuntimeException("No process with processDefinitionName(" + m_processDefinitionName + ") in namespace(" + m_namespace + ")");
				}
			} else {
				processDefinition = (ProcessDefinitionEntity) getPE().getRepositoryService().createProcessDefinitionQuery().
					processDefinitionId(m_processDefinitionId).
					processDefinitionCategory(m_namespace).
					singleResult();
				if (processDefinition == null) {
					throw new RuntimeException("No process with processDefinitionId(" + m_processDefinitionId + ") in namespace(" + m_namespace + ")");
				}
			}
			if (m_processDefinitionId == null) {
				m_processDefinitionId = processDefinition.getId();
			}
			List<IdentityLink> links = getPE().getRepositoryService().getIdentityLinksForProcessDefinition(m_processDefinitionId);
			System.out.println("CandidateLinks:" + links);
			if (links.size() > 0) {
				processDefinition = (ProcessDefinitionEntity) getPE().getRepositoryService().createProcessDefinitionQuery().
					processDefinitionId(processDefinition.getId()).
					startableByUser(uid).
					singleResult();
				if (processDefinition == null) {
					throw new RuntimeException("User(" + uid + ") not allowed to start process with processDefinitionId(" + m_processDefinitionId + ") in namespace(" + m_namespace + ")");
				}
			}
			String deploymentId = processDefinition.getDeploymentId();
			RepositoryService rs = getPE().getRepositoryService();
			try {
				InputStream is = rs.getResourceAsStream(deploymentId, "initialParameter");
				Map params = (Map) new JSONDeserializer().deserialize(IOUtils.toString(is));
				if (params.get("items") != null) {
					List<Map> items = (List) params.get("items");
					for (Map<String, String> item : items) {
						String name = item.get("name");
						Object value = item.get("value");
						if( value instanceof String ){
							String v = ((String)value).trim();
							if( v.length() > 1 && (v.startsWith("{") || v.startsWith("["))){
								value = new JSONDeserializer().deserialize(v);
							}
						}
						System.out.println("put:" + name + "=" + value);
						variables.put(name, value);
					}
				}
				variables.put("__currentUser", uid);
				variables.put("__startingUser", uid);
				variables.put("__namespace", m_namespace);
			} catch (Exception e) {
				if (e instanceof NullPointerException) {
					e.printStackTrace();
				}
				System.out.println("getResourceAsStream:" + e);
			}
			ProcessInstance processInstance = getPE().getRuntimeService().startProcessInstanceById(processDefinition.getId(), m_businessKey, variables);
			variables.put("__processInstanceId", processInstance.getProcessInstanceId());
			System.out.println("StartProcessInstanceResource.variables:" + variables);
			Map<String, String> response = new HashMap();
			response.put("id", processInstance.getId());
			response.put("businessKey", processInstance.getBusinessKey());
			response.put("processInstanceId", processInstance.getProcessInstanceId());
			response.put("processDefinitionId", processInstance.getProcessDefinitionId());
			return response;
		} catch (Exception e) {
			createLogEntry(processDefinition,uid, e);
			if (e instanceof RuntimeException)
				throw (RuntimeException) e;
			throw new RuntimeException("Failed to retrieve the process definition parameters", e);
		}
	}

	private ProcessDefinitionQuery addVersion(ProcessDefinitionQuery query) {
		if (m_version != null) {
			if (m_version != -1) {
				query = query.processDefinitionVersion(m_version);
			} else {
				query = query.latestVersion();
			}
		}
		return query;
	}
	private void createLogEntry(ProcessDefinitionEntity processDefinition, String uid, Exception e){
		info("createLogEntry.StartProcessInstanceResource");
		Map props = new HashMap();
		Map hint = new HashMap();
		props.put("type", "exception/startprocess");
		props.put("key", m_namespace+"/"+processDefinition.getId());
		info("createLogEntry.props:" + props);
		Throwable r = getRootCause(e);
		props.put("msg", r != null ? getStackTrace(r) : getStackTrace(e));
		hint.put("processDefinitionId", processDefinition.getId());
		hint.put("processDefinitionKey", processDefinition.getKey());
		hint.put("processDefinitionName", processDefinition.getName());
		hint.put("processDeploymentId", processDefinition.getDeploymentId());
		hint.put("startUserId", uid);
		props.put("hint", m_js.deepSerialize(hint));
		getEventAdmin().sendEvent(new Event("log", props));
	}
}
