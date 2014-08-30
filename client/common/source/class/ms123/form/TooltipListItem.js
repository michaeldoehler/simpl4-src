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

   Authors:
     * Manfred Sattler

************************************************************************ */

/**
 * A item with tooltips  for a list. Could be added to all List like widgets but also
 * to the {@link qx.ui.form.SelectBox} and {@link qx.ui.form.ComboBox}.
 */
qx.Class.define("ms123.form.TooltipListItem", {
	extend: qx.ui.form.ListItem,

	/**
	 *****************************************************************************
	 CONSTRUCTOR
	 *****************************************************************************
	 */

	construct: function (label, icon, model) {
		this.base(arguments, label, icon);
	},

	members: {
		setTooltip: function (value) {
			this.getChildControl("label", false).setToolTipText(value);
		}
	}
});
