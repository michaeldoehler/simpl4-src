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
qx.Class.define("ms123.pdf.Style", {
	extend: qx.core.Object,

	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function () {},

	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},

	/******************************************************************************
	 STATICS
	 ******************************************************************************/
	statics: {
		_prefixes: ['ms', 'Moz', 'Webkit', 'O'],
		_cache: {},
		getProp: function (propName, element) {
			if (arguments.length == 1 && typeof ms123.pdf.Style._cache[propName] == 'string') {
				return ms123.pdf.Style._cache[propName];
			}

			element = element || document.documentElement;
			var style = element.style,
				prefixed, uPropName;

			if (typeof style[propName] == 'string') {
				return (ms123.pdf.Style._cache[propName] = propName);
			}
			uPropName = propName.charAt(0).toUpperCase() + propName.slice(1);
			for (var i = 0, l = ms123.pdf.Style._prefixes.length; i < l; i++) {
				prefixed = ms123.pdf.Style._prefixes[i] + uPropName;
				if (typeof style[prefixed] == 'string') {
					return (ms123.pdf.Style._cache[propName] = prefixed);
				}
			}
			return (ms123.pdf.Style._cache[propName] = 'undefined');
		},
		setProp: function (propName, element, str) {
			var prop = ms123.pdf.Style.getProp(propName);
			if (prop != 'undefined') {
				element.style[prop] = str;
			}
		}
	},


	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {}
});
