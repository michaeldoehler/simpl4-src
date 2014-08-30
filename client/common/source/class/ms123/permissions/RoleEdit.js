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
	@ignore($)
	@asset(qx/icon/${qx.icontheme}/16/actions/*)
	@asset(qx/icon/${qx.icontheme}/16/places/*)
*/
qx.Class.define("ms123.permissions.RoleEdit", {
	extend: ms123.util.TableEdit,

	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (facade) {
		this.base(arguments, facade);
	},

	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		_createColumnModel: function () {
			var columnmodel = [{
				name: "permission",
				width: 300,
				type: "TextField",
				header: "%permissions.permission"
			},
			{
				name: "actions",
				header: "%permissions.actions",
				type: "SelectBox",
				options: [{
					value: 'read',
					label: 'read'
				},
				{
					value: 'write',
					label: 'write'
				},
				{
					value: 'read,write',
					label: 'read,write'
				}]
			},
			{
				name: "enabled",
				type: "CheckBox",
				width: 60,
				header: "%permissions.enabled"
			}];
			return this._translate(columnmodel);
		},
		_load: function () {
			var permissions = null;
			try {
				var name = this._facade.event.name;
				var ns = name.split(".")[0];
				var ret  = ms123.util.Remote.rpcSync("permission:getRole", {
					namespace: ns,//this._facade.storeDesc.getNamespace(),
					name: name
				});
				permissions = ret.permissions;
			} catch (e) {
				ms123.form.Dialog.alert("RoleEdit._initRecords:" + e);
				return null;
			}
			return permissions;
		},
		_save: function () {
			var permissions = this._getRecords();
			console.log("_save:"+qx.util.Serializer.toJson(permissions));
			try {
				var name = this._facade.event.name;
				var ns = name.split(".")[0];
				ms123.util.Remote.rpcSync("permission:saveRole", {
					name: name,
					namespace: ns,
					data: { permissions: permissions }
				});
			} catch (e) {
				ms123.form.Dialog.alert("RoleEdit.saveRecords:" + e);
				return;
			}
			ms123.form.Dialog.alert("permissions.role_permission_saved");
		}
	}
});
