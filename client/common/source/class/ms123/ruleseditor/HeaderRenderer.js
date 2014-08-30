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
	* @ignore(Hash)
*/
qx.Class.define('ms123.ruleseditor.HeaderRenderer', {
	extend: qx.ui.table.headerrenderer.Default,

	construct: function () {
		this.base(arguments);
	},

	members: {
		// overridden
		createHeaderCell: function (cellInfo) {
			var widget = new ms123.ruleseditor.HeaderCell();
			widget.setFont(qx.bom.Font.fromString("9px sans-serif")),
console.log("cellInfo.col:"+cellInfo.col);
			var model = cellInfo.table.getTableModel();
			var colid = model.getColumnId(cellInfo.col);
			if( colid.match("^C")){
				widget.setBackgroundColor("#e1dbb1");
			}else{
				widget.setBackgroundColor("#b6b5ca");
			}
			this.updateHeaderCell(cellInfo, widget);
			return widget;
		}
	}
});
