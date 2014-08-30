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
package org.ms123.common.permission;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleRole;
import org.apache.shiro.realm.*;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.CollectionUtils;

/**
 */
public class MyRealm extends AuthorizingRealm {

	protected final Map<String, SimpleAccount> users;

	//username-to-SimpleAccount
	protected final Map<String, SimpleRole> roles;

	//roleName-to-SimpleRole
	public MyRealm() {
		this.users = new LinkedHashMap<String, SimpleAccount>();
		this.roles = new LinkedHashMap<String, SimpleRole>();
		setCachingEnabled(false);
	}

	public MyRealm(String name) {
		this();
		setName(name);
	}

	protected SimpleAccount getUser(String username) {
		return this.users.get(username);
	}

	public boolean accountExists(String username) {
		return getUser(username) != null;
	}

	public void addAccount(String username, String password) {
		addAccount(username, password, (String[]) null);
	}

	public void addAccount(String username, String password, String... roles) {
		Set<String> roleNames = CollectionUtils.asSet(roles);
		SimpleAccount account = new SimpleAccount(username, password, getName(), roleNames, null);
		add(account);
	}

	protected String getUsername(SimpleAccount account) {
		return getUsername(account.getPrincipals());
	}

	protected String getUsername(PrincipalCollection principals) {
		return getAvailablePrincipal(principals).toString();
	}

	protected void add(SimpleAccount account) {
		String username = getUsername(account);
		this.users.put(username, account);
	}

	protected SimpleRole getRole(String rolename) {
		return roles.get(rolename);
	}

	public boolean roleExists(String name) {
		return getRole(name) != null;
	}

	public void addRole(String name) {
		add(new SimpleRole(name));
	}

	protected void add(SimpleRole role) {
		roles.put(role.getName(), role);
	}

	protected static Set<String> toSet(String delimited, String delimiter) {
		if (delimited == null || delimited.trim().equals("")) {
			return null;
		}
		Set<String> values = new HashSet<String>();
		String[] rolenamesArray = delimited.split(delimiter);
		for (String s : rolenamesArray) {
			String trimmed = s.trim();
			if (trimmed.length() > 0) {
				values.add(trimmed);
			}
		}
		return values;
	}

	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
		UsernamePasswordToken upToken = (UsernamePasswordToken) token;
		SimpleAccount account = getUser(upToken.getUsername());
		if (account != null) {
			if (account.isLocked()) {
				throw new LockedAccountException("Account [" + account + "] is locked.");
			}
			if (account.isCredentialsExpired()) {
				String msg = "The credentials for account [" + account + "] are expired";
				throw new ExpiredCredentialsException(msg);
			}
		}
		return account;
	}

	protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
		return this.users.get(getUsername(principals));
	}
}
