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
package org.ms123.common.workflow;

import java.util.Date;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.delegate.VariableScope;
import org.activiti.engine.impl.pvm.delegate.ActivityExecution;
import org.activiti.engine.impl.context.Context;
import groovy.lang.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.codehaus.groovy.control.*;
// import org.activiti.engine.delegate.Expression;
import org.ms123.common.libhelper.Utils;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.history.HistoricActivityInstance;

import org.activiti.engine.impl.el.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Manfred Sattler
 */
@SuppressWarnings({"unchecked","deprecation"})
public class GroovyExpression implements Expression {
	private static final Logger m_logger = LoggerFactory.getLogger(GroovyExpression.class);
	protected String m_expressionText;
	protected GroovyShell m_shell;
	protected ProcessEngine m_processEngine;

	private static ConcurrentMap<String, Script> m_scriptCache = new ConcurrentLinkedHashMap.Builder<String, Script>()
			.maximumWeightedCapacity(100).build();

	public GroovyExpression(GroovyShell shell, ProcessEngine pe, String expressionText) {
		log("GroovyExpression:" + expressionText);
		m_shell = shell;
		m_processEngine = pe;
		m_expressionText = expressionText;
	}

	public synchronized Object getValue(VariableScope variableScope) {
		long start = new Date().getTime();
		log("GroovyExpression.getValue-->" + m_expressionText);
		Object o = expandString(m_expressionText, variableScope);
		long end = new Date().getTime();
		log("GroovyExpression.getValue<---:" + o);
		log("TIME:" + (end - start));
		return o;
	}

	public void setValue(Object value, VariableScope variableScope) {
		log("GroovyExpression.setValue:" + value);
	}

	@Override
	public String toString() {
		return m_expressionText;
	}

	public String getExpressionText() {
		return m_expressionText;
	}

	private synchronized Object eval(String expr, VariableScope scope) {
		try {
			Script script = null;//m_scriptCache.get(expr);
			script = m_shell.parse(expr);
			Binding binding = new Binding(scope.getVariables());
			script.setBinding(binding);
			log("GroovyExpression.vars:" + scope.getVariables());
			return script.run();
		} catch (Throwable e) {
			log(">>>>>>>>>>>>" + e);
			e.printStackTrace();
			String msg = Utils.formatGroovyException(e, expr);
			if (scope instanceof ActivityExecution) {
				ActivityExecution de = (ActivityExecution) scope;
				String activityId = de.getCurrentActivityId();
				List<HistoricActivityInstance> activityList = m_processEngine.getHistoryService()
						.createHistoricActivityInstanceQuery().activityId(activityId).list();
				String activityName = "";
				for (HistoricActivityInstance h : activityList) {
					log("h:" + h.getActivityName());
					if ("".equals(activityName)) {
						activityName = h.getActivityName();
					}
				}
				msg = activityName + "(" + activityId + ")|" + msg;
			}
			throw new RuntimeException(msg);
		}
	}

	private Object getService(String clazzName) {
		log("getService:" + clazzName);
		Map beans = Context.getProcessEngineConfiguration().getBeans();
		BundleContext bc = (BundleContext) beans.get("bundleContext");
		log("__beans__:" + beans);
		ServiceReference sr = bc.getServiceReference(clazzName);
		log("sr___:" + sr);
		Object o = bc.getService(sr);
		log("o:" + o);
		return o;
	}

	private synchronized Object expandString(String str, VariableScope scope) {
		if (str.startsWith("@")) {
			return getService(str.substring(1));
		}
		if (str.startsWith("~")) {
			return str.substring(1);
		}
		int countRepl = 0;
		int countPlainStr = 0;
		Object replacement = null;
		String newString = "";
		int openBrackets = 0;
		int first = 0;
		/*		if( str.indexOf("${") == -1){
		 return eval(str, binding,scope);
		 }*/
		for (int i = 0; i < str.length(); i++) {
			if (i < str.length() - 2 && str.substring(i, i + 2).compareTo("${") == 0) {
				if (openBrackets == 0) {
					first = i + 2;
				}
				openBrackets++;
			} else if (str.charAt(i) == '}' && openBrackets > 0) {
				openBrackets -= 1;
				if (openBrackets == 0) {
					countRepl++;
log("Eval1:"+str);
log("Eval2:"+str.substring(first,i));
log("Eval3:"+scope.getVariables());
					replacement = eval(str.substring(first, i), scope);
					newString += replacement;
				}
			} else if (openBrackets == 0) {
				newString += str.charAt(i);
				countPlainStr++;
			}
		}
		if (countRepl == 1 && countPlainStr == 0) {
			return replacement;
		} else {
			return newString;
		}
	}

	private void log(String message) {
		m_logger.info(message);
		System.out.println(message);
	}
}

