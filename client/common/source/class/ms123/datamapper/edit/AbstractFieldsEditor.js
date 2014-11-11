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

qx.Class.define("ms123.datamapper.edit.AbstractFieldsEditor", {
	extend: qx.core.Object,
	implement: [qx.ui.form.IStringForm, qx.ui.form.IForm],
	include: [qx.ui.form.MForm, qx.locale.MTranslation],
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (facade, context, title, data) {
		this.base(arguments,facade,context);
		this._title = title;
		this._facade = facade;
		this._data = data;
		if( context){
			this._format = context.config.format;
		}else{
			this._format = facade.format;
		}
		this._init();
		var self = this;
		this._form.getValidationManager().addListener("changeValid", function (ev) {
				console.log("FieldsEditor.changeValid:"+ev);
				self._buttonOk.setEnabled(self._form.validate());
			}, this);

		if( data)this.setValue(data);
	},

	/******************************************************************************
	 EVENTS
	 ******************************************************************************/
	events: {
		"changeValue": "qx.event.type.Data",
		"changeEnabled": "qx.event.type.Data"
	},

	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		_init:function(){
			this._window = this._createWindow(this._title);
			this._form = this._createForm(this._facade);
			this._window.add(this._form, {
				edge: "center"
			});
			var buttons = this._createButtons();
			this._window.add(buttons, {
				edge: "south"
			});
			this._window.open();
		},
		getForm:function(){
			return this._form;
		},
		_createForm: function (facade) {},
		setEnabled: function () {},
		getEnabled: function () {return true;},
		resetValue: function () {},
		getValue: function () {
			return this._form.getData();
		},
		setValue: function (value) {
			if( value != null){
				this._form.setData(value);
			}
		},
		_handleOkButton: function (e) {
			var self = this;
			var ok = true;
			if (!this._form.validate()) return;
			var data = this.getValue();
			this.fireDataEvent("changeValue", data, null);
			if( this._window) this._window.close();
			//this._form.setData({});
		},
		_createButtons: function () {
			var toolbar = new qx.ui.toolbar.ToolBar();
			toolbar.setSpacing(5);
			toolbar.addSpacer();
			toolbar.addSpacer();

			var buttonOk = new qx.ui.toolbar.Button(this.tr("Ok"), "icon/16/actions/dialog-ok.png");
			buttonOk.addListener("execute", function (e) {
				this._handleOkButton(e);
			}, this);
			toolbar._add(buttonOk)
			this._buttonOk = buttonOk;
			this._buttonOk.setEnabled(false);

			var buttonCancel = new qx.ui.toolbar.Button(this.tr("Cancel"), "icon/16/actions/dialog-close.png");
			buttonCancel.addListener("execute", function () {
				if( this._window)this._window.close();
			}, this);
			toolbar._add(buttonCancel)

			return toolbar;
		},
		_createWindow: function (name) {
			var win = new qx.ui.window.Window(name, "").set({
				resizable: true,
				useMoveFrame: true,
				useResizeFrame: true
			});
			win.setLayout(new qx.ui.layout.Dock);
			win.setWidth(450);
			win.setHeight(400);
			win.setAllowMaximize(false);
			win.setAllowMinimize(false);
			win.setModal(true);
			win.setActive(false);
			win.minimize();
			win.center();
			return win;
		},
		_getFieldEditConfig: function () {
			var items = [{
				"id": "icon",
				"name": "",
				"type": "icon",
				"value": "",
				"width": 40,
				"optional": false
			},
			{
				"id": "name",
				"name": "Name",
				"type": ms123.oryx.Config.TYPE_STRING,
				"value": "field_",
				"initialFunction": function (context) {
					return "field_"+context.counter;
				},
				"validationFunction": function (value, origValue) {
					if (value.match(/^[A-Za-z]([-0-9A-Za-z_]){0,48}$/)) {
						return value;
					} else {
						return origValue;
					}
				},
				"width": 200,
				"optional": false
			},
			{
				"id": "fieldType",
				"name": this.tr("datamapper.type"),
				"type": ms123.oryx.Config.TYPE_CHOICE,
				"value": "string",
				"width": 120,
				"items": this._getDatatypes()
			}];
			if (this._format == "fw") {
				var item = {
					"id": "fieldWidth",
					"name": this.tr("datamapper.width"),
					"type": ms123.oryx.Config.TYPE_STRING,
					"value": "10",
					"validationFunction": function (value, origValue) {
						if (value.match(/^[0-9]{1,3}$/)) {
							return value;
						} else {
							return origValue;
						}
					},
					"width": 80,
					"optional": false
				}
				items.push(item)
			}
			return items;
		},
		_getNodetypes: function () {
			var items = [{
				"title": "Element",
				"value": ms123.datamapper.Config.NODETYPE_ELEMENT 
			},
			{
				"title": "List<Element>",
				"value": ms123.datamapper.Config.NODETYPE_COLLECTION 
			}]
			items.each(function(item){
				item.label = item.title;
			});
			return items
		},
		_getDatatypes: function () {
			var items = [{
				"title": "String",
				"value": "string"
			},
			{
				"title": "Calendar",
				"value": "calendar"
			},
			{
				"title": "Date",
				"value": "date"
			},
			{
				"title": "Boolean",
				"value": "boolean"
			},
			{
				"title": "Decimal",
				"value": "decimal"
			},
			{
				"title": "Double",
				"value": "double"
			},
			{
				"title": "Long",
				"value": "long"
			},
			{
				"title": "Integer",
				"value": "integer"
			}/*,
			{
				"title": "Byte",
				"value": "byte"
			},
			{
				"title": "List<String>",
				"value": "list_string"
			},
			{
				"title": "List<Calendar>",
				"value": "list_calendar"
			},
			{
				"title": "List<Date>",
				"value": "list_date"
			},
			{
				"title": "List<Boolean>",
				"value": "list_boolean"
			},
			{
				"title": "List<Decimal>",
				"value": "list_decimal"
			},
			{
				"title": "List<Double>",
				"value": "list_double"
			},
			{
				"title": "List<Long>",
				"value": "list_long"
			},
			{
				"title": "List<Integer>",
				"value": "list_integer"
			},
			{
				"title": "List<Byte>",
				"value": "list_byte"
			}*/
			];
			if (this._format == "xml" || this._format == "json") {
				items.push({
					"title": "Element",
					"value": ms123.datamapper.Config.NODETYPE_ELEMENT 
				});
				items.push({
					"title": "List<Element>",
					"value": ms123.datamapper.Config.NODETYPE_COLLECTION 
				});

			}
			items.each(function(item){
				item.label = item.title;
			});
			return items
		},
		_fieldsFormData:function(){	
			return  {
					'rowflex':1,
					'type': "ComplexEdit",
					'caption': this.tr("datamapper.children_fields"),
					'config':this._getFieldEditConfig(),
					'validation': {
						required: true,
						"validator": function (value, item) {
							if( item.isExcluded()){
								return true;
							}
							return value.length>0;
						}
					},
					'value': []
				}
		},
		__createForm:function(formData, layout){
			var form = new ms123.form.Form({
				"tabs": [{
					id: "tab1",
					layout: layout,
					lineheight: 22
				}],
				"formData": formData,
				"buttons": [],
				"useScroll":false,
				"callback": function (m, v) {},
				"inWindow": false,
				"render": true
			});
			return form;
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
