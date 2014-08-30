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

qx.Class.define("ms123.shell.views.Editor", {
	extend: qx.ui.core.Widget,
	include: qx.locale.MTranslation,


	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (model,param,facade) {
		this.base(arguments);
		this.facade=facade;
		this._setLayout(new qx.ui.layout.Dock());
		console.log("model:" + qx.util.Serializer.toJson(model));

		this._realEditor=null;
		var widget = null;
		if( model.getType() == ms123.shell.Config.PROCESS_FT ){
			widget = this._handleProcess(model);
		}else if( model.getType() == ms123.shell.Config.FILTER_FT ){
			widget = this._handleFilter(model);
		}else if( model.getType() == ms123.shell.Config.CAMEL_FT ){
			widget = this._handleCamel(model);
		}else if( model.getType() == ms123.shell.Config.STENCIL_FT ){
			widget = this._handleStencil(model);
		}else if( model.getType() == ms123.shell.Config.DATAMAPPER_FT ){
			widget = this._handleDatamapper(model);
		}else if( model.getType() == ms123.shell.Config.WEBPAGE_FT ){
			widget = this._handleWebpage(model);
		}else if( model.getType() == ms123.shell.Config.GROOVY_FT ){
			widget = this._handleGroovy(model);
		}else if( model.getType() == ms123.shell.Config.RULE_FT ){
			widget = this._handleRule(model);
		}else{
			widget = this._handle(model);
		}
		if( widget){
			this._add( widget, {edge:"center"});
		}
	},


	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},


	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		_handleProcess:function(model){
			var context = {};
			context.storeDesc = this.facade.storeDesc;
			var ge = new ms123.graphicaleditor.GraphicalEditor(context);
			this._realEditor = ge;
			ge.addListener("save", function(e){
				var json = e.getData();
				this._saveContent( model, model.getType().substring(3), json);
			}, this);
			ge.addListener("deploy", function(e){
				console.log("model:"+qx.util.Serializer.toJson(model));
				this.__deployProcess(model,e.getData());
			}, this);
			ge.addListener("undeploy", function(e){
				this.__undeployProcess(model,e.getData());
			}, this);
			var content = this._getContent(model.getPath());
			ge.init(model.getType(), model.getValue(), content,this.facade.rootNode);
			return ge;
		},
		_handle:function(model){
			var type = model.getType();
			var context = {};
			context.storeDesc = this.facade.storeDesc;
			var ge = new ms123.graphicaleditor.GraphicalEditor(context);
			this._realEditor = ge;
			ge.addListener("save", function(e){
				var json = e.getData();
				this._saveContent( model, type.substring(3), json);
			}, this);
			var content = this._getContent(model.getPath());
			ge.init(type, model.getValue(), content,this.facade.rootNode);
			return ge;
		},
		_handleCamel:function(model){
			var type = model.getType();
			var context = {};
			context.storeDesc = this.facade.storeDesc;
			var ge = new ms123.graphicaleditor.GraphicalEditor(context);
			this._realEditor = ge;
			var content = this._getContent(model.getPath());
			ge.init(type, model.getValue(), content,this.facade.rootNode);
			ge.addListener("save", function(e){
				var json = e.getData();
				this._saveCamel( model, type.substring(3), json);
			}, this);
			return ge;
		},
		_handleGroovy:function(model){
			var type = model.getType();
			var config = {};
			config.storeDesc = this.facade.storeDesc;
			config.facade = this.facade;
			config.mode="text/x-groovy";
			var content = this._getContentRaw(model.getPath());
			var ge = new ms123.shell.views.TextEditor(config,content);
			this._realEditor = ge;
			ge.addListener("save", function(e){
				var content = e.getData();
				this._saveGroovy( model, type.substring(3), content);
			}, this);
			return ge;
		},
		_handleWebpage:function(model){
			var type = model.getType();
			var config = {};
			config.storeDesc = this.facade.storeDesc;
			config.facade = this.facade;
			config.helper = "DocumentMarkdown";
			config.toolbarAddon = "WikiMarkdown";
			config.mode="text/tiki";
			var content = this._getContent(model.getPath());
			var ge = new ms123.shell.views.TextEditor(config,content);
			this._realEditor = ge;
			ge.addListener("save", function(e){
				var content = e.getData();
				this._saveContent( model, type.substring(3), content);
			}, this);
			return ge;
		},
		_handleRule:function(model){
			var context = {};
			context.storeDesc = this.facade.storeDesc;
			context.name = model.getValue();
			var re = new ms123.ruleseditor.RulesEditor(context);
			this._realEditor = re;
			re.addListener("save", function(e){
				var json = e.getData();
				this._saveContent( model, "rule", json);
			}, this);
			var content = this._getContent(model.getPath());
			re.init(content);
			return re;
		},
		_handleDatamapper:function(model){
			var context = {};
			context.storeDesc = this.facade.storeDesc;
			context.name = model.getValue();
			var re = new ms123.datamapper.Datamapper(context);
			this._realEditor = re;
			re.addListener("save", function(e){
				var data = e.getData();
				this._saveContent( model, "datamapper", data);
			}, this);
			var content = this._getContent(model.getPath());
			re.init(content);
			return re;
		},
		_handleStencil:function(model){
			console.log("_handleStencil:"+model);
			var context = {};
			context.storeDesc = this.facade.storeDesc;
			context.name = model.getValue();
			var content = this._getContent(model.getPath());
			var re = new ms123.shell.views.StencilEditor(context,content);
			this._realEditor = re;
			re.addListener("save", function(e){
				var data = e.getData();
				this._saveContent( model, "stencil", data);
			}, this);
			return re;
		},
		_handleFilter:function(model){
			var context = {};
			context.storeDesc = ms123.StoreDesc.getNamespaceDataStoreDesc();//this.facade.storeDesc;
			var fe = new ms123.filtereditor.FilterEditor(context);
			this._realEditor = fe;
			fe.addListener("save", function(e){
				var filterProps = e.getData();
				this._saveContent( model, "filter", qx.lang.Json.stringify(filterProps,null,2));
			}, this);
			var content = this._getContent(model.getPath());
			var modulename = null;	
			var filter = null;	
			var fields = null;	
			var exclusion = null;	
			var json = content;	
			if( json && json != "" && json.length>2){
				json = qx.lang.Json.parse(json);
				modulename = json.modulename;	
				filter = json.filter;	
				fields = json.fields;	
				exclusion = json.exclusion;	
			}
			fe.init.call(fe,model.getValue(), model.getPath(), modulename, fields, filter, exclusion);
			return fe;
		},
		getEditor:function(){
			return this._realEditor;
		},
		_getContentRaw:function(path){
			try{
				return ms123.util.Remote.rpcSync( "git:getContentRaw",{
												reponame:this.facade.storeDesc.getNamespace(),
												path:path
											});
			}catch(e){
				ms123.form.Dialog.alert("shell.views.Editor._getContentRaw:"+e.message);
				return null;
			}
		},
		_getContent:function(path){
			try{
				return ms123.util.Remote.rpcSync( "git:getContent",{
												reponame:this.facade.storeDesc.getNamespace(),
												path:path
											});
			}catch(e){
				ms123.form.Dialog.alert("shell.views.Editor._getContent:"+e.message);
				return null;
			}
		},
		_saveContent: function (model, what, content) {
			var path = model.getPath();
			var completed = (function (e) {
				ms123.form.Dialog.alert(this.tr("shell."+what+"_saved"));
			}).bind(this);

			var failed = (function (e) {
				ms123.form.Dialog.alert(this.tr("shell."+what+"_save_failed")+":"+e.message);
			}).bind(this);

			var rpcParams = {
				reponame:this.facade.storeDesc.getNamespace(),
				path:path,
				type:model.getType(),
				content: content
			};

			var params = {
				method:"putContent",
				service:"git",
				parameter:rpcParams,
				async: false,
				context: this,
				completed: completed,
				failed: failed
			}
			ms123.util.Remote.rpcAsync(params);
		},
		_saveGroovy: function (model, what, content) {
			this._saveCamel(model,what,content);
		},
		_saveCamel: function (model, what, content) {
			var methods = {
				"camel":"saveRouteJsonDescription",
				"groovy":"saveGroovyScript"
			}
			var path = model.getPath();
			var completed = (function (e) {
				ms123.form.Dialog.alert(this.tr("shell."+what+"_saved"));
				var eventBus = qx.event.message.Bus;
				eventBus.getInstance().dispatchByName("camelroutes.deployed", {});
			}).bind(this);

			var failed = (function (ret) {
				ret = ret.toString();
				var msg = ret.replace(/\|/g, "<br/>");
				var msg = msg.replace(/Script.*groovy: [0-9]{0,4}:/g, "<br/><br/>");
				var msg = msg.replace(/ for class: Script[0-9]{1,2}/g, "");
				var msg = msg.replace(/Script[0-9]{1,2}/g, "");
				var msg = msg.replace(/Application error 500:/g, "");
				var msg = msg.replace(/:java.lang.RuntimeException/g, "");
				var msg = msg.replace(/:Line:/g, "<br/>Line:");
				var msg = ms123.util.Text.explode( msg, 90 );

				var message = "<b>" + this.tr("shell."+what+"_save_failed")+":</b><pre style='font-size:10px'>" + msg + "</pre></div>";
				var alert = new ms123.form.Alert({
					"message": message,
					"windowWidth": 600,
					"windowHeight": 400,
					"useHtml": true,
					"inWindow": true
				});
				alert.show();
			}).bind(this);

			var rpcParams = {
				namespace:this.facade.storeDesc.getNamespace(),
				path:path,
				content: content
			};

			var params = {
				method:methods[what],
				service:"camel",
				parameter:rpcParams,
				async: false,
				context: this,
				completed: completed,
				failed: failed
			}
			ms123.util.Remote.rpcAsync(params);
		},
		_deployProcess:function(prefix,model,data){
			console.log("_deployProcess:"+qx.util.Serializer.toJson(model));
			var okMessage = this.tr("data.process."+prefix+"deployed");
			var failMessage = this.tr("data.process."+prefix+"deploy_failed");

			var completed = function (e) {
				ms123.form.Dialog.alert(okMessage);
				var eventBus = qx.event.message.Bus;
				eventBus.getInstance().dispatchByName("processdiagram.deployed", {});
			};

			var failed = function (e) {
				var txt = e.message.replace(/\\n/g, "<br />");
				var txt = txt.replace(/\|/g, "<br />");
				ms123.form.Dialog.alert(failMessage + ":" + txt);
			};

			var rpcParams = {
				namespace:this.facade.storeDesc.getNamespace(),
				path:model.getPath()
			};

			var params = {
				method:prefix+"deployProcess",
				service:"workflow",
				parameter:rpcParams,
				async: false,
				context: this,
				completed: completed,
				failed: failed
			}
			ms123.util.Remote.rpcAsync(params);
		},
		__undeployProcess:function(model,data){
			this._deployProcess("un",model,data);
		},
		__deployProcess:function(model,data){
			this._deployProcess("",model,data);
		}
	},
	destruct: function () {
	}
});
