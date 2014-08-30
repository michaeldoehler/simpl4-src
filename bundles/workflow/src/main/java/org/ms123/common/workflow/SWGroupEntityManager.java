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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.identity.Group;
import org.activiti.engine.impl.GroupQueryImpl;
import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.persistence.entity.GroupEntity;
import org.activiti.engine.impl.persistence.entity.GroupEntityManager;
import org.apache.commons.lang.StringUtils;
import org.ms123.common.permission.api.PermissionService;
import org.ms123.common.auth.api.AuthService;
import flexjson.*;

@SuppressWarnings("unchecked")
public class SWGroupEntityManager extends GroupEntityManager {

	protected JSONSerializer m_js = new JSONSerializer();

	protected PermissionService m_permissionService;

	protected AuthService m_authService;

	public SWGroupEntityManager(AuthService as, PermissionService ps) {
		m_js.prettyPrint(true);
		m_authService = as;
		m_permissionService = ps;
	}

	@Override
	public Group createNewGroup(String groupId) {
		throw new ActivitiException("My group manager doesn't support creating a new group");
	}

	@Override
	public void insertGroup(Group group) {
		throw new ActivitiException("My group manager doesn't support inserting a new group");
	}

	@Override
	public void deleteGroup(String groupId) {
		throw new ActivitiException("My group manager doesn't support deleting a new group");
	}

	@Override
	public long findGroupCountByQueryCriteria(GroupQueryImpl query) {
		return findGroupByQueryCriteria(query, null).size();
	}

	@Override
	public List<Group> findGroupByQueryCriteria(GroupQueryImpl query, Page page) {
		List<Group> groupList = new ArrayList<Group>();
		GroupQueryImpl groupQuery = (GroupQueryImpl) query;
		if (StringUtils.isNotEmpty(groupQuery.getId())) {
			GroupEntity singleGroup = findGroupById(groupQuery.getId());
			groupList.add(singleGroup);
			return groupList;
		} else if (StringUtils.isNotEmpty(groupQuery.getName())) {
			GroupEntity singleGroup = findGroupById(groupQuery.getId());
			groupList.add(singleGroup);
			return groupList;
		} else if (StringUtils.isNotEmpty(groupQuery.getUserId())) {
			return findGroupsByUser(groupQuery.getUserId());
		} else {
			//TODO: get all groups from your identity domain and convert them to List<Group>
			return null;
		}
	}

	//@Override
	public GroupEntity findGroupById(String activitiGroupID) {
		System.out.println("SWGroupEntityManager:findGroupById:" + activitiGroupID);
		if (m_permissionService.hasRole(activitiGroupID)) {
			GroupEntity g = convertToGroup(activitiGroupID);
			System.out.println("SWGroupEntityManager:findGroupById:" + m_js.deepSerialize(g));
			return g;
		}
		return null;
	}

	@Override
	public List<Group> findGroupsByUser(String userLogin) {
		List<Group> roleListRet = new ArrayList();
		System.out.println("SWGroupEntityManager:findGroupsByUser:" + userLogin);
		try{
			List<String> roleList = m_permissionService.getUserRoles(userLogin);
			for (String roleid : roleList) {
				roleListRet.add(convertToGroup(roleid));
			}
		}catch(Exception e){
			e.printStackTrace();
			throw new RuntimeException("SWGroupEntityManager.findGroupsByUser:",e);
		}
		System.out.println("SWGroupEntityManager:findGroupsByUser:" + m_js.deepSerialize(roleListRet));
		return roleListRet;
	}

	private GroupEntity convertToGroup(String id) {
		GroupEntity g = new GroupEntity();
		g.setId(id);
		return g;
	}
}
