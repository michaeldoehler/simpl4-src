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
	@asset(qx/icon/${qx.icontheme}/16/status/*)
	@asset(qx/icon/${qx.icontheme}/16/devices/drive-harddisk.png)
*/


qx.Class.define("ms123.permissions.RoleSelector", {
	extend: ms123.widgets.CollapsablePanel,
	include: qx.locale.MTranslation,


	/**
	 *****************************************************************************
	 CONSTRUCTOR
	 *****************************************************************************
	 */
	construct: function (facade) {
  	this.base(arguments,this.tr("permissions.roles"), new qx.ui.layout.VBox(), "icon/22/apps/preferences-users.png");
		this.facade = facade;
		this.setValue(false);

		var roles = null;
		try{
			roles = ms123.util.Remote.rpcSync( "permission:getRoles", {
				withGlobal:true,
				namespace: this.facade.storeDesc.getNamespace()
			});
		}catch(e){
			ms123.form.Dialog.alert("RoleSelector.getRoles:"+e);
			return;
		}
		this._sort( roles, "name");

		var container = new qx.ui.container.Composite(new qx.ui.layout.Dock());
		this.add(container);

		this._roleMap = {};
		var panel = new qx.ui.container.Composite(new qx.ui.layout.VBox());
		this._panel = panel;
		roles.each((function(role){
			this._roleMap[role.name] = true;
			var re = this._createRoleEntry( role.name );
			panel.add(re);
		}).bind(this));

		var toolbar = this._createToolbar();

		container.add( panel, {edge:"center"});
		container.add( toolbar, {edge:"south"});

		facade.leftSpace.add(this);
	},


	/**
	 *****************************************************************************
	 PROPERTIES
	 *****************************************************************************
	 */

	events: {
		"changeSelection": "qx.event.type.Data"
	},


	properties: {
		// overridden
	},


	/**
	 *****************************************************************************
	 MEMBERS
	 *****************************************************************************
	 */


	members: {
		_sort:function(array, key){
			array.sort( function(a,b){
				a = a[key].toLowerCase();
				b = b[key].toLowerCase();
				if( a < b ) return -1;
				if( a > b ) return 1;
				return 0;
			});
		},
		_createRoleEntry: function (name) {
			var b = new qx.ui.form.Button(name, "icon/16/apps/preferences-users.png");
			b.setCenter(false);
			b.setUserData("name", name);
			b.addListener("execute", function (e) {
				console.log("execute:" + b+"/"+name+"/facade:"+this.facade);
				this.facade.raiseEvent({
					name: name,
					facade:this.facade,
					icon:"icon/16/apps/preferences-users.png",
					type: ms123.permissions.Config.EVENT_ROLE_SELECTED
				});
			},this);
			b.setPaddingTop(1);
			b.setPaddingBottom(1);
			return b;
		},
		_createToolbar: function () {
			var toolbar = new qx.ui.toolbar.ToolBar();
			toolbar.setSpacing(5);
			var bNew = new qx.ui.toolbar.Button("", "icon/16/actions/list-add.png");
			bNew.addListener("execute", function () {
				this._createRoleDialog();
			}, this);
			toolbar._add(bNew)
			toolbar.addSpacer();
			return toolbar;
		},
		_createRole: function (ns, name) {
			console.log("create.ns:" + ns+"/name:"+name);
			try {
				var namespace = this.facade.storeDesc.getNamespace();
				ms123.util.Remote.rpcSync("permission:saveRole", {
					namespace: ns,
					data:{},
					name: ns+"."+name
				});
			} catch (e) {
				ms123.form.Dialog.alert("RoleSelector._createRole:" + e);
				return;
			}
			ms123.form.Dialog.alert(this.tr("roleselector.role_created"));
			this._roleMap[name] = true;
			var re = this._createRoleEntry( ns+"."+name);
			this._panel.addAt(re,0);
		},
/*		_getNamespaces: function () {
			var retList=[];
			var nsList = ms123.util.Remote.rpcSync("namespace:getNamespaces");
			for( var i =0; i < nsList.length;i++){
				var ns = nsList[i];
				if( ns.isInstalled != "no"){
					var o = {};
					o.value = ns.name;
					o.label = ns.name;
					retList.push(o);
				}
			}
			return retList;
		},*/
		_getNamespaces: function () {
			var retList=[];
			var o = {};
			o.value = "global";
			o.label = "global";
			retList.push(o);
			o = {};
			o.value = this.facade.storeDesc.getNamespace();
			o.label = this.facade.storeDesc.getNamespace();
			retList.push(o);
			return retList;
		},
		_createRoleDialog: function () {
			var namespaces = this._getNamespaces();
			var formData = {
				"name": {
					'type': "TextField",
					'label': this.tr("roleselector.roleid"),
					'validation': {
						required: true,
						validator: "/^[A-Za-z]([0-9A-Za-z_]){2,50}$/"
					},
					'value': ""
				},

				"namespace" : {
					'type'  : "SelectBox",
					'label' : this.tr("roleselector.namespace"),
					'value' : 1,
					'options' : namespaces
				}

			};

			var self = this;
			var form = new ms123.form.Form({
				"formData": formData,
				"allowCancel": true,
				"inWindow": true,
				"callback": function (m) {
					if (m !== undefined) {
						var val = m.get("name");
						var ns = m.get("namespace");
						if (self._roleMap[ns+"."+val]) { //Duplicated entry
							ms123.form.Dialog.alert(self.tr("roleselector.duplicated"));
							return;
						} else {
							self._createRole(ns,val);
						}
					}
				},
				"context": self
			});
			form.show();
		}
	}
});
