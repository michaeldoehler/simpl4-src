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
	* @ignore(Clazz.extend)
*/

qx.Class.define("ms123.graphicaleditor.plugins.bpmn.Plugin", {
	extend: qx.core.Object,
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (facade) {
		this.base(arguments);
		this.facade = facade;

		this.facade.registerOnEvent(ms123.oryx.Config.EVENT_DRAGDOCKER_DOCKED, this.handleDockerDocked.bind(this));
		this.facade.registerOnEvent(ms123.oryx.Config.EVENT_PROPWINDOW_PROP_CHANGED, this.handlePropertyChanged.bind(this));
		this.facade.registerOnEvent('layout.bpmn2_0.pool', this.handleLayoutPool.bind(this));
		this.facade.registerOnEvent('layout.bpmn2_0.subprocess', this.handleSubProcess.bind(this));
		this.facade.registerOnEvent(ms123.oryx.Config.EVENT_SHAPEREMOVED, this.handleShapeRemove.bind(this));
		this.facade.registerOnEvent(ms123.oryx.Config.EVENT_LOADED, this.afterLoad.bind(this));
		this.facade.registerOnEvent(ms123.oryx.Config.EVENT_SELECTION_CHANGED, this.onSelectionChanged.bind(this));

		this.namespace = undefined;
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
		 * Force to update every pool
		 */
		afterLoad: function () {
			this.facade.getCanvas().getChildNodes().each((function (shape) {
				if (shape.getStencil().id().endsWith("Pool")) {
					this.handleLayoutPool({
						shape: shape
					});
				}
			}).bind(this))
		},

		/**
		 * If a pool is selected and contains no lane,
		 * a lane is created automagically
		 */
		onSelectionChanged: function (event) {
			var selection = event.elements;

			if (selection && selection.length === 1) {
				var namespace = this.getNamespace();
				var shape = selection[0];
				if (shape.getStencil().idWithoutNs() === "Pool") {
					if (shape.getChildNodes().length === 0) {
						// create a lane inside the selected pool
						var option = {
							type: namespace + "Lane",
							position: {
								x: 0,
								y: 0
							},
							namespace: shape.getStencil().namespace(),
							parent: shape
						};
						this.facade.createShape(option);
						this.facade.getCanvas().update();
						this.facade.setSelection([shape]);
					}
				}
			}

			// Preventing selection of all lanes but not the pool
			if (selection.any(function (s) {
				return s instanceof ms123.oryx.core.Node && s.getStencil().id().endsWith("Lane")
			})) {
				var lanes = selection.findAll(function (s) {
					return s instanceof ms123.oryx.core.Node && s.getStencil().id().endsWith("Lane")
				});

				var pools = [];
				var unselectLanes = [];
				lanes.each((function (lane) {
					pools.push(this.getParentPool(lane))
				}).bind(this));

				pools = pools.uniq().findAll((function (pool) {
					var childLanes = this.getLanes(pool, true);
					if (childLanes.all(function (lane) {
						return lanes.include(lane)
					})) {
						unselectLanes = unselectLanes.concat(childLanes);
						return true;
					} else if (selection.include(pool) && childLanes.any(function (lane) {
						return lanes.include(lane)
					})) {
						unselectLanes = unselectLanes.concat(childLanes);
						return true;
					} else {
						return false;
					}
				}).bind(this))

				if (unselectLanes.length > 0 && pools.length > 0) {
					selection = selection.without.apply(selection, unselectLanes);
					selection = selection.concat(pools);
					this.facade.setSelection(selection.uniq());
				}
			}
		},

		handleShapeRemove: function (option) {

			var sh = option.shape;
			var parent = option.parent;

			if (sh instanceof ms123.oryx.core.Node && sh.getStencil().idWithoutNs() === "Lane" && this.facade.isExecutingCommands()) {

				var pool = this.getParentPool(parent);
				if (pool && pool.parent) {

					var isLeafFn = function (leaf) {
						return !leaf.getChildNodes().any(function (r) {
							return r.getStencil().idWithoutNs() === "Lane"
						});
					}

					var isLeaf = isLeafFn(sh);
					var parentHasMoreLanes = parent.getChildNodes().any(function (r) {
						return r.getStencil().idWithoutNs() === "Lane"
					});

					if (isLeaf && parentHasMoreLanes) {

						var command = new ms123.graphicaleditor.plugins.bpmn.ResizeLanesCommand(sh, parent, pool, this);
						this.facade.executeCommands([command]);

					} else if (!isLeaf && !this.facade.getSelection().any(function (select) { // Find one of the selection, which is a lane and child of "sh" and is a leaf lane
						return select instanceof ms123.oryx.core.Node && select.getStencil().idWithoutNs() === "Lane" && select.isParent(sh) && isLeafFn(select);
					})) {

						var Command = Clazz.extend({
							construct: function (shape, facade) {
								this.children = shape.getChildNodes(true);
								this.facade = facade;
							},
							execute: function () {
								this.children.each(function (child) {
									child.bounds.moveBy(30, 0)
								});
								//this.facade.getCanvas().update();
							},
							rollback: function () {
								this.children.each(function (child) {
									child.bounds.moveBy(-30, 0)
								})
								//this.facade.getCanvas().update();
							}
						});
						this.facade.executeCommands([new Command(sh, this.facade)]);

					} else if (isLeaf && !parentHasMoreLanes && parent == pool) {
						parent.add(sh);
					}
				}

			}

		},

		hashedSubProcesses: {},

		hashChildShapes: function (shape) {
			var children = shape.getChildNodes();
			children.each((function (child) {
				if (this.hashedSubProcesses[child.id]) {
					this.hashedSubProcesses[child.id] = child.absoluteXY();
					this.hashedSubProcesses[child.id].width = child.bounds.width();
					this.hashedSubProcesses[child.id].height = child.bounds.height();
					this.hashChildShapes(child);
				}
			}).bind(this));
		},

		/**
		 * Handle the layouting of a sub process.
		 * Mainly to adjust the child dockers of a sub process. 
		 *
		 */
		handleSubProcess: function (option) {

			var sh = option.shape;

			if (!this.hashedSubProcesses[sh.id]) {
				this.hashedSubProcesses[sh.id] = sh.absoluteXY();
				this.hashedSubProcesses[sh.id].width = sh.bounds.width();
				this.hashedSubProcesses[sh.id].height = sh.bounds.height();
				return;
			}

			var offset = sh.absoluteXY();
			offset.x -= this.hashedSubProcesses[sh.id].x;
			offset.y -= this.hashedSubProcesses[sh.id].y;

			var resized = this.hashedSubProcesses[sh.id].width !== sh.bounds.width() || this.hashedSubProcesses[sh.id].height !== sh.bounds.height();

			this.hashedSubProcesses[sh.id] = sh.absoluteXY();
			this.hashedSubProcesses[sh.id].width = sh.bounds.width();
			this.hashedSubProcesses[sh.id].height = sh.bounds.height();
			this.hashChildShapes(sh);


			// Move dockers only if currently is not resizing
			if (this.facade.isExecutingCommands() && !resized) {
				this.moveChildDockers(sh, offset);
			}
		},

		moveChildDockers: function (shape, offset) {

			if (!offset.x && !offset.y) {
				return;
			}

			var children = shape.getChildNodes(true);

			// Get all nodes
			var dockers = children
			// Get all incoming and outgoing edges
			.map(function (node) {
				return [].concat(node.getIncomingShapes()).concat(node.getOutgoingShapes())
			})
			// Flatten all including arrays into one
			.flatten()
			// Get every edge only once
			.uniq()
			// Get all dockers
			.map(function (edge) {
				return edge.dockers.length > 2 ? edge.dockers.slice(1, edge.dockers.length - 1) : [];
			})
			// Flatten the dockers lists
			.flatten();

			var abs = shape.absoluteBounds();
			abs.moveBy(-offset.x, -offset.y)
			var obj = {};
			dockers.each(function (docker) {

				if (docker.isChanged) {
					return;
				}

				var off = Object.clone(offset);

				if (!abs.isIncluded(docker.bounds.center())) {
					var index = docker.parent.dockers.indexOf(docker);
					var size = docker.parent.dockers.length;
					var from = docker.parent.getSource();
					var to = docker.parent.getTarget();

					var bothAreIncluded = children.include(from) && children.include(to);

					if (!bothAreIncluded) {
						var previousIsOver = index !== 0 ? abs.isIncluded(docker.parent.dockers[index - 1].bounds.center()) : false;
						var nextIsOver = index !== size - 1 ? abs.isIncluded(docker.parent.dockers[index + 1].bounds.center()) : false;

						if (!previousIsOver && !nextIsOver) {
							return;
						}

						var ref = docker.parent.dockers[previousIsOver ? index - 1 : index + 1];
						if (Math.abs(-Math.abs(ref.bounds.center().x - docker.bounds.center().x)) < 2) {
							off.y = 0;
						} else if (Math.abs(-Math.abs(ref.bounds.center().y - docker.bounds.center().y)) < 2) {
							off.x = 0;
						} else {
							return;
						}
					}

				}

				obj[docker.getId()] = {
					docker: docker,
					offset: off
				}
			})

			// Set dockers
			this.facade.executeCommands([new ms123.oryx.core.MoveDockersCommand(obj)]);

		},

		/**
		 * DragDocker.Docked Handler
		 *
		 */
		handleDockerDocked: function (options) {
			var namespace = this.getNamespace();

			var edge = options.parent;
			var edgeSource = options.target;

			if (edge.getStencil().id() === namespace + "SequenceFlow") {
				var isGateway = edgeSource.getStencil().groups().find(function (group) {
					if (group == "Gateways") return group;
				});
				if (!isGateway && (edge.properties["oryx-conditiontype"] == "Expression"))
				// show diamond on edge source
				edge.setProperty("oryx-showdiamondmarker", true);
				else
				// do not show diamond on edge source
				edge.setProperty("oryx-showdiamondmarker", false);

				// update edge rendering
				//edge.update();
				this.facade.getCanvas().update();
			}
		},

		/**
		 * PropertyWindow.PropertyChanged Handler
		 */
		handlePropertyChanged: function (option) {
			var namespace = this.getNamespace();

			var shapes = option.elements;
			var propertyKey = option.key;
			var propertyValue = option.value;

			var changed = false;
			shapes.each((function (shape) {
				if ((shape.getStencil().id() === namespace + "SequenceFlow") && (propertyKey === "oryx-conditiontype")) {

					if (propertyValue != "Expression")
					// Do not show the Diamond
					shape.setProperty("oryx-showdiamondmarker", false);
					else {
						var incomingShapes = shape.getIncomingShapes();

						if (!incomingShapes) {
							shape.setProperty("oryx-showdiamondmarker", true);
						}

						var incomingGateway = incomingShapes.find(function (aShape) {
							var foundGateway = aShape.getStencil().groups().find(function (group) {
								if (group == "Gateways") return group;
							});
							if (foundGateway) return foundGateway;
						});

						if (!incomingGateway)
						// show diamond on edge source
						shape.setProperty("oryx-showdiamondmarker", true);
						else
						// do not show diamond
						shape.setProperty("oryx-showdiamondmarker", false);
					}

					changed = true;
				}
			}).bind(this));

			if (changed) {
				this.facade.getCanvas().update();
			}

		},

		hashedPoolPositions: {},
		hashedLaneDepth: {},
		hashedBounds: {},
		hashedPositions: {},

		/**
		 * Handler for layouting event 'layout.bpmn2_0.pool'
		 * param {Object} event
		 */
		handleLayoutPool: function (event) {


			var pool = event.shape;
			var selection = this.facade.getSelection();
			var currentShape = selection.include(pool) ? pool : selection.first();

			currentShape = currentShape || pool;

			this.currentPool = pool;

			// Check if it is a pool or a lane
			if (!(currentShape.getStencil().id().endsWith("Pool") || currentShape.getStencil().id().endsWith("Lane"))) {
				return;
			}

			// Check if the lane is within the pool and is not removed lately 
			if (currentShape !== pool && !currentShape.isParent(pool) && !this.hashedBounds[pool.id][currentShape.id]) {
				return;
			}


			if (!this.hashedBounds[pool.id]) {
				this.hashedBounds[pool.id] = {};
			}

			// Find all child lanes
			var lanes = this.getLanes(pool);

			if (lanes.length <= 0) {
				return
			}

			var allLanes = this.getLanes(pool, true),
				hp;
			var considerForDockers = allLanes.clone();

			var hashedPositions = $H({});
			allLanes.each(function (lane) {
				hashedPositions[lane.id] = lane.bounds.upperLeft();
			})



			// Show/hide caption regarding the number of lanes
			if (lanes.length === 1 && this.getLanes(lanes.first()).length <= 0) {
				// TRUE if there is a caption
				lanes.first().setProperty("oryx-showcaption", lanes.first().properties["oryx-name"].trim().length > 0);
				var rect = lanes.first().node.getElementsByTagName("rect");
				rect[0].setAttributeNS(null, "display", "none");
			} else {
				allLanes.invoke("setProperty", "oryx-showcaption", true);
				allLanes.each(function (lane) {
					var rect = lane.node.getElementsByTagName("rect");
					rect[0].removeAttributeNS(null, "display");
				})
			}

			var deletedLanes = [];
			var addedLanes = [];

			// Get all new lanes
			var i = -1;
			while (++i < allLanes.length) {
				if (!this.hashedBounds[pool.id][allLanes[i].id]) {
					addedLanes.push(allLanes[i])
				}
			}

			if (addedLanes.length > 0) {
				currentShape = addedLanes.first();
			}


			// Get all deleted lanes
			var resourceIds = $H(this.hashedBounds[pool.id]).keys();
			var i = -1;
			while (++i < resourceIds.length) {
				if (!allLanes.any(function (lane) {
					return lane.id == resourceIds[i]
				})) {
					deletedLanes.push(this.hashedBounds[pool.id][resourceIds[i]]);
					selection = selection.without(function (r) {
						return r.id == resourceIds[i]
					});
				}
			}

			var height, width, x, y;

			if (deletedLanes.length > 0 || addedLanes.length > 0) {

				if (addedLanes.length === 1 && this.getLanes(addedLanes[0].parent).length === 1) {
					// Set height from the pool
					height = this.adjustHeight(lanes, addedLanes[0].parent);
				} else {
					// Set height from the pool
					height = this.updateHeight(pool);
				}
				// Set width from the pool
				width = this.adjustWidth(lanes, pool.bounds.width());

				pool.update();
			}

			/**
			 * Set width/height depending on the pool
			 */
			else if (pool == currentShape) {

				if (selection.length === 1 && this.isResized(pool, this.hashedPoolPositions[pool.id])) {
					var oldXY = this.hashedPoolPositions[pool.id].upperLeft();
					var xy = pool.bounds.upperLeft();
					var scale = 0;
					if (this.shouldScale(pool)) {
						var old = this.hashedPoolPositions[pool.id];
						scale = old.height() / pool.bounds.height();
					}

					this.adjustLanes(pool, allLanes, oldXY.x - xy.x, oldXY.y - xy.y, scale);
				}

				// Set height from the pool
				height = this.adjustHeight(lanes, undefined, pool.bounds.height());
				// Set width from the pool
				width = this.adjustWidth(lanes, pool.bounds.width());
			}

			/**‚
			 * Set width/height depending on containing lanes
			 */
			else {

				// Reposition the pool if one shape is selected and the upperleft has changed
				if (selection.length === 1 && this.isResized(currentShape, this.hashedBounds[pool.id][currentShape.id])) {
					var oldXY = this.hashedBounds[pool.id][currentShape.id].upperLeft();
					var xy = currentShape.absoluteXY();
					x = oldXY.x - xy.x;
					y = oldXY.y - xy.y;

					// Adjust all other lanes beneath this lane
					if (x || y) {
						considerForDockers = considerForDockers.without(currentShape);
						this.adjustLanes(pool, this.getAllExcludedLanes(pool, currentShape), x, 0);
					}

					// Adjust all child lanes
					var childLanes = this.getLanes(currentShape, true);
					if (childLanes.length > 0) {
						if (this.shouldScale(currentShape)) {
							var old = this.hashedBounds[pool.id][currentShape.id];
							var scale = old.height() / currentShape.bounds.height();
							this.adjustLanes(pool, childLanes, x, y, scale);
						} else {
							this.adjustLanes(pool, childLanes, x, y, 0);
						}
					}
				}

				// Cache all bounds
				var changes = allLanes.map(function (lane) {
					return {
						shape: lane,
						bounds: lane.bounds.clone()
					}
				});

				// Get height and adjust child heights
				height = this.adjustHeight(lanes, currentShape);
				// Check if something has changed and maybe create a command
				this.checkForChanges(allLanes, changes);

				// Set width from the current shape
				width = this.adjustWidth(lanes, currentShape.bounds.width() + (this.getDepth(currentShape, pool) * 30));
			}

			this.setDimensions(pool, width, height, x, y);


			if (this.facade.isExecutingCommands() && (deletedLanes.length === 0 || addedLanes.length !== 0)) {
				// Update all dockers
				this.updateDockers(considerForDockers, pool);

				// Check if the order has changed
				if (this.hashedPositions[pool.id] && this.hashedPositions[pool.id].keys().any(function (key, i) {
					return (allLanes[i] || {}).id !== key;
				})) {

					var LanesHasBeenReordered = Clazz.extend({
						construct: function (originPosition, newPosition, lanes, plugin, poolId) {
							this.originPosition = Object.clone(originPosition);
							this.newPosition = Object.clone(newPosition);
							this.lanes = lanes;
							this.plugin = plugin;
							this.pool = poolId;
						},
						execute: function () {
							if (!this.executed) {
								this.executed = true;
								this.lanes.each((function (lane) {
									if (this.newPosition[lane.id]) lane.bounds.moveTo(this.newPosition[lane.id])
								}).bind(this));
								this.plugin.hashedPositions[this.pool] = Object.clone(this.newPosition);
							}
						},
						rollback: function () {
							this.lanes.each((function (lane) {
								if (this.originPosition[lane.id]) lane.bounds.moveTo(this.originPosition[lane.id])
							}).bind(this));
							this.plugin.hashedPositions[this.pool] = Object.clone(this.originPosition);
						}
					});

					var hp2 = $H({});
					allLanes.each(function (lane) {
						hp2[lane.id] = lane.bounds.upperLeft();
					})

					var command = new LanesHasBeenReordered(hashedPositions, hp2, allLanes, this, pool.id);
					this.facade.executeCommands([command]);

				}
			}

			this.hashedBounds[pool.id] = {};
			this.hashedPositions[pool.id] = hashedPositions;

			var i = -1;
			while (++i < allLanes.length) {
				// Cache positions
				this.hashedBounds[pool.id][allLanes[i].id] = allLanes[i].absoluteBounds();

				// Cache also the bounds of child shapes, mainly for child subprocesses
				this.hashChildShapes(allLanes[i]);

				this.hashedLaneDepth[allLanes[i].id] = this.getDepth(allLanes[i], pool);

				this.forceToUpdateLane(allLanes[i]);
			}

			this.hashedPoolPositions[pool.id] = pool.bounds.clone();


			// Update selection
			//this.facade.setSelection(selection);		
		},

		shouldScale: function (element) {
			var childLanes = element.getChildNodes().findAll(function (shape) {
				return shape.getStencil().id().endsWith("Lane")
			})
			return childLanes.length > 1 || childLanes.any((function (lane) {
				return this.shouldScale(lane)
			}).bind(this))
		},

		/**
		 * Lookup if some bounds has changed
		 * param {Object} lanes
		 * param {Object} changes
		 */
		checkForChanges: function (lanes, changes) {
			// Check if something has changed
			if (this.facade.isExecutingCommands() && changes.any(function (change) {
				return change.shape.bounds.toString() !== change.bounds.toString();
			})) {

				var Command = Clazz.extend({
					construct: function (changes) {
						this.oldState = changes;
						this.newState = changes.map(function (s) {
							return {
								shape: s.shape,
								bounds: s.bounds.clone()
							}
						});
					},
					execute: function () {
						if (this.executed) {
							this.applyState(this.newState);
						}
						this.executed = true;
					},
					rollback: function () {
						this.applyState(this.oldState);
					},
					applyState: function (state) {
						state.each(function (s) {
							s.shape.bounds.set(s.bounds.upperLeft(), s.bounds.lowerRight());
						})
					}
				});

				this.facade.executeCommands([new Command(changes)]);
			}
		},

		isResized: function (shape, bounds) {

			if (!bounds || !shape) {
				return false;
			}

			var oldB = bounds;
			//var oldXY = oldB.upperLeft();
			//var xy = shape.absoluteXY();
			return Math.round(oldB.width() - shape.bounds.width()) !== 0 || Math.round(oldB.height() - shape.bounds.height()) !== 0
		},

		adjustLanes: function (pool, lanes, x, y, scale) {

			scale = scale || 0;

			// For every lane, adjust the child nodes with the offset
			lanes.each((function (l) {
				l.getChildNodes().each((function (child) {
					if (!child.getStencil().id().endsWith("Lane")) {
						var cy = scale ? child.bounds.center().y - (child.bounds.center().y / scale) : -y;
						child.bounds.moveBy((x || 0), -cy);

						if (scale && child.getStencil().id().endsWith("Subprocess")) {
							this.moveChildDockers(child, {
								x: (0),
								y: -cy
							});
						}

					}
				}).bind(this));
				this.hashedBounds[pool.id][l.id].moveBy(-(x || 0), !scale ? -y : 0);
				if (scale) {
					l.isScaled = true;
				}
			}).bind(this))

		},

		getAllExcludedLanes: function (parent, lane) {
			var lanes = [];
			parent.getChildNodes().each((function (shape) {
				if ((!lane || shape !== lane) && shape.getStencil().id().endsWith("Lane")) {
					lanes.push(shape);
					lanes = lanes.concat(this.getAllExcludedLanes(shape, lane));
				}
			}).bind(this));
			return lanes;
		},


		forceToUpdateLane: function (lane) {

			if (lane.bounds.height() !== lane._svgShapes[0].height) {
				lane.isChanged = true;
				lane.isResized = true;
				lane._update();
			}
		},

		getDepth: function (child, parent) {

			var i = 0;
			while (child && child.parent && child !== parent) {
				child = child.parent;
				++i
			}
			return i;
		},

		/*@@@MSupdateDepth: function (lane, fromDepth, toDepth) {

			var xOffset = (fromDepth - toDepth) * 30;

			lane.getChildNodes().each(function (shape) {
				shape.bounds.moveBy(xOffset, 0);

				[].concat(children[j].getIncomingShapes()).concat(children[j].getOutgoingShapes())

			})

		},*/

		setDimensions: function (shape, width, height, x, y) {
			var isLane = shape.getStencil().id().endsWith("Lane");
			// Set the bounds
			shape.bounds.set(
			isLane ? 30 : (shape.bounds.a.x - (x || 0)), isLane ? shape.bounds.a.y : (shape.bounds.a.y - (y || 0)), width ? shape.bounds.a.x + width - (isLane ? 30 : (x || 0)) : shape.bounds.b.x, height ? shape.bounds.a.y + height - (isLane ? 0 : (y || 0)) : shape.bounds.b.y);
		},

		setLanePosition: function (shape, y) {

			shape.bounds.moveTo(30, y);

		},

		adjustWidth: function (lanes, width) {

			// Set width to each lane
			(lanes || []).each((function (lane) {
				this.setDimensions(lane, width);
				this.adjustWidth(this.getLanes(lane), width - 30);
			}).bind(this));

			return width;
		},


		adjustHeight: function (lanes, changedLane, propagateHeight) {

			var oldHeight = 0;
			if (!changedLane && propagateHeight) {
				var i = -1;
				while (++i < lanes.length) {
					oldHeight += lanes[i].bounds.height();
				}
			}

			var i = -1;
			var height = 0;

			// Iterate trough every lane
			while (++i < lanes.length) {

				if (lanes[i] === changedLane) {
					// Propagate new height down to the children
					this.adjustHeight(this.getLanes(lanes[i]), undefined, lanes[i].bounds.height());

					lanes[i].bounds.set({
						x: 30,
						y: height
					}, {
						x: lanes[i].bounds.width() + 30,
						y: lanes[i].bounds.height() + height
					})

				} else if (!changedLane && propagateHeight) {

					var tempHeight = (lanes[i].bounds.height() * propagateHeight) / oldHeight;
					// Propagate height
					this.adjustHeight(this.getLanes(lanes[i]), undefined, tempHeight);
					// Set height propotional to the propagated and old height
					this.setDimensions(lanes[i], null, tempHeight);
					this.setLanePosition(lanes[i], height);
				} else {
					// Get height from children
					var tempHeight = this.adjustHeight(this.getLanes(lanes[i]), changedLane, propagateHeight);
					if (!tempHeight) {
						tempHeight = lanes[i].bounds.height();
					}
					this.setDimensions(lanes[i], null, tempHeight);
					this.setLanePosition(lanes[i], height);
				}

				height += lanes[i].bounds.height();
			}

			return height;

		},


		updateHeight: function (root) {

			var lanes = this.getLanes(root);

			if (lanes.length == 0) {
				return root.bounds.height();
			}

			var height = 0;
			var i = -1;
			while (++i < lanes.length) {
				this.setLanePosition(lanes[i], height);
				height += this.updateHeight(lanes[i]);
			}

			this.setDimensions(root, null, height);

			return height;
		},

		getOffset: function (lane, includePool, pool) {

			var offset = {
				x: 0,
				y: 0
			};

			var offset = lane.absoluteXY();

			var hashed = this.hashedBounds[pool.id][lane.id] || (includePool === true ? this.hashedPoolPositions[lane.id] : undefined);
			if (hashed) {
				offset.x -= hashed.upperLeft().x;
				offset.y -= hashed.upperLeft().y;
			} else {
				return {
					x: 0,
					y: 0
				}
			}
			return offset;
		},

		getNextLane: function (shape) {
			while (shape && !shape.getStencil().id().endsWith("Lane")) {
				if (shape instanceof ms123.oryx.core.Canvas) {
					return null;
				}
				shape = shape.parent;
			}
			return shape;
		},

		getParentPool: function (shape) {
			while (shape && !shape.getStencil().id().endsWith("Pool")) {
				if (shape instanceof ms123.oryx.core.Canvas) {
					return null;
				}
				shape = shape.parent;
			}
			return shape;
		},

		updateDockers: function (lanes, pool) {

			var absPool = pool.absoluteBounds(),
				movedShapes = [];
			var oldPool = (this.hashedPoolPositions[pool.id] || absPool).clone();

			var i = -1,
				j = -1,
				k = -1,
				l = -1,
				docker;
			var dockers = {};

			while (++i < lanes.length) {

				if (!this.hashedBounds[pool.id][lanes[i].id]) {
					continue;
				}

				var isScaled = lanes[i].isScaled;
				delete lanes[i].isScaled;
				var children = lanes[i].getChildNodes();
				var absBounds = lanes[i].absoluteBounds();
				var oldBounds = (this.hashedBounds[pool.id][lanes[i].id] || absBounds);
				//oldBounds.moveBy((absBounds.upperLeft().x-lanes[i].bounds.upperLeft().x), (absBounds.upperLeft().y-lanes[i].bounds.upperLeft().y));
				var offset = this.getOffset(lanes[i], true, pool);
				var xOffsetDepth = 0;

				var depth = this.getDepth(lanes[i], pool);
				if (this.hashedLaneDepth[lanes[i].id] !== undefined && this.hashedLaneDepth[lanes[i].id] !== depth) {
					xOffsetDepth = (this.hashedLaneDepth[lanes[i].id] - depth) * 30;
					offset.x += xOffsetDepth;
				}

				j = -1;

				while (++j < children.length) {

					if (xOffsetDepth && !children[j].getStencil().id().endsWith("Lane")) {
						movedShapes.push({
							xOffset: xOffsetDepth,
							shape: children[j]
						});
						children[j].bounds.moveBy(xOffsetDepth, 0);
					}

					if (children[j].getStencil().id().endsWith("Subprocess")) {
						this.moveChildDockers(children[j], offset);
					}

					var edges = [].concat(children[j].getIncomingShapes()).concat(children[j].getOutgoingShapes())
					// Remove all edges which are included in the selection from the list
					.findAll(function (r) {
						return r instanceof ms123.oryx.core.Edge
					})

					k = -1;
					while (++k < edges.length) {

						if (edges[k].getStencil().id().endsWith("MessageFlow")) {
							this.layoutEdges(children[j], [edges[k]], offset);
							continue;
						}

						l = -1;
						while (++l < edges[k].dockers.length) {

							docker = edges[k].dockers[l];

							if (docker.getDockedShape() || docker.isChanged) {
								continue;
							}


							var pos = docker.bounds.center();

							// Check if the modified center included the new position
							var isOverLane = oldBounds.isIncluded(pos);
							// Check if the original center is over the pool
							var isOutSidePool = !oldPool.isIncluded(pos);
							var previousIsOverLane = l == 0 ? isOverLane : oldBounds.isIncluded(edges[k].dockers[l - 1].bounds.center());
							var nextIsOverLane = l == edges[k].dockers.length - 1 ? isOverLane : oldBounds.isIncluded(edges[k].dockers[l + 1].bounds.center());
							var off = Object.clone(offset);

							// If the 
							if (isScaled && isOverLane && this.isResized(lanes[i], this.hashedBounds[pool.id][lanes[i].id])) {
								var relY = (pos.y - absBounds.upperLeft().y + off.y);
								off.y -= (relY - (relY * (absBounds.height() / oldBounds.height())));
							}

							// Check if the previous dockers docked shape is from this lane
							// Otherwise, check if the docker is over the lane OR is outside the lane 
							// but the previous/next was over this lane
							if (isOverLane) {
								dockers[docker.id] = {
									docker: docker,
									offset: off
								};
							}
						}
					}

				}
			}

			// Move the moved children 
			var MoveChildCommand = Clazz.extend({
				construct: function (state) {
					this.state = state;
				},
				execute: function () {
					if (this.executed) {
						this.state.each(function (s) {
							s.shape.bounds.moveBy(s.xOffset, 0);
						});
					}
					this.executed = true;
				},
				rollback: function () {
					this.state.each(function (s) {
						s.shape.bounds.moveBy(-s.xOffset, 0);
					});
				}
			})


			// Set dockers
			this.facade.executeCommands([new ms123.oryx.core.MoveDockersCommand(dockers), new MoveChildCommand(movedShapes)]);

		},

		moveBy: function (pos, offset) {
			pos.x += offset.x;
			pos.y += offset.y;
			return pos;
		},

		getHashedBounds: function (shape) {
			return this.currentPool && this.hashedBounds[this.currentPool.id][shape.id] ? this.hashedBounds[this.currentPool.id][shape.id] : shape.absoluteBounds();
		},

		/**
		 * Returns a set on all child lanes for the given Shape. If recursive is TRUE, also indirect children will be returned (default is FALSE)
		 * The set is sorted with first child the lowest y-coordinate and the last one the highest.
		 * param {ms123.oryx.core.Shape} shape
		 * param {boolean} recursive
		 */
		getLanes: function (shape, recursive) {
			var namespace = this.getNamespace();

			// Get all the child lanes
			var lanes = shape.getChildNodes(recursive || false).findAll(function (node) {
				return (node.getStencil().id() === namespace + "Lane");
			});

			// Sort all lanes by there y coordinate
			lanes = lanes.sort((function (a, b) {

				// Get y coordinates for upper left and lower right
				var auy = Math.round(a.bounds.upperLeft().y);
				var buy = Math.round(b.bounds.upperLeft().y);
				var aly = Math.round(a.bounds.lowerRight().y);
				var bly = Math.round(b.bounds.lowerRight().y);

				var ha = this.getHashedBounds(a);
				var hb = this.getHashedBounds(b);

				// Get the old y coordinates
				var oauy = Math.round(ha.upperLeft().y);
				var obuy = Math.round(hb.upperLeft().y);
				var oaly = Math.round(ha.lowerRight().y);
				var obly = Math.round(hb.lowerRight().y);

				// If equal, than use the old one
				if (auy == buy && aly == bly) {
					auy = oauy;
					buy = obuy;
					aly = oaly;
					bly = obly;
				}

				if (Math.round(a.bounds.height() - ha.height()) === 0 && Math.round(b.bounds.height() - hb.height()) === 0) {
					return auy < buy ? -1 : (auy > buy ? 1 : 0);
				}

				// Check if upper left and lower right is completely above/below
				var above = auy < buy && aly < bly;
				var below = auy > buy && aly > bly;
				// Check if a is above b including the old values
				var slightlyAboveBottom = auy < buy && aly >= bly && oaly < obly;
				var slightlyAboveTop = auy >= buy && aly < bly && oauy < obuy;
				// Check if a is below b including the old values
				var slightlyBelowBottom = auy > buy && aly <= bly && oaly > obly;
				var slightlyBelowTop = auy <= buy && aly > bly && oauy > obuy;

				// Return -1 if a is above b, 1 if b is above a, or 0 otherwise
				return (above || slightlyAboveBottom || slightlyAboveTop ? -1 : (below || slightlyBelowBottom || slightlyBelowTop ? 1 : 0))
			}).bind(this));

			// Return lanes
			return lanes;
		},

		getNamespace: function () {
			if (!this.namespace) {
				var stencilsets = this.facade.getStencilSets();
				if (stencilsets.keys()) {
					this.namespace = stencilsets.keys()[0];
				} else {
					return undefined;
				}
			}
			return this.namespace;
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
