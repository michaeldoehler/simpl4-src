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
	@ignore(moment)
	@ignore(jQuery)
	@ignore(jQuery.each)
	@lint ignoreDeprecated(alert,eval) 
	@asset(qx/icon/${qx.icontheme}/22/actions/list-add.png)
	@asset(qx/icon/${qx.icontheme}/22/actions/list-remove.png)
	@asset(qx/icon/${qx.icontheme}/22/status/dialog-information.png)
	@asset(qx/icon/${qx.icontheme}/16/apps/office-calendar.png)
*/

qx.Class.define('ms123.widgets.Form', {
	extend: ms123.widgets.Widget,

	construct: function (context) {
		this.base(arguments);
		this._context = context;
		this.__configManager = new ms123.config.ConfigManager();
		this.__dataAccess = context.dataAccess;
		this.__storeDesc = context.storeDesc;
		this._init(context);
	},
	statics: {
		formContainer: ["xform", "tabview", "page", "group"]
	},

	members: {
		_init: function (context) {
			this.name = context.config;
			this.parent = context.parent;
			var view = context.view ? context.view : "main";
			var self = this;
			this.formData = {};
			this._enabled = true;
			this.showDefaultButtons = true;

			var model = context.model;
			if (model) {
				var cols = context.model.attr("colModel");
				this._setKeyColumn(cols);
				if( this._context.dependent === false ){
					this._mode = "select";
				}

				var props = model.attr("gridProps");
				if (!props.formlayout) {
					props.formlayout = "tab1";
				}

				if (typeof props.formlayout == "string") {
					props.formlayout = this._buildFormLayout(props.formlayout, this.name, new ms123.config.ConfigManager().getCategory(this.name));
				}
				this.formLayout = props.formlayout;
				var cols = model.attr("colModel");
				this.formData = this._getFormData(cols);
			} else if (context.columns) {
				this.formData = this._getFormData(context.columns);
			} else if (context.formData) {
				this.formLayout = context.formLayout;
				this.formData = context.formData;
			} else if (context.formDesc) {
				this.formData = {};
				this.formLayout = context.formDesc;
				this.showDefaultButtons = this._showDefaultButtons(context.formDesc);
				this._getFormDataGE(context.formDesc,context.formVariables);
				//return;
			} else {
				//@@@MS Not used
				alert("Code wrong");
				return;
			}
			
			this.form = this._createForm();
			this.__changeListener();
			this.__addChangeListener();
			var dock = new qx.ui.layout.Dock();
			this._setLayout(dock);
			this.add(this.form, {
				edge: "center"
			});
		},
		_showDefaultButtons: function (formDesc) {
			var stencilId = formDesc.stencil.id.toLowerCase();
			var properties = formDesc.properties;
			return properties.xf_default_buttons ==  undefined ? true : properties.xf_default_buttons;
		},

		_getFormDataGE: function (formDesc,variables) {
			var stencilId = formDesc.stencil.id.toLowerCase();
			var properties = formDesc.properties;

			var isContainer = ms123.widgets.Form.formContainer.indexOf(stencilId) != -1;
			if (!isContainer) {
				var col = {};
				col.name = properties.xf_id || formDesc.resourceId.replace(/-/g,"");
				if( stencilId == "input"){
					col.edittype = "text";
					col.editable = true;
					col.datatype = properties.xf_type;
					if( col.datatype == "boolean"){
						col.edittype = "checkbox";
					}
					if( col.datatype == "datetime"){
						col.edittype = "datetime";
						col.datatype = "date";
					}
				}
				if( stencilId == "textarea"){
					col.edittype = "textarea";
					col.editable = true;
					col.datatype = "string";
				}
				if( stencilId == "moduleselector"){
					col.editable = true;
					col.config   = {
						entity : properties.xf_module,
						field : properties.xf_field,
						fieldList : properties.xf_fieldlist
					}
					col.edittype = "selector";
					col.datatype = properties.xf_type;
				}
				if( stencilId == "tableselect"){
					var iln = properties.xf_inputlistname;
					col.options = variables ? variables[iln] : [];
					col.config = { columns:properties.xf_columns,
												 filter:properties.xf_filter};
					col.multiselection = properties.xf_multiselection;;
					col.editable = true;
					col.edittype = "tableselect";
					col.datatype = "string";
				}
				if( stencilId == "gridinput"){
					var iln = properties.xf_inputlistname;
					col.options = variables ? variables[iln] : [];
					col.config = properties.xf_columns;
					col.editable = true;
					col.edittype = "gridinput";
					col.datatype = "string";
				}
				if( stencilId == "enumselect"){
					var enumembed = properties.xf_enumembed;
					if( enumembed && enumembed.totalCount>0){
						col.selectable_items = new ms123.SelectableItems( {url:enumembed.items,storeDesc:this.__storeDesc} );
					}else{
						var enums = properties.xf_enum;
						var varmap = { NAMESPACE : this.__storeDesc.getNamespace() };
						col.selectable_items = this.__configManager.createSelectableItems( this.__storeDesc, JSON.stringify(enums), varmap );
					}
					col.editable = true;
					col.edittype = properties.xf_multiselection ? "multiselect" : "select";
					col.datatype = "string";
				}
				if( stencilId == "alert"){
					col.editable = true;
					col.edittype = "alert";
					col.message = this._expandString(properties.xf_message,variables);
					col.datatype = "string";
				}
				if( stencilId == "actionbutton"){
					col.editable = true;
					col.edittype = "actionbutton";
					col.datatype = "string";
					col.action = properties.xf_action;
					col.label = properties.xf_label;
					if( col.label && col.label.match(/^@/)){
						col.label = ""+this.tr(col.label.substring(1));
					}
					col.iconname = properties.xf_iconname;
				}
				col.default_value = properties.xf_default;
				if( properties.xf_constraint_text ){
        	var map = qx.lang.Json.parse(properties.xf_constraint_text);
					var pattern = map["Pattern"];
					if( pattern.length>1 && pattern[0] === true){
						col.constraints = [{annotation:"Pattern", parameter1:pattern[1]}];
					}
				}
				this._getFieldData( this.formData, col );
				if( this.formData[col.name]){
					this.formData[col.name].resourceId = formDesc.resourceId;
					this.formData[col.name].requiredExpr = properties.xf_required;
					this.formData[col.name].exclude = properties.xf_exclude;
					this.formData[col.name].readonly = this._getReadonly(properties.xf_readonly);
				}
			}
			var childs = formDesc.childShapes;
			childs.each((function (child) {
				this._getFormDataGE(child,variables);
			}).bind(this));
		},

		_getReadonly:function(ro){
			if( ro === "true") return true;
			return false;
		},

		_getFormData: function (cols) {
			var formData = {};
			for (var i = 0; i < cols.length; i++) {
				var col = cols[i];
				this._getFieldData(formData, col);
			}
			return formData;
		},
		_getFieldData: function (formData, col) {
			if (col.editable == undefined || col.editable === false) return;
			var formoptions = col.formoptions;
			var fieldData = {};
			fieldData.position = col["form.position"];
			fieldData.enabled = col["form_enabled_expr"];
			if( col.readonly === true ){
				fieldData.readonly = true;
			}
			fieldData.exclude = col["form_exclude_expr"];
			fieldData.label = col.label;
			fieldData.header = col.mainform_header ? this.tr(col.mainform_header) : null;
			fieldData.datatype = col.datatype;
			fieldData.value = "";
			//				if( this._context.edit || this._context.multiedit){
			//					fieldData.readonly = col.edit_readonly;
			//				}
			if (col.default_value !== undefined) {
				fieldData.defaultValue = col.default_value;
			}
			fieldData.tab = (formoptions && formoptions.tab) ? formoptions.tab : "tab1";
			if (col.edittype == "text") {
				fieldData.type = "TextField";
				if (col.datatype && col.datatype == 'date') {
					fieldData.type = "DateField";
					fieldData.value = null;
				}
				if (col.datatype && (col.datatype == 'number' || col.datatype == 'integer' || col.datatype == 'long')) {
					fieldData.type = "NumberField";
					fieldData.value = null;
				}
				if (col.datatype && (col.datatype == 'decimal' || col.datatype == 'double')) {
					fieldData.type = "DecimalField";
					fieldData.value = null;
				}
				if (col.constraints) {
					fieldData.validation = this._getValidations(col.constraints);
				}
				if (col.filter) {
					if( !fieldData.validation){
						fieldData.validation = {};
					}
					fieldData.validation.filter = col.filter;
				}
			
			} else if (col.edittype=="date" && col.datatype=="date") {
				fieldData.type = "DateField";
				fieldData.value = null;
				if (col.constraints) {
					fieldData.validation = this._getValidations(col.constraints);
				}
			} else if (col.edittype=="datetime" && col.datatype=="date") {
				fieldData.type = "DateTimeField";
				fieldData.value = null;
				if (col.constraints) {
					fieldData.validation = this._getValidations(col.constraints);
				}
			} else if (col.edittype.match("^graphical")) {
				fieldData.type = "Button";
				fieldData.arg = col.edittype.substring("graphical/".length);
				fieldData.clazz = "ms123.graphicaleditor.GraphicalEditorWrapper";
			} else if (col.edittype == "auto") {
				fieldData.type = "NumberField";
				fieldData.readonly = true;
			} else if (col.edittype == "functional") {
				if (this._context.multiedit) return;
				if (col.datatype && col.datatype == 'date') {
					fieldData.type = "DateField";
				} else {
					fieldData.type = "TextField";
				}
				if (col.datatype && col.datatype == 'number') {
					fieldData.type = "NumberField";
				}
				if (col.datatype && col.datatype == 'decimal') {
					fieldData.type = "DecimalField";
				}

				fieldData.value = null;
				fieldData.readonly = true;
				fieldData.__formula = col.formula_client;
				if (col.selectable_items && col.selectable_items.length != undefined) {
					fieldData._select = this._list2map(col.selectable_items);
				}else{
					if(col.selectable_items){
						fieldData._select = col.selectable_items.getItemsAsMap();
					}
				}
			} else if (col.edittype && col.edittype.match("^tableselect")) {
				fieldData.type = "TableSelect";
				fieldData.value = null;
				fieldData.options = col.options;
				fieldData.config = col.config;
				fieldData.multiselection = col.multiselection;
			} else if (col.edittype && col.edittype == "gridinput") {
				fieldData.type = col.edittype;
				fieldData.value = null;
				fieldData.options = col.options;
				fieldData.config = col.config;
			} else if (col.edittype && col.edittype == "complexedit") {
				fieldData.type = col.edittype;
				fieldData.value = null;
				fieldData.options = col.options;
				fieldData.config = col.config;
			} else if (col.edittype && col.edittype == "resourceselector") {
				fieldData.type = col.edittype;
				fieldData.value = null;
				fieldData.config = col.config;
			} else if (col.edittype && col.edittype == 'alert') {
				fieldData.type = "Alert";
				fieldData.message = col.message;
			} else if (col.edittype && col.edittype == 'actionbutton') {
				fieldData.type = "ActionButton";
				fieldData.action = col.action;
				fieldData.callback = col.callback;
				fieldData.buttonlabel = col.buttonlabel;
				fieldData.iconname = col.iconname;
			} else if (col.edittype && col.edittype == "selector") {
				fieldData.type = "Selector";
				fieldData.value = null;
				fieldData.config = col.config;
			} else if (col.edittype && col.edittype.match("^related")) {
				fieldData.type = "RelatedTo";
				fieldData.value = null;
				fieldData.module = col.datatype.substring(col.datatype.indexOf("/") + 1);
			} else if (col.edittype && col.edittype == 'upload') {
				fieldData.type = "UploadField";
				fieldData.value = null;
				fieldData.validation = {};
				fieldData.validation.required = true;
			} else if (col.edittype && col.edittype == 'checkbox') {
				fieldData.type = "CheckBox";
				fieldData.value = null;
			} else if (col.edittype && col.edittype == 'textarea') {
				fieldData.type = "TextArea";
				fieldData.lines = col.editoptions_rows>0 ? col.editoptions_rows : 2;
				fieldData.value = "";
			} else if (col.edittype && col.edittype == 'table') {
				fieldData.type = "TableEdit";
				fieldData.config = col.editoptions_config;
				fieldData.value = "";
			} else if (col.edittype && (col.edittype == 'select' || col.edittype == 'combo')) {
				if (col.edittype == 'combo') {
					fieldData.type = "ComboBox";
				} else {
					fieldData.type = "SelectBox";
				}
				fieldData.value = null;
				if (col.default_value !== undefined) {
					fieldData.defaultValue = col.default_value;
				}
				if (col.selectable_items) {
					fieldData._select = col.selectable_items.getItemsAsMap();
					fieldData.options = col.selectable_items.getItems();
					if (typeof fieldData.options == 'string'){
						fieldData.selectable_items = col.selectable_items;
					}
				}
			} else if (col.edittype && col.edittype == 'treeselect') {
				fieldData.type = "TreeSelectBox";
				fieldData.value = null;
				fieldData.options = col.selectable_items;
			} else if (col.edittype && col.edittype == 'treemultiselect') {
				if (col.name == 'trait_list' || col.name == '_team_list') {
					fieldData.type = "TraitTreeMultiSelectBox";
				} else {
					fieldData.type = "TreeMultiSelectBox";
				}
				fieldData.value = null;
				fieldData.options = col.selectable_items.getItems();
			} else if (col.edittype && col.edittype == 'multiselect') {
				if (col.name == 'trait_list' || col.name == '_team_list') {
					fieldData.type = "TraitDoubleSelectBox";
				} else {
					fieldData.type = "DoubleSelectBox";
				}
				fieldData.value = null;
				fieldData.options = [];
				if (col.selectable_items) {
					fieldData.options = col.selectable_items.getItems();
				}
			} else if (col.edittype && col.edittype != '') {
				fieldData.type = col.edittype;
				fieldData.config = col.editoptions_config;
				if( col.selectable_items) fieldData.options = col.selectable_items.getItems();
				fieldData.value = "";
			}
			if (fieldData.type == null) {
				alert("no fieldtype:" + fieldData.label + "," + col.edittype);
			} else {
				formData[col.name] = fieldData;
			}
		},
		_getValidations: function (data) {
			var validation = {}
			var cv = null;
 			if( typeof data == 'string'){
        cv = qx.lang.Json.parse(data);
      }else{
        cv = data;
      }

			for (var i = 0; i < cv.length; i++) {
				var c = cv[i];
				if (c.annotation == "NotEmpty") {
					validation.required = true;
				}
				if (c.annotation == "Pattern") {
					validation.validator = "/^" + c.parameter1 + "$/";
				}
				if (c.message) {
					validation.invalidMessage = c.message;
				}
			}
			return validation;
		},
		validate: function () {
			return this.form.validate();
		},
		_saveForm: function (data) {
			var self = this;
			var sep = (this.current_url.indexOf("?") == -1) ? "?" : "&";
			var url = this.current_url + sep + "module=" + this.name;
			console.log("_saveForm:" + url);
			var completed = function (e) {
				var content = e;
				var cv = e["constraintViolations"];
				if( cv ){
					var message = "";
					for (var i = 0; i < cv.length; i++) {
						var c = cv[i];
						message += this.getLabel(c.path) + " : " + c.message + "<br />";
					}
					ms123.form.Dialog.alert(message);
					this.setErrors(cv);
					return;
				}
				ms123.form.Dialog.alert(self.tr("data.form.saved"));
				this.setAllValid();
				this._mode = "edit";
			};

			this._rpcParams.data = data;
			try{ 
				var ret = null;
				if( this._mode == "add" ) ret = this.__dataAccess.insert( this._rpcParams );
				else                      ret = this.__dataAccess.update( this._rpcParams );
				completed.call(this,ret);
			}catch(e){
				ms123.form.Dialog.alert( e.message);
			}


		},

		_selectData: function () {
				var self = this;
				var context = {};
				context.modulename = this._rpcParams.entityChild;
				context.user = this._context.user;
				context.title = this.tr("widgets.table.select_record");
				context.storeDesc = this.__storeDesc;
				context.selected_callback = function(value){
					self.fillForm( value );	
					var id = value[self._keyColumn];
					self.__updateAssignment(id,true);
				};
				new ms123.util.RecordSelector(context);
		},

		__updateAssignment:function(id,assign){
			var data = {};
			data[this._context.fieldname] = id;	
			var hints = {};
			var params ={
				storeDesc : this.__storeDesc,
				entity:this._rpcParams.entity,
				id:this._rpcParams.id+"",
				data : data,
				hints : hints
			}

			try{
				this.__dataAccess.update( params );
				ms123.form.Dialog.alert(this.tr("data.form.saved"));
			}catch(e){
				ms123.form.Dialog.alert( "Form.__updateAssignment:"+e);
			}
		},
		getData: function () {
			var data = "";
			var m = this.form.getModel();
			var items = this.form.getItems();

			var props = qx.Class.getProperties(m.constructor);
			var js = "var _mode='"+this._mode+"';\n";
			var self = this;
			jQuery.each(props, function (index, p) {
				var val = m.get(p);
				if (val == null) val = "";
				var fd = self.formData[p];
				if (self._checkVar(p)) {
					if ((fd.type == "DateField" || fd.type == "DateTimeField") && typeof val == 'object' && val.constructor == Date && val != "") {
						if (val) {
							var df = new qx.util.format.DateFormat("dd.MM.yyyy");
							var v = df.format(val);
							js += "var " + p + " = '" + v + "';\n";
						} else {
							js += "var " + p + " = null;\n";
						}
					} else {
						if (typeof val == 'string') {
							val = val.replace(/\n/g, "");
						}
						js += "var " + p + " = '" + val + "';\n";
					}
					if (fd._select) {

						var sel_enum = qx.util.Serializer.toJson(fd._select);
						js += "var " + p + "_enum = " + sel_enum + ";\n";
					}
				}
			});
			var map = {};
			var hints = {};
			jQuery.each(props, function (index, p) {
				var fd = self.formData[p];
				var val = m.get(p);
console.log("val:"+p+"="+val);
				if ((fd.type == "DateField" || fd.type == "DateTimeField") && val != null && val != "" && typeof val == 'object' && val.constructor == Date) {
					val = val.getTime()-(val.getTimezoneOffset()*60000);
				}
				console.log("__formula:"+fd.__formula);
				if (fd.__formula) {
					var cu = "function _cu(s){try{if( s === undefined || s == null ) { return ''; }else return s;}catch(e){return ''}}";
					var evalstr = cu + ";\nvar _b = ' ';\n" + js + ';' + fd.__formula + ";";
					var ret = "";
					try {
						ret = eval(evalstr);
						console.log("eval(" + p + "):" + ret);
					} catch (ex) {
						console.log("Ex1("+p+"):" + ex + ",evalstr:" + evalstr);
					}
					try {
						if (fd.type == "DateField" || fd.type == "DateTimeField" ) {
							var d = self._getDate(ret);
							if (d != null) {
								m.set(p, d);
								val = d.getTime();
							}
						} else {
							m.set(p, ret + "");
							val = ret;
						}
					} catch (ex) {
						console.error("Ex2:" + ex + ",evalstr:" + evalstr + ",ret:" + ret + "|");
					}
				}
				if (self._context.multiedit || self._context.useitCheckboxes) {
					var useit = false;
					if (items[p]) {
						var u = items[p].getUserData("useit");
						if (u) {
							var v = u.getValue();
							if (typeof v == "boolean") {
								useit = v;
							} else {
								useit = (v == "ignore") ? false : true;
								if (useit) {
									var mode = {
										mode: v
									};
									hints[p] = mode;
								}
							}
						}
					}
					if (useit) {
						map[p] = val;
					}
				} else {
					if(!items[p].isExcluded()){
						map[p] = val;
					}
				}
			});
			if( Object.keys(hints).length > 0){
				map["__hints__"] = hints;
			}
			return map;
		},
		_loadData: function () {
			var sep = (this.current_url.indexOf("?") == -1) ? "?" : "&";
			var map = null;
			try {
				var failed = function (details) {
					ms123.form.Dialog.alert( details.message);
				};

				var p = this._rpcParams;
				p.storeDesc = this.__storeDesc,
				p.async = false;
				p.failed = failed;
				p.context = this;
				var map = this.__dataAccess.queryOne( p );
				if (map == null || ((typeof map === 'string') && map.trim() == "")) {
					map = {};
				}
			} catch (e) {
				map = {};
			}
			return map;
		},
		beforeEdit: function (context) {
			this.form.beforeEdit(context);
			this._mode = "edit";
		},
		beforeAdd: function (context) {
			this.form.beforeAdd(context);
			this._mode = "add";
		},
		beforeSave: function (context) {
			this.form.beforeSave(context);
		},
		afterSave: function (context) {
			this.form.afterSave(context);
		},
		setData: function (map) {
			this.fillForm(map);
		},
		fillForm: function (map) {
			this.__removeChangeListener();
			var m = this.form.getModel();
			var props = qx.Class.getProperties(m.constructor);
			var items = this.form.getItems();
			for (var i = 0, l = props.length; i < l; i++) {
				try {
					var p = props[i];
					if (map[p] != undefined && map[p] != null) {
						if (this.formData[p].type == "DateField" || this.formData[p].type == "DateTimeField"  ) {
							var d = new Date();
							if (map[p] != "") {
								if (!isNaN(map[p] - 1)) {
									d.setTime(map[p]);
									m.set(p, d);
								} else {
									var d = new Date(moment(map[p]));
									m.set(p, d);
								}
							} else {
								m.set(p, null);
							}
						} else if (this.formData[p].type.indexOf("DoubleSelectBox") != -1) {
							if (typeof map[p] === 'string' && map[p]) {
								if (map[p].match("^{") || map[p].match("^\\[")) {
									map[p] = qx.lang.Json.parse(map[p], false);
								} else {
									map[p] = map[p].split(",");
								}
							}
							m.set(p, map[p]);
						} else {
							if (this.formData[p].datatype == "boolean" && (typeof map[p] == "string")) {
								m.set(p, this._stringToBoolean(map[p]));
							} else {
								m.set(p, map[p]);
							}
						}
					} else {
						if (this.formData[p].type == "SelectBox") { //@@@MS First value as default
							if (this.formData[p].defaultValue) {
								m.set(p, this.formData[p].defaultValue);
							} else if (this.formData[p].options && this.formData[p].options.length > 0) {
								var o = this.formData[p].options[0];
								m.set(p, o.value);
							}
							//}else if (this.formData[p].type.indexOf( "DoubleSelectBox") != -1) { //@@@MS nothing todo @@@MS why?
						} else {
							if (this.formData[p].defaultValue !== undefined) {
								if (this.formData[p].datatype == "boolean" && (typeof this.formData[p].defaultValue == "string")) {
									m.set(p, this._stringToBoolean(this.formData[p].defaultValue));
								}else if (this.formData[p].datatype == "date" && (typeof this.formData[p].defaultValue == "string")) {
									;
								} else {
									m.set(p, this.formData[p].defaultValue);
								}
							} else {
								try{
									m.set(p, null);
								}catch(e){
									console.error("-> Error setting null on:"+p);
									console.log(e.stack);
									console.log("Ex:"+e+"\n------------------------------");
								}
							}
						}
					}
					if (items[p]) items[p].setValid(true);
					if ((this._context.multiedit || this._context.useitCheckboxes) && items[p] && items[p].getUserData("useit")) {
						items[p].getUserData("useit").setValue(this._context.useitCheckboxesDefault !== undefined ? this._context.useitCheckboxesDefault : false);
					}
				} catch (e) {
					console.log(e.stack);
					console.error("widgets.Form.fillForm:" + e + ",(" + p + "=" + map[p] + ")");
				}
			}
			this.__changeListener();
			this.__addChangeListener();
		},
		__removeChangeListener: function () {
			var m = this.form.getModel();
			m.removeListener("changeBubble", this.__changeListener, this);
		},
		__addChangeListener: function () {
			var m = this.form.getModel();
			m.addListener("changeBubble", this.__changeListener, this);
		},
		__changeListener: function (e) {
			var eventData = e ? e.getData() : null;
			var m = this.form.getModel();
			var props = qx.Class.getProperties(m.constructor);
			var items = this.form.getItems();
			var map = {};
			for (var i = 0, l = props.length; i < l; i++) {
				var p = props[i];
				map[p] = m.get(p);
			}
			map["isEdit"] = this._mode == "edit";
			//var fD = qx.util.Serializer.toJson(map);console.log("map:"+fD);
			for (var i = 0, l = props.length; i < l; i++) {
				var p = props[i];
				if (items[p]) {
					if( eventData && items[p].updateEvent){
						items[p].updateEvent(eventData);
					}
					var enabledExpr = items[p].getUserData("enabled");
					if (enabledExpr) {
						try {

							var e = this.__maskedEval(enabledExpr, map);
							items[p].setEnabled(e);
						} catch (ee) {
							console.error("ee:" + ee);
							console.log("enabledExpr:"+enabledExpr+"/map:"+Object.toJSON(map));
						}
					}
					var excludeExpr = items[p].getUserData("exclude");
					if (excludeExpr) {
						try {
							var e = this.__maskedEval(excludeExpr, map);
							var label = items[p].getUserData("label");
							if( e){
								items[p].exclude();
								//this._setValidator(items[p],null);
								if( label) label.exclude();
							}else{
								//this._setValidator(items[p], items[p].getUserData("validator"));
								items[p].show();
								if( label) label.show();
							}
						} catch (ee) {
							console.error("ee:" + ee);
							console.log("excludeExpr:"+excludeExpr+"/map:"+Object.toJSON(map));
						}
					}
					var requiredExpr = items[p].getUserData("requiredExpr");
					if (requiredExpr) {
						try {
							var e = this.__maskedEval(requiredExpr, map);
							items[p].setRequired(e);
							items[p].setValid(true);
						} catch (ee) {
							console.error("ee:" + ee);
							console.log("requiredExpr:"+requiredExpr+"/map:"+Object.toJSON(map));
						}
					}else{
						items[p].setValid(true);
					}
				}
			}
		},
		__maskedEval: function (scr, env) {
			try{
				return (new Function("with(this) { return " + scr + "}")).call(env);
			}catch(e){
				console.log("Form.__maskedEval:"+scr);
				console.error("error:"+e);
				console.error(e.stack);
			}
		},
		/*_setValidator:function(item, validator){
			var formItems = this.getValidationManager().__formItems;
      for (var i=0; i < formItems.length; i++) {
        if(formItems[i].item==item){
					formItems[i].validator=validator;
				}
      }
		},*/
		getLabel: function (path) {
			if (this.formData[path] !== undefined && this.formData[path].label !== undefined) return this.formData[path].label;
			return path;
		},
		getValidationManager: function () {
			return this.form.getValidationManager();
		},
		setAllValid: function () {
			var m = this.form.getModel();
			var props = qx.Class.getProperties(m.constructor);
			var items = this.form.getItems();
			for (var i = 0, l = props.length; i < l; i++) {
				var p = props[i];
				if (items[p]) items[p].setValid(true);
			}
		},
		clearForm: function () {
			var m = this.form.getModel();
			var props = qx.Class.getProperties(m.constructor);
			for (var i = 0, l = props.length; i < l; i++) {
				var p = props[i];
				m.set(p, null);
			}
		},
		setErrors: function (constraintViolations) {
			this.setAllValid();
			var items = this.form.getItems();
			for (var i = 0; i < constraintViolations.length; i++) {
				var cv = constraintViolations[i];
				if( cv.path){
					items[cv.path].setInvalidMessage(cv.message);
					items[cv.path].setValid(false);
				}
			}
		},
		setAllEnabled: function () {
			var m = this.form.getModel();
			var props = qx.Class.getProperties(m.constructor);
			var items = this.form.getItems();
			for (var i = 0, l = props.length; i < l; i++) {
				var p = props[i];
				if (items[p]) items[p].setEnabled(true);
			}
		},
		setDisabled: function (fieldList) {
			this.setAllEnabled();
			var items = this.form.getItems();
			for (var i = 0; i < fieldList.length; i++) {
				var f = fieldList[i];
				items[f].setEnabled(false);
			}
		},
		_createForm: function () {
			var buttons = [{
				'label': (this._mode == "select") ? this.tr("data.form.select") : this.tr("data.form.save"),
				'icon': "icon/22/actions/dialog-ok.png",
				'enabled': (this._mode == "select") ? true : null,
				'value': 1
			}];
			var self = this;
			if (this._context.buttons) {
				buttons = this._context.buttons;
			}
			if (!this.showDefaultButtons) {
				buttons = [];
			}
//console.log("FORM:"+ qx.lang.Json.stringify(  this.formData, null, 4 ));
			var form = new ms123.form.Form({
				"tabs": this.formLayout,
				"useitCheckboxes": (this._context.multiedit === true || this._context.useitCheckboxes) ? true : false,
				"formData": this.formData,
				"buttons": buttons,
				"entityName": this.name,
				"allowCancel": true,
				"inWindow": false,
				"storeDesc": this.__storeDesc,
				"actionCallback": function(e){
					if( self._context.actionCallback ){
						self._context.actionCallback.call(self,e);
					}
				},
				"callback": function (m, v) {
					if (self._context.buttons) {
						var map = self.getData();
						for (var i = 0; i < buttons.length; i++) {
							var b = buttons[i];
							if (b.value == v && b.callback) {
								b.callback.call(self, map);
							}
						}
					} else {
						var map = self.getData();
						var formData = qx.util.Serializer.toJson(map);
						var data = "__data=" + encodeURIComponent(formData);
						if (self._enabled) {
							if( self._mode == "select"){
								self._selectData(map);
							}else{
								self._saveForm(map);
							}
						}
					}
				},
				"context": null
			});
			return form;
		},
		_getDate: function (str) {
			try {
				if (typeof str == 'object' && str.constructor == Date) {
					return str;
				}
				var d = moment(str);
				return isNaN(d) ? null : new Date(d);
			} catch (ex) {
				return null;
			}
		},
		_checkVar: function (str) {
			if (/^[a-z$_]\w{2,64}$/i.test(str)) {
				return true;
			}
			return false;
		},
		_list2map: function (list) {
			var ret = {};
			if (list.length === undefined) {
				return ret;
			}
			for (var i = 0; i < list.length; i++) {
				var o = list[i];
				ret[o["value"]] = o;
			}
			return ret;
		},
		_stringToBoolean: function (string) {
			switch (string.toLowerCase()) {
			case "true":
			case "yes":
			case "1":
				return true;
			case "false":
			case "no":
			case "0":
			case null:
				return false;
			default:
				return Boolean(string);
			}
		},
	 _expandString:function(str, binding) {
			if( !str) return "";
			var countRepl = 0;
			var countPlainStr = 0;
			var replacement = null;
			var   newString = "";
			var      openBrackets = 0;
			var      first = 0;
			for (var i = 0; i < str.length; i++) {
				if (i < str.length - 2 && str.substring(i, i + 2)=="${") {
					if (openBrackets == 0) {
						first = i + 2;
					}
					openBrackets++;
				} else if (str.charAt(i) == '}' && openBrackets > 0) {
					openBrackets -= 1;
					if (openBrackets == 0) {
						countRepl++;
						replacement = this.__maskedEval(str.substring(first, i), binding);
						newString += replacement;
					}
				} else if (openBrackets == 0) {
					newString += str.charAt(i);
					countPlainStr++;
				}
			}
			if (countRepl == 1 && countPlainStr == 0) {
				return replacement;
			} else {
				return newString;
			}
		},

		_buildFormLayout: function (formLayout, module, category) {
			console.log("_buildFormLayout:" + formLayout + "/" + module + "/" + category);
			if (formLayout == null) {
				return [{
					id: "tab1"
				}];
			} else {
				try {
					var lays = formLayout.split(";");
					var formLayout = [];
					for (var i = 0; i < lays.length; i++) {
						var t = lays[i].split(":");
						var tab = {};
						if (t.length == 2) {
							tab.id = t[0];
							tab.layout = t[1];
						}
						if (t.length == 1) {
							tab.id = t[0];
						}
						tab.title = this.tr(category + "." + module + "." + tab.id);
						formLayout.push(tab);
					}
				} catch (e) {
					console.log("catch:" + e);
					formLayout = [{
						id: "tab1"
					}];
				}
			}
			return formLayout;
		},
		_setKeyColumn: function (cols) {
			this._keyColumn=null;
			var _hasIdColumn  = false;
			for (var i = 0; i < cols.length; i++) {
				var col = cols[i];
				if (col.key) {
					this._keyColumn = col.name;
				}
				if (col.name == "id") {
					_hasIdColumn = true;
				}
			}
			if (this._keyColumn == null && _hasIdColumn) this._keyColumn = "id";
		},
		showRecord: function (id) {
			this._enabled = true;
			this._rpcParams = {};
			var sdesc = this._context.storeDesc;
			this._rpcParams.storeDesc= sdesc;
			this._rpcParams.namespace= sdesc.getNamespace();
			this._rpcParams.pack= sdesc.getPack();
			this._rpcParams.storeId= sdesc.getStoreId();
			if (this._context.configMaster === this._context.config) {
				this.current_url = "data/" + this._context.configMaster + "/" + id;
				this._rpcParams.entity = this._context.configMaster;
				this._rpcParams.id = id+"";
			} else {
				var p = this._context.fieldname;
				if (!p) p = this._context.config;
				this.current_url = "data/" + this._context.configMaster + "/" + id + "/" + p;
				this._rpcParams.entity = this._context.configMaster;
				this._rpcParams.id = id+"";
				this._rpcParams.entityChild = p;
				if( this._context.dependent === false ){
					this._mode = "select";
				}
			}
			var map = {};
			if (id != null) {
				map = this._loadData();
			}
			this.form.setEnabled(true);
			if( this._mode != "select" ){
				if (map.id === undefined) {
					this._mode = "add";
				} else {
					if (this._context.configMaster !== this._context.config) {
						this._rpcParams.idChild = map.id;
					}
					this._mode = "edit";
				}
			}else{
				this.form.setButtonsReadonly(id==null ? true : false);
			}
			this.fillForm(map);
		}
	}
});
