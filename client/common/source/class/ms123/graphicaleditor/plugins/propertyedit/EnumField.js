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

qx.Class.define("ms123.graphicaleditor.plugins.propertyedit.EnumField", {
	extend: ms123.graphicaleditor.plugins.propertyedit.ComplexListField,
	include : [ms123.graphicaleditor.plugins.propertyedit.MEnum],
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (config, title, items, key, facade) {
		this.base(arguments, config, title, items, key, facade);
	},
	members: {
		setValue: function (value) {
			this.base(arguments, value);
			var data = value;
			if (value != undefined && value && value != "") {;
				try{
					value = qx.lang.Json.parse(value);
					console.log("EnumField.setValue:" + value.enumDescription);
					if (this.enumDisplay) this.enumDisplay.setValue(value.enumDescription);
					this.enumDescription = value.enumDescription;
				}catch(e){
					console.error("EnumField.setValue:"+value+" wrong value");
				}
			}
		},
		createToolbar: function () {
			var toolbar = this.base(arguments, ["del"]);
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
