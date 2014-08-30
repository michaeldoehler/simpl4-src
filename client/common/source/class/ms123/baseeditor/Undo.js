/*
 * This file is part of SIMPL4(http://simpl4.org).
 *
 * 	Copyright [2014] [Manfred Sattler] <manfred@ms123.org>
 *
 * SIMPL4 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SIMPL4 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SIMPL4.  If not, see <http://www.gnu.org/licenses/>.
 */
/**
	* @ignore(Hash)
*/

qx.Class.define("ms123.baseeditor.Undo", {
	extend: qx.core.Object,
 include : [ qx.locale.MTranslation],
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (facade) {
		this.base(arguments);
		this.facade = facade;


		this.undoStack  = new Array();
		this.redoStack  = new Array();

		// Offers the functionality of undo                
		var undo_msg = this.tr("baseeditor.edit_undo");
		var redo_msg = this.tr("baseeditor.edit_redo");
		var group = "1";
		this.facade.offer({
			name: undo_msg,
			description: undo_msg,
			icon: "icon/16/actions/edit-undo.png",
			keyCodes: [{
				metaKeys: [ms123.baseeditor.Config.META_KEY_META_CTRL],
				keyCode: 90,
				keyAction: ms123.baseeditor.Config.KEY_ACTION_DOWN
			}],
			functionality: this.doUndo.bind(this),
			group: group,
			isEnabled: qx.lang.Function.bind(function () {
				return this.undoStack.length > 0
			}, this),
			index: 0
		});

		// Offers the functionality of redo
		this.facade.offer({
			name: redo_msg,
			description: redo_msg,
			icon: "icon/16/actions/edit-redo.png",
			keyCodes: [{
				metaKeys: [ms123.baseeditor.Config.META_KEY_META_CTRL],
				keyCode: 89,
				keyAction: ms123.baseeditor.Config.KEY_ACTION_DOWN
			}],
			functionality: this.doRedo.bind(this),
			group: group,
			isEnabled: qx.lang.Function.bind(function () {
				return this.redoStack.length > 0
			}, this),
			index: 1
		});

		// Register on event for executing commands --> store all commands in a stack		 
		this.facade.registerOnEvent(ms123.baseeditor.Config.EVENT_EXECUTE_COMMANDS, this.handleExecuteCommands.bind(this));
		this.facade.registerOnEvent(ms123.baseeditor.Config.EVENT_RESET_UNDO, this.reset.bind(this));
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
		reset:function(){
			this.undoStack  = new Array();
			this.redoStack  = new Array();
			this.facade.update()
		},
		/**
		 * Stores all executed commands in a stack
		 * 
		 * param {Object} evt
		 */
		handleExecuteCommands: function (evt) {
			if (!evt.commands) {
				return
			}

			this.undoStack.push(evt.commands);
			this.redoStack = [];

			//this.facade.updateSelection();
			this.facade.update();

		},

		doUndo: function () {
			var lastCommands = this.undoStack.pop();
			if (lastCommands) {
				this.redoStack.push(lastCommands);

				for (var i = lastCommands.length - 1; i >= 0; --i) {
					lastCommands[i].rollback();
				}

				this.facade.raiseEvent({
					type: ms123.baseeditor.Config.EVENT_UNDO_ROLLBACK,
					commands: lastCommands
				});

				this.facade.update()
			}
		},

		doRedo: function () {

			var lastCommands = this.redoStack.pop();

			if (lastCommands) {
				this.undoStack.push(lastCommands);

				lastCommands.each(function (command) {
					command.execute();
				});

				// Update and refresh the canvas		
				this.facade.raiseEvent({
					type: ms123.baseeditor.Config.EVENT_UNDO_EXECUTE,
					commands: lastCommands
				});

				// Update
				this.facade.update()
			}
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
