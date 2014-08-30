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
 @asset(qx/icon/${qx.icontheme}/22/actions/*)
 @asset(qx/icon/${qx.icontheme}/16/apps/*)
 @asset(ms123/icons/*)
 @asset(ms123/*)
 */

qx.Class.define("ms123.widgets.DupCandidateEdit", {
	extend: qx.core.Object,
	include: qx.locale.MTranslation,

	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (context) {
		this.base(arguments);
		this._context = context;
		this._entityName = context.entityName;
		this.__storeDesc = context.storeDesc;
		this.__configManager = new ms123.config.ConfigManager();
		this._init();
	},


	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},


	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		_init: function () {
			this._win = this._createWindow("DublettenCheck");
			var sp = this._splitPane();
			this._win.add(sp);
			this._createDupForm(this._context.entityName);
			this._createRefForm(this._context.entityName);
			var dupLabel = new qx.ui.basic.Label(this.tr("quality.dup_candidate")).set({
				rich: true
			});
			var refLabel = new qx.ui.basic.Label(this.tr("quality.ref_record")).set({
				rich: true
			});
			this._upperWidget.add(dupLabel, {
				edge: "north"
			});
			this._bottomWidget.add(refLabel, {
				edge: "north"
			});
			this._dupForm.form.addListener("tabChanged", function (e) {
				var tabid = e.getData().tabid;
				this._refForm.form.selectTab(tabid);
			}, this);
			this._refForm.form.addListener("tabChanged", function (e) {
				var tabid = e.getData().tabid;
				this._dupForm.form.selectTab(tabid);
			}, this);
		},
		open: function (data) {
			this._dupData = data;
			this._dupForm.fillForm(data);
			this._fillRefForm(data);
			this._win.setActive(true);
			this._win.open();
		},
		_fillRefForm:function(data){
			var refid = data["_dup_refid"];
			if (refid) {
				data = this._getRef(refid) || {};
				this._refData = data;
				this._refForm.fillForm(data);
			}
		},
		_getRef: function (id) {
			try {
				var result = ms123.util.Remote.rpcSync("data:queryOne", {
					storeId: this.__storeDesc.getStoreId(),
					entity: this._entityName,
					id: id
				});
				return result;
			} catch (e) {
				ms123.form.Dialog.alert("DupCandidateEdit._getRef:" + e);
				return [];
			}
		},
		_createRefForm: function (entityName) {
			var self = this;
			var buttons = [{
				'label': this.tr("tasks.usertasks.dublettencheck.save"),
				'icon': "icon/22/actions/dialog-ok.png",
				'callback': function (m) {
					var map = {};
					ms123.util.Clone.merge(map, self._refData, m);
					self._save(self._refForm, map);
				},
				'value': "save"
			}];

			var context = {};
			context.buttons = buttons;
			context.model = this.__configManager.getEntityModel(entityName, this.__storeDesc, "main-form", "properties");
			context.config = entityName;
			this._refForm = new ms123.widgets.Form(context);
			this._bottomWidget.add(this._refForm, {
				edge: "center"
			});
		},
		_createDupForm: function (entityName) {
			var self = this;
			var tableModel = this._context.table.tableModel;
			var table = this._context.table;
			var buttons = [{
				'label': "",
				'icon': "icon/22/actions/go-previous.png",
				'callback': function (m) {
					var rc = tableModel.getRowCount();
					if (table._currentIndex > 0) {
						table._currentIndex--;
						var map = table._currentRowData = tableModel.getRowDataAsMap(table._currentIndex);
						var selModel = table.table.getSelectionModel();
						selModel._resetSelection();
						table.table.setFocusedCell(0, table._currentIndex, true);
						selModel.setSelectionInterval(table._currentIndex, table._currentIndex);
						table.__fillForm(self._dupForm, map);
						self._fillRefForm(map);
						self._dupData = map;
					}
					self._setPrevNextButtons(true);
				},
				'value': "prev"
			},
			{
				'label': "",
				'icon': "icon/22/actions/go-next.png",
				'callback': function (m) {
					var rc = tableModel.getRowCount();
					if (table._currentIndex < (rc - 1)) {
						table._currentIndex++;
						var map = table._currentRowData = tableModel.getRowDataAsMap(table._currentIndex);
						var selModel = table.table.getSelectionModel();
						selModel._resetSelection();
						selModel.setSelectionInterval(table._currentIndex, table._currentIndex);
						table.table.setFocusedCell(0, table._currentIndex, true);
						table.__fillForm(self._dupForm, map);
						self._fillRefForm(map);
						self._dupData = map;
					}
					self._setPrevNextButtons(true);
				},
				'value': "next"
			},
			{
				'label': this.tr("tasks.usertasks.dublettencheck.checkit"),
				'icon': "icon/22/actions/edit-redo.png",
				'callback': function (formData) {
					if(!self._validateForm(self._dupForm)) return;
					var map = {};
					ms123.util.Clone.merge(map, self._dupData, formData);
					var result = self._checkData(map);
					var cv = null;
					var first=null;
					if (result.length > 0) {
						first = result[0];
						cv = result[0].cvList;
					}
					if (cv) {
						var message = self.tr("quality.maybe_exists")+"<br />";
						for (var i = 0; i < cv.length; i++) {
							var c = cv[i];
							var msg = c.messageRef || '';
							message += this.getLabel(c.path) + " : " + msg + "<br />";
						}
						if( first.refid){
							message += this.getLabel("Id") + " : " + first.refid + "<br />";
						}
						ms123.form.Dialog.alert(message);
						this.setErrors(cv);
					} else {
						ms123.form.Dialog.alert("Ok");
					}
				},
				'value': "check"
			},
			{
				'label': this.tr("tasks.usertasks.dublettencheck.save"),
				'icon': "icon/22/actions/dialog-ok.png",
				'callback': function (m) {
					var map = {};
					ms123.util.Clone.merge(map, self._dupData, m);
					self._save(self._dupForm, map);
				},
				'value': "save"
			}];

			var context = {};
			context.buttons = buttons;
			context.model = this.__configManager.getEntityModel(entityName, this.__storeDesc, "main-form", "properties");
			context.config = entityName;
			this._dupForm = new ms123.widgets.Form(context);

			for (var i = 0; i < context.buttons.length; i++) {
				var b = context.buttons[i];
				if (b.value == 'prev') {
					this._prevButton = b.button;
				}
				if (b.value == 'next') {
					this._nextButton = b.button;
				}
			}
			this._upperWidget.add(this._dupForm, {
				edge: "center"
			});
		},
		_validateForm:function(form){
			var validate = form.validate();
			if (!validate) {
				var vm = form._form.getValidationManager();
				var items = vm.getInvalidFormItems();
				var message = "<br />";
				for (var i = 0; i < items.length; i++) {
					var name = items[i].getUserData("key");
					var msg = items[i].getInvalidMessage();
					message += name + " : " + msg + "<br />";
				}
				ms123.form.Dialog.alert(this.tr("widgets.table.form_incomplete") + ":" + message);
			}
			return validate;
		},
		_checkData: function (data) {
			try {
				var result = ms123.util.Remote.rpcSync("quality:dupCheck", {
					namespace: this.__storeDesc.getNamespace(),
					entityName: this._entityName,
					candidateList: [data]
				});
				console.log("Result:" + JSON.stringify(result, null, 2));
				return result;
			} catch (e) {
				ms123.form.Dialog.alert("DupCandidateEdit._checkData:" + e);
				return [];
			}
		},
		_save: function (form, data) {
			if( !this._validateForm(form)) return;
			var self = this;
			var id = data[this._context.table._keyColumn];
			var params = this._context.table._buildBaseRpcParams(false, id);;
			params.data = data;

			var completed = function (e) {
				var content = e;
				if (typeof content == "string") {
					ms123.form.Dialog.alert(message);
				}

				var cv = e["constraintViolations"];
				if (cv) {
					var message = "";
					if (cv.length > 0 && cv[0].idHitList) {
						message = self.tr("data.record_exists") + "<br />";
					}
					for (var i = 0; i < cv.length; i++) {
						var c = cv[i];
						message += this.getLabel(c.path) + " : " + c.message + "<br />";
					}
					ms123.form.Dialog.alert(message);
					this.setErrors(cv);
					return;
				}
				ms123.form.Dialog.alert(self.tr("data" + ".form.saved"));
				this.setAllValid();
			};
			try {
				var ret = this._context.table.__dataAccess.update(params);
				completed.call(form, ret);
			} catch (e) {
				console.log(e.stack);
				ms123.form.Dialog.alert(e.message);
			}
		},
		_setPrevNextButtons: function (enabled) {
			var table = this._context.table;
			var rc = table.tableModel.getRowCount();
			this._prevButton.setEnabled(enabled);
			this._nextButton.setEnabled(enabled);
			if (table._currentIndex == 0) {
				this._prevButton.setEnabled(false);
			}
			if (table._currentIndex == (rc - 1)) {
				this._nextButton.setEnabled(false);
			}
		},
		_splitPane: function (parent) {
			var splitpane = new qx.ui.splitpane.Pane("vertical");
			splitpane.setDecorator("main");

			var upperWidget = new qx.ui.container.Composite();
			upperWidget.setLayout(new qx.ui.layout.Dock());
			upperWidget.setDecorator(null);
			splitpane.add(upperWidget, 2);
			this._upperWidget = upperWidget;

			var bottomWidget = new qx.ui.container.Composite();
			bottomWidget.setLayout(new qx.ui.layout.Dock());
			bottomWidget.setDecorator(null);
			splitpane.add(bottomWidget, 2);
			this._bottomWidget = bottomWidget;

			return splitpane;
		},
		_createWindow: function (name) {
			var win = new qx.ui.window.Window(this._context.title, "").set({
				resizable: true,
				useMoveFrame: true,
				useResizeFrame: true
			});
			win.setLayout(new qx.ui.layout.Grow);
			win.setWidth(750);
			win.setHeight(700);
			win.setAllowMaximize(false);
			win.setAllowMinimize(false);
			var props = this.__configManager.getEntityViewProperties(this._context.entityName, this.__storeDesc, "main-form");
			var modal = true;
			if (props.modal === false) modal = false;
			win.setModal(modal);
			win.setActive(false);
			win.minimize();
			win.center();
			this._context.table.getApplicationRoot().add(win);
			return win;
		}
	}
});
