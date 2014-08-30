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
 * A cell editor factory creating select boxes.
 */
qx.Class.define("ms123.util.SelectBox",
{
  extend : qx.core.Object,
  implement : qx.ui.table.ICellEditorFactory,


  properties :
  {
    /**
     * function that validates the result
     * the function will be called with the new value and the old value and is
     * supposed to return the value that is set as the table value.
     **/
    validationFunction :
    {
      check : "Function",
      nullable : true,
      init : null
    },

    /** array of data to construct ListItem widgets with */
    listData :
    {
      check : "Array",
      init : null,
      nullable : true
    }

  },


  members :
  {
    // interface implementation
    createCellEditor : function(cellInfo)
    {
      var cellEditor = new qx.ui.form.SelectBox().set({
        appearance: "table-editor-selectbox"
      });

      var value = cellInfo.value;
      cellEditor.originalValue = value;

      // check if renderer does something with value
      var cellRenderer = cellInfo.table.getTableColumnModel().getDataCellRenderer(cellInfo.col);
      var label = cellRenderer._getContentHtml(cellInfo);
      if ( value != label ) {
        value = label;
      }

      // replace null values
      if ( value === null ) {
        value = "";
      }

      var list = this.getListData();
      if (list)
      {
        var item;

        for (var i=0,l=list.length; i<l; i++)
        {
          var row = list[i];
          if ( row instanceof Array ) {
            item = new qx.ui.form.ListItem(row[0], row[1]);
            item.setUserData("row", row[2]);
          } else {
            item = new qx.ui.form.ListItem(row, null);
            item.setUserData("row", row);
          }
          cellEditor.add(item);
        };
      }

      var itemToSelect = cellEditor.getChildrenContainer().findItem("" + value);

      if (itemToSelect) {
        cellEditor.setSelection([itemToSelect]);
      } else {
        cellEditor.resetSelection();
      }
      cellEditor.addListener("appear", function() {
        cellEditor.open();
      });

			cellEditor.addListener("changeSelection", function() {//@@@MS
				cellInfo.table.stopEditing();
			}, this);
      return cellEditor;
    },


    // interface implementation
    getCellEditorValue : function(cellEditor)
    {
      var selection = cellEditor.getSelection();
      var value = "";

      if (selection && selection[0]) {
        value = selection[0].getUserData("row") || selection[0].getLabel();
      }

      // validation function will be called with new and old value
      var validationFunc = this.getValidationFunction();
      if (validationFunc ) {
         value = validationFunc( value, cellEditor.originalValue );
      }

      if (typeof cellEditor.originalValue == "number") {
        value = parseFloat(value);
      }

      return value;
    }
  }
});
