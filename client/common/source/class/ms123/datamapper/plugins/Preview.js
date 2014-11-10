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

qx.Class.define("ms123.datamapper.plugins.Preview", {
	extend: qx.ui.container.Composite,
	include: [qx.locale.MTranslation],
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (facade, config) {
		this.base(arguments);
		this._facade = facade;
		this._config = config;

		this._addButton();
		this._init();
		this._currentFileId = config.fileId;
		this._uploaded = config.fileId!=null;

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
			var execute_msg = this.tr("datamapper.execute_preview");
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
				name: execute_msg,
				description: execute_msg,
				icon: "resource/ms123/preview.png",
				functionality: this.execute.bind(this),
				group: group,
				index: 2,
				isEnabled: qx.lang.Function.bind(function () {
					return this._uploaded === true;
				}, this)
			});
		},
		_init:function(){
			var layout = new qx.ui.layout.Dock();
			this.setLayout(layout);

			this._mainTab = new qx.ui.tabview.TabView().set({
			});

			var page = new qx.ui.tabview.Page(this.tr("datamapper.text")).set({
				showCloseButton: false,
				decorator:null
			});
			page.setLayout(new qx.ui.layout.Dock());

			this._mainTab.add(page, {
				edge: 5
			});

			this._mainTab.addListener("changeSelection", function (e) {
			}, this);
			this.add( this._mainTab,{edge:"center"});

      this._msgArea = new qx.ui.form.TextArea();
			this._msgArea.setReadOnly(true);
      page.add(this._msgArea,{edge:"center"});
		},
		getFileId:function(){
			return this._currentFileId;
		},
		createUploadWindow:function(id){
			return new ms123.datamapper.edit.UploadWindow(this._facade,{id:id}, this.tr("datamapper.upload_file"));
		},
		uploadFile: function (e) {
			var id = ms123.util.IdGen.id();
			var ul = this.createUploadWindow(id);
			ul.addListener('ready', function (e) {
				console.log("ready:",JSON.stringify(e.getData(), null,2));
				this._uploaded = true;
				this._facade.update();
				this._currentFileId = id;
			},this);
		},
		execute: function (e) {

			var rpcParams = {
				namespace: this._facade.storeDesc.getNamespace(),
				fileId: this._currentFileId,
				config: this._facade.getConfig()
			};
			var params = {
				method: "transform",
				service: "datamapper",
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
				msg = msg.replace(/DatamapperService.preview:/g, "");
				ms123.form.Dialog.alert("<b>Error</b>"+msg);
			}
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {
	}

});
