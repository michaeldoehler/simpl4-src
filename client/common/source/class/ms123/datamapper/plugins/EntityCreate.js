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

qx.Class.define("ms123.datamapper.plugins.EntityCreate", {
	extend: qx.core.Object,
	include: [qx.locale.MTranslation],
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (facade,context) {
		this.base(arguments);
		this._facade = facade;
		this._side = context.side;

		var ec_msg = this.tr("datamapper.entitytypes_create");
		var group = "4";
		this._facade.offer({
			name: ec_msg,
			description: ec_msg,
			icon: "resource/ms123/table.png",
			functionality: this.create.bind(this),
			group: group,
			isEnabled: qx.lang.Function.bind(function () {
				return true;
			}, this),
			index: 0
		});


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
		create: function () {
			var allList = this._toStringList(this._getEntitytypes());
			console.log("allList:" + JSON.stringify(allList, null, 2));
			var ret = this._createEntitytypes(true);
			this._newList = this._toStringList(ret.entityList);
			console.log("newList:" + JSON.stringify(this._newList, null, 2));
			this._existsList = this._getExistsList(allList, this._newList);
			console.log("existsList:" + JSON.stringify(this._existsList, null, 2));

			var message = "<b>" + this.tr("datamapper.create_classes") + "</b><br/><br/>";
			message += this.tr("datamapper.classes_exists") + ":<br/>";
			message += "<ul>";
			for (var i = 0; i < this._existsList.length; i++) {
				message += "<li>" + this._existsList[i] + "</li>";

			}
			message += "</ul><br/>";

			message += this.tr("datamapper.new_classes") + ":<br/>";
			message += "<ul>";
			for (var i = 0; i < this._newList.length; i++) {
				message += "<li>" + this._newList[i] + "</li>";

			}
			message += "</ul><br/>";
			this._createForm(message);
		},
		_getExistsList: function (allList, newList) {
			var retList = [];
			for (var i = 0; i < newList.length; i++) {
				var entity = newList[i];
				if (allList.indexOf(entity) != -1 ) {
					retList.push(entity);
				}
			}
			return retList;
		},
		_toStringList: function (mapList) {
			var stringList = [];
			for (var i = 0; i < mapList.length; i++) {
				stringList.push(mapList[i].name);	
			}
			return stringList;
		},
		_getEntitytypes: function () {
			try {
				var ret = ms123.util.Remote.rpcSync("entity:getEntitytypes", {
					storeId: this._facade.storeDesc.getStoreId()
				});
				return ret;
			} catch (e) {
				ms123.form.Dialog.alert("Datamapper.getEntitytypes failed:" + e.message);
				return null;
			}
		},
		_getEntitytypeInfo: function (nameList) {
			try {
				var ret = ms123.util.Remote.rpcSync("entity:getEntitytypeInfo", {
					storeId: this._facade.storeDesc.getStoreId(),
					names:nameList
				});
				return ret;
			} catch (e) {
				ms123.form.Dialog.alert("Datamapper.getEntitytypeInfo failed:" + e.message);
				return null;
			}
		},
		_createEntitytypes: function (infoOnly) {
			try {
				var ret = ms123.util.Remote.rpcSync("entity:createEntitytypes", {
					storeId: this._facade.storeDesc.getStoreId(),
					datamapperConfig: this._facade.getConfig(),
					side: this._side,
					infoOnly: infoOnly
				});
				return ret;
			} catch (e) {
				ms123.form.Dialog.alert("Datamapper.createEntitytypes failed:" + e.message);
				return null;
			}
		},
		_removeSettings: function () {
			var etList = this._getEntitytypeInfo(this._existsList);
			var namespace= this._facade.storeDesc.getNamespace();
			var lang= ms123.config.ConfigManager.getLanguage();
			var ds = new ms123.entitytypes.DefaultSettings(namespace,lang);
			ds.deleteMessages(etList);
			ds.deleteResources(etList);	
		},
		_createSettings: function (etList) {
			var namespace= this._facade.storeDesc.getNamespace();
			var lang= ms123.config.ConfigManager.getLanguage();
			var ds = new ms123.entitytypes.DefaultSettings(namespace,lang);
			ds.createMessages(etList);
			ds.createResources(etList);	
		},
		_createClasses:function(){
			try {

			var namespace= this._facade.storeDesc.getNamespace();
				ms123.util.Remote.rpcSync("domainobjects:createClasses", {
					storeId: namespace+"_data"//this._facade.storeDesc.getStoreId()
				});
			} catch (e) {
				ms123.form.Dialog.alert("EntityCreate.updateDb:" + e);
				return;
			}
		},
		_cleanTables: function () {
			if( this._existsList.length==0) return;
			try {
				var namespace= this._facade.storeDesc.getNamespace();
				var result = ms123.util.Remote.rpcSync("nucleus:schemaTool", {
					storeId: namespace +"_data",//this._facade.storeDesc.getStoreId()
					dry: false,
					classes: this._existsList,
					op: "delete"
				});
			} catch (e) {
				ms123.form.Dialog.alert("DatabaseAdmin._cleanTable:" + e);
				return;
			}
		},
		_doAll: function (flags) {
			var kind = flags.get("kind");
			var createSettings = flags.get("createSettings");
			if (kind == "overwrite") {
				this._cleanTables();
				//this._removeSettings();
			}
			var ret = this._createEntitytypes(false);
			if (createSettings) {
				this._createSettings(ret.entityList);
			}
			this._createClasses(false);
			//this._setEntityProperties(data.name);
			ms123.config.ConfigManager.clearCache();
		},
		_createForm: function (message) {
			var buttons = [{
				'label': this.tr("datamapper.generate_classes"),
				'icon': "icon/22/actions/dialog-ok.png",
				'value': 1
			},
			{
				'label': this.tr("composite.select_dialog.cancel"),
				'icon': "icon/22/actions/dialog-cancel.png",
				'value': 2
			}];
			var formData = {
				"createSettings": {
					'type': "CheckBox",
					'label': this.tr("datamapper.with_msgs_and_settings"),
					'value': true
				},
				"kind": {
					'height': 40,
					'type': "RadioGroup",
					'label': this.tr("datamapper.exist_classes"),
					'value': "change",
					'options': [{
						value: "overwrite",
						label: this.tr("datamapper.overwrite_classes")
					},
					{
						value: "change",
						label: this.tr("datamapper.change_classes")
					}]
				}
			};

			var self = this;
			var form = new ms123.form.Form({
				"buttons": buttons,
				"tabs": [{
					id: "tab1",
					layout: "single"
				}],
				"useScroll": false,
				"formData": formData,
				"hide": false,
				"width": 650,
				"message": message,
				"inWindow": true,
				"callback": function (m, v) {
					if (m !== undefined) {
						form.hide();
						if (v == 1) {
							self._doAll(m);
						} else if (v == 2) {}
					}
				},
				"context": self
			});
			form.show();
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
