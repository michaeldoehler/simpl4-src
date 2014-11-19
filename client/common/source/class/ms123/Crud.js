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
 * @ignore(jQuery) 
 * @ignore(jQuery.extend)
 * @ignore(jQuery.each)
 * @lint ignoreDeprecated(alert) 
 */

qx.Class.define("ms123.Crud", {
	extend: qx.ui.container.Composite,
	implement: [ms123.IState],
	include: [qx.locale.MTranslation, ms123.searchfilter.MSearchFilter],


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
		_init: function (context) {
			this.__configManager = new ms123.config.ConfigManager();
			this._user = this.__configManager.getUser();
			this.units = {};
			this._dunitmap = {};
			this.widgets = context.widgets;
			this.unit_id = context.unit_id;
			var mwidget = context.widgets[0];
			this._configMaster = mwidget.config;
			this._appid = context.appid;

			var mainTab = new qx.ui.tabview.TabView().set({
				contentPadding: 0
			});
			this._mainTab = mainTab;

			mainTab.addListener("changeSelection", function (e) {
				this.widgets = e._target.getSelection()[0].getUserData("widgets");
			}, this);

			mainTab.setDecorator(null);

			this._mainContainer = context.window ? context.window : this;
			if (!context.window) {
				this.setLayout(new qx.ui.layout.Grow());
			}
			this._mainContainer.add(mainTab);
			mwidget.isMaster = true;

			this.numTab = 1;
			mwidget.model = this.__configManager.getEntityModel(mwidget.config, mwidget.storeDesc, "main-grid", "properties");
			mwidget.modelForm = this.__configManager.getEntityModel(mwidget.config, mwidget.storeDesc, "main-form", "properties");
			mwidget.unit_id = this.unit_id;
			if (mwidget.exclusionlist) {
				mwidget.contextmenu = this._contextMenuHandlerExclusion;
			}
			if (mwidget.contextMenuEntries) {
				mwidget.contextmenu = this._contextMenuHandler;
			}
			mwidget.user = this._user;
			mwidget.appid = this._appid;
			mwidget.title = this._getTitle();
			mwidget.table = new ms123.widgets.Table(mwidget);

			if (mwidget.contextMenuEntries) {
				mwidget.table.table.setUserData("contextMenuEntries", mwidget.contextMenuEntries);
				mwidget.table.table.setUserData("moduleName", mwidget.config);
			}
			this._mainMenu = this._createMainMenu(mwidget);
			this._actionMenu = null;
			if (this.__isUpdateAllowed(mwidget)) {
				this._actionMenu = this._createActionMenu(mwidget);
			}
			this._createUnit(null);
			if (!mwidget.multipletabs) {
				var bar = mainTab.getChildControl("bar");
				bar.setVisibility("hidden");
				bar.set({
					height: 1
				});
			}


			var eventBus = qx.event.message.Bus;
			eventBus.subscribe(this.unit_id + ".table.contextmenu.row.selected", function (msg) {
				var data = msg.getData();
				this.widgets[0].exclusionListTable.addRecord(data.row);
			}, this);
			eventBus.subscribe(context.unit_id + ".table.row.selected", this._table_row_selected, this);
		},
		_createSidebar: function () {
			var sidebar = new qx.ui.container.Composite();
			sidebar.setLayout(new qx.ui.layout.Dock());
			sidebar.setWidth(120);
			return sidebar;
		},
		_getTitle:function(){
			return this.tr(this._configMaster + "-" + this.numTab);
		},
		_createUnit: function (filterModel) {
			var mainTab = this._mainTab;
			var name = this._getTitle();
			var page = new qx.ui.tabview.Page(name).set({
				showCloseButton: true
			});
			page.addListener("close", function (e) {
				var widgets = page.getUserData("widgets");
				delete this.units[widgets.numTab];
			}, this);
			page.setDecorator(null);
			page.setLayout(new qx.ui.layout.Grow());

			if (this.numTab != 1) {
				var widgets = [];
				for (var i = 0; i < this.widgets.length; i++) {
					var w = jQuery.extend(true, {}, this.widgets[i]);
					widgets.push(w);
				}
				this.widgets = widgets;
			}

			this.widgets.numTab = this.numTab;
			this.units[this.numTab] = this.widgets;
			var mwidget = this.widgets[0];
			mwidget.title = name;
			var detailsTabView = new qx.ui.tabview.TabView().set({
				contentPadding: 0
			});
			detailsTabView.setDecorator("main");
			page.setUserData("widgets", this.widgets);
			mainTab.add(page, {
				edge: 0
			});
			mainTab.setSelection([page]);
			if (this.numTab != 1) {
				mwidget.table = new ms123.widgets.Table(mwidget);
			}

			var details = this._setupDetailsView();
			var sidebar = null;
			if (mwidget.sidebar) {
				sidebar = this._createSidebar();
			}

			var search = this.__createSearchFilter(mwidget, filterModel);
				search.addListenerOnce("appear", function () {
					search.scrollToEnd();
				}, this);
			search.setHeight(50);
			this._doLayout(page, sidebar, search, mwidget.table, details);
			try {
				search.scrollToEnd();
			} catch (e) {
				console.error("Scrll:"+e);
				console.log(e.stack);
			}

			this._createExclusionWindow(mwidget);
			this.numTab++;
		},
		_setupDetailsView: function () {
			var detailsTabView = new qx.ui.tabview.TabView().set({
				contentPadding: 0
			});
			var self = this;
			if (this.widgets.length == 1) {
				detailsTabView.setEnabled(false);
				detailsTabView.setVisibility(false);
				return null;
			}
			if (this.widgets.length > 1) {} else {
				var bar = detailsTabView.getChildControl("bar");
				bar.setVisibility(false);
				bar.set({
					height: 0
				});
			}
			var mwidget = this.widgets[0];
			detailsTabView.setEnabled(false);
			mwidget.detailsTabView = detailsTabView;
			jQuery.each(this.widgets, function (index, widget) {
				if (index == 0) return;
				var page = new qx.ui.tabview.Page(widget.tab_title).set({
					showCloseButton: false
				});
				page.setDecorator(null);
				page.setLayout(new qx.ui.layout.Grow());
				detailsTabView.add(page, {
					edge: 0
				});
				widget.parent = page;

				widget.configMaster = self._configMaster;
				if (widget.type == "table") {
					widget.model = self.__configManager.getEntityModel(widget.config, mwidget.storeDesc, "main-grid", "properties");
					widget.modelForm = self.__configManager.getEntityModel(widget.config, mwidget.storeDesc, "main-form", "properties");
					widget.user = self._user;
					widget.title=self.tr("data."+widget.config);
					widget.appid = self._appid;
					if (widget.disable) {
						widget.disable = widget.disable.concat(["import", "export"]);
					} else {
						widget.disable = ["import", "export"];
					}
					widget.storeDesc = mwidget.storeDesc;

					if (widget.config != "team") widget.contextmenu = self._contextMenuHandlerDetails;
					widget.component = new ms123.widgets.Table(widget);
					if (widget.config != "team") {
						widget.component.table.setUserData("entityName", widget.config);
						widget.component.table.setUserData("storeDesc", widget.storeDesc);
						widget.component.table.setUserData("dunitmap", self._dunitmap);
					}
					widget.parent._add(widget.component);
				} else if (widget.type == "nform") {
					widget.hasDelayedComponent = true;
					widget.component = null;
					widget.model = self.__configManager.getEntityModel(widget.config, mwidget.storeDesc, "main-form", "properties");
				} else {}

				if (widget.multi_add) {
					widget.mainwidget = self.widgets[0];
				}

				if (index != 1) page.hide();
			});
			return detailsTabView;
		},
		_doLayout: function (container, sidebar, search, table, details) {
			var searchMasterDetailsSplitPane = new qx.ui.splitpane.Pane("vertical").set({
				decorator: null
			});

			var searchMasterSplitPane = new qx.ui.splitpane.Pane("vertical").set({
				decorator: null
			});

			var mainSplitPane = details ? searchMasterDetailsSplitPane : searchMasterSplitPane;
			var sidebarMainSplitPane = null;
			if (sidebar != null) {
				sidebarMainSplitPane = new qx.ui.splitpane.Pane("horizontal").set({
					decorator: null
				});
				sidebarMainSplitPane.add(sidebar, 0);
				sidebarMainSplitPane.add(mainSplitPane, 4);
			}

			searchMasterSplitPane.add(search, 0);
			searchMasterSplitPane.add(table, 3);

			if (details) {
				searchMasterDetailsSplitPane.add(searchMasterSplitPane, 6);
				searchMasterDetailsSplitPane.add(details, 4);
			}

			container.add(sidebar ? sidebarMainSplitPane : mainSplitPane, {
			});
		},
		_createIdFilter: function (idArray,entity) {
			var idFilter = {
				label: "1",
				connector: "or",
				children: []
			};
			for (var i = 0; i < idArray.length; i++) {
				var node = new ms123.searchfilter.Node();
				if( entity ){
					node.setField(entity+".id");
				}else{
					node.setField("id");
				}
				node.setOp("eq");
				node.setData(idArray[i]);
				idFilter.children.push(node);
			}
			var f = qx.lang.Json.parse(qx.util.Serializer.toJson(idFilter));
			return f;
		},
		selectId: function (id) {
			var _filter = this._createIdFilter([id],this._configMaster);
			var filter = qx.util.Serializer.toJson(_filter);
			console.log("_selectId:" + JSON.stringify(_filter,null,2));
			this.widgets[0].table.setFilter(filter);
			this.widgets[0].table.selectRow(0);

			var sf = this.widgets[0].searchFilter;
			var sdesc = this.widgets[0].storeDesc;
			var fields = this._getSearchFilterFields(this._configMaster, sdesc);
			if (_filter != null && fields.indexOf("id")) {
				sf.setFilter(_filter);
				sf.renameModelLabel(sf.getModel(),"1");

			}
		},
		__createSearchFilter: function (mwidget, filterModel) {
			var self = this;
			var sdesc = mwidget.storeDesc;
			var f1 = this._getSearchFilterFieldSets(this._configMaster, sdesc);
			var fsids = [];
			f1.each(function (e) {
				fsids.push(e.itemval);
			});
			var f2 = this._getSearchFilterFields(this._configMaster, sdesc);
			var fields = f1.concat(f2);

			var moduleList = this.__configManager.getEntity(mwidget.config, sdesc);
			if (moduleList && moduleList.childs != null) {
				for (var j = 0; j < moduleList.childs.length; j++) {
					var child = moduleList.childs[j];
					child.title = this.tr("data." + moduleList.name + "." + child.name);
					var fs = this._getSearchFilterFieldSets(child.modulename, sdesc);
					var ff = this._getSearchFilterFields(child.modulename, sdesc);
					child.fields = fs.concat(ff);
				}
			}


			var root = {};
			root.id = "root";
			root.title = "root";
			root.children = [];
			//console.log("fsids:"+Object.toJSON(fsids));
			//console.log("fields:"+ qx.lang.Json.stringify(fields,null, 4));
			for (var i = 0; i < fields.length; i++) {
				var f = fields[i];
				f.module = "";
				var node = {};

				if (fsids.indexOf(f.itemval) == -1) {
					node.id = mwidget.config + "." + f.itemval;
					f.itemval = node.id;
				} else {
					node.id = f.itemval;
				}
				node.title = f.text;
				node.module = "";
				node.moduleTitle = "";
				node.children = [];
				root.children.push(node);
			}
			for (var i = 0;
			(moduleList && moduleList.childs != null) && i < moduleList.childs.length; i++) {
				var child = moduleList.childs[i];
				if (child.name == mwidget.config) continue; //@@@MS Besser
				var node = {};
				node.id = child.name;
				node.title = child.title;
				node.children = [];
				node.selectable = false;
				root.children.push(node);
				var fchildren = node.children;
				for (var j = 0; j < child.fields.length; j++) {
					var field = child.fields[j];
					field.module = child.name;
					fields.push(field);
					var fnode = {};
					fnode.id = mwidget.config + "$" + child.name + "." + field.itemval;
					field.itemval = fnode.id;
					fnode.title = field.text;
					fnode.module = child.name;
					fnode.moduleTitle = child.title;
					fnode.children = [];
					fchildren.push(fnode);
				}
			}

			var stateSelect = null;
			if(ms123.config.ConfigManager.isAdmin() && mwidget.state_select && ms123.config.ConfigManager.hasStateSelect()) {
				stateSelect = this._createStateSelect(mwidget);
			}


			var params = {
				mainMenu: this._mainMenu.getChildren().length > 0 ? this._mainMenu : null,
				actionMenu: this._actionMenu,
				stateSelect: stateSelect,
				onSearch: function (data) {
					var state= "ok";
					if(mwidget.stateSelect){
						state = mwidget.stateSelect.getModelSelection().getItem(0);
					}
					if (mwidget.preFilter) {
						var app = qx.core.Init.getApplication();
						var appid = app.getUserData("appid");
						var vars = {};
						vars.appname = appid;
						var preFilter = self.__supplant(mwidget.preFilter, vars);
						preFilter = qx.lang.Json.parse(preFilter);
						if (!preFilter.children) {
							preFilter.children = [];
						}
						var filter = qx.lang.Json.parse(data);
						var newFilter = {
							label: "1",
							connector: "and",
							children: []
						};
						newFilter.children.push(filter);
						newFilter.children.push(preFilter);
						data = qx.util.Serializer.toJson(newFilter);
					}

					var idArray = mwidget.exclusionListTable.getIdArray();
					if (idArray.length == 0) {
						mwidget.table.setFilter(data,state);
					} else {
						var filter = qx.lang.Json.parse(data);
						var idFilter = self._createIdFilter(idArray);

						var newFilter = {
							label: "1",
							connector: "except",
							children: []
						};
						newFilter.children.push(filter);
						newFilter.children.push(idFilter);

						newFilter = qx.util.Serializer.toJson(newFilter);
						mwidget.table.setFilter(newFilter,state);
					}
					self._enableActionMenuButtons(true);

					var ns = mwidget.storeDesc.getNamespace();
					var app = qx.core.Init.getApplication();
					var dt = app.getDesktop(ns);
					dt.updateStatus();
				},
				onSave: function (data) {
					self._saveSearchFilter(data, mwidget.config, mwidget.exclusionListTable);
				},
				onSelect: function (sf) {
					self._selectSearchFilter(mwidget.config, mwidget.exclusionListTable, mwidget.storeDesc, sf);
				},
				windowTitle: this.tr("meta.lists.windowtitle")
			}
			var sf = new ms123.searchfilter.SearchFilter(root, fields, params, true);
			if (sf == null) return;
			mwidget.searchFilter = sf;
			if (filterModel != null) {
				sf.setModel(filterModel);
			}
			return sf;
		},
		_createExclusionWindow: function (mwidget) {
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
			this._mainTab.getApplicationRoot().add(win);
			var self = this;
			var buttons = [{
				'label': "",
				'icon': "icon/16/places/user-trash.png",
				'callback': function (m) {
					this.deleteCurrentRecord();
				},
				'value': "del"
			}];
			var context = {};
			context.buttons = buttons;
			console.error("MW:" + mwidget.storeDesc + "/" + mwidget.config);
			context.model = this.__configManager.getEntityModel(mwidget.config, mwidget.storeDesc, "main-grid", "properties");
			context.modelForm = this.__configManager.getEntityModel(mwidget.config, mwidget.storeDesc, "main-form", "properties");
			context.unit_id = this.unit_id;
			context.config = "exclusion";
			context.user = this._user;
			context.storeDesc = mwidget.storeDesc;

			mwidget.exclusionListTable = new ms123.widgets.Table(context);
			win.add(mwidget.exclusionListTable);
			mwidget.exclusionListWindow = win;

		},
		_createMainMenu: function (mwidget) {
			var menu = new qx.ui.menu.Menu;

			var exclusionListButton = new qx.ui.menu.Button(this.tr("composite.exclusion_list"), "icon/16/actions/list-remove.png", new qx.ui.core.Command("Ctrl+E"));
			var newSearchButton = new qx.ui.menu.Button(this.tr("filter.text.new_search"), "icon/16/actions/system-search.png", new qx.ui.core.Command("Ctrl+N"));
			var addButton = new qx.ui.menu.Button(this.tr("filter.text.add_to"), "icon/16/actions/list-add.png", new qx.ui.core.Command("Ctrl+A"));
			var subButton = new qx.ui.menu.Button(this.tr("filter.text.sub_from"), "icon/16/actions/list-remove.png", new qx.ui.core.Command("Ctrl+S"));
			var newWinButton = new qx.ui.menu.Button(this.tr("filter.text.new_win"), "icon/16/actions/edit-undo.png", new qx.ui.core.Command("Ctrl+W"));
			var dupCheckButton = new qx.ui.menu.Button(this.tr("composite.quality.start_dupcheck"), "icon/16/actions/system-run.png");
			var ns = mwidget.storeDesc.getNamespace();

			exclusionListButton.addListener("execute", this._exclusionListListener, this);
			newSearchButton.addListener("execute", this._mainMenuListener, this);
			addButton.addListener("execute", this._mainMenuListener, this);
			subButton.addListener("execute", this._mainMenuListener, this);
			newWinButton.addListener("execute", this._mainMenuListener, this);
			dupCheckButton.addListener("execute", this._runDupCheck.bind(this,ns, mwidget.config), this);

			if (mwidget.exclusionlist) {
				menu.add(exclusionListButton);
			}
			if (mwidget.multipletabs) {
				menu.add(newSearchButton);
				menu.add(addButton);
				menu.add(subButton);
			}

			if(ms123.config.ConfigManager.isAdmin() && mwidget.state_select && ms123.config.ConfigManager.hasStateSelect()) {
				menu.add(dupCheckButton);
			}

			return menu;
		},
		_createActionMenu: function (mwidget) {
			var menu = new qx.ui.menu.Menu;
			this._actionMenuButtons = [];
			var settings = this.__configManager.getEntityViewProperties(mwidget.config,mwidget.storeDesc);
			console.log("settings:"+JSON.stringify(settings,null,2));
			var workflowList = settings.workflow_list;

			var multiEditButton = new qx.ui.menu.Button(this.tr("filter.text.multi_edit"), "icon/16/actions/edit-copy.png", new qx.ui.core.Command("Ctrl+M"));
			multiEditButton.addListener("execute", this._actionMenuListener, this);
			multiEditButton.setUserData("cmd", "multiedit");
			multiEditButton.setUserData("widget", mwidget);
			menu.add(multiEditButton);
			this._actionMenuButtons.push(multiEditButton);

			for (var i = 1; i < this.widgets.length; i++) {
				var widget = this.widgets[i];
				if (widget.multi_add) {
					var multiAddButton = new qx.ui.menu.Button(this.tr("filter.text.multi_add_" + widget.config), "icon/16/actions/list-add.png");
					multiAddButton.addListener("execute", this._actionMenuListener, this);
					multiAddButton.setUserData("cmd", "multiadd");
					multiAddButton.setUserData("widget", widget);
					this._actionMenuButtons.push(multiAddButton);
					menu.add(multiAddButton);
				}
			}
			if( workflowList){
				menu.addSeparator();
				for(var i=0;i < workflowList.length;i++){
					var wf = workflowList[i];
					console.log("menuEntry:"+wf.menuname);
					var name= wf.menuname ? this.tr(wf.menuname) : wf.workflow;
					var wfButton = new qx.ui.menu.Button(name, "icon/16/actions/system-run.png");
					wfButton.addListener("execute", this._workflowMenuListener, this);
					wfButton.setUserData("cmd", "startwf");
					wfButton.setUserData("workflow", wf);
					this._actionMenuButtons.push(wfButton);
					menu.add(wfButton);
				}
			}	
			return menu;
		},

		_startWorkflow: function (wf,filter,mwidget) {
			var uprops = ms123.config.ConfigManager.getUserProperties();
			filter.modulename = mwidget.config;
			var storeDesc = mwidget.storeDesc;
			var _filter = filter;
			console.log("_filter:"+wf.filtername);
			if( wf.filtername && wf.filtername != "" ){
				_filter = this._getFilter(wf.filtername, storeDesc);
				_filter.filter = filter;
			}
			console.log("_filter:"+JSON.stringify(_filter,null,2));
			var context = {
				namespace:storeDesc.getNamespace(),
				workflowName:wf.workflow,
				parameter:{filter:_filter},
				title: wf.menuname ? this.tr(wf.menuname).toString() : wf.workflow,
				window_title: wf.menuname ? this.tr(wf.menuname).toString() : wf.workflow,
				storeDesc:storeDesc,
				userid: uprops.userid,
				parentContainer: null
			};
			console.log("namespace:" + this.namespace + "/" + name);
			new ms123.DesktopWindow(context, ms123.processexplorer.ProcessController);
		},
		_runDupCheck: function (namespace, entityName) {
			var dia = new ms123.form.Alert({
				"message": this.tr("composite.quality.dupcheck.started"),
				"noOkButton": true,
				"inWindow": true,
				"hide": false,
				"context": this
			});
			dia.show();
			var completed = function (e) {
				var content = e;
				dia.hide();
				ms123.form.Dialog.alert(this.tr("composite.quality.dupcheck_ok"));
			};
			var failed = function (e) {
				dia.hide();
				ms123.form.Dialog.alert("CRUD._runDupCheck:" + e);
			};
			var rpcParams = {
				namespace: namespace,
				entityName: entityName
			}

			var params = {
				service: "quality",
				method: "dupCheck",
				parameter: rpcParams,
				completed:completed.bind(this),
				failed: failed,
				async: true,
				context: this
			}
			ms123.util.Remote.rpcAsync(params);
		},
		_createStateSelect: function (mwidget) {
			var data = {
				items: [
				{
					label: this.tr("composite.stateselect.ok_new"),
					data: "ok"
				},
				{
					label: this.tr("composite.stateselect.dup"),
					data: "dup"
				},
				{
					label: this.tr("composite.stateselect.new"),
					data: "new"
				}
				]
			};
			var model = qx.data.marshal.Json.createModel(data);
			var ss = new qx.ui.form.SelectBox();
			var ssController = new qx.data.controller.List(null, ss);
			ssController.setDelegate({
				bindItem: function (controller, item, index) {
					controller.bindProperty("label", "label", null, item, index);
					controller.bindProperty("data", "model", null, item, index);
				}
			});
			ssController.setModel(model.getItems());
			mwidget.stateSelect = ss;
			return ss;
		},

		_getFilter:function(filterName,storeDesc){
			var filter = null;
			try {
				filter = ms123.util.Remote.rpcSync("git:searchContent", {
					reponame: storeDesc.getNamespace(),
					name: filterName,
					type: "sw.filter"
				});
				filter = filter.evalJSON();
			} catch (e) {
				ms123.form.Dialog.alert("DocumentMarkdown._getFilterField:" + e);
				return;
			}
			return filter;
		},
		_enableActionMenuButtons: function (enable) {
			for (var i = 0; i < this._actionMenuButtons.length; i++) {
				this._actionMenuButtons[i].setEnabled(enable);
			}
		},
		_exclusionListListener: function (e) {
			this.widgets[0].exclusionListWindow.setActive(true);
			this.widgets[0].exclusionListWindow.open();
		},
		_actionMenuListener: function (e) {
			var button = e.getTarget();
			var widget = button.getUserData("widget");
			var mwidget = this.widgets[0];
			var cmd = button.getUserData("cmd");
			if (cmd == "multiedit") {
				if (mwidget.multiEditWindow == null) {
					this._createMultiEditWindow(mwidget);
				}
				mwidget.multiEditWindow.setActive(true);
				mwidget.multiEditForm.fillForm({});
				mwidget.multiEditWindow.open();
			} else if (cmd == "multiadd") {
				if (widget.multiAddWindow == null) {
					this._createMultiAddWindow(mwidget, widget);
				}
				widget.multiAddWindow.setActive(true);
				widget.multiAddForm.fillForm({});
				widget.multiAddWindow.open();
			}
		},
		_workflowMenuListener: function (e) {
			var button = e.getTarget();
			var wf = button.getUserData("workflow");
			var mwidget = this.widgets[0];
			var cmd = button.getUserData("cmd");
			if (cmd == "startwf") {
				console.error("start:"+wf.workflow);
				var filter = mwidget.searchFilter.getFilterObject();
				this._startWorkflow(wf,filter,mwidget);
			} 
		},
		_mainMenuListener: function (e) {
			var button = e.getTarget();
			var cmd = button.getCommand();
			cmd = cmd.toString().match("\\+.$");
			if (cmd == "+N") {
				this._createUnit(null);
			} else if (cmd == "+A" || cmd == "+S") {
				var selection = [];
				for (var u in this.units) {
					var item = {};
					item.label = this.units[u][0].title;
					item.value = u;
					if (this.widgets.numTab != this.units[u].numTab) {
						selection.push(item);
					}
				}
				var curname = this.widgets[0].title;
				var formData = {
					'tab': {
						'type': "SelectBox",
						'label': this.tr("composite.contextmenu.tab"),
						'value': 1,
						'options': selection
					}
				};
				if (selection.length < 1) {
					ms123.form.Dialog.alert(this.tr("composite.contextmenu.too_little_tabs"));
				} else {
					var msg = this.tr("composite.contextmenu.sub_from_tab");
					if (cmd == "+A") {
						msg = this.tr("composite.contextmenu.add_to_tab");
					}
					ms123.form.DialogForm.form(curname + " " + msg, formData, function (result) {
						if (result) {
							var widgets = this.units[result.getTab()];
							var sf = widgets[0].searchFilter;
							var data = {
								label: "1",
								nodeName: "1",
								connector: (cmd == "+A") ? "union" : "except",
								children: []
							};
							var model = qx.data.marshal.Json.createModel(data);


							data = qx.util.Serializer.toNativeObject(sf.getModel(), null);
							var model1 = qx.data.marshal.Json.createModel(data);

							data = qx.util.Serializer.toNativeObject(this.widgets[0].searchFilter.getModel(), null);
							var model2 = qx.data.marshal.Json.createModel(data);

							model.getChildren().push(model1);
							model.getChildren().push(model2);
							model.setLabel(" ");

							sf.renameModelLabel(model1, "1");
							sf.renameModelLabel(model2, "2");
							this._createUnit(model);
						}
					}, this);
				}
			}
		},
		_createMultiEditWindow: function (mwidget) {
			var win = this._createWindow(mwidget.config, 400, 650, false);
			this._mainContainer.getApplicationRoot().add(win);
			var self = this;
			var buttons = [{
				'label': this.tr("data.multiedit.start"),
				'icon': "icon/22/actions/dialog-ok.png",
				'callback': function (formData) {
					var state= "ok";
					console.log("stateSelect;"+mwidget.stateSelect);
					if(mwidget.stateSelect){
						state = mwidget.stateSelect.getModelSelection().getItem(0);
					}
					var filter = mwidget.searchFilter.getFilterObject();
					var hints = formData["__hints__"];
					if (hints) {
						delete formData["__hints__"];
					} else {
						hints = {};
					}
					console.log("hints:" + hints);
					var idArray = mwidget.exclusionListTable.getIdArray();
					var newFilter = filter;
					if (idArray.length > 0) {
						var idFilter = self._createIdFilter(idArray);
						newFilter = {
							label: "1",
							connector: "except",
							children: []
						};
						newFilter.children.push(filter);
						newFilter.children.push(idFilter);
					}
					var props = mwidget.model.attr("gridProps");
					var dia = new ms123.form.Alert({
						"message": self.tr("composite.waiting"),
						"noOkButton": true,
						"inWindow": true,
						"hide": false,
						"context": self
					});
					dia.show();
					var completed = function (e) {
						var content = e;
						dia.hide();
						var cv = content;
						if (cv.constraintViolations !== undefined) {
							var message = "";
							for (var i = 0; i < cv.constraintViolations.length; i++) {
								var c = cv.constraintViolations[i];
								message += mwidget.multiEditForm.getLabel(c.path) + " : " + c.message + "<br />";
							}
							ms123.form.Dialog.alert(message);
							mwidget.multiEditForm.setErrors(cv.constraintViolations);
						} else {
							ms123.form.Dialog.alert(self.tr("data.multiedit.finished"));
							var f = qx.util.Serializer.toJson(filter);
							mwidget.table.setFilter(f,state);
						}
					};
					var failed = function (e) {
						dia.hide();
						var msg = e.message;
						msg = msg.replace(/\|/g, "<br/>");
						msg = msg.replace(/Script.*groovy: [0-9]{0,4}:/g, "<br/><br/>");
						msg = msg.replace(/ for class: Script[0-9]{1,2}/g, "");
						msg = msg.replace(/Script[0-9]{1,2}/g, "");
						msg = msg.replace(/Application error 500:/g, "");
						msg = msg.replace(/:java.lang.RuntimeException/g, "");
						msg = msg.replace(/: {0,2}Line:/g, "<br/>Line:");
						msg = msg.replace(/-{10,100}/g, "<br/>");

						var alert = new ms123.form.Alert({
								"message": "<b>Error</b><br/>"+msg,
								"windowWidth": 500,
								"windowHeight": 100,
								"useHtml": true,
								"inWindow": true
								});
						alert.show();
					};
					var rpcParams = {
						storeId: mwidget.storeDesc.getStoreId(),
						entity: mwidget.config,
						data: formData,
						filter: newFilter,
						state: state,
						hints: hints
					}

					var params = {
						service: "data",
						method: "update",
						parameter: rpcParams,
						completed:completed,
						failed: failed,
						async: true,
						context: this
					}
					ms123.util.Remote.rpcAsync(params);
				},
				'value': "save"
			}];
			var context = {};
			context.buttons = buttons;
			context.model = this.__configManager.getEntityModel(mwidget.config, mwidget.storeDesc, "main-form", "properties");
			context.unit_id = mwidget.unit_id;
			context.config = mwidget.config;
			context.storeDesc = mwidget.storeDesc;
			context.multiedit = true;
			var form = new ms123.widgets.Form(context);
			win.add(form);
			mwidget.multiEditForm = form;
			mwidget.multiEditWindow = win;
		},
		_createMultiAddWindow: function (mwidget, widget) {
			var win = this._createWindow(widget.config, 400, 650, false);
			this._mainContainer.getApplicationRoot().add(win);
			var self = this;
			var buttons = [{
				'label': this.tr("data." + widget.config + ".multiadd"),
				'icon': "icon/22/actions/dialog-ok.png",
				'callback': function (formData) {
					var state= "ok";
					if(mwidget.stateSelect){
						state = mwidget.stateSelect.getModelSelection().getItem(0);
					}
					var filter = mwidget.searchFilter.getFilterObject();
					var idArray = mwidget.exclusionListTable.getIdArray();
					var newFilter = filter;
					if (idArray.length > 0) {
						var idFilter = self._createIdFilter(idArray);
						newFilter = {
							label: "1",
							connector: "except",
							children: []
						};
						newFilter.children.push(filter);
						newFilter.children.push(idFilter);
					}
					var dia = new ms123.form.Alert({
						"message": self.tr("composite.waiting"),
						"noOkButton": true,
						"inWindow": true,
						"hide": false,
						"context": self
					});
					dia.show();
					var completed = function (e) {
						dia.hide();
						var cv = e;
						dia.hide();
						if (cv.constraintViolations !== undefined) {
							var message = "";
							for (var i = 0; i < cv.constraintViolations.length; i++) {
								var c = cv.constraintViolations[i];
								message += widget.multiAddForm.getLabel(c.path) + " : " + c.message + "<br />";
							}
							ms123.form.Dialog.alert(message);
							widget.multiAddForm.setErrors(cv.constraintViolations);
						} else {
							ms123.form.Dialog.alert(self.tr("data." + widget.config + ".created"));
							this.fillForm({});
						}

					};
					var failed = function (e) {
						ms123.form.Dialog.alert("Composite._createMultiAddWindow:" + e);
					};
					var rpcParams = {
						storeId: mwidget.storeDesc.getStoreId(),
						entity: mwidget.config,
						entityChild: widget.config,
						data: formData,
						state: state,
						filter: newFilter
					}

					var params = {
						service: "data",
						method: "insert",
						parameter: rpcParams,
						completed:completed,
						failed: failed,
						async: true,
						context: this
					}
					ms123.util.Remote.rpcAsync(params);
				},
				'value': "save"
			}];
			var context = {};
			context.buttons = buttons;
			context.model = this.__configManager.getEntityModel(widget.config, mwidget.storeDesc, "main-form", "properties");
			//context.model = widget.modelForm;
			context.unit_id = widget.unit_id;
			context.config = widget.config;
			context.storeDesc = widget.storeDesc;
			var form = new ms123.widgets.Form(context);
			win.add(form);
			widget.multiAddForm = form;
			widget.multiAddWindow = win;
		},
		_createWindow: function (name, height, width, minmax) {
			var win = new qx.ui.window.Window(name, "").set({
				resizable: true,
				useMoveFrame: true,
				useResizeFrame: true
			});
			win.setLayout(new qx.ui.layout.Grow);
			win.setWidth(width !== undefined ? width : 600);
			win.setHeight(height !== undefined ? height : 300);
			win.setAllowMaximize(minmax);
			win.setAllowMinimize(minmax);
			win.setModal(true);
			win.setActive(false);
			win.minimize();
			win.center();
			return win;
		},
		_contextMenuHandler: function (col, row, table, dataModel, contextMenu) {
			var contextMenuEntries = table.getUserData("contextMenuEntries");
			var moduleName = table.getUserData("moduleName");
			for (var i = 0; i < contextMenuEntries.length; i++) {
				var clazz = contextMenuEntries[i].clazz;

				var menuEntry = new qx.ui.menu.Button(contextMenuEntries[i].text, contextMenuEntries[i].icon);
				menuEntry.setUserData("contextMenuEntry", contextMenuEntries[i]);
				menuEntry.addListener("execute", function (e) {
					var map = table.getTableModel().getRowDataAsMap(row);

					var contextMenuEntry = this.getUserData("contextMenuEntry");
					var clazz = contextMenuEntry.clazz;
					var inWindow = contextMenuEntry.inWindow !== undefined ? contextMenuEntry.inWindow : true;
					var win = null;
					var context = {};
					context.data = map;
					context.menuEntry = contextMenuEntry;
					context.moduleName = moduleName;
					context.window_title = contextMenuEntry.text;
					if (inWindow) {
						new ms123.DesktopWindow(context, clazz);
					} else {
						new clazz(context);
					}
				}, menuEntry);
				contextMenu.add(menuEntry);
			}
			return true;
		},
		_contextMenuHandlerExclusion: function (col, row, table, dataModel, contextMenu) {
			var menuEntry = new qx.ui.menu.Button(this.tr("composite.add_to_exclusion_list"), "icon/16/actions/list-remove.png");
			menuEntry.addListener("execute", function (e) {
				var map = table.getTableModel().getRowDataAsMap(row);
				var eventBus = qx.event.message.Bus;
				var context = table.getUserData("context");
				eventBus.getInstance().dispatchByName(context.unit_id + ".table.contextmenu.row.selected", {
					row: map
				});
			});
			contextMenu.add(menuEntry);
			return true;
		},
		_contextMenuHandlerDetails: function (col, row, table, dataModel, contextMenu) {
			var entityName = table.getUserData("entityName");
			var storeDesc = table.getUserData("storeDesc");
			var dunitmap = table.getUserData("dunitmap");
			var menuEntry = new qx.ui.menu.Button(this.tr("composite.open_record_in_window"), "icon/16/actions/window-new.png");
			menuEntry.addListener("execute", function (e) {
				var rmap = table.getTableModel().getRowDataAsMap(row);
				console.error("entityName:" + entityName);
				console.log("map:" + JSON.stringify(rmap));

				var id = rmap.id;
				var c = dunitmap[entityName];
				if (c === undefined || c == null) {
					var m = new ms123.config.ConfigManager().getEntity(entityName, storeDesc);
					var widgetList = ms123.MainMenu.createWidgetList(m, storeDesc, this);
					widgetList[0].loadSync = true;
					var context = {
						storeDesc: storeDesc,
						unit_id: ms123.util.IdGen.nextId(),
						config: ms123.Crud,
						window_title: this.tr("data." + entityName),
						widgets: widgetList
					}
					var dt = new ms123.DesktopWindow(context);
					c = dt.getDesktopUnit();
					dunitmap[entityName] = c;
					c.window.addListener("beforeClose", function (e) {
						console.error("CRUD.CLOSE");
						dunitmap[entityName] = null;
					}, this);
				}
				c.selectId(id);
			},this);
			contextMenu.add(menuEntry);
			return true;
		},
		_clone: function (obj) {
			if (obj == null || typeof(obj) != 'object') {
				return obj;
			}
			var temp = new obj.constructor(); // changed (twice)
			for (var key in obj) {
				temp[key] = this._clone(obj[key]);
			}
			return temp;
		},
		__supplant: function (s, o) {
			if (!o) return s;
			return s.replace(/[\\$@]{([^{}]*)}/g, function (a, b) {
				var r = o[b];
				return typeof r === 'string' || typeof r === 'number' ? r : a;
			});
		},
		_table_row_selected: function (msg) {
			console.log("_table_row_selected:" + msg);
			var data = msg.getData();
			console.log("==>> table.row.selected:" + data.idMaster);
			console.log("\tname:" + data.context.config);
			console.log("\tmaster:" + this._configMaster);
			var mwidget = this.widgets[0];
			if ((this.widgets && this.widgets.length > 1) && this._configMaster == data.context.config) {
				mwidget.detailsTabView.setEnabled(true);
				var idMaster = data.idMaster;
				jQuery.each(this.widgets, function (index, widget) {
					if (index == 0) return;
					console.log("\twidget.type:" + widget.type);
					console.log("\twidget.name:" + widget.config);
					console.log("\twidget.component:" + widget.component);
					console.log("\tidMaster:" + idMaster);
					console.log("\twidget.depend:" + widget.dependent);
					if (widget.component || widget.hasDelayedComponent) {
						if (widget.component == null) {
							widget.isDetail = true;
							widget.component = new ms123.widgets.Form(widget);
							widget.parent._add(widget.component);
						}
						try {
							widget.component.showRecord(idMaster, data.row);
							if (widget.component instanceof ms123.widgets.Form) {
								if (data.idMaster == null) {
									widget.component.form.setReadonly(true);
								} else {
									widget.component.form.setReadonly(false);
								}
								if (widget.dependent == false) {
									widget.component.form.setReadonly(true);
								}
							}
						} catch (e) {
							console.error("widget.component.showRecord.error:" + e);
						}
					}
				});
				if (data.idMaster == null) {
					mwidget.detailsTabView.setEnabled(false);
				}
			}
		},
		__isUpdateAllowed: function (mwidget) {
			if (this._user.admin) return true;
			if (!this.__access) {
				this.__access = this.__configManager.getEntityAccess(mwidget.storeDesc, this._configMaster);
			}

			if (this.__access["update"] === undefined || this.__access["update"] == "all" || this.__access["update"] == "owner") return true;
			return false;
		},
		getState:function(){
		  var widgets = this._mainTab.getSelection()[0].getUserData("widgets");
			var sf = widgets[0].searchFilter;
			var f = sf.getFilter();
			return f;
		},
		setState:function(state){
			console.log("CRUD.setState:"+state);
			var sf = this.widgets[0].searchFilter;
			var f = qx.lang.Json.parse(state);
			try{
				sf.setFilter(f);
			}catch(e){
				console.error("Errror:"+e);
				console.log(e.stack);
			}
			console.log("after:");
			try {
				sf.scrollToEnd();
			} catch (e) {}
		}
	},
	destruct: function () {
		console.error("Crud.destruct1");
		var eventBus = qx.event.message.Bus;
		eventBus.unsubscribe(this.unit_id + ".table.contextmenu.row.selected");
		eventBus.unsubscribe(this.unit_id + ".table.row.selected");
		this._disposeObjects("_mainMenu");
		this._disposeObjects("_dunitmap");
		this._disposeArray("_actionMenuButtons");
	}

});
