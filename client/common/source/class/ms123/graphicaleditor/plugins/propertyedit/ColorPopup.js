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
qx.Class.define("ms123.graphicaleditor.plugins.propertyedit.ColorPopup", {
	extend: qx.ui.container.Composite,
	implement: [qx.ui.form.IStringForm],


	/**
	 *****************************************************************************
	 CONSTRUCTOR
	 *****************************************************************************
	 */

	construct: function () {
		this.base(arguments);
		this.setLayout(new qx.ui.layout.HBox());
		this._colorPopup = new qx.ui.control.ColorPopup();
		this._colorPopup.exclude();

		this._colorPopup.addListener("changeValue", function (e) {
 			var value = e.getData();
			if( value == null){
				this._button.setLabel( "------");
				this._button.setTextColor( null);
				this._button.setBackgroundColor( null);
			}else{
				this._button.setLabel( value);
				this._button.setTextColor( value);
				this._button.setBackgroundColor( value);
			}
      this.fireDataEvent("changeValue", value, e.getOldData());
		},this);

		this._button = new qx.ui.form.Button("Choose Color");
		this.add(this._button);

		this._button.addListener("mousedown", function (e) {
			this._colorPopup.placeToPointer(e)
			this._colorPopup.show();
		}, this);

		this.setFocusable(true);
		this._button.setFocusable(false);
		this._colorPopup.setFocusable(false);
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
		setValue: function (value) {
			try{
				if( value != null && value!="none"){
					this._button.setLabel( value);
					this._colorPopup.setValue(value);
					this._button.setTextColor( value);
					this._button.setBackgroundColor( value);
				}else{
					this._button.setLabel( "------");
					this._button.setTextColor( null);
					this._button.setBackgroundColor( null);
				}
			}catch(e){
				console.error("ColorPopup.setValue:"+e);
			}
		},

		getValue: function () {
			return this._colorPopup.getValue();
		},

		resetValue: function () {
			this._colorPopup.resetValue();
			this._button.setTextColor( this._colorPopup.getValue());
		}
	}
});
