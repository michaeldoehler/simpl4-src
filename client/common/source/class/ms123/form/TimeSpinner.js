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
/** **********************************************************************

   Copyright : 2008 CELCAT, http://www.celcat.com/
   Authors   : Nathan Hadley, Matthew Gregory

************************************************************************ */


/**
 A variant of the spinner class to allow for the entry of a time value with
 24 hour clock values
 
 each time value is represented by a numerical value e.g 
 0  = 00:00
 1  = 00:01
 60 = 01:00
 */

qx.Class.define("ms123.form.TimeSpinner", {
	extend: qx.ui.core.Widget,
  include : [
		qx.ui.core.MContentPadding,
    qx.ui.core.MRemoteChildrenHandling,
    qx.ui.form.MForm
  ],
  implement : [
    qx.ui.form.IForm,
    qx.ui.form.IDateForm
  ],

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

		// MAIN LAYOUT
		var layout = new qx.ui.layout.Grid();
		layout.setColumnFlex(0, 1);
		layout.setRowFlex(0, 1);
		layout.setRowFlex(1, 1);
		this._setLayout(layout);

		// EVENTS
		this.addListener("keydown", this._onKeyDown, this);
		this.addListener("keyup", this._onKeyUp, this);
		this.addListener("mousewheel", this._onMouseWheel, this);

		// CREATE CONTROLS
		this._createChildControl("textfield");
		this._createChildControl("upbutton");
		this._createChildControl("downbutton");

		if (!value) {
			this.setValue(0);
		}
		else if (typeof value === "string") {
			this.setValue(ms123.form.TimeSpinner.intTimeFromString(value));
		}
		else {
			this.setValue(value);
		}

		this._applyValue();
	},



	/**
	 *****************************************************************************
	 STATICS
	 *****************************************************************************
	 */

	statics: {
		intTimeFromString: function (time) {
			//check text entered for validity   
			var re = /^\s*([01]\d|2[0123]|\d)\s*[\:\.\-]?\s*([0-5]\d)?\s*(\S*)\s*$/;

			var validate = re.exec(time);

			if (validate == null || validate.length < 3) {
				validate = false;
			}
			else {
				var hour = validate[1];
				var mins = validate[2] | "0";

				//has the user entered am or pm?
				if (validate[3].length) {
					if (validate[3].toLowerCase() == "pm") {
						hour = (hour % 12) + 12;
					}
					else if (validate[3].toLowerCase() !== "am") {
						validate = false;
					}
				}
			}

			// if valid get value of time as an integer and return
			if (validate) {
				//forces the values back to integers
				var nf = new qx.util.format.NumberFormat();
				hour = nf.parse(hour) * 60;
				mins = nf.parse(mins);
				return (hour + mins);
			}
			else return -1;
		}
	},



	/**
	 *****************************************************************************
	 PROPERTIES
	 *****************************************************************************
	 */

	properties: {
		// overridden
		appearance: {
			refine: true,
			init: "spinner"
		},

		// overridden
		focusable: {
			refine: true,
			init: true
		},

		/** The amount to increment on each event (keypress or mousedown) */
		singleStep: {
			check: "Number",
			init: 1
		},

		/** The amount to increment on each pageup/pagedown keypress */
		pageStep: {
			check: "Number",
			init: 60
		},

		/** minimal value of the Range object */
		min: {
			check: "Number",
			apply: "_applyMin",
			init: 0
		},

		/** The name of the widget. Mainly used for serialization proposes. */
		name: {
			check: "String",
			nullable: true,
			event: "changeName"
		},

		/** The value of the spinner. */
		value: {
			check: "typeof value==='number'&&value>=this.getMin()&&value<=this.getMax()",
			apply: "_applyValue",
			init: 0,
			event: "changeValue"
		},

		/** maximal value of the Range object */
		max: {
			check: "number",
			apply: "_applyMax",
			init: 1439
		},

		/** whether the value should wrap around */
		wrap: {
			check: "Boolean",
			init: false,
			apply: "_applyWrap"
		},

		/** Controls whether the textfield of the spinner is editable or not */
		editable: {
			check: "Boolean",
			init: true,
			apply: "_applyEditable"
		},

		// overridden
		allowShrinkY: {
			refine: true,
			init: false
		}
	},



	/**
	 *****************************************************************************
	 MEMBERS
	 *****************************************************************************
	 */

	members: {
		__lastValidValue: null,
		__pageUpMode: null,
		__pageDownMode: null,


		/**
		 ---------------------------------------------------------------------------
		 WIDGET INTERNALS
		 ---------------------------------------------------------------------------
		 */

		// overridden
		_createChildControlImpl: function (id) {
			var control;

			switch (id) {
			case "textfield":
				control = new qx.ui.form.TextField();
				control.addState("inner");
				control.setWidth(40);
				control.setFocusable(false);
				control.addListener("changeValue", this._onTextChange, this);

				this._add(control, {
					column: 0,
					row: 0,
					rowSpan: 2
				});
				break;

			case "upbutton":
				control = new qx.ui.form.RepeatButton();
				control.addState("inner");
				control.setFocusable(false);
				control.addListener("execute", this.__countUp, this);
				this._add(control, {
					column: 1,
					row: 0
				});
				break;

			case "downbutton":
				control = new qx.ui.form.RepeatButton();
				control.addState("inner");
				control.setFocusable(false);
				control.addListener("execute", this.__countDown, this);
				this._add(control, {
					column: 1,
					row: 1
				});
				break;
			}

			return control || this.base(arguments, id);
		},


		// overridden
		_forwardStates: {
			focused: true
		},


		// overridden
		tabFocus: function () {
			this.ignoreBase;
			var field = this.getChildControl("textfield");

			field.getFocusElement().focus();
			field.selectAll();
		},


		/**
		 
		 */
		_applyValue: function () {
			var upButton = this.getChildControl("upbutton");
			var downButton = this.getChildControl("downbutton");
			var textField = this.getChildControl("textfield");

			// up button enabled/disabled
			if (this.getValue() < this.getMax()) {
				// only enable the button if the spinner itself is enabled
				if (this.getEnabled()) {
					upButton.resetEnabled();
				}
			} else {
				// only disable the buttons if wrapping is disabled
				if (!this.getWrap()) {
					upButton.setEnabled(false);
				}
			}

			// down button enabled/disabled
			if (this.getValue() > this.getMin()) {
				// only enable the button if the spinner itself is enabled
				if (this.getEnabled()) {
					downButton.resetEnabled();
				}
			} else {
				// only disable the buttons if wrapping is disabled
				if (!this.getWrap()) {
					downButton.setEnabled(false);
				}
			}
			var timetext = this._getTimeString();
			if (this.getChildControl("textfield").getValue() != timetext) this.getChildControl("textfield").setValue(timetext);
//      this.fireDataEvent("changeValue", this.getValue(),"XXX");
		},


		/**
		 * Apply routine for the editable property.<br/>
		 * It sets the textfield of the spinner to not read only.
		 *
		 * param value {Boolean} The new value of the editable property
		 */
		_applyEditable: function (value) {
			var textField = this.getChildControl("textfield");

			if (textField) {
				textField.setReadOnly(!value);
			}
		},


		/**
		 * Apply routine for the wrap property.<br/>
		 * Enables all buttons if the wrapping is enabled.
		 *
		 * param value {Boolean} The new value of the wrap property
		 * param old {Boolean} The former value of the wrap property
		 */
		_applyWrap: function (value, old) {
			if (value) {
				var upButton = this.getChildControl("upbutton");
				var downButton = this.getChildControl("downbutton");

				if (this.getEnabled()) {
					upButton.setEnabled(true);
					downButton.setEnabled(true);
				}
			}
		},


		/**
		 ---------------------------------------------------------------------------
		 KEY EVENT-HANDLING
		 ---------------------------------------------------------------------------
		 */

		/**
		 * Callback for "keyDown" event.<br/>
		 * Controls the interval mode ("single" or "page")
		 * and the interval increase by detecting "Up"/"Down"
		 * and "PageUp"/"PageDown" keys.<br/>
		 * The corresponding button will be pressed.
		 *
		 * param e {qx.event.type.KeyEvent} keyDown event
		 */
		_onKeyDown: function (e) {
			switch (e.getKeyIdentifier()) {
			case "PageUp":
				// mark that the spinner is in page mode and process further
				this.__pageUpMode = true;

			case "Up":
				this.getChildControl("upbutton").press();
				break;

			case "PageDown":
				// mark that the spinner is in page mode and process further
				this.__pageDownMode = true;

			case "Down":
				this.getChildControl("downbutton").press();
				break;

			default:
				// Do not stop unused events
				return;
			}

			e.stopPropagation();
			e.preventDefault();
		},


		/**
		 * Callback for "keyUp" event.<br/>
		 * Detecting "Up"/"Down" and "PageUp"/"PageDown" keys.<br/>
		 * Releases the button and disabled the page mode, if necessary.
		 *
		 * param e {qx.event.type.KeyEvent} keyUp event
		 * @return {void}
		 */
		_onKeyUp: function (e) {
			switch (e.getKeyIdentifier()) {
			case "PageUp":
				this.getChildControl("upbutton").release();
				this.__pageUpMode = false;
				break;

			case "Up":
				this.getChildControl("upbutton").release();
				break;

			case "PageDown":
				this.getChildControl("downbutton").release();
				this.__pageDownMode = false;
				break;

			case "Down":
				this.getChildControl("downbutton").release();
				break;
			}
		},


/*
    ---------------------------------------------------------------------------
      OTHER EVENT HANDLERS
    ---------------------------------------------------------------------------
    */

		/**
		 * Callback method for the "mouseWheel" event.<br/>
		 * Increments or decrements the value of the spinner.
		 *
		 * param e {qx.event.type.MouseEvent} mouseWheel event
		 */
		_onMouseWheel: function (e) {
			if (e.getWheelDelta() > 0) {
				this.__countDown();
			}
			else {
				this.__countUp();
			}

			e.stopPropagation();
		},


		_getTimeString: function () //TO DO : CREATE A TIME STRING THAT CONFORMS TO LOCALE
		{
			//this._onTextChange(); // force update
			var time = "";
			var cpyValue = this.getValue();
			var hour = cpyValue / 60;

			//set hour from number value
			hour = Math.floor(hour);

			//add 0 if hour is less than 10 first
			if (hour < 10) {
				time = "0";
			}

			//add hours to time string
			time = time + hour.toString();

			//add seperator to time string
			time = time + ":";

			//subtract hours from time
			cpyValue = cpyValue - (60 * hour);

			//attach mintues to time string
			if (cpyValue < 10) {
				time = time + "0" + cpyValue.toString();
			}
			else {
				time = time + cpyValue.toString();
			}

			return time;
		},

		_onTextChange: function (e) {
			var time = ms123.form.TimeSpinner.intTimeFromString(this.getChildControl("textfield").getValue());
			if (time > -1) {
				this.setValue(time);
			}

			this._applyValue();
		},


		__countUp: function () {
			var max = this.getMax();
			var inc = this.getSingleStep();

			if (this.__pageUpMode) {
				inc = this.getPageStep();
			}

			if ((this.getValue() + inc) <= max) {
				this.setValue(this.getValue() + inc);
				this._applyValue();
			}
		},

		__countDown: function () {
			var min = this.getMin();
			var inc = this.getSingleStep();

			if (this.__pageDownMode) {
				inc = this.getPageStep();
			}

			if ((this.getValue() - inc) >= min) {
				this.setValue(this.getValue() - inc);
				this._applyValue();
			}
		}
	}
});
