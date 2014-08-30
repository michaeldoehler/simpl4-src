/*
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
/**
	* @ignore($)
*/
qx.Class.define("ms123.graphicaleditor.StencilManager", {
	extend: qx.core.Object,
	include: qx.locale.MTranslation,


	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function () {
		var m = qx.locale.Manager.getInstance();
		var locale = m.getLocale();

		this.__lang = locale;
		this.__groups = {};
		this.__groupNameList = [];
		this.__stencilsMap = {};
		this.__stencilList = [];
	},


	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},


	/******************************************************************************
	 STATICS
	 ******************************************************************************/
	statics: {
		__stencilsetcache: {}
	},


	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		loadStencilSet: function (name) {
			var cm = new ms123.config.ConfigManager();
			var stencilset = cm.getStencilSet(name);
			var stencils = stencilset["stencils"];
			console.log("stencils:" + stencils.length + "," + this.__lang);
			for (var i = 1; i < stencils.length; i++) {
				var stencilMap = stencils[i];
				var stencil = new ms123.graphicaleditor.Stencil();
				stencil.setType(stencilMap.type);
				stencil.setView(stencilMap.view);
				var desc = this.__getLangString(stencilMap, "description");
				stencil.setDescription(desc);
				var title = this.__getLangString(stencilMap, "title");
				stencil.setTitle(title);
				stencil.setId(stencilMap.id);
				stencil.setIcon(this.__getResourceUrl(name,stencilMap.icon));
				var groups = this.__getLangGroups(stencilMap);
				for (var j = 0; groups && j < groups.length; j++) {
					var groupName = groups[j];
					var groupList = this.__groups[groupName];
					if (groupList == undefined) {
						groupList = [];
						this.__groups[groupName] = groupList;
					}
					groupList.push(stencil);
					if (!this.__contains(this.__groupNameList, groupName)) {
						this.__groupNameList.push(groupName);
					}
				}
				this.__stencilList.push(stencil);
				this.__stencilsMap[stencilMap.id] = stencil;
			}
			return this.__stencilList;
		},

		getGroupListByName: function (name) {
			return this.__groups[name];
		},
		getGroupNameList: function () {
			return this.__groupNameList;
		},
		getStencilById: function (id) {
			return this.__stencilsMap[id];
		},

		__contains:function(a, obj) {
			for (var i = 0; i < a.length; i++) {
				if (a[i] === obj) {
					return true;
				}
			}
			return false;
		},

		__getResourceUrl: function (name, i) {
			var am = qx.util.AliasManager.getInstance(name);
			return am.resolve("resource/ms123/stencilsets/"+name+"/" + i );
		},

		__getLangString: function (map, key) {
			var val = map[key + "_" + this.__lang];
			if (val && val.length > 0) return val;
			return map[key];
		},

		__getLangGroups: function (map) {
			var key = "groups";
			var val = map[key + "_" + this.__lang];
			if (val && val.length > 0) return val;
			return map[key];
		}
	}
});
