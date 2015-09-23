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
public class Simpl4RulesTaskJsonConverter extends BaseBpmnJsonConverter {

	protected JSONDeserializer m_ds = new JSONDeserializer();

	protected JSONSerializer m_js = new JSONSerializer();

	private final String RULESNAME_PROP = "el$activiti$rulesname";

	private final String RULESNAME = "rulesname";

	private final String CLASSNAME_PROP = "attr$activiti$class";

	private final String CLASSNAME = "class";

	private final String VARMAPPING_PROP = "el$activiti$variablesmapping";

	private final String VARMAPPING = "variablesmapping";

	protected String getStencilId(FlowElement flowElement) {
		return "RulesTask";
	}

	protected void convertElementToJson(ObjectNode propertiesNode, FlowElement flowElement) {
		ServiceTask serviceTask = (ServiceTask) flowElement;
	}

	protected FlowElement convertJsonToElement(JsonNode elementNode, JsonNode modelNode, Map<String, JsonNode> shapeMap) {
		m_js.prettyPrint(true);
		ServiceTask task = new ServiceTask();
		Map elementMap = (Map) m_ds.deserialize(elementNode.toString());
		Map<String, Object> propMap = (Map) elementMap.get("properties");
		String clazz = checkNull(CLASSNAME, propMap.get(CLASSNAME_PROP));
		System.out.println("RulesTask.class:" + clazz);
		task.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_CLASS);
		task.setImplementation(clazz);

		String variablesmapping = checkNull(VARMAPPING, propMap.get(VARMAPPING_PROP));
		FieldExtension field = new FieldExtension();
		field.setFieldName(VARMAPPING);
		field.setExpression(variablesmapping);
		task.getFieldExtensions().add(field);

		field = new FieldExtension();
		field.setFieldName(RULESNAME);
		field.setStringValue(checkNull(RULESNAME, propMap.get(RULESNAME_PROP)));
		task.getFieldExtensions().add(field);
		return task;
	}

	public static void fillTypes(Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap, Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {
		fillJsonTypes(convertersToBpmnMap);
	}

	public static void fillJsonTypes(Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap) {
		convertersToBpmnMap.put("RulesTask", Simpl4RulesTaskJsonConverter.class);
	}

	private String checkNull(String name, Object value) {
		if (value == null)
			throw new RuntimeException("Simpl4RulesTaskJsonConverter:" + name + " is null");
		return value.toString();
	}
}
