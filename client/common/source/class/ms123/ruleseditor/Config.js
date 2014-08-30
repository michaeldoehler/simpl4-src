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
*/

/**
	* @ignore(Hash)
*/
qx.Class.define("ms123.ruleseditor.Config", {
	/******************************************************************************
	 STATICS
	 ******************************************************************************/
	statics: {

		/* Event types */
		EVENT_MOUSEDOWN: "mousedown",
		EVENT_MOUSEUP: "mouseup",
		EVENT_MOUSEOVER: "mouseover",
		EVENT_MOUSEOUT: "mouseout",
		EVENT_MOUSEMOVE: "mousemove",
		EVENT_DBLCLICK: "dblclick",
		EVENT_KEYDOWN: "keydown",
		EVENT_KEYUP: "keyup",

		EVENT_LOADED: "editorloaded",

		EVENT_EXECUTE_COMMANDS: "executeCommands",
		EVENT_CELL_CHANGED: "cellChanged",
		EVENT_TABLE_CREATED: "tableCreated",
		EVENT_COLUMNS_CHANGED: "columnsChanged",
		EVENT_FOCUSEDCELL_CHANGED: "focusedcellChanged",
		EVENT_PROPERTY_CHANGED: "propertyChanged",
		EVENT_DRAGDROP_START: "dragdrop.start",
		EVENT_DRAGDROP_END: "dragdrop.end",
		EVENT_RESIZE_START: "resize.start",
		EVENT_RESIZE_END: "resize.end",
		EVENT_UNDO_EXECUTE: "undo.execute",
		EVENT_UNDO_ROLLBACK: "undo.rollback",
		EVENT_BUTTON_UPDATE: "toolbar.button.update",

		/* Copy & Paste */
		EDIT_OFFSET_PASTE: 10,

		/* Key-Codes */
		KEY_CODE_X: 88,
		KEY_CODE_C: 67,
		KEY_CODE_V: 86,
		KEY_CODE_DELETE: 46,
		KEY_CODE_META: 224,
		KEY_CODE_BACKSPACE: 8,
		KEY_CODE_LEFT: 37,
		KEY_CODE_RIGHT: 39,
		KEY_CODE_UP: 38,
		KEY_CODE_DOWN: 40,

		KEY_Code_enter: 12,
		KEY_Code_left: 37,
		KEY_Code_right: 39,
		KEY_Code_top: 38,
		KEY_Code_bottom: 40,

		/* Supported Meta Keys */
		META_KEY_META_CTRL: "metactrl",
		META_KEY_ALT: "alt",
		META_KEY_SHIFT: "shift",

		/* Key Actions */
		KEY_ACTION_DOWN: "down",
		KEY_ACTION_UP: "up"

	}
});
