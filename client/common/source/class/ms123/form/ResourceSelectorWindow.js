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
	@asset(qx/icon/${qx.icontheme}/16/actions/*)
	@asset(qx/icon/${qx.icontheme}/16/apps/*)
	@asset(qx/icon/${qx.icontheme}/48/actions/*)
	@asset(qx/icon/${qx.icontheme}/48/apps/*)
	@ignore($)
*/
qx.Class.define("ms123.form.ResourceSelectorWindow", {
	extend: qx.core.Object,
	include: [qx.locale.MTranslation],


	/**
	 *****************************************************************************
	 CONSTRUCTOR
	 *****************************************************************************
	 */

	construct: function (context, curValue) {
		this.base(arguments);

		if (curValue) {
			var s = curValue.split(",");
			if (s.length == 2) {
				curValue = s[1];
			}
		}else{
			curValue = "";
		}
		var resourceTypes = context.resourceType;
		if( !Array.isArray(context.resourceType) ){
			resourceTypes = [context.resourceType];
		}

		this._config = context.config;
		this.__storeDesc = context.storeDesc;
		this._selected_callback = context.selected_callback;
		var title = context.title;
		var app = qx.core.Init.getApplication();

		var includeTypeList = ["sw.directory"];
		var excludePathList = ["messages","stencilsets","process-explorer"];
		var includePathList = null;
		var path = null;
		includeTypeList= includeTypeList.concat(resourceTypes);
		if( resourceTypes.indexOf(  "sw.entitytype") != -1){
			excludePathList = null;
			includePathList = null;
			path = "data_description/data/entitytypes";
		}else if( resourceTypes.indexOf( "sw.enum") != -1){
			excludePathList = null;
			includePathList = null;
			path = "enumerations/data";
		}else{
			excludePathList.push("enumerations");
			excludePathList.push("data_description");
		}
		var resdata = this._getResources(path,includeTypeList,excludePathList,includePathList);
		var win = this._createWindow(title);
		var centerContent = new qx.ui.container.Composite();
		centerContent.setLayout(new qx.ui.layout.Dock());

		var buttons = this._createButtons(win);
		this._tree = this._createTree();
		centerContent.add(this._tree, {
			edge: "center"
		});
		var textField = this._createVarnameTextfield();
		if( context.showTextField !== false ){
			centerContent.add(textField, {
				edge: "south"
			});
		}

		win.add(centerContent, {
			edge: "center"
		});

		this._setup(resdata);

		win.add(buttons, {
			edge: "south"
		});
		if( curValue && curValue.indexOf(",")!=-1){
			curValue="";
		}
		console.log("curValue2:"+curValue+"/"+curValue.length);
		this._selectNode( curValue);
		this._selectedTextField.setValue( curValue );
		this._onTreeChangeSelection();
		app.getRoot().add(win);
		win.open();
	},

	/**
	 *****************************************************************************
	 PROPERTIES
	 *****************************************************************************
	 */
	properties: {},

	/**
	 *****************************************************************************
	 EVENTS
	 *****************************************************************************
	 */

	events: {
		/** Whenever the value is changed this event is fired
		 *
		 *  Event data: The new text value of the field.
		 */
		"changeValue": "qx.event.type.Data"
	},


	/**
	 *****************************************************************************
	 MEMBERS
	 *****************************************************************************
	 */

	members: {
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
		_selectNode: function( path ){
			var selectables = this._getSelectables();
			for (var i = 0; i < selectables.length; i++) {
				var p = selectables[i].getValue();
				console.log("p:"+p+"|"+path);
				if( p == path ){
					this._tree.openNodeAndParents(selectables[i]);
					console.log("\tsel:"+selectables[i]);
					var sel = this._tree.getSelection();
					sel.splice(0, 1, selectables[i]);
				}
			}
		},
		_createButtons: function (win, table) {
			var toolbar = new qx.ui.toolbar.ToolBar();
			toolbar.setSpacing(5);
			toolbar.addSpacer();
			toolbar.addSpacer();

			this._buttonSelect = new qx.ui.toolbar.Button(this.tr("Ok"), "icon/16/actions/dialog-ok.png");
			this._buttonSelect.addListener("execute", function () {
				var sel = this._tree.getSelection().getItem(0);
				var map = {};
				map.value = sel.getValue();	
				//var varname = this._selectedTextField.getValue();
				//if( varname && varname.length>0){
				//	map.relpath+= ","+varname;
				//}
				console.error("ResourceSelectorWindow.value:"+qx.util.Serializer.toJson(map));
				if (this._selected_callback) {
					this._selected_callback(map);
				}
				win.close();
			}, this);
			toolbar._add(this._buttonSelect)
			this._buttonSelect.setEnabled(false);

			var buttonCancel = new qx.ui.toolbar.Button(this.tr("Cancel"), "icon/16/actions/dialog-close.png");
			buttonCancel.addListener("execute", function () {
				win.close();
			}, this);
			toolbar._add(buttonCancel)
			return toolbar;
		},

		_onTreeChangeSelection: function (e) {
			var tree = this._tree;
			if (!tree) return;
			var selection = tree.getSelection();
			if (!selection || selection.getLength() == 0) return;
			var item = tree.getSelection().getItem(0);
			var type = item.getType();
			if( type != null){
				this._selectedTextField.setValue( item.getValue());	
				this._buttonSelect.setEnabled(true);
				//this._selectedTextField.setReadOnly(false);
				//this._selectedTextField.setEnabled(true);
			}else{
				this._buttonSelect.setEnabled(false);
				this._selectedTextField.setValue( null );	
				//this._selectedTextField.setReadOnly(true);
				//this._selectedTextField.setEnabled(false);
			}
		},
		_createVarnameTextfield: function(){
			var container = new qx.ui.container.Composite();
			var layout = new qx.ui.layout.HBox();
			container.setLayout(layout);

			var l1 = new qx.ui.basic.Label().set({ value: this.tr("Variablename") });
			container.add(l1, { flex: 1 });

			var tf1 = new qx.ui.form.TextField();
			tf1.setFocusable(true);
			tf1.setReadOnly(true);
			tf1.setEnabled(false);
			container.add(tf1, { flex: 1 });
			this._selectedTextField = tf1;
			return container;
		},
		_getResources: function (path,includeTypeList, excludePathList,includePathList) {
			var t = ms123.util.Remote.rpcSync("git:getWorkingTree", {
				name: this._getNamespace(),
				path:path,
				includeTypeList: includeTypeList,
				excludePathList: excludePathList,
				includePathList: includePathList,
				mapping: {
					path: "path",
					value: "name",
					title: "name",
					type: "type"
				}
			});
			if( includeTypeList && includeTypeList.indexOf(ms123.shell.Config.CAMEL_FT) > -1){
				var procedureShapes = this._getCamelProcedureShapes();
				this._appendCamelRoutes(t, procedureShapes);
				this._removeUnused(t);
			}
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
			control.setIconPath("path");
			control.setIconOptions({
				converter: (function (value, model, source, target) {
					return ms123.shell.FileType.getIcon(target, model, value);
				}).bind(this)
			});
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
				}).bind(this)
			};

			this._tree.setDelegate(delegate);
  		//var value = qx.lang.Json.stringify(treeData, null, 4); console.log("tree:" + value);

			var model = qx.data.marshal.Json.createModel(treeData, true);
			this._tree.setModel(model);
			this._tree.getSelection().addListener("change", this._onTreeChangeSelection, this);
			this.__model = model;
		},

		_getCamelProcedureShapes:function(){
			var procedureShapes;
			try {
				procedureShapes = ms123.util.Remote.rpcSync("camel:getProcedureShapesForPrefix", {
					prefix: this._getNamespace() + "/"
				});
			} catch (e) {
				ms123.form.Dialog.alert("ResourceSelectorWindow._getCamelProcedureShapes:" + e);
				return null;
			}
			return procedureShapes;
		},
		_appendCamelRoutes: function (model,procedureShapes) {
			if (model.type == ms123.shell.Config.CAMEL_FT) {
				this._getRouteChildren( model, procedureShapes);
			}
			for (var i = 0; model.children && i < model.children.length; i++) {
				var c = model.children[i];
				this._appendCamelRoutes(c,procedureShapes);
			}
		},
		_removeUnused: function (model) {
			model.children = model.children.filter(function(child) {
				return  child.type != ms123.shell.Config.CAMEL_FT || child.children.length>0;
			});
			for (var i = 0; model.children && i < model.children.length; i++) {
				var c = model.children[i];
				this._removeUnused(c);
			}
		},
		_getRouteChildren:function(model, procedureShapes){
			model.children=[];
			var val = model.value;
			for( var i=0; i< procedureShapes.length; i++){
				var shape = procedureShapes[i];
				if( qx.lang.String.startsWith(shape.properties.overrideid,val)){
					var node ={};
					node.id = node.name = node.value = node.title = shape.properties.urivalue_name;
					node.path = model.path + "/"+ node.name;
					node.type = "sw.route";
					node.children=[];
					model.children.push(node);
				}
			}
		},
		_getNamespace:function(){
			if(this._config.namespace && this._config.namespace!='-'){
				return this._config.namespace;
			}
			return this.__storeDesc.getNamespace();
		},
		_createWindow: function (name) {
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
			win.addListener("close", function () {
				win.destroy();
			}, this);
			return win;
		}
	}
});
