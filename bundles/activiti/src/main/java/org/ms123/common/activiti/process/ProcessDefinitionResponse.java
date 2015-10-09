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

import java.io.Serializable;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;

/**
 */
public class ProcessDefinitionResponse implements Serializable {

	private static final long serialVersionUID = 1L;

	String id;

	String key;

	String name;

	int version;

	String deploymentId;

	String resourceName;

	String diagramResourceName;

	String startFormResourceKey;

	boolean isGraphicNotationDefined;

	String tenantId;

	public ProcessDefinitionResponse(ProcessDefinitionEntity processDefinition) {
		this.setId(processDefinition.getId());
		this.setKey(processDefinition.getKey());
		this.setName(processDefinition.getName());
		this.setVersion(processDefinition.getVersion());
		this.setDeploymentId(processDefinition.getDeploymentId());
		this.setResourceName(processDefinition.getResourceName());
		this.setDiagramResourceName(processDefinition.getDiagramResourceName());
		this.setTenantId(processDefinition.getTenantId());
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public String getDeploymentId() {
		return deploymentId;
	}

	public void setDeploymentId(String deploymentId) {
		this.deploymentId = deploymentId;
	}

	public String getResourceName() {
		return resourceName;
	}

	public void setResourceName(String resourceName) {
		this.resourceName = resourceName;
	}

	public String getDiagramResourceName() {
		return diagramResourceName;
	}

	public void setDiagramResourceName(String diagramResourceName) {
		this.diagramResourceName = diagramResourceName;
	}

	public boolean isGraphicNotationDefined() {
		return isGraphicNotationDefined;
	}

	public void setGraphicNotationDefined(boolean graphicNotationDefined) {
		isGraphicNotationDefined = graphicNotationDefined;
	}

	public String getStartFormResourceKey() {
		return startFormResourceKey;
	}

	public void setStartFormResourceKey(String startFormResourceKey) {
		this.startFormResourceKey = startFormResourceKey;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}
}
