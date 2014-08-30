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

qx.Class.define("ms123.shell.views.StencilEditor", {
	extend: qx.ui.container.Composite,
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (config,value) {
		this.base(arguments);
		this.config = config||{};
		var layout = new qx.ui.layout.Dock();
		this.setLayout(layout);
		this.add(this._createEditor(config,value),{edge:"center"});
		this.add(this._createButtons(),{edge:"south"});

		this._form.getValidationManager().addListener("changeValid", function (ev) {
			console.log("StencilEditor.changeValid:"+ev);
			this._buttonSave.setEnabled(this._form.validate());
		}, this);
		var value = JSON.parse(value);
		var groups = value.groups;
		if( groups && groups.length>0){
			value.group = groups[0];
		}
		this._form.setData(value);
		this._form.validate();
	},

	/******************************************************************************
	 EVENTS
	 ******************************************************************************/
	events: {
		"save": "qx.event.type.Data"
	},


	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},

	/******************************************************************************
	 STATICS
	 ******************************************************************************/
	statics: {},
	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		_createEditor:function(config, value){
			console.log("Value:"+value);
			this._createForm();
			return this._form;
		},

		_createButtons: function () {
			var toolbar = new qx.ui.toolbar.ToolBar();
			toolbar.setSpacing(5);

			var buttonSave = new qx.ui.toolbar.Button(this.tr("Save"), "icon/16/actions/dialog-apply.png");
			buttonSave.addListener("execute", function () {
				if (!this._form.validate()){
					console.log("Not valid");
					 return;
				}
				var data = this._form.getData();
				this._setDefaultValues(data);
				console.log("Data:"+JSON.stringify(data,null,2));
				this.fireDataEvent("save", JSON.stringify(data,null,2));
			}, this);
			buttonSave.setEnabled(false);
			toolbar._add(buttonSave)

			toolbar.addSpacer();
			toolbar.addSpacer();

			this._buttonSave = buttonSave;
			return toolbar;
		},
		_createForm: function () {
			var formData = {
				"id":{
					'type': "Textfield",
					'label': "Id",
					'value': "",
					'validation': {
						required: true,
						filter:/[a-zA-Z0-9_]/,
						validator: "/^[A-Za-z]([0-9A-Za-z_]){2,32}$/"
					}
				},
				"title":{
					'type': "Textfield",
					'label': this.tr("stencileditor.title"),
					'value': ""
				},
				"description":{
					'type': "Textfield",
					'label': this.tr("stencileditor.description"),
					'value': ""
				},
				"group":{
					'type': "Textfield",
					'label': this.tr("stencileditor.group"),
					'value': "",
					'validation': {
						required: true,
						filter:/[a-zA-Z0-9_]/,
						validator: "/^[A-Za-z]([0-9A-Za-z_]){2,32}$/"
					}
				},
				"routing":{
					'type': "Textarea",
					'header': this.tr("stencileditor.routes"),
					'lines':10,
					'value': ""
				},
				"properties":  {
						'rowflex':1,
						'type': "ComplexEdit",
						'caption': this.tr("stencileditor.properties"),
						'config':this._getRowConfig(),
						'validation': {
							required: false,
							"validator": function (value, item) {
								return true;//value.length>0;
							}
						},
						'value': []
					}
				/*"properties":{
					'type': "GridInput",
					'label': this.tr("stencileditor.properties"),
					"config": {
          		"totalCount": 2,
          		"items": [
								{
									"colname": "id",
									"display": "Id",
									"type": "text",
									'validation': {
										required: true,
										filter:/[a-zA-Z0-9_]/,
										validator: "/^[A-Za-z]([0-9A-Za-z_]){0,32}$/"
									}
								},
								{
									"colname": "title",
									"display": this.tr("stencileditor.title"),
									"type": "text"
								},
								{
									"colname": "description",
									"display": this.tr("stencileditor.description"),
									"type": "text"
								}
							]
					},
					'value': ""
				},*/
			}
			var context = {};
			context.formData = formData;
			context.buttons = [];
			context.formLayout = [{
				id: "tab1", lineheight:-1
			}];
			var form = new ms123.widgets.Form(context);
			this._form = form;
			return this._form;
		},
		_getRowConfig: function () {
			var items = [ {
				"id": "id",
				"name": "Id",
				"type": "string",
				"value": "prop_",
				"initialFunction": function (context) {
					return "prop_"+context.counter;
				},
				"validationFunction": function (value, origValue) {
					if (value.match(/^[A-Za-z]([0-9A-Za-z_]){0,48}$/)) {
						return value;
					} else {
						return origValue;
					}
				},
				"width": 100,
				"optional": false
			}, {
				"id": "description",
				"name": this.tr("stencileditor.description"),
				"validationFunction": function (value, origValue) {},
				"type": "string",
				"value": "",
				"width": 200,
				"optional": false
			} ];
			return items;
		},
		_setDefaultValues:function(data){
			data.type = "node";
			data.view = "activity/servicetask.svg";
			data.icon = "activity/list/type.service.png";
			data.groups = [ "TestActivities" ];
			data.propertyPackages = [ "elementbase", "camelbase", "baseAttributes", "bgColor", "borderColor", "asynchronousbase", "executionlistenersbase", "loopcharacteristics", "activity" ];
			data.roles = [ "sequence_start", "Activity", "sequence_end", "ActivitiesMorph", "all" ];
			data.groups = [ data.group ];
			delete data.group;
			for(var i=0; i< data.properties.length;i++){
				var p = data.properties[i];
				p.type= "String";
				p.value= "";
				p.readonly= false;
				p.optional= true;
			}
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
