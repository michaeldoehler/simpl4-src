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

qx.Class.define("ms123.oryx.core.controls.Magnet", {
	extend: ms123.oryx.core.controls.Control,
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function () {
		this.base(arguments);
		//this.anchors = [];
		this.anchorLeft;
		this.anchorRight;
		this.anchorTop;
		this.anchorBottom;

		this.bounds.set(0, 0, 16, 16);

		//graft magnet's root node into owner's control group.
		this.node = ms123.oryx.Editor.graft("http://www.w3.org/2000/svg", null, ['g',
		{
			"pointer-events": "all"
		}, ['circle',
		{
			cx: "8",
			cy: "8",
			r: "4",
			stroke: "black",
			fill: "red",
			"fill-opacity": "0.6"
		}] ]);

		this.hide();
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

		update: function () {
			this.base(arguments);
		},

		_update: function () {
			this.update();
		},

		refresh: function () {
			this.base(arguments);

			var p = this.bounds.upperLeft();
			this.node.setAttributeNS(null, 'transform', 'translate(' + p.x + ', ' + p.y + ')');
		},

		show: function () {
			this.base(arguments);
		},

		toString: function () {
			return "Magnet " + this.id;
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
