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
qx.Class.define("ms123.util.RelationList", {
 extend: qx.core.Object,
 include : [ qx.locale.MTranslation],


	/**
	 *****************************************************************************
	 CONSTRUCTOR
	 *****************************************************************************
	 */

	construct: function (facade,formElement,config) {
		this.base(arguments);
		this._facade = facade;	
		var relList = this._getRelations();
		var listItem = new qx.ui.form.ListItem("------",null,"-");
		formElement.addItem(listItem);
		for(var i=0; i < relList.length; i++){
			var relMap = relList[i];
			var leftfield = relMap.leftfield || this._baseName(relMap.rightmodule);
			listItem = new qx.ui.form.ListItem(this.tr(relMap.leftmodule)+","+leftfield,null,relMap.leftmodule+","+leftfield);
			formElement.addItem(listItem);
		}
	},

	/**
	 *****************************************************************************
	 MEMBERS
	 *****************************************************************************
	 */

	members: {
		_getRelations: function (thisEntity) {
			var completed = (function (data) {}).bind(this);

			var failed = (function (details) {
				ms123.form.Dialog.alert(this.tr("entitytypes.getRelations_failed") + ":" + details.message);
			}).bind(this);

			try {
				var storeId = this._facade.storeDesc.getStoreId();
				var ret = ms123.util.Remote.rpcSync("entity:getRelations", {
					storeId: this._getStoreId()
				});
				console.log("rel:"+JSON.stringify(ret,null,2));
				var relList = [];
				for( var i = 0; i< ret.length;i++){
					var r = ret[i];
					if( !thisEntity || r.rightmodule == "data."+thisEntity){
						relList.push( r );
					}
					
				}
				console.log("rel:"+JSON.stringify(relList,null,2));
				return relList;
			} catch (e) {
				//failed.call(this,e);
				return [];
			}
		},
		_baseName:function(s){
			var dot = s.lastIndexOf(".");
			return dot == -1 ? s : s.substring(dot+1);
		},
		_getStoreId:function(){
			var storeId = this._facade.storeDesc.getStoreId();
			return storeId;
		}
	}
});
