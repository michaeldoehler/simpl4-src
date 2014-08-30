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
	* @ignore($)
*/
qx.Class.define("ms123.form.GridInputButton", {
	extend: qx.ui.form.MenuButton,
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (icon) {
		this.base(arguments, "", icon);
		this.addListener("mouseover", this.hover.bind(this));
		this.addListener("mouseout", this.reset.bind(this));
		this.addListener("mouseup", this.hover.bind(this));

		this.setDecorator(null);
		this.setPadding(0, 0, 0, 0);
		this.setWidth(16);
		this.setHeight(16);

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
		reset: function () {
			var node = this._getNode();
			//			node.style.border='0px';
			//			node.style.padding='0px';
			node.style.opacity = '1.0';
		},

		hover: function (evt) {
			var node = this._getNode();
			//			node.style.border='1px solid gray';
			//			node.style.borderRadius='2px';
			node.style.opacity = '0.5';
			//			node.style.padding='1px';
		},

		_getNode: function () {
			var icon = this.getChildControl("icon", false);
			var node = icon.getContentElement().getDomElement();
			return node;
		}

	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
