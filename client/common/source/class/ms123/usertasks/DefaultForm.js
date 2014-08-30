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

qx.Class.define("ms123.usertasks.DefaultForm", {
 extend: qx.core.Object,
 include : qx.locale.MTranslation,

	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function () {
		this.base(arguments);
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
			var processInstanceId = params.processInstanceId;
			var formDesc = params.formDesc;
			var callback = params.callback;
			this.user = params.user;
			var pdata = qx.util.Serializer.toJson(formDesc);
			var isProcess = (callback instanceof ms123.processexplorer.ProcessExplorer);
			var formData = formDesc.formData;
			var addProcessVariables = formDesc.processVariables;
			if( addProcessVariables == null ) addProcessVariables = {};

			formData = this._preprocessFormData( formData );

//			var app = qx.core.Init.getApplication();
//			var a = app.toString();
//			var appid = a.substring(0, a.indexOf("."));

			var app = qx.core.Init.getApplication();
			var appid = app.getUserData("appid");

			addProcessVariables["appid"] = appid;

			var buttons = [{
				'label': isProcess ? this.tr("processes.form.start") : this.tr("tasks.form.start"),
				'icon': "icon/22/actions/dialog-ok.png",
				'value': 0
			  },{
				'label': this.tr("tasks.form.cancel"),
				'icon': "icon/22/actions/dialog-cancel.png",
				'value': 1
			}];
			var _this = this;
			var form = new ms123.form.Form({
				"message": "",
				"formData": formData,
				"buttons": buttons,
				"allowCancel": true,
				"inWindow": true,
				"callback": function (m,v) {
					if( v == 0 ){
						var processVariables = addProcessVariables;
						if( m ){
							var props = qx.Class.getProperties(m.constructor);
							props.forEach(function (p,index) {
								var val = m.get(p);
								if( val ){
									processVariables[p] = val;
								}
							});
						}
						processVariables["processDefinitionId"] = processInstanceId;
						callback._completeActivity( processVariables );
					}
				},
				"context": this
			});
			form.show();
			return form;
		},
		_preprocessFormData: function(fd){
			for (var key in fd) {
				if (fd.hasOwnProperty(key)) {
					var obj = fd[key];
					for (var prop in obj) {
						if (obj.hasOwnProperty(prop)) {
							if( obj[prop].match("^%url:")){
								obj[prop] = this._getValues(obj[prop].substring(5));
							}
						}
					}
				}
			}
			return fd;
		},
		_getValues:function(str){
			var v = str.split(";"); 
      var d = ms123.util.Remote.sendSync(v[0]);
			var max = 0;
			if (d.rows) {
				max = d.rows.length;
			}
			var list = [];
			for (var i = 0; i < max; i++) {
				var rec = d.rows[i];
				if (v.length == 2) {
					var option = {};
					option.value = rec[v[1]];
					option.label = rec[v[1]];
					list.push(option);
				}
			}
			return list;
		}
	}
});
