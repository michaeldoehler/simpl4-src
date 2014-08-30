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
qx.Class.define("ms123.form.TextArea", {
	extend: ms123.form.AbstractField,


	/**
	 *****************************************************************************
	 CONSTRUCTOR
	 *****************************************************************************
	 */
	construct: function (useitCheckboxes) {
		this.base(arguments,useitCheckboxes);
	},

	properties: {
 // overridden
    focusable : {
      refine : true,
      init : true
    }
	},

	/**
	 *****************************************************************************
	 MEMBERS
	 *****************************************************************************
	 */

	members: {
		/**
		 ---------------------------------------------------------------------------
		 WIDGET API
		 ---------------------------------------------------------------------------
		 */

		// overridden
		_createChildControlImpl: function (id, hash) {
			var control;

			switch (id) {
			case "textfield":
				control = new qx.ui.form.TextArea();
				control.setFocusable(false);
				control.addState("inner");
				control.addListener("changeValue", this._onTextFieldChangeValue, this);
				this._add(control, { flex:1 });
				break;
			}
			return control || this.base(arguments, id);
		},
		setAutoSize:function( h ){
			this.getChildControl("textfield").setAutoSize(h);
		},

		_setHeight:function( h ){
			this.getChildControl("textfield").setHeight(h);
			this.setHeight(h);
		}
	}
});
