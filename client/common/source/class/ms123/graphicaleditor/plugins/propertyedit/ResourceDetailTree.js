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

qx.Class.define("ms123.graphicaleditor.plugins.propertyedit.ResourceDetailTree", {
	extend: qx.ui.container.Composite,
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (config, facade) {
		this.base(arguments);
		this.config = config || {};

		this.__storeDesc = facade.storeDesc;
		this.facade = facade;
		var layout = new qx.ui.layout.Dock();
		this.setLayout(layout);


		var includeList = ["sw.directory"];
		var excludeList = ["messages"];
		if( config.helperTree ){
			includeList = includeList.concat(config.helperTree);
		}
		if( !config.helperTree || config.helperTree.indexOf("sw.entitytype") == -1){
			excludeList.push("data_description");
		}
		if( !config.helperTree || config.helperTree.indexOf("sw.enum") == -1){
			excludeList.push("enumerations");
		}
		this._resourceTypes = includeList.slice(1);
		this.add(this._createResourceTree(includeList, excludeList), {
			edge: "center"
		});
		this.add(this._createPathTextfield(), {
			edge: "south"
		});
	},

	/******************************************************************************
	 EVENTS
	 ******************************************************************************/
	events: {
		"nodeSelected": "qx.event.type.Data"
	},


	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},
	statics: {
     formContainer: ["xform", "tabview", "page", "actionbutton", "label", "group"]
  },

	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		_createResourceTree: function (includeList,excludeList) {
			this._tree = this._createTree();
			var resdata = this._getResource(includeList,excludeList);
			this._setup(resdata);
			this._tree.setDraggable(true);
      this._tree.addListener("dragstart", this._handleDragStart, this);
			return this._tree;
		},
		_getResource: function (includeList, excludeList) {
			var t = ms123.util.Remote.rpcSync("git:getWorkingTree", {
				name: this.__storeDesc.getNamespace(),
				includeTypeList: includeList,
				excludePathList: excludeList,
				mapping: {
					path: "path",
					value: "name",
					title: "name",
					type: "type"
				}
			});
			return t;
		},
		_createTree: function () {
			var control = new qx.ui.tree.VirtualTree(null, "title", "children").set({
				focusable: false,
				hideRoot: true,
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
					return this._getIcon(target, model, value);
				}).bind(this)
			});
			control.addListener("open", this._onOpenNode, this);
			control.addListener("click", this._onClickTree, this);
			return control;
		},
		_setup: function (treeData) {
			var delegate = {
				configureItem: function (item) {
					item.setIndent(13);
				},
				createItem: function () {
					return new ms123.shell.TreeItem();
				},
				bindItem: (function (controller, item, id) {
					controller.bindProperty("", "model", null, item, id);
					controller.bindProperty("title", "label", null, item, id);
					controller.bindProperty(controller.getIconPath(), "icon", controller.getIconOptions(), item, id);

					if( this.config.createContextMenu ){
						var menu = this.config.createContextMenu(item,id);
						if (menu != null) {
							item.setContextMenu(menu);
						}else{
							item.setContextMenu(null);
						}
					}


				}).bind(this)
			};

			this._tree.setDelegate(delegate);
			this._setDummyInsteedFields(treeData);

			//var xxx = qx.lang.Json.stringify(treeData,null,2);console.log("xxx:"+xxx);
			var model = qx.data.marshal.Json.createModel(treeData, true);
			this._tree.setModel(model);
			this.__model = model;
		},
		_getSelectables: function () {
			var selectables = [];
			if( !this.__model ) return selectables;
			var childs = this.__model.getChildren();
			for (var i = 0; i < childs.getLength(); i++) {
				this.__getItemFromModel(this.__model.getChildren().getItem(i), selectables);
			}
			return selectables;
		},
		__getItemFromModel: function (model, selectables) {
			selectables.push(model);
			var children = model.getChildren();
			for (var i = 0; i < children.getLength(); i++) {
				var c = children.getItem(i);
				this.__getItemFromModel(c, selectables);
			}
		},
		selectNode: function( path ){
			if( path.indexOf(".") != -1){
				path = path.split(".")[0];
			}
			var selectables = this._getSelectables();
			for (var i = 0; i < selectables.length; i++) {
				var p = selectables[i].getValue();
				console.log("p:"+p+"|"+path);
				if( p == path ){
					this._tree.openNodeAndParents(selectables[i]);
					console.log("\tsel:"+selectables[i]);
					var sel = this._tree.getSelection();
					sel.splice(0, 1, selectables[i]);
					break;
				}
			}
		},
		_onClickTree: function (e) {
			var model = this._tree.getSelection().getItem(0);
			var childs = model.getChildren();
			if (childs.getLength() == 1 && childs.getItem(0).getValue() == "dummy") {
				this._onOpenNode(model);
			}

			var type = model.getType();
			var output = model.getValue();
			if( model.getParent ){
					var parent = model.getParent();
				output = parent +"."+ output;
	
			}
			console.log("_onClickTree:" + output+"/"+type+"/"+model);
			var data = {model:model,type:type};
			this.fireDataEvent("nodeSelected", data, null);
			this._pathTextfield.setValue( output );
		},

		_onOpenNode: function (e) {
			var item = e.getData ? e.getData() : e;
			var childs = item.getChildren();
			if (childs.getLength() == 1 && childs.getItem(0).getValue() == "dummy") {
				var fieldList = [];
				var fieldArray = null;
				if( item.getType() == "sw.form"){
					var formDesc = null;
					try {
						var resource = childs.getItem(0).getResource();
						var type = childs.getItem(0).getType();
						formDesc = ms123.util.Remote.rpcSync("git:searchContent", {
							reponame: this.__storeDesc.getNamespace(),
							name: resource,
							type: type
						});
						if( !formDesc || formDesc == "" ){
								console.log("_onOpenNode:no content");
								childs.removeAll();
							 return;
						}
						formDesc = formDesc.evalJSON();
					} catch (e) {
						ms123.form.Dialog.alert("ResourceDetailTree._onOpenNode:" + e);
						childs.removeAll();
						return;
					}
					this._getFormFields(formDesc, fieldList);
			/*	}else if( item.getType() == "sw.document"){
					var formDesc = null;
					try {
						var resource = childs.getItem(0).getResource();
						var type = childs.getItem(0).getType();
						formDesc = ms123.util.Remote.rpcSync("git:searchContent", {
							reponame: this.__storeDesc.getNamespace(),
							name: resource,
							type: type
						});
						formDesc = formDesc.evalJSON();
					} catch (e) {
						ms123.form.Dialog.alert("ResourceDetailTree._onOpenNode:" + e);
						return;
					}
					this._getFormFields(formDesc, fieldList);*/
				}else if( item.getType() == "sw.entitytype"){
					var fields = null;
					try {
						var entity = item.getValue();
						var data = ms123.util.Remote.rpcSync("entity:getEntitytype", {
							storeId: this.__storeDesc.getStoreId(),
							name: entity
						});
						fieldList = this._toArray(data["fields"]);
					} catch (e) {
						ms123.form.Dialog.alert("ResourceDetailTree._onOpenNode:" + e);
						childs.removeAll();
						return;
					}
				}else if( item.getType() == "sw.enum"){
					var enumeration = null;
					try {
						var resource = childs.getItem(0).getResource();
						var type = childs.getItem(0).getType();
						enumeration = ms123.util.Remote.rpcSync("git:searchContent", {
							reponame: this.__storeDesc.getNamespace(),
							name: resource,
							type: type
						});
						enumeration = enumeration.evalJSON();
					} catch (e) {
						ms123.form.Dialog.alert("ResourceDetailTree._onOpenNode:" + e);
						childs.removeAll();
						return;
					}
					this._getEnumFields(enumeration, fieldList);
				}else if( item.getType() == "sw.filter"){
					var filter = null;
					try {
						var resource = childs.getItem(0).getResource();
						var type = childs.getItem(0).getType();
						filter = ms123.util.Remote.rpcSync("git:searchContent", {
							reponame: this.__storeDesc.getNamespace(),
							name: resource,
							type: type
						});
						filter = filter.evalJSON();
					} catch (e) {
						ms123.form.Dialog.alert("ResourceDetailTree._onOpenNode:" + e);
						childs.removeAll();
						return;
					}
					if( this.config.kind == "filterparam"){
						this._getFilterParams(filter.filter, fieldList);
					}else if( this.config.kind == "filterboth"){
						var pl=[];
						var fl=[];
						var dirp = this._createDirectory("Parameter",item.getValue());
						var dirf = this._createDirectory("Fields",item.getValue());
						this._getFilterParams(filter.filter, pl);
						this._getFilterFields(filter, fl);
						dirp.children = this._createChilds(pl,"parameter");
						dirf.children = this._createChilds(fl,"fields");
						fieldArray=[];
						fieldArray.push(dirp);
						fieldArray.push(dirf);
					}else{
						this._getFilterFields(filter, fieldList);
					}
				}
				if( fieldArray == null){
					fieldArray = this._createChilds(fieldList, item.getValue());
				}
				var model = qx.data.marshal.Json.createModel(fieldArray, true);
				childs.removeAll();
				childs.append(model);
			}
		},
		_createChilds:function(fieldList, _parent){
			var fieldarray = [];
			for (var i = 0; i < fieldList.length; i++) {
				var fname = fieldList[i].name;
				var id = fieldList[i].id;
				var f = {}
				f.type = "sw.field";
				f.value = fname;
				f.parent = _parent;
				f.id = id || fname;
				f.title = fname;
				if( fieldList[i].columns){
					f.children = this._getFieldColums(f.parent,fname,fieldList[i]);
				}else{
					f.children = [];
				}
				fieldarray.push(f);
			}
			return fieldarray;
		},
		_createDirectory:function(name,_parent){
			var f = {}
			f.type = "sw.directory";
			f.value = name.toLowerCase();
			f.parent = _parent;
			f.id = name.toLowerCase();
			f.title = this.tr(name)+"";
			return f;
		},
		_getFieldColums:function(_parent,fname,field){
			var colarray = [];
			var colList = field.columns.items;
			for (var i = 0; i < colList.length; i++) {
				var cname = colList[i].colname;
				var ctitle = colList[i].display;
				var type = colList[i].type;
				if( type == "search") continue;
				var c = {}
				c.type = "sw.fieldcolumn";
				c.value = cname;
				c.parent = _parent+"."+fname;
				c.id = cname;
				c.title = cname;
				c.display = ctitle||cname;
				c.children = [];
				colarray.push(c);
			}
			return colarray;
		},
		_getFormFields: function (formDesc, fieldList) {
			var stencilId = formDesc.stencil.id.toLowerCase();
			var properties = formDesc.properties;
			console.log("stencilId:" + stencilId + "/" + qx.util.Serializer.toJson(properties));
			var isContainer = ms123.graphicaleditor.plugins.propertyedit.ResourceDetailTree.formContainer.indexOf(stencilId) != -1;
			if (!isContainer) {
				var model = {};
				model.value = properties.xf_id;
				model.name = properties.xf_id;
				model.columns = properties.xf_columns;
				model.id = stencilId;
				fieldList.push(model);
			}
			var childs = formDesc.childShapes;
			childs.each((function (child) {
				this._getFormFields(child, fieldList);
			}).bind(this));
		},
		_getEnumFields: function (enumeration, fieldList) {
			for( var i=0; i < enumeration.fieldList.length;i++){
				var fieldName = enumeration.fieldList[i].fieldname;
				var model = {};
				model.value = fieldName;
				model.name = fieldName;
				model.id = fieldName;
				fieldList.push(model);
			}
		},
		_getFilterFields: function (filter, fieldList) {
			for( var i=0; i < filter.fields.length;i++){
				var fieldName = filter.fields[i].id;
				var path = filter.fields[i].path;
				if( path.indexOf("$") != -1) continue;
				var model = {};
				model.value = fieldName;
				model.name = fieldName;
				model.id = fieldName;
				fieldList.push(model);
			}
		},
		_getFilterParams: function (filter,fieldList) {
			var label = filter.label;
			if (filter.connector == null && label != null) {
				//label = label.toLowerCase();
				if (label.match(/^[a-zA-Z].*/)) {
					var model = {};
					model.value = label;
					model.name = label;
					model.id = label;
					fieldList.push(model);
				}
			}
			var children = filter.children;
			for (var i = 0; children && i < children.length; i++) {
				var c =  children[i];
				this._getFilterParams(c,fieldList);
			}
		},
		_setDummyInsteedFields: function (model) {
			if (!model.children) {
				model.children = [];
			}
			for (var i = 0; model.children && i < model.children.length; i++) {
				var c = model.children[i];
				this._setDummyInsteedFields(c);
			}
			if (model.type==null || this._resourceTypes.indexOf(model.type) == -1) {
				return;
			}

			var o = {};
			o.resource = model.value;
			o.value = "dummy";
			o.type = model.type;
			o.children = [];
			model.children.push(o);
		},
		_createPathTextfield: function(){
			var container = new qx.ui.container.Composite();
			var layout = new qx.ui.layout.HBox();
			container.setLayout(layout);

			var l1 = new qx.ui.basic.Label().set({ value: this.tr("Variablenname") });
			container.add(l1, { flex: 1 });

			var tf1 = new qx.ui.form.TextField();
			tf1.setFocusable(true);
			tf1.setReadOnly(true);
			tf1.setEnabled(true);
			container.add(tf1, { flex: 1 });
			this._pathTextfield = tf1;
			tf1.setDraggable(true);
      tf1.addListener("dragstart", this._handleDragStart, this);
			return container;
		},

  	_handleDragStart: function(e) {
      e.addAction("move");
    },

		_getIcon: function (target, model, id) {
			var icon = ms123.shell.FileType.getIcon(target,model);
			if( icon.indexOf("file.png") == -1) return icon;
			var type = model.getType();
			if (type == "sw.fieldcolumn") {
				return this._getIconUrl("forms/new_input.png");
			}else if (type == "sw.directory") {
				return "resource/ms123/directory.png";
			}else if (type == "sw.field") {
				var id = model.getId ? model.getId().toLowerCase() : "";
				if( id == "enumselect" ){
					return this._getIconUrl("forms/new_select1.png");
				}
				if( id == "gridinput" ){
					return this._getIconUrl("forms/new_tableedit.png");
				}
				if( id == "moduleselector" ){
					return this._getIconUrl("forms/entity_select.png");
				}
				if( id == "tableselect" ){
					return this._getIconUrl("forms/new_select.png");
				}
				if( id == "textarea" ){
					return this._getIconUrl("forms/new_textarea.png");
				}
				return this._getIconUrl("forms/new_input.png");
			} else if (type == ms123.shell.Config.ENUM_FT) {
				return "resource/ms123/enum.png";
			}
			return "resource/ms123/file.png";
		},
		_getIconUrl: function (name) {
			var am = qx.util.AliasManager.getInstance(name);
			return am.resolve("resource/ms123/stencilsets/" + name);
		},
		_toArray: function (map) {
			var arr = [];
			if (!map) return arr;
			for (var i in map) {
				if (map.hasOwnProperty(i)) {
					arr.push(map[i]);
				}
			}
			return arr;
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
