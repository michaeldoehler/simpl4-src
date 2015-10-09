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

import flexjson.*;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.ms123.common.data.api.DataLayer;
import org.ms123.common.form.FormService;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.nucleus.api.NucleusService;
import org.ms123.common.workflow.api.WorkflowService;
import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;
import org.activiti.engine.runtime.*;
import org.activiti.engine.history.*;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.ProcessDefinition;
import org.osgi.service.event.EventAdmin;

/**
 *
 */
class BaseActivitiServiceImpl implements Constants {

	protected JSONDeserializer m_ds = new JSONDeserializer();
	protected JSONSerializer m_js = new JSONSerializer();

	protected WorkflowService m_workflowService;

	protected DataLayer m_dataLayer;
	protected EventAdmin m_eventAdmin;

	protected NucleusService m_nucleusService;

	protected FormService m_formService;

	protected ProcessEngine m_processEngine;

	protected String _getTenantId(String processInstanceId) {
		ProcessInstance processInstance = m_processEngine.getRuntimeService().createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
		String processDefinitionId = null;
		String tenantId = null;
		if (processInstance == null) {
			HistoricProcessInstance instance = m_processEngine.getHistoryService().createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
			if (instance == null) {
				throw new RuntimeException("BaseActivitiServiceImpl._getTenantId:processInstance not found:" + processInstanceId);
			}
			processDefinitionId = instance.getProcessDefinitionId();
			tenantId = instance.getTenantId();
		} else {
			processDefinitionId = processInstance.getProcessDefinitionId();
			tenantId = processInstance.getTenantId();
		}
		RepositoryService repositoryService = m_processEngine.getRepositoryService();
		ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(processDefinitionId).processDefinitionTenantId(tenantId).singleResult();
		return processDefinition.getTenantId();
	}

	protected ClassLoader _setContextClassLoader(String processInstanceId) {
		String tenantId = _getTenantId(processInstanceId);
		ClassLoader saveCl = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(m_nucleusService.getClassLoader(StoreDesc.getNamespaceData(tenantId)));
		return saveCl;
	}
}
