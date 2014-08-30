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
package org.ms123.common.data.scripting;

import org.mvel2.*;
import org.mvel2.integration.*;
import org.mvel2.compiler.*;
import org.mvel2.integration.impl.*;
import org.mvel2.util.*;
import java.io.*;
import java.util.*;
import javax.jdo.PersistenceManager;

@SuppressWarnings("unchecked")
public class MVELEvaluator {

	private Map<String, Serializable> m_compiledExpr = new HashMap();

	private ParserContext m_ctx = new ParserContext();

	private VariableResolverFactory m_globalFactory = null;

	private VariableResolverFactory m_localFactory = null;

	private boolean m_analize = false;

	private PersistenceManager m_pm;

	private Map<String, Class> m_module;

	public MVELEvaluator() {
		this(null);
		m_analize = true;
	}

	public MVELEvaluator(Map vars, Map<String, Class> entity, PersistenceManager pm) {
		m_globalFactory = new MapVariableResolverFactory(vars);
		m_pm = pm;
		m_module = entity;
		addImports();
	}

	public MVELEvaluator(Map vars) {
		m_globalFactory = new MapVariableResolverFactory(vars);
		addImports();
	}

	private void addImports() {
		try {
			m_ctx.addImport("NN", MVELEvaluator.class.getMethod("notNull", String.class));
			m_ctx.addPackageImport("java.util");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setLocalVars(Map vars) {
		m_localFactory = new MyVariableResolverFactory(new HashMap(vars), m_module, m_pm);
	}

	public void setLocalVar(String var, Object value) {
		m_localFactory.createVariable(var, value);
	}

	public List<String> getInputs() {
		List<String> iList = new ArrayList();
		Map in = m_ctx.getInputs();
		Map im = m_ctx.getImports();
		Iterator<String> it = in.keySet().iterator();
		while (it.hasNext()) {
			String key = it.next();
			if ("se".equals(key)) {
				continue;
			}
			if (key.indexOf("(") != -1) {
				continue;
			}
			if (im.get(key) != null) {
				continue;
			}
			iList.add(key);
		}
		return iList;
	}

	public Object eval(String x) {
		VariableResolverFactory factory = m_globalFactory;
		if (m_localFactory != null) {
			factory = m_localFactory;
			m_localFactory.setNextFactory(m_globalFactory);
		}
		Serializable expr = null;
		if (m_compiledExpr.get(x) == null) {
			ExpressionCompiler ec = new ExpressionCompiler(x);
			if (m_analize) {
				ec.setVerifyOnly(true);
				ec.compile(m_ctx);
			} else {
				expr = ParseTools.optimizeTree(ec.compile(m_ctx));
			}
			m_compiledExpr.put(x, expr);
		} else {
			expr = m_compiledExpr.get(x);
		}
		if (expr != null) {
			return ((ExecutableStatement) expr).getValue(m_ctx, factory);
		}
		return null;
	}

	public static String notNull(String x) {
		if (x == null) {
			return "";
		}
		return x;
	}

	public void test() {
		String expression = "NN(letter_salutation_enum[salutation_key].BriefanredeBeginn) + se.eval(letter_salutation_enum[salutation_key].BriefanredeReihe1)";
		Map vars = new HashMap();
		MVELEvaluator ana = new MVELEvaluator();
		ana.eval(expression);
		System.out.println("Input:" + ana.getInputs());
		Map anredebeg = new HashMap();
		anredebeg.put("BriefanredeBeginn", "XXXXX");
		anredebeg.put("BriefanredeReihe1", "Reihe1");
		Map letter_salutation_enum = new HashMap();
		letter_salutation_enum.put("01", anredebeg);
		vars.put("letter_salutation_enum", letter_salutation_enum);
		vars.put("salutation_key", "01");
		vars.put("Reihe1", "YYYY");
		vars.put("foobar", 100);
		MVELEvaluator e = new MVELEvaluator(vars);
		vars.put("se", e);
		long start = new Date().getTime();
		Map lvars = new HashMap();
		lvars.put("Reihe1", "UYYY");
		e.setLocalVars(lvars);
		e.setLocalVar("Reihe1", "ZYYY");
		Object result = e.eval(expression);
		System.out.println("Result:" + result);
		lvars.put("Reihe1", "WYYY");
		e.setLocalVars(lvars);
		result = e.eval(expression);
		/* for(int i= 0; i< 10000000;i++){
		 result = e.eval(expression);
		 result = e.eval(expression);
		 result = e.eval(expression);
		 }*/
		long end = new Date().getTime();
		System.out.println("Dauer:" + (end - start));
		System.out.println("Result:" + result);
		System.out.println("It works:" + ("XXXXXWYYY".equals(result) ? "Yes" : "Not"));
	}
}
