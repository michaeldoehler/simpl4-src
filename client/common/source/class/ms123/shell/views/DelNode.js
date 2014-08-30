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

qx.Class.define("ms123.shell.views.DelNode", {
	extend: qx.core.Object,
	include: qx.locale.MTranslation,


	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (model,param,facade) {
		this.base(arguments);
		this.model = model;
		this.facade=facade;
		this._delDialog();
	},


	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},


	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		_delDialog: function () {
			console.log("_delNode:" + qx.util.Serializer.toJson(this.model));
			ms123.form.Dialog.confirm(this.tr("shell.confirm_delete")+":"+this.model.getPath(), function (e) {
				if (e) {
					this._delNode.call(this);
				}
			}, this);
		},
		_delNode: function () {
			console.log("_delNode:" + qx.util.Serializer.toJson(this.model));

			var	path = this.model.getPath();
			console.log("path:" + path);
			var completed = (function (data) {
				ms123.form.Dialog.alert(this.tr("shell.node_deleted")+":"+this.model.getPath());
				var name = this.model.getValue();
				console.log("name:"+name);
				var children = this.model.parent.getChildren();
				var len = children.getLength();
				console.log("len:"+len);
				for(var i=0; i < len; i++){
					var child = children.getItem(i);
					console.log("\tname:"+child.getValue());
					if( child.getValue() == name){
						children.remove(child);
						break;
					}
				}
			}).bind(this);

			var failed = (function (details) {
				ms123.form.Dialog.alert(this.tr("shell.node_delete_failed")+":"+details.message);
			}).bind(this);

			var rpcParams = {
				reponame:this.facade.storeDesc.getNamespace(),
				path:path
			};

			var params = {
				method:"deleteObject",
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
