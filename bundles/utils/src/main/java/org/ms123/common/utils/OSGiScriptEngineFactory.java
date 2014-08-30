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
package org.ms123.common.utils;


import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;


/**
 *
 */
public class OSGiScriptEngineFactory implements ScriptEngineFactory {
	private ScriptEngineFactory factory;
	private ClassLoader contextClassLoader;
	public OSGiScriptEngineFactory(ScriptEngineFactory factory, ClassLoader contextClassLoader) {
		this.factory = factory;
		this.contextClassLoader = contextClassLoader;
	}

	public String getEngineName() {
		return factory.getEngineName();
	}

	public String getEngineVersion() {
		return factory.getEngineVersion();
	}

	public List<String> getExtensions() {
		return factory.getExtensions();
	}

	public String getLanguageName() {
		return factory.getLanguageName();
	}

	public String getLanguageVersion() {
		return factory.getLanguageVersion();
	}

	public String getMethodCallSyntax(String obj, String m, String... args) {
		return factory.getMethodCallSyntax(obj, m, args);
	}

	public List<String> getMimeTypes() {
		return factory.getMimeTypes();
	}

	public List<String> getNames() {
		return factory.getNames();
	}

	public String getOutputStatement(String toDisplay) {
		return factory.getOutputStatement(toDisplay);
	}

	public Object getParameter(String key) {
		return factory.getParameter(key);
	}

	public String getProgram(String... statements) {
		return factory.getProgram(statements);
	}

	public ScriptEngine getScriptEngine() {
		ScriptEngine engine = null;
		if (contextClassLoader != null) {
			ClassLoader old = Thread.currentThread().getContextClassLoader();
			Thread.currentThread().setContextClassLoader(contextClassLoader);
			engine = factory.getScriptEngine();
			Thread.currentThread().setContextClassLoader(old);
		} else {
			engine = factory.getScriptEngine();
		}
		return engine;
	}
	
}
