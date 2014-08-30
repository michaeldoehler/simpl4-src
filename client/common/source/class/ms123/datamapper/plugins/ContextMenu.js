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
 * @ignore(Hash)
 * @ignore($A)
 */
qx.Class.define("ms123.datamapper.plugins.ContextMenu", {
	extend: qx.core.Object,
	include: [qx.locale.MTranslation],

	/**
	 * Constructor
	 */
	construct: function (facade, context) {
		this.base(arguments);
		this._facade = facade;
		this._context = context;

		if (context.side == ms123.datamapper.Config.INPUT_SIDE) {
			this._facade.registerOnEvent(ms123.datamapper.Config.EVENT_INPUTTREE_CREATED, this._treeCreated.bind(this));
		} else {
			this._facade.registerOnEvent(ms123.datamapper.Config.EVENT_OUTPUTTREE_CREATED, this._treeCreated.bind(this));
		}
	},

	/**
	 * ****************************************************************************
	 * MEMBERS
	 * ****************************************************************************
	 */
	members: {
		_treeCreated: function () {
			this.groupIndex = new Hash();
			var pluginsData = this._facade.getPluginsData();
			this.plugs = [];
			var newPlugs = pluginsData.sortBy((function (value) {
				return ((this.groupIndex[value.group] != undefined ? this.groupIndex[value.group] : "") + value.group + "" + value.index).toLowerCase();
			}).bind(this));
			var plugs = $A(newPlugs).findAll(qx.lang.Function.bind(function (value) {
				return !this.plugs.include(value) && (value.target === ms123.datamapper.plugins.ContextMenu)
			}, this));
			if (plugs.length < 1) return;

			plugs.each((function (value) {
				if (!value.name) {
					return
				}
				this.plugs.push(value);
			}).bind(this));

			var tree = this._context.tree;
/*var cc = table.getTableColumnModel().getVisibleColumnCount();
			for (var c = 0; c < cc; c++) {
				table.setContextMenuHandler(c, this._contextMenuHandler.bind(this));
			}*/
			console.log("Tree:" + tree);
		},

		_contextMenuHandler: function (col, row, table, dataModel, contextMenu) {
			this.entries = [];
			var currentGroupsName = this.plugs.last() ? this.plugs.last().group : this.plugs[0].group;
			this.plugs.each((function (value) {
				console.log("contextMenu.value:" + value);
				// Add seperator if new group begins
				if (currentGroupsName != value.group) {
					//this._toolbar.add( new qx.ui.toolbar.Separator());
					currentGroupsName = value.group;
				}

				if (value.addFill) {
					//this._toolbar.addSpacer();
				} else {
					var menuEntry = new qx.ui.menu.Button(value.name, value.icon);
					menuEntry.addListener("execute", value.functionality, this);

					if (value.description) {
						menuEntry.setToolTipText(value.description);
					}

					menuEntry.setUserData("id", value.id);
					value['menuEntryInstance'] = menuEntry;
					contextMenu.add(menuEntry);
				}
				this.entries.push(value);
			}).bind(this));
			this.enableEntries([]);
			return true;
		},

		enableEntries: function (elements) {
			this.entries.each((function (value) {
				value.menuEntryInstance.setEnabled(true);
				if (value.isEnabled && !value.isEnabled(value.menuEntryInstance)) value.menuEntryInstance.setEnabled(false);
			}).bind(this));
		}
	}
});
