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

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.admin.AdminProvider;
import org.jivesoftware.openfire.group.Group;
import org.jivesoftware.openfire.group.GroupManager;
import org.jivesoftware.openfire.group.GroupNotFoundException;
import org.jivesoftware.openfire.group.GroupProvider;
import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;

/**
 * Admin provider which will map a crowd group with openfire authorized admin users
 */
public class Simpl4AdminProvider implements AdminProvider {

	private static final Logger LOG = LoggerFactory.getLogger(Simpl4AdminProvider.class);

	private static final String JIVE_AUTHORIZED_GROUPS = "admin.authorizedGroups";

	public List<JID> getAdmins() {
		List<JID> results = new ArrayList<JID>();
		results.add(new JID("admin", XMPPServer.getInstance().getServerInfo().getXMPPDomain(), null, true));
		return results;
	}

	public void setAdmins(List<JID> admins) {
		return;
	}

	public boolean isReadOnly() {
		return true;
	}
}
