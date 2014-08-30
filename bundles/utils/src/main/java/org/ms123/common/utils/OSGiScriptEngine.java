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


import java.io.Reader;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;


public class OSGiScriptEngine implements ScriptEngine {
	private ScriptEngine engine;
	private OSGiScriptEngineFactory factory;
	public OSGiScriptEngine(ScriptEngine engine, OSGiScriptEngineFactory factory) {
		this.engine = engine;
		this.factory = factory;
	}

	public Bindings createBindings() {
		return engine.createBindings();
	}

	public Object eval(Reader reader, Bindings n) throws ScriptException {
		return engine.eval(reader, n);
	}

	public Object eval(Reader reader, ScriptContext context) throws ScriptException {
		return engine.eval(reader, context);
	}

	public Object eval(Reader reader) throws ScriptException {
		return engine.eval(reader);
	}

	public Object eval(String script, Bindings n) throws ScriptException {
		return engine.eval(script, n);
	}

	public Object eval(String script, ScriptContext context) throws ScriptException {
		return engine.eval(script, context);
	}

	public Object eval(String script) throws ScriptException {
		return engine.eval(script);
	}

	public Object get(String key) {
		return engine.get(key);
	}

	public Bindings getBindings(int scope) {
		return engine.getBindings(scope);
	}

	public ScriptContext getContext() {
		return engine.getContext();
	}

	public ScriptEngineFactory getFactory() {
		return factory;
	}

	public void put(String key, Object value) {
		engine.put(key, value);
	}

	public void setBindings(Bindings bindings, int scope) {
		engine.setBindings(bindings, scope);
	}

	public void setContext(ScriptContext context) {
		engine.setContext(context);
	}

}
