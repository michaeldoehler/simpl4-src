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
import org.activiti.bpmn.model.BaseElement;
import org.activiti.bpmn.model.FieldExtension;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.ImplementationType;
import org.activiti.bpmn.model.ServiceTask;
import org.apache.commons.lang.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.activiti.editor.language.json.converter.*;
import flexjson.*;

/**
 */
@SuppressWarnings("unchecked")
public class Simpl4ScriptTaskJsonConverter extends BaseBpmnJsonConverter {

	protected JSONDeserializer m_ds = new JSONDeserializer();

	private final String SCRIPT_PROP = "script";

	private final String SCRIPT = "script";

	protected String getStencilId(BaseElement flowElement) {
		return "ScriptTask";
	}

	protected void convertElementToJson(ObjectNode propertiesNode, BaseElement flowElement) {
		ServiceTask serviceTask = (ServiceTask) flowElement;
	}

	protected FlowElement convertJsonToElement(JsonNode elementNode, JsonNode modelNode, Map<String, JsonNode> shapeMap) {
		ServiceTask task = new ServiceTask();
		Map elementMap = (Map) m_ds.deserialize(elementNode.toString());
		Map<String, Object> propMap = (Map) elementMap.get("properties");
		String clazz = Simpl4BpmnJsonConverter.getFullnameForTask("TaskScriptExecutor");
		System.out.println("ScriptTask.class:" + clazz);
		task.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_CLASS);
		task.setImplementation(clazz);
		FieldExtension field = new FieldExtension();
		field.setFieldName(SCRIPT);
		field.setStringValue(checkNull2(SCRIPT, propMap.get(SCRIPT_PROP)));
		task.getFieldExtensions().add(field);
		return task;
	}

	public static void fillTypes(Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap, Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {
		fillJsonTypes(convertersToBpmnMap);
	}

	public static void fillJsonTypes(Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap) {
		convertersToBpmnMap.put("ScriptTask", Simpl4ScriptTaskJsonConverter.class);
	}

	private String checkNull(String name, Object value) {
		if (value == null)
			throw new RuntimeException("Simpl4ScriptTaskJsonConverter:" + name + " is null");
		return value.toString();
	}
	private String checkNull2(String name, Object value) {
		if (value == null){
			return "";
		}
		return value.toString();
	}
}
