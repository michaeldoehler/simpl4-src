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
qx.Class.define("ms123.form.DecimalField", {
	extend: ms123.form.AbstractField,

	/**
	 *****************************************************************************
	 CONSTRUCTOR
	 *****************************************************************************
	 */

	construct: function (useitCheckboxes) {
		this.base(arguments,useitCheckboxes);
		this.setFocusable(true);
	},

	/**
	 *****************************************************************************
	 PROPERTIES
	 *****************************************************************************
	 */

	properties: {
		placeholder: {
			check: "String",
			nullable: true,
			apply: "_applyPlaceholder"
		}
	},

	/**
	 *****************************************************************************
	 MEMBERS
	 *****************************************************************************
	 */

	members: {
		// property apply
		_applyPlaceholder: function (value, old) {
			this.getChildControl("textfield").setPlaceholder(value);
		},

		setReadOnly:function(flag){
			this.getChildControl("textfield").setReadOnly(flag);
			this.getChildControl("textfield").setBackgroundColor("#cfcfcf");
		},
		setFilter:function(flag){
			this.getChildControl("textfield").setFilter(flag);
		},
		setValid:function(flag){
			this.getChildControl("textfield").setValid(flag);
		},
		setInvalidMessage:function(msg){
			this.getChildControl("textfield").setInvalidMessage(msg);
		},
		// interface implementation
		setValue: function (value) {
			var textfield = this.getChildControl("textfield");
			if ((typeof textfield.getValue()) == (typeof value) &&  textfield.getValue() == value) {
				return;
			}

			// Apply to text field
			if( value != undefined && value != null && !isNaN(value) ){
				textfield.setValue(new String(value));
			}else{
				textfield.setValue(new String(""));
			}
		},
		// interface implementation
		getValue: function () {
			var value = this.getChildControl("textfield").getValue();
			if( value != undefined && value != null ) value = value.replace(",",".");
			try{
				var v = parseFloat(value);
				if( isNaN(v)) return null;
				return v;
			}catch(e){
				return null;
			}
		},
    _onTextFieldChangeValue : function(e) {
      var value = e.getData();
			var v;
			try{ v = parseFloat(value); }catch(e){
				console.log("DecimalField._onTextFieldChangeValue:"+e);
			}
      this.fireDataEvent("changeValue", v, e.getOldData());
    },

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
				control = new qx.ui.form.TextField();
				control.setLiveUpdate(true);
				control.setFocusable(false);
				control.addState("inner");
				control.addListener("changeValue", this._onTextFieldChangeValue, this);
				this._add(control, { flex:1 });
				break;
			}
			return control || this.base(arguments, id);
		},

    _onTextFieldChangeValue : function(e) {
      var value = e.getData();
      // Fire event
			if( value != undefined && value != null ) value = value.replace(",",".");
      this.fireDataEvent("changeValue", value, e.getOldData());
    }
	}
});
