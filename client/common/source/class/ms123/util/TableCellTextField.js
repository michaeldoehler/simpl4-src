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
qx.Class.define("ms123.util.TableCellTextField", {
	extend: qx.ui.table.celleditor.AbstractField,
	construct: function (options) {
		this.options = options;
	},

	members: {
		// overridden
		getCellEditorValue: function (cellEditor) {
			var value = cellEditor.getValue();

			// validation function will be called with new and old value
			var validationFunc = this.getValidationFunction();
			if (validationFunc) {
				value = validationFunc(value, cellEditor.originalValue);
			}

			if (typeof cellEditor.originalValue == "number") {
				if (value != null) {
					value = parseFloat(value);
				}
			}
			return value;
		},


		_createEditor: function () {
			var cellEditor = new qx.ui.form.TextField();
			if( this.options && this.options.filter){
				cellEditor.setFilter(this.options.filter);
			}
			cellEditor.setAppearance("table-editor-textfield");
			return cellEditor;
		}
	}
});
