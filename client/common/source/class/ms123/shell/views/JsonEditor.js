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

qx.Class.define("ms123.shell.views.JsonEditor", {
	extend: ms123.shell.views.SimpleTextEditor,
	include: qx.locale.MTranslation,


	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (model,param,facade) {
		this.base(arguments,model,param,facade);
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
			var item=null;
			try{
				item = ms123.util.Remote.rpcSync( "git:getContent",{
												reponame:this.facade.storeDesc.getNamespace(),
												path:model.getPath()
											});
				console.log("JsonEditor:"+qx.util.Serializer.toJson(item));
			}catch(e){
				ms123.form.Dialog.alert("JsonEditor._show:"+e.message);
				return;
			}


			if( item ){
				var value = qx.lang.Json.stringify(  qx.lang.Json.parse(item), null, 4 );
				this.msgArea.setValue(value);
			}
		},
		_createToolbar: function (model) {
			var toolbar = new qx.ui.toolbar.ToolBar();
			toolbar.setSpacing(5);
			var buttonSave = new qx.ui.toolbar.Button(this.tr("shell.save"), "icon/22/actions/dialog-ok.png");
			buttonSave.addListener("execute", function () {
				var value =  this.msgArea.getValue();
				try{
				value = qx.lang.Json.stringify(qx.lang.Json.parse(value),null,2);
				}catch(e){
					ms123.form.Dialog.alert("Error:"+e);
					return;
				}
				this._saveContent(model, null, {json: value});
			}, this);
			toolbar.addSpacer();
			toolbar._add(buttonSave)
			return toolbar;
		}
	}
});
