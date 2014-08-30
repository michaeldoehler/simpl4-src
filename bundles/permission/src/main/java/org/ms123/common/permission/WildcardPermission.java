/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.ms123.common.permission;

import org.apache.shiro.authz.Permission;
import org.apache.shiro.util.CollectionUtils;
import org.apache.shiro.util.StringUtils;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WildcardPermission implements Permission, Serializable {

	/* --------------------------------------------
	 |             C O N S T A N T S             |
	 ============================================*/
	protected static final String WILDCARD_TOKEN = "*";

	protected static final String PART_DIVIDER_TOKEN = ":";

	protected static final String SUBPART_DIVIDER_TOKEN = ",";

	protected static final boolean DEFAULT_CASE_SENSITIVE = false;

	/* --------------------------------------------
	 |    I N S T A N C E   V A R I A B L E S    |
	 ============================================*/
	private List<Set<String>> parts;

	private Set<String> actions;

	/* --------------------------------------------
	 |         C O N S T R U C T O R S           |
	 ============================================*/
	/**
	 * Default no-arg constructor for subclasses only - end-user developers instantiating Permission instances must
	 * provide a wildcard string at a minimum, since Permission instances are immutable once instantiated.
	 * <p/>
	 * Note that the WildcardPermission class is very robust and typically subclasses are not necessary unless you
	 * wish to create type-safe Permission objects that would be used in your application, such as perhaps a
	 * {@code UserPermission}, {@code SystemPermission}, {@code PrinterPermission}, etc.  If you want such type-safe
	 * permission usage, consider subclassing the {@link DomainPermission DomainPermission} class for your needs.
	 */
	protected WildcardPermission() {
	}

	public WildcardPermission(String wildcardString) {
		int colon = wildcardString.lastIndexOf(PART_DIVIDER_TOKEN);
		if (colon == -1) {
			throw new IllegalArgumentException("Wildcard without dividers. Make sure permission strings are properly formatted.");
		}
		this.actions = splitToSet(wildcardString.substring(colon + 1), SUBPART_DIVIDER_TOKEN);
		setParts(wildcardString.substring(0, colon), DEFAULT_CASE_SENSITIVE);
	}

	public WildcardPermission(String wildcardString, String actions) {
		this(wildcardString, actions, DEFAULT_CASE_SENSITIVE);
	}

	public WildcardPermission(String wildcardString, String actions, boolean caseSensitive) {
		this.actions = splitToSet(actions, SUBPART_DIVIDER_TOKEN);
		setParts(wildcardString, caseSensitive);
	}

	protected void setParts(String wildcardString) {
		setParts(wildcardString, DEFAULT_CASE_SENSITIVE);
	}

	protected void setParts(String wildcardString, boolean caseSensitive) {
		if (wildcardString == null || wildcardString.trim().length() == 0) {
			throw new IllegalArgumentException("Wildcard string cannot be null or empty. Make sure permission strings are properly formatted.");
		}
		wildcardString = wildcardString.trim();
		List<String> parts = splitToList(wildcardString, PART_DIVIDER_TOKEN);
		this.parts = new ArrayList<Set<String>>();
		for (String part : parts) {
			// Set<String> subparts = CollectionUtils.asSet(part.split(SUBPART_DIVIDER_TOKEN));
			Set<String> subparts = splitToSet(part, SUBPART_DIVIDER_TOKEN);
			if (!caseSensitive) {
				subparts = lowercase(subparts);
			}
			if (subparts.isEmpty()) {
				throw new IllegalArgumentException("Wildcard string cannot contain parts with only dividers. Make sure permission strings are properly formatted.");
			}
			this.parts.add(subparts);
		}
		if (this.parts.isEmpty()) {
			throw new IllegalArgumentException("Wildcard string cannot contain only dividers. Make sure permission strings are properly formatted.");
		}
	}

	private static Set<String> splitToSet(String s, String delim) {
		Set<String> ret = new HashSet<String>();
		int idx = 0;
		int next = 0;
		while ((next = s.indexOf(delim, idx)) > -1) {
			ret.add(s.substring(idx, next));
			idx = next + 1;
		}
		if (idx < s.length()) {
			ret.add(s.substring(idx));
		}
		return ret;
	}

	private static List<String> splitToList(String s, String delim) {
		List<String> ret = new ArrayList<String>();
		int idx = 0;
		int next = 0;
		while ((next = s.indexOf(delim, idx)) > -1) {
			ret.add(s.substring(idx, next));
			idx = next + 1;
		}
		if (idx < s.length()) {
			ret.add(s.substring(idx));
		}
		return ret;
	}

	private Set<String> lowercase(Set<String> subparts) {
		Set<String> lowerCasedSubparts = new HashSet<String>(subparts.size());
		for (String subpart : subparts) {
			lowerCasedSubparts.add(subpart.toLowerCase());
		}
		return lowerCasedSubparts;
	}

	/* --------------------------------------------
	 |  A C C E S S O R S / M O D I F I E R S    |
	 ============================================*/
	public List<Set<String>> getParts() {
		return this.parts;
	}

	public Set<String> getActions() {
		return this.actions;
	}

	/* --------------------------------------------
	 |               M E T H O D S               |
	 ============================================*/
	public boolean implies(Permission p) {
		if (!(p instanceof WildcardPermission)) {
			return false;
		}
		WildcardPermission wp = (WildcardPermission) p;
		List<Set<String>> otherParts = wp.getParts();
		List<Set<String>> thisParts = getParts();
		int thisPartsSize = thisParts.size();
		int i = 0;
		for (Set<String> otherPart : otherParts) {
			if (thisPartsSize - 1 < i) {
				return this.getActions().containsAll(wp.getActions());
			} else {
				Set<String> thisPart = thisParts.get(i);
				if (!thisPart.contains(WILDCARD_TOKEN) && !thisPart.containsAll(otherPart)) {
					return false;
				}
				i++;
			}
		}
		// If this permission has more parts than the other parts, only imply it if all of the other parts are wildcards
		/*for (; i < thisPartsSize; i++) {
			Set<String> thisPart = thisParts.get(i);
			if (!thisPart.contains(WILDCARD_TOKEN)) {
				return false;
			}
		}*/
		return this.getActions().containsAll(wp.getActions());
	}

	public String toString() {
		StringBuilder buffer = new StringBuilder();
		for (Set<String> part : parts) {
			if (buffer.length() > 0) {
				buffer.append(":");
			}
			buffer.append(part);
		}
		return buffer.toString() + "/" + this.actions;
	}

	public boolean equals(Object o) {
		if (o instanceof WildcardPermission) {
			WildcardPermission wp = (WildcardPermission) o;
			return parts.equals(wp.parts);
		}
		return false;
	}

	public int hashCode() {
		return parts.hashCode();
	}
}
