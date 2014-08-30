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
qx.Class.define("ms123.datamapper.ChoiceCellRenderer", {
	extend: qx.ui.table.cellrenderer.Replace,

	/**
	 *****************************************************************************
	 MEMBERS
	 *****************************************************************************
	 */

	members: {
		// overridden
		_getContentHtml: function (cellInfo) {
			var value = cellInfo.value;
			var replaceMap = this.getReplaceMap();
			var replaceFunc = this.getReplaceFunction();
			var label;

			if (replaceMap) {
				label = replaceMap[value];
				if (typeof label != "undefined") {
					cellInfo.value = label;
					return qx.util.StringEscape.escape(this._formatValue(cellInfo), qx.bom.String.FROM_CHARCODE);
				}
			}

			if (replaceFunc) {
				cellInfo.value = replaceFunc(value);
			}
			return qx.util.StringEscape.escape(this._formatValue(cellInfo), qx.bom.String.FROM_CHARCODE);
		}
	}
});
