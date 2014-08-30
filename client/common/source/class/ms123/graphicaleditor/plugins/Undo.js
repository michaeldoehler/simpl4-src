/**
 * Copyright (c) 2008
 * Willi Tscheschner
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


qx.Class.define("ms123.graphicaleditor.plugins.Undo", {
	extend: qx.core.Object,
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (facade) {
		this.base(arguments);
		this.facade = facade;

		this.undoStack = [];
		this.redoStack = [];
		// Offers the functionality of undo                
		this.facade.offer({
			name: ms123.oryx.Translation.Undo.undo,
			description: ms123.oryx.Translation.Undo.undoDesc,
			icon: this.__getResourceUrl("arrow_undo.png"),
			keyCodes: [{
				metaKeys: [ms123.oryx.Config.META_KEY_META_CTRL],
				keyCode: 90,
				keyAction: ms123.oryx.Config.KEY_ACTION_DOWN
			}],
			functionality: this.doUndo.bind(this),
			group: ms123.oryx.Translation.Undo.group,
			isEnabled: qx.lang.Function.bind(function () {
				return this.undoStack.length > 0
			}, this),
			index: 0
		});

		// Offers the functionality of redo
		this.facade.offer({
			name: ms123.oryx.Translation.Undo.redo,
			description: ms123.oryx.Translation.Undo.redoDesc,
			icon: this.__getResourceUrl("arrow_redo.png"),
			keyCodes: [{
				metaKeys: [ms123.oryx.Config.META_KEY_META_CTRL],
				keyCode: 89,
				keyAction: ms123.oryx.Config.KEY_ACTION_DOWN
			}],
			functionality: this.doRedo.bind(this),
			group: ms123.oryx.Translation.Undo.group,
			isEnabled: qx.lang.Function.bind(function () {
				return this.redoStack.length > 0
			}, this),
			index: 1
		});

		// Register on event for executing commands --> store all commands in a stack		 
		this.facade.registerOnEvent(ms123.oryx.Config.EVENT_EXECUTE_COMMANDS, this.handleExecuteCommands.bind(this));
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
		 * Stores all executed commands in a stack
		 * 
		 * param {Object} evt
		 */
		handleExecuteCommands: function (evt) {

			// If the event has commands
			if (!evt.commands) {
				return
			}

			// Add the commands to a undo stack ...
			this.undoStack.push(evt.commands);
			// ...and delete the redo stack
			this.redoStack = [];

			// Update
			//this.facade.getCanvas().update();
			//@@@MS this.facade.updateSelection();

		},

		/**
		 * Does the undo
		 * 
		 */
		doUndo: function () {

			// Get the last commands
			var lastCommands = this.undoStack.pop();

			if (lastCommands) {
				// Add the commands to the redo stack
				this.redoStack.push(lastCommands);

				// Rollback every command
				for (var i = lastCommands.length - 1; i >= 0; --i) {
					lastCommands[i].rollback();
				}

				// Update and refresh the canvas
				//this.facade.getCanvas().update();
				//this.facade.updateSelection();
				this.facade.raiseEvent({
					type: ms123.oryx.Config.EVENT_UNDO_ROLLBACK,
					commands: lastCommands
				});

				// Update
				//this.facade.getCanvas().update();
				//@@@MSthis.facade.updateSelection();
			}
		},

		/**
		 * Does the redo
		 * 
		 */
		doRedo: function () {

			// Get the last commands from the redo stack
			var lastCommands = this.redoStack.pop();

			if (lastCommands) {
				// Add this commands to the undo stack
				this.undoStack.push(lastCommands);

				// Execute those commands
				lastCommands.each(function (command) {
					command.execute();
				});

				// Update and refresh the canvas		
				//this.facade.getCanvas().update();
				//this.facade.updateSelection();
				this.facade.raiseEvent({
					type: ms123.oryx.Config.EVENT_UNDO_EXECUTE,
					commands: lastCommands
				});

				// Update
				//this.facade.getCanvas().update();
				//this.facade.updateSelection();
			}
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
