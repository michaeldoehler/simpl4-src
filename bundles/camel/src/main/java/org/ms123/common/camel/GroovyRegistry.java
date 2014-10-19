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
package org.ms123.common.camel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.io.File;
import org.ms123.common.libhelper.FileSystemClassLoader;
import org.apache.camel.NoSuchBeanException;
import org.apache.camel.spi.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class GroovyRegistry implements Registry {

	private ClassLoader m_fsClassLoader;

	private File[] m_locations;

	public GroovyRegistry(ClassLoader parent, String namespace) {
		String basedir = System.getProperty("workspace") + "/" + "groovy" + "/" + namespace;
		m_locations = new File[1];
		m_locations[0] = new File(basedir);
		m_fsClassLoader = new FileSystemClassLoader(parent, m_locations);
	}

	public Object lookupByName(String name) {
		debug("lookupByName1:" + name);
		try {
			Class clazz = m_fsClassLoader.loadClass(name);
			Object obj = clazz.newInstance();
			debug("lookupByName2:" + clazz + "/" + obj);
			return obj;
		} catch (Throwable e) {
			debug("lookupByNameError(" + name + "):" + e);
		}
		return null;
	}

	public <T> T lookupByNameAndType(String name, Class<T> type) {
		debug("lookupByNameAndType:" + name + "/" + type);
		Object answer = lookupByName(name);
		if (answer == null) {
			return null;
		}
		try {
			return type.cast(answer);
		} catch (Throwable e) {
			String msg = "Found bean: " + name + " in GroovyRegistry: " + this + " of type: " + answer.getClass().getName() + " expected type was: " + type;
			throw new NoSuchBeanException(name, msg, e);
		}
	}

	public <T> Map<String, T> findByTypeWithName(Class<T> type) {
		debug("findByTypeWithName:" + type);
		Map<String, T> result = new HashMap<String, T>();
		T answer = lookupByNameAndType(type.getName(), type);
		if (answer != null) {
			result.put(type.getName(), lookupByNameAndType(type.getName(), type));
		}
		return result;
	}

	public <T> Set<T> findByType(Class<T> type) {
		debug("findByType:" + type);
		Set<T> result = new HashSet<T>();
		T answer = lookupByNameAndType(type.getName(), type);
		if (answer != null) {
			result.add(answer);
		}
		return result;
	}

	public Object lookup(String name) {
		return lookupByName(name);
	}

	public <T> T lookup(String name, Class<T> type) {
		return lookupByNameAndType(name, type);
	}

	public <T> Map<String, T> lookupByType(Class<T> type) {
		return findByTypeWithName(type);
	}

	public void newClassLoader() {
		m_fsClassLoader = new FileSystemClassLoader(GroovyRegistry.class.getClassLoader(), m_locations);
	}
	private static void debug(String msg) {
		m_logger.debug(msg);
	}
	private static final Logger m_logger = LoggerFactory.getLogger(GroovyRegistry.class);
}
