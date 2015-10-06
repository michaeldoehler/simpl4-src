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
qx.Class.define("ms123.graphicaleditor.plugins.propertyedit.DefinitionSelect", {
	extend: qx.core.Object,

	/**
	 *****************************************************************************
	 CONSTRUCTOR
	 *****************************************************************************
	 */

	construct: function (facade, formElement, config) {
		this.base(arguments);
		this._facade = facade;
		this._init(formElement, config);
	},
	/**
	 *****************************************************************************
	 MEMBERS
	 *****************************************************************************
	 */

	members: {
		_init: function (formElement, config) {
			var json = this._facade.getJSON();

			var item = new qx.ui.form.ListItem("-", null, null);
			formElement.add(item);

			var defs = json.properties[config.definitions];
			console.log("defs:",defs);
			for (var i = 0; i < defs.length; i++) {
				var d = defs[i];
				var item = new qx.ui.form.ListItem(d.name, null, d.id);
				formElement.add(item);
			}
		}
	}
});
