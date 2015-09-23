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
import org.activiti.bpmn.model.StartEvent;
import org.apache.commons.lang.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.activiti.editor.language.json.converter.*;
import flexjson.*;

/**
 */
@SuppressWarnings("unchecked")
public class Simpl4StartEventJsonConverter extends StartEventJsonConverter {

	protected JSONDeserializer m_ds = new JSONDeserializer();

	protected JSONSerializer m_js = new JSONSerializer();

	private final String FORMKEY_PROP = "attr$activiti$formkey";

	public static void fillTypes(Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap, Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {
		fillJsonTypes(convertersToBpmnMap);
	}

	public static void fillJsonTypes(Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap) {
		convertersToBpmnMap.put(STENCIL_EVENT_START_NONE, Simpl4StartEventJsonConverter.class);
		convertersToBpmnMap.put(STENCIL_EVENT_START_TIMER, Simpl4StartEventJsonConverter.class);
		convertersToBpmnMap.put(STENCIL_EVENT_START_ERROR, Simpl4StartEventJsonConverter.class);
		convertersToBpmnMap.put(STENCIL_EVENT_START_MESSAGE, Simpl4StartEventJsonConverter.class);
		convertersToBpmnMap.put(STENCIL_EVENT_START_SIGNAL, Simpl4StartEventJsonConverter.class);
	}

	protected FlowElement convertJsonToElement(JsonNode elementNode, JsonNode modelNode, Map<String, JsonNode> shapeMap) {
		StartEvent event = (StartEvent) super.convertJsonToElement(elementNode, modelNode, shapeMap);
		m_js.prettyPrint(true);
		Map elementMap = (Map) m_ds.deserialize(elementNode.toString());
		Map<String, Object> propMap = (Map) elementMap.get("properties");
		System.out.println("StartEvent.propMap:" + m_js.deepSerialize(propMap));
		String formkey = getString(propMap.get(FORMKEY_PROP));
		System.out.println("StartEvent.formkey:" + formkey);
		event.setFormKey(formkey);
		System.out.println("StartEvent.event:" + m_js.deepSerialize(event));
		return event;
	}

	private String getString(Object value) {
		if (value == null)
			return null;
		return value.toString();
	}
}
