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
	@asset(qx/icon/${qx.icontheme}/16/actions/*)
	@asset(qx/icon/${qx.icontheme}/16/places/*)
	@asset(qx/icon/${qx.icontheme}/16/status/*)
	@asset(qx/icon/${qx.icontheme}/16/apps/*)
*/

/**
 */
qx.Class.define("ms123.form.MutableList", {
	extend: qx.ui.container.Composite

/*
 *****************************************************************************
  CONSTRUCTOR
 *****************************************************************************
 */

	/**
	 * Create a new custom button
	 * 
	 * param label {String} The label to associate with this list
	 * param list {qx.data.Array?} List of prepopulated entries
	 */
	,
	construct: function (label, title, extra) {
		this.base(arguments);

		// Establish the basic layout of the widget
		var l = new qx.ui.layout.Grid();
		l.setRowFlex(2, 1);
		l.setColumnFlex(0, 1);
		l.setSpacing(3);
		this.setLayout(l);

		// Create the label
		this._add(new qx.ui.basic.Label(label), {
			row: 0,
			column: 0
		});

		// Create the entry box
		this.__addBox = new qx.ui.form.TextField();
		this.__addBox.setValue('');
		var add = new qx.ui.form.Button(null, "icon/16/actions/list-add.png");
		var del = new qx.ui.form.Button(null, "icon/16/actions/list-remove.png");
		this._add(this.__addBox, {
			row: 1,
			column: 0
		});
		this._add(add, {
			row: 1,
			column: 1
		});
		this._add(del, {
			row: 1,
			column: 2
		});

		// Create the "list", using a table for editing's sake
		// Table model is needed first
		this.__model = new qx.ui.table.model.Simple();
		//		this.__model.setColumnEditable(0, true);
		//		this.__model.setData([ [] ]);
		var colHeader = new Array();
		var colIds = new Array();
		var colRenderer = new Array();
		var colEditor = new Array();
		colRenderer.push(null);
		colEditor.push(null);

		if ( title instanceof Array && title.length>1) {
			colHeader.push(title[0]);
			colIds.push(title[1]);
			this._firstColId = title[1];
		}else{
			colHeader.push(title);
			colIds.push(title);
			this._firstColId = title;
		}
		this._extraCols = [];
		if (extra && extra instanceof Array) {
			this._addExtraColums( colRenderer, colEditor, colHeader, colIds, extra );
			this._extraCols = extra;
		}

		this.__model.setColumns(colHeader,colIds);

		var customMap = {
			tableColumnModel: function (obj) {
				return new qx.ui.table.columnmodel.Resize(obj);
			}
		};

		// Now the widget itself
		this.__list = new qx.ui.table.Table(this.__model, customMap);
		this.__list.set({
			height: 100,
			statusBarVisible: false
		});
		this.__list.getSelectionModel().setSelectionMode(qx.ui.table.selection.Model.SINGLE_SELECTION);
		this._add(this.__list, {
			row: 2,
			column: 0,
			colSpan: 3
		});

		var columnModel = this.__list.getTableColumnModel();
		for (var c = 0; c < colRenderer.length; c++) {
			if (colRenderer[c]) {
				columnModel.setDataCellRenderer(c, colRenderer[c]);
			}
		}
		for (var c = 0; c < colEditor.length; c++) {
			if (colEditor[c]) {
				columnModel.setCellEditorFactory(c, colEditor[c]);
				this.__model.setColumnEditable(c, true);
			}
		}

		// Behavioral listeners
		add.addListener('click', this.__add, this);
		del.addListener('click', this.__del, this);
	},
	members: {
		__model: null,
		__addBox: null,
		__list: null
/** **************************************************************
	 * PUBLICS
	 * ****************************************************************/

		/**
		 * Adds an item to the list
		 *
		 * param t {String} An element to add to the list
		 */
		,
		addEntry: function (t) {
console.log("addEntry:"+qx.util.Serializer.toJson(t));
			if( typeof t == "string" ){
				var map = {};
				map[this._firstColId] = t;
				for( var i=0; i< this._extraCols.length;i++){
					var col = this._extraCols[i];
					map[col.name] = col.defaultValue;
				}
				this.__model.addRowsAsMapArray([map], null, true);
			}else{
				this.__model.addRowsAsMapArray([t], null, true);
			}
		}

		/**
		 * Instead of a parameter, because they seem to be slightly broken
		 * in this instance.
		 * 
		 * param t {Array} An array of String.
		 */
		,
		setData: function (t) {
			this.__model.setDataAsMapArray(t, true);
		}

		/**
		 * Instead of a paramter, because they seem slightly broken in this
		 * instance
		 * 
		 * @return {Array} An array of strings that the user has put into
		 * this widget.
		 */
		,
		getData: function () {
			var data = this.__model.getDataAsMapArray();
			console.log("getData:"+qx.util.Serializer.toJson(data));
			return data;
		}

/** **************************************************************
	 * PRIVATES
	 * ****************************************************************/
		/**
		 * Event callback when the 'add' button is clicked.
		 */
		,
		__add: function (e) {
			var t = this.__addBox.getValue().trim();
			if (t != '') {
				this.addEntry(t);
				this.__addBox.setValue('');
				this.__addBox.focus();
			}
		}

		/**
		 * Event callback when the 'delete' button is clicked.
		 */
		,
		__del: function (e) {
			var sel = this.__list.getSelectionModel();

			var idxs = new qx.type.Array();
			// Let us do it all at once
			sel.iterateSelection(function (idx) {
				idxs.push(idx);
			}, this);

			// This is dumb, but it appears to be a necessary workaround to
			// allow me to remove rows from the table on demand.
			var ct = 0;
			idxs.forEach(function (a) {
				console.log("Removing " + a);
				this.__model.removeRows(a - ct, 1);
				ct += 1;
			}, this);
		},
		_addExtraColums: function (colRenderer, colEditor, colHds, colIds, cols) {
			for (var i = 0; i < cols.length; i++) {
				var col = cols[i];
				colIds.push(col.name);
				if (col.label === undefined) {
					col.label = col.name;
				}
				colHds.push(col.label);
				if (col.datatype == "date") {
					colRenderer.push(new ms123.util.DateRenderer());
				} else {
					if (col.edittype == "select") {
						var r = new qx.ui.table.cellrenderer.Replace();
						colRenderer.push(r);
					} else {
						colRenderer.push(new qx.ui.table.cellrenderer.Default());
					}
				}
				if (col.edittype == "select") {
					var selectbox = new qx.ui.table.celleditor.SelectBox();
					var listData = [];
					if (col.selectable_items.length !== undefined) {
						for (var j = 0; j < col.selectable_items.length; j++) {
							var o = col.selectable_items[j];
							var option;
							if( typeof o == "string" ){
								option = [o, null, o];
							}else{
								option = [o.label, null, o.value];
							}
							listData.push(option);
						}
					} else {
						for (var key in col.selectable_items) {
							var value = col.selectable_items[key]
							var option = [value, null, key];
							listData.push(option);
						}
					}
					var replaceMap = {};
					listData.forEach(function (row) {
						if (row instanceof Array) {
							replaceMap[row[0]] = row[2];
						}
					});
					var renderer = colRenderer[colRenderer.length - 1];
					renderer.setReplaceMap(replaceMap);
					renderer.addReversedReplaceMap();

					if (col.editable && col.editable == true) {
						selectbox.setListData(listData);
						colEditor.push(selectbox);
					} else {
						colEditor.push(null);
					}
				} else if (col.editable && col.editable == true && col.edittype == "combo") {
					var selectbox = new qx.ui.table.celleditor.ComboBox();
					var listData = [];
					for (var key in col.selectable_items) {
						var value = col.selectable_items[key]
						var option = [value, null, key];
						listData.push(option);
					}
					selectbox.setListData(listData);
					colEditor.push(selectbox);
				} else if (col.editable && col.editable == true) {
					var textfield = new qx.ui.table.celleditor.TextField();
					colEditor.push(textfield);
				} else {
					colEditor.push(null);
				}
			}
		}
	}
});
