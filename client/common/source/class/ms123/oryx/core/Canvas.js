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
	* @ignore($A)
*/
qx.Class.define("ms123.oryx.core.Canvas", {
	extend: ms123.oryx.core.AbstractShape,
  include : [ qx.locale.MTranslation],
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (options,stencil) {
		this.base(arguments,options,stencil);

		if (!(options && options.width && options.height)) {

			console.fatal("Canvas is missing mandatory parameters options.width and options.height.");
			return;
		}

		this.resourceId = options.id;
		this.nodes = [];
		this.edges = [];

		//init svg document
		this.rootNode = ms123.oryx.Editor.graft("http://www.w3.org/2000/svg", options.parentNode, ['svg',
		{
			id: this.id,
			width: options.width,
			height: options.height
		}, ['defs',
		{}]]);

		this.rootNode.setAttribute("xmlns:xlink", "http://www.w3.org/1999/xlink");
		this.rootNode.setAttribute("xmlns:svg", "http://www.w3.org/2000/svg");

		this._htmlContainer = ms123.oryx.Editor.graft("http://www.w3.org/1999/xhtml", options.parentNode, ['div',
		{
			id: "oryx_canvas_htmlContainer",
			style: "position:absolute; top:5px"
		}]);

		this.node = ms123.oryx.Editor.graft("http://www.w3.org/2000/svg", this.rootNode, ['g',
		{}, ['g',
		{
			"class": "stencils"
		}, ['g',
		{
			"class": "me"
		}],
			['g',
			{
				"class": "children"
			}],
			['g',
			{
				"class": "edge"
			}]
		],
			['g',
			{
				"class": "svgcontainer"
			}]
		]);


		this.node.setAttributeNS(null, 'stroke', 'black');
		this.node.setAttributeNS(null, 'font-family', 'Verdana, sans-serif');
		this.node.setAttributeNS(null, 'font-size-adjust', 'none');
		this.node.setAttributeNS(null, 'font-style', 'normal');
		this.node.setAttributeNS(null, 'font-variant', 'normal');
		this.node.setAttributeNS(null, 'font-weight', 'normal');
		this.node.setAttributeNS(null, 'line-heigth', 'normal');

		this.node.setAttributeNS(null, 'font-size', ms123.oryx.Config.LABEL_DEFAULT_LINE_HEIGHT);

		this.bounds.set(0, 0, options.width, options.height);

		this.addEventHandlers(this.rootNode.parentNode);

		//disable context menu
		this.rootNode.oncontextmenu = function () {
			return false;
		};
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
		 * Defines the current zoom level
		 */
		zoomLevel: 1,
		focus: function () {

			/*try {
				// Get a href
				if (!this.focusEl) {
					this.focusEl = Ext.getBody().createChild({
						tag: "a",
						href: "#",
						cls: "x-grid3-focus x-grid3-focus-canvas",
						tabIndex: "-1"
					});
					this.focusEl.swallowEvent("click", true);
				}

				// Focus it
				if (qx.bom.client.Engine.__isGecko()) {
					this.focusEl.focus();
				}
				else {
					this.focusEl.focus.defer(1, this.focusEl);
				}
				this.focusEl.blur.defer(3, this.focusEl);

			} catch (e) {}*/
		},

		update: function () {

			this.nodes.each(this.bindTo(function (node) {
				this._traverseForUpdate(node);
			}, this));

			var layoutEvents = this.getStencil().layout();

			if (layoutEvents) {
				layoutEvents.each(this.bindTo(function (event) {

					// setup additional attributes
					event.shape = this;
					event.forceExecution = true;
					event.target = this.rootNode;

					// do layouting
					this._delegateEvent(event);
				}, this))
			}

			this.nodes.invoke("_update");

			this.edges.invoke("_update", true);

		},

		_traverseForUpdate: function (shape) {
			var childRet = shape.isChanged;
			shape.getChildNodes(false, this.bindTo(function (child) {
				if (this._traverseForUpdate(child)) {
					childRet = true;
				}
			}, this));

			if (childRet) {
				shape.layout();
				return true;
			} else {
				return false;
			}
		},

		layout: function () {



		},

		/**
		 * 
		 * param {Object} deep
		 * param {Object} iterator
		 */
		getChildNodes: function (deep, iterator) {
			if (!deep && !iterator) {
				return this.nodes.clone();
			} else {
				var result = [];
				this.nodes.each(function (uiObject) {
					if (iterator) {
						iterator(uiObject);
					}
					result.push(uiObject);

					if (deep && uiObject instanceof ms123.oryx.core.Shape) {
						result = result.concat(uiObject.getChildNodes(deep, iterator));
					}
				});

				return result;
			}
		},

		/**
		 * Overrides the UIObject.add method. Adds uiObject to the correct sub node.
		 * param {UIObject} uiObject
		 */
		add: function (uiObject, index, silent) {
			//if uiObject is child of another UIObject, remove it.
			if (uiObject instanceof ms123.oryx.core.UIObject) {
				if (!(this.children.member(uiObject))) {
					//if uiObject is child of another parent, remove it from that parent.
					if (uiObject.parent) {
						uiObject.parent.remove(uiObject, true);
					}

					//add uiObject to the Canvas
					//add uiObject to this Shape
					if (index != undefined) this.children.splice(index, 0, uiObject);
					else this.children.push(uiObject);

					//set parent reference
					uiObject.parent = this;

					//add uiObject.node to this.node depending on the type of uiObject
					if (uiObject instanceof ms123.oryx.core.Shape) {
						if (uiObject instanceof ms123.oryx.core.Edge) {
							uiObject.addMarkers(this.rootNode.getElementsByTagNameNS(ms123.oryx.Config.NAMESPACE_SVG, "defs")[0]);
							uiObject.node = this.node.childNodes[0].childNodes[2].appendChild(uiObject.node);
							this.edges.push(uiObject);
						} else {
							uiObject.node = this.node.childNodes[0].childNodes[1].appendChild(uiObject.node);
							this.nodes.push(uiObject);
						}
					} else { //UIObject
						uiObject.node = this.node.appendChild(uiObject.node);
					}

					uiObject.bounds.registerCallback(this._changedCallback);

					if (this.eventHandlerCallback && silent !== true) this.eventHandlerCallback({
						type: ms123.oryx.Config.EVENT_SHAPEADDED,
						shape: uiObject
					})
				} else {

					console.warn("add: ms123.oryx.core.UIObject is already a child of this object.");
				}
			} else {

				console.fatal("add: Parameter is not of type ms123.oryx.core.UIObject.");
			}
		},

		/**
		 * Overrides the UIObject.remove method. Removes uiObject.
		 * param {UIObject} uiObject
		 */
		remove: function (uiObject, silent) {
			//if uiObject is a child of this object, remove it.
			if (this.children.member(uiObject)) {
				//remove uiObject from children
				var parent = uiObject.parent;

				this.children = this.children.without(uiObject);

				//delete parent reference of uiObject
				uiObject.parent = undefined;

				//delete uiObject.node from this.node
				if (uiObject instanceof ms123.oryx.core.Shape) {
					if (uiObject instanceof ms123.oryx.core.Edge) {
						uiObject.removeMarkers();
						uiObject.node = this.node.childNodes[0].childNodes[2].removeChild(uiObject.node);
						this.edges = this.edges.without(uiObject);
					} else {
						uiObject.node = this.node.childNodes[0].childNodes[1].removeChild(uiObject.node);
						this.nodes = this.nodes.without(uiObject);
					}
				} else { //UIObject
					uiObject.node = this.node.removeChild(uiObject.node);
				}

				if (this.eventHandlerCallback && silent !== true) this.eventHandlerCallback({
					type: ms123.oryx.Config.EVENT_SHAPEREMOVED,
					shape: uiObject,
					parent: parent
				});

				uiObject.bounds.unregisterCallback(this._changedCallback);
			} else {

				console.warn("remove: ms123.oryx.core.UIObject is not a child of this object.");
			}
		},

		/**
		 * Creates shapes out of the given collection of shape objects and adds them to the canvas.
		 * @example 
		 * canvas.addShapeObjects({
		 bounds:{ lowerRight:{ y:510, x:633 }, upperLeft:{ y:146, x:210 } },
		 resourceId:"oryx_F0715955-50F2-403D-9851-C08CFE70F8BD",
		 childShapes:[],
		 properties:{},
		 stencil:{
		 id:"Subprocess"
		 },
		 outgoing:[{resourceId: 'aShape'}],
		 target: {resourceId: 'aShape'}
		 });
		 * param {Object} shapeObjects 
		 * param {Function} [eventHandler] An event handler passed to each newly created shape (as eventHandlerCallback)
		 * @return {Array} A collection of ms123.oryx.core.Shape
		 * @methodOf ms123.oryx.core.Canvas.prototype
		 */
		addShapeObjects: function (shapeObjects, eventHandler) {
			if (!shapeObjects) return;

			this.initializingShapes = true;

/*FIXME This implementation is very evil! At first, all shapes are created on
          canvas. In a second step, the attributes are applied. There must be a distinction
          between the configuration phase (where the outgoings, for example, are just named),
          and the creation phase (where the outgoings are evaluated). This must be reflected
          in code to provide a nicer API/ implementation!!! */

			var addShape = this.bindTo(function (shape, parent) {
				// Create a new Stencil
				var stencil = ms123.oryx.core.StencilSet.stencil(this.getStencil().namespace() + shape.stencil.id);
				if( !stencil ){
					ms123.form.Dialog.alert(this.tr("oryx.core.canvas.stencil_not_found")+":"+shape.stencil.id);
					return null;
				}

				// Create a new Shape
				var ShapeClass = (stencil.type() == "node") ? ms123.oryx.core.Node : ms123.oryx.core.Edge;
				var newShape = new ShapeClass({
					'eventHandlerCallback': eventHandler
				}, stencil);

				// Set the resource id
				newShape.resourceId = shape.resourceId;
				newShape.node.id = "svg-" + shape.resourceId;

				// Set parent to json object to be used later
				// Due to the nested json structure, normally shape.parent is not set/ must not be set. 
				// In special cases, it can be easier to set this directly instead of a nested structure.
				shape.parent = "#" + ((shape.parent && shape.parent.resourceId) || parent.resourceId);

				// Add the shape to the canvas
				this.add(newShape);

				return {
					json: shape,
					object: newShape
				};
			}, this);

			/** Builds up recursively a flatted array of shapes, including a javascript object and json representation
			 * param {Object} shape Any object that has Object#childShapes
			 */
			var addChildShapesRecursively = this.bindTo(function (shape) {
				var addedShapes = [];
				shape.childShapes.each(function (childShape) {
					var addedShape = addShape(childShape, shape);
					if( addedShape != null){
						addedShapes.push(addedShape);
						addedShapes = addedShapes.concat(addChildShapesRecursively(childShape));
					}
				});

				return addedShapes;
			}, this);

			var shapes = addChildShapesRecursively({
				childShapes: shapeObjects,
				resourceId: this.resourceId
			});


			// prepare deserialisation parameter
			shapes.each(this.bindTo(function (shape) {
				var properties = [];
				for (var field in shape.json.properties) {
					properties.push({
						prefix: 'oryx',
						name: field,
						value: shape.json.properties[field]
					});
				}

				// Outgoings
				shape.json.outgoing.each(function (out) {
					properties.push({
						prefix: 'raziel',
						name: 'outgoing',
						value: "#" + out.resourceId
					});
				});

				// Target 
				// (because of a bug, the first outgoing is taken when there is no target,
				// can be removed after some time)
				if (shape.object instanceof ms123.oryx.core.Edge) {
					var target = shape.json.target || shape.json.outgoing[0];
					if (target) {
						properties.push({
							prefix: 'raziel',
							name: 'target',
							value: "#" + target.resourceId
						});
					}
				}

				// Bounds
				if (shape.json.bounds) {
					properties.push({
						prefix: 'oryx',
						name: 'bounds',
						value: shape.json.bounds.upperLeft.x + "," + shape.json.bounds.upperLeft.y + "," + shape.json.bounds.lowerRight.x + "," + shape.json.bounds.lowerRight.y
					});
				}

				//Dockers [{x:40, y:50}, {x:30, y:60}] => "40 50 30 60  #"
				if (shape.json.dockers) {
					properties.push({
						prefix: 'oryx',
						name: 'dockers',
						value: shape.json.dockers.inject("", function (dockersStr, docker) {
							return dockersStr + docker.x + " " + docker.y + " ";
						}) + " #"
					});
				}

				//Parent
				properties.push({
					prefix: 'raziel',
					name: 'parent',
					value: shape.json.parent
				});

				shape.__properties = properties;
			}, this));

			// Deserialize the properties from the shapes
			// This can't be done earlier because Shape#deserialize expects that all referenced nodes are already there
			// first, deserialize all nodes
			shapes.each(function (shape) {
				if (shape.object instanceof ms123.oryx.core.Node) {
					shape.object.deserialize(shape.__properties, shape.json);
				}
			});

			// second, deserialize all edges
			shapes.each(function (shape) {
				if (shape.object instanceof ms123.oryx.core.Edge) {
					shape.object.deserialize(shape.__properties, shape.json);
					shape.object._oldBounds = shape.object.bounds.clone();
					shape.object._update();
				}
			});

			delete this.initializingShapes;
			return shapes.pluck("object");
		},

		/**
		 * Updates the size of the canvas, regarding to the containg shapes.
		 */
		updateSize: function () {
			// Check the size for the canvas
			var maxWidth = 0;
			var maxHeight = 0;
			var offset = 100;
			this.getChildShapes(true, function (shape) {
				var b = shape.bounds;
				maxWidth = Math.max(maxWidth, b.lowerRight().x + offset)
				maxHeight = Math.max(maxHeight, b.lowerRight().y + offset)
			});

			if (this.bounds.width() < maxWidth || this.bounds.height() < maxHeight) {
				this.setSize({
					width: Math.max(this.bounds.width(), maxWidth),
					height: Math.max(this.bounds.height(), maxHeight)
				})
			}
		},

		getRootNode: function () {
			return this.rootNode;
		},

		getSvgContainer: function () {
			return this.node.childNodes[1];
		},

		getHTMLContainer: function () {
			return this._htmlContainer;
		},

		/**
		 * Return all elements of the same highest level
		 * param {Object} elements
		 */
		getShapesWithSharedParent: function (elements) {

			// If there is no elements, return []
			if (!elements || elements.length < 1) {
				return []
			}
			// If there is one element, return this element
			if (elements.length == 1) {
				return elements
			}

			return elements.findAll(function (value) {
				var parentShape = value.parent;
				while (parentShape) {
					if (elements.member(parentShape)) return false;
					parentShape = parentShape.parent
				}
				return true;
			});

		},

		setSize: function (size, dontSetBounds) {
			if (!size || !size.width || !size.height) {
				return
			}

			if (this.rootNode.parentNode) {
				this.rootNode.parentNode.style.width = size.width + 'px';
				this.rootNode.parentNode.style.height = size.height + 'px';
			}

			this.rootNode.setAttributeNS(null, 'width', size.width);
			this.rootNode.setAttributeNS(null, 'height', size.height);

			//this._htmlContainer.style.top = "-" + (size.height + 4) + 'px';		
			if (!dontSetBounds) {
				this.bounds.set({
					a: {
						x: 0,
						y: 0
					},
					b: {
						x: size.width / this.zoomLevel,
						y: size.height / this.zoomLevel
					}
				})
			}
		},

		/**
		 * Returns an SVG document of the current process.
		 * param {Boolean} escapeText Use true, if you want to parse it with an XmlParser,
		 * 					false, if you want to use the SVG document in browser on client side.
		 */
		getSVGRepresentation: function (escapeText) {
			// Get the serialized svg image source
			var svgClone = this.getRootNode().cloneNode(true);

			this._removeInvisibleElements(svgClone);

			var x1, y1, x2, y2;
			try {
				var bb = this.getRootNode().childNodes[1].getBBox();
				x1 = bb.x;
				y1 = bb.y;
				x2 = bb.x + bb.width;
				y2 = bb.y + bb.height;
			} catch (e) {
				this.getChildShapes(true).each(function (shape) {
					var absBounds = shape.absoluteBounds();
					var ul = absBounds.upperLeft();
					var lr = absBounds.lowerRight();
					if (x1 == undefined) {
						x1 = ul.x;
						y1 = ul.y;
						x2 = lr.x;
						y2 = lr.y;
					} else {
						x1 = Math.min(x1, ul.x);
						y1 = Math.min(y1, ul.y);
						x2 = Math.max(x2, lr.x);
						y2 = Math.max(y2, lr.y);
					}
				});
			}

			var margin = 50;

			var width, height, tx, ty;
			if (x1 == undefined) {
				width = 0;
				height = 0;
				tx = 0;
				ty = 0;
			} else {
				width = x2 - x1;
				height = y2 - y1;
				tx = -x1 + margin / 2;
				ty = -y1 + margin / 2;
			}



			// Set the width and height
			svgClone.setAttributeNS(null, 'width', width + margin);
			svgClone.setAttributeNS(null, 'height', height + margin);

			svgClone.childNodes[1].firstChild.setAttributeNS(null, 'transform', 'translate(' + tx + ", " + ty + ')');

			//remove scale factor
			svgClone.childNodes[1].removeAttributeNS(null, 'transform');

			try {
				var svgCont = svgClone.childNodes[1].childNodes[1];
				svgCont.parentNode.removeChild(svgCont);
			} catch (e) {}

			if (escapeText) {
				$A(svgClone.getElementsByTagNameNS(ms123.oryx.Config.NAMESPACE_SVG, 'tspan')).each(function (elem) {
					elem.textContent = elem.textContent.escapeHTML();
				});

				$A(svgClone.getElementsByTagNameNS(ms123.oryx.Config.NAMESPACE_SVG, 'text')).each(function (elem) {
					if (elem.childNodes.length == 0) elem.textContent = elem.textContent.escapeHTML();
				});
			}

			// generating absolute urls for the pdf-exporter
			$A(svgClone.getElementsByTagNameNS(ms123.oryx.Config.NAMESPACE_SVG, 'image')).each(function (elem) {
				var href = elem.getAttributeNS("http://www.w3.org/1999/xlink", "href");

				if (!href.match("^(http|https)://")) {
					href = window.location.protocol + "//" + window.location.host + href;
					elem.setAttributeNS("http://www.w3.org/1999/xlink", "href", href);
				}
			});


			// escape all links
			$A(svgClone.getElementsByTagNameNS(ms123.oryx.Config.NAMESPACE_SVG, 'a')).each(function (elem) {
				elem.setAttributeNS("http://www.w3.org/1999/xlink", "xlink:href", (elem.getAttributeNS("http://www.w3.org/1999/xlink", "href") || "").escapeHTML());
			});

			return svgClone;
		},

		/**   
		 * Removes all nodes (and its children) that has the
		 * attribute visibility set to "hidden"
		 */
		_removeInvisibleElements: function (element) {
			var index = 0;
			while (index < element.childNodes.length) {
				var child = element.childNodes[index];
				if (child.getAttributeNS && child.getAttributeNS(null, "visibility") === "hidden") {
					element.removeChild(child);
				} else {
					this._removeInvisibleElements(child);
					index++;
				}
			}

		},


		_delegateEvent: function (event) {
			if (this.eventHandlerCallback && (event.target == this.rootNode || event.target == this.rootNode.parentNode)) {
				this.eventHandlerCallback(event, this);
			}
		},

		toString: function () {
			return "Canvas " + this.id
		},

		/**
		 * Calls {@link ms123.ms123.oryx.core.AbstractShape#toJSON} and adds some stencil set information.
		 */
		toJSON: function () {
			var json = this.base(arguments);

			//		if(ms123.oryx.Config.STENCILSET_HANDLER.length > 0) {
			//			json.stencilset = {
			//				url: this.getStencil().stencilSet().namespace()
			//	        };
			//		} else {
			json.stencilset = {
				url: this.getStencil().stencilSet().source(),
				namespace: this.getStencil().stencilSet().namespace()
			};
			//		}
			return json;
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {
	}

});
