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
/*
*/
qx.Class.define("ms123.settings.views.SearchPropertyEdit", {
	extend: ms123.settings.PropertyEdit,

	/**
	 *****************************************************************************
	 CONSTRUCTOR
	 *****************************************************************************
	 */

	construct: function (facade) {
		this.base(arguments, facade);
	},

	/**
	 *****************************************************************************
	 PROPERTIES
	 *****************************************************************************
	 */
	/**
	 *****************************************************************************
	 MEMBERS
	 *****************************************************************************
	 */

	members: {
		_createEditForm: function () {
			var formData = {
				"orderby": {
					'type': "TextField",
					'label': this.tr("settings.views.propertyedit.orderby"),
					'validation': {
						required: false,
						validator: "/^[A-Za-z]([0-9A-Za-z_.:;,]){0,60}$/"
					},
					'value': ""
				}
			}
			this._form = new ms123.form.Form({
				"tabs": [{
					id: "tab1",
					layout: "single",
					lineheight: 20
				}],
				"formData": formData,
				"allowCancel": true,
				"inWindow": false,
				"buttons": [],
				"callback": function (m, v) {},
				"context": null
			});
			return this._form;
		}
	}
});
