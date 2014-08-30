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
qx.Class.define("ms123.util.Clone", {

/*
  *****************************************************************************
     PROPERTIES
  *****************************************************************************
  */

	/******************************************************************************
	 STATICS
	 ******************************************************************************/
	statics: {
		clone: function (item) {
			if (!item) {
				return item;
			}
			var types = [Number, String, Boolean];
			var result;

			types.forEach(function (type) {
				if (item instanceof type) {
					result = type(item);
				}
			});

			if (typeof result == "undefined") {
				if (Object.prototype.toString.call(item) === "[object Array]") {
					result = [];
					item.forEach(function (child, index, array) {
						result[index] = ms123.util.Clone.clone(child);
					});
				} else if (ms123.util.Clone.isRegExp(item)) {
					result = ms123.util.Clone.regexpClone(item);
				} else if (typeof item == "object") {
					if (!item.prototype) {
						// it is an object literal
						result = {};
						for (var i in item) {
							result[i] = ms123.util.Clone.clone(item[i]);
						}
					} else {
						result = item;
					}
				} else {
					result = item;
				}
			}
			return result;
		},
		isRegExp: function (o) {
			return 'object' == typeof o && '[object RegExp]' == Object.prototype.toString.call(o);
		},
		regexpClone: function (regexp) {
			var flags = [];
			if (regexp.global) flags.push('g');
			if (regexp.multiline) flags.push('m');
			if (regexp.ignoreCase) flags.push('i');
			return new RegExp(regexp.source, flags.join(''));
		},
		merge: function (target, varargs) {
			var len = arguments.length;

			for (var i = 1; i < len; i++) {
				qx.lang.Object.mergeWith(target, arguments[i]);
			}
			return target;
		}
	}
});
