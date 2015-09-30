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
	@ignore(Hash)
	@ignore(Clazz)
	@asset(qx/icon/${qx.icontheme}/16/actions/*)
*/

qx.Class.define("ms123.processexplorer.plugins.CamelHistoryInstance", {
	extend: qx.ui.container.Composite,
	include: [qx.locale.MTranslation],

	/**
	 * Constructor
	 */
	construct: function (facade) {
		this.base(arguments);
		this.facade = facade;
		this.setLayout(new qx.ui.layout.Dock());

		var tableInstance = this._createRouteInstanceTable();
		var details = this._createDetailsArea();
		var splitpane = this._splitpaneInstance(tableInstance, details);
		this.add(splitpane, {
			edge: "center"
		});
	},

	/**
	 * ****************************************************************************
	 * MEMBERS
	 * ****************************************************************************
	 */
	members: {
		_splitpaneInstance:function(tableInstance, details){
			 var bottomSplitPane = new qx.ui.splitpane.Pane("vertical").set({decorator: null}); 

			 // width center <-> bottom -> 1/2 <-> 1/2 
			 bottomSplitPane.add(tableInstance, 1); 
			 bottomSplitPane.add(details, 1); 
			return bottomSplitPane;
		},
		getRouteInstance: function (contextKey, routeId, exchangeId) {
			var completed = (function (data) {
				if( data == null){
					console.log("No instance found for:", contextKey);
					return;
				}
				this.setRouteInstanceData(data);
			}).bind(this);

			try {
				console.log("arguments:",arguments);
				if( arguments.length==1){
					var result = ms123.util.Remote.rpcSync("history:getRouteInstanceByActivitiId", {
						activitiId: arguments[0]
					});
				}else{
					var result = ms123.util.Remote.rpcSync("history:getRouteInstance", {
						contextKey: contextKey,
						routeId: routeId,
						exchangeId: exchangeId
					});
				}
				completed.call(this, result);
			} catch (e) {
				console.log(e.stack);
				ms123.form.Dialog.alert("History._getRouteInstance:" + e);
				return;
			}
		},
		setRouteInstanceData:function(data){
			console.log("Data:",JSON.stringify(data,null,2));
			this._tableModelRouteInstance.removeRows(0, this._tableModelRouteInstance.getRowCount());
			for( var i =0; i< data.length;i++){
				var rmap = data[i];
				rmap.time = this._formatTime(rmap.time);
				rmap.status = rmap.hint;
				rmap.msg = this._checkParse(rmap.msg);
				rmap.msg.properties = this._checkParse(rmap.msg.properties);
				rmap.msg.headers = this._checkParse(rmap.msg.headers);
				rmap.from = rmap.msg.previousNode != null ? rmap.msg.previousNode : rmap.msg.fromEndpointUri;
				rmap.to = rmap.msg.node;
				rmap.direction = rmap.msg.direction;
				this._tableModelRouteInstance.addRowsAsMapArray([rmap], null, true);
			}
		},
		_createRouteInstanceTable: function (data) {
			data = data || [];
			var colIds = new Array();
			var colHds = new Array();
			var colWidth = new Array();

			colIds.push("status");
			colHds.push(this.tr("processexplorer.history.status"));
			colWidth.push(30);

			colIds.push("to");
			colHds.push(this.tr("processexplorer.node"));
			colWidth.push("1*");

			colIds.push("direction");
			colHds.push("");
			colWidth.push(50);

			colIds.push("time");
			colHds.push(this.tr("processexplorer.time"));
			colWidth.push(120);

			var tableModel = new qx.ui.table.model.Simple();
			tableModel.setColumns(colHds, colIds);
			this._tableModelRouteInstance = tableModel;
			var customMap = {
				tableColumnModel: function (obj) {
					return new qx.ui.table.columnmodel.Resize(obj);
				}
			};
			var table = new qx.ui.table.Table(tableModel, customMap);
			var tcm = table.getTableColumnModel();

			tcm.setDataCellRenderer(0, new ms123.processexplorer.plugins.ImageCellRenderer());
			table.getTableModel().setColumnEditable(0, false);


			colWidth.each((function (w, index) {
				var resizeBehavior = tcm.getBehavior();
				resizeBehavior.setWidth(index, w);

			}).bind(this));
			table.setStatusBarVisible(false);
			for(var i=0; i < data.length;i++){
				data[i].status = data[i].hint=="ok" ? "finished": "error";
				data[i].time = this._formatTime(data[i].time);
			}
			tableModel.setDataAsMapArray(data, true);

			var selModel = table.getSelectionModel();
			selModel.setSelectionMode(qx.ui.table.selection.Model.SINGLE_SELECTION);
			selModel.addListener("changeSelection", function (e) {
				var index = selModel.getLeadSelectionIndex();
				if( index<0) return;
				var map = tableModel.getRowDataAsMap(index);
				var count = selModel.getSelectedCount();
				if (count == 0) {
					return;
				}
				if( this.facade){
					this.facade.vis.selectNode( map.msg.resourceId);
				}
				this._setDetails(map.msg.headers, map.msg.properties, map.msg.body, map.msg.outBody, map.msg.causedByException );
			}, this);


			return table;
		},

		_createDetailsArea: function () {
			var detailsTabs = new qx.ui.tabview.TabView().set({
				contentPadding: 0,
				minHeight: 150
			});

			var page1 = new qx.ui.tabview.Page(this.tr("processexplorer.details_headers"), "icon/16/actions/help-faq.png").set({
				showCloseButton: false
			});
			page1.setLayout(new qx.ui.layout.Grow());
			detailsTabs.add(page1, {
				edge: 0
			});
			this._tableDetailsHeaders = this._createDetailsHeadersTable();
			page1.add(this._tableDetailsHeaders);

			var page2 = new qx.ui.tabview.Page(this.tr("processexplorer.details_properties"), "icon/16/actions/help-faq.png").set({
				showCloseButton: false
			});
			page2.setLayout(new qx.ui.layout.Grow());
			detailsTabs.add(page2, {
				edge: 0
			});
			this._tableDetailsProperties = this._createDetailsPropertiesTable();
			page2.add(this._tableDetailsProperties);

			var page3 = new qx.ui.tabview.Page(this.tr("processexplorer.details_body"), "icon/16/actions/help-faq.png").set({
				showCloseButton: false
			});
			page3.setLayout(new qx.ui.layout.Grow());
			detailsTabs.add(page3, {
				edge: 0
			});
			this._viewDetailsBody = this._createViewDetailsBody();
			page3.add(this._viewDetailsBody);

			var page4 = new qx.ui.tabview.Page(this.tr("processexplorer.details_outbody"), "icon/16/actions/help-faq.png").set({
				showCloseButton: false
			});
			page4.setLayout(new qx.ui.layout.Grow());
			detailsTabs.add(page4, {
				edge: 0
			});
			this._viewDetailsOutBody = this._createViewDetailsBody();
			page4.add(this._viewDetailsOutBody);

			var page5 = new qx.ui.tabview.Page(this.tr("processexplorer.details_exception"), "resource/ms123/camel.png").set({
				showCloseButton: false
			});
			page5.setLayout(new qx.ui.layout.Grow());
			detailsTabs.add(page5, {
				edge: 0
			});
			this._viewDetailsException = this._createViewDetailsException();
			page5.add(this._viewDetailsException);

			detailsTabs.setSelection([page1]);
			this._detailsTabs = detailsTabs;
			return detailsTabs;
		},
		_createDetailsHeadersTable: function () {
			var colIds = new Array();
			var colHds = new Array();
			var colWidth = new Array();

			colIds.push("name");
			colHds.push(this.tr("processexplorer.name"));
			colWidth.push(200);

			colIds.push("value");
			colHds.push(this.tr("processexplorer.value"));
			colWidth.push("1*");

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
			this._tableModelDetailsHeaders = tableModel;
			return table;
		},

		_createDetailsPropertiesTable: function () {
			var colIds = new Array();
			var colHds = new Array();
			var colWidth = new Array();

			colIds.push("name");
			colHds.push(this.tr("processexplorer.name"));
			colWidth.push(200);

			colIds.push("value");
			colHds.push(this.tr("processexplorer.value"));
			colWidth.push("1*");

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
			this._tableModelDetailsProperties = tableModel;
			return table;
		},
		_createViewDetailsException:function(){
      var msgArea = new qx.ui.form.TextArea();
			msgArea.setFont(qx.bom.Font.fromString("Mono, 9px")); 
			msgArea.setReadOnly(true);
			return msgArea;
		},
		_createViewDetailsBody:function(){
      var msgArea = new qx.ui.form.TextArea();
			msgArea.setFont(qx.bom.Font.fromString("Mono, 9px")); 
			msgArea.setReadOnly(true);
			return msgArea;
		},

		_formatTime:function(time){
			var m = qx.locale.Manager.getInstance();
			var lang = m.getLanguage();
			var df = new qx.util.format.DateFormat("MM-dd-yyyy HH:mm:ss");
			if (lang == "de") {
				df = new qx.util.format.DateFormat("dd.MM.yyyy HH:mm:ss");
			}
			return qx.bom.String.escape(df.format(new Date(time)));
		},
		_setDetails:function(dataHeaders,dataProperties, body, outBody, exception){
			var headersArray = Object.keys(dataHeaders).map(function(key) {
					var value = dataHeaders[key];
					if( (typeof value == "object") && (value !== null) ){
						value = JSON.stringify(value);
					}
					return {"name" : key, "value" : value }
			})
			var propertiesArray = Object.keys(dataProperties).map(function(key) {
					var value = dataProperties[key];
					if( (typeof value == "object") && (value !== null) ){
						value = JSON.stringify(value);
					}
					return {"name" : key, "value" : value }
			})
			this._tableModelDetailsHeaders.setDataAsMapArray(headersArray, true);
			this._tableModelDetailsProperties.setDataAsMapArray(propertiesArray, true);
			this._viewDetailsException.setValue( exception );
			this._viewDetailsBody.setValue( body );
			this._viewDetailsOutBody.setValue( outBody );
		},
		_clearDetails:function(){
			this._tableDetailsHeaders.removeRows(0, this._tableModelDetailsHeaders.getRowCount());
			this._tableDetailsProperties.removeRows(0, this._tableModelDetailsProperties.getRowCount());
		},
		_checkParse:function(s){
			try{
				return JSON.parse(s);
			}catch(e){
				return "";
			}
		}
	}
});
