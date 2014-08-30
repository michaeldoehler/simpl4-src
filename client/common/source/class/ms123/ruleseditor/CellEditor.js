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
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


qx.Class.define('ms123.ruleseditor.CellEditor', {

	extend: qx.ui.table.celleditor.TextField,

	construct: function () {
		this.base(arguments);
	},

	members: {
		createCellEditor: function (cellInfo) {
			var cellEditor = new qx.ui.form.TextField;
			cellEditor.setAppearance("table-editor-textfield");

			cellEditor.originalValue = cellInfo.value;
			if (cellInfo.value === null || cellInfo.value === undefined || cellInfo.value.toString() == 'NaN') {
				cellInfo.value = "";
			}
			cellEditor.setValue("" + cellInfo.value);
			cellEditor.selectAllText();

			return cellEditor;
		},

		getCellEditorValue: function (cellEditor) {
			var value = cellEditor.getValue();

			// validation function will be called with new and old value
			var validationFunc = this.getValidationFunction();
			if (!this.__done && validationFunc) {
				value = validationFunc(value, cellEditor.originalValue);
				this.__done = true;
			}
console.log("getCellEditorValue:"+value);
			return value;
		}
	}
});
