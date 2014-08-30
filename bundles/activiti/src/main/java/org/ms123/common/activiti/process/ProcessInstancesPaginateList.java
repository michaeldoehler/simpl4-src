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
import java.util.Map;
import java.util.HashMap;
import org.activiti.engine.history.HistoricProcessInstance;
import org.ms123.common.activiti.AbstractPaginateList;
import org.ms123.common.activiti.BaseResource;
import org.ms123.common.activiti.Util;
import org.activiti.engine.ProcessEngine;

/**
 */
@SuppressWarnings("unchecked")
public class ProcessInstancesPaginateList extends AbstractPaginateList {

	private ProcessEngine m_pe;

	public ProcessInstancesPaginateList(BaseResource br) {
		m_pe = br.getPE();
	}

	protected List processList(List list) {
		List<Map> processResponseList = new ArrayList<Map>();
		for (Object instance : list) {
			Map responseMap = new HashMap();
			HistoricProcessInstance processInstance = (HistoricProcessInstance) instance;
			responseMap.put("id", processInstance.getId());
			responseMap.put("businessKey", processInstance.getBusinessKey());
			responseMap.put("startTime", Util.dateToString(processInstance.getStartTime()));
			responseMap.put("endTime", Util.dateToString(processInstance.getEndTime()));
			responseMap.put("_startTime",processInstance.getStartTime().getTime());
			if( processInstance.getEndTime()!=null){
				responseMap.put("_endTime", processInstance.getEndTime().getTime());
			}
			responseMap.put("duration", processInstance.getDurationInMillis());
			responseMap.put("processDefinitionId", processInstance.getProcessDefinitionId());
			responseMap.put("startUserId", processInstance.getStartUserId());
			processResponseList.add(responseMap);
		}
		return processResponseList;
	}
}
