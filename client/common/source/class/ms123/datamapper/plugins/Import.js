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
 * @ignore(Hash)
 * @ignore(Clazz)
 * @ignore(jQuery)
 * @ignore(jsPlumb.*)
 * @ignore(Clazz.extend)
 */

qx.Class.define("ms123.datamapper.plugins.Import", {
	extend: ms123.datamapper.plugins.Preview,
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (facade, config) {
		this.base(arguments,facade,config);
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
		_addButton:function(){
			var upload_msg = this.tr("datamapper.load_import_file");
			var execute_msg = this.tr("datamapper.execute");
			var execute_preview_msg = this.tr("datamapper.execute_preview");
			var group = "2";
			this._facade.offer({
				name: upload_msg,
				description: upload_msg,
				icon: "resource/ms123/upload_icon.gif",
				functionality: this.uploadFile.bind(this),
				group: group,
				index: 1,
				isEnabled: qx.lang.Function.bind(function () {
					return true;
				}, this)
			});
			this._facade.offer({
				name: execute_preview_msg,
				description: execute_preview_msg,
				icon: "resource/ms123/preview.png",
				functionality: this.execute_preview.bind(this),
				group: group,
				index: 2,
				isEnabled: qx.lang.Function.bind(function () {
					return this._uploaded === true;
				}, this)
			});
			this._facade.offer({
				name: execute_msg,
				description: execute_msg,
				icon: "resource/ms123/run_exec.png",
				functionality: this.execute_import.bind(this),
				group: group,
				index: 3,
				isEnabled: qx.lang.Function.bind(function () {
					return this._uploaded === true;
				}, this)
			});
		},
		createUploadWindow:function(id){
			return new ms123.datamapper.edit.ImportUploadWindow(this._facade,{id:id}, this.tr("datamapper.upload_file"));
		},
		_execute: function (withoutSave) {
			var rpcParams = {
				storeId: this._facade.storeDesc.getStoreId(),
				withoutSave:withoutSave,
				importingid: this._facade.importingid
			};
			var params = {
				service: "importing",
				method: "doImport",
				parameter: rpcParams,
				async: false,
				context: this
			}
			try {
				var map = ms123.util.Remote.rpcAsync(params);
				var format = this._facade.getConfig().output.format;
				if( format == ms123.datamapper.Config.FORMAT_JSON){
					//map = JSON.stringify(map,null,2);
				}
				if(!qx.lang.Type.isString(map)){
					map = JSON.stringify(map,null,2);
				}
				this._msgArea.setValue( map );
			} catch (details) {
				var msg = details.message;
				console.log(details.stack);
				msg = msg.replace(/Application error 500/g, "");
				msg = msg.replace(/ImportingService.doImport:/g, "");
				ms123.form.Dialog.alert("<b>Error</b>"+msg);
			}
		},
		execute_import: function (e) {
			this._execute(false);
		},
		execute_preview: function (e) {
			this._execute(true);
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {
	}

});
