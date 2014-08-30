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
	* @lint ignoreDeprecated(alert,eval) 
*/

qx.Class.define("ms123.graphicaleditor.plugins.propertyedit.ConstraintsField", {
	extend: ms123.graphicaleditor.plugins.propertyedit.ConstraintsWindow,
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (config, title, items, key) {
		this.base(arguments,config,title);
		this.key = key;
	},

	/******************************************************************************
	 EVENTS
	 ******************************************************************************/
	events: {
		"changeValue": "qx.event.type.Data"
	},


	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},

	/******************************************************************************
	 STATICS
	 ******************************************************************************/
	statics: {},
	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		getFieldKey: function () {
			return this.key;
		},
		_init:function(){
			var layout = new qx.ui.layout.HBox();
			this._setLayout(layout);

			this.textField = this._createChildControl("textfield");
			var select = this._createChildControl("select");
			this.setFocusable(true);
		},
		getValue: function () {
			return this.data;
		},

		/**
		 * Sets the value of the trigger field.
		 * In this case this sets the data that will be shown in
		 * the grid of the dialog.
		 * 
		 * param {Object} value The value to be set (JSON format or empty string)
		 */
		setValue: function (value) {
			this.textField.setValue(value);
			this.data = value;
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
				control.addListener("execute", function () {
					alert("clear");
					this.resetValue();
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
				var selectionPane = this.createSelectionPane(selectionPane);
				var buttons = this.createButtons();
				var win = this.createWindow(this.title);
				win.add(selectionPane, {
					edge: "center"
				});
				win.add(buttons, {
					edge: "south"
				});
				this.editWindow = win;
				win.open();
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
