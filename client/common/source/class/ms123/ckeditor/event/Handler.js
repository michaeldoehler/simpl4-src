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
/** **********************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2007-2008 1&1 Internet AG, Germany, http://www.1und1.de
     2011      Derrell Lipman

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Sebastian Werner (wpbasti)
     * Fabian Jakobs (fjakobs)
     * Christian Hagendorn (chris_schmidt)
     * Derrell Lipman

************************************************************************ */
/*
 @require(qx.event.type.Data)
 @require(qx.event.Pool)
 @ignore(CKEDITOR)
*/

/**
 * This handler provides qooxdoo events translated from CkEditor events
 */
qx.Class.define("ms123.ckeditor.event.Handler", {
	extend: qx.core.Object,
	implement: qx.event.IEventHandler,

	/**
	 * Create a new instance
	 *
	 * param manager {qx.event.Manager} Event manager for the window to use
	 * 
	 * @ignore(CKEDITOR)
	 */
	construct: function (manager) {
		this.base(arguments);

		// Prepare to store maps of functions
		this._handlers = {};
	},





/*
  *****************************************************************************
     STATICS
  *****************************************************************************
  */

	statics: { /** {Integer} Priority of this handler */
		PRIORITY: qx.event.Registration.PRIORITY_NORMAL,

		/**
		 *{Map} Supported event types 
		 *
		 * Value 0 means event is dispatched on the global CKEDITOR object;
		 * Value 1 means event is dispatched on the instantiated editor object
		 */
		SUPPORTED_TYPES: {
			afterCommandExec: 1,
			afterPaste: 1,
			afterSetData: 1,
			afterUndoImage: 1,
			beforeCommandExec: 1,
			beforeGetData: 1,
			beforeModeUnload: 1,
			beforePaste: 1,
			beforeSetMode: 1,
			beforeUndoImage: 1,
			blur: 1,
			click: 1,
			contentDirChanged: 1,
			contentDom: 1,
			contentDomUnload: 1,
			customConfigLoaded: 1,
			dataReady: 1,
			destroy: 1,
			dirChanged: 1,
			doubleclick: 1,
			editingBlockReady: 1,
			elementsPathUpdate: 1,
			focus: 1,
			getData: 1,
			getSnapshot: 1,
			insertElement: 1,
			insertHtml: 1,
			insertText: 1,
			instanceDestroyed: 0,
			instanceReady: 0,
			key: 1,
			keydown: 1,
			loadSnapshot: 1,
			loaded: 0,
			menuShow: 1,
			mode: 1,
			mousemove: 1,
			mouseup: 1,
			paste: 1,
			pasteDialog: 1,
			pasteState: 1,
			readOnly: 1,
			ready: 1,
			removeFormatCleanup: 1,
			reset: 0,
			resize: 1,
			saveSnapshot: 1,
			scaytDialog: 1,
			scaytReady: 0,
			selectionChange: 1,
			showScaytState: 1,
			themeLoaded: 1,
			themeSpace: 1,
			uiReady: 1,
			updateSnapshot: 1
		},

		/** {Integer} Which target check to use */
		TARGET_CHECK: qx.event.IEventHandler.OBJECT,

		/** {Integer} Whether the method "canHandleEvent" must be called */
		IGNORE_CAN_HANDLE: false
	},

	members: {
		_handlers: null,

		// interface implementation
		canHandleEvent: function (target, type) {
			var ret;
			var types = ms123.ckeditor.event.Handler.SUPPORTED_TYPES;

			// Determine whether this event can be handled.
			ret = (target instanceof ms123.ckeditor.Ckeditor && qx.lang.Array.contains(Object.keys(types), type));

			return ret;
		},


		/**
		 * // interface implementation
		 * @ignore(CKEDITOR)
		 */
		registerEvent: function (target, type, capture) {
			var ckrte_editor = target;
			var supportedTypes = ms123.ckeditor.event.Handler.SUPPORTED_TYPES;

			// SUPPORTED_TYPES has value 0 if the listen target is CKEDITOR, and has
			// value 1 if the listen target is the editor instance.
			var listenTarget = [CKEDITOR, ckrte_editor.getCkEditor()][supportedTypes[type]];

			// Wrap the event handler to provide the target and type
			var _this = this;
			var wrappedHandler = function (eventData) {
				// Bind to this Handler object and call the function to fire the event.
				qx.lang.Function.bind(

				function (eventData, target, type) {
					// Fire the event on the specified target
					target.fireDataEvent(type, eventData.data);
				}, _this)(eventData, target, type);
			};

			// Do we have any handlers of this type yet?
			if (!this._handlers[type]) {
				// Nope. Initialize the array
				this._handlers[type] = [];
			}

			// Save the handler function for this target
			this._handlers[type].push({
				target: target,
				handler: wrappedHandler
			});

			// Listen for this event type
			listenTarget.on(type, wrappedHandler);
		},


		/**
		 * // interface implementation
		 * @ignore(CKEDITOR)
		 */
		unregisterEvent: function (target, type, capture) {
			var ckrte_editor = target;
			var supportedTypes = ms123.ckeditor.event.Handler.SUPPORTED_TYPES;

			// SUPPORTED_TYPES has value 0 if the listen target is CKEDITOR, and has
			// value 1 if the listen target is the editor instance.
			var listenTarget = [CKEDITOR, ckrte_editor.getCkEditor()][supportedTypes[type]];

			// Find the handler for this type and target
			this._handlers[type].forEach(

			function (handlerInfo, i) {
				// Is this the one we're looking for?
				if (handlerInfo.target == target) {
					// Yup. Remove the listener
					listenTarget.removeListener(type, handlerInfo.handler);

					// Remove this element from the handlers array for this type
					qx.lang.Array.removeAt(this._handlers[type], i);
				}
			}, this);
		}
	},


	defer: function (statics) {
		qx.event.Registration.addHandler(statics);
	}
});
