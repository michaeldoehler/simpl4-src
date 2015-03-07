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
	@ignore($)
	@asset(qx/icon/${qx.icontheme}/16/actions/*)
	@asset(qx/icon/${qx.icontheme}/16/places/*)
*/




qx.Class.define("ms123.settings.views.SelectedItems", {
	extend: ms123.util.TableEdit,

	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (facade) {
		this._model = facade.model;
		this.base(arguments, facade);
	},

	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		_createColumnModel: function () {
			var view = this._facade.view;
			if (view == "main-form") {
				var tabList = [];
				for (var i = 1; i < 5; i++) {
					var o = {
						label: "tab" + i,
						value: "tab" + i
					};
					tabList.push(o);
				}
				var columnmodel = [{
					name: "name",
					width: 40,
					header: "%settings.name"
				},
				{
					name: "displayname",
					width: 40,
					header: "%settings.displayname"
				},
				{
					name: "tab",
					type: "SelectBox",
					width: 15,
					options: tabList,
					header: "%settings.tab"
				},
				{
					name: "fillin",
					type: "ComboBox",
					options: [{value:"",label:""}, {value:"entity",label:"entity"},
										{value:"entity:expr",label:"entity:expr"},
										{value:"entity,entity",label:"entity,entity"},
										{value:"entity:expr,entity:expr",label:"entity:expr,entity:expr"}
									],
					width: 40,
					header: "%settings.fillin"
				},
				{
					name: "readonly",
					type: "CheckBox",
					width: 15,
					header: "%settings.readonly"
				},
				{
					name: "enabled",
					type: "CheckBox",
					width: 15,
					header: "%settings.enabled"
				}];
				this._columnModel = columnmodel;
				return this._translate(columnmodel);
			}else if (view == "main-grid") {
				var columnmodel = [{
					name: "name",
					header: "%settings.name"
				},
				{
					name: "displayname",
					header: "%settings.displayname"
				},
				{
					name: "hide",
					type: "SelectBox",
					width: 30,
					options: [{value:null, label:"---"},{value:"phone",label:"Phone"},{value:"phone,tablet",label:"Phone,Tablet"}],
					header: "%settings.hide"
				},
				{
					name: "tags",
					type: "TextField",
					width: 30,
					header: "Tags",
				  filter:/[a-zA-Z0-9_ ]/
				},
				{
					name: "enabled",
					type: "CheckBox",
					width: 15,
					header: "%settings.enabled"
				}];
				this._columnModel = columnmodel;
				return this._translate(columnmodel);
			}else if (view == "search") {
				var columnmodel = [{
					name: "name",
					header: "%settings.name"
				},
				{
					name: "displayname",
					header: "%settings.displayname"
				},
				{
          name: "search_options",
					header: "%settings.search_options",
          type: "DoubleSelectBox",
          options: [{"value":"eq","label":"%meta.lists.eq"},{"value":"ne","label":"%meta.lists.ne"},{"value":"lt","label":"%meta.lists.lt"},{"value":"le","label":"%meta.lists.le"},{"value":"gt","label":"%meta.lists.gt"},{"value":"ge","label":"%meta.lists.ge"},{"value":"bw","label":"%meta.lists.bw"},{"value":"bn","label":"%meta.lists.bn"},{"value":"in","label":"%meta.lists.empty"},{"value":"ni","label":"%meta.lists.not_empty"},{"value":"cn","label":"%meta.lists.cn"},{"value":"nc","label":"%meta.lists.nc"} ]
        },
				{
					name: "enabled",
					type: "CheckBox",
					width: 40,
					header: "%settings.enabled"
				}];
				this._columnModel = columnmodel;
				return this._translate(columnmodel);
			}else if (view == "duplicate-check") {
				var columnmodel = [{
					name: "name",
					header: "%settings.name"
				},
				{
					name: "displayname",
					header: "%settings.displayname"
				},
				{
					name: "check_type",
					type: "SelectBox",
					width: 20,
					options: [{value:"fuzzy", label:"fuzzy"},{value:"equal",label:"Equal"}],
					header: "%settings.check_type"
				},
				{
					name: "threshold",
					type: "DecimalField",
					value:0.85,
					width: 20,
					header: "%settings.threshold"
				},
				{
					name: "innerThreshold",
					type: "DecimalField",
					value:0.85,
					width: 20,
					header: "%settings.innerThreshold"
				},
				{
					name: "enabled",
					type: "CheckBox",
					width: 20,
					header: "%settings.enabled"
				}];
				this._columnModel = columnmodel;
				return this._translate(columnmodel);
			} else {
				var columnmodel = [{
					name: "name",
					header: "%settings.name"
				},
				{
					name: "displayname",
					header: "%settings.displayname"
				},
				{
					name: "enabled",
					type: "CheckBox",
					width: 60,
					header: "%settings.enabled"
				}];
				this._columnModel = columnmodel;
				return this._translate(columnmodel);
			}
		},
		_load: function () {
			return this._facade.selected;
		},
		_save: function () {
			var resourceid = this._model.getId();
			var records = this._getRecords();
			var fields = [];
			for (var i = 0; i < records.length; i++) {
				var field = {};
				for (var j = 0; j < this._columnModel.length; j++) {
					var colName = this._columnModel[j].name;
					var value = records[i][colName];
					if( Array.isArray(value) && value.length==0){
						value = null;
					}
					if( value || value === false ){
						if( value && colName == 'displayname') value = value+'';
						field[colName] = value;
					}
				}

				fields.push(field);
			}
			try {
				var data = {
					fields: fields
				};
				var json = data;
				ms123.util.Remote.rpcSync("setting:setResourceSetting", {
					namespace: this._facade.namespace,
					settingsid: this._facade.settingsid,
					resourceid: resourceid,
					settings: json
				});
				ms123.form.Dialog.alert(this.tr("settings.views_selected_items_saved"));
				ms123.config.ConfigManager.clearCache();
			} catch (e) {
				ms123.form.Dialog.alert("settings.views.SelectedItems._save:" + e);
			}
		}
	}
});
