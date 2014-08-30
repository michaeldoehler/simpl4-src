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
qx.Class.define('ms123.shell.PluginManager', {
 extend: qx.core.Object,
	implement: ms123.shell.IShellPlugin,


	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (plugins) {
		this.base(arguments);
		this._contextMenuActions = [];
		this._onClickActions = [];
		this._iconMapping = {};
		this._excludePaths = [];
		for( var i=0; i< plugins.length;i++){
			this._contextMenuActions = this._contextMenuActions.concat(plugins[i].getContextMenuActions());
			this._onClickActions = this._onClickActions.concat(plugins[i].getOnClickActions());
			if( plugins[i].getExcludePaths && plugins[i].getExcludePaths()!=null ){
				this._excludePaths = this._excludePaths.concat(plugins[i].getExcludePaths());
			}
			qx.lang.Object.mergeWith(this._iconMapping, plugins[i].getIconMapping());
		}
		this._plugins = plugins;
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
		getContextMenuActions: function () {
			return this._contextMenuActions;
		},

		/**
		 */
		getOnClickActions: function () {
			return this._onClickActions;
		},

		/**
		 */
		getExcludePaths: function () {
			return this._excludePaths;
		},

		/**
		 */
		prepareNode: function (model,level) {
			for( var i=0; i< this._plugins.length;i++){
				this._plugins[i].prepareNode(model,level);
			}
		},
		onOpenNode: function (e) {
			for( var i=0; i< this._plugins.length;i++){
				this._plugins[i].onOpenNode(e);
			}
		},

		/**
		 */
		getIconMapping: function () {},
		getNodeTypeIcon: function (type) {
			var icon = this._iconMapping[type];
			if( icon ) return icon;
			return "resource/ms123/file.png";
		}
	}
});
