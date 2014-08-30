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
  * @ignore(ms123.graphicaleditor.plugins.propertyedit.FieldSelector) 
*/

qx.Class.define("ms123.graphicaleditor.plugins.propertyedit.FieldSelectorField", {
	extend: qx.ui.core.Widget,
	implement: [
	qx.ui.form.IStringForm, qx.ui.form.IForm],
	include: [
	qx.ui.form.MForm],
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (config, title, key, facade,data) {
		this.base(arguments);
		this.config = config || {};
		this.title = title;

		this.key = key;
		this.facade = facade;
		var layout = new qx.ui.layout.HBox();
		this._setLayout(layout);
		this._entityField = this.config.entityField;
		this.data=data;
		this._fieldmap={};
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
	 MEMBERS
	 ******************************************************************************/
	members: {
		_init:function(){
			this.textField = this._createChildControl("textfield");
			var select = this._createChildControl("select");
			this.setFocusable(true);
		},
		resetValue: function () {},
		getValue: function () {
			return this.data;
		},
		setValue: function (value) {
			this.textField.setValue(value);
			this.data = value;
			if (value != undefined && value && value != "") {;
				try{
					this.data = qx.lang.Json.parse(value);
				}catch(e){
					console.error("FieldSelectorField.setValue:"+value+" wrong value:"+e);
				}
			}
			
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
				this._add(control, {
					flex: 1
				});
				break;
			case "select":
				control = this.createActionButton();
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
		createActionButton: function () {
			var control = new qx.ui.form.Button(null, "icon/16/apps/utilities-text-editor.png").set({
				padding: 0,
				margin: 0,
				maxHeight: 30
			});
			control.setFocusable(false);
			control.addListener("execute", function (e) {
				this._createWindow(this.facade.getPropertyValue(this._entityField));
			}, this);
			this._add(control);
			return control;
		},
		/**
		 * Returns the field key.
		 */
		getFieldKey: function () {
			return this.key;
		},

		_createWindow:function(mainEntity){
			var container = new qx.ui.container.Composite();
			container.setLayout(new qx.ui.layout.Dock());
			var win = this.createWindow(this.title);
			var table = this._createFieldSelector(win,mainEntity);
			container.add(table, {
				edge: "center"
			});
			if (this.config.helperTree) {
				var rh = new ms123.graphicaleditor.plugins.propertyedit.ResourceDetailTree(this.config, this.facade);
				rh.addListener("nodeSelected", function (e) {
					this.handleNodeSelected(e);
				}, this);
				rh.setWidth(300);
				this._resourceHelper = rh;
				var sp = this._splitPane(container, rh);
				win.add(sp, {
					edge: "center"
				});
			} else {
				win.add(container, {
					edge: "center"
				});
			}
			this.editWindow = win;
			win.open();
		},
		handleNodeSelected:function(e){
			var model = e.getData().model;
			var type = e.getData().type;
			console.log("type:" + type);
			console.log("model:" + model.getValue() + "/" + type);
		},
		handleOkButton:function(e){
			var value = this._fieldSelector.getSelectedFields();
			var data = {
				totalCount: value.length,
				items: value
			};

			data = qx.util.Serializer.toJson(data);
			console.log("data:" + data);
			var oldVal = this.data;
			this.data = data;
			this.fireDataEvent("changeValue", data, oldVal);
			this.editWindow.close();
		},
		_getButtons: function () {
			var list = [];

			var buttonSave = new qx.ui.toolbar.Button(this.tr("Ok"), "icon/16/actions/dialog-ok.png");
			buttonSave.addListener("execute", function (e) {
				this.handleOkButton(e);
			}, this);
			list.push(buttonSave);

			var buttonCancel = new qx.ui.toolbar.Button(this.tr("Cancel"), "icon/16/actions/dialog-close.png");
			buttonCancel.addListener("execute", function () {
				this.editWindow.close();
			}, this);
			list.push(buttonCancel);
			return list;
		},
		_splitPane: function (left, right) {
			var splitPane = new qx.ui.splitpane.Pane("horizontal").set({
				decorator: null
			});

			splitPane.add(left, 6);
			splitPane.add(right, 2);
			return splitPane;
		},
		_getSelectableFields: function (entity) {
			var colModel = this._fieldmap[entity];
			if (colModel === undefined) {
				try {
					var cm = new ms123.config.ConfigManager();
					var data = cm.getEntityViewFields(entity,this.facade.storeDesc,"report",false);
					colModel = cm.buildColModel(data, entity, this.facade.storeDesc, "data", "search");
					this._fieldmap[entity] = colModel;
				} catch (e) {
					ms123.form.Dialog.alert("FieldSelectorField._getSelectableFields:" + e);
					return;
				}
			}
			return colModel;
		},
		_getFieldDesc: function (module, id) {
			var colModel = this._fieldmap[module];
			if (colModel == null) {
				this._getSelectableFields(module);
				colModel = this._fieldmap[module];
			}
			for (var f = 0; f < colModel.length; f++) {
				var field = colModel[f];
				if (field.hidden) continue;
				if (field.id == id) {
					return field;
				}
			}
			return null;
		},
		_removeFirstSegment: function (s) {
			var i = s.indexOf(".");
			if (i == -1) {
				return "";
			}
			return s.substring(i + 1);
		},
		_createFieldSelector:function(win,mainEntity){
			var fscontext = {};
			fscontext.tableColumns = [{
				name: "db_fieldname",
				header: this.tr("graphicaleditor.fieldselectorfield.db_fieldname")
			},
			{
				name: "form_fieldname",
				type: "TextField",
				header: this.tr("graphicaleditor.fieldselectorfield.form_fieldname")
			}];
			fscontext.buttons=this._getButtons();
			fscontext.getName=(function(treeSelector,id){
				return this.tr("data." + treeSelector + "." + id)+"/"+id;
			}).bind(this);
			fscontext.removeFromFieldTable = (function (map, treeModel, path, checkbox) {
			}).bind(this);
			fscontext.addToFieldTable = (function (map, treeModel, path, checkbox) {
				map.display = true;
				map.path = path;
				var prefix = this._removeFirstSegment(path.replace("$", "."));
				map.db_fieldname = prefix + (prefix.length > 0 ? "." : "") + map.id;
				var fieldDesc = this._getFieldDesc(map.module, map.id);
				var dt = fieldDesc["datatype"];
				if (dt != null && dt.match("^array")) {
					map.display = false;
				}
				return map;
			}).bind(this);
			this._fieldSelector = new ms123.graphicaleditor.plugins.propertyedit.FieldSelector(fscontext);
			this._fieldSelector.addListener("treeClicked", function (e) {
				var model = e.getData().selectionModel;
				var path = e.getData().treePath;
				var fields = this._getSelectableFields(model.getEntity());
				this._fieldSelector.createFieldsWindow(path, model, fields);
			}, this);

			this._mainEntity = mainEntity;
			var treeModel = null;
			var cm = new ms123.config.ConfigManager();
			var treeModel = cm.getEntityTree(this.facade.storeDesc,this._mainEntity,5);
			this._fieldSelector.setTreeModel(this._translateModel(treeModel));
			this._fieldSelector.createTable(fscontext.tableColumns);
			if(this.data && this.data.items){
				this._fieldSelector.setSelectedFields(this.data.items);
			}
			return this._fieldSelector;
		},
		_translateModel: function (model) {
			model.title = this.tr(model.title);
			var children = model.children;
			if (children) {
				for (var i = 0; i < children.length; i++) {
					var c = children[i];
					this._translateModel(c);
				}
			}
			return model;
		},
		createWindow: function (name) {
			var win = new qx.ui.window.Window(name, "").set({
				resizable: true,
				useMoveFrame: true,
				useResizeFrame: true
			});
			win.setLayout(new qx.ui.layout.Dock);
			win.setWidth(700);
			win.setHeight(500);
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

/**
 */
qx.Class.define("ms123.graphicaleditor.plugins.propertyedit.FieldSelector", {
  extend: ms123.util.BaseFieldSelector,
  include: [qx.locale.MTranslation],



  /******************************************************************************
   CONSTRUCTOR
   ******************************************************************************/
  construct: function (context) {
		this._buttons = context.buttons;
    this.base(arguments,context);
  },

  /******************************************************************************
   MEMBERS
   ******************************************************************************/
  members: {
    _saveFields: function () {
      this._context.saveFields();
    },
		_createToolbar: function () {
			var toolbar = new qx.ui.toolbar.ToolBar();
			this._buttonEdit = new qx.ui.toolbar.Button("", "icon/16/apps/utilities-text-editor.png");
			this._buttonEdit.addListener("execute", function (e) {
				this._table.stopEditing();

				var curRecord = this._getRecordAtPos(this._currentTableIndex);
				var f = qx.util.Serializer.toJson(curRecord);
				this._propertyEditWindow.setActive(true);
				this._propertyEditForm.fillForm(curRecord);
				this._propertyEditWindow.open();
			}, this);
			toolbar._add(this._buttonEdit);
			this._buttonEdit.setEnabled(false);

			this._buttonDel = new qx.ui.toolbar.Button("", "icon/16/actions/list-remove.png");
			this._buttonDel.addListener("execute", function (e) {
				this._table.stopEditing();
				this._deleteRecordAtPos(this._currentTableIndex);
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

			for(var i=0; i< this._buttons.length;i++){
				toolbar.add(this._buttons[i], {
					flex: 0
				});
				
			}
			return toolbar;
		}
  }
});
