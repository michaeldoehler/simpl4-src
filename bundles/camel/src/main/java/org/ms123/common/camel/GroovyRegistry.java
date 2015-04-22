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
import org.osgi.framework.BundleContext;
import org.apache.camel.NoSuchBeanException;
import org.osgi.framework.ServiceReference;
import org.apache.camel.spi.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class GroovyRegistry implements Registry {

	private ClassLoader m_fsClassLoader;

	private BundleContext m_bundleContect;
	private File[] m_locations;

	public GroovyRegistry(ClassLoader parent, BundleContext bc, String namespace) {
		String basedir = System.getProperty("workspace") + "/" + "groovy" + "/" + namespace;
		m_locations = new File[1];
		m_locations[0] = new File(basedir);
		m_fsClassLoader = new FileSystemClassLoader(parent, m_locations);
		m_bundleContect=bc;
	}


	private Object _lookupByNameType (String name,Class type) {
		debug("_lookupByNameAndType1:" + name+"/"+type);
		Object obj = getObjectFromFileSystemClassloader(name);
		if( obj == null || !canCast(obj,type)){
			obj = getDataSource(name);
		}
		debug("_lookupByNameAndType2:" + obj);
		return obj;
	}
	private boolean canCast(Object obj, Class type){
		if( type == null) return true;
		try {
			return type.cast(obj) != null;
		} catch (Throwable e) {
			return false;
		}
	}

	private Object getDataSource(String name){
		try {
			ServiceReference[] sr = m_bundleContect.getServiceReferences((String)null, "(&(objectClass=javax.sql.DataSource)(dataSourceName="+name+"))");
			if( sr != null && sr.length>0){
					debug("ds:"+sr[0]);
				return m_bundleContect.getService(sr[0]);
			}else{
				error("getDataSource1(" + name + "):not found");
			}
		} catch (Throwable e) {
			error("getDataSource2(" + name + "):" + e);
		}
		return null;
	}

	private Object getObjectFromFileSystemClassloader(String name){
		try {
			Class clazz = m_fsClassLoader.loadClass(name);
			Object obj = clazz.newInstance();
			debug("getObjectFromFileSystemClassloader:" + clazz + "/" + obj);
			return obj;
		} catch (Throwable e) {
			debug("getObjectFromFileSystemClassloader(" + name + "):" + e);
		}
		return null;
	}

	public Object lookupByName(String name) {
		return _lookupByNameType(name,null);
	}
	public <T> T lookupByNameAndType(String name, Class<T> type) {
		debug("lookupByNameAndType:" + name + "/" + type);
		Object answer = _lookupByNameType(name,type);
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
	private static void error(String msg) {
		System.out.println(msg);
		m_logger.error(msg);
	}
	private static void debug(String msg) {
		m_logger.debug(msg);
	}
	private static final Logger m_logger = LoggerFactory.getLogger(GroovyRegistry.class);
}
