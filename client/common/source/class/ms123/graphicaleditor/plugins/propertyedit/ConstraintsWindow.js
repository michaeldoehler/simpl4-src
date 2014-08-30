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

qx.Class.define("ms123.graphicaleditor.plugins.propertyedit.ConstraintsWindow", {
	extend: qx.ui.core.Widget,
	implement: [
	qx.ui.form.IStringForm, qx.ui.form.IForm],
	include: [
	qx.ui.form.MForm],
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (config, title, data) {
		this.base(arguments);
		this.config = config || {};
		this.title = title;
		this._controls = {};
		this.data = data;
		console.log("config:"+qx.util.Serializer.toJson(config));
		this._init();
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
		_init: function () {
			var app = qx.core.Init.getApplication();
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
			app.getRoot().add(win);
			win.open();
		},
		setEnabled: function () {},
		getEnabled: function () {return true;},
		resetValue: function () {},
		/**
		 * Returns the actual value of the trigger field.
		 * If the table does not contain any values the empty
		 * string will be returned.
		 */
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
			this.data = value;
		},


		// overridden
		_createChildControlImpl: function (id, hash) {},
		createActionButton: function () {},
		createSelectionPane: function (optionPane) {
			var data = this.data;
			if (data == undefined || !data || data == "") {;
				data = {};
			} else {
				try {
					console.log("data:" + data);
					data = qx.lang.Json.parse(data);
				} catch (e) {
					console.error("ConstraintsField.createSelectionPane:" + data + " wrong value");
				}
			}
			var ownDecorator = new ms123.util.RoundSingleBorder(1, "solid", "gray", 5);
			var pane = new qx.ui.container.Composite().set({
				decorator: ownDecorator
			});
			pane.setPadding(15);
			var layout = new qx.ui.layout.Grid(20, 10);
			pane.setLayout(layout);
			for (var i = 0; i < this.config.length; i++) {
				var map = this.config[i];
				var clazz = map.clazz;
				var options = map.options || [];
				var l = map.label;
				if (l && l.length > 1 && l.match(/^@/)) {
					l = this.tr(l.substring(1));
				}

				var dataList = data[clazz];
				var cb = new qx.ui.form.CheckBox(l || clazz);
				var controlList = [];
				controlList.push(cb);
				cb.setUserData("clazz", clazz);
				cb.setUserData("optionsCtrl", []);
				var enabled = dataList ? dataList[0] : false;
				cb.setValue(enabled);
				cb.addListener("execute", function (e) {
					var c = e.getTarget();
					var value = c.getValue();
					var ol = c.getUserData("optionsCtrl");
					for (var z = 0; z < ol.length; z++) {
						ol[z].setVisibility(value ? "visible" : "hidden");
					}
				}, this);
				pane.add(cb, {
					row: i,
					column: 0
				});
				for (var j = 0; j < options.length; j++) {
					layout.setColumnFlex(j + 1, 1);

					var value = dataList && dataList.length > (j + 1) ? dataList[j + 1] : null;
					var pair = this.getPair(options[j], value, controlList);
					if (!enabled) {
						pair.setVisibility("hidden");
					}
					cb.getUserData("optionsCtrl").push(pair);
					pane.add(pair, {
						row: i,
						column: 1 + j
					});
				}
				this._controls[clazz] = controlList;
			}
			return pane;
		},
		getPair: function (option, value, controlList) {
			var l = option.label;
			if (l.length > 1 && l.match(/^@/)) {
				l = this.tr(l.substring(1));
			}
			var noLabel = false;
			if (l == null || l.length == 0) {
				noLabel = true;
			}
			var label = new qx.ui.basic.Label(l).set({
				alignY: "middle"
			});
			var input = null;
			var type = option.type;
			if (type.indexOf(",")) {
				type = type.split(",")[0];
			}
			if (type == "int") {
				input = new ms123.form.NumberField();
				input.setWidth(35);
				input.setFilter(/[0-9]/);
			} else if (type == "string") {
				input = new qx.ui.form.TextField();
				input.setMaxLength(50);
				input.setLiveUpdate(false);
				input.setWidth(55);
			} else if (type == "date") {
				var m = qx.locale.Manager.getInstance();
				var lang = m.getLanguage();
				input = new qx.ui.form.DateField();
				var format = new qx.util.format.DateFormat("MM-dd-yyyy");
				if (lang == "de") {
					format = new qx.util.format.DateFormat("dd.MM.yyyy");
				}
				input.setDateFormat(format);
			} else if (type == "double" || type=="decimal") {
				input = new ms123.form.DecimalField();
				input.setFilter(/[0-9.,]/);
				input.setValue(0.0);
				input.setWidth(55);
			}
			controlList.push(input);
			if (input instanceof qx.ui.form.DateField) {
				input.setValue(this._getDate(value));
			} else {
				input.setValue(value);
			}
			var pane = new qx.ui.container.Composite();
			pane.setLayout(new qx.ui.layout.HBox(5));
			if (noLabel) {
				pane.add(input, {
					width: "100%",
					flex: 1
				});
			} else {
				pane.add(label, {
					width: "33%",
					flex: 1
				});
				pane.add(input, {
					width: "33%",
					flex: 1
				});
			}
			return pane;
		},
		_getDate: function (time) {
			try {
				if (time == null) return new Date();
				return new Date(time);
			} catch (e) {
				return new Date();
			}
		},
		_getTime: function (date) {
			try {
				return date.getTime();
			} catch (e) {
				return null;
			}
		},
		handleOkButton: function (e) {
			var dataMap = {};
			for (var i = 0; i < this.config.length; i++) {
				var map = this.config[i];
				var clazz = map.clazz;
				var cl = this._controls[clazz];
				var values = [];
				for (var j = 0; j < cl.length; j++) {
					var c = cl[j];
					var value = c.getValue();
					if (c instanceof qx.ui.form.DateField) {
						value = this._getTime(value);
					}
					values.push(value);
				}
				dataMap[cl[0].getUserData("clazz")] = values;
			}
			//var value = qx.lang.Json.stringify(dataMap, null, 4); console.log("ENV1:" + value);
			dataMap = qx.util.Serializer.toJson(dataMap);
			var oldVal = this.data;
			this.data = dataMap;
			this.fireDataEvent("changeValue", dataMap, oldVal);
			this.editWindow.close();
		},
		createButtons: function () {
			var toolbar = new qx.ui.toolbar.ToolBar();
			toolbar.setSpacing(5);
			toolbar.addSpacer();
			toolbar.addSpacer();

			var buttonSave = new qx.ui.toolbar.Button(this.tr("Ok"), "icon/16/actions/dialog-ok.png");
			buttonSave.addListener("execute", function (e) {
				this.handleOkButton(e);
			}, this);
			toolbar._add(buttonSave)

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
			win.setLayout(new qx.ui.layout.Dock);
			win.setWidth(500);
			win.setHeight(400);
			win.setAllowMaximize(false);
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
