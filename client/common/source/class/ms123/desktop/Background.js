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
qx.Class.define("ms123.desktop.Background", {
	extend: qx.ui.decoration.Decorator,

	construct: function (backgroundColor) {
		this.base(arguments);

		if (backgroundColor != null) {
			this.setBackgroundColor(backgroundColor);
		}
	},

	members: {
		// overridden
		_getDefaultInsets: function () {
			return {
				top: 0,
				right: 0,
				bottom: 0,
				left: 0
			};
		}
	}
});
