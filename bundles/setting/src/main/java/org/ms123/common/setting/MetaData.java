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
package org.ms123.common.setting;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import org.ms123.common.store.StoreDesc;

/**
 *
 */
interface  MetaData {

	public final String SETTING_PATH = "settings/{0}";
	public final String SETTINGS_PATH = "settings";
	public final String SETTING_TYPE = "sw.setting";
	public final String SETTINGS_TYPE = "sw.settings";

	public static String SETTINGS = "settings";

	public void setResourceSetting(String namespace, String settingsid, String resourceid, Map settings,boolean overwrite) throws Exception;
	public Map getResourceSetting(String namespace, String settingsid, String resourceid) throws Exception;
	public void deleteResourceSetting(String namespace, String settingsid, String resourceid) throws Exception;

	public Map getFieldSets(String settingsid, String namespace, String entityName) throws Exception;

	public List<Map> getFieldsetsForEntity(String namespace, String settingsid, String entity) throws Exception;
	public List<Map> getFieldsForEntityView(String namespace, String settingsid, String entity, String view) throws Exception;
	public List<Map> getFieldsForEntityView(String namespace, String settingsid, String entity, String view, Map mapping, String filter, String sortField) throws Exception;

	public Map getPropertiesForEntity(String namespace, String settingsid, String entity) throws Exception;
	public Map getPropertiesForEntityView(String namespace, String settingsid, String entity, String view) throws Exception;

}
