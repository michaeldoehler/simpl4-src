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
 @asset(qx/icon/${qx.icontheme}/16/places/*)
 @asset(qx/icon/${qx.icontheme}/22/status/*)
 @asset(qx/icon/${qx.icontheme}/16/apps/*)
 @asset(ms123/*)
 @lint ignoreDeprecated(alert) 
 */



qx.Class.define('ms123.widgets.Table', {
	extend: ms123.widgets.Widget,


	construct: function (context) {
		this.base(arguments);
		var colIds = new Array();
		var colHds = new Array();
		var colRenderer = new Array();
		var colEditor = new Array();
		this._loadBeforeEdit = context.loadBeforeEdit == null ? true : context.loadBeforeEdit;
		this._context = context;
		this.bar = null;
		this._disabledButton = new qx.data.Array(context.disable);
		this.__configManager = new ms123.config.ConfigManager();
		this.__access = this.__configManager.getEntityAccess(context.storeDesc, context.config);
		if (context.configMaster !== undefined) {
			this.__accessMaster = this.__configManager.getEntityAccess(context.storeDesc, context.configMaster);
		}

		if (context.dataAccess) {
			this.__dataAccess = context.dataAccess;
		} else if (context.config == "user" || context.config == "group") {
			this.__dataAccess = new ms123.widgets.UserDataAccess();
		} else {
			this.__dataAccess = new ms123.widgets.DefaultDataAccess();
		}
		this.__storeDesc = context.storeDesc;
		console.error("Table(" + context.config + "):" + this.__storeDesc);
		this._entityConfig = null;
		if (context.config) {
			this._entityConfig = this.__configManager.getEntityConfig(context.config, this.__storeDesc);
			var f = qx.util.Serializer.toJson(this._entityConfig);
			console.log("_entityConfig(" + context.config + "):" + f);
		}


		qx.Class.include(qx.ui.table.Table, qx.ui.table.MTableContextMenu);

		var _hasIdColumn = false;
		this._keyColumn = null;

		this._props = context.model.attr("gridProps") || {};
		var f = qx.util.Serializer.toJson(this._props);
		console.log("_props1(" + context.config + "):" + f);
		if (this._props.baseurl && !this._props.path) {
			alert("Baseurl insteed Path defined");
			this._props.path = this._props.baseurl;
		}
		var cols = context.model.attr("colModel");

		for (var i = 0; i < cols.length; i++) {
			var col = cols[i];
			if (col.key) {
				this._keyColumn = col.name;
			}
			if (col.name == "id") {
				_hasIdColumn = true;
			}

			if (col.hidden == true) continue;
			colIds.push(col.name);
			if (col.label === undefined) {
				col.label = col.name;
			}
			colHds.push(col.label);
			if (col.datatype == "date") {
				if (col.edittype == "datetime") {
					colRenderer.push(new ms123.util.DateTimeRenderer());
				} else {
					colRenderer.push(new ms123.util.DateRenderer());
				}
			}else if (col.datatype == "boolean") {
					var r = new qx.ui.table.cellrenderer.Boolean();
					colRenderer.push(r);
			} else {
				if (col.edittype == "select") {
					var r = new qx.ui.table.cellrenderer.Replace();
					colRenderer.push(r);
				} else {
					colRenderer.push(new qx.ui.table.cellrenderer.Default());
				}
			}
			var selectable_items = col.selectable_items ? col.selectable_items.getItems() : null;
			if (col.edittype == "select" && selectable_items) {
				var selectbox = new qx.ui.table.celleditor.SelectBox();
				var listData = [];
				if (selectable_items.length !== undefined) {
					for (var j = 0; j < selectable_items.length; j++) {
						var o = selectable_items[j];
						var option = [o.label, null, o.value];
						listData.push(option);
					}
				} else {
					for (var key in selectable_items) {
						var value = selectable_items[key]
						var option = [value, null, key];
						listData.push(option);
					}
				}
				var replaceMap = {};
				listData.forEach(function (row) {
					if (row instanceof Array) {
						replaceMap[row[0]] = row[2];
					}
				});
				var renderer = colRenderer[colRenderer.length - 1];
				renderer.setReplaceMap(replaceMap);
				renderer.addReversedReplaceMap();

				if (col.tableedit && col.tableedit == true) {
					selectbox.setListData(listData);
					colEditor.push(selectbox);
				} else {
					colEditor.push(null);
				}
			} else if (col.tableedit && col.tableedit == true && col.edittype == "combo" && selectable_items) {
				var selectbox = new qx.ui.table.celleditor.ComboBox();
				var listData = [];
				for (var key in selectable_items) {
					var value = selectable_items[key]
					var option = [value, null, key];
					listData.push(option);
				}
				selectbox.setListData(listData);
				colEditor.push(selectbox);
			} else if (col.tableedit && col.tableedit == true) {
				var textfield = new qx.ui.table.celleditor.TextField();
				colEditor.push(textfield);
			} else {
				colEditor.push(null);
			}
		}

		if (this._keyColumn == null && _hasIdColumn) this._keyColumn = "id";

		this._colHds = colHds;
		this._colIds = colIds;
		this._colRenderer = colRenderer;
		this._colEditor = colEditor;
		this._createTable();
		if (this._buttonEdit && context.noEdit !== true) {
			this._editWindow = this._createEditWindow();
			this._copyWindow = this._createEditWindow();
		}
		this._currentIndex = -1;

		this.addListener("dblclick", this._onDblClick, this);
		try {
			var storeId = this.__storeDesc.getStoreId();
			this._tableStoreKey = "widgets.table." + storeId + "." + this._context.config+"/"+this._context.isMaster;
			this._loadTableSetup();
		} catch (e) {
			console.error("_loadTableSetup:" + e);
			console.log(e.stack);
		}
	},

	events: { /** Fires after a record has modified */
		"addRecord": "qx.event.type.Data",
		"deleteRecord": "qx.event.type.Data",
		"changeRecord": "qx.event.type.Data"
	},
	members: {
		_createTable: function () {
			var dock = new qx.ui.layout.Dock();
			this.setLayout(dock);
			this.add(this._createChildControlImpl("table"), {
				edge: "center"
			});
			if (this._context.toolbar) {
				var toolbar = this._context.toolbar;
				if (qx.Class.implementsInterface(this._context.toolbar, ms123.widgets.IToolbar)) {
					toolbar = toolbar.getToolbar(this);
				}
				if (toolbar) {
					this.add(toolbar, {
						edge: "south"
					});
				}
				this._createDefaultButtons();
				this._context.toolbar.setDefaultButtons(this._buttonMap);
			} else {
				this.add(this.getChildControl("toolbar"), {
					edge: "south"
				});
				if (this._context.buttons) {
					this._addButtons(this.getChildControl("toolbar"), this._context.buttons);
				} else {
					this._createDefaultButtons();
					this._addDefaultButtons(this.getChildControl("toolbar"));
				}
			}
			var selModel = this.table.getSelectionModel();
			selModel.setSelectionMode(qx.ui.table.selection.Model.SINGLE_SELECTION);
			selModel.addListener("changeSelection", function (e) {
				if (this._context.toolbar && qx.Class.implementsInterface(this._context.toolbar, ms123.widgets.IToolbar)) {
					this._context.toolbar.selectionChanged(this.table);
				}
				var index = selModel.getLeadSelectionIndex();
				var map = this.tableModel.getRowDataAsMap(index);
				var count = selModel.getSelectedCount();
				if (count == 0) {
					if (this._buttonDel) this._buttonDel.setEnabled(false);
					if (this._buttonEdit) this._buttonEdit.setEnabled(false);
					if (this._buttonCopy) this._buttonCopy.setEnabled(false);
					return;
				}
				this._currentIndex = index;
				if (this._buttonDel) this._buttonDel.setEnabled(true);
				if (this._buttonEdit) this._buttonEdit.setEnabled(true);
				if (this._buttonCopy) this._buttonCopy.setEnabled(true);
				if (this._buttonSelect) this._buttonSelect.setEnabled(true);
				var id = map[this._keyColumn];
				this._sendMessage(this._context.unit_id + ".table.row.selected", map, this._context, id);
			}, this);
		},
		_loadTableSetup: function () {
			var loc = qx.bom.storage.Web.getLocal();
			var colModel = this.table.getTableColumnModel();
			var storeId = this.__storeDesc.getStoreId();
			var cc = colModel.getOverallColumnCount();
			if( cc != this._colIds.length){
				console.error("Table verändert:"+this._context.config+"("+cc+","+this._colIds.length+")");
				loc.removeItem(this._tableStoreKey);
				return;
			}
			var ts = loc.getItem(this._tableStoreKey);
			if (!ts) return;
			colModel.setColumnsOrder(ts.orderArray);
			for (var i = 0; i < this._colIds.length; i++) {
				colModel.setColumnWidth(i, ts.widthArray[i]);
				colModel.setColumnVisible(i, ts.visibleArray[i]);
			}
		},
		_saveTableSetup: function () {
			var colModel = this.table.getTableColumnModel();
			var orderArray = [];
			var visibleArray = [];
			var widthArray = [];
			for (var i = 0; i < this._colIds.length; i++) {
				var col = this._colIds[i];
				var width = colModel.getColumnWidth(i);
				widthArray.push(width);
				var ox = colModel.getOverallX(i);
				var isVis = colModel.isColumnVisible(i);
				visibleArray.push(isVis);
				var atx = colModel.getOverallColumnAtX(i);
				var vx = colModel.getVisibleColumnAtX(i);
				orderArray.push(atx);
				console.log("col:" + col + ",\t\ti:" + i + ",\tw:" + width + ",\tox:" + ox + ",\tatx:" + atx + ",\tisVis:" + isVis+"/"+vx);
			}
			var loc = qx.bom.storage.Web.getLocal();
			var ts = {
				widthArray: widthArray,
				visibleArray: visibleArray,
				orderArray: orderArray
			}
			console.log(JSON.stringify(ts,null,2));
			loc.setItem(this._tableStoreKey, ts);
		},
		getTable: function () {
			return this.table;
		},
		selectRow: function (row) {
			var map = this.tableModel.getRowDataAsMap(row);
			var id = map[this._keyColumn];
			console.log("selectRow:" + id + "," + this._context.unit_id);
			this._sendMessage(this._context.unit_id + ".table.row.selected", map, this._context, id);
		},
		_sendMessage: function (name, row, context, id) {
			var eventBus = qx.event.message.Bus;
			eventBus.getInstance().dispatchByName(name, {
				row: row,
				context: context,
				idMaster: id
			});
		},
		_createChildControlImpl: function (id) {
			var control;

			switch (id) {
			case "table":
				this.tableModel = new qx.ui.table.model.Simple();
				this.tableModel.setColumns(this._colHds, this._colIds);
				var self = this;
				var customMap = {
					tableColumnModel: function (obj) {
						if (self._props.horizontal_scroll === true) {
							return new qx.ui.table.columnmodel.Basic(obj);
						} else {
							return new qx.ui.table.columnmodel.Resize(obj);
						}
					},
					tablePaneScroller: function (obj) {
            var paneScroller = new qx.ui.table.pane.Scroller(obj);
						if (self._props.horizontal_scroll === true) {
							paneScroller.setHorizontalScrollBarVisible(true)
						} else {
							paneScroller.setLiveResize(true);
							paneScroller.setHorizontalScrollBarVisible(false)
						}
						return paneScroller;
					}
				};
				control = this.table = new qx.ui.table.Table(this.tableModel, customMap); //.set({});
				this.table.setUserData("context", this._context);
				var columnModel = this.table.getTableColumnModel();
				for (var c = 0; c < this._colRenderer.length; c++) {
					columnModel.setDataCellRenderer(c, this._colRenderer[c]);
				}
				for (var c = 0; c < this._colRenderer.length; c++) {
					this.table.setContextMenuHandler(c, this._context.contextmenu);
				}
				for (var c = 0; c < this._colEditor.length; c++) {
					if (this._colEditor[c]) {
						columnModel.setCellEditorFactory(c, this._colEditor[c]);
						this.tableModel.setColumnEditable(c, true);
					}
				}
				control.setStatusBarVisible(false);
				control.addListener("columnVisibilityMenuCreateStart", function (e) {
					var menu = e.getData().menu;
					console.error("Menu:" + menu);
					var item = new qx.ui.menu.Button(this.tr("widgets.table.save_settings"), "icon/16/actions/document-save.png");
					item.addListener("execute", function (e) {
						console.error("Button");
						this._saveTableSetup();
					}, this);
					menu.add(item);
				}, this);
				break;
			case "toolbar":
				control = new qx.ui.toolbar.ToolBar().set({});
				control.setSpacing(5);

				break;
			}

			return control || this.base(arguments, id);
		},
		_onDblClick: function (e) {
			console.log("modelForm:" + this._context.modelForm);
			if (!this._context.modelForm) return;
			if (!this.__updateAccess() && !this.__viewAccess()) return;
			if (this._disabledButton.contains("edit")) return;
			this._editButtonHandler();
		},
		_addButtons: function (toolbar, buttons) {
			for (var i = 0; i < buttons.length; i++) {
				var bd = buttons[i];
				var b = new qx.ui.toolbar.Button(bd.label, bd.icon);
				if (bd.tooltip) {
					b.setToolTipText(bd.tooltip);
				}
				b.addListener("execute", bd.callback, this);
				toolbar.add(b);
				if (bd.value == "del") {
					this._buttonDel = b;
					b.setEnabled(false);
				}
				if (bd.value == "add") {
					this._buttonAdd = b;
				}
				if (bd.value == "edit") {
					this._buttonEdit = b;
					b.setEnabled(false);
				}
				if (bd.value == "copy") {
					this._buttonCopy = b;
					b.setEnabled(false);
				}
				if (bd.value == "select") {
					this._buttonSelect = b;
					b.setEnabled(false);
				}
				if (bd.value == "import") {
					this._buttonImport = b;
					b.setEnabled(false);
				}
				if (bd.value == "export") {
					this._buttonExport = b;
					b.setEnabled(false);
				}
			}
		},
		_createButton: function (text, icon) {
			var b = new qx.ui.toolbar.Button(text, icon);
			b.setPaddingTop(1);
			b.setPaddingBottom(1);
			return b;
		},
		_addDefaultButtons: function (toolbar) {
			var menuPart1 = new qx.ui.toolbar.Part;
			var menuPart2 = new qx.ui.toolbar.Part;
			toolbar.add(menuPart1);
			toolbar.addSpacer();
			toolbar.add(menuPart2);
			toolbar.addSpacer();
			if( this.__isDocumentEntity()){
				this._disabledButton.push("copy");
			}
			if( this.__isTeamEntity()){
				this._disabledButton.push("add");
				this._disabledButton.push("copy");
				this._disabledButton.push("del");
				this._disabledButton.push("select");
			}
			if( this.__isNoAddDelInMaster()){
				this._disabledButton.push("add");
				this._disabledButton.push("copy");
				this._disabledButton.push("del");
				this._disabledButton.push("import");
			}
			var buttonMap = this._buttonMap;
			if (this._context.configMaster !== undefined && this._context.dependent == false) {
				if (this.__updateAccessMaster()) {
					if (!this._disabledButton.contains("select")) {
						menuPart1.add(buttonMap["select"]);
					}
				}
			}
			if (this.__updateAccess()) {
				if (!this._disabledButton.contains("add")) {
					menuPart1.add(buttonMap["add"]);
				}
			}
			if (this.__updateAccess()) {
				if (!this._disabledButton.contains("copy")) {
					menuPart1.add(buttonMap["copy"]);
				}
			}
			if (this.__updateAccess() || this.__viewAccess()) {
				if (!this._disabledButton.contains("edit")) {
					menuPart1.add(buttonMap["edit"]);
				}
			}
			if (this.__importAccess()) {
				if (!this._disabledButton.contains("import")) {
					menuPart2.add(buttonMap["import"]);
				}
			}
			if (this.__exportAccess()) {
				if (!this._disabledButton.contains("export")) {
					menuPart2.add(buttonMap["export"]);
				}
			}
			if (this.__deleteAccess()) {
				if (!this._disabledButton.contains("del")) {
					menuPart1.add(buttonMap["del"]);
				}
			}
			var menuPart3 = new qx.ui.toolbar.Part;
			if (this._props.paging === undefined || this._props.paging == true) {
				toolbar.add(menuPart3);
			}
			menuPart3.add(buttonMap["first"]);
			menuPart3.add(buttonMap["prev"]);
			menuPart3.add(buttonMap["next"]);
			menuPart3.add(buttonMap["last"]);
			this._status = new qx.ui.basic.Label("").set({
				marginTop: 0,
				paddingTop: 4,
				paddingBottom: 0
			});
			this._status.setRich(true);
			menuPart3.add(this._status);
		},
		_createDefaultButtons: function (toolbar) {
			var buttonMap = {};
			this._buttonSelect = this._createButton("", "icon/16/actions/edit-paste.png");
			this._buttonSelect.setToolTipText(this.tr("widgets.table.select_button"));
			this._buttonSelect.addListener("execute", function () {
				this._selectButtonHandler();
			}, this);
			buttonMap["select"] = this._buttonSelect;

			this._buttonAdd = this._createButton("", "icon/16/actions/list-add.png");
			this._buttonAdd.setToolTipText(this.tr("widgets.table.add_button"));
			this._buttonAdd.addListener("execute", function () {
				if (this._context.noEdit !== true) {
					this._currentIndex = -1;
					this._addEditForm();
					if (this._context.preAddFunc) {
						this._context.preAddFunc(this._editForm);
					}
					this._editForm.setMode("add");
					this._editForm.fillForm({});
					this._editForm.beforeAdd({
						storeDesc: this.__storeDesc,
						parentData: this._parentData,
						data: this._editForm.getData()
					});
					this._editWindow.setActive(true);
					this._editWindow.open();
					this._setPrevNextButtons(false);
				}
			}, this);
			buttonMap["add"] = this._buttonAdd;

			this._buttonCopy = this._createButton("", "icon/16/actions/edit-copy.png");
			this._buttonCopy.setToolTipText(this.tr("widgets.table.copy_button"));
			this._buttonCopy.addListener("execute", function () {
				if (this._context.noEdit === true) {
					return;
				}
				var selModel = this.table.getSelectionModel();
				var index = selModel.getLeadSelectionIndex();
				var map = this._currentRowData = this.tableModel.getRowDataAsMap(index);
				var count = selModel.getSelectedCount();
				if (count == 0) {
					return;
				}
				this._currentIndex = index;
				this._addCopyForm();
				this._copyWindow.setActive(true);
				this._copyWindow.open();
				this._setPrevNextButtons(false);
				if (this._loadBeforeEdit) {
					this.__fillForm(this._copyForm, map);
				} else {
					this._copyForm.fillForm(map);
				}
			}, this);
			buttonMap["copy"] = this._buttonCopy;

			if (this.__updateAccess()) {
				this._buttonEdit = this._createButton("", "icon/16/apps/utilities-text-editor.png");
				this._buttonEdit.setToolTipText(this.tr("widgets.table.edit_button"));
			} else {
				this._buttonEdit = this._createButton("", "icon/16/apps/utilities-dictionary.png");
				this._buttonEdit.setToolTipText(this.tr("widgets.table.view_button"));
			}
			this._buttonEdit.addListener("execute", function () {
				this._editButtonHandler();
			}, this);
			buttonMap["edit"] = this._buttonEdit;

			this._buttonImport = this._createButton("", "icon/16/actions/document-revert.png");
			this._buttonImport.setToolTipText(this.tr("widgets.table.import_button"));
			this._buttonImport.addListener("execute", function () {
				var context = {};
				context.user = this._context.user;
				context.model = this._context.modelForm;
				context.storeDesc = this.__storeDesc;
				context.mainEntity = this._context.config;
				context.fileType = "csv";
				context.prefix = "user/" + context.user.userid + "/" + context.mainEntity;
				context.window_title = "Import(" + context.mainEntity + ")";
				new ms123.DesktopWindow(context, ms123.importing.Importing);
			}, this);
			buttonMap["import"] = this._buttonImport;

			this._buttonExport = this._createButton("", "icon/16/actions/view-restore.png");
			this._buttonExport.setToolTipText(this.tr("widgets.table.export_button"));
			this._buttonExport.addListener("execute", function () {
				var context = {};
				if (!this.filter) return;
				var url = this._buildUrl(false);
				var cm = new ms123.config.ConfigManager();
				var fields = cm.getEntityViewFields(this._context.config, this.__storeDesc, "export", false);
				var fieldList = [];
				var aliasList = [];
				for (var i = 0; i < fields.length; i++) {
					var map = fields[i];
					fieldList.push(map.name);
					aliasList.push(map.displayname);
				}
				context.filter = (typeof this.filter == "string") ? this.filter : qx.util.Serializer.toJson(this.filter);
				context.url = url;
				context.fields = qx.util.Serializer.toJson(fieldList);
				context.aliases = qx.util.Serializer.toJson(aliasList);
				context.mainModule = this._context.config;
				context.storeDesc = this.__storeDesc;
				new ms123.exporter.ExportDialog(context);
			}, this);
			buttonMap["export"] = this._buttonExport;

			if (this._context.configMaster !== undefined && this._context.dependent == false) {
				this._buttonDel = this._createButton("", "icon/16/actions/list-remove.png");
				this._buttonDel.setToolTipText(this.tr("widgets.table.unassign_button"));
			} else {
				this._buttonDel = this._createButton("", "icon/16/places/user-trash.png");
				this._buttonDel.setToolTipText(this.tr("widgets.table.del_button"));
			}
			this._buttonDel.addListener("execute", function () {
				var selModel = this.table.getSelectionModel();
				var index = selModel.getLeadSelectionIndex();
				var map = this._currentRowData = this.tableModel.getRowDataAsMap(index);
				var count = selModel.getSelectedCount();
				if (count == 0) {
					return;
				}
				if (this._context.configMaster === undefined || this._context.dependent == true) {
					ms123.form.Dialog.confirm(this.tr("table.confirm.delete"), function (e) {
						if (e) {
							var id = map[this._keyColumn];
							var rpcParams = this._buildBaseRpcParams(false, id);
							//rpcParams.id = id+"";
							var completed = function (data) {
								this.tableModel.removeRows(index, 1);
								this.table.resetSelection();
								if (this._context.isMaster == true) {
									this._sendMessage(this._context.unit_id + ".table.row.selected", {}, this._context, null);
								}
								this.fireDataEvent("deleteRecord", {}, null);
							};

							try {
								var ret = this.__dataAccess["delete"](rpcParams);
								completed.call(this, ret);
							} catch (e) {
								this._showDetails(e);
							}
						}
					}, this);
				} else {
					var id = map[this._keyColumn];
					this.tableModel.removeRows(index, 1);
					this.table.resetSelection();
					this.__updateAssignment(id, false);
				}
			}, this);
			buttonMap["del"] = this._buttonDel;

			this._buttonFirst = this._createButton("", "icon/16/actions/go-first.png").set({});
			this._buttonFirst.addListener("execute", function () {
				this.page = 1;
				this.loadData();
			}, this);
			buttonMap["first"] = this._buttonFirst;

			this._buttonPrev = this._createButton("", "icon/16/actions/go-previous.png").set({});
			this._buttonPrev.addListener("execute", function () {
				this.page--;
				this.loadData();
			}, this);
			buttonMap["prev"] = this._buttonPrev;

			this._buttonNext = this._createButton("", "icon/16/actions/go-next.png").set({});
			this._buttonNext.addListener("execute", function () {
				this.page++;
				this.loadData();
			}, this);
			buttonMap["next"] = this._buttonNext;

			this._buttonLast = this._createButton("", "icon/16/actions/go-last.png").set({});
			this._buttonLast.addListener("execute", function () {
				this.page = this.total;
				this.loadData();
			}, this);
			buttonMap["last"] = this._buttonLast;

			this._buttonFirst.setEnabled(false);
			this._buttonNext.setEnabled(false);
			this._buttonPrev.setEnabled(false);
			this._buttonLast.setEnabled(false);
			this._buttonDel.setEnabled(false);
			this._buttonEdit.setEnabled(false);
			this._buttonCopy.setEnabled(false);
			this._buttonMap = buttonMap;
		},

		_selectButtonHandler: function () {
			var _this = this;
			var context = {};
			context.modulename = this._context.config;
			context.user = this._context.user;
			context.title = this.tr("widgets.table.select_record");
			context.storeDesc = this.__storeDesc;
			context.selected_callback = function (value) {
				_this.tableModel.addRowsAsMapArray([value], null, true);
				if (!_this._idMaster) {
					alert("nothing selected");
					return;
				}
				var id = value[_this._keyColumn];
				_this.__updateAssignment(id, true);
			};
			new ms123.util.RecordSelector(context);
		},

		__updateAssignment: function (id, assign) {
			var data = {};
			data[this._context.fieldname] = [{
				id: id,
				assign: assign
			}];

			var hints = {};
			hints[this._context.fieldname] = {
				mode: "assign"
			};

			var params = {
				storeDesc: this.__storeDesc,
				entity: this._context.configMaster,
				id: this._idMaster + "",
				data: data,
				hints: hints
			}

			try {
				this.__dataAccess.update(params);
				ms123.form.Dialog.alert(this.tr("data.form.saved"));
			} catch (e) {
				this._showDetails(e);
			}
		},

		_editButtonHandler: function () {
			if (!this._editWindow) return;
			var selModel = this.table.getSelectionModel();
			var index = selModel.getLeadSelectionIndex();
			var map = this._currentRowData = this.tableModel.getRowDataAsMap(index);
			var count = selModel.getSelectedCount();
			if (count == 0) {
				return;
			}
			this._currentIndex = index;
			this._addEditForm();
			this._setPrevNextButtons(true);
			if (this._loadBeforeEdit) {
				var completed = function (map) {
					this._editForm.beforeEdit({
						storeDesc: this.__storeDesc,
						parentData: this._parentData,
						mainEntity: this._context.config,
						data: map
					});
					if(this.__isDupState()){
						this._createDupCandidateEdit();
						this._dupCandidateEdit.open(map);
					}else{
						this._editForm.fillForm(map);
						this._editWindow.setActive(true);
						this._editWindow.open();
					}
				};

				var failed = function (e) {
					this._showDetails(e);
				};

				var p = this._buildGetRpcParams(map[this._keyColumn]);
				p.storeDesc = this.__storeDesc, p.async = false;
				p.failed = failed;
				p.completed = completed;
				p.context = this;
				this.__dataAccess.queryOne(p);

			} else {
				if (this._context.preEditFunc) {
					this._context.preEditFunc(this._editForm, map);
				}
				this._editForm.beforeEdit({
					storeDesc: this.__storeDesc,
					parentData: this._parentData,
					data: map
				});
				this._editForm.fillForm(map);
				this._editWindow.setActive(true);
				this._editWindow.open();
			}
		},
		loadData: function () {
			this._showProgressBar();

			var completed = function (e) {
				var crows = 0;
				//var data = e.getContent();
				var data = e;
				this.tableModel.removeRows(0, this.tableModel.getRowCount());
				this.records = data.records;
				if (this.records == 0) {
					this.total = 0;
				} else {
					this.total = data.total;
					crows = data.rows.length;
					this.tableModel.setRowsAsMapArray(data.rows, 0, true);
				}
				if (this._buttonNext) {
					if (this.page < this.total) {
						this._buttonNext.setEnabled(true);
						this._buttonLast.setEnabled(true);
					} else {
						this._buttonNext.setEnabled(false);
						this._buttonLast.setEnabled(false);
					}
					if (this.page > 1) {
						this._buttonPrev.setEnabled(true);
						this._buttonFirst.setEnabled(true);
					} else {
						this._buttonPrev.setEnabled(false);
						this._buttonFirst.setEnabled(false);
					}
					if (this.total == -1) {
						this._buttonNext.setEnabled(true);
						this._buttonLast.setEnabled(false);
					}
				}
				if (this._status) {
					var start = this.rows * (this.page - 1) + 1;
					var end = start + crows - 1;
					if (start > end) start = end;
					this._status.setValue("<b>Zeige " + start + " - " + end + " von " + this.records + "</b>");
				}
				if (this._context.isMaster == true) {
					this._sendMessage(this._context.unit_id + ".table.row.selected", {}, this._context, null);
				}
				this._stopProgressBar();
			};

			var failed = function (e) {
				this._showDetails(e);
				this._stopProgressBar();
			};

			var timeout = function (e) {
				alert("failed2:" + e);
				this._stopProgressBar();
			};

			var async = true;
			if (this._context.loadSync !== undefined && this._context.loadSync == true) {
				async = false;
			}
			var p = this._rpcParams;
			if (this.filter) {
				if (typeof this.filter == "string") {
					p.filter = qx.lang.Json.parse(this.filter);
				} else {
					p.filter = this.filter;
				}
			}
			p.storeDesc = this.__storeDesc, p.pageSize = this.rows;
			p.page = this.page;
			p.async = async;
			p.failed = failed;
			p.completed = completed;
			p.context = this;
			this.__dataAccess.query(p);
		},
		_showDetails: function (details) {
			console.log("_showDetails:"+details.stack);
			ms123.form.Dialog.alert(details.message);
		},
		_buildUrl: function (withOrderby) {
			var idMaster = this._idMaster ? this._idMaster : "";
			var url = null;
			console.log("path:" + this._props.path);
			if (this._props.path === undefined || this._props.path == null || this._props.path === "") {
				if (this._context.configMaster === undefined) {
					url = "data/" + this._context.config;
				} else {
					url = "data/" + this._context.configMaster + "/" + idMaster + "/" + this._context.fieldname;
				}
			} else {
				url = this._props.path;
				if (this._context.configMaster !== undefined) {
					if (url.indexOf("/%m/") != -1) {
						url = url.replace("/%m/", "/" + idMaster + "/");
					}
				}
			}
			var append = url.indexOf("?") != -1 ? "&" : "?";
			if (this._props.orderby && withOrderby) {
				url += (append + "orderby=" + this._props.orderby);
			}
			console.log("BuildUrl:" + url);
			return url;
		},
		_buildGetRpcParams: function (key) {
			var params = {};
			if (this._props.path) {
				var path = this._props.path + "/" + key;
				if (this._context.configMaster !== undefined) {
					var idMaster = this._idMaster ? this._idMaster : "";
					if (path.indexOf("/%m/") != -1) {
						path = path.replace("/%m/", "/" + idMaster + "/");
					}
					params.entity = this._context.configMaster;
				} else {
					params.entity = this._context.config;
				}
				params.pathInfo = path;
			} else {
				params.entity = this._context.config;
			}
			params.id = key + "";
			params.namespace = this.__storeDesc.getNamespace();
			params.pack = this.__storeDesc.getPack();
			params.storeId = this.__storeDesc.getStoreId();
			return params;
		},
		_buildBaseRpcParams: function (withOrderby, id) {
			var params = {};
			if (id) id = id + "";
			var idMaster = this._idMaster ? this._idMaster : "";
			if (this._context.configMaster === undefined) {
				params.entity = this._context.config;
				params.id = id;
			} else {
				params.entity = this._context.configMaster;
				params.id = idMaster + "";
				params.entityChild = this._context.fieldname;
				params.idChild = id;
			}
			if (this._entityConfig && this._entityConfig.path) {
				params.pathInfo = this._entityConfig.path;
			} else if (this._props.path) {
				params.pathInfo = this._props.path;
			}
			params.storeDesc = this.__storeDesc;
			params.namespace = this.__storeDesc.getNamespace();
			params.pack = this.__storeDesc.getPack();
			params.storeId = this.__storeDesc.getStoreId();
			if (this._props.orderby && withOrderby) {
				params.orderby = this._props.orderby;
			}
			return params;
		},
		currentRecordDown: function () {
			this.table.stopEditing();
			var rc = this.tableModel.getRowCount();
			if (this._currentIndex >= (rc - 1)) return;
			var curRecord = this.getRecord(this._currentIndex);
			this.deleteRecord(this._currentIndex);
			this.insertRecord(curRecord, this._currentIndex + 1);
			var selModel = this.table.getSelectionModel();
			selModel.setSelectionInterval(this._currentIndex + 1, this._currentIndex + 1);
		},
		currentRecordUp: function () {
			this.table.stopEditing();
			if (this._currentIndex == 0) return;
			var curRecord = this.getRecord(this._currentIndex);
			this.deleteRecord(this._currentIndex);
			this.insertRecord(curRecord, this._currentIndex - 1);
			var selModel = this.table.getSelectionModel();
			selModel.setSelectionInterval(this._currentIndex - 1, this._currentIndex - 1);
		},
		addRecord: function (map) {
			this.tableModel.addRowsAsMapArray([map], null, true);
		},
		setCurrentRecord: function (map) {
			this.tableModel.setRowsAsMapArray([map], this._currentIndex, true);
		},
		getCurrentRecord: function () {
			if (this._currentIndex > -1) {
				return this.tableModel.getRowDataAsMap(this._currentIndex);
			}
			return null;
		},
		getRecord: function (row) {
			var map = this.tableModel.getRowDataAsMap(row);
			return map;
		},
		getData: function () {
			var arr = this.tableModel.getDataAsMapArray();
			return arr;
		},
		setData: function (data) {
			//	this.tableModel.removeRows(0,this.tableModel.getRowCount());
			this.tableModel.setDataAsMapArray(data, true);
		},
		clearData: function () {
			this.tableModel.removeRows(0, this.tableModel.getRowCount());
		},
		getIdArray: function () {
			var ret = [];
			var arr = this.tableModel.getDataAsMapArray();
			for (var i = 0; i < arr.length; i++) {
				var row = arr[i];
				ret.push(row[this._keyColumn]);
			}
			return ret;
		},
		insertRecord: function (map, pos) {
			this.tableModel.addRowsAsMapArray([map], pos, true);
		},
		deleteRecord: function (row) {
			this.table.stopEditing();
			this.tableModel.removeRows(row, 1);
		},
		deleteCurrentRecord: function () {
			this.table.stopEditing();
			if (this._currentIndex > -1) {
				this.tableModel.removeRows(this._currentIndex, 1);
			}
		},
		getRowCount: function () {
			return this.tableModel.getRowCount();
		},
		setFilter: function (filter,state) {
			this.filter = filter;
			this._rpcParams = this._buildBaseRpcParams(true, null);
			if(state)this._rpcParams.state=state;
			this.page = 1;
			this.rows = 100;
			this._postData = null;
			this.loadData();
		},
		setRpcParams: function (rpcParams) {
			var p = this._buildBaseRpcParams(true, null);
			this._rpcParams = {};
			ms123.util.Clone.merge(this._rpcParams, p, rpcParams);
			this.page = 1;
			this.rows = rpcParams.pageSize ? rpcParams.pageSize : 100;
			this.filter = null;
			this.loadData();
		},
		setUrl: function (url, postData) {
			alert("setUrl notallowed");
			this.url = url;
			this.filter = "";
			this.page = 1;
			this.rows = 100;
			this._postData = postData;
			console.log("setUrl:" + url);
			this.loadData();
		},
		showRecord: function (id, parentData) {
			if (id == null) {
				this.tableModel.removeRows(0, this.tableModel.getRowCount());
				return;
			}
			this._idMaster = id;
			this._parentData = parentData;
			if (this._context.toolbar && qx.Class.implementsInterface(this._context.toolbar, ms123.widgets.IToolbar)) {
				this._context.toolbar.setParentData(id, parentData);
			}
			//this.url = this._buildUrl(true);
			this._rpcParams = this._buildBaseRpcParams(true, null);
			this.filter = null;
			this.page = 1;
			this.rows = 100;
			this._postData = null;
			this.loadData();
		},
		_stopProgressBar: function () {
			//		this.bar.update("100%");
			// 	this.getApplicationRoot().remove(this.bar);
			//	this.bar = null;
			this.getApplicationRoot().setGlobalCursor("default");
		},
		_showProgressBar: function () {
			//	if( this.bar ) return;
			//	this.bar = new ms123.util.ProgressBar();
			//	this.bar.set( { width          : 300, showPcntStatus : true, proportion     : "50%" });
			// this.getApplicationRoot().add(this.bar, { top  : 5, left : 300 });
			//this.bar.showOff();
			this.getApplicationRoot().setGlobalCursor("wait");
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
		_createEditWindow: function () {
			var win = new qx.ui.window.Window(this._context.title, "").set({
				resizable: true,
				useMoveFrame: true,
				useResizeFrame: true
			});
			win.setLayout(new qx.ui.layout.Grow);
			win.setWidth(680);
			win.setHeight(460);
			win.setAllowMaximize(false);
			win.setAllowMinimize(false);
			var props = this.__configManager.getEntityViewProperties(this._context.config, this.__storeDesc, "main-form");
			var modal = true;
			if( props.modal === false ) modal = false;
			win.setModal(modal);
			win.setActive(false);
			win.minimize();
			win.center();
			this.getApplicationRoot().add(win);
			return win;
		},
		__fillForm: function (form, map) {
			var self = this;
			var completed = function (data) {
				form.beforeEdit({
					storeDesc: self.__storeDesc,
					parentData: self._parentData,
					data: data
				});
				form.fillForm(data);
			};
			var failed = function (e) {
				this._showDetails(e);
			};

			var p = this._buildGetRpcParams(map[this._keyColumn]);
			p.storeDesc = this.__storeDesc, p.async = false;
			p.failed = failed;
			p.completed = completed;
			p.context = this;
			this.__dataAccess.queryOne(p);

		},
		_checkData: function (data) {
			try {
				var result = ms123.util.Remote.rpcSync("quality:dupCheck", {
					namespace: this.__storeDesc.getNamespace(),
					entityName: this._context.config,
					candidateList: [data]
				});
				console.log("Result:" + JSON.stringify(result, null, 2));
				return result;
			} catch (e) {
				ms123.form.Dialog.alert("DupCandidateEdit._checkData:" + e);
				return [];
			}
		},
		_createEditForm: function (copy) {
			var self = this;
			var buttons = [{
				'label': "",
				'icon': "icon/22/actions/go-previous.png",
				'callback': function (m) {
					var rc = self.tableModel.getRowCount();
					if (self._currentIndex > 0) {
						self._currentIndex--;
						var map = self._currentRowData = self.tableModel.getRowDataAsMap(self._currentIndex);
						var selModel = self.table.getSelectionModel();
						selModel._resetSelection();
						self.table.setFocusedCell(0, self._currentIndex, true);
						selModel.setSelectionInterval(self._currentIndex, self._currentIndex);
						if (self._loadBeforeEdit) {
							self.__fillForm(self._editForm, map);
						} else {
							self._editForm.beforeEdit({
								storeDesc: self.__storeDesc,
								parentData: self._parentData,
								data: map
							});
							self._editForm.fillForm(map);
						}
					}
					self._setPrevNextButtons(true);
				},
				'value': "prev"
			},
			{
				'label': "",
				'icon': "icon/22/actions/go-next.png",
				'callback': function (m) {
					var rc = self.tableModel.getRowCount();
					if (self._currentIndex < (rc - 1)) {
						self._currentIndex++;
						var map = self._currentRowData = self.tableModel.getRowDataAsMap(self._currentIndex);
						var selModel = self.table.getSelectionModel();
						selModel._resetSelection();
						selModel.setSelectionInterval(self._currentIndex, self._currentIndex);
						self.table.setFocusedCell(0, self._currentIndex, true);
						if (self._loadBeforeEdit) {
							self.__fillForm(self._editForm, map);
						} else {
							self._editForm.beforeEdit({
								storeDesc: self.__storeDesc,
								parentData: self._parentData,
								data: map
							});
							self._editForm.fillForm(map);
						}
					}
					self._setPrevNextButtons(true);
				},
				'value': "next"
			},
			{
				'label': this.tr("tasks.usertasks.dublettencheck.checkit"),
				'icon': "icon/22/actions/edit-redo.png",
				'callback': function (m) {
					if(!self._validateForm(this.form)) return;
					var map = {};
					if (!copy && self._currentIndex > -1) { //Edit
						ms123.util.Clone.merge(map, self._currentRowData, m);
					}else{
						map = m;
					}
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
				'label': this.tr("data.form.save"),
				'icon': "icon/22/actions/dialog-ok.png",
				'callback': function (m) {
					var validate = this.form.validate();
					console.error("validate:" + validate);
					if (!validate) {
						var vm = this.form._form.getValidationManager();
						var items = vm.getInvalidFormItems();
						var message = "<br />";
						for (var i = 0; i < items.length; i++) {
							var name = items[i].getUserData("key");
							var msg = items[i].getInvalidMessage();
							message += name + " : " + msg + "<br />";
						}
						ms123.form.Dialog.alert(self.tr("widgets.table.form_incomplete") + ":" + message);
						return;
					}

					var map = {};
					var url = self._buildUrl(false);
					var method = "insert";
					var add = false;
					var id = null;
					if (!copy && self._currentIndex > -1) { //Edit
						ms123.util.Clone.merge(map, self._currentRowData, m);
						self.tableModel.setRowsAsMapArray([map], self._currentIndex, true);
						id = map[self._keyColumn] + "";
						method = "update";
						url = url + '/' + id;
					} else { //Add
						qx.lang.Object.mergeWith(map, m);
						if (map[self._keyColumn]) {
							id = map[self._keyColumn] + "";
						} else {}
						add = true;
					}
					var params = self._buildBaseRpcParams(false, id);;
					params.data = map;


					var completed = function (e) {
						var content = e;
						if (typeof content == "string") {
							ms123.form.Dialog.alert(message);
						}

						var cv = e["constraintViolations"];
						if (cv) {
							var message = "";
							if( cv.length>0 && cv[0].idHitList){
								message = self.tr("data.record_exists")+"<br />";
							}
							for (var i = 0; i < cv.length; i++) {
								var c = cv[i];
								message += this.getLabel(c.path) + " : " + c.message + "<br />";
							}
							ms123.form.Dialog.alert(message);
							this.setErrors(cv);
							return;
						}

						var id = add ? content["id"] : map[self._keyColumn];
						if (copy || self._currentIndex == -1) { //Add  //Copy???
							map[self._keyColumn] = id;
							self.tableModel.addRowsAsMapArray([map], null, true);
							this.fillForm({});
							ms123.form.Dialog.alert(self.tr("data" + ".form.created"));
						} else {
							ms123.form.Dialog.alert(self.tr("data" + ".form.saved"));
						}
						if (self._context.isMaster == true || self._context.isMaster == undefined) {
							self._sendMessage(self._context.unit_id + ".table.row.selected", map, self._context, id);
							if (add) {
								self.fireDataEvent("addRecord", map, null);
							} else {
								self.fireDataEvent("changeRecord", map, null);
							}
						}
						this.setAllValid();
						if( self._context.configMaster != undefined){//@@@MS ??? Der hack, wird wohl so passen.
							delete map[self._context.configMaster];
							params.entity =self._context.config;
						}
						this.afterSave({
							id: id,
							map: map,
							service: "data",
							method: method,
							rpcParams: params,
							url: url
						});
					};
					this.beforeSave({
						data: map
					});

					try {
						var ret = null;
						if (add) ret = self.__dataAccess.insert(params);
						else ret = self.__dataAccess.update(params);
						completed.call(this, ret);
					} catch (e) {
						self._showDetails(e);
					}

				},
				'value': "save"
			}];
			var context = {};
			context.useitCheckboxes = copy;
			context.useitCheckboxesDefault = copy;
			context.buttons = buttons;
			context.dataAccess = this._context.dataAccess;
			context.model = this._context.modelForm;
			context.unit_id = this._context.unit_id;
			context.config = this._context.config;
			context.storeDesc = this._context.storeDesc;
			context.dependent = this._context.dependent;
			var form = new ms123.widgets.Form(context);
			for (var i = 0; i < context.buttons.length; i++) {
				var b = context.buttons[i];
				if (b.value == 'prev') {
					this._prevButton = b.button;
				}
				if (b.value == 'next') {
					this._nextButton = b.button;
				}
				if (b.value == 'save') {
					b.button.setEnabled(false);
					if (this.__updateAccess()) {
						b.button.setEnabled(true);
					} else if (this.__viewAccess()) {}
				}
				if (b.value == 'check') {
					b.button.setEnabled(true);
					if( this.__hasDupCheck()==false){
						b.button.setEnabled(false);
					}
				}
			}
			return form;
		},
		_setPrevNextButtons: function (enabled) {
			var rc = this.tableModel.getRowCount();
			this._prevButton.setEnabled(enabled);
			this._nextButton.setEnabled(enabled);
			if (this._currentIndex == 0) {
				this._prevButton.setEnabled(false);
			}
			if (this._currentIndex == (rc - 1)) {
				this._nextButton.setEnabled(false);
			}
		},
		_addCopyForm: function () {
			if (!this._copyForm) {
				this._copyForm = this._createEditForm(true);
				this._copyWindow.add(this._copyForm);
			}
		},
		_addEditForm: function () {
			if (!this._editForm) {
				this._editForm = this._createEditForm(false);
				this._editWindow.add(this._editForm);
			}
		},

		_createDupCandidateEdit: function () {
			if (!this._dupCandidateEdit) {
				var context = {};
				context.table = this;
				context.entityName = this._context.config;
				context.storeDesc = this.__storeDesc;
				this._dupCandidateEdit = new ms123.widgets.DupCandidateEdit(context);
			}
		},
		__isDupState:function(){
			var stateSelect = this._context.stateSelect;
			if( stateSelect){
				var state = stateSelect.getModelSelection().getItem(0);
				if( state == "dup"){
					return true;
				}
			}
			return false;
		},

		__hasDupCheck:function(){
			var dupProps = this.__configManager.getEntityViewProperties(this._context.config, this.__storeDesc, "duplicate-check");
			if( dupProps.fieldsets && dupProps.fieldsets.length>0){
				return true;
			} 
			var dupFields = this.__configManager.getEntityViewFields(this._context.config, this.__storeDesc,    "duplicate-check");
			if( dupFields.length>0){
				return true;
			}
			return false;
		},
		__isDocumentEntity:function(){
			if( this._context.config=="document")return true;
			return false;
		},
		__isTeamEntity:function(){
			if( this._context.config=="team")return true;
			return false;
		},
		__isNoAddDelInMaster:function(){
			if( this._context.isMaster && this._context.no_add_del_in_master)return true;
			return false;
		},
		__updateAccessMaster: function () {
			if (this._context.user.admin) return true;
			if (this.__accessMaster["update"] === undefined || this.__accessMaster["update"] == "all" || this.__accessMaster["update"] == "owner") return true;
			return false;
		},
		__updateAccess: function () {
			if (this._context.user.admin) return true;
			if (this.__access["update"] === undefined || this.__access["update"] == "all" || this.__access["update"] == "owner") return true;
			return false;
		},
		__exportAccess: function () {
			if (this._context.user.admin) return true;
			if (this.__access["export"] === undefined || this.__access["export"] == "all" || this.__access["export"] == "owner") return true;
			return false;
		},
		__importAccess: function () {
			if (this._context.user.admin) return true;
			if (this.__access["import"] === undefined || this.__access["import"] == "all" || this.__access["import"] == "owner") return true;
			return false;
		},
		__viewAccess: function () {
			if (this._context.user.admin) return true;
			if (this.__access["view"] === undefined || this.__access["view"] == "all" || this.__access["view"] == "owner") return true;
			if (this.__access["list"] === undefined || this.__access["list"] == "all" || this.__access["list"] == "owner") return true;
			return false;
		},
		__deleteAccess: function () {
			if (this._context.user.admin) return true;
			if (this.__access["delete"] === undefined || this.__access["delete"] == "all" || this.__access["delete"] == "owner") return true;
			return false;
		}
	},
	destruct: function () {
		if (this._editWindow) {
			//			this.getApplicationRoot().remove(this._editWindow);
			//			this._disposeObjects("_editWindow");
		}
		if (this._copyWindow) {
			//			this.getApplicationRoot().remove(this._copyWindow);
			//			this._disposeObjects("_copyWindow");
		}
	}

});
