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

qx.Class.define("ms123.usertasks.DublettenCheckForm", {
 extend: qx.core.Object,
 include : qx.locale.MTranslation,

	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function () {
		this.base(arguments);
		this.__configManager = new ms123.config.ConfigManager();
	},


	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},


	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		init: function (params) {
			this._processInstanceId = params.processInstanceId;
			var formDesc = params.formDesc;
			this.callback = params.callback;
			this.user = params.user;
			var app = qx.core.Init.getApplication();
			var win = this._createWindow(app.getRoot(), "DublettenCheck");
			var sp = this._splitPane();
			win.add(sp);
			this.mainWin = win;
			var duplicates = this._getProcessVariable(this._processInstanceId, "duplicates");
			if( !duplicates) {
				ms123.form.Dialog.alert(this.tr("tasks.usertasks.dublettencheck.no_duplicates"));
				this.callback._completeTask( {} );
				return;
			}
			var module = this._getProcessVariable(this._processInstanceId, ms123.config.ConfigManager.CS_ENTITY);
			var imported_ok = this._getProcessVariable(this._processInstanceId, "imported_ok");
			this._createUpperDesc(imported_ok, duplicates.data.length);
			this._createRefDesc();
			this._createToolbar();
			this._createDupTable(module, duplicates.data);
			this._createRefForm(module);
		},
		_getProcessVariable: function (processInstanceId, variable) {
			var ret = null;
			var url = "workflowrest/runtime/" + processInstanceId + "/variable/" + variable;
   		var ret = ms123.util.Remote.sendSync(url);
			var p = qx.util.Serializer.toJson(ret);
			return ret.value; //@@@MS remember
		},
		_setProcessVariable:function( processInstanceId, variable, value ){
			var url = "workflowrest/runtime/"+processInstanceId+"/variable/"+variable;
			
			var d = {value: value};
			var pdata = qx.util.Serializer.toJson(d);

			var params = {
				url:url,
				method:"POST",
				async:false,
				data:pdata,
				contenttype:"application/json;charset=UTF-8",
				context:this
			}
			ms123.util.Remote.send( params );
		},
		_createUpperDesc: function (imported_ok, count_dups) {
			var msg = this.tr("tasks.usertasks.dublettencheck.desc");
			msg = msg.replace("%r", imported_ok);
			msg = msg.replace("%d", count_dups);
			var label = new qx.ui.basic.Label().set({
				decorator: "",
				allowGrowX: true,
				paddingTop: 1,
				paddingBottom: 1,
				value: msg,
				rich: true
			});
			this.upperWidget.add(label, {
				edge: "north"
			});
		},
		_createRefDesc: function () {
			var label = new qx.ui.basic.Label().set({
				decorator: "",
				allowGrowX: true,
				paddingTop: 1,
				paddingBottom: 1,
				value: this.tr("tasks.usertasks.dublettencheck.reference"),
				rich: true
			});
			this.bottomWidget.add(label, {
				edge: "north"
			});
		},
		_createToolbar: function () {
			var toolbar = new qx.ui.toolbar.ToolBar().set({
				decorator: "",
				allowGrowX: true
			});
			var buttonComplete = new qx.ui.toolbar.Button(this.tr("tasks.table.complete"), "icon/22/actions/dialog-ok.png");
			buttonComplete.addListener("execute", function () {
					var map = this.dupTable.getData();
					var processVariables = {
						duplicates : {data : map}
					}
				this.mainWin.close();
				var ret = this.callback._completeTask( processVariables );
				if( ret != null ){
					//this.mainWin.close();
				}
			}, this);

			toolbar.add(buttonComplete);
			this.upperWidget.add(toolbar, {
				edge: "south"
			});
		},
		_createDupTable: function (module, data) {
			var context = {};
			var mode_col = {
				"name": "_mode",
				"editable": false,
				"tableedit": true,
				"edittype": "select",
				"datatype": "string",
				"label": this.tr("tasks.usertasks.dublettencheck.mode"),
				"editoptions_value": {
					"update": this.tr("tasks.usertasks.dublettencheck.update"),
					"insert": this.tr("tasks.usertasks.dublettencheck.insert"),
					"ignore": this.tr("tasks.usertasks.dublettencheck.ignore")
				},
				"formatter": "select"
			}

			var sdesc = new ms123.StoreDesc({});
			var model = this.__configManager.getEntityModel(module,sdesc, "main-grid", "properties");
			var cols = model.attr("colModel");
			var configdata = {}
			configdata.gridProps = model.attr("gridProps");
			configdata.colModel = [];
			for (var i = 0; i < cols.length; i++) {
				configdata.colModel.push(cols[i]);
			}
			context.model = this.__configManager.buildModel(configdata.colModel, configdata.gridProps );
			context.user = this.user;
			context.isMaster = true;
			context.unit_id = "dupcheck";
			var _this = this;
			var buttons = [
			{
				'label': "",
        'tooltip': this.tr("tasks.usertasks.dublettencheck.delete_dublette"),
				'icon': "icon/16/places/user-trash.png",
				'callback': function (m) {
					ms123.form.Dialog.confirm(_this.tr("table.confirm.delete"),function(e){
						if( e ){
							_this.dupTable.deleteCurrentRecord();
							var map = _this.dupTable.getData();
							var duplicates = {data : map}
							_this._setProcessVariable(_this._processInstanceId, "duplicates", duplicates);
							_this.refForm.clearForm();
							if( _this.dupTable.getRowCount() == 0){
								_this.callback._completeTask( {} );
							}
							_this.currentRefId = null;
						}
					},this);
				},
				'value': "del"
			}, {
				'label': "",
        'tooltip': this.tr("tasks.usertasks.dublettencheck.uebernahme_von_teilen"),
				'icon': "icon/16/actions/edit-copy.png",
				'callback': function (m) {
					var map = _this.dupTable.getCurrentRecord();
					if( _this.dupCopyWindow == null ){
						var app = qx.core.Init.getApplication();
						_this._createDupCopyWindow(app.getRoot(), module);
					}
					_this.dupCopyForm.fillForm(map);
					_this.dupCopyWindow.setActive(true);
					_this.dupCopyWindow.open();
				},
				'value': "copy"
			}, {
				'label': "",
				'icon': "icon/16/apps/utilities-text-editor.png",
        'tooltip': this.tr("tasks.usertasks.dublettencheck.edit_dublette"),
				'callback': function (m) {
					var map = _this.dupTable.getCurrentRecord();

					if( _this.dupEditWindow == null ){
						var app = qx.core.Init.getApplication();
						_this._createDupEditWindow(app.getRoot(), module);
					}
					_this.dupEditForm.fillForm(map);
					_this.dupEditWindow.setActive(true);
					_this.dupEditWindow.open();
				},
				'value': "edit"
			}];
			context.buttons = buttons;
			var table = new ms123.widgets.Table(context);
			this.upperWidget.add(table, {
				edge: "center"
			});
			for (var i = 0; i < data.length; i++) {
				var row = data[i];
				row._mode = this.tr("tasks.usertasks.dublettencheck.insert");
			}
			table.setData(data);
			this.dupTable = table;

			var eventBus = qx.event.message.Bus;
			eventBus.subscribe(context.unit_id + ".table.row.selected", function (msg) {
				var data = msg.getData();
				this.currentRefId = data.row._duplicated_id_;
				var props = model.attr("gridProps");
				var url = "data/"+ module + "/" + data.row._duplicated_id_+"?what=asRow";
				var completed = function (e) {
					var data = e.getContent();
					this.refForm.clearForm();
					this.refForm.fillForm(data);
				};
				var params = {
					url:url,
					method:"GET",
					async:true,
					context:this,
					completed: completed
				}
				ms123.util.Remote.send( params );
			}, this);

		},
		_createRefForm: function (module) {
			var context = {};
			var _this = this;
			var buttons = [ {
				'label': this.tr("tasks.usertasks.dublettencheck.save"),
				'icon': "icon/22/actions/dialog-ok.png",
				'callback': function(map){

					if( !_this.currentRefId ){
						ms123.form.Dialog.alert(this.tr("tasks.usertasks.dublettencheck.no_dublette_selected"));
						return;
					}
					var formData = qx.util.Serializer.toJson(map);
					var data = "__data="+encodeURIComponent(formData);
					var completed = function (e) {
						ms123.form.Dialog.alert(_this.tr("data."+ module+ ".saved"));
					};

					var failed = function (e) {
						var cv = null;
						try{
							cv = qx.lang.Json.parse( e.getContent() );
						}catch(e){ }
						if( cv && cv.constraintViolations !== undefined ){
							var message = "";
							for( var i=0; i< cv.constraintViolations.length;i++){
								var c = cv.constraintViolations[i];	
								message += this.getLabel(c.path) + " : " + c.message + "<br />";
							}
							ms123.form.Dialog.alert(message);
							this.setErrors( cv.constraintViolations );
						}else{
							ms123.form.Dialog.alert("Error:"+e.getStatusCode()+"/"+e.getContent());
						}
					};

					var url = "data/"+module+"/"+_this.currentRefId;	
					var params = {
						url:url,
						method:"PUT",
						data:data,
						async:true,
						context:this,
						completed: completed,
						failed:failed	
					}
					ms123.util.Remote.send( params );
				},
				'value': "save"
			}
			];

			context.buttons = buttons;
			var sdesc = new ms123.StoreDesc({});
			context.model = this.__configManager.getEntityModel(module,sdesc, "main-form", "properties");
			context.config = module;
			var form = new ms123.widgets.Form(context);
			this.bottomWidget.add(form, {
				edge: "center"
			});
			this.refForm = form;

		},
		_splitPane: function (parent) {
			var splitpane = new qx.ui.splitpane.Pane("vertical");
			splitpane.setDecorator("main");

			var upperWidget = new qx.ui.container.Composite();
			upperWidget.setLayout(new qx.ui.layout.Dock());
			upperWidget.setDecorator(null);
			splitpane.add(upperWidget, 2);
			this.upperWidget = upperWidget;

			var bottomWidget = new qx.ui.container.Composite();
			bottomWidget.setLayout(new qx.ui.layout.Dock());
			bottomWidget.setDecorator(null);
			splitpane.add(bottomWidget, 2);
			this.bottomWidget = bottomWidget;

			return splitpane;
		},
		_createWindow: function (root, name) {
			var win = new qx.ui.window.Window(name, "").set({
				resizable: true,
				useMoveFrame: true,
				useResizeFrame: true
			});
			win.setLayout(new qx.ui.layout.Grow);
			win.setWidth(750);
			win.setHeight(590);
			win.setAllowMaximize(true);
			win.open();
			win.addListener("changeActive", function (e) {
				console.log("changeActive:" + e.getData());
			}, this);
			win.addListener("close", function (e) {
				//if( this.dupTable.getRowCount() == 0){
				//	this.callback._completeTask( {} );
				//}
			}, this);
			root.add(win, {
				left: ms123.DesktopWindow._left,
				top: ms123.DesktopWindow._top
			});
			ms123.DesktopWindow._top += 15;
			ms123.DesktopWindow._left += 15;
			return win;
		},
		_createDupEditWindow: function (root, module) {
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
			root.add(win);
			var context = {};


			var _this = this;
			var buttons = [
				{
				'label': this.tr("tasks.usertasks.dublettencheck.checkit"),
				'icon': "icon/22/actions/edit-redo.png",
				'callback': function(formData){
					var postData = {};
					postData.filterfields = ["company1","shortname_person","shortname_company"];
					postData.module = module;
					postData.datamap = formData;
					postData = qx.util.Serializer.toJson(postData);
					var cv  = ms123.util.Remote.sendSync("service/importing.dubletten-check/check","POST",null,postData);
						if( cv && cv.constraintViolations !== undefined ){
							var message = "";
							for( var i=0; i< cv.constraintViolations.length;i++){
								var c = cv.constraintViolations[i];	
								message += this.getLabel(c.path) + " : " + c.message + "<br />";
							}
							ms123.form.Dialog.alert(message);
							this.setErrors( cv.constraintViolations );
						}else{
							ms123.form.Dialog.alert("Ok");
						}
				},
				'value': "check"
			},{
				'label': this.tr("tasks.usertasks.dublettencheck.save"),
				'icon': "icon/22/actions/dialog-ok.png",
				'callback': function(map){
					var formData = qx.util.Serializer.toJson(map);
					var data = "__data="+encodeURIComponent(formData);
					var completed = function (e) {
						ms123.form.Dialog.alert(_this.tr("data."+ module+ ".created"));
						this.setAllValid();
						_this.dupTable.deleteCurrentRecord();
						_this.refForm.clearForm();
						_this.currentRefId = null;
						var map = _this.dupTable.getData();
						var duplicates = {data : map}
						_this._setProcessVariable(_this._processInstanceId, "duplicates", duplicates);
						if( _this.dupTable.getRowCount() == 0){
							_this.callback._completeTask( {} );
						}
						_this.dupEditWindow.close();
					};

					var failed = function (e) {
						var cv = null;
						try{
							cv = qx.lang.Json.parse( e.getContent() );
						}catch(e){ }
						if( cv && cv.constraintViolations !== undefined ){
							var message = "";
							for( var i=0; i< cv.constraintViolations.length;i++){
								var c = cv.constraintViolations[i];	
								message += this.getLabel(c.path) + " : " + c.message + "<br />";
							}
							ms123.form.Dialog.alert(message);
							this.setErrors( cv.constraintViolations );
						}else{
							ms123.form.Dialog.alert("Error:"+e.getStatusCode()+"/"+e.getContent());
						}
					};

					var url = "data/"+module;	
					var params = {
						url:url,
						method:"POST",
						data:data,
						async:true,
						context:this,
						completed: completed,
						failed:failed	
					}
					ms123.util.Remote.send( params );
				},
				'value': "save"
			}
			];

			context.buttons = buttons;
			var sdesc = new ms123.StoreDesc({});
			context.model = this.__configManager.getEntityModel(module,sdesc, "main-form", "properties");
			context.config = module;
			var form = new ms123.widgets.Form(context);
			win.add(form);
			this.dupEditWindow = win;
			this.dupEditForm = form;
		},
		_createDupCopyWindow: function (root, module) {
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
			root.add(win);
			var context = {};


			var _this = this;
			var buttons = [
				{
        'label': this.tr("tasks.usertasks.dublettencheck.uebernehmen"),
				'icon': "icon/16/actions/edit-copy.png",
				'callback': function(formData){
					var data = this.getData();
					var m = _this.refForm.form.getModel();
					for(var key in data){
						if( key.match("^__hint"))continue;
						if (_this.refForm.formData[key].type == "DateField") {
							var date = new Date();
							if( data[key] != ""){
								date.setTime(data[key]);
								m.set(key, date);
							}
						}else{
							m.set(key, data[key]);
						}
					}
				},
				'value': "check"
			}
			];

			context.buttons = buttons;
			var sdesc = new ms123.StoreDesc({});
			context.model = this.__configManager.getEntityModel(module,sdesc, "main-form", "properties");
			context.useitCheckboxes = true;
			context.useitCheckboxesDefault = false;
			context.config = module;
			var form = new ms123.widgets.Form(context);
			win.add(form);
			this.dupCopyWindow = win;
			this.dupCopyForm = form;
		},
		_createIdFilter: function (idArray) {
			var idFilter = {
				label: "1",
				connector: "or",
				children: []
			};
			for (var i = 0; i < idArray.length; i++) {
				var node = new ms123.searchfilter.Node();
				node.setField("id");
				node.setOp("eq");
				node.setData(idArray[i]);
				idFilter.children.push(node);
			}
			return idFilter;
		}
	}
});
