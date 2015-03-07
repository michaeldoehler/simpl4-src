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




/**
 */
qx.Class.define("ms123.util.TableEdit", {
	extend: qx.ui.container.Composite,

	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (facade) {
		this.base(arguments);
		this._facade = facade;
		this._loaded = false;
		var columnmodel = this._createColumnModel();
		this._columnModel = columnmodel;
		var table = this._createTable(columnmodel);
		this._doLayout(table, columnmodel);

		// table.setDroppable(true);
		table.addListener("drop", function (e) {
			console.debug("Related of drop: " + e.getRelatedTarget());

			// Move items from source to target
			var items = e.getData("items");
			for (var i = 0, l = items.length; i < l; i++) {
				console.log("item:" + items[i]);
				//this.add(items[i]);
			}
		});

		this._init();
	},

	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		_createColumnModel: function () {
		},
		_load: function () {
		},
		_save: function () {
		},
		_init: function () {
			this._reload();
		},
		_reload: function () {
			this._loaded = false;
			 try{
				 this._clearTable();
			 }catch(e){
				 console.error("_clearTable:"+e);
			 }

			var records = this._load();
			if (records != null) {
				for (var i = 0; i < records.length; i++) {
					var p = records[i];
					this.addRecord(p);
				}
			}
			this._loaded = true;
		},
		_doLayout:function(table, columnmodel){
			this.setLayout(new qx.ui.layout.Dock());
			this.add(table, { edge: "center" });
			var tb = this._createToolbar();
			this.add(tb, { edge: "south" });
		},
		_getRecordPosByNamedField: function (fieldname, value) {
			var rc = this._tableModel.getRowCount();
			for (var i = 0; i < rc; i++) {
				var rd = this._tableModel.getRowDataAsMap(i);
				if( rd[fieldname] == value ){
					return i;
				}
			}
			return null;
		},
		_getRecords: function () {
			this._table.stopEditing();
			var rc = this._tableModel.getRowCount();
			var records = [];
			for (var i = 0; i < rc; i++) {
				var rd = this._tableModel.getRowDataAsMap(i);
				records.push(rd);
			}
			return records;
		},
		_setRecords: function (records) {
			if (records != null) {
				for (var i = 0; i < records.length; i++) {
					var p = records[i];
					this.addRecord(p);
				}
			}
		},
		_clearTable: function () {
			this._tableModel.removeRows(0, this._tableModel.getRowCount());
		},
		addRecord: function (map) {
			for (var i = 0; i < this._columnModel.length; i++) {
				var col = this._columnModel[i];
				if( col.value && map[col.name] == null){
					map[col.name] = col.value;
				}
			}
			this._tableModel.addRowsAsMapArray([map], null, true);
		},
		addRecordByNamedField: function (map,fieldname, value) {
			var pos = this._getRecordPosByNamedField(fieldname,value);
			if( pos != null ) return;
			this._tableModel.addRowsAsMapArray([map], null, true);
		},
		removeRecordByNamedField: function (fieldname,value) {
			var pos = this._getRecordPosByNamedField(fieldname,value);
			if( pos == null ) return;
			this._deleteRecordAtPos(pos);
		},
		_insertRecordAtPos: function (map, pos) {
			this._tableModel.addRowsAsMapArray([map], pos, true);
		},
		_deleteRecordAtPos: function (pos) {
			this._tableModel.removeRows(pos, 1);
		},
		_getRecordAtPos: function (pos) {
			return this._tableModel.getRowDataAsMap(pos);
		},
		_createTable: function (tableColumns) {
			var colIds = new Array();
			var colHds = new Array();

			for (var i = 0; i < tableColumns.length; i++) {
				var col = tableColumns[i];
				colIds.push(col.name);
				colHds.push(col.header || col.label);
			}
			this._tableModel = this._createTableModel();
			this._tableModel.setColumns(colHds, colIds);
			var customMap = {
				tableColumnModel: function (obj) {
					return new qx.ui.table.columnmodel.Resize(obj);
				}
			};
			var table = new qx.ui.table.Table(this._tableModel, customMap);
			table.setStatusBarVisible(false);


			var tcm = table.getTableColumnModel();

			var booleanCellRendererFactory = new qx.ui.table.cellrenderer.Dynamic(this._booleanCellRendererFactoryFunc);
			var booleanCellEditorFactory = new qx.ui.table.celleditor.Dynamic(this._booleanCellEditorFactoryFunc);

			table.addListener("cellTap", this._onCellClick, this, false);
			this._booleanCols = [];
			for (var i = 0; i < tableColumns.length; i++) {
				var col = tableColumns[i];
				if (col.type == "CheckBox") {
					tcm.setDataCellRenderer(i, booleanCellRendererFactory);
					tcm.setCellEditorFactory(i, booleanCellEditorFactory);
					table.getTableModel().setColumnEditable(i, true);
					this._booleanCols.push(i);
				}
				if (col.type == "DoubleSelectBox") {
					tcm.setDataCellRenderer(i, new ms123.util.MultiValueRenderer());
				}
				if (col.type == "DateTimeField") {
					tcm.setDataCellRenderer(i, new ms123.util.DateRenderer());
				}
				if (col.type == "TextField") {
					var f = new ms123.util.TableCellTextField(col);
					tcm.setCellEditorFactory(i, f);
					table.getTableModel().setColumnEditable(i, true);
				}
				if (col.type == "DecimalField") {
					var f = new ms123.ruleseditor.DecimalCellEditor();
					tcm.setCellEditorFactory(i, f);
					table.getTableModel().setColumnEditable(i, true);
				}
				if (col.type == "ComboBox") {
					var comboBox = new qx.ui.table.celleditor.ComboBox();
					var o = col.options;
					var listData = [];
					for (var j = 0; j < o.length; j++) {
						var value = o[j].value;
						var option = [value, null, value];
						listData.push(option);
					}
					comboBox.setListData(listData);
					tcm.setCellEditorFactory(i, comboBox);
					table.getTableModel().setColumnEditable(i, true);
				}
				if (col.type == "SelectBox") {
					var r = new qx.ui.table.cellrenderer.Replace();
					var f = new qx.ui.table.celleditor.SelectBox();
					var listData = [];
					var o = col.options;
					var listData = [];
					for (var j = 0; j < o.length; j++) {
						var value = o[j]
						var label = value.label || value.value;
						var option = [label, null, value.value];
						listData.push(option);
					}
					f.setListData(listData);

					var replaceMap = {};
					listData.forEach(function (row) {
						if (row instanceof Array) {
							replaceMap[row[0]] = row[2];
						}
					});
					r.setReplaceMap(replaceMap);
					r.addReversedReplaceMap();
					tcm.setDataCellRenderer(i,r);

					tcm.setCellEditorFactory(i, f);
					table.getTableModel().setColumnEditable(i, true);
				}
				if( col.readonly === true ){
					table.getTableModel().setColumnEditable(i, false);
				}
				if (col.width !== undefined) {
					var resizeBehavior = tcm.getBehavior();
					//resizeBehavior.setWidth(i, col.width);
					resizeBehavior.setMinWidth(i, col.width);
				}
			}
			this._createTableListener(table);
			this._table = table;
			this._createPropertyEdit(tableColumns);
			return table;
		},
		_createTableModel:function(){
			return new qx.ui.table.model.Simple();
		},
		_createPropertyEdit:function(tableColumns){
			this._propertyEditForm = this._createPropertyEditForm(tableColumns);
			this._propertyEditWindow = this._createPropertyEditWindow();
			this._propertyEditWindow.add(this._propertyEditForm);
		},
		_createTableListener:function(table){
			this._tableModel = table.getTableModel();
			var selModel = table.getSelectionModel();
			selModel.setSelectionMode(qx.ui.table.selection.Model.SINGLE_SELECTION);
			selModel.addListener("changeSelection", function (e) {
				var index = selModel.getLeadSelectionIndex();
				var map = this._tableModel.getRowDataAsMap(index);
				var count = selModel.getSelectedCount();
				if (count == 0) {
					if( this._buttonUp ) this._buttonUp.setEnabled(false);
					if( this._buttonDown ) this._buttonDown.setEnabled(false);
					if( this._buttonEdit ) this._buttonEdit.setEnabled(false);
					if( this._buttonArchive) this._buttonArchive.setEnabled(false);
					if( this._buttonDel ) this._buttonDel.setEnabled(false);
					return;
				}
				this._currentTableIndex = index;
				if( this._buttonUp ) this._buttonUp.setEnabled(true);
				if( this._buttonDown ) this._buttonDown.setEnabled(true);
				if( this._buttonEdit) this._buttonEdit.setEnabled(true);
				if( this._buttonArchive) this._buttonArchive.setEnabled(true);
				if( this._buttonDel ) this._buttonDel.setEnabled(true);
			}, this);
		},
		_createToolbar: function () {
			var toolbar = new qx.ui.toolbar.ToolBar();
			this._buttonEdit = new qx.ui.toolbar.Button("", "icon/16/apps/utilities-text-editor.png");
			this._buttonEdit.addListener("execute", function (e) {
				this._table.stopEditing();
				this._isEditMode = true;
				var curRecord = this._getRecordAtPos(this._currentTableIndex);
				this._propertyEditWindow.setActive(true);
				this._propertyEditForm.fillForm(curRecord);
				this._propertyEditWindow.open();
			}, this);
			toolbar._add(this._buttonEdit);
			this._buttonEdit.setEnabled(false);


			this._buttonDel = new qx.ui.toolbar.Button("", "icon/16/actions/list-remove.png");
			this._buttonDel.addListener("execute", function () {
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
			var buttonSave = new qx.ui.toolbar.Button(this.tr("meta.lists.savebutton"), "icon/16/actions/document-save.png");
			buttonSave.setToolTipText(this.tr("meta.lists.fs.save"));
			buttonSave.addListener("execute", function () {
				this._table.stopEditing();
				this._save();
			}, this);
			toolbar._add(buttonSave);
			toolbar.addSpacer();

			return toolbar;
		},
		_createPropertyEditForm: function (columns) {
			var formData = {};
			for (var i = 0; i < columns.length; i++) {
				var col = columns[i];
				var fieldData = {};
				fieldData.type = col.type;
				fieldData.label = col.header || col.label;
				fieldData.options = col.options;
				fieldData.defaultValue = col.value;
				if (!fieldData.type) {
					fieldData.type = "TextField";
					fieldData.readonly = true;
				}
				if (col.filter) {
					fieldData.validation = {filter: col.filter};
				}
				formData[col.name] = fieldData;
			}

			var _this = this;
			var buttons = [{
				'label': this.tr("meta.lists.takeit"),
				'icon': "icon/22/actions/dialog-ok.png",
				'callback': function (m) {
					var f = qx.util.Serializer.toJson(m);
					console.log("formData:" + f);
					if( _this._isEditMode ){
						_this._tableModel.setRowsAsMapArray([m], _this._currentTableIndex, true);
					}else{
						_this._tableModel.addRowsAsMapArray([m], null, true);
					}
					_this._propertyEditWindow.close();
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
		_createPropertyEditWindow: function () {
			var win = new qx.ui.window.Window("", "").set({
				resizable: true,
				useMoveFrame: true,
				useResizeFrame: true
			});
			win.setLayout(new qx.ui.layout.Grow);
			win.setWidth(600);
			win.setHeight(300);
			win.setAllowMaximize(false);
			win.setAllowMinimize(false);
			win.setModal(true);
			win.setActive(false);
			win.minimize();
			win.center();
			this.getApplicationRoot().add(win);
			return win;
		},
		_booleanCellRendererFactoryFunc: function (cellInfo) {
			return new qx.ui.table.cellrenderer.Boolean;
		},
		_booleanCellEditorFactoryFunc: function (cellInfo) {
			return new qx.ui.table.celleditor.CheckBox;
		},
		_onCellClick: function (e) {
			var colnum = this._table.getFocusedColumn();
			var rownum = this._table.getFocusedRow();
			if (this._booleanCols.indexOf(colnum) < 0) return;
			if (this._tableModel.getValue(colnum, rownum) === true) {
				this._tableModel.setValue(colnum, rownum, false);
			} else {
				this._tableModel.setValue(colnum, rownum, true);
			}
		},
		_translate: function (o) {
			if (typeof o == "string") {
				if (o.match(/^%/)) {
					var tr = this.tr(o.substring(1)).toString();
					if (tr) {
						o = tr;
					}
				}
				return o;
			}
			for (var i in o) {
				if (typeof o[i] == "function") continue;
				o[i] = this._translate(o[i]);
			}
			return o;
		}
	}
});
