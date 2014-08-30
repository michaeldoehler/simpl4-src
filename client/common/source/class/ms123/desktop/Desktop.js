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
qx.Class.define('ms123.desktop.Desktop', {
	extend: qx.ui.window.Desktop,
	include: [ms123.desktop.MDesktopPersist],

	construct: function (namespace, manager) {
		this.base(arguments, manager);

		var am = qx.util.AliasManager.getInstance();
		var file = am.resolve("resource/ms123/wallpaper1.png");
		var deco = new ms123.desktop.Background();
		this.setDecorator(deco);

		this._namespace = namespace;

	},

	/*******************************************************************************
	 EVENTS
	 ***************************************************************************** */
	events: {
		windowAdded: "qx.event.type.Data",
		windowRemoved: "qx.event.type.Data"
	},
	/*******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		add: function (element) {
			this.base(arguments, element);

			if (element instanceof ms123.desktop.Window) {
				this.fireDataEvent('windowAdded', element);
				element.addListener('move', function () {
					var bounds = this.getBounds();

					if (bounds.top < 0) {
						this.moveTo(bounds.left, 0);
					}
				});

				element.addListener('close', function () {
					this.fireDataEvent('windowRemoved', element);
					this.remove(element);
					delete element;
				}, this);
			}
		}
	}
});
