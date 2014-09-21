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
 * @ignore($)
 */
qx.Class.define("ms123.enumerations.EnumEditor", {
	extend: qx.ui.container.Composite,


	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (model, param, facade) {
		this.base(arguments);
		this._model = model;
		this._facade = facade;
		this._init(facade);
	},


	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},


	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		__user: null,

		_init: function (context) {
			this.setLayout(new qx.ui.layout.Dock());
			this.setBackgroundColor("white");
			//this.__user = ms123.util.Remote.rpcSync("auth:getUserProperties");
			this.__user = ms123.config.ConfigManager.getUserProperties();
			this.__storeDesc = context.storeDesc;


			var rightTabView = new qx.ui.tabview.TabView().set({
				contentPadding: 0
			});

			this.add(rightTabView, {
				edge: "center"
			});
			var toolbar = this.__createToolbar();
			this.add(toolbar, {
				edge: "south"
			});
			this.__rightWidget = rightTabView;

			this.__makeTabs();

			var name = this._model.getId();
			this.__currentEnumName = name;

			var en = null;
			try {
				en = ms123.util.Remote.rpcSync("enumeration:getEnumeration", {
					namespace: this.__storeDesc.getNamespace(),
					name: name
				});
			} catch (e) {
				ms123.form.Dialog.alert("EnumEditor._init:" + e);
				return;
			}
			console.log("EN:" + en);
			this.__currentEnumData = en;
			if (!en.fieldList) en.fieldList = [];
			if (!en.dataList) en.dataList = [];
			this.__showFieldsgrid(en.fieldList);
			this.__showDatagrid(en.fieldList, en.dataList);
		},

		__makeTabs: function () {
			this.dataPage = new qx.ui.tabview.Page(this.tr("meta.enums.data")).set({
				showCloseButton: false
			});
			this.dataPage.setDecorator(null);
			this.dataPage.setLayout(new qx.ui.layout.Grow());
			this.__rightWidget.add(this.dataPage, {
				edge: 0
			});
			this.fieldsPage = new qx.ui.tabview.Page(this.tr("meta.enums.fields")).set({
				showCloseButton: false
			});
			this.fieldsPage.setDecorator(null);
			this.fieldsPage.setLayout(new qx.ui.layout.Grow());
			this.__rightWidget.add(this.fieldsPage, {
				edge: 0
			});

		},
		__showFieldsgrid: function (fieldList) {
			this.fieldsPage.removeAll();

			for (var i = 0; i < fieldList.length; i++) {
				var row = fieldList[i];
				row.description = this.tr(row.description).toString();
			}

			var gridProps = {};
			gridProps.paging = false;
			gridProps.search = false;
			gridProps.formlayout = "tab1";

			var gridConfig = {};
			gridConfig.isMaster = true;
			gridConfig.config = "enumfield";
			gridConfig.loadBeforeEdit = false;
			gridConfig.toolbar = new ms123.enumerations.EnumToolbar({save:this.__saveEnum.bind(this)});
			var cm = new ms123.config.ConfigManager();
			gridConfig.model = cm.getEntityModel("enumfield", ms123.StoreDesc.getGlobalMetaStoreDesc(), "main-grid", gridProps);
			gridConfig.modelForm = cm.getEntityModel("enumfield", ms123.StoreDesc.getGlobalMetaStoreDesc(), "main-form", gridProps);
			gridConfig.unit_id = "xxx";
			gridConfig.user = this.__user;
			gridConfig.disable = ["import", "export"];
			gridConfig.storeDesc = this.__storeDesc;
			gridConfig.dataAccess = new ms123.enumerations.EnumDataAccess();
			var table = new ms123.widgets.Table(gridConfig);
			this.fieldsPage.add(table);
			table.setData(fieldList);
			this._fieldTable = table;
			table.addListener("addRecord", function () {
				this.__saveEnum(this);
			}, this);
			table.addListener("changeRecord", function () {
				this.__saveEnum(this);
			}, this);
			table.addListener("deleteRecord", function () {
				this.__saveEnum(this);
			}, this);

		},
		__showDatagrid: function (fieldList, dataList) {
			this.dataPage.removeAll();
			var cm = new ms123.config.ConfigManager();
			var columns = cm.getEntityViewFields("enumdata", ms123.StoreDesc.getGlobalMetaStoreDesc(), "main-grid", false);
			columns = ms123.util.Clone.clone(columns);
			for (var i = fieldList.length - 1; i >= 0; i--) {
				var row = fieldList[i];
				var col = {};
				col.id = row.fieldname;
				col.name = row.fieldname;
				if (col.id == 'value' || col.id == 'label') {
					col.constraints = "[{\"annotation\":\"NotEmpty\",\"parameter1\":\"\"}]";
				}
				col.label = this.tr(row.description);
				col.datatype = "string";
				columns.splice(0, 0, col);
			}
			var cm = new ms123.config.ConfigManager();
			var cols = cm.buildColModel(columns, "enumdata", this.__storeDesc, "meta");

			var gridProps = {};
			gridProps.paging = false;
			gridProps.search = false;
			gridProps.formlayout = "tab1";

			var gridConfig = {};
			gridConfig.isMaster = true;
			gridConfig.config = "enumdata";
			gridConfig.toolbar = new ms123.enumerations.EnumToolbar({save:this.__saveEnum.bind(this)});

			gridConfig.loadBeforeEdit = false;
			gridConfig.model = this.__buildModel(cols, gridProps);
			gridConfig.modelForm = gridConfig.model;
			gridConfig.unit_id = "xxx";
			gridConfig.storeDesc = this.__storeDesc;
			gridConfig.disable = ["import", "export"];
			gridConfig.dataAccess = new ms123.enumerations.EnumDataAccess();
			gridConfig.user = this.__user;
			var table = new ms123.widgets.Table(gridConfig);
			table.setData(dataList);
			this.dataPage.add(table);
			this._dataTable = table;
			table.addListener("addRecord", function () {
				this.__saveEnum(this);
			}, this);
			table.addListener("changeRecord", function () {
				this.__saveEnum(this);
			}, this);
			table.addListener("deleteRecord", function () {
				this.__saveEnum(this);
			}, this);
		},

		__createToolbar: function () {
			var toolbar = new qx.ui.toolbar.ToolBar();
			toolbar.setSpacing(5);
			toolbar.addSpacer();

			var buttonDel = new qx.ui.toolbar.Button(this.tr("entitytypes.delete"), "icon/16/places/user-trash.png");
			buttonDel.addListener("execute", function () {
				var ce = new ms123.form.Confirm({
					"message": this.tr("enumerations.confirm_delete"),
					"warn": true,
					"callback": function (ce) {
						console.log("ce:" + ce);
						if (ce) {
							this.__delete();
							this.setEnabled(false);
						} else {
							console.log("nicht Löschen");
						}
					},
					"context": this,
					"inWindow": true
				});
				ce.setWidth(400);
				ce.show();
			}, this);
			toolbar._add(buttonDel);
			return toolbar;
		},

		__buildModel: function (columns, props) {
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
		__deleteEnum: function () {
			try {
				ms123.util.Remote.rpcSync("enumeration:deleteEnumeration", {
					namespace: this.__storeDesc.getNamespace(),
					name: this.__currentEnumName
				});
			} catch (e) {
				ms123.form.Dialog.alert(this.tr("enumerations.delete_error") + ":" + e);
			}
		},
		__delete: function () {
			var children = this._model.parent.getChildren();
			var len = children.getLength();
			for (var i = 0; i < len; i++) {
				var child = children.getItem(i);
				if (child.getId() == this.__currentEnumName) {
					children.remove(child);
					this.__deleteEnum();
					break;
				}
			}
		},
		__saveEnum: function () {
			try {
				ms123.util.Remote.rpcSync("enumeration:saveEnumeration", {
					namespace: this.__storeDesc.getNamespace(),
					data: {
						dataList: this._dataTable.getData(),
						fieldList: this._fieldTable.getData()
					},
					name: this.__currentEnumName
				});
			} catch (e) {
				ms123.form.Dialog.alert(this.tr("enumerations.save_error") + ":" + e);
			}
		}
	}
});
