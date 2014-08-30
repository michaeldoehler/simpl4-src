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
	@asset(qx/icon/${qx.icontheme}/22/actions/*)
	@asset(qx/icon/${qx.icontheme}/16/places/*)
*/
qx.Class.define("ms123.form.TableSelect", {
	extend: qx.ui.container.Composite,
	implement: [qx.ui.form.IStringForm, qx.ui.form.IForm],
	include: [qx.ui.form.MForm],


	/**
	 *****************************************************************************
	 CONSTRUCTOR
	 *****************************************************************************
	 */

	construct: function (data, config, multiselection, key) {
		this.base(arguments);
		var layout = new qx.ui.layout.Dock();
		this.setLayout(layout);

		this.key = key;
		console.log("data:" + qx.util.Serializer.toJson(data));
		console.log("key:" + key);
		console.log("config:" + qx.util.Serializer.toJson(config));
		this.multiselection = multiselection;
		
		if( config.filter){
			data = ms123.util.Remote.rpcSync("data:executeFilterByName", {
				storeId: config.storeDesc.getStoreId(),
				name: config.filter
			});
			console.log("data:" + qx.util.Serializer.toJson(data));
		}
		this._table = this._createTable(data, config.columns, multiselection);
		this.add(this._table, {
			edge: "center"
		});
		this.addListener("changeValid", this._changeValid, this);
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
		 *  Event data: The new text value of the table.
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
		/**
		 * @lint ignoreReferenceField(_forwardStates)
		 */
		_forwardStates: {
			focused: true
		},

		_changeValid: function (e) {
			if (!this.decorator) {
				this.decorator = this.getDecorator();
			}
			var value = e.getData();
			if (value) {
				this.setDecorator(this.decorator);
			} else {
				this.setDecorator(new qx.ui.decoration.Decorator(1, 'solid', 'red'));
			}
		},


		// interface implementation
		setValue: function (value) {
			try {
				if (value == null || value == "") {
					//this._table.setData([]);
				} else {
					//var data = qx.lang.Json.parse(value);
					//this._table.setData(data);
				}
			} catch (e) {}
		},

		// interface implementation
		getValue: function () {
			var data = this.getSelectedRows(this.multiselection);
			if (data === undefined || data === null) {
				console.log("getValue:[]");
				return this.multiselection ? [] : null;
			}
			console.log("getValue:data:" + qx.util.Serializer.toJson(data));
			return data;
		},


		// interface implementation
		resetValue: function () {},

		setMinWidth: function (w) {
			console.log("setWidth:" + w);
			this._table.setWidth(w);
		},

		// useit checkbox
		getCheckBox: function () {
			return this.getChildControl("checkbox");
		},

		// overridden
		_createChildControlImpl: function (id, hash) {
			var control;
			switch (id) {
			case "checkbox":
				control = new qx.ui.form.CheckBox();
				control.setFocusable(false);
				control.setKeepActive(true);
				control.addState("inner");
				control.set({
					decorator: "main"
				});
				this.add(control, {
					edge: "west"
				});
				break;
			case "select":
				break;
			}
			return control;
		},
		_createTable: function (data, columns, multiselection) {
			var colIds = new Array();
			var colHds = new Array();

			var items = columns.items;
			//colIds.push("id");
			var key = this.key;
			//colHds.push(this.tr("Id"));

			if (items) {
				for (var i = 0; i < items.length; i++) {
					var item = items[i];
					colIds.push(item.colname);
					if( item.display){
						if( item.display.match(/^@/)){
							colHds.push(this.tr(item.display.substring(1)));
						}else{
							colHds.push(this.tr(item.display));
						}
					}else{
						colHds.push(this.tr(key + "." + item.colname));
					}
				}
			}

			this._tableModel = new qx.ui.table.model.Simple();
			this._tableModel.setColumns(colHds, colIds);
			var customMap = {
				tableColumnModel: function (obj) {
					return new qx.ui.table.columnmodel.Resize(obj);
				}
			};

			var table = new qx.ui.table.Table(this._tableModel, customMap);
			var tcm = table.getTableColumnModel();
			//var resizeBehavior = tcm.getBehavior();
			//resizeBehavior.setWidth(0, 30);
			table.setStatusBarVisible(null);
			var selModel = table.getSelectionModel();
			selModel.setSelectionMode(multiselection ? qx.ui.table.selection.Model.MULTIPLE_INTERVAL_SELECTION : qx.ui.table.selection.Model.SINGLE_SELECTION);
			selModel.addListener("changeSelection", function (e) {
				var index = selModel.getLeadSelectionIndex();
				var map = this._tableModel.getRowDataAsMap(index);
				var count = selModel.getSelectedCount();
				if (count == 0) {
					this.fireDataEvent("changeValue", multiselection ? [] : null, null);
					selModel.resetSelection();
					table.setFocusedCell(null, null);
					return;
				}

				var data = this.getSelectedRows(multiselection);
				var data1 = qx.util.Serializer.toJson(data);
				console.log("selData:" + data1);
				this.fireDataEvent("changeValue", data, null);

			}, this);

			this._tableModel.setDataAsMapArray(data, true);
			this.fireDataEvent("changeValue", multiselection ? [] : null, null);
			return table;
		},
		getSelectedRows: function (multiselection) {
			console.log("multiselection:" + multiselection);
			var selModel = this._table.getSelectionModel();
			var index = selModel.getLeadSelectionIndex();
			var count = selModel.getSelectedCount();
			console.log("index:" + index + "/" + count);
			if (count == 0) {
				return;
			}
			if (!multiselection) {
				var map = this._tableModel.getRowDataAsMap(index);
				return map;
			}

			var data = [];
			selModel.iterateSelection(function (index) {
				var map = this._tableModel.getRowDataAsMap(index);
				data.push(map);
			}, this);
			return data;
		}
	}
});
