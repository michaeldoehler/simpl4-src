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
qx.Class.define('ms123.ruleseditor.BooleanCellEditor', {
	extend: qx.ui.table.celleditor.CheckBox,

	construct: function () {
		this.base(arguments);
	},

	members: {
		_createCellEditor: function (cellInfo) {
			if (cellInfo.value === null || cellInfo.value === undefined) {
				cellInfo.value = false;
			}
			return this.base(arguments, cellInfo);
		},


		createCellEditor: function (cellInfo) {
			if (cellInfo.value === undefined) {
				cellInfo.value = null;
			}
			var editor = new qx.ui.container.Composite(new qx.ui.layout.HBox().set({
				alignX: "center",
				alignY: "middle"
			})).set({
				focusable: true
			});

console.log("BooleanCellEditor:"+cellInfo.value);
			var checkbox = new ms123.ruleseditor.TristateCheckBox().set({
				triState: true,
				value: cellInfo.value
			});
			editor.add(checkbox);

			// propagate focus
			editor.addListener("focus", function () {
				checkbox.focus();
			});

			// propagate active state
			editor.addListener("activate", function () {
				checkbox.activate();
			});

			// propagate stopped enter key press to the editor
			checkbox.addListener("keydown", function (e) {
				if (e.getKeyIdentifier() == "Enter") {
					var clone = qx.event.Pool.getInstance().getObject(qx.event.type.KeySequence);
					var target = editor.getContentElement().getDomElement();
					clone.init(e.getNativeEvent(), target, e.getKeyIdentifier());
					clone.setType("keypress");
					qx.event.Registration.dispatchEvent(target, clone);
				}
			}, this);

			return editor;
		},


		getCellEditorValue: function (cellEditor) {
			var value = cellEditor.getChildren()[0].getValue();
			return value;
		}
	}
});
