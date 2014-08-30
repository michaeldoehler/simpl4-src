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
 qooxdoo dialog library
 
 http://qooxdoo.org/contrib/project#dialog
 
 Copyright:
 2007-2010 Christian Boulanger
 
 License:
 LGPL: http://www.gnu.org/licenses/lgpl.html
 EPL: http://www.eclipse.org/org/documents/epl-v10.php
 See the LICENSE file in the project's top-level directory for details.
 
 Authors:
 *  Christian Boulanger (cboulanger)
 ************************************************************************ */

qx.Class.define("ms123.form.FormRenderer", {
	extend: qx.ui.form.renderer.Single,
	implement: qx.ui.form.renderer.IFormRenderer,

	members: {
		_row: 0,
		_buttonRow: null,

		addItems: function (items, names, title) {
			if (title != null) {
				this._add(
				this._createHeader(title), {
					row: this._row,
					column: 0,
					colSpan: 2
				});
				this._row++;
			}

			for (var i = 0; i < items.length; i++) {
				var item = items[i];
				if (item instanceof qx.ui.form.RadioGroup) {
					if (item.getUserData("orientation") == "horizontal") {
						var widget = this._createHBoxForRadioGroup(item);
					}
					else {
						var widget = this._createWidgetForRadioGroup(item);
					}
				} else {
					var widget = item;
				}

				if (names[i] && item.getUserData("excluded")) {
					var label = new qx.ui.basic.Label(names[i]);
					label.setRich(true);
					this._add(label, {
						row: this._row,
						column: 0,
						colSpan: 2
					});
				} else if (!names[i]) {
					this._add(widget, {
						row: this._row,
						column: 0,
						colSpan: 2
					});
				} else {
					var label = this._createLabel(names[i], item);
					label.setRich(true);
					this._add(label, {
						row: this._row,
						column: 0
					});
					this._add(widget, {
						row: this._row,
						column: 1
					});
				}
				this._row++;

			}
		},

		_createWidgetForRadioGroup: function (group) {
			var widget = new qx.ui.container.Composite(new qx.ui.layout.VBox(5));
			var items = group.getItems();
			for (var i = 0; i < items.length; i++) {
				widget.add(items[i]);
			}
			return widget;
		},

		_createHBoxForRadioGroup: function (group) {
			var widget = new qx.ui.container.Composite(new qx.ui.layout.HBox(5));
			var items = group.getItems();
			for (var i = 0; i < items.length; i++) {
				widget.add(items[i]);
			}
			return widget;
		}
	}
});
