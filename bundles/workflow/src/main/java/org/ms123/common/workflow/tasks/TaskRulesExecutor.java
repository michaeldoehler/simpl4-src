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
package org.ms123.common.workflow.tasks;

import java.util.*;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.activiti.engine.impl.el.Expression;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.scripting.ScriptingEngines;
import org.ms123.common.data.api.DataLayer;
import javax.transaction.UserTransaction;
import org.ms123.common.data.api.SessionContext;
import org.apache.commons.beanutils.*;
import flexjson.*;
import org.ms123.common.store.StoreDesc;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.ProcessEngine;
import org.ms123.common.git.GitService;
import org.ms123.common.workflow.RulesProcessor;

@SuppressWarnings("unchecked")
public class TaskRulesExecutor extends TaskBaseExecutor implements JavaDelegate {

	protected JSONDeserializer m_ds = new JSONDeserializer();

	protected JSONSerializer m_js = new JSONSerializer();

	private Expression rulesname;

	private Expression variablesmapping;

	@Override
	public void execute(DelegateExecution execution) {
		TaskContext tc = new TaskContext();
		tc.setExecution(execution);
		setCategory(tc);
		String namespace = tc.getCategory();
		String rname = rulesname.getValue(execution).toString();
		String vm = variablesmapping.getValue(execution).toString();
		Map map = (Map) m_ds.deserialize(vm);
		List<Map> varmap = (List<Map>) map.get("items");
		m_js.prettyPrint(true);
		System.out.println("varmap:" + m_js.deepSerialize(varmap));
		Map<String, Object> values = new HashMap();
		try {
			for (Map<String, String> m : varmap) {
				String direction = m.get("direction");
				if (direction.equals("incoming")) {
					String processvar = m.get("processvar");
					Object o = getValue(execution, processvar);
					String rulesvar = m.get("rulesvar");
					values.put(rulesvar, o);
				}
			}
			System.out.println("Incomingvalues:" + m_js.deepSerialize(values));
			Map rules = getRules(rname, namespace);
			RulesProcessor rp = new RulesProcessor(rules, values);
			Map ret = rp.execute();
			System.out.println("RulesProcessor.ret:" + ret);
			for (Map<String, String> m : varmap) {
				String direction = m.get("direction");
				if (direction.equals("outgoing")) {
					String rulesvar = m.get("rulesvar");
					Object o = ret.get(rulesvar);
					String processvar = m.get("processvar");
					System.out.println("ProcessVarsetting:processvar:" + processvar + "->rulesvar:" + rulesvar + "("
							+ o + ")");
					setValue(execution, processvar, o);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("TaskRulesExecutor.execute:", e);
		}
	}

	public Map getRules(String name, String namespace) {
		Map beans = Context.getProcessEngineConfiguration().getBeans();
		GitService gitService = (GitService) beans.get("gitService");
		String filterJson = gitService.searchContent(namespace, name, "sw.rule");
		Map contentMap = (Map) m_ds.deserialize(filterJson);
		return contentMap;
	}
}

