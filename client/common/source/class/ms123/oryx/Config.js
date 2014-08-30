/**
 * Copyright (c) 2006
 * Martin Czuchra, Nicolas Peters, Daniel Polak, Willi Tscheschner
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 **/

/**
	* @ignore(Hash)
*/
qx.Class.define("ms123.oryx.Config", {
	/******************************************************************************
	 STATICS
	 ******************************************************************************/
	statics: {

		BACKEND_SWITCH: true,
		PANEL_LEFT_WIDTH: 250,
		PANEL_RIGHT_COLLAPSED: true,
		PANEL_RIGHT_WIDTH: 300,
		APPNAME: 'Signavio',
		WEB_URL: "explorer",

		/* Show grid line while dragging */
		SHOW_GRIDLINE: true,

		/* Editor-Mode */
		MODE_READONLY: "readonly",
		MODE_FULLSCREEN: "fullscreen",
		WINDOW_HEIGHT: 400,
		PREVENT_LOADINGMASK_AT_READY: false,

		/* Plugins */
		PLUGINS_ENABLED: true,
		PLUGINS_FOLDER: "Plugins/",

		BPMN20_SCHEMA_VALIDATION_ON: true,

		/* Namespaces */
		NAMESPACE_ORYX: "http://www.b3mn.org/oryx",
		NAMESPACE_SVG: "http://www.w3.org/2000/svg",

		/* UI */
		CANVAS_WIDTH: 2485,
		CANVAS_HEIGHT: 3050,
		CANVAS_RESIZE_INTERVAL: 300,
		SELECTED_AREA_PADDING: 4,
		CANVAS_BACKGROUND_COLOR: "none",
		GRID_DISTANCE: 30,
		GRID_ENABLED: true,
		ZOOM_OFFSET: 0.1,
		DEFAULT_SHAPE_MARGIN: 60,
		SCALERS_SIZE: 7,
		MINIMUM_SIZE: 20,
		MAXIMUM_SIZE: 10000,
		OFFSET_MAGNET: 15,
		OFFSET_EDGE_LABEL_TOP: 8,
		OFFSET_EDGE_LABEL_BOTTOM: 8,
		OFFSET_EDGE_BOUNDS: 5,
		COPY_MOVE_OFFSET: 30,

		BORDER_OFFSET: 14,

		MAX_NUM_SHAPES_NO_GROUP: 9,

		SHAPEMENU_CREATE_OFFSET_CORNER: 30,
		SHAPEMENU_CREATE_OFFSET: 45,

		/* Shape-Menu Align */
		SHAPEMENU_RIGHT: "Oryx_Right",
		SHAPEMENU_BOTTOM: "Oryx_Bottom",
		SHAPEMENU_LEFT: "Oryx_Left",
		SHAPEMENU_TOP: "Oryx_Top",


		/* Morph-Menu Item */
		MORPHITEM_DISABLED: "Oryx_MorphItem_disabled",

		/* Property type names */
		TYPE_STRING: "string",
		TYPE_BOOLEAN: "boolean",
		TYPE_INTEGER: "integer",
		TYPE_FLOAT: "float",
		TYPE_COLOR: "color",
		TYPE_DATE: "date",
		TYPE_CHOICE: "choice",
		TYPE_COMBO: "combo",
		TYPE_URL: "url",
		TYPE_DIAGRAM_LINK: "diagramlink",
		TYPE_COMPLEX: "complex",
		TYPE_CONSTRAINTS: "constraints",
		TYPE_TEXT: "text",
		TYPE_HTML: "html",
		TYPE_EPC_FREQ: "epcfrequency",
		TYPE_GLOSSARY_LINK: "glossarylink",
		TYPE_MODULE_SELECTOR: "moduleselector",
		TYPE_RESOURCE_SELECTOR: "resourceselector",
		TYPE_MULTISELECT: "multiselect",
		TYPE_FIELD_SELECTOR: "fieldselector",


		/* Vertical line distance of multiline labels */
		LABEL_LINE_DISTANCE: 2,
		LABEL_DEFAULT_LINE_HEIGHT: 12,

		/* Open Morph Menu with Hover */
		ENABLE_MORPHMENU_BY_HOVER: false,


		/* Editor constants come here */
		EDITOR_ALIGN_BOTTOM: 0x01,
		EDITOR_ALIGN_MIDDLE: 0x02,
		EDITOR_ALIGN_TOP: 0x04,
		EDITOR_ALIGN_LEFT: 0x08,
		EDITOR_ALIGN_CENTER: 0x10,
		EDITOR_ALIGN_RIGHT: 0x20,
		EDITOR_ALIGN_SIZE: 0x30,

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
		EVENT_STENCIL_SET_LOADED: "stencilSetLoaded",
		EVENT_SELECTION_CHANGED: "selectionchanged",
		EVENT_SHAPEADDED: "shapeadded",
		EVENT_SHAPEREMOVED: "shaperemoved",
		EVENT_PROPERTY_CHANGED: "propertyChanged",
		EVENT_DRAGDROP_START: "dragdrop.start",
		EVENT_SHAPE_MENU_CLOSE: "shape.menu.close",
		EVENT_DRAGDROP_END: "dragdrop.end",
		EVENT_RESIZE_START: "resize.start",
		EVENT_RESIZE_END: "resize.end",
		EVENT_DRAGDOCKER_DOCKED: "dragDocker.docked",
		EVENT_HIGHLIGHT_SHOW: "highlight.showHighlight",
		EVENT_HIGHLIGHT_HIDE: "highlight.hideHighlight",
		EVENT_LOADING_ENABLE: "loading.enable",
		EVENT_LOADING_DISABLE: "loading.disable",
		EVENT_LOADING_STATUS: "loading.status",
		EVENT_OVERLAY_SHOW: "overlay.show",
		EVENT_OVERLAY_HIDE: "overlay.hide",
		EVENT_ARRANGEMENT_TOP: "arrangement.setToTop",
		EVENT_ARRANGEMENT_BACK: "arrangement.setToBack",
		EVENT_ARRANGEMENT_FORWARD: "arrangement.setForward",
		EVENT_ARRANGEMENT_BACKWARD: "arrangement.setBackward",
		EVENT_PROPWINDOW_PROP_CHANGED: "propertyWindow.propertyChanged",
		EVENT_LAYOUT_ROWS: "layout.rows",
		EVENT_LAYOUT_VERTICAL: "layout.vertical",
		EVENT_LAYOUT_BPEL: "layout.BPEL",
		EVENT_LAYOUT_BPEL_VERTICAL: "layout.BPEL.vertical",
		EVENT_LAYOUT_BPEL_HORIZONTAL: "layout.BPEL.horizontal",
		EVENT_LAYOUT_BPEL_SINGLECHILD: "layout.BPEL.singlechild",
		EVENT_LAYOUT_BPEL_AUTORESIZE: "layout.BPEL.autoresize",
		EVENT_AUTOLAYOUT_LAYOUT: "autolayout.layout",
		EVENT_UNDO_EXECUTE: "undo.execute",
		EVENT_UNDO_ROLLBACK: "undo.rollback",
		EVENT_BUTTON_UPDATE: "toolbar.button.update",
		EVENT_LAYOUT: "layout.dolayout",
		EVENT_GLOSSARY_LINK_EDIT: "glossary.link.edit",
		EVENT_GLOSSARY_SHOW: "glossary.show.info",
		EVENT_GLOSSARY_NEW: "glossary.show.new",
		EVENT_DOCKERDRAG: "dragTheDocker",

		EVENT_SHOW_PROPERTYWINDOW: "propertywindow.show",
		EVENT_ABOUT_TO_SAVE: "file.aboutToSave",

		/* Selection Shapes Highlights */
		SELECTION_HIGHLIGHT_SIZE: 5,
		SELECTION_HIGHLIGHT_COLOR: "#4444FF",
		SELECTION_HIGHLIGHT_COLOR2: "#9999FF",

		SELECTION_HIGHLIGHT_STYLE_CORNER: "corner",
		SELECTION_HIGHLIGHT_STYLE_RECTANGLE: "rectangle",

		SELECTION_VALID_COLOR: "#00FF00",
		SELECTION_INVALID_COLOR: "#FF0000",


		DOCKER_DOCKED_COLOR: "#00FF00",
		DOCKER_UNDOCKED_COLOR: "#FF0000",
		DOCKER_SNAP_OFFSET: 10,

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

		// TODO Determine where the lowercase constants are still used and remove them from here.
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
