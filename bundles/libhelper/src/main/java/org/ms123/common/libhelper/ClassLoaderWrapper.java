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


import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;


/**
 */
@SuppressWarnings("unchecked")
public class ClassLoaderWrapper extends ClassLoader {

	private ClassLoader[] parents;

	public ClassLoaderWrapper(ClassLoader... parents) {
		this.parents = parents;
	}

	protected synchronized Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
		Class cachedClass = findLoadedClass(name);

		if (cachedClass != null) {
			if (resolve) {
				resolveClass(cachedClass);
			}
			return cachedClass;
		}

		for (int i = 0; i < parents.length; i++) {
			ClassLoader parent = parents[i];

			try {
				Class clazz = parent.loadClass(name);
				if (resolve) {
					resolveClass(clazz);
				}
				return clazz;
			} catch (ClassNotFoundException ignored) {
			}
		}
		throw new ClassNotFoundException(name);
	}

	public URL getResource(String name) {
		for (int i = 0; i < parents.length; i++) {
			ClassLoader parent = parents[i];
			URL url = parent.getResource(name);
			if (url != null) {
				return url;
			}
		}
		return null;
	}

	public Enumeration findResources(String name) throws IOException {
		List resources = new ArrayList();

		for (int i = 0; i < parents.length; i++) {
			ClassLoader parent = parents[i];
			List parentResources = Collections.list(parent.getResources(name));
			resources.addAll(parentResources);
		}
		return Collections.enumeration(resources);
	}

}
