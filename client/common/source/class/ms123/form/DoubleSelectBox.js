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
/** **********************************************************************
   Authors:
     * Manfred Sattler

************************************************************************ */

/**
	@lint ignoreDeprecated(alert,eval) 
*/
/**
 * A form widget which allows a multiple selection. 
 *
 */
qx.Class.define("ms123.form.DoubleSelectBox", {
	extend: qx.ui.core.Widget,
	implement: [
	qx.ui.core.IMultiSelection, qx.ui.form.IModelSelection, qx.ui.form.IForm],
	include: [qx.ui.form.MForm],


/**
  *****************************************************************************
     CONSTRUCTOR
  *****************************************************************************
  */


	construct: function () {
		this.base(arguments);
		this._list_all = new qx.data.Array();
		this._setLayout(new qx.ui.layout.HBox());

		this._createChildControl("list_avalaible");
		this._createChildControl("control");
		this._createChildControl("list_selected");
		this.setHeight(100);
	},


/**
  *****************************************************************************
     PROPERTIES
  *****************************************************************************
  */

	events: { /** Fires after the selection was modified */
		"changeSelection": "qx.event.type.Data"
	},


	properties: {
		// overridden
		appearance: {
			refine: true,
			init: "doubleselectbox"
		}
	},


	/**
	 *****************************************************************************
	 MEMBERS
	 *****************************************************************************
	 */


	members: {
		_no_update: false,
		_list_all: null,


		/**---------------------------------------------------------------------------
		 WIDGET API
		 ---------------------------------------------------------------------------*/

		// overridden
		_createChildControlImpl: function (id) {
			var control;

			switch (id) {
			case "list_avalaible":
				control = new qx.ui.list.List().set({
					itemHeight: 18,
					minWidth: 200,
					minHeight: 100,
					selectionMode: "one",
					quickSelection: false
				});

				control.setScrollbarY("on");

				control.addListener("dblclick", this._onDblClickAvailable, this);
				this._add(control, {
					flex: 1
				});
				break;

			case "list_selected":
				control = new qx.ui.list.List().set({
					itemHeight: 25,
					minWidth: 200,
					minHeight: 100,
					selectionMode: "one",
					quickSelection: false
				});
				control.addListener("dblclick", this._onDblClickSelected, this);
				this._add(control, {
					flex: 1
				});
				break;

			case "control":
				control = new qx.ui.container.Composite(new qx.ui.layout.VBox(5));
				this.set({
					height: 70
				});
				var toSelected = new qx.ui.form.Button(">");
				toSelected.addListener("execute", this._toSelected, this);
				var toAvail = new qx.ui.form.Button("<");
				toAvail.addListener("execute", this._toAvalaible, this);
				control._add(toSelected, {
					flex: 1
				});
				control._add(toAvail, {
					flex: 1
				});
				this._add(control);
				break;
			}

			return control || this.base(arguments, id);
		},
		getChildrenContainerAvalaible: function () {
			return this.getChildControl("list_avalaible");
		},

		getChildrenContainerSelected: function () {
			return this.getChildControl("list_selected");
		},

		/**
		 ---------------------------------------------------------------------------
		 PUBLIC SELECTION API
		 ---------------------------------------------------------------------------
		 */
		getSelectables: function () {
			return this._list_all;
		},

		getSelection: function () {
			return null;
		},

		setSelection: function (values) {
			if (this._no_update) return;
			var selectionsModel = this._valuesToModel(values);
			this._updateLists(selectionsModel);
		},
		setModel: function (model) {
			var list_sel = this.getChildControl("list_selected");
			var list_ava = this.getChildControl("list_avalaible");
			var delegate = {
				configureItem: function (item) {
					item.setPadding(3);
				},
				createItem: function () {
					return new ms123.form.TooltipListItem();
				},
				bindItem: function (controller, item, id) {
					controller.bindProperty("label", "label", null, item, id);
					item.setTooltip(null);
					try{
						if( list_ava.getModel().getItem(id).getTooltip !== undefined ){
							var t = list_ava.getModel().getItem(id).get("tooltip");
							if( item.setTooltip !== undefined){
								item.setTooltip(t);
							}
						}
					}catch(e){ }
				}
			};

			var delegate_sel = {
				configureItem: function (item) {
					item.setPadding(3);
				},
				createItem: function () {
					return new ms123.form.TooltipListItem();
				},
				bindItem: function (controller, item, id) {
					controller.bindProperty("label", "label", null, item, id);
					item.setTooltip(null);
					try{
						if( list_sel.getModel().getItem(id).getTooltip !== undefined ){
							var t = list_sel.getModel().getItem(id).get("tooltip");
							if( item.setTooltip !== undefined){
								item.setTooltip(t);
							}
						}
					}catch(e){ }
				}
			};
			list_ava.setDelegate(delegate);
			list_ava.setModel(model);
			list_sel.setDelegate(delegate_sel);
			this._list_all = model.copy();


		},

		_updateLists: function (list) {
			if (!list || list.length < 0) return;
			var list_ava = this.getChildControl("list_avalaible");
			var list_sel = this.getChildControl("list_selected");
			if( !list_sel.getModel() ){
				list_sel.setModel(new qx.data.Array());
			}
			if( !list_ava.getModel() ){
				list_ava.setModel(new qx.data.Array());
			}
			list_sel.getModel().removeAll();
			list_ava.getModel().removeAll();

			list_ava.getModel().append( this._list_all );

			for (var i = 0; i < list.length; i++) {
				var item = list.getItem(i);
				list_ava.getModel().remove( item );
				list_sel.getModel().push( item );
			}
			return;
		},

		_getNeighbor: function (list, item) {
			var prev = item;
			var childs = list.getModel();
			for (var i = 0; i < childs.getLength(); i++) {
				if (childs.getItem(i) === item) {
					return prev;
				}
				prev = childs.getItem(i);
			}
		},

		/**
		 */
		_onDblClickAvailable: function (e) {
			var listItem = e._target;
			var list_sel = this.getChildControl("list_selected");
			var list_ava = this.getChildControl("list_avalaible");
			var selection = list_ava.getSelection();
			if (selection.getLength() == 0) return;
			var selItem = selection.getItem(0);
			var n = this._getNeighbor(list_ava, selItem);
			list_ava.getModel().remove(selItem);
			if (listItem.setSelected != null) {
				listItem.setSelected(true);
			}
			list_sel.getModel().push(selItem);
			this._no_update = true;
			this.fireDataEvent("changeSelection", this._modelToValues(list_sel.getModel()), null);
			this._no_update = false;
			if (n != null) {
				var a = new qx.data.Array();
				a.push(n);
				list_ava.setSelection(a);
			}
		},
		/**
		 */
		_toSelected: function (e) {
			var listItem = e._target;
			var list_sel = this.getChildControl("list_selected");
			var list_ava = this.getChildControl("list_avalaible");
			var selection = list_ava.getSelection();
			if (selection.getLength() == 0) return;
			var n = this._getNeighbor(list_ava, selection.getItem(0));
			for (var i = 0; i < selection.getLength(); i++) {
				var item = list_ava.getSelection().getItem(i);
				list_ava.getModel().remove(item);
				if (listItem.setSelected != null) {
					listItem.setSelected(true);
				}
				list_sel.getModel().push(item);
			}
			if (n != null) {
				var a = new qx.data.Array();
				a.push(n);
				list_ava.setSelection(a);
			}
			this._no_update = true;
			this.fireDataEvent("changeSelection", this._modelToValues(list_sel.getModel()), null);
			this._no_update = false;
		},

		/**
		 */
		_onDblClickSelected: function (e) {
			var listItem = e._target;
			var list_sel = this.getChildControl("list_selected");
			var list_ava = this.getChildControl("list_avalaible");
			var selection = list_sel.getSelection();
			if (selection.getLength() == 0) return;
			var selItem = selection.getItem(0);
			list_sel.getModel().remove(selItem);
			if (listItem.setSelected != null) {
				listItem.setSelected(false);
			}
			list_ava.getModel().push(selItem);
			this._no_update = true;
			this.fireDataEvent("changeSelection", this._modelToValues(list_sel.getModel()), null);
			this._no_update = false;
		},


		/**
		 */
		_toAvalaible: function (e) {
			var list_sel = this.getChildControl("list_selected");
			var list_ava = this.getChildControl("list_avalaible");
			var selection = list_sel.getSelection();
			if (selection.getLength() == 0) return;
			for (var i = 0; i < selection.getLength(); i++) {
				var selItem = selection.getItem(i);
				list_sel.getModel().remove(selItem);
				list_ava.getModel().push(selItem);
			}
			this._no_update = true;
			this.fireDataEvent("changeSelection", this._modelToValues(list_sel.getModel()), null);
			this._no_update = false;
		},
		_modelToValues: function (model) {
			var selected = [];
			model.forEach(function (select) {
				selected.push(select.getValue());
			});
			var r = qx.util.Serializer.toJson(selected);
			console.log("_modelToValues:" + r);
			return selected;
		},
		_valuesToModel: function (values) {
			var r = qx.util.Serializer.toJson(values); console.log("_valuesToModel.values:" + r);
			var selectables = this.getSelectables();
			var model = new qx.data.Array();
			if (values == null || values.length == 0) return model;
			for (var i = 0; i < selectables.getLength(); i++) {
				var selectable = selectables.getItem(i);
				for (var j = 0; j < values.length; j++) {
					var value = values[j];
					if( typeof value == "string" ){
						if (selectable.getValue() == value) {
							model.push(selectable);
						}
					}else{
						if (selectable.getValue() == value.value) {
							model.push(selectable);
						}
					}
				}
			}
			var r = qx.util.Serializer.toJson(model); console.log("_valuesToModel.model:" + r);
			return model;
		},
		getModelSelection : function() {
			alert("DoubleSelectBox.getModelSelection");
    },
		setModelSelection : function() {
			alert("DoubleSelectBox.setModelSelection");
    },
		isSelectionEmpty : function() {
			alert("DoubleSelectBox.isSelectionEmpty");
    },
		isSelected : function() {
			alert("DoubleSelectBox.isSelected");
    },
		resetSelection : function() {
			alert("DoubleSelectBox.resetSelection");
    },
		removeFromSelection : function() {
			alert("DoubleSelectBox.removeFromSelection");
    },
		addToSelection : function() {
			alert("DoubleSelectBox.addToSelection");
    },
		selectAll : function() {
			alert("DoubleSelectBox.selectAll");
    },

		removeAll: function () {
			this._list_all = new qx.data.Array();
			return [];
		},
		getChildren: function () {
			return new Array();
		}
	}
});
