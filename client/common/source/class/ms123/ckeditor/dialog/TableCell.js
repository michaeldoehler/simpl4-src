/**
 * Copyright (c) 2011 Derrell Lipman
 * 
 * License:
 *   LGPL: http://www.gnu.org/licenses/lgpl.html 
 *   EPL : http://www.eclipse.org/org/documents/epl-v10.php
 */

/**
	@ignore(CKEDITOR.plugins.tabletools.getSelectedCells)
	@ignore(CKEDITOR.tools.cssLength)
 */

qx.Class.define("ms123.ckeditor.dialog.TableCell", {
	extend: ms123.ckeditor.dialog.AbstractDialog,

	construct: function (ckrte_editor) {
		// Call the superclass constructor
		this.base(arguments, this.tr("Cell Properties"), ckrte_editor);
	},

	members: {
		_selectedImage: null,

		/**
		 * //overridden
		 * @ignore(CKEDITOR,localfile)
		 */
		_create: function (ckrte_editor) {
			var selection;

			// Save the CkEditor editor object
			this.setCkEditor(ckrte_editor.getCkEditor());

			this.cells = CKEDITOR.plugins.tabletools.getSelectedCells( this.getCkEditor().getSelection() );

			this.setGridConfiguration(

			function (grid) {
				grid.setSpacingX(5);
				grid.setSpacingY(15);
				grid.setColumnAlign(0, "right", "middle");
			});

			this.setDialogConfiguration([
			{
				label: this.tr("Width"),
				model: "txtWidth",
				type: qx.ui.form.ComboBox,
				col: 0,
				items: [
				{
					value: "40px"
				},
				{
					value: "40%"
				}],
				set: {
				//	filter: /[0-9]/
				},
				setup: function (elem) {
					return this.cells[0].getAttribute("width") ;
				}
			},
			/*{
				label: this.tr("Border size"),
				model: "txtBorder",
				type: qx.ui.form.TextField,
				value: "1",
				col: 0,
				set: {
					filter: /[0-9]/
				},
				setup: function (elem) {
					if (this._selectedImage) {
						return (
						this._getPx(this._selectedImage.getStyle("border-width")) || "");
					}

					return undefined;
				}
			},*/
			{
				label: this.tr("Alignment"),
				model: "cmbAlign",
				type: qx.ui.form.SelectBox,
				col: 0,
				items: [{
					label: this.tr("<not set>"),
					model: "inherit"
				},
				{
					label: this.tr("Center"),
					model: "center"
				},
				{
					label: this.tr("Left"),
					model: "left"
				},
				{
					label: this.tr("Right"),
					model: "right"
				}],
				setup: function (elem) {
					return this.cells[0].getAttribute("align");
				}
			},
			{
				label: this.tr("Vertical Alignment"),
				model: "cmbVAlign",
				type: qx.ui.form.SelectBox,
				col: 0,
				items: [{
					label: this.tr("<not set>"),
					model: "inherit"
				},
				{
					label: this.tr("Middle"),
					model: "middle"
				},
				{
					label: this.tr("Top"),
					model: "top"
				},
				{
					label: this.tr("Bottom"),
					model: "bottom"
				}],
				setup: function (elem) {
					return this.cells[0].getAttribute("valign");
				}
			}

			]);

			this.base(arguments);

		},

		/**
		 * Convert an alphanumeric string representing a number of pixels, e.g.,
		 * "8px" to a string containing on the number, e.g., "8".
		 * 
		 * param str {String}
		 *   The string which (presumably) contains a number followed by "px"
		 * 
		 * @return {String}
		 *   A string containing only the numeric portion of the number of pixels
		 */
		_getPx: function (str) {
			// If there's a pixel string...
			if (str && str.length > 0) {
				// ... then extract the number from the beginning, and convert to string
				return "" + parseInt(str, 10);
			}

			// We were given no string to parse.
			return "";
		},

		/**
		 * //overridden
		 * @ignore(CKEDITOR)
		 */
		_onOk: function (model) {
			var ckeditor = this.getCkEditor();
			var selection = ckeditor.getSelection();
			var bms = selection.createBookmarks();

			var info = qx.util.Serializer.toNativeObject(this._model);
console.log("info:"+JSON.stringify(info));
			for(var i=0; i < this.cells.length;i++){
				this._configureCellAttributes(this.cells[i], info);
			}

			selection.selectBookmarks(bms);

			this.close();
		},

		/**
		 * Alter the attributes of an cell based on provided data.
		 * 
		 * param cell {Node}
		 *   The <cell> node to be modified
		 * 
		 * param info {Map}
		 *   A map of data to apply to the cell node.
		 * 
		 * @ignore(CKEDITOR)
		 */
		_configureCellAttributes: function (cell, info) {
			// Add the width property
			if (info.txtWidth && info.txtWidth.length > 0) {
				cell.setStyle("width", CKEDITOR.tools.cssLength(info.txtWidth));
				cell.setAttribute("width", info.txtWidth);
			} else {
				cell.removeStyle("width");
				cell.removeAttribute("width");
			}

			// Add the border width property
			if (info.txtBorder && info.txtBorder.length > 0) {
				cell.setStyle("border-width", CKEDITOR.tools.cssLength(info.txtBorder));
				cell.setStyle("border-style", "solid");
			} else {
				cell.removeStyle("border-width");
				cell.removeStyle("border-style");
			}

			cell.setAttribute("align", info.cmbAlign);
			cell.setAttribute("valign", info.cmbVAlign);
		}
	}
});
