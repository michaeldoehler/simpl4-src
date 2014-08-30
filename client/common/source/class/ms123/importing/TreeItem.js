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
     2004-2011 1&1 Internet AG, Germany, http://www.1und1.de

   License:
     LGPL: http://www.gnu.org/licenses/lgpl.html
     EPL: http://www.eclipse.org/org/documents/epl-v10.php
     See the LICENSE file in the project's top-level directory for details.

   Authors:
     * Sebastian Werner (wpbasti)
     * Fabian Jakobs (fjakobs)
     * Christian Hagendorn (chris_schmidt)

************************************************************************ */

qx.Class.define("ms123.importing.TreeItem", {
	extend: qx.ui.tree.VirtualTreeItem,

	properties: {
		leadIcon: {
			check: "String",
			event: "changeLeadIcon",
			nullable: true
		},

		mapping: {
			check: "String",
			event: "changeMapping",
			nullable: true
		}
	},

	members: {
		__leadIcon: null,
		__mapping: null,

		_addWidgets: function () {
			var leadIcon = this.__leadIcon = new qx.ui.basic.Image();
			this.bind("leadIcon", leadIcon, "source");
			leadIcon.setWidth(16);

			// Here's our indentation and tree-lines
			this.addSpacer();
			this.addOpenButton();

			// The standard tree icon follows
			this.addIcon();
			this.setIcon("icon/16/places/user-desktop.png");


			// The label
			this.addLabel();


			this.addWidget(leadIcon);

			// All else should be right justified
			this.addWidget(new qx.ui.core.Spacer(), {
				flex: 1
			});

			var text = this.__mapping = new qx.ui.basic.Label();
			this.bind("mapping", text, "value");
			text.setWidth(80);
			this.addWidget(text);
		}
	}
});
