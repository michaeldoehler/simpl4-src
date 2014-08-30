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
	@asset(qx/icon/${qx.icontheme}/16/apps/*)
	@asset(ms123/icons/*)
	@asset(ms123/*)
*/

qx.Class.define("ms123.shell.views.Details", {
	extend: qx.ui.core.Widget,
	include: qx.locale.MTranslation,


	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (model,facade) {
		this.base(arguments);
		this.facade=facade;
		this.parentModel = model;
		this._setLayout(new qx.ui.layout.Dock());
		this._table = this._createTable();
		this._add(this._table, {
			edge: "center"
		});

		this._toolbar = this._createToolbar();
		this._add(this._toolbar, {
			edge: "south"
		});
		this._showData();
		
		var pathLabel = new qx.ui.basic.Label();
		pathLabel.setRich(true);
		pathLabel.setValue( "&nbsp;&nbsp;<span style='color:blue'>"+ model.getPath()+ "</span>");
		this._add(pathLabel, {
			edge: "north"
		});
			
		this.parentModel.addListener("changeBubble", function(){
			this._showData();	
		}, this);
	},


	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},


	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		_createTable: function () {
			var colIds = new Array();
			var colWidth = new Array();
			var colHds = new Array();
			colIds.push("value");
			colHds.push(this.tr("shell.details.value"));
			colWidth.push(40);

			colIds.push("type");
			colHds.push(this.tr("shell.detail.type"));
			colWidth.push(100);

			colIds.push("date");
			colHds.push(this.tr("shell.details.date"));
			colWidth.push(40);

			this._tableModel = new qx.ui.table.model.Simple();
			this._tableModel.setColumns(colHds, colIds);
			var customMap = {
				tableColumnModel: function (obj) {
					return new qx.ui.table.columnmodel.Resize(obj);
				}
			};
			var table = new qx.ui.table.Table(this._tableModel, customMap);
			var tcm = table.getTableColumnModel();
			table.getTableModel().setColumnEditable(0, false);
			table.getTableModel().setColumnEditable(1, false);
			table.getTableModel().setColumnEditable(2, false);
			table.setStatusBarVisible(false);

			var resizeBehavior = tcm.getBehavior();
			for (var c = 0; c < colWidth.length; c++) {
				resizeBehavior.setMinWidth(c, colWidth[c]);
				//				resizeBehavior.set(c, { width:"1*", minWidth:40, maxWidth:80  });
			}
			return table;
		},
		_showData:function(){
			var rootpath = ms123.shell.Config.FSROOT;
			var namespace = this.facade.storeDesc.getNamespace();
			var path = this.parentModel.getPath();
			console.log("path:" + path);
			var t = null;
			try{
				t = ms123.util.Remote.rpcSync( "jcrfs:getTree",{
												workspace:namespace,
												path:path,
												mapping:"path:nodepath,value:nodename,uuid:uuid,title:nodename,type:nodetype",
												nodetype:ms123.shell.Config.FILE_FT, 
												depth:6
											});
				console.log("treedata:"+qx.util.Serializer.toJson(t));
			}catch(e){
				ms123.form.Dialog.alert("Details._showData:"+e.message);
				return;
			}
			var fileModel = qx.data.marshal.Json.createModel(t);

			var props = qx.Class.getProperties(fileModel.constructor);
			var list = [];
			var children = fileModel.getChildren();
			for (var i = 0; i < children.getLength(); i++) {
				var child = children.getItem(i);
				var map = {};
				props.each(function (p) {
					map[p] = child.get(p);
				});
				list.push(map);
			};
			this._tableModel.setDataAsMapArray(list, true);
		},
		_createToolbar: function () {
			var toolbar = new qx.ui.toolbar.ToolBar();
			toolbar.setSpacing(5);
			var buttonNew = new qx.ui.toolbar.Button(this.tr("shell.details_new"), "icon/16/actions/list-add.png");
			buttonNew.addListener("execute", function () {
				new ms123.shell.views.NewFile( this.parentModel );
			}, this);
			toolbar._add(buttonNew)
			toolbar.addSpacer();
			this._buttonNew = buttonNew;
			return toolbar;
		}
	}
});
