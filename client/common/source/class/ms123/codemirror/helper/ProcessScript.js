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
*/

/**
 * Container for the source code editor.
 */
qx.Class.define("ms123.codemirror.helper.ProcessScript", {
	extend: qx.ui.container.Composite,


	/**
	 *****************************************************************************
	 CONSTRUCTOR
	 *****************************************************************************
	 */
	construct: function (context) {
		this.base(arguments);
		this.__storeDesc = context.facade.storeDesc;
		this.setLayout(new qx.ui.layout.Dock());
/*		var model = this.__createTreeModel();

		var tree = this.getChildControl("tree");
		tree.setMaxWidth(500);
		this.__setModel(model);*/

		context.helperTree=["sw.filter","sw.form","sw.entitytype"];
		context.createContextMenu = this._createContextMenu.bind(this);
			context.kind = "filterboth";
		var rh = new ms123.graphicaleditor.plugins.propertyedit.ResourceDetailTree(context, context.facade);
		this._rh = rh;
		rh.addListener("nodeSelected", function (e) {
			console.log("nodeSelected");
		}, this);
		
		this.add(rh, {
			edge: "center"
		});
		this._context = context;
	},

	/******************************************************************************
	 STATICS
	 ******************************************************************************/
	statics: {
	},

	events: {},


	members: {
		 _createContextMenu:function(item, id){
			var model = item.getModel();
			var type = model.getType();
			item.setUserData("model", item.getModel());
			item.setUserData("code", item.getModel().getValue());
			item.setDraggable(true);
			item.addListener("dragstart", function (e) {
				e.addAction("move");
			},this);
			if( type == "sw.filter"){
				var childs = model.getChildren();
				if(childs.getLength()==1 &&  childs.getItem(0).getValue()=="dummy"){
					this._rh._onOpenNode(model);
				}else{
					var button=null;
					var menu = new qx.ui.menu.Menu;
					button = this._createFilterButton(model,"single");  menu.add(button);
					button = this._createFilterButton(model,"list"); menu.add(button);
					return menu;
				}
			}
			if( type == "sw.entitytype"){
				var menu = new qx.ui.menu.Menu;
				var button = this._createCreateEntityButton(model);  menu.add(button);
				var button = this._createGetEntityButton(model);  menu.add(button);
				return menu;
			}
		},
		_createCreateEntityButton:function(model){
			var button = new qx.ui.menu.Button(this.tr("processscript.insert_create_object"),"icon/16/actions/insert-text.png");
			button.setUserData("model",model);
			button.addListener("execute", function (e) {
				var b = e.getTarget();
				var model = b.getUserData("model");
				var t = 'def '+model.getValue()+'Obj = createObject("'+model.getValue()+'")\n';
				var cm = this._context.cmWrapper.getEditor();
				if (cm.somethingSelected()) {
					cm.replaceSelection(t);
				} else {
					cm.replaceRange(t, cm.getCursor());
				}

			}, this);
			return button;
		},
		_createGetEntityButton:function(model){
			var button = new qx.ui.menu.Button(this.tr("processscript.insert_get_objectbyid"),"icon/16/actions/insert-text.png");
			button.setUserData("model",model);
			button.addListener("execute", function (e) {
				var b = e.getTarget();
				var model = b.getUserData("model");
				var t = 'def '+model.getValue()+'Obj = getObjectById("'+model.getValue()+'",\"__ID__\")\n';
				var cm = this._context.cmWrapper.getEditor();
				if (cm.somethingSelected()) {
					cm.replaceSelection(t);
				} else {
					cm.replaceRange(t, cm.getCursor());
				}

			}, this);
			return button;
		},
		_createFilterButton:function(model, kind){
			var config = {
				single:{
					1: "first",
					2: "single",
					3: "map"
				},
				list:{
					1: "list",
					2: "list",
					3: "list"
				}
			}
			var c = config[kind];
			var button = new qx.ui.menu.Button(this.tr("processscript.insert_filter_"+c[2]),"icon/16/actions/insert-text.png");
			button.setUserData("model",model);
			button.addListener("execute", function (e) {
				var b = e.getTarget();
				var model = b.getUserData("model");
				var komma="";
				var params = model.getChildren().getItem(0).getChildren();
				var t = 'def '+c[3]+' = executeNamedFilter("'+model.getValue()+'",[';
				for(var i=0;i< params.getLength();i++){
					var pname = params.getItem(i).getValue();
					t+=komma+pname+':"__VALUE__"';
					komma=", ";
				}
				t+="])."+c[1]+"()\n";
				var cm = this._context.cmWrapper.getEditor();
				if (cm.somethingSelected()) {
					cm.replaceSelection(t);
				} else {
					cm.replaceRange(t, cm.getCursor());
				}

			}, this);
			return button;
		},
		/** ---------------------------------------------------------------------------
		 WIDGET API
		 ---------------------------------------------------------------------------*/
		// overridden
		_createChildControlImpl: function (id, hash) {
			var control;

			switch (id) {
			case "tree":
				control = new qx.ui.tree.VirtualTree(null, "title", "children").set({
					decorator: null,
					contentPadding: 0,
					focusable: false,
					hideRoot: false,
					keepFocus: true,
					openMode: "tap",
					height: null,
					itemHeight: 20,
					//width: null,
					width: this.getWidth() + 70,
					//maxHeight: this.getMaxListHeight(),
					selectionMode: "one",
					contentPaddingLeft: 0,
					showTopLevelOpenCloseIcons: true,
					quickSelection: false
				});
				control.setIconPath("title");

				control.setIconOptions({
					converter: function (value, model) {
						if (model.getChildren != null && model.getChildren().getLength() > 0) {
							return "qx/decoration/Classic/shadow/shadow-small-r.png";
						} else {
							return "qx/decoration/Classic/shadow/shadow-small-tl.png";
						}
					}
				});
				this.__changeListenerDisabled = false;
				//control.addListener("mousedown", this._onTreeMouseDown, this);
				//control.addListener("open", this._onOpen, this);
				this.add(control, {
					edge: "center"
				});
				break;
			}
			return control || this.base(arguments, id);
		},

		__setModel: function (model) {
			var tree = this.getChildControl("tree");
			tree.setDelegate(this);
			//tree.getSelection().removeListener("change", this._onTreeChangeSelection, this);
			tree.setModel(model);
			//tree.getSelection().addListener("change", this._onTreeChangeSelection, this);
		},

 		bindItem: function (controller, item, id) {
      controller.bindDefaultProperties(item, id);
			var model = item.getModel();
			console.log("bindItem:"+item+"/"+id+"/"+model+"/type:"+model.getType());
			if( model.getType() == ms123.config.ConfigManager.CS_ENTITY){
				var cm  = this.__createModuleContextMenu(model);
				item.setContextMenu(cm);
				item.setUserData("model", model);
				item.setDraggable(true);
				item.addListener("dragstart", function (e) {
					e.addAction("move");
				},this);
			}
			if( model.getType() == "field"){
				item.setUserData("model", model);
				item.setDraggable(true);
				item.addListener("dragstart", function (e) {
					e.addAction("move");
				},this);
			}
    },

		configureItem: function (item) {
			item.setIndent(13);
			item.setPadding(0);
		},

		__createTreeModel: function () {
			var moduleContextMenuDesc = [
				{
					label:"Filter",
					icon:"icon/16/actions/edit-paste.png",
					code:"var list  = executeFilter(\"${id}\",\"id==1\");"
				},
				{
					label:"Create object",
					icon:"icon/16/actions/edit-paste.png",
					code:"var obj  = createObject(\"${id}\");"
				}
			];

			var modarray = [];

			var cm = new ms123.config.ConfigManager();
			var modules = cm.getEntities(this.__storeDesc);
			for (var i = 0; i < modules.length; i++) {
				var modname = modules[i].name;
				var m = {}
				m.id = modname;
				m.title = modname;
				m.type = ms123.config.ConfigManager.CS_ENTITY;
				m.children = this.__getFieldChildrens(modname);
				m.contextMenu = moduleContextMenuDesc;
				modarray.push(m);
			}

			var model = {}
			model.id = "ROOT";
			model.title = "Moduls";
			model.type = "root";
			model.contextMenu = null;
			model.children = modarray;
			return qx.data.marshal.Json.createModel(model);
		},

		__createModuleContextMenu: function (model) {
			var menu = new qx.ui.menu.Menu;

			var contextMenu = model.getContextMenu();
			if( contextMenu != null){
				for(var i=0; i < contextMenu.getLength();i++){
					var desc = contextMenu.getItem(i);
					var button = new qx.ui.menu.Button(desc.getLabel(), desc.getIcon(), null);
					
					button.setUserData("code", desc.getCode());
					button.setUserData("model", model);
					button.setDraggable(true);
					button.addListener("dragstart", function (e) {
						e.addAction("move");
						menu.hide();
					},this);
					menu.add(button);
				}
			}
			return menu;
		},
		__getFieldChildrens: function (module,view) {
			var fields = this.__getFields(module,"main-grid");
			var fieldarray = [];
			for (var i = 0; i < fields.length; i++) {
				var fieldname = fields[i].name;
				if( fieldname.match("^_") ) continue;
				var m = {}
				m.id = fieldname;
				m.title = fieldname;
				m.type = "field";
				m.children = [];
				m.contextMenu = null;
				fieldarray.push(m);
			}
			return fieldarray;
		},
		__getFields: function (entity,view) {
			var filter = null;
			if( view == "export" || view == "main-grid"){
				filter = "filter=!datatype.startsWith('array/team')";
			}
			if( view == "search" ){
				filter = "filter=!datatype.startsWith('related')";
			}

			var cm = new ms123.config.ConfigManager();
			return cm.getFields(this.__storeDesc,entity, true, false,filter,null);
		}
	},



	/**
	 *****************************************************************************
	 DESTRUCTOR
	 *****************************************************************************
	 */

	destruct: function () {}
});
