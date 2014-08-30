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
package org.ms123.common.namespace;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 *
 */
interface MetaData {

	public final String NAMESPACE_PATH = "namespaces/{0}";

	public final String BRANDING_PATH = "etc/branding";

	public final String NAMESPACES_PATH = "namespaces";

	public final String NAMESPACE_TYPE = "sw.namespace";

	public final String NAMESPACES_TYPE = "sw.namespaces";

	public List<Map> getNamespaces() throws Exception;

	public Map<String, List> getNamespace(String name) throws Exception;

	public void saveNamespace(String name, Map<String, List> desc) throws Exception;

	public void deleteNamespace(String name) throws Exception;
	public Map<String, String> getBranding() throws Exception;

	public void saveBranding(Map<String, String> desc) throws Exception;
}
