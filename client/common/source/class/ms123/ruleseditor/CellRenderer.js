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
qx.Class.define('ms123.ruleseditor.CellRenderer', {

	extend: qx.ui.table.cellrenderer.Html,

	construct: function (model, table) {
		this.base(arguments);
		this._model = model;
	},


	members: {

		_model: null,

		_getCellAttributes: function (cellInfo) {
			return cellInfo.attributes || "";
		},

		_getCellStyle: function (cellInfo) {

			if (cellInfo.style) {
				return cellInfo.style;
			}

			var style = {
				"text-align": 'left',
				"color": '#606060',
				"font-style": 'Lucida Grande, Verdana, Arial',
				"font-weight": 'normal'
			};

			var value = cellInfo.value;

			if (value == null) {
				return "";
			}

			if (typeof value == "string") {
				if (value.substr(0, 1) == "=") {
					style['text-align'] = 'right';
				}
			} else if (typeof value == "number") {
				style['text-align'] = 'right';
			}
		},

		createDataCellHtml: function (cellInfo, htmlArr) {
			var extra = '';
			var extra2 = '';

			if (cellInfo.inSelection) {
				extra += 'background-color: #E0E0E0';
			}

			cellInfo.style = '';
			cellInfo.attributes = '';

			var value = cellInfo.value;
			if ((!isNaN(value) && value != '') || (typeof(value) == 'string' && value.substr(0, 1) == "=")) {
				extra2 = ' qooxdoo-table-cell-right';
			}

			htmlArr.push('<div class="', this._getCellClass(cellInfo), extra2, '" style="', 'left:', cellInfo.styleLeft, 'px;', this._getCellSizeStyle(cellInfo.styleWidth, cellInfo.styleHeight, this._insetX, this._insetY), extra, this._getCellStyle(cellInfo), '" ', this._getCellAttributes(cellInfo), '>' + this._formatValue(cellInfo), '</div>');

		},

		_formatValue: function (cellInfo) {

			var value = cellInfo.value;

			if (value == null || value == undefined || value.toString() == 'NaN') {
				return "";
			}

			if (typeof value == "string") {
					return value;
			}
			else if (typeof value == "number") {
				if (!qx.ui.table.cellrenderer.Default._numberFormat) {
					qx.ui.table.cellrenderer.Default._numberFormat = new qx.util.format.NumberFormat();
					qx.ui.table.cellrenderer.Default._numberFormat.setMaximumFractionDigits(10);
				}
				var res = qx.ui.table.cellrenderer.Default._numberFormat.format(value);
			}
			else if (value instanceof Date) {
				res = qx.util.format.DateFormat.getDateInstance().format(value);
			}
			else {
				res = value;
			}

			return res;
		},

		getModel: function () {
			return this._model;
		},

		setModel: function (model) {
			this._model = model;
		}
	}
});
