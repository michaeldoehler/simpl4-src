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
 2007 Visionet GmbH, http://www.visionet.de
 
 License:
 LGPL: http://www.gnu.org/licenses/lgpl.html
 EPL: http://www.eclipse.org/org/documents/epl-v10.php
 See the LICENSE file in the project's top-level directory for details.
 
 Authors:
 * Dietrich Streifert (level420)
 
 Contributors:
 * Petr Kobalicek (e666e)
 * Tobi Oetiker (oetiker)
 
 ************************************************************************ */

/**
 * An upload button which allows selection of a file through the browser fileselector.
 *
 */
qx.Class.define("ms123.upload.UploadButton", {
	extend: qx.ui.form.Button,

	// --------------------------------------------------------------------------
	// [Constructor]
	// --------------------------------------------------------------------------
	/**
	 */

	construct: function (fieldName, label, icon, command) {
		this.base(arguments, label, icon, command);

		this.__inputEl = this._createInput();
		if (fieldName) {
			this.setFieldName(fieldName);
		}

		this.appeared = false;
		this.addListener('appear', function () {
			if (!this.appeared) {
				this.getContentElement().addAt(this.__inputEl, 0);
				this.appeared = true;
			}
		}, this);

		// Fix for bug #3027
		if (qx.bom.client.Engine.OPERA) {
			this.setSelectable(true);
		}
	},

	// --------------------------------------------------------------------------
	// [Properties]
	// --------------------------------------------------------------------------
	events: {
		changeFileName: 'qx.event.type.Data'
	},
	properties: {
		/**
		 * The field name which is assigned to the form
		 */
		fieldName: {
			check: "String",
			init: "",
			apply: "_applyFieldName"
		},

		/**
		 * The value which is assigned to the form
		 */
		fileName: {
			check: "String",
			init: "",
			apply: "_applyFileName"
		},

		/**
		 * the size of the selected File. This may not work on all browsers. It does work
		 * on FireFox and Chrome at least. So be prepared to get a 'Null' response.
		 */
		fileSize: {
			check: "Integer",
			nullable: true,
			init: null
		}
	},





	// --------------------------------------------------------------------------
	// [Members]
	// --------------------------------------------------------------------------
	members: {

		__valueInputOnChange: false,
		__mouseUpListenerId: null,
		__inputEl: null,

		// overridden
		capture: qx.core.Environment.select("engine.name", {
			"mshtml": function () {
				this.__mouseUpListenerId = this.getApplicationRoot().addListenerOnce("mouseup", this._onMouseUp, this);
			},

			"default": function (value, old) {
				this.base(arguments);
			}
		}),
/*		capture: qx.core.Variant.select("qx.client", {
			"mshtml": function () {
				this.__mouseUpListenerId = this.getApplicationRoot().addListenerOnce("mouseup", this._onMouseUp, this);
			},

			"default": function () {
				this.base(arguments);
			}
		}),*/



		// overridden
		releaseCapture: qx.core.Environment.select("engine.name", {
			"mshtml": qx.lang.Function.empty,

			"default": function (value, old) {
				this.base(arguments);
			}
		}),
/*		releaseCapture: qx.core.Variant.select("qx.client", {
			"mshtml": qx.lang.Function.empty,

			"default": function () {
				this.base(arguments);
			}
		}),*/

		// ------------------------------------------------------------------------
		// [Modifiers]
		// ------------------------------------------------------------------------
		/**
		 * Modifies the name property of the hidden input type=file element.
		 *
		 */
		_applyFieldName: function (value, old) {
			this.__inputEl.setAttribute("name", value, true);
		},


		/**
		 * Modifies the value property of the hidden input type=file element.
		 * Only an empty string is accepted for clearing out the value of the
		 * selected file.
		 * 
		 * As a special case for IE the hidden input element is recreated because
		 * setting the value is generally not allowed in IE.
		 *
		 */
		_applyFileName: function (value, old) {
			if (value == old) { // can not change, but setting it to what it is is fine
				return;
			}
			if (this.__valueInputOnChange) {
				this.__valueInputOnChange = false;
			}
			else {
				throw new Error("You can not change the value of a fileName field. Reset the form instead by using  the .clear() method!");
			}
		},


		/**
		 * Apply the enabled property.
		 *
		 */
		_applyEnabled: function (value, old) {
			// just move it behind the button, do not actually
			// disable it since this would stop any upload in progress
			this.__inputEl.setStyle('zIndex', value ? this.getZIndex() + 11 : -10000);
			return this.base(arguments, value, old);
		},

		/**
		 * Create the widget child controls.
		 */

		_createInput: function () {
			var control;
			// styling the input[type=file]
			// element is a bit tricky. Some browsers just ignore the normal
			// css style input. Firefox is especially tricky in this regard.
			// since we are providing our one look via the underlying qooxdoo
			// button anyway, all we have todo is position the ff upload
			// button over the button element. This is tricky in itself
			// as the ff upload button consists of a text and a button element
			// which are not css accessible themselfes. So the best we can do,
			// is align to the top right corner of the upload widget and set its
			// font so large that it will cover even realy large underlying buttons.
			var css = {
				position: "absolute",
				cursor: "pointer",
				hideFocus: "true",
				zIndex: this.getZIndex() + 11,
				opacity: 0,
				// align to the top right hand corner
				top: '0px',
				right: '0px',
				// ff ignores the width setting
				// pick a realy large font size to get
				// a huge button that covers
				// the area of the upload button
				fontSize: '400px'
			};
			if (qx.bom.client.Engine.MSHTML && qx.bom.client.Engine.VERSION < 9.0) {
				css.filter = 'alpha(opacity=0)';
				css.width = '200%';
				css.height = '100%';
			}

			control = new qx.html.Element('input', css, {
				type: 'file',
				name: ''
			});
			control.addListener("change", function (e) {
				var controlDom = control.getDomElement();
				this.__valueInputOnChange = true;
				if (controlDom.files && controlDom.files.length > 0) {
					if (controlDom.files[0].fileSize) {
						this.setFileSize(controlDom.files[0].fileSize);
					}
				}
				var value = e.getData();
				this.setFileName(value);
				this.fireDataEvent('changeFileName', value);
			}, this);

			return control;
		}
	},

	destruct: function () {
		if (this.__mouseUpListenerId) {
			this.getApplicationRoot().removeListenerById(this.__mouseUpListenerId);
		}
	}
});
