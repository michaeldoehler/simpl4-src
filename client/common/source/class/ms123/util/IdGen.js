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
 * Specific data cell renderer for dates.
 */
qx.Class.define("ms123.util.IdGen", {

/*
  *****************************************************************************
     PROPERTIES
  *****************************************************************************
  */

	/******************************************************************************
	 STATICS
	 ******************************************************************************/
	statics: {
		// private property
		_id: 100,

		// public method for encoding
		nextId: function () {
			var id = ms123.util.IdGen._id++;
			return "xid"+ id;
		},
		id: function (prefix) {
			var res = [],
				hex = '0123456789ABCDEF';

			for (var i = 0; i < 36; i++) res[i] = Math.floor(Math.random() * 0x10);

			res[14] = 4;
			res[19] = (res[19] & 0x3) | 0x8;

			for (var i = 0; i < 36; i++) res[i] = hex[res[i]];

			res[8] = res[13] = res[18] = res[23] = '-';

			return (prefix ? prefix : "ID_") + res.join('');
		}
	}
});
