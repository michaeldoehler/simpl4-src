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
/*
*/
qx.Class.define("ms123.searchfilter.ExistsSubSelect", {
	extend: qx.ui.core.Widget,

	construct: function (params) {
		this.params = params;
		this.base(arguments);

		var hboxLayout = new qx.ui.layout.HBox().set({
			spacing: 4,
			alignX: "right"
		});

		this._setLayout(hboxLayout);

		this._add(this.getChildControl("exists_subselect"), { });
		this._add(this.getChildControl("filtername"), { });
		this._add(this.getChildControl("params"), { });
	},

	properties: {
		field: {
			check: "String",
			init: "",
			event: "changeField"
		},
		op: {
			check: "String",
			init: "",
			event: "changeOp"
		},
		data: {
			check: "String",
			init: "",
			event: "changeData"
		}
	},
	events: {
		"change": "qx.event.type.Data"
	},

	members: {
		// overridden
		_createChildControlImpl: function (id) {
			var control;

			switch (id) {
			case "exists_subselect":
				control = new qx.ui.form.SelectBox();
				control.setWidth(170);
				var tempItem = this.getChildControl("exists_subselect_item");
				control.add(tempItem);
				tempItem = this.getChildControl("exists_not_subselect_item");
				control.add(tempItem);
				control.addListener("changeSelection", function (e) {
					if (e.getData().length > 0) {
						this.fireDataEvent("changeField", e.getData()[0].getModel(), null);
					}
				}, this);
				break;
			case "exists_subselect_item":
				control = new qx.ui.form.ListItem(this.tr("filter.exists_subselect"), null, "_exists_subselect");
				break
			case "exists_not_subselect_item":
				control = new qx.ui.form.ListItem(this.tr("filter.exists_not_subselect"), null, "_exists_not_subselect");
				break
			case "connector_label":
				control = new qx.ui.basic.Label(this.tr("filter.text.and_help"));
				control.setRich( true );
				break
			case "filtername":
				control = new qx.ui.form.TextField();
				control.setWidth(150);
				control.setFilter("[0-9a-z_]");
				control.addListener("changeValue", function (e) {
					console.log("changeValue:" + e.getData());
					if (e.getData().length > 0) {
						this.fireDataEvent("changeOp", e.getData(), null);
					} else {
						this.fireDataEvent("changeOp", "", null);
					}
				}, this);
				break
			case "params":
				control = new qx.ui.form.TextField();
				control.setWidth(250);
				control.setFilter("[0-9a-z_ . =]");
				control.addListener("changeValue", function (e) {
					console.log("changeValue:" + e.getData());
					if (e.getData().length > 0) {
						this.fireDataEvent("changeData", e.getData(), null);
					} else {
						this.fireDataEvent("changeData", "", null);
					}
				}, this);
				break
			}

			return control || this.base(arguments, id);
		},

		getField: function () {
			var x = this.getChildControl("exists_subselect").getModelSelection();
			return x.getItem(0);
		},
		setField: function (value) {
		  this.fireDataEvent("change", value, null);
			this.getChildControl("exists_subselect").setModelSelection([value]);
		},
		getOp: function () {
			return this.getChildControl("filtername").getValue();
		},
		setOp: function (value) {
		  this.fireDataEvent("change", value, null);
		  this.getChildControl("filtername").setValue(value);
		},
		getData: function () {
			return this.getChildControl("params").getValue();
		},
		setData: function (value) {
		  this.fireDataEvent("change", value, null);
		  this.getChildControl("params").setValue(value);
		}
	}
});
