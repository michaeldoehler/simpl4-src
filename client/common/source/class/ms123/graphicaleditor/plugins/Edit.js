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

qx.Class.define("ms123.graphicaleditor.plugins.Edit", {
	extend: qx.core.Object,
	include: [qx.locale.MTranslation],
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (facade) {
		this.base(arguments);
		this.facade = facade;

		this.clipboard = new ms123.graphicaleditor.plugins.EditClipBoard();

		this.facade.offer({
			name: this.tr("ge.Edit.cut"),
			description: this.tr("ge.Edit.cutDesc"),
			icon: this.__getResourceUrl("cut.png"),
			keyCodes: [{
				metaKeys: [ms123.oryx.Config.META_KEY_META_CTRL],
				keyCode: 88,
				keyAction: ms123.oryx.Config.KEY_ACTION_DOWN
			}],
			functionality: this.callEdit.bind(this, this.editCut),
			group: this.tr("ge.Edit.group"),
			index: 1,
			minShape: 1
		});

		this.facade.offer({
			name: this.tr("ge.Edit.copy"),
			description: this.tr("ge.Edit.copyDesc"),
			icon: this.__getResourceUrl("page_copy.png"),
			keyCodes: [{
				metaKeys: [ms123.oryx.Config.META_KEY_META_CTRL],
				keyCode: 67,
				keyAction: ms123.oryx.Config.KEY_ACTION_DOWN
			}],
			functionality: this.callEdit.bind(this, this.editCopy, [true, false]),
			group: this.tr("ge.Edit.group"),
			index: 2,
			minShape: 1
		});

		this.facade.offer({
			name: this.tr("ge.Edit.paste"),
			description: this.tr("ge.Edit.pasteDesc"),
			icon: this.__getResourceUrl("page_paste.png"),
			keyCodes: [{
				metaKeys: [ms123.oryx.Config.META_KEY_META_CTRL],
				keyCode: 86,
				keyAction: ms123.oryx.Config.KEY_ACTION_DOWN
			}],
			functionality: this.callEdit.bind(this, this.editPaste),
			isEnabled: this.clipboard.isOccupied.bind(this.clipboard),
			group: this.tr("ge.Edit.group"),
			index: 3,
			minShape: 0,
			maxShape: 0
		});

		this.facade.offer({
			name: this.tr("ge.Edit.del"),
			description: this.tr("ge.Edit.delDesc"),
			icon: this.__getResourceUrl("cross.png"),
			keyCodes: [{
				metaKeys: [ms123.oryx.Config.META_KEY_META_CTRL],
				keyCode: 8,
				keyAction: ms123.oryx.Config.KEY_ACTION_DOWN
			},
			{
				keyCode: 46,
				keyAction: ms123.oryx.Config.KEY_ACTION_DOWN
			}],
			functionality: this.callEdit.bind(this, this.editDelete),
			group: this.tr("ge.Edit.group"),
			index: 4,
			minShape: 1
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
		callEdit: function (fn, args) {
			window.setTimeout(qx.lang.Function.bind(function () {
				fn.apply(this, (args instanceof Array ? args : []));
			}, this), 1);
		},

		/**
		 * Handles the mouse down event and starts the copy-move-paste action, if
		 * control or meta key is pressed.
		 */
		handleMouseDown: function (event) {
			if (this._controlPressed) {
				this._controlPressed = false;
				this.editCopy();
				//			console.log("copiedEle: %0",this.clipboard.shapesAsJson)
				//			console.log("mousevent: %o",event)
				this.editPaste();
				event.forceExecution = true;
				this.facade.raiseEvent(event, this.clipboard.shapesAsJson);

			}
		},

		/**
		 * Returns a list of shapes which should be considered while copying.
		 * Besides the shapes of given ones, edges and attached nodes are added to the result set.
		 * If one of the given shape is a child of another given shape, it is not put into the result. 
		 */
		getAllShapesToConsider: function (shapes) {
			var shapesToConsider = []; // only top-level shapes
			var childShapesToConsider = []; // all child shapes of top-level shapes
			shapes.each(qx.lang.Function.bind(function (shape) {
				//Throw away these shapes which have a parent in given shapes
				var isChildShapeOfAnother = shapes.any(function (s2) {
					return s2.hasChildShape(shape);
				});
				if (isChildShapeOfAnother) return;

				// This shape should be considered
				shapesToConsider.push(shape);
				// Consider attached nodes (e.g. intermediate events)
				if (shape instanceof ms123.oryx.core.Node) {
					var attached = shape.getOutgoingNodes();
					attached = attached.findAll(function (a) {
						return !shapes.include(a)
					});
					shapesToConsider = shapesToConsider.concat(attached);
				}

				childShapesToConsider = childShapesToConsider.concat(shape.getChildShapes(true));
			}, this));

			// All edges between considered child shapes should be considered
			// Look for these edges having incoming and outgoing in childShapesToConsider
			var edgesToConsider = this.facade.getCanvas().getChildEdges().select(function (edge) {
				// Ignore if already added
				if (shapesToConsider.include(edge)) return false;
				// Ignore if there are no docked shapes
				if (edge.getAllDockedShapes().size() === 0) return false;
				// True if all docked shapes are in considered child shapes
				return edge.getAllDockedShapes().all(function (shape) {
					// Remember: Edges can have other edges on outgoing, that is why edges must not be included in childShapesToConsider
					return shape instanceof ms123.oryx.core.Edge || childShapesToConsider.include(shape);
				});
			});
			shapesToConsider = shapesToConsider.concat(edgesToConsider);

			return shapesToConsider;
		},

		/**
		 * Performs the cut operation by first copy-ing and then deleting the
		 * current selection.
		 */
		editCut: function () {
			//TODO document why this returns false.
			//TODO document what the magic boolean parameters are supposed to do.
			this.editCopy(false, true);
			this.editDelete(true);
			return false;
		},

		/**
		 * Performs the copy operation.
		 * param {Object} will_not_update ??
		 */
		editCopy: function (will_update, useNoOffset) {
			var selection = this.facade.getSelection();

			//if the selection is empty, do not remove the previously copied elements
			if (selection.length == 0) return;

			this.clipboard.refresh(selection, this.getAllShapesToConsider(selection), this.facade.getCanvas().getStencil().stencilSet().namespace(), useNoOffset);

			if (will_update) this.facade.updateSelection();
		},

		/**
		 * Performs the paste operation.
		 */
		editPaste: function () {
			// Create a new canvas with childShapes 
			//and stencilset namespace to be JSON Import conform
			var canvas = {
				childShapes: this.clipboard.shapesAsJson,
				stencilset: {
					namespace: this.clipboard.SSnamespace
				}
			}
			// Apply json helper to iterate over json object
			qx.lang.Object.mergeWith(canvas, ms123.oryx.core.AbstractShape.JSONHelper);

			var childShapeResourceIds = canvas.getChildShapes(true).pluck("resourceId");
			var outgoings = {};
			// Iterate over all shapes
			canvas.eachChild(qx.lang.Function.bind(function (shape, parent) {
				// Throw away these references where referenced shape isn't copied
				shape.outgoing = shape.outgoing.select(function (out) {
					return childShapeResourceIds.include(out.resourceId);
				});
				shape.outgoing.each(function (out) {
					if (!outgoings[out.resourceId]) {
						outgoings[out.resourceId] = []
					}
					outgoings[out.resourceId].push(shape)
				});

				return shape;
			}, this), true, true);


			// Iterate over all shapes
			canvas.eachChild(qx.lang.Function.bind(function (shape, parent) {

				// Check if there has a valid target
				if (shape.target && !(childShapeResourceIds.include(shape.target.resourceId))) {
					shape.target = undefined;
					shape.targetRemoved = true;
				}

				// Check if the first docker is removed
				if (shape.dockers && shape.dockers.length >= 1 && shape.dockers[0].getDocker && ((shape.dockers[0].getDocker().getDockedShape() && !childShapeResourceIds.include(shape.dockers[0].getDocker().getDockedShape().resourceId)) || !shape.getShape().dockers[0].getDockedShape() && !outgoings[shape.resourceId])) {

					shape.sourceRemoved = true;
				}

				return shape;
			}, this), true, true);


			// Iterate over top-level shapes
			canvas.eachChild(qx.lang.Function.bind(function (shape, parent) {
				// All top-level shapes should get an offset in their bounds
				// Move the shape occording to COPY_MOVE_OFFSET
				if (this.clipboard.useOffset) {
					shape.bounds = {
						lowerRight: {
							x: shape.bounds.lowerRight.x + ms123.oryx.Config.COPY_MOVE_OFFSET,
							y: shape.bounds.lowerRight.y + ms123.oryx.Config.COPY_MOVE_OFFSET
						},
						upperLeft: {
							x: shape.bounds.upperLeft.x + ms123.oryx.Config.COPY_MOVE_OFFSET,
							y: shape.bounds.upperLeft.y + ms123.oryx.Config.COPY_MOVE_OFFSET
						}
					};
				}
				// Only apply offset to shapes with a target
				if (shape.dockers) {
					shape.dockers = shape.dockers.map(qx.lang.Function.bind(function (docker, i) {
						// If shape had a target but the copied does not have anyone anymore,
						// migrate the relative dockers to absolute ones.
						if ((shape.targetRemoved === true && i == shape.dockers.length - 1 && docker.getDocker) || (shape.sourceRemoved === true && i == 0 && docker.getDocker)) {

							docker = docker.getDocker().bounds.center();
						}

						// If it is the first docker and it has a docked shape, 
						// just return the coordinates
						if ((i == 0 && docker.getDocker instanceof Function && shape.sourceRemoved !== true && (docker.getDocker().getDockedShape() || ((outgoings[shape.resourceId] || []).length > 0 && (!(shape.getShape() instanceof ms123.oryx.core.Node) || outgoings[shape.resourceId][0].getShape() instanceof ms123.oryx.core.Node)))) || (i == shape.dockers.length - 1 && docker.getDocker instanceof Function && shape.targetRemoved !== true && (docker.getDocker().getDockedShape() || shape.target))) {

							return {
								x: docker.x,
								y: docker.y,
								getDocker: docker.getDocker
							}
						} else if (this.clipboard.useOffset) {
							return {
								x: docker.x + ms123.oryx.Config.COPY_MOVE_OFFSET,
								y: docker.y + ms123.oryx.Config.COPY_MOVE_OFFSET,
								getDocker: docker.getDocker
							};
						} else {
							return {
								x: docker.x,
								y: docker.y,
								getDocker: docker.getDocker
							};
						}
					}, this));

				} else if (shape.getShape() instanceof ms123.oryx.core.Node && shape.dockers && shape.dockers.length > 0 && (!shape.dockers.first().getDocker || shape.sourceRemoved === true || !(shape.dockers.first().getDocker().getDockedShape() || outgoings[shape.resourceId]))) {

					shape.dockers = shape.dockers.map(qx.lang.Function.bind(function (docker, i) {

						if ((shape.sourceRemoved === true && i == 0 && docker.getDocker)) {
							docker = docker.getDocker().bounds.center();
						}

						if (this.clipboard.useOffset) {
							return {
								x: docker.x + ms123.oryx.Config.COPY_MOVE_OFFSET,
								y: docker.y + ms123.oryx.Config.COPY_MOVE_OFFSET,
								getDocker: docker.getDocker
							};
						} else {
							return {
								x: docker.x,
								y: docker.y,
								getDocker: docker.getDocker
							};
						}
					}, this));
				}

				return shape;
			}, this), false, true);

			this.clipboard.useOffset = true;
			this.facade.importJSON(canvas,false,true);
		},

		/**
		 * Performs the delete operation. No more asking.
		 */
		editDelete: function () {
			var selection = this.facade.getSelection();

			var clipboard = new ms123.graphicaleditor.plugins.EditClipBoard();
			clipboard.refresh(selection, this.getAllShapesToConsider(selection));

			var command = new ms123.graphicaleditor.plugins.EditDeleteCommand(clipboard, this.facade);

			this.facade.executeCommands([command]);
		},

		__getResourceUrl: function (name) {
			var am = qx.util.AliasManager.getInstance();
			return am.resolve("resource/ms123/" + name);
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
