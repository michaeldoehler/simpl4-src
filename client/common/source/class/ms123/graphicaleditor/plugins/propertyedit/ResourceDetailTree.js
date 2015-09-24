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
				name: this._getNamespace(),
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
							reponame: this._getNamespace(),
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
							reponame: this._getNamespace(),
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
							reponame: this._getNamespace(),
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
				}else if( item.getType() == "sw.camel"){
					var procedureShapes = null;
					try {
						var resource = childs.getItem(0).getResource();
						var type = childs.getItem(0).getType();
						procedureShapes = ms123.util.Remote.rpcSync("camel:getProcedureShapesForPrefix", {
							prefix: this._getNamespace() + "/" + item.getValue()
						});
					} catch (e) {
						ms123.form.Dialog.alert("ResourceDetailTree._onOpenNode:" + e);
						childs.removeAll();
						return;
					}
					fieldArray= this._getCamelFields(procedureShapes,item);
				}
				if( fieldArray == null){
					fieldArray = this._createChilds(fieldList, item.getValue());
				}
				var model = qx.data.marshal.Json.createModel(fieldArray, true);
				childs.removeAll();
				childs.append(model);
			}
		},
		_createChilds:function(fieldList, _parent, type){
			var fieldarray = [];
			for (var i = 0; i < fieldList.length; i++) {
				var fname = fieldList[i].name;
				var id = fieldList[i].id || fieldList[i].name;
				var f = {}
				if( type ){
					f.type = type + "_" + fieldList[i].type;
				}else{
					f.type = "sw.field";
				}
				f.value = fname;
				if( _parent){
					f.parent = _parent;
				}
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
		_getCamelFields: function (procedureShapes,item) {
			var fieldArray=[];
			if( procedureShapes.length==0){
				return null;
			}
			for( var i=0; i < procedureShapes.length;i++){
				var procedureShape = procedureShapes[i];
				var stencilId = procedureShape.stencil.id.toLowerCase();
				var properties = procedureShape.properties;
				var rpcParameter = properties.rpcParameter ? properties.rpcParameter.items : [];
				var dir = this._createDirectory(properties.urivalue_name,item.getValue());
				dir.children = this._createChilds(rpcParameter,null,"camelparam");
				fieldArray.push(dir);
			}
			console.log("Push:", JSON.stringify(fieldArray,null,2));
			return fieldArray;
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
			}else if (type.startsWith("camelparam")) {
				var x = type.split("_");
				return this.getDataTypeIcon('_'+x[1]);
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
		getDataTypeIcon:function(type){
			var map = {
				_string: 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAMAAAAoLQ9TAAAAtFBMVEUAAAD29/z4+fz19/z09vvx9Pvo7vrr8Pru8vrx9Pr5+vzj6/nt8vvw9Pvh6vnj6/jo7/rq8Prt8vrz9vvg6vni6/nh6vjj7Pnl7fno7/nr8frw9Pr3+fzg6vjj7Pjn7/rr8fnx9fopSGuQrs6iw+drgZnt8/r1+Pt9dU57dE+FeUyCd02kiUGfhkOahEWVgUeQfkiLe0upi0DgyI+0kD2xjj6ujT+qikDUsmjUsmnavXzVsml/JLp3AAAAAXRSTlMAQObYZgAAAAFiS0dEAIgFHUgAAAAJcEhZcwAACxMAAAsTAQCanBgAAAAHdElNRQffCRgGMxEtVyArAAAAlUlEQVQY023PxxaCMBBGYewFLJEgscQycQTpqKDi+7+XyqBuuMtvMf8ZTavpWvWDbFj2yL6QExTPvILbuqkA4FLczwRBowV4QHCcgCBsm4Dmp5Ag6owQAPerQUQQG0ZfIeBxFhMkjPXYEoHtEoJ0uyiPdqcpgSeEeM8qITwC39It257o+sYncCWXkvO5HLsEp381r78AhOcSEfdXGPIAAAAASUVORK5CYII=',
				_map :'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAMAAAAoLQ9TAAAAxlBMVEUAAABolMV0ncmCp9CJrNOLrtTX4/He6PPk7PVFfbRNg7hLf7NOg7hNgLRRhrtOgbVShrpTh7tThrldjr9gkcJfj8BiksNhkcJkk8NmlMRmlMNum8humsd2ocx7pM5+ps59pc2CqM+IrNKRs9aTtNeWtteXtteaudqowdupwturxN2xyOC80ebc5vDl7fVOgrRPg7VSiLpVirxVibtWirxXi7tbjr5dj75ilMNfkL5hksCqw9uwyN+yyuDf6fKmxN7H2ur///9YyPm8AAAAAXRSTlMAQObYZgAAAAFiS0dEAIgFHUgAAAAJcEhZcwAACxMAAAsTAQCanBgAAAAHdElNRQfdCgMSBA/RAL2VAAAApElEQVQY042P2xaBUBRFDyVFCoUuinRSqBRyCdX+/58ydm4vHsy3NccaY+1NyE8qdqxO2OqTPdVkGMZUvXfOHL8sS9/JnoaT7NhG4pnEoWCnIfCWZfEQNl0U2nEDUIwKgNtJQzHoBSBHEMkQdPt1I11BkkRJAsG2briHNQwRuO6XKNpCXuTI/SzUK4R2GnPksqOvy6ioG4ahi/T7y0JJlVZF/uIBSGAUYfqEPuUAAAAASUVORK5CYII=',
				_list: 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAMAAAAoLQ9TAAAAPFBMVEUAAAAAgIBfn5+fv784mFg4eDg4WDh/n3/A2MBffz9/n1+YuHjY2Jjf37/4+PD///+fn5+AgIBfX19YWFheY3MBAAAAAXRSTlMAQObYZgAAAAFiS0dEAIgFHUgAAAAJcEhZcwAACxMAAAsTAQCanBgAAAAHdElNRQfdCgMSBxaeRkaWAAAAbklEQVQY05WO0RKDMAgEEy0ngvTU/v+/lljTTB+7b9zsAaUUl8TLF7Egw6TPbnTAad2RIJ6OLboipOIF5Qg2ZCmD/WxkReEZxMrG2ZY+ADNd5lqnDO6zCt7G9RjWhSl8jIudHEbj4FR/jON/Y/AG2/AHPeWumhMAAAAASUVORK5CYII=',
				_integer: 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAMAAAAoLQ9TAAAAzFBMVEUAAADFvODFvd7X0elwXrBvXa5yYLFxX7BzYbF1ZLF4ZrR9bLh9bLeCcrmPgMKXiMaVh8WYiseekcucj8mfksuilsylmc+onNCupNGxp9Tc1+zi3vBVQp9aR59dSqRaSJ9ZR55cSaBeS6RdSqFcSqBdS6FhT6dgT6ZhT6ZiUKdkUqhiUKVgT6JkUqdlU6hlVKlsW61rW6xsXKxxYLFvXq51ZLKDdLyJer+GeLuLfb+KfL6OgMGhlcyupdK5sNrMxuReTqFpWayupdT///9zohUCAAAAAXRSTlMAQObYZgAAAAFiS0dEAIgFHUgAAAAJcEhZcwAACxMAAAsTAQCanBgAAAAHdElNRQffCRgHBhzwFfQXAAAAqElEQVQY05WPXReBQBRFQ0OJQikSjUI+okIjitL9//9JE/Fsv929zlrnXIYpSKUm25JSpiJhBVEUBTapbi9W8zxXY+9t9NVFg4KGdl3pVEgdQgYANUJIV6aCeyKEAB4IBQFHxWZuYwwwwtiersuEszRNgLuJ7UWZkA87NwMYZ667H1IxMc4RbalHJ6NsYZRZ+0Y5bpXPMsUKeZ4PLeW7Pe35jt///fInL3ghFal4aGkqAAAAAElFTkSuQmCC',
				_boolean: 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAMAAAAoLQ9TAAABXFBMVEUAAAAGDAULEgoNEwsNFwkPFw0QFw4OGQoSFhEQHQwTGw8SHA8WGRUQHgwSIQ0ZHRcTIw4cIBsZLxMmRhwmRxwnSR08QTstUiEwWSQxWiQxWyRGS0RHS0UyXCQzXSVKUEg1YSdMUUo2ZCg5aCk5aSpVW1M8byw8cCw+cS0/dC5Adi9BeDBhYWFiYmJDfDFjY2NgZl9kZGREfjJFfzNGgjNGgjRHgzRlbGNJhjVKiDZMizdNjThQkzpSlzxVnD5YoUBYo0GDh4KGioWIjIdfrkWIjYeJjoiMkItitUiOk41jtkmQlo9lukplu0pmvEtnvUtnvktnvkxov0xowExpwk1qwk1qw01qw05qxE5rxE5rxk5rxk9sxk9sx09syE9tyE+0vbK8xLrHzMXIzsbHz8XKz8jP1M3O1czR1s/R1tDU2dLV29LY3dba4Njf5N7k6OPk6ePo6+f9/v3///9HAKs+AAAA5ElEQVQY02MoBoFMT0N99zQwkwGIcxT5JE3NZAVksiACiWzagdGhIdFBeqwJIIFcZrsYXx8g8I22Z8oGCkhrxfrIMTAwsFj5xBiJFTFk8Ab4+sjJ+fhYMfr4BAskM7hJRPmABVyAAtEKjgw6JmFAARYhIUZlH58QSx0GfeNwoICwlZWmAFDAQpfBWzwaosVHyMonWsqVIYfdF2KoLaODrz9nBkOxqko8xFpNn1gNeaA7Cnis4/1ADvOLs+HIAzk9XVDJIyoyItpLmT8F4rlCcy4RNXVRboN8qG+Li1Od9fWdksBMAIWiRLLlwAe2AAAAAElFTkSuQmCC',
				_double: 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABmJLR0QA/wD/AP+gvaeTAAAAB3RJTUUH2QcRExQLBHwYWQAAAm9JREFUOI2Fk0tIlFEUx3/fNz7HwdGpmTRNhRmLJDVJsUUPI1+LQvLVorDaqEnCWOHaoqBdJZhItCho45j5iMgHhNODghYqKqUjMaaj46hkjlIz+t0W1uCk2H93zzm//73n3Hsl/tXL6mBWvAUIkY8gAVAhMQH0IFStlD5wbyyX/GBL5SkQDSnamLiS2EOYNAYCJJnxZRcdjkHez487gVpKmp5s2hhLuVnbZl5r/vZJKEIRW6lrZlhEd9YKLJU3/eGWyjxtm3l14PvkluBG2Zfn101ayi+st9BUHohOGn6ccTGxLP4wAOaBZt7O2QiQVEQEhpKui+fcnkz2h0cB0O0cIe9N/Sy/ghJldCLbpNEnno/L9B3I5nYxtOggS7+XmNAIGmx9pPXewjo3BkDuriSO7DQZCPYUySDlnI5OQZb85xmiCuRO8hkepZdhzbqGAG6MvPDlC3anAiJXRhBn1Og3D3WDkrUxpGpjGVp0+GLGMD0gx8lIklCE2NYAYGn1J+qAIN9aIAChyEh8HXPPbgu/dn1h1O3khH6fLza65ASwy0hKd4djgFWh+EErax5KPzwk23qPHOt9TBoDtw8U+PKtU/0g8UpFsdG+6KUwKiTckKFLAGBiZQFtoJoQVQAmjYEq43HqD54lMkgNwPOpfu6O9U4i1FXrdpbLR9Wt1Z53c7b/PqTPP2aErv2qQktlkX+jlopLoa1XvI3jfcKrrG0CFaGIp/aPIrK9RsFScf0v5n/5zyqOodCYELYjqTAmjcQ/n8nmdtE5PcjI0vQ4UENxU+fWBgB1dTJJ0yeRyUdgBCGBZEeiB2Whi1KLZ2P5bwjMSQjLsL1pAAAAAElFTkSuQmCC'
			}
			return map[type];
		},
		_getIconUrl: function (name) {
			var am = qx.util.AliasManager.getInstance(name);
			return am.resolve("resource/ms123/stencilsets/" + name);
		},
		_getNamespace:function(){
			if(this.config.namespace && this.config.namespace!='-'){
				return this.config.namespace;
			}
			return this.__storeDesc.getNamespace();
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
