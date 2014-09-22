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
/*
*/
/**
	* @ignore(Hash)
	* @ignore(Clazz)
*/

qx.Class.define("ms123.graphicaleditor.plugins.Save", {
	extend: qx.core.Object,
	include: [qx.locale.MTranslation],
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (facade, main) {
		this.base(arguments);
		this.facade = facade;
		this.main = main;
		this.metadata = main.context.data;
		this.moduleName = main.context.moduleName;
		this.editorType = main.context.editorType;

		this.facade.offer({
			'name': this.tr("ge.Save.save"),
			'functionality': this.save.bind(this, false),
			'group': this.tr("ge.Save.group"),
			'icon': this.__getResourceUrl("disk.png"),
			'description': this.tr("ge.Save.saveDesc"),
			'index': 1,
			'minShape': 0,
			'maxShape': 0
		});

		this.facade.offer({
			'name': this.tr("ge.Save.showJson"),
			'functionality': this.showJson.bind(this),
			'group': this.tr("ge.Save.group"),
			'icon': this.__getResourceUrl("json.png"),
			'description': this.tr("ge.Save.showJsonDesc"),
			'index': 2,
			'minShape': 0,
			'maxShape': 0
		});

		this.facade.offer({
			'name': this.tr("ge.Save.renewResourceIds"),
			'functionality': this.renewResourceIds.bind(this),
			'group': this.tr("ge.Save.group"),
			'icon': "icon/16/actions/object-rotate-right.png",
			'description': this.tr("ge.Save.renewResourceIdsDesc"),
			'index': 3,
			'minShape': 0,
			'maxShape': 0
		});

		if( this.editorType == "sw.process" ){
			this.facade.offer({
				'name': this.tr("ge.Save.deploy"),
				'functionality': this.deploy.bind(this, false),
				'group': this.tr("ge.Save.group"),
				'icon': "icon/16/actions/media-playback-start.png",
				'description': this.tr("ge.Save.deploy"),
				'index': 4,
				'minShape': 0,
				'maxShape': 0
			});
			this.facade.offer({
				'name': this.tr("ge.Save.undeploy"),
				'functionality': this.undeploy.bind(this, false),
				'group': this.tr("ge.Save.group"),
				'icon': "icon/16/actions/media-playback-stop.png",
				'description': this.tr("ge.Save.undeploy"),
				'index': 5,
				'minShape': 0,
				'maxShape': 0
			});
		}
		if( this.editorType == "sw.form" ){
			this.facade.offer({
				'name': this.tr("graphicaleditor.plugins.save.formtest"),
				'functionality': this.formTest.bind(this),
				'group': this.tr("ge.Save.group"),
				'icon': "icon/16/actions/check-spelling.png",
				'description': "Formtest",
				'index': 4,
				'minShape': 0,
				'maxShape': 0
			});
		}

		if( this.editorType == "sw.document" ){
			this.facade.offer({
				'name': this.tr("graphicaleditor.plugins.save.doctest"),
				'functionality': this.documentTest.bind(this),
				'group': this.tr("ge.Save.group"),
				'icon': "icon/16/actions/check-spelling.png",
				'description': "Documenttest",
				'index': 4,
				'minShape': 0,
				'maxShape': 0
			});
		}

	},


	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},

	/******************************************************************************
	 STATICS
	 ******************************************************************************/
	statics: {},
	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		undeploy: function () {
			this.main.fireAction( "undeploy", null);
		},
		deploy: function () {
			this.main.fireAction( "deploy", null);
		},

		showJson: function () {
			var jsonProcessModel = qx.lang.Json.stringify(this.facade.getJSON(),null,4);
			var win = this._createWindow(jsonProcessModel);
			win.setActive(true);
			win.open();
		},
		renewResourceIds: function () {
			var json = this.facade.getJSON();
			this.facade.importJSON(json,true,true);
			json.childShapes.each(qx.lang.Function.bind(function (shapeAsJson) {
				this.facade.deleteShape(shapeAsJson.getShape());
			},this));
		},
		formTest: function () {
			var fw = new ms123.processexplorer.FormWindow({});
			var context = {};
			context.formDesc= this.facade.getJSON();
			context.processName = "Formtest";
			context.buttons = [];
			context.processCategory = ms123.StoreDesc.getCurrentNamespace();
			fw.open(context);
		},
		documentTest: function () {
			var json = this.facade.getJSON();
			var sales = [];
			var rec1={
				menge:12,
				name:"Schuhreme",
				preis:14.22
			}
			sales.push(rec1);
			rec1={
				menge:22,
				name:"Milch",
				preis:1.22
			}
			sales.push(rec1);
			var rechnungsdaten={
				sales:sales
			}
			var rpc = {
				"service": "docbook",
				"method": "jsonToDocbookPdf",
				"id": 31,
				"params": {
					namespace: ms123.StoreDesc.getCurrentNamespace(),
					params:{ param1:"Value1",rechnungsdaten:rechnungsdaten },
					json:qx.lang.Json.stringify(json,2)
				}
			};
			var u = ms123.util.Remote._username;
			var p = ms123.util.Remote._password;
			var credentials = ms123.util.Base64.encode( u + ":"+ p );

			var downloadForm = window.document.getElementById("graphicaleditor-downloadForm");
			downloadForm.action = "/rpc/__rpcForm__?credentials=" + credentials;
			var rpcString = qx.util.Serializer.toJson(rpc);
			downloadForm["__rpc__"].value = rpcString;
			downloadForm.submit(); 
		},
		_createWindow: function (value) {
			var win = new qx.ui.window.Window("JSON", "").set({
				resizable: true,
				useMoveFrame: true,
				useResizeFrame: true
			});
			win.setLayout(new qx.ui.layout.Dock);
			win.setWidth(500);
			win.setHeight(400);
			win.setAllowMaximize(false);
			win.setAllowMinimize(false);
			win.setModal(true);
			win.setActive(true);
			win.minimize();
			win.center();

      var msgArea = new qx.ui.form.TextArea();
			msgArea.setValue( value );
				
      win.add(msgArea,{edge:"center"});
			return win;
		},

		/**
		 * Saves the current process to the server.
		 */
		save: function () {
			this.facade.raiseEvent({
				type: ms123.oryx.Config.EVENT_LOADING_ENABLE,
				text: this.tr("ge.Save.saving")
			});

			var json = this.facade.getJSON();
			var zoomLevel = this.facade.view.getZoomLevel();
			json.zoomLevel= zoomLevel;
			var jsonProcessModel = qx.lang.Json.stringify(json,null,2);
			this.main.fireAction( "save", jsonProcessModel);
			return true;
		},
		__getResourceUrl: function (name) {
			var am = qx.util.AliasManager.getInstance();
			return am.resolve("resource/ms123/" + name);
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
