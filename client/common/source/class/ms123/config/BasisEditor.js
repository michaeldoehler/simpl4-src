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
	@asset(qx/icon/${qx.icontheme}/16/places/*)
	@asset(qx/icon/${qx.icontheme}/22/status/*)
	@asset(qx/icon/${qx.icontheme}/16/apps/*)
	@asset(qx/icon/${qx.icontheme}/16/mimetypes/*)

	@asset(ms123/icons/*)
	@asset(ms123/*)
*/

qx.Class.define("ms123.config.BasisEditor", {
 extend: qx.core.Object,
 include : qx.locale.MTranslation,


	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (context) {
	},


	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},


	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		_managedApp:null,
		_removeUnusedFields: function( fields, selectables ){
			var ret = [];
			for(var i=0; i< fields.length;i++){
				if( this._contains( selectables,fields[i] )){
					ret.push(fields[i]);
				}
			}
			return ret;
		},
		_contains: function (selectables, f) {
			for(var i=0; i< selectables.length;i++){
				if( selectables[i].name == f.name ) return true;
			}
			return false;
		},
		_convertValue:function(f,tableColumns){
			for( var j=0; j< tableColumns.length;j++){
				var col = tableColumns[j];
				if( col.type == "DoubleSelectBox" ){
					var val = f[col.name];
					console.log("\tvorher:"+val);
					if( val && typeof val == "object" && val.length > 0){
						f[col.name] = qx.util.Serializer.toJson(val);
					}else{
						f[col.name] = null;
					}
					console.log("\tnacher:"+f[col.name]);
				}
			}					
		},
		
		_prefixUrl: function(url ){
			if( this._managedApp ){
				return "/"+this._managedApp+"/"+url;
			}else{
				return url;
			}
		},
		_translate: function (o) {
			if (typeof o == "string") {
				if (o.match(/^%/)) {
					var tr = this.tr(o.substring(1));
					if (tr) {
						o = tr;
					}
				}
				return o;
			}
			for (var i in o) {
 			  if (typeof o[i] == "function")continue;
				o[i] = this._translate(o[i]);
			}
			return o;
		}
	}
});
