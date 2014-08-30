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

qx.Class.define("ms123.graphicaleditor.plugins.HighlightingSelectedShapes", {
	extend: qx.core.Object,
	include: [ms123.util.MBindTo],
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (facade) {
		this.base(arguments);
		this.facade = facade;

		this.opacityFull = 0.9;
		this.opacityLow = 0.4;

		this.facade.registerOnEvent(ms123.oryx.Config.EVENT_SELECTION_CHANGED, this.onSelectionChanged.bind(this));
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
		 * On the Selection-Changed
		 *
		 */
		onSelectionChanged: function (event) {
//console.log("HighlightingSelectedShapes.onSelectionChanged:"+event.elements);
			if (event.elements && event.elements.length > 1) {
				this.facade.raiseEvent({
					type: ms123.oryx.Config.EVENT_HIGHLIGHT_SHOW,
					highlightId: 'selection',
					elements: event.elements.without(event.subSelection),
					color: ms123.oryx.Config.SELECTION_HIGHLIGHT_COLOR,
					opacity: !event.subSelection ? this.opacityFull : this.opacityLow
				});

				if (event.subSelection) {
					this.facade.raiseEvent({
						type: ms123.oryx.Config.EVENT_HIGHLIGHT_SHOW,
						highlightId: 'subselection',
						elements: [event.subSelection],
						color: ms123.oryx.Config.SELECTION_HIGHLIGHT_COLOR,
						opacity: this.opacityFull
					});
				} else {
					this.facade.raiseEvent({
						type: ms123.oryx.Config.EVENT_HIGHLIGHT_HIDE,
						highlightId: 'subselection'
					});
				}

			} else {
				this.facade.raiseEvent({
					type: ms123.oryx.Config.EVENT_HIGHLIGHT_HIDE,
					highlightId: 'selection'
				});
				this.facade.raiseEvent({
					type: ms123.oryx.Config.EVENT_HIGHLIGHT_HIDE,
					highlightId: 'subselection'
				});
			}
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
