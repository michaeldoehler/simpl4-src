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
 * Specific data cell renderer for dates.
 */
qx.Class.define("ms123.util.Remote", {

/*
  *****************************************************************************
     PROPERTIES
  *****************************************************************************
  */

	/******************************************************************************
	 STATICS
	 ******************************************************************************/
	statics: {
		_username: "",
		_password: "",
		_lastuse: 0,
		_sessionTimeout:60*1000*30,
		_getPassword: function () {
			var now = new Date().getTime();
			var time = now - ms123.util.Remote._lastuse;
			if( ms123.util.Remote._lastuse!=0 && time  > ms123.util.Remote._sessionTimeout){
				ms123.form.Dialog.alert(qx.locale.Manager.tr('util.remote.session-timeout'),function(e){
					window.location.reload();
				});

			}else{
				ms123.util.Remote._lastuse = new Date().getTime();
			}
			return ms123.util.Remote._password;
		},
		_getUserName: function () {
			return ms123.util.Remote._username;
		},
		getPrefix:function(){
			var i = location.pathname.indexOf("/sw/start");
			return location.pathname.substring(0,i);
		},
		sendSync: function (url, method, type, data, msg) {
			var params = {
				url: url,
				method: method,
				type: type,
				data: data,
				async: false,
				msg: msg
			}
			return ms123.util.Remote.send(params);
		},

		rpcSync: function (dummy,parameter) {
			if (arguments.length < 1) {
				throw "RpcSync not enough args";
			}
			var s = arguments[0].split(":");
			if( s.length != 2 ){
				throw "RpcSync wrong arg(service:method)";
			}
			var service = s[0];
			var method = s[1];
			var newArgs = [];
			newArgs.push(method);
			for (var i = 1; i < arguments.length; i++) {
				newArgs.push(arguments[i]);
			}
				var namespace = "xyz";
				if(  parameter && parameter.namespace ){
					namespace = parameter.namespace;	
				}
			var rpc = new ms123.util.Rpc(this.getPrefix()+"/rpc/"+namespace, service);
//			rpc.setProtocol("2.0");
			rpc.setUsername(ms123.util.Remote._getUserName());
			rpc.setPassword(ms123.util.Remote._getPassword());
			rpc.setUseBasicHttpAuth(true);
			rpc.setTimeout( 5000000 );
			if( newArgs.length == 0)newArgs.push({});
			var result = rpc.callSync.apply(rpc, newArgs);
			return result;
		},

		rpcAsync:function(params){
			if( params.async!=null && params.async == false ){
				try{
					var result = ms123.util.Remote.rpcSync( params.service+":"+params.method, params.parameter);
					if( params.completed ) params.completed.call(params.context,result);
					return result;
				}catch(ex){
					if( params.failed ) params.failed.call(params.context,ex);
					else throw ex;
				}
			}else{
				var namespace = "xyz";
				if(  params.parameter && params.parameter.namespace ){
					namespace = params.parameter.namespace;	
				}
				var rpc = new ms123.util.Rpc(this.getPrefix()+"/rpc/"+namespace, params.service);
				rpc.setUsername(ms123.util.Remote._getUserName());
				rpc.setPassword(ms123.util.Remote._getPassword());
				rpc.setUseBasicHttpAuth(true);
				rpc.setTimeout( 5000000 );
				var handler = function(result, ex) {
					if (ex == null) {
						params.completed.call(params.context,result);
					} else {
						params.failed.call(params.context,ex);
					}
				};
				rpc.callAsync(handler, params.method, params.parameter);	
			}
		},

		send: function (params) {
			var ret = null;
			var url = params.url;
			var method = (params.method != null) ? params.method : "GET";
			var type = (params.type != null) ? params.type : "application/json";
			var async = (params.async != null) ? params.async : false;
			var req = new qx.io.remote.Request(url, method, type);
			if (params.data != null) {
				req.setData(params.data);
			}
			if (params.timeoutvalue != null) {
				req.setTimeout(params.timeoutvalue);
			}
			if (params.contenttype != null) {
				req.setRequestHeader("Content-Type", params.contenttype);
			}
			req.setAsynchronous(async);
			if (params.completed == null) {
				req.addListener("completed", function (e) {
					ret = e.getContent();
					if (params.msg != null) {
						ms123.form.Dialog.alert(params.msg);
					}
				}, this);
			} else {
				req.addListener("completed", params.completed, params.context);
			}
			if (params.failed != null) {
				req.addListener("failed", params.failed, params.context);
			}
			if (params.timeout != null) {
				req.addListener("timeout", params.timeout, params.context);
			}
			req.setUsername(ms123.util.Remote._getUserName());
			req.setPassword(ms123.util.Remote._getPassword());
			req.setUseBasicHttpAuth(true);
			req.send();
			return ret;
		},
		setSessionTimeout:function(timeout){
			if( timeout == undefined ) timeout = 60*1000*30;
			if( timeout == -1) timeout = 60*1000*30000;
			ms123.util.Remote._sessionTimeout = timeout;
		},
		setCredentials: function (username, password) {
			ms123.util.Remote._username = username;
			ms123.util.Remote._password = password;
			ms123.util.Remote._lastuse = new Date().getTime();
		}
	}

});
