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


qx.Class.define("ms123.oryx.core.AbstractLayouter", {
	extend: ms123.oryx.core.AbstractPlugin,
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (facade) {
		this.base(arguments,facade);
		this.facade.registerOnEvent(ms123.oryx.Config.EVENT_LAYOUT, this._initLayout.bind(this));
	},


	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},

	/******************************************************************************
	 STATICS
	 ******************************************************************************/
	statics: {},
	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		/**
		 * 'layouted' defined all types of shapes which will be layouted. 
		 * It can be one value or an array of values. The value
		 * can be a Stencil ID (as String) or an class type of either 
		 * a ORYX.Core.Node or ORYX.Core.Edge
		 * type Array|String|Object
		 * @memberOf ORYX.Plugins.AbstractLayouter.prototype
		 */
		layouted: [],

		/**
		 * Proofs if this shape should be layouted or not
		 * param {Object} shape
		 * @memberOf ORYX.Plugins.AbstractLayouter.prototype
		 */
		isIncludedInLayout: function (shape) {
			if (!(this.layouted instanceof Array)) {
				this.layouted = [this.layouted].compact();
			}

			// If there are no elements
			if (this.layouted.length <= 0) {
				// Return TRUE
				return true;
			}

			// Return TRUE if there is any correlation between 
			// the 'layouted' attribute and the shape themselve.
			return this.layouted.any(function (s) {
				if (typeof s == "string") {
					return shape.getStencil().id().include(s);
				} else {
					return shape instanceof s;
				}
			})
		},

		/**
		 * Callback to start the layouting
		 * param {Object} event Layout event
		 * param {Object} shapes Given shapes
		 * @memberOf ORYX.Plugins.AbstractLayouter.prototype
		 */
		_initLayout: function (event) {

			// Get the shapes
			var shapes = [event.shapes].flatten().compact();

			// Find all shapes which should be layouted
			var toLayout = shapes.findAll(qx.lang.Function.bind(function (shape) {
				return this.isIncludedInLayout(shape)
			}, this))

			// If there are shapes left 
			if (toLayout.length > 0) {
				// Do layout
				this.layout(toLayout);
			}
		},

		/**
		 * Implementation of layouting a set on shapes
		 * param {Object} shapes Given shapes
		 * @memberOf ORYX.Plugins.AbstractLayouter.prototype
		 */
		layout: function (shapes) {
			throw new Error("Layouter has to implement the layout function.")
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
