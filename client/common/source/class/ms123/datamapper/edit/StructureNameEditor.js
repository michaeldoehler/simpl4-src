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
/*
*/

qx.Class.define("ms123.datamapper.edit.StructureNameEditor", {
	extend: ms123.datamapper.edit.AbstractFieldsEditor,
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (facade, context, data) {
		var title = this.tr("datamapper.edit_structure_name");
		this.base(arguments,facade,context, title,data);
	},

	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		_createForm:function(){
			var formData = {
				"name": {
					'type': "TextField",
					'label': this.tr("datamapper.name"),
					'validation': {
						required: true
					},
					'value': ""
				},
				"map2parent": {
					'type': "Checkbox",
					'label': this.tr("datamapper.map2parent"),
					'value': true
				}
			};
			return this.__createForm(formData, "single");
		},
		__createForm:function(formData, layout){
			var context = {};
			context.formData = formData;
			context.buttons = [];
			context.formLayout = [{
				id: "tab1", lineheight:-1
			}];
			var form = new ms123.widgets.Form(context);
			return form;
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
