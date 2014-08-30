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

qx.Class.define("ms123.graphicaleditor.plugins.propertyedit.ComplexListField", {
	extend: ms123.graphicaleditor.plugins.propertyedit.ComplexListWindow,
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (config, title, items, key, facade) {
		this.base(arguments,config,title,items,key,facade);
	},

	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},

	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		_init:function(){
			this.textField = this._createChildControl("textfield");
			var select = this._createChildControl("select");
			this._createChildControl("clear");
			this.setFocusable(true);
		},
		setValue: function (value) {
			console.log("ComplexListWindow.setValue:" + value);
			this.textField.setValue(value);
			this.data = value;
		},
		resetValue: function () {
			this.getChildControl("textfield").setValue(null);
			this.data = null;
		},
		// overridden
		_createChildControlImpl: function (id, hash) {
			var control;
			switch (id) {
			case "textfield":
				control = new qx.ui.form.TextField();
				control.setLiveUpdate(true);
				control.setFocusable(false);
				control.setReadOnly(true);
				control.setEnabled(false);
				control.addState("inner");
				this._add(control, {
					flex: 1
				});
				break;
			case "select":
				control = this.createActionButton();
				break;
			case "clear":
				var control = new qx.ui.form.Button(null, "icon/16/actions/edit-clear.png").set({
					padding: 0,
					margin: 0
				});
				control.setFocusable(false);
				control.addListener("execute", function () {
					var oldval = this.data;
					this._internalChange = true;
					this.resetValue();
					this.fireDataEvent("changeValue", null, oldval);
					this._internalChange = false;
				}, this);
				this._add(control);
				break;
			}
			return control;
		},
		createActionButton: function () {
			var control = new qx.ui.form.Button(null, "icon/16/apps/utilities-text-editor.png").set({
				padding: 0,
				margin: 0,
				maxHeight: 30
			});
			control.setFocusable(false);
			control.addListener("execute", function (e) {
				this._createWindow();
			}, this);
			this._add(control);
			return control;
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
