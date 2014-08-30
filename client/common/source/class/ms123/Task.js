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

qx.Class.define("ms123.Task", {
	extend: qx.ui.container.Composite,
	include: qx.locale.MTranslation,


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
		_init: function (context) {
			this._user = ms123.config.ConfigManager.getUserProperties();

			var mainContainer = this;
			mainContainer.set({
				decorator: "main",
				allowGrowX: true,
				allowGrowY: true
			});
			mainContainer.setLayout(new qx.ui.layout.Dock());
			if( context && context.window ){
				context.window.add(mainContainer, {
					edge: 0
				});
			}

			this._table = this._createTable();

			var assignedButton;
			var notAssignedButton;
			var tableCellEvents = function (e) {
				var rmap = this._tableModel.getRowDataAsMap(e.getRow());
				var self = this;
				if (rmap["assignee"] == null) {
					ms123.form.Dialog.confirm(this.tr("tasks.task.claim"), function (result) {
						if (result) {
							self._claimTask(rmap["id"]);
							self._getTasks("assigned");
							assignedButton.setBackgroundColor("lightblue");
							notAssignedButton.setBackgroundColor(null);
						}
					});
				} else {
					var formKey = rmap.formResourceKey;
					this._taskid = rmap.id;

					mainContainer.getApplicationRoot().setGlobalCursor("wait");
					qx.event.Timer.once(function () {
						var pid = rmap["processInstanceId"];
						console.log("pid:" + pid + "/" + Object.toJSON(rmap));
						if (formKey == "dublettenCheck.form") {
							var form = this._getTaskForm(rmap["id"]);
							this._showTaskFormOld(pid, form);
						} else {
							var pc = new ms123.processexplorer.ProcessController({appRoot:mainContainer.getApplicationRoot()});
							rmap.fromTaskList=true;
							pc.showForm(rmap);
							pc.addListener("taskCompleted", function(e){
								console.log("taskCompleted:"+qx.util.Serializer.toJson(e.getData()));
								this._getTasks("assigned");
							},this);
						}
						mainContainer.getApplicationRoot().setGlobalCursor("default");
					}, this, 200);
				}
			};
			this._table.addListener("cellClick", tableCellEvents, this);

			var toolbar = new qx.ui.toolbar.ToolBar();
			notAssignedButton = new qx.ui.toolbar.Button(this.tr("tasks.table.not_assigned"), "icon/22/actions/view-fullscreen.png");
			notAssignedButton.addListener("execute", function () {
				this._getTasks("notassigned");
				notAssignedButton.setBackgroundColor("lightblue");
				assignedButton.setBackgroundColor(null);
			}, this);

			assignedButton = new qx.ui.toolbar.Button(this.tr("tasks.table.assigned"), "icon/22/actions/view-restore.png");
			assignedButton.addListener("execute", function () {
				this._getTasks("assigned");
				assignedButton.setBackgroundColor("lightblue");
				notAssignedButton.setBackgroundColor(null);
			}, this);

			assignedButton.setDecorator(null);
			notAssignedButton.setDecorator(null);

			toolbar.add(assignedButton);
			toolbar.add(notAssignedButton);
			mainContainer.add(toolbar);

			mainContainer.add(toolbar, {
				edge: "north"
			});
			mainContainer.add(this._table, {
				edge: "center"
			});
			assignedButton.setBackgroundColor("lightblue");
			this._getTasks("assigned");
		},
		_createTable: function () {
			var colIds = new Array();
			var colWidth = new Array();
			var colHds = new Array();
			colIds.push("processInstanceId");
			colHds.push(this.tr("tasks.table.processInstanceId"));
			colWidth.push(40);

			colIds.push("processName");
			colHds.push(this.tr("tasks.table.processName"));
			colWidth.push(80);

			colIds.push("processCategory");
			colHds.push(this.tr("tasks.table.processCategory"));
			colWidth.push(60);

			colIds.push("id");
			colHds.push(this.tr("tasks.table.taskid"));
			colWidth.push(40);

			colIds.push("name");
			colHds.push(this.tr("tasks.table.name"));
			colWidth.push(150);

			colIds.push("description");
			colHds.push(this.tr("tasks.table.description"));
			colWidth.push(180);

			colIds.push("createTime");
			colHds.push(this.tr("tasks.table.time"));
			colWidth.push(60);

			//colIds.push("assigned");
			//colHds.push(this.tr("tasks.table.assigned"));
			//colWidth.push(60);

			//colIds.push("action");
			//colHds.push(this.tr("tasks.table.action"));
			//colWidth.push(80);

			this._tableModel = new qx.ui.table.model.Simple();
			this._tableModel.setColumns(colHds, colIds);
			var customMap = {
				tableColumnModel: function (obj) {
					return new qx.ui.table.columnmodel.Resize(obj);
				}
			};
			var table = new qx.ui.table.Table(this._tableModel, customMap);
			table.setStatusBarVisible(false);
			var tcm = table.getTableColumnModel();
			table.getTableModel().setColumnEditable(0, false);
			table.getTableModel().setColumnEditable(1, false);
			table.getTableModel().setColumnEditable(2, false);

			var resizeBehavior = tcm.getBehavior();
			for( var c=0; c < colWidth.length;c++){
				resizeBehavior.setMinWidth(c, colWidth[c]);
//				resizeBehavior.set(c, { width:"1*", minWidth:40, maxWidth:80  });
			}
			return table;
		},
		_getTasks: function (what) {
			var completed = function (map) {
				this._tableModel.removeRows(0, this._tableModel.getRowCount());
				var data = map["data"];
				for (var row = 0; row < data.length; row++) {
					var rmap = data[row];
					if (rmap.assignee == null) {
						rmap["assigned"] = this.tr("tasks.table.not_assigned");
						rmap["action"] = this.tr("tasks.table.claim");
					} else {
						rmap["assigned"] = rmap.assignee;
						rmap["action"] = this.tr("tasks.table.complete");
					}
					this._addRecord(rmap);
				}
			};
			try {
				var	queryParams={ assignee:this._user.userid };
				if (what == "notassigned"){
					queryParams={ candidate:this._user.userid };
				}
			
				var result = ms123.util.Remote.rpcSync("activiti:getTasks", {
					queryParams:queryParams,
					listParams:{
						size:1000
					}
				});
				completed.call(this,result);
			} catch (e) {
				ms123.form.Dialog.alert("Tasks._getTasks:" + e);
				return;
			}
			return result;


		},
		_getTaskList: function (processInstanceId) {
		},
		__eval: function (clazz) {
			var parts = clazz.split("\.");
			var obj = window;
			for (var i = 0; i < parts.length; i++) {
				obj = obj[parts[i]];
			}
			return new obj();
		},
		_showTaskFormOld: function (processInstanceId, formDesc) {
			//var clazz = eval ( "(new "+formDesc.formClass+"())");
			//console.log("_showTaskForm:"+clazz);
			var clazz = this.__eval(formDesc.formClass);
			console.log("_showTaskFormOld:" + clazz);
			var params = {};
			params.processInstanceId = processInstanceId;
			params.callback = this;
			params.formDesc = formDesc;
			params.user = this._user;
			var ret = clazz.init(params);
		},
		_addRecord: function (map) {
			this._tableModel.addRowsAsMapArray([map], null, true);
		},
		_claimTask: function (taskid) {
			var completed = function (e) {
				ms123.form.Dialog.alert(this.tr("tasks.task.claimed"));
			};
			var failed = function (e) {
				ms123.form.Dialog.alert(this.tr("tasks.task.not_claimed") + ":" + e);
			};

			this._getTasks("assigned");
			var result = null;
			try {
				result = ms123.util.Remote.rpcSync("activiti:executeTaskOperation", {
					taskId:taskid,
					operation: "claim"
				});
				completed.call(this,result);
			} catch (e) {
				failed.call(this,e);
				return;
			}
			return result;
		},
		_completeTask: function (processVariables) {
			var completed = function (e) {
				ms123.form.Dialog.alert(this.tr("tasks.task.completed"));
			};
			var failed = function (e) {
				ms123.form.Dialog.alert(this.tr("tasks.task.not_completed") + ":" + e);
			};

			this._getTasks("assigned");
			var result = null;
			try {
				result = ms123.util.Remote.rpcSync("activiti:executeTaskOperation", {
					taskId:this._taskid,
					operation: "complete",
					startParams: processVariables
				});
				completed.call(this,result);
			} catch (e) {
				failed.call(this,e);
				return;
			}
			return result;
		},
		_getTaskForm: function (id) {
			var url = "workflowrest/task/" + id + "/form?format=json"
			var ret = ms123.util.Remote.sendSync(url);
			return ret;
		},
		toString: function () {
			return "_task_";
		}

	}
});
