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
can.Construct.extend("s4.util.Remote", {
	_username: "",
	_password: "",
	setCredentials: function (username, password) {
		s4.util.Remote._username = username;
		s4.util.Remote._password = password;
	},
	_getPassword: function () {
		return s4.util.Remote._password;
	},
	_getUserName: function () {
		return s4.util.Remote._username;
	},
	rpcSync: function (dummy, parameter) {
		if (arguments.length < 1) {
			throw "RpcSync not enough args";
		}
		var s = arguments[0].split(":");
		if (s.length != 2) {
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
		if (parameter && parameter.namespace) {
			namespace = parameter.namespace;
		}
		var url = "/rpc/" + namespace + "/" + service;

		var requestObject = {
			"service": service,
			"method": method,
			"id": id,
			"params": parameter
		};

		var config = {
			completed: parameter.completed,
			failed: parameter.failed,
			context: parameter.context,
			msg: parameter.msg,
		}
		var result = s4.util.Remote._send(url, false, config, requestObject);
		return result;
	},

	rpcAsync: function (params) {
		if (params.async != null && params.async == false) {
			try {
				var result = s4.util.Remote.rpcSync(params.service + ":" + params.method, params.parameter);
				if (params.completed) params.completed.call(params.context, result);
				return result;
			} catch (ex) {
				if (params.failed) params.failed.call(params.context, ex);
				else throw ex;
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
			"id": id,
			"params": params.parameter
		};
		var config = {
			completed: parameter.completed,
			failed: parameter.failed,
			context: parameter.context,
			msg: parameter.msg
		}
		s4.util.Remote._send(url, true, config, requestObject);
	},

	_send: function (url, async, config, requestObject) {
		var ret = null;
		var req = {};
		req.data = requestObject;
		req.timeout = 50000000;
		req.contentType = "application(json";
		req.async = async;
		req.type = "POST";
		if (config.completed == null) {
			req.success = function (e) {
				ret = e.getContent();
				if (config.msg != null) {
					console.error(config.msg);
				}
			};
			req.context = this;
		} else {
			req.success = config.completed;
			req.context = config.context;
		}
		if (config.failed != null) {
			req.error = config.failed
			req.context = config.context;
		}
		req.username = s4.util.Remote._getUserName();
		req.password = s4.util.Remote._getPassword();
		console.log("Ajax:"+JSON.stringify(req,null,2));
		return $.ajax(req);
	}
});
