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

qx.Class.define("ms123.entitytypes.RelationsEdit", {
	extend: ms123.util.TableEdit,

	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (model, param, facade) {
		this._facade = facade;
		this._model = model;
		this._createEntitytypeList();
		this._createRelationList();
		this.base(arguments, facade);
	},

	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		_createColumnModel: function () {
			var columnmodel = [{
				name: "leftmodule",
				type: "SelectBox",
				width: 80,
				options: this._entitytypeList,
				label: "%entitytypes.leftmodule"
			},
			{
				name: "leftfield",
				width: 60,
				type: "TextField",
				label: "%entitytypes.leftfield"
			},
			{
				name: "relation",
				type: "SelectBox",
				width: 120,
				options: this._relationList,
				label: "%entitytypes.relation"
			},
			{
				name: "rightmodule",
				type: "SelectBox",
				width: 60,
				options: this._entitytypeList,
				label: "%entitytypes.rightmodule"
			},
			{
				name: "rightfield",
				width: 80,
				type: "TextField",
				label: "%entitytypes.rightfield"
			},
			{
				name: "dependent",
				type: "CheckBox",
				width: 10,
				label: "%entitytypes.dependent"
			}];
			this._columnModel = this._translate(columnmodel);
			return this._columnModel;
		},
		_createToolbar: function () {
			var toolbar = new qx.ui.toolbar.ToolBar();

			var buttonUpdateDb = new qx.ui.toolbar.Button(this.tr("entitytypes.update_db"), "icon/16/actions/object-rotate-right.png");
			buttonUpdateDb.addListener("execute", function () {
				try{
					ms123.util.Remote.rpcSync("domainobjects:createClasses", {
						storeId: this._getStoreId()
					});
					ms123.form.Dialog.alert(this.tr("entitytypes.update_db_successfull"));
				}catch(e){
					ms123.form.Dialog.alert("RelationsEdit.updateDb:"+e);
					return;
				}
			}, this);
			toolbar._add(buttonUpdateDb);
			toolbar.addSpacer();

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
				this._save();
				this._reload();
			}, this);
			toolbar._add(this._buttonDel);
			this._buttonDel.setEnabled(false);

			toolbar.setSpacing(5);
			toolbar.addSpacer();

			this._buttonUp = new qx.ui.toolbar.Button("", "icon/16/actions/go-up.png");
			this._buttonUp.setToolTipText(this.tr("meta.lists.fs.up"));
			this._buttonUp.addListener("execute", function () {
				if (this._currentTableIndex == 0) return;
				var curRecord = this._getRecordAtPos(this._currentTableIndex);
				this._deleteRecordAtPos(this._currentTableIndex);
				this._insertRecordAtPos(curRecord, this._currentTableIndex - 1);
				var selModel = this._table.getSelectionModel();
				selModel.setSelectionInterval(this._currentTableIndex - 1, this._currentTableIndex - 1);
			}, this);
			toolbar._add(this._buttonUp);
			this._buttonUp.setEnabled(false);

			this._buttonDown = new qx.ui.toolbar.Button("", "icon/16/actions/go-down.png");
			this._buttonDown.setToolTipText(this.tr("meta.lists.fs.down"));
			this._buttonDown.addListener("execute", function () {
				var rc = this._tableModel.getRowCount();
				if (this._currentTableIndex >= (rc - 1)) return;
				var curRecord = this._getRecordAtPos(this._currentTableIndex);
				this._deleteRecordAtPos(this._currentTableIndex);
				this._insertRecordAtPos(curRecord, this._currentTableIndex + 1);
				var selModel = this._table.getSelectionModel();
				selModel.setSelectionInterval(this._currentTableIndex + 1, this._currentTableIndex + 1);
			}, this);
			toolbar._add(this._buttonDown);
			this._buttonDown.setEnabled(false);

			toolbar.add(new qx.ui.core.Spacer(), {
				flex: 1
			});
			this._buttonSave = new qx.ui.toolbar.Button(this.tr("meta.lists.savebutton"), "icon/16/actions/document-save.png");
			this._buttonSave.addListener("execute", function () {
				this._save();
				this._reload();
			}, this);
			this._buttonSave.setEnabled(false);
			toolbar._add(this._buttonSave);
			return toolbar;
		},
		_createPropertyEdit: function (tableColumns) {
			this._propertyEditWindow = this._createPropertyEditWindow();
		},
		_createTableListener: function (table) {
			this._tableModel = table.getTableModel();
			//table.addListener("dblclick", this._onDblClick, this);
			var selModel = table.getSelectionModel();
			selModel.setSelectionMode(qx.ui.table.selection.Model.SINGLE_SELECTION);
			selModel.addListener("changeSelection", function (e) {
				var index = selModel.getLeadSelectionIndex();
				var map = this._tableModel.getRowDataAsMap(index);
				var count = selModel.getSelectedCount();
				if (count == 0) {
					if (this._buttonUp) this._buttonUp.setEnabled(false);
					if (this._buttonDown) this._buttonDown.setEnabled(false);
					if (this._buttonEdit) this._buttonEdit.setEnabled(false);
					if (this._buttonSave) this._buttonSave.setEnabled(false);
					if (this._buttonDel) this._buttonDel.setEnabled(false);
					return;
				}
				this._currentTableIndex = index;
				if (this._buttonUp) this._buttonUp.setEnabled(true);
				if (this._buttonDown) this._buttonDown.setEnabled(true);
				if (this._buttonEdit) this._buttonEdit.setEnabled(true);
				if (this._buttonSave) this._buttonSave.setEnabled(true);
				if (this._buttonDel) this._buttonDel.setEnabled(true);
			}, this);
		},
		_onDblClick: function (e) {
			var selModel = this._table.getSelectionModel();
			var index = selModel.getLeadSelectionIndex();
			if (index < 0) return;
			var map = this._tableModel.getRowDataAsMap(index);
			console.log("_onDblClick:" + map.name);
			var context = {
				config: "projectshell",
				name: map.name,
				rootNode: "/"
			};
			new ms123.DesktopWindow(context, ms123.shell.ProjectShell);
		},
		_createAddForm: function () {
			var formData = {};
			for( var i=0; i <this._columnModel.length;i++){
				var col = this._columnModel[i];
				formData[col.name] = col;
			}
			var self = this;
			var buttons = [{
				'label': this.tr("entitytypes.relations_save"),
				'icon': "icon/22/actions/dialog-ok.png",
				'callback': function (m) {
					//self._reload();
					var map = {};
					qx.lang.Object.mergeWith(map, m);
					if( self._isEditMode ){
						self._tableModel.setRowsAsMapArray([m], self._currentTableIndex, true);
					}else{
						self._tableModel.addRowsAsMapArray([m], null, true);
					}
					self._save();
					self._reload();
					self._propertyEditWindow.close();
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
		_createEntitytypeList: function () {
			this._entitytypeList = this._getEntitytypes();
		},
		_createRelationList: function () {
			this._relationList = [{
				"value": "one-to-one",
				"label": "%relations.one-to-one"
			},
			{
				"value": "one-to-one-bi",
				"label": "%relations.one-to-one-bi"
			},
			{
				"value": "one-to-many",
				"label": "%relations.one-to-many"
			},
			{
				"value": "one-to-many-map",
				"label": "%relations.one-to-many-map"
			},
			{
				"value": "one-to-many-bi",
				"label": "%relations.one-to-many-bi"
			},
			{
				"value": "many-to-many",
				"label": "%relations.many-to-many"
			}];

		},
		_getStoreId:function(){
			var storeId = this._facade.storeDesc.getStoreId();
			return storeId;
		},
		_saveRelations: function (data) {
			var completed = (function (data) {
				ms123.form.Dialog.alert(this.tr("entitytypes.relations_saved"));
			}).bind(this);

			var failed = (function (details) {
				ms123.form.Dialog.alert(this.tr("entitytypes.saveRelations_failed") + ":" + details.message);
			}).bind(this);

			try {
				var storeId = this._facade.storeDesc.getStoreId();
				var ret = ms123.util.Remote.rpcSync("entity:saveRelations", {
					storeId: this._getStoreId(),
					relations: data
				});
				completed.call(this, ret);
				ms123.config.ConfigManager.clearCache();
			} catch (e) {
				failed.call(this, e);
				return;
			}
		},
		_getRelations: function () {
			var completed = (function (data) {}).bind(this);

			var failed = (function (details) {
				ms123.form.Dialog.alert(this.tr("entitytypes.getRelations_failed") + ":" + details.message);
			}).bind(this);

			try {
				var storeId = this._facade.storeDesc.getStoreId();
				var ret = ms123.util.Remote.rpcSync("entity:getRelations", {
					storeId: this._getStoreId()
				});
				completed.call(this, ret);
				return ret;
			} catch (e) {
				//failed.call(this,e);
				return [];
			}
		},
		_getEntitytypes: function () {
			var completed = (function (data) {}).bind(this);

			var failed = (function (details) {
				ms123.form.Dialog.alert(this.tr("entitytypes.getEntitytypes_failed") + ":" + details.message);
			}).bind(this);

			try {
				var ret = ms123.util.Remote.rpcSync("entity:getEntitytypes", {
					storeId: this._getStoreId()
				});
				completed.call(this, ret);
				var retList = [];
				for (var i = 0; i < ret.length; i++) {
					var o = {};
					o.value = this._facade.storeDesc.getPack() + "." + ret[i].name;
					o.label = this.tr(this._facade.storeDesc.getPack() + "." + ret[i].name);
					retList.push(o);
				}

				var value = qx.lang.Json.stringify(retList, null, 4);
				console.log("retList:" + value);
				return retList;
			} catch (e) {
				failed.call(this, e);
				return;
			}
		},
		_save: function () {
			this._saveRelations(this._getRecords());
		},
		_load: function () {
			return this._getRelations();
		}
	}
});
