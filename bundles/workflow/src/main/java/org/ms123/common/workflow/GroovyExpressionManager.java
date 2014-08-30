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

import org.activiti.engine.delegate.VariableScope;
import org.activiti.engine.impl.el.ExpressionManager;
import org.activiti.engine.impl.javax.el.CompositeELResolver;
import groovy.lang.*;
import org.codehaus.groovy.control.*;
//import org.activiti.engine.delegate.Expression;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.impl.el.Expression;

/**
 */
public class GroovyExpressionManager extends ExpressionManager {
	private ProcessEngine m_processEngine;

	public GroovyExpressionManager() {
	}

	public void setProcessEngine(ProcessEngine pe) {
		m_processEngine = pe;
	}

	public synchronized Expression createExpression(String expression) {
		long st = new java.util.Date().getTime();
		CompilerConfiguration config = new CompilerConfiguration();
		config.setScriptBaseClass(org.ms123.common.workflow.api.GroovyTaskDslBase.class.getName());
		GroovyShell shell = new GroovyShell(this.getClass().getClassLoader(), new Binding(), config);
		System.out.println("createExpression:" + (new java.util.Date().getTime() - st));
		return new GroovyExpression(shell, m_processEngine, expression);
	}

}

