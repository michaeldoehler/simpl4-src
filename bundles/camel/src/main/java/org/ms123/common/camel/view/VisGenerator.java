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
package org.ms123.common.camel.view;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Set;
import java.net.URL;
import org.apache.camel.model.ChoiceDefinition;
import org.apache.camel.model.FromDefinition;
import org.apache.camel.model.MulticastDefinition;
import org.apache.camel.model.PipelineDefinition;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.ToDefinition;
import org.apache.camel.model.OptionalIdentifiedDefinition;
import org.apache.camel.view.NodeData;
import static org.apache.camel.util.ObjectHelper.isEmpty;
import static org.apache.camel.util.StringHelper.xmlEncode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.Ostermiller.util.CGIParser;

/**
 * @version 
 */
@SuppressWarnings("unchecked")
public class VisGenerator {

	private boolean addUrl = true;

	private static String NODES = "nodes";

	private static String EDGES = "edges";

	private static String ID = "id";

	private static String LABEL = "label";

	private static String TITLE = "title";

	private static String SHAPE = "shape";

	private static String FROM = "from";

	private static String TO = "to";

	private static String LEVEL = "level";

	private static String IMAGE = "image";

	private final Map<Object, NodeData> nodeMap = new HashMap<Object, NodeData>();

	private String imagePrefix = "resource/eip/";

	public VisGenerator() {
	}

	public Map<String, List> getGraph(List<RouteDefinition> routeDefinitions) {
		Map<String, List> data = new HashMap();
		List<Map> nodes = new ArrayList();
		List<Map> edges = new ArrayList();
		data.put(NODES, nodes);
		data.put(EDGES, edges);
		for( RouteDefinition routeDef : routeDefinitions){
			doRoutes(data, routeDef);
		}
		return data;
	}

	private Map<String, Object> createNodeMap(String id, String label, String title, String shape, String image, int level, boolean first) {
		Map<String, Object> node = new HashMap();
		node.put(ID, id);
		node.put(LABEL, label);
		if (title != null) {
			node.put(TITLE, title);
		}
		if (shape != null) {
			node.put(SHAPE, shape);
		}
		if (image != null) {
			node.put(IMAGE, image);
		}
		node.put(LEVEL, level + "");
		return node;
	}

	private Map<String, Object> createEdgeMap(String from, String to) {
		return createEdgeMap(from, to, null);
	}

	private Map<String, Object> createEdgeMap(String from, String to, String label) {
		Map<String, Object> edge = new HashMap();
		edge.put(FROM, from);
		edge.put(TO, to);
		if (label != null) {
			edge.put(LABEL, label);
		}
		return edge;
	}

	private void doRoutes(Map<String, List> data, RouteDefinition route) {
		List<Map> nodes = data.get(NODES);
		List<Map> edges = data.get(EDGES);
		List<FromDefinition> inputs = route.getInputs();
		for (FromDefinition input : inputs) {
			debug("From:" + input);
			NodeData nodeData = getNodeData(input);
			doRoute(data, route, nodeData, 0);
		}
	}

	private void doRoute(Map<String, List> data, final RouteDefinition route, NodeData nodeData, int level) {
		createNode(data, nodeData, level);
		NodeData from = nodeData;
		for (ProcessorDefinition<?> output : route.getOutputs()) {
			level++;
			debug("doNode1:" + output+"/"+level);
			Object[] ret = doNode(data, from, output, level);
			from = (NodeData)ret[0];
			level = (Integer)ret[1];
		}
	}

	private Object[] doNode(Map<String, List> data, NodeData fromData, ProcessorDefinition<?> node, int level) {
		List<Map> edges = data.get(EDGES);
		if (node instanceof MulticastDefinition) {
			// no need for a multicast node
			List<ProcessorDefinition<?>> outputs = node.getOutputs();
			for (ProcessorDefinition<?> output : outputs) {
				debug("doNode:multi:" + output);
				doNode(data, fromData, output, level + 1);
			}
			return new Object[] {fromData,level};
		}
		NodeData toData = getNodeData(node);
		debug("doNode2.to:" + toData.label);
		createNode(data, toData, level);
		if (fromData != null) {
			edges.add(createEdgeMap(xmlEncode(fromData.id), xmlEncode(toData.id)));
		}
		List<ProcessorDefinition<?>> outputs = toData.outputs;
		int retLevel=level;
		if (outputs != null) {
			debug("doNode3(" + toData.label + "),childs:" + outputs.size() + "/mc:" + isMulticastNode(node) + "/level:" + level);
			for (ProcessorDefinition<?> output : outputs) {
				int l = 0;
				if (!isMulticastNode(node)) {
					level++;
					l = level;
				} else {
					l = level + 1;
				}
				debug("\tdoNode4:" + output + "/level:" + level);
				Object[] ret = doNode(data, toData, output, l);
				if (!isMulticastNode(node)) {
					toData = (NodeData)ret[0];
					retLevel = (Integer)ret[1];
					debug("retLevel:"+retLevel);
				}
			}
		}
		return new Object[]{toData,retLevel};
	}

	private void createNode(Map<String, List> _data, NodeData data, int level) {
		List<Map> nodes = _data.get(NODES);
		if (!data.nodeWritten) {
			data.nodeWritten = true;
			String nodeType = data.image;
			if (isEmpty(nodeType)) {
				nodeType = data.shape;
				if (isEmpty(nodeType)) {
					nodeType = "node";
				}
			}
			debug("\tcreateNode:" + data.label + "/" + level);
			debug("");
			String shape = "ellipse";
			if ("box".equals(nodeType)) {
				shape = nodeType;
			}
			if (data.image != null) {
				shape = "image";
			}
			int ind;
			String tooltip=null;
			if( data.tooltop!= null && (ind=data.tooltop.indexOf("?")) != -1){
				CGIParser cp = null;
				try{
					cp = new CGIParser( data.tooltop.substring(ind+1),"UTF-8");
				}catch(Exception e){
					e.printStackTrace();
				}
				tooltip = "<table class=\"visTooltip\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">";
				tooltip+="<tr><th>Name</th><th>Value</th></tr>";
				for( String name : cp.getParameterNameList()){
					String value=cp.getParameter(name);
					tooltip+="<tr><td>"+name+"</td><td>"+value+"</td></tr>";
				}
				tooltip+="</table>";
			}else{
				tooltip = data.tooltop;
			}
			nodes.add(createNodeMap(xmlEncode(data.id), data.label, tooltip, shape, data.image, level, nodes.size() == 0));
		}
	}

	private NodeData getNodeData(Object node) {
		Object key = node;
		if (node instanceof FromDefinition) {
			FromDefinition fromType = (FromDefinition) node;
			key = fromType.getUriOrRef();
		} else if (node instanceof ToDefinition) {
			ToDefinition toType = (ToDefinition) node;
			key = toType.getUriOrRef();
		}
	
		String resId=((OptionalIdentifiedDefinition)node).getId();
		NodeData answer = null;
		if (key != null) {
			answer = nodeMap.get(key);
		}
		if (answer == null) {
			//String id = "node" + (nodeMap.size() + 1);
			answer = new NodeData(resId, node, imagePrefix);
			nodeMap.put(key, answer);
		}
		return answer;
	}

	protected boolean isMulticastNode(ProcessorDefinition<?> node) {
		return node instanceof MulticastDefinition || node instanceof ChoiceDefinition;
	}

	private static void debug(String msg) {
		//System.err.println(msg);
		m_logger.debug(msg);
	}

	private static void info(String msg) {
		System.err.println(msg);
		m_logger.info(msg);
	}

	private static final Logger m_logger = LoggerFactory.getLogger(VisGenerator.class);
}
