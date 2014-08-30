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
import java.util.List;
import java.util.Date;
import org.activiti.bpmn.model.BaseElement;
import org.activiti.bpmn.model.FieldExtension;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.ImplementationType;
import org.activiti.bpmn.model.ServiceTask;
import org.apache.commons.lang.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.activiti.editor.language.json.converter.*;
import org.ms123.common.stencil.api.StencilService;

/**
 */
@SuppressWarnings("unchecked")
public class SWBpmnJsonConverter extends BpmnJsonConverter {

	public SWBpmnJsonConverter(String namespace, StencilService stencilService) {
		try{
			List<Map> additionalStencils = stencilService.getAddonStencils( namespace);
			for( Map<String,Object> stencil : additionalStencils){
				String id = (String)stencil.get("id");
				Class c = stencilService.getConverterClass(namespace,id);
				System.out.println("SWBpmnJsonConverter.classd("+id+"):"+c);
				convertersToBpmnMap.put(id, c);
				DI_RECTANGLES.add(id);
			}
		}catch(Exception e){
			throw new RuntimeException("SWBpmnJsonConverter:",e);
		}
		System.out.println("SWBpmnJsonConverter:"+convertersToBpmnMap);
		SWSequenceFlowJsonConverter.fillTypes(convertersToBpmnMap, convertersToJsonMap);
		SWFilterTaskJsonConverter.fillTypes(convertersToBpmnMap, convertersToJsonMap);
		SWDocumentTaskJsonConverter.fillTypes(convertersToBpmnMap, convertersToJsonMap);
		SWUserTaskJsonConverter.fillTypes(convertersToBpmnMap, convertersToJsonMap);
		SWScriptTaskJsonConverter.fillTypes(convertersToBpmnMap, convertersToJsonMap);
		SWRulesTaskJsonConverter.fillTypes(convertersToBpmnMap, convertersToJsonMap);
		SWSendTaskJsonConverter.fillTypes(convertersToBpmnMap, convertersToJsonMap);
		SWStartEventJsonConverter.fillTypes(convertersToBpmnMap, convertersToJsonMap);
		DI_RECTANGLES.add("SendTask");
		DI_RECTANGLES.add("FilterTask");
		DI_RECTANGLES.add("RulesTask");
		DI_RECTANGLES.add("DocumentTask");
	}

}
