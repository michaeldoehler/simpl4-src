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
 * @ignore(jQuery.each)
 * @ignore(jQuery.inArray)
 */
qx.Class.define("ms123.team.TeamEditor", {
	extend: qx.ui.core.Widget,


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
		__valueModelMap: null,
		__selected_teamid: null,
		__currentTeamData: null,
		__currentTeamid: null,
		__currentLevel: null,
		__currentModel: null,
		__user: null,

		_init: function (context) {
			this._setLayout(new qx.ui.layout.Grow());
			this.setBackgroundColor("white");

			this.__storeDesc = context.storeDesc;
			//this.__user = ms123.util.Remote.rpcSync( "auth:getUserProperties");
			this.__user = ms123.config.ConfigManager.getUserProperties();
			this.__currentTeamData = null;

			var u = ms123.util.Remote.rpcSync("auth:getUsers", {});
			var users = new qx.data.Array();
			jQuery.each(u.rows, function (index, row) {
				users.push(row.userid);
			});
/*		var g = ms123.util.Remote.rpcSync( "auth:getGroups", {});
			var groups = new qx.data.Array();
			jQuery.each(g, function (index, row) {
				groups.push(row.groupid);
			});*/

			console.log("users:" + qx.util.Serializer.toJson(users));
			//console.log("groups:" + jQuery.toJSON(groups));
			this.__users = users;
			//			this.__groups = groups;
			var sp = this.__doLayout();
			this._add(sp, {
				edge: 0
			});
			context.window.add(this, {
				edge: 0
			});
			var childs = sp.getChildren();
			this._leftWidget = childs[0];
			this._rightWidget = childs[1];
			this.__createTree();
			if(ms123.config.ConfigManager.hasTeamEditorSearch()){
				this._createChildControl("searchField");
			}
			this.__createTeamEdit(context);
			this.__hideForm();
		},
		__createTree: function () {
			var tree = this._createChildControl("tree");
			var t = null;
			try {
				t = ms123.util.Remote.rpcSync("team:getTeamTree", {
					namespace: this.__storeDesc.getNamespace()
				});
			} catch (e) {
				ms123.form.Dialog.alert("TeamEditor._init:" + e);
				return;
			}
			t.title = this.tr("meta.teams.root.title");
			this.__valueModelMap = {};
			this.__completeTree(t);
			var model = qx.data.marshal.Json.createModel(t);
			this.__modelList = [];
			this.__createValueToModelMap(model, this.__valueModelMap);
			tree.setModel(model);
		},
		__completeTree: function (model) {
			if (!model.children) {
				model.children = [];
			}
			if (!model.title) {
				model.title = model.name + "/" + model.description;
			}
			for (var i = 0; model.children && i < model.children.length; i++) {
				var c = model.children[i];
				this.__completeTree(c);
			}
		},
		__createTeamEdit: function (maincontext) {
			var detailsForm = this.__createTeamDetailsDialog();
			this._rightWidget._add(detailsForm, {
				edge: "north"
			});
			var sp = this.__doUserPartLayout();
			this._rightWidget._add(sp, {
				edge: "center"
			});

			var childs = sp.getChildren();
			var userWidget = childs[0];
			var label = null;
			try {
				label = new qx.ui.basic.Label(this.tr("meta.teams.member_users")).set({
					decorator: "radiobutton-hovered"
				});
			} catch (e) {
				label = new qx.ui.basic.Label(this.tr("meta.teams.member_users")).set({});
			}
			userWidget.add(label, {
				edge: "north"
			});

			var userTable = this.__createUserTable("user");
			this.__userTable = userTable;
			userWidget.add(userTable, {
				edge: "center"
			});
			var self = this;
			var userToolbar = this.__createUserToolbar(

			function () {
				var context = {};
				context.modulename = "user";
				context.user = this.__user;
				context.title = self.tr("meta.teams.select_user");
				context.selected_callback = function (value) {
					if (self.__userExists(value.userid)) {
						ms123.form.Dialog.alert(self.tr("meta.teams.user_exists"));
					} else if (value.userid == "admin") {
						ms123.form.Dialog.alert(self.tr("meta.teams.admin_not"));
					} else {
						var map = {
							member: value.userid,
							create: false,
							manage: false
						};
						var tm = userTable.getTableModel();
						tm.addRowsAsMapArray([map], null, true);
					}
				}
				context.storeDesc = ms123.StoreDesc.getGlobalMetaStoreDesc();

				new ms123.util.RecordSelector(context);
			}, function () {
				userTable.stopEditing();
				var selModel = userTable.getSelectionModel();
				var index = selModel.getLeadSelectionIndex();
				console.log("delete:" + index);
				if (index >= 0) {
					userTable.getTableModel().removeRows(index, 1);
				}
			});
			userWidget.add(userToolbar, {
				edge: "south"
			});

			var groupWidget = childs[1];
			var glabel = null;
			try {
				glabel = new qx.ui.basic.Label(this.tr("meta.teams.member_groups")).set({
					decorator: "radiobutton-hovered"
				});
			} catch (e) {
				glabel = new qx.ui.basic.Label(this.tr("meta.teams.member_groups")).set({});
			}
			groupWidget.add(glabel, {
				edge: "north"
			});
			var groupTable = this.__createUserTable("group");
			this.__groupTable = groupTable;
			groupWidget.add(groupTable, {
				edge: "center"
			});
			var groupToolbar = this.__createUserToolbar(

			function () {
				var context = {};
				context.modulename = "group";
				context.title = self.tr("meta.teams.select_group");
				context.selected_callback = function (value) {
					if (self.__groupExists(value.groupid)) {
						ms123.form.Dialog.alert(self.tr("meta.teams.group_exists"));
					} else {
						var map = {
							member: value.groupid,
							create: false,
							manage: false
						};
						var tm = groupTable.getTableModel();
						tm.addRowsAsMapArray([map], null, true);
					}
				};
				context.storeDesc = ms123.StoreDesc.getGlobalMetaStoreDesc();
				new ms123.util.RecordSelector(context);
			}, function () {
				groupTable.stopEditing();
				var selModel = groupTable.getSelectionModel();
				var index = selModel.getLeadSelectionIndex();
				if (index >= 0) {
					groupTable.getTableModel().removeRows(index, 1);
				}
			});
			groupWidget.add(groupToolbar, {
				edge: "south"
			});


			var tree = this.__getTree()
			var item = tree.getSelection().getItem(0);


			var toolbar = new qx.ui.toolbar.ToolBar();

			if(ms123.config.ConfigManager.hasTeamEditorSearch()){
				var bMember = new qx.ui.toolbar.Button(this.tr("meta.teams.member_list"), "icon/22/apps/preferences-users.png").set({})
				bMember.addListener("execute", function () {
					this.__memberList();
				}, this);
				if (this.__user.admin) {
					toolbar._add(bMember);
					toolbar._add(new qx.ui.core.Spacer(), {
						flex: 0
					});
				}
				var self = this;
				var bAllUser = new qx.ui.toolbar.Button(this.tr("meta.teams.all_user"), "").set({})
				bAllUser.addListener("execute", function () {
					var list = self._getTeamUsageByUser(null);
					var table = self._createUserUsageTable(list);
					var w = self._createUsageWindow();
					w.add(table);
					w.open();
				}, this);
				if (this.__user.admin) {
					toolbar._add(bAllUser);
					toolbar._add(new qx.ui.core.Spacer(), {
						flex: 1
					});
				}
			}

			var bsave = new qx.ui.toolbar.Button(this.tr("meta.teams.save_team"), "icon/22/actions/dialog-ok.png").set({})
			bsave.addListener("execute", function () {
				this.__saveTeam();
			}, this);
			var bdel = new qx.ui.toolbar.Button(this.tr("meta.teams.delete_team"), "icon/16/places/user-trash.png").set({})
			bdel.addListener("execute", function () {
				var ce = new ms123.form.Confirm({
					"message": this.tr("meta.teams.confirm_delete"),
					"warn": true,
					"callback": function (ce) {
						console.log("ce:" + ce);
						if (ce) {
							console.log("Löschen");
							//this.__deleteForm();
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
			/*var name = item.getName();
			if (this.__manageAllowed(name) || this.__user.admin) {
				toolbar._add(bdel);
				toolbar._add(new qx.ui.core.Spacer(), {
					flex: 0
				});
			}*/
			toolbar._add(bsave);
			this._rightWidget._add(toolbar, {
				edge: "south"
			});

		},
		__createTeamDetailsDialog: function () {
			var formData = {
				"name": {
					'type': "TextField",
					'label': this.tr("meta.teams.create_team1"),
					'validation': {
						required: true,
						validator: "/^[0-9A-Za-z_\-]{1,20}$/"
					},
					'value': ""
				},
				"description": {
					'type': "TextField",
					'label': this.tr("meta.teams.create_team2"),
					'validation': {
						required: true,
						validator: "/^.{2,80}$/"
					},
					'value': ""
				}/*,
				"validFrom": {
					'type': "DateField",
					'label': this.tr("meta.teams.valid_from"),
					'validation': {
						required: true
					},
					'value': null
				},
				"validTo": {
					'type': "DateField",
					'label': this.tr("meta.teams.valid_to"),
					'validation': {
						required: true
					},
					'value': null
				}*/
			};

			var self = this;
			var form = new ms123.form.Form({
				"tabs": [{
					id: "tab1",
					layout: "single"
				}],
				"useScroll": false,
				"formData": formData,
				"buttons": [],
				"inWindow": false,
				"context": self
			});
			this.__detailsForm = form;
			return form;
		},
		__hideForm: function () {
			this._rightWidget.hide();
		},
		__showForm: function () {
			this._rightWidget.show();
		},
		__saveTeam: function () {
			var valid = this.__detailsForm.validate();
			console.log("valid:" + valid);
			if (valid == false) {
				ms123.form.Dialog.alert(this.tr("meta.teams.form_not_valid"));
				return;
			}
			var m = this.__detailsForm.getModel();
			var props = qx.Class.getProperties(m.constructor);
			var items = this.__detailsForm.getItems();
			var data = {};
			var tid = m.get("name");
			for (var i = 0, l = props.length; i < l; i++) {
				var p = props[i];
				var item = items[p];
				var val = m.get(p);
				if (val.getTime) {
					data[p] = val.getTime();
				} else {
					data[p] = val;
				}

				if (p == "description") {
					this.__currentModel.setTitle(tid + "/" + val);
				}
				if (p == "teamid") {
					this.__currentModel.setName(val);
				}
			}

			var lists = ["userRead", "userManage", "userCreate"];
			for (var j = 0; j < lists.length; j++) {
				var v = lists[j];
				var fields = this.__getFieldsFromTable(v, j);
				var l = [];
				if (fields.length >= 0) {
					jQuery.each(fields, function (index, field) {
						console.log("index:" + field.name + "=" + field.val);
						l.push(field.val);
					});
					data[v] = l;
				}
			}
			if (this.__selected_teamid) {
				try {
					var team = ms123.util.Remote.rpcSync("team:updateTeam", {
						namespace: this.__storeDesc.getNamespace(),
						name: data["name"],
						description: data["description"],
						userCreate: data["userCreate"],
						userRead: data["userRead"],
						userManage: data["userManage"],
						teamid: this.__selected_teamid
					});
					ms123.form.Dialog.alert(this.tr("meta.teams.team_saved"));
				} catch (e) {
					ms123.form.Dialog.alert("TeamEditor.__saveTeam:" + e);
					return;
				}
			}
		},
		__getFieldsFromTable: function (v, j) {
			var tm = this.__userTable.getTableModel();
			var arr = tm.getDataAsMapArray();
			var ret = [];
			for (var i = 0; i < arr.length; i++) {
				var umap = arr[i];
				if (v.indexOf("Read") != -1) {
					var field = {};
					field.name = v;
					field.val = umap.member;
					ret.push(field);
				} else if (v.indexOf("Create") != -1) {
					if (umap.create) {
						var field = {};
						field.name = v;
						field.val = umap.member;
						ret.push(field);
					}
				} else if (v.indexOf("Manage") != -1) {
					if (umap.manage) {
						var field = {};
						field.name = v;
						field.val = umap.member;
						ret.push(field);
					}
				}
			}
			return ret;
		},
		__fillForm: function (map) {
			var x = qx.util.Serializer.toJson(map);
			console.log("fill:" + x + "/v:" + map.validTo);
			this.__detailsForm.fillForm(map);
			var lists = ["userRead", "userManage", "userCreate"];
			var user_list = [];
			var group_list = [];
			for (var j = 0; j < lists.length; j++) {
				var v = lists[j];
				var ur = map[v];
				for (var i = 0; ur && i < ur.length; i++) {
					var u = ur[i];
					console.log("\tlist:" + v + ",u:" + u);
					this.__addOrUpdateUserList(user_list, v, u);
				}
			}
			var x = qx.util.Serializer.toJson(user_list);
			console.log("user_list:" + x);
			this.__fillUserTable(user_list);
			this.__fillGroupTable(group_list);
		},
		__fillUserTable: function (data) {
			var tm = this.__userTable.getTableModel();
			tm.removeRows(0, tm.getRowCount());
			tm.addRowsAsMapArray(data, null, true);
		},
		__fillGroupTable: function (data) {
			var tm = this.__groupTable.getTableModel();
			tm.removeRows(0, tm.getRowCount());
			tm.addRowsAsMapArray(data, null, true);
		},
		__addOrUpdateUserList: function (user_list, right, user) {
			if (!user || user == "" || user == "admin") return;
			if (!this.__users.contains(user)) {
				console.error("User not exists:" + user);
			}
			for (var i = 0; i < user_list.length; i++) {
				var umap = user_list[i];
				if (umap.member == user) {
					if (right == "userCreate") {
						umap.create = true;
					} else if (right == "userManage") {
						umap.manage = true;
					}
					return;
				}
			}
			var umap = {};
			umap.member = user;
			umap.create = false;
			umap.manage = false;
			if (right == "userCreate") {
				umap.create = true;
			} else if (right == "userManage") {
				umap.manage = true;
			}
			user_list.push(umap);
		},
		__addOrUpdateGroupList: function (group_list, right, group) {
			if (!group || group == "") return;
			for (var i = 0; i < group_list.length; i++) {
				var gmap = group_list[i];
				if (gmap.member == group) {
					if (right == "groupCreate") {
						gmap.create = true;
					} else if (right == "groupManage") {
						gmap.manage = true;
					}
					return;
				}
			}
			var gmap = {};
			gmap.member = group;
			gmap.create = false;
			gmap.manage = false;
			if (right == "groupCreate") {
				gmap.create = true;
			} else if (right == "groupManage") {
				gmap.manage = true;
			}
			group_list.push(gmap);
		},
		__groupExists: function (group) {
			var tm = this.__groupTable.getTableModel();
			var arr = tm.getDataAsMapArray();
			for (var i = 0; i < arr.length; i++) {
				var gmap = arr[i];
				if (gmap.member == group) return true;
			}
			return false;
		},
		__userExists: function (user) {
			var tm = this.__userTable.getTableModel();
			var arr = tm.getDataAsMapArray();
			for (var i = 0; i < arr.length; i++) {
				var umap = arr[i];
				if (umap.member == user) return true;
			}
			return false;
		},
		__clearForm: function () {
			this.__fillForm({});
		},
		__enableForm: function (flag) {
			this._rightWidget.setEnabled(flag);
		},
		__readAllowed: function (id) {
			if (this.__currentTeamData == null) return false;
			if (id) {
				if (id != this.__currentTeamData.teamid) return false;
			}
			if (this.__user.admin) return true;
			return ((this.__currentTeamData.userRead && (jQuery.inArray(this.__user.userid, this.__currentTeamData.userRead) != -1)) || (this.__currentTeamData.groupRead && (jQuery.inArray(this.__user.groupid, this.__currentTeamData.groupRead) != -1)));
		},
		__createAllowed: function (id) {
			if (this.__currentTeamData == null) return false;
			if (id) {
				if (id != this.__currentTeamData.teamid) return false;
			}
			if (this.__user.admin) return true;
			return ((this.__currentTeamData.userCreate && (jQuery.inArray(this.__user.userid, this.__currentTeamData.userCreate) != -1)) || (this.__currentTeamData.groupCreate && (jQuery.inArray(this.__user.groupid, this.__currentTeamData.groupCreate) != -1)));
		},
		__manageAllowed: function (id) {
			if (this.__currentTeamData == null) return false;
			if (id) {
				if (id != this.__currentTeamData.teamid) return false;
			}
			if (this.__user.admin) return true;

			return ((this.__currentTeamData.userManage && (jQuery.inArray(this.__user.userid, this.__currentTeamData.userManage) != -1)) || (this.__currentTeamData.groupManage && (jQuery.inArray(this.__user.groupid, this.__currentTeamData.groupManage) != -1)));
		},
		__doLayout: function () {
			var splitpane = new qx.ui.splitpane.Pane("horizontal");
			splitpane.setDecorator("main");


			var leftWidget = new qx.ui.core.Widget();
			leftWidget._setLayout(new qx.ui.layout.Dock());
			leftWidget.setDecorator(null);
			leftWidget.setMinWidth(50);
			splitpane.add(leftWidget, 4);

			var rightWidget = new qx.ui.core.Widget();
			rightWidget._setLayout(new qx.ui.layout.Dock());
			rightWidget.setDecorator(null);
			rightWidget.setMinWidth(150);
			splitpane.add(rightWidget, 7);

			return splitpane;
		},
		__doUserPartLayout: function () {
			var splitpane = new qx.ui.splitpane.Pane("vertical");
			splitpane.setDecorator("main");


			var upperWidget = new qx.ui.container.Composite();
			upperWidget.setLayout(new qx.ui.layout.Dock());
			upperWidget.setDecorator(null);
			upperWidget.setMinHeight(100);
			splitpane.add(upperWidget, 4);

			var bottomWidget = new qx.ui.container.Composite();
			bottomWidget.setLayout(new qx.ui.layout.Dock());
			bottomWidget.setDecorator(null);
			bottomWidget.setHeight(0);
			bottomWidget.setMinHeight(0);
			bottomWidget.setMaxHeight(0);
			splitpane.add(bottomWidget, 0);
			bottomWidget.setEnabled(false);

			return splitpane;
		},
		__createUserToolbar: function (listener_add, listener_del) {
			var toolbar = new qx.ui.toolbar.ToolBar();
			var badd = new qx.ui.toolbar.Button("", "icon/16/actions/list-add.png");
			badd.setToolTipText(this.tr("meta.teams.add_member"));
			badd.addListener("execute", listener_add, this);
			toolbar._add(badd);
			var bdel = new qx.ui.toolbar.Button("", "icon/16/places/user-trash.png");
			bdel.setToolTipText(this.tr("meta.teams.delete_member"));
			bdel.addListener("execute", listener_del, this);
			toolbar._add(bdel);
			toolbar.add(new qx.ui.core.Spacer(), {
				flex: 1
			});
			return toolbar;
		},
		__createUserTable: function (kind) {
			var tableColumns = [{
				name: "member",
				header: this.tr("meta.teams." + kind + "_header")
			},
			{
				name: "create",
				type: "checkbox",
				header: this.tr("meta.teams.create_header")
			},
			{
				name: "manage",
				type: "checkbox",
				header: this.tr("meta.teams.manage_header")
			}];

			var colIds = new Array();
			var colHds = new Array();

			for (var i = 0; i < tableColumns.length; i++) {
				var col = tableColumns[i];
				colIds.push(col.name);
				colHds.push(col.header);
			}
			var tableModel = new qx.ui.table.model.Simple();
			tableModel.setColumns(colHds, colIds);
			var customMap = {
				tableColumnModel: function (obj) {
					return new qx.ui.table.columnmodel.Resize(obj);
				}
			};
			var table = new qx.ui.table.Table(tableModel, customMap);
			table.setStatusBarVisible(false);


			var tcm = table.getTableColumnModel();

			var booleanCellRendererFactory = new qx.ui.table.cellrenderer.Dynamic(this.__booleanCellRendererFactoryFunc);
			var booleanCellEditorFactory = new qx.ui.table.celleditor.Dynamic(this.__booleanCellEditorFactoryFunc);

			for (var i = 0; i < tableColumns.length; i++) {
				var col = tableColumns[i];
				if (col.type == "checkbox") {
					tcm.setDataCellRenderer(i, booleanCellRendererFactory);
					tcm.setCellEditorFactory(i, booleanCellEditorFactory);
					table.getTableModel().setColumnEditable(i, true);
				}
				if (col.width !== undefined) {
					var resizeBehavior = tcm.getBehavior();
					resizeBehavior.setWidth(i, col.width);
				}
			}
			table.addListener("cellTap", function (e) {
				var colnum = table.getFocusedColumn();
				var rownum = table.getFocusedRow();
				if (colnum == 0) return;
				var tm = table.getTableModel();
				if (tm.getValue(colnum, rownum) === true) {
					tm.setValue(colnum, rownum, false);
				} else {
					tm.setValue(colnum, rownum, true);
				}
			}, this, false);

			tableModel = table.getTableModel();
			var selModel = table.getSelectionModel();
			selModel.setSelectionMode(qx.ui.table.selection.Model.SINGLE_SELECTION);
			selModel.addListener("changeSelection", function (e) {}, this);

			return table;
		},
		__booleanCellRendererFactoryFunc: function (cellInfo) {
			return new qx.ui.table.cellrenderer.Boolean;
		},
		__booleanCellEditorFactoryFunc: function (cellInfo) {
			return new qx.ui.table.celleditor.CheckBox;
		},
		__getTree: function () {
			return this.getChildControl("tree");
		},
		__createValueToModelMap: function (model, map) {
			if (model.getTeamid) {
				map[model.getTeamid()] = model;
				this.__modelList.push(model);
			}
			var children = model.getChildren();
			for (var i = 0; i < children.getLength(); i++) {
				var c = children.getItem(i);
				this.__createValueToModelMap(c, map);
			}
		},
		__createTeam: function (name, desc) {
			var parentpath = null;
			if (this.__currentLevel == 0) {
				parentpath = "";
			} else {
				parentpath = this.__currentTeamid.replace(/\./g, "/");
			}
			console.log("create.parentpath:" + parentpath);

			var parent = null;
			try {
				parent = ms123.util.Remote.rpcSync("team:getTeam", {
					namespace: this.__storeDesc.getNamespace(),
					teamid: this.__currentTeamid
				});
			} catch (e) {
				ms123.form.Dialog.alert("TeamEditor.__createTeam:" + e);
				return;
			}

			if (parent.userManage === undefined) {
				parent.userManage = [];
			}
			var data = {};
			parent.userManage.push(this.__user.userid);
			var lists = ["userRead", "userManage", "userCreate"];
			for (var j = 0; j < lists.length; j++) {
				var v = lists[j];
				var l = parent[v];
				if (l !== undefined) {
					var list = [];
					jQuery.each(l, function (index, field) {
						list.push(field);
					});
					data[v] = list;
				}
			}
			/*if (parent.validFrom !== undefined && parent.validFrom != "") {
				data["validFrom"] = parent.validFrom;
			} else {
				data["validFrom"] = 0;
			}
			if (parent.validTo !== undefined && parent.validTo != "") {
				data["validTo"] = parent.validTo;
			} else {
				data["validTo"] = 2537992800000;
			}*/
			var ret = null;
			try {
				ret = ms123.util.Remote.rpcSync("team:createTeam", {
					namespace: this.__storeDesc.getNamespace(),
					teamid:this.__currentTeamid + "." + name,
					name: name,
					description: desc,
					userCreate: data["userCreate"],
					userRead: data["userRead"],
					userManage: data["userManage"]
				});
				ms123.form.Dialog.alert(this.tr("meta.teams.team_created"));
			} catch (e) {
				if (e.toString().indexOf("allowed") != -1) {
					ms123.form.Dialog.alert(this.tr("teameditor.createteam.team_manage_is_false"));
				} else {
					ms123.form.Dialog.alert("TeamEditor.__createTeam:" + e);
				}
				return;
			}

			console.log("retname:" + ret.name);
			var nm = {};
			nm.teamid = ret.id;
			nm.name = name;
			nm.title = name + "/" + desc;
			nm.children = [];
			var model = qx.data.marshal.Json.createModel(nm);
			this.__valueModelMap[nm.teamid] = model;
			this.__modelList.push(model);
			var parentChilds = this.__currentModel.getChildren();
			parentChilds.insertAt(0, model);
			this.__getTree().openNodeAndParents(this.__currentModel);
			this.__getTree().refresh();
			this.__currentModel = model;

		},
		__createTeamDialog: function () {
			var formData = {
				"teamname": {
					'type': "TextField",
					'label': this.tr("meta.teams.create_team1"),
					'validation': {
						required: true,
						validator: "/^[A-Za-z]([0-9A-Za-z_]){2,20}$/"
					},
					'value': ""
				},
				"teamdesc": {
					'type': "TextField",
					'label': this.tr("meta.teams.create_team2"),
					'validation': {
						required: true,
						validator: "/^.{3,60}$/"
					},
					'value': ""
				}
			};

			var self = this;
			var form = new ms123.form.Form({
				"formData": formData,
				"allowCancel": true,
				"inWindow": true,
				"callback": function (m) {
					if (m !== undefined) {
						var val = m.get("teamname");
						var desc = m.get("teamdesc");
						self.__createTeam(val, desc);
					}
				},
				"context": self
			});
			form.show();
		},
		__createEntityDialog: function () {
			var cm = new ms123.config.ConfigManager();
			var entities = cm.getEntities(ms123.StoreDesc.getNamespaceDataStoreDesc());
			var options = [];
			for (var i = 0; i < entities.length; i++) {
				var e = entities[i];
				console.debug("Name:" + e.name);
				if (e.default_fields == null || e.default_fields === false) {
					console.debug("\tNo defaultfields");
					continue;
				}
				var item = {};
				item.value = e.name;
				item.label = this.tr("data." + e.name) + "";
				options.push(item);
			}
			var formData = {
				"entities": {
					'type': "DoubleSelectBox",
					'label': this.tr("team.select_entities"),
					'options': options,
					'value': ""
				}
			};

			var self = this;
			var form = new ms123.form.Form({
				"formData": formData,
				"allowCancel": true,
				"inWindow": true,
				"callback": function (m) {
					if (m !== undefined) {
						var val = m.get("entities");
						if (val && val.length > 0) {
							var usage = self._getTeamUsage(val);
							for (var i = 0; i < usage.length; i++) {
								var u = usage[i];
								var model = self.__valueModelMap[u.teamid];
								u.team = model.getTitle();
								u.entityName = self.tr("data." + u.entityName);
								u.today = self._dateFormat(new Date(), "dd.MM.yyyy");
							}
							var table = self._createUsageTable(usage);
							var w = self._createUsageWindow();
							w.add(table);
							w.open();
						}
					}
				},
				"context": self
			});
			form.show();
		},
		_getTeamUsage: function (entities) {
			try {
				return ms123.util.Remote.rpcSync("team:teamUsage", {
					namespace: this.__storeDesc.getNamespace(),
					teamid: this.__currentTeamid,
					entityNameList: entities
				});
			} catch (e) {
				ms123.form.Dialog.alert("TeamEditor._getTeamUsage:" + e);
				return null;
			}
		},
		_createUsageTable: function (data) {
			var colIds = new Array();
			var colHds = new Array();
			var colWidth = new Array();
			colIds.push("team");
			colHds.push(this.tr("team.team"));
			colWidth.push("41%");

			colIds.push("entityName");
			colHds.push(this.tr("team.entityName"));
			colWidth.push("20%");

			colIds.push("total");
			colHds.push(this.tr("team.usageTotal"));
			colWidth.push("9%");

			colIds.push("active");
			colHds.push(this.tr("team.active"));
			colWidth.push("9%");

			colIds.push("inactive");
			colHds.push(this.tr("team.inactive"));
			colWidth.push("9%");

			colIds.push("today");
			colHds.push(this.tr("team.today"));
			colWidth.push("12%");

			var tableModel = new qx.ui.table.model.Simple();
			tableModel.setColumns(colHds, colIds);
			var customMap = {
				tableColumnModel: function (obj) {
					return new qx.ui.table.columnmodel.Resize(obj);
				}
			};
			var table = new qx.ui.table.Table(tableModel, customMap);
			var tcm = table.getTableColumnModel();
			colWidth.each((function (w, index) {
				var resizeBehavior = tcm.getBehavior();
				resizeBehavior.setWidth(index, w);
			}).bind(this));
			table.setStatusBarVisible(false);
			tableModel.setDataAsMapArray(data, true);
			return table;
		},
		_createUsageWindow: function () {
			var win = new qx.ui.window.Window("", "").set({
				resizable: true,
				useMoveFrame: true,
				useResizeFrame: true
			});
			win.setLayout(new qx.ui.layout.Grow);
			win.setWidth(650);
			win.setHeight(350);
			win.setAllowMaximize(false);
			win.setAllowMinimize(false);
			win.setModal(true);
			win.setActive(false);
			win.minimize();
			win.center();
			this.getApplicationRoot().add(win);
			return win;
		},

		__memberList:function(){
			var self = this;
			var context = {};
			context.modulename = "user";
			context.user = this.__user;
			context.title = this.tr("meta.teams.select_user");
			context.selected_callback = function (value) {
				var user = value.userid;
				var list = self._getTeamUsageByUser(user);
				var table = self._createUserUsageTable(list);
				var w = self._createUsageWindow();
				w.add(table);
				w.open();
			}
			context.storeDesc = ms123.StoreDesc.getGlobalMetaStoreDesc();
			new ms123.util.RecordSelector(context);
		},
		_getTeamUsageByUser: function (userid) {
			try {
				return ms123.util.Remote.rpcSync("team:teamUsageByUser", {
					namespace: this.__storeDesc.getNamespace(),
					userid: userid
				});
			} catch (e) {
				ms123.form.Dialog.alert("TeamEditor._getTeamUsageByUser:" + e);
				return null;
			}
		},
		_createUserUsageTable: function (data) {
			var colIds = new Array();
			var colHds = new Array();
			var colWidth = new Array();
			colIds.push("teamid");
			colHds.push(this.tr("meta.teams.teamid"));
			colWidth.push("10%");

			colIds.push("description");
			colHds.push(this.tr("team.description"));
			colWidth.push("20%");

			colIds.push("name");
			colHds.push(this.tr("meta.teams.name"));
			colWidth.push("10%");

			colIds.push("userRead");
			colHds.push(this.tr("meta.teams.user_read"));
			colWidth.push("20%");

			colIds.push("userManage");
			colHds.push(this.tr("meta.teams.user_manage"));
			colWidth.push("20%");

			colIds.push("userCreate");
			colHds.push(this.tr("meta.teams.user_create"));
			colWidth.push("20%");

			var tableModel = new qx.ui.table.model.Simple();
			tableModel.setColumns(colHds, colIds);
			var customMap = {
				tableColumnModel: function (obj) {
					return new qx.ui.table.columnmodel.Resize(obj);
				}
			};
			var table = new qx.ui.table.Table(tableModel, customMap);

			var selModel = table.getSelectionModel();
			selModel.setSelectionMode(qx.ui.table.selection.Model.SINGLE_SELECTION);
			selModel.addListener("changeSelection", function (e) {
				var index = selModel.getLeadSelectionIndex();
				var map = tableModel.getRowDataAsMap(index);
				var model = this.__valueModelMap[map.teamid];
				var tree = this.__getTree()
				var sel = tree.getSelection();
				sel.removeAll();
				sel.push(model);
				tree.openNodeAndParents(model);
				tree.setSelection(sel);
				this._onTreeClick();
			}, this);

			var tcm = table.getTableColumnModel();
			colWidth.each((function (w, index) {
				var resizeBehavior = tcm.getBehavior();
				resizeBehavior.setWidth(index, w);
			}).bind(this));
			table.setStatusBarVisible(false);
			tableModel.setDataAsMapArray(data, true);
			return table;
		},
		_getTreeContextMenu: function (name, level, ca) {
			var menu = new qx.ui.menu.Menu();

			var label = this.tr("team.new_team");
			if( ca ){
				var newButton = new qx.ui.menu.Button(label, "icon/16/actions/edit-undo.png");
				newButton.addListener("execute", function () {
					this.__createTeamDialog();
				}, this);
				menu.add(newButton);
			}

			if (level > 0) {
				label = this.tr("team.count_assignments");
				var newButton = new qx.ui.menu.Button(label, "icon/16/status/dialog-information.png");
				newButton.addListener("execute", function () {
					this.__createEntityDialog();
				}, this);
				menu.add(newButton);
			}


			return menu;
		},
		_onTreeContextMenu: function (e) {
			var tree = this.__getTree()
			var item = tree.getSelection().getItem(0);
			var filter = qx.util.Serializer.toJson(item);
			console.log("item:" + filter);
			var lookup = tree.getLookupTable();
			var index = lookup.indexOf(item);
			var level = tree.getLevel(index);
			var name = item.getTeamid();
			var teamid = item.getTeamid();
			this.__currentTeamid = teamid;
			this.__currentLevel = level;
			var model = this.__valueModelMap[teamid];
			this.__currentModel = model;
			console.log("_onTreeContextMenu:" + teamid + ",name:" + name + ",index:" + index + ",level:" + level);
			var ca = (this.__createAllowed(name) || this.__manageAllowed(name) || this.__user.admin || (this.__user.team_manage && level == 0));

			if (level == 4) ca = false;
			console.log("ca:" + ca + ",create:" + this.__createAllowed(name) + ",manage:" + this.__manageAllowed(name) + ",level:" + level + ",name:" + name);
			//if (ca) {
				var menu = this._getTreeContextMenu(name, level,ca);
				menu.setOpener(this);
				menu.openAtMouse(e);
			//}
		},
		_onTreeClick: function (e) {
			var tree = this.__getTree()
			var item = tree.getSelection().getItem(0);
			var lookup = tree.getLookupTable();
			var index = lookup.indexOf(item);
			var level = tree.getLevel(index);
			this.__currentLevel = level;
			var name = item.getName();
			var teamid = item.getTeamid();
			this.__currentTeamid = teamid;
			var model = this.__valueModelMap[teamid];
			this.__currentModel = model;
			console.log("_onClick:" + teamid + ",name:" + name + ",index:" + index + ",level:" + level + "," + model);

			if (level > 0) {
				this.__selected_teamid = teamid;
				var completed = function (teamdata) {
					console.log("teamid:" + teamdata.teamid);
					console.log("validFrom:" + teamdata.validFrom);
					var ma = this.__manageAllowed();
					var ra = this.__readAllowed();
					console.log("ma:" + ma);
					console.log("ra:" + ra);
					this.__clearForm();
					this.__fillForm(teamdata);
					this.__currentTeamData = teamdata;
					if (this.__manageAllowed()) {
						this.__showForm();
						this.__enableForm(true);
					} else if (this.__readAllowed()) {
						this.__showForm();
						this.__enableForm(false);
					} else {
						this.__hideForm();
						this.__enableForm(false);
					}
				}
				try {
					var team = ms123.util.Remote.rpcSync("team:getTeam", {
						namespace: this.__storeDesc.getNamespace(),
						teamid: teamid
					});
					completed.call(this, team);
				} catch (e) {
					console.log(e.stack);
					ms123.form.Dialog.alert("TeamEditor._onTreeClick:" + e);
					return;
				}
			} else {
				this.__hideForm();
				this.__select_path = null;
				this.__enableForm(false);
			}
		},
		_onOpen: function (e) {
			var item = e.getData();
			var tree = this.__getTree()
			var lookup = tree.getLookupTable();
			var index = lookup.indexOf(item);
			if (index == -1) {
				tree.openNodeAndParents(item);
				index = lookup.indexOf(item);
			}
			var sel = tree.getSelection();
			sel.splice(0, 1, item);
		},
		_dateFormat: function (date, format) {
			var o = {
				"M+": date.getMonth() + 1,
				"d+": date.getDate(),
				"h+": date.getHours(),
				"m+": date.getMinutes(),
				"s+": date.getSeconds(),
				"q+": Math.floor((date.getMonth() + 3) / 3),
				"S": date.getMilliseconds() 
			}
			if (/(y+)/.test(format)){
				 format = format.replace(RegExp.$1, (date.getFullYear() + "").substr(4 - RegExp.$1.length));
			}
			for (var k in o){
			  if (new RegExp("(" + k + ")").test(format)){
					 format = format.replace(RegExp.$1, RegExp.$1.length == 1 ? o[k] : ("00" + o[k]).substr(("" + o[k]).length));
				}
			}
			return format;
		},
		/**---------------------------------------------------------------------------
		 WIDGET API
		 ---------------------------------------------------------------------------*/
		// overridden
		_createChildControlImpl: function (id) {
			var control;

			switch (id) {
			case "tree":
				control = new qx.ui.tree.VirtualTree(null, "title", "children").set({
					focusable: false,
					hideRoot: false,
					keepFocus: true,
					openMode: "none",
					height: null,
					itemHeight: 20,
					width: null,
					maxWidth: this.getWidth(),
					maxHeight: this.getHeight(),
					selectionMode: "one",
					contentPaddingLeft: 0,
					showTopLevelOpenCloseIcons: true,
					quickSelection: false
				});
				control.setIconPath("teamid");
				control.setIconOptions({
					converter: function (value, model) {
						if (model.getChildren != null && model.getChildren().getLength() > 0) {
							return "qx/decoration/Classic/shadow/shadow-small-r.png";
						} else {
							return "qx/decoration/Classic/shadow/shadow-small-tl.png";
						}
					}
				});
				control.addListener("contextmenu", this._onTreeContextMenu, this);
				control.addListener("open", this._onOpen, this);
				control.addListener("click", this._onTreeClick, this);
				this._leftWidget._add(control, {
					edge: "center"
				});
				break;

			case "searchField":
				var searchField = new qx.ui.form.TextField().set({
					padding: 2,
					margin: 2
				});
				searchField.setLiveUpdate(true);
				searchField.setFocusable(true);
				searchField.setEnabled(true);

				var bSearch = new qx.ui.form.Button(null, "icon/16/actions/system-search.png").set({
					padding: 2,
					margin: 2
				});
				bSearch.setEnabled(false);

				searchField.addListener('keyup', (function (e) {
					var value = searchField.getValue();
					if( value.length>2){
						bSearch.setEnabled(true);
					}else{
						bSearch.setEnabled(false);
					}
				}).bind(this));

				bSearch.setFocusable(false);
				bSearch.addListener("execute", function () {
					var text = searchField.getValue();
					var len = this.__modelList.length;
					var	start = this._lastIndex!=undefined ? this._lastIndex+1:0;
					if( this._lastSearch != text){
						start = 0;
					}
					if( start >= len){
						start = 0;
					}
					for( var i=start; i< len;i++){
						var model = this.__modelList[i];
						var desc = model.getDescription();
						var name = model.getName();
						var teamid = model.getTeamid();
						if( (desc && desc.toLowerCase().indexOf(text) != -1) || (name && name.toLowerCase().indexOf(text)!=-1)){
							var tree = this.__getTree()
							var sel = tree.getSelection();
							sel.removeAll();
							sel.push(model);
							tree.openNodeAndParents(model);
							tree.setSelection(sel);
							this._onTreeClick();
							this._lastIndex=i;
							this._lastSearch = text;
							break;
						}
					}
				}, this);

				var c = new qx.ui.container.Composite();
				c.setLayout(new qx.ui.layout.Dock());
				this._leftWidget._add(c, {
					edge: "north"
				});
				control=searchField;
				c.add(searchField, {
					edge: "center"
				});
				c.add(bSearch, {
					edge: "east"
				});
				break;
			}

			return control || this.base(arguments, id);
		}
	}
});
