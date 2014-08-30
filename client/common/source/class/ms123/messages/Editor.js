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
	@ignore($)
	@asset(qx/icon/${qx.icontheme}/16/actions/*)
	@asset(qx/icon/${qx.icontheme}/16/places/*)
*/

qx.Class.define("ms123.messages.Editor", {
	extend: ms123.util.TableEdit,

	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (model, param, facade) {
		this._facade = facade;
		this._model = model;
		this._lang = model.getId();
		this.base(arguments, facade);
	},

	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		__searchFilter: '',
		_createColumnModel: function () {
			var columnmodel = [{
				name: "msgid",
				width: 60,
				type: "TextField",
				label: "%messages.msgid"
			},
			{
				name: "msgstr",
				width: 80,
				type: "TextField",
				label: "%messages.msgstr"
			}];
			this._columnModel = this._translate(columnmodel);
			return this._columnModel;
		},
		_views: {
			"All": {},
			"SearchAsYouType": {
				// All rows matching search string are visible
				filters: function (rowdata) {
					var d0 = rowdata[0].toLowerCase();
					var d1 = rowdata[1].toLowerCase();
					return ((d0.indexOf(this.__searchFilter) != -1) || (d1.indexOf(this.__searchFilter) != -1));
				}
			}
		},
		_doLayout: function (table, columnmodel) {
			this.base(arguments, table, columnmodel);
			var sp = this._createSearchPanel();
			this.add(sp, {
				edge: "north"
			});
		},
		_createSearchPanel: function () {
			var container = new qx.ui.container.Composite(new qx.ui.layout.HBox()).set({
				paddingBottom: 4
			});
			container.add(new qx.ui.basic.Label(this.tr("messages.filter") + ":").set({
				textColor: '#4886ce'
			}));

			var sf = new qx.ui.form.TextField();
			sf.addListener('keyup', function (e) {
				this.__searchFilter = sf.getValue().toLowerCase();
				this._tableModel.updateView(this._views["SearchAsYouType"].id);
				this._tableModel.setView(this._views["SearchAsYouType"].id);
			}, this);
			container.add(sf);
			return container;
		},
		_createToolbar: function () {
			var toolbar = new qx.ui.toolbar.ToolBar();

			this._buttonAdd = new qx.ui.toolbar.Button("", "icon/16/actions/list-add.png");
			this._buttonAdd.addListener("execute", function () {

				this._isEditMode = false;
				this._table.stopEditing();
				if (this._currentForm) {
					this._propertyEditWindow.remove(this._currentForm);
				}
				this._currentForm = this._createAddForm();
				this._currentForm.fillForm({});
				this._propertyEditWindow.add(this._currentForm);
				this._propertyEditWindow.setActive(true);
				this._propertyEditWindow.open();

			}, this);
			this._buttonAdd.setEnabled(true);
			toolbar._add(this._buttonAdd);

			this._buttonDel = new qx.ui.toolbar.Button("", "icon/16/actions/list-remove.png");
			this._buttonDel.addListener("execute", function () {
				this._deleteRecordAtPos(this._currentTableIndex);
			}, this);
			toolbar._add(this._buttonDel);
			this._buttonDel.setEnabled(false);

			toolbar.setSpacing(5);
			toolbar.addSpacer();

			toolbar.add(new qx.ui.core.Spacer(), {
				flex: 1
			});
			this._buttonSave = new qx.ui.toolbar.Button(this.tr("meta.lists.savebutton"), "icon/16/actions/document-save.png");
			this._buttonSave.addListener("execute", function () {
				this._save();
			}, this);
			toolbar._add(this._buttonSave);
			return toolbar;
		},
		_createTableModel: function () {
			var tm = new ms123.widgets.smart.model.Default();
			var id = 0;
			for (var view in this._views) {
				if (view == 'All') {
					this._views[view].id = 0;
					continue;
				}
				this._views[view].id = ++id;
				tm.addView(this._views[view].filters, this, this._views[view].conjunction);
			}
			return tm;
		},
		_createPropertyEdit: function (tableColumns) {
			this._propertyEditWindow = this._createPropertyEditWindow();
		},
		_createTableListener: function (table) {
			this._tableModel = table.getTableModel();
			var selModel = table.getSelectionModel();
			selModel.setSelectionMode(qx.ui.table.selection.Model.SINGLE_SELECTION);
			selModel.addListener("changeSelection", function (e) {
				var index = selModel.getLeadSelectionIndex();
				var map = this._tableModel.getRowDataAsMap(index);
				var count = selModel.getSelectedCount();
				if (count == 0) {
					if (this._buttonEdit) this._buttonEdit.setEnabled(false);
					if (this._buttonDel) this._buttonDel.setEnabled(false);
					return;
				}
				this._currentTableIndex = index;
				if (this._buttonEdit) this._buttonEdit.setEnabled(true);
				if (this._buttonDel) this._buttonDel.setEnabled(true);
			}, this);

			this._tableModel.addListener("dataChanged", function (event) {
				if (!this._loaded ) {
					return;
				}
				var index = selModel.getLeadSelectionIndex();
				var map = this._tableModel.getRowDataAsMap(index);
				this._messagesChange(map);
			}, this);
		},
		_createAddForm: function () {
			var formData = {};
			for (var i = 0; i < this._columnModel.length; i++) {
				var col = this._columnModel[i];
				formData[col.name] = col;
			}
			var self = this;
			var buttons = [{
				'label': this.tr("messages.takeover"),
				'icon': "icon/22/actions/dialog-ok.png",
				'callback': function (m) {
					var map = {};
					qx.lang.Object.mergeWith(map, m);
					self._messagesChange(map);
					if (self._isEditMode) {
						self._tableModel.setRowsAsMapArray([m], self._currentTableIndex, true);
						self._propertyEditWindow.close();
					} else {
						self._tableModel.addRowsAsMapArray([m], null, true);
						self._currentForm.fillForm({});
					}
				},
				'value': "save"
			}];

			var context = {};
			context.formData = formData;
			context.buttons = buttons;
			context.formLayout = [{
				id: "tab1"
			}];
			return new ms123.widgets.Form(context);
		},
		_messagesChange: function (rec) {
			if( rec === null || rec === undefined ) return;
			var map = {};
			map[rec.msgid] = rec.msgstr;
			var m = qx.locale.Manager.getInstance();
			var locale = m.getLocale();
			qx.locale.Manager.getInstance().addTranslation(locale, map);
			qx.locale.Manager.getInstance().setLocale("en");
			qx.locale.Manager.getInstance().setLocale("de");
			qx.locale.Manager.getInstance().setLocale(locale);
		},
		_saveMessages: function (data) {
			var completed = (function (data) {
				ms123.form.Dialog.alert(this.tr("messages.messages_saved"));
			}).bind(this);

			var failed = (function (details) {
				ms123.form.Dialog.alert(this.tr("messages.saveMessages_failed") + ":" + details.message);
			}).bind(this);

			try {
				var storeId = this._facade.storeDesc.getStoreId();
				var ret = ms123.util.Remote.rpcSync("message:saveMessages", {
					namespace: this._facade.storeDesc.getNamespace(),
					lang: this._lang,
					msgList: data
				});
				completed.call(this, ret);
			} catch (e) {
				failed.call(this, e);
				return;
			}
		},
		_getMessages: function () {
			var completed = (function (data) {}).bind(this);

			var failed = (function (details) {
				ms123.form.Dialog.alert(this.tr("messages.getMessages_failed") + ":" + details.message);
			}).bind(this);

			try {
				var ret = ms123.util.Remote.rpcSync("message:getMessages", {
					namespace: this._facade.storeDesc.getNamespace(),
					lang: this._lang
				});
				completed.call(this, ret);
				return ret;
			} catch (e) {
				return [];
			}
		},
		_getRecords: function () {
			this._table.stopEditing();
			var rc = this._tableModel.getRowCount(0);
			var records = [];
			var cm = this._createColumnModel();
			var cc = cm.length;
			for (var i = 0; i < rc; i++) {
				var _rd = this._tableModel.getRowData(i, 0);
				var rd = {};
				for (var c = 0; c < cc; c++) {
					rd[cm[c].name] = _rd[c];
				}
				records.push(rd);
			}
			return records;
		},
		_save: function () {
			this._saveMessages(this._getRecords());
		},
		_load: function () {
			return this._getMessages();
		}
	}
});
