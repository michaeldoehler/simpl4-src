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
package org.ms123.common.rpc;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * Util class to handle annotation {@link PName}.
 * 
 * @author Karel Hovorka
 * 
 */
public enum ParameterNameUtil {

	INSTANCE;

	private ParameterNameUtil() {
	}

	public static boolean isMethodAnotatedByPName(Method method) {
		if (method.getParameterTypes().length == 0) {
			return false;
		}
		for (Annotation a : method.getParameterAnnotations()[0]) {
			if (a instanceof PName) {
				return true;
			}
		}
		return false;
	}

	public static PName getPName(Method method, int index) {
		if (method.getParameterTypes().length == 0) {
			return null;
		}
		for (Annotation a : method.getParameterAnnotations()[index]) {
			if (a instanceof PName) {
				return (PName) a;
			}
		}
		return null;
	}

	public static boolean isOptional(Method method, int index) {
		if (method.getParameterTypes().length == 0) {
			return false;
		}
		for (Annotation a : method.getParameterAnnotations()[index]) {
			if (a instanceof POptional) {
				return true;
			}
		}
		return false;
	}

	public static Integer getDefaultInt(Method method, int index) {
		if (method.getParameterTypes().length == 0) {
			return null;
		}
		for (Annotation a : method.getParameterAnnotations()[index]) {
			if (a instanceof PDefaultInt) {
				return ((PDefaultInt) a).value();
			}
		}
		return null;
	}

	public static Long getDefaultLong(Method method, int index) {
		if (method.getParameterTypes().length == 0) {
			return null;
		}
		for (Annotation a : method.getParameterAnnotations()[index]) {
			if (a instanceof PDefaultLong) {
				return ((PDefaultLong) a).value();
			}
		}
		return null;
	}

	public static String getDefaultString(Method method, int index) {
		if (method.getParameterTypes().length == 0) {
			return null;
		}
		for (Annotation a : method.getParameterAnnotations()[index]) {
			if (a instanceof PDefaultString) {
				return ((PDefaultString) a).value();
			}
		}
		return null;
	}

	public static Boolean getDefaultBool(Method method, int index) {
		if (method.getParameterTypes().length == 0) {
			return null;
		}
		for (Annotation a : method.getParameterAnnotations()[index]) {
			if (a instanceof PDefaultBool) {
				return ((PDefaultBool) a).value();
			}
		}
		return null;
	}

	public static Double getDefaultFloat(Method method, int index) {
		if (method.getParameterTypes().length == 0) {
			return null;
		}
		for (Annotation a : method.getParameterAnnotations()[index]) {
			if (a instanceof PDefaultFloat) {
				return ((PDefaultFloat) a).value();
			}
		}
		return null;
	}
}
