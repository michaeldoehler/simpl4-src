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
/**
	* @ignore(Hash)
	* @ignore($H)
	* @ignore(Clazz)
*/

qx.Class.define("ms123.graphicaleditor.plugins.bpmn.ResizeLanesCommand", {
	extend: ms123.oryx.core.Command,
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (shape, parent, pool, plugin) {
		this.base(arguments);

		this.facade = plugin.facade;
		this.plugin = plugin;
		this.shape = shape;
		this.changes;

		this.pool = pool;

		this.parent = parent;


		this.shapeChildren = [];

		/**
		 * The Bounds have to be stored 
		 * separate because they would
		 * otherwise also be influenced 
		 */
		this.shape.getChildShapes().each((function (childShape) {
			this.shapeChildren.push({
				shape: childShape,
				bounds: {
					a: {
						x: childShape.bounds.a.x,
						y: childShape.bounds.a.y
					},
					b: {
						x: childShape.bounds.b.x,
						y: childShape.bounds.b.y
					}
				}
			});
		}).bind(this));

		this.shapeUpperLeft = this.shape.bounds.upperLeft();
		this.parentHeight = this.parent.bounds.height();

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
		getLeafLanes: function (lane) {
			var childLanes = this.plugin.getLanes(lane).map((function (child) {
				return this.getLeafLanes(child);
			}).bind(this)).flatten();
			return childLanes.length > 0 ? childLanes : [lane];
		},

		findNewLane: function () {

			var lanes = this.plugin.getLanes(this.parent);

			var leafLanes = this.getLeafLanes(this.parent);
			this.lane = leafLanes.find((function (l) {
				return l.bounds.upperLeft().y >= this.shapeUpperLeft.y
			}).bind(this)) || leafLanes.last();
			this.laneUpperLeft = this.lane.bounds.upperLeft();
		},

		execute: function () {

			if (this.changes) {
				this.executeAgain();
				return;
			}

			/**
			 * Rescue all ChildShapes of the deleted
			 * Shape into the lane that takes its 
			 * place 
			 */

			if (!this.lane) {
				this.findNewLane();
			}

			if (this.lane) {

				var laUpL = this.laneUpperLeft;
				var shUpL = this.shapeUpperLeft;

				var depthChange = this.plugin.getDepth(this.lane, this.parent) - 1;

				this.changes = $H({});

				// Selected lane is BELOW the removed lane
				if (laUpL.y >= shUpL.y) {
					this.lane.getChildShapes().each((function (childShape) {

						/**
						 * Cache the changes for rollback
						 */
						if (!this.changes[childShape.getId()]) {
							this.changes[childShape.getId()] = this.computeChanges(childShape, this.lane, this.lane, this.shape.bounds.height());
						}

						childShape.bounds.moveBy(0, this.shape.bounds.height());
					}).bind(this));

					this.plugin.hashChildShapes(this.lane);

					this.shapeChildren.each((function (shapeChild) {
						shapeChild.shape.bounds.set(shapeChild.bounds);
						shapeChild.shape.bounds.moveBy((shUpL.x - 30) - (depthChange * 30), 0);

						/**
						 * Cache the changes for rollback
						 */
						if (!this.changes[shapeChild.shape.getId()]) {
							this.changes[shapeChild.shape.getId()] = this.computeChanges(shapeChild.shape, this.shape, this.lane, 0);
						}

						this.lane.add(shapeChild.shape);

					}).bind(this));

					this.lane.bounds.moveBy(0, shUpL.y - laUpL.y);

					// Selected lane is ABOVE the removed lane	
				} else if (shUpL.y > laUpL.y) {

					this.shapeChildren.each((function (shapeChild) {
						shapeChild.shape.bounds.set(shapeChild.bounds);
						shapeChild.shape.bounds.moveBy((shUpL.x - 30) - (depthChange * 30), this.lane.bounds.height());

						/**
						 * Cache the changes for rollback
						 */
						if (!this.changes[shapeChild.shape.getId()]) {
							this.changes[shapeChild.shape.getId()] = this.computeChanges(shapeChild.shape, this.shape, this.lane, 0);
						}

						this.lane.add(shapeChild.shape);

					}).bind(this));
				}




			}

			/**
			 * Adjust the height of the lanes
			 */
			// Get the height values
			var oldHeight = this.lane.bounds.height();
			var newHeight = this.lane.length === 1 ? this.parentHeight : this.lane.bounds.height() + this.shape.bounds.height();

			// Set height
			this.setHeight(newHeight, oldHeight, this.parent, this.parentHeight, true);

			// Cache all sibling lanes
			//this.changes[this.shape.getId()] = this.computeChanges(this.shape, this.parent, this.parent, 0);
			this.plugin.getLanes(this.parent).each((function (childLane) {
				if (!this.changes[childLane.getId()] && childLane !== this.lane && childLane !== this.shape) {
					this.changes[childLane.getId()] = this.computeChanges(childLane, this.parent, this.parent, 0);
				}
			}).bind(this))

			// Update
			this.update();
		},

		setHeight: function (newHeight, oldHeight, parent, parentHeight, store) {

			// Set heigh of the lane
			this.plugin.setDimensions(this.lane, this.lane.bounds.width(), newHeight);
			this.plugin.hashedBounds[this.pool.id][this.lane.id] = this.lane.absoluteBounds();

			// Adjust child lanes
			this.plugin.adjustHeight(this.plugin.getLanes(parent), this.lane);

			if (store === true) {
				// Store changes
				this.changes[this.shape.getId()] = this.computeChanges(this.shape, parent, parent, 0, oldHeight, newHeight);
			}

			// Set parents height
			this.plugin.setDimensions(parent, parent.bounds.width(), parentHeight);

			if (parent !== this.pool) {
				this.plugin.setDimensions(this.pool, this.pool.bounds.width(), this.pool.bounds.height() + (newHeight - oldHeight));
			}
		},

		update: function () {

			// Hack to prevent the updating of the dockers
			this.plugin.hashedBounds[this.pool.id]["REMOVED"] = true;
			// Update
			//this.facade.getCanvas().update();
		},

		rollback: function () {

			var laUpL = this.laneUpperLeft;
			var shUpL = this.shapeUpperLeft;

			this.changes.each((function (pair) {

				var parent = pair.value.oldParent;
				var shape = pair.value.shape;
				var parentHeight = pair.value.parentHeight;
				var oldHeight = pair.value.oldHeight;
				var newHeight = pair.value.newHeight;

				// Move siblings
				if (shape.getStencil().id().endsWith("Lane")) {
					shape.bounds.moveTo(pair.value.oldPosition);
				}

				// If lane
				if (oldHeight) {
					this.setHeight(oldHeight, newHeight, parent, parent.bounds.height() + (oldHeight - newHeight));
					if (laUpL.y >= shUpL.y) {
						this.lane.bounds.moveBy(0, this.shape.bounds.height() - 1);
					}
				} else {
					parent.add(shape);
					shape.bounds.moveTo(pair.value.oldPosition);

				}


			}).bind(this));

			// Update
			//this.update();
		},

		executeAgain: function () {

			this.changes.each((function (pair) {
				var parent = pair.value.newParent;
				var shape = pair.value.shape;
				var newHeight = pair.value.newHeight;
				var oldHeight = pair.value.oldHeight;

				// If lane
				if (newHeight) {
					var laUpL = this.laneUpperLeft.y;
					var shUpL = this.shapeUpperLeft.y;

					if (laUpL >= shUpL) {
						this.lane.bounds.moveBy(0, shUpL - laUpL);
					}
					this.setHeight(newHeight, oldHeight, parent, parent.bounds.height() + (newHeight - oldHeight));
				} else {
					parent.add(shape);
					shape.bounds.moveTo(pair.value.newPosition);
				}

			}).bind(this));

			// Update
			this.update();
		},

		computeChanges: function (shape, oldParent, parent, yOffset, oldHeight, newHeight) {

			oldParent = this.changes[shape.getId()] ? this.changes[shape.getId()].oldParent : oldParent;
			var oldPosition = this.changes[shape.getId()] ? this.changes[shape.getId()].oldPosition : shape.bounds.upperLeft();

			var sUl = shape.bounds.upperLeft();

			var pos = {
				x: sUl.x,
				y: sUl.y + yOffset
			};

			var changes = {
				shape: shape,
				parentHeight: oldParent.bounds.height(),
				oldParent: oldParent,
				oldPosition: oldPosition,
				oldHeight: oldHeight,
				newParent: parent,
				newPosition: pos,
				newHeight: newHeight
			};

			return changes;
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
