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
package org.ms123.common.xmpp;

import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.metatype.*;
import java.io.*;
import java.util.*;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
//import org.ms123.common.rpc.PDefaultBool;
//import org.ms123.common.rpc.PDefaultFloat;
//import org.ms123.common.rpc.PDefaultInt;
//import org.ms123.common.rpc.PDefaultLong;
//import org.ms123.common.rpc.PDefaultString;
//import org.ms123.common.rpc.PName;
//import org.ms123.common.rpc.POptional;
//import org.ms123.common.rpc.RpcException;
import org.ms123.common.permission.api.PermissionException;
import org.ms123.common.permission.api.PermissionService;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import static org.apache.commons.io.FileUtils.readFileToString;
//import static org.ms123.common.rpc.JsonRpcServlet.ERROR_FROM_METHOD;
//import static org.ms123.common.rpc.JsonRpcServlet.INTERNAL_SERVER_ERROR;
//import static org.ms123.common.rpc.JsonRpcServlet.PERMISSION_DENIED;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.openfire.user.UserManager;
import org.jivesoftware.openfire.group.GroupManager;
import org.jivesoftware.openfire.auth.AuthFactory;
import org.jivesoftware.openfire.admin.AdminManager;

/** XmppService implementation
 */
@SuppressWarnings("unchecked")
@Component(enabled = true, configurationPolicy = ConfigurationPolicy.optional, immediate = true, properties = { "rpc.prefix=xmpp" })
public class XmppServiceImpl extends BaseXmppServiceImpl implements XmppService {

	private static final Logger m_logger = LoggerFactory.getLogger(XmppServiceImpl.class);

	public XmppServiceImpl() {
	}

	protected void activate(BundleContext bundleContext, Map<?, ?> props) {
		Simpl4Manager.setBundleContext(bundleContext);
		UserManager.setUserProvider(new Simpl4UserProvider());
		GroupManager.setGroupProvider(new Simpl4GroupProvider());
		AuthFactory.setAuthProvider(new Simpl4AuthProvider());
		AdminManager.setAdminProvider(new Simpl4AdminProvider());
		new XMPPServer();
		DbConnectionManager.setConnectionProvider(new Simpl4ConnectionProvider());
	}

	protected void deactivate() throws Exception {
	}
}
