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
*/

qx.Class.define("ms123.oryx.core.stencilset.ComplexPropertyItem", {
	extend: qx.core.Object,
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (jsonItem, namespace, property) {
		this.base(arguments);
		if (!jsonItem) {
			throw "ms123.oryx.core.stencilset.ComplexPropertyItem(construct): Parameter jsonItem is not defined.";
		}
		if (!namespace) {
			throw "ms123.oryx.core.stencilset.ComplexPropertyItem(construct): Parameter namespace is not defined.";
		}
		if (!property) {
			throw "ms123.oryx.core.stencilset.ComplexPropertyItem(construct): Parameter property is not defined.";
		}

		this._jsonItem = jsonItem;
		this._namespace = namespace;
		this._property = property;
		this._items = new Hash();

		//init all values
		if (!jsonItem.name) {
			throw "ms123.oryx.core.stencilset.ComplexPropertyItem(construct): Name is not defined.";
		}

		if (!jsonItem.type) {
			throw "ms123.oryx.core.stencilset.ComplexPropertyItem(construct): Type is not defined.";
		} else {
			jsonItem.type = jsonItem.type.toLowerCase();
		}

		if (jsonItem.type === ms123.oryx.Config.TYPE_CHOICE) {
			if (jsonItem.items && jsonItem.items instanceof Array) {
				jsonItem.items.each((function (item) {
					this._items[item.value] = new ms123.oryx.core.stencilset.PropertyItem(item, namespace, this);
				}).bind(this));
			} else {
				throw "ms123.oryx.core.stencilset.Property(construct): No property items defined."
			}
		}
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
		 * param {ORYX.Core.StencilSet.PropertyItem} item
		 * @return {Boolean} True, if item has the same namespace and id.
		 */
		equals: function (item) {
			return (this.property().equals(item.property()) && this.name() === item.name());
		},

		namespace: function () {
			return this._namespace;
		},

		property: function () {
			return this._property;
		},

		name: function () {
			return ms123.oryx.core.StencilSet.getTranslation(this._jsonItem, "name");
		},

		id: function () {
			return this._jsonItem.id;
		},

		type: function () {
			return this._jsonItem.type;
		},

		config: function () {
			return this._jsonItem.config;
		},

		optional: function () {
			return this._jsonItem.optional;
		},

		width: function () {
			return this._jsonItem.width;
		},

		value: function () {
			return this._jsonItem.value;
		},

		filter: function () {
			return this._jsonItem.filter;
		},

		items: function () {
			return this._items.values();
		},

		disable: function () {
			return this._jsonItem.disable;
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {
	}

});
