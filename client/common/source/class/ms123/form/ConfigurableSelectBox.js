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

qx.Class.define("ms123.form.ConfigurableSelectBox", {
	extend: qx.ui.form.SelectBox,

	implement: [qx.ui.form.IStringForm, qx.ui.form.IForm, ms123.form.IConfig],
	include: [qx.ui.form.MForm],


	/**
	 *****************************************************************************
	 CONSTRUCTOR
	 *****************************************************************************
	 */


	construct: function (config, options) {
		this.base(arguments);
		this._optionsUrl = options;
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
		___onChangeSelection: function (e) {
			var model = e.getData()[0].getModel();
			this.fireDataEvent("changeValue", model.getValue(), null);
		},
		__before: function (context) {
			var app = qx.core.Init.getApplication();
			var params = ms123.util.Clone.merge({}, app.__userData, context.parentData);

			if (context.managedApp) {
				params["app_ns"] = context.managedApp;
			}

			var url = this.__supplant(this._optionsUrl, params);
			var url = this._optionsUrl;
			console.warn("aftersupplant:"+url);

			url = url.substring("delayed_rpc:".length);
			var obj = qx.lang.Json.parse(url); 
			var list = ms123.util.Remote.rpcSync( obj.method, obj.params );

			this.removeListener("changeSelection", this.___onChangeSelection, this);
			this.removeAll();
			var model = qx.data.marshal.Json.createModel(list);
			new qx.data.controller.List(model, this, "label");
			this.addListener("changeSelection", this.___onChangeSelection, this);

			var selectables = this.getSelectables(true);
			if (selectables.length > 0) {
				var model = selectables[0].getModel();
				this.setSelection([selectables[0]]);

				this.fireDataEvent("changeValue", model.getValue(), null);
			}
		},
    beforeSave : function(context) {
    },
		beforeAdd: function (context) {
			this.__before(context);
		},
		beforeEdit: function (context) {
			this.__before(context);
		},
		afterSave: function (context) {
		},
		__createListFromUrl: function (url) {
			return ms123.util.Remote.sendSync(url);
		},
		__supplant: function (s, o) {
			if (!o) return s;
			return s.replace(/@{([^{}]*)}/g, function (a, b) {
				var r = o[b];
				return typeof r === 'string' || typeof r === 'number' ? r : a;
			});
		},

		setValue: function (value) {
			if (value == null || value == "") {
				this.fireDataEvent("changeValue", this.getValue(), null);
			} else {
				var selectables = this.getSelectables(true);
				for (var i = 0; i < selectables.length; i++) {
					var model = selectables[i].getModel();
					console.log("mv:" + model.getValue());
					if (model.getValue() == value) {
						this.setSelection([selectables[i]]);
					}
				}
			}
		},

		getValue: function () {
			var selection = this.getSelection();
			if (selection == null || selection.length == 0) return null;
			var model = selection[0].getModel();
			console.log("getValue.value:" + model.getValue());
			return model.getValue();
		},

		resetValue: function () {}

	}


});
