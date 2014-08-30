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
 */
qx.Class.define('ms123.management.UnitToolbar', {
	extend: qx.core.Object,
	implement: ms123.widgets.IToolbar,
	include: [qx.locale.MTranslation],


	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (context) {
		this.base(arguments);
		this._idMaster = null;
		this._toolbar = this._createToolbar();
		this._createButtons();
	},


	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},


	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		/**
		 */
		getToolbar: function (p) {
			return this._toolbar;
		},
		selectionChanged: function (table) {
			if (this._idMaster == null) return;
			this._table = table;
			var selModel = table.getSelectionModel();
			var count = selModel.getSelectedCount();
			var index = selModel.getLeadSelectionIndex();
			this._selected = index;
			var tableModel = table.getTableModel();
			var map = tableModel.getRowDataAsMap(index);
			this._data = map;
			this._buttonDel.setEnabled(true);
			console.log("map:" + qx.util.Serializer.toJson(map));
		},
		setDefaultButtons: function (buttonMap) {
			var toolbar = this._toolbar;
			var menuPart1 = new qx.ui.toolbar.Part;
			var menuPart2 = new qx.ui.toolbar.Part;
			toolbar.add(menuPart1);
			toolbar.addSpacer();
			toolbar.add(menuPart2);
			toolbar.addSpacer();
			var menuPart3 = new qx.ui.toolbar.Part;
			toolbar.add(menuPart3);
			menuPart3.add(buttonMap["first"]);
			menuPart3.add(buttonMap["prev"]);
			menuPart3.add(buttonMap["next"]);
			menuPart3.add(buttonMap["last"]);
			var _status = new qx.ui.basic.Label("").set({});
			menuPart3.add(_status);
		},
		setParentData: function (masterid, parentData) {
			console.log("setParentData:" + masterid);
			this._idMaster = masterid;
			this._buttonAdd.setEnabled(true);
		},
		_hideWait: function () {
			this._waitdia.hide();
		},
		_showWait: function () {
			this._waitdia = new ms123.form.Alert({
				"message": "<h2>"+this.tr("management.please_wait")+"</h2>",
				"noOkButton": true,
				"inWindow": true,
				"hide": false,
				"context": this
			});
			this._waitdia.show();
		},
		_createUnit: function (data) {
			self = this;
			var completed = function (ret) {
				self._hideWait();
				ms123.form.Dialog.alert(self.tr("management.unit_created") + ':<b><a target="_blank" href="' + ret + '">'+ret+'</a></b>');
			}
			var failed = function (e) {
				self._hideWait();
				if( e && e.toString().indexOf("name_exists") != -1){
					ms123.form.Dialog.alert(self.tr("management.unit_exists")+":"+data.name);
				}else{
					ms123.form.Dialog.alert("UnitToolbar:_createUnit:" + e);
				}
			}
			var timeout = function (e) {
				self._hideWait();
				ms123.form.Dialog.alert("UnitToolbar:_createUnit.timeout:" + e);
			}

			var rpcParams = {
				customerId: this._idMaster,
				data: data
			}

			var params = {
				service: "management",
				method: "createUnit",
				parameter: rpcParams,
				failed: failed,
				completed: completed,
				timeout: timeout,
				async: true
			}
			this._showWait();
			ms123.util.Remote.rpcAsync(params);
		},
		_destroyUnit: function (unitId, name) {
			self = this;
			var completed = function (ret) {
				self._hideWait();
				ms123.form.Dialog.alert(self.tr("management.unit_destroyed"));
				self._table.getTableModel().removeRows(self._selected, 1);
			}
			var failed = function (e) {
				self._hideWait();
				ms123.form.Dialog.alert("UnitToolbar:_destroyUnit:" + e);
			}
			var rpcParams = {
				customerId: this._idMaster,
				unitId: unitId,
				name: name
			}

			var params = {
				service: "management",
				method: "destroyUnit",
				parameter: rpcParams,
				failed: failed,
				completed: completed,
				async: true
			}
			this._showWait();
			ms123.util.Remote.rpcAsync(params);
		},
		_showForm: function () {
			var formData = {
				"name": {
					'type': "TextField",
					'label': this.tr("management.unitname"),
					'validation': {
						required: true,
						validator: "/^[A-Za-z]([0-9A-Za-z-]){2,32}$/"
					},
					'value': ""
				},
				"usage": {
					'type': "SelectBox",
					'label': this.tr("data.unit.usage"),
					'value': "runtime",
					'options': [{
						value: "developement",
						label: this.tr("management.developement")
					},
					{
						value: "runtime",
						label: this.tr("management.runtime")
					}]
				},
				"githost": {
					'type': "ComboBox",
					'label': this.tr("management.githost"),
					'options': [{
						value: "swdemostore.ms123.org",
						label: "swdemostore.ms123.org"
					},
					{
						value: "swstore.ms123.org",
						label: "swstore.ms123.org"
					}],
					'value': "swdemostore.ms123.org"
				}
			};
			var form = new ms123.form.Form({
				"formData": formData,
				"allowCancel": true,
				"inWindow": true,
				"tabs": [{
					"id": "tab1"
				}],
				"message": "                          ",
				"windowWidth": 400,
				"callback": function (m) {
					if (m !== undefined) {
						this._createUnit({
							githost: m.get("githost"),
							usage: m.get("usage"),
							name: m.get("name")
						});
					}
				},
				"context": this
			});
			form.show();
		},
		_createButtons: function () {
			this._buttonAdd = this._createButton("", "icon/16/actions/list-add.png");
			this._buttonAdd.addListener("execute", function () {
				this._showForm();
			}, this);
			this._buttonAdd.setEnabled(false);
			this._toolbar.add(this._buttonAdd);

			this._buttonDel = this._createButton("", "icon/16/actions/list-remove.png");
			this._buttonDel.addListener("execute", function () {
				ms123.form.Dialog.confirm(this.tr("management.confirm_unit_destroy") + this._data["name"], function (e) {
					if (e) {
						this._destroyUnit(this._data["id"], this._data["name"]);
					}
				}, this);
			}, this);
			this._toolbar.add(this._buttonDel);
			this._buttonDel.setEnabled(false);
		},
		_createButton: function (text, icon) {
			var b = new qx.ui.toolbar.Button(text, icon);
			b.setPaddingTop(1);
			b.setPaddingBottom(1);
			return b;
		},
		_createToolbar: function () {
			var tb = new qx.ui.toolbar.ToolBar().set({});
			tb.setSpacing(5);
			return tb;
		}
	}
});
