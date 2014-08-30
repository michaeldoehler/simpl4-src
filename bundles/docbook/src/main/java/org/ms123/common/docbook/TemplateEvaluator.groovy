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
package org.ms123.common.docbook;

import groovy.text.GStringTemplateEngine;
import groovy.text.SimpleTemplateEngine;
import groovy.text.Template;
import java.util.*;
import groovy.lang.*;
import org.codehaus.groovy.control.*;

@groovy.transform.CompileStatic
public class TemplateEvaluator{
	private static Map<String, Template> m_cache = Collections.synchronizedMap(new TemplateCache());
	private GroovyShell m_shell;
	public TemplateEvaluator(GroovyShell shell){
		m_shell = shell;
	}
	public String render(String text,Map params){
		long starttime= new java.util.Date().getTime();
		Template temp = m_cache.get(text);
		if( !temp ){
			//GStringTemplateEngine engine = new GStringTemplateEngine();
			SimpleTemplateEngine engine = new SimpleTemplateEngine(m_shell);
			temp = engine.createTemplate(text);
			m_cache.put(text,temp);
		}
		long time = new java.util.Date().getTime(); System.out.println("groovy.time:" + (time - starttime));
//		Map m= [:].withDefault{key->
//			return "("+key+")"
//		}
		Map m= [:];
		m.putAll( params );
		String ret = temp.make( m ).toString();

		return ret;
	}
}
