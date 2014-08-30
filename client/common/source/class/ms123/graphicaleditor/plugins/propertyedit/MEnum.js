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
qx.Mixin.define("ms123.graphicaleditor.plugins.propertyedit.MEnum", {
  members: {
		createSelectedEnumDisplay: function () {
			var control = new qx.ui.form.TextField();
			control.setFocusable(false);
			control.setReadOnly(true);
			control.setEnabled(false);
			control.addState("inner");
			return control;
		},
		handleNodeSelected: function (e) {
			var model = e.getData().model;
			var type = e.getData().type;
			console.log("xtype:" + type);
			console.log("xmodel:" + model.getValue() + "/" + type);
			if (type == "sw.enum" || type == "sw.filter") {
				this.enumDisplay.setValue(type + ":" + model.getValue());
				var childs = model.getChildren();
				this.setTableData([]);
				for (var i = 0; i < childs.getLength(); i++) {
					var field = model.getChildren().getItem(i);
					var map = {};
					map.colname = field.getValue();
					this.addRecord(map);
				}
			}
		},
		handleOkButton: function (e) {
			this.table.stopEditing();
			var value = this.getTableData();
			var data = {
				totalCount: value.length,
				enumDescription: this.enumDisplay.getValue(),
				items: value
			};

			data = qx.util.Serializer.toJson(data);
			console.log("data:" + data);
			var oldVal = this.data;
			this.data = data;
			this.fireDataEvent("changeValue", data, oldVal);
			this.editWindow.close();
		}
	}
});
