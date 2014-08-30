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

import java.io.InputStream;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.ProcessDefinition;
import org.ms123.common.activiti.ActivitiService;
import org.ms123.common.activiti.BaseResource;
import org.ms123.common.activiti.Util;
import org.ms123.common.libhelper.Base64;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.image.impl.DefaultProcessDiagramGenerator;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.impl.RepositoryServiceImpl;
import org.activiti.bpmn.model.BpmnModel;

public class ProcessInstanceDiagramResource extends BaseResource {

	String m_processInstanceId;

	public ProcessInstanceDiagramResource(ActivitiService as, String processInstanceId) {
		super(as, null);
		m_processInstanceId = processInstanceId;
	}

	public String getDiagram() {
		if (m_processInstanceId == null) {
			throw new RuntimeException("No process Instance id provided");
		}
		ExecutionEntity pi = (ExecutionEntity) getPE().getRuntimeService().createProcessInstanceQuery().processInstanceId(m_processInstanceId).singleResult();
		if (pi == null) {
			return "data:image/png;base64," + notfound;
		}
		ProcessDefinitionEntity pde = (ProcessDefinitionEntity) ((RepositoryServiceImpl) getPE().getRepositoryService()).getDeployedProcessDefinition(pi.getProcessDefinitionId());
		InputStream resource = null;
		if (pde != null && pde.isGraphicalNotationDefined()) {
			BpmnModel bpmnModel = getPE().getRepositoryService().getBpmnModel(pde.getId());
			DefaultProcessDiagramGenerator dp = new DefaultProcessDiagramGenerator();
			resource = dp.generateDiagram(bpmnModel, "png", getPE().getRuntimeService().getActiveActivityIds(m_processInstanceId));
		} else {
			throw new RuntimeException("Process instance with id " + m_processInstanceId + " has no graphic description");
		}
		if (resource == null) {
			throw new RuntimeException("Diagram resource could not be found");
		}
		return "data:image/png;base64," + Base64.encode(resource);
	}

	String notfound = "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";
}
