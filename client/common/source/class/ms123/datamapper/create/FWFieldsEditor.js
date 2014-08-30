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

qx.Class.define("ms123.datamapper.create.FWFieldsEditor", {
	extend: ms123.datamapper.edit.AbstractFieldsEditor,
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (facade, side) {
		this._side = side;
		var title = this.tr("datamapper.define") + this.tr("datamapper.fixed_width");
		this.base(arguments,facade,null,title);
	},

	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		_createForm:function(){
			var formData = {
				"type": {
					'type': "SelectBox",
					'label': this.tr("datamapper.type"),
					'value': ms123.datamapper.Config.NODETYPE_ELEMENT,
					'options': [{
						value: ms123.datamapper.Config.NODETYPE_ELEMENT,
						label: "Element"
					}]
				},
				"name": {
					'type': "TextField",
					'label': this.tr("datamapper.name"),
					'validation': {
						required: true,
						filter:/[a-zA-Z0-9_]/,
						validator: "/^[A-Za-z]([0-9A-Za-z_]){0,48}$/"
					},
					'value': ""
				},
				"children": this._fieldsFormData()
			};
			if( this._side == ms123.datamapper.Config.OUTPUT){
				formData.type.value = ms123.datamapper.Config.NODETYPE_COLLECTION;
				formData.type.options[0].value = ms123.datamapper.Config.NODETYPE_COLLECTION;
				formData.type.options[0].label = "List<Element>";
			}
			return this.__createForm(formData, "single");
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
