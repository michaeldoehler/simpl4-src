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
 * @ignore(jQuery) 
 * @ignore(jQuery.each)
 * @ignore(jQuery.inArray)
 */
qx.Class.define("ms123.team.ExpiredTeams", {
	extend: qx.ui.core.Widget,


	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (context) {
		this.base(arguments);
		this._init(context);
	},


	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},


	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		__user: null,

		_init: function (context) {
			var teams = this._getExpiredTeams(context);
			for (var i = 0; i < teams.length; i++) {
				var t = teams[i];
				t.validFrom = t.validFrom ? this._getDate(t.validFrom) : "";
				t.validTo = t.validTo ? this._getDate(t.validTo) : "";
			}
			var table = this._createTable(teams);
			var toolbar = this._createToolbar();
			var w = this._createWindow();
			w.add(table,{edge:"center"});
			w.add(toolbar,{edge:"south"});
			w.open();
		},

		_getDate:function(time){
			var m = qx.locale.Manager.getInstance();
			var lang = m.getLanguage();
			var format=null;
			if( lang == "de" ){
				format = this._dateFormat(new Date(time), "dd.MM.yyyy");
			}else{
				format = this._dateFormat(new Date(time), "MM/dd/yyyy");
			}
			return format;
		},
		_dateFormat: function (date, format) {
			var o = {
				"M+": date.getMonth() + 1,
				"d+": date.getDate(),
				"h+": date.getHours(),
				"m+": date.getMinutes(),
				"s+": date.getSeconds(),
				"q+": Math.floor((date.getMonth() + 3) / 3),
				"S": date.getMilliseconds() 
			}
			if (/(y+)/.test(format)){
				 format = format.replace(RegExp.$1, (date.getFullYear() + "").substr(4 - RegExp.$1.length));
			}
			for (var k in o){
			  if (new RegExp("(" + k + ")").test(format)){
					 format = format.replace(RegExp.$1, RegExp.$1.length == 1 ? o[k] : ("00" + o[k]).substr(("" + o[k]).length));
				}
			}
			return format;
		},
		_getExpiredTeams: function (context) {
			try {
				return ms123.util.Remote.rpcSync("team:expiredTeams", {
					namespace: context.storeDesc.getNamespace(),
					entityName: context.entityName
				});
			} catch (e) {
				ms123.form.Dialog.alert("ExpiredTeams._getExpiredTeams:" + e);
				return null;
			}
		},
		_createTable: function (data) {
			var colIds = new Array();
			var colHds = new Array();
			var colWidth = new Array();
			colIds.push("teamid");
			colHds.push(this.tr("team.team"));
			colWidth.push("35%");

			colIds.push("description");
			colHds.push(this.tr("team.description"));
			colWidth.push("35%");

			colIds.push("validFrom");
			colHds.push(this.tr("team.validFrom"));
			colWidth.push("15%");

			colIds.push("validTo");
			colHds.push(this.tr("team.validTo"));
			colWidth.push("15%");

		//	colIds.push("disabled");
		//	colHds.push(this.tr("team.disabled"));
		//	colWidth.push("15%");

			var tableModel = new qx.ui.table.model.Simple();
			tableModel.setColumns(colHds, colIds);
			var customMap = {
				tableColumnModel: function (obj) {
					return new qx.ui.table.columnmodel.Resize(obj);
				}
			};
			var table = new qx.ui.table.Table(tableModel, customMap);
			var tcm = table.getTableColumnModel();
			colWidth.each((function (w, index) {
				var resizeBehavior = tcm.getBehavior();
				resizeBehavior.setWidth(index, w);
			}).bind(this));
			table.setStatusBarVisible(false);
			tableModel.setDataAsMapArray(data, true);
			return table;
		},
		_createToolbar:function(){
			var control = new qx.ui.toolbar.ToolBar().set({});
			control.setSpacing(5);
			var buttonExport = new qx.ui.toolbar.Button(this.tr("export.export_button"), "ms123/csv_icon.png");
			buttonExport.addListener("execute", function () {
			},this);
			control.add(buttonExport);
			return control;
		},
		_createWindow: function () {
			var win = new qx.ui.window.Window("", "").set({
				resizable: true,
				useMoveFrame: true,
				useResizeFrame: true
			});
			win.setLayout(new qx.ui.layout.Dock);
			win.setWidth(650);
			win.setHeight(350);
			win.setAllowMaximize(false);
			win.setAllowMinimize(false);
			win.setModal(true);
			win.setActive(false);
			win.minimize();
			win.center();
			this.getApplicationRoot().add(win);
			return win;
		}
	}
});
