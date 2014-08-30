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
/*
*/

qx.Class.define("ms123.settings.views.SelectableItems", {
	extend: qx.ui.container.Composite,
	include: qx.locale.MTranslation,

	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (facade) {
		this.base(arguments);
		this.facade = facade;
		this.setLayout(new qx.ui.layout.Dock());
		this._selectedMap = {};


		this._mainArea = new qx.ui.container.Scroll().set({});
		this.add(this._mainArea, {
			edge: "center"
		});
		var toolbar = this._createToolbar();
		this.add(toolbar, {
			edge: "south"
		});
		for (var i = 0; i < facade.selected.length; i++) {
			this._selectedMap[facade.selected[i].name] = true;
		}
		this._createCheckBoxes(facade.selectables, facade.selected, facade.namespace, facade.entity);
	},


	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},

	events: {
		"change": "qx.event.type.Data"
	},

	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		_createCheckBoxes: function (selectables, selected, namespace, entity) {
			var cCols = 3;
			var cRows = ((selectables.length / cCols) | 0) + 1;
			console.log("cFields:" + selectables.length + ",cCols:" + cCols + ",cRows:" + cRows);

			var layout = new qx.ui.layout.Grid(cRows, cCols);
			for (var i = 0; i < cCols; i++) {
				layout.setColumnFlex(i, 1);
			}
			var content = new qx.ui.core.Widget().set({
				padding: 10
			});
			content._setLayout(layout);
			this._mainArea.add(content, {});

			var row = 0,
				col = 0;
			this._cbList = [];
			for (var f = 0; f < selectables.length; f++) {
				var field = selectables[f];
				var dt = field.datatype;
				if (dt != undefined && (dt.match("^set") || dt.match("^list") || /*dt.match("^relat") ||*/ dt.match("^object"))) continue;
				var displayname = field.name.match("^_") ?
						this.tr("data." + field.name) :
						this.tr("data." + entity + "." + field.name);
				var cb = new qx.ui.form.CheckBox(displayname);
				cb.setUserData("name", field.name);
				cb.setUserData("displayname", displayname);
				if (this._isSelected(field.name)) {
					cb.setValue(true);
				}
				cb.addListener("execute", function (e) {
					var c = e.getTarget();
					var name = c.getUserData("name");
					var value = c.getValue();
					this._fireChangeEvent(name, c.getUserData("displayname"), value);
				}, this);

				if (f == (cRows * (col + 1))) {
					row = 0;
					col++;
				}

				this._cbList.push(cb);
				content._add(cb, {
					row: row,
					column: col
				});
				row++;
			}
		},
		_fireChangeEvent: function (name, displayname, value) {
			var data = {
				name: name,
				tab: "tab1",
				displayname: displayname,
				value: value
			};
			this.fireDataEvent("change", data);
			this._setSelected(name, value);
		},
		_setSelected: function (name, value) {
			this._selectedMap[name] = value;
		},
		_isSelected: function (name) {
			if (this._selectedMap[name] === true) return true;
			return false;
		},
		_createToolbar: function () {
			var toolbar = new qx.ui.toolbar.ToolBar();
			var b = new qx.ui.toolbar.Button(this.tr("fieldselector.removeall"), "icon/16/actions/media-seek-backward.png");
			b.addListener("execute", function (e) {
				for (var i = 0; i < this._cbList.length; i++) {
					var cb = this._cbList[i];
					var name = cb.getUserData("name");
					if (this._isSelected(name)) {
						cb.setValue(false);
						this._fireChangeEvent(cb.getUserData("name"), cb.getUserData("displayname"), false);
					}
				}
			}, this);
			toolbar._add(b);
			toolbar.setSpacing(5);

			b = new qx.ui.toolbar.Button(this.tr("fieldselector.selectall"), "icon/16/actions/media-seek-forward.png");
			b.addListener("execute", function (e) {
				for (var i = 0; i < this._cbList.length; i++) {
					var cb = this._cbList[i];
					var name = cb.getUserData("name");
					if (!this._isSelected(name)) {
						cb.setValue(true);
						this._fireChangeEvent(cb.getUserData("name"), cb.getUserData("displayname"), true);
					}
				}
			}, this);
			toolbar._add(b);
			toolbar.setSpacing(5);

			b = new qx.ui.toolbar.Button(this.tr("fieldselector.selectall_without"), "icon/16/actions/media-skip-forward.png");
			b.addListener("execute", function (e) {
				for (var i = 0; i < this._cbList.length; i++) {
					var cb = this._cbList[i];
					var name = cb.getUserData("name");
					if (name.match("^_")) continue;
					if (!this._isSelected(name)) {
						cb.setValue(true);
						this._fireChangeEvent(cb.getUserData("name"), cb.getUserData("displayname"), true);
					}
				}
			}, this);
			toolbar._add(b);
			return toolbar;
		}
	}
});
