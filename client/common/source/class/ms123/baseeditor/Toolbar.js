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
 * @ignore(Hash)
 * @ignore($A)
 */
qx.Class.define("ms123.baseeditor.Toolbar", {
	extend: qx.ui.container.Composite,
	include: [qx.locale.MTranslation],

	/**
	 * Constructor
	 */
	construct: function (facade) {
		this.base(arguments);
		this.facade = facade;
		this._init();
		this.plugs = [];
		this.facade.registerOnEvent(ms123.ruleseditor.Config.EVENT_BUTTON_UPDATE, this.onButtonUpdate.bind(this));
	},

	/**
	 *****************************************************************************
	 *  PROPERTIES
	 *****************************************************************************
	 */
	properties: {
		toolbar: {
			check: "qx.ui.toolbar.ToolBar",
			nullable: true
		}
	},

	/**
	 * ****************************************************************************
	 * MEMBERS
	 * ****************************************************************************
	 */
	members: {
		_init: function () {
			this.setLayout(new qx.ui.layout.VBox());
			var toolbar = new qx.ui.toolbar.ToolBar().set({});
			this.add(toolbar);
			this.setToolbar(toolbar);
			toolbar.setSpacing(2);
			this.groupIndex = new Hash();
		},

		onButtonUpdate: function (event) {
			var button = this.buttons.find(function (button) {
				return button.getUserData("id") === event.id;
			});

			if (event.pressed !== undefined) {
				button.buttonInstance.toggle(event.pressed);
			}
		},
		registryChanged: function (pluginsData) {
			// Sort plugins by group and index
			var newPlugs = pluginsData.sortBy((function (value) {
				console.log("value:" + value);
				return ((this.groupIndex[value.group] != undefined ? this.groupIndex[value.group] : "") + value.group + "" + value.index).toLowerCase();
			}).bind(this));
			var plugs = $A(newPlugs).findAll(qx.lang.Function.bind(function (value) {
				return !this.plugs.include(value) && (!value.target || value.target === ms123.ruleseditor.plugins.Toolbar)
			}, this));
			if (plugs.length < 1) return;

			this.buttons = [];

			var currentGroupsName = this.plugs.last() ? this.plugs.last().group : plugs[0].group;

			// Map used to store all drop down buttons of current group
			var currentGroupsDropDownButton = {};
			var currentRadioGroup = {};


			plugs.each((function (value) {
				if (!value.name) {
					return
				}
				this.plugs.push(value);
				// Add seperator if new group begins
				if (currentGroupsName != value.group) {
					this.getToolbar().add(new qx.ui.toolbar.Separator());
					currentGroupsName = value.group;
					currentGroupsDropDownButton = {};
				}

				// If an drop down group icon is provided, a split button should be used
				if (value.dropDownGroupIcon) {
					var splitMenu = currentGroupsDropDownButton[value.dropDownGroupIcon];

					if (splitMenu === undefined) {
						var menu = new qx.ui.menu.Menu;
						this.getToolbar().add(new qx.ui.toolbar.SplitButton("", value.dropDownGroupIcon, menu));
						splitMenu = currentGroupsDropDownButton[value.dropDownGroupIcon] = menu;
					}

					if (value.toggle) {
						var button = new qx.ui.toolbar.CheckBox(value.name, value.icon);
						button.addListener("execute", value.functionality, this);

					} else {
						var button = new qx.ui.toolbar.Button(value.name, value.icon);
						button.addListener("execute", value.functionality, this);
					}
					if (value.description) {
						button.setToolTipText(value.description);
					}
					button.setUserData("id", value.id);
					value['buttonInstance'] = button;
					splitMenu.add(button);
				} else if (value.radioGroup) {
					var radioGroup = currentRadioGroup[value.radioGroup];
					if (radioGroup === undefined) {
						var menu = new qx.ui.form.RadioGroup();
						radioGroup = currentRadioGroup[value.radioGroup] = menu;
					}
					var button = new qx.ui.form.ToggleButton(value.name, value.icon);
					button.addListener("execute", value.functionality, this);

					if (value.description) {
						button.setToolTipText(value.description);
					}
					button.setUserData("id", value.id);
					value['buttonInstance'] = button;
					radioGroup.add(button);
					this.getToolbar().add(button);
				} else if (value.addFill) {
					this.getToolbar().addSpacer();
				} else { // create normal, simple button
					if (value.toggle) {
						var button = new qx.ui.toolbar.CheckBox("", value.icon);
						button.addListener("execute", value.functionality, this);
					} else {
						var button = new qx.ui.toolbar.Button("", value.icon);
						button.addListener("execute", value.functionality, this);
					}
					if (value.description) {
						button.setToolTipText(value.description);
					}
					button.setUserData("id", value.id);
					this.getToolbar().add(button);
					value['buttonInstance'] = button;
				}
				this.buttons.push(value);
			}).bind(this));
			this.enableButtons([]);
		},

		onUpdate: function () {
			this.enableButtons();
		},

		enableButtons: function () {
			this.buttons.each((function (value) {
				value.buttonInstance.setEnabled(true);
				if (value.isEnabled && !value.isEnabled(value.buttonInstance)) value.buttonInstance.setEnabled(false);
			}).bind(this));
		}
	}
});
