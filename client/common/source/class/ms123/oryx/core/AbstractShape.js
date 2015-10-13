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
	* @ignore($A)
*/
qx.Class.define("ms123.oryx.core.AbstractShape", {
	extend: ms123.oryx.core.UIObject,
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (options,stencil) {
		this.base(arguments,options);
		this.resourceId = ms123.oryx.Editor.provideId(); //Id of resource in DOM
		// stencil reference
		this._stencil = stencil;
		// if the stencil defines a super stencil that should be used for its instances, set it.
		if (this._stencil._jsonStencil.superId) {
			var stencilId = this._stencil.id()
			var superStencilId = stencilId.substring(0, stencilId.indexOf("#") + 1) + stencil._jsonStencil.superId;
			var stencilSet = this._stencil.stencilSet();
			this._stencil = stencilSet.stencil(superStencilId);
		}

		//Hash map for all properties. Only stores the values of the properties.
		this.properties = new Hash();
		this.propertiesChanged = new Hash();

		// List of properties which are not included in the stencilset, 
		// but which gets (de)serialized
		this.hiddenProperties = new Hash();


		//Initialization of property map and initial value.
		this._stencil.properties().each((function (property) {
			var key = property.prefix() + "-" + property.id();
			this.properties[key] = property.value();
			this.propertiesChanged[key] = true;
		}).bind(this));

		// if super stencil was defined, also regard stencil's properties:
		if (stencil._jsonStencil.superId) {
			stencil.properties().each((function (property) {
				var key = property.prefix() + "-" + property.id();
				var value = property.value();
				var oldValue = this.properties[key];
				this.properties[key] = value;
				this.propertiesChanged[key] = true;

				// Raise an event, to show that the property has changed
				// required for plugins like processLink.js
				//window.setTimeout( function(){
				this._delegateEvent({
					type: ms123.oryx.Config.EVENT_PROPERTY_CHANGED,
					name: key,
					value: value,
					oldValue: oldValue
				});

				//}.bind(this), 10)
			}).bind(this));
		}

	},


	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},

	/******************************************************************************
	 STATICS
	 ******************************************************************************/
	statics: {
		JSONHelper : {
			/**
			 * Iterates over each child shape.
			 * param {Object} iterator Iterator function getting a child shape and his parent as arguments.
			 * param {boolean} [deep=false] Iterate recursively (childShapes of childShapes)
			 * param {boolean} [modify=false] If true, the result of the iterator function is taken as new shape, return false to delete it. This enables modifying the object while iterating through the child shapes.
			 * @example
			 * // Increases the lowerRight x value of each direct child shape by one. 
			 * myShapeAsJson.eachChild(function(shape, parentShape){
			 *     shape.bounds.lowerRight.x = shape.bounds.lowerRight.x + 1;
			 *     return shape;
			 * }, false, true);
			 */
			eachChild: function (iterator, deep, modify) {
				if (!this.childShapes) return;

				var newChildShapes = []; //needed if modify = true
				this.childShapes.each(qx.lang.Function.bind(function (shape) {
					if (!(shape.eachChild instanceof Function)) {
						//Ext.apply(shape, ORYX.Core.AbstractShape.JSONHelper);
						qx.lang.Object.mergeWith(shape, ms123.oryx.core.AbstractShape.JSONHelper);
					}
					var res = iterator(shape, this);
					if (res) newChildShapes.push(res); //if false is returned, and modify = true, current shape is deleted.
					if (deep) shape.eachChild(iterator, deep, modify);
				},this));

				if (modify) this.childShapes = newChildShapes;
			},

			getShape: function () {
				return null;
			},
			getChildShapes: function (deep) {
				var allShapes = this.childShapes;

				if (deep) {
					this.eachChild(function (shape) {
						if (!(shape.getChildShapes instanceof Function)) {
						//	Ext.apply(shape, ORYX.Core.AbstractShape.JSONHelper);
							qx.lang.Object.mergeWith(shape, ms123.oryx.core.AbstractShape.JSONHelper);
						}
						allShapes = allShapes.concat(shape.getChildShapes(deep));
					}, true);
				}

				return allShapes;
			},

			/**
			 * @return {String} Serialized JSON object
			 */
			serialize: function () {
				return qx.util.Serializer.toJson(this);
			}
		}
	},
	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		layout: function () {

		},

		/**
		 * Returns the stencil object specifiing the type of the shape.
		 */
		getStencil: function () {
			return this._stencil;
		},

		/**
		 * 
		 * param {Object} resourceId
		 */
		getChildShapeByResourceId: function (resourceId) {

			resourceId = this.__stripHashes(resourceId);

			return this.getChildShapes(true).find(function (shape) {
				return shape.resourceId == resourceId
			});
		},
		/**
		 * 
		 * param {Object} deep
		 * param {Object} iterator
		 */
		getChildShapes: function (deep, iterator) {
			var result = [];

			this.children.each(function (uiObject) {
				if (uiObject instanceof ms123.oryx.core.Shape && uiObject.isVisible) {
					if (iterator) {
						iterator(uiObject);
					}
					result.push(uiObject);
					if (deep) {
						result = result.concat(uiObject.getChildShapes(deep, iterator));
					}
				}
			});

			return result;
		},

		/**
		 * param {Object} shape
		 * @return {boolean} true if any of shape's childs is given shape
		 */
		hasChildShape: function (shape) {
			return this.getChildShapes().any(function (child) {
				return (child === shape) || child.hasChildShape(shape);
			});
		},

		/**
		 * 
		 * param {Object} deep
		 * param {Object} iterator
		 */
		getChildNodes: function (deep, iterator) {
			var result = [];

			this.children.each(function (uiObject) {
				if (uiObject instanceof ms123.oryx.core.Node && uiObject.isVisible) {
					if (iterator) {
						iterator(uiObject);
					}
					result.push(uiObject);
				}
				if (uiObject instanceof ms123.oryx.core.Shape) {
					if (deep) {
						result = result.concat(uiObject.getChildNodes(deep, iterator));
					}
				}
			});

			return result;
		},

		/**
		 * 
		 * param {Object} deep
		 * param {Object} iterator
		 */
		getChildEdges: function (deep, iterator) {
			var result = [];

			this.children.each(function (uiObject) {
				if (uiObject instanceof ms123.oryx.core.Edge && uiObject.isVisible) {
					if (iterator) {
						iterator(uiObject);
					}
					result.push(uiObject);
				}
				if (uiObject instanceof ms123.oryx.core.Shape) {
					if (deep) {
						result = result.concat(uiObject.getChildEdges(deep, iterator));
					}
				}
			});

			return result;
		},

		/**
		 * Returns a sorted array of ORYX.Core.Node objects.
		 * Ordered in z Order, the last object has the highest z Order.
		 */
		//TODO deep iterator
		getAbstractShapesAtPosition: function () {
			var x, y;
			switch (arguments.length) {
			case 1:
				x = arguments[0].x;
				y = arguments[0].y;
				break;
			case 2:
				//two or more arguments
				x = arguments[0];
				y = arguments[1];
				break;
			default:
				throw "getAbstractShapesAtPosition needs 1 or 2 arguments!"
			}

			if (this.isPointIncluded(x, y)) {

				var result = [];
				result.push(this);

				//check, if one child is at that position						

				var childNodes = this.getChildNodes();
				var childEdges = this.getChildEdges();

				[childNodes, childEdges].each(function (ne) {
					var nodesAtPosition = new Hash();

					ne.each(function (node) {
						if (!node.isVisible) {
							return
						}
						var candidates = node.getAbstractShapesAtPosition(x, y);
						if (candidates.length > 0) {
							var nodesInZOrder = $A(node.node.parentNode.childNodes);
							var zOrderIndex = nodesInZOrder.indexOf(node.node);
							nodesAtPosition[zOrderIndex] = candidates;
						}
					});

					nodesAtPosition.keys().sort().each(function (key) {
						result = result.concat(nodesAtPosition[key]);
					});
				});

				return result;

			} else {
				return [];
			}
		},

		/**
		 * 
		 * param key {String} Must be 'prefix-id' of property
		 * param value {Object} Can be of type String or Number according to property type.
		 */
		setProperty: function (key, value, force) {
			var oldValue = this.properties[key];
			if (oldValue !== value || force === true) {
				this.properties[key] = value;
				this.propertiesChanged[key] = true;
				this._changed();

				// Raise an event, to show that the property has changed
				//window.setTimeout( function(){
				if (!this._isInSetProperty) {
					this._isInSetProperty = true;

					this._delegateEvent({
						type: ms123.oryx.Config.EVENT_PROPERTY_CHANGED,
						elements: [this],
						name: key,
						value: value,
						oldValue: oldValue
					});

					delete this._isInSetProperty;
				}
				//}.bind(this), 10)
			}
		},

		/**
		 * Returns TRUE if one of the properties is flagged as dirty
		 * @return {boolean}
		 */
		isPropertyChanged: function () {
			return this.propertiesChanged.any(function (property) {
				return property.value
			});
		},

		/**
		 * 
		 * param {String} Must be 'prefix-id' of property
		 * param {Object} Can be of type String or Number according to property type.
		 */
		setHiddenProperty: function (key, value) {
			// IF undefined, Delete
			if (value === undefined) {
				delete this.hiddenProperties[key];
				return;
			}
			var oldValue = this.hiddenProperties[key];
			if (oldValue !== value) {
				this.hiddenProperties[key] = value;
			}
		},
		/**
		 * Calculate if the point is inside the Shape
		 * param {Point}
		 */
		isPointIncluded: function (pointX, pointY, absoluteBounds) {
			var absBounds = absoluteBounds ? absoluteBounds : this.absoluteBounds();
			return absBounds.isIncluded(pointX, pointY);

		},

		/**
		 * Get the serialized object
		 * return Array with hash-entrees (prefix, name, value)
		 * Following values will given:
		 * 		Type
		 * 		Properties
		 */
		serialize: function () {
			var serializedObject = [];

			// Add the type
			serializedObject.push({
				name: 'type',
				prefix: 'oryx',
				value: this.getStencil().id(),
				type: 'literal'
			});

			// Add hidden properties
			this.hiddenProperties.each(qx.lang.Function.bind(function (prop) {
				serializedObject.push({
					name: prop.key.replace("oryx-", ""),
					prefix: "oryx",
					value: prop.value,
					type: 'literal'
				});
			},this));

			// Add all properties
			this.getStencil().properties().each((function (property) {

				var prefix = property.prefix(); // Get prefix
				var name = property.id(); // Get name
				//if(typeof this.properties[prefix+'-'+name] == 'boolean' || this.properties[prefix+'-'+name] != "")
				serializedObject.push({
					name: name,
					prefix: prefix,
					value: this.properties[prefix + '-' + name],
					type: 'literal'
				});

			}).bind(this));

			return serializedObject;
		},


		deserialize: function (serialize) {
			// Search in Serialize
			var initializedDocker = 0;

			// Sort properties so that the hidden properties are first in the list
/*@@@MS Don't no for what			serialize = serialize.sort(qx.lang.Function.bind(function (a, b) {
				a = Number(this.properties.keys().member(a.prefix + "-" + a.name));
				b = Number(this.properties.keys().member(b.prefix + "-" + b.name));
				return a > b ? 1 : (a < b ? -1 : 0)
			},this));*/

			serialize.each((function (obj) {

				var name = obj.name;
				var prefix = obj.prefix;
				var value = obj.value;

				// Complex properties can be real json objects, encode them to a string
				//if (Ext.type(value) === "object"){
				if (typeof value == "object"){
					 if( value != null){
						 value = qx.util.Serializer.toJson(value);
					 }
				}

				switch (prefix + "-" + name) {
				case 'raziel-parent':
					// Set parent
					if (!this.parent) {
						break
					};

					// Set outgoing Shape
					var parent = this.getCanvas().getChildShapeByResourceId(value);
					if (parent) {
						parent.add(this);
					}

					break;
				default:
					// If list, eval as an array
					var prop = this.getStencil().property(prefix + "-" + name);
					if (prop && prop.isList() && typeof value === "string") {
						if ((value || "").strip() && !value.startsWith("[") && !value.startsWith("]")) value = "[\"" + value.strip() + "\"]";
						value = ((value || "").strip() || "[]").evalJSON();
					}

					// Set property
					if (this.properties.keys().member(prefix + "-" + name)) {
						this.setProperty(prefix + "-" + name, value);
					} else if (!(name === "bounds" || name === "parent" || name === "target" || name === "dockers" || name === "docker" || name === "outgoing" || name === "incoming")) {
						this.setHiddenProperty(prefix + "-" + name, value);
					}

				}
			}).bind(this));
		},

		toString: function () {
			return "ms123.oryx.core.AbstractShape " + this.id
		},

		/**
		 * Converts the shape to a JSON representation.
		 * @return {Object} A JSON object with included ORYX.Core.AbstractShape.JSONHelper and getShape() method.
		 */
		toJSON: function () {
			var json = {
				resourceId: this.resourceId,
				properties: ms123.util.Clone.merge({}, this.properties, this.hiddenProperties).inject({}, qx.lang.Function.bind(function (props, prop) {
					var key = prop[0];
					var value = prop[1];

					//If complex property, value should be a json object
					if (this.getStencil().property(key) && this.getStencil().property(key).type() === ms123.oryx.Config.TYPE_COMPLEX && 
							//Ext.type(value) === "string"
							typeof value == "string"
							) {

						try {
							value = qx.lang.Json.parse(value);
						} catch (error) {}

						// Parse date
					} else if (value instanceof Date && this.getStencil().property(key)) {
						try {
							value = value.format(this.getStencil().property(key).dateFormat());
						} catch (e) {}
					}

					//Takes "my_property" instead of "oryx-my_property" as key
					key = key.replace(/^[\w_]+-/, "");
					props[key] = value;

					return props;
				},this)),
				stencil: {
					id: this.getStencil().idWithoutNs()
				},
				childShapes: this.getChildShapes().map(function (shape) {
					return shape.toJSON()
				})
			};

			if (this.getOutgoingShapes) {
				json.outgoing = this.getOutgoingShapes().map(function (shape) {
					return {
						resourceId: shape.resourceId
					};
				});
			}

			if (this.bounds) {
				json.bounds = {
					lowerRight: this.bounds.lowerRight(),
					upperLeft: this.bounds.upperLeft()
				};
			}

			if (this.dockers) {
				json.dockers = this.dockers.map(function (docker) {
					var d = docker.getDockedShape() && docker.referencePoint ? docker.referencePoint : docker.bounds.center();
					d.getDocker = function () {
						return docker;
					};
					return d;
				})
			}

			//Ext.apply(json, ms123.oryx.core.AbstractShape.JSONHelper);
			qx.lang.Object.mergeWith(json, ms123.oryx.core.AbstractShape.JSONHelper);

			// do not pollute the json attributes (for serialization), so put the corresponding
			// shape is encapsulated in a method
			json.getShape = (function () {
				return this;
			}).bind(this);

			return json;
		},
		__stripHashes: function (s) {
		 return (s && s.substring(0, 1) == '#') ? s.substring(1, s.length) : s;
	  }
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {
	}

});
