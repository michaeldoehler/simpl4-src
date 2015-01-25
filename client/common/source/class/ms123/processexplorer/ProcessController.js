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
	* @lint ignoreDeprecated(alert,eval) 
*/
qx.Class.define("ms123.processexplorer.ProcessController", {
  extend: qx.core.Object,
	include: [qx.locale.MTranslation],

	/**
	 * Constructor
	 */
	construct: function (context) {
		this.base(arguments);
		this.userid = context.userid  ? context.userid : ms123.util.Remote._username;
		if( context.title && context.window ){
			context.window.setCaption(this.tr(context.title));
		}
		this.init(context);
	},
	events: { 
		"taskCompleted": "qx.event.type.Data"
	},

	/**
	 * ****************************************************************************
	 * MEMBERS
	 * ****************************************************************************
	 */
	members: {
		init:function(context){
			if( context.appRoot){
				this._formContainer = 	new ms123.processexplorer.FormWindow(context);
			}else{
				this._formContainer = 	new ms123.processexplorer.FormContainer(context);
			}
			console.log("ProcessController:"+context.workflowName+"/"+context.namespace);
			if( context.workflowName && context.namespace){
				this.startByName(context.namespace,context.workflowName,context.parameter);
			}
		},
		startByName: function (namespace, name,parameter) {
			this._namespace = namespace;
			ms123.config.ConfigManager.installMessages(namespace);
			var pc = this._getProcessDefinition(namespace,name);
			if( pc == null){
				ms123.form.Dialog.alert("ProcessController.workflow("+namespace+","+name+") not found");
				return;
			}
			console.log("ProcessController:startByName"+namespace+"/"+name);
			this.start(pc,parameter);
		},
		start: function (processDefinition,parameter) {
			this.processDefinition = processDefinition;
			this.processName = processDefinition.name;
			console.log("Start:"+qx.util.Serializer.toJson(processDefinition));
			var pid = this.processDefinition.id;
			if (true/*pid.match("^sid")*/) {
				console.log("startFormResourceKey:" + this.processDefinition.startFormResourceKey);
				if (this.processDefinition.startFormResourceKey) {
					this.showForm(null);
				} else {
					var processVariables = parameter || {};
					processVariables["processDefinitionId"] = this.processDefinition.id;
					this._completeActivity(processVariables,null);
				}
			}
		},
		showForm: function (task) {
			var formResourceKey = null;
			var taskName = null;
			var processName = null;
			var processCategory = null;
			if( task == null){
				formResourceKey = this.processDefinition.startFormResourceKey;
				processName = this.processName;
			}else{
				formResourceKey = task.formResourceKey;
				if( task.processName) this.processName = task.processName;
				processCategory = task.processCategory;
				processName = this.processName;
				taskName = task.name;
			}
			if( !processCategory){
				processCategory = ms123.StoreDesc.getCurrentNamespace()
			}
			if( formResourceKey==null){
				this._handleExecuteButton(null, null, task, null,null);
				return;
			}
			var data = this._getFormVarAndPath(formResourceKey);
			var formPath = data.formPath;
			var formVar = data.formVar;
			console.warn("{task,process}Name:"+taskName+"/"+processName);
			console.warn("formResourceKey:"+formResourceKey);
			console.warn("formPath:"+formPath);
			console.warn("formVar:"+formVar);
			var self = this;
			var buttons = [{
				'label': (task==null) ? this.tr("processes.form.start") : this.tr("tasks.form.start"),
				'icon': "icon/22/actions/dialog-ok.png",
				'callback': function (data) {
					self._handleExecuteButton(this.form, data, task, formVar,null);
				},
				'value': 0
			},
			{
				'label': this.tr("tasks.form.cancel"),
				'icon': "icon/22/actions/dialog-cancel.png",
				'callback': (function (data) {
					self._formContainer.destroy({});
				}).bind(this),
				'value': 1
			}];

			var actionCallback = function(e){
				console.log("actionCallback:"+this+"/"+qx.util.Serializer.toJson(e));
				if( e.action == "execute" ){
					var buttonKey = e.key;
					var data = this.getData();
					self._handleExecuteButton(this.form, data, task, formVar, buttonKey);
				}
				if( e.action == "cancel" ){
					self._formContainer.destroy({});
				}
			};

			var processVariables={};
			var mappedFormValues=null;
			if( task != null){
				processVariables = this._getProcessVariables( task.processInstanceId);
				mappedFormValues = this._getMappedFormValues( task.id, task.processInstanceId);
			}else{
				processVariables["__namespace"] = ms123.StoreDesc.getCurrentNamespace();
			}
			this._formContainer.open({
				formPath: formPath,
				formVar: formVar,
				mappedFormValues: mappedFormValues,
				actionCallback: actionCallback,
				processName: processName,
				processCategory: processCategory,
				processVariables: processVariables,
				buttons: buttons,
				taskName: taskName
			});
		},
		_getFormVarAndPath:function(formResourceKey){
			var formVar = null;
			var formPath = null;
			if(  formResourceKey.indexOf(",") == -1){
				 formVar = formResourceKey;
				 formPath = formResourceKey;
			}else{
				formVar = formResourceKey.split(",")[1];
				formPath = formResourceKey.split(",")[0];
			}
			if( formVar.match(/.*\.form$/)){
				formVar = formVar.substring(0, formVar.length-5);
			}
			return {
				formVar : formVar,
				formPath : formPath
			}
		},
		_handleExecuteButton:function(form,data,task, formVar, executeButtonKey){
			var validate = form==null ? true : form.validate();
			if( !validate){
				var vm = form._form.getValidationManager();
				var items = vm.getInvalidFormItems();
				var message = "<br />";
				for( var i=0; i < items.length;i++){
					var name = items[i].getUserData("key");
					var msg = items[i].getInvalidMessage();
					message += name + " : " + msg + "<br />";
				}
				ms123.form.Dialog.alert(this.tr("process.form_incomplete")+":"+message);
				return;
			}

			var processVariables = {};
			var formVals = {};
			if (data) {
				Object.keys(data).each(function (p, index) {
					if (form.formData[p] && form.formData[p].type.toLowerCase() == "actionbutton" ) {
						return;
					}
					if (form.formData[p] && form.formData[p].type.toLowerCase() == "alert" ) {
						return;
					}
					if (p.match("^__")) return;
					var val = data[p];
					formVals[p] = val;
				});
			}
			if( executeButtonKey != null){
				formVals["actionButton"] = executeButtonKey;
			}
			if( task == null){
				processVariables["processDefinitionId"] = this.processDefinition.id;
			}
			if( formVar) {
				processVariables[formVar] = formVals;
			}
			this._completeActivity(processVariables, task);
		},
		_getMappedFormValues: function (tid,processInstanceId) {
			var failed = (function (e) {
				var message = "<div style='width:100%;overflow:auto'>" + e + "</div>";
				ms123.form.Dialog.alert(message);
			}).bind(this);

			var completed = (function (e) {
				ms123.form.Dialog.alert(e.getContent());
			}).bind(this);

			var result = null;
			try {
				result = ms123.util.Remote.rpcSync("activiti:getTaskFormProperties", {
					executionId:processInstanceId,
					taskId: tid
				});
			} catch (e) {
				ms123.form.Dialog.alert("ProcessController._getMappedFormValues:" + e);
				failed.call(this,e);
				return;
			}
			if( result && result.values ){
				var m = qx.util.Serializer.toJson(result.values  );console.warn("values:"+m);
				return result.values;
			}
			return null;
		},
		_getProcessVariables: function (pid) {
			var result = null;
			try {
				result = ms123.util.Remote.rpcSync("activiti:getVariables", {
					executionId:pid
				});
			} catch (e) {
				ms123.form.Dialog.alert("ProcessController._getProcessVariables:" + e);
				return;
			}
			return result;
		},
		_completeActivity: function (processVariables, task) {
			var pdata = qx.util.Serializer.toJson(processVariables);
			var _this = this;
			var completed = (function (ret) {
				var x = qx.util.Serializer.toJson(ret); console.log("ret:"+x);
				var pid = ret.id;
				if( task ){
					x = qx.util.Serializer.toJson(task); console.log("task:"+x);
					pid = task.executionId;
				}
				this.fireDataEvent("taskCompleted", task, null);
				var tasks = this._getTasks(pid);
				if (tasks.total > 0) {
					this.showForm(tasks.data[0]);
				} else {
					if( task && task.fromTaskList){
          	ms123.form.Dialog.alert(this.tr("processes.taskform.started") + " -> ID" + (task.processInstanceId));
					}else{
          	ms123.form.Dialog.alert(this.tr("processes.startform.started") + " -> ID" + (task ? task.processInstanceId : ret.id));
					}
					this._formContainer.destroy();
				}
			}).bind(this);
			var failed = (function (ret) {
				console.log("ret:"+qx.util.Serializer.toJson(ret));
				ret = ret.toString();
				var msg = ret.replace(/\|/g, "<br/>");
				var msg = msg.replace(/Script.*groovy: [0-9]{0,4}:/g, "<br/><br/>");
				var msg = msg.replace(/ for class: Script[0-9]{1,2}/g, "");
				var msg = msg.replace(/Script[0-9]{1,2}/g, "");
				var msg = msg.replace(/Application error 500:/g, "");
				var msg = msg.replace(/:java.lang.RuntimeException/g, "");
				var msg = msg.replace(/:Line:/g, "<br/>Line:");
				var msg = msg.replace(/: {0,2}Line:/g, "<br/>Line:");

				msg = ms123.util.Text.explode( msg, 100 );
				var message = "<b>" + this.tr("processes." + ((task!=null) ? "taskform":"startform")+".notstarted") + ": </b><pre style='font-size:10px'>" + msg + "</pre>";
				var alert = new ms123.form.Alert({
					"message": message,
					"windowWidth": 600,
					"windowHeight": 400,
					"useHtml": true,
					"inWindow": true
				});
				alert.show();
				if( task != null){
					this.showForm( task);
				}
			}).bind(this);

			var params = null; 
			if( task == null ){
				params = {
					service: "activiti",
					method: "startProcessInstance",
					parameter: {
						namespace: this._namespace ? this._namespace : ms123.StoreDesc.getCurrentNamespace(),
						processDefinitionId: processVariables["processDefinitionId"],
						processDefinitionKey: processVariables["processDefinitionKey"],
						processDefinitionName: processVariables["processDefinitionName"],
						businessKey: processVariables["businessKey"],
						startParams: processVariables
					},
					async: false,
					context: this,
					failed: failed,
					completed: completed
				}
				return ms123.util.Remote.rpcAsync(params);
			}else{
				this._completeTask( task.id, processVariables,completed, failed);
			}
		},
		_completeTask: function (taskId, processVariables,completed, failed) {
			var showErrors = (function (cv) {
				if( cv ){
					var message = "";
					for (var i = 0; i < cv.length; i++) {
						var c = cv[i];
						if(c.time){
							var d = new Date();
							d.setTime(c.time);
							var lang = qx.locale.Manager.getInstance().getLanguage();
							c.message = c.message.replace('{0}', d.toString(lang=="de" ? 'd.M.yyyy' : 'M/d/yyyy'));
						}
						if( c.message && c.message.match(/^@/)){
							c.message = this.tr(c.message.substring(1));
						}
						if( c.message && c.message.match(/^%/)){
							c.message = this.tr(c.message.substring(1));
						}
						if( c.path){
							message += this._formContainer.getLabel(c.path) + " : " + c.message + "<br />";
						}else{
							message += c.message + "<br />";
						}
					}
					ms123.form.Dialog.alert(message);
					this._formContainer.showErrors(cv);
					return;
				}
			}).bind(this);
			var result = null;
			try {
				result = ms123.util.Remote.rpcSync("activiti:executeTaskOperation", {
					taskId:taskId,
					operation: "complete",
					startParams: processVariables
				});

				console.error("RET:"+result.success);
				if( result.success===true){
						this._formContainer.close({});
						completed.call(this,result);
					 return result;
				}else{
					showErrors(result.errors);
				}
			} catch (e) {
				this._formContainer.close({});
				failed.call(this,e);
				return;
			}
			return result;
		},
		_getTasks: function (processInstanceId) {
			var result = null;
			try {
				result = ms123.util.Remote.rpcSync("activiti:getTasks", {
					queryParams:{
						assignee:this.userid,
						processInstanceId: processInstanceId
					},

					listParams:{
						size:1000
					}
				});
			} catch (e) {
				ms123.form.Dialog.alert("ProcessController._getTasks:" + e);
				return;
			}
			return result;
		},
		_getProcessDefinition: function (namespace,name) {
			var result = null;
			try {
				result = ms123.util.Remote.rpcSync("activiti:getProcessDefinitions", {
					namespace:namespace ? namespace : ms123.StoreDesc.getCurrentNamespace(),
					version:-1,
					key:name
				});
			} catch (e) {
				ms123.form.Dialog.alert("ProcessController._getProcessDefinitions:" + e);
				return;
			}
			var defs = result["data"];
			if( defs.length>0) return defs[0];
			return null;
		}
	}
});
