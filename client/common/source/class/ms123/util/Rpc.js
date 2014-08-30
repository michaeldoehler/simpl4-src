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
qx.Class.define("ms123.util.Rpc", {
	extend: qx.io.remote.Rpc,

/*
  *****************************************************************************
     PROPERTIES
  *****************************************************************************
  */

	properties: {},


/* 
  *****************************************************************************
     MEMBERS 
  *****************************************************************************
  */

	members: {
		createRpcData: function (id, method, parameters, serverData) {
			var requestObject;
			var service;

			// Create a protocol-dependent request object
			if (this.getProtocol() == "qx1") {
				// Create a qooxdoo-modified version 1.0 rpc data object
				console.warn("--> method("+method+"):"+qx.util.Serializer.toJson(parameters));
				requestObject = {
					"service": method == "refreshSession" ? null : this.getServiceName(),
					"method": method,
					"id": id,
					"params": parameters.pop()
				};

				// Only add the server_data member if there is actually server data
				if (serverData) {
					requestObject.server_data = serverData;
				}
			} else {
				// If there's a service name, we'll prepend it to the method name
				service = this.getServiceName();
				if (service && service != "") {
					service += ".";
				} else {
					service = "";
				}

				// Create a standard version 2.0 rpc data object
				requestObject = {
					"jsonrpc": "2.0",
					"method": service + method,
					"id": id,
					"params": parameters
				};
			}

			return requestObject;
		}
	}
});
