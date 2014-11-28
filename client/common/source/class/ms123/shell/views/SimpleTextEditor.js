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
	@asset(qx/icon/${qx.icontheme}/22/actions/*)
	@asset(qx/icon/${qx.icontheme}/16/apps/*)
	@asset(ms123/icons/*)
	@asset(ms123/*)
*/

qx.Class.define("ms123.shell.views.SimpleTextEditor", {
	extend: qx.ui.core.Widget,
	include: qx.locale.MTranslation,


	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (model,param,facade) {
		this.base(arguments);
		this.facade=facade;
		this._setLayout(new qx.ui.layout.Dock());
		console.log("model:" + qx.util.Serializer.toJson(model));

    //this.msgArea = new qx.ui.form.TextArea();

		this.msgArea = new ms123.codemirror.CodeMirror({});
		this.msgArea.set({
			height: null,
			width: null
		});

		this._add( this.msgArea, {edge:"center"});
		this._toolbar = this._createToolbar(model);
		this._add(this._toolbar, {
			edge: "south"
		});
		this._show(model);
	},


	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},


	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		_show:function(model){
			var value=null;
			try{
				value = ms123.util.Remote.rpcSync( "git:getContent",{
												reponame:this.facade.storeDesc.getNamespace(),
												path:model.getPath()
											});
				console.log("TextEditor:"+value);
			}catch(e){
				ms123.form.Dialog.alert("TextEditor._show:"+e.message);
				return;
			}


			if( value ){
				this.msgArea.setValue(value);
			}
		},
		_createToolbar: function (model) {
			var toolbar = new qx.ui.toolbar.ToolBar();
			toolbar.setSpacing(5);
			var buttonSave = new qx.ui.toolbar.Button(this.tr("shell.save"), "icon/22/actions/dialog-ok.png");
			buttonSave.addListener("execute", function () {
				var value =  this.msgArea.getValue();
				this._saveContent(model, model.getType(), {json: value});
			}, this);
			toolbar.addSpacer();
			toolbar._add(buttonSave)
			return toolbar;
		},
		_saveContent: function (model, type, content) {
			var path = model.getPath();
			console.log("path:" + path);
			var completed = (function (e) {
				ms123.form.Dialog.alert(this.tr("shell.saved"));
			}).bind(this);

			var failed = (function (e) {
				ms123.form.Dialog.alert(this.tr("shell.save_failed")+":"+e.message);
			}).bind(this);

			var rpcParams = {
				reponame:this.facade.storeDesc.getNamespace(),
				path:path,
				type:type,
				content: content.json
			};

			var params = {
				method:"putContent",
				service:"git",
				parameter:rpcParams,
				async: false,
				context: this,
				completed: completed,
				failed: failed
			}
			ms123.util.Remote.rpcAsync(params);
		}
	}
});
