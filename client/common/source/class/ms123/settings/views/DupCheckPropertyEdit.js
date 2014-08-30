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
qx.Class.define("ms123.settings.views.DupCheckPropertyEdit", {
	extend: ms123.settings.BaseTableEdit,

	/**
	 *****************************************************************************
	 CONSTRUCTOR
	 *****************************************************************************
	 */

	construct: function (facade) {
		this.facade = facade;
		this.base(arguments, facade, "dupcheck");
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
		_init:function(){
			this._model = this.facade.model;
			var id = this._model.getId();
			var namespace = this.facade.storeDesc.getNamespace();
			var p = id.split(".");
			var entity = p[1];
			console.log("entity:" + entity);

			try {
				var mapping = {
					value: "name"
				};
				var filter = 'datatype=="string"||datatype=="text"';
				var cm = new ms123.config.ConfigManager();
				this._selectableFields = cm.getFields(this.facade.storeDesc,entity, false, false,filter,mapping);
				for (var i = 0; i < this._selectableFields.length; i++) {
					var field = this._selectableFields[i];
					field.label = this.tr("data." + entity + "." + field.value);
				}

			} catch (e) {
				ms123.form.Dialog.alert("settings.DupcheckProperties._init:" + e);
				return;
			}
		},
		prepareColumns: function (columns) {
			for (var i = 0; i < columns.length; i++) {
				var col = columns[i];
				if (col.name == "fields") {
					col.selectable_items = this._selectableFields;
					col.mainform_tab = "tab2";
				}
			}
		},
		_getFormLayout:function(){
			return "tab1;tab2:full";
		},
		_prepareFormFields:function(form){
			var items = form.form.getItems();
			//items["dupname"].setFilter("[a-z0-9A-z.]");
		},
		_onDblClick:function(e){
			this._mode = "edit";
			var map = this._table.getCurrentRecord();
			this._editForm.fillForm(map);
			this._editWindow.setActive(true);
			this._editWindow.open();
		},
		_load:function(){
			var fieldsets = null;
			try {
				var resourceid = this._model.getId();
				console.log("resourceid:" + resourceid);
				fieldsets = ms123.util.Remote.rpcSync("setting:getResourceSetting", {
					namespace: this.facade.storeDesc.getNamespace(),
					settingsid: this.facade.settingsid,
					resourceid: resourceid
				});
				if( fieldsets ){
					fieldsets = fieldsets.fieldsets;
				}else{
					fieldsets = [];
				}
				this.setValueAsList(fieldsets);
			} catch (e) {
				ms123.form.Dialog.alert("settings.DupcheckProperties.load:" + e);
				return;
			}
		},
		_save:function(){
			var fieldsets = { fieldsets : this.getValueAsList()}
			try {
				ms123.util.Remote.rpcSync("setting:setResourceSetting", {
					namespace: this.facade.storeDesc.getNamespace(),
					settingsid: this.facade.settingsid,
					resourceid: this._model.getId(),
					settings: fieldsets
				});
				ms123.form.Dialog.alert(this.tr("settings.dupcheck_saved"));
			} catch (e) {
				ms123.form.Dialog.alert("settings.DupcheckProperties._save:" + e);
			}
		}
	}
});
