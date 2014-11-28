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
qx.Class.define('ms123.shell.InternalPlugin', {
 extend: qx.core.Object,
	implement: ms123.shell.IShellPlugin,
	include: qx.locale.MTranslation,


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
		getContextMenuActions: function () {
			var contextMenu = [
			{
				nodetypes: ["sw.directory", "sw.project"],
				clazz: ms123.shell.views.NewDirectory,
				menuicon: "icon/16/actions/list-add.png",
				title: this.tr("shell.new_directory"),
				kind: "dialog"
			},
/*			{
				nodetypes: ["sw.directory"],
				clazz: ms123.shell.views.Details,
				icon: "icon/16/actions/view-restore.png",
				title: this.tr("shell.details"),
				defaultEntry:true,
				kind: "tab"
			},*/
			{
				nodetypes: ["sw.project","sw.directory"],
				clazz: ms123.shell.views.NewFile,
				menuicon: "icon/16/actions/list-add.png",
				title: this.tr("shell.new_file"),
				kind: "dialog"
			},
			{
				nodetypes: ms123.shell.FileType.getAllEditables().concat(["sw.directory"]).concat(ms123.shell.FileType.getAllForeigns()),
				clazz: ms123.shell.views.DelNode,
				menuicon: "icon/16/actions/list-remove.png",
				title: this.tr("shell.del_node"),
				kind: "dialog"
			},
			{
				nodetypes: ms123.shell.FileType.getAllEditables().concat(["sw.directory"]).concat(ms123.shell.FileType.getAllForeigns()),
				clazz: ms123.shell.views.MoveNode,
				menuicon: "icon/16/actions/edit-paste.png",
				title: this.tr("shell.rename_node"),
				kind: "dialog"
			},
			{
				nodetypes: ms123.shell.FileType.getAllEditables().concat(ms123.shell.FileType.getAllForeigns()),
				clazz: ms123.shell.views.CopyNode,
				menuicon: "icon/16/actions/edit-copy.png",
				title: this.tr("shell.copy_node"),
				kind: "dialog"
			},
	/*		{
				nodetypes: ms123.shell.FileType.getAllEditables(),
				clazz: ms123.shell.views.Editor,
				menuicon: "icon/16/apps/utilities-text-editor.png",
				title: this.tr("shell.editor"),
				defaultEntry:true,
				kind: "tab"
			},*/
			{
				nodetypes: ms123.shell.FileType.getAllJsonEditables(),
				clazz: ms123.shell.views.JsonEditor,
				menuicon: "icon/16/apps/utilities-text-editor.png",
				title: this.tr("shell.jsoneditor"),
				kind: "tab"
			},
			{
				nodetypes: ms123.shell.FileType.getAllTextEditables(),
				clazz: ms123.shell.views.SimpleTextEditor,
				menuicon: "icon/16/apps/utilities-text-editor.png",
				title: this.tr("shell.texteditor"),
				tabtitle: "%n",
				kind: "tab"
			},
			{
				nodetypes: ["sw.module"],
				clazz: ms123.shell.views.Editor,
				menuicon: "icon/16/apps/utilities-text-editor.png",
				title: this.tr("shell.editor"),
				tabtitle: "%n",
				kind: "tab"
			}];
			return contextMenu;
		},

		/**
		 */
		getOnClickActions: function () {
			var actions = [ {
				nodetypes: ms123.shell.FileType.getAllEditables(),
				clazz: ms123.shell.views.Editor,
				menuicon: "icon/16/apps/utilities-text-editor.png",
				title: "%n",
				defaultEntry:true,
				kind: "tab"
			}];
			return actions;
		},

		/**
		 */
		prepareNode: function (model,level) {
			if (model.id == "filter" && level == 1) {
				model.title = this.tr("shell.filter_dir");
			}
			if (model.id == "forms" && level == 1) {
				model.title = this.tr("shell.forms_dir");
			}
			if (model.id == "rules" && level == 1) {
				model.title = this.tr("shell.rules_dir");
			}
			if (model.id == "processes" && level == 1) {
				model.title = this.tr("shell.processes_dir");
			}
		},
		onOpenNode: function (e) {},

		getExcludePaths: function () {return null},
		/**
		 */
		getIconMapping: function () {
			return ms123.shell.FileType.getIconMapping();
		}
	}
});
