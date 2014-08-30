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
	* @ignore($)
*/
qx.Class.define("ms123.form.RelatedTo", {
	extend: qx.ui.core.Widget,
	implement: [
	qx.ui.form.IStringForm, qx.ui.form.IForm],
	include: [
	qx.ui.form.MForm, ms123.searchfilter.MSearchFilter],


	/**
	 *****************************************************************************
	 CONSTRUCTOR
	 *****************************************************************************
	 */

	construct: function (context, useitCheckboxes) {
		this.base(arguments);
		var layout = new qx.ui.layout.HBox();
		this._setLayout(layout);
		this._storeDesc = context.storeDesc || ms123.StoreDesc.getNamespaceDataStoreDesc();
		this._disableSettingValue = false;

		this._init(useitCheckboxes);
		this._user = ms123.config.ConfigManager.getUserProperties();
		this.addListener("changeValid", function(e){
			this.getChildControl("textfield").setValid(e.getData());
		},this);
	},

	/**
	 *****************************************************************************
	 PROPERTIES
	 *****************************************************************************
	 */
	properties: {
		// overridden
		appearance: {
			refine: true,
			init: "combobox"
		},
		height: {
			nullable: true,
			transform: "_setHeight"
		}
	},

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
		_init: function (useitCheckboxes) {
			var textField = this._createChildControl("textfield");
			var textfieldVisible = this._createChildControl("textfieldVisible");
			var select = this._createChildControl("select");
			var clear = this._createChildControl("clear");
			if (useitCheckboxes) {
				this._createChildControl("checkbox");
			}
		},
		_setHeight: function (h) {
			var select = this.getChildControl("select");
			select.setHeight(h);
			var clear = this.getChildControl("clear");
			if (clear) clear.setHeight(h);
			return h;
		},
		// overridden
		_createChildControlImpl: function (id, hash) {
			var control;
			switch (id) {
			case "textfield":
				control = new qx.ui.form.TextField();
				control.setLiveUpdate(true);
				control.setFocusable(false);
				control.setReadOnly(true);
				control.setVisibility("excluded");
				control.addState("inner");
				control.addListener("changeValue", this._onTextFieldChangeValue, this);
				this._add(control, {
					flex: 1
				});
				break;
			case "textfieldVisible":
				control = new qx.ui.form.TextField();
				control.setLiveUpdate(true);
				control.setFocusable(false);
				control.setReadOnly(true);
				control.addState("inner");
				this._add(control, {
					flex: 1
				});
				break;
			case "checkbox":
				control = new qx.ui.form.CheckBox();
				control.setFocusable(false);
				control.setKeepActive(true);
				control.addState("inner");
				control.set({
					decorator: "main"
				});
				this._add(control);
				break;
			case "select":
				var control = new qx.ui.form.Button(null, "icon/16/apps/utilities-text-editor.png").set({
					padding: 0,
					margin: 0,
					maxHeight: 22
				});
				control.addListener("execute", function () {
					this._displayWindow();
				}, this);
				this._add(control);
				break;
			case "clear":
				var control = new qx.ui.form.Button(null, "icon/16/actions/edit-clear.png").set({
					padding: 0,
					margin: 0,
					maxHeight: 22
				});
				control.addListener("execute", function () {
					this.resetValue();
				}, this);
				this._add(control);
				break;
			}
			return control;
		},
		_displayWindow: function () {
			var win = this._createWindow(this.tr("relatedto.selection") + ": " + this.tr("data."+this._modulename));
			var table = this._createTable(win);
			var self = this;
			var params = {
				modulename: this._modulename,
				onSave: function (data) {
					self._saveSearchFilter(data, self._modulename, null);
				},
				onSelect: function (sf) {
					self._selectSearchFilter(self._modulename, null, self._storeDesc, sf);
				},
				onSearch: function (data) {
					table.setFilter(data);
				}
			}

			var sf = this._createSearchFilterWithChilds(this._modulename, params, this._storeDesc);
			var sp = this._doLayout(win, sf, table)
			win.add(sp, {});
			var app = qx.core.Init.getApplication();
			app.getRoot().add(win);
			win.open();
			this._win = win;
		},

		// overridden
		/**
		 * @lint ignoreReferenceField(_forwardStates)
		 */
		_forwardStates: {
			focused: true
		},
		// overridden
		tabFocus: function () {
			var field = this.getChildControl("select");
			field.getFocusElement().focus();
			field.selectAllText();
		},
		// overridden
		focus: function () {
			this.base(arguments);
			this.getChildControl("select").getFocusElement().focus();
		},
		// interface implementation
		setValue: function (value) {
			if (this._disableSettingValue === true) return;
			var textfield = this.getChildControl("textfield");
			if (textfield.getValue() == value) {
				return;
			}
			// Apply to text field
			textfield.setValue(value);
		},
		// interface implementation
		getValue: function () {
			return this.getChildControl("textfield").getValue();
		},
		// interface implementation
		resetValue: function () {
			this.getChildControl("textfield").setValue(null);
		},
		// useit checkbox
		getCheckBox: function () {
			return this.getChildControl("checkbox");
		},

		/**
		 ---------------------------------------------------------------------------
		 EVENT LISTENERS
		 ---------------------------------------------------------------------------
		 */
		/**
		 * Reacts on value changes of the text field and syncs the
		 *
		 * param e {qx.event.type.Data} Change event
		 */
		_onTextFieldChangeValue: function (e) {
			var value = e.getData();
			// Fire event
			var displayValue = value ? value.replace(/(\/[0-9a-f]{32})|(^[0-9a-f]{32})/, "") : value;
			this.getChildControl("textfieldVisible").setValue(displayValue);
			this.fireDataEvent("changeValue", value, e.getOldData()); //@@@MS siehe andere
		},


		/**
		 ---------------------------------------------------------------------------
		 TEXTFIELD SELECTION API
		 ---------------------------------------------------------------------------
		 */
		getTextSelection: function () {
			return this.getChildControl("textfield").getTextSelection();
		},
		getTextSelectionLength: function () {
			return this.getChildControl("textfield").getTextSelectionLength();
		},
		setTextSelection: function (start, end) {
			this.getChildControl("textfield").setTextSelection(start, end);
		},
		clearTextSelection: function () {
			this.getChildControl("textfield").clearTextSelection();
		},
		selectAllText: function () {
			this.getChildControl("textfield").selectAllText();
		},
		setFormData: function (formData,entityName) {
			this._formData = formData;
			var c = new  ms123.config.ConfigManager();
			this._fillinFields = c.getEntityViewFields(entityName, this._storeDesc, "main-form",false);
			//console.log("f:"+JSON.stringify(this._fillinFields,null,2));
			var fromFields = c.getEntityViewFields(this._modulename, this._storeDesc, "main-form",false);
			this._fromFields ={};
			for(var i=0; i < fromFields.length;i++){
				this._fromFields[fromFields[i].name] = fromFields[i];
			}
			
		},
		setModule: function (mod) {
			 console.log("relatedto.mod:"+mod);
			if (mod.indexOf("/") != -1) {
				var part0 = mod.split("/")[0];
				var part1 = mod.split("/")[1];
				var lastDot = part0.lastIndexOf(".");
				if( lastDot!=-1){
					mod = part0.substring(lastDot+1);
				}else{
					mod = part1;
				}
			}
			if (mod.indexOf(".") != -1) {
				mod = mod.split(".")[1];
			}
			this._modulename = ms123.util.Inflector.singularize(mod).toLowerCase();
			var cm = new ms123.config.ConfigManager();
			this._idField = cm.getIdField(this._storeDesc, this._modulename);
			var entProps = cm.getEntityViewProperties(this._modulename, this._storeDesc);
			this._titleExpression = entProps.title_expression;
		},
		/**
		 ---------------------------------------------------------------------------
		 Related Selection
		 ---------------------------------------------------------------------------
		 */
		_createTable: function (win) {
			var self = this;
			var buttons = [{
				'label': "",
				'icon': "icon/16/actions/dialog-ok.png",
				'callback': function (m) {
					var cr = self._table.getCurrentRecord();
					self._setSelectedValues(cr, m);
					self._disableSettingValue = true;
					self.fireDataEvent("changeValue", cr); //@@@MS ????
					self._disableSettingValue = false;
					win.close();
				},
				'value': "select"
			},
			{
				'label': "",
				'icon': "icon/16/actions/dialog-close.png",
				'callback': function (m) {
					win.close();
				},
				'value': "cancel"
			}];
			var cm = new ms123.config.ConfigManager();
			var context = {};
			context.buttons = buttons;
			console.log("RelatedTo.storeDesc:" + this._storeDesc + "/" + this._modulename);
			context.model = cm.getEntityModel(this._modulename, this._storeDesc, "main-grid", "properties");
			//context.modelForm = cm.getEntityModel(this._modulename, this._storeDesc, "main-form", "properties");
			context.unit_id = "related/+" + this._modulename;
			context.config = this._modulename;
			context.storeDesc = this._storeDesc;
			context.user = this._user;

			this._table = new ms123.widgets.Table(context);
			this._table.addListener("dblclick", this._onCellDblClick, this);
			return this._table;
		},
		_onCellDblClick:function(e){
			var cr = this._table.getCurrentRecord();
			this._setSelectedValues(cr);
			this._disableSettingValue = true;
			this.fireDataEvent("changeValue", cr); //@@@MS ????
			this._disableSettingValue = false;
			this._win.close();
		},
		_setSelectedValues: function (cr) {
			var formModel = this.getUserData("formmodel");
			if (!formModel) return;

			var fillinArr = [];
			for(var i=0; i < this._fillinFields.length;i++){
				var f = this._fillinFields[i];
				if( f.fillin){
					var m = this._parseFillin(f.fillin);
					if(m[this._modulename]){
						var x=m[this._modulename];
						x.name = f.name; 
						fillinArr.push(x);
					}
				}
			}
			console.log("fillinArr:"+JSON.stringify(fillinArr,null,2));
			if( fillinArr.length>0){
				for(var i=0; i < fillinArr.length;i++){
					var f = fillinArr[i];
					if( f.expr=="false"){
						continue;
					}
					var name = f.name;
					if(this._fromFields[f.expr]){
						name = f.expr;
						console.log("name2:"+name);
					}else if( f.expr=="true"){
					}else{
						var res = this.__maskedEval(f.expr,cr);
						console.log("evalResult:"+res);
						if( res != null && res !== true){
							formModel.set(name, res );
							continue;
						}
					}
					try{
						this._setValue(formModel,name,cr);
					}catch(e){
						console.error("_setValue("+name+"):"+e);
					}
				}
			}else{
				var formModelProps = qx.Class.getProperties(formModel.constructor);
				for (var i = 0, l = formModelProps.length; i < l; i++) {
					var fmProp = formModelProps[i];
					if (fmProp != "name" && fmProp != "id" && cr[fmProp] != undefined) { //@@@MS Hack, do not copy name and id values
						var fieldData = this._formData[fmProp];
						if( !fieldData.readonly && fmProp.match(/^[a-z]/)){	
							this._setValue(formModel, fmProp, cr);
						}
					}
				}
			}

			var key = this.getUserData("key");
			var mainValue = cr[key];
			var title = this._getRecordTitle(cr);
			if( this._titleExpression){
				var v = this.__maskedEval(this._titleExpression,cr,"Id");
				this.setValue(v+"/"+cr[this._idField] );
			}else if (mainValue) {
				this.setValue(cr[this._idField] + "/" + mainValue);
			} else if (title) {
				this.setValue(cr[this._idField] + "/" + title);
			} else {
				this.setValue(cr[this._idField] + "/Id");
			}
		},
		_setValue:function(formModel, name,cr){
			var fieldData = this._formData[name];
			console.log("setting:" + name + "->" + cr[name]+"/"+fieldData.readonly);
			if (fieldData.type == "DateField" ||fieldData.type == "DateTimeField") {
				var d = new Date();
				d.setTime(cr[name]);
				formModel.set(name, d);
			}else{
				formModel.set(name, cr[name]);
			}
		},
		__maskedEval: function (scr, env,def) {
			try{
				return (new Function("with(this) { return " + scr + "}")).call(env);
			}catch(e){
				console.log("RelatedTo.__maskedEval:"+scr);
				console.log("\t:"+e);
			}
			return def;
		},
		_getRecordTitle: function (map) {
			var names = ["name", "title", "shortname", "shortname_company", "shortname_person", "name1"];
			for (var i = 0; i < names.length; i++) {
				if (map[names[i]]) {
					return map[names[i]];
				}
			}
			return null;
		},

		_parseFillin:function(str) {
			var line = [];
			var quote = false;  

			for (var  col = 0,c = 0; c < str.length; c++) {
					var cc = str[c], nc = str[c+1];        
					line[col] = line[col] || '';   				
					if (cc == '"' && quote && nc == '"') { line[col] += cc; ++c; continue; }  
					if (cc == '"') { quote = !quote; continue; }
					if (cc == ',' && !quote) { ++col; continue; }
					line[col] += cc;
			}
			console.log("line:"+JSON.stringify(line,null,2));
			var ret = {};
			for(var i=0; i< line.length;i++){
				var col = line[i];
				var k;
				if( (k=col.indexOf(":"))!=-1){
					ret[col.substring(0,k)]={expr:col.substring(k+1)};
				}else{
					ret[col]={expr:true};
				}
			}
			console.log("ret:"+JSON.stringify(ret,null,2));
			return ret;
		},
		_doLayout: function (parent, upperTabView, bottomTabView) {
			var splitpane = new qx.ui.splitpane.Pane("vertical");
			splitpane.setHeight(parent.getHeight());
			splitpane.setDecorator(null);

			var topWidget = upperTabView;
			topWidget.setDecorator(null);
			topWidget.setMinHeight(150);
			splitpane.add(topWidget, 3);

			var bottomWidget = bottomTabView;
			bottomWidget.setDecorator(null);
			bottomWidget.setMinHeight(250);
			splitpane.add(bottomWidget, 5);
			return splitpane;
		},
		_createWindow: function (name) {
			var win = new qx.ui.window.Window(name, "").set({
				resizable: true,
				useMoveFrame: true,
				useResizeFrame: true
			});
			win.setLayout(new qx.ui.layout.Grow);
			win.setWidth(700);
			win.setHeight(500);
			win.setAllowMaximize(false);
			win.setAllowMinimize(false);
			win.setModal(true);
			win.setActive(false);
			win.minimize();
			win.center();
			return win;
		}
	}
});
