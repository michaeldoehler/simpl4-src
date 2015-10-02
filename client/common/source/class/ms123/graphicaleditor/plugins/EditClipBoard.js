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

qx.Class.define("ms123.graphicaleditor.plugins.EditClipBoard", {
	extend: qx.core.Object,
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (namespace) {
		this.base(arguments);
		var store = new qx.data.store.Offline("clipboard-"+namespace);
		var model = null;
		if (store.getModel() === null) {
			model = qx.data.marshal.Json.createModel({
				shapesAsJson : [],
				namespace : "",
				useOffset : true
			}, true);
			model.setNamespace(namespace);
			model.setUseOffset(true);
			model.setShapesAsJson([]);
			store.setModel(model);
		} else {
			model = store.getModel();
		}
		this._offlineModel = model;
	},


	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},

	/******************************************************************************
	 STATICS
	 ******************************************************************************/
	statics: {
		__clipboards:{},
		getClipboard:function(namespace){
			var clipboard = ms123.graphicaleditor.plugins.EditClipBoard.__clipboards[namespace];
			if( clipboard == null){
				clipboard = new ms123.graphicaleditor.plugins.EditClipBoard(namespace);
				ms123.graphicaleditor.plugins.EditClipBoard.__clipboards[namespace] = clipboard;
			}
			return clipboard;;
		}
	},
	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		isOccupied: function () {
			return this._offlineModel.getShapesAsJson().length > 0;
		},
		get:function(){
			var a = this._offlineModel.getShapesAsJson();
			if( a.getLength ){
			 return qx.lang.Json.parse(qx.util.Serializer.toJson(a));
			}else{
				return a;
			}
		},
		getNamespace:function(){
			return this._offlineModel.getNamespace();
		},
		useOffset:function(){
			return this._offlineModel.getUseOffset();
		},
		setUseOffset:function(b){
			this._offlineModel.setUseOffset(b);
		},
		refresh: function (shapes, namespace, useNoOffset) {
			this._offlineModel.setUseOffset(useNoOffset !== true);

			var json = shapes.map(function (shape) {
				var s = shape.toJSON();
				s.parent = {
					resourceId: shape.getParentShape().resourceId
				};
				s.parentIndex = shape.getParentShape().getChildShapes().indexOf(shape)
				return s;
			});
			this._offlineModel.setShapesAsJson(json);
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
