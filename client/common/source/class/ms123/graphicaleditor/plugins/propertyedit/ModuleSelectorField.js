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

qx.Class.define("ms123.graphicaleditor.plugins.propertyedit.ModuleSelectorField", {
	extend: qx.ui.core.Widget,
	implement: [
	qx.ui.form.IStringForm, qx.ui.form.IForm],
	include: [
	qx.ui.form.MForm],
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (from, key, facade) {
		this.base(arguments);
		var p  = from.split(":");
		this.module = p[0];
		this.field = p[1];
		this.key = key;
		this.facade = facade;
		var layout = new qx.ui.layout.HBox();
		this._setLayout(layout);

console.error("ModuleSelectorField:"+from+"/"+key);
		var textField = this._createChildControl("textfield");
		var select = this._createChildControl("select");
		var clear = this._createChildControl("clear");
		this.setFocusable(true);

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
		/**
		 * Returns the field key.
		 */
		getFieldKey: function () {
			return this.key;
		},

		/**
		 * Returns the actual value of the trigger field.
		 * If the table does not contain any values the empty
		 * string will be returned.
		 */
		getValue: function () {
			console.log("ModuleSelectorField.getValue:" + this.data);
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
			console.log("ModuleSelectorField.setValue:" + value);
			if (value != undefined && value && value.length > 0) {
				if (this.data == undefined) {
					this.data = value;
					this.getChildControl("textfield").setValue(value);
				}
			}
		},
		resetValue: function () {
			this.getChildControl("textfield").setValue(null);
		},

		// overridden
		_createChildControlImpl: function (id, hash) {
			var control;
			switch (id) {
			case "textfield":
				control = new qx.ui.form.TextField();
				control.setLiveUpdate(true);
				control.setFocusable(false);
				//control.setReadOnly(true);
				//control.setEnabled(false);
				control.addState("inner");
				control.addListener("changeValue", this._onTextFieldChangeValue, this);
				this._add(control, {
					flex: 1
				});
				break;
			case "select":
				var control = new qx.ui.form.Button(null, "icon/16/apps/utilities-text-editor.png").set({
					padding: 0,
					margin: 0,
					maxHeight: 20
				});
				control.setFocusable(false);
				control.addListener("execute", function (e) {
					var context = {};
					context.modulename = this.module;
					context.title = this.tr("graphicaleditor.bpmn.select_form");
					context.selected_callback = (function(value){
						var data = value[this.field];
						var oldVal = this.data;
						this.data = data;
						this.getChildControl("textfield").setValue(data);
						this.fireDataEvent("changeValue", data, oldVal);
					}).bind(this);
					context.storeDesc = this.facade.storeDesc;
					new ms123.util.RecordSelector(context);
				}, this);
				this._add(control);
				break;
			case "clear":
				var control = new qx.ui.form.Button(null, "icon/16/actions/edit-clear.png").set({
					padding: 0,
					maxHeight: 20,
					margin: 0
				});
				control.addListener("execute", function () {
					var oldval = this.data;
					this.data = "";
					this.resetValue();
					this.fireDataEvent("changeValue", null, oldval);
				}, this);
				this._add(control);
				break;
			}
			return control;
		},
		_onTextFieldChangeValue:function(){
			var oldVal = this.data;
			this.data = this.getChildControl("textfield").getValue();
			this.fireDataEvent("changeValue", this.data, oldVal);
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
