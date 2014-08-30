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
qx.Class.define("ms123.ruleseditor.DateCellRenderer", {
	extend: qx.ui.table.cellrenderer.Conditional,

/*
  *****************************************************************************
     PROPERTIES
  *****************************************************************************
  */

	properties: {
		/**
		 * DateFormat used to format the data.
		 */
		dateFormat: {
			check: "qx.util.format.DateFormat",
			init: null,
			nullable: true
		}
	},


/*
  *****************************************************************************
     MEMBERS
  *****************************************************************************
  */

	members: {
		_getContentHtml: function (cellInfo) {
			var df = this.getDateFormat();

			if (df) {
				if (cellInfo.value) {
					return qx.bom.String.escape(df.format(new Date(cellInfo.value)));
				} else {
					return "";
				}
			}
			else {
				return cellInfo.value || "";
			}
		},


		// overridden
		_getCellClass: function (cellInfo) {
			return "qooxdoo-table-cell";
		}
	}
});
