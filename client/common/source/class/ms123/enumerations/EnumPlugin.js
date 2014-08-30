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
qx.Class.define('ms123.enumerations.EnumPlugin', {
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
				nodetypes: ["sw.enums"],
				clazz: ms123.enumerations.NewEnum,
				menuicon: "icon/16/actions/list-add.png",
				title: this.tr("enumerations.new_enum"),
				kind: "dialog"
			}];
			return contextMenu;
		},

		/**
		 */
		getOnClickActions: function () {
			var onclick = [{
				nodetypes: ["sw.enum"],
				clazz: ms123.enumerations.EnumEditor,
				menuicon: "icon/16/actions/list-add.png",
				tabicon: "resource/ms123/enum.png",
				title: "%n",
				kind: "tab"
			}];
			return onclick;
		},

		/**
		 */
		prepareNode: function (model) {
			if (model.id == "enumerations") {
				model.title = this.tr("enumerations.enumeration_dir");
				model.type = "sw.enums";
			}
		},
		onOpenNode: function (e) {},

		getExcludePaths: function () {return null},
		/**
		 */
		getIconMapping: function () {
			var iconMap = {};
			iconMap["sw.enums"] = "sw.directory";
			iconMap["sw.enum"] = "resource/ms123/enum.png";
			return iconMap;
		}
	}
});
