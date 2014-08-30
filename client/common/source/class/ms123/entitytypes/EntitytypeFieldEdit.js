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
qx.Class.define("ms123.entitytypes.EntitytypeFieldEdit", {
	extend: ms123.entitytypes.FormEdit,

	/**
	 *****************************************************************************
	 CONSTRUCTOR
	 *****************************************************************************
	 */

	construct: function (model,param,facade,data) {
		if( data ){
			this.__noLoad = true;
		}
		this.base(arguments,model,param,facade);
		if( data ){
			this._setData(data);
			this._buttonDel.setEnabled(false);
		}
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
		_saveEntitytypeField: function (entitytype, name, data) {
			var completed = (function (data) {
				ms123.form.Dialog.alert(this.tr("entitytypes.field_saved"));
			}).bind(this);

			var failed = (function (details) {
				ms123.form.Dialog.alert(this.tr("entitytypeFields.saveEntitytypeField_failed")+":"+details.message);
			}).bind(this);

			try {
				var storeId = this._facade.storeDesc.getStoreId();
				var ret = ms123.util.Remote.rpcSync("entity:saveEntitytypeField", {
					storeId: this._getStoreId(),
					data:data,
					entitytype:entitytype,
					name: name
				});
				completed.call(this,ret);
				ms123.config.ConfigManager.clearCache();
			} catch (e) {
				failed.call(this,e);
				return;
			}
		},
		_getEntitytypeField: function (entitytype,name) {
			var completed = (function (data) {
			}).bind(this);

			var failed = (function (details) {
				ms123.form.Dialog.alert(this.tr("entitytypeFields.getEntitytypeField_failed")+":"+details.message);
			}).bind(this);

			try {
				var storeId = this._facade.storeDesc.getStoreId();
				var ret = ms123.util.Remote.rpcSync("entity:getEntitytypeField", {
					storeId: this._getStoreId(),
					entitytype:entitytype,
					name: name
				});
				completed.call(this,ret);
				return ret;
			} catch (e) {
				failed.call(this,e);
				return;
			}
		},
		_deleteEntitytypeField: function (entitytype,name) {
			var failed = (function (details) {
				ms123.form.Dialog.alert(this.tr("entitytypeFields.deleteEntitytypeField_failed")+":"+details.message);
			}).bind(this);
			try {
				var storeId = this._facade.storeDesc.getStoreId();
				var ret = ms123.util.Remote.rpcSync("entity:deleteEntitytypeField", {
					storeId: this._getStoreId(),
					entitytype:entitytype,
					name: name
				});
			} catch (e) {
				failed.call(this,e);
			}
		},
		_load: function () {
			if( !this.__noLoad){
				var data = this._getEntitytypeField( this._model.getEntitytype(), this._model.getId() );
				this._form._mode = this._isNew ? "add" : "edit";
				this._form.setData(data);
			}
		},
		_setData: function (data) {
			this._form._mode = this._isNew ? "add" : "edit";
			this._form.setData(data);
		},
		_delete:function(data){
			console.log("delete.data:"+qx.util.Serializer.toJson(data));
			var entityType = this._model.parent.getId();
			var children = this._model.parent.getChildren();
			var len = children.getLength();
			console.log("len:"+len);
			for(var i=0; i < len; i++){
				var child = children.getItem(i);
				console.log("\tname:"+child.getId());
				if( child.getId() == data.name){
					children.remove(child);
					this._deleteEntitytypeField(entityType,data.name);
					break;
				}
			}
		},
		_save:function(data){
			var entitytype = this._isNew ? this._model.getId() : this._model.getEntitytype();
			this._saveEntitytypeField( entitytype, data.name, data );
			if( this._isNew ){
				var f = {}
				f.id = data.name;
				f.value = data.name;
				f.entitytype = entitytype;
				f.title = data.name;
				f.type = "sw.field";
				f.pack = this._model.getPack();
				f.children = [];
				var fmodel = qx.data.marshal.Json.createModel(f, true);
				var children = this._model.getChildren();
				fmodel.parent = this._model;
				children.insertAt(0,fmodel);
			}
			this.fireDataEvent("changeValue", data, null);
		}
	}
});
