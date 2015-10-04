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

import java.util.Map;
import java.util.HashMap;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.activiti.engine.impl.pvm.delegate.ActivityExecution;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.ms123.common.camel.api.CamelService;
import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.CamelContext;

public class CamelBehaviorDefaultImpl extends org.activiti.camel.impl.CamelBehaviorDefaultImpl {

	String m_category, m_processDefinitionKey;

	protected void setAppropriateCamelContext(ActivityExecution execution) {
		info("getProcessVariables:" + execution.getVariables());
		Map vars = execution.getVariables();
		String ns = (String) vars.get("__namespace");
		Map beans = Context.getProcessEngineConfiguration().getBeans();
		setCategoryAndName(execution);
		CamelService cs = (CamelService) lookupServiceByName(CamelService.class.getName());
		info("m_category:" + m_category + "/" + ns + "/" + cs);
		camelContextObj = cs.getCamelContext(ns);
		info("camelContextObj:" + camelContextObj);
	}

	protected ActivitiEndpoint getEndpoint(String key) {
		info("getEndpoint.key:" + key);
		for (Endpoint e : camelContextObj.getEndpoints()) {
			info("\tgetEndpoint.e:" + e + "/" + e.getEndpointKey());
			if (e.getEndpointKey().equals(key) && (e instanceof ActivitiEndpoint)) {
				return (ActivitiEndpoint) e;
			}
		}
		throw new RuntimeException("Activiti endpoint not defined for " + key);
	}

	protected void setCategoryAndName(ActivityExecution execution) {
		Map beans = Context.getProcessEngineConfiguration().getBeans();
		ProcessEngine pe = (ProcessEngine) beans.get("processEngine");
		String processDefinitionId = ((ExecutionEntity) execution).getProcessDefinitionId();
		RepositoryService repositoryService = pe.getRepositoryService();
		ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(processDefinitionId).singleResult();
		m_category = processDefinition.getCategory();
		m_processDefinitionKey = processDefinition.getKey();
		info("ID:" + processDefinition.getId());
		info("Name:" + processDefinition.getName());
		info("Key:" + processDefinition.getKey());
	}

	public Object lookupServiceByName(String name) {
		Map beans = Context.getProcessEngineConfiguration().getBeans();
		BundleContext bc = (BundleContext) beans.get("bundleContext");
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

	private void info(String msg) {
		System.out.println(msg);
		m_logger.info(msg);
	}

	private static final org.slf4j.Logger m_logger = org.slf4j.LoggerFactory.getLogger(CamelBehaviorDefaultImpl.class);
}
