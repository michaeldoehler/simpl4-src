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


qx.Class.define('ms123.ruleseditor.DateCellEditor', {
	extend: qx.ui.table.celleditor.TextField,

	construct: function () {
		this.base(arguments);
	},

	members: {
		createCellEditor: function (cellInfo) {
			var cellEditor = new ms123.ruleseditor.DateField;
//			cellEditor.setAppearance("table-editor-textfield");

			cellEditor.originalValue = cellInfo.value;
			if (cellInfo.value === null || cellInfo.value === undefined || cellInfo.value == '') {
				cellInfo.value = new Date().getTime();
			}
console.log("createCellEditor.value:"+cellInfo.value);
			cellEditor.setValue(new Date(cellInfo.value));
			cellEditor.selectAllText();

			return cellEditor;
		},

		getCellEditorValue: function (cellEditor) {
			var value = cellEditor.getValue();
console.log("getCellEditorValue.value:"+cellEditor.getValue().getTime());
			return value.getTime();
		}
	}
});
