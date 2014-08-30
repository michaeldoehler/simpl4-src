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
qx.Class.define("ms123.enumerations.NewEnum", {
	extend: qx.core.Object,
	include: qx.locale.MTranslation,


	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (model, param, facade) {
		this.base(arguments);
		this._model = model;
		this._facade = facade;
		this.__storeDesc = facade.storeDesc;
		this.__createEnumDialog(facade);
	},


	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},


	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		__createEnum: function (name) {
			console.log("create.name:" + name);
			var enumData = {
				"fieldList": [{
					"description": "meta.enumdata.value",
					"fieldname": "value"
				},
				{
					"description": "meta.enumdata.label",
					"fieldname": "label"
				},
				{
					"description": "meta.enumdata.tooltip",
					"fieldname": "tooltip"
				}]
			};
			try {
				ms123.util.Remote.rpcSync("enumeration:saveEnumeration", {
					namespace: this.__storeDesc.getNamespace(),
					data: enumData,
					name: name
				});
			} catch (e) {
				ms123.form.Dialog.alert("EnumEditor.__createEnum:" + e);
				return;
			}
			ms123.form.Dialog.alert(this.tr("meta.enums.enum_created"));
			var nm = {};
			nm.id = name;
			nm.name = name;
			nm.value = name;
			nm.title = name;
			nm.type = "sw.enum";
			nm.children = [];
			var model = qx.data.marshal.Json.createModel(nm);
			var parentChilds = this._model.getChildren();
			model.parent = this._model;
			parentChilds.insertAt(0, model);
		},
		__isDup: function (array, name) {
			var lname = name.toLowerCase();
			var len = array.getLength();
			for (var i = 0; i < len; i++) {
				var n = array.getItem(i).getId().toLowerCase();
				if (n == lname) return true;
			}
			return false;
		},
		__createEnumDialog: function () {
			var formData = {
				"enumname": {
					'type': "TextField",
					'label': this.tr("meta.enums.new_enum_name"),
					'validation': {
						required: true,
						validator: "/^[A-Za-z]([0-9A-Za-z_]){2,20}$/"
					},
					'value': ""
				}
			};

			var self = this;
			var form = new ms123.form.Form({
				"formData": formData,
				"allowCancel": true,
				"inWindow": true,
				"callback": function (m) {
					if (m !== undefined) {
						var val = m.get("enumname");
						if (self.__isDup(self._model.getChildren(), val)) { //Duplicated entry
							ms123.form.Dialog.alert(self.tr("meta.enums.duplicated"));
							return;
						} else {
							self.__createEnum(val);
						}
					}
				},
				"context": self
			});
			form.show();
		}
	}
});
