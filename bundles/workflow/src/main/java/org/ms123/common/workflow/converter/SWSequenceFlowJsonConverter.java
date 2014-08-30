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
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.activiti.editor.language.json.converter.*;
import org.activiti.bpmn.model.SequenceFlow;
import flexjson.*;

/**
 */
public class SWSequenceFlowJsonConverter extends SequenceFlowJsonConverter {

	private String PROPERTY_SEQUENCEFLOW_CONDITIONALFLOW = "conditionalflow";

	protected String getStencilId(FlowElement flowElement) {
    return STENCIL_SEQUENCE_FLOW;
	}
  public static void fillTypes(Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap, Map<Class<? extends BaseElement>, Class<? extends BaseBpmnJsonConverter>> convertersToJsonMap) {
    fillJsonTypes(convertersToBpmnMap);
  }
  public static void fillJsonTypes(Map<String, Class<? extends BaseBpmnJsonConverter>> convertersToBpmnMap) {
    convertersToBpmnMap.put(STENCIL_SEQUENCE_FLOW, SWSequenceFlowJsonConverter.class);
  }

  @Override
  protected FlowElement convertJsonToElement(JsonNode elementNode, JsonNode modelNode, Map<String, JsonNode> shapeMap) {
    SequenceFlow flow = new SequenceFlow();
    
    String sourceRef = lookForSourceRef(elementNode.get(EDITOR_SHAPE_ID).asText(), modelNode.get(EDITOR_CHILD_SHAPES));
    
    if (sourceRef != null) {
      flow.setSourceRef(sourceRef);
      String targetId = elementNode.get("target").get(EDITOR_SHAPE_ID).asText();
      flow.setTargetRef(BpmnJsonConverterUtil.getElementId(shapeMap.get(targetId)));
    }
    String conditionalFlow = getPropertyValueAsString(PROPERTY_SEQUENCEFLOW_CONDITIONALFLOW, elementNode); 
		System.out.println("SWSequenceFlowJsonConverter:"+conditionalFlow);
		if( conditionalFlow==null || !conditionalFlow.toLowerCase().equals("none")){
    	flow.setConditionExpression(getPropertyValueAsString(PROPERTY_SEQUENCEFLOW_CONDITION, elementNode));
		}
    
    return flow;
  }
  private String lookForSourceRef(String flowId, JsonNode childShapesNode) {
    String sourceRef = null;
    
    if (childShapesNode != null) {
    
      for (JsonNode childNode : childShapesNode) {
        ArrayNode outgoingNode = (ArrayNode) childNode.get("outgoing");
        if (outgoingNode != null && outgoingNode.size() > 0) {
          for (JsonNode outgoingChildNode : outgoingNode) {
            JsonNode resourceNode = outgoingChildNode.get(EDITOR_SHAPE_ID);
            if (resourceNode != null && flowId.equals(resourceNode.asText())) {
              sourceRef = BpmnJsonConverterUtil.getElementId(childNode);
              break;
            }
          }
          
          if (sourceRef != null) {
            break;
          }
        }
        sourceRef = lookForSourceRef(flowId, childNode.get(EDITOR_CHILD_SHAPES));
        
        if (sourceRef != null) {
          break;
        }
      }
    }
    return sourceRef;
  }
}
