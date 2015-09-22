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
import org.activiti.engine.RuntimeService;
import org.activiti.engine.ProcessEngine;
import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.impl.DefaultComponent;
import org.ms123.common.permission.api.PermissionService;
import org.ms123.common.workflow.api.WorkflowService;

/**
 */
public class ActivitiComponent extends org.activiti.camel.ActivitiComponent {

	private WorkflowService m_workflowService;

	private PermissionService m_permissionService;

	private boolean copyVariablesToProperties;

	private boolean copyVariablesToBodyAsMap;

	private boolean copyCamelBodyToBody;

	public ActivitiComponent() {
	}

	@Override
	public void setCamelContext(CamelContext context) {
		super.setCamelContext(context);
		getServices(context);
	}

	private void getServices(CamelContext context){
		if( m_workflowService == null){
			m_permissionService = getByType(context, PermissionService.class);
			m_workflowService = getByType(context, WorkflowService.class);
			info("PermissionService:"+m_permissionService);
			info("WorkflowService:"+m_workflowService);
		}
	}

	private <T> T getByType(CamelContext ctx, Class<T> kls) {
		return kls.cast(ctx.getRegistry().lookupByName(kls.getName()));
	}

	@Override
	protected Endpoint createEndpoint(String s, String s1, Map<String, Object> stringObjectMap) throws Exception {
		CamelContext cc = getCamelContext();
		getServices(cc);
		ActivitiEndpoint ae = new ActivitiEndpoint(s, cc, m_workflowService, m_permissionService);
		info("createEndpoint:" + s );
		ae.setCopyVariablesToProperties(this.copyVariablesToProperties);
		ae.setCopyVariablesToBodyAsMap(this.copyVariablesToBodyAsMap);
		ae.setCopyCamelBodyToBody(this.copyCamelBodyToBody);
		return ae;
	}

	public boolean isCopyVariablesToProperties() {
		return copyVariablesToProperties;
	}

	public void setCopyVariablesToProperties(boolean copyVariablesToProperties) {
		this.copyVariablesToProperties = copyVariablesToProperties;
	}

	public boolean isCopyCamelBodyToBody() {
		return copyCamelBodyToBody;
	}

	public void setCopyCamelBodyToBody(boolean copyCamelBodyToBody) {
		this.copyCamelBodyToBody = copyCamelBodyToBody;
	}

	public boolean isCopyVariablesToBodyAsMap() {
		return copyVariablesToBodyAsMap;
	}

	public void setCopyVariablesToBodyAsMap(boolean copyVariablesToBodyAsMap) {
		this.copyVariablesToBodyAsMap = copyVariablesToBodyAsMap;
	}

	private void info(String msg) {
		System.out.println(msg);
		m_logger.info(msg);
	}
	private static final org.slf4j.Logger m_logger = org.slf4j.LoggerFactory.getLogger(ActivitiComponent.class);
}
