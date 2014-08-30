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
*/
qx.Class.define("ms123.processexplorer.FormWindow", {
  extend: qx.core.Object,
	include: [qx.locale.MTranslation],

	/**
	 * Constructor
	 */
	construct: function (context) {
		this.base(arguments);
		this._init(context);
	},
	statics: {
		__formCache: {}
	},

	/**
	 * ****************************************************************************
	 * MEMBERS
	 * ****************************************************************************
	 */
	members: {
		_init:function(context){
			this._destroyWindow = true;
			this._window = this._createFormWindow(context.name);
			if( context.appRoot ){
				context.appRoot.add( this._window);
			}
		},
		close: function () {
			this._destroyWindow = false;
			this._window.close();
			this._destroyWindow = true;
		},
		destroy: function () {
			this._window.destroy();
		},
		open: function (params) {
			if( params.taskName ){
				this._window.setCaption(params.processName+"/"+params.taskName);
			}else{
				this._window.setCaption(params.processName);
			}
			var form = this.createForm(params);
			if (this._window.hasChildren()) {
				this._window.removeAll();
			}
			this._window.add(form, {
				edge: "center"
			});
			this._form = form;
			this._window.open();
		},
		getLabel: function (path) {
			if (this._form.formData[path] !== undefined && this._form.formData[path].label !== undefined) return this._form.formData[path].label;
			return path;
		},
		showErrors: function(cv){
			this._form.setErrors(cv);
		},
		createForm: function (params) {
			var formVar = params.formVar;
			var formPath = params.formPath;
			var mappedFormValues = params.mappedFormValues;
			var processVariables = params.processVariables;
			var processCategory = params.processCategory;
			var buttons = params.buttons;
			console.log("formVar:"+formVar+"/"+formPath);

			var formDesc=params.formDesc || null;
			if( formDesc==null){
				try{
					//formDesc = ms123.processexplorer.FormWindow.__formCache[params.processName+formVar];
					if( !formDesc ){
						formDesc = ms123.util.Remote.rpcSync( "git:searchContent",{
										reponame:processCategory,
										name:formPath,
										type:"sw.form"
								});	
						//console.log("parse:"+formDesc);
						formDesc = formDesc.evalJSON();
						//ms123.processexplorer.FormWindow.__formCache[params.processName+formVar]=formDesc;
					}
				}catch(e){
					ms123.form.Dialog.alert("FormWindow.open:"+e);
					return null;
				}
			}


			
			//var postdata = 'filters={"field":"fid", "op":"eq", "data":"' + formKey + '"}&rows=10000&page=1';
			//var ret = ms123.util.Remote.sendSync("data/form?query=true", "POST", null, postdata, null);

			if (!formDesc ) {
				formDesc = "{stencil:{id:\"XForm\"},childShapes:[]}";//@@@MS braucht man das, Task ohne Form?
			}

			var context = {};
			context.buttons = buttons;
			context.actionCallback = params.actionCallback;
			context.formDesc = formDesc;
			context.formVariables = processVariables;
			context.storeDesc = ms123.StoreDesc.getNamespaceDataStoreDescForNS(processCategory);
			console.log("StoreDesc1:"+context.storeDesc);
			if(!context.storeDesc){
				context.storeDesc = new ms123.StoreDesc({
					namespace: processCategory 
				});
			}
			console.log("StoreDesc2:"+context.storeDesc);
			console.log("formDesc:" + context.formDesc.resourceId);
			var form = new ms123.widgets.Form(context);
			var fd = (processVariables && processVariables[formVar]) ? processVariables[formVar] : {};
			if (mappedFormValues) {
				fd = ms123.util.Clone.merge({}, fd, mappedFormValues);
				var x = qx.util.Serializer.toJson(fd);
				console.log("fd:" + x);
			}
			form.fillForm(fd);
			return form;
		},
		_createFormWindow: function (name) {
			var win = new qx.ui.window.Window(name, "").set({
				resizable: true,
				useMoveFrame: true,
				useResizeFrame: true
			});
			win.setLayout(new qx.ui.layout.Dock);
			win.setWidth(600);
			win.setHeight(450);
			win.setAllowMaximize(false);
			win.setAllowMinimize(false);
			win.setModal(true);
			win.setActive(false);
			win.minimize();
			win.center();
			win.addListener("close", function (e) {
				if( this._destroyWindow ){
					win.destroy();
				}
			}, this);
			return win;
		}
	}
});
