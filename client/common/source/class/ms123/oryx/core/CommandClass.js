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
qx.Class.define("ms123.oryx.core.CommandClass", {
	extend: ms123.oryx.core.Command,
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (jsonObject, loadSerializedCB, noSelectionAfterImport, facade) {
		this.base(arguments);
		this.jsonObject = jsonObject;
		this.noSelection = noSelectionAfterImport;
		this.facade = facade;
		this.shapes;
		this.connections = [];
		this.parents = new Hash();
		this.selection = this.facade.getSelection();
		this.loadSerialized = loadSerializedCB;
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

			if (!this.shapes) {
				// Import the shapes out of the serialization		
				this.shapes = this.loadSerialized(this.jsonObject);

				//store all connections
				this.shapes.each(this.bindTo(function (shape) {

					if (shape.getDockers) {
						var dockers = shape.getDockers();
						if (dockers) {
							if (dockers.length > 0) {
								this.connections.push([dockers.first(), dockers.first().getDockedShape(), dockers.first().referencePoint]);
							}
							if (dockers.length > 1) {
								this.connections.push([dockers.last(), dockers.last().getDockedShape(), dockers.last().referencePoint]);
							}
						}
					}

					//store parents
					this.parents[shape.id] = shape.parent;
				}, this));
			} else {
				this.shapes.each(this.bindTo(function (shape) {
					this.parents[shape.id].add(shape);
				}, this));

				this.connections.each(function (con) {
					con[0].setDockedShape(con[1]);
					con[0].setReferencePoint(con[2]);
					con[0].update();
				});
			}

			//this.parents.values().uniq().invoke("update");
			this.facade.getCanvas().update();

			if (!this.noSelection) this.facade.setSelection(this.shapes);
			else this.facade.updateSelection();

			// call updateSize again, because during loadSerialized the edges' bounds  
			// are not yet initialized properly
			this.facade.getCanvas().updateSize();

		},
		rollback: function () {
			var selection = this.facade.getSelection();

			this.shapes.each(this.bindTo(function (shape) {
				selection = selection.without(shape);
				this.facade.deleteShape(shape);
			}, this));


			this.facade.getCanvas().update();

			this.facade.setSelection(selection);
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
