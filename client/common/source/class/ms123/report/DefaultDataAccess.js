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
qx.Class.define('ms123.report.DefaultDataAccess', {
 extend: qx.core.Object,
	implement: ms123.report.IDataAccess,


	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (context) {
		this.base(arguments);
		this.__storeDesc = context.storeDesc;
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
		getReports: function ( p ) {
			var rpcParams ={
				namespace : this.__storeDesc.getNamespace()
			}

			var params = {
				service: "report",
				method: "getReports",
				parameter: rpcParams,
				async: false
			}
			return ms123.util.Remote.rpcAsync(params);
		},

		/**
		 */
		createReport: function ( p ) {
			var rpcParams ={
				namespace : this.__storeDesc.getNamespace(),
				name : p.name,
				report : p.report
			}

			var params = {
				service: "report",
				method: "saveReport",
				parameter: rpcParams,
				async: false
			}
			return ms123.util.Remote.rpcAsync(params);
		},

		/**
		 */
		updateReport: function ( p ) {
			var rpcParams ={
				namespace : this.__storeDesc.getNamespace(),
				name:p.id,
				report : p.report
			}

			var params = {
				service: "report",
				method: "saveReport",
				parameter: rpcParams,
				async: false
			}
			return ms123.util.Remote.rpcAsync(params);
		},

		/**
		 */
		deleteReport: function ( p ) {
			var rpcParams ={
				namespace : this.__storeDesc.getNamespace(),
				name:p.id
			}

			var params = {
				service: "report",
				method: "deleteReport",
				parameter: rpcParams,
				async: false
			}
			return ms123.util.Remote.rpcAsync(params);
		}
	}
});
