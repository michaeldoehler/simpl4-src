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
/**
	* @ignore(Hash)
	* @ignore(Clazz)
	* @ignore(Clazz.extend)
*/

qx.Class.define("ms123.graphicaleditor.plugins.shapemenu.Plugin", {
	extend: ms123.oryx.core.AbstractPlugin,
	include: [qx.locale.MTranslation],
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (facade,editorType) {
		this.base(arguments, facade);
		this.facade = facade;
		this.editorType = editorType;
		this.alignGroups = new Hash();

		var containerNode = this.facade.getCanvas().getHTMLContainer();

		this.shapeMenu = new ms123.graphicaleditor.plugins.shapemenu.Menu(containerNode);
		this.currentShapes = [];

		// Register on dragging and resizing events for show/hide of Menu
		this.facade.registerOnEvent(ms123.oryx.Config.EVENT_DRAGDROP_START, this.hideShapeMenu.bind(this));
		this.facade.registerOnEvent(ms123.oryx.Config.EVENT_DRAGDROP_END, this.showShapeMenu.bind(this));
		this.facade.registerOnEvent(ms123.oryx.Config.EVENT_RESIZE_START, (function () {
			this.hideShapeMenu();
			this.hideMorphMenu();
		}).bind(this));
		this.facade.registerOnEvent(ms123.oryx.Config.EVENT_RESIZE_END, this.showShapeMenu.bind(this));

		this.createdButtons = {};

		this.facade.registerOnEvent(ms123.oryx.Config.EVENT_STENCIL_SET_LOADED, (function () {
			this.registryChanged()
		}).bind(this));

		this.timer = null;

		this.facade.getWidget().setDroppable(true);
		this.resetElements = true;
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

		hideShapeMenu: function (event) {
			window.clearTimeout(this.timer);
			this.timer = null;
			this.shapeMenu.hide();
		},

		showShapeMenu: function (dontGenerateNew) {

			if (!dontGenerateNew || this.resetElements) {

				window.clearTimeout(this.timer);
				this.timer = window.setTimeout(qx.lang.Function.bind(function () {

					// Close all Buttons
					this.shapeMenu.closeAllButtons();

					// Show the Morph Button
					this.showMorphButton(this.currentShapes);

					// Show the Stencil Buttons
					this.showStencilButtons(this.currentShapes);

					// Show the ShapeMenu
					this.shapeMenu.show(this.currentShapes);

					this.resetElements = false;
				}, this), 300)

			} else {

				window.clearTimeout(this.timer);
				this.timer = null;

				// Show the ShapeMenu
				this.shapeMenu.show(this.currentShapes);

			}
		},

		registryChanged: function (pluginsData) {
			if (pluginsData) {
				pluginsData = pluginsData.each(function (value) {
					value.group = value.group ? value.group : 'unknown'
				});
				this.pluginsData = pluginsData.sortBy(function (value) {
					return (value.group + "" + value.index);
				});
			}

			this.shapeMenu.removeAllButtons();
			this.shapeMenu.setNumberOfButtonsPerLevel(ms123.oryx.Config.SHAPEMENU_RIGHT, 2);
			this.createdButtons = {};

			this.createMorphMenu();

			if (!this.pluginsData) {
				this.pluginsData = [];
			}

			this.baseMorphStencils = this.facade.getRules().baseMorphs();

			// Checks if the stencil set has morphing attributes
			var isMorphing = this.facade.getRules().containsMorphingRules();

			// Create Buttons for all Stencils of all loaded stencilsets
			var stencilsets = this.facade.getStencilSets();
			stencilsets.values().each((function (stencilSet) {

				var nodes = stencilSet.nodes();
				nodes.each((function (stencil) {

					// Create a button for each node
					var option = {
						type: stencil.id(),
						namespace: stencil.namespace(),
						connectingType: true
					};
					var button = new ms123.graphicaleditor.plugins.shapemenu.Button({
						callback: this.newShape.bind(this, option),
						icon: this.__getStencilIconUrl(stencil.icon()),
						align: ms123.oryx.Config.SHAPEMENU_RIGHT,
						group: 0,
						//dragcallback: this.hideShapeMenu.bind(this),
						msg: stencil.title() + " - " + this.tr("ge.ShapeMenuPlugin.clickDrag")
					});

					// Add button to shape menu
					this.shapeMenu.addButton(button);

					// Add to the created Button Array
					this.createdButtons[stencil.namespace() + stencil.type() + stencil.id()] = button;

					// Drag'n'Drop will enable
					//Ext.dd.Registry.register(button.node.lastChild, option);

					button.setDraggable(true);
					button.setUserData("option", option);
					button.addListener("drag", this.__dragover, this);
					button.addListener("dragstart", function (e) {
						console.log("DRAG:dragstart:" + button);
						this.facade.getWidget().addListener("drop", this.__drop, this);
						e.addAction("move");
					}, this);
					button.addListener("dragend", function (e) {
						console.log("DRAG:dragend:" + button);
						this.facade.getWidget().removeListener("drop", this.__drop, this);
						this.facade.updateSelection();
					}, this);

				}).bind(this));


				var edges = stencilSet.edges();
				edges.each((function (stencil) {

					// Create a button for each edge
					var option = {
						type: stencil.id(),
						namespace: stencil.namespace()
					};
					var button = new ms123.graphicaleditor.plugins.shapemenu.Button({
						callback: this.newShape.bind(this, option),
						// icon: 		isMorphing ? ORYX.PATH + "images/edges.png" : stencil.icon(),
						icon: this.__getStencilIconUrl(stencil.icon()),
						align: ms123.oryx.Config.SHAPEMENU_RIGHT,
						group: 1,
						//dragcallback: this.hideShapeMenu.bind(this),
						msg: (isMorphing ? this.tr("ge.Edge") : stencil.title()) + " - " + this.tr("ge.ShapeMenuPlugin.drag")
					});

					// Add button to shape menu
					this.shapeMenu.addButton(button);

					// Add to the created Button Array
					this.createdButtons[stencil.namespace() + stencil.type() + stencil.id()] = button;


					// Drag'n'Drop will enable
					//Ext.dd.Registry.register(button.node.lastChild, option);
					button.setDraggable(true);
					button.setUserData("option", option);
					button.addListener("drag", this.__dragover, this);
					button.addListener("dragstart", function (e) {
						console.log("DRAG:dragstart:" + button);
						this.facade.getWidget().addListener("drop", this.__drop, this);
						e.addAction("move");
					}, this);

					button.addListener("dragend", function (e) {
						console.log("DRAG:dragend:" + button);
						this.facade.getWidget().removeListener("drop", this.__drop, this);
						this.facade.updateSelection();
					}, this);
				}).bind(this));

			}).bind(this));

		},

		createMorphMenu: function () {
			this.morphMenu = new qx.ui.menu.Menu();

			// Create the button to show the morph menu
			var button = new ms123.graphicaleditor.plugins.shapemenu.Button({
			//	hovercallback: (ms123.oryx.Config.ENABLE_MORPHMENU_BY_HOVER ? this.showMorphMenu.bind(this) : undefined),
			//	resetcallback: (ms123.oryx.Config.ENABLE_MORPHMENU_BY_HOVER ? this.hideMorphMenu.bind(this) : undefined),
			//	callback: (ms123.oryx.Config.ENABLE_MORPHMENU_BY_HOVER ? undefined : this.toggleMorphMenu.bind(this)),
				icon: this.__getResourceUrl('wrench_orange.png'),
				align: ms123.oryx.Config.SHAPEMENU_BOTTOM,
				group: 0,
				msg: this.tr("ge.ShapeMenuPlugin.morphMsg")
			});

			this.shapeMenu.setNumberOfButtonsPerLevel(ms123.oryx.Config.SHAPEMENU_BOTTOM, 1)
			this.shapeMenu.addButton(button);
			button.setMenu(this.morphMenu);
			this.morphButton = button;
		},

		showMorphMenu: function () {
			return;
			this.morphMenu.show(this.morphButton.node);
			this._morphMenuShown = true;
		},

		hideMorphMenu: function () {
			this.morphMenu.hide();
			this._morphMenuShown = false;
		},

		toggleMorphMenu: function () {
			return;
			if (this._morphMenuShown) this.hideMorphMenu();
			else this.showMorphMenu();
		},

		onSelectionChanged: function (event) {
			var elements = event.elements;

			this.hideShapeMenu();
			this.hideMorphMenu();

			if (this.currentShapes.inspect() !== elements.inspect()) {
				this.currentShapes = elements;
				this.resetElements = true;
				this.showShapeMenu();
			} else {
				this.showShapeMenu(true)
			}

		},

		/**
		 * Show button for morphing the selected shape into another stencil
		 */
		showMorphButton: function (elements) {
			if (elements.length != 1) return;

			var possibleMorphs = this.facade.getRules().morphStencils({
				stencil: elements[0].getStencil()
			});
			possibleMorphs = possibleMorphs.select(qx.lang.Function.bind(function (morph) {
				if (elements[0].getStencil().type() === "node") {
					//check containment rules
					return this.facade.getRules().canContain({
						containingShape: elements[0].parent,
						containedStencil: morph
					});
				} else {
					//check connect rules
					return this.facade.getRules().canConnect({
						sourceShape: elements[0].dockers.first().getDockedShape(),
						edgeStencil: morph,
						targetShape: elements[0].dockers.last().getDockedShape()
					});
				}
			}, this));
			if (possibleMorphs.size() <= 1) return; // if morphing to other stencils is not possible, don't show button
			this.morphMenu.removeAll();

			// populate morph menu with the possible morph stencils ordered by their position
			possibleMorphs = possibleMorphs.sortBy(function (stencil) {
				return stencil.position();
			});
			possibleMorphs.each((function (morph) {
				var menuItem = new qx.ui.menu.Button(morph.title(), this.__getStencilIconUrl(morph.icon()));
				menuItem.addListener("execute", (function () {
					this.morphShape(elements[0], morph);
				}).bind(this));
				menuItem.setEnabled(morph.id() != elements[0].getStencil().id());
				this.morphMenu.add(menuItem);
			}).bind(this));

			this.morphButton.prepareToShow();

		},

		/**
		 * Show buttons for creating following shapes
		 */
		showStencilButtons: function (elements) {

			if (elements.length != 1) return;

			//TODO temporaere nutzung des stencilsets
			var sset = this.facade.getStencilSets()[elements[0].getStencil().namespace()];

			// Get all available edges
			var edges = this.facade.getRules().outgoingEdgeStencils({
				canvas: this.facade.getCanvas(),
				sourceShape: elements[0]
			});

			// And find all targets for each Edge
			var targets = new Array();
			var addedEdges = new Array();

			var isMorphing = this.facade.getRules().containsMorphingRules();

			edges.each((function (edge) {

				if (isMorphing) {
					if (this.baseMorphStencils.include(edge)) {
						var shallAppear = true;
					} else {

						// if edge is member of a morph groups where none of the base morphs is in the outgoing edges
						// we want to display the button (but only for the first one)
						var possibleMorphs = this.facade.getRules().morphStencils({
							stencil: edge
						});

						var shallAppear = !possibleMorphs.any((function (morphStencil) {
							if (this.baseMorphStencils.include(morphStencil) && edges.include(morphStencil)) return true;
							return addedEdges.include(morphStencil);
						}).bind(this));

					}
				}
				if (shallAppear || !isMorphing) {
					if (this.createdButtons[edge.namespace() + edge.type() + edge.id()]) this.createdButtons[edge.namespace() + edge.type() + edge.id()].prepareToShow();
					addedEdges.push(edge);
				}

				// get all targets for this edge
				targets = targets.concat(this.facade.getRules().targetStencils({
					canvas: this.facade.getCanvas(),
					sourceShape: elements[0],
					edgeStencil: edge
				}));

			}).bind(this));

			targets.uniq();

			var addedTargets = new Array();
			// Iterate all possible target 
			targets.each((function (target) {

				if (isMorphing) {

					// continue with next target stencil
					if (target.type() === "edge") return;

					// continue when stencil should not shown in the shape menu
					if (!this.facade.getRules().showInShapeMenu(target)) return;

					// if target is not a base morph 
					if (!this.baseMorphStencils.include(target)) {

						// if target is member of a morph groups where none of the base morphs is in the targets
						// we want to display the button (but only for the first one)
						var possibleMorphs = this.facade.getRules().morphStencils({
							stencil: target
						});
						if (possibleMorphs.size() == 0) return; // continue with next target
						var baseMorphInTargets = possibleMorphs.any((function (morphStencil) {
							if (this.baseMorphStencils.include(morphStencil) && targets.include(morphStencil)) return true;
							return addedTargets.include(morphStencil);
						}).bind(this));

						if (baseMorphInTargets) return; // continue with next target
					}
				}

				// if this is reached the button shall appear in the shape menu:
				if (this.createdButtons[target.namespace() + target.type() + target.id()]) this.createdButtons[target.namespace() + target.type() + target.id()].prepareToShow();
				addedTargets.push(target);

			}).bind(this));

		},


		//beforeDragOver: function (dragZone, target, event) {
		__dragover: function (e) {
		//	console.log("shapeMenu.plugins.__dragover");
			var cursor = qx.ui.core.DragDropCursor.getInstance();

			if (this.shapeMenu.isVisible) {
				this.hideShapeMenu();
			}

			var event = {};
			event.clientX = e.getDocumentLeft();
			event.clientY = e.getDocumentTop();
			var coord = this.facade.eventCoordinates(event);

			var aShapes = this.facade.getCanvas().getAbstractShapesAtPosition(coord);
			if (aShapes.length <= 0) {
				return false;
			}

			var el = aShapes.last();

			if (this._lastOverElement == el) {

				return false;

			} else {
				// check containment rules
				var option = e.getTarget().getUserData("option");

				// revert to original options if these were modified
				if (option.backupOptions) {
					for (var key in option.backupOptions) {
						option[key] = option.backupOptions[key];
					}
					delete option.backupOptions;
				}

				var stencilSet = this.facade.getStencilSets()[option.namespace];

				var stencil = stencilSet.stencil(option.type);

				var candidate = aShapes.last();

				if (stencil.type() === "node") {
					//check containment rules
					var canContain = this.facade.getRules().canContain({
						containingShape: candidate,
						containedStencil: stencil
					});

					// if not canContain, try to find a morph which can be contained
					if (!canContain) {
						var possibleMorphs = this.facade.getRules().morphStencils({
							stencil: stencil
						});
						for (var i = 0; i < possibleMorphs.size(); i++) {
							canContain = this.facade.getRules().canContain({
								containingShape: candidate,
								containedStencil: possibleMorphs[i]
							});
							if (canContain) {
								option.backupOptions = Object.clone(option);
								option.type = possibleMorphs[i].id();
								option.namespace = possibleMorphs[i].namespace();
								break;
							}
						}
					}

					this._currentReference = canContain ? candidate : undefined;


				} else { //Edge
					var curCan = candidate,
						orgCan = candidate;
					var canConnect = false;
					while (!canConnect && curCan && !(curCan instanceof ms123.oryx.core.Canvas)) {
						candidate = curCan;
						//check connection rules
						canConnect = this.facade.getRules().canConnect({
							sourceShape: this.currentShapes.first(),
							edgeStencil: stencil,
							targetShape: curCan
						});
						curCan = curCan.parent;
					}

					// if not canConnect, try to find a morph which can be connected
					if (!canConnect) {

						candidate = orgCan;
						var possibleMorphs = this.facade.getRules().morphStencils({
							stencil: stencil
						});
						for (var i = 0; i < possibleMorphs.size(); i++) {
							var curCan = candidate;
							var canConnect = false;
							while (!canConnect && curCan && !(curCan instanceof ms123.oryx.core.Canvas)) {
								candidate = curCan;
								//check connection rules
								canConnect = this.facade.getRules().canConnect({
									sourceShape: this.currentShapes.first(),
									edgeStencil: possibleMorphs[i],
									targetShape: curCan
								});
								curCan = curCan.parent;
							}
							if (canConnect) {
								option.backupOptions = Object.clone(option);
								option.type = possibleMorphs[i].id();
								option.namespace = possibleMorphs[i].namespace();
								break;
							} else {
								candidate = orgCan;
							}
						}
					}

					this._currentReference = canConnect ? candidate : undefined;

				}

				this.facade.raiseEvent({
					type: ms123.oryx.Config.EVENT_HIGHLIGHT_SHOW,
					highlightId: 'shapeMenu',
					elements: [candidate],
					color: this._currentReference ? ms123.oryx.Config.SELECTION_VALID_COLOR : ms123.oryx.Config.SELECTION_INVALID_COLOR
				});

				if (this._currentReference) {
					cursor.setAction("move");
				} else {
					cursor.setAction(null);
				}

			}

			this._lastOverElement = el;

			return false;
		},

		__drop: function (e) {

			if (!(this.currentShapes instanceof Array) || this.currentShapes.length <= 0) {
				return;
			}
			var sourceShape = this.currentShapes;

			this._lastOverElement = undefined;

			// Hide the highlighting
			this.facade.raiseEvent({
				type: ms123.oryx.Config.EVENT_HIGHLIGHT_HIDE,
				highlightId: 'shapeMenu'
			});

			// Check if there is a current Parent
			if (!this._currentReference) {
				return this.facade.updateSelection();
			}

			var option = e.getRelatedTarget().getUserData("option");
			option['parent'] = this._currentReference;

			var pos = {
				x: e.getDocumentLeft(),
				y: e.getDocumentTop()
			};

			var a = this.facade.getCanvas().node.getScreenCTM();
			// Correcting the UpperLeft-Offset
			pos.x -= a.e;
			pos.y -= a.f;
			// Correcting the Zoom-Faktor
			pos.x /= a.a;
			pos.y /= a.d;
			// Correcting the ScrollOffset
			pos.x -= document.documentElement.scrollLeft;
			pos.y -= document.documentElement.scrollTop;

			var parentAbs = this._currentReference.absoluteXY();
			pos.x -= parentAbs.x;
			pos.y -= parentAbs.y;

			// If the ctrl key is not pressed, 
			// snapp the new shape to the center 
			// if it is near to the center of the other shape
/*			if (!e.ctrlPressed()) {
				// Get the center of the shape
				var cShape = this.currentShapes[0].bounds.center();
				// Snapp +-20 Pixel horizontal to the center 
				if (20 > Math.abs(cShape.x - pos.x)) {
					pos.x = cShape.x;
				}
				// Snapp +-20 Pixel vertical to the center 
				if (20 > Math.abs(cShape.y - pos.y)) {
					pos.y = cShape.y;
				}
			}*/

			option['position'] = pos;
			option['connectedShape'] = this.currentShapes[0];
			if (option['connectingType']) {
				var stencilset = this.facade.getStencilSets()[option.namespace];
				var containedStencil = stencilset.stencil(option.type);
				var args = {
					sourceShape: this.currentShapes[0],
					targetStencil: containedStencil
				};
				option['connectingType'] = this.facade.getRules().connectMorph(args).id();
			}

			if (ms123.oryx.Config.SHAPEMENU_DISABLE_CONNECTED_EDGE === true) {
				delete option['connectingType'];
			}

			var command = new ms123.graphicaleditor.plugins.shapemenu.CreateCommand(Object.clone(option), this._currentReference, pos, this);

			this.facade.executeCommands([command]);

			// Inform about completed Drag 
			this.facade.raiseEvent({
				type: ms123.oryx.Config.EVENT_SHAPE_MENU_CLOSE,
				source: sourceShape,
				destination: this.currentShapes
			});

			// revert to original options if these were modified
			if (option.backupOptions) {
				for (var key in option.backupOptions) {
					option[key] = option.backupOptions[key];
				}
				delete option.backupOptions;
			}

			this._currentReference = undefined;
		},

		newShape: function (option, event) {
			var stencilset = this.facade.getStencilSets()[option.namespace];
			var containedStencil = stencilset.stencil(option.type);

			if (this.facade.getRules().canContain({
				containingShape: this.currentShapes.first().parent,
				"containedStencil": containedStencil
			})) {

				option['connectedShape'] = this.currentShapes[0];
				option['parent'] = this.currentShapes.first().parent;
				option['containedStencil'] = containedStencil;

				var args = {
					sourceShape: this.currentShapes[0],
					targetStencil: containedStencil
				};
				var targetStencil = this.facade.getRules().connectMorph(args);
				if (!targetStencil) {
					return
				} // Check if there can be a target shape
				option['connectingType'] = targetStencil.id();

				if (ms123.oryx.Config.SHAPEMENU_DISABLE_CONNECTED_EDGE === true) {
					delete option['connectingType'];
				}

				var command = new ms123.graphicaleditor.plugins.shapemenu.CreateCommand(option, undefined, undefined, this);

				this.facade.executeCommands([command]);
			}
		},

		/**
		 * Morph a shape to a new stencil
		 * {Command implemented}
		 * param {Shape} shape
		 * param {Stencil} stencil
		 */
		morphShape: function (shape, stencil) {

			var MorphTo = Clazz.extend({
				construct: function (shape, stencil, facade) {
					this.shape = shape;
					this.stencil = stencil;
					this.facade = facade;
				},
				execute: function () {

					var shape = this.shape;
					var stencil = this.stencil;
					var resourceId = shape.resourceId;

					// Serialize all attributes
					var serialized = shape.serialize();
					stencil.properties().each((function (prop) {
						if (prop.readonly()) {
							serialized = serialized.reject(function (serProp) {
								return serProp.name == prop.id();
							});
						}
					}).bind(this));

					// Get shape if already created, otherwise create a new shape
					var newShape;
					if (this.newShape) {
						newShape = this.newShape;
						this.facade.getCanvas().add(newShape);
					} else {
						newShape = this.facade.createShape({
							type: stencil.id(),
							namespace: stencil.namespace(),
							resourceId: resourceId
						});
					}

					// calculate new bounds using old shape's upperLeft and new shape's width/height
					var boundsObj = serialized.find(function (serProp) {
						return (serProp.prefix === "oryx" && serProp.name === "bounds");
					});

					var changedBounds = null;

					if (!this.facade.getRules().preserveBounds(shape.getStencil())) {

						var bounds = boundsObj.value.split(",");
						if (parseInt(bounds[0], 10) > parseInt(bounds[2], 10)) { // if lowerRight comes first, swap array items
							var tmp = bounds[0];
							bounds[0] = bounds[2];
							bounds[2] = tmp;
							tmp = bounds[1];
							bounds[1] = bounds[3];
							bounds[3] = tmp;
						}
						bounds[2] = parseInt(bounds[0], 10) + newShape.bounds.width();
						bounds[3] = parseInt(bounds[1], 10) + newShape.bounds.height();
						boundsObj.value = bounds.join(",");

					} else {

						var height = shape.bounds.height();
						var width = shape.bounds.width();

						// consider the minimum and maximum size of
						// the new shape
						if (newShape.minimumSize) {
							if (shape.bounds.height() < newShape.minimumSize.height) {
								height = newShape.minimumSize.height;
							}


							if (shape.bounds.width() < newShape.minimumSize.width) {
								width = newShape.minimumSize.width;
							}
						}

						if (newShape.maximumSize) {
							if (shape.bounds.height() > newShape.maximumSize.height) {
								height = newShape.maximumSize.height;
							}

							if (shape.bounds.width() > newShape.maximumSize.width) {
								width = newShape.maximumSize.width;
							}
						}

						changedBounds = {
							a: {
								x: shape.bounds.a.x,
								y: shape.bounds.a.y
							},
							b: {
								x: shape.bounds.a.x + width,
								y: shape.bounds.a.y + height
							}
						};

					}

					var oPos = shape.bounds.center();
					if (changedBounds !== null) {
						newShape.bounds.set(changedBounds);
					}

					// Set all related dockers
					this.setRelatedDockers(shape, newShape);

					// store DOM position of old shape
					var parentNode = shape.node.parentNode;
					var nextSibling = shape.node.nextSibling;

					// Delete the old shape
					this.facade.deleteShape(shape);

					// Deserialize the new shape - Set all attributes
//					newShape.deserialize(serialized);
/*
				 * Change color to default if unchanged
				 * 23.04.2010
				 */
					if (shape.getStencil().property("oryx-bgcolor") && shape.properties["oryx-bgcolor"] && shape.getStencil().property("oryx-bgcolor").value().toUpperCase() == shape.properties["oryx-bgcolor"].toUpperCase()) {
						if (newShape.getStencil().property("oryx-bgcolor")) {
							newShape.setProperty("oryx-bgcolor", newShape.getStencil().property("oryx-bgcolor").value());
						}
					}
					if (changedBounds !== null) {
						newShape.bounds.set(changedBounds);
					}

					if (newShape.getStencil().type() === "edge" || (newShape.dockers.length == 0 || !newShape.dockers[0].getDockedShape())) {
						newShape.bounds.centerMoveTo(oPos);
					}

					if (newShape.getStencil().type() === "node" && (newShape.dockers.length == 0 || !newShape.dockers[0].getDockedShape())) {
						this.setRelatedDockers(newShape, newShape);

					}

					// place at the DOM position of the old shape
					if (nextSibling) parentNode.insertBefore(newShape.node, nextSibling);
					else parentNode.appendChild(newShape.node);

					// Set selection
					this.facade.setSelection([newShape]);
					this.facade.getCanvas().update();
					this.facade.updateSelection();
					this.newShape = newShape;

				},
				rollback: function () {

					if (!this.shape || !this.newShape || !this.newShape.parent) {
						return
					}

					// Append shape to the parent
					this.newShape.parent.add(this.shape);
					// Set dockers
					this.setRelatedDockers(this.newShape, this.shape);
					// Delete new shape
					this.facade.deleteShape(this.newShape);
					// Set selection
					this.facade.setSelection([this.shape]);
					// Update
					this.facade.getCanvas().update();
					this.facade.updateSelection();
				},

				/**
				 * Set all incoming and outgoing edges from the shape to the new shape
				 * param {Shape} shape
				 * param {Shape} newShape
				 */
				setRelatedDockers: function (shape, newShape) {

					if (shape.getStencil().type() === "node") {

						(shape.incoming || []).concat(shape.outgoing || []).each(function (i) {
							i.dockers.each(function (docker) {
								if (docker.getDockedShape() == shape) {
									var rPoint = Object.clone(docker.referencePoint);
									// Move reference point per percent
									var rPointNew = {
										x: rPoint.x * newShape.bounds.width() / shape.bounds.width(),
										y: rPoint.y * newShape.bounds.height() / shape.bounds.height()
									};

									docker.setDockedShape(newShape);
									// Set reference point and center to new position
									docker.setReferencePoint(rPointNew);
									if (i instanceof ms123.oryx.core.Edge) {
										docker.bounds.centerMoveTo(rPointNew);
									} else {
										var absXY = shape.absoluteXY();
										docker.bounds.centerMoveTo({
											x: rPointNew.x + absXY.x,
											y: rPointNew.y + absXY.y
										});
										//docker.bounds.moveBy({x:rPointNew.x-rPoint.x, y:rPointNew.y-rPoint.y});
									}
								}
							});
						});

						// for attached events
						if (shape.dockers.length > 0 && shape.dockers.first().getDockedShape()) {
							newShape.dockers.first().setDockedShape(shape.dockers.first().getDockedShape());
							newShape.dockers.first().setReferencePoint(Object.clone(shape.dockers.first().referencePoint));
						}

					} else { // is edge
						newShape.dockers.first().setDockedShape(shape.dockers.first().getDockedShape());
						newShape.dockers.first().setReferencePoint(shape.dockers.first().referencePoint);
						newShape.dockers.last().setDockedShape(shape.dockers.last().getDockedShape());
						newShape.dockers.last().setReferencePoint(shape.dockers.last().referencePoint);
					}
				}
			});

			// Create and execute command (for undo/redo)			
			var command = new MorphTo(shape, stencil, this.facade);
			this.facade.executeCommands([command]);
		},
		__getStencilIconUrl: function (name) {
			if( name.match(/^data:image/)) return name;
console.error("name:"+name);
			var am = qx.util.AliasManager.getInstance(name);
			return am.resolve("resource/ms123/stencilsets/"+this.editorType+"/" + name);
		},
		__getResourceUrl: function (name) {
			if( name.match(/^data:image/)) return name;
			var am = qx.util.AliasManager.getInstance();
			return am.resolve("resource/ms123/" + name);
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
