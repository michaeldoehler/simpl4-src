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
	* @ignore(Clazz)
	* @ignore(Clazz.extend)
	* @ignore($A)
*/

qx.Class.define("ms123.graphicaleditor.plugins.Arrangement", {
	extend: ms123.oryx.core.AbstractPlugin,
	include: [qx.locale.MTranslation],
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (facade, editorType) {
		this.base(arguments, facade);
		this.facade = facade;
		this.editorType = editorType;

		var config = {};
		if( editorType == "sw.document"){
			config.arrangment_am = ms123.oryx.Config.EDITOR_ALIGN_TOP;
			config.arrangment_ac = ms123.oryx.Config.EDITOR_ALIGN_LEFT;
		}else{
			config.arrangment_am = ms123.oryx.Config.EDITOR_ALIGN_MIDDLE;
			config.arrangment_ac = ms123.oryx.Config.EDITOR_ALIGN_CENTER;
		}

		this.facade.offer({
			'name': ms123.oryx.Translation.Arrangement.am,
			'functionality': this.alignShapes.bind(this, [config.arrangment_am]),
			'group': ms123.oryx.Translation.Arrangement.groupA,
			'icon': this.__getResourceUrl("shape_align_middle.png"),
			'description': ms123.oryx.Translation.Arrangement.amDesc,
			'index': 1,
			'minShape': 2
		});

		this.facade.offer({
			'name': ms123.oryx.Translation.Arrangement.ac,
			'functionality': this.alignShapes.bind(this, [config.arrangment_ac]),
			'group': ms123.oryx.Translation.Arrangement.groupA,
			'icon': this.__getResourceUrl("shape_align_center.png"),
			'description': ms123.oryx.Translation.Arrangement.acDesc,
			'index': 2,
			'minShape': 2
		});


		this.facade.offer({
			'name': ms123.oryx.Translation.Arrangement.as,
			'functionality': this.alignShapes.bind(this, [ms123.oryx.Config.EDITOR_ALIGN_SIZE]),
			'group': ms123.oryx.Translation.Arrangement.groupA,
			'icon': this.__getResourceUrl("shape_align_size.png"),
			'description': ms123.oryx.Translation.Arrangement.asDesc,
			'index': 3,
			'minShape': 2
		});


		this.facade.registerOnEvent(ms123.oryx.Config.EVENT_ARRANGEMENT_TOP, this.setZLevel.bind(this, this.setToTop));
		this.facade.registerOnEvent(ms123.oryx.Config.EVENT_ARRANGEMENT_BACK, this.setZLevel.bind(this, this.setToBack));
		this.facade.registerOnEvent(ms123.oryx.Config.EVENT_ARRANGEMENT_FORWARD, this.setZLevel.bind(this, this.setForward));
		this.facade.registerOnEvent(ms123.oryx.Config.EVENT_ARRANGEMENT_BACKWARD, this.setZLevel.bind(this, this.setBackward));
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
		onSelectionChanged: function (elemnt) {
			var selection = this.facade.getSelection();
			if (selection.length === 1 && selection[0] instanceof ms123.oryx.core.Edge) {
				this.setToTop(selection);
			}
		},

		setZLevel: function (callback, event) {

			//Command-Pattern for dragging one docker
			var zLevelCommand = ms123.oryx.core.Command.extend({
				construct: function (callback, elements, facade) {
					this.callback = callback;
					this.elements = elements;
					// For redo, the previous elements get stored
					this.elAndIndex = elements.map(function (el) {
						return {
							el: el,
							previous: el.parent.children[el.parent.children.indexOf(el) - 1]
						}
					})
					this.facade = facade;
				},
				execute: function () {

					// Call the defined z-order callback with the elements
					this.callback(this.elements)
					this.facade.setSelection(this.elements)
				},
				rollback: function () {

					// Sort all elements on the index of there containment
					var sortedEl = this.elAndIndex.sortBy(function (el) {
						var value = el.el;
						var t = $A(value.node.parentNode.childNodes);
						return t.indexOf(value.node);
					});

					// Every element get setted back bevor the old previous element
					for (var i = 0; i < sortedEl.length; i++) {
						var el = sortedEl[i].el;
						var p = el.parent;
						var oldIndex = p.children.indexOf(el);
						var newIndex = p.children.indexOf(sortedEl[i].previous);
						newIndex = newIndex || 0
						p.children = this.insertFrom(p.children,oldIndex, newIndex)
						el.node.parentNode.insertBefore(el.node, el.node.parentNode.childNodes[newIndex + 1]);
					}

					// Reset the selection
					this.facade.setSelection(this.elements)
				}
			});

			// Instanziate the dockCommand
			var command = new zLevelCommand(callback, this.facade.getSelection(), this.facade);
			if (event.excludeCommand) {
				command.execute();
			} else {
				this.facade.executeCommands([command]);
			}

		},

		setToTop: function (elements) {

			// Sortieren des Arrays nach dem Index des SVGKnotens im Bezug auf dem Elternknoten.
			var tmpElem = elements.sortBy(function (value, index) {
				var t = $A(value.node.parentNode.childNodes);
				return t.indexOf(value.node);
			});
			// Sortiertes Array wird nach oben verschoben.
			tmpElem.each(function (value) {
				var p = value.parent;
				if (p.children.last() === value) {
					return;
				}
				p.children = p.children.without(value)
				p.children.push(value);
				value.node.parentNode.appendChild(value.node);
			});
		},

		setToBack: function (elements) {
			// Sortieren des Arrays nach dem Index des SVGKnotens im Bezug auf dem Elternknoten.
			var tmpElem = elements.sortBy(function (value, index) {
				var t = $A(value.node.parentNode.childNodes);
				return t.indexOf(value.node);
			});

			tmpElem = tmpElem.reverse();

			// Sortiertes Array wird nach unten verschoben.
			tmpElem.each(function (value) {
				var p = value.parent
				p.children = p.children.without(value)
				p.children.unshift(value);
				value.node.parentNode.insertBefore(value.node, value.node.parentNode.firstChild);
			});


		},

		setBackward: function (elements) {
			// Sortieren des Arrays nach dem Index des SVGKnotens im Bezug auf dem Elternknoten.
			var tmpElem = elements.sortBy(function (value, index) {
				var t = $A(value.node.parentNode.childNodes);
				return t.indexOf(value.node);
			});

			// Reverse the elements
			tmpElem = tmpElem.reverse();

			// Delete all Nodes who are the next Node in the nodes-Array
			var compactElem = tmpElem.findAll(function (el) {
				return !tmpElem.some(function (checkedEl) {
					return checkedEl.node == el.node.previousSibling
				})
			});

			// Sortiertes Array wird nach eine Ebene nach oben verschoben.
			compactElem.each(function (el) {
				if (el.node.previousSibling === null) {
					return;
				}
				var p = el.parent;
				var index = p.children.indexOf(el);
				p.children = this.insertFrom(p.children,index, index - 1)
				el.node.parentNode.insertBefore(el.node, el.node.previousSibling);
			});


		},

		setForward: function (elements) {
			// Sortieren des Arrays nach dem Index des SVGKnotens im Bezug auf dem Elternknoten.
			var tmpElem = elements.sortBy(function (value, index) {
				var t = $A(value.node.parentNode.childNodes);
				return t.indexOf(value.node);
			});


			// Delete all Nodes who are the next Node in the nodes-Array
			var compactElem = tmpElem.findAll(function (el) {
				return !tmpElem.some(function (checkedEl) {
					return checkedEl.node == el.node.nextSibling
				})
			});


			// Sortiertes Array wird eine Ebene nach unten verschoben.
			compactElem.each(function (el) {
				var nextNode = el.node.nextSibling
				if (nextNode === null) {
					return;
				}
				var index = el.parent.children.indexOf(el);
				var p = el.parent;
				p.children = this.insertFrom(p.children,index, index + 1)
				el.node.parentNode.insertBefore(nextNode, el.node);
			});
		},


		alignShapes: function (way) {

			var elements = this.facade.getSelection();

			// Set the elements to all Top-Level elements
			elements = this.facade.getCanvas().getShapesWithSharedParent(elements);
			// Get only nodes
			elements = elements.findAll(function (value) {
				return (value instanceof ms123.oryx.core.Node)
			});
			// Delete all attached intermediate events from the array
			elements = elements.findAll(function (value) {
				var d = value.getIncomingShapes()
				return d.length == 0 || !elements.include(d[0])
			});
			if (elements.length < 2) {
				return;
			}

			// get bounds of all shapes.
			var bounds = elements[0].absoluteBounds().clone();
			elements.each(function (shape) {
				bounds.include(shape.absoluteBounds().clone());
			});

			// get biggest width and heigth
			if( this.editorType == "sw.document"){
				var maxWidth = ms123.oryx.Config.MAXIMUM_SIZE;
				var maxHeight = ms123.oryx.Config.MAXIMUM_SIZE;
				elements.each(function (shape) {
					maxWidth = Math.min(shape.bounds.width(), maxWidth);
					maxHeight = Math.min(shape.bounds.height(), maxHeight);
				});
			}else{
				var maxWidth = 0;
				var maxHeight = 0;
				elements.each(function (shape) {
					maxWidth = Math.max(shape.bounds.width(), maxWidth);
					maxHeight = Math.max(shape.bounds.height(), maxHeight);
				});
			}

			var commandClass = Clazz.extend({
				construct: function (elements, bounds, maxHeight, maxWidth, way, plugin) {
					this.elements = elements;
					this.bounds = bounds;
					this.maxHeight = maxHeight;
					this.maxWidth = maxWidth;
					this.way = way;
					this.facade = plugin.facade;
					this.plugin = plugin;
					this.orgPos = [];
				},
				setBounds: function (shape, maxSize) {
					if (!maxSize) maxSize = {
						width: ms123.oryx.Config.MAXIMUM_SIZE,
						height: ms123.oryx.Config.MAXIMUM_SIZE
					};

					if (!shape.bounds) {
						throw "Bounds not definined."
					}

					var newBounds = {
						a: {
							x: shape.bounds.upperLeft().x - (this.maxWidth - shape.bounds.width()) / 2,
							y: shape.bounds.upperLeft().y - (this.maxHeight - shape.bounds.height()) / 2
						},
						b: {
							x: shape.bounds.lowerRight().x + (this.maxWidth - shape.bounds.width()) / 2,
							y: shape.bounds.lowerRight().y + (this.maxHeight - shape.bounds.height()) / 2
						}
					}

					/* If the new width of shape exceeds the maximum width, set width value to maximum. */
					if (this.maxWidth > maxSize.width) {
						newBounds.a.x = shape.bounds.upperLeft().x - (maxSize.width - shape.bounds.width()) / 2;

						newBounds.b.x = shape.bounds.lowerRight().x + (maxSize.width - shape.bounds.width()) / 2
					}

					/* If the new height of shape exceeds the maximum height, set height value to maximum. */
					if (this.maxHeight > maxSize.height) {
						newBounds.a.y = shape.bounds.upperLeft().y - (maxSize.height - shape.bounds.height()) / 2;

						newBounds.b.y = shape.bounds.lowerRight().y + (maxSize.height - shape.bounds.height()) / 2
					}

					/* set bounds of shape */
					shape.bounds.set(newBounds);

				},
				execute: function () {
					// align each shape according to the way that was specified.
					this.elements.each((function (shape, index) {
						this.orgPos[index] = shape.bounds.upperLeft();

						var relBounds = this.bounds.clone();
						var newCoordinates;
						if (shape.parent && !(shape.parent instanceof ms123.oryx.core.Canvas)) {
							var upL = shape.parent.absoluteBounds().upperLeft();
							relBounds.moveBy(-upL.x, -upL.y);
						}

						switch (this.way) {
							// align the shapes in the requested way.
						case ms123.oryx.Config.EDITOR_ALIGN_BOTTOM:
							newCoordinates = {
								x: shape.bounds.upperLeft().x,
								y: relBounds.b.y - shape.bounds.height()
							};
							break;

						case ms123.oryx.Config.EDITOR_ALIGN_MIDDLE:
							newCoordinates = {
								x: shape.bounds.upperLeft().x,
								y: (relBounds.a.y + relBounds.b.y - shape.bounds.height()) / 2
							};
							break;

						case ms123.oryx.Config.EDITOR_ALIGN_TOP:
							newCoordinates = {
								x: shape.bounds.upperLeft().x,
								y: relBounds.a.y
							};
							break;

						case ms123.oryx.Config.EDITOR_ALIGN_LEFT:
							newCoordinates = {
								x: relBounds.a.x,
								y: shape.bounds.upperLeft().y
							};
							break;

						case ms123.oryx.Config.EDITOR_ALIGN_CENTER:
							newCoordinates = {
								x: (relBounds.a.x + relBounds.b.x - shape.bounds.width()) / 2,
								y: shape.bounds.upperLeft().y
							};
							break;

						case ms123.oryx.Config.EDITOR_ALIGN_RIGHT:
							newCoordinates = {
								x: relBounds.b.x - shape.bounds.width(),
								y: shape.bounds.upperLeft().y
							};
							break;

						case ms123.oryx.Config.EDITOR_ALIGN_SIZE:
							if (shape.isResizable) {
								this.orgPos[index] = {
									a: shape.bounds.upperLeft(),
									b: shape.bounds.lowerRight()
								};
								this.setBounds(shape, shape.maximumSize);
							}
							break;
						}

						if (newCoordinates) {
							var offset = {
								x: shape.bounds.upperLeft().x - newCoordinates.x,
								y: shape.bounds.upperLeft().y - newCoordinates.y
							}
							// Set the new position
							shape.bounds.moveTo(newCoordinates);
							this.plugin.layoutEdges(shape, shape.getAllDockedShapes(), offset);
							//shape.update()
						}
					}).bind(this));

					this.facade.getCanvas().update();
					this.facade.updateSelection();
				},
				rollback: function () {
					this.elements.each((function (shape, index) {
						if (this.way == ms123.oryx.Config.EDITOR_ALIGN_SIZE) {
							if (shape.isResizable) {
								shape.bounds.set(this.orgPos[index]);
							}
						} else {
							shape.bounds.moveTo(this.orgPos[index]);
						}
					}).bind(this));

					this.facade.getCanvas().update();
					this.facade.updateSelection();
				}
			})

			var command = new commandClass(elements, bounds, maxHeight, maxWidth, parseInt(way), this);

			this.facade.executeCommands([command]);
		},
		insertFrom: function (array, from, to) {
			to = Math.max(0, to);
			from = Math.min(Math.max(0, from), array.length - 1);

			var el = array[from];
			var old = array.without(el);
			var newA = old.slice(0, to);
			newA.push(el);
			if (old.length > to) {
				newA = newA.concat(old.slice(to))
			};
			return newA;
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
