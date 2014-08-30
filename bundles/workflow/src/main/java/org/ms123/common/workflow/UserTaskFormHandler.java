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

import java.util.List;
import java.util.ArrayList;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.activiti.engine.impl.form.DefaultFormHandler;
import org.activiti.engine.impl.form.TaskFormHandler;
import org.activiti.engine.impl.form.TaskFormDataImpl;
import org.activiti.engine.impl.persistence.entity.DeploymentEntity;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;

/**
 */
public class UserTaskFormHandler extends DefaultFormHandler implements TaskFormHandler {

	public TaskFormData createTaskForm(TaskEntity task) {
		System.out.println("UserTaskFormHandler.createTaskForm");
		TaskFormDataImpl taskFormData = new TaskFormDataImpl();
		//taskFormData.setFormKey(formKey);
		taskFormData.setDeploymentId(deploymentId);
		taskFormData.setTask(task);
		initializeFormProperties(taskFormData, task.getExecution());
		return taskFormData;
	}

	public void parseConfiguration(List<org.activiti.bpmn.model.FormProperty> formProperties, String formKey, DeploymentEntity deployment, ProcessDefinitionEntity processDefinition) {
		System.out.println("UserTaskFormHandler.parseConfiguration");
		super.parseConfiguration(formProperties, formKey, deployment, processDefinition);
	}
}
