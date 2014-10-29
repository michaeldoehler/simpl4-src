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
package org.ms123.common.libhelper;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.apache.commons.io.FileUtils.readFileToByteArray;

/**
 */
public class FileSystemClassLoader extends ClassLoader {

	private File[] locations;
	private String[] includePattern;

	private boolean loadCustomClassFirst = true;

	private boolean isValid = true;

	private Map<String, Class<?>> loadedClasses = new ConcurrentHashMap<String, Class<?>>();

	public FileSystemClassLoader(File[] locations) {
		super(FileSystemClassLoader.getDefaultClassLoader());
		this.locations = locations;
	}

	public FileSystemClassLoader(ClassLoader parent, File[] locations) {
		this(parent,locations,null);
	}
	public FileSystemClassLoader(ClassLoader parent, File[] locations, String[] includePattern) {
		super(parent);
		this.locations = locations;
		this.includePattern = includePattern;
		for( File f : locations){
			debug("location:"+f.toString());
		}
	}

	protected Class<?> loadCustomClass(String className) {
		byte[] classData = null;
		try {
			classData = findClassBytes(className);
			if (classData != null) {
				Class<?> result = defineClass(className, classData, 0, classData.length, null);
				loadedClasses.put(className, result);
				debug(className + " loaded" );
				return result;
			}
			return null;
		} catch (IOException e) {
			error("IOException attempting to read class " + className + " from location(s) " + Arrays.toString(locations));
			return null;
		} catch (ClassFormatError e) {
			error("Invalid format for class " + className + " from location(s) " + Arrays.toString(locations));
			return null;
		}
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		return loadClass(name, false);
	}

	public synchronized Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
		//System.out.println("\tloadClass:"+className);
		Class<?> toReturn = null;
		if (!isValid) {
			//System.out.println("loadClass:isValid:"+this);
			return toReturn;
		}
		if (toReturn == null) {
			toReturn = getAlreadyLoadedClass(className);
		}
		if (loadCustomClassFirst) {
			if (toReturn == null) {
				toReturn = loadCustomClass(className);
			}
			if (toReturn == null) {
				toReturn = loadParentClass(className);
			}
		} else {
			if (toReturn == null) {
				toReturn = loadParentClass(className);
			}
			if (toReturn == null) {
				toReturn = loadCustomClass(className);
			}
		}
		if (toReturn == null) {
			debug("Class not found: " + className);
			throw new ClassNotFoundException(className);
		} else {
			if (resolve) {
				resolveClass(toReturn);
			}
		}
		return toReturn;
	}

	public void setInvalid() {
		isValid = false;
	}

	protected byte[] findClassBytes(String className) throws IOException {
		byte[] classData = null;
		String relativeClassFile = className.replace('.', '/') + ".class";
		boolean include = includePattern==null;
	
		if( !include){
			for (int i = 0; i < includePattern.length; i++) {
				if( className.matches(includePattern[i])){
					include=true;
					break;
				}
			}
		}
		debug("findClassBytes:"+className+"="+include+"/"+(includePattern != null ? includePattern[0] : ""));
		if(!include){
			 return null;
		}
		for (int i = 0; i < locations.length; i++) {
			File file = new File(locations[i], relativeClassFile);
			debug("\tfindClassBytes.file:"+file+"/"+file.exists());
			if (file.exists()) {
				classData = readFileToByteArray(file);
				break;
			}
		}
		if (classData != null) {
		} else {
		}
		return classData;
	}

	/**
	 * Returns a cached <code>Class</code> instance, if it has already been
	 * loaded using the <code>ClassLoader</code>. Designed to be used by
	 * subclasses.
	 * @param className the name of the class to return
	 * @return returns the cached <code>Class</code> instance, if previously
	 * loaded using this <code>ClassLoader</code>, otherwise null.
	 */
	protected Class<?> getAlreadyLoadedClass(String className) {
		Class<?> loadedClass = loadedClasses.get(className);
		if (loadedClass != null) {
			debug("Returning already loaded custom class: " + className);
			return loadedClass;
		}
		return loadedClass;
	}

	/**
	 * Attempts to load the designated class by delegating to the parent class
	 * loader.
	 * @param className the name of the class to load
	 * @return a <code>Class</code> instance successfully loaded, otherwise
	 * null
	 */
	protected Class<?> loadParentClass(String className) {
		try {
			Class<?> parentClass = getParent().loadClass(className);
			debug("Returning from parent class loader {}: {}" + getParent() + ": " + parentClass);
			return parentClass;
		} catch (Exception e) {
		}
		return null;
	}

	/**
	 * Attempt to load a resoure, first by calling
	 * <code>getCustomResource</code>. If the resource is not found
	 * <code>super.getResource(name)</code> is called.
	 */
	@Override
	public URL getResource(String name) {
		final URL url = getCustomResource(name);
		if (url != null) {
			return url;
		}
		return super.getResource(name);
	}

	/**
	 * Attempts to find a resource from one of the file system locations
	 * specified in a constructor.
	 * @param name the name of the resource to load
	 * @return a <code>URL</code> instance, if the resource can be found,
	 * otherwise null.
	 */
	protected URL getCustomResource(String name) {
		try {
			for (int i = 0; i < locations.length; i++) {
				File file = new File(locations[i], name);
				if (file.exists()) {
					URL url = file.toURI().toURL();
					return url;
				}
			}
		} catch (IOException e) {
			error("IOException attempting to load resource " + name + " from location(s) " + Arrays.toString(locations));
		}
		return null;
	}

	/**
	 * Returns an immutable map of name to <code>Class</code> instances loaded
	 * via this class loader
	 * @return
	 */
	public Map<String, Class<?>> getLoadedClasses() {
		return Collections.unmodifiableMap(loadedClasses);
	}

	protected File[] getLocations() {
		return locations;
	}

	/*private void debug(String message) {
		logger.debug(this.getClass().getSimpleName() + "[" + System.identityHashCode(this) + "]: " + message);
	}*/

	public static ClassLoader getDefaultClassLoader() {
		ClassLoader cl = null;
		try {
			cl = Thread.currentThread().getContextClassLoader();
		} catch (Throwable ex) {
		}
		if (cl == null) {
			// No thread context class loader -> use class loader of this class.
			cl = FileSystemClassLoader.class.getClassLoader();
		}
		return cl;
	}
	protected void debug(String message) {
		m_logger.debug(message);
	}
	protected void error(String message) {
		m_logger.error(message);
		System.out.println(message);
	}
	protected void info(String message) {
		m_logger.info(message);
		System.out.println(message);
	}
	private static final Logger m_logger = LoggerFactory.getLogger(FileSystemClassLoader.class);
}
