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
*/
qx.Class.define("ms123.graphicaleditor.GraphicalEditorWrapper", {
	extend: ms123.graphicaleditor.GraphicalEditor,

	/**
	 * Constructor
	 */
	construct: function (context) {
		this.base(arguments);
		this.setLayout(new qx.ui.layout.Dock());
		context.window.add(this, {});
		var editorType=null;
		var diagramName = null;

		if( !context.editorType ){
			if( context.moduleName == "activitiprocess" ){
				editorType = "sw.process";
				context.window.setCaption("GraphicalEditor("+context.data.pid+")");
				diagramName = context.data.pid;
			}
			if( context.moduleName == "form" ){
				editorType = "sw.form";
				context.window.setCaption("GraphicalEditor("+context.data.fid+")");
				diagramName = context.data.fid;
			}
		}
		this.addListener("save",this._save,this);
		this.addListener("deploy",this._deploy,this);
		this.addListener("undeploy",this._undeploy,this);

		this._moduleName = context.moduleName;
		this._id = context.data.id;
		this._pid = context.data.pid;
		var json = this._loadFile();
		this.init(editorType,diagramName,json);
	},

	events: {
		"changeValue": "qx.event.type.Data"
	},
	/**
	 * ****************************************************************************
	 * MEMBERS
	 * ****************************************************************************
	 */
	members: {
		_loadFile:function(){
			var id = this._id;
			console.log("import:" + id);
			if (id != undefined && id) {
				var url = "data/"+this._moduleName+"/" + id;
				var map = ms123.util.Remote.sendSync(url + "?what=asRow");
				return map.json;
			}
			return "";
		},
		__deploy:function(prefix){
			var okMessage = this.tr("data.process."+prefix+"deployed");
			var failMessage = this.tr("data.process."+prefix+"deploy_failed");
			var url = "xconfig/"+prefix+"deployprocess/" + this._pid;

			var completed = function (e) {
				ms123.form.Dialog.alert(okMessage);
				var eventBus = qx.event.message.Bus;
				eventBus.getInstance().dispatchByName("processdiagram.deployed", {});
			};

			var failed = function (e) {
				var txt = e.getContent().replace(/\\n/g, "<br />");
				ms123.form.Dialog.alert(failMessage + ":" + e.getStatusCode() + "/" + txt);
			};
			var params = {
				url: url,
				method: "GET",
				async: true,
				context: this,
				failed: failed,
				completed: completed
			}
			ms123.util.Remote.send(params);
		},
		_undeploy:function(e){
			this.__deploy("un");
		},
		_deploy:function(e){
			this.__deploy("");
		},
		_save:function(e){
			var jsonProcessModel = e.getData();
			var url = "data/" + this._moduleName + "/" + this._id + "?what=asRow";
			var completed = function (e) {
				ms123.form.Dialog.alert(this.tr("data." + this._moduleName + ".saved"));
			};
			var failed = function (e) {
				ms123.form.Dialog.alert(this.tr("data." + this._moduleName + ".savefailed:" + e));
			};
			var data = "json=" + encodeURIComponent(jsonProcessModel);
			this.fireChanged(jsonProcessModel);

			var params = {
				url: url,
				method: "PUT",
				data: data,
				async: true,
				context: this,
				completed: completed,
				failed: failed
			}
			ms123.util.Remote.send(params);
		},
		fireChanged: function (data) {
			console.log("graphicaleditor.fireDataEvent");
			this.fireDataEvent("changeValue", data, null);
		}
	}
});
