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
qx.Class.define('ms123.ruleseditor.ColumnModel', {

	extend: qx.ui.table.columnmodel.Resize,

	construct: function (obj) {
		this.base(arguments);
		this._table = obj;
	},

	statics: { /** {int} the default width of a column in pixels. */
		DEFAULT_WIDTH: 100,
		/** {DefaultDataCellRenderer} the default header cell renderer. */
		DEFAULT_HEADER_RENDERER: qx.ui.table.headerrenderer.Default,
		/** {DefaultDataCellRenderer} the default data cell renderer. */
		DEFAULT_DATA_RENDERER: ms123.ruleseditor.CellRenderer,
		/** {TextFieldCellEditorFactory} the default editor factory. */
		DEFAULT_EDITOR_FACTORY: qx.ui.table.celleditor.TextField
	},

	members: {

		init: function (colCount, param) {
			this.base(arguments, colCount, param);
			for (var i = 0; i < colCount; ++i) {
				this.setDataCellRenderer(i, new ms123.ruleseditor.CellRenderer(this._table.getTableModel(), this._table));
				this.setHeaderCellRenderer(i, new ms123.ruleseditor.HeaderRenderer());
				
			}
		},

		moveColumn: function (a, b) {

		}
	}
});
