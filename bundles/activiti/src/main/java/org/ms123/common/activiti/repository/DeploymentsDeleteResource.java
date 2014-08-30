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
package org.ms123.common.activiti.repository;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import org.ms123.common.activiti.ActivitiService;
import org.ms123.common.activiti.BaseResource;
import org.ms123.common.activiti.Util;

/**
 */
@SuppressWarnings("unchecked")
public class DeploymentsDeleteResource extends BaseResource {
	List<String> m_deploymentIds;
	boolean m_cascade;

	public DeploymentsDeleteResource(ActivitiService as, List<String> deploymentIds, boolean cascade) {
		super(as, null);
		m_deploymentIds = deploymentIds;
		m_cascade = cascade;
	}

	public Map execute() {
    try {
      Boolean cascade = m_cascade;
      for (String deploymentId : m_deploymentIds) {
        if (cascade) {
          getPE().getRepositoryService().deleteDeployment(deploymentId, true);
        }
        else {
          getPE().getRepositoryService().deleteDeployment(deploymentId);
        }
      }
			Map successNode = new HashMap();
			successNode.put("success", true);
			return successNode;
    } catch(Exception e) {
      throw new RuntimeException("Failed to delete deployments", e);
    }
	}
}
