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
import groovy.lang.*;
import groovy.transform.*;
import org.codehaus.groovy.control.*;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.builder.*;
import org.codehaus.groovy.ast.expr.*;

@CompileStatic
@TypeChecked
public class FunctionCallVisitor extends ClassCodeVisitorSupport implements Constants{

	private List<Map> m_funcCallList = [];
	public void visitMethodCallExpression(MethodCallExpression node) {
		super.visitMethodCallExpression(node);
		def text = node.text;
		if( !text.startsWith("this.")){
			return;
		}
		def method = node.method.text;
		def funcCallMap = [:];
		m_funcCallList.add(funcCallMap);
		funcCallMap[METHOD] = method;
		def argList = [];
		node.getArguments().each{arg->
			if( arg instanceof ListExpression){
				def list = [];
				argList.add(list);
				arg.expressions.each{e->
					list.add(getValue(e))
				}
			}else{
				argList.add(getValue(arg));
			}
		}
		funcCallMap[ARGS] = argList;
	}
	def getValue(e){
		if( e instanceof ConstantExpression){
			return e.value;
		}
		if( e instanceof VariableExpression){
			return e.text;
		}
		return null;
	}


	protected SourceUnit getSourceUnit() {
		return null;
	} 
	public static List<Map> getFunctionCalls(String sourceText){
		def visitor = new FunctionCallVisitor();
		ClassNode ast = (ClassNode)new AstBuilder().buildFromString(CompilePhase.CONVERSION, false, sourceText).find { it.class == ClassNode.class };
		ast.visitContents(visitor);
		return visitor.m_funcCallList;
	}
}


//def sourceText = "fuzzy(feld1,0.85) || equal(plz)";
//println FunctionCallVisitor.getFunctionCalls(sourceText);
