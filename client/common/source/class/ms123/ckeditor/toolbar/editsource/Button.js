/**
 * Copyright (c) 2011 Derrell Lipman
 * 
 * License:
 *   LGPL: http://www.gnu.org/licenses/lgpl.html 
 *   EPL : http://www.eclipse.org/org/documents/epl-v10.php
 */

qx.Class.define("ms123.ckeditor.toolbar.editsource.Button", {
	extend: ms123.ckeditor.toolbar.AbstractToolbarEntry,
	include: ms123.ckeditor.toolbar.editsource.MAction,

	construct: function (_ckrte) {
		var control;

		// Call the superclass constructor
		this.base(arguments, _ckrte, false);

		// Instantiate the control.
		control = new qx.ui.form.ToggleButton(null, "rte/toolbar/insert-text.png");

		// Set common button properties
		control.set({
			toolTipText: "Edit Source",
			focusable: false,
			keepFocus: true,
			width: 16,
			height: 16,
			margin: [0, 0]
		});

		// When the button is pressed, call the appropriate action
		control.addListener("execute", this._action, this);

		// Save this control
		this.setControl(control);
	}
});
