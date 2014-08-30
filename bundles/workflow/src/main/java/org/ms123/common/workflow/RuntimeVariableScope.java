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

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.activiti.engine.delegate.VariableScope;
import org.activiti.engine.RuntimeService;

public class RuntimeVariableScope implements  VariableScope {
	private RuntimeService m_runtimeService;
	private String m_executionId;
	
	public RuntimeVariableScope(RuntimeService rs, String executionId){
		m_runtimeService=rs;
		m_executionId = executionId;
	}


  public Map<String, Object> getVariables(){
		return m_runtimeService.getVariables(m_executionId);
	}

  public Map<String, Object> getVariablesLocal(){
		return m_runtimeService.getVariablesLocal(m_executionId);
	}

  public Object getVariable(String variableName){
		return m_runtimeService.getVariable(m_executionId,variableName);
	}

  public Object getVariableLocal(String variableName){
		return m_runtimeService.getVariableLocal(m_executionId,(String)variableName);
	}

  public Set<String> getVariableNames(){
		return getVariables().keySet();
	}

  public Set<String> getVariableNamesLocal(){
		return getVariablesLocal().keySet();
	}

  public void setVariable(String variableName, Object value){
		m_runtimeService.setVariable(m_executionId,variableName,value);
	}

  public Object setVariableLocal(String variableName, Object value){
		m_runtimeService.setVariableLocal(m_executionId,variableName,value);
		return null;
	}

  public void setVariables(Map<String, ? extends Object> variables){
		m_runtimeService.setVariables(m_executionId,variables);
	}

  public void setVariablesLocal(Map<String, ? extends Object> variables){
		m_runtimeService.setVariablesLocal(m_executionId,variables);
	}

  public boolean hasVariables(){
		throw new RuntimeException("RuntimeVariableScope.hasVariables_not_implemented");
	}

  public boolean hasVariablesLocal(){
		throw new RuntimeException("RuntimeVariableScope.hasVariablesLocal_not_implemented");
	}

  public boolean hasVariable(String variableName){
		//return m_runtimeService.hasVariable(m_executionId,variableName);
		throw new RuntimeException("RuntimeVariableScope.hasVariable_not_implemented");
	}

  public boolean hasVariableLocal(String variableName){
		//return m_runtimeService.hasVariableLocal(m_executionId,variableName);
		throw new RuntimeException("RuntimeVariableScope.hasVariablesLocal_not_implemented");
	}

  public void createVariableLocal(String variableName, Object value){
		throw new RuntimeException("RuntimeVariableScope.createVariablesLocal_not_implemented");
	}

  public void removeVariable(String variableName){
		//return m_runtimeService.removeVariable(m_executionId,variableName);
		throw new RuntimeException("RuntimeVariableScope.removeVariable_not_implemented");
	}

  public void removeVariableLocal(String variableName){
		//return m_runtimeService.removeVariableLocal(m_executionId,variableName);
		throw new RuntimeException("RuntimeVariableScope.removeVariableLocal_not_implemented");
	}

  public void removeVariables(Collection<String> variableNames){
		//return m_runtimeService.removeVariables(m_executionId,variableNames);
		throw new RuntimeException("RuntimeVariableScope.removeVariables_not_implemented");
	}

  public void removeVariablesLocal(Collection<String> variableNames){
		//return m_runtimeService.removeVariablesLocal(m_executionId,variableNames);
		throw new RuntimeException("RuntimeVariableScope.removeVariablesLocal_not_implemented");
	}

  public void removeVariables(){
		throw new RuntimeException("RuntimeVariableScope.removeVariables_not_implemented");
	}

  public void removeVariablesLocal(){
		throw new RuntimeException("RuntimeVariableScope.removeVariablesLocal_not_implemented");
	}

}
