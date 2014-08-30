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
       * @lint ignoreDeprecated(alert,eval)
*/
qx.Class.define("ms123.form.DateTimeField", {
	extend: qx.ui.core.Widget,
	include: [
	qx.ui.core.MContentPadding, qx.ui.core.MRemoteChildrenHandling, qx.ui.form.MForm],
	implement: [
	qx.ui.form.IForm, qx.ui.form.IDateForm],

	/**
	 *****************************************************************************
	 CONSTRUCTOR
	 *****************************************************************************
	 */

	/**
	 *  value is an integer representing a time, or a time string in 24 hour format
	 */
	construct: function (value) {
		this.base(arguments);

		var layout = new qx.ui.layout.HBox(2);
		this._setLayout(layout);

		this.__datefield = this.__createChildControl("date");
		this.__timefield = this.__createChildControl("time");
		this.setValue(value);
	},



	/**
	 *****************************************************************************
	 STATICS
	 *****************************************************************************
	 */

	statics: {},

	/**
	 *****************************************************************************
	 EVENTS
	 *****************************************************************************
	 */
	events: {
		"changeValue": "qx.event.type.Data"
	},



	/**
	 *****************************************************************************
	 PROPERTIES
	 *****************************************************************************
	 */

	properties: {
		// overridden
		focusable: {
			refine: true,
			init: true
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
		 WIDGET INTERNALS
		 ---------------------------------------------------------------------------
		 */

		// overridden
		__createChildControl: function (id) {
			var control;

			switch (id) {
			case "date":
				control = new qx.ui.form.DateField();
				control.addState("inner");
				control.setFocusable(false);
				control.addListener("changeValue", this._onDateChange, this);

				this._add(control, {
					flex: 1
				});
				break;

			case "time":
				//control = new ms123.form.TimeSpinner();
				control = new ms123.form.TimeChooser();
				control.setTimeFormat("24");
				control.setLayoutFormat("right/horizontal");

				control.setFocusable(false);
				control.addListener("changeValue", this._onTimeChange, this);

				this._add(control, {
					flex: 0
				});
				break;
			}
			return control || this.base(arguments, id);
		},


		// overridden
		_forwardStates: {
			focused: true
		},
		resetValue: function () {
			alert("resetValue");
		},


		// overridden
		tabFocus: function () {},
		setDateFormat: function (value) {
			this.__datefield.setDateFormat(value);
		},
		setTimeFormat: function (value) {
			this.__timefield.setTimeFormat(value);
		},

		getValue: function () {
			if (this.__datefield.getValue() == undefined || this.__timefield.getValue() == undefined) return null;
			var ret = new Date();
			var time = this.__timefield.getValue();
			ret.setTime(this.__datefield.getValue().getTime() + (1000 * this.__timefield.getValue()));
			return ret;
		},

		setValue: function (value) {
			if (value === undefined || value == null) {
				value = new Date();
			}
			if (typeof(value) == "number") {
				var val = value;
				value = new Date();
				value.setTime(val);
			}
			if (value instanceof Date) {
				var date = new Date(value.getTime());
				date.setHours(0);
				date.setMinutes(0);
				date.setSeconds(0);
				var datevalue = date;
				var minutes = value.getMinutes();
				var hour = value.getHours();
				var seconds = value.getSeconds();
				var timevalue = hour * 3600 + minutes * 60 + seconds;
				this.__timefield.setValue(timevalue);
				this.__datefield.setValue(datevalue);
			}
		},

		_onDateChange: function (e) {
			var newValue = this.getValue();
			this.fireDataEvent("changeValue", newValue, e.oldValue);
		},
		_onTimeChange: function (e) {
			var newValue = this.getValue();
			this.fireDataEvent("changeValue", newValue, e.oldValue);
		}
	}
});
