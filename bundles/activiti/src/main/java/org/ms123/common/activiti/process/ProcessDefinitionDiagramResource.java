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

public class ProcessDefinitionDiagramResource extends BaseResource {

	String m_processDefinitionId;

	public ProcessDefinitionDiagramResource(ActivitiService as, String processDefinitionId) {
		super(as, null);
		m_processDefinitionId = processDefinitionId;
	}

	public String getDiagram() {
		if (m_processDefinitionId == null) {
			throw new RuntimeException("No process definition id provided");
		}
		RepositoryService repositoryService = getPE().getRepositoryService();
		ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(m_processDefinitionId).singleResult();
		if (processDefinition == null) {
			throw new RuntimeException("Process definition " + m_processDefinitionId + " could not be found");
		}
		if (processDefinition.getDiagramResourceName() == null) {
			throw new RuntimeException("Diagram resource could not be found");
		}
		final InputStream definitionImageStream = repositoryService.getResourceAsStream(processDefinition.getDeploymentId(), processDefinition.getDiagramResourceName());
		if (definitionImageStream == null) {
			throw new RuntimeException("Diagram resource could not be found");
		}
		return "data:image/png;base64," + Base64.encode(definitionImageStream);
	}
}
