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
*/
qx.Class.define('ms123.baseeditor.BaseManager', {
	extend: qx.core.Object,
	include: [qx.locale.MTranslation, ms123.baseeditor.MPlugin],

	construct: function (context) {
		this.setWindow(context.window);
		this._pluginsData = [];
		this._eventsQueue = [];
		this._eventListeners = new Hash();
		this._facade = this.getPluginFacade();
		this._facade.storeDesc = context.storeDesc;
		this._facade.settingsid = context.settingsid;

		this._registerPluginsOnKeyEvents();
		this._initEventListener();
		var window = this.getWindow();
		window.setLayout(new qx.ui.layout.Grow());

		window.set({
			contentPadding: 2
		});
		this._facade.leftSpace = this._createLeftSpace();
		this._facade.rightSpace = this._createRightSpace();
		this._addPlugins();
		var splitPane = this._splitPane( this._facade.leftSpace, this._facade.rightSpace);
		window.add(splitPane );
	},

	properties: {
		window: {
			check: 'Object'
		}
	},

	members: {
		_addPlugins: function () {
		},

		_createLeftSpace: function () {
			var leftSpace = new qx.ui.container.Composite(new qx.ui.layout.VBox()).set({
				allowGrowY: true,
				allowGrowX: true
			});

			leftSpace.setPadding(0);
			return leftSpace;
		},

		_createRightSpace: function () {
			var rightSpace = new qx.ui.container.Composite(new qx.ui.layout.Dock()).set({ });
			rightSpace.setPadding(0);
			return rightSpace;
		},

		_splitPane: function (left, right) {
			var splitPane = new qx.ui.splitpane.Pane("horizontal").set({
				decorator: null
			});

			splitPane.add(left, 3);
			splitPane.add(right, 8);
			return splitPane;
		}
	}
});
