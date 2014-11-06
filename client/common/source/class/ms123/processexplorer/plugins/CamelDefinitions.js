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
	* @ignore(Clazz)
*/
qx.Class.define("ms123.processexplorer.plugins.CamelDefinitions", {
	extend: qx.ui.container.Composite,
	include: [qx.locale.MTranslation],

	/**
	 * Constructor
	 */
	construct: function (facade) {
		this.base(arguments);
		this.facade = facade;
		this.setLayout(new qx.ui.layout.Dock(2,10));
		this._namespace = facade.storeDesc.getNamespace();

		this._createContextSelect();
		var contextSelection = this._contextSelect.getModelSelection().getItem(0);
		if( contextSelection)
		this._createRouteTable(this._getRouteInfoList(contextSelection.getLabel()));
		//this._refreshDefinitions();
		this.facade.registerOnEvent(ms123.processexplorer.Config.EVENT_CAMELROUTESDEPLOYMENT_CHANGED, this._handleEvent.bind(this));
		var options= {
			physics: {
				hierarchicalRepulsion: {
					nodeDistance: 100, springLength: 0
				}
			},
			smoothCurves:false,
			hierarchicalLayout: {
      	enabled:true,
      	levelSeparation: 82,
      	direction: "UD"
    	},
			edges: {
					color: {color:'red'},
 					style:"arrow"
				},
  		tooltip: {
        delay: 300,
        fontColor: 'black',
        fontSize: 9, // px
        fontFamily: "Arial", // px
        color: {
          border: '#666',
          background: '#FfFfff'
        }
      }

		}


		this.addListenerOnce("appear", function () {
			console.log("Xidth:"+this.getBounds().width);
			var container = new qx.ui.container.Composite(new qx.ui.layout.Dock());
			this._vis = new ms123.processexplorer.plugins.VisWidget(options,this.getBounds().width-2,300);
			this._vis.setDecorator("main");
			this.facade.vis = this._vis;
			var toolbar = this._createToolbar();

			container.add(this._vis, {
				edge: "center"
			});
			container.add(toolbar, {
				edge: "south"
			});
			this.add(container, {
				edge: "south"
			});

		}, this);

		
	},

	/**
	 * ****************************************************************************
	 * MEMBERS
	 * ****************************************************************************
	 */
	members: {
		_createContextSelect:function(){
			var contextSelect = new qx.ui.form.SelectBox();
			var contextController = new qx.data.controller.List(null, contextSelect);
			contextController.setDelegate({
				bindItem: function (controller, item, index) {
					controller.bindProperty("label", "label", null, item, index);
					//controller.bindProperty("data", "model", null, item, index);
				}
			});
			this._contextController=contextController;
			this._contextSelect=contextSelect;
			this._refreshContextModel();
			this.add(contextSelect, {
				edge: "north"
			});
			contextSelect.addListener("changeSelection", function (e) {
				this._contextSelectionChanged();
			}, this);
			return contextSelect;
		},
		_contextSelectionChanged:function(){
			var contextSelection = this._contextSelect.getModelSelection().getItem(0);
			if( contextSelection){
				var data = this._getRouteInfoList(contextSelection.getLabel());
				this._setDataRouteTable(data);
			}
		},
		_refreshContextModel:function(){
			var data = {};
			data.items= []
			var contextList = this._getContextList();
			for( var i=0; i < contextList.length;i++){
				var item = {};
				item.label= contextList[i];
				data.items.push(item);
			}

			var model = qx.data.marshal.Json.createModel(data);
			var items = model.getItems();
			this._contextController.setModel(items);
			if( items.getLength()>0){
				this._contextSelect.setModelSelection([items.getItem(0)]);
			}
		},
		_createRouteTable: function (data) {
			var colIds = new Array();
			var colHds = new Array();
			var colWidth = new Array();

			colIds.push("id");
			colHds.push(this.tr("processexplorer.id"));
			colWidth.push(45);

			colIds.push("route");
			colHds.push(this.tr("processexplorer.route"));
			colWidth.push("7*");

			var tableModel = new qx.ui.table.model.Simple();
			this._routeTableModel = tableModel;
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


			var selModel = table.getSelectionModel();
			selModel.setSelectionMode(qx.ui.table.selection.Model.SINGLE_SELECTION);
			selModel.addListener("changeSelection", function (e) {
				var index = selModel.getLeadSelectionIndex();
				console.log("index:"+index);
				var count = selModel.getSelectedCount();
				console.log("count:"+count);
				if (count == 0) {
					return;
				}
				var contextKey = this._contextSelect.getModelSelection().getItem(0).getLabel();
				var map = this._routeTableModel.getRowDataAsMap(index);
				this.facade.raiseEvent({
					type: ms123.processexplorer.Config.EVENT_CAMELROUTEDEFINITION_CHANGED,
					camelContextKey: contextKey,
					camelRouteDefinition: map 
				});
				var graph = this._getRouteVisGraph(contextKey, map.id);
				this._vis.setData(graph);
			}, this);


			tableModel.setDataAsMapArray(data, true);
			this.add( table,{edge:"center"});
			this._table = table;
			return table;
		},
		_setDataRouteTable: function (data) {
			this._clearRouteTable();
			try{
				this._routeTableModel.setDataAsMapArray(data, true);
			}catch(e){
			}
		},
		_clearRouteTable: function () {
			try{
				this._routeTableModel.removeRows(0, this._routeTableModel.getRowCount());
			}catch(e){
			}
		},
		_createToolbar: function () {
			var toolbar = new qx.ui.toolbar.ToolBar();
			var buttonRefresh = new qx.ui.toolbar.Button("", "icon/16/actions/view-refresh.png");
			buttonRefresh.addListener("execute", function () {
				this._refreshDefinitions();
				this.facade.raiseEvent({
					type: ms123.processexplorer.Config.EVENT_CAMELROUTESDEPLOYMENT_CHANGED
				});
			}, this);
			toolbar.setSpacing(5);
			toolbar._add(buttonRefresh);
			return toolbar;
		},
		_refreshDefinitions:function(){
			this._refreshContextModel();
			this._clearRouteTable();
			this._contextSelectionChanged();
			if( this._vis){
				this._vis.setData(null);
			}
		},
		_getContextList: function () {
			var result = null;
			try {
				result = ms123.util.Remote.rpcSync("camel:getContextNames", {
					namespace: this._namespace
				});
			} catch (e) {
				ms123.form.Dialog.alert("CamelDefinitions._getContextNames:" + e);
				return;
			}
			return result;
		},
		_getRouteInfoList: function (contextKey) {
			var result = null;
			try {
				result = ms123.util.Remote.rpcSync("camel:getRouteInfoList", {
					contextKey: contextKey
				});
			} catch (e) {
				ms123.form.Dialog.alert("CamelDefinitions._getRouteInfoList:" + e);
				return;
			}
			return result;
		},
		_getRouteVisGraph: function (contextKey,routeId) {
			var result = null;
			try {
				result = ms123.util.Remote.rpcSync("camel:getRouteVisGraph", {
					contextKey: contextKey,
					routeId: routeId
				});
			} catch (e) {
				ms123.form.Dialog.alert("CamelDefinitions._getRouteVisGraph:" + e);
				return;
			}
			return result;
		},
		_handleEvent:function(){
			this._refreshDefinitions();
		}
	}
});
