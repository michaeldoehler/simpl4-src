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
 * The TextField is a single-line text input field.
 */
qx.Class.define("ms123.graphicaleditor.plugins.propertyedit.NumberField", {
	extend: qx.ui.form.AbstractField,


/*
  *****************************************************************************
     PROPERTIES
  *****************************************************************************
  */

	properties: {
		// overridden
		appearance: {
			refine: true,
			init: "textfield"
		},

		// overridden
		allowGrowY: {
			refine: true,
			init: false
		},

		// overridden
		allowShrinkY: {
			refine: true,
			init: false
		}
	},

	members: {
		// interface implementation
		setValue: function (value) {
			var oldValue = this.getValue();
			if ((typeof oldValue) == (typeof value) &&  oldValue == value) {
				return;
			}

			var newValue;
			if( value != undefined && value != null && !isNaN(value)){
				newValue = new String(value);
			}else{
				newValue = new String("");
			}
			this.base(arguments,newValue);
		},
		// interface implementation
		getValue: function () {
			var value = this.base(arguments);
			try{
				var v = parseInt(value);
				if( isNaN(v)) return null;
				return v;
			}catch(e){
				return null;
			}
		},

    _onChangeContent : function(e) {
      var value = e.getData();
			var v;
			try{ v = parseInt(value); }catch(e){
				console.log("NumberField._onChangeContent:"+e);
			}
      this.__nullValue = e.getData() === null;
      this.__fireChangeValueEvent(e.getData());
    },

		// overridden
		_renderContentElement: function (innerHeight, element) {
			if ((qx.core.Environment.get("engine.name") == "mshtml") && (parseInt(qx.core.Environment.get("engine.version"), 10) < 9 || qx.core.Environment.get("browser.documentmode") < 9)) {
				element.setStyles({
					"line-height": innerHeight + 'px'
				});
			}
		}
	}
});
