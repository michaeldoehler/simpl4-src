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
	@ignore($)
	@lint ignoreDeprecated(alert,eval) 
	@asset(qx/icon/${qx.icontheme}/22/actions/*)
	@asset(qx/icon/${qx.icontheme}/16/places/*)
*/
qx.Class.define("ms123.form.ModuleAccessEditor", {
	extend: qx.ui.container.Composite,
	implement: [qx.ui.form.IStringForm, qx.ui.form.IForm],
	include: [qx.ui.form.MForm],


	/**
	 *****************************************************************************
	 CONSTRUCTOR
	 *****************************************************************************
	 */

	construct: function (config) {
		this.base(arguments);
		var layout = new qx.ui.layout.Grow();
		this.setLayout(layout);

		var mainContainer = this._doLayout();

		this.add(mainContainer, {});
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
		/** Whenever the value is changed this event is fired
		 *
		 *  Event data: The new text value of the table.
		 */
		"changeValue": "qx.event.type.Data"
	},

	statics: {
		__defOptions: [{
			value: "all",
			label: "%moduleaccesseditor.all"
		},
		{
			value: "none",
			label: "%moduleaccesseditor.none"
		},
		{
			value: "notset",
			label: "%moduleaccesseditor.notset"
		}],

		__importOptions: [{
			value: "all",
			label: "%moduleaccesseditor.all"
		},
		{
			value: "none",
			label: "%moduleaccesseditor.none"
		},
		{
			value: "notset",
			label: "%moduleaccesseditor.notset"
		}],

		__accessOptions: [{
			value: "enabled",
			label: "%moduleaccesseditor.enabled"
		},
		{
			value: "disabled",
			label: "%moduleaccesseditor.disabled"
		},
		{
			value: "notset",
			label: "%moduleaccesseditor.notset"
		}]
	},


	/**
	 *****************************************************************************
	 MEMBERS
	 *****************************************************************************
	 */

	members: {
		__tableModel: null,
		__reverseMap: {},
		__ignoreChangeValue: false,
		__tableConfig: [{
			id: ms123.config.ConfigManager.CS_ENTITY,
			width:100,
			type: "label"
		},
		{
			id: "access",
			options: function () {
				return ms123.form.ModuleAccessEditor.__accessOptions;
			},
			type: "selectbox"
		},
		{
			id: "delete",
			options: function () {
				return ms123.form.ModuleAccessEditor.__defOptions;
			},
			type: "selectbox"
		},
		{
			id: "update",
			options: function () {
				return ms123.form.ModuleAccessEditor.__defOptions;
			},
			type: "selectbox"
		},
		{
			id: "export",
			options: function () {
				return ms123.form.ModuleAccessEditor.__defOptions;
			},
			type: "selectbox"
		},
		{
			id: "import",
			options: function () {
				return ms123.form.ModuleAccessEditor.__importOptions;
			},
			type: "selectbox"
		}/*,
		{
			id: "list",
			options: function () {
				return ms123.form.ModuleAccessEditor.__defOptions;
			},
			type: "selectbox"
		}*/,
		{
			id: "view",
			options: function () {
				return ms123.form.ModuleAccessEditor.__defOptions;
			},
			type: "selectbox"
		}],


		// interface implementation
		setValue: function (value) {
			console.error("setValue:" + value+",notset:"+this.__ignoreChangeValue);
			if (this.__ignoreChangeValue) return;
			try {
				if (value == null || value == "") {
					this._setValues({});
					this.__ignoreChangeValue = true;
					var arr = this.__tableModel.getDataAsMapArray();
					var data = this.__arrToMapString(arr);
					this.fireDataEvent("changeValue", data, null);
					this.__ignoreChangeValue = false;
				} else {
					var data = qx.lang.Json.parse(value);
					this._setValues(data);
				}
			} catch (e) {}
		},

		// interface implementation
		getValue: function () {
			var data = null;
			if (this.__tableModel) {
				var arr = this.__tableModel.getDataAsMapArray();
				data = this.__arrToMapString(arr);
			}
			//console.error("getValue:" + data);
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

		_setValues: function (data) {
console.log("ManagedApp:"+this.getUserData("managedApp"));
			var modules = new ms123.config.ConfigManager().getEntities(this.getUserData("managedApp"));
			var rows = [];
			this.__tableModel.removeRows(0, this.__tableModel.getRowCount());
			for (var i = 0; i < modules.length; i++) {
				var module = modules[i];
				var modname = module.name;
				var row = {};
				row.module = module.name;
				var dmap = data[module.name] ? data[module.name] : {};
				for (var j = 1; j < this.__tableConfig.length; j++) {
					var tc = this.__tableConfig[j];
					row[tc.id] = dmap[tc.id] ? dmap[tc.id] : "notset";

				}
//				var x = qx.util.Serializer.toJson(row); console.log("x:" + x);
				this.__tableModel.addRowsAsMapArray([row], null, true);
			}
		},
		_doLayout: function () {

			var mainArea = new qx.ui.container.Composite(new qx.ui.layout.Dock()).set({});
			mainArea.add(this._createEditTable(this.__tableConfig));

			console.log("mainArea:" + mainArea);
			return mainArea;
		},
		__getSelectBox: function (type) {
			var sb = new qx.ui.form.SelectBox();
			var tempItem = new qx.ui.form.ListItem("true");
			sb.add(tempItem);
			tempItem = new qx.ui.form.ListItem("notset");
			sb.add(tempItem);
			tempItem = new qx.ui.form.ListItem("false");
			sb.add(tempItem);
			return sb;
		},
		__getModuleLabel: function (name) {
			var label = new qx.ui.basic.Label().set({
				value: name,
				decorator: "group-item"
			});
			label.setWidth(500);
			label.setHeight(25);
			return label;
		},
		_createEditTable: function (tableConfig) {
			var colIds = new Array();
			var colHds = new Array();

			var _this = this;
			for (var i = 0; i < tableConfig.length; i++) {
				var col = tableConfig[i];
				colIds.push(col.id);
				colHds.push(this.tr("moduleaccesseditor." + col.id));
			}
			var tableModel = new qx.ui.table.model.Simple();
			tableModel.setColumns(colHds, colIds);
			this.__tableModel = tableModel;
			var customMap = {
				tableColumnModel: function (obj) {
					return new qx.ui.table.columnmodel.Resize(obj);
				}
			};
			var table = new qx.ui.table.Table(tableModel, customMap);
   		var selModel = table.getSelectionModel(); 
			selModel.setSelectionMode(1);
			table.addListener("dataEdited", function (e) {
				_this.__ignoreChangeValue = true;
				var arr = _this.__tableModel.getDataAsMapArray();
				var data = _this.__arrToMapString(arr);
				_this.fireDataEvent("changeValue", data, null);
				_this.__ignoreChangeValue = false;

			});
			table.setStatusBarVisible(false);
			var tcm = table.getTableColumnModel();

			for (var i = 0; i < tableConfig.length; i++) {
				var col = tableConfig[i];
				if (col.type == "selectbox") {
					var f = new ms123.util.SelectBox();
					var listData = [];
					var o = col.options;
					if (typeof o == "function") {
						o = col.options();
					}
					var listData = [];
					for (var j = 0; j < o.length; j++) {
						var value = o[j]
						var label;
						if (value.label.match(/^%/)) {
							label = this.tr(value.label.substring(1));
						} else {
							label = value.label;
						}
						this.__reverseMap[value.value] = label;
						var option = [label, null, value.value];
						listData.push(option);
					}
					f.setListData(listData);
					tcm.setCellEditorFactory(i, f);
					table.getTableModel().setColumnEditable(i, true);


					var colRenderer = new qx.ui.table.cellrenderer.Replace();
					colRenderer.setReplaceFunction(function (x) {
						return _this.__reverseMap[x];
					});
					tcm.setDataCellRenderer(i, colRenderer);

				}
				if (col.width !== undefined) {
					var resizeBehavior = tcm.getBehavior();
					resizeBehavior.setWidth(i, col.width);
				}
			}
			return table;
		},
		__arrToMapString: function (arr) {
			var d = {};
			for (var i = 0; i < arr.length; i++) {
				var m = arr[i];
				var modname = m.module;
				delete m.module;
				d[modname] = m;
			}
			return qx.util.Serializer.toJson(d);
		},
		_createChildControlImpl: function (id, hash) {
			var control;
			switch (id) {
			case "selectbox":
				var modules = new ms123.config.ConfigManager().getEntities();
				control = new qx.ui.form.SelectBox();
				for (var i = 0; i < modules.length; i++) {
					var module = modules[i];
					var modname = module.name;
					var tempItem = new qx.ui.form.ListItem(modname);
					control.add(tempItem);
				}
				break;
			}
			return control;
		}
	}
});
