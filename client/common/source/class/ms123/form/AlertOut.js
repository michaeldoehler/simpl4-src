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
	@asset(qx/icon/${qx.icontheme}/22/actions/*)
	@asset(qx/icon/${qx.icontheme}/16/places/*)
*/
qx.Class.define("ms123.form.AlertOut", {
	extend: qx.ui.container.Composite,
	implement: [ qx.ui.form.IStringForm, qx.ui.form.IForm],
	include: [qx.ui.form.MForm],


	/**
	 *****************************************************************************
	 CONSTRUCTOR
	 *****************************************************************************
	 */

	construct: function (message) {
		this.base(arguments);
		var layout = new qx.ui.layout.Grow();
		this.setLayout(layout);
		var label = new qx.ui.basic.Label( message );
		label.setRich( true )
		this.add(label);
	},

	/**
	 *****************************************************************************
	 PROPERTIES
	 *****************************************************************************
	 */

	properties: {
	},

	events: {
		"changeValue": "qx.event.type.Data"
	},

	/**
	 *****************************************************************************
	 MEMBERS
	 *****************************************************************************
	 */

	members: {
    setValue : function(value) { 
			this.fireDataEvent("changeValue", value, this.__value);
			this.__value = value;
		},
    resetValue : function() {},
    getValue : function() {
			return this.__value;
		}
	}
});
