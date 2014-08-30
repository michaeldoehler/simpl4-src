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
	* @ignore($)
*/
qx.Class.define("ms123.oryx.core.SelectedRect", {
	extend: qx.core.Object,
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (parentId,id) {
		this.base(arguments);

		this.id = id;
		this.parentId = parentId;

		this.node = ms123.oryx.Editor.graft("http://www.w3.org/2000/svg", $(parentId), ['g']);

		this.dashedArea = ms123.oryx.Editor.graft("http://www.w3.org/2000/svg", this.node, ['rect',
		{
			x: 0,
			y: 0,
			'stroke-width': 1,
			stroke: '#777777',
			fill: 'none',
			'stroke-dasharray': '2,2',
			'pointer-events': 'none'
		}]);

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

		hide: function () {
			this.node.setAttributeNS(null, 'display', 'none');
		},

		show: function () {
			this.node.setAttributeNS(null, 'display', '');
		},

		resize: function (bounds) {
			var upL = bounds.upperLeft();

			var padding = ms123.oryx.Config.SELECTED_AREA_PADDING;

			this.dashedArea.setAttributeNS(null, 'width', bounds.width() + 2 * padding);
			this.dashedArea.setAttributeNS(null, 'height', bounds.height() + 2 * padding);
			this.node.setAttributeNS(null, 'transform', "translate(" + (upL.x - padding) + ", " + (upL.y - padding) + ")");
		},
		toString:function(){
			return "SelectedRect("+this.id+")";
		}
		

	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
