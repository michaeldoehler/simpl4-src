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
qx.Class.define('ms123.ruleseditor.DecisionTable', {

	extend: qx.core.Object,
	include: [qx.locale.MTranslation],

	construct: function (facade) {
		this._facade = facade;
  	var m = qx.locale.Manager.getInstance();
    this.__lang = m.getLanguage();
		this._buildTable();
		this._addListeners();
	},

	properties: {
		table: {
			check: 'Object'
		},
		model: {
			check: 'Object'
		}
	},

	members: {
		_buildTable: function () {
			var model = new  qx.ui.table.model.Simple();
			this.setModel(model);
			
			var colArr = [];
			var colLabelArr = [];
			var ccols = this._facade.getConditionColumns();
			var acols = this._facade.getActionColumns();
			ccols.each( (function(col,index){
				var header = {
					header1:this.tr("ruleseditor.bedingung")+" "+index,
					header2:col.getVariableName(),
					header3:col.getVariableType(),
					header4:col.getOperationText()
				};
				colArr.push( "C"+index);
				colLabelArr.push( header);
			}).bind(this));
			acols.each( (function(col,index){
				var header = {
					header1:this.tr("ruleseditor.ergebnis")+" "+index,
					header2:col.getVariableName(),
					header3:col.getVariableType(),
					header4:''
				};
				colArr.push( "A"+index);
				colLabelArr.push( header);
			}).bind(this));
			model.setColumns(colLabelArr,colArr);

			var table = new ms123.ruleseditor.Table(model, {
				tablePane: function (obj) {
					return new qx.ui.table.pane.Pane(obj);
				},
				tablePaneScroller: function (obj) {
					return new ms123.ruleseditor.PaneScroller(obj);
				},
				tableColumnModel: function (obj) {
					return new ms123.ruleseditor.ColumnModel(obj);
				}
			}).set({
				columnVisibilityButtonVisible: false,
				dataRowRenderer: new ms123.ruleseditor.RowRenderer()
			});

			qx.Class.include(qx.ui.table.Table, qx.ui.table.MTableContextMenu);
			table.setHeaderCellHeight(60);
			this.setTable(table);

			var columnCount = model.getColumnCount();
			for (var i = 0; i < columnCount; ++i) {
				model.setColumnEditable(i, true);
				var config = null;
				if( i < ccols.length){
					config  = this._getCellEditor(ccols[i]);
				}else{
					config= this._getCellEditor(acols[i-ccols.length]);
				}
				if( config.renderer ){
					table.getTableColumnModel().setDataCellRenderer(i, config.renderer);
				}
				if( config.editor ){
					table.getTableColumnModel().setCellEditorFactory(i, config.editor);
				}
				if( config.width ){
					var resizeBehavior = table.getTableColumnModel().getBehavior();
					resizeBehavior.setWidth(i, config.width);
				}
			}
			for (var i = 0; i < columnCount; ++i) {
				model.setColumnSortable(i, false);
			}

			table.getSelectionModel().setSelectionMode(qx.ui.table.selection.Model.SINGLE_SELECTION);
			this.updateRules();
		},

		_getCellEditor: function(cc){
			var ret = {};
			var vartype = cc.getVariableType();	
			ret.editor =  new ms123.ruleseditor.StringCellEditor();
			ret.width = 100;
			if( vartype.toLowerCase()=="string"){
			}else if( vartype.toLowerCase()=="boolean"){
				ret.editor =  new ms123.ruleseditor.BooleanCellEditor();
				ret.renderer =  new qx.ui.table.cellrenderer.Boolean();
			}else if( vartype.toLowerCase()=="date"){
				ret.width = 150;
				ret.editor =  new ms123.ruleseditor.DateCellEditor();
				ret.renderer = new ms123.ruleseditor.DateCellRenderer();
				if( this.__lang == "de" ){
					var format = new qx.util.format.DateFormat("dd.MM.yyyy"); //@@@MS TODO
					ret.renderer.setDateFormat(format);
				}
			}else if( vartype.toLowerCase()=="decimal"){
				ret.editor =  new ms123.ruleseditor.DecimalCellEditor();
			}else if( vartype.toLowerCase()=="integer"){
				ret.editor =  new ms123.ruleseditor.IntCellEditor();
			}
			return ret;
		},
		updateRules: function(){
			var ccols = this._facade.getConditionColumns();
			var acols = this._facade.getActionColumns();
			var countRules = this._facade.getCountRules();
			var data = new Array();
			for( var r=0; r < countRules; r++){
				data.push({});
			}

			ccols.each( function(col,index){
				var colData = col.getData();
				//var colName = col.getName();
				var colName = "C"+index;
				for( var r=0; r < countRules; r++){
					var map = data[r];
					map[colName] = colData[r];
				}
			});
			acols.each( function(col,index){
				var colData = col.getData();
				//var colName = col.getName();
				var colName = "A"+index;
				for( var r=0; r < countRules; r++){
					var map = data[r];
					map[colName] = colData[r];
				}
			});
			console.log("updateRules:"+data.length);
			this.getModel().setDataAsMapArray(data, true);
		},

		_addListeners: function () {
			this.getTable().addListener('cellTap', function (e) {
				var table = this.getTable();
				var vartype=null;
				var col = table.getFocusedColumn();
				if (col >= this._facade.getConditionColumns().length) {
					vartype = this._facade.getActionColumns()[col - this._facade.getConditionColumns().length].getVariableType();
				} else {
					vartype = this._facade.getConditionColumns()[col].getVariableType();
				}
				if( vartype == "boolean") table.startEditing();
			}, this);

			this.getTable().addListener('cellDblTap', function (e) {
				this.startEditing();
			}, this.getTable());
		}
	},

	statics: {
		ROWS: 0
	}
});
