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
package org.ms123.common.entity.api;

import java.util.*;

public interface Constants {


	public final String RIGHT_ENTITY = "rightmodule";
	public final String LEFT_ENTITY = "leftmodule";
	public final String RIGHT_FIELD = "rightfield";
	public final String LEFT_FIELD = "leftfield";
	public final String RELATION = "relation";
	public final String DATAPACK_PREFIX1 = "app.";
	public final String DATAPACK_PREFIX2 = "data.";

	public final String STATE_FIELD = "_state";
	public final String STATE_OK = "ok";
	public final String STATE_DUP = "dup";
	public final String STATE_NEW = "new";
	public final String STATE_DEL = "del";
	public final String STATE_REFID = "_dup_refid";
	public final List<Map> m_defaultFields = new ArrayList<Map>() {

		{
			add(new HashMap<String, Object>() {

				{
					put("id", "_owner");
					put("name", "_owner");
					put("datatype", "string");
					put("readonly", true);
					put("formula_in", "if(_isnew){ _user } else{ \"_ignore_\"}");
				}
			});
			add(new HashMap<String, Object>() {

				{
					put("id", "_created_by");
					put("name", "_created_by");
					put("datatype", "string");
					put("readonly", true);
					put("formula_in", "if(_isnew){ _user } else{ \"_ignore_\"}");
				}
			});
			add(new HashMap<String, Object>() {

				{
					put("id", "_updated_by");
					put("name", "_updated_by");
					put("datatype", "string");
					put("readonly", true);
					put("formula_in", "_user");
				}
			});
			add(new HashMap<String, Object>() {

				{
					put("id", "_created_at");
					put("name", "_created_at");
					put("datatype", "date");
					put("readonly", true);
					put("formula_in", "if(_isnew){ new Date().getTime() } else {\"_ignore_\"}");
				}
			});
			add(new HashMap<String, Object>() {

				{
					put("id", "_updated_at");
					put("name", "_updated_at");
					put("datatype", "date");
					put("readonly", true);
					put("formula_in", "new Date().getTime()");
				}
			});
			add(new HashMap<String, String>() {

				{
					put("id", "_team_list");
					put("name", "_team_list");
					put("datatype", "array/team");
					put("edittype", "treemultiselect");
					put("selectable_items", "rpc:team:getTeamTree,namespace:\"${NAMESPACE}\",mapping:{value:\"teamid\",title:\"description\",name:\"name\",tooltip:\"(name+'/'+description)\"}");
					put("search_options", "['bw','eq','ne']");
				}
			});
		}
	};
	public final List<Map> m_stateFields = new ArrayList<Map>() {

		{
			add(new HashMap<String, Object>() {

				{
					put("id", "_state");
					put("name", "_state");
					put("datatype", "string");
					put("selectable_items", "["+
																			"{\"value\":null,\"label\":\"%composite.stateselect.new\"},"+
																			"{\"value\":\""+STATE_OK+"\",\"label\":\"%composite.stateselect.ok\"},"+
																			"{\"value\":\""+STATE_DUP+"\",\"label\":\"%composite.stateselect.dup\"}"+
																	"]");
					put("edittype", "select");
					put("readonly", false);
				}
			});
			add(new HashMap<String, Object>() {

				{
					put("id", "_dup_refid");
					put("name", "_dup_refid");
					put("datatype", "string");
					put("readonly", true);
				}
			});
		}
	};

	public final List<Map> m_teamFields = new ArrayList<Map>() {

		{
			add(new HashMap<String, String>() {

				{
					put("id", "_team_list");
					put("name", "_team_list");
					put("datatype", "array/team");
					put("edittype", "treemultiselect");
					put("selectable_items", "rpc:team:getTeamTree,namespace:\"${NAMESPACE}\",mapping:{value:\"teamid\",title:\"description\",name:\"name\",tooltip:\"(name+'/'+description)\"}");
					put("search_options", "['bw','eq','ne']");
				}
			});
		}
	};
}
