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
qx.Class.define("ms123.searchfilter.SetConnector", {
	extend: ms123.searchfilter.Connector,

	construct: function (params) {
		this.base(arguments, params);

		this._setLayout(new qx.ui.layout.HBox());

		this._add(this.getChildControl("connector"), {
		});
		this._add(this.getChildControl("connector_label"), {
		});
	},

	properties: {
	},
	events: {
		"changeValue": "qx.event.type.Data"
	},

	members: {
		// overridden
		_createChildControlImpl: function (id) {
			var control;

			switch (id) {
			case "connector":
				control = new qx.ui.form.SelectBox();
				var tempItem = this.getChildControl("connector_union");
				control.add(tempItem);
				tempItem = this.getChildControl("connector_except");
				control.add(tempItem);
				tempItem = this.getChildControl("connector_intersect");
				control.add(tempItem);
				control.addListener("changeSelection", function (e) {
					if (e.getData().length > 0) {
						this.fireDataEvent("changeValue", e.getData()[0].getModel(), null);
//						this.getChildControl("connector_label").setValue(this.params[e.getData()[0].getModel() + "_" + "helpText"]);
						this.getChildControl("connector_label").setValue(this.tr("filter.text."+e.getData()[0].getModel()+"_help"));
					}
				}, this);
				break;
			case "connector_union":
				control = new qx.ui.form.ListItem(this.tr("filter.text.union"), null, "union");
				break
			case "connector_intersect":
				control = new qx.ui.form.ListItem(this.tr("filter.text.intersect"), null, "intersect");
				break
			case "connector_except":
				control = new qx.ui.form.ListItem(this.tr("filter.text.except"), null, "except");
				break
			case "connector_label":
				control = new qx.ui.basic.Label(this.tr("filter.text.union_help"));
				control.setRich( true );
				break
			}

			return control || this.base(arguments, id);
		},

		_applyValue: function (value, old) {
			this.getChildControl("connector").setModelSelection([value]);
		}
	}
});
