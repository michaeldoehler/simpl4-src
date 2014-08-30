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
	@asset(qx/icon/${qx.icontheme}/16/actions/*)
	@asset(qx/icon/${qx.icontheme}/16/apps/*)
	@asset(qx/icon/${qx.icontheme}/48/actions/*)
	@asset(qx/icon/${qx.icontheme}/48/apps/*)
	@ignore($)
*/
qx.Class.define("ms123.form.ActionButton", {
	extend: qx.ui.core.Widget,
	implement: [
	qx.ui.form.IStringForm, qx.ui.form.IForm],
	include: [qx.ui.form.MForm, qx.locale.MTranslation ],


	/**
	 *****************************************************************************
	 CONSTRUCTOR
	 *****************************************************************************
	 */

	construct: function (useitCheckboxes) {
		this.base(arguments);
		var layout = new qx.ui.layout.HBox();
		this._setLayout(layout);

		var button = this._createChildControl("button");
		if (useitCheckboxes) {
			this._createChildControl("checkbox");
		}
	},

	/**
	 *****************************************************************************
	 PROPERTIES
	 *****************************************************************************
	 */
	properties: {},

	/**
	 *****************************************************************************
	 EVENTS
	 *****************************************************************************
	 */

	events: {
		/** Whenever the value is changed this event is fired
		 *
		 *  Event data: The new text value of the field.
		 */
		"changeValue": "qx.event.type.Data"
	},


	/**
	 *****************************************************************************
	 MEMBERS
	 *****************************************************************************
	 */

	members: {
		// overridden
		_createChildControlImpl: function (id, hash) {
			var control;
			switch (id) {
			case "checkbox":
				control = new qx.ui.form.CheckBox();
				control.setFocusable(false);
				control.setKeepActive(true);
				control.setEnabled(true);
				control.addState("inner");
				control.set({
					height: 10,
					maxHeight: 10,
					minWidth: 12,
					maxWidth: 12,
					decorator: "checkbox"
				});
				control.setToolTipText(this.tr("usebox.use"));
				this._add(control);
				break;
			case "button":
				var control = new qx.ui.form.Button(null, "icon/22/actions/dialog-ok.png").set({
				});
				control.addListener("execute", function () {
					var key = this.getUserData("key");
					var action = this.__action || "buttonpressed";
					this.__callback.call(this, {action:this.__action, key:key, form:this.__form,button:this.getChildControl("button")});
				}, this);
				this._add(control,{flex:1});
				break;
			}
			return control;
		},

		// overridden
		_forwardStates: {
			focused: true
		},
		// interface implementation
		setValue: function (value) { 
			if( this.__action == null){
				var key = this.getUserData("key");
				console.log("ActionButton.setValue:"+key+"="+value);
				this.__callback.call(this, {action:"setvalue", value:value,key:key, form:this.__form,button:this.getChildControl("button")});
			}
		},
		setParams: function (label, iconname, action, form) {
			if( label){
				try{
		  		this.getChildControl("button").setLabel(this.tr(label));
				}catch(e){
					console.log("label:"+e);
				}
			}
			if( iconname && iconname != "" ){
				if( iconname.match(/^icon/)){
		  		this.getChildControl("button").setIcon(iconname);
				}else{
		  		this.getChildControl("button").setIcon(this.__getResourceUrl(iconname));
				}
			}else{
				if( action == "execute"){
		  		this.getChildControl("button").setIcon("icon/22/actions/dialog-ok.png");
				}
				if( action == "cancel"){
		  		this.getChildControl("button").setIcon("icon/22/actions/dialog-close.png");
				}
			}
			this.__action = action;
			this.__form = form;
		},
		getValue: function () {
			return null;
		},
		resetValue: function () {},
		setCallback: function (callback) {
			this.__callback = callback;
		},
		getCheckBox: function () {
			return this.getChildControl("checkbox");
		},
		__getResourceUrl: function (name) {
			var am = qx.util.AliasManager.getInstance();
			return am.resolve("resource/ms123/" + name);
		}
	}
});
