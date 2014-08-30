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

qx.Class.define("ms123.datamapper.edit.StructureEditor", {
	extend: qx.core.Object,
	implement: [qx.ui.form.IStringForm, qx.ui.form.IForm],
	include: [qx.ui.form.MForm, qx.locale.MTranslation],
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (facade, config, mappingEdit,title) {
		this.base(arguments);
		this._title = title;
		this._config = config;
		this._mappingEdit = mappingEdit;
		if (config) {
			this._format = config.format;
		} else {
			this._format = facade.format;
		}

		this._window = this._createWindow(this._title);
		this._form = this._createForm(facade);
		var formContainer = this._renderForm(this._form);
		this._window.add(formContainer, {
			edge: "center"
		});
		var buttons = this._createButtons();
		this._window.add(buttons, {
			edge: "south"
		});
		this._window.open();
		var self = this;
		this._form.getValidationManager().addListener("changeValid", function (ev) {
			console.log("StructureEditor.changeValid");
			self._buttonOk.setEnabled(self._form.validate());
		}, this);
		ms123.datamapper.StructureTree;
	},

	/******************************************************************************
	 EVENTS
	 ******************************************************************************/
	events: {
		"changeEnabled": "qx.event.type.Data",
		"changeValue": "qx.event.type.Data"
	},

	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		_createForm: function (facade) {
			var self = this;
			var formData = {
				"name": {
					'type': "TextField",
					'label': this.tr("datamapper.name"),
					'validation': {
						required: true,
						filter: /[a-zA-Z0-9_ \->]/,
						validator: "/^[A-Za-z]([0-9A-Za-z_i\\-> ]){0,64}$/"
					},
					'value': ""
				},
				"condition": {
					'type': "TextField",
					'label': this.tr("datamapper.condition"),
					'value': ""
				},
				"map2parent": {
					'type': "Checkbox",
					'label': this.tr("datamapper.map2parent"),
					'value': true
				},
				"input": {
					'type': "ms123.datamapper.StructureTree",
					'label': this.tr("datamapper.input"),
					'options': this._filter(null,this._config.input),
					'validation': {
						'required': true,
						'validator': this._validateTree.bind(self)
					},
					'value': null
				},
				"output": {
					'type': "ms123.datamapper.StructureTree",
					'label': this.tr("datamapper.output"),
					'options': this._filter(null,this._config.output),
					'validation': {
						'required': true,
						'validator': this._validateTree.bind(self)
					},
					'value': null
				}
			};
			return this.__createForm(formData, "double");
		},
		_validateTree: function (value, item) {
			console.log("thisform:"+this+"/"+this._form);
			if(this._form){
				var itree = this._form.getFormElementByKey("input");
				var otree = this._form.getFormElementByKey("output");
				if( itree.getModel() && otree.getModel()){
					var error = this._mappingEdit.validateConnection(itree.getModel(),otree.getModel());
					if( error ){
						//ms123.form.Dialog.alert(error);
						otree.setInvalidMessage(error);
						return false;
					}
				}
			}

			if (value == null) return false;
			if( value.type ==  ms123.datamapper.Config.NODETYPE_COLLECTION) return true;
			if( value.root === true) return true;
			return false;
		},
		_filter: function (parentList,model) {
			if( parentList && model.type == ms123.datamapper.Config.NODETYPE_ATTRIBUTE){
				var index = parentList.indexOf(model);
				parentList.splice(index,1);
				return;
			}
			if( parentList && model.type == ms123.datamapper.Config.NODETYPE_ELEMENT){
				if( !this._hasListChilds(model)){
					var index = parentList.indexOf(model);
					parentList.splice(index,1);
					return;
				}
			}
			for (var i = model.children.length-1; i>=0; i--) {
				var c = model.children[i];
				this._filter(model.children, c );
			}
			return model;
		},
		_hasListChilds:function(model){
			if( model.type == ms123.datamapper.Config.NODETYPE_COLLECTION){
				return true;
			}
			for (var i = model.children.length-1; i>=0; i--) {
				var c = model.children[i];
				var ret = this._hasListChilds(c );
				if( ret === true) return true;
			}
			return false;
		},
		setEnabled: function () {},
		getEnabled: function () {return true;},
		resetValue: function () {},
		getValue: function () {
			return this._form.getData();
		},
		setValue: function (value) {
			if (value != null) {
				this._form.setData(value);
			}
		},
		_handleOkButton: function (e) {
			var ok = true;
console.log("_handleOkButton:"+this._form.validate());
			if (!this._form.validate()) return;
			var data = this._form.getData();
console.log("_handleOkButton.data:"+JSON.stringify(data,null,2));
			this.fireDataEvent("changeValue", data, null);
			this._window.close();
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
				this._window.close();
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
			win.setLayout(new qx.ui.layout.Dock(2,2));
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
		_renderForm: function (form) {
			var container = new qx.ui.container.Composite(new qx.ui.layout.Dock(3,3));
			var labels = form.getLabels();
			var items = form.getItems();
			var layout = new qx.ui.layout.Grid(3, 3);
			var upper = new qx.ui.container.Composite(layout);
			layout.setColumnFlex(0, 0);
			layout.setColumnMaxWidth(0, 50);
			layout.setColumnFlex(1, 1);
			upper.add(this._createLabel(labels["name"], items["name"]), {
				row: 0,
				column: 0
			});
			upper.add(items["name"], {
				row: 0,
				column: 1
			});
			upper.add(this._createLabel(labels["condition"], items["condition"]), {
				row: 1,
				column: 0
			});
			upper.add(items["condition"], {
				row: 1,
				column: 1
			});

			upper.add(this._createLabel(labels["map2parent"], items["map2parent"]), {
				row: 2,
				column: 0
			});
			upper.add(items["map2parent"], {
				row: 2,
				column: 1
			});

			container.add(upper, {
				edge: "north"
			});

			var lower = new qx.ui.container.Composite(new qx.ui.layout.HBox(2));
			lower.add(items["input"], {
				flex: 1
			});
			lower.add(items["output"], {
				flex: 1
			});
			container.add(lower, {
				edge: "center"
			});
			var m = form.getModel();
			var props = qx.Class.getProperties(m.constructor);
			var items = form.getItems();
			for (var i = 0, l = props.length; i < l; i++) {
				var p = props[i];
				if (items[p]) items[p].setValid(true);
			}
			return container;
		},
		_createLabel: function (name, item) {
			var required = "";
			if (item.getRequired()) {
				required = " <span style='color:red'>*</span> ";
			}
			var colon = name.length > 0 || item.getRequired() ? " :" : "";
			var label = new qx.ui.basic.Label(name + required + colon);
			label.setRich(true);
			return label;
		},
		__createForm: function (formData, layout) {
			var form = new ms123.form.Form({
				"tabs": [{
					id: "tab1",
					layout: layout,
					lineheight: 22
				}],
				"formData": formData,
				"buttons": [],
				"useScroll": false,
				"callback": function (m, v) {},
				"inWindow": false,
				"render": false
			});
			return form;
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
