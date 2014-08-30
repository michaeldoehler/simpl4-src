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
qx.Class.define("ms123.importing.ModelMapping", {
	extend: qx.ui.core.Widget,
	include: qx.locale.MTranslation,


	/**
	 *****************************************************************************
	 CONSTRUCTOR
	 *****************************************************************************
	 */
	construct: function (context) {
		this.base(arguments);
		this._setLayout(new qx.ui.layout.HBox());

		this.__storeDesc = context.storeDesc;
		var sp = this._doLayoutVertical();
		this._add(sp, {
			flex: 1
		});
		var spc = sp.getChildren();
		this._topWidget = spc[0];
		this._bottomWidget = spc[1];

		sp = this._doLayoutHorizontal();
		this._topWidget._add(sp, {
			flex: 1
		});

		spc = sp.getChildren();
		this._sourceTreeWidget = spc[0];
		this._targetTreeWidget = spc[1];
		this._sourceTree = this._createSourceTree();
		this._targetTree = this._createTargetTree();


		this._mappingTable = this._createMappingTable();
		this._bottomWidget._add(this._mappingTable, {
			edge: "center"
		});
		this._mappingTableToolbar = this._createMappingTableToolbar();
		this._bottomWidget._add(this._mappingTableToolbar, {
			edge: "south"
		});
	},


	/**
	 *****************************************************************************
	 PROPERTIES
	 *****************************************************************************
	 */

	events: { /** Fires after the selection was modified */
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
		_createSourceTree: function () {
			var control = new qx.ui.tree.VirtualTree(null, "title", "children").set({
				focusable: false,
				hideRoot: false,
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
				converter: function (value, model) {
					if (model.getChildren != null && model.getChildren().getLength() > 0) {
						return "qx/decoration/Classic/shadow/shadow-small-r.png";
					} else {
						return "qx/decoration/Classic/shadow/shadow-small-tl.png";
					}
				}
			});
			this._sourceTreeWidget._add(control, {
				flex: 1
			});
			control.addListener("open", this._onOpenI, this);
			return control;
		},
		_createTargetTree: function () {
			var control = new qx.ui.tree.VirtualTree(null, "title", "children").set({
				focusable: false,
				hideRoot: false,
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
				converter: function (value, model) {
					if (model.getChildren != null && model.getChildren().getLength() > 0) {
						return "qx/decoration/Classic/shadow/shadow-small-r.png";
					} else {
						return "qx/decoration/Classic/shadow/shadow-small-tl.png";
					}
				}
			});
			this._targetTreeWidget.add(control, {
				edge: "center"
			});
			control.addListener("open", this._onOpenM, this);
			control.addListener("dblclick", this._onDblClickTargetTree, this);
			control.addListener("click", this._onClickTargetTree, this);
			return control;
		},

		_findNodeByValue: function (startNode, value) {
			if (startNode.getValue() == value) {
				return startNode;
			}
			var children = startNode.get("children");
			if (children == null) {
				return null;
			}
			for (var i = 0; i < children.getLength(); i++) {
				var child = children.getItem(i);
				var result = this._findNodeByValue(child, value);
				if (result) {
					return result;
				}
			}
			return null;
		},
		_getSourceNodeByPath: function (path) {
			path = path.replace(/@/g, "");
			//console.log("_getSourceNodeByPath:" + path);
			var patharray = path.split("/");
			return this._getNodeByPath(this._sourceTree.getModel(), patharray);
		},
		_getTargetNodeByPath: function (path) {
			//console.log("_getTargetNodeByPath:" + path);
			var patharray = path.split("\.");
			return this._getNodeByPath(this._targetTree.getModel(), patharray);
		},
		_getNodeByPath: function (node, patharray) {
			//console.log("_getNodeByPath:" + patharray);
			for (var i = 0; i < patharray.length; i++) {
				var value = patharray[i];
				node = this._findNodeByValue(node, value);
				console.log("_getNodeByPath.node:" + node);
				if (node == null) return null;
			}
			return node;
		},

		_clearMapping:function(){
			this._removeAllMappings();
		},
		_autoMapping:function(){
			this._removeAllMappings();
			this.__autoMapping(this._mainEntity);
		},
		__autoMapping:function(path){
			var children = this._sourceTree.getModel().getChildren();
			for (var i = 0; i < children.getLength(); i++) {
				var sourceNode = children.getItem(i);
				var targetNode = this._getTargetNodeByPath( path + "."+sourceNode.getValue());
	
				if( targetNode ){
					var targetType = targetNode.getDatatype();
					var targetPath = this._getPathTarget(targetNode);

					var sourcePath = this._getPathSource(sourceNode);
					//console.log(sourcePath +" ==> "+ targetPath);

					var mmap = {};
					mmap["source"] = sourcePath.join("/");
					mmap["target"] = targetPath.join(".");
					mmap["targetType"] = targetType;
					this._addMapping(mmap);
				}
			}
		},	
		_createPreviewWindow: function (root, name, text) {
			var win = new qx.ui.window.Window(name, "").set({
				resizable: true,
				useMoveFrame: true,
				useResizeFrame: true
			});
			win.setLayout(new qx.ui.layout.Dock());
			win.setWidth(650);
			win.setHeight(590);
			win.setAllowMaximize(false);
			win.open();
			root.add(win, {left:50,top:50});
			win.setModal(true);
			var value = qx.lang.Json.stringify(text, null, 4); 
			var x = new qx.ui.form.TextArea(value);
			x.setReadOnly(true);
			win.add(x, { edge:"center" });
			return win;
		},
		_showPreview: function (mwidget, sf) {
			var result = this._importDialog._doPreviewOrImport(true, false,true, 1 );
			var app = qx.core.Init.getApplication();
			this._createPreviewWindow(app.getRoot(), "Preview",result);
		},

		setValue: function (value) {
			this._mappingTableModel.setDataAsMapArray(value, true);
		},
		getValue: function () {
			var arr = this._mappingTableModel.getDataAsMapArray();
			return arr;
		},

		prepareForSave:function(){
			this._mappingTable.stopEditing();
		},
		setup: function (importDialog, sourceModel, mainEntity, filetype,user) {
			this._importDialog = importDialog;
			this._user = user;
			this._fileType = filetype;
			this._mainEntity = mainEntity;
			sourceModel= qx.data.marshal.Json.createModel(sourceModel);
			this._setParentAndLeaf(sourceModel, null);
			var sourceDelegate = {
				configureItem: function (item) {
					item.setIndent(13);
				},
				createItem: function () {
					return new ms123.importing.TreeItem();
				},
				bindItem: function (controller, item, id) {
					controller.bindDefaultProperties(item, id);
					controller.bindProperty("type", "leadIcon", {
						converter: function (data) {
							if( data == "attr" ){
								return "resource/ms123/attribute_obj.gif";
							}else if( data == "leaf_element"){
								return "resource/ms123/leaf_element_obj.gif";
							}else if( data == "element"){
								return "resource/ms123/element_obj.gif";
							}	
						}
					}, item, id);

				}
			};

			this._sourceTree.setDelegate(sourceDelegate);
			this._sourceTree.getSelection().removeListener("change", this.__onSourceTreeChangeSelection, this);
			this._sourceTree.setModel(sourceModel);
			this._sourceTree.getSelection().addListener("change", this.__onSourceTreeChangeSelection, this);


			var maxlevel = 3;
			if( filetype == "csv") maxlevel = 1;
			var cm = new ms123.config.ConfigManager();
			var moduleTree = cm.getEntityTree(this.__storeDesc,mainEntity,maxlevel,true);
			this._setFieldsForModules(moduleTree, 0);

			var targetDelegate = {
				configureItem: function (item) {
					item.setIndent(13);
				},
				createItem: function () {
					return new ms123.importing.TreeItem();
				},
				bindItem: function (controller, item, id) {
					controller.bindDefaultProperties(item, id);
					controller.bindProperty("datatype", "leadIcon", {
						converter: function (data) {
							if (data == "list") {
								return "resource/ms123/javalistmodel.gif";
							} else if (data == "object") {
								return "resource/ms123/obj.gif";
							} else if (data == "date") {
								return "resource/ms123/d16.png";
							} else if (data == "text") {
								return "resource/ms123/string.gif";
							} else if (data == "string") {
								return "resource/ms123/string.gif";
							} else if (data == "number") {
								return "resource/ms123/integer.gif";
							} else if (data == ms123.config.ConfigManager.CS_ENTITY) {
								return "resource/ms123/m16.png";
							} else {
								return "resource/ms123/u16.png";
							}
						}
					}, item, id);

				}
			};

			this._targetTree.setDelegate(targetDelegate);
			this._targetTree.getSelection().removeListener("change", this.__onTargetTreeChangeSelection, this);
			var targetModel = qx.data.marshal.Json.createModel(moduleTree);
			this._setParent(targetModel, null);
			this._targetTree.setModel(targetModel);
			this._targetTree.getSelection().addListener("change", this.__onTargetTreeChangeSelection, this);

//			var m = qx.util.Serializer.toJson(targetModel); console.log("targetModel:"+m);
		},

		_setFieldsForModules: function (model, level) {
			if (!model.children) {
				model.children = [];
			}
			if (!model.value) {
				model.value = model.id;
			}
			if (!model.datatype) {
				model.datatype = "object";
			}
			if(model.datatype == "list" || model.datatype=="object"){
				model.title = this.tr(model.title);
			}
			for (var i = 0; model.children && i < model.children.length; i++) {
				var c = model.children[i];
				this._setFieldsForModules(c, level + 1);
			}
			if(model.datatype == "list" ){
				return;
			}
			var cm = new ms123.config.ConfigManager();
			var fields = cm.getEntityPermittedFields(this.__storeDesc,model.id);
			fields = this._mapToList(fields);
			this._sortFieldList(fields);
			for (var f = 0; f < fields.length; f++) {
				var name = fields[f].name;
				var datatype = fields[f].datatype;
				var readonly = fields[f].readonly;
				if (!name || name.match("^_")) continue;
				if (!datatype) continue;
				if (datatype=="set" || datatype=="list" || datatype.match(/^related/)) continue;
				if (readonly) continue;
				var o = {};
				o.name = name;
				o.title = this.tr("data."+model.id+"."+name)+"("+name+")";
				o.value = name;
				o.datatype = datatype;
				o.children = [];
				model.children.splice(0, 0, o);
			}
		},
		_getTabs: function (level) {
			var tabs = "";
			for (var i = 0; i < level; i++) {
				tabs += "\t";
			}
			return tabs;
		},
		_mapToList: function (map) {
			var ret = [];
			for( var key in map ){
				ret.push( map[key] );
			}
			return ret;
		},
		_sortFieldList: function (list) {
			list.sort(function (a, b) {
				if( !a || !a.name || !b || !b.name){
						console.log("a.name:"+a.name+"/"+b.name);
					 return null;
				}
				a = a.name.toLowerCase();
				b = b.name.toLowerCase();
				if (a < b) return 1;
				if (a > b) return -1;
				return 0;
			});
		},
		_doLayoutVertical: function () {
			var splitpane = new qx.ui.splitpane.Pane("vertical");
			var topWidget = new qx.ui.core.Widget();
			topWidget._setLayout(new qx.ui.layout.HBox());
			topWidget.setDecorator(null);
			topWidget.setMinWidth(100);
			splitpane.add(topWidget, 5);

			var bottomWidget = new qx.ui.container.Composite();
			bottomWidget.setLayout(new qx.ui.layout.Dock());
			bottomWidget.setDecorator(null);
			bottomWidget.setMinWidth(100);
			splitpane.add(bottomWidget, 3);
			return splitpane;
		},
		_doLayoutHorizontal: function () {
			var splitpane = new qx.ui.splitpane.Pane("horizontal");
			var leftWidget = new qx.ui.core.Widget();
			leftWidget._setLayout(new qx.ui.layout.HBox());
			leftWidget.setDecorator(null);
			leftWidget.setMinWidth(100);
			splitpane.add(leftWidget, 3);

			var rightWidget = new qx.ui.container.Composite();
			rightWidget.setLayout(new qx.ui.layout.Dock());
			rightWidget.setDecorator(null);
			rightWidget.setMinWidth(100);
			splitpane.add(rightWidget, 3);
			return splitpane;
		},
		__onSourceTreeChangeSelection: function (e) {
			console.log("__onChangeSelection");
			var tree = this._sourceTree;
			if (!tree) return;
			var selection = tree.getSelection();
			if (!selection || selection.getLength() == 0) return;
			var item = tree.getSelection().getItem(0);

			console.log("__onChangeSelection:" + item.getValue());
		},
		__onTargetTreeChangeSelection: function (e) {
			console.log("__onChangeSelection");
			var tree = this._targetTree;
			if (!tree) return;
			var selection = tree.getSelection();
			if (!selection || selection.getLength() == 0) return;
			var item = tree.getSelection().getItem(0);

			console.log("__onTargetTreeChangeSelection:" + item.getValue());
		},
		_onClickTargetTree: function (e) {
			console.log("nothing");
		//	this._addOrSelectMapping("select");
		},
		_onDblClickTargetTree: function (e) {
			this._addOrSelectMapping("add");
		},
		_addOrSelectMapping:function(what){
			var tree = this._targetTree;
			var item = tree.getSelection().getItem(0);
			var targetType = item.getDatatype();
			var targetPath = this._getPathTarget(item);
			console.log("targetpath:" + targetPath);

			var sourceTree = this._sourceTree;
			item = sourceTree.getSelection().getItem(0);
			var sourcePath = this._getPathSource(item);
			console.log("sourcepath:" + sourcePath);

			var mmap = {};
			mmap["source"] = sourcePath.join("/");
			mmap["target"] = targetPath.join(".");
			mmap["targetType"] = targetType;
			var rc = this._mappingExists(mmap);
			if (rc>=0) {
				console.log("Exists:"+rc);
				if( what == "add"){
					this._deleteRecord(rc);
					this._insertRecord(mmap,rc);
					var selModel = this._mappingTable.getSelectionModel();
					selModel.setSelectionInterval(rc,rc);
				}
			} else {
				if( what == "add"){
					this._addMapping(mmap);
				}
				if( what == "select"){
					this._selectTarget(mmap);
				}
			}
		},
		_getPathTarget: function (item) {
			var path = new qx.data.Array();
			while (item) {
				path.push(item.getValue());
				item = item.parent;
			}
			path.reverse();
			return path;
		},
		_getPathSource: function (item) {
			var path = new qx.data.Array();
			while (item) {
				var type = item.getType();
				if (type == "attr") {
					path.push("@" + item.getValue());
				} else {
					path.push(item.getValue());
				}
				item = item.parent;
			}
			path.reverse();
			return path;
		},
		_setParent: function (node, parent) {
			var children = node.getChildren();
			node.parent = parent;
			if (children.getLength() > 0) {
				for (var i = 0; i < children.getLength(); i++) {
					var c = children.getItem(i);
					this._setParent(c, node);
				}
			}
		},
		_setParentAndLeaf: function (node, parent) {
			var children = node.getChildren();
			node.parent = parent;
			
			//var attrOnly = true;
			if (children.getLength() > 0) {
				for (var i = 0; i < children.getLength(); i++) {
					var c = children.getItem(i);
					//if( c.getType() != "attr") attrOnly=false;
					this._setParentAndLeaf(c, node);
				}
			}
			//if( attrOnly && node.getType() != "attr"){
			//	node.setType( "leaf_element");
			//}
		},

		_onOpenI: function (e) {
			var item = e.getData();
			var tree = this._sourceTree;
			var lookup = tree.getLookupTable();
			var index = lookup.indexOf(item);
			if (index == -1) {
				tree.openNodeAndParents(item);
				index = lookup.indexOf(item);
			}
			var sel = tree.getSelection();
			sel.splice(0, 1, item);
		},
		_onOpenM: function (e) {
			var item = e.getData();
			var tree = this._targetTree;
			var lookup = tree.getLookupTable();
			var index = lookup.indexOf(item);
			if (index == -1) {
				tree.openNodeAndParents(item);
				index = lookup.indexOf(item);
			}
			var sel = tree.getSelection();
			sel.splice(0, 1, item);
		},

		///Mapping Table
		_createMappingTable: function () {
			var colIds = new Array();
			var colHds = new Array();
			colIds.push("source");
			colHds.push(this.tr("import.mapping.sourcepath"));

		//	colIds.push("decoder");
		//	colHds.push(this.tr("import.mapping.decoder"));

			colIds.push("target");
			colHds.push(this.tr("import.mapping.targetpath"));

			this._mappingTableModel = new qx.ui.table.model.Simple();
			this._mappingTableModel.setColumns(colHds, colIds);
			var customMap = {
				tableColumnModel: function (obj) {
					return new qx.ui.table.columnmodel.Resize(obj);
				}
			};
			var table = new qx.ui.table.Table(this._mappingTableModel, customMap);
			var tcm = table.getTableColumnModel();
			table.getTableModel().setColumnEditable(0, false);
			table.getTableModel().setColumnEditable(1, false);
			table.getTableModel().setColumnEditable(2, false);
			table.setStatusBarVisible(false);
			this._mappingTable = table;

			var selModel = this._mappingTable.getSelectionModel();
			selModel.addListener("changeSelection", function (e) {
				var index = selModel.getLeadSelectionIndex();
				var count = selModel.getSelectedCount();
				if (count == 0 || index < 0) {
					return;
				}
				var map = this._mappingTableModel.getRowDataAsMap(index);
console.log("changeSelection:"+map.source+"|"+map.target);
				var sourceItem = this._getSourceNodeByPath(map.source);
				var sel = new qx.data.Array();
				sel.push(sourceItem);
				this._sourceTree.setSelection(sel);
				this._sourceTree.openNode(sourceItem);

				var targetItem = this._getTargetNodeByPath(map.target);
				sel = new qx.data.Array();
				sel.push(targetItem);
				this._targetTree.setSelection(sel);
				this._targetTree.openNode(targetItem);
			}, this);



			return table;
		},
		_createMappingTableToolbar: function () {
			var tb = new qx.ui.toolbar.ToolBar().set({});
			tb.setSpacing(5);
			this._addButtons(tb);
			return tb;
		},

		_addButtons: function (toolbar) {
			var _this = this;
			var buttons = [{
				'label': "",
				'icon': "icon/16/places/user-trash.png",
				'callback': function (m) {
          _this._clearMapping();
				},
				'value': "del"
			},
			{
				'label': "",
				'icon': "icon/16/actions/go-up.png",
				'callback': function (m) {
					_this._currentRecordUp();
				},
				'value': "up"
			},
			{
				'label': "",
				'icon': "icon/16/actions/go-down.png",
				'callback': function (m) {
					_this._currentRecordDown();
				},
				'value': "down"
			}
			];
			var delCurrentButton =  {
        'label': "",
        'icon': "icon/16/actions/list-remove.png",
        'callback': function (m) {
					_this._deleteCurrentRecord();
        },
        'value': "auto"
      };
			
			var autoButton =  {
        'label': this.tr("import.mapping.automatic"),
        'icon': "",
        'callback': function (m) {
          _this._autoMapping();
        },
        'value': "auto"
      };
			var previewButton =  {
        'label': this.tr("import.mapping.preview"),
        'icon': "icon/16/actions/edit-select-all.png",
        'callback': function (m) {
          _this._showPreview();
        },
        'value': "load"
      };

			buttons.push(delCurrentButton);
			buttons.push(autoButton);
			buttons.push(previewButton);

			for (var i = 0; i < buttons.length; i++) {
				var bd = buttons[i];
				var b = new qx.ui.toolbar.Button(bd.label, bd.icon);
				if (bd.tooltip) {
					b.setToolTipText(bd.tooltip);
				}
				b.addListener("execute", bd.callback, this);
				toolbar.add(b);
			}
		},
		_currentRecordDown: function () {
			this._mappingTable.stopEditing();
			var rc = this._mappingTableModel.getRowCount();
			var selModel = this._mappingTable.getSelectionModel();
			var index = selModel.getLeadSelectionIndex();
			if (index >= (rc - 1)) return;
			var curRecord = this._getRecord(index);
			this._deleteRecord(index);
			this._insertRecord(curRecord, index + 1);
			selModel.setSelectionInterval(index + 1, index + 1);
			//this._mappingTable.scrollCellVisible(0,index+1);
			this._mappingTable.setFocusedCell(0,index+1,true);
		},
		_currentRecordUp: function () {
			this._mappingTable.stopEditing();
			var selModel = this._mappingTable.getSelectionModel();
			var index = selModel.getLeadSelectionIndex();
			if (index == 0) return;
			var curRecord = this._getRecord(index);
			this._deleteRecord(index);
			this._insertRecord(curRecord, index - 1);
			selModel.setSelectionInterval(index - 1, index - 1);
			//this._mappingTable.scrollCellVisible(0,index-1);
			this._mappingTable.setFocusedCell(0,index-1,true);
		},
		_insertRecord: function (map, pos) {
			this._mappingTableModel.addRowsAsMapArray([map], pos, true);
		},
		_deleteRecord: function (row) {
			this._mappingTable.stopEditing();
			this._mappingTableModel.removeRows(row, 1);
		},
		_getRecord: function (row) {
			var map = this._mappingTableModel.getRowDataAsMap(row);
			return map;
		},
		_deleteCurrentRecord: function () {
			this._mappingTable.stopEditing();
			var selModel = this._mappingTable.getSelectionModel();
			var index = selModel.getLeadSelectionIndex();

			if (index > -1) {
				this._mappingTableModel.removeRows(index, 1);
			}
		},
		_removeAllMappings: function () {
			this._mappingTableModel.removeRows(0,this._mappingTableModel.getRowCount());
		},
		_addMapping: function (map) {
			this._mappingTableModel.addRowsAsMapArray([map], null, true);
		},
		_selectTarget: function (mmap) {
			var arr = this._mappingTableModel.getDataAsMapArray();
			for (var i = 0; i < arr.length; i++) {
				var tmap = arr[i];
				if (tmap.target == mmap.target) {
					var selModel = this._mappingTable.getSelectionModel();
					selModel.setSelectionInterval( i,i );
			//		this._mappingTable.scrollCellVisible(0,i);
					this._mappingTable.setFocusedCell(0,i,true);
				}
			}
		},
		_mappingExists: function (mmap) {
			var arr = this._mappingTableModel.getDataAsMapArray();
			for (var i = 0; i < arr.length; i++) {
				var tmap = arr[i];
				if (/*tmap.source == mmap.source &&*/ tmap.target == mmap.target) {
					var selModel = this._mappingTable.getSelectionModel();
					selModel.setSelectionInterval( i,i );
					this._mappingTable.setFocusedCell(0,i,true);
					return i;
				}
			}
			return -1;
		}
	}
});
