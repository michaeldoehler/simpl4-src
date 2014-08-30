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
/*
*/

qx.Class.define("ms123.graphicaleditor.plugins.shapemenu.CreateCommand", {
	extend: ms123.oryx.core.Command,
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (option, currentReference, position, plugin) {
		this.base(arguments);
		this.option = option;
		this.currentReference = currentReference;
		this.position = position;
		this.plugin = plugin;
		this.shape;
		this.edge;
		this.targetRefPos;
		this.sourceRefPos;
/*
		 * clone options parameters
		 */
		this.connectedShape = option.connectedShape;
		this.connectingType = option.connectingType;
		this.namespace = option.namespace;
		this.type = option.type;
		this.containedStencil = option.containedStencil;
		this.parent = option.parent;
		this.currentReference = currentReference;
		this.shapeOptions = option.shapeOptions;
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
			var resume = false;
			if (this.shape) {
				if (this.shape instanceof ms123.oryx.core.Node) {
					this.parent.add(this.shape);
					if (this.edge) {
						this.plugin.facade.getCanvas().add(this.edge);
						this.edge.dockers.first().setDockedShape(this.connectedShape);
						this.edge.dockers.first().setReferencePoint(this.sourceRefPos);
						this.edge.dockers.last().setDockedShape(this.shape);
						this.edge.dockers.last().setReferencePoint(this.targetRefPos);
					}

					this.plugin.facade.setSelection([this.shape]);

				} else if (this.shape instanceof ms123.oryx.core.Edge) {
					this.plugin.facade.getCanvas().add(this.shape);
					this.shape.dockers.first().setDockedShape(this.connectedShape);
					this.shape.dockers.first().setReferencePoint(this.sourceRefPos);
				}
				resume = true;
			} else {
				this.shape = this.plugin.facade.createShape(this.option);
				this.edge = (!(this.shape instanceof ms123.oryx.core.Edge)) ? this.shape.getIncomingShapes().first() : undefined;
			}

			if (this.currentReference && this.position) {
				if (this.shape instanceof ms123.oryx.core.Edge) {
					if (!(this.currentReference instanceof ms123.oryx.core.Canvas)) {
						this.shape.dockers.last().setDockedShape(this.currentReference);

						// @deprecated It now uses simply the midpoint
						var upL = this.currentReference.absoluteXY();
						var refPos = {
							x: this.position.x - upL.x,
							y: this.position.y - upL.y
						};

						this.shape.dockers.last().setReferencePoint(this.currentReference.bounds.midPoint());
					}
					else {
						this.shape.dockers.last().bounds.centerMoveTo(this.position);
						//this.shape.dockers.last().update();
					}
					this.sourceRefPos = this.shape.dockers.first().referencePoint;
					this.targetRefPos = this.shape.dockers.last().referencePoint;

				} else if (this.edge) {
					this.sourceRefPos = this.edge.dockers.first().referencePoint;
					this.targetRefPos = this.edge.dockers.last().referencePoint;
				}
			} else {
				var containedStencil = this.containedStencil;
				var connectedShape = this.connectedShape;
				var bc = connectedShape.bounds;
				var bs = this.shape.bounds;

				var pos = bc.center();
				if (containedStencil.defaultAlign() === "north") {
					pos.y -= (bc.height() / 2) + ms123.oryx.Config.SHAPEMENU_CREATE_OFFSET + (bs.height() / 2);
				} else if (containedStencil.defaultAlign() === "northeast") {
					pos.x += (bc.width() / 2) + ms123.oryx.Config.SHAPEMENU_CREATE_OFFSET_CORNER + (bs.width() / 2);
					pos.y -= (bc.height() / 2) + ms123.oryx.Config.SHAPEMENU_CREATE_OFFSET_CORNER + (bs.height() / 2);
				} else if (containedStencil.defaultAlign() === "southeast") {
					pos.x += (bc.width() / 2) + ms123.oryx.Config.SHAPEMENU_CREATE_OFFSET_CORNER + (bs.width() / 2);
					pos.y += (bc.height() / 2) + ms123.oryx.Config.SHAPEMENU_CREATE_OFFSET_CORNER + (bs.height() / 2);
				} else if (containedStencil.defaultAlign() === "south") {
					pos.y += (bc.height() / 2) + ms123.oryx.Config.SHAPEMENU_CREATE_OFFSET + (bs.height() / 2);
				} else if (containedStencil.defaultAlign() === "southwest") {
					pos.x -= (bc.width() / 2) + ms123.oryx.Config.SHAPEMENU_CREATE_OFFSET_CORNER + (bs.width() / 2);
					pos.y += (bc.height() / 2) + ms123.oryx.Config.SHAPEMENU_CREATE_OFFSET_CORNER + (bs.height() / 2);
				} else if (containedStencil.defaultAlign() === "west") {
					pos.x -= (bc.width() / 2) + ms123.oryx.Config.SHAPEMENU_CREATE_OFFSET + (bs.width() / 2);
				} else if (containedStencil.defaultAlign() === "northwest") {
					pos.x -= (bc.width() / 2) + ms123.oryx.Config.SHAPEMENU_CREATE_OFFSET_CORNER + (bs.width() / 2);
					pos.y -= (bc.height() / 2) + ms123.oryx.Config.SHAPEMENU_CREATE_OFFSET_CORNER + (bs.height() / 2);
				} else {
					pos.x += (bc.width() / 2) + ms123.oryx.Config.SHAPEMENU_CREATE_OFFSET + (bs.width() / 2);
				}

				// Move shape to the new position
				this.shape.bounds.centerMoveTo(pos);

				// Move all dockers of a node to the position
				if (this.shape instanceof ms123.oryx.core.Node) {
					(this.shape.dockers || []).each(function (docker) {
						docker.bounds.centerMoveTo(pos);
					})
				}

				//this.shape.update();
				this.position = pos;

				if (this.edge) {
					this.sourceRefPos = this.edge.dockers.first().referencePoint;
					this.targetRefPos = this.edge.dockers.last().referencePoint;
				}
			}

			this.plugin.facade.getCanvas().update();
			this.plugin.facade.updateSelection();

			if (!resume) {
				// If there is a connected shape
				if (this.edge) {
					// Try to layout it
					this.plugin.doLayout(this.edge);
				} else if (this.shape instanceof ms123.oryx.core.Edge) {
					// Try to layout it
					this.plugin.doLayout(this.shape);
				}
			}

		},
		rollback: function () {
			this.plugin.facade.deleteShape(this.shape);
			if (this.edge) {
				this.plugin.facade.deleteShape(this.edge);
			}
			//this.currentParent.update();
			this.plugin.facade.setSelection(this.plugin.facade.getSelection().without(this.shape, this.edge));
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
