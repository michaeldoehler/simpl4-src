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
package org.ms123.common.activiti.task;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.form.FormProperty;
import org.ms123.common.utils.Utils;
import groovy.lang.*;
import org.codehaus.groovy.control.*;
import flexjson.*;
import org.apache.commons.beanutils.PropertyUtils;
import org.ms123.common.activiti.ActivitiService;
import org.ms123.common.activiti.BaseResource;
import org.ms123.common.activiti.Util;

/**
 */
@SuppressWarnings("unchecked")
public class TaskFormPropertiesResource extends BaseResource {

	JSONDeserializer ds = new JSONDeserializer();
	JSONSerializer js = new JSONSerializer();

	private String m_executionId;

	private String m_taskId;

	/**
	 */
	public TaskFormPropertiesResource(ActivitiService as, String executionId, String taskId) {
		super(as, null);
		m_executionId = executionId;
		m_taskId = taskId;
	}

	public Map getTaskFormProperties() {
		Map<String, Object> taskFormMap = new HashMap();
		Map pv = null;
		if (m_executionId != null) {
			pv = getPE().getRuntimeService().getVariables(m_executionId);
			js.prettyPrint(true);
			System.out.println("getTaskFormProperties.pv:"+js.deepSerialize(pv));
		}
		TaskFormData formData = getPE().getFormService().getTaskFormData(m_taskId);
		Map values = new HashMap();
		if (formData != null) {
			List<FormProperty> formProperties = formData.getFormProperties();
			for (FormProperty fp : formProperties) {
				System.out.println("fp::" + fp.getName() + "/" + fp.getValue());
				String value = fp.getValue();
				if( value.length() > 0 && value.startsWith("~")){
					value = value.substring(1);
				}
				if (pv != null && "variablesmapping".equals(fp.getName())) {
					Map v = (Map) ds.deserialize(value);
					List<Map> items = (List) v.get("items");
					for (Map item : items) {
						String processvar = (String) item.get("processvar");
						String formvar = (String) item.get("formvar");
						Object val = getProcessVariableValue(pv, processvar);
						values.put(formvar, val);
					}
				} else {
					taskFormMap.put(fp.getName(), value);
				}
				taskFormMap.put("values", values);
			}
		}
		System.out.println("<<<---TaskFormProperties:"+taskFormMap);
		return taskFormMap;
	}

	private Object eval(String scriptStr, Map vars) {
		System.out.println("eval:" + scriptStr);
		CompilerConfiguration config = new CompilerConfiguration();
		config.setScriptBaseClass(org.ms123.common.workflow.api.GroovyTaskDslBase.class.getName());
		Binding binding = new MyBinding(vars);
		GroovyShell shell = new GroovyShell(this.getClass().getClassLoader(), binding, config);
		return shell.evaluate(scriptStr);
	}

	private Object getProcessVariableValue(Map processVariables, String processvar) {
		if (processvar.indexOf("${") == 0) {
			String expr = processvar.substring(2, processvar.length() - 1);
			try {
				Object val = eval(expr, processVariables);
				System.out.println("val:" + val);
				return val;
			} catch (Throwable e) {
				e.printStackTrace();
				String msg = Utils.formatGroovyException(e,expr);
				throw new RuntimeException(msg);
			}
		} else {
			System.out.println("_getProcessVariableValue:" + processvar);
			if (processvar.indexOf(".") == -1) {
				return processVariables.get(processvar);
			}
			String[] parts = processvar.split("\\.");
			Object o = processVariables;
			for (int i = 0; i < parts.length; i++) {
				try {
					o = PropertyUtils.getProperty(o, parts[i]);
				} catch (Exception e) {
					throw new RuntimeException("TaskFormMapping.Exception:" + e.getMessage());
				}
				if (i < (parts.length - 1) && o == null) {
					throw new RuntimeException("TaskFormMapping:processvar.not_exists: " + processvar + " (" + parts[i] + ")");
				}
			}
			return o;
		}
	}

	private class MyBinding extends Binding {

		public MyBinding(Map vars) {
			super(vars);
		}

		public Object getVariable(String name) {
			if (super.hasVariable(name)) {
				return super.getVariable(name);
			}
			System.out.println("getVariable.not_defined:" + name);
			return null;
		}
	}

	public abstract static class ScriptBase extends Script {

		public ScriptBase() {
			System.out.println("ScriptBase");
		}
	}
}
