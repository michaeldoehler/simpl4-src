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

qx.Class.define("ms123.datamapper.edit.ImportUploadWindow", {
	extend: ms123.datamapper.edit.UploadWindow,
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (facade, config, title) {
		this._facade = facade;
		this.base(arguments,facade,config,title);
	},

	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		_getRpcHeader: function (id) {
			var rpc = {
				"service": "importing",
				"method": "upload",
				"id": 31,
				"params": {
					"importingid": this._facade.importingid,
					"withoutImport": true,
					"storeId": this._facade.storeDesc.getStoreId()
				}
			};
			return rpc;
		},
		_saveFile: function (params) {
			try {
				ms123.util.Remote.rpcSync("importing:upload", params);
			} catch (e) {
				ms123.form.Dialog.alert("ImportUploadWindow._saveFile:" + e);
				return null;
			}
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
