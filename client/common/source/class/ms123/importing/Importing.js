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
	@asset(qx/icon/${qx.icontheme}/16/mimetypes/*)

	@asset(ms123/icons/*)
	@asset(ms123/*)
*/

qx.Class.define("ms123.importing.Importing", {
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
		_init: function (context) {
			//this._user = ms123.util.Remote.rpcSync("auth:getUserProperties");
			this._user = ms123.config.ConfigManager.getUserProperties();
			this.__storeDesc = context.storeDesc;
			this.__prefix = context.prefix||"alluser";
			this.__mainEntity = context.mainEntity;
			this.__fileType = context.fileType;
			this.__allUser = this.__prefix==="alluser";

			var modulesMenu = [];
			var o = {};
			var cm = new ms123.config.ConfigManager();

			var cm = new ms123.config.ConfigManager();
			var _modules = cm.getEntities(context.storeDesc);
			for (var i = 0; i < _modules.length; i++) {
				var module = _modules[i];
				var modname = module.name;
				o = {};
				o.label = this.tr("data." + modname);
				o.value = modname;
				modulesMenu.push(o);
			}

			var rightWidget = new qx.ui.container.Composite();
			rightWidget.setLayout(new qx.ui.layout.Dock());

			this._window = context.window;
			var sp = this._doLayout(context.window, rightWidget);
			context.window.add(sp, {});
			var childs = sp.getChildren();

			this._leftTree = childs[0];
			this._rightWidget = childs[1];


			this._enableRightSide(false);

			var tcontext = {};
			tcontext.treeModel = this.__createTreeModel();
			tcontext.user = this._user;

			tcontext.createMenu = function (item, level, id, treeModul) {
				var menu = new qx.ui.menu.Menu();
				if (level == "0") {
					var cmd = new qx.ui.core.Command();

					var text = self.tr("importing.newImport");
					var ctext = self.tr("importing.importName");
					var addButton = new qx.ui.menu.Button(text, null, cmd);
					addButton.addListener("execute", function () {
						var formData = {
							"importName": {
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
								'label': self.tr("importing.mainEntity"),
								'value': 1,
								'options': modulesMenu
							},
							"fileType": {
								'type': "SelectBox",
								'label': self.tr("importing.fileType"),
								'value': "csv",
								'options': [{
									label: "CSV",
									value: "csv"
								},
								{
									label: "XML",
									value: "xml"
								}/*,
								{
									label: "JSON",
									value: "json"
								},
								{
									label: "EDIFACT",
									value: "edi"
								}*/ ]
							}
						};
						if( self.__allUser===false || ms123.config.ConfigManager.isDatamapperImport()){
							delete formData.mainEntity;
							delete formData.fileType;
						}
						
						var form = new ms123.form.Form({
							"formData": formData,
							"allowCancel": true,
							"inWindow": true,
							"callback": function (m) {
								if (m !== undefined) {
									var val = m.get("importName");
									var mm=self.__mainEntity;
									var ft=self.__fileType;
									if( self.__allUser){
										try{
											mm = m.get("mainEntity");
											ft = m.get("fileType");
										}catch(e){}
									}
									if (self.__checkdup(id, val)) { 
										ms123.form.Dialog.alert(self.tr("importing.duplicated"));
										return;
									} else {
										var meta = {};
										if( ! ms123.config.ConfigManager.isDatamapperImport()){
											meta = { mainEntity: mm, fileType: ft }
										}
										if (self.__createImporting(val, meta)) {
											this._createTreeNode(id, val, meta);
											return;
										}
									}
								}
							},
							"context": this
						});
						form.show();


					}, treeModul);
					menu.add(addButton);
				}


				if (level == "0") {
					item.setIcon("icon/16/mimetypes/office-contact.png");
					item.setIconOpened("icon/16/mimetypes/office-contact.png");
					item.setOpen(true);
				}
				if (level == "1") {
					item.setIcon("icon/16/actions/bookmark-new.png");
					item.setIconOpened("icon/16/actions/bookmark-new.png");
				}
				if (level == "1") {
					var deltext = self.tr("importing.deleteImport");
					var cmd = new qx.ui.core.Command();
					var delButton = new qx.ui.menu.Button(deltext, null, cmd);
					delButton.addListener("execute", function () {
						ms123.form.Dialog.confirm(self.tr("composite.select_dialog.confirm_delete"), function (e) {
							if (e) {
								var children = id.parent.getChildren();
								for (var i = 0; i < children.getLength(); i++) {
									var xid = children.getItem(i);
									if (xid == id) {
										children.removeAt(i);
										break;
									}
								}
								if (self.__deleteImporting(id.getId())) {
									ms123.form.Dialog.alert(self.tr("importing.importingDeleted"));
									self._enableRightSide(false);
								};
							}
						}, this);
					}, treeModul);

					menu.add(delButton);
				}
				return menu;
			}


			var self = this;
			tcontext.clickListener = function (e) {
				if (e.isLeftPressed()) {
					self._currentTreeModel = this.getSelection()[0].getModel();
					var level = this.getSelection()[0].getModel().getLevel();
					if (level == "0") {
						//Root clicked
						self._enableRightSide(false);
						return;
					}
					if (level == "1") {
						var model = this.getSelection()[0].getModel();
						var mainEntity = null;
						var fileType = null;
						if( model.getMainEntity){
							mainEntity = model.getMainEntity();
							fileType = model.getFileType();
						}
						var id = model.getId();

						self._window.getApplicationRoot().setGlobalCursor("wait");

						qx.event.Timer.once(function () {
							self._enableRightSide(true);
							var context = {};
							context.user = self._user;
							context.storeDesc = self.__storeDesc;
							context.prefix = self.__prefix;
							context.mainEntity = mainEntity;
							context.fileType = fileType;
							context.id = id;
							context.parentWidget = self._rightWidget;
							console.log("step1");

							if( ms123.config.ConfigManager.isDatamapperImport()){
								new ms123.importing.ImportDatamapperDialog(context);
							}else{
								var importDialog = new ms123.importing.ImportDialog(context);
							}
							//importDialog.setup(id, mainEntity, fileType);
							self._window.getApplicationRoot().setGlobalCursor("default");
						}, this, 200);
					}
				}
			}

			var tree = new ms123.widgets.Tree(tcontext);
			this._leftTree.setLayout(new qx.ui.layout.Grow());
			this._leftTree.add(tree, {});
			this._tree = tree;
		},
		__createTreeModel: function () {
			var t = {};
			try {
				t.children = ms123.util.Remote.rpcSync("importing:getImportings", {
					prefix: this.__prefix,
					namespace: this.__storeDesc.getNamespace()
				});
			} catch (e) {
				ms123.form.Dialog.alert("Importing.__createTreeModel:" + e);
				return;
			}
			t.title = this.tr("Importing.title")+"";
			t.id = "ROOT";
			//var value = qx.lang.Json.stringify(t, null, 4); console.log("tree1:" + value);
			this.__completeTree(t, 0);

			var value = qx.lang.Json.stringify(t, null, 4); console.log("tree2:" + value);
			var model = qx.data.marshal.Json.createModel(t);
			return model;
		},
		__completeTree: function (model, level) {
			model.level = level;
			if (!model.children) {
				model.children = [];
			}
			if (!model.id && model.importingid) {
				model.id = model.importingid;
			}
			model.id = this.__removePrefix(model.id);
			if (!model.title) {
				model.title = model.id;
			}
			if (model.settings) {
				model.fileType = model.settings.fileType;
				model.mainEntity = model.settings.mainEntity;
				delete model.settings;
			}
			if( !this.__allUser ){
				model.fileType = this.__fileType;
				model.mainEntity = this.__mainEntity;
			}
			if( model.fileType===undefined)delete model.fileType;
			if( model.mainEntity===undefined)delete model.mainEntity;
			for (var i = 0; model.children && i < model.children.length; i++) {
				var c = model.children[i];
				this.__completeTree(c, level + 1);
			}
		},
		__createImporting: function (name, data) {
			console.log("create.name:" + name);
			try {
				var ret = ms123.util.Remote.rpcSync("importing:createImporting", {
					namespace: this.__storeDesc.getNamespace(),
					settings: data,
					importingid: this.__prefix + "/" + name
				});
				if (ret && ret.constraintViolations) {
					var msg = ret.constraintViolations[0].message;
					ms123.form.Dialog.alert("Importing.__createImporting:Name " + msg);
					return false;
				}
			} catch (e) {
				ms123.form.Dialog.alert("Importing.__createImporting:" + e);
				return false;
			}
			ms123.form.Dialog.alert(this.tr("importing.importingCreated"));
			return true;
		},
		__deleteImporting: function (name) {
			console.log("delete.name:" + name);
			try {
				ms123.util.Remote.rpcSync("importing:deleteImporting", {
					namespace: this.__storeDesc.getNamespace(),
					importingid: this.__prefix + "/" + name
				});
			} catch (e) {
				ms123.form.Dialog.alert("Importing.__deleteImporting:" + e);
				return false;
			}
			return true;
		},
		__removePrefix: function (id) {
			var index = id.indexOf(this.__prefix);
			if (index != 0) return id;
			return id.substring(this.__prefix.length + 1);
		},
		__checkdup: function (model, val) {
			console.log("model:" + qx.util.Serializer.toJson(model));
			var children = model.getChildren();
			if (children.getLength() > 0) {
				for (var i = 0; i < children.getLength(); i++) {
					var c = children.getItem(i);
					var id = c.getId();
					if (id == val) return true;
				}
			}
			return false;
		},
		_enableRightSide: function (enable) {
			if (enable) {
				this._rightWidget.setEnabled(true);
			} else {
				this._rightWidget.setEnabled(false);
			}
		},
		_doLayout: function (parent, rightWidget) {
			var splitpane = new qx.ui.splitpane.Pane("horizontal");
			//parent.addListener("resize", function () {
			//	splitpane.setHeight(parent.getHeight());
			//}, this);
			//parent.addListener("maximize", function () {
			//	splitpane.setHeight(parent.getHeight());
			//}, this);
			//splitpane.setHeight(parent.getHeight());
			var leftWidget = new qx.ui.container.Composite();
			leftWidget.setDecorator(null);
			leftWidget.setMinHeight(150);
			splitpane.add(leftWidget, 2);
			splitpane.add(rightWidget, 8);

			return splitpane;
		},
		_getModelPath: function (model) {
			var path = [];
			path.push(model.getId());
			while (model.parent) {
				model = model.parent;
				path.push(model.getId());
			}
			return path.reverse();
		}
	}
});
