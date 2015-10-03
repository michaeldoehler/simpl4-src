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

qx.Class.define("ms123.processexplorer.plugins.ProcessHistory", {
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

		this.toolbar = this._createSearchPanelInstances();
		this.add(this.toolbar, {
			edge: "north"
		});

		this._startTimeFrom = null;
		this._startTimeTo = null;

		this.facade.registerOnEvent(ms123.processexplorer.Config.EVENT_PROCESSDEFINITION_CHANGED, this._handleEvent.bind(this));
		this.facade.registerOnEvent(ms123.processexplorer.Config.EVENT_PROCESSSTARTED, this._handleEvent.bind(this));
		this.facade.registerOnEvent(ms123.processexplorer.Config.EVENT_PROCESSDEPLOYMENT_CHANGED, this._handleEventDeplomentChanged.bind(this));
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

			var table = this._createInstanceTable();
			var tabs = this._createDetailsArea();

			var splitpane = this._split(table, tabs);
			this.add(splitpane, {
				edge: "center"
			});
			this._splitPane=splitpane;
			this._currentDiagram = null;
		},
		_getHistory: function (what) {
			if (!this._processDefinition) return;
			var completed = (function (map) {
				this._tableModelInstances.removeRows(0, this._tableModelInstances.getRowCount());
				var data = map["data"];
				this._mergeExceptions(data);
				this._addStartExceptions(data);
				for (var row = 0; row < data.length; row++) {
					var rmap = data[row];
					this._tableModelInstances.addRowsAsMapArray([rmap], null, true);
				}
			}).bind(this);

			var result = null;
			try {
				var s = this._select;
				result = ms123.util.Remote.rpcSync("activiti:getProcessInstances", {
					processDefinitionKey: s=='key' ? this._processDefinition.key:null,
					processDefinitionId: s=='id' ? this._processDefinition.id:null,
					unfinished: this.__unfinished,
					listParams: {
						sort: "startTime",
						size: 1000,
						order: "desc"
					}
				});
				completed.call(this, result);
			} catch (e) {
				ms123.form.Dialog.alert("History._getHistory:" + e);
				return;
			}
		},

		_mergeExceptions:function(rowList){
			var keyList = [];
			for(var i=0; i< rowList.length;i++){
				var row = rowList[i];
				var key =  this.namespace+"/"+this._processDefinition.name+"/"+row.id
				keyList.push(key);
			}
			var complete = (function (content) {
				if (content) {
					for(var i=0; i< rowList.length;i++){
						var row = rowList[i];
						var key =  this.namespace+"/"+this._processDefinition.name+"/"+row.id
						var logList = content[key];
						row.status= row.endTime ? "finished" : "notfinished";
						if( logList){
							console.log("key has exception:"+key);
							row.logEntry = logList[0];
							row.status="error";
						}
					}
				}
			}).bind(this);
			try {
				var result = ms123.util.Remote.rpcSync("history:getHistoryByKeyList", {
					keyList: keyList,
					type : "activiti/job/exception"
				});
				complete.call(this, result);
			} catch (e) {
				ms123.form.Dialog.alert("_mergeExceptions.getHistoryByKeyList:" + e);
				return;
			}
		},
		_addStartExceptions:function(rowList){
			var complete = (function (content) {
				if (content) {
					for(var i=0; i< content.length;i++){
						var logEntry = content[i];
						var msg = logEntry.msg;
						var row = {};
						var hint = JSON.parse(logEntry.hint);
						row.startTime=this._formatTime(logEntry.time);
						row._startTime=logEntry.time;
						row.startUserId=logEntry.hint.startUserId;
						row.status="notstartet";
						row.id="Starterror";
						row.processDefinitionId=hint.processDefinitionId;
						row.logEntry = logEntry;
						rowList.push(row);
					}
					rowList.sort(function (a, b) {
						return parseInt(b._startTime) - parseInt(a._startTime);
					});
				}
			}).bind(this);
			try {
				var result = ms123.util.Remote.rpcSync("history:getHistory", {
					key: this.namespace+"/"+this._processDefinition.id,
					type : "activiti/startprocess/exception"
				});
				complete.call(this, result);
			} catch (e) {
				ms123.form.Dialog.alert("_addStartExceptions.getHistory:" + e);
				return;
			}
		},
		_getProcessDetails: function (id) {
			if(!id || id.toLowerCase() == "starterror" ) return;
			var completed = (function (content) {
				var containerInfo = this.__getContainer( this._detailsInfoPage, "grid");

				var row = 0;
				this._addLine(containerInfo, "startUserId", content.startUserId, row++);
				this._addLine(containerInfo, "startTime", this._formatTime(content.startTime), row++);
				this._addLine(containerInfo, "endTime", this._formatTime(content.endTime), row++);
				this._addLine(containerInfo, "duration", content.duration, row++);
				this._addLine(containerInfo, "completed", content.completed, row++);
				this._addLine(containerInfo, "startActivityId", content.startActivityId, row++);
				this._addLine(containerInfo, "endActivityId", content.endActivityId, row++);
				this._addLine(containerInfo, "processInstanceId", content.processInstanceId, row++);
				this._addLine(containerInfo, "processDefinitionId", content.processDefinitionId, row++);
				this._addLine(containerInfo, "businessKey", content.businessKey, row++);


				var containerActivity = this.__getContainer( this._detailsActivityPage,"dock");
				for(var i = 0; i < content.activities.length;i++){
					var a = content.activities[i];
					a.startTime = this._formatTime(a.startTime);
					a.endTime = this._formatTime(a.endTime);
				}
				var table = this._createActivityTable(content.activities);
				containerActivity.add(table, {
					edge:"center"
				});


				if( content.historyVariables ){
					content.variables = content.historyVariables;
				}
				if (content.variables) {
					for(var i = 0; i < content.variables.length;i++){
						var a = content.variables[i];
						a.time = this._formatTime(a.time);
					}
					var containerVariables = this.__getContainer( this._detailsVariablesPage,"dock");
					var panel = this._createSearchPanelVariables();
					containerVariables.add(panel, {
						edge:"north"
					});
					var table = this._createVariablesTable(content.variables);
					containerVariables.add(table, {
						edge:"center"
					});
				}
			}).bind(this);

			try {
				var result = ms123.util.Remote.rpcSync("activiti:getProcessInstance", {
					processInstanceId: id
				});
				completed.call(this, result);
			} catch (e) {
				console.log(e.stack);
				ms123.form.Dialog.alert("History._getProcessDetails:" + e);
				return;
			}

			var completedCamel = (function (content) {
				if (content) {
					var containerCamel = this.__getContainer( this._detailsCamelPage,"dock");
					var table = this._createCamelTable(content);
					containerCamel.add(table, {
						edge:"center"
					});
				}
			}).bind(this);
			try {
				result = ms123.util.Remote.rpcSync("history:getHistory", {
					key: this.namespace+"/"+this._processDefinition.name+"/"+id,
					type: "camel/history"
				});
				completedCamel.call(this, result);
			} catch (e) {
				ms123.form.Dialog.alert("Camel._camellog:" + e);
				return;
			}

		},

		_getDiagram: function (id) {
			if(!id || id.toLowerCase() == "starterror" ) return;
			var source = ms123.util.Remote.rpcSync("activiti:getInstanceDiagram", { processInstanceId:id });
			var image = new qx.ui.basic.Image(source);
			//image.setScale(true);
			//image.setWidth(500);
			image.setAllowShrinkY(false);
			image.setAllowGrowY(false);
			this._currentDiagram = image;
			this._diagramPage.add(image);
			this._diagramPage.show();
		},
		_addLine: function (container, key, value, row) {
			var k = new qx.ui.basic.Label(key);
			var v = new qx.ui.basic.Label(value);
			k.setSelectable(true);
			v.setSelectable(true);
			container.add(k, {
				row: row,
				column: 0
			});
			container.add(v, {
				row: row,
				column: 1
			});
		},
		_createDetailsArea: function () {
			var detailsTabs = new qx.ui.tabview.TabView().set({
				contentPadding: 0,
				minHeight: 150
			});

			var page1 = new qx.ui.tabview.Page(this.tr("processexplorer.details_info"), "icon/16/actions/help-faq.png").set({
				showCloseButton: false
			});
			page1.setLayout(new qx.ui.layout.Grow());
			detailsTabs.add(page1, {
				edge: 0
			});
			this._detailsInfoPage = page1;

			var page2 = new qx.ui.tabview.Page(this.tr("processexplorer.details_activity"), "icon/16/actions/help-faq.png").set({
				showCloseButton: false
			});
			page2.setLayout(new qx.ui.layout.Grow());
			detailsTabs.add(page2, {
				edge: 0
			});
			this._detailsActivityPage = page2;

			var page3 = new qx.ui.tabview.Page(this.tr("processexplorer.details_variables"), "icon/16/actions/help-faq.png").set({
				showCloseButton: false
			});
			page3.setLayout(new qx.ui.layout.Grow());
			detailsTabs.add(page3, {
				edge: 0
			});
			this._detailsVariablesPage = page3;

			var page4 = new qx.ui.tabview.Page(this.tr("processexplorer.details_camel"), "resource/ms123/camel.png").set({
				showCloseButton: false
			});
			page4.setLayout(new qx.ui.layout.Grow());
			detailsTabs.add(page4, {
				edge: 0
			});
			this._detailsCamelPage = page4;

			var page5 = new qx.ui.tabview.Page(this.tr("processexplorer.instance_diagram"), "icon/16/actions/format-justify-fill.png").set({
				showCloseButton: false
			});
			page5.setLayout(new qx.ui.layout.VBox());
			var scroll = new qx.ui.container.Scroll();
			page5.add(scroll, {
				flex: 1
			});
			this._diagramPage = scroll;
			detailsTabs.add(page5, {
				edge: 0
			});

			detailsTabs.setSelection([this._detailsActivityPage]);
			this._detailsTabs = detailsTabs;
			return detailsTabs;
		},
		_createInstanceTable: function () {
			var colIds = new Array();
			var colHds = new Array();
			var colWidth = new Array();
			colIds.push("id");
			colWidth.push(60);
			colHds.push(this.tr("processexplorer.history.id"));

			colWidth.push("2*");
			colIds.push("processDefinitionId");
			colHds.push(this.tr("processexplorer.history.processDefinitionId"));

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

			colWidth.push(60);
			colIds.push("startUserId");
			colHds.push(this.tr("processexplorer.history.startUserId"));

			//colWidth.push("1*");
			//colIds.push("businessKey");
			//colHds.push(this.tr("processexplorer.history.businesskey"));

			var tableModel = this._createTableModelInstances();
			this._tableModelInstances = tableModel;
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
				if( colnum != 2 ) return;
				var map = tableModel.getRowDataAsMap(rownum);
				if( !(map.status == "error" || map.status=="notstartet")) return;
				var msg = map.logEntry.msg;
				this.facade.raiseEvent({
					type: ms123.processexplorer.Config.EVENT_SHOWDETAILS,
					name: "History",
					value: msg
				});
			}, this, false);

			tcm.setDataCellRenderer(2, new ms123.processexplorer.plugins.ImageCellRenderer());
			table.getTableModel().setColumnEditable(2, false);

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
				var id = map.id;
				this._getProcessDetails(id);
				this._getDiagram(id);
			}, this);

			return table;
		},
		_createActivityTable: function (data) {
			var colIds = new Array();
			var colHds = new Array();
			var colWidth = new Array();

			colIds.push("id");
			colHds.push(this.tr("processexplorer.id"));
			colWidth.push(45);

			colIds.push("activityName");
			colHds.push(this.tr("processexplorer.activityName"));
			colWidth.push("7*");

			colIds.push("activityType");
			colHds.push(this.tr("processexplorer.activityType"));
			colWidth.push("6*");

			colIds.push("assignee");
			colHds.push(this.tr("processexplorer.assignee"));
			colWidth.push("4*");

			colIds.push("completed");
			colHds.push(this.tr("processexplorer.completed"));
			colWidth.push(40);

			colIds.push("duration");
			colHds.push(this.tr("processexplorer.duration"));
			colWidth.push(50);

			colIds.push("startTime");
			colHds.push(this.tr("processexplorer.startTime"));
			colWidth.push(125);

			colIds.push("endTime");
			colHds.push(this.tr("processexplorer.endTime"));
			colWidth.push(125);


			var tableModel = new qx.ui.table.model.Simple();
			tableModel.setColumns(colHds, colIds);
			var customMap = {
				tableColumnModel: function (obj) {
					return new qx.ui.table.columnmodel.Resize(obj);
				}
			};
			var table = new qx.ui.table.Table(tableModel, customMap);
			table.addListener("cellTap", function (e) {
				var colnum = table.getFocusedColumn();
				var rownum = table.getFocusedRow();
				var map = tableModel.getRowDataAsMap(rownum);
				if( "serviceTask" == map.activityType ){
					var key =  this.namespace+"/"+this._processDefinition.name+"/"+map.executionId +"/"+map.activityId;
					var data = this.__getRouteInstance(key);
					if( data == null || data.length==0){
						return;
					}
					this.facade.raiseEvent({
						type: ms123.processexplorer.Config.EVENT_SHOW_ROUTEINSTANCE,
						name: "Route",
						value:data 
					});
				}
			}, this, false);
			var tcm = table.getTableColumnModel();
			colWidth.each((function (w, index) {
				var resizeBehavior = tcm.getBehavior();
				resizeBehavior.setWidth(index, w);

			}).bind(this));
			table.setStatusBarVisible(false);
			tableModel.setDataAsMapArray(data, true);
			return table;
		},
		__getRouteInstance: function (activitiId) {
			var result = ms123.util.Remote.rpcSync("history:getRouteInstanceByActivitiId", {
				activitiId: activitiId
			});
			return result;
		},
		_createCamelTable: function (data) {
			var colIds = new Array();
			var colHds = new Array();
			var colWidth = new Array();

			colIds.push("time");
			colHds.push(this.tr("processexplorer.time"));
			colWidth.push("30%");
			colIds.push("key");
			colHds.push(this.tr("processexplorer.id"));
			colWidth.push("60%");

			colIds.push("status");
			colHds.push(this.tr("processexplorer.history.status"));
			colWidth.push("10%");


			/*colIds.push("routeId");
			colHds.push(this.tr("processexplorer.routeId"));
			colWidth.push("15%");

			colIds.push("fromEndpointUri");
			colHds.push(this.tr("processexplorer.fromEndpointUri"));
			colWidth.push("15%");

			colIds.push("body");
			colHds.push(this.tr("processexplorer.body"));
			colWidth.push("20%");

			colIds.push("headers");
			colHds.push(this.tr("processexplorer.headers"));
			colWidth.push("20%");

			colIds.push("properties");
			colHds.push(this.tr("processexplorer.properties"));
			colWidth.push("15%");

			colIds.push("toNode");
			colHds.push(this.tr("processexplorer.toNode"));
			colWidth.push("15%");*/



			var tableModel = new qx.ui.table.model.Simple();
			tableModel.setColumns(colHds, colIds);
			var customMap = {
				tableColumnModel: function (obj) {
					return new qx.ui.table.columnmodel.Resize(obj);
				}
			};
			var table = new qx.ui.table.Table(tableModel, customMap);
			table.addListener("cellTap", function (e) {
				var colnum = table.getFocusedColumn();
				var rownum = table.getFocusedRow();
				var map = tableModel.getRowDataAsMap(rownum);
				//var value = qx.lang.Json.stringify(qx.lang.Json.parse(map.body), null, 4);
				this.facade.raiseEvent({
					type: ms123.processexplorer.Config.EVENT_SHOWDETAILS,
					name: "History",
					value: map.msg
				});
			}, this, false);
			var tcm = table.getTableColumnModel();

			tcm.setDataCellRenderer(2, new ms123.processexplorer.plugins.ImageCellRenderer());
			table.getTableModel().setColumnEditable(2, false);


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
			return table;
		},
		_createVariablesTable: function (data) {
			var colIds = new Array();
			var colHds = new Array();
			var colWidth = new Array();
			colIds.push("variableName");
			colHds.push(this.tr("processexplorer.variableName"));
			colWidth.push("20%");

			colIds.push("variableValue");
			colHds.push(this.tr("processexplorer.variableValue"));
			colWidth.push("28%");

			colIds.push("activityName");
			colHds.push(this.tr("processexplorer.activityName"));
			colWidth.push("22%");

			colIds.push("variableType");
			colHds.push(this.tr("processexplorer.variableType"));
			colWidth.push("10%");

			colIds.push("time");
			colHds.push(this.tr("processexplorer.time"));
			colWidth.push("19%");

			//colIds.push("revision");
			//colHds.push(this.tr("processexplorer.revision"));
			//colWidth.push("4%");

			var tableModel = this._createTableModelVariables();
			this._tableModelVariables = tableModel;
			tableModel.setColumns(colHds, colIds);
			var customMap = {
				tableColumnModel: function (obj) {
					return new qx.ui.table.columnmodel.Resize(obj);
				}
			};
			var variables = [];
			var variablesNames = [];
			var table = new qx.ui.table.Table(tableModel, customMap);
			table.addListener("cellTap", function (e) {
				var colnum = table.getFocusedColumn();
				var rownum = table.getFocusedRow();
				var map = tableModel.getRowDataAsMap(rownum);
				var value = qx.lang.Json.stringify(qx.lang.Json.parse(map.variableValue), null, 4);
				this.facade.raiseEvent({
					type: ms123.processexplorer.Config.EVENT_SHOWDETAILS,
					name: map.variableName,
					value: value
				});
			}, this, false);

			var tcm = table.getTableColumnModel();
			colWidth.each((function (w, index) {
				var resizeBehavior = tcm.getBehavior();
				resizeBehavior.setWidth(index, w);

			}).bind(this));
			table.setStatusBarVisible(false);
			var escape = document.createElement('textarea');
			function escapeHTML(html) {
					escape.innerHTML = html;
					return escape.innerHTML;
			}

			for (var i = 0; i < data.length; i++) {
				var row = data[i];
				variablesNames.push(row["variableName"]);
				variables.push(row["variableValue"]);
				row["variableValue"] = escapeHTML(qx.util.Serializer.toJson(row["variableValue"]));
			}
			tableModel.setDataAsMapArray(data, true);
			return table;
		},
		//Helper for VariablenTable-------------------------------------------------------------------
		_createTableModelVariables: function () {
			var tm = new ms123.widgets.smart.model.Default();
			tm.clearSorting();
			var id = 0;
			for (var view in this._viewsVariables) {
				if (view == 'All') {
					this._viewsVariables[view].id = 0;
					continue;
				}
				this._viewsVariables[view].id = ++id;
				tm.addView(this._viewsVariables[view].filters, this, this._viewsVariables[view].conjunction);
			}
			return tm;
		},
		_createSearchPanelVariables: function () {
			var container = new qx.ui.container.Composite(new qx.ui.layout.HBox()).set({
				paddingTop: 2,
				paddingBottom: 2
			});
			container.add(new qx.ui.basic.Label(this.tr("processexplorer.filter") + ":").set({
				textColor: '#4886ce'
			}));

			var sf = new qx.ui.form.TextField();
			sf.addListener('keyup', function (e) {
				this.__searchFilterVariables = sf.getValue().toLowerCase();
				this._tableModelVariables.updateView(this._viewsVariables["SearchAsYouType"].id);
				this._tableModelVariables.setView(this._viewsVariables["SearchAsYouType"].id);
			}, this);
			container.add(sf);
			return container;
		},

		_viewsVariables: {
			"All": {},
			"SearchAsYouType": {
				filters: function (rowdata) {
          var d0 = (rowdata[0] != undefined ) ? rowdata[0].toLowerCase() : "";
          var d1 = (rowdata[1] != undefined ) ? rowdata[1].toLowerCase() : "";
          var d2 = (rowdata[2] != undefined ) ? rowdata[2].toLowerCase() : "";
					return (
									 (d0.indexOf(this.__searchFilterVariables) != -1) || 
									 (d1.indexOf(this.__searchFilterVariables) != -1) || 
									 (d2.indexOf(this.__searchFilterVariables) != -1)
								);
				}
			}
		},
		//Helper for InstanceTable------------------------------------------------------------
		_createTableModelInstances: function () {
			var tm = new ms123.widgets.smart.model.Default();
			var id = 0;
			for (var view in this._viewsInstances) {
				if (view == 'All') {
					this._viewsInstances[view].id = 0;
					continue;
				}
				this._viewsInstances[view].id = ++id;
				tm.addView(this._viewsInstances[view].filters, this, this._viewsInstances[view].conjunction);
			}
			return tm;
		},

		_createSearchPanelInstances:function(){
			var formData = {
				"processState": {
					'type': "SelectBox",
					'label': this.tr("processexplorer.history.state"),
					'options':[
						{value:'unfinished',label:'unfinished'},
						{value:'finished',label:'finished'},
						{value:'all', label:'all'}
					],
					'value': "all"
				},
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
		__changeListenerInstances: function () {
			var m = this._searchForm.getModel();
			var processState = m.get("processState");
			if( processState != this._processState){
				this._processState = processState;
				this._tableModelInstances.updateView(this._viewsInstances["SearchAsYouType"].id);
				this._tableModelInstances.setView(this._viewsInstances["SearchAsYouType"].id);
				this._tableInstance.getSelectionModel().setSelectionInterval(-1,-1);
					this._clearDetails();
			}
			var startTimeFrom = m.get("startTimeFrom");
			if( startTimeFrom != this._startTimeFrom){
				this._startTimeFrom = startTimeFrom;
				this._tableModelInstances.updateView(this._viewsInstances["SearchAsYouType"].id);
				this._tableModelInstances.setView(this._viewsInstances["SearchAsYouType"].id);
			}
			var startTimeTo = m.get("startTimeTo");
			if( startTimeTo != this._startTimeTo){
				this._startTimeTo = startTimeTo;
				this._tableModelInstances.updateView(this._viewsInstances["SearchAsYouType"].id);
				this._tableModelInstances.setView(this._viewsInstances["SearchAsYouType"].id);
			}
		},

		_viewsInstances: {
			"All": {},
			"SearchAsYouType": {
				filters: function (rowdata) {
					var map = rowdata.originalData;
          var endTime = map.endTime;
          var startTime = map._startTime;
					var state = this._processState;
					var unfinished = endTime == null || endTime == '';
					var stateB = state=='all' || (state=='unfinished' && unfinished) || (state=='finished' && !unfinished);
					var startTimeFromB = this._startTimeFrom == null || this._startTimeFrom.getTime() < startTime;
					var startTimeToB = this._startTimeTo == null || ((3600*24*1000)+this._startTimeTo.getTime()) > startTime;;
					return stateB && startTimeFromB && startTimeToB;
				}
			}
		},
		//------------------------------------------------------------------------------------

		__getContainer:function(page, sLayout){
			if (page.hasChildren()) {
				page.removeAll();
			}

			var layout = new qx.ui.layout.Dock();
			if( sLayout == "grid"){
				var layout = new qx.ui.layout.Grid();
				layout.setColumnFlex(1, 1);
				layout.setSpacing(3);
			}
			var container = new qx.ui.container.Composite(layout).set({
				//minWidth: 800
			});

			page.add(container);
			return container;
		},


		_clearDetails:function(){
			if (this._detailsInfoPage.hasChildren()) {
				this._detailsInfoPage.removeAll();
			}
			if (this._detailsActivityPage.hasChildren()) {
				this._detailsActivityPage.removeAll();
			}
			if (this._detailsVariablesPage.hasChildren()) {
				this._detailsVariablesPage.removeAll();
			}
			if (this._detailsCamelPage.hasChildren()) {
				this._detailsCamelPage.removeAll();
			}
			this._diagramPage.add(null);
		},
		_formatTime:function(time){
			var m = qx.locale.Manager.getInstance();
			var lang = m.getLanguage();
			var df = new qx.util.format.DateFormat("MM-dd-yy HH:mm:ss.SSS");
			if (lang == "de") {
				df = new qx.util.format.DateFormat("dd.MM.yy HH:mm:ss.SSS");
			}
			return qx.bom.String.escape(df.format(new Date(time)));
		},
		_split: function (top, bottom) {
			var splitPane = new qx.ui.splitpane.Pane("vertical").set({
				decorator: null
			});

			splitPane.add(top, 1);
			splitPane.add(bottom, 4);
			return splitPane;
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
			if (e && e.processDefinition) {
				this._processDefinition = e.processDefinition;
				this._select = e.select;
			}
			this._init();
			this._formInstance.setData({processState:'all',startTimeFrom:null,startTimeTo:null});
			this._getHistory();
			this.setEnabled(true);
		}
	}
});
