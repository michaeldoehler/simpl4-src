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
qx.Class.define('ms123.messages.Plugin', {
	extend: qx.core.Object,
	implement: ms123.shell.IShellPlugin,
	include: qx.locale.MTranslation,


	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (facade) {
		this.base(arguments);
		this._facade = facade;
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
			var contextMenu = [{
				nodetypes: ["sw.messages"],
				clazz: ms123.messages.NewLang,
				menuicon: "icon/16/actions/list-add.png",
				title: this.tr("messages.new_messageslang"),
				kind: "dialog"
			}];
			return contextMenu;
		},

		/**
		 */
		getOnClickActions: function () {
			var onclick = [{
				nodetypes: ["sw.messageslang"],
				clazz: ms123.messages.Editor,
				menuicon: "icon/16/actions/list-add.png",
				tabicon: "icon/16/actions/format-text-bold.png",
				title: "%n",
				kind: "tab"
			}];
			return onclick;
		},

		/**
		 */
		prepareNode: function (model,level) {
			if (model.id == "messages" && level == 1) {
				model.title = this.tr("messages.messages_dir");
				model.type = "sw.messages";
			}
		},
		onOpenNode: function (e) {},

		getExcludePaths: function () {return null},
		/**
		 */
		getIconMapping: function () {
			var iconMap = {};
			iconMap["sw.messages"] = "sw.directory";
			iconMap["sw.messageslang"] =  "icon/16/actions/format-text-bold.png";
			return iconMap;
		}
	}
});
