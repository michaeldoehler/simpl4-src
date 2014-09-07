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

qx.Class.define("ms123.graphicaleditor.plugins.propertyedit.TextAreaField", {
	extend: qx.ui.core.Widget,
	implement: [
	qx.ui.form.IStringForm, qx.ui.form.IForm],
	include: [
	qx.ui.form.MForm],
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (key,config,facade) {
		this.base(arguments);
		this.key = key;
		this.facade = facade;
		this.config = config||{};
		var layout = new qx.ui.layout.HBox();
		this._setLayout(layout);

		this.scriptField = key.match(".*activiti.script");
		var textField = this._createChildControl("textfield");
		var select = this._createChildControl("select");
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
		resetValue: function () {},

		/**
		 * Returns the actual value of the trigger field.
		 * If the table does not contain any values the empty
		 * string will be returned.
		 */
		getValue: function () {
			if( this.data && this.data.length> 32){
				console.log("TextAreaField.getValue1:" + this.data.substring(0,31));
			}else{
				console.log("TextAreaField.getValue2:" + this.data);
			}
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
			//console.log("TextAreaField.setValue:" + value);
			//if (value != undefined && value && value.length > 0) {
			//	if (this.data == undefined) {
					this.data = value;
					this.getChildControl("textfield").setValue(value);
			//	}
		//	}
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
				//control.addListener("changeValue", this._onTextFieldChangeValue, this);
				this._add(control, {
					flex: 1
				});
				break;
			case "select":
				var control = new qx.ui.form.Button(null, "icon/16/apps/utilities-text-editor.png").set({
					padding: 0,
					margin: 0,
					maxHeight: 30
				});
				control.setFocusable(false);
				control.addListener("execute", function (e) {

					var context = {}
					context.insertBar = this.scriptField;
					context.facade = this.facade;
					context.helper = this.config.helper;
					context.toolbarAddon = this.config.toolbarAddon;
					context.mode = (this.config && this.config.mode) ? this.config.mode : "text/x-groovy";
					this.textArea = this.createTextArea(context);
					if( !this.data ) this.data = "";
					this.textArea.setValue( this.data);
					var buttons = this.createButtons();
					this._editContainer = new qx.ui.container.Composite();
					this._editContainer.setLayout(new qx.ui.layout.Dock);
					this._editContainer.add(this.textArea, {
						edge: "center"
					});
					this._editContainer.add(buttons, {
						edge: "south"
					});
					var stack = this.facade.mainStack;
					stack.add(this._editContainer);
					stack.setSelection([this._editContainer]);
				}, this);
				this._add(control);
				break;
			case "clear":
				var control = new qx.ui.form.Button(null, "icon/16/actions/edit-clear.png").set({
					padding: 0,
					margin: 0
				});
				control.addListener("execute", function () {
					this.resetValue();
				}, this);
				this._add(control);
				break;
			}
			return control;
		},
		createTextArea: function (context) {
			var textAreaMax = new ms123.codemirror.CodeMirror(context);//new qx.ui.form.TextArea();
      textAreaMax.set({
        height: null,
        width: null
      });
			return textAreaMax;
		},
		createButtons: function () {
			var toolbar = new qx.ui.toolbar.ToolBar();
			toolbar.setSpacing(5);

			var buttonSave = new qx.ui.toolbar.Button(this.tr("save"), this.__getResourceUrl("disk.png"));
			buttonSave.addListener("execute", function () {
				var value = this.textArea.getValue();
				var data = value;
				var oldVal = this.data;
				this.data = data;
				this.fireDataEvent("changeValue", data, oldVal);
				this.getChildControl("textfield").setValue(data);
				this.facade.save.save();
			}, this);
			toolbar._add(buttonSave)

			if(this.facade.editorType == "sw.process"){
				var buttonDeploy = new qx.ui.toolbar.Button(this.tr("deploy"), "icon/16/actions/media-playback-start.png");
				buttonDeploy.addListener("execute", function () {
					var value = this.textArea.getValue();
					var data = value;
					var oldVal = this.data;
					this.data = data;
					this.fireDataEvent("changeValue", data, oldVal);
					this.getChildControl("textfield").setValue(data);
					this.facade.save.deploy();
				}, this);
				toolbar._add(buttonDeploy)
			}

			toolbar.addSpacer();
			toolbar.addSpacer();

			var buttonOk = new qx.ui.toolbar.Button(this.tr("Ok"), "icon/16/actions/dialog-ok.png");
			buttonOk.addListener("execute", function () {
				var value = this.textArea.getValue();
				var data = value;
				var oldVal = this.data;
				this.data = data;
				this.fireDataEvent("changeValue", data, oldVal);
				var stack = this.facade.mainStack;
				stack.setSelection([stack.getChildren()[0]]);
				stack.remove(this._editContainer);
				this.getChildControl("textfield").setValue(data);
			}, this);
			toolbar._add(buttonOk)

			var buttonCancel = new qx.ui.toolbar.Button(this.tr("Cancel"), "icon/16/actions/dialog-close.png");
			buttonCancel.addListener("execute", function () {
				var stack = this.facade.mainStack;
				stack.setSelection([stack.getChildren()[0]]);
				stack.remove(this._editContainer);
			}, this);
			toolbar._add(buttonCancel)

			return toolbar;
		},
		__getResourceUrl: function (name) {
			var am = qx.util.AliasManager.getInstance();
			return am.resolve("resource/ms123/" + name);
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
