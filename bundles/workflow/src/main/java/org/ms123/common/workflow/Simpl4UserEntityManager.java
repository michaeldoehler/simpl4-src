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
import java.util.List;
import java.util.Map;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.identity.User;
import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.UserQueryImpl;
import org.activiti.engine.impl.persistence.entity.UserEntity;
import org.activiti.engine.impl.persistence.entity.UserEntityManager;
import org.apache.commons.lang.StringUtils;
import org.ms123.common.permission.api.PermissionService;
import org.ms123.common.auth.api.AuthService;
import flexjson.*;

@SuppressWarnings("unchecked")
public class Simpl4UserEntityManager extends UserEntityManager {
	protected JSONSerializer m_js = new JSONSerializer();

	protected PermissionService m_permissionService;

	protected AuthService m_authService;

	public Simpl4UserEntityManager(AuthService as, PermissionService ps) {
		m_js.prettyPrint(true);
		m_authService = as;
		m_permissionService = ps;
	}

	@Override
	public User createNewUser(String userId) {
		throw new ActivitiException("My user manager doesn't support creating a new user");
	}

	@Override
	public void insertUser(User user) {
		throw new ActivitiException("My user manager doesn't support inserting a new user");
	}

	@Override
	public void deleteUser(String userId) {
		throw new ActivitiException("My user manager doesn't support deleting a user");
	}

	@Override
	public UserEntity findUserById(String userLogin) {
		//TODO: get my user according to userLogin and convert it to UserEntity
		throw new ActivitiException("My user manager doesn't support finding a user");
	}

	@Override
	public List<User> findUserByQueryCriteria(UserQueryImpl query, Page page) {
		System.out.println("Simpl4UserEntityManager.findUserByQueryCriteria:"+m_js.deepSerialize(query));
		List<User> userList = new ArrayList<User>();
		UserQueryImpl userQuery = (UserQueryImpl) query;
		if (StringUtils.isNotEmpty(userQuery.getId())) {
			userList.add(convertToUser(m_authService.getUser(userQuery.getId())));
			System.out.println("Simpl4UserEntityManager.findUserByQueryCriteria:"+m_js.deepSerialize(userList));
			return userList;
		} else {
			//TODO: get all users from your identity domain and convert them to List<User>
			return null;
		}
	}

	@Override
	public long findUserCountByQueryCriteria(UserQueryImpl query) {
		return findUserByQueryCriteria(query, null).size();
	}

	@Override
	public Boolean checkPassword(String userId, String password) {
		//TODO: check the password in your domain and return the appropriate boolean
		return false;
	}
	private User convertToUser(Map<String,String> userProps){
		User u =new UserEntity();
		u.setId( userProps.get("userid"));
		return u;
	}
}
