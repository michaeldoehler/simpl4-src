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
qx.Class.define('ms123.ruleseditor.Table', {

	extend: qx.ui.table.Table,

	construct: function (model, custom) {
		this.base(arguments, model, custom);
		this.setStatusBarVisible(false);
	},

	properties: {
		application: {
			check: 'Object'
		}
	},

	members: {
		_onKeyPress: function (evt) {
			console.log("Table._onKeyPress:" + evt);
			if (!this.getEnabled()) {
				return;
			}

			// No editing mode
			var oldFocusedRow = this.__focusedRow;
			var consumed = true;

			// Handle keys that are independent from the modifiers
			var identifier = evt.getKeyIdentifier();

			if (this.isEditing()) {
				// Editing mode
				if (evt.getModifiers() == 0) {
					switch (identifier) {
					case "Enter":
						this.stopEditing();
						var oldFocusedRow = this.__focusedRow;
						this.moveFocusedCell(0, 1); //@@@MS
						this.getSelectionModel().setSelectionInterval(this.__focusedRow, this.__focusedRow);
						if (this.__focusedRow != oldFocusedRow) {
							//consumed = this.startEditing();
						}

						break;

					case "Escape":
						this.cancelEditing();
						this.focus();
						break;

					default:
						consumed = false;
						break;
					}
				}
				return
			}
			else {
				// No editing mode
				if (evt.isCtrlPressed()) {
					// Handle keys that depend on modifiers
					consumed = true;

					switch (identifier) {
					case "A":
						// Ctrl + A
						var rowCount = this.getTableModel().getRowCount();

						if (rowCount > 0) {
							this.getSelectionModel().setSelectionInterval(0, rowCount - 1);
						}

						break;

					default:
						consumed = false;
						break;
					}
				}
				else {
					// Handle keys that are independent from the modifiers
					console.log("identifier:"+identifier);
					switch (identifier) {
					case "Delete":
						consumed = false; //@@@MS
						break;
					case "Space":
						this.__selectionManager.handleSelectKeyDown(this.__focusedRow, evt);
						break;

					case "F2":
					case "Enter":
						consumed = this.startEditing();
						break;

					case "Home":
						this.setFocusedCell(this.__focusedCol, 0, true);
						break;

					case "End":
						var rowCount = this.getTableModel().getRowCount();
						this.setFocusedCell(this.__focusedCol, rowCount - 1, true);
						break;

					case "Left":
						this.stopEditing();
						this.moveFocusedCell(-1, 0);
						break;

					case "Right":
						this.stopEditing();
						this.moveFocusedCell(1, 0);
						break;

					case "Up":
						this.stopEditing();
						this.moveFocusedCell(0, -1);
						break;

					case "Down":
						this.stopEditing();
						this.moveFocusedCell(0, 1);
						break;

					case "PageUp":
					case "PageDown":
						var scroller = this.getPaneScroller(0);
						var pane = scroller.getTablePane();
						var rowCount = pane.getVisibleRowCount() - 1;
						var rowHeight = this.getRowHeight();
						var direction = (identifier == "PageUp") ? -1 : 1;
						scroller.setScrollY(scroller.getScrollY() + direction * rowCount * rowHeight);
						this.moveFocusedCell(0, direction * rowCount);
						break;

					default:
						consumed = this.startEditing();
						consumed = false; //@@@MS
						break;
					}
				}
			}

			if (oldFocusedRow != this.__focusedRow && this.getRowFocusChangeModifiesSelection()) {
				// The focus moved -> Let the selection manager handle this event
				this.__selectionManager.handleMoveKeyDown(this.__focusedRow, evt);
			}

			if (consumed) {
				evt.preventDefault();
				evt.stopPropagation();
			}
		}
	}
});
