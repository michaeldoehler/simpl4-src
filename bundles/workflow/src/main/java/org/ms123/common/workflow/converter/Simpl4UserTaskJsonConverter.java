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
package org.ms123.common.workflow.converter;

import java.util.Map;
import java.util.Arrays;
import org.activiti.bpmn.model.BaseElement;
import org.activiti.bpmn.model.FieldExtension;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.ImplementationType;
import org.activiti.bpmn.model.ServiceTask;
import org.activiti.bpmn.model.UserTask;
import org.activiti.bpmn.model.FormProperty;
import org.apache.commons.lang.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.activiti.editor.language.json.converter.*;
import flexjson.*;

/**
 */
@SuppressWarnings("unchecked")
public class Simpl4UserTaskJsonConverter extends UserTaskJsonConverter {

	protected JSONDeserializer m_ds = new JSONDeserializer();

	protected JSONSerializer m_jsPretty = new JSONSerializer();

	protected JSONSerializer m_js = new JSONSerializer();

	private final String FORMKEY_PROP = "formkey";
	private final String FORMKEY = "formkey";

	private final String CANDIDATEGROUPS_PROP = "candidategroups";
	private final String CANDIDATEGROUPS = "candidategroups";

	private final String ASSIGNEE_PROP = "assignee";
	private final String ASSIGNEE = "assignee";

	private final String VARMAPPING_PROP = "variablesmapping";
	private final String VARMAPPING = "variablesmapping";

	private final String FORMARNAME_PROP = "formvarname";
	private final String FORMARNAME = "formvarname";

	protected String getStencilId(FlowElement flowElement) {
		return "UserTask";
	}

	protected FlowElement convertJsonToElement(JsonNode elementNode, JsonNode modelNode, Map<String, JsonNode> shapeMap) {
		UserTask task = (UserTask) super.convertJsonToElement(elementNode, modelNode, shapeMap);
		m_jsPretty.prettyPrint(true);
		Map elementMap = (Map) m_ds.deserialize(elementNode.toString());
		Map<String, Object> propMap = (Map) elementMap.get("properties");
		System.out.println("\n--->>> UserTask.propMap:" + m_jsPretty.deepSerialize(propMap));
		String formkey = getString(propMap.get(FORMKEY_PROP));
		task.setFormKey(formkey);
		String assignee = getString(propMap.get(ASSIGNEE_PROP));
		task.setAssignee(assignee);
		String candidategroups = getString(propMap.get(CANDIDATEGROUPS_PROP));
		if (candidategroups != null) {
			task.setCandidateGroups(Arrays.asList(candidategroups.split(",")));
		}
		String variablesmapping = getVarMapping(propMap.get(VARMAPPING_PROP));
		System.out.println("UserTask.variablesmapping:" + variablesmapping);
		if (variablesmapping != null) {
			FormProperty formProperty = new FormProperty();
			formProperty.setId(VARMAPPING);
			formProperty.setName(VARMAPPING);
			formProperty.setDefaultExpression("~" + variablesmapping);
			formProperty.setVariable("~" + variablesmapping);
			task.getFormProperties().add(formProperty);
		}
		String formVarname = getVarMapping(propMap.get(FORMARNAME_PROP));
		System.out.println("UserTask.formVarname:" + formVarname);
		if (formVarname != null) {
			FormProperty formProperty = new FormProperty();
			formProperty.setId(FORMARNAME);
			formProperty.setName(FORMARNAME);
			formProperty.setDefaultExpression("~" + formVarname);
			formProperty.setVariable("~" + formVarname);
			task.getFormProperties().add(formProperty);
		}
		System.out.println("\n<<<---UserTask.task:" + m_jsPretty.deepSerialize(task));
		return task;
	}

	public static void fillTypes(Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap, Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {
		fillJsonTypes(convertersToBpmnMap);
	}

	public static void fillJsonTypes(Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap) {
		convertersToBpmnMap.put("UserTask", Simpl4UserTaskJsonConverter.class);
	}

	private String checkNull(String name, Object value) {
		if (value == null)
			throw new RuntimeException("Simpl4UserTaskJsonConverter:" + name + " is null");
		return value.toString();
	}

	private String getVarMapping(Object value) {
		if (value == null || value.toString().trim().length() == 0) {
			return null;
		}
		return m_js.deepSerialize(value);
	}

	private String getString(Object value) {
		if (value == null)
			return null;
		return value.toString();
	}
}
