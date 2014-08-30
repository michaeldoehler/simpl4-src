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

qx.Class.define("ms123.graphicaleditor.plugins.propertyedit.EnumWindow", {
	extend: ms123.graphicaleditor.plugins.propertyedit.ComplexListWindow,
	include : [ms123.graphicaleditor.plugins.propertyedit.MEnum],
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (config, title, items, data, facade,data) {
		this.base(arguments, config, title, items, null, facade,data);
	},
	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		createToolbar: function () {
			var toolbar = this.base(arguments, ["del","add"]);
			var buttonDel = new qx.ui.toolbar.Button("", "icon/16/places/user-trash.png");
			buttonDel.addListener("execute", function () {
				this.enumDisplay.setValue(null);
				this.setValue(null);
				this.setTableData([]);
			}, this);
			toolbar._add(buttonDel);

			this.enumDisplay = this.createSelectedEnumDisplay();
			if (this.enumDescription) this.enumDisplay.setValue(this.enumDescription);
			var container = new qx.ui.container.Composite();
			container.setLayout(new qx.ui.layout.Dock());
			container.add(toolbar, {
				edge: "north"
			});
			container.add(this.enumDisplay, {
				edge: "center"
			});
			return container;
		}
	}

});
