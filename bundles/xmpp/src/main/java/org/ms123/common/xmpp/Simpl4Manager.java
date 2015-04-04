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

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;
import org.apache.commons.lang.StringUtils;
import org.osgi.framework.BundleContext;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.openfire.group.Group;
import org.osgi.framework.ServiceReference;

public class Simpl4Manager {

	private static final Logger LOG = LoggerFactory.getLogger(Simpl4Manager.class);

	private static final Object O = new Object();

	private static Simpl4Manager INSTANCE;

	private static BundleContext m_bundleContext;

	public static Simpl4Manager getInstance() {
		if (INSTANCE == null) {
			synchronized (O) {
				if (INSTANCE == null) {
					Simpl4Manager manager = new Simpl4Manager();
					if (manager != null)
						INSTANCE = manager;
				}
			}
		}
		return INSTANCE;
	}

	public static void setBundleContext(BundleContext bc) {
		m_bundleContext = bc;
	}

	private Simpl4Manager() {
	}

	public <T> T lookupServiceByClass(Class<T> clazz) {
		T service = null;
		ServiceReference sr = m_bundleContext.getServiceReference(clazz);
		if (sr != null) {
			service = (T) m_bundleContext.getService(sr);
		}
		if (service == null) {
			throw new RuntimeException("Simpl4Manager.Cannot resolve service:" + clazz);
		}
		return service;
	}

	/**
	 * Get all the crowd groups
	 * @return a List of group names
	 * @throws RemoteException
	 */
	public List<String> getAllGroupNames() throws Exception {
		if (LOG.isDebugEnabled())
			LOG.debug("fetch all crowd groups");
		int maxResults = 100;
		int startIndex = 0;
		List<String> groups = new ArrayList<String>();
		return groups;
	}

	/**
	 * Get all the groups of a given username
	 * @param username
	 * @return a List of groups name
	 * @throws RemoteException
	 */
	public List<String> getUserGroups(String username) throws Exception {
		username = JID.unescapeNode(username);
		if (LOG.isDebugEnabled())
			LOG.debug("fetch all crowd groups for user:" + username);
		int maxResults = 100;
		int startIndex = 0;
		List<String> userGroups = new ArrayList<String>();
		return userGroups;
	}

	/**
	 * Get the description of a group from crowd
	 * @param groupName
	 * @return a Group object
	 * @throws RemoteException
	 */
	public Group getGroup(String groupName) throws Exception {
		if (LOG.isDebugEnabled())
			LOG.debug("Get group:" + groupName + " from crowd");
		Group group = null;
		return group;
	}

	/**
	 * Get the members of the given group
	 * @param groupName
	 * @return a List of String with the usernames members of the given group
	 * @throws RemoteException
	 */
	public List<String> getGroupMembers(String groupName) throws Exception {
		if (LOG.isDebugEnabled())
			LOG.debug("Get all members for group:" + groupName);
		int maxResults = 100;
		int startIndex = 0;
		List<String> groupMembers = new ArrayList<String>();
		return groupMembers;
	}
}
