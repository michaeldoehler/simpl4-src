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
import java.security.MessageDigest;

@groovy.transform.CompileStatic
public class TemplateEvaluator{
	private static Map<String, Template> m_cache = Collections.synchronizedMap(new TemplateCache());
	private GroovyShell m_shell;
	public TemplateEvaluator(GroovyShell shell){
		m_shell = shell;
	}
	public String render(String text,Map params){
		long starttime= new java.util.Date().getTime();
		String key = getMD5OfUTF8(text);
		Template temp = m_cache.get(key);
		if( !temp ){
			SimpleTemplateEngine engine = new SimpleTemplateEngine(m_shell);
			temp = engine.createTemplate(text);
			m_cache.put(key,temp);
		}
		long time = new java.util.Date().getTime(); System.out.println("groovy.time:" + (time - starttime));
		Map m= [:];
		m.putAll( params );
		String ret = temp.make( m ).toString();

		return ret;
	}
	private static String getMD5OfUTF8(String text) {
		try {
			MessageDigest msgDigest = MessageDigest.getInstance("MD5");
			byte[] mdbytes = msgDigest.digest(text.getBytes("UTF-8"));
			StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < mdbytes.length; i++) {
				String hex = Integer.toHexString(0xff & mdbytes[i]);
				if (hex.length() == 1){
					hexString.append('0');
				}
				hexString.append(hex);
			}
			return hexString.toString();
		} catch (Exception ex) {
			throw new RuntimeException("TemplateEvaluator.getMD5OfUTF8");
		}
	}
}
