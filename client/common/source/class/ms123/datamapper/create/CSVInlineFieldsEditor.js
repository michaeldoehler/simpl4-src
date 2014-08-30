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

qx.Class.define("ms123.datamapper.create.CSVInlineFieldsEditor", {
	extend: ms123.datamapper.create.CSVFieldsEditor,
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (facade, side) {
		this.base(arguments,facade,side);
	},

	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		_init:function(){
			this._form = this._createForm(this._facade);
		},
		_createForm:function(){
			var formData = {
				"quote": {
					'type': "ComboBox",
					'label': this.tr("export.csv.quote"),
					'value': '\"',
					'options': [{
						'label': "\""
					},
					{
						'label': "'"
					}]
				},
				"columnDelim": {
					'type': "ComboBox",
					'label': this.tr("export.csv.col_delimeter"),
					'value': ',',
					'options': [{
						'label': ","
					},
					{
						'label': "TAB"
					},
					{
						'label': ";"
					}]
				},
				"header": {
					'type': "CheckBox",
					'label': this.tr("export.csv.include_column_header"),
					'value': true
				}
			};
			return this.__createForm(formData, "single");
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
