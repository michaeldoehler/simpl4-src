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

qx.Class.define("ms123.graphicaleditor.plugins.EditDeleteCommand", {
	extend: ms123.oryx.core.Command,
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (clipboard, facade) {
		this.base(arguments);
		this.clipboard = clipboard;
		this.shapesAsJson = clipboard.get();
		this.facade = facade;

		// Store dockers of deleted shapes to restore connections
		this.dockers = this.shapesAsJson.map(function (shapeAsJson) {
			var shape = shapeAsJson.getShape();
			var incomingDockers = shape.getIncomingShapes().map(function (s) {
				return s.getDockers().last()
			})
			var outgoingDockers = shape.getOutgoingShapes().map(function (s) {
				return s.getDockers().first()
			})
			var dockers = shape.getDockers().concat(incomingDockers, outgoingDockers).compact().map(function (docker) {
				return {
					object: docker,
					referencePoint: docker.referencePoint,
					dockedShape: docker.getDockedShape()
				};
			});
			return dockers;
		}).flatten();
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
		execute: function () {
			this.shapesAsJson.each(qx.lang.Function.bind(function (shapeAsJson) {
				// Delete shape
				this.facade.deleteShape(shapeAsJson.getShape());
			},this));

			this.facade.setSelection([]);
			this.facade.getCanvas().update();
			this.facade.updateSelection();

		},
		rollback: function () {
			this.shapesAsJson.each(qx.lang.Function.bind(function (shapeAsJson) {
				var shape = shapeAsJson.getShape();
				var parent = this.facade.getCanvas().getChildShapeByResourceId(shapeAsJson.parent.resourceId) || this.facade.getCanvas();
				parent.add(shape, shape.parentIndex);
			},this));

			//reconnect shapes
			this.dockers.each(qx.lang.Function.bind(function (d) {
				d.object.setDockedShape(d.dockedShape);
				d.object.setReferencePoint(d.referencePoint);
			},this));

			this.facade.setSelection(this.selectedShapes);
			this.facade.getCanvas().update();
			this.facade.updateSelection();

		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
