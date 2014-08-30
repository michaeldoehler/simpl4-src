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
	* @ignore($H)
*/
qx.Class.define("ms123.oryx.core.MoveDockersCommand", {
	extend: ms123.oryx.core.Command,
 include : [ ms123.util.MBindTo ],
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (dockers) {
		this.base(arguments);
		this.dockers = $H(dockers);
		this.edges = $H({});
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
			if (this.changes) {
				this.executeAgain();
				return;
			} else {
				this.changes = $H({});
			}

			this.dockers.values().each(this.bindTo(function (docker) {
				var edge = docker.docker.parent;
				if (!edge) {
					return
				}

				if (!this.changes[edge.getId()]) {
					this.changes[edge.getId()] = {
						edge: edge,
						oldDockerPositions: edge.dockers.map(function (r) {
							return r.bounds.center()
						})
					}
				}
				docker.docker.bounds.moveBy(docker.offset);
				this.edges[edge.getId()] = edge;
				docker.docker.update();
			},this));
			this.edges.each(this.bindTo(function (edge) {
				this.updateEdge(edge.value);
				if (this.changes[edge.value.getId()]) this.changes[edge.value.getId()].dockerPositions = edge.value.dockers.map(function (r) {
					return r.bounds.center()
				})
			},this));
		},
		updateEdge: function (edge) {
			edge._update(true);
			[edge.getOutgoingShapes(), edge.getIncomingShapes()].flatten().invoke("_update", [true])
		},
		executeAgain: function () {
			this.changes.values().each(this.bindTo(function (change) {
				// Reset the dockers
				this.removeAllDocker(change.edge);
				change.dockerPositions.each(this.bindTo(function (pos, i) {
					if (i == 0 || i == change.dockerPositions.length - 1) {
						return
					}
					var docker = change.edge.createDocker(undefined, pos);
					docker.bounds.centerMoveTo(pos);
					docker.update();
				},this));
				this.updateEdge(change.edge);
			},this));
		},
		rollback: function () {
			this.changes.values().each(this.bindTo(function (change) {
				// Reset the dockers
				this.removeAllDocker(change.edge);
				change.oldDockerPositions.each(this.bindTo(function (pos, i) {
					if (i == 0 || i == change.oldDockerPositions.length - 1) {
						return
					}
					var docker = change.edge.createDocker(undefined, pos);
					docker.bounds.centerMoveTo(pos);
					docker.update();
				},this));
				this.updateEdge(change.edge);
			},this));
		},
		removeAllDocker: function (edge) {
			edge.dockers.slice(1, edge.dockers.length - 1).each(function (docker) {
				edge.removeDocker(docker);
			})
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
