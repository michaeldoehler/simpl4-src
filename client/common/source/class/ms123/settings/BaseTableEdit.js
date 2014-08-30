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
qx.Class.define("ms123.settings.BaseTableEdit", {
	extend: ms123.form.TableEdit,

	/**
	 *****************************************************************************
	 CONSTRUCTOR
	 *****************************************************************************
	 */

	construct: function (facade,configName) {
		this.facade = facade;
		this._configName = configName;
		this.base(arguments, configName, ms123.StoreDesc.getGlobalMetaStoreDesc());

		this._init();
		this._load();

		this._editForm = this._createEditForm();
		this._editWindow = this._createEditWindow();
		this._editWindow.add(this._editForm);

	},

	/**
	 *****************************************************************************
	 PROPERTIES
	 *****************************************************************************
	 */
	/**
	 *****************************************************************************
	 MEMBERS
	 *****************************************************************************
	 */

	members: {
		_init:function(){
		},
		configGridContext: function (context) {
			var _this = this;
			var buttons = [{
				'label': "",
				'icon': "icon/16/actions/list-add.png",
				'callback': function (m) {
					_this._mode = "add";
					_this._editWindow.setActive(true);
					_this._editWindow.open();
					_this._editForm.fillForm({});
				},
				'value': "add"
			},
			{
				'label': "",
				'icon': "icon/16/apps/utilities-text-editor.png",
				'callback': function (m) {
					_this._mode = "edit";
					var map = _this._table.getCurrentRecord();
					_this._editForm.fillForm(map);
					_this._editWindow.setActive(true);
					_this._editWindow.open();
				},
				'value': "edit"
			},
			{
				'label': "",
				'icon': "icon/16/places/user-trash.png",
				'callback': function (m) {
					_this._table.deleteCurrentRecord();
					_this._save();
				},
				'value': "del"
			},
			{
				'label': "",
				'icon': "icon/16/actions/go-up.png",
				'callback': function (m) {
					_this._table.currentRecordUp();
				},
				'value': "up"
			},
			{
				'label': "",
				'icon': "icon/16/actions/go-down.png",
				'callback': function (m) {
					_this._table.currentRecordDown();
				},
				'value': "down"
			},
			{
				'label': "",
				'icon': "icon/16/actions/document-save.png",
				'callback': function (m) {
					_this._save();
				},
				'value': "save"
			}
			];


			context.buttons = buttons;
			var cols = context.model.attr("colModel");
		},
		propagateTable: function (table) {
			this._table = table;
			table.getTable().addListener("cellClick", this._onCellClick, this);
			table.getTable().addListener("dblclick", this._onDblClick, this);
		},
		prepareColumns: function (columns) {
		},
		_onCellClick:function(e){
		},
		_onDblClick:function(e){
			this._mode = "edit";
			var map = this._table.getCurrentRecord();
			this._editForm.fillForm(map);
			this._editWindow.setActive(true);
			this._editWindow.open();
		},
		_createEditWindow: function () {
			var win = new qx.ui.window.Window("", "").set({
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
			win.setActive(false);
			win.minimize();
			win.center();
			this.getApplicationRoot().add(win);
			return win;
		},
		_createEditForm: function () {
			var _this = this;
			var buttons = [{
				'label': this.tr("settings."+this._configName+".save"),
				'icon': "icon/22/actions/dialog-ok.png",
				'callback': function (m) {
					var map = {};
					qx.lang.Object.mergeWith(map, m);
					if (_this._mode == "edit") {
						_this._table.setCurrentRecord(map);
					} else {
						_this._table.addRecord(map);
					}
					_this._editWindow.close();
					_this._save();
				},
				'value': "save"
			}];
			var context = {};
			context.buttons = buttons;
			var cm = new ms123.config.ConfigManager();
			var columns = cm.getEntityViewFields(this._configName,ms123.StoreDesc.getGlobalMetaStoreDesc(),"main-form",false);
			this.prepareColumns(columns);
			var cm = new ms123.config.ConfigManager();
			var cols = cm.buildColModel(columns, this._configName, this.facade.storeDesc, "meta", "form");
			context.model = cm.buildModel(cols, {
				formlayout: this._getFormLayout()
			});
			context.unit_id = this._configName+"sXXX";
			context.config = this._configName;
			var form = new ms123.widgets.Form(context);
			this._prepareFormFields(form);
			return form;
		},
		_prepareFormFields:function(form){
		},
		_getFormLayout:function(){
			return "tab1;tab2:full;tab3:full";
		},
		_load:function(){
		},
		_save:function(){
		}
	}
});
