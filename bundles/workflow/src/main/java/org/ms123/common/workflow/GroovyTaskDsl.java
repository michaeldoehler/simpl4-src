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


import java.io.*;
import groovy.lang.*;
import org.codehaus.groovy.control.*;
import java.util.*;
import org.ms123.common.libhelper.Utils;

import org.ms123.common.data.api.SessionContext;
import org.ms123.common.workflow.api.WorkflowService;
import org.ms123.common.camel.api.CamelService;
import org.osgi.service.event.EventAdmin;
import org.osgi.framework.BundleContext;

import org.codehaus.groovy.control.customizers.*;

@SuppressWarnings("unchecked")
public class GroovyTaskDsl {
	private SessionContext m_sessionContext;
	private Binding m_binding;
	private GroovyShell m_shell;
	private String m_hint;
	public GroovyTaskDsl(SessionContext sc, EventAdmin ea, WorkflowService ws, String namespace, String processDefinitionKey, String pid, String hint, Map<String, Object> vars) {
		m_sessionContext = sc;
		m_hint = hint;
		CompilerConfiguration config = new CompilerConfiguration();
		config.setDebug(true);
		config.setScriptBaseClass(org.ms123.common.workflow.api.GroovyTaskDslBase.class.getName());
		vars.put("__sessionContext", sc);
		vars.put("__eventAdmin", ea);
		vars.put("__workflowService", ws);
		vars.put("__camelService", ws.lookupServiceByName(CamelService.class.getName()));
		vars.put("__pid", pid);
		vars.put("__namespace", namespace);
		vars.put("__processDefinitionKey", processDefinitionKey);
		vars.put("__queriedObjects", new ArrayList());
		vars.put("__createdObjects", new ArrayList());
		m_binding = new Binding(vars);

		ImportCustomizer importCustomizer = new ImportCustomizer();
		importCustomizer.addStaticStars( "java.util.concurrent.TimeUnit");
		//importCustomizer.addStaticImport( "java.util.concurrent.TimeUnit.MILLISECONDS", "toDays");
		//importCustomizer.addImports( "java.util.concurrent.TimeUnit");
		importCustomizer.addStarImports("org.apache.camel");
		importCustomizer.addStarImports("org.apache.camel.impl");
		importCustomizer.addStarImports("org.apache.camel.builder");
		config.addCompilationCustomizers(importCustomizer);

		m_shell = new GroovyShell(this.getClass().getClassLoader(), m_binding, config);
	}
	
	public Object eval(String scriptStr) {
		try {
			return m_shell.evaluate(scriptStr);
		} catch (Throwable e) {
			String msg = Utils.formatGroovyException(e,scriptStr);
			String hint = "";
			if( m_hint != null){
				hint = "\n-----------------------------\n"+m_hint+"\n";
				hint += "------------------------------\n";
			}
			throw new RuntimeException(hint +msg);
		}
	}

	public List<Object> getCreatedObjects() {
		return (List) m_binding.getVariable("__createdObjects");
	}

	public List<Object> getQueriedObjects() {
		return (List) m_binding.getVariable("__queriedObjects");
	}

}
