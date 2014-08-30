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
qx.Class.define('ms123.ruleseditor.HeaderCell', {
	extend: qx.ui.container.Composite,

	construct: function () {
		this.base(arguments);

		var layout = new qx.ui.layout.Grid();
		layout.setRowFlex(0, 1);
		layout.setColumnFlex(0, 1);
		layout.setRowFlex(1, 1);
		this.setLayout(layout);
//		this.setBackgroundColor("white");
	},

	properties: {
		appearance: {
			refine: true,
			init: "table-header-cell"
		},

		label: {
			check: "String",
			init: null,
			nullable: true,
			apply: "_applyLabel"
		},

		sortIcon: {
			check: "String",
			init: null,
			nullable: true,
			apply: "_applySortIcon",
			themeable: true
		},

		icon: {
			check: "String",
			init: null,
			nullable: true,
			apply: "_applyIcon"
		}
	},

	members: {
		// property apply
		_applyLabel: function (value, old) {
			this._showChildControl("label1").setValue(value["header1"]);
			this._showChildControl("label2").setValue(value["header2"]);
			this._showChildControl("label3").setValue(value["header3"]);
			this._showChildControl("label4").setValue(value["header4"]);
		},

		_applySortIcon: function (value, old) {
		},

		_applyIcon: function (value, old) {
			if (value) {
				this._showChildControl("icon").setSource(value);
			} else {
				this._excludeChildControl("icon");
			}
		},

		// overridden
		_createChildControlImpl: function (id, hash) {
			var control;

			var font=(id == "label1") ? qx.bom.Font.fromString("11px sans-serif") : qx.bom.Font.fromString("9px sans-serif");
			var color=(id == "label4") ? "blue" : "black";
			switch (id) {
			case "label1":
			case "label2":
			case "label3":
			case "label4":
				control = new qx.ui.basic.Label().set({
					font:font,
					textColor:color,
					textAlign:"center",
					allowGrowX: true
				});

				var row = 0;
				if( id == "label2") row = 1;
				if( id == "label3") row = 2;
				if( id == "label4") row = 3;
				this._add(control, {
					row: row,
					column: 0
				});
				break;

			case "icon":
				control = new qx.ui.basic.Image(this.getIcon()).set({
					anonymous: true,
					allowShrinkX: true
				});
				this._add(control, {
					row: 0,
					column: 0
				});
				break;
			}

			return control || this.base(arguments, id);
		}
	}
});
