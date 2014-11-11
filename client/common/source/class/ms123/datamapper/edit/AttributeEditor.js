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

qx.Class.define("ms123.datamapper.edit.AttributeEditor", {
	extend: ms123.datamapper.edit.AbstractFieldsEditor,
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (facade, context, data) {
		var title = this.tr("datamapper.edit_attribute");
		this._side = context.side;
		this._data  = data;
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
						required: true,
						filter:/[a-zA-Z0-9_]/,
						validator: "/^[A-Za-z]([0-9A-Za-z_]){0,48}$/"
					},
					'value': ""
				},
				"fieldType": {
					'type': "SelectBox",
					'label': this.tr("datamapper.datatype"),
					'value': "string",
					'options': this._getDatatypes()
				},
				"type": {
					'type': "SelectBox",
					'label': this.tr("datamapper.nodetype"),
					'value': "string",
					'options': this._getNodetypes()
				},
				"fieldFormat": {
					'type': "TextField",
					'label': this.tr("datamapper.format"),
					'value': ""
				},
				"fieldWidth" : {
					'type': "NumberField",
					'label': this.tr("datamapper.width"),
					'validation': {
						required: true
					},
					'value': 20 
				}
			};
			if( this._format != ms123.datamapper.Config.FORMAT_FW){
				delete formData.fieldWidth;
			}
			if( this._format == ms123.datamapper.Config.FORMAT_MAP ){
				formData.fieldFormat.exclude= "fieldType!='string'";
			}
			if( this._side != ms123.datamapper.Config.OUTPUT || !this._data.root){
				delete formData.type;
			}
			if( this._format == ms123.datamapper.Config.FORMAT_POJO){
				delete formData.fieldFormat;
			}
			if( this._data.type == ms123.datamapper.Config.NODETYPE_COLLECTION ||
					this._data.type == ms123.datamapper.Config.NODETYPE_ELEMENT
				){
				delete formData.fieldType;
				delete formData.fieldFormat;
			}
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
