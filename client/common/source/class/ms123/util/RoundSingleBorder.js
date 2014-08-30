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
qx.Class.define("ms123.util.RoundSingleBorder", {
	extend: qx.ui.decoration.Decorator,
	implement: [qx.ui.decoration.IDecorator],

	include: [
	qx.ui.decoration.MBorderRadius, qx.ui.decoration.MBackgroundColor],
	construct: function (width, style, color, radius) {
		this.base(arguments, width, style, color);
		this.setRadius(radius);
	},

	members: {
		getMarkup: function () {
			if (this._markup) {
				return this._markup;
			}

			var styles = {};

			styles.zIndex = "10000";
			this._styleBorderRadius(styles);
			this._styleBorder(styles);

			var html = this._generateBackgroundMarkup(styles);

			return this._markup = html;
		}
	}
});
