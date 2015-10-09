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
import org.apache.camel.*;
import org.activiti.camel.ActivitiConsumer;
import org.apache.camel.impl.DefaultEndpoint;
import org.ms123.common.permission.api.PermissionService;
import org.ms123.common.workflow.api.WorkflowService;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import flexjson.*;

/**
 */
public class ActivitiEndpoint extends org.activiti.camel.ActivitiEndpoint {

	private JSONDeserializer ds = new JSONDeserializer();
	private RuntimeService m_runtimeService;
	private Map m_options;
	private Map<String, String> processCriteria = new HashMap<String, String>();
	private String namespace;

	private PermissionService m_permissionService;
	private WorkflowService m_workflowService;

	public ActivitiEndpoint(String uri, CamelContext camelContext, WorkflowService ws, PermissionService ps) {
		super(uri, camelContext);
		m_runtimeService = ws.getProcessEngine().getRuntimeService();
		setRuntimeService(m_runtimeService);
		m_permissionService = ps;
		m_workflowService = ws;
	}

	public Producer createProducer() throws Exception {
		info("ActivitiEndpoint.createProducer");
		return new org.ms123.common.camel.components.activiti.ActivitiProducer(this, m_workflowService, m_permissionService);
	}

	public void configureProperties(Map<String, Object> options) {
		info("ActivitiEndpoint:" + options);
		m_options = options;
	}

	public Map getOptions() {
		return m_options;
	}

	public void setNamespace(String data) {
		this.namespace = data;
	}

	public String getNamespace() {
		if ("-".equals(this.namespace)) {
			return null;
		}
		return this.namespace;
	}

	public void setProcessCriteria(String data) {
		Map<String, String> ret = new HashMap<String, String>();
		if (data != null) {
			List<Map<String, String>> l = (List) ds.deserialize(data);
			for (Map<String, String> m : l) {
				String name = m.get("name");
				String value = m.get("value");
				ret.put(name, value);
			}
		}
		this.processCriteria = ret;
	}

	public Map<String, String> getProcessCriteria() {
		return this.processCriteria;
	}

	private static final org.slf4j.Logger m_logger = org.slf4j.LoggerFactory.getLogger(ActivitiEndpoint.class);

	private void debug(String msg) {
		System.out.println(msg);
		m_logger.debug(msg);
	}

	private void info(String msg) {
		System.out.println(msg);
		m_logger.info(msg);
	}
}

