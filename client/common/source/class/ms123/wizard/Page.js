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
 * A wizard consists of a collection of pages which are all one step in the
 * wizard.
 */
qx.Class.define("ms123.wizard.Page", {
	extend: qx.ui.groupbox.GroupBox,
	implement: ms123.wizard.IPage,

	/**
	 *****************************************************************************
	 CONSTRUCTOR
	 *****************************************************************************
	 */
	/**
	 * param legend {String} The label of the page.
	 * param icon {String} The icon of the page.
	 */
	construct: function (legend, icon) {
		this.base(arguments, legend, icon);
	},

	/**
	 *****************************************************************************
	 PROPERTIES
	 *****************************************************************************
	 */
	properties: {
		// overridden
		appearance: {
			refine: true,
			init: "wizard-page"
		},

		/**
		 * Previous page to navigate to.
		 */
		previous: {
			check: "ms123.wizard.Page",
			nullable: true,
			init: null,
			event: "changePrevious"
		},

		/**
		 * Next page to navigate to.
		 */
		next: {
			check: "ms123.wizard.Page",
			nullable: true,
			init: null,
			event: "changeNext"
		},

		/**
		 * Whether to allow to go to the previous wizard pane.
		 */
		allowPrevious: {
			check: "Boolean",
			init: false,
			event: "changeAllowPrevious"
		},

		/**
		 * Whether to allow to go to the next wizard pane.
		 */
		allowNext: {
			check: "Boolean",
			init: false,
			event: "changeAllowNext"
		}
	}
});
