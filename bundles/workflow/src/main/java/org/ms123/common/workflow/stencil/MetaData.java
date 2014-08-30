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
package org.ms123.common.workflow.stencil;

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

	public static final String STENCIL_PATH = "stencils/{0}";

	public static final String STENCILS_PATH = "stencils";

	public static final String STENCIL_TYPE = "sw.stencil";

	public static final String STENCILS_TYPE = "sw.stencils";

	public static final String PROCESS_SS = "process";

	public List<Map> getAddonStencils(String namespace) throws Exception;
	public String getStencilView(String ssname, String viewpath) throws Exception;
	public String getStencilIcon(String ssname, String iconpath) throws Exception;

	public Map<String, Object> getAddonStencil(String namespace, String name) throws Exception;

	public void saveAddonStencil(String namespace, String name, Map<String, Object> desc) throws Exception;

	public void deleteAddonStencil(String namespace, String name) throws Exception;
}
