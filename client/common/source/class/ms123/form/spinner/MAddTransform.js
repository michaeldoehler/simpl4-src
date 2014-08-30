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
/** **********************************************************************

   qooxdoo - the new era of web development

   http://qooxdoo.org

   Copyright:
     2008 Derrell Lipman

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Derrell Lipman

************************************************************************ */

/**
 * A mixin to add a "transform" key to Spinner's "value" property.
 */
qx.Mixin.define("ms123.form.spinner.MAddTransform", {
	construct: function () {
		// KLUDGE: Add a 'transform' key
		//
		// Since there's currently no way for subclasses to add to a property of
		// the superclass using legitimate code, kludge it by futzing with
		// internals of the Class and Property systems.  This is *not* a good
		// long-term solution!
		if (!qx.ui.form.Spinner.$$properties["value"].transform) {
			qx.ui.form.Spinner.$$properties["value"].transform = "_transformValue";
		}
	},

	members: {
		_transformValue: function (value) {
			return value;
		}
	}
});
