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
	@lint ignoreDeprecated(alert,eval) 
	@asset(qx/icon/${qx.icontheme}/22/actions/*)
	@asset(qx/icon/${qx.icontheme}/16/places/*)
*/
qx.Class.define("ms123.form.TableEdit", {
	extend: qx.ui.container.Composite,
	implement: [qx.ui.form.IStringForm, qx.ui.form.IForm],
	include: [qx.ui.form.MForm],


	/**
	 *****************************************************************************
	 CONSTRUCTOR
	 *****************************************************************************
	 */

	construct: function (config,storeDesc) {
		this.base(arguments);
		var layout = new qx.ui.layout.Grow();
		this.setLayout(layout);

		this._config = config;
		
		this._storeDesc = storeDesc || ms123.StoreDesc.getNamespaceDataStoreDesc();
		console.log("_storeDesc:" + this._storeDesc);
		var mainContainer = new qx.ui.container.Composite(new qx.ui.layout.Dock()).set({
			decorator: "main",
			allowGrowX: true,
			allowGrowY: true
		});
		this._add(mainContainer, {});

		this._table = this._createEditTable(config);
		console.error("table:" + this._table);
		mainContainer.add(this._table, {
			edge: "center"
		});
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
			init: "combobox"
		}
	},

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
	 MEMBERS
	 *****************************************************************************
	 */

	members: {
		// overridden
		/**
		 * @lint ignoreReferenceField(_forwardStates)
		 */
		_forwardStates: {
			focused: true
		},


		// interface implementation
		setValue: function (value) {
			try {
				if (value == null || value == "") {
					this._table.setData([]);
				} else {
					var data = qx.lang.Json.parse(value);
					this._table.setData(data);
				}
			} catch (e) {}
		},

		setValueAsList: function (value) {
			try {
				if (value == null || value == undefined) {
					this._table.setData([]);
				} else {
					this._table.setData(value);
				}
			} catch (e) {}
		},
		// interface implementation
		getValue: function () {
			var data = this._table.getData();
			data = qx.util.Serializer.toJson(data);
			return data;
		},
		getValueAsList: function () {
			var data = this._table.getData();
			return data;
		},


		// interface implementation
		resetValue: function () {
			alert("resetValue");
		},


		// useit checkbox
		getCheckBox: function () {
			return this.getChildControl("checkbox");
		},

		/**
		 ---------------------------------------------------------------------------
		 EVENT LISTENERS
		 ---------------------------------------------------------------------------
		 */
		_createEditTable: function (module) {
			var cm = new ms123.config.ConfigManager();
			var columns = null;
			try {
				var storeId = this._storeDesc.getStoreId();
				columns = cm.getEntityViewFields(module, this._storeDesc,"main-grid", false);
			} catch (e) {
				ms123.form.Dialog.alert("TableEdit._createEditTable:" + e);
				return;
			}
			console.log("columns:" + columns);
			console.log("module:" + module);

			this.prepareColumns(columns);
			var gridProps = {};
			gridProps.list = columns;
			gridProps.paging = false;
			gridProps.search = false;
			gridProps.formlayout = 'tab1';

			var gridContext = {};
			gridContext.isMaster = true;
			gridContext.config = module;

			var cols = cm.buildColModel(columns, module, this.__storeDesc, "meta", "grid");
			gridContext.model = cm.buildModel(cols, gridProps);


			gridContext.unit_id = "unused";
			gridContext.user = {
				manage: true,
				read: true
			};
			gridContext.storeDesc = this._storeDesc;
			this.configGridContext(gridContext);
			var table = new ms123.widgets.Table(gridContext);
			this.propagateTable(table);
			var _this = this;
			table.getTable().getTableModel().addListener("dataChanged", function (e) {
				var data = _this._table.getData();
				data = qx.util.Serializer.toJson(data);
				_this.fireDataEvent("changeValue", data, null);
			});
			return table;
		},
		_buildModel: function (columns, props) {
			var model = {
				attr: function (what) {
					if (what == "gridProps") {
						return props;
					}
					if (what == "colModel") {
						return columns;
					}
				}
			}
			return model;
		},
		configGridContext: function (context) {},
		prepareColumns: function (columns) {},
		propagateTable: function (table) {}
	}
});
