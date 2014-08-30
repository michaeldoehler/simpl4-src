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
	@asset(qx/icon/${qx.icontheme}/16/status/*)
	@asset(qx/icon/${qx.icontheme}/16/devices/drive-harddisk.png)
*/


/**
 * A form widget which allows a multiple selection. 
 *
 */
qx.Class.define("ms123.shell.Navigator", {
	extend: qx.ui.core.Widget,
	include: qx.locale.MTranslation,


	/**
	 *****************************************************************************
	 CONSTRUCTOR
	 *****************************************************************************
	 */
	construct: function (facade) {
		this.base(arguments);
		this.facade = facade;
		this._setLayout(new qx.ui.layout.Dock());

		this._navTree = this._createNavTree();
		this._add(this._navTree, {
			edge: "center"
		});
		this._pluginManager = this.facade.pluginManager;
		this.contextMenu = this._pluginManager.getContextMenuActions();
		this._setup();
	},


	/**
	 *****************************************************************************
	 PROPERTIES
	 *****************************************************************************
	 */

	events: {
		"changeSelection": "qx.event.type.Data"
	},


	properties: {
		// overridden
	},


	/**
	 *****************************************************************************
	 MEMBERS
	 *****************************************************************************
	 */


	members: {
		_createNavTree: function () {
			var control = new qx.ui.tree.VirtualTree(null, "title", "children").set({
				focusable: false,
				hideRoot: this.facade.hideRoot,
				keepFocus: true,
				openMode: "none",
				height: null,
				itemHeight: 20,
				selectionMode: "one",
				contentPaddingLeft: 0,
				showTopLevelOpenCloseIcons: true,
				quickSelection: false
			});
			control.setIconPath("value");
			control.setIconOptions({
				converter: (function (value, model, source, target) {
					var menu = this._createContextMenu(target, model, value);
					if (menu != null) {
						target.setContextMenu(menu);
					}
					var type = model.getType();
					var icon = this._pluginManager.getNodeTypeIcon(type);
					if (icon && icon.match("^sw.")) type = icon;
					if (type == ms123.shell.Config.DIRECTORY_FT) {
						var isOpen = target.isOpen();
						if (isOpen) {
							return "resource/ms123/directory_open.png";
						} else {
							return "resource/ms123/directory.png";
						}
					} else if (type == ms123.shell.Config.PROJECT_FT) {
						var isOpen = target.isOpen();
						if (isOpen) {
							return "resource/ms123/project.png";
						} else {
							return "resource/ms123/project_closed.png";
						}

					} else {
						return this._pluginManager.getNodeTypeIcon(type);
					}


				}).bind(this)
			});
			control.addListener("click", this._onClickTree, this);
			control.addListener("open", this._onOpenNode, this);
			return control;
		},

		_setup: function () {
			var delegate = {
				configureItem: function (item) {
					item.setIndent(13);
				},
				createItem: function () {
					return new ms123.shell.TreeItem();
				},
				bindItem: (function (controller, item, id) {
					//controller.bindDefaultProperties(item, id);
					controller.bindProperty("", "model", null, item, id);
					controller.bindProperty(controller.getLabelPath(), "label", controller.getLabelOptions(), item, id);
					controller.bindProperty(controller.getIconPath(), "icon", controller.getIconOptions(), item, id);
				}).bind(this)
			};

			this._navTree.setDelegate(delegate);
			this._navTree.getSelection().removeListener("change", this.__onNavTreeChangeSelection, this);
			var t = null;
			try {
				t = ms123.util.Remote.rpcSync("git:getWorkingTree", {
					name: this.facade.storeDesc.getRepository(),
					includePathList:this.facade.includePathList,
					excludePathList:this.facade.excludePathList,
					mapping: {
						path: "path",
						value: "name",
						id: "name",
						title: "name",
						type: "type"
					}
				});
//				var value = qx.lang.Json.stringify(t, null, 4); console.log("tree:" + value);

			} catch (e) {
				ms123.form.Dialog.alert("Navigator._setup:" + e.message);
				return;
			}
			this._prepareTree(t, 0);
			var model = qx.data.marshal.Json.createModel(t, true);
			model.getChildren().sort( this._sortFirstLevel );
			this._navTree.setModel(model);
			this.setParentModel(model);
			this.facade.treeModel = model;
			this._navTree.getSelection().addListener("change", this.__onNavTreeChangeSelection, this);
		},
		_prepareTree: function (model, level) {
			if (!model.children) {
				model.children = [];
			}
			this._pluginManager.prepareNode(model,level);
			if (!model.type) {
				model.type = ms123.shell.Config.DIRECTORY_FT;
			}
			for (var i = 0; model.children && i < model.children.length; i++) {
				var c = model.children[i];
				this._prepareTree(c, level + 1);
			}
		},

		setParentModel: function (model) {
			var children = model.getChildren();
			if (children.getLength() > 0) {
				for (var i = 0; i < children.getLength(); i++) {
					var c = children.getItem(i);
					c.parent = model;
					this.setParentModel(c);
				}
			}
			return null;
		},
		
		_sortFirstLevel:function (a, b) {
			a = a.getTitle();
			b = b.getTitle();
			if (a < b) return -1;
			if (a > b) return 1;
			return 0;
		},
		__onNavTreeChangeSelection: function (e) {
			var tree = this._navTree;
			if (!tree) return;
			var selection = tree.getSelection();
			if (!selection || selection.getLength() == 0) return;
			var item = tree.getSelection().getItem(0);
		},

		_onOpenNode: function (e) {
			this._pluginManager.onOpenNode(e);
		},
		_createContextMenu: function (item, model, id) {
			var menu = new qx.ui.menu.Menu;

			var type = model.getType();
			var name = model.getTitle();

			for (var i = 0; i < this.contextMenu.length; i++) {
				var cme = this.contextMenu[i];
				if (!cme.clazz) {
					menu.add(new qx.ui.menu.Separator());
					continue;
				}
				if (cme.nodetypes && cme.nodetypes.indexOf(type) < 0) {
					continue;
				}
				var button = new qx.ui.menu.Button(cme.title, cme.menuicon);
				button.setUserData("entry", cme);
				button.setUserData("item", item);
				button.setUserData("id", id);
				button.addListener("execute", function (e) {
					var b = e.getTarget();
					console.log("b:" + b);
					var cme = b.getUserData("entry");
					var item = b.getUserData("item");
					var id = b.getUserData("id");
					var model = item.getModel();
					var tabicon = cme.tabicon || this._pluginManager.getNodeTypeIcon(type);
					this.facade.raiseEvent({
						clazz: cme.clazz,
						param: cme.param,
						model: model,
						title: cme.title,
						tabtitle: cme.tabtitle,
						kind: cme.kind,
						facade: this.facade,
						icon: tabicon,
						type: ms123.shell.Config.EVENT_ITEM_SELECTED
					});
				}, this);
				menu.add(button);
			}
			return menu;
		},
		_onClickTree: function (e) {
			console.log("e:" + e._target);
			if (!(e._target instanceof ms123.shell.TreeItem)) {
				return;
			}
			if( !e.isLeftPressed()) return;
			var model = this._navTree.getSelection().getItem(0);
			var type = model.getType();
			console.log("_onClickTree:"+qx.util.Serializer.toJson(model)+"/type:"+type);
			var onClickDef = this._pluginManager.getOnClickActions();
			for (var i = 0; i < onClickDef.length; i++) {
				var cme = onClickDef[i];
				if (cme.nodetypes && cme.nodetypes.indexOf(type) < 0) {
					continue;
				}
				var tabicon = cme.tabicon || this._pluginManager.getNodeTypeIcon(type);
				this.facade.raiseEvent({
					clazz: cme.clazz,
					param: cme.param,
					title: cme.title,
					icon: tabicon,
					kind: cme.kind,
					model: model,
					facade: this.facade,
					type: ms123.shell.Config.EVENT_ITEM_SELECTED
				});
				break;
			};
		},
		_onDblClickTree: function (e) {}
	}
});
