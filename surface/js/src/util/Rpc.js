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
can.Construct.extend("simpl4.util.Rpc", {
	_getPassword: function() {
		return simpl4.util.BaseManager.getPassword();
	},
	_getUserName: function() {
		return simpl4.util.BaseManager.getUser();
	},
	rpcSync: function(dummy, parameter) {
		if (arguments.length < 1) {
			throw "RpcSync not enough args";
		}
		var s = arguments[0].split(":");
		if (s.length != 2) {
			throw "RpcSync wrong arg(service:method)";
		}
		parameter = parameter || {};
		var service = s[0];
		var method = s[1];
		var newArgs = [];
		newArgs.push(method);
		for (var i = 1; i < arguments.length; i++) {
			newArgs.push(arguments[i]);
		}
		var namespace = "xyz";
		if (parameter && parameter.namespace) {
			namespace = parameter.namespace;
		}
		var url = simpl4.util.BaseManager.getBaseUrl() + "/rpc/" + namespace + "/" + service;

		var requestObject = {
			"service": service,
			"method": method,
			"params": parameter
		};

		var config = {
			completed: parameter.completed,
			failed: parameter.failed,
			context: parameter.context,
			msg: parameter.msg,
		}
		var result = simpl4.util.Rpc._send(url, false, config, requestObject);
		if (!result) {
			throw "Networkerror:result is null";
		}
		if (result.error) {
			throw "ServerError:"+result.error.message;
		}
		if( typeof result.result == 'string'){
			try{
				return JSON.parse(result.result);
			}catch(e){
				return result.result;
			}
		}
		return result.result;
	},

	rpcAsync: function(params) {
		if (params.async != null && params.async == false) {
			try {
				var result = simpl4.util.Rpc.rpcSync(params.service + ":" + params.method, params.parameter);
				if (params.completed) params.completed.call(params.context, result);
				return result;
			} catch (ex) {
				if (params.failed) {
					params.failed.call(params.context, ex);
					return;
				} else throw ex;
			}
		} else {
			var namespace = "xyz";
			if (params.parameter && params.parameter.namespace) {
				namespace = params.parameter.namespace;
			}
		}
		var url = "/rpc/" + namespace + "/" + params.service;
		var requestObject = {
			"service": params.service,
			"method": params.method,
			"params": params.parameter
		};
		var config = {
			completed: params.completed,
			failed: params.failed,
			context: params.context,
			msg: params.msg
		}
		simpl4.util.Rpc._send(url, true, config, requestObject);
	},

	_send: function(url, async, config, requestObject) {
		var ret = null;
		var req = {};
		req.data = JSON.stringify(requestObject);
		req.timeout = 50000000;
		req.contentType = "application/json; charset=utf-8";
		req.dataType = "json";
		req.async = async;
		req.url = url;
		req.type = "POST";
		if (config.completed == null) {
			req.success = function(data, status) {
				ret = data;
				if (config.msg != null) {
					console.error(config.msg);
				}
			};
			req.context = this;
		} else {
			req.success = function(data, status) {
				if (data.result) {
					var result = data.result;
					if( typeof result == 'string'){
						result = JSON.parse(result);
					}
					config.completed.call(config.context, result);
				} else {
					if (config.failed) {
						config.failed.call(config.context, data.error);
					} else {
						console.error(data.error);
					}
				}
			}
		}
		if (config.failed != null) {
			req.error = config.failed
			req.context = config.context;
		}
		var username = simpl4.util.Rpc._getUserName();
		var password = simpl4.util.Rpc._getPassword();
		req.headers = {
			"Authorization": "Basic " + btoa(username + ":" + password)
		}
		$.ajax(req);
		return ret;
	}
}, {});
