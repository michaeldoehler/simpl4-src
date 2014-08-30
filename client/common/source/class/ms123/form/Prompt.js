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
 qooxdoo dialog library
 
 http://qooxdoo.org/contrib/project#dialog
 
 Copyright:
 2007-2010 Christian Boulanger
 
 License:
 LGPL: http://www.gnu.org/licenses/lgpl.html
 EPL: http://www.eclipse.org/org/documents/epl-v10.php
 See the LICENSE file in the project's top-level directory for details.
 
 Authors:
 *  Christian Boulanger (cboulanger)
 ************************************************************************ */

/**
 * Confirmation popup singleton
 */
qx.Class.define("ms123.form.Prompt", {
	extend: ms123.form.Dialog,

	/**
	 *****************************************************************************
	 PROPERTIES
	 *****************************************************************************
	 */
	properties: {
		/**
		 * The default value of the textfield
		 * @type {String}
		 */
		value: {
			check: "String",
			nullable: true,
			apply: "_applyValue",
			event: "changeValue"
		}
	},

	/**
	 *****************************************************************************
	 MEMBERS
	 *****************************************************************************
	 */
	members: {

		/**
		 ---------------------------------------------------------------------------
		 PRIVATE MEMBERS
		 ---------------------------------------------------------------------------
		 */
		_textField: null,

		/**
		 ---------------------------------------------------------------------------
		 WIDGET LAYOUT
		 ---------------------------------------------------------------------------
		 */

		/**
		 * Create the main content of the widget
		 */
		_createWidgetContent: function () {

			/**
			 * groupbox
			 */
			var groupboxContainer = new qx.ui.groupbox.GroupBox().set({
				contentPadding: [16, 16, 16, 16]
			});
			groupboxContainer.setLayout(new qx.ui.layout.VBox(10));
			this.add(groupboxContainer);

			var hbox = new qx.ui.container.Composite;
			hbox.setLayout(new qx.ui.layout.HBox(10));
			groupboxContainer.add(hbox);

			/**
			 * Add message label
			 */
			this._message = new qx.ui.basic.Label();
			this._message.setRich(true);
			this._message.setWidth(200);
			this._message.setAllowStretchX(true);
			hbox.add(this._message, {
				flex: 1
			});

			/**
			 * textfield
			 */
			this._textField = new qx.ui.form.TextField();
			this._textField.addListener("changeValue", function (e) {
				this.setValue(e.getData());
			}, this);
			this.addListener("show", function () {
				this.setValue("");
			}, this);
			groupboxContainer.add(this._textField);


			/**
			 * buttons pane
			 */
			var buttonPane = new qx.ui.container.Composite;
			var bpLayout = new qx.ui.layout.HBox(5)
			bpLayout.setAlignX("center");
			buttonPane.setLayout(bpLayout);
			buttonPane.add(this._createOkButton());
			buttonPane.add(this._createCancelButton());
			groupboxContainer.add(buttonPane);

		},

		/**
		 ---------------------------------------------------------------------------
		 APPLY METHODS
		 ---------------------------------------------------------------------------
		 */

		_applyValue: function (value, old) {
			this._textField.setValue(value);
		},

		/**
		 ---------------------------------------------------------------------------
		 EVENT HANDLERS
		 ---------------------------------------------------------------------------
		 */

		/**
		 * Handle click on the OK button
		 */
		_handleOk: function () {
			this.hide();
			if (this.getCallback()) {
				this.getCallback().call(this.getContext(), this.getValue());
			}
		}
	}
});
