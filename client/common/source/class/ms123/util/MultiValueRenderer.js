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
 * Specific data cell renderer for dates.
 */
qx.Class.define("ms123.util.MultiValueRenderer", {
	extend: qx.ui.table.cellrenderer.Conditional,

/*
  *****************************************************************************
     PROPERTIES
  *****************************************************************************
  */

	properties: {},


/* 
  *****************************************************************************
     MEMBERS 
  *****************************************************************************
  */

	members: {
		_getContentHtml: function (cellInfo) {
			var d = new Date();
			try {
				var str = "";
				var komma = "";
				var value = cellInfo.value;
				if( typeof value == "string" && (value.match("^{") || value.match("^\\["))){
					try{
						value = qx.lang.Json.parse(value);
					}catch(e){
						console.error("MultiValueRenderer:"+e);
					}
				}else if( typeof value == "string"){
					return value;
				}
				for (var i = 0; i < value.length; i++) {
					var val = value[i];
					str += komma + val;
					komma = ",";
				}
				return str;
			} catch (e) {
				return "";
			}

		},


		// overridden
		_getCellClass: function (cellInfo) {
			return "qooxdoo-table-cell";
		}
	}
});
