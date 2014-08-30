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
qx.Class.define("ms123.form.ConstraintEdit", {
	extend: ms123.form.TableEdit,

	/**
	 *****************************************************************************
	 CONSTRUCTOR
	 *****************************************************************************
	 */

	construct: function (config) {
		this.base(arguments,config,ms123.StoreDesc.getGlobalMetaStoreDesc());
	},

	/**
	 *****************************************************************************
	 PROPERTIES
	 *****************************************************************************
	 */
	/**
	 *****************************************************************************
	 MEMBERS
	 *****************************************************************************
	 */

	members: {
		configGridContext:function(context){
			var _this = this;
			var buttons = [ {
				'label': "",
				'icon': "icon/16/actions/list-add.png",
				'callback': function(m){
					_this._table.addRecord( { annotation:"AssertFalse", parameter1:"", parameter2:"", message:"" } );
				},
				'value': "add"
			}, 
			{
				'label': "",
				'icon': "icon/16/places/user-trash.png",
				'callback': function(m){
					_this._table.deleteCurrentRecord();
				},
				'value': "del"
			} ,
			{
				'label': "",
				'icon': "icon/16/actions/go-up.png",
				'callback': function(m){
					_this._table.currentRecordUp();
				},
				'value': "up"
			} ,
			{
				'label': "",
				'icon': "icon/16/actions/go-down.png",
				'callback': function(m){
					_this._table.currentRecordDown();
				},
				'value': "down"
			} 
			];


			context.buttons=buttons;
			var cols = context.model.attr("colModel");
			this._replaceMap = {};
			for (var i = 0; i < cols.length; i++) {
				if(cols[i].name == "annotation" || cols[i].name == "parameter1" || cols[i].name == "parameter2" || cols[i].name == "message"){
					cols[i].tableedit=true;
				}
				if(cols[i].name == "annotation"){
					this._colNumAnno = i;
				}
			}
		},
		propagateTable:function(table){
			this._table = table;

//			var colModel = table.getTable().getTableColumnModel();
//			var colRenderer = new qx.ui.table.cellrenderer.Replace();
//			var _this = this;
//			colRenderer.setReplaceFunction( function(x){
//				return _this._replaceMap[x];
//			});
//			colModel.setDataCellRenderer(this._colNumAnno-1 , colRenderer );
			

			table.getTable().addListener("cellClick", function(e) {
//  			this.startEditing();
			}, table.getTable());
		}
	}
});
