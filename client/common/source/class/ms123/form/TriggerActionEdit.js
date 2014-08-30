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
 @ignore(moment)
 @asset(qx/icon/${qx.icontheme}/22/actions/*)
 @asset(qx/icon/${qx.icontheme}/16/places/*)
 */
qx.Class.define("ms123.form.TriggerActionEdit", {
	extend: qx.ui.container.Composite,
	implement: [qx.ui.form.IStringForm, qx.ui.form.IForm, ms123.form.IConfig],
	include: [
	qx.ui.form.MForm, ms123.searchfilter.MSearchFilter],


	/**
	 *****************************************************************************
	 CONSTRUCTOR
	 *****************************************************************************
	 */

	construct: function (config) {
		this.base(arguments);
		var layout = new qx.ui.layout.Grow();
		this.setLayout(layout);

		this._config = config;
		this.set({
			decorator: "main",
			minHeight: 200,
			allowGrowX: true,
			allowGrowY: true
		});
		this.__modelNull = qx.data.marshal.Json.createModel({
			title: "ROOT",
			children: []
		});
		this.__configManager = new ms123.config.ConfigManager();
		this.add(this.getChildControl("tabView"));
		this.__buildFieldActionPage();
		this.__buildServiceActionPage();
		this.__buildProcessActionPage();
	},

	/**
	 *****************************************************************************
	 PROPERTIES
	 *****************************************************************************
	 */

	properties: {
		// overridden
	},

	/**
	 *****************************************************************************
	 EVENTS
	 *****************************************************************************
	 */

	events: {
		"changeValue": "qx.event.type.Data"
	},


	/**
	 *****************************************************************************
	 MEMBERS
	 *****************************************************************************
	 */

	members: {
		// overridden
		/**
		 * @lint ignoreReferenceField(_forwardStates)
		 */
		__value: {},
		_forwardStates: {
			focused: true
		},
    beforeSave : function(context) {
    },
		beforeAdd: function (context) {
			this.__storeDesc = context.storeDesc;
			this.__createModel(context);
			this.__targetmodulename = context.parentData.targetmodule;
			this.__updateTargetTypeItem.setLabel(this.tr("tactions.update-target") + "(" + this.__targetmodulename + ")");

			this.__value.targetmodule = context.parentData.targetmodule;
			this.__value.action = "update-target";
			this.__value.fieldactive = false;
			this.__value.serviceactive = false;
			this.__value.processactive = false;
			this._fireChangeValue();

			this.getChildControl("targetSelect").setEnabled(true);
			this.getChildControl("typeSelect").setEnabled(true);
			this.__isEdit = false;
			this.__lastModuleName = null;
		},
		beforeEdit: function (context) {
			this.__storeDesc = context.storeDesc;
			this.__createModel(context);
			this.__value.fieldactive = false;
			this.__value.serviceactive = false;
			this.__value.processactive = false;
			this.__targetmodulename = context.parentData.targetmodule;
			this.__updateTargetTypeItem.setLabel(this.tr("tactions.update-target") + "(" + this.__targetmodulename + ")");

			this._fireChangeValue();

			this.getChildControl("targetSelect").setEnabled(false);
			this.getChildControl("typeSelect").setEnabled(false);
			this.__isEdit = true;
		},
		afterSave: function (context) {},
		__createModel: function (context) {
			var parentData = context.parentData;
			this.__modelOne = this._createModel(parentData.targetmodule, "one");
			this.__modelColl = this._createModel(parentData.targetmodule, "collection");
		},
		_createModel: function (mainModule, type) {
			console.log("mainModule:" + mainModule);
			var x = mainModule.split(".");
			var mainEntity = mainModule;
			var pack = "default";
			if (x.length == 2) {
				mainEntity = x[1];
				pack = x[0];
			}
			var cm = new ms123.config.ConfigManager();
			var treeModel = cm.getEntityTree(this.__storeDesc, mainEntity, 2, false, true, type);
			this._translateModel(treeModel);
			var model = qx.data.marshal.Json.createModel(treeModel);
			return model;
		},
		setValue: function (value) {
			try {
				if (value == null || value == "") {
					var typeSelect = this.getChildControl("typeSelect");
					if (typeSelect.getModelSelection().getItem(0) == "update-target") {
						this.__value.targetmodule = this.__targetmodulename;
						this._fireChangeValue();
					} else {
						this.__changeTargetSelectModel(this.__value.action);
					}
				} else {
					this.__value = qx.lang.Json.parse(value);
					var typeSelect = this.getChildControl("typeSelect");
					typeSelect.setModelSelection([this.__value.action]);
					this.__changeTargetSelectModel(this.__value.action);
				}
				var serviceInput = this.getChildControl("serviceInput");
				serviceInput.setValue(this.__value.servicecall ? this.__value.servicecall : "");

				var serviceActive = this._getCheckbox("serviceActive");
				serviceActive.setValue(this.__getBoolean(this.__value.serviceactive));

				var fieldActive = this._getCheckbox("fieldActive");
				fieldActive.setValue(this.__getBoolean(this.__value.fieldactive));


				var processInput = this.getChildControl("processInput");
				processInput.setValue(this.__value.processcall ? this.__value.processcall : "");

				var processActive = this._getCheckbox("processActive");
				processActive.setValue(this.__getBoolean(this.__value.processactive));

				var startProcessUser = this.getChildControl("startProcessUser");
				startProcessUser.setModelSelection([this.__value.startProcessUser]);


			} catch (e) {
				console.debug(e.stack);
				console.error("TriggerActionEdit.setValue.Ex:" + e);
			}
		},

		getValue: function () {
			var data = qx.util.Serializer.toJson(this.__value);
			console.log("getValue:" + data);
			return data;
		},
		resetValue: function () {},
		// useit checkbox
		getCheckBox: function () {
			return this.getChildControl("checkbox");
		},
		__getBoolean: function (value) {
			if (value === false) return false;
			return true;
		},
		_translateModel: function (model) {
			model.title = this.tr(model.title);
			var children = model.children;
			if (children) {
				for (var i = 0; i < children.length; i++) {
					var c = children[i];
					this._translateModel(c);
				}
			} else {
				model.children = [];
			}
			return model;
		},
		__buildServiceActionPage: function () {
			var activeLabel = this.getChildControl("activeServiceLabel");
			this.__serviceActionPage.add(activeLabel, {
				row: 0,
				column: 0
			});
			var serviceActive = this._getCheckbox("serviceActive");
			this.__serviceActionPage.add(serviceActive, {
				row: 0,
				column: 1
			});
			serviceActive.addListener("execute", function (e) {
				var c = e.getTarget();
				this.__value.serviceactive = c.getValue();
				this._fireChangeValue();
			}, this);


			var serviceLabel = this.getChildControl("serviceLabel");
			this.__serviceActionPage.add(serviceLabel, {
				row: 1,
				column: 0
			});
			var serviceInput = this.getChildControl("serviceInput");
			this.__serviceActionPage.add(serviceInput, {
				row: 1,
				column: 1
			});
			serviceInput.addListener("changeValue", function (e) {
				this.__value.servicecall = e.getData();
				this._fireChangeValue();
			}, this);

		},
		__buildProcessActionPage: function () {
			var activeLabel = this.getChildControl("activeProcessLabel");
			this.__processActionPage.add(activeLabel, {
				row: 0,
				column: 0
			});
			var processActive = this._getCheckbox("processActive");
			this.__processActionPage.add(processActive, {
				row: 0,
				column: 1
			});
			processActive.addListener("execute", function (e) {
				var c = e.getTarget();
				this.__value.processactive = c.getValue();
				this._fireChangeValue();
			}, this);


			var processLabel = this.getChildControl("processLabel");
			this.__processActionPage.add(processLabel, {
				row: 1,
				column: 0
			});
			var processInput = this.getChildControl("processInput");
			this.__processActionPage.add(processInput, {
				row: 1,
				column: 1
			});
			processInput.addListener("changeValue", function (e) {
				this.__value.processcall = e.getData();
				this._fireChangeValue();
			}, this);


			var startProcessLabel = this.getChildControl("startProcessLabel");
			this.__processActionPage.add(startProcessLabel, {
				row: 2,
				column: 0
			});
			var startProcessUser = this.getChildControl("startProcessUser");
			this.__processActionPage.add(startProcessUser, {
				row: 2,
				column: 1
			});
			startProcessUser.addListener("changeSelection", function (e) {
				this.__value.startProcessUser = e.getData()[0].getModel();
				this._fireChangeValue();
			}, this);
			startProcessUser.setSelection([this.__startProcessUserItem]);



		},
		__buildFieldActionPage: function () {
			var activeLabel = this.getChildControl("activeFieldLabel");
			this.__fieldActionPage.add(activeLabel, {
				row: 0,
				column: 0
			});
			var fieldActive = this._getCheckbox("fieldActive");
			this.__fieldActionPage.add(fieldActive, {
				row: 0,
				column: 1
			});
			fieldActive.addListener("execute", function (e) {
				var c = e.getTarget();
				this.__value.fieldactive = c.getValue();
				this._fireChangeValue();
			}, this);

			var typeLabel = this.getChildControl("typeLabel");
			this.__fieldActionPage.add(typeLabel, {
				row: 1,
				column: 0
			});
			var typeSelect = this.getChildControl("typeSelect");
			this.__fieldActionPage.add(typeSelect, {
				row: 1,
				column: 1
			});
			typeSelect.addListener("changeSelection", function (e) {
				var type = e.getData()[0].getModel();
				this.__value.action = type;
				if (type != "update-target") {
					this.__value.targetmodule = null;
				} else {
					this.__value.targetmodule = this.__targetmodulename;
				}
				this._fireChangeValue();
			}, this);
			typeSelect.setSelection([this.__updateTargetTypeItem]);

			var targetLabel = this.getChildControl("targetLabel");
			this.__fieldActionPage.add(targetLabel, {
				row: 2,
				column: 0
			});
			var targetSelect = this.getChildControl("targetSelect");
			targetSelect.addListener("changeValue", function (e) {
				var target = e.getData();
				this.__value.targetmodule = target;
				this._fireChangeValue();
			}, this);
			this.__fieldActionPage.add(targetSelect, {
				row: 2,
				column: 1
			});
			var fieldButton = this.getChildControl("fieldButton");
			fieldButton.addListener("execute", function (e) {
				console.log("targetmodule:" + this.__value.targetmodule);
				if (!this.__value.targetmodule) {
					var targetSelect = this.getChildControl("targetSelect");
					this.__value.targetmodule = targetSelect.getValue();
				}
				console.log("targetmodule:" + this.__value.targetmodule);
				if (this.__value.targetmodule == null) {
					ms123.form.Dialog.alert(this.tr("taction.no_target_set"));
					return;
				}
				var slash = this.__value.targetmodule.lastIndexOf("/");
				if (slash == -1) {
					slash = this.__value.targetmodule.lastIndexOf(".");
				}
				var modulename = ms123.util.Inflector.getModuleName(this.__value.targetmodule.substring(slash + 1)).toLowerCase();
				console.log("lastModuleName:" + this.__lastModuleName + "/" + modulename);
				if (this.__lastModuleName != modulename) {
					this.__createFieldSelectWindow(modulename);
				}
				if (this.__isEdit) {
					this.__setFields();
				}
				this.__fieldSelectWindow.setActive(true);
				this.__fieldSelectWindow.open();

				this.__lastModuleName = modulename;
			}, this);
			this.__fieldActionPage.add(fieldButton, {
				row: 3,
				column: 1
			});
			this.__changeTargetSelectModel("update-target", true);
		},
		__changeTargetSelectModel: function (type) {
			var targetSelect = this.getChildControl("targetSelect");
			switch (type) {
			case "update-target":
				targetSelect.setModel(this.__modelNull);
				targetSelect.setEnabled(false);
				break;
			case "update-related":
				if (!this.__isEdit) targetSelect.setEnabled(true);
				targetSelect.setModel(this.__modelOne);
				if (this.__value.targetmodule) {
					targetSelect.setValue(this.__value.targetmodule);
				} else {
					var selectables = targetSelect.getSelectables(true);
					targetSelect.setSelection([selectables[0]]);
				}
				break;
			case "create-record":
				if (!this.__isEdit) targetSelect.setEnabled(true);
				targetSelect.setModel(this.__modelColl);
				if (this.__value.targetmodule) {
					targetSelect.setValue(this.__value.targetmodule);
				} else {
					var selectables = targetSelect.getSelectables(true);
					targetSelect.setSelection([selectables[0]]);
				}
				break;
			}
		},
		__getFields: function () {
			var map = {};
			var hints = {};
			var m = this.__fieldSelectForm.form.getModel();
			var props = qx.Class.getProperties(m.constructor);
			var items = this.__fieldSelectForm.form.getItems();
			for (var i = 0, l = props.length; i < l; i++) {
				var p = props[i];
				var v = items[p].getUserData("useit").getValue();
				var useit = false;
				if (typeof v == "boolean") {
					useit = v;
				} else {
					useit = (v == "ignore") ? false : true;
				}
				if (useit) {
					var val = m.get(p);
					if (this.__fieldSelectForm.form.formData[p].type == "DateField") {
						val = this.__getDate(val);
					}
					map[p] = val;
					hints[p] = {
						useit: v
					};
				}
			}
			this.__value.fields = map;
			this.__value.hints = hints;
			return map;
		},
		__setFields: function () {
			var map = this.__value.fields;
			console.log("Map:" + JSON.stringify(map, null, 2));
			var hints = this.__value.hints;
			var model = this.__fieldSelectForm.form.getModel();
			var items = this.__fieldSelectForm.form.getItems();
			for (var p in map) {
				console.log("p:" + p);
				if (this.__fieldSelectForm.form.formData[p].type == "RelatedTo") {
					if (typeof map[p] == 'object') {
						var id = map[p]["id"];
						var name = map[p]["name"] || "";
						model.set(p, id + "/" + name);
					} else {
						model.set(p, map[p]);
					}
				} else if (this.__fieldSelectForm.form.formData[p].type == "DateField") {
					if (map[p] != "") {
						var d = new Date();
						d.setTime(map[p]);
						model.set(p, d);
					} else {
						model.set(p, null);
					}
				} else {
					model.set(p, map[p]);
				}
				var hint = hints[p];
				var useit = hint["useit"];
				console.log("p:" + p + " -> " + useit);
				items[p].getUserData("useit").setValue(useit);
			}
		},
		__getDate: function (str) {
			try {
				if (typeof str == 'object' && str.constructor == Date) {
					return str;
				}
				var d = moment(str);
				return isNaN(d) ? null : new Date(d);
			} catch (ex) {
				return null;
			}
		},
		__createFieldSelectWindow: function (module) {
			console.log("module:" + module);
			var app = qx.core.Init.getApplication();
			var root = app.getRoot();
			var win = new qx.ui.window.Window("", "").set({
				resizable: true,
				useMoveFrame: true,
				useResizeFrame: true
			});
			win.setLayout(new qx.ui.layout.Grow);
			win.setWidth(600);
			win.setHeight(370);
			win.setAllowMaximize(false);
			win.setAllowMinimize(false);
			win.setModal(true);
			win.setActive(false);
			win.minimize();
			win.center();
			root.add(win);
			var context = {};


			var _this = this;
			var buttons = [{
				'label': this.tr("tactions.uebernehmen"),
				'icon': "icon/16/actions/edit-copy.png",
				'callback': function (formData) {
					_this.__getFields();
					_this._fireChangeValue();
					_this.__fieldSelectWindow.close();
				},
				'value': "check"
			}];

			context.buttons = buttons;
			var sdesc = this.__storeDesc.getNamespaceDataStoreDesc();
			context.model = this.__configManager.getEntityModel(module, sdesc, "main-form", "properties");
			console.error("Module:" + module);
			context.useitCheckboxes = true;
			context.useitCheckboxesDefault = true;
			context.config = module;
			var form = new ms123.widgets.Form(context);
			win.add(form);
			this.__fieldSelectWindow = win;
			this.__fieldSelectForm = form;
		},
		_createChildControlImpl: function (id) {
			var control;

			switch (id) {
			case "typeSelect":
				control = new qx.ui.form.SelectBox();
				var item = new qx.ui.form.ListItem(this.tr("tactions.update-target"), null, "update-target");
				this.__updateTargetTypeItem = item;
				control.add(item);
				item = new qx.ui.form.ListItem(this.tr("tactions.update-related"), null, "update-related");
				control.add(item);
				item = new qx.ui.form.ListItem(this.tr("tactions.create-record"), null, "create-record");
				control.add(item);
				break;
			case "typeLabel":
				control = new qx.ui.basic.Label(this.tr("tactions.type"));
				break
			case "targetLabel":
				control = new qx.ui.basic.Label(this.tr("tactions.targetmodule"));
				break
			case "targetSelect":
				control = new ms123.form.TreeSelectBox("data");
				control.setWidth(500);
				control.addListener("changeSelection", function (e) {}, this);
				break
			case "fieldButton":
				control = new qx.ui.form.Button(this.tr("tactions.editfields"), "icon/16/apps/utilities-text-editor.png");
				break
			case "serviceLabel":
				control = new qx.ui.basic.Label(this.tr("tactions.servicelabel"));
				break
			case "serviceInput":
				control = new qx.ui.form.TextField();
				break
			case "activeFieldLabel":
				control = new qx.ui.basic.Label(this.tr("tactions.active"));
				break
			case "activeServiceLabel":
				control = new qx.ui.basic.Label(this.tr("tactions.active"));
				break
			case "processLabel":
				control = new qx.ui.basic.Label(this.tr("tactions.processlabel"));
				break
			case "processInput":
				control = new qx.ui.form.TextField();
				break
			case "activeProcessLabel":
				control = new qx.ui.basic.Label(this.tr("tactions.active"));
				break
			case "startProcessLabel":
				control = new qx.ui.basic.Label(this.tr("tactions.startProcessUser"));
				break
			case "startProcessUser":
				control = new qx.ui.form.SelectBox();
				var item = new qx.ui.form.ListItem(this.tr("tactions.process.admin"), null, "admin");
				this.__startProcessUserItem = item;
				control.add(item);
				item = new qx.ui.form.ListItem(this.tr("tactions.process.callinguser"), null, "user");
				control.add(item);
				break;
			case "tabView":
				control = new qx.ui.tabview.TabView().set({
					contentPadding: 0
				});
				var page = new qx.ui.tabview.Page(this.tr("tactions.fieldaction")).set({
					showCloseButton: false
				});
				var layout = new qx.ui.layout.Grid();
				layout.setSpacing(3);
				layout.setColumnAlign(0, "right", "middle");
				layout.setColumnFlex(0, 0);
				layout.setColumnWidth(0, 100);
				layout.setColumnMaxWidth(0, 100);
				layout.setColumnFlex(1, 1);
				page.setLayout(layout);
				this.__fieldActionPage = page;
				control.add(page);

				page = new qx.ui.tabview.Page(this.tr("tactions.serviceaction")).set({
					showCloseButton: false
				});
				var layout = new qx.ui.layout.Grid();
				layout.setSpacing(3);
				layout.setColumnAlign(0, "right", "middle");
				layout.setColumnFlex(0, 0);
				layout.setColumnWidth(0, 100);
				layout.setColumnMaxWidth(0, 100);
				layout.setColumnFlex(1, 1);
				page.setLayout(layout);
				this.__serviceActionPage = page;
				control.add(page);

				page = new qx.ui.tabview.Page(this.tr("tactions.processaction")).set({
					showCloseButton: false
				});
				var layout = new qx.ui.layout.Grid();
				layout.setSpacing(3);
				layout.setColumnAlign(0, "right", "middle");
				layout.setColumnFlex(0, 0);
				layout.setColumnWidth(0, 100);
				layout.setColumnMaxWidth(0, 100);
				layout.setColumnFlex(1, 1);
				page.setLayout(layout);
				this.__processActionPage = page;
				control.add(page);
				break
			}

			return control || this.base(arguments, id);
		},
		_getCheckbox: function (id) {
			if (!this._checkboxes) this._checkboxes = {};
			if (!this._checkboxes[id]) {
				this._checkboxes[id] = new qx.ui.form.CheckBox();
				this._checkboxes[id].setValue(false);
			}
			return this._checkboxes[id];
		},
		_fireChangeValue: function () {
			var value = this.getValue();
			this.fireDataEvent("changeValue", value, null);
		}
	}
});
