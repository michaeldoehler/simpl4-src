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
package org.ms123.common.system.jndi;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.resource.ResourceRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.naming.Binding;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;
import javax.naming.ServiceUnavailableException;
import java.util.Hashtable;
import bitronix.tm.BitronixTransactionManager;

/**
 * Implementation of {@link javax.naming.Context} that allows lookup of transaction manager
 * and registered resources.
 */
public class GlobalContext implements Context {

	private static final Logger m_logger = LoggerFactory.getLogger(GlobalContext.class);
	protected BitronixTransactionManager m_btm;
	private boolean closed = false;
	private final String userTransactionName;
	private final String synchronizationRegistryName;

	public GlobalContext() {
		userTransactionName = TransactionManagerServices.getConfiguration().getJndiUserTransactionName();
		debug("binding transaction manager at name '" + userTransactionName + "'");
		synchronizationRegistryName = TransactionManagerServices.getConfiguration().getJndiTransactionSynchronizationRegistryName();
		debug("binding synchronization registry at name '" + synchronizationRegistryName + "'");
	}

	private void checkClosed() throws ServiceUnavailableException {
		if (closed)
			throw new ServiceUnavailableException("context is closed");
	}

	@Override
	public void close() throws NamingException {
		closed = true;
	}

	@Override
	public Object lookup(Name name) throws NamingException {
		return lookup(name.toString());
	}

	@Override
	public Object lookup(String s) throws NamingException {
		debug("Context:lookup:" + s);
		checkClosed();
		Object o;
		if (userTransactionName.equals(s)) {
			if (m_btm == null) {
				m_btm = new BitronixTransactionManager();
			}
			o = m_btm;
			debug("context:" + this.hashCode() + "/" + o.hashCode());
		} else if (synchronizationRegistryName.equals(s)) {
			o = TransactionManagerServices.getTransactionSynchronizationRegistry();
		} else {
			o = ResourceRegistrar.get(s);
		}
		if (o == null) {
			throw new NameNotFoundException("unable to find a bound object at name '" + s + "'");
		}
		return o;
	}

	@Override
	public String toString() {
		return "a GlobalContext with userTransactionName='" + userTransactionName + "' and synchronizationRegistryName='" + synchronizationRegistryName + "'";
	}

	@Override
	public void bind(Name name, Object o) throws NamingException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void bind(String s, Object o) throws NamingException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void rebind(Name name, Object o) throws NamingException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void rebind(String s, Object o) throws NamingException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void unbind(Name name) throws NamingException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void unbind(String s) throws NamingException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void rename(Name name, Name name1) throws NamingException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void rename(String s, String s1) throws NamingException {
		throw new OperationNotSupportedException();
	}

	@Override
	public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
		throw new OperationNotSupportedException();
	}

	@Override
	public NamingEnumeration<NameClassPair> list(String s) throws NamingException {
		throw new OperationNotSupportedException();
	}

	@Override
	public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
		throw new OperationNotSupportedException();
	}

	@Override
	public NamingEnumeration<Binding> listBindings(String s) throws NamingException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void destroySubcontext(Name name) throws NamingException {
		throw new OperationNotSupportedException();
	}

	@Override
	public void destroySubcontext(String s) throws NamingException {
		throw new OperationNotSupportedException();
	}

	@Override
	public Context createSubcontext(Name name) throws NamingException {
		throw new OperationNotSupportedException();
	}

	@Override
	public Context createSubcontext(String s) throws NamingException {
		throw new OperationNotSupportedException();
	}

	@Override
	public Object lookupLink(Name name) throws NamingException {
		throw new OperationNotSupportedException();
	}

	@Override
	public Object lookupLink(String s) throws NamingException {
		throw new OperationNotSupportedException();
	}

	@Override
	public NameParser getNameParser(Name name) throws NamingException {
		return BitronixNameParser.INSTANCE;
	}

	@Override
	public NameParser getNameParser(String s) throws NamingException {
		return BitronixNameParser.INSTANCE;
	}

	@Override
	public Name composeName(Name name, Name name1) throws NamingException {
		throw new OperationNotSupportedException();
	}

	@Override
	public String composeName(String s, String s1) throws NamingException {
		throw new OperationNotSupportedException();
	}

	@Override
	public Object addToEnvironment(String s, Object o) throws NamingException {
		throw new OperationNotSupportedException();
	}

	@Override
	public Object removeFromEnvironment(String s) throws NamingException {
		throw new OperationNotSupportedException();
	}

	@Override
	public Hashtable<?, ?> getEnvironment() throws NamingException {
		throw new OperationNotSupportedException();
	}

	@Override
	public String getNameInNamespace() throws NamingException {
		throw new OperationNotSupportedException();
	}

	private static final class BitronixNameParser implements NameParser {

		private static final BitronixNameParser INSTANCE = new BitronixNameParser();

		@Override
		public Name parse(final String name) throws NamingException {
			return new CompositeName(name);
		}
	}

	protected static void debug(String msg) {
		System.out.println(msg);
		m_logger.debug(msg);
	}

	protected static void info(String msg) {
		System.out.println(msg);
		m_logger.info(msg);
	}
}
