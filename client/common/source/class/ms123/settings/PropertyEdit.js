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
qx.Class.define("ms123.settings.PropertyEdit", {
	extend: qx.ui.container.Composite,

	/**
	 *****************************************************************************
	 CONSTRUCTOR
	 *****************************************************************************
	 */

	construct: function (facade) {
		this.base(arguments);
		this._facade = facade;
		this._model = this._facade.model;
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

	members: {
		__init:function(){
			this.setLayout(new qx.ui.layout.Dock());
			var form =  this._createEditForm();
			this.add( form, {edge:"center"});
			var toolbar =  this._createToolbar();
			this.add( toolbar, {edge:"south"});
			this._load();
		},
		_createEditForm: function () {
		},
		_getModelPath: function (model) {
			var path = [];
			path.push( model.getId() );
			while(model.parent){
				model = model.parent;
				path.push( model.getId() );
			}
			path= path.reverse();
			path.splice(0,1);
			return path.join("/");
		},
		_load: function () {
			var resourceid = this._model.getId();
			try {
				var data = ms123.util.Remote.rpcSync("setting:getResourceSetting", {
					namespace: this._facade.storeDesc.getNamespace(),
					settingsid: this._facade.settingsid,
					resourceid: resourceid
				});
				if (data) {
console.log("data:"+JSON.stringify(data,null,2));
					this._form.setData(data);
				}
			} catch (e) {
				ms123.form.Dialog.alert("settings.views.PropertyEdit._load:" + e);
			}
		},
		_save: function () {
			var resourceid = this._model.getId();
			var data = this._form.getData();
			try {
				ms123.util.Remote.rpcSync("setting:setResourceSetting", {
					namespace: this._facade.storeDesc.getNamespace(),
					settingsid: this._facade.settingsid,
					resourceid: resourceid,
					settings: data
				});
				ms123.form.Dialog.alert(this.tr("settings.properties_saved"));
				ms123.config.ConfigManager.clearCache();
			} catch (e) {
				ms123.form.Dialog.alert("settings.views.PropertyEdit._save:" + e);
			}
		},
		_createToolbar: function () {
			var toolbar = new qx.ui.toolbar.ToolBar();
			toolbar.setSpacing(5);
			toolbar.addSpacer();
			var buttonSave = new qx.ui.toolbar.Button(this.tr("meta.lists.savebutton"), "icon/16/actions/document-save.png");
			buttonSave.setToolTipText(this.tr("meta.lists.fs.save"));
			buttonSave.addListener("execute", function () {
				this._save();
			}, this);
			toolbar._add(buttonSave);
			return toolbar;
		}
	}
});
