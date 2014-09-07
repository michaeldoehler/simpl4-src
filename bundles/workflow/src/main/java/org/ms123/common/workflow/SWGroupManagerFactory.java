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

import org.activiti.engine.impl.interceptor.Session;
import org.activiti.engine.impl.interceptor.SessionFactory;
import org.activiti.engine.impl.persistence.entity.GroupEntityManager;
import org.activiti.engine.impl.persistence.entity.GroupIdentityManager;
import org.ms123.common.permission.api.PermissionService;
import org.ms123.common.auth.api.AuthService;
import flexjson.*;

public class SWGroupManagerFactory implements SessionFactory {

	protected PermissionService m_permissionService;

	protected AuthService m_authService;

	public SWGroupManagerFactory(AuthService as, PermissionService ps) {
		m_authService = as;
		m_permissionService = ps;
	}

	@Override
	public Class<?> getSessionType() {
    return GroupIdentityManager.class;
	}

	@Override
	public Session openSession() {
		return new SWGroupEntityManager(m_authService, m_permissionService);
	}
}
