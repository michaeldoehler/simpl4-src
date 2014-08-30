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
*/

qx.Class.define("ms123.graphicaleditor.plugins.AddDocker", {
	extend: qx.core.Object,
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (facade) {
		this.base(arguments);
		this.facade = facade;

		this.facade.offer({
			'name': ms123.oryx.Translation.AddDocker.add,
			'functionality': this.enableAddDocker.bind(this),
			'group': ms123.oryx.Translation.AddDocker.group,
			'icon': this.__getResourceUrl("vector_add.png"),
			'description': ms123.oryx.Translation.AddDocker.addDesc,
			'index': 1,
			'toggle': true,
			'minShape': 0,
			'maxShape': 0
		});


		this.facade.offer({
			'name': ms123.oryx.Translation.AddDocker.del,
			'functionality': this.enableDeleteDocker.bind(this),
			'group': ms123.oryx.Translation.AddDocker.group,
			'icon': this.__getResourceUrl("vector_delete.png"),
			'description': ms123.oryx.Translation.AddDocker.delDesc,
			'index': 2,
			'toggle': true,
			'minShape': 0,
			'maxShape': 0
		});

		this.facade.registerOnEvent(ms123.oryx.Config.EVENT_MOUSEDOWN, this.handleMouseDown.bind(this));
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
		enableAddDocker: function (e, pressed) {
			this.addDockerButton = e.getTarget();
			console.log("enableAddDocker:" + this.addDockerButton + "/" + pressed);

			// Unpress deleteDockerButton
			if (pressed && this.deleteDockerButton) this.deleteDockerButton.setValue(false);
		},
		enableDeleteDocker: function (e, pressed) {
			this.deleteDockerButton = e.getTarget();

			// Unpress addDockerButton
			if (pressed && this.addDockerButton) this.addDockerButton.setValue(false);
		},

		enabledAdd: function () {
			if (this.addDockerButton) {
				console.log("enabledAdd.pressed:" + this.addDockerButton.getValue());
			}
			return this.addDockerButton ? this.addDockerButton.getValue() : false;
		},
		enabledDelete: function () {
			return this.deleteDockerButton ? this.deleteDockerButton.getValue() : false;
		},

		/**
		 * MouseDown Handler
		 *
		 */
		handleMouseDown: function (event, uiObj) {
			console.log("handleMouseDown.inst:" + (uiObj instanceof ms123.oryx.core.Edge) + "/e:" + this.enabledAdd() + "/" + event);

			if (this.enabledAdd() && uiObj instanceof ms123.oryx.core.Edge) {
				this.newDockerCommand({
					edge: uiObj,
					position: this.facade.eventCoordinates(event)
				});
			} else if (this.enabledDelete() && uiObj instanceof ms123.oryx.core.controls.Docker && uiObj.parent instanceof ms123.oryx.core.Edge) {
				this.newDockerCommand({
					edge: uiObj.parent,
					docker: uiObj
				});
			} else if (this.enabledAdd()) {
				this.addDockerButton.setValue(false);
			} else if (this.enabledDelete()) {
				this.deleteDockerButton.setValue(false);
			}
		},

		// Options: edge (required), position (required if add), docker (required if delete)
		newDockerCommand: function (options) {
			if (!options.edge) return;

			var commandClass = Clazz.extend({
				construct: function (addEnabled, deleteEnabled, edge, docker, pos, facade) {
					this.addEnabled = addEnabled;
					this.deleteEnabled = deleteEnabled;
					this.edge = edge;
					this.docker = docker;
					this.pos = pos;
					this.facade = facade;
					//this.index = docker.parent.dockers.indexOf(docker);
				},
				execute: function () {
					if (this.addEnabled) {
						if (!this.docker) {
							this.docker = this.edge.addDocker(this.pos);
							this.index = this.edge.dockers.indexOf(this.docker);
						} else {
							this.edge.add(this.docker, this.index);
						}
					}
					else if (this.deleteEnabled) {
						this.index = this.edge.dockers.indexOf(this.docker);
						this.pos = this.docker.bounds.center();
							console.log("execute.removeDocker:" +this.docker);
						this.edge.removeDocker(this.docker);
					}
					this.edge.getLabels().invoke("show");
					this.facade.getCanvas().update();
					this.facade.updateSelection();
				},
				rollback: function () {
					if (this.addEnabled) {
						if (this.docker instanceof ms123.oryx.core.controls.Docker) {
							this.edge.removeDocker(this.docker);
						}
					}
					else if (this.deleteEnabled) {
						this.edge.add(this.docker, this.index);
					}
					this.edge.getLabels().invoke("show");
					this.facade.getCanvas().update();
					this.facade.updateSelection();
				}
			})

console.log("addDocker:"+this.enabledDelete());
			var command = new commandClass(this.enabledAdd(), this.enabledDelete(), options.edge, options.docker, options.position, this.facade);

			this.facade.executeCommands([command]);
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
