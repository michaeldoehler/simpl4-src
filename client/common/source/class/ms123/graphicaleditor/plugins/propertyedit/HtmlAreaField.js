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

qx.Class.define("ms123.graphicaleditor.plugins.propertyedit.HtmlAreaField", {
	extend: qx.ui.core.Widget,
	implement: [
	qx.ui.form.IStringForm, qx.ui.form.IForm],
	include: [
	qx.ui.form.MForm],
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (key, config, facade) {
		this.base(arguments);
		this.key = key;
		this.facade = facade;
		this.config = config || {};
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
			if (this.data && this.data.length > 32) {
				console.log("HtmlAreaField.getValue1:" + this.data.substring(0, 31));
			} else {
				console.log("HtmlAreaField.getValue2:" + this.data);
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

					var app = qx.core.Init.getApplication();
					var win = this.createWindow(this.title);
					win.addListener("close", function (e) {
						win.destroy();
					}, this);
					var context = {}
					context.insertBar = this.scriptField;
					context.facade = this.facade;
					context.helper = this.config.helper;
					context.toolbarAddon = this.config.toolbarAddon;
					context.mode = (this.config && this.config.mode) ? this.config.mode : "text/x-groovy";
					this.textArea = this.createTextArea(win, context);
					if (!this.data) this.data = "";
					//this.textArea.setValue( this.data);
					var buttons = this.createButtons();
/*					win.add(this.textArea, {
						edge: "center"
					});*/
					win.add(buttons, {
						edge: "south"
					});
					app.getRoot().add(win);
					this.editWindow = win;
					win.open();
					this.win = win;
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
		createTextArea: function (win, context) {

			console.log("Data:" + this.data);
			var c = new qx.ui.container.Composite(new qx.ui.layout.Dock());
			var ckrte_editor = new ms123.ckeditor.Ckeditor(null, this.facade);
			var toolbar = new ms123.ckeditor.ToolbarCombined(ckrte_editor);
			c.add(toolbar, {
				edge: "north"
			});
			c.add(ckrte_editor, {
				edge: "center"
			});
			win.add(c, {
				edge: "center"
			});
			ckrte_editor.addListenerOnce("appear", function (e) {
				var namespace= this.facade.storeDesc.getNamespace();
				var data = this.data.replace(/src="repo/g, 'src="'+namespace+'/repo');
				//data = data.replace('repo:','repo%3A');
				console.log("Data2:" + data);
				ckrte_editor.setData(data);
			}, this);
			return ckrte_editor;
		},
		createButtons: function () {
			var toolbar = new qx.ui.toolbar.ToolBar();
			toolbar.setSpacing(5);

			var buttonSave = new qx.ui.toolbar.Button(this.tr("Save"), "icon/16/actions/dialog-apply.png");
			buttonSave.addListener("execute", function () {
				var value = this.textArea.getData();
				value = value.replace(/src="[a-z_]*\/repo/g, 'src="repo');
				var data = value;
				var oldVal = this.data;
				this.data = data;
				this.fireDataEvent("changeValue", data, oldVal);
				this.getChildControl("textfield").setValue(data);
				this.facade.save.save();
			}, this);
			toolbar._add(buttonSave)

			toolbar.addSpacer();
			toolbar.addSpacer();

			var buttonOk = new qx.ui.toolbar.Button(this.tr("Ok"), "icon/16/actions/dialog-ok.png");
			buttonOk.addListener("execute", function () {
				var value = this.textArea.getData();
				value = value.replace(/src="[a-z_]*\/repo/g, 'src="repo');
				var data = value;
				var oldVal = this.data;
				this.data = data;
				this.fireDataEvent("changeValue", data, oldVal);
				this.win.destroy();
				this.getChildControl("textfield").setValue(data);
			}, this);
			toolbar._add(buttonOk)

			var buttonCancel = new qx.ui.toolbar.Button(this.tr("Cancel"), "icon/16/actions/dialog-close.png");
			buttonCancel.addListener("execute", function () {
				this.editWindow.close();
			}, this);
			toolbar._add(buttonCancel)

			return toolbar;
		},
		createWindow: function (name) {
			var win = new qx.ui.window.Window(name, "").set({
				resizable: true,
				useMoveFrame: true,
				useResizeFrame: true
			});
			var root = qx.core.Init.getApplication().getRoot();
			var w = root.getInnerSize().width;
			var h = root.getInnerSize().height;

			win.setLayout(new qx.ui.layout.Dock);
			if (this.config && this.config.width) {
				win.setWidth(this.config.width);
			} else if (this.config && this.config.helper) {
				win.setWidth(w * .8);
			} else {
				win.setWidth(w * .7);
			}
			if (this.config && this.config.height) {
				win.setHeight(this.config.height);
			} else if (this.config && this.config.helper) {
				win.setHeight(h * .7);
			} else {
				win.setHeight(h * .6);
			}
			win.setAllowMaximize(true);
			win.setAllowMinimize(false);
			win.setModal(true);
			win.setActive(false);
			win.minimize();
			win.center();
			return win;
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
