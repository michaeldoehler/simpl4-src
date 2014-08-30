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
qx.Class.define('ms123.ruleseditor.PaneScroller', {

	extend: qx.ui.table.pane.Scroller,

	construct: function (table) {
		this.base(arguments, table);
	},

	members: {
		_onClickPane: function (e) {
			var model = this.getTable().getTableModel();
			//model.setMouseSelectingRange(false);
			this.base(arguments, e);
			if( !this.getTable().isEditing() ){
				console.log("SetFocusetoTable");
				this.getTable().focus();
			}
		},

		stopEditing: function () {
		 	qx.event.Registration.removeAllListeners( this.getChildControl('focus-indicator'));
			this.flushEditor();
			this.cancelEditing();
		}
	}
});
