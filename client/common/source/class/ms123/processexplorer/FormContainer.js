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
	* @ignore(Hash)
	* @ignore(Clazz)
*/
qx.Class.define("ms123.processexplorer.FormContainer", {
  extend: ms123.processexplorer.FormWindow,
	include: [qx.locale.MTranslation],

	/**
	 * Constructor
	 */
	construct: function (context) {
		this.base(arguments,null,null);
		this._parenContainer = context.parentContainer;
		if( context.window){
			this._parenContainer = new qx.ui.container.Composite(new qx.ui.layout.Dock());
			context.window.add(this._parenContainer );
			this._parenContainer.setLayout(new qx.ui.layout.Dock());
		}
	},
	statics: {
		__formCache: {}
	},

	/**
	 * ****************************************************************************
	 * MEMBERS
	 * ****************************************************************************
	 */
	members: {
		_init: function () {
		},
		close: function () {
		},
		destroy: function () {
			this._parenContainer._removeAll();
		},
		open: function (params) {
			if( params.taskName ){
				//this._window.setCaption(params.processName+"/"+params.taskName);
			}else{
				//this._window.setCaption(params.processName);
			}
			var form = this.createForm(params);
			if (this._parenContainer._hasChildren()) {
				this._parenContainer._removeAll();
			}
			this._parenContainer._add(form, {
				edge: "center"
			});
			this._form = form;
		},
		_createFormWindow: function (name) {
		}
	}
});
