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

qx.Class.define("ms123.processexplorer.plugins.CamelHistory", {
	extend: qx.ui.container.Composite,
	include: [qx.locale.MTranslation],

	/**
	 * Constructor
	 */
	construct: function (facade) {
		this.base(arguments);
		this.facade = facade;
		this.namespace = facade.storeDesc.getNamespace();
		this.setLayout(new qx.ui.layout.Dock());

		this.toolbar = this._createSearchPanelRouteInstances();
		this.add(this.toolbar, {
			edge: "north"
		});

		this._startTimeFrom = null;
		this._startTimeTo = null;

		this.facade.registerOnEvent(ms123.processexplorer.Config.EVENT_CAMELROUTEDEFINITION_CHANGED, this._handleEvent.bind(this));
		this.facade.registerOnEvent(ms123.processexplorer.Config.EVENT_CAMELROUTESDEPLOYMENT_CHANGED, this._handleEventDeplomentChanged.bind(this));
		this.setEnabled(false);
	},

	/**
	 * ****************************************************************************
	 * MEMBERS
	 * ****************************************************************************
	 */
	members: {
		_init:function(){
			if( this._splitPane){
				this.remove(this._splitPane);
				this._splitPane.dispose();
			}
			this._processState = 'all';

			var tableInstances = this._createRouteInstancesTable();
			var tableInstance = this._createRouteInstanceTable();

			
			var details = this._createDetailsArea();
			var splitpane = new ms123.processexplorer.plugins.Split3(tableInstances,tableInstance, details);
			//var splitpane = this._split(tableInstances, tableInstance);
			this.add(splitpane, {
				edge: "center"
			});
			this._splitPane=splitpane;
		},
		_createSearchPanelRouteInstances:function(){
			var formData = {
				"startTimeFrom": {
					'type': "DateField",
					'label': this.tr("processexplorer.history.started_from"),
					'position':"1,0",
					'value': null
				},
				"startTimeTo": {
					'type': "DateField",
					'label': this.tr("processexplorer.history.started_to"),
					'position':"1,1",
					'value': null
				}
			};

			var self = this;
			var form = new ms123.form.Form({
				"tabs": [{
					id: "tab1",
					layout: "double"
				}],
				"useScroll": false,
				"formData": formData,
				"buttons": [],
				"inWindow": false,
				"callback": function (m, v) {
					console.error("callback:"+m+"/"+v);
				},
				"context": self
			});
			this._searchForm = form;
			this.add(form, {
				edge: "north"
			});
			var m = form.getModel();
			form.setData({processState:'all'});
			this._formInstance = form;
			m.addListener("changeBubble", this.__changeListenerInstances, this);
			return form;
		},
		_getRouteInstances: function () {
			if (!this._camelRouteDefinition) return;
			var completed = (function (data) {
				console.log("Data:"+JSON.stringify(data,null,2));
				this._tableModelRouteInstances.removeRows(0, this._tableModelRouteInstances.getRowCount());
				for (var row = 0; row < data.length; row++) {
					var rmap = data[row];
					rmap._startTime = rmap.startTime;
					rmap._endTime = rmap.endTime;
					rmap.startTime = this._formatTime(rmap.startTime);
					rmap.endTime = this._formatTime(rmap.endTime);
					rmap.duration = rmap._endTime - rmap._startTime;
					this._tableModelRouteInstances.addRowsAsMapArray([rmap], null, true);
				}
			}).bind(this);

			var result = null;
			try {
				var s = this._select;
				result = ms123.util.Remote.rpcSync("history:getRouteInstances", {
					contextKey: this._camelContextKey,
					routeId: this._camelRouteDefinition.id
				});
				completed.call(this, result);
			} catch (e) {
				console.log(e.stack);
				ms123.form.Dialog.alert("History._getRouteInstances:" + e);
				return;
			}
		},

		_checkParse:function(s){
			try{
				return JSON.parse(s);
			}catch(e){
				return "";
			}
		},
		_getRouteInstance: function (contextKey, routeId, exchangeId) {
			var completed = (function (data) {
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
			}).bind(this);

			try {
				var result = ms123.util.Remote.rpcSync("history:getRouteInstance", {
					contextKey: contextKey,
					routeId: routeId,
					exchangeId: exchangeId
				});
				completed.call(this, result);
			} catch (e) {
				console.log(e.stack);
				ms123.form.Dialog.alert("History._getRouteInstance:" + e);
				return;
			}
		},

		_createRouteInstancesTable: function () {
			var colIds = new Array();
			var colHds = new Array();
			var colWidth = new Array();
			colIds.push("exchangeId");
			colWidth.push("1*");
			colHds.push(this.tr("processexplorer.history.id"));

			colWidth.push(30);
			colIds.push("status");
			colHds.push(this.tr("processexplorer.history.status"));

			colWidth.push(120);
			colIds.push("startTime");
			colHds.push(this.tr("processexplorer.history.starttime"));

			colWidth.push(120);
			colIds.push("endTime");
			colHds.push(this.tr("processexplorer.history.endtime"));

			colWidth.push(50);
			colIds.push("duration");
			colHds.push(this.tr("processexplorer.history.duration"));

			//var tableModel = this._createTableModelInstances();
			var tableModel = new qx.ui.table.model.Simple();
			this._tableModelRouteInstances = tableModel;
			tableModel.setColumns(colHds, colIds);
			var customMap = {
				tableColumnModel: function (obj) {
					return new qx.ui.table.columnmodel.Resize(obj);
				}
			};
			var table = new qx.ui.table.Table(tableModel, customMap);
			table.highlightFocusedRow(false);
			table.setShowCellFocusIndicator(false);
			this._tableInstance = table;
			var tcm = table.getTableColumnModel();

			table.addListener("cellTap", function (e) {
				var colnum = table.getFocusedColumn();
				var rownum = table.getFocusedRow();
				//if( colnum != 2 ) return;
				var map = tableModel.getRowDataAsMap(rownum);
				//if( !(map.status == "error" || map.status=="notstartet")) return;
				/*var msg = map.logEntry.msg;
				this.facade.raiseEvent({
					type: ms123.processexplorer.Config.EVENT_SHOWDETAILS,
					name: "History",
					value: msg
				});*/
			}, this, false);

			tcm.setDataCellRenderer(1, new ms123.processexplorer.plugins.ImageCellRenderer());
			table.getTableModel().setColumnEditable(1, false);

			colWidth.each((function (w, index) {
				var resizeBehavior = tcm.getBehavior();
				resizeBehavior.setWidth(index, w);

			}).bind(this));
			table.setStatusBarVisible(false);
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
				this._getRouteInstance(this._camelContextKey, this._camelRouteDefinition.id, map.exchangeId);
			}, this);

			return table;
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
				this.facade.vis.selectNode( map.msg.resourceId);
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
		_escapeHTML:function(html) {
			var escape = document.createElement('textarea');
			escape.innerHTML = html;
			return escape.innerHTML;
		},
		_handleEventDeplomentChanged: function (e) {
			if( this._splitPane){
				this.remove(this._splitPane);
				this._splitPane.dispose();
				this._splitPane=null;
			}
			this.setEnabled(false);
		},
		_handleEvent: function (e) {
			if (e && e.camelRouteDefinition) {
				this._camelRouteDefinition = e.camelRouteDefinition;
				this._camelContextKey = e.camelContextKey;
				this._select = e.select;
			}
			console.log("_handleEvent:"+JSON.stringify(this._camelRouteDefinition,null,2));
			this._init();
			this._formInstance.setData({processState:'all',startTimeFrom:null,startTimeTo:null});
			this._getRouteInstances();
			this.setEnabled(true);
		}
	}
});
