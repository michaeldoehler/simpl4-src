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

qx.Class.define("ms123.graphicaleditor.plugins.propertyedit.FilterParamField", {
	extend: ms123.graphicaleditor.plugins.propertyedit.ComplexListField,
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (config, title, items, key, facade) {
		this.base(arguments, config, title, items, key, facade);
	},
	members: {
		setValue: function (value) {
			this.base(arguments, value);
			if (value != undefined && value && value != "") {;
				try{
					value = qx.lang.Json.parse(value);
					this._value = value;
					//console.log("FilterParamField.setValue:" + JSON.stringify(value,null,2));
					if (this._filterForm) this._filterForm.setData(value);
				}catch(e){
					console.error("FilterParamField.setValue:"+value+" wrong value");
				}
			}else{
				this._value = {};
			}
		},
		handleNodeSelected: function (e) {
			var model = e.getData().model;
			var type = e.getData().type;
			console.log("model:" + JSON.stringify(e.getData(),null,2));
			if (type == "sw.filter") {
				this._filterForm.setData({name:model.getValue()});
				var childs = model.getChildren();
				this.setTableData([]);
				for (var i = 0; i < childs.getLength(); i++) {
					var field = model.getChildren().getItem(i);
					var map = {};
					map.param = field.getValue();
					this.addRecord(map);
				}
			}
		},
		_createForm: function () {
			var formData = {
				"name": {
					'type': "TextField",
					'label': this.tr("filtercheck.name"),
					'readonly': true,
					'value': ""
				},
				"message": {
					'type': "TextField",
					'label': this.tr("filtercheck.message"),
					'validation': {
						required: false
					},
					'value': ""
				}
			};

			var self = this;
			var form = new ms123.form.Form({
				"tabs": [{
					id: "tab1",
					layout: "single"
				}],
				"useScroll": false,
				"formData": formData,
				"buttons": [],
				"inWindow": false,
				"context": self
			});
			this._filterForm = form;
			return form;
		},
		handleOkButton: function (e) {
			this.table.stopEditing();
			var value = this.getTableData();
			var formData = this._filterForm.getData();
			var data = null;
			if( value.length > 0){
				data = {
					totalCount: value.length,
					name: formData.name,
					message: formData.message,
					items: value
				};
				data = qx.util.Serializer.toJson(data);
			}

			console.log("data:" + data);
			var oldVal = this.data;
			this.data = data;
			this.fireDataEvent("changeValue", data, oldVal);
			this.editWindow.close();
		},
		createToolbar: function () {
			var toolbar = this.base(arguments, ["del"]);
			var form = this._createForm();
			if (this._value) form.setData(this._value);
			var container = new qx.ui.container.Composite();
			container.setLayout(new qx.ui.layout.Dock());
			container.add(toolbar, {
				edge: "north"
			});
			container.add(form, {
				edge: "center"
			});
			return container;
		}
	}
});
