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
	@asset(qx/icon/${qx.icontheme}/22/actions/dialog-cancel.png)
	@asset(qx/icon/${qx.icontheme}/22/actions/dialog-ok.png)
	@asset(qx/icon/${qx.icontheme}/22/actions/document-save.png)
*/


/**
 * Base class for dialog widgets
 */
qx.Class.define("ms123.form.DialogForm", {
	extend: ms123.form.Dialog,

	/**
	 *****************************************************************************
	 STATICS
	 *****************************************************************************
	 */
	statics: {

		form: function (message, formData, callback, context, inWindow) {
			(new ms123.form.Form({
				"message": message,
				"formData": formData,
				"allowCancel": true,
				"callback": callback,
				"context": context || null,
				"inWindow": inWindow !== undefined ? inWindow : true
			})).show();
		}
	}
});
