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
		GroupProvider provider = GroupManager.getInstance().getProvider();
		String groups = JiveGlobals.getProperty(JIVE_AUTHORIZED_GROUPS);
		groups = (groups == null || groups.trim().length() == 0) ? "" : groups;
		JiveGlobals.setProperty(JIVE_AUTHORIZED_GROUPS, groups);
		// make sure the property is created
		StringTokenizer tokenizer = new StringTokenizer(groups, ",");
		while (tokenizer.hasMoreTokens()) {
			String groupName = tokenizer.nextToken().trim().toLowerCase();
			if (groupName != null && groupName.length() > 0) {
				try {
					LOG.info("Adding admin users from group: " + groupName);
					Group group = provider.getGroup(groupName);
					if (group != null) {
						results.addAll(group.getMembers());
					}
				} catch (GroupNotFoundException gnfe) {
					LOG.error("Error when trying to load the members of group:" + String.valueOf(groupName), gnfe);
				}
			}
		}
		if (results.isEmpty()) {
			// Add default admin account when none was specified
			results.add(new JID("admin", XMPPServer.getInstance().getServerInfo().getXMPPDomain(), null, true));
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug("admin users: " + results.toString());
		}
		return results;
	}

	public void setAdmins(List<JID> admins) {
		return;
	}

	public boolean isReadOnly() {
		return true;
	}
}
