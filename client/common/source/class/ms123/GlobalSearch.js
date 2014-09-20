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
	@asset(qx/icon/${qx.icontheme}/22/actions/*)
	@asset(qx/icon/${qx.icontheme}/16/places/*)
	@asset(qx/icon/${qx.icontheme}/22/status/*)
	@asset(qx/icon/${qx.icontheme}/16/apps/*)
	@asset(qx/icon/${qx.icontheme}/16/mimetypes/*)

	@asset(ms123/icons/*)
	@asset(ms123/*)
************************************************************************ */

qx.Class.define("ms123.GlobalSearch", {
	extend: qx.core.Object,
	include: qx.locale.MTranslation,



	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (context) {
		this.base(arguments);
		this._init(context);
		this.__storeDesc = context.storeDesc;
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

			this._fieldmap = {};
			this._dunitmap = {};
			this._tablemodelmap = {};

			var upperWidget = new qx.ui.container.Composite();
			upperWidget.setLayout(new qx.ui.layout.Dock());

			var sp = this._doLayout(data.window, upperWidget);
			data.window.add(sp, {});
			var childs = sp.getChildren();
			var bottonWidget = childs[1];

			this._mainTabs = new qx.ui.tabview.TabView().set({
				contentPadding: 0
			});
			this._mainTabs.addListener("changeSelection", function (e) {
				try {
					this._currentModule = e._target.getSelection()[0].getUserData(ms123.config.ConfigManager.CS_ENTITY);
				} catch (e) {}
				console.log("_cureent:" + this._currentModule);
			}, this);
			bottonWidget.add(this._mainTabs, {
				edge: "center"
			});

			var textArea = new qx.ui.form.TextArea();
			textArea.setAutoSize(true);
			var label = new qx.ui.basic.Label(this.tr("globalsearch.searchstringinput"));
			upperWidget.add(label, {
				edge: "north"
			});
			upperWidget.add(textArea, {
				edge: "center"
			});
			var toolbar = new qx.ui.toolbar.ToolBar();
			toolbar.setSpacing(5);

			var menuPart = new qx.ui.toolbar.Part;
			toolbar.add(menuPart);
			toolbar.addSpacer();

			var clearbutton = new qx.ui.toolbar.Button(this.tr("meta.lists.resetbutton"), "icon/16/actions/view-restore.png");
			clearbutton.addListener("execute", function () {
				var text = textArea.setValue("");
				this._makeResultTabs({});
			}, this);
			toolbar.add(clearbutton);

			var searchbutton = new qx.ui.toolbar.Button(this.tr("meta.lists.searchbutton"), "icon/16/actions/system-search.png");
			searchbutton.addListener("execute", function () {
				var text = textArea.getValue();
				var result = null;
				try {
					result = ms123.util.Remote.rpcSync("lucene:query", {
						namespace: this.__storeDesc.getNamespace(),
						query: text
					});
				} catch (e) {
					ms123.form.Dialog.alert("GlobalSearch.query:" + e);
					return;
				}
				this._makeResultTabs(result);
			}, this);
			toolbar.add(searchbutton);
			upperWidget.add(toolbar, {
				edge: "south"
			});

			this._makeResultTabs({});
		},
		_doLayout: function (parent, upperWidget) {
			var splitpane = new qx.ui.splitpane.Pane("vertical").set({

			});
			splitpane.setDecorator(null);
			var bottonWidget = new qx.ui.container.Composite();
			bottonWidget.setLayout(new qx.ui.layout.Dock());
			bottonWidget.setDecorator(null);
			bottonWidget.setMinHeight(150);
			splitpane.add(upperWidget, 2);
			splitpane.add(bottonWidget, 10);

			return splitpane;
		},
		_makeResultTabs: function (searchResult) {
			var childs = this._mainTabs.getSelectables(true);
			var len = childs.length;
			for (var i = 0; i < len; i++) {
				this._mainTabs.remove(childs[i]);
			}
			for (var module in searchResult) {
				var list = searchResult[module];
				var fields = this._getConfigFields(module);
				var page = new qx.ui.tabview.Page(this.tr("data." + module)).set({
					showCloseButton: true
				});
				page.setUserData(ms123.config.ConfigManager.CS_ENTITY, module);
				page.setDecorator(null);
				page.setLayout(new qx.ui.layout.Grow());
				page.add(this._createResultTable(module, fields, list), {
					edge: "center"
				});
				this._mainTabs.add(page, {
					edge: 0
				});
			}
		},
		_createResultTable: function (module, fields, list) {
			var colIds = new Array();
			var colHds = new Array();

			for (var i = 0; i < fields.length; i++) {
				var col = fields[i];
				colIds.push(col.name);
				colHds.push(this.tr("data." + module + "." + col.name));
			}
			var tableModel = new qx.ui.table.model.Simple();
			this._tablemodelmap[module] = tableModel;
			tableModel.setColumns(colHds, colIds);
			var customMap = {
				tableColumnModel: function (obj) {
					return new qx.ui.table.columnmodel.Resize(obj);
				}
			};
			var table = new qx.ui.table.Table(tableModel, customMap);
			table.setStatusBarVisible(false);
			var tcm = table.getTableColumnModel();
			var resizeBehavior = tcm.getBehavior();
			for (var i = 0; i < 2 && i < fields.length-1; i++) {
				resizeBehavior.setWidth(i, 140);
			}
			for (var i = 0; i < list.length; i++) {
				var map = list[i];
				tableModel.addRowsAsMapArray([map], null, true);
			}
			table.addListener("cellTap", this._tableCellEvents, this);
			return table;
		},
		_tableCellEvents: function (e) {
			var module = this._currentModule;
			var rmap = this._tablemodelmap[module].getRowDataAsMap(e.getRow());
			var id = rmap.id;
			var c = this._dunitmap[module];
			if (c === undefined || c == null) {
				var m = new ms123.config.ConfigManager().getEntity(module,this.__storeDesc);
				var widgetList = ms123.MainMenu.createWidgetList(m, this.__storeDesc, this);
				widgetList[0].loadSync = true;
				var context = {
					storeDesc:this.__storeDesc,
					unit_id: ms123.util.IdGen.nextId(),
					config: ms123.Crud,
					window_title: this.tr("data." + module),
					widgets: widgetList
				}
				var dt = new ms123.DesktopWindow(context);
				c = dt.getDesktopUnit();
				this._dunitmap[module] = c;
				c.window.addListener("close", function (e) {
					this._dunitmap[module] = null;
				}, this);
			}
			c.selectId(id);
		},
		_getConfigFields: function (module) {
			console.log("_getConfigFields:" + module);
			var f = this._fieldmap[module];
			if (f === undefined) {
				try {
					var cm = new ms123.config.ConfigManager();
					f = cm.getEntityViewFields(module,this.__storeDesc,"global-search",false);
					this._fieldmap[module] = f;
				} catch (e) {
					ms123.form.Dialog.alert("GlobalSearch._getConfigFields:" + e);
					return;
				}
			}
			return f;
		}
	}
});
