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
	* @ignore(jQuery)
	* @ignore($)
*/
qx.Class.define("ms123.form.MultiUseBox", {
	extend: qx.ui.core.Widget,

	/**
	 *****************************************************************************
	 CONSTRUCTOR
	 *****************************************************************************
	 */

	construct: function (useitCheckboxes) {
		this.base(arguments);
		var layout = new qx.ui.layout.HBox();
		this._setLayout(layout);

		this.setMaxWidth(40);
		this.setMaxHeight(25);
		this._createChildControl("selectbox");

	},

	/**
	 *****************************************************************************
	 PROPERTIES
	 *****************************************************************************
	 */

	properties: {
	},

	/**
	 *****************************************************************************
	 MEMBERS
	 *****************************************************************************
	 */

	members: {
		// overridden
		_createChildControlImpl: function (id, hash) {
			var control;
			switch (id) {
			case "selectbox":
				control = new qx.ui.form.SelectBox();
				control.setFocusable(false);
				control.setKeepActive(true);
				control.addState("inner");
				var tempItem = new qx.ui.form.ListItem("", "icon/16/actions/media-playback-stop.png", "ignore");
				tempItem.setToolTipText(this.tr("multiusebox.ignore"));
				control.add(tempItem);
				tempItem = new qx.ui.form.ListItem("", "icon/16/actions/dialog-ok.png", "replace");
				tempItem.setToolTipText(this.tr("multiusebox.replace"));
				control.add(tempItem);
				tempItem = new qx.ui.form.ListItem("", "icon/16/actions/list-add.png", "add");
				tempItem.setToolTipText(this.tr("multiusebox.add"));
				control.add(tempItem);
				tempItem = new qx.ui.form.ListItem("", "icon/16/actions/list-remove.png", "remove");
				tempItem.setToolTipText(this.tr("multiusebox.remove"));
				control.add(tempItem);
				control.setMaxWidth(35);
				this._add(control);
				break;
			}
			return control;
		},
		setValue: function (val) {
			if( typeof val == "boolean" ){
				val = "ignore";
			}
			var sb = this.getChildControl("selectbox");
			var widgets = sb.getSelectables(true);
			for( var i=0;i < widgets.length;i++){
				var w = widgets[i];
				if( w.getModel() == val ){
					sb.setSelection([w]);
					break;
				}
			}
		},
		getValue: function () {
			var sb = this.getChildControl("selectbox");
			var selection = sb.getSelection();
			return selection[0].getModel();
		}
	}
});
