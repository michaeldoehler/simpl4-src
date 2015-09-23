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
public class Simpl4SendTaskJsonConverter extends BaseBpmnJsonConverter {

	private final String PROPERTY_MAILTASK_TO = "el$activiti$to";

	private final String PROPERTY_MAILTASK_FROM = "el$activiti$from";

	private final String PROPERTY_MAILTASK_SUBJECT = "el$activiti$subject";
	private final String PROPERTY_MAILTASK_ATTACHMENT = "el$activiti$attachment";

	private final String PROPERTY_MAILTASK_CC = "el$activiti$cc";

	private final String PROPERTY_MAILTASK_BCC = "el$activiti$bcc";

	private final String PROPERTY_MAILTASK_TEXT = "el$activiti$text";

	private final String PROPERTY_MAILTASK_HTML = "el$activiti$html";

	public static void fillTypes(Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap, Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {
		fillJsonTypes(convertersToBpmnMap);
		fillBpmnTypes(convertersToJsonMap);
	}

	public static void fillJsonTypes(Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap) {
		convertersToBpmnMap.put("SendTask", Simpl4SendTaskJsonConverter.class);
	}

	public static void fillBpmnTypes(Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {
	}

	protected String getStencilId(FlowElement flowElement) {
		return "SendTask";
	}

	protected void convertElementToJson(ObjectNode propertiesNode, FlowElement flowElement) {
	}

	protected FlowElement convertJsonToElement(JsonNode elementNode, JsonNode modelNode, Map<String, JsonNode> shapeMap) {
		ServiceTask task = new ServiceTask();
		//task.setType(ServiceTask.MAIL_TASK);

		String clazz = "org.ms123.common.workflow.TaskSendExecutor";
		task.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_CLASS);
		task.setImplementation(clazz);

		addField(PROPERTY_MAILTASK_TO, elementNode, task);
		addField(PROPERTY_MAILTASK_FROM, elementNode, task);
		addField(PROPERTY_MAILTASK_SUBJECT, elementNode, task);
		addField(PROPERTY_MAILTASK_ATTACHMENT, elementNode, task);
		addField(PROPERTY_MAILTASK_CC, elementNode, task);
		addField(PROPERTY_MAILTASK_BCC, elementNode, task);
		addField(PROPERTY_MAILTASK_TEXT, elementNode, task);
		addField(PROPERTY_MAILTASK_HTML, elementNode, task);
		return task;
	}

	protected void addField(String name, JsonNode elementNode, ServiceTask task) {
		FieldExtension field = new FieldExtension();
		field.setFieldName(name.substring(12));
		String value = getPropertyValueAsString(name, elementNode);
		if (StringUtils.isNotEmpty(value)) {
			if ((value.contains("${") || value.contains("#{")) && value.contains("}")) {
				field.setExpression(value);
			} else {
				field.setStringValue(value);
			}
		}
		task.getFieldExtensions().add(field);
	}
}
