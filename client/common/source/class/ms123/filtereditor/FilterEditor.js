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
	@ignore(jQuery)
	@ignore(jQuery.each)
	@ignore(jQuery.inArray)
	@ignore(jQuery.parseJSON)
	@lint ignoreDeprecated(alert,eval) 
	@asset(qx/icon/${qx.icontheme}/22/actions/*)
	@asset(qx/icon/${qx.icontheme}/16/places/*)
	@asset(qx/icon/${qx.icontheme}/22/status/*)
	@asset(qx/icon/${qx.icontheme}/16/apps/*)
	@asset(qx/icon/${qx.icontheme}/16/mimetypes/*)
	@asset(qx/icon/${qx.icontheme}/16/categories/*)

	@asset(ms123/icons/*)
	@asset(ms123/*)
*/

qx.Class.define("ms123.filtereditor.FilterEditor", {
	extend: qx.ui.container.Composite,
	include: qx.locale.MTranslation,


	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (context) {
		this.base(arguments);
		this.context = context;
		this.setLayout(new qx.ui.layout.Grow());
		this.parentComponent = this;
		this.__storeDesc = context.storeDesc;
		this._cm = new ms123.config.ConfigManager();
	},


	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},

	events: {
		"save": "qx.event.type.Data"
	},

	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		init: function (name, path, modulename, fields, filter, exclusion) {
			this._filterProps = {
				id: name,
				name: name,
				path: path,
				user: null,
				type: "jcr",
				storeId: this.__storeDesc.getStoreId(),
				modulename: modulename,
				description: null,
				fields: fields,
				filter: filter,
				exclusion: exclusion
			};
			this._createJsonWindow();

			if (!modulename) {
				var modulesMenu = [];

				var _modules = this._cm.getEntities(this.__storeDesc);;
				for (var i = 0; i < _modules.length; i++) {
					var module = _modules[i];
					var modname = module.name;
					var o = {};
					o.label = this.tr("data." + modname);
					o.value = modname;
					modulesMenu.push(o);
				}
				var formData = {
					"modulename": {
						'type': "SelectBox",
						'label': this.tr("filtereditor.mainmodule"),
						'value': 1,
						'options': modulesMenu
					}
				};

				var self = this;
				var form = new ms123.form.Form({
					"formData": formData,
					"allowCancel": true,
					"inWindow": true,
					"callback": function (m) {
						if (m !== undefined) {
							var val = m.get("modulename");
							self._filterProps.modulename = val;
							self._init();
						}
					},
					"context": self
				});
				form.show();
			} else {
				this._init();
			}
		},
		_init: function () {
			if (!this._filterProps.modulename) return;

			this._user = ms123.config.ConfigManager.getUserProperties();
			this._fieldmap = {};
			this._need_filterrefresh = false;

			var mainTabView = new qx.ui.tabview.TabView().set({
				decorator: "main",
				contentPadding: 0,
				backgroundColor: "white"
			});

			this.parentComponent.add(mainTabView, {});
			this._mainTabs = mainTabView;

			this._makeTabs();
			this._mainMenu = this._createMainMenu();

			var fscontext = {};
			fscontext.tableColumns = [{
				name: "column",
				header: this.tr("meta.lists.fs.column")
			},
			{
				name: "display",
				type: "CheckBox",
				width: 60,
				header: this.tr("meta.lists.fs.display")
			},
			{
				name: "mapping",
				type: "TextField",
				header: this.tr("meta.lists.fs.mapping")
			}];
			fscontext.saveFields = (this._save).bind(this);
			fscontext.removeFromFieldTable = (function (map, treeModel, path, checkbox) {
				this._need_filterrefresh = true;
			}).bind(this);
			fscontext.addToFieldTable = (function (map, treeModel, path, checkbox) {
				this._need_filterrefresh = true;
				map.display = true;
				map.path = path;
				var prefix = this._removeFirstSegment(path.replace("$", "."));
				map.column = prefix + (prefix.length > 0 ? "." : "") + map.id;
				var fieldDesc = this._getFieldDesc(map.module, map.id);
				var dt = fieldDesc["datatype"];
				if (dt != null && dt.match("^array")) {
					map.display = false;
				}
				return map;
			}).bind(this);
			this._fieldSelector = new ms123.filtereditor.FieldSelector(fscontext);
			this._fieldSelector.addListener("treeClicked", function (e) {
				var model = e.getData().selectionModel;
				var path = e.getData().treePath;
				var fields = this._getSelectableFields(model.getEntity());
				this._fieldSelector.createFieldsWindow(path, model, fields);
			}, this);

			this._mainTabs.addListener("changeSelection", function (e) {
				var pid = e._target.getSelection()[0].getUserData("id");
				if (pid == "filter" && this._need_filterrefresh) {
					this._need_filterrefresh = false;
					var selFields = this._fieldSelector.getSelectedFields();
					this._createSearchFilter(this._filterPage, selFields, this._filterProps, this._mainModule);
				}
			}, this);


			this._mainModule = this._filterProps.modulename;
			var cm = new ms123.config.ConfigManager();
			var treeModel = cm.getEntityTree(this.__storeDesc,this._mainModule,5);

			this._fieldSelector.setTreeModel(this._translateModel(treeModel));
			this._fieldSelector.createTable(fscontext.tableColumns);

			var fields = this._filterProps.fields;
			if (!fields || fields == "") {
				this._fieldSelector.setSelectedFields(null);
			} else {
				if (fields && typeof fields == 'string' && fields != "") {
					fields = qx.lang.Json.parse(fields);
				}
				var f = this._filterFields(fields);
				this._fieldSelector.setSelectedFields(f);
			}
			//this._filterPage.setEnabled(false);
			this._resultPage.setEnabled(false);
			this._mainTabs.setSelection([this._fieldsPage]);

			this._createExclusionWindow(null, this._filterPage, this._filterProps);
			var selFields = this._fieldSelector.getSelectedFields();
			if (selFields && selFields.length > 0) {
				this._createSearchFilter(this._filterPage, selFields, this._filterProps, this._mainModule);
			}
			this._fieldsPage.add(this._fieldSelector);
		},

		_createSearchFilterFields: function (selFields) {
			var allops = ['eq', 'ne', 'lt', 'le', 'gt', 'ge', 'bw', 'bn', 'in', 'inn', 'ew', 'en', 'cn', 'nc'];
			var odata = this.tr("meta.lists.odata");
			odata = odata.replace(/'/g, '"');
			odata = qx.lang.Json.parse(odata);
			var fields = [];
			for (var f = 0; f < selFields.length; f++) {
				var selField = selFields[f];
				var fieldDesc = this._getFieldDesc(selField.module, selField.id); //selField.fieldDesc; //allFields[selField.column];
				var field = {};
				field.text = selField.column;
				field.itemval = selField.path + "." + selField.id;

				var sopt = this._setDefaultSearchOptions(fieldDesc, selField.module, selField.id);
				sopt = eval(this._checkvalue(sopt));
				var ops = [];
				jQuery.each(sopt, function (index, so) {
					var pos = -1;
					if ((pos = jQuery.inArray(so, allops)) != -1) {
						ops.push({
							op: so,
							text: odata[pos]
						});
					}
				});
				var hasSearchables = false;
				if (fieldDesc.searchable_items) {
					field.dataValues = fieldDesc.searchable_items.getItems();
					hasSearchables = true;
				}
				if (!field.dataValues) {
					if (fieldDesc.selectable_items) {
						field.dataValues = fieldDesc.selectable_items.getItems();
					}
				}
				field.ops = ops;
				if (fieldDesc.datatype && fieldDesc.datatype == 'date') {
					field.type = fieldDesc.edittype == "text" ? "date" : fieldDesc.edittype;
				} else if (fieldDesc.datatype && fieldDesc.datatype == 'decimal') {
					field.type = "decimal";
				} else if (fieldDesc.datatype && fieldDesc.datatype == 'number') {
					field.type = "number";
				} else {
					field.type = hasSearchables ? "select" : fieldDesc.edittype;
				}
				fields.push(field);
			}
			return fields;
		},
		_setDefaultSearchOptions: function (col, module, id) {
			var cm = new ms123.config.ConfigManager();
			var sdesc = this.__storeDesc;
			var modFields = cm.getEntityViewFields(module, sdesc, "search");
			if (modFields) {
				for (var f = 0; f < modFields.length; f++) {
					var field = modFields[f];
					if (field.id == id && field.search_options) return field.search_options;
				}
			}
			var search_options;
			if (col.datatype == 'date' || col.datatype == 'integer' || col.id == 'id' || col.datatype == 'number' || col.datatype == 'decimal' || col.datatype == 'double') {
				search_options = ["gt", "lt", "eq"];
			} else if (col.edittype == "select" || col.edittype == "checkbox") {
				search_options = ["eq", "ne"];
			} else {
				search_options = ["cn", "bw", "eq", "ne"];
			}
			return search_options;
		},
		_checkvalue: function (val) {
			if (typeof val == 'string' && !val.match("^\\[")) {
				var values = val.split(",");
				var ret = [];
				for (var i = 0; i < values.length; i++) {
					ret.push(values[i]);
				}
				return ret;
			}
			return val;
		},
		_createSearchFilter: function (parent, selFields, filterprops, mainModule) {
			var self = this;
			var fields = this._createSearchFilterFields(selFields);
			var params = {
				mainMenu: this._mainMenu,
				onSearch: function (data) {
					self._resultPage.setEnabled(true);
					self._mainTabs.setSelection([self._resultPage]);
					self._showResult(data, mainModule,self._getTableColumns());
				},
				onSave: function (data) {
					self._save();
				},
				windowTitle: self.tr("meta.lists.windowtitle")
			}

			var root = {};
			root.id = "root";
			root.title = "root";
			root.children = [];
			for (var i = 0; i < fields.length; i++) {
				var f = fields[i];
				f.module = "";
				var node = {};
				node.id = f.itemval;
				node.title = f.text;
				node.module = "";
				node.moduleTitle = "";
				node.children = [];
				root.children.push(node);
			}

			var sf = new ms123.searchfilter.SearchFilter(root, fields, params);
			this._currentSearchFilter = sf;
			if (filterprops) {
				var filter = filterprops.filter;

				if (filter != null && filter != "") {
					if (typeof filter == 'string') {
						filter = qx.lang.Json.parse(filter);
					}
					sf.setFilter(filter);
				}
			}
			parent.removeAll();
			parent.add(sf);
		},
		_createNewFilterProps:function(){
			var filterProps = {
				id: this._filterProps.id,
				modulename: this._filterProps.modulename,
				storeId: this._filterProps.storeId,
				user: this._filterProps.user,
				type: (!this._filterProps.type ? "report" : this._filterProps.type),
				description: this._filterProps.description,
				exclusion: this._exclusionListTable.getData(),
				filter: this._currentSearchFilter.getFilterObject(),
				fields: this._getFields()
			};
			return filterProps;
		},
		_save: function () {
			if (!this._currentSearchFilter) {
				var selFields = this._fieldSelector.getSelectedFields();
				this._createSearchFilter(this._filterPage, selFields, this._filterProps, this._mainModule);
			}

			this.fireDataEvent("save", this._createNewFilterProps(), null);
			return true;
		},
		_getFields: function () {
			var fields = this._fieldSelector.getSelectedFields();
			for (var i = 0; i < fields.length; i++) {
				var map = fields[i];
				delete map.column;
				delete map.displayname;
				var fieldDesc = this._getFieldDesc(map.module, map.id);
				var fdes = qx.util.Serializer.toJson(fieldDesc);
				var dt = fieldDesc["datatype"];
				if (dt != null && dt.match("^array")) {
					map.display = false;
				}
			}
			return fields;
		},
		_makeTabs: function () {
			this._fieldsPage = new qx.ui.tabview.Page(this.tr("meta.lists.fieldtab")).set({
				showCloseButton: false
			});
			this._fieldsPage.setUserData("id", "fields");
			this._fieldsPage.setDecorator(null);
			this._fieldsPage.setLayout(new qx.ui.layout.Grow());
			this._mainTabs.add(this._fieldsPage, {
				edge: 0
			});


			this._filterPage = new qx.ui.tabview.Page(this.tr("meta.lists.filtertab")).set({
				showCloseButton: false
			});
			this._filterPage.setDecorator(null);
			this._filterPage.setLayout(new qx.ui.layout.Grow());
			this._filterPage.setUserData("id", "filter");
			this._mainTabs.add(this._filterPage, {
				edge: 0
			});

			this._resultPage = new qx.ui.tabview.Page(this.tr("meta.lists.resulttab")).set({
				showCloseButton: false
			});
			this._resultPage.setDecorator(null);
			this._resultPage.setLayout(new qx.ui.layout.Dock());
			this._resultPage.setUserData("id", "result");
			this._resultPage.add(this._createResultToolbar(), {
				edge: "south"
			});
			this._mainTabs.add(this._resultPage, {
				edge: 0
			});

		},
		_filterSplitPane: function (parent) {
			var splitpane = new qx.ui.splitpane.Pane("vertical");
			splitpane.setDecorator("main");

			var upperWidget = new qx.ui.container.Composite();
			upperWidget.setLayout(new qx.ui.layout.Grow());
			upperWidget.setDecorator(null);
			splitpane.add(upperWidget, 2);

			var bottomWidget = new qx.ui.container.Composite();
			bottomWidget.setLayout(new qx.ui.layout.Dock());
			bottomWidget.setDecorator(null);
			splitpane.add(bottomWidget, 8);

			return splitpane;
		},
		_createMainMenu: function () {
			var menu = new qx.ui.menu.Menu;
			var exclusionListButton = new qx.ui.menu.Button(this.tr("composite.exclusion_list"), "icon/16/actions/list-remove.png", new qx.ui.core.Command("Ctrl+E"));
			exclusionListButton.addListener("execute", this._exclusionListListener, this);
			menu.add(exclusionListButton);
			return menu;
		},
		_exclusionListListener: function (e) {
			this._exclusionListWindow.setActive(true);
			this._exclusionListWindow.open();
		},
		_createResultToolbar: function () {
			var toolbar = new qx.ui.toolbar.ToolBar();
			toolbar.setSpacing(5);

			var buttonExport = new qx.ui.toolbar.Button("Export", "icon/16/actions/view-restore.png");
			buttonExport.addListener("execute", function () {
				var context = {};
				context.filter = this._createFilterWithExclusion();
				context.fields = this._currentFields;
				context.aliases = this._currentAliases;
				context.mainModule = this._currentMainModule;
				context.storeDesc = this.__storeDesc;
				context.noXML = true;
				new ms123.exporter.ExportDialog(context);
			}, this);
			toolbar._add(buttonExport)

			toolbar.addSpacer();
			toolbar.addSpacer();
			toolbar.addSpacer();
			var buttonRefresh = new qx.ui.toolbar.Button("Refresh", "icon/16/actions/edit-redo.png");
			buttonRefresh.addListener("execute", function () {
				var data = ms123.util.Remote.rpcSync("data:executeFilter", {
					storeId: this.__storeDesc.getStoreId(),
					desc: this._createNewFilterProps()
				});

				this._currentData = data.rows;
				this._resultTable.setData(data.rows);
			}, this);
			toolbar._add(buttonRefresh)

			return toolbar;
		},
		_createFilterWithExclusion:function(){
			var newFilter=null;
			var exData = this._exclusionListTable.getData();
			if (exData.length > 0) {
				var idFilter = this._createIdFilter();
				newFilter = {
					label: "1",
					connector: "except",
					children: []
				};
				newFilter.children.push(this._currentSearchFilter.getModel());
				newFilter.children.push(idFilter);
				newFilter = qx.util.Serializer.toJson(newFilter);
			}else{
				newFilter= this._currentSearchFilter.getFilter();
			}
			return newFilter;
		},
		_getTableColumns: function () {
			var selFields = this._fieldSelector.getSelectedFields();
			var columns = new Array();
			var displayColumns = new Array();
			var aliasColumns = new Array();
			for (var f = 0; f < selFields.length; f++) {
				var selField = selFields[f];

				if (selField.display === true) {
					var fieldDesc = this._getFieldDesc(selField.module, selField.id);
					var dt = fieldDesc["datatype"];
					if (dt && dt.match("^array")) {
						continue;
					}
					var fd = qx.lang.Object.mergeWith({}, fieldDesc);
					fd.fqn = selField.path + "." + selField.id;
					fd.label = selField.column;
					if (selField.column.indexOf(".") == -1) {
						displayColumns.push(selField.id);
						aliasColumns.push(selField.mapping);
						fd["id"] = selField.mapping || selField.id;
						fd["name"] = selField.mapping || selField.id;
						columns.push(fd);
					}
				}
			}
			return {
				columns: columns,
				displayColumns: displayColumns,
				aliasColumns: aliasColumns
			};
		},
		_showResult: function (filter, mainModule,tableColumns) {
			var c = tableColumns;

			this._currentFields = qx.util.Serializer.toJson(c.displayColumns);
			this._currentAliases = qx.util.Serializer.toJson(c.aliasColumns);
			this._currentMainModule = mainModule;


			var cm = new ms123.config.ConfigManager();
			var cols = cm.buildColModel(c.columns, "lists", this.__storeDesc, "data");
			var model = this._buildModel(cols, {});

			var gridConfig = {};
			gridConfig.isMaster = true;
			gridConfig.config = "lists";
			gridConfig.disable = ["export", "import", "add", "copy", "del", "edit"];
			gridConfig.model = model;
			gridConfig.user = this._user;
			gridConfig.noEdit = true;
			gridConfig.modul = this;
			gridConfig.contextmenu = this._contextMenuHandler;
			gridConfig.storeDesc= this.__storeDesc;
			var table = new ms123.widgets.Table(gridConfig);

			table.getTable().addListener("cellTap", function (e) {
				if (e.getButton() != "left") { 
					return;
				}
				var selModel = table.getTable().getSelectionModel();
				var rownum = table.getTable().getFocusedRow();
				var value = qx.lang.Json.stringify(this._currentData[rownum], null, 4);
				this._showJsonWindow(value);
			}, this);
			if (this._resultTable) {
				this._resultPage.remove(this._resultTable);
			}
			this._resultTable = table;
			this._resultPage.add(table, {
				edge: "center"
			});

			var data = ms123.util.Remote.rpcSync("data:executeFilter", {
				storeId: this.__storeDesc.getStoreId(),
				desc: this._createNewFilterProps()
			});

			//var baseurl = "xdata/execute-filter/" + name;
			//var data = ms123.util.Remote.sendSync(baseurl);
			this._currentData = data.rows;
			this._resultTable.setData(data.rows);
		},
		_buildModel: function (columns, props) {
			var model = {
				attr: function (what) {
					if (what == "gridProps") {
						return props;
					}
					if (what == "colModel") {
						return columns;
					}
				}
			}
			return model;
		},
		_contextMenuHandler: function (col, row, table, dataModel, contextMenu) {
			var menuEntry = new qx.ui.menu.Button(this.tr("composite.add_to_exclusion_list"));
			menuEntry.addListener("execute", function (e) {
				var map = table.getTableModel().getRowDataAsMap(row);
				var context = table.getUserData("context");
				context.modul._exclusionListTable.addRecord(map);
			});
			contextMenu.add(menuEntry);
			return true;
		},
		_showJsonWindow: function (value) {
			this.msgArea.setValue(value);
			this._jsonWindow.setCaption("X");
			this._jsonWindow.open();
		},
		_createJsonWindow: function (name) {
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

			this.msgArea = new qx.ui.form.TextArea();
			win.add(this.msgArea, {
				edge: "center"
			});
			this._jsonWindow = win;
			return win;
		},
		_createExclusionWindow: function (mwidget, parent, filterProps) {
			var win = new qx.ui.window.Window(this.tr("composite.exclusion_list"), "").set({
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
			parent.getApplicationRoot().add(win);

			var cm = new ms123.config.ConfigManager();
			var c = this._getTableColumns();
			var cols = cm.buildColModel(c.columns, "lists", this.__storeDesc, "data");
			var model = this._buildModel(cols, {});

			var buttons = [{
				'label': "",
				'icon': "icon/16/places/user-trash.png",
				'callback': function (m) {
					this.deleteCurrentRecord();
				},
				'value': "del"
			}];

			var gridConfig = {};
			gridConfig.buttons = buttons;
			gridConfig.isMaster = true;
			gridConfig.config = "exclusion";
			gridConfig.model = model;
			gridConfig.user = this._user;
			gridConfig.noEdit = true;
			gridConfig.storeDesc = this.__storeDesc;
			var table = new ms123.widgets.Table(gridConfig);

			var exdata = filterProps.exclusion;
			if (exdata != null && exdata != "") {
				try {
					if( typeof exdata=='object'){
						table.setData(exdata);
					}else{
						table.setData(jQuery.parseJSON(exdata));
					}
				} catch (e) {
					alert("FilterEditor._createExclusionWindow:" + e)
				}
			}

			win.add(table);
			this._exclusionListWindow = win;
			this._exclusionListTable = table;

		},
		_createIdFilter: function () {
			var idArray = this._exclusionListTable.getIdArray();
			var idFilter = {
				label: "1",
				connector: "or",
				children: []
			};
			for (var i = 0; i < idArray.length; i++) {
				var node = new ms123.searchfilter.Node();
				node.setField("id");
				node.setOp("eq");
				node.setData(idArray[i]);
				idFilter.children.push(node);
			}
			var f = qx.lang.Json.parse(qx.util.Serializer.toJson(idFilter));
			return f;
		},
		_getModelPath: function (model) {
			var path = [];
			path.push(model.getId());
			while (model.parent) {
				model = model.parent;
				path.push(model.getId());
			}
			return path.reverse();
		},
		_contains: function (a, obj) {
			var i = a.length;
			while (i--) {
				if (a[i] === obj) {
					return true;
				}
			}
			return false;
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
		_getSelectableFields: function (entity) {
			var colModel = this._fieldmap[entity];
			if (colModel === undefined) {
				try {
					var cm = new ms123.config.ConfigManager();
					var data = cm.getEntityViewFields(entity,this.__storeDesc,"report",false);
					colModel = cm.buildColModel(data, entity, this.__storeDesc, "data", "search");
					this._fieldmap[entity] = colModel;
				} catch (e) {
					ms123.form.Dialog.alert("FilterEditor._getSelectableFields:" + e);
					return;
				}
			}
			return colModel;
		},
		_filterFields: function (fields) {
			var newFields = [];
			if (fields != null) {
				for (var i = 0; i < fields.length; i++) {
					var field = fields[i];
					if (this._getFieldDesc(field.module, field.id) == null) continue;
					var prefix = this._removeFirstSegment(field.path.replace("$", "."));
					field.column = prefix + (prefix.length > 0 ? "." : "") + field.id;
					newFields.push(field);
				}
			}
			return newFields;
		},
		_removeFirstSegment: function (s) {
			var i = s.indexOf(".");
			if (i == -1) {
				return "";
			}
			return s.substring(i + 1);
		}
	}
});
