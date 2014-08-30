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
qx.Class.define('ms123.widgets.UserDataAccess', {
	extend: ms123.widgets.DefaultDataAccess,


	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (context) {
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
		/**
		 */
		insert: function (p) {
			var rpcParams = {
				userid: p.id,
				data: p.data
			}

			var params = {
				service: "auth",
				method: "createUser",
				parameter: rpcParams,
				async: false
			}
			return ms123.util.Remote.rpcAsync(params);
		},
		query: function (p) {
			var params = {
				service: "auth",
				method: "getUsers",
				parameter: {filter:p.filter},
				context: p.context,
				failed: p.failed,
				completed: p.completed,
				async: p.async
			}
			return ms123.util.Remote.rpcAsync(params);
		},

		queryOne: function (p) {
			var params = {
				service: "auth",
				method: "getUser",
				parameter: {userid:p.id},
				context: p.context,
				failed: p.failed,
				completed: p.completed,
				async: p.async
			}
			return ms123.util.Remote.rpcAsync(params);
		},

		/**
		 */
		update: function (p) {
			return this.insert(p);
		},

		/**
		 */
		'delete': function (p) {
			var params = {
				service: "auth",
				method: "deleteUser",
				parameter: {userid:p.id},
				async: false
			}
			return ms123.util.Remote.rpcAsync(params);
		}
	}
});
