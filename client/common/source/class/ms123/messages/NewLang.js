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
qx.Class.define("ms123.messages.NewLang", {
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
		this.__createLangDialog(facade);
	},


	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},


	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		__createLang: function (lang) {
			console.log("create.lang:" + lang);
			try {
				ms123.util.Remote.rpcSync("message:saveMessages", {
					namespace: this.__storeDesc.getNamespace(),
					msgList: [],
					lang: lang
				});
			} catch (e) {
				ms123.form.Dialog.alert("messages.__createLang:" + e);
				return;
			}
			ms123.form.Dialog.alert(this.tr("messages.lang_created"));
			var nm = {};
			nm.id = lang;
			nm.name = lang;
			nm.value = lang;
			nm.title = lang;
			nm.type = "sw.messageslang";
			nm.children = [];
			var model = qx.data.marshal.Json.createModel(nm);
			var parentChilds = this._model.getChildren();
			parentChilds.insertAt(0, model);
		},
		__isDup: function (array, lang) {
			var llang = lang.toLowerCase();
			var len = array.getLength();
			for (var i = 0; i < len; i++) {
				var n = array.getItem(i).getId().toLowerCase();
				if (n == llang) return true;
			}
			return false;
		},
		__createLangDialog: function () {
			var formData = {
				"lang": {
					'type': "TextField",
					'label': this.tr("messages.new_lang"),
					'validation': {
						required: true,
						validator: "/^[A-Za-z]([0-9A-Za-z_]){1,20}$/"
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
						var val = m.get("lang");
						if (self.__isDup(self._model.getChildren(), val)) { //Duplicated entry
							ms123.form.Dialog.alert(self.tr("messages.lang_duplicated"));
							return;
						} else {
							self.__createLang(val);
						}
					}
				},
				"context": self
			});
			form.show();
		}
	}
});
