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
	@asset(qx/icon/${qx.icontheme}/16/actions/*)
	@asset(qx/icon/${qx.icontheme}/16/places/*)
*/


qx.Class.define("ms123.report.FieldSelector", {
	extend: ms123.util.BaseFieldSelector,
	include: [qx.locale.MTranslation],

	events: {
		"treeClicked": "qx.event.type.Data"
	},


	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (context) {
		this.base(arguments,context);
	},

	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		_saveFields: function () {
			this._table.stopEditing();
			var rc = this._tableModel.getRowCount();
			var fields = [];
			for (var i = 0; i < rc; i++) {
				var rd = this._tableModel.getRowDataAsMap(i);
				fields.push(rd);
			}
			this._context.saveFields(fields);
		}
	}
});
