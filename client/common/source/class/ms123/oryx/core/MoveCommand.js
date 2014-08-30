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

qx.Class.define("ms123.oryx.core.MoveCommand", {
	extend: ms123.oryx.core.Command,
	include: [ms123.util.MBindTo],
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (moveShapes, offset, parent, selectedShapes, plugin) {
		this.base(arguments);
		this.moveShapes = moveShapes;
		this.selectedShapes = selectedShapes;
		this.offset = offset;
		this.plugin = plugin;
		// Defines the old/new parents for the particular shape
		this.newParents = moveShapes.collect(function (t) {
			return parent || t.parent
		});
		this.oldParents = moveShapes.collect(function (shape) {
			return shape.parent
		});
		this.dockedNodes = moveShapes.findAll(function (shape) {
			return shape instanceof ms123.oryx.core.Node && shape.dockers.length == 1
		}).collect(function (shape) {
			return {
				docker: shape.dockers[0],
				dockedShape: shape.dockers[0].getDockedShape(),
				refPoint: shape.dockers[0].referencePoint
			}
		});
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
			this.dockAllShapes()
			// Moves by the offset
			this.move(this.offset);
			// Addes to the new parents
			this.addShapeToParent(this.newParents);
			// Set the selection to the current selection
			this.selectCurrentShapes();
			this.plugin.facade.getCanvas().update();
			this.plugin.facade.updateSelection();
		},
		rollback: function () {
			// Moves by the inverted offset
			var offset = {
				x: -this.offset.x,
				y: -this.offset.y
			};
			this.move(offset);
			// Addes to the old parents
			this.addShapeToParent(this.oldParents);
			this.dockAllShapes(true)

			// Set the selection to the current selection
			this.selectCurrentShapes();
			this.plugin.facade.getCanvas().update();
			this.plugin.facade.updateSelection();

		},
		move: function (offset, doLayout) {

			// Move all Shapes by these offset
			for (var i = 0; i < this.moveShapes.length; i++) {
				var value = this.moveShapes[i];
				value.bounds.moveBy(offset);

				if (value instanceof ms123.oryx.core.Node) {

					(value.dockers || []).each(function (d) {
						d.bounds.moveBy(offset);
					})

					var allEdges = [].concat(value.getIncomingShapes()).concat(value.getOutgoingShapes())
					// Remove all edges which are included in the selection from the list
					.findAll(qx.lang.Function.bind(function (r) {
						return r instanceof ms123.oryx.core.Edge && !this.moveShapes.any(function (d) {
							return d == r || (d instanceof ms123.oryx.core.controls.Docker && d.parent == r)
						})
					},this))
					// Remove all edges which are between the node and a node contained in the selection from the list
					.findAll(qx.lang.Function.bind(function (r) {
						return (r.dockers.first().getDockedShape() == value || !this.moveShapes.include(r.dockers.first().getDockedShape())) && (r.dockers.last().getDockedShape() == value || !this.moveShapes.include(r.dockers.last().getDockedShape()))
					},this))

					// Layout all outgoing/incoming edges
					this.plugin.layoutEdges(value, allEdges, offset);


					var allSameEdges = [].concat(value.getIncomingShapes()).concat(value.getOutgoingShapes())
					// Remove all edges which are included in the selection from the list
					.findAll(qx.lang.Function.bind(function (r) {
						return r instanceof ms123.oryx.core.Edge && r.dockers.first().isDocked() && r.dockers.last().isDocked() && !this.moveShapes.include(r) && !this.moveShapes.any(function (d) {
							return d == r || (d instanceof ms123.oryx.core.controls.Docker && d.parent == r)
						})
					},this))
					// Remove all edges which are included in the selection from the list
					.findAll(qx.lang.Function.bind(function (r) {
						return this.moveShapes.indexOf(r.dockers.first().getDockedShape()) > i || this.moveShapes.indexOf(r.dockers.last().getDockedShape()) > i
					},this))

					for (var j = 0; j < allSameEdges.length; j++) {
						for (var k = 1; k < allSameEdges[j].dockers.length - 1; k++) {
							var docker = allSameEdges[j].dockers[k];
							if (!docker.getDockedShape() && !this.moveShapes.include(docker)) {
								docker.bounds.moveBy(offset);
							}
						}
					}
				}
			}

		},
		dockAllShapes: function (shouldDocked) {
			// Undock all Nodes
			for (var i = 0; i < this.dockedNodes.length; i++) {
				var docker = this.dockedNodes[i].docker;

				docker.setDockedShape(shouldDocked ? this.dockedNodes[i].dockedShape : undefined)
				if (docker.getDockedShape()) {
					docker.setReferencePoint(this.dockedNodes[i].refPoint);
					//docker.update();
				}
			}
		},

		addShapeToParent: function (parents) {

			// For every Shape, add this and reset the position		
			for (var i = 0; i < this.moveShapes.length; i++) {
				var currentShape = this.moveShapes[i];
				if (currentShape instanceof ms123.oryx.core.Node && currentShape.parent !== parents[i]) {

					// Calc the new position
					var unul = parents[i].absoluteXY();
					var csul = currentShape.absoluteXY();
					var x = csul.x - unul.x;
					var y = csul.y - unul.y;

					// Add the shape to the new contained shape
					parents[i].add(currentShape);
					// Add all attached shapes as well
					currentShape.getOutgoingShapes((function (shape) {
						if (shape instanceof ms123.oryx.core.Node && !this.moveShapes.member(shape)) {
							parents[i].add(shape);
						}
					}).bind(this));

					// Set the new position
					if (currentShape instanceof ms123.oryx.core.Node && currentShape.dockers.length == 1) {
						var b = currentShape.bounds;
						x += b.width() / 2;
						y += b.height() / 2
						currentShape.dockers.first().bounds.centerMoveTo(x, y);
					} else {
						currentShape.bounds.moveTo(x, y);
					}

				}

				// Update the shape
				//currentShape.update();
			}
		},
		selectCurrentShapes: function () {
			this.plugin.facade.setSelection(this.selectedShapes);
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
