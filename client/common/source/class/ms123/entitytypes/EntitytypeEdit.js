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
qx.Class.define("ms123.entitytypes.EntitytypeEdit", {
	extend: ms123.entitytypes.FormEdit,

	/**
	 *****************************************************************************
	 CONSTRUCTOR
	 *****************************************************************************
	 */

	construct: function (model,param,facade) {
		this.base(arguments,model,param,facade);
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

	members: {
		_saveEntitytype: function (name, data) {
			var completed = (function (data) {
				ms123.form.Dialog.alert(this.tr("entitytypes.entitytype_saved"));
			}).bind(this);

			var failed = (function (details) {
				ms123.form.Dialog.alert(this.tr("entitytypes.saveEntitytype_failed")+":"+details.message);
			}).bind(this);

			try {
				var ret = ms123.util.Remote.rpcSync("entity:saveEntitytype", {
					storeId: this._getStoreId(),
					data:data,
					name: name
				});
				completed.call(this,ret);
				ms123.config.ConfigManager.clearCache();
			} catch (e) {
				failed.call(this,e);
				return;
			}
		},
		_getEntitytype: function (name) {
			var completed = (function (data) {
			}).bind(this);

			var failed = (function (details) {
				ms123.form.Dialog.alert(this.tr("entitytypes.getEntitytype_failed")+":"+details.message);
			}).bind(this);

			try {
				var ret = ms123.util.Remote.rpcSync("entity:getEntitytype", {
					storeId: this._getStoreId(),
					name: name
				});
				completed.call(this,ret);
				return ret;
			} catch (e) {
				failed.call(this,e);
				return;
			}
		},
		_deleteEntitytype: function (name) {
			var failed = (function (details) {
				ms123.form.Dialog.alert(this.tr("entitytypes.deleteEntitytype_failed")+":"+details.message);
			}).bind(this);

			try {
				var ret = ms123.util.Remote.rpcSync("entity:deleteEntitytype", {
					storeId: this._getStoreId(),
					name: name
				});
			} catch (e) {
				failed.call(this,e);
			}
		},
		_getRelations: function () {
			var failed = (function (details) {
				ms123.form.Dialog.alert(this.tr("entitytypes.getRelations_failed") + ":" + details.message);
			}).bind(this);

			try {
				var storeId = this._facade.storeDesc.getStoreId();
				var ret = ms123.util.Remote.rpcSync("entity:getRelations", {
					storeId: this._getStoreId()
				});
				return ret;
			} catch (e) {
				return [];
			}
		},
		_deleteIsOk:function(data){
			var relations = this._getRelations();
			var etname = this._facade.storeDesc.getPack()+"."+data.name;
			for( var i=0; i< relations.length;i++){
				var rel = relations[i];
				if( rel.leftmodule == etname || rel.rightmodule == etname){
					return this.tr("entitytypes.entitytype_exists_in_relations");
				}
			}
			return null;
		},

		_confirmDelete: function () {
			var buttons = [{
				'label': this.tr("entitytypes.delete_class"),
				'icon': "icon/22/actions/dialog-ok.png",
				'value': 1
			},
			{
				'label': this.tr("composite.select_dialog.cancel"),
				'icon': "icon/22/actions/dialog-cancel.png",
				'value': 2
			}];
			var formData = {
				delete_messages: {
					name: "delete_messages",
					type: "CheckBox",
					value: true,
					label: this.tr("entitytypes.delete_messages")
				},
				delete_settings: {
					name: "delete_settings",
					type: "CheckBox",
					value: true,
					label: this.tr("entitytypes.delete_settings")
				}
			};

			var self = this;
			var form = new ms123.form.Form({
				"buttons": buttons,
				"tabs": [{
					id: "tab1",
					layout: "single"
				}],
				"useScroll": false,
				"formData": formData,
				"hide": false,
				"inWindow": true,
				"callback": function (m, v) {
					if (m !== undefined) {
						form.hide();
						if (v == 1) {
							self._delete(self._form.getData(),m);
							self.setEnabled(false);
						} else if (v == 2) {}
					}
				},
				"context": self
			});
			form.show();
		},
		_load: function () {
			var data = this._getEntitytype( this._model.getId() );
			this._fields = data.fields;
			this._form._mode = this._isNew ? "add" : "edit";
			this._form.setData(data);
		},
		_delete:function(data,flags){
			console.log("delete.data:"+qx.util.Serializer.toJson(data));
			var children = this._model.parent.getChildren();
			var len = children.getLength();
			console.log("len:"+len);
			for(var i=0; i < len; i++){
				var child = children.getItem(i);
				console.log("\tname:"+child.getId());
				if( child.getId() == data.name){
					children.remove(child);

					var dm = flags.get("delete_messages");
					var ds = flags.get("delete_settings");

					var 	namespace= this._facade.storeDesc.getNamespace();
					var lang= ms123.config.ConfigManager.getLanguage();
					var ds = new ms123.entitytypes.DefaultSettings(namespace,lang);
					
					if (dm) ds.deleteMessages({name:data.name,fields:this._fields});
					if (ds) ds.deleteResources(data);	

					this._deleteEntitytype(data.name);
					break;
				}
			}
		},
		_save:function(data){
			this._saveEntitytype( data.name, data );
			if( this._isNew ){
				var e = {}
				e.id = data.name;
				e.value = data.name;
				e.title = data.name;
				e.pack = this._model.getPack();
				e.type = "sw.entitytype";
				e.children = [];
				var fmodel = qx.data.marshal.Json.createModel(e, true);
				var children = this._model.getChildren();
				fmodel.parent = this._model;
				children.insertAt(0,fmodel);
			}
		}
	}
});
