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
 @asset(qx/decoration/*)
 @asset(qx/decoration/Modern/tree/closed.png)
 */

qx.Class.define("ms123.form.SelectBox", {
	extend: qx.ui.form.SelectBox,

	implement: [ms123.form.IConfig],


	/**
	 *****************************************************************************
	 CONSTRUCTOR
	 *****************************************************************************
	 */

	construct: function (selectable_items) {
		this.base(arguments);
		this._selectable_items = selectable_items;
    this.getChildControl("list").setQuickSelection(false);
	},


	/**
	 *****************************************************************************
	 PROPERTIES
	 *****************************************************************************
	 */


	properties: {},

	events: {
		"changeValue": "qx.event.type.Data"
	},


	/**
	 *****************************************************************************
	 MEMBERS
	 *****************************************************************************
	 */
	members: {
		createList: function (options) {
			if (typeof options == 'string') {
				return;
			}
			var model = qx.data.marshal.Json.createModel(this._correctOptions(options));
			var lc = new qx.data.controller.List(model, this, "label");
			this._listController = lc;

			var delegate = {
				createItem: function () {
					return new ms123.form.TooltipListItem();
				},

				bindItem: function (controller, item, index) {
					controller.bindProperty("tooltip", "tooltip", null, item, index);
					controller.bindProperty("", "model", null, item, index);
					controller.bindProperty(controller.getLabelPath(), "label", controller.getLabelOptions(), item, index);
					if (controller.getIconPath() != null) {
						controller.bindProperty(controller.getIconPath(), "icon", this.getIconOptions(), item, index);
					}
				}
			};
			lc.setDelegate(delegate);
		},
		_correctOptions: function (options) {
			for (var i = 0; i < options.length; i++) {
				var o = options[i];
				if (!o.value === undefined) {
					o.value = null;
				}
				if (o.label === undefined) {
					o.label = null;
				}
				if (o.tooltip === undefined) {
					o.tooltip = null;
				}
			}
			return options;
		},
		_refreshItems: function (vars) {
			console.log("_refreshItems.Vars:" + JSON.stringify(vars, null, 2));
			this.resetSelection();
			this.setModelSelection([]);
			this._selectable_items.setVarMap(vars);
			var items = this._selectable_items.getItems();
	    if(typeof items == "string" || items.length == null)return;
			var listModel = this._listController.getModel();
			listModel.splice(0, listModel.getLength());
			var newElements = qx.data.marshal.Json.createModel(this._correctOptions(items), true);
			listModel.append(newElements);
		},
		beforeSave: function (context) {},
		updateEvent: function (eventData) {
			if (this._selectable_items == null) return;
			var name = eventData.name;
			var value = eventData.value;
			console.log("updateEvent.in:" + this.getUserData("key") + "/field:" + name + "=" + value+"/missingParamList:"+this._missingParamList);
			if (this._missingParamList && this._missingParamList.indexOf(name) >= 0) {
				var vars = {};
				vars[name] = value;
				this._refreshItems(vars);
			}
		},
		beforeAdd: function (context) {
			if (this._selectable_items == null) return;
			this._missingParamList = this._selectable_items.getMissingParamList();
		},
		beforeEdit: function (context) {
			if (this._selectable_items == null) return;
			console.log("Missing:" + JSON.stringify(this._selectable_items.getMissingParamList(), null, 2));
			var missingParamList = this._selectable_items.getMissingParamList();
			this._missingParamList = missingParamList;
			if (missingParamList) {
				var vars = {};
				for (var i = 0; i < missingParamList.length; i++) {
					var mp = missingParamList[i];
					vars[mp] = context.data[mp];
				}
				this._refreshItems(vars);
			}
		},
		afterSave: function (context) {}
	}


});
