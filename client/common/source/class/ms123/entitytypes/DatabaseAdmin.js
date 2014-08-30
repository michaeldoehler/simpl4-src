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
qx.Class.define("ms123.entitytypes.DatabaseAdmin", {
	extend: qx.ui.container.Composite,
	include: [qx.locale.MTranslation],

	/**
	 *****************************************************************************
	 CONSTRUCTOR
	 *****************************************************************************
	 */

	construct: function (model, param, facade) {
		this.base(arguments);
		this._facade = facade;
		this.setLayout(new qx.ui.layout.VBox(2));
		this.setPadding(10);
		 var spacer = new qx.ui.core.Spacer(30, 40);
		this.add(spacer, { });
		var control = new qx.ui.form.Button(this.tr("entitytypes.databasescheme_clean"), "icon/16/actions/go-previous.png");
		control.setCenter(false);
		control.setBackgroundColor("red");
		control.addListener("execute", this._cleanDatabase, this);
		this.add(control, { });
		 var spacer = new qx.ui.core.Spacer(30, 40);
		this.add(spacer, { });
		this._cbMap = {};
		for(var i=0; i < model.getChildren().getLength(); i++){
			var child = model.getChildren().getItem(i);
			var cb = new qx.ui.form.CheckBox(child.getValue());
			this.add(cb, { });
			this._cbMap[child.getValue()] = cb;
		}
		var control = new qx.ui.form.Button(this.tr("entitytypes.databasescheme_table_clean"), "icon/16/actions/go-previous.png");
		control.setCenter(false);
		control.addListener("execute", this._cleanTable, this);
		this.add(control, { alignX:"left" });
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
		_cleanDatabase: function (name, data) {
			ms123.form.Dialog.confirm(this.tr("entitytypes.databasescheme_clean.confirm"), function (e) {
				if (e) {
					try {
						var result = ms123.util.Remote.rpcSync("nucleus:schemaTool", {
							storeId: ms123.StoreDesc.getNamespaceDataStoreDesc().getStoreId(),
							dry:false,
							op: "delete"
						});
					} catch (e) {
						ms123.form.Dialog.alert("DatabaseAdmin._cleanDatabase:" + e);
						return;
					}
				}
			}, this);
		},
		_cleanTable: function (name, data) {
			var keys = Object.keys( this._cbMap);
			var kList = [];
			for (var i = 0; i < keys.length; i++) {
				var key = keys[i];
				var cb = this._cbMap[key];
				if( cb.getValue() ){
					kList.push(key);
				}
			}
			console.log("kList:"+JSON.stringify(kList));
			ms123.form.Dialog.confirm(this.tr("entitytypes.databasescheme_clean.klass_confirm"), function (e) {
				if (e) {
					try {
						var result = ms123.util.Remote.rpcSync("nucleus:schemaTool", {
							storeId: ms123.StoreDesc.getNamespaceDataStoreDesc().getStoreId(),
							dry:false,
							classes:kList,
							op: "delete"
						});
					} catch (e) {
						ms123.form.Dialog.alert("DatabaseAdmin._cleanTable:" + e);
						return;
					}
				}
			}, this);
		}
	}
});
