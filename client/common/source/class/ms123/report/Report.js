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

qx.Class.define("ms123.report.Report", {
	extend: qx.core.Object,
	include: qx.locale.MTranslation,


	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (context) {
		this.base(arguments);
		this._init(context);
	},


	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},


	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		_init: function (data) {
			//this._user = ms123.util.Remote.rpcSync("auth:getUserProperties");
			this._user = ms123.config.ConfigManager.getUserProperties();
			this.__configManager = new ms123.config.ConfigManager();
			this.__storeDesc = data.storeDesc;
			this.__dataAccess = new ms123.report.DefaultDataAccess({
				storeDesc: this.__storeDesc
			});
			this.__reportMap = {};

			this._fieldmap = {};
			var entitiesMenu = [];
			var _entities = null;
			try {
				var cm = new ms123.config.ConfigManager();
				_entities = cm.getEntities(data.storeDesc);
			} catch (e) {
				ms123.form.Dialog.alert("Report._init:" + e);
				return;
			}

			for (var i = 0; i < _entities.length; i++) {
				var entity = _entities[i];
				var entityName = entity.name;
				var o = {};
				o.label = this.tr("data." + entityName);
				o.value = entityName;
				entitiesMenu.push(o);
			}
			this._need_filterrefresh = false;

			var rightTabView = new qx.ui.tabview.TabView().set({
				decorator: "main",
				contentPadding: 0,
				backgroundColor: "white"
			});

			var sp = this._doLayout(rightTabView);
			sp.setBackgroundColor("white");
			data.window.add(sp, {});
			var childs = sp.getChildren();

			this._leftTree = childs[0];
			this._mainTabs = childs[1];

			this._makeTabs();
			this._hideTabs();

			this._mainMenu = this._createMainMenu();


			var cm = new ms123.config.ConfigManager();
			var tcontext = {};

			var reportList = this.__dataAccess.getReports({
				storeDesc: this.__storeDesc
			});
			var ex = qx.util.Serializer.toJson(reportList);

			var value = qx.lang.Json.stringify(reportList, null, 2);
			tcontext.treeModel = this._createTreemodel(reportList);

			tcontext.user = this._user;
			tcontext.createMenu = function (item, level, nodeMap, treeModul) {
				var menu = new qx.ui.menu.Menu();
				if (level == "0" || level == "1") {
					var cmd = new qx.ui.core.Command();

					var text = (level == "0") ? self.tr("meta.lists.new_list") : self.tr("meta.lists.new_filter");
					var ctext = (level == "0") ? self.tr("meta.lists.list_name") : self.tr("meta.lists.filter_name");
					var addButton = new qx.ui.menu.Button(text, null, cmd);
					addButton.addListener("execute", function () {
						var formData = {
							"listname": {
								'type': "TextField",
								'label': ctext,
								'validation': {
									required: true,
									validator: "/^[A-Za-z]([0-9A-Za-z_]){2,20}$/"
								},
								'value': ""
							},
							"mainEntity": {
								'type': "SelectBox",
								'label': self.tr("meta.lists.main_module"),
								'value': 1,
								'options': entitiesMenu
							}
						};

						if (level == 1) {
							delete formData.mainEntity;
						}
						var form = new ms123.form.Form({
							"formData": formData,
							"allowCancel": true,
							"inWindow": true,
							"callback": function (m) {
								if (m !== undefined) {
									var newName = m.get("listname");
									var meta = null;
									if (level == 0) {
										var mm = m.get("mainEntity");
										meta = {
											mainEntity: mm,
											name: newName
										}
									} else {
										meta = {
											name: newName
										}
									}
									var ex = qx.util.Serializer.toJson(nodeMap);
									var ppath = this._getModelPath(nodeMap).join("/");

									var completed = function () {
										if (level == 0) {
											ms123.form.Dialog.alert(self.tr("meta.lists.listfields_created"));
										} else {
											ms123.form.Dialog.alert(self.tr("meta.lists.listfilter_created"));
										}
									};
									var reportData;
									if (level == 0) {
										reportData = self._initBody(mm);
									} else {
										reportData = self.__reportMap[nodeMap.getId()];
										var newFilter = {
											name: newName
										}
										reportData.filterList.push(newFilter);
									}
									try {
										if (level == 0) {
											var params = {
												name: newName,
												report: reportData
											};
											self.__dataAccess.createReport(params);
											meta.id = newName;
											meta.name = newName;
											self.__reportMap[newName] = reportData;
										} else {
											var params = {
												id: nodeMap.getId(),
												report: reportData
											};
											self.__dataAccess.updateReport(params);
										}
										completed.call(this);
										var path = this._createTreeNode(nodeMap, newName, meta);
									} catch (e) {
										self._showDetails(e);
									}
								}
							},
							"context": this
						});
						form.show();


					}, treeModul);
					menu.add(addButton);
				}

				var deltext;
				if (level == "2") {
					item.setIcon("qx/decoration/Classic/shadow/shadow-small-tl.png");
					item.setIconOpened("qx/decoration/Classic/shadow/shadow-small-tl.png");
					deltext = self.tr("meta.lists.del_filter");
				}
				if (level == "1") {
					item.setIcon("qx/decoration/Classic/shadow/shadow-small-r.png");
					item.setIconOpened("qx/decoration/Classic/shadow/shadow-small-r.png");
					item.setMaxHeight(20);
					deltext = self.tr("meta.lists.del_list");
				}

				if (level == "0") {
					item.setIcon("icon/16/actions/format-justify-fill.png");
					item.setIconOpened("icon/16/mimetypes/text-plain.png");
					item.setOpen(true);
				}
				var cmd = new qx.ui.core.Command();
				var delButton = new qx.ui.menu.Button(deltext, null, cmd);
				delButton.addListener("execute", function () {
					ms123.form.Dialog.confirm(self.tr("composite.select_dialog.confirm_delete"), function (e) {
						if (e) {
							var children = nodeMap.parent.getChildren();
							for (var i = 0; i < children.getLength(); i++) {
								var xid = children.getItem(i);
								if (xid == nodeMap) {
									children.removeAt(i);
									break;
								}
							}
							var path = this._getModelPath(nodeMap);
							var completed = function () {
								if (level == 1) {
									ms123.form.Dialog.alert(self.tr("meta.lists.list_deleted"));
								} else {
									ms123.form.Dialog.alert(self.tr("meta.lists.filter_deleted"));
								}
								self._hideTabs();
							};
							var ex = qx.util.Serializer.toJson(nodeMap);
							try {
								if (level == 1) {
									var params = {
										id: nodeMap.getId()
									};
									self.__dataAccess.deleteReport(params);
								} else {
									var reportData = self.__reportMap[nodeMap.parent.getId()];
									self._removeFilterFromList(reportData.filterList, nodeMap.getName());
									var params = {
										id: nodeMap.getId(),
										report: reportData
									};
									self.__dataAccess.updateReport(params);
								}
								completed.call(this);
							} catch (e) {
								console.log(e.stack);
								self._showDetails(e);
							}



						}
					}, this);
				}, treeModul);

				if (level == "1" || level == "2") {
					menu.add(delButton);
				}
				return menu;
			}

			var self = this;
			var fscontext = {};
			fscontext.user = this._user;
			fscontext.tableColumns = [{
				name: "column",
				header: self.tr("meta.lists.fs.column")
			},
			{
				name: "display",
				type: "CheckBox",
				width: 60,
				header: self.tr("meta.lists.fs.display")
			},
			{
				name: "mapping",
				type: "TextField",
				header: self.tr("meta.lists.fs.mapping")
			}];
			fscontext.saveFields = function (_fields) {
				self._need_filterrefresh = true;
				for (var i = 0; i < _fields.length; i++) {
					var map = _fields[i];
					var fieldDesc = self._getFieldDesc(map.module, map.id);
					var dt = fieldDesc["datatype"];
					if (dt != null && dt.match("^array")) {
						map.display = false;
					}
				}
				var id = self._currentList;
				var reportData = self.__reportMap[id];
				reportData.fieldList = _fields;

				try {
					var params = {
						id: id,
						report: reportData
					};
					self.__dataAccess.updateReport(params);
					ms123.form.Dialog.alert(self.tr("meta.lists.listfields_saved"));
				} catch (e) {
					self._showDetails(e);
				}
			}
			fscontext.addToFieldTable = function (map, treeModel, path, checkbox) {
				map.display = true;
				map.path = path;
				map.column = treeModel.getTitle() + "." + checkbox.getLabel();
				var fieldDesc = self._getFieldDesc(map.module, map.id);
				var dt = fieldDesc["datatype"];
				if (dt != null && dt.match("^array")) {
					map.display = false;
				}
				return map;
			}
			this._fieldSelector = new ms123.report.FieldSelector(fscontext);
			this._fieldSelector.addListener("treeClicked", function (e) {
				var model = e.getData().selectionModel;
				var path = e.getData().treePath;
				var fields = self._getSelectableFields(model.getEntity());
				this._fieldSelector.createFieldsWindow(path, model, fields);
			}, this);

			this._mainTabs.addListener("changeSelection", function (e) {
				var pid = e._target.getSelection()[0].getUserData("id");
				if (pid == "filter" && this._need_filterrefresh) {
					this._need_filterrefresh = false;
					var selFields = self._fieldSelector.getSelectedFields();
//			var value = qx.lang.Json.stringify(selFields, null, 2);console.log(value);
					this._createSearchFilter(this._filterPage, selFields, this._filterProps, self._mainModule);
				}
			}, this);

			tcontext.clickListener = function (e) {
				if (e.isLeftPressed()) {
					self._currentTreeModel = this.getSelection()[0].getModel();
					var level = this.getSelection()[0].getModel().getLevel();
					if (level == "0") {
						self._hideTabs();
						return;
					}
					self._showTabs();
					if (level == "1") {
						var model = this.getSelection()[0].getModel();
						var reportData = self.__reportMap[model.getId()];
						var mainmodule = reportData.mainEntity;

						var cm = new ms123.config.ConfigManager();
						var treeModel = cm.getEntityTree(self.__storeDesc,mainmodule,5);
						var fieldList = reportData.fieldList;

						self._fieldSelector.setTreeModel(self._translateModel(treeModel));
						self._fieldSelector.createTable(fscontext.tableColumns);
						var f = self._filterFields(fieldList);
						self._fieldSelector.setSelectedFields(f);
						self._filterPage.setEnabled(false);
						self._resultPage.setEnabled(false);
						self._mainTabs.setSelection([self._fieldsPage]);
						self._currentList = model.getId();
						self._mainModule = mainmodule;
					}
					if (level == "2") {
						var model = this.getSelection()[0].getModel();
						var reportData = self.__reportMap[model.parent.getId()];
						var mainmodule = reportData.mainEntity;

						var cm = new ms123.config.ConfigManager();
						var treeModel = cm.getEntityTree(self.__storeDesc,mainmodule,5);
						var fieldList = reportData.fieldList;


						self._fieldSelector.setTreeModel(self._translateModel(treeModel));
						self._fieldSelector.createTable(fscontext.tableColumns);
						var f = self._filterFields(fieldList);
						self._fieldSelector.setSelectedFields(f);
						self._filterPage.setEnabled(true);
						self._resultPage.setEnabled(false);
						self._mainTabs.setSelection([self._filterPage]);
						self._currentList = model.parent.getId();
						self._mainModule = mainmodule;

						var newFilter = qx.util.Serializer.toJson(reportData.filterList);
						var filter = self._getFilterByName(reportData.filterList, model.getName());

						self._filterProps = filter;

						var selFields = self._fieldSelector.getSelectedFields();
						self._createSearchFilter(self._filterPage, selFields, filter, self._mainModule);
						self._createExclusionWindow(null, self._filterPage, filter,selFields);
					}
				}
			}

			var tree = new ms123.widgets.Tree(tcontext);
			this._leftTree.setLayout(new qx.ui.layout.Grow());
			this._leftTree.add(tree, {});
			this._tree = tree;

			this._fieldsPage.add(this._fieldSelector);
		},
		_createTreemodel: function (reportList) {
			var root = {
				level: "0",
				id: "reports",
				name: "reports",
				title: "Reports"
			};
			var reportChilds = [];
			for (var i = 0; i < reportList.length; i++) {
				var report = reportList[i];

				var reportData = null;
				var filterChilds = [];
				if (report) {
					reportData = report;
					if (!reportData.filterList) reportData.filterList = [];
					for (var f = 0; f < reportData.filterList.length; f++) {
						var filter = reportData.filterList[f];
						var fchild = {};
						fchild.level = "2";
						fchild.name = filter.name;
						fchild.title = filter.name;
						fchild.id = report.name;
						filterChilds.push(fchild);
					}
				}

				var rchild = {};
				rchild.level = "1";
				rchild.id = report.name;
				rchild.title = report.name;
				rchild.name = report.name;
				rchild.children = filterChilds;
				reportChilds.push(rchild);
				this.__reportMap[report.name] = reportData;
			}
			root.children = reportChilds;

			var value = qx.lang.Json.stringify(root, null, 2);
			return root;
		},
		_initBody: function (mainEntity) {
			var reportData = {
				mainEntity: mainEntity,
				fieldList: [],
				filterList: []
			}
			return reportData;
		},
		_removeFilterFromList: function (filterList, name) {
			var index = -1;
			for (var i = 0; i < filterList.length; i++) {
				var f = filterList[i];
				if (f.name == name) {
					index = i;
					break;
				}
			}
			if (index != -1) {
				filterList.splice(index, 1);
			}
		},
		_getFilterByName: function (filterList, name) {
			for (var i = 0; i < filterList.length; i++) {
				var f = filterList[i];
				if (f.name == name) {
					return f;
				}
			}
		},
		_replaceFilterByName: function (filterList, newFilter, name) {
			var found = false;
			for (var i = 0; i < filterList.length; i++) {
				var f = filterList[i];
				if (f.name == name) {
					filterList[i] = newFilter;
					found = true;
				}
			}
			if (!found) filterList.push(newFilter);
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
			var modFields = cm.getEntityViewFields(module, this.__storeDesc, "search");
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
		_createSearchFilter: function (parent, selFields, filter, mainModule) {
			var self = this;
			var fields = this._createSearchFilterFields(selFields);
			var params = {
				mainMenu: this._mainMenu,
				onSearch: function (data) {
					var filter = self._createFilterWithExclusion();
					self._currentMainModule = mainModule;
					self._showResult(filter, mainModule,selFields);
				},
				onSave: function (data) {
					self._saveSearchFilter(filter, data);
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
			if (filter && filter.filter != null) {
				sf.setFilter(filter.filter);
			}
			parent.removeAll();
			parent.add(sf);
		},
		_createFilterWithExclusion:function(){
			var newFilter=null;
			var exData = this._exclusionListTable.getData();
			if (exData.length > 0) {
				var exFilter = this._createExclusionFilter();
				newFilter = {
					label: "1",
					connector: "except",
					children: []
				};
				newFilter.children.push(this._currentSearchFilter.getModel());
				newFilter.children.push(exFilter);
				newFilter = qx.util.Serializer.toJson(newFilter);
			}else{
				newFilter= this._currentSearchFilter.getFilter();
			}
			return newFilter;
		},
		_saveSearchFilter: function (filter, data) {
			var exdata = this._exclusionListTable.getData();
			data = qx.lang.Json.parse(data);

			var id = this._currentList;
			var reportData = this.__reportMap[id];
			var newFilter = {
				name: filter.name,
				filter: data,
				exclusion: exdata
			}
			this._replaceFilterByName(reportData.filterList, newFilter, filter.name);
			try {
				var params = {
					id: id,
					report: reportData
				};
				this.__dataAccess.updateReport(params);
				ms123.form.Dialog.alert(this.tr("meta.lists.listfilter_saved"));
			} catch (e) {
				ms123.form.Dialog.alert("Report._saveSearchFilter:" + e);
			}
		},
		_doLayout: function (rightTabView) {
			var splitpane = new qx.ui.splitpane.Pane("horizontal");
			splitpane.setDecorator(null);


			var leftWidget = new qx.ui.container.Composite();
			leftWidget.setDecorator(null);
			leftWidget.setMinWidth(50);
			splitpane.add(leftWidget, 1);

			var rightWidget = rightTabView;
			rightWidget.setDecorator(null);
			rightWidget.setMinWidth(250);
			splitpane.add(rightWidget, 3);

			return splitpane;
		},
		_hideTabs: function () {
			this._mainTabs.setEnabled(false);
			this._mainTabs.setVisibility("hidden");
		},
		_showTabs: function () {
			this._mainTabs.setEnabled(true);
			this._mainTabs.setVisibility("visible");
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
			var copyButton = new qx.ui.menu.Button(this.tr("filter.text.copy_to"), "icon/16/actions/edit-copy.png", new qx.ui.core.Command("Ctrl+C"));
			var addButton = new qx.ui.menu.Button(this.tr("filter.text.add_to"), "icon/16/actions/list-add.png", new qx.ui.core.Command("Ctrl+A"));
			var subButton = new qx.ui.menu.Button(this.tr("filter.text.sub_from"), "icon/16/actions/list-remove.png", new qx.ui.core.Command("Ctrl+S"));
			exclusionListButton.addListener("execute", this._exclusionListListener, this);

			addButton.addListener("execute", this._mainMenuListener, this);
			subButton.addListener("execute", this._mainMenuListener, this);
			copyButton.addListener("execute", this._mainMenuListener, this);

			menu.add(exclusionListButton);
			menu.add(copyButton);
			menu.add(addButton);
			menu.add(subButton);
			return menu;
		},
		_exclusionListListener: function (e) {
			this._exclusionListWindow.setActive(true);
			this._exclusionListWindow.open();
		},
		_mainMenuListener: function (e) {
			var button = e.getTarget();
			var cmd = button.getCommand();
			cmd = cmd.toString().match("\\+.$");
			if (cmd == "+A" || cmd == "+S" || cmd == "+C") {
				var selection = [];
				var filterNames = [];
				var curname = this._currentTreeModel.getName();
				var curid = this._currentTreeModel.getId();
				var lists = this._currentTreeModel.parent.getChildren();
				for (var i = 0; i < lists.getLength(); i++) {
					var item = {};
					var c = lists.getItem(i);
					item.label = c.getTitle();
					item.value = c.getName();
					filterNames.push(c.getName());
					if (curname != item.value) {
						selection.push(item);
					}
				}
				var i = 1;
				var newFiltername = "newfilter";
				while (true) {
					if (!this._contains(filterNames, newFiltername + i)) {
						newFiltername = newFiltername + i;
						break;
					}
					i++;
				}
				var formData = {
					'filtername': {
						'type': "SelectBox",
						'label': this.tr("meta.lists.which_filter"),
						'value': 1,
						'options': selection
					},
					"newfiltername": {
						'type': "TextField",
						'label': this.tr("meta.lists.new_filtername"),
						'validation': {
							required: true,
							validator: "/^[A-Za-z]([0-9A-Za-z_]){2,20}$/"
						},
						'value': newFiltername
					}
				};
				if (cmd == "+C") {
					delete formData.filtername;
				}

				if ((cmd == "+S" || cmd == "+A") && selection.length < 1) {
					ms123.form.Dialog.alert(this.tr("meta.lists.too_little_filter"));
				} else {
					var msg = "";
					if (cmd == "+A") {
						msg = this.tr("meta.lists.add_to_filter");
					} else if (cmd == "+S") {
						msg = this.tr("meta.lists.sub_from_filter");
					} else if (cmd == "+C") {
						msg = this.tr("meta.lists.copy_from_filter");
					}
					ms123.form.DialogForm.form("\"" + this._currentTreeModel.getTitle() + "\" " + msg, formData, function (result) {
						if (result) {
							newFiltername = result.getNewfiltername();

							if (this._contains(filterNames, newFiltername)) {
								ms123.form.Dialog.alert(this.tr("meta.lists.filter_used") + ":" + newFiltername);
								return;
							}
							if (cmd == "+A" || cmd == "+S") {
								var filtername = result.getFiltername();
								var reportData = this.__reportMap[curid];
								var filterProps = this._getFilterByName(reportData.filterList, filtername);

								var secondFilter = filterProps["filter"];
								var data = {
									label: "1",
									connector: (cmd == "+A") ? "union" : "except",
									children: []
								};
								var model = qx.data.marshal.Json.createModel(data);

								if (secondFilter == undefined) {
									ms123.form.Dialog.alert(this.tr("meta.lists.filter_empty") + ":" + filtername);
									return;
								}
								//secondFilter = qx.lang.Json.parse(secondFilter);
								var model1 = qx.data.marshal.Json.createModel(secondFilter);

								data = qx.util.Serializer.toNativeObject(this._currentSearchFilter.getModel(), null);
								var model2 = qx.data.marshal.Json.createModel(data);

								model.getChildren().push(model1);
								model.getChildren().push(model2);
								model.setLabel(" ");

								this._currentSearchFilter.renameModelLabel(model1, "1");
								this._currentSearchFilter.renameModelLabel(model2, "2");
								var newFilterProps = {};
								newFilterProps.name = newFiltername;
								var newFilter = qx.util.Serializer.toJson(model);
								this._saveSearchFilter(newFilterProps, newFilter);
								var meta = {
									name: newFiltername
								}
								this._tree._createTreeNode(this._currentTreeModel.parent, newFiltername, meta);
							} else if (cmd == "+C") {
								var data = qx.util.Serializer.toNativeObject(this._currentSearchFilter.getModel(), null);
								var model = qx.data.marshal.Json.createModel(data);

								var newFilterProps = {};
								newFilterProps.path = "lists/" + this._currentTreeModel.parent.getId() + "/" + newFiltername;
								newFilterProps.name = newFiltername;
								var newFilter = qx.util.Serializer.toJson(model);
								this._saveSearchFilter(newFilterProps, newFilter);
								var meta = {
									name: newFiltername
								}
								var path = this._tree._createTreeNode(this._currentTreeModel.parent, newFiltername, meta);
							}
						}
					}, this);
				}
			}
		},
		_createResultToolbar: function () {
			var toolbar = new qx.ui.toolbar.ToolBar();
			toolbar.setSpacing(5);

			var buttonExport = new qx.ui.toolbar.Button("Export", "icon/16/actions/view-restore.png");
			buttonExport.addListener("execute", function () {
				var selFields = this._fieldSelector.getSelectedFields();
				var c = this._getTableColumns(selFields);
				var context = {};
				context.filter = this._createFilterWithExclusion();
				context.fields = qx.util.Serializer.toJson(c.displayColumns);
				context.aliases = qx.util.Serializer.toJson(c.aliasColumns);
				context.mainModule = this._currentMainModule;
				context.noXML = true;
				context.storeDesc = this.__storeDesc;
				new ms123.exporter.ExportDialog(context);
			}, this);
			toolbar._add(buttonExport)

			toolbar.addSpacer();
			toolbar.addSpacer();
			toolbar.addSpacer();
			var buttonRefresh = new qx.ui.toolbar.Button("Refresh", "icon/16/actions/edit-redo.png");
			buttonRefresh.addListener("execute", function () {
				var filter = this._createFilterWithExclusion();
				var cm = new ms123.config.ConfigManager();
				var selFields = this._fieldSelector.getSelectedFields();
				var c = this._getTableColumns(selFields);
				var rpc = {
					namespace: this.__storeDesc.getNamespace(),
					storeId: this.__storeDesc.getStoreId(),
					entity: this._currentMainModule,
					fields: c.displayColumns,
					pageSize: 1000,
					filter: qx.lang.Json.parse(filter)
				};
				this._resultTable.setRpcParams(rpc);

			}, this);
			toolbar._add(buttonRefresh)

			return toolbar;
		},
		_getTableColumns: function (selFields) {
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
					displayColumns.push(fd.fqn);
					aliasColumns.push(selField.mapping);
					fd["id"] = fd.fqn;
					fd["name"] = fd.fqn;
					columns.push(fd);
				}
			}
			return {
				columns: columns,
				displayColumns: displayColumns,
				aliasColumns: aliasColumns
			};
		},
		_showResult: function (filter, mainModule, selFields) {
			this._resultPage.setEnabled(true);
			this._mainTabs.setSelection([this._resultPage]);
			var c = this._getTableColumns(selFields);

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
			gridConfig.storeDesc = this.__storeDesc;
			gridConfig.contextmenu = this._contextMenuHandler;
			var table = new ms123.widgets.Table(gridConfig);
			if (this._resultTable) {
				this._resultPage.remove(this._resultTable);
			}
			this._resultTable = table;
			this._resultPage.add(table, {
				edge: "center"
			});
			var rpc = {
				namespace: this.__storeDesc.getNamespace(),
				entity: mainModule,
				fields: c.displayColumns,
				filter: qx.lang.Json.parse(filter)
			};
			this._resultTable.setRpcParams(rpc);
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
		_createExclusionWindow: function (mwidget, parent, filterProps,selFields) {
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
			var c = this._getTableColumns(selFields);
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
			gridConfig.storeDesc = this.__storeDesc;
			gridConfig.user = this._user;
			gridConfig.noEdit = true;
			var table = new ms123.widgets.Table(gridConfig);

			var exdata = filterProps["exclusion"];
			if (exdata != null) {
				try {
					table.setData(exdata);
				} catch (e) {
					alert(e)
				}
			}

			win.add(table);
			this._exclusionListWindow = win;
			this._exclusionListTable = table;

		},
		_createDatatypeArray: function (row) {
			var dtypes = [];
			for (var field in row) {
				if (field == 'id') continue;
				if (field == 'masterid') continue;
				if (field == 'nodename') continue;
				try {
					var dot = field.lastIndexOf(".");
					var id = field.substring(dot + 1);
					var prefix = field.substring(0, dot);
					var dollar = prefix.lastIndexOf("$");
					var module = ms123.util.Inflector.getModuleName(prefix.substring(dollar + 1));
					if( module.match(/^_.*/)) module = module.substring(1);
					var fd = this._getFieldDesc(module, id);
					dtypes.push(fd.edittype);
				} catch (e) {
					dtypes.push(null);
				}
			}
			return dtypes;
		},
		_createExclusionFilter: function () {
			var exData = this._exclusionListTable.getData();
			var exFilter = {
				label: "1",
				connector: "or",
				children: []
			};
			var dtypes;
			for (var i = 0; i < exData.length; i++) {
				var row = exData[i];
				if (i == 0) {
					dtypes = this._createDatatypeArray(row);
				}
				var and = {
					label: "2",
					connector: "and",
					children: []
				};
				exFilter.children.push(and);
				var col = 0;
				for (var field in row) {
					if (field == 'id') continue;
					if (field == 'masterid') continue;
					if (field == 'nodename') continue;
					var dt = dtypes[col++];
					if (dt && dt == "functional") continue;
					var ok = false;
					var value = row[field];
					if (value != null) {
						if (typeof value == "string") {
							if (value.length > 0) {
								ok = true;
							}
						} else {
							ok = true;
						}
					}
					if (ok) {
						var node = new ms123.searchfilter.Node();
						node.setField(field);
						node.setOp("ceq");
						node.setData(row[field]);
						and.children.push(node);
					} else {
						var or = {
							label: "1",
							connector: "or",
							children: []
						};
						var node = new ms123.searchfilter.Node();
						node.setField(field);
						node.setOp("ceq");
						node.setData("");
						or.children.push(node);

						node = new ms123.searchfilter.Node();
						node.setField(field);
						node.setOp("in");
						node.setData("");
						or.children.push(node);

						and.children.push(or);
					}
				}
			}
			//			var ex = qx.util.Serializer.toJson(exFilter); alert(ex);
			return exFilter;
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
		_showDetails: function (details) {
			ms123.form.Dialog.alert(details.message);
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
					ms123.form.Dialog.alert("Report._getSelectableFields:" + e);
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
					newFields.push(field);
				}
			}
			return newFields;
		}
	}
});
