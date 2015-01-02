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
simpl4.util.BaseManager.extend("simpl4.util.FormManager", {
	getForm: function(name) {
		var failed = function(details) {
			alert("GetForm failed" + ":" + details.message);
		};

		try {
			var ret = simpl4.util.Rpc.rpcSync("git:searchContent", {
				reponame: simpl4.util.BaseManager.getNamespace(),
				name: name,
				type:"sw.form"
			});
			return ret;
		} catch (e) {
			failed(e);
			return [];
		}
	}
}, {});
