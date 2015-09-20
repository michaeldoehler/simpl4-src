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

import groovy.text.Template;
import java.util.*;
import groovy.lang.*;
import org.asciidoctor.groovydsl.AsciidoctorExtensions
import org.asciidoctor.Asciidoctor

//@groovy.transform.CompileStatic
public class AsciidoctorX{
	public AsciidoctorX(){
	}

	def ext1 = {  
			block(name: 'BIG', contexts: [':paragraph']) {
					parent, reader, attributes ->
					def upperLines = reader.readLines()
					.collect {it.toUpperCase()}
					.inject('') {a, b -> a + '\n' + b}

					createBlock(parent, 'paragraph', [upperLines], attributes, [:])
			}
	}

	def ext2 = {  
			blockmacro (name: 'imagezoom') {
						parent, target, attributes ->
System.out.println("parent:"+parent);
System.out.println("target:"+target);
System.out.println("attributes:"+attributes);
						String content = "<simpl-zoom image=\"${target}\"></simpl-zoom>"
						createBlock(parent, "pass", [content], attributes, config);
				}
	}
	public void register(){
			AsciidoctorExtensions.extensions(ext1);
			AsciidoctorExtensions.extensions(ext2);
	}
}
