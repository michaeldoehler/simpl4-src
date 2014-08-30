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
	* @ignore(Hash)
	* @ignore(Ajax.Request)
	* @ignore(jsonExtension.*)
	* @lint ignoreDeprecated(alert,eval) 
*/
qx.Class.define("ms123.oryx.core.stencilset.StencilSet", {
	extend: qx.core.Object,
 include : [ ms123.util.MBindTo ],
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (source) {
		this.base(arguments);
		if (!source) {
			throw "ms123.oryx.core.stencilset.StencilSet(construct): Parameter 'source' is not defined.";
		}

		if (source.endsWith("/")) {
			source = source.substr(0, source.length - 1);
		}

		this._extensions = new Hash();

		this._source = source;
		this._baseUrl = "xconfig/stencilset";

		this._stencils = new Hash();
		this._availableStencils = new Hash();

		var cm = new ms123.config.ConfigManager();
		this._jsonObject = cm.getStencilSet(source);
		this._init();

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
		 * Finds a root stencil in this stencil set. There may be many of these. If
		 * there are, the first one found will be used. In Firefox, this is the
		 * topmost definition in the stencil set description file.
		 */
		findRootStencilName: function () {

			// find any stencil that may be root.
			var rootStencil = this._stencils.values().find(function (stencil) {
				return stencil._jsonStencil.mayBeRoot
			});

			// if there is none, just guess the first.
			if (!rootStencil) {
				new ms123.form.Alert("Did not find any stencil that may be root. Taking a guess.");
				rootStencil = this._stencils.values()[0];
			}

			// return its id.
			return rootStencil.id();
		},

		/**
		 * param {ms123.oryx.core.stencilset.StencilSet} stencilSet
		 * @return {Boolean} True, if stencil set has the same namespace.
		 */
		equals: function (stencilSet) {
			return (this.namespace() === stencilSet.namespace());
		},

		/**
		 * 
		 * param {ms123.oryx.core.stencilset.Stencil} rootStencil If rootStencil is defined, it only returns stencils
		 * 			that could be (in)direct child of that stencil.
		 */
		stencils: function (rootStencil, rules, sortByGroup) {
			if (rootStencil && rules) {
				var stencils = this._availableStencils.values();
				var containers = [rootStencil];
				var checkedContainers = [];

				var result = [];

				while (containers.size() > 0) {
					var container = containers.pop();
					checkedContainers.push(container);
					var children = stencils.findAll(function (stencil) {
						var args = {
							containingStencil: container,
							containedStencil: stencil
						};
						return rules.canContain(args);
					});
					for (var i = 0; i < children.size(); i++) {
						if (!checkedContainers.member(children[i])) {
							containers.push(children[i]);
						}
					}
					result = result.concat(children).uniq();
				}

				// Sort the result to the origin order
				result = result.sortBy(function (stencil) {
					return stencils.indexOf(stencil);
				});


				if (sortByGroup) {
					result = result.sortBy(function (stencil) {
						return stencil.groups().first();
					});
				}

				var edges = stencils.findAll(function (stencil) {
					return stencil.type() == "edge";
				});
				result = result.concat(edges);

				return result;

			} else {
				if (sortByGroup) {
					return this._availableStencils.values().sortBy(function (stencil) {
						return stencil.groups().first();
					});
				} else {
					return this._availableStencils.values();
				}
			}
		},

		nodes: function () {
			return this._availableStencils.values().findAll(function (stencil) {
				return (stencil.type() === 'node')
			});
		},

		edges: function () {
			return this._availableStencils.values().findAll(function (stencil) {
				return (stencil.type() === 'edge')
			});
		},

		stencil: function (id) {
			return this._stencils[id];
		},

		title: function () {
			return ms123.oryx.core.StencilSet.getTranslation(this._jsonObject, "title");
		},

		description: function () {
			return ms123.oryx.core.StencilSet.getTranslation(this._jsonObject, "description");
		},

		namespace: function () {
			return this._jsonObject ? this._jsonObject.namespace : null;
		},

		prefix: function () {
			return this._jsonObject ? (this._jsonObject.prefix==undefined?"oryx":this._jsonObject.prefix) : "oryx";
		},

		idIsCaseSensitiv: function () {
			return this._jsonObject ? (this._jsonObject.idIsCaseSensitiv==undefined?false:this._jsonObject.idIsCaseSensitiv) : false;
		},

		jsonRules: function () {
			return this._jsonObject ? this._jsonObject.rules : null;
		},

		source: function () {
			return this._source;
		},

		extensions: function () {
			return this._extensions;
		},

		addExtension: function (url) {

			new Ajax.Request(url, {
				method: 'GET',
				asynchronous: false,
				onSuccess: (function (transport) {
					this.addExtensionDirectly(transport.responseText);
				}).bind(this),
				onFailure: (function (transport) {
					new ms123.form.Alert("Loading stencil set extension file failed. The request returned an error." + transport);
				}).bind(this),
				onException: (function (transport) {
					new ms123.form.Alert("Loading stencil set extension file failed. The request returned an error." + transport);
				}).bind(this)

			});
		},

		addExtensionDirectly: function (str) {

			try {
				eval("var jsonExtension = " + str);

				if (!(jsonExtension["extends"].endsWith("#"))) jsonExtension["extends"] += "#";

				if (jsonExtension["extends"] == this.namespace()) {
					this._extensions[jsonExtension.namespace] = jsonExtension;

					var defaultPosition = this._stencils.keys().size();
					//load new stencils
					if (jsonExtension.stencils) {
						$A(jsonExtension.stencils).each(this.bindTo(function (stencil) {
							defaultPosition++;
							var oStencil = new ms123.oryx.core.stencilset.Stencil(stencil, this.namespace(), this._baseUrl, this, undefined, defaultPosition);
							this._stencils[oStencil.id()] = oStencil;
							this._availableStencils[oStencil.id()] = oStencil;
						},this));
					}

					//load additional properties
					if (jsonExtension.properties) {
						var stencils = this._stencils.values();

						stencils.each(this.bindTo(function (stencil) {
							var roles = stencil.roles();

							jsonExtension.properties.each(function (prop) {
								prop.roles.any(function (role) {
									role = jsonExtension["extends"] + role;
									if (roles.member(role)) {
										prop.properties.each(function (property) {
											stencil.addProperty(property, jsonExtension.namespace);
										});

										return true;
									}
									else return false;
								})
							})
						},this));
					}

					//remove stencil properties
					if (jsonExtension.removeproperties) {
						jsonExtension.removeproperties.each(this.bindTo(function (remprop) {
							var stencil = this.stencil(jsonExtension["extends"] + remprop.stencil);
							if (stencil) {
								remprop.properties.each(function (propId) {
									stencil.removeProperty(propId);
								});
							}
						},this));
					}

					//remove stencils
					if (jsonExtension.removestencils) {
						$A(jsonExtension.removestencils).each(this.bindTo(function (remstencil) {
							delete this._availableStencils[jsonExtension["extends"] + remstencil];
						},this));
					}
				}
			} catch (e) {
				new ms123.form.Alert("StencilSet.addExtension: Something went wrong when initialising the stencil set extension. " + e);
			}
		},

		removeExtension: function (namespace) {
			var jsonExtension = this._extensions[namespace];
			if (jsonExtension) {

				//unload extension's stencils
				if (jsonExtension.stencils) {
					$A(jsonExtension.stencils).each(this.bindTo(function (stencil) {
						var oStencil = new ms123.oryx.core.stencilset.Stencil(stencil, this.namespace(), this._baseUrl, this);
						delete this._stencils[oStencil.id()]; // maybe not ??
						delete this._availableStencils[oStencil.id()];
					},this));
				}

				//unload extension's properties
				if (jsonExtension.properties) {
					var stencils = this._stencils.values();

					stencils.each(this.bindTo(function (stencil) {
						var roles = stencil.roles();

						jsonExtension.properties.each(function (prop) {
							prop.roles.any(function (role) {
								role = jsonExtension["extends"] + role;
								if (roles.member(role)) {
									prop.properties.each(function (property) {
										stencil.removeProperty(property.id);
									});

									return true;
								}
								else return false;
							})
						})
					},this));
				}

				//restore removed stencil properties
				if (jsonExtension.removeproperties) {
					jsonExtension.removeproperties.each(this.bindTo(function (remprop) {
						var stencil = this.stencil(jsonExtension["extends"] + remprop.stencil);
						if (stencil) {
							var stencilJson = $A(this._jsonObject.stencils).find(function (s) {
								return s.id == stencil.id()
							});
							remprop.properties.each(this.bindTo(function (propId) {
								var propertyJson = $A(stencilJson.properties).find(function (p) {
									return p.id == propId
								});
								stencil.addProperty(propertyJson, this.namespace());
							},this));
						}
					},this));
				}

				//restore removed stencils
				if (jsonExtension.removestencils) {
					$A(jsonExtension.removestencils).each(this.bindTo(function (remstencil) {
						var sId = jsonExtension["extends"] + remstencil;
						this._availableStencils[sId] = this._stencils[sId];
					},this));
				}
			}
			delete this._extensions[namespace];
		},

		__handleStencilset: function () {
			var jo = this._jsonObject;
			// assert there is a namespace.
			if (!jo.namespace || jo.namespace === "") throw "Namespace definition missing in stencilset.";

			if (!(jo.stencils instanceof Array)) throw "Stencilset corrupt.";

			// assert namespace ends with '#'.
			if (!jo.namespace.endsWith("#")) jo.namespace = jo.namespace + "#";

			// assert title and description are strings.
			if (!jo.title) jo.title = "";
			if (!jo.description) jo.description = "";
		},

		/**
		 * This method is called when the HTTP request to get the requested stencil
		 * set succeeds. The response is supposed to be a JSON representation
		 * according to the stencil set specification.
		 * param {Object} response The JSON representation according to the
		 * 			stencil set specification.
		 */
		_init: function () {

			// init and check consistency.
			this.__handleStencilset();

			var pps = new Hash();

			// init property packages
			if (this._jsonObject.propertyPackages) {
				$A(this._jsonObject.propertyPackages).each((function (pp) {
					pps[pp.name] = pp.properties;
				}).bind(this));
			}

			var defaultPosition = 0;

			// init each stencil
			$A(this._jsonObject.stencils).each((function (stencil) {
				defaultPosition++;

				// instantiate normally.
				var oStencil = new ms123.oryx.core.stencilset.Stencil(stencil, this.namespace(), this._baseUrl, this, pps, defaultPosition);
				this._stencils[oStencil.id()] = oStencil;
				this._availableStencils[oStencil.id()] = oStencil;

			}).bind(this));
		},

		toString: function () {
			return "StencilSet " + this.title() + " (" + this.namespace() + ")";
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {
	}
});
