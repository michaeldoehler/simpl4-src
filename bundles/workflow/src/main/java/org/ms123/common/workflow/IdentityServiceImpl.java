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

import java.util.List;
import java.util.Map;

import org.activiti.engine.IdentityService;
import org.activiti.engine.identity.Group;
import org.activiti.engine.identity.GroupQuery;
import org.activiti.engine.identity.Picture;
import org.activiti.engine.identity.User;
import org.activiti.engine.identity.UserQuery;
import org.activiti.engine.impl.cmd.CheckPassword;
import org.activiti.engine.impl.cmd.CreateGroupCmd;
import org.activiti.engine.impl.cmd.CreateGroupQueryCmd;
import org.activiti.engine.impl.cmd.CreateMembershipCmd;
import org.activiti.engine.impl.cmd.CreateUserCmd;
import org.activiti.engine.impl.cmd.CreateUserQueryCmd;
import org.activiti.engine.impl.cmd.DeleteGroupCmd;
import org.activiti.engine.impl.cmd.DeleteMembershipCmd;
import org.activiti.engine.impl.cmd.DeleteUserCmd;
import org.activiti.engine.impl.cmd.DeleteUserInfoCmd;
//import org.activiti.engine.impl.cmd.GetUserAccountCmd;
import org.activiti.engine.impl.cmd.GetUserInfoCmd;
import org.activiti.engine.impl.cmd.GetUserInfoKeysCmd;
import org.activiti.engine.impl.cmd.GetUserPictureCmd;
import org.activiti.engine.impl.cmd.SaveGroupCmd;
import org.activiti.engine.impl.cmd.SaveUserCmd;
import org.activiti.engine.impl.cmd.SetUserInfoCmd;
import org.activiti.engine.impl.cmd.SetUserPictureCmd;
//import org.activiti.engine.impl.identity.Account;
import org.activiti.engine.impl.identity.Authentication;
import org.activiti.engine.impl.persistence.entity.GroupEntity;
import org.activiti.engine.identity.NativeUserQuery;
import org.activiti.engine.identity.NativeGroupQuery;
import org.activiti.engine.impl.persistence.entity.IdentityInfoEntity;
import org.activiti.engine.impl.*;


/**
 * @author Tom Baeyens
 */
public class IdentityServiceImpl extends ServiceImpl implements IdentityService {
  
	public IdentityServiceImpl() {
		super();
		System.out.println("IdentityServiceImpl.IdentityServiceImpl");
	}

  public NativeUserQuery createNativeUserQuery(){
		throw new RuntimeException("createNativeUserQuery bnot implemented");
	}
  public NativeGroupQuery createNativeGroupQuery(){
		throw new RuntimeException("createNativeGroupQuery bnot implemented");
	}

	public Group newGroup(String groupId) {
		System.out.println("IdentityServiceImpl.newGroup");
		return commandExecutor.execute(new CreateGroupCmd(groupId));
	}

	public User newUser(String userId) {
		System.out.println("IdentityServiceImpl.newUser");
		return commandExecutor.execute(new CreateUserCmd(userId));
	}

	public void saveGroup(Group group) {
		System.out.println("IdentityServiceImpl.saveGroup");
		commandExecutor.execute(new SaveGroupCmd((GroupEntity) group));
	}

	public void saveUser(User user) {
		System.out.println("IdentityServiceImpl.saveUser");
		commandExecutor.execute(new SaveUserCmd(user));
	}
  
	public UserQuery createUserQuery() {
		System.out.println("IdentityServiceImpl.createUserQuery");
		return commandExecutor.execute(new CreateUserQueryCmd());
	}
  
	public GroupQuery createGroupQuery() {
		System.out.println("IdentityServiceImpl.createGroupQuery");
		return commandExecutor.execute(new CreateGroupQueryCmd());
	}

	public void createMembership(String userId, String groupId) {
		System.out.println("IdentityServiceImpl.createMembership");
		commandExecutor.execute(new CreateMembershipCmd(userId, groupId));
	}

	public void deleteGroup(String groupId) {
		commandExecutor.execute(new DeleteGroupCmd(groupId));
	}

	public void deleteMembership(String userId, String groupId) {
		commandExecutor.execute(new DeleteMembershipCmd(userId, groupId));
	}

	public boolean checkPassword(String userId, String password) {
		System.out.println("IdentityServiceImpl.checkPassword");
		return commandExecutor.execute(new CheckPassword(userId, password));
	}

	public void deleteUser(String userId) {
		commandExecutor.execute(new DeleteUserCmd(userId));
	}

	public void setUserPicture(String userId, Picture picture) {
		commandExecutor.execute(new SetUserPictureCmd(userId, picture));
	}

	public Picture getUserPicture(String userId) {
		return commandExecutor.execute(new GetUserPictureCmd(userId));
	}

	public void setAuthenticatedUserId(String authenticatedUserId) {
		System.out.println("IdentityServiceImpl.setAuthenticatedUserId:"+authenticatedUserId);
		Authentication.setAuthenticatedUserId(authenticatedUserId);
	}

	public String getUserInfo(String userId, String key) {
		System.out.println("IdentityServiceImpl.getUserInfo:" + userId);
		return commandExecutor.execute(new GetUserInfoCmd(userId, key));
	}

	public List<String> getUserInfoKeys(String userId) {
		System.out.println("IdentityServiceImpl.getUserInfoKeys:" + userId);
		return commandExecutor.execute(new GetUserInfoKeysCmd(userId, IdentityInfoEntity.TYPE_USERINFO));
	}

	/*public List<String> getUserAccountNames(String userId) {
		System.out.println("getUserAccountNames:" + userId);
		return commandExecutor.execute(new GetUserInfoKeysCmd(userId, IdentityInfoEntity.TYPE_USERACCOUNT));
	}*/

	public void setUserInfo(String userId, String key, String value) {
		System.out.println("IdentityServiceImpl.setUserInfo:" + userId);
		commandExecutor.execute(new SetUserInfoCmd(userId, key, value));
	}

	public void deleteUserInfo(String userId, String key) {
		commandExecutor.execute(new DeleteUserInfoCmd(userId, key));
	}

	public void deleteUserAccount(String userId, String accountName) {
		commandExecutor.execute(new DeleteUserInfoCmd(userId, accountName));
	}

/*	public Account getUserAccount(String userId, String userPassword, String accountName) {
		System.out.println("getUserAccount:" + userId);
		return commandExecutor.execute(new GetUserAccountCmd(userId, userPassword, accountName));
	}

	public void setUserAccount(String userId, String userPassword, String accountName, String accountUsername, String accountPassword, Map<String, String> accountDetails) {
		System.out.println("setUserAccount:" + userId);
		commandExecutor.execute(new SetUserInfoCmd(userId, userPassword, accountName, accountUsername, accountPassword, accountDetails));
	}*/
}
