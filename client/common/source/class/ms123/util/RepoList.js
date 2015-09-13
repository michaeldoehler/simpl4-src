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
qx.Class.define("ms123.util.RepoList", {
 extend: qx.core.Object,
 include : [ qx.locale.MTranslation],


	/**
	 *****************************************************************************
	 CONSTRUCTOR
	 *****************************************************************************
	 */

	construct: function (facade,formElement,config) {
		this.base(arguments);
console.log("RepoList");
		this._facade = facade;	
		var repoList = this._getRepos();
		for(var i=0; i < repoList.length; i++){
			var repoName = repoList[i];
			var listItem = new qx.ui.form.ListItem(repoName,null,repoName);
			formElement.addItem(listItem);
		}
	},

	/**
	 *****************************************************************************
	 MEMBERS
	 *****************************************************************************
	 */

	members: {
		_getRepos: function () {
			var completed = (function (data) {}).bind(this);

			var failed = (function (details) {
				ms123.form.Dialog.alert(this.tr("namespace.getNamespaces") + ":" + details.message);
			}).bind(this);

			try {
				var ret = ms123.util.Remote.rpcSync("git:getRepositories", {
				});
				console.log("namespace:"+JSON.stringify(ret,null,2));
				var repoList = [];
				repoList.push("-");
				for( var i = 0; i< ret.length;i++){
					var r = ret[i];
					repoList.push( r.name );
				}
				console.log("rel:"+JSON.stringify(repoList,null,2));
				return repoList;
			} catch (e) {
				//failed.call(this,e);
				return [];
			}
		}
	}
});
