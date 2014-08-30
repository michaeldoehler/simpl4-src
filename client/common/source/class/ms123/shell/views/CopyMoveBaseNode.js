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

qx.Class.define("ms123.shell.views.CopyMoveBaseNode", {
	extend: ms123.shell.views.NewNode,
	include: qx.locale.MTranslation,


	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (model, param, facade) {
		this.base(arguments, model, facade);
		this._model = model;
		console.log("model:" + qx.util.Serializer.toJson(model));
		this._createResourceWindow();
	},


	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},


	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		_getFunctionString:function(){
		},
		_getDestModel: function (model,path) {
			if( model.getPath && model.getPath() == path) return model;
			var children = model.getChildren();
			for (var i = 0; i < children.getLength(); i++) {
				var c = children.getItem(i);
				var model = this._getDestModel(c,path);
				if( model != null) return model;
			}
			return null;
		},
		_createResourceWindow:function(){
			var context = {};
			context.storeDesc = this.facade.storeDesc;
			var path = this.model.getPath();
			context.title=path +" " + this.tr("shell."+this._getFunctionString()+"_to");
			var currDir = this._getParent(path);

			var destName = null;
			if( this._getFunctionString() == "move"){
				destName = this._getBase(path);
			}
			context.ok_callback = (function(data){
				var path = data.path;
				var name = data.name;
				var destModel = this._getDestModel(this.facade.treeModel, path);;
				var nt = this.model.getType();
				if (this._getFunctionString() == "move" || !this._assetExists(name, nt)) {
					var newPath = path + "/"+ name;
					if( path == "root"){
						newPath = name;
					}
					this._doIt(this.model.getPath(), newPath, name, destModel, nt);
				}
			}).bind(this);
			new ms123.shell.ResourceSelectorWindow(context,currDir,destName);
		},
		_getParent: function (path) {
			var lastIndex = path.lastIndexOf("/");
			if (lastIndex == -1) return "";
			return path.substring(0, lastIndex);
		},
		_getBase: function (path) {
			var lastIndex = path.lastIndexOf("/");
			if (lastIndex == -1) return path;
			return path.substring(lastIndex+1);
		},
		_doIt: function (origPath, newPath, name,destModel, nodetype) {
			console.log("_doIt:origPath:"+origPath+"|newPath:"+newPath+"|name:"+name);
			var completed = (function (data) {
				ms123.form.Dialog.alert(this.tr("shell."+this._getFunctionString()+"_ready"));
				var ret = data;
				var m = {
					"title": name,
					"path": newPath,
					"value": name,
					"id": name,
					"children": [],
					"type": nodetype
				};
				var parentChilds = destModel.getChildren();
				var nodeModel = qx.data.marshal.Json.createModel(m);
				nodeModel.parent = destModel;
				parentChilds.insertAt(0, nodeModel);

				if( this._getFunctionString() == "move"){
					this.model.parent.getChildren().remove(this.model);
				}
			}).bind(this);

			var failed = (function (details) {
				ms123.form.Dialog.alert(this.tr("shell."+this._getFunctionString()+"_failed") + ":" + details.message);
			}).bind(this);

			var rpcParams = {
				reponame: this.facade.storeDesc.getNamespace(),
				oldPath: origPath,
				origPath: origPath,
				newPath: newPath
			};

			var params = {
				method: this._getFunctionString()+"Object",
				service: "git",
				parameter: rpcParams,
				async: false,
				context: this,
				completed: completed,
				failed: failed
			}
			ms123.util.Remote.rpcAsync(params);
		}
	}
});
