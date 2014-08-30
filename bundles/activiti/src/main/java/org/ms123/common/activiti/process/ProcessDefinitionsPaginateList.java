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
package org.ms123.common.activiti.process;

import java.util.ArrayList;
import java.util.List;
import org.ms123.common.activiti.BaseResource;
import org.activiti.engine.form.StartFormData;
import org.activiti.engine.impl.RepositoryServiceImpl;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.ms123.common.activiti.AbstractPaginateList;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.impl.persistence.entity.IdentityLinkEntity;
import org.activiti.engine.task.IdentityLink;

/**
 */
@SuppressWarnings("unchecked")
public class ProcessDefinitionsPaginateList extends AbstractPaginateList {

	private ProcessEngine m_pe;
	private String m_starterGroup;

	public ProcessDefinitionsPaginateList(BaseResource br,String starterGroup) {
		m_pe = br.getPE();
		m_starterGroup = starterGroup;
	}

	protected List processList(List list) {
		List<ProcessDefinitionResponse> responseProcessDefinitions = new ArrayList<ProcessDefinitionResponse>();
		for (Object definition : list) {
			if( m_starterGroup != null){
				boolean ok = false;
      	List<IdentityLink> links = m_pe.getRepositoryService().getIdentityLinksForProcessDefinition(((ProcessDefinitionEntity) definition).getId());
				//List<IdentityLinkEntity> links = ((ProcessDefinitionEntity)definition).getIdentityLinks();
				for(IdentityLink il : links){
					System.out.println("IdentityLink:"+il.getGroupId()+"/"+il.getUserId()+"/"+m_starterGroup);
					if(il.getGroupId() != null && il.getGroupId().equals(m_starterGroup)){
						ok=true;
					}
				}
				System.out.println("ok:"+ok);
				if(!ok) continue;
			}
			ProcessDefinitionResponse processDefinition = new ProcessDefinitionResponse((ProcessDefinitionEntity) definition);
			StartFormData startFormData = m_pe.getFormService().getStartFormData(((ProcessDefinitionEntity) definition).getId());
			if (startFormData != null) {
				processDefinition.setStartFormResourceKey(startFormData.getFormKey());
			}
			processDefinition.setGraphicNotationDefined(isGraphicNotationDefined(((ProcessDefinitionEntity) definition).getId()));
			responseProcessDefinitions.add(processDefinition);
		}
		return responseProcessDefinitions;
	}

	private boolean isGraphicNotationDefined(String id) {
		try {
			return ((ProcessDefinitionEntity) ((RepositoryServiceImpl) m_pe.getRepositoryService()).getDeployedProcessDefinition(id)).isGraphicalNotationDefined();
		} catch (Exception e) {
		}
		return false;
	}
}
