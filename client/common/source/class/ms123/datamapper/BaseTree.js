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
/*
*/

qx.Class.define("ms123.datamapper.BaseTree", {
	extend: qx.ui.container.Composite,
	type: "abstract",
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (modelData) {
		this.base(arguments);
		this._init(modelData);
	},

	/******************************************************************************
	 EVENTS
	 ******************************************************************************/
	events: {
		"open": "qx.event.type.Data"
	},


	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {
		tree: {
			check: "qx.ui.tree.Tree",
			nullable: true
		}
	},
	statics: {
		makeCleanName:function(name,list){
			name = name.replace(/[\-.]/g, '_');
			name = name.replace(/\W/g, '');
			if( name.match(/[a-zA-Z]/) == false) name = "_"+name;
			var counter=1;
			var tryName = name;
			while(true){
				if(list.indexOf(tryName) == -1){
					list.push(tryName);
					return tryName;
				}
				tryName = name + counter;
				counter++;
			}
			return name;//Not reached
		},
		prepareTreeData: function (model, path,nameList) {
			nameList = nameList || [];
			model.children = model.children || [];
			model.type = model.type || ms123.datamapper.Config.NODETYPE_ATTRIBUTE;
			model.icon = model.icon || "-";
			model.id = model.id || ms123.util.IdGen.id();

			model.cleanName = model.cleanName || ms123.datamapper.BaseTree.makeCleanName(model.name,nameList);
			if( model.type == ms123.datamapper.Config.NODETYPE_ATTRIBUTE){
				model.scripts = model.scripts || [];
				delete model.script;
				model.fieldFormat = model.fieldFormat || null;
			}
			model.path = path = path + ms123.datamapper.Config.PATH_DELIM + model.name;
			var nameList=[];
			for (var i = 0; model.children && i < model.children.length; i++) {
				var c = model.children[i];
				if( c.cleanName) nameList.push(c.cleanName);
			}
			for (var i = 0; model.children && i < model.children.length; i++) {
				var c = model.children[i];
				ms123.datamapper.BaseTree.prepareTreeData(c, path,nameList);
			}
		},
		getIconFromFormat: function (format) {
			if (format == ms123.datamapper.Config.FORMAT_JSON) return "resource/ms123/json-16x16.png";
			if (format == ms123.datamapper.Config.FORMAT_POJO) return "resource/ms123/bean-16x16.png";
			if (format == ms123.datamapper.Config.FORMAT_XML) return "resource/ms123/xml-16x16.png";
			if (format == ms123.datamapper.Config.FORMAT_CSV || format == ms123.datamapper.Config.FORMAT_FW) return "resource/ms123/csv-16x16.png";
			if (format == ms123.datamapper.Config.FORMAT_MAP) return "resource/ms123/map-16x16.png";
		}
	},

	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		getSide: function () {},
		_init: function (modelData) {
			var layout = new qx.ui.layout.Dock();
			this.setLayout(layout);

			this.add(this._createTree(), {
				edge: "center"
			});
			this._customize(modelData);
			this.setModelData(modelData);
		},
		_customize: function (modelData) {},
		setModelData: function (modelData) {
			if (!modelData) return;
			modelData.icon = ms123.datamapper.BaseTree.getIconFromFormat(modelData.format);
			modelData.type = modelData.type || ms123.datamapper.Config.NODETYPE_ELEMENT;
			modelData.id = modelData.id || ms123.util.IdGen.id();
			modelData.root = true;
			modelData.parentSpec = modelData.parentSpec || {};
			this._prepareModelData(modelData, "");
			var temp = qx.lang.Json.stringify(modelData, null, 2);
			//console.log("PreparedModelData:" + temp);

			var model = qx.data.marshal.Json.createModel(modelData, true);
			this._treeController.setModel(model);
			this.__model = model;
		},
		setModel: function (model) {
			this._treeController.setModel(model);
			this.__model = model;
		},
		_createTree: function () {
			var tree = new qx.ui.tree.Tree().set({
				decorator: null,
				focusable: false,
				hideRoot: false,
				//openMode:"none",
				selectionMode: "one",
				contentPaddingLeft: 0,
				quickSelection: false
			});

			tree.setRootOpenClose(true);
			this._treeController = new ms123.datamapper.TreeController(null, tree, "children", "name");
			this._treeController.setIconPath("icon");
			this._treeController.setIconOptions({
				converter: (function (value, model, source, target) {
					return this._getIcon(target, model, value);
				}).bind(this)
			});
			this.setTree(tree);
			this._setup();
			return tree;
		},
		_treeClick: function (e) {
			//console.log("treeClick:" + e.getData());
		},
		_setup: function () {
			var self = this;
			var delegate = {
				configureItem: function (item) {
					item.setIndent(13);
				},
				createItem: function () {
					var item = new ms123.datamapper.TreeItem(self.getSide());
					item.setOpen(true);

					item.addListener("open", function (e) {
						self.fireDataEvent("open", e.getData());
					}, this);
					return item;
				},
				bindItem: (function (controller, item, id) {
					controller.bindProperty("name", "title", {
						converter:function(data, model, source, target) {
							//console.log("converterName:"+data+"/"+source+"/"+target);
							target.setTitle( model.getName());
							return data;
						},
						onUpdate : function(source, target, data) {
							//console.log("onUpdataName:"+data+"/"+source+"/"+target);
						}
					}, item, id);
					controller.bindProperty(controller.getIconPath(), "icon", controller.getIconOptions(), item, id);

/*
					if (this._facade.createContextMenu) {
						var menu = this._facade.createContextMenu(item, id);
						if (menu != null) {
							item.setContextMenu(menu);
						} else {
							item.setContextMenu(null);
						}
					}*/


				}).bind(this)
			};
			this._treeController.setDelegate(delegate);
		},
		_prepareModelData: function (model, path) {
			ms123.datamapper.BaseTree.prepareTreeData(model,path);
		},
		__getItemFromModel: function (model, selectables) {
			selectables.push(model);
			var children = model.getChildren();
			for (var i = 0; i < children.getLength(); i++) {
				var c = children.getItem(i);
				this.__getItemFromModel(c, selectables);
			}
		},
		getModel: function () {
			return this._treeController.getModel();
		},
		getModelSelection: function () {
			return this.getTree().getModelSelection();
		},
		setModelSelection: function (node) {
			return this.getTree().setModelSelection([node]);
		},
		getSelection: function () {
			return this.getTree().getSelection();
		},
		setSelection: function (item) {
			return this.getTree().setSelection([item]);
		},
		getItems: function () {
			return this.getTree().getItems(true,true);
		},
		getRootModel: function () {
			return this.getTree().getRoot().getModel();
		},
		enableChangeEvents: function (enable) {
			this._treeController.enableChangeEvents(enable);
		},
		insertDataNode: function (model, data) {
			var children = model.getChildren();

			this._prepareModelData(data, model.getPath());
			//console.log("Data:" + JSON.stringify(data, null, 2));
			var m = qx.data.marshal.Json.createModel(data, true);
			children.push( m);
			return m;
		},
		insertSavedNode: function (saved) {
			var parentList = saved.parentList;
			parentList.insertAt(saved.index, saved.model);
		},
		removeNode: function (parentModel, model) {
			var p = parentModel;
			var index = p.getChildren().indexOf(model);
			p.getChildren().remove(model);
			return {
				index: index,
				parentList: p.getChildren(),
				model: model
			}
		},
		removeModel: function (model) {
			var list = this._getParentList(null, model);
			list.remove(model);
		},
		_getParentList: function (root, model) {
			if (root == null) {
				root = this.getTree().getRoot().getModel();
			}
			var children = root.getChildren();
			for (var i = 0; i < children.getLength(); i++) {
				var child = children.getItem(i);
				if (child.getId() == model.getId()) {
					return children;
				}
				var ret = this._getParentList(child, model);
				if (ret) return ret;
			}
			return null;
		},
		getParent: function (model) {
			return this._getParent(null,model);
		},
		_getParent: function (root, model) {
			if (root == null) {
				root = this.getTree().getRoot().getModel();
			}
			var children = root.getChildren();
			for (var i = 0; i < children.getLength(); i++) {
				var child = children.getItem(i);
				if (child.getId() == model.getId()) {
					return root;
				}
				var ret = this._getParent(child, model);
				if (ret) return ret;
			}
			return null;
		},

		_getIcon: function (target, model, id) {
			var type = ms123.datamapper.Config.NODETYPE_ATTRIBUTE;
			var icon = model.getIcon();
			if (icon != "-") return icon;
			if (model.getType) {
				type = model.getType();
			}
			if (type == ms123.datamapper.Config.NODETYPE_COLLECTION) {
				return "resource/ms123/elementlist-16x16.png";
			} else if (type == ms123.datamapper.Config.NODETYPE_ELEMENT) {
				return "resource/ms123/element.png";
			} else {
				return "resource/ms123/attribute.png";
			}
			return "resource/ms123/file.png";
		},
		_getIconUrl: function (name) {
			var am = qx.util.AliasManager.getInstance(name);
			return am.resolve("resource/ms123/stencilsets/" + name);
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
