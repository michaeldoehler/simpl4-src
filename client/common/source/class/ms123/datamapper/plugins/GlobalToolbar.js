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
	* @ignore($A)
*/
qx.Class.define("ms123.datamapper.plugins.GlobalToolbar", {
	extend: ms123.baseeditor.Toolbar,
	include: [qx.locale.MTranslation],

	/**
	 * Constructor
	 */
	construct: function (facade) {
		this.base(arguments,facade);
		this.facade.registerOnEvent(ms123.baseeditor.Config.EVENT_EXECUTE_COMMANDS, this.onUpdate.bind(this));
		this.facade.registerOnEvent(ms123.baseeditor.Config.EVENT_UNDO_ROLLBACK, this.onUpdate.bind(this));
		this.facade.registerOnEvent(ms123.baseeditor.Config.EVENT_UNDO_EXECUTE, this.onUpdate.bind(this));
		this.facade.registerOnEvent(ms123.baseeditor.Config.EVENT_RESET_UNDO, this.onUpdate.bind(this));
	},

	/**
	 * ****************************************************************************
	 * MEMBERS
	 * ****************************************************************************
	 */
	members: {

	}
});
