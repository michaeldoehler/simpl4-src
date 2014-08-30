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

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 *
 */
interface MetaData {

	public final String ROLE_PATH = "roles/{0}";

	public final String ROLES_PATH = "roles";

	public final String ROLE_TYPE = "sw.role";

	public final String ROLES_TYPE = "sw.role";

	public static String PERMISSIONS = "permissions";

	public List<Map> getRoles(String namespace) throws Exception;

	public void saveRole(String namespace, String name, Map<String, Object> permissions) throws Exception;

	public Map getRole(String namespace, String name) throws Exception;

	public void deleteRole(String namespace, String name) throws Exception;
}
