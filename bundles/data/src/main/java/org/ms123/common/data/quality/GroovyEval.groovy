/*
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
package org.ms123.common.data.quality;

import java.io.*;
import java.util.*;
import flexjson.*;
import groovy.lang.*;
import org.codehaus.groovy.control.*;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
@groovy.transform.CompileStatic
@groovy.transform.TypeChecked
public class GroovyEval implements Constants{
	private Script m_script;

	public GroovyEval(Map fieldset){
		String expr = (String)fieldset.get(EXPRESSION);
		m_script = parse(expr);
		setProperty("fieldset", fieldset);
	}

	private Script parse(String scriptStr) {
		ClassLoader parentLoader = this.getClass().getClassLoader();
		CompilerConfiguration config = new CompilerConfiguration();
		config.getOptimizationOptions().put("indy", false);
		config.setScriptBaseClass(org.ms123.common.data.quality.GroovyBase.class.getName());
		GroovyClassLoader loader =   new GroovyClassLoader(parentLoader,config);

		GroovyShell sh = new GroovyShell(loader,new Binding(),config);
		GroovyCodeSource gcs = new GroovyCodeSource( scriptStr, "QualityCheckEval", "/groovy/shell");
		return sh.parse(gcs);
	}

	public Object eval(refObj,candidate){
		return run(m_script,refObj, candidate);
	}
	public void setProperty(String name, Object value){
		m_script.setProperty(name,value);
	}
	private Object run(Script script, Object refObj, Object candidate) {
		script.setProperty("__refObj",refObj);
		script.setProperty("__candidate",candidate);
		return script.run();
	}
	private void debug(String message) {
		m_logger.debug(message);
		System.out.println(message);
	}

	private void info(String message) {
		m_logger.info(message);
		System.out.println(message);
	}

	private static final Logger m_logger = LoggerFactory.getLogger(GroovyEval.class);
}

































