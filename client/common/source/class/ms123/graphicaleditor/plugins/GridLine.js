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
	* @ignore($)
*/
qx.Class.define("ms123.graphicaleditor.plugins.GridLine", {
	extend: qx.core.Object,

	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (parentId, direction) {
		this.base(arguments);
		if (ms123.graphicaleditor.plugins.GridLine.DIR_HORIZONTAL !== direction && ms123.graphicaleditor.plugins.GridLine.DIR_VERTICAL !== direction) {
			direction = ms123.graphicaleditor.plugins.GridLine.DIR_HORIZONTAL
		}


		this.parent = $(parentId);
		this.direction = direction;
		this.node = ms123.oryx.Editor.graft("http://www.w3.org/2000/svg", this.parent, ['g']);

		this.line = ms123.oryx.Editor.graft("http://www.w3.org/2000/svg", this.node, ['path',
		{
			'stroke-width': 1,
			stroke: 'silver',
			fill: 'none',
			'stroke-dasharray': '5,5',
			'pointer-events': 'none'
		}]);

		this.hide();

	},

	/******************************************************************************
	 STATICS
	 ******************************************************************************/
	statics: {
		DIR_HORIZONTAL: "hor",
		DIR_VERTICAL: "ver"

	},

	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},

	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		hide: function () {
			this.node.setAttributeNS(null, 'display', 'none');
		},

		show: function () {
			this.node.setAttributeNS(null, 'display', '');
		},

		getScale: function () {
			try {
				return this.parent.parentNode.transform.baseVal.getItem(0).matrix.a;
			} catch (e) {
				return 1;
			}
		},

		update: function (pos) {

			if (this.direction === ms123.graphicaleditor.plugins.GridLine.DIR_HORIZONTAL) {
				var y = pos instanceof Object ? pos.y : pos;
				var cWidth = this.parent.parentNode.parentNode.width.baseVal.value / this.getScale();
				this.line.setAttributeNS(null, 'd', 'M 0 ' + y + ' L ' + cWidth + ' ' + y);
			} else {
				var x = pos instanceof Object ? pos.x : pos;
				var cHeight = this.parent.parentNode.parentNode.height.baseVal.value / this.getScale();
				this.line.setAttributeNS(null, 'd', 'M' + x + ' 0 L ' + x + ' ' + cHeight);
			}

			this.show();
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
