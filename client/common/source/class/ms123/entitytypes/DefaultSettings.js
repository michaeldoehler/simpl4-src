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
 */
qx.Class.define("ms123.entitytypes.DefaultSettings", {
	extend: qx.core.Object,
	include: [qx.locale.MTranslation],

	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (namespase,language) {
		this._namespace = namespase;
		this._language = language;
	},

	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		createResources:function(_etList,cm,sf,st,ss){
			cm = cm!=null ? cm : true;
			sf = sf!=null ? sf : true;
			st = st!=null ? st : true;
			ss = ss!=null ? ss : true;
			var etList = _etList;
			if( !Array.isArray(_etList)){
				etList = [_etList];
			}
			for( var i=0; i< etList.length;i++){
				var et = etList[i];
				var fieldSettings = {
					fields: []
				}

				var fields = et.fields;
				var keys = Object.keys(fields);
				for (var j = 0; j < keys.length; j++) {
					var key = keys[j];
					var f = fields[key];
					var msgid = "data." + et.name + "." + f.name;

					var sfield = {
						enabled: true,
						name: f.name,
						tab: "tab1",
						displayname: cm ? this._capitaliseFirstLetter(f.name) : msgid
					}
					fieldSettings.fields.push(sfield);
				}

				if (sf) this._setResourceSetting("entities." + et.name + ".views.main-form.fields", fieldSettings);
				if (st) this._setResourceSetting("entities." + et.name + ".views.main-grid.fields", fieldSettings);
				if (ss) this._setResourceSetting("entities." + et.name + ".views.search.fields", fieldSettings);
			}
		},
		deleteResources: function (_etList) {
			var etList = _etList;
			if( !Array.isArray(_etList)){
				etList = [_etList];
			}
			var resourceRegex = "";
			var or = "";
			for( var i=0; i< etList.length;i++){
				var et = etList[i];
				resourceRegex += or + "(^entities\."+et.name+"\..*)"
				or = "|";
			}

			var failed = (function (details) {
				ms123.form.Dialog.alert("DeleteSettings:" + details.message);
			}).bind(this);

			try {
				 ms123.util.Remote.rpcSync("setting:deleteResourceSetting", {
					namespace: this._namespace,
					settingsid: "global",
					resourceid: resourceRegex
				});
			} catch (e) {
				failed.call(this, e);
				return;
			}
		},
		deleteMessages: function (_etList) {
			var etList = _etList;
			if( !Array.isArray(_etList)){
				etList = [_etList];
			}

			var messages = [];
			for( var i=0; i< etList.length;i++){
				var et = etList[i];
				var fields = et.fields;
				var msgid = "data." + et.name;
				messages.push(msgid);
				var keys = Object.keys(fields);
				for (var j = 0; j < keys.length; j++) {
					var key = keys[j];
					var f = fields[key];
					msgid = "data." + et.name + "." + f.name;
					messages.push(msgid);
				}
				this._deleteMessage(messages,  "data."+et.name+"._team_list");
			}
			var failed = (function (details) {
				ms123.form.Dialog.alert("DeleteMessages:" + details.message);
			}).bind(this);

			try {
				var ret = ms123.util.Remote.rpcSync("message:deleteMessages", {
					namespace: this._namespace,
					lang: this._language,
					msgIds: messages
				});
			} catch (e) {
				failed.call(this, e);
				return;
			}
		},
		createMessages: function (_etList) {
			var etList = _etList;
			if( !Array.isArray(_etList)){
				etList = [_etList];
			}
			var messages = [];
			for( var i=0; i< etList.length;i++){
				var et = etList[i];
				var fields = et.fields;
				var msg = {
					msgid: "data."+et.name,
					msgstr: this._capitaliseFirstLetter(et.name)
				}
				messages.push(msg);
				msg = {
					msgid: "data." + et.name + ".id",
					msgstr: "Id"
				}
				messages.push(msg);

				var keys = Object.keys(fields);
				for (var j = 0; j < keys.length; j++) {
					var key = keys[j];
					var f = fields[key];
					var msgid = "data." + et.name + "." + f.name;

					var msg = {
						msgid: msgid,
						msgstr: this._capitaliseFirstLetter(f.name)
					}
					messages.push(msg);
				}
				this._createMessage(messages,  "data."+et.name+"._team_list", "Teams");
			}
			var failed = (function (details) {
				ms123.form.Dialog.alert("entitytypes.createMessages:" + details.message);
			}).bind(this);

			try {
				var ret = ms123.util.Remote.rpcSync("message:addMessages", {
					namespace: this._namespace,
					lang: this._language,
					overwrite:false,
					msgs: messages
				});
			} catch (e) {
				failed.call(this, e);
				return;
			}
		},
		_createMessage:function(messages,key,txt){
				var msg = {
					msgid: key,
					msgstr: txt
				}
				messages.push(msg);
		},
		_deleteMessage:function(messages,key){
				messages.push(key);
		},
		_setResourceSetting: function (resourceid, settings) {
			var failed = (function (details) {
				ms123.form.Dialog.alert(this.tr("entitytypes.addSettings") + ":" + details.message);
			}).bind(this);

			try {
				var curSetting = this._getResourceSetting(resourceid);
				if( curSetting && curSetting.fields){
					var curFields = curSetting.fields;
					var newFields = settings.fields;
					for( var i=0; i< newFields.length;i++){
						var newField=newFields[i];
						if(  !this._isFieldinList( curFields, newField)){
							curFields.push(newField);
						}
					}
				}else{
					curSetting = settings;
				}
				var ret = ms123.util.Remote.rpcSync("setting:setResourceSetting", {
					namespace: this._namespace,
					settingsid: "global",
					resourceid: resourceid,
					overwrite:true,
					settings: curSetting
				});
			} catch (e) {
				failed.call(this, e);
				return;
			}
		},

		_getResourceSetting: function (resourceid, settings) {
			var failed = (function (details) {
				ms123.form.Dialog.alert(this.tr("entitytypes.getSettings") + ":" + details.message);
			}).bind(this);

			try {
				var ret = ms123.util.Remote.rpcSync("setting:getResourceSetting", {
					namespace: this._namespace,
					settingsid: "global",
					resourceid: resourceid
				});
				return ret;
			} catch (e) {
				failed.call(this, e);
				return;
			}
		},
		_isFieldinList:function(list, field){
			if( list == null) return false;
			for( var i=0; i< list.length;i++){
				var f=list[i];
				if(f.name == field.name){
					return true;
				}
			}
			return false;
		},
		_capitaliseFirstLetter: function (s) {
			return s.charAt(0).toUpperCase() + s.slice(1);
		}
	}
});
