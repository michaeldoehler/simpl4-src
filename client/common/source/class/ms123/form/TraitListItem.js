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
	@ignore(jQuery)
	@ignore($)
	@lint ignoreDeprecated(alert,eval) 
	@asset(qx/icon/${qx.icontheme}/16/apps/*)
	@asset(ms123/icons/*)
	@asset(ms123/*)
*/


qx.Class.define("ms123.form.TraitListItem", {
	extend: qx.ui.basic.Atom,
	implement: [qx.ui.form.IModel],
	//include: [qx.ui.form.MModelProperty],

/*
  *****************************************************************************
     CONSTRUCTOR
  *****************************************************************************
  */

	/**
	 * param label {String} Label to use
	 * param icon {String?null} Icon to use
	 * param model {String?null} The items value
	 */
	construct: function (context, label, icon, model) {
		this._context = context;
		this.base(arguments, label, icon);

		var layout = new qx.ui.layout.Dock();
		//		layout.setColumnWidth(1, 300);
		//		layout.setColumnWidth(2, 20);
		//		layout.setColumnMinWidth(2, 20);
		this._setLayout(layout);

		this.set({
			padding: 0,
			margin: 2
		})

		if (model != null) {
			this.setModel(model);
		}
		this.getChildControl("button", false);
		//this._button.exclude();
		this._button.show();

		this._value = {};

		this.addListener("mouseover", this._onMouseOver, this);
		this.addListener("mouseout", this._onMouseOut, this);
	},


	/**
	 *****************************************************************************
	 EVENTS
	 *****************************************************************************
	 */
	events: { /** Fires after the selection was modified */
		"change": "qx.event.type.Data",
		"changeModel": "qx.event.type.Data"
	},


	/**
	 *****************************************************************************
	 PROPERTIES
	 *****************************************************************************
	 */
	properties: {
		appearance: {
			refine: true,
			init: "listitem"
		}
	},
	members: {
		getModel: function () {
			return this.__model;
		},
		setModel: function (value) {
			this.__model = value;
		},
		getValue: function () {
			this._value["teamid"] = this.getModel().getValue();
			return this._value;
		},
		setValue: function (value) {
			this._value = value;
			this._button.show();
		},
		setTooltip: function (value) {
			this.getChildControl("label", false).setToolTipText(value);
		},
		// overridden
		_createChildControlImpl: function (id, hash) {
			var control;
			switch (id) {
			case "label":
				control = new qx.ui.basic.Label(this.getLabel());
				control.setAnonymous(true);
				control.setRich(false);
				control.setPaddingLeft(3);
				this._add(control, {
					edge: "center"
				});
				if (this.getLabel() == null || this.getShow() === "icon") {
					control.exclude();
				}
				break;

			case "button":
				var control = new qx.ui.form.Button(null, "icon/16/apps/utilities-text-editor.png").set({
					padding: 0,
					margin: 0
				});
				this._button = control;

				this._button.addListener("execute", function () {
					this._createTraitEditWindow();
				}, this);

				//control.exclude();	
				control.show();
				this._add(control, {
					edge: "east"
				});
				break;

			case "icon":
				control = new qx.ui.basic.Image(this.getIcon());
				control.setAnonymous(true);
				this._add(control, {
					edge: "west"
				});
				if (this.getIcon() == null || this.getShow() === "label") {
					control.exclude();
				}
				break;
			}

			return control || this.base(arguments, id);
		},
		_createTraitEditWindow: function () {
			var win = this._createWindow("");
			this.getApplicationRoot().add(win);
			var _that = this;
			var buttons = [{
				'label': this.tr("data.datatraits.save"),
				'icon': "icon/22/actions/dialog-ok.png",
				'callback': function (formData) {
					_that._traitEditWindow.close();
					_that._copy(formData, _that.__model);
					var x = JSON.stringify(_that.__model); console.log("after:" + x);
					_that.fireDataEvent("change", _that.__model, null);
				},
				'value': "save"
			}];
			var context = {};
			context.buttons = buttons;
			var sdesc = ms123.StoreDesc.getGlobalMetaStoreDesc();
			context.model = new ms123.config.ConfigManager().getEntityModel("team", sdesc, "main-form", "properties");
			var cols = context.model.attr("colModel");
			var props = context.model.attr("gridProps");
			var entityName = this._context.entityName;
			var newCols = [];
			context.model.attr= function (what) {
					if (what == "gridProps") {
						return props;
					}
					if (what == "colModel") {
						return newCols;
					}
				}

			cols.each((function (c) {
				var name = c.name;
				if( c.name.match(/^property/)){
					var msg = this.tr("teamproperty."+entityName+"."+c.name);
					console.log("Msg:"+msg+"/"+entityName);
					if( !msg.match(/^teamproperty/)){
						c.label= msg;
						newCols.push(c);
					}
				}else{
					newCols.push(c);
				}
			}).bind(this));

			context.config = "team";
			var form = new ms123.widgets.Form(context);
			this.__model["description"] = this.getLabel();
			form.fillForm(this.__model);
			this.setAllEnabled(form);
			win.add(form);
			this._traitEditForm = form;
			this._traitEditWindow = win;
		},
		setAllEnabled: function (form) {
			var form =  form.form;
			var m = form.getModel();
			var props = qx.Class.getProperties(m.constructor);
			var items = form.getItems();
			for (var i = 0, l = props.length; i < l; i++) {
				var item = items[props[i]];
				if (item){
				  item.setEnabled(true);
					if( item.getUserData("key").match(/^property/)){
				 		item.setReadOnly(false);
					}
				}
			}
		},
		_createWindow: function (name) {
			var win = new qx.ui.window.Window(name, "").set({
				resizable: true,
				useMoveFrame: true,
				useResizeFrame: true
			});
			win.setLayout(new qx.ui.layout.Grow);
			win.setWidth(600);
			win.setHeight(300);
			win.setAllowMaximize(false);
			win.setAllowMinimize(false);
			win.setModal(true);
			win.setActive(true);
			//	win.minimize();
			win.center();
			win.open();
			return win;
		},


		// overridden
		/**
		 * @lint ignoreReferenceField(_forwardStates)
		 */
		_forwardStates: {
			focused: true,
			hovered: true,
			selected: true,
			dragover: true
		},


		/**
		 * Event handler for the mouse over event.
		 */
		_onMouseOver: function () {
			this.addState("hovered");
		},


		/**
		 * Event handler for the mouse out event.
		 */
		_onMouseOut: function () {
			this.removeState("hovered");
		},
		_copy: function (obj, to) {
			for (var key in obj) {
				to[key] = obj[key];
			}
		},
		resetModel: function () {
			alert("TraitListItem.resetModel");
		}
	},

	destruct: function () {
		this.removeListener("mouseover", this._onMouseOver, this);
		this.removeListener("mouseout", this._onMouseOut, this);
	}
});
