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
 qooxdoo dialog library
 
 http://qooxdoo.org/contrib/project#dialog
 
 Copyright:
 2007-2010 Christian Boulanger
 
 License:
 LGPL: http://www.gnu.org/licenses/lgpl.html
 EPL: http://www.eclipse.org/org/documents/epl-v10.php
 See the LICENSE file in the project's top-level directory for details.
 
 Authors:
 *  Christian Boulanger (cboulanger)
 ************************************************************************ */

/**
	@asset(qx/icon/${qx.icontheme}/22/actions/dialog-cancel.png)
	@asset(qx/icon/${qx.icontheme}/22/actions/dialog-ok.png)
	@asset(qx/icon/${qx.icontheme}/22/actions/document-save.png)
*/


/**
 * Base class for dialog widgets
 */
qx.Class.define("ms123.form.Dialog", {
	extend: qx.ui.container.Composite,

	/**
	 *****************************************************************************
	 STATICS
	 *****************************************************************************
	 */
	statics: {

		/**
		 * Returns a dialog instance by type
		 * param type {String}
		 */
		getInstanceByType: function (type) {
			try {
				if (type == null) return "";
				return new ms123.form[qx.lang.String.firstUp(type)];
			}
			catch (e) {
				this.error(type + " is not a valid ms123.form type");
			}
		},

		/**
		 * Shortcut for alert dialog
		 * param message {String}
		 * param callback {Function}
		 * param context {Object} 
		 */
		alert: function (message, callback, context, inWindow) {
			(new ms123.form.Alert({
				"message": message,
				"callback": callback || null,
				"context": context || null,
				"inWindow": inWindow !== undefined ? inWindow : true
			})).show();
		},

		/**
		 * Shortcut for confirm dialog
		 * param message {String}
		 * param callback {Function}
		 * param context {Object} 
		 */
		confirm: function (message, callback, context, inWindow) {
			(new ms123.form.Confirm({
				"message": message,
				"callback": callback || null,
				"context": context || null,
				"inWindow": inWindow !== undefined ? inWindow : true
			})).show();
		},

		/**
		 * Shortcut for prompt dialog
		 * param message {String}
		 * param callback {Function}
		 * param context {Object} 
		 */
		prompt: function (message, callback, context, inWindow) {
			(new ms123.form.Prompt({
				"message": message,
				"callback": callback || null,
				"context": context || null,
				"inWindow": inWindow !== undefined ? inWindow : true
			})).show();
		},

		/**
		 * Shortcut for select dialog
		 * param message {String}
		 * param options {Array}
		 * param callback {Function}
		 * param context {Object} 
		 */
		select: function (message, options, callback, context, inWindow) {
			(new ms123.form.Select({
				"message": message,
				"allowCancel": true,
				"options": options,
				"callback": callback || null,
				"context": context || null,
				"inWindow": inWindow !== undefined ? inWindow : true
			})).show();
		}

		/**
		 * Shortcut for form dialog
		 * param message {String}
		 * param formData {Map}
		 * param callback {Function}
		 * param context {Object} 
		 */
		/*form: function (message, formData, callback, context, inWindow) {
			(new ms123.form.Form({
				"message": message,
				"formData": formData,
				"allowCancel": true,
				"callback": callback,
				"context": context || null,
				"inWindow": inWindow !== undefined ? inWindow : true
			})).show();
		}*/
	},

	/**
	 *****************************************************************************
	 PROPERTIES
	 *****************************************************************************
	 */
	properties: {

		/**
		 * Callback function that will be called when the user 
		 * has interacted with the widget. See sample callback
		 * method supplied in the source code of each dialog 
		 * widget.
		 */
		callback: {
			check: "Function",
			nullable: true
		},
		actionCallback: {
			check: "Function",
			nullable: true
		},

		/**
		 * The context for the callback function
		 */
		context: {
			check: "Object",
			nullable: true
		},

		/**
		 * A banner image/logo that is displayed on the widget,
		 * if applicable
		 */
		image: {
			check: "String",
			nullable: true,
			apply: "_applyImage"
		},

		/**
		 * The message that is displayed
		 */
		message: {
			check: "String",
			nullable: true,
			apply: "_applyMessage"
		},

		/**
		 * Whether to block the ui while the widget is displayed
		 */
		useBlocker: {
			check: "Boolean",
			init: true
		},

		/**
		 * The blocker's color
		 */
		blockerColor: {
			check: "String",
			init: "black"
		},

		/**
		 * The blocker's opacity
		 */
		blockerOpacity: {
			check: "Number",
			init: 0.5
		},
		windowWidth: {
			check: "Number",
			init: 400
		},
		windowHeight: {
			check: "Number",
			init: 500
		},

		/**
		 * Whether to allow cancelling the dialog
		 */
		allowCancel: {
			check: "Boolean",
			init: true,
			event: "changeAllowCancel"
		},

		/**
		 * Whether run in a Window
		 */
		inWindow: {
			check: "Boolean",
			init: true
		},
		render: {
			check: "Boolean",
			init: true
		},
		useHtml: {
			check: "Boolean",
			init: false
		},
		/**
		 * Whether okButton in Alert
		 */
		noOkButton: {
			check: "Boolean",
			init: false
		},
		/**
		 */
		hide: {
			check: "Boolean",
			init: true
		},

		// overridden
		focusable: {
			refine: true,
			init: true
		}
	},

	/**
	 *****************************************************************************
	 EVENTS
	 *****************************************************************************
	 */
	events: {
		/**
		 * Event dispatched when widget is shown
		 */
		"show": "qx.event.type.Event",

		/**
		 * Data event dispatched when widget is hidden
		 */
		"hide": "qx.event.type.Event"
	},

	/**
	 *****************************************************************************
	 CONSTRUCTOR
	 *****************************************************************************
	 */

	/**
	 * param properties {Map|String|undefined} If you supply a map, all the 
	 * corresponding properties will be set. If a string is given, use it 
	 * as to set the 'message' property.
	 */
	construct: function (properties) {
		this.base(arguments);

		this.setLayout(new qx.ui.layout.Grow());

		var inWindow = true;
		var render = true;
		this._hasMessage = false;
		if (typeof properties == "object") {
			inWindow = (properties.inWindow !== undefined) ? properties.inWindow : true;
			render = (properties.render !== undefined) ? properties.render : true;
			this._hasMessage = (properties.message !== undefined) ? true : false;
			this._noOkButton = (properties.noOkButton !== undefined) ? properties.noOkButton : false;
			if( properties.actionCallback ){
				this.setActionCallback( properties.actionCallback);
			}
			if( properties.useHtml ){
				this.setUseHtml( properties.useHtml);
			}
			if( properties.windowWidth ){
				this.setWindowWidth( properties.windowWidth);
			}
			if( properties.windowHeight ){
				this.setWindowHeight( properties.windowHeight);
			}
		}

		if (inWindow && render) {
			this._addToRoot(properties);
		}
		this._render = render;
		this._warn= false ;
		if( properties.warn !== undefined ){
			this._warn= properties.warn ;
			delete properties.warn;
		}

		/**
		 * create widget content
		 */
		this._createWidgetContent();

		/**
		 * set properties if given
		 */
		if (typeof properties == "object") {
			this.set(properties);
		}
		/**
		 * if argument is a string, assume it is a message
		 */
		else if (typeof properties == "string") {
			this._hasMessage = true;
			this.setMessage(properties);
		}
	},


	/**
	 *****************************************************************************
	 MEMBERS
	 *****************************************************************************
	 */
	members: {

		/**
		 ---------------------------------------------------------------------------
		 PRIVATE MEMBERS
		 ---------------------------------------------------------------------------
		 */
		_image: null,
		_message: null,
		_okButton: null,
		_cancelButton: null,
		_inWindow: false,
		_render: true,
		_hide: true,
		_hasMessage: false,
		/**
		 ---------------------------------------------------------------------------
		 WIDGET LAYOUT
		 ---------------------------------------------------------------------------
		 */


		/**
		 * Extending classes must implement this method.
		 */
		_createWidgetContent: function () {
			this.error("_createWidgetContent not implemented!");
		},

		/**
		 * _addToRoot.
		 */
		_addToRoot: function (properties) {
			this._inWindow = true;
			/**
			 * basic settings
			 */
			this.set({
				'visibility': "hidden"//,
//				'decorator': "popup"
//				'decorator': "shadow-popup"
			});
			this.setLayout(new qx.ui.layout.Grow());

			/**
			 * automatically add to application's root
			 */
			var root = qx.core.Init.getApplication().getRoot();
			root.add(this);

			/**
			 * make sure the dialog is above any opened window
			 */
			var maxWindowZIndex = 1E5;
			var windows = root.getWindows();
			for (var i = 0; i < windows.length; i++) {
				var zIndex = windows[i].getZIndex();
				maxWindowZIndex = Math.max(maxWindowZIndex, zIndex);
			}
			this.setZIndex(maxWindowZIndex + 1);

			/**
			 * make it a focus root
			 */
			qx.ui.core.FocusHandler.getInstance().addRoot(this);

			/**
			 * resize event 
			 */
			this.getApplicationRoot().addListener("resize", function (e) {
				var bounds = this.getBounds();
				this.set({
					marginTop: Math.round((qx.bom.Document.getHeight() - bounds.height) / 2),
					marginLeft: Math.round((qx.bom.Document.getWidth() - bounds.width) / 2)
				});
			}, this);

			/**
			 * appear event 
			 */
			this.addListener("appear", function (e) {
				var bounds = this.getBounds();
				this.set({
					marginTop: Math.round((qx.bom.Document.getHeight() - bounds.height) / 2),
					marginLeft: Math.round((qx.bom.Document.getWidth() - bounds.width) / 2)
				});
			}, this);
		},
		/**
		 * Create a button
		 * @return {qx.ui.form.Button}
		 */
		_createButton: function (b) {
			var button = new qx.ui.form.Button(b.label);
			button.setIcon(b.icon);
			button.setAllowStretchX(false);
			button.addListener("execute", function () {
				if (this.getCallback()) {
					this.getCallback().call(this.getContext(), b.value);
				}
			}, this);
			b.button = button;
			return button;
		},

		/**
		 * Create a cancel button
		 * @return {qx.ui.form.Button}
		 */
		_createOkButton: function () {
			var okButton = this._okButton = new qx.ui.form.Button(this.tr("OK"));
			okButton.setIcon("icon/22/actions/dialog-ok.png");
			okButton.setAllowStretchX(false);
			okButton.addListener("execute", this._handleOk, this);
			return okButton;
		},

		/**
		 * Create a cancel button, which is hidden by default and will be shown
		 * if allowCancel property is set to true.
		 * @return {qx.ui.form.Button}
		 */
		_createCancelButton: function () {
			var cancelButton = this._cancelButton = new qx.ui.form.Button(this.tr("Cancel"));
			cancelButton.setAllowStretchX(false);
			cancelButton.setIcon("icon/22/actions/dialog-cancel.png");
			cancelButton.addListener("execute", this._handleCancel, this);
			this.bind("allowCancel", cancelButton, "visibility", {
				converter: function (value) {
					return value ? "visible" : "excluded";
				}
			});
			return cancelButton;
		},

		/**
		 ---------------------------------------------------------------------------
		 APPLY METHODS
		 ---------------------------------------------------------------------------
		 */
		_applyImage: function (value, old) {
			this._image.setSource(value);
			this._image.setVisibility(value ? "visible" : "excluded");
		},

		_applyMessage: function (value, old) {
			this._message.setValue(value);
			this._message.setVisibility(value ? "visible" : "excluded");
		},

		/**
		 ---------------------------------------------------------------------------
		 API METHODS
		 ---------------------------------------------------------------------------
		 */

		/**
		 * Show the widget. Overriding methods must call this parent method
		 */
		show: function () {
			if (this.isUseBlocker()) {
				this.__blocker = new qx.ui.core.Blocker(this.getApplicationRoot());
				this.__blocker.setColor(this.getBlockerColor());
				this.__blocker.setOpacity(this.getBlockerOpacity());
				this.__blocker.blockContent(this.getZIndex() - 1);
			}
			this.setVisibility("visible");
			this.__previousFocus = qx.ui.core.FocusHandler.getInstance().getActiveWidget();
			this.focus();
			this.fireEvent("show");
		},

		/**
		 * Hide the widget. Overriding methods must call this parent method
		 */
		hide: function () {
			if (this._inWindow == false) return;
			this.setVisibility("hidden");
			if (this.isUseBlocker()) {
				this.__blocker.forceUnblock();
			}
			if (this.__previousFocus) {
				try {
					this.__previousFocus.focus();
				}
				catch (e) {}
			}
			this.fireEvent("hide");
		},

		/** @@@MS hmmm   resetCallback : function()
		 {
		 if( this._inWindow == false ) return;
		 this.base(arguments);
		 },*/

		/**
		 ---------------------------------------------------------------------------
		 EVENT HANDLERS
		 ---------------------------------------------------------------------------
		 */

		/**
		 * Handle click on ok button. Calls callback with a "true" argument
		 */
		_handleOk: function () {
			if (this._hide) this.hide();
			if (this.getCallback()) {
				this.getCallback().call(this.getContext(), true);
			}
			this.resetCallback();
		},

		/**
		 * Handle click on cancel button. Calls callback with 
		 * an "undefined" argument
		 */
		_handleCancel: function () {
			if (this._hide) this.hide();
			if (this.getCallback()) {
				this.getCallback().call(this.getContext());
			}
			this.resetCallback();
		}
	},

	/**
	 *****************************************************************************
	 DEFERRED ACTION
	 *****************************************************************************
	 */
	defer: function () {
		/**
		 * create shortcut methods for backward compatibility
		 */
		ms123.form["alert"] = ms123.form.Dialog.alert;
		ms123.form["confirm"] = ms123.form.Dialog.confirm;
		ms123.form["prompt"] = ms123.form.Dialog.prompt;
		ms123.form["select"] = ms123.form.Dialog.select;
		ms123.form["form"] = ms123.form.Dialog.form;
	}
});
