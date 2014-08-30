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
qx.Class.define("ms123.util.FormTest", {
	extend: qx.ui.container.Composite,

	/**
	 * Constructor
	 */
	construct: function (context) {
		this.base(arguments);
		this.setLayout(new qx.ui.layout.Dock());
		context.window.add(this, {});

		this.context = context;
		var id = this.context.data.id;
		console.log("FormTest:" + id);
		if (id != undefined && id) {
			var url = "data/" + this.context.moduleName + "/" + id;
			var map = ms123.util.Remote.sendSync(url + "?what=asRow");
			var json = map.json;
//			console.log("json:" + json);
			var context = {};
			context.formDesc = json.evalJSON();
			console.log("formDesc:" + context.formDesc.resourceId);
			var form = new ms123.widgets.Form(context);
			this.add(form, { edge: "center" });
		}
	},

	events: {
		"changeValue": "qx.event.type.Data"
	},
	/**
	 * ****************************************************************************
	 * MEMBERS
	 * ****************************************************************************
	 */
	members: {

	}
});
