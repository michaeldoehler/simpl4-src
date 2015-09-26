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
public class Simpl4FilterTaskJsonConverter extends BaseBpmnJsonConverter {

	protected JSONDeserializer m_ds = new JSONDeserializer();


	private final String LISTNAME_PROP = "listname";

	private final String LISTNAME = "listname";

	private final String FILTERNAME_PROP = "filtername";

	private final String FILTERNAME = "filtername";

	private final String FILTERVARNAME_PROP = "filtervarname";

	private final String FILTERVARNAME = "filtervarname";

	private final String VARMAPPING_PROP = "variablesmapping";

	private final String VARMAPPING = "variablesmapping";

	protected String getStencilId(FlowElement flowElement) {
		return "FilterTask";
	}

	protected void convertElementToJson(ObjectNode propertiesNode, FlowElement flowElement) {
		ServiceTask serviceTask = (ServiceTask) flowElement;
	}

	protected FlowElement convertJsonToElement(JsonNode elementNode, JsonNode modelNode, Map<String, JsonNode> shapeMap) {
		ServiceTask task = new ServiceTask();
		Map elementMap = (Map) m_ds.deserialize(elementNode.toString());
		Map<String, Object> propMap = (Map) elementMap.get("properties");

		String clazz = Simpl4BpmnJsonConverter.getFullnameForTask("TaskFilterExecutor");
		task.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_CLASS);
		task.setImplementation(clazz);

		FieldExtension field = new FieldExtension();
		field.setFieldName(VARMAPPING);
		String variablesmapping = getValue(VARMAPPING, propMap.get(VARMAPPING_PROP));
		field.setExpression(variablesmapping);
		task.getFieldExtensions().add(field);

		field = new FieldExtension();
		field.setFieldName(FILTERNAME);
		field.setStringValue(getValue(FILTERNAME, propMap.get(FILTERNAME_PROP)));
		task.getFieldExtensions().add(field);

		field = new FieldExtension();
		field.setFieldName(FILTERVARNAME);
		field.setStringValue(getValue(FILTERVARNAME, propMap.get(FILTERVARNAME_PROP)));
		task.getFieldExtensions().add(field);

		field = new FieldExtension();
		field.setFieldName(LISTNAME);
		field.setStringValue(checkNull(LISTNAME, propMap.get(LISTNAME_PROP)));
		task.getFieldExtensions().add(field);
		return task;
	}

	public static void fillTypes(Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap, Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {
		fillJsonTypes(convertersToBpmnMap);
	}

	public static void fillJsonTypes(Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap) {
		convertersToBpmnMap.put("FilterTask", Simpl4FilterTaskJsonConverter.class);
	}
	private String checkEmpty(String name, Object value) {
		if (value == null)
			throw new RuntimeException("Simpl4FilterTaskJsonConverter:" + name + " is null");
		String val=value.toString();
		if( val.trim().length() ==0){
			throw new RuntimeException("Simpl4FilterTaskJsonConverter:" + name + " is empty");
		}
		return val;
	}

	private String checkNull(String name, Object value) {
		if (value == null)
			throw new RuntimeException("Simpl4FilterTaskJsonConverter:" + name + " is null");
		return value.toString();
	}
	private String getValue(String name, Object value) {
		if (value == null){
			return null;
		}
		return value.toString();
	}
}








































