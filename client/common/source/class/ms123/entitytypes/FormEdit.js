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
	* @ignore(Clazz.extend)
*/
qx.Class.define("ms123.entitytypes.FormEdit", {
	extend: qx.ui.container.Composite,
	include: [qx.locale.MTranslation],

	/**
	 *****************************************************************************
	 CONSTRUCTOR
	 *****************************************************************************
	 */

	construct: function (model, param, facade) {
		this.base(arguments);
		this._model = model;
		this._facade = facade;
		this._isNew = param.isNew;
		this._mode = param.mode;
		console.log("isNew:" + this._isNew+"/Mode:"+this._mode);
		this.__init();
	},

	/**
	 *****************************************************************************
	 PROPERTIES
	 *****************************************************************************
	 */
	/**
	 *****************************************************************************
	 MEMBERS
	 *****************************************************************************
	 */

	/**
	 *****************************************************************************
	 EVENTS
	 *****************************************************************************
	 */
	events: {
		"changeValue": "qx.event.type.Data"
	},
	members: {
		__init: function () {
			this.setLayout(new qx.ui.layout.Dock());
			var form = this._createEditForm();
			this.add(form, {
				edge: "center"
			});
			var toolbar = this._createToolbar();
			this.add(toolbar, {
				edge: "south"
			});
			if (!this._isNew) {
				this._load();
			}
		},
		_prepareFormModel: function (model) {},
		_enumCallback: function (context) {
			var Item = Clazz.extend({
				construct: function (context) {
					this._context = context;
				},
				id: function () {
					return this._context.id;
				},
				width: function () {
					return this._context.width;
				},
				name: function () {
					return this._context.name;
				},
				type: function () {
					return this._context.type;
				},
				items: function () {
					return this._context.items;
				},
				title: function () {
					return this._context.title;
				},
				config: function () {
					return this._context.config;
				},
				value: function () {
					return this._context.value;
				}
			})
			var formModel = context.form.getModel();
			console.log("_enumCallback:" + context.form + "/" + context.key + "/" + context.action);
			if( context.action == "setvalue"){
				if( context.value && context.value!=""){
					var v = qx.lang.Json.parse(context.value);
					context.button.setLabel( v.enumDescription );
				}else{
					context.button.setLabel( null);
				}
				context.button.setCenter(false);
				return;
			}
			var config = {
				"helperTree": ["sw.enum", "sw.filter"],
				"kind": "enum"
			};
			var title = "EnumSelect";
			var complexItems = [new Item({
				"id": "colname",
				"name": "Columnname",
				"name_de": "Spaltenname",
				"type":  ms123.oryx.Config.TYPE_STRING,
				"value": "",
				"width": 150,
				"optional": false
			}), new Item({
				"id": "mapping",
				"name": "Mapping",
				"name_de": "Mapping",
				"readonly": false,
				"type": ms123.oryx.Config.TYPE_CHOICE,
				"value": "value",
				"items": [ new Item({
					"id": "empty",
					"title": "",
					"value": null
				}), 
					new Item({
					"id": "value",
					"title": "value",
					"value": "value"
				}), 
					new Item({
					"id": "label",
					"title": "label",
					"value": "label"
				}), new Item({
					"id": "tooltip",
					"title": "tooltip",
					"value": "tooltip"
				})],
				"width": 80,
				"optional": false
			})];

			var data= formModel.get("selectable_items");
			var cl = new ms123.graphicaleditor.plugins.propertyedit.EnumWindow(config, title, complexItems, null, this._facade,data );
			cl.addListener('changeValue', (function (e) {
				formModel.set("selectable_items", e.getData());
			}).bind(this))
		},
		_createEditForm: function () {
			var context = {};
			context.buttons = {};
			var storeDesc = ms123.StoreDesc.getGlobalMetaStoreDesc();
			var cm = new ms123.config.ConfigManager();
			context.model = cm.getEntityModel(this._mode, storeDesc, "main-form", "properties");

			var cols = context.model.attr("colModel");
			for (var i = 0; i < cols.length; i++) {
				var col = cols[i];
				if (col.name == "selectable_items") {
					col.edittype = "actionbutton";
					col.callback = this._enumCallback.bind(this);
					col.action = null;
					col.buttonlabel = " ";
					col.iconname = "enum.png";
				}
			}
			this._prepareFormModel(context.model);
			var value = qx.lang.Json.stringify(context.model.attr("colModel"), null, 4);
			console.log("tree:" + value);
			context.unit_id = "";
			context.config = this._mode;
			context.storeDesc = storeDesc;
			var form = new ms123.widgets.Form(context);
			form.setData({});
			this._form = form;
			return form;
		},
		_deleteIsOk: function (data) {
			return null;
		},
		_load: function () {},
		_delete: function (data) {},
		_save: function (data) {},
		_saveForm: function () {
			var validate = this._form.validate();
			console.error("validate:" + validate);
			if (!validate) {
				var vm = this._form.form._form.getValidationManager();
				var items = vm.getInvalidFormItems();
				var message = "<br />";
				for (var i = 0; i < items.length; i++) {
					var name = items[i].getUserData("key");
					var msg = items[i].getInvalidMessage();
					message += name + " : " + msg + "<br />";
				}
				ms123.form.Dialog.alert(this.tr("entitytypes.form_incomplete") + ":" + message);
				return;
			}
			var data = this._form.getData();
			if (this._isNew) {
				var children = this._model.getChildren();
				if (this._isDup(children, data)) {
					ms123.form.Dialog.alert(this.tr("entitytypes.name_exists"));
					return;
				}
			}
			console.log("_form:" + qx.util.Serializer.toJson(this._form.getData()));
			this._save(data);
		},
		_confirmDelete:function(){
			var ce = new ms123.form.Confirm({
				"message": this.tr("entitytypes.confirm_delete"),
				"warn": true,
				"callback": function (ce) {
					console.log("ce:" + ce);
					if (ce) {
						this._delete(this._form.getData());
						this.setEnabled(false);
					} else {
						console.log("nicht Löschen");
					}
				},
				"context": this,
				"inWindow": true
			});
			ce.setWidth(400);
			ce.show();
		},
		_createToolbar: function () {
			var toolbar = new qx.ui.toolbar.ToolBar();
			var buttonUpdateDb = new qx.ui.toolbar.Button(this.tr("entitytypes.update_db"), "icon/16/actions/object-rotate-right.png");
			buttonUpdateDb.addListener("execute", function () {
				try {
					ms123.util.Remote.rpcSync("domainobjects:createClasses", {
						storeId: this._getStoreId()
					});
					ms123.form.Dialog.alert(this.tr("entitytypes.update_db_successfull"));
				} catch (e) {
					ms123.form.Dialog.alert("FormEdit.updateDb:" + e);
					return;
				}
			}, this);
			toolbar._add(buttonUpdateDb);
			toolbar.setSpacing(5);
			toolbar.addSpacer();

			var buttonDel = new qx.ui.toolbar.Button(this.tr("entitytypes.delete"), "icon/16/places/user-trash.png");
			buttonDel.addListener("execute", function () {
				var x = this._deleteIsOk(this._form.getData());
				if (x) {
					ms123.form.Dialog.alert(this.tr("entitytypes.delete_not_possible") + ":" + x);
					return
				}
				this._confirmDelete();
			}, this);
			toolbar._add(buttonDel);
			if (this._isNew) {
				buttonDel.setEnabled(false);
			}
			this._buttonDel = buttonDel;

			var buttonSave = new qx.ui.toolbar.Button(this.tr("meta.lists.savebutton"), "icon/16/actions/document-save.png");
			buttonSave.setToolTipText(this.tr("meta.lists.fs.save"));
			buttonSave.addListener("execute", function () {
				this._saveForm();
			}, this);
			toolbar._add(buttonSave);
			return toolbar;
		},
		_getStoreId: function () {
			var storeId = this._facade.storeDesc.getStoreId();
			if (this._model.getPack() == "aid") {
				storeId = ms123.StoreDesc.getGlobalMetaStoreDesc().getStoreId();
			}
			return storeId;
		},
		_isDup: function (array, data) {
			var name = data.name
			var lname = name.toLowerCase();
			var len = array.getLength();
			for (var i = 0; i < len; i++) {
				var n = array.getItem(i).getId().toLowerCase();
				if (n == lname) return true;
			}
			return false;
		}
	}
});
