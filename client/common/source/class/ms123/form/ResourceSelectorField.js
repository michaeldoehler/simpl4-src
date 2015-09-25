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

qx.Class.define("ms123.form.ResourceSelectorField", {
	extend: qx.ui.core.Widget,
	implement: [
	qx.ui.form.IStringForm, qx.ui.form.IForm],
	include: [
	qx.ui.form.MForm],
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (config, key, facade) {
		this.base(arguments);
		if( config && facade){
			this.set(config,facade, key);
		}

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
		set: function (config,facade, key) {
			this._config  = config;
			this._resourceType = config.type;
			this._showTextField = config.showTextField;
			this._key = config.key || key;
			this._facade = config.facade || facade;
			this._setLayout(new qx.ui.layout.HBox());

			this._createChildControl("textfield");
			this._createChildControl("select");
			this._createChildControl("clear");
			this.setFocusable(true);
		},
		/**
		 * Returns the field key.
		 */
		getFieldKey: function () {
			return this._key;
		},

		/**
		 * Returns the actual value of the trigger field.
		 * If the table does not contain any values the empty
		 * string will be returned.
		 */
		getValue: function () {
			console.error("ResourceSelectorField.getValue:" + this.data);
			return this.getChildControl("textfield").getValue();
		},

		/**
		 * Sets the value of the trigger field.
		 * In this case this sets the data that will be shown in
		 * the grid of the dialog.
		 * 
		 * param {Object} value The value to be set (JSON format or empty string)
		 */
		setValue: function (value) {
			console.log("ResourceSelectorField.setValue:" + value);
			if (value != undefined && value && value.length > 0) {
				if (this.data == undefined) {
				}
					this.data = value;
				this.getChildControl("textfield").setValue(value);
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
				var editable = this._config.editable===true;
				control.setReadOnly(!editable);
				control.setEnabled(editable);
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
					context.resourceType = this._resourceType;
					context.showTextField = this._showTextField;
					context.storeDesc = this._facade.storeDesc;
					context.config = this._config;
					context.title = this.tr("graphicaleditor.select_resource");
					context.selected_callback = (function(data){
						console.log("selected_callback:"+qx.util.Serializer.toJson(data));
						data = data.value;
						if(this._config.convertOut){
							data = this._config.convertOut(data);
						}
						var oldVal = this.data;
						this.data = data;
						this._internalChange = true;
						this.getChildControl("textfield").setValue(data);
						this._internalChange = false;
						console.log("----->data:"+data+"/"+oldVal);
						this.fireDataEvent("changeValue", data, oldVal);
					}).bind(this);
					var val = this.getChildControl("textfield").getValue();
					if(this._config.convertIn){
						val = this._config.convertIn(val);
					}
					new ms123.form.ResourceSelectorWindow(context, val);
				}, this);
				this._add(control);
				break;
			case "clear":
				var control = new qx.ui.form.Button(null, "icon/16/actions/edit-clear.png").set({
					padding: 0,
					margin: 0,
					maxHeight: 20
				});
				control.setFocusable(false);
				control.addListener("execute", function () {
					var oldval = this.data;
					this.data = "";
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
		_onTextFieldChangeValue:function(){
			if( this._internalChange === true) return;
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
