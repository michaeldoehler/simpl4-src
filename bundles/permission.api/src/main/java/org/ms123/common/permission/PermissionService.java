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
package org.ms123.common.permission.api;

import java.util.Map;
import java.util.List;
import org.ms123.common.store.StoreDesc;
import org.ms123.common.rpc.RpcException;
import org.apache.shiro.authz.Permission;

public interface PermissionService {
	public final String PERMISSION_SERVICE = "permissionService";

	public boolean login(String appName, String username, String password);

	public boolean loginInternal(String appName);
	public boolean loginInternal(String appName, String username,String password);

	public boolean isPermitted(String permission);

	public boolean isPermitted(Permission wp);

	public boolean hasRole(String role);

	public boolean hasAdminRole();

	public Map getRole( String namespace, String name) throws RpcException;

	public List<String> getUserRoles( String name) throws Exception;

	public boolean hasEntityPermissions(StoreDesc sdesc, String entity, String actions);

	public Map<String, Object> permissionFieldMapFilter(StoreDesc sdesc, String entity, Map<String, Object> fieldMap, String actions);

	public List<Map> permissionFieldListFilter(StoreDesc sdesc, String entity, List<Map> fieldList, String fieldKey, String actions);
	public List<String> permissionFieldListFilter(StoreDesc sdesc, String entity, List<String> fieldList, String actions);
}
