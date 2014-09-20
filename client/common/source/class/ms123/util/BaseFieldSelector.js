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
qx.Class.define("ms123.util.BaseFieldSelector", {
	extend: qx.ui.container.Composite,
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (context) {
		this.base(arguments);
		this._context = context;
		this.setLayout(new qx.ui.layout.Grow());
		this._checkboxMap = {};

		var leftScroll = new qx.ui.container.Scroll().set({});
		var leftComposite = new qx.ui.container.Composite();
		leftComposite.setLayout(new qx.ui.layout.HBox());
		leftScroll.add(leftComposite);

		this._selectorTree = new ms123.widgets.Tree(this._createTreeContext(this._context.selectorTreeHideRoot));
		leftScroll.add(this._selectorTree, {
			edge: "center"
		});

		var rightScroll = new qx.ui.container.Scroll().set({});
		this._rightScroll = rightScroll;

		var spUpper = this._doLayoutUpper(this, leftScroll, rightScroll);
		this._bottom = new qx.ui.container.Composite();
		this._bottom.setLayout(new qx.ui.layout.Dock());
		var sp = this._doLayout(this, spUpper, this._bottom);
		this.add(sp);

		this._table = null;

		var taskbar = this._createToolbar();
		this._bottom.add(taskbar, {
			edge: "south"
		});
	},
	/******************************************************************************
	EVENTS
	******************************************************************************/
	events: {
		"treeClicked": "qx.event.type.Data"
	},
	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		setTreeModel: function (treeModel) {
			this._selectorTree.setModel(treeModel);
		},
		setSelectedFields: function (fields) {
			this._clearTable();
			if (this._rightContent) {
				this._rightScroll.remove(this._rightContent);
				this._rightContent = null;
			}
			var f = qx.util.Serializer.toJson(fields);
			console.log("setSelectedFields:" + f);

			if (fields != null) {
				for (var i = 0; i < fields.length; i++) {
					var field = fields[i];
					this.addToFieldTable(field);
				}
			}
		},
		getSelectedFields: function () {
			this._table.stopEditing();
			var rc = this._tableModel.getRowCount();
			var fields = [];
			for (var i = 0; i < rc; i++) {
				var rd = this._tableModel.getRowDataAsMap(i);
				fields.push(rd);
			}
			return fields;
		},
		_saveFields: function () {
			this._table.stopEditing();
			var rc = this._tableModel.getRowCount();
			var fields = [];
			for (var i = 0; i < rc; i++) {
				var rd = this._tableModel.getRowDataAsMap(i);
				fields.push(rd);
			}
			var props;
			if (this._propForm) {
				props = this._propForm.getModel();
			}
			this._context.saveFields(fields, props);
		},
		_clearTable: function () {
			this._tableModel.removeRows(0, this._tableModel.getRowCount());
		},
		_doLayoutUpper: function (parent, leftWidget, rightWidget) {
			var splitpane = new qx.ui.splitpane.Pane("horizontal");
			splitpane.setDecorator(null);

			leftWidget.setDecorator(null);
			splitpane.add(leftWidget, 2);

			rightWidget.setDecorator(null);
			splitpane.add(rightWidget, 3);

			return splitpane;
		},
		_doLayout: function (parent, topWidget, bottomWidget) {
			var splitpane = new qx.ui.splitpane.Pane("vertical");
			splitpane.setDecorator(null);

			topWidget.setDecorator(null);
			splitpane.add(topWidget, 2);

			bottomWidget.setDecorator(null);
			splitpane.add(bottomWidget, 3);

			return splitpane;
		},
		_handleDrop: function (e) {
			console.log("_handleDrop:" + e);
			if (this._table.isEditing()) {
				this._table.stopEditing();
			}
			var col = this._table.getFocusedColumn();
			var row = this._table.getFocusedRow();
			if (col === undefined || row == undefined) return;
			var target = e.getRelatedTarget();
			var value = null;


			if (qx.Class.implementsInterface(target, qx.ui.form.IStringForm)) {
				value = target.getValue();
			} else {
				value = target.getSelection().getItem(0).getValue();
			}
			console.log("cell:" + row + "/" + col);
			console.log("_handleDrop:" + value);
			this._table.getTableModel().setValue(col, row, value);
		},
		addToFieldTable: function (map) {
			this._tableModel.addRowsAsMapArray([map], null, true);
		},
		_isSelected: function (path, id) {
			var rc = this._tableModel.getRowCount();
			for (var i = 0; i < rc; i++) {
				var rdata = this._tableModel.getRowDataAsMap(i);
				if (this._compareRow(rdata, path, id)) {
					return true;
				}
			}
			return false;
		},
		removeFromFieldTable: function (path, id) {
			var rc = this._tableModel.getRowCount();
			for (var i = 0; i < rc; i++) {
				var rd = this._tableModel.getRowDataAsMap(i);
				if (this._compareRow(rd, path, id)) {
					this._tableModel.removeRows(i, 1);
					break;
				}
			}
		},
		_compareRow: function (rdata, path, id) {
			if (rdata["id"] == id && rdata["path"] == path) {
				return true;
			}
			return false;
		},
		_createTreeContext: function (hideRoot) {
			var _this = this;
			var tcontext = {};
			tcontext.hideRoot = hideRoot;
			tcontext.clickListener = function (e) {
				_this.getApplicationRoot().setGlobalCursor("wait");
				qx.event.Timer.once(function () {
					_this.getApplicationRoot().setGlobalCursor("default");
				}, this, 500);
				if (!this.getSelection() || !this.getSelection()[0]) return;
				var model = this.getSelection()[0].getModel();
				var path = _this._selectorTree._getModelPath(model);
				var data = {};
				data.treePath = path;
				data.selectionModel = model;

				_this.fireDataEvent("treeClicked", data, null);
			}
			tcontext.createMenu = function (item, level, id, treeModul) {}
			return tcontext;
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
				this._saveFields();
			}, this);
			toolbar._add(buttonSave);
			toolbar.addSpacer();

			return toolbar;
		},
		createTable: function (tableColumns) {
			if (this._table) {
				this._bottom.remove(this._table);
			}
			var colIds = new Array();
			var colHds = new Array();

			for (var i = 0; i < tableColumns.length; i++) {
				var col = tableColumns[i];
				colIds.push(col.name);
				colHds.push(col.header);
			}
			this._tableModel = new qx.ui.table.model.Simple();
			this._tableModel.setColumns(colHds, colIds);
			var customMap = {
				tableColumnModel: function (obj) {
					return new qx.ui.table.columnmodel.Resize(obj);
				}
			};
			var table = new qx.ui.table.Table(this._tableModel, customMap);
			table.setStatusBarVisible(false);
			table.setDroppable(true);
			table.setFocusCellOnPointerMove(true);
			table.addListener("drop", this._handleDrop, this);


			var tcm = table.getTableColumnModel();

			var booleanCellRendererFactory = new qx.ui.table.cellrenderer.Dynamic(this._booleanCellRendererFactoryFunc);
			var booleanCellEditorFactory = new qx.ui.table.celleditor.Dynamic(this._booleanCellEditorFactoryFunc);

			for (var i = 0; i < tableColumns.length; i++) {
				var col = tableColumns[i];
				if (col.type == "CheckBox") {
					tcm.setDataCellRenderer(i, booleanCellRendererFactory);
					tcm.setCellEditorFactory(i, booleanCellEditorFactory);
					table.getTableModel().setColumnEditable(i, true);
					table.addListener("cellTap", this._onCellClick, this, false); //@@@MS Achtung colnum fix
				}
				if (col.type == "DoubleSelectBox") {
					tcm.setDataCellRenderer(i, new ms123.util.MultiValueRenderer());
				}
				if (col.type == "TextField") {
					var f = new qx.ui.table.celleditor.TextField();
					tcm.setCellEditorFactory(i, f);
					table.getTableModel().setColumnEditable(i, true);
				}
				if (col.type == "SelectBox") {
					var f = new qx.ui.table.celleditor.SelectBox();
					var listData = [];
					var o = col.options;
					if (typeof o == "function") {
						var modfunc = this._context.getModule;
						var module = null;
						if (modfunc) {
							module = modfunc();
						}
						o = col.options(module);
					}
					var listData = [];
					for (var j = 0; j < o.length; j++) {
						var value = o[j]
						var option = [value.value, null, value.value];
						listData.push(option);
					}
					f.setListData(listData);
					tcm.setCellEditorFactory(i, f);
					table.getTableModel().setColumnEditable(i, true);
				}
				if (col.width !== undefined) {
					var resizeBehavior = tcm.getBehavior();
					resizeBehavior.setWidth(i, col.width);
				}
			}

			this._tableModel = table.getTableModel();
			var selModel = table.getSelectionModel();
			selModel.setSelectionMode(qx.ui.table.selection.Model.SINGLE_SELECTION);
			selModel.addListener("changeSelection", function (e) {
				var index = selModel.getLeadSelectionIndex();
				var map = this._tableModel.getRowDataAsMap(index);
				var count = selModel.getSelectedCount();
				if (count == 0) {
					this._buttonUp.setEnabled(false);
					this._buttonDown.setEnabled(false);
					this._buttonEdit.setEnabled(false);
					if( this._buttonDel) this._buttonDel.setEnabled(false);
					return;
				}
				this._currentTableIndex = index;
				this._buttonUp.setEnabled(true);
				this._buttonDown.setEnabled(true);
				this._buttonEdit.setEnabled(true);
				if( this._buttonDel) this._buttonDel.setEnabled(true);
			}, this);
			this._table = table;
			this._bottom.add(this._table, {
				edge: "center"
			});

			this._propertyEditForm = this._createPropertyEditForm(tableColumns);
			this._propertyEditWindow = this._createPropertyEditWindow();
			this._propertyEditWindow.add(this._propertyEditForm);

			return table;
		},
		_booleanCellRendererFactoryFunc: function (cellInfo) {
			return new qx.ui.table.cellrenderer.Boolean;
		},
		_booleanCellEditorFactoryFunc: function (cellInfo) {
			return new qx.ui.table.celleditor.CheckBox;
		},
		_createPropertyEditForm: function (columns) {
			var formData = {};
			for (var i = 0; i < columns.length; i++) {
				var col = columns[i];
				var fieldData = {};
				fieldData.type = col.type;
				fieldData.label = col.header;
				if (typeof col.options == "function") {
					var modfunc = this._context.getModule;
					var module = null;
					if (modfunc) {
						module = modfunc();
					}
					fieldData.options = col.options(module);
				} else {
					fieldData.options = col.options;
				}
				if (!fieldData.type) {
					fieldData.type = "TextField";
					fieldData.readonly = true;
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
					_this._tableModel.setRowsAsMapArray([m], _this._currentTableIndex, true);
					_this._propertyEditWindow.close();
					//	_this._saveFields();
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
		_onCellClick: function (e) {
			var colnum = this._table.getFocusedColumn();
			var rownum = this._table.getFocusedRow();
			if (colnum != 1) return;
			if (this._tableModel.getValue(colnum, rownum) === true) {
				this._tableModel.setValue(colnum, rownum, false);
			} else {
				this._tableModel.setValue(colnum, rownum, true);
			}
		},
		createFieldsWindow: function (treePath, treeModel, fields) {
			var f = qx.util.Serializer.toJson(treeModel);
			console.log("treeModel:" + f);
			console.log("path:" + treePath);
			var path = treePath.join("$");
			var treeSelector = treeModel.getEntity();
			var title = treeModel.getTitle();

			var content = new qx.ui.core.Widget().set({
				padding: 10
			});
			content._setLayout(new qx.ui.layout.VBox(2).set({
				alignX: "left"
			}));

			this._rightContent = content;
			this._rightScroll.add(content, {});

			console.log("ts:" + treeSelector + ",id:" + treeModel.getId());
			var namefunc = this._context.getName;
			var _this = this;
			fields.sort(function (a, b) {
				a = namefunc ? namefunc(treeSelector, a.id) : _this.tr("data." + treeSelector + "." + a.id);
				b = namefunc ? namefunc(treeSelector, b.id) : _this.tr("data." + treeSelector + "." + b.id);
				if (a < b) return -1;
				if (a > b) return 1;
				return 0;
			});
			for (var f = 0; f < fields.length; f++) {
				var field = fields[f];
				var dt = field.datatype;
				if (field.hidden) continue;
				if (dt != undefined && (dt.match("^list") || dt.match("^relat") || dt.match("^object"))) continue;
				var name = namefunc ? namefunc(treeSelector, field.id) : this.tr("data." + treeSelector + "." + field.id);
				//var name = field.id;
				var cb = new qx.ui.form.CheckBox(name);
				cb.setUserData("selector", treeSelector);
				cb.setUserData("id", field.id);
				if (this._isSelected(path, field.id)) {
					cb.setValue(true);
				}
				cb.addListener("execute", function (e) {
					var c = e.getTarget();
					var data = {};
					data.checkbox = c;
					if (c.getValue()) {
						var map = {
							id: c.getUserData("id"),
							module: c.getUserData("selector")
						};
						console.log("id:" + map.id + ",selector:" + map.module + ",path:" + path);
						if (this._context.addToFieldTable) {
							map = this._context.addToFieldTable(map, treeModel, path, c);
						}
						this.addToFieldTable(map);
					} else {
						if (this._context.removeFromFieldTable) {
							this._context.removeFromFieldTable(map, treeModel, path, c);
						}
						this.removeFromFieldTable(path, c.getUserData("id"));
					}
				}, this);

				content._add(cb);
			}
		}
	}
});
