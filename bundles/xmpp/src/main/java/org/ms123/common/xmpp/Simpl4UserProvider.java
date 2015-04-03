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

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.jivesoftware.openfire.user.UserAlreadyExistsException;
import org.jivesoftware.openfire.user.UserProvider;
import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ms123.common.auth.api.AuthService;

/**
 */
public class Simpl4UserProvider implements UserProvider {

	private static final String SEARCH_FIELD_USERID = "userid";

	private static final String SEARCH_FIELD_GIVENNAME = "givenname";

	private static final String SEARCH_FIELD_NAME = "surname";

	private static final String SEARCH_FIELD_EMAIL = "email";

	private static final Set<String> SEARCH_FIELDS = new TreeSet<String>(Arrays.asList(new String[] { SEARCH_FIELD_USERID, SEARCH_FIELD_NAME, SEARCH_FIELD_EMAIL }));

	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	private AuthService m_authService;

	public Simpl4UserProvider() {
		Simpl4Manager sm = Simpl4Manager.getInstance();
		m_authService = sm.lookupServiceByClass(AuthService.class);
	}

	public User loadUser(String username) throws UserNotFoundException {
		try {
			Map<String, String> map = m_authService.getUser(username);
			if (map == null) {
				throw new UserNotFoundException();
			}
			return fillUser(map);
		} catch (Exception e) {
			e.printStackTrace();
			throw new UserNotFoundException();
		}
	}

	private String getDisplayName(Map<String, String> map) {
		String givenname = map.get(SEARCH_FIELD_GIVENNAME);
		String surname = map.get(SEARCH_FIELD_NAME);
		String userid = map.get(SEARCH_FIELD_USERID);
		String displayName = userid;
		if (surname != null && givenname == null) {
			displayName = surname;
		} else if (surname == null && givenname != null) {
			displayName = givenname;
		} else if (surname != null && givenname != null) {
			displayName = givenname + " " + surname;
		}
		return displayName;
	}

	private User fillUser(Map<String, String> map) {
		String email = map.get(SEARCH_FIELD_EMAIL);
		String userid = map.get(SEARCH_FIELD_USERID);
		String displayName = getDisplayName(map);
		return new User(userid, displayName, email, new Date(), new Date());
	}

	public int getUserCount() {
		List list = m_authService.getUserList(null);
		return list.size();
	}

	public Collection<User> getUsers() {
		lock.readLock().lock();
		try {
			List<Map> list = m_authService.getUserList(null);
			Collection<User> results = new ArrayList<User>();
			for (Map<String, String> m : list) {
				results.add(fillUser(m));
			}
			return results;
		} finally {
			lock.readLock().unlock();
		}
	}

	public Collection<String> getUsernames() {
		lock.readLock().lock();
		try {
			Collection<String> results = new ArrayList();
			List<Map> list = m_authService.getUserList(null, 0, 0);
			for (Map<String, String> m : list) {
				results.add(m.get(SEARCH_FIELD_USERID));
			}
			return results;
		} finally {
			lock.readLock().unlock();
		}
	}

	public Collection<User> getUsers(int startIndex, int numResults) {
		lock.readLock().lock();
		try {
			List<Map> list = m_authService.getUserList(null, startIndex, numResults);
			Collection<User> results = new ArrayList<User>();
			for (Map<String, String> m : list) {
				results.add(fillUser(m));
			}
			return results;
		} finally {
			lock.readLock().unlock();
		}
	}

	public Set<String> getSearchFields() throws UnsupportedOperationException {
		return SEARCH_FIELDS;
	}

	public Collection<User> findUsers(Set<String> fields, String query) throws UnsupportedOperationException {
		lock.readLock().lock();
		try {
			ArrayList<User> results = new ArrayList<User>();
			if (query != null && query.trim().length() > 0) {
				if (query.endsWith("*")) {
					query = query.substring(0, query.length() - 1);
				}
				if (query.startsWith("*")) {
					query = query.substring(1);
				}
				query = query.toLowerCase();
				List<Map> list = m_authService.getUserList(null);
				if (SEARCH_FIELDS.containsAll(fields)) {
					if (fields.contains(SEARCH_FIELD_USERID)) {
						for (Map<String, String> m : list) {
							if (m.get(SEARCH_FIELD_USERID).toLowerCase().contains(query)) {
								results.add(fillUser(m));
							}
						}
					} else if (fields.contains(SEARCH_FIELD_NAME)) {
						for (Map<String, String> m : list) {
							String displayName = getDisplayName(m);
							if (displayName.toLowerCase().contains(query)) {
								results.add(fillUser(m));
							}
						}
					} else {
						for (Map<String, String> m : list) {
							if (m.get(SEARCH_FIELD_EMAIL).toLowerCase().contains(query)) {
								results.add(fillUser(m));
							}
						}
					}
				}
			}
			return results;
		} finally {
			lock.readLock().unlock();
		}
	}

	public Collection<User> findUsers(Set<String> fields, String query, int startIndex, int numResults) throws UnsupportedOperationException {
		lock.readLock().lock();
		try {
			ArrayList<User> foundUsers = (ArrayList<User>) findUsers(fields, query);
			Collection<User> results = new ArrayList<User>(foundUsers.size());
			for (int i = 0, j = startIndex; i < numResults && j < foundUsers.size(); ++i, ++j) {
				results.add(foundUsers.get(j));
			}
			return results;
		} finally {
			lock.readLock().unlock();
		}
	}

	public boolean isReadOnly() {
		return true;
	}

	public boolean isNameRequired() {
		return false;
	}

	public boolean isEmailRequired() {
		return false;
	}

	protected void debug(String msg) {
		//System.out.println(msg);
		m_logger.debug(msg);
	}

	protected void info(String msg) {
		System.out.println(msg);
		m_logger.info(msg);
	}

	private static final Logger m_logger = LoggerFactory.getLogger(Simpl4UserProvider.class);

	/*
	 * Not implemented methods
	 */
	public User createUser(String username, String password, String name, String email) throws UserAlreadyExistsException {
		throw new UnsupportedOperationException("Create new user not implemented by this version of user provider");
	}

	public void deleteUser(String username) {
		throw new UnsupportedOperationException("Delete a user not implemented by this version of user provider");
	}

	public void setName(String username, String name) throws UserNotFoundException {
		throw new UnsupportedOperationException("Setting user name not implemented by this version of user provider");
	}

	public void setEmail(String username, String email) throws UserNotFoundException {
		throw new UnsupportedOperationException("Setting user email not implemented by this version of user provider");
	}

	public void setCreationDate(String username, Date creationDate) throws UserNotFoundException {
		throw new UnsupportedOperationException("Setting user creation date unsupported by this version of user provider");
	}

	public void setModificationDate(String username, Date modificationDate) throws UserNotFoundException {
		throw new UnsupportedOperationException("Setting user modification date unsupported by this version of user provider");
	}
}
