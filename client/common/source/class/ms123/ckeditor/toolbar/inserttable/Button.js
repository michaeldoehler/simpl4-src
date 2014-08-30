/**
 * Copyright (c) 2011 Derrell Lipman
 * 
 * License:
 *   LGPL: http://www.gnu.org/licenses/lgpl.html 
 *   EPL : http://www.eclipse.org/org/documents/epl-v10.php
 */

qx.Class.define("ms123.ckeditor.toolbar.inserttable.Button", {
	extend: ms123.ckeditor.toolbar.AbstractSimpleButton,
	include: ms123.ckeditor.toolbar.inserttable.MAction,

	construct: function (_ckrte) {
		// Define the tooltip and image to use for this button
		var tooltip = "Insert Table";
		var image = "rte/toolbar/insert-table.png";

		// Call the superclass constructor
		this.base(arguments, _ckrte, tooltip, image);
	}
});
