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
qx.Class.define("ms123.form.TreeMultiSelectBox", {
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
		this._setLayout(new qx.ui.layout.HBox());

		var sp = this._doLayout();
		this._add(sp, {flex:1});

		var spc = sp.getChildren();
		this._leftWidget = spc[0];
		this._rightWidget = spc[1];
		this._createChildControl("list_avalaible");
		this._createChildControl("control");
		this._createChildControl("list_selected");
		this.setHeight(100);
    this.addListener("keypress", this._onKeyPress);
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
		__selectableItems: null,
		__globalSearchString: null,


		/**---------------------------------------------------------------------------
		 WIDGET API
		 ---------------------------------------------------------------------------*/

		// overridden
		_createChildControlImpl: function (id) {
			var control;

			switch (id) {
			case "list_avalaible":
				control = new qx.ui.tree.VirtualTree(null, "title", "children").set({
					focusable: false,
					hideRoot: true,
					keepFocus: true,
					openMode: "click",
					height: null,
					itemHeight: 20,
					width: null,
					maxWidth: this.getWidth(),
					maxHeight: this.getHeight(),
					selectionMode: "one",
					contentPaddingLeft: 0,
					showTopLevelOpenCloseIcons: true,
					quickSelection: false
				});
				control.setIconPath("value");
				control.setIconOptions({
					converter: function (value, model) {
						if (model.getChildren != null && model.getChildren().getLength() > 0) {
							return "qx/decoration/Classic/shadow/shadow-small-r.png";
						} else {
							return "qx/decoration/Classic/shadow/shadow-small-tl.png";
						}
					}
				});
				this.__changeListenerDisabled = false;
				control.addListener("open", this._onOpen, this);
				control.addListener("dblclick", this._onDblClickTree, this);
				this._leftWidget._add(control, {
					flex: 1
				});
				control.setModel( new qx.data.Array());
				break;

			case "list_selected":
				control = new qx.ui.list.List().set({
					itemHeight: 25,
					minWidth: 70,
					minHeight: 100,
					selectionMode: "one",
					quickSelection: false
				});
				control.addListener("dblclick", this._onDblClickSelected, this);
				control.addListener("click", this._onClickSelected, this);
				this._rightWidget.add(control, {
					edge: "center"
				});
				control.setModel( new qx.data.Array());
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
				this._rightWidget.add(control,{edge:"west"});
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
			return this.__selectableItems;
		},

		getSelection: function () {
			return null;
		},

		setSelection: function (values) {
			if (this._no_update) return;
			var selectionsModel = this._valuesToModel(values);
			this.__updateListSelected(selectionsModel);
			this.__closeTree();
		},
		setModel: function (model) {
			this.__model = model;
			var list_sel = this.getChildControl("list_selected");
			var list_ava = this.getChildControl("list_avalaible");
			var delegate = {
				configureItem: function (item) {
					item.setIndent(13);
				},
				bindItem: function (controller, item, id) {
					controller.bindDefaultProperties(item, id);
					//if( this.__model.getTooltip ){
						//controller.bindProperty("tooltip", "toolTipText", null, item, id);
					//}
					//controller.bindProperty("label", "label", null, item, id);
					//item.setTooltip(null);
					//try{
					//	var t = list_ava.getModel().getItem(id).get("tooltip");
					//	item.setTooltip(t);
					//}catch(e){ }
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
					controller.bindProperty("title", "label", null, item, id);
					item.setTooltip(null);
					try{
						var t = list_sel.getModel().getItem(id).get("tooltip");
						item.setTooltip(t);
					}catch(e){ }
				}
			};
			list_ava.setDelegate(delegate);
			list_ava.setModel(model);
			list_sel.setDelegate(delegate_sel);
			this._getItems();
		},

		__updateListSelected: function (list) {
			if (!list || list.length < 0) return;
			var list_sel = this.getChildControl("list_selected");
			list_sel.getModel().removeAll();

			for (var i = 0; i < list.length; i++) {
				var item = list.getItem(i);
				list_sel.getModel().push( item );
			}
			if( list_sel.getModel().getLength() > 0){
				var firstSel  = list_sel.getModel().getItem(0);
				var sel = this.__getTree().getSelection();
				sel.splice(0, 1, firstSel);
			}
			return;
		},

		__closeTree: function () {
			var selectables = this.getSelectables();
			var tree = this.__getTree();
			for (var i = 0; i < selectables.getLength(); i++) {
				var selectable = selectables.getItem(i);
				if( tree.isNodeOpen( selectable)){
					console.log("closenode:"+selectable);
					tree.closeNode(selectable);
				}
			}
		},

		/**
		 */
		_toSelected: function (e) {
			var list_sel = this.getChildControl("list_selected");
			var list_ava = this.getChildControl("list_avalaible");
			var selection = list_ava.getSelection();
			for (var i = 0; i < selection.getLength(); i++) {
				var item = list_ava.getSelection().getItem(i);
				console.log("item:"+item.getTitle());
				var contains = list_sel.getModel().contains(item);
				console.log("contains:"+contains);
				if(!contains){
					list_sel.getModel().push(item);
				}
			}
			this._no_update = true;
			this.fireDataEvent("changeSelection", this._modelToValues(list_sel.getModel()), null);
			this._no_update = false;
		},

		/**
		 */
		_onDblClickSelected: function (e) {
			var list_sel = this.getChildControl("list_selected");
			var list_ava = this.getChildControl("list_avalaible");
			var selection = list_sel.getSelection();
			if (selection.getLength() == 0) return;
			var selItem = selection.getItem(0);
			list_sel.getModel().remove(selItem);
			this._no_update = true;
			this.fireDataEvent("changeSelection", this._modelToValues(list_sel.getModel()), null);
			this._no_update = false;
		},

		/**
		 */
		_onClickSelected: function (e) {
			var listItem = e._target;
			var list_sel = this.getChildControl("list_selected");
			var list_ava = this.getChildControl("list_avalaible");
			var selection = list_sel.getSelection();
			var selItem = selection.getItem(0);
			var sel = list_ava.getSelection();
			sel.splice(0, 1, selItem);
			this.__closeTree();
			list_ava.openNode(selItem);
		},

		/**
		 */
		_toAvalaible: function (e) {
			var list_sel = this.getChildControl("list_selected");
			var selection = list_sel.getSelection();
			if (selection.getLength() == 0) return;
			for (var i = 0; i < selection.getLength(); i++) {
				var selItem = selection.getItem(i);
				list_sel.getModel().remove(selItem);
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
			return selected;
		},
		_valuesToModel: function (values) {
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
			return model;
		},
		_onDblClickTree: function (e) {
			var tree = this.__getTree()
			var item = tree.getSelection().getItem(0);
			console.log("_onDblClickTree:"+ item.getTitle());
			this._toSelected();
		},
		// overridden
		_onOpen: function (e) {
			var item = e.getData();
			if( item.getLength !== undefined ) return; //@@@MS ???? update to 1.6
			var tree = this.__getTree()
			var lookup = tree.getLookupTable();
			var index = lookup.indexOf(item);
			if (index == -1) {
				tree.openNodeAndParents(item);
				index = lookup.indexOf(item);
			}
			var sel = tree.getSelection();
			sel.splice(0, 1, item);
		},
		// overridden
		_onKeyPress: function (e) {
			var iden = e.getKeyIdentifier();
			console.log("_onKeyPress.iden:" + iden);
			var tree = this.__getTree();

			if (iden == "/") {
				this.__globalSearchString = "";
				this.__nextSearch=0;
				return;
			}
			if( this.__globalSearchString != null ){
				if( iden == "Space") iden = " ";
				if( iden == "Shift") iden = "*";
				if (iden == "Enter") {
					this.__globalSearchString = null;
					return;
				}
				var old = this.__globalSearchString;
				if( iden != '+' && iden != '.' ){
					this.__globalSearchString += iden;
					this.__nextSearch=0;
				}
				console.log("__globalSearchString:"+this.__globalSearchString);
				var pattern=null;
				try{
					pattern=eval("/" + this.__globalSearchString + "/i");
				}catch(e){
					this.__globalSearchString=old;
					return;
				}
				console.log("\tpattern:"+pattern+"/"+this.__nextSearch+"/"+this.__selectableItems.getLength());
				for (var i = this.__nextSearch; i < this.__selectableItems.getLength(); i++) {
					var item = this.__selectableItems.getItem(i);
					var t = item.getTitle();
					if (t && t.match( pattern )) {
						console.log("\tsetSelection:" + item.getTitle());
						var sel = tree.getSelection();
						sel.splice(0, 1, item );
						var openNodes = tree.getOpenNodes();
						openNodes.forEach(function(n,i){
							if( tree.isNodeOpen(n) && i>0 ){
								tree.closeNode(n);
							}
						},this);
						tree.openNodeAndParents( item);
						this.__nextSearch = i+1;
						if( this.__nextSearch>=(this.__selectableItems.getLength()-1)){
							this.__nextSearch=0;
						}
						return;
					}else{
						this.__nextSearch=0;
					}
				}
				return;
			}

			var sel = tree.getSelection().getItem(0);
			var index = this.__selectableItems.indexOf(sel);

			var a1 = this.__selectableItems.slice(0,index);				
			var a2 = this.__selectableItems.slice(index+1);				
			var a = a2.concat(a1.toArray());

			for (var i = 0; i < a.getLength(); i++) {
				var t=null;
				if( a.getItem(i).getTitle){
					t = a.getItem(i).getTitle();
				}
				if (t && t.match("^" + iden)) {
					var item = a.getItem(i);
					sel = tree.getSelection();
					sel.splice(0, 1, item );
					var openNodes = tree.getOpenNodes();
					openNodes.forEach(function(n,i){
						if( tree.isNodeOpen(n) && i>0 ){
							tree.closeNode(n);
						}
					},this);
					tree.openNodeAndParents( item);
					return;
				}
			}
		},
		__getTree: function () {
			return this.getChildControl("list_avalaible");
		},
		getModelSelection : function() {
			alert("TreeMultiSelectBox.getModelSelection");
    },
		setModelSelection : function() {
			alert("TreeMultiSelectBox.setModelSelection");
    },
		isSelectionEmpty : function() {
			alert("TreeMultiSelectBox.isSelectionEmpty");
    },
		isSelected : function() {
			alert("TreeMultiSelectBox.isSelected");
    },
		resetSelection : function() {
			alert("TreeMultiSelectBox.resetSelection");
    },
		removeFromSelection : function() {
			alert("TreeMultiSelectBox.removeFromSelection");
    },
		addToSelection : function() {
			alert("TreeMultiSelectBox.addToSelection");
    },
		selectAll : function() {
			alert("TreeMultiSelectBox.selectAll");
    },

		removeAll: function () {
			return [];
		},
		getChildren: function () {
			return new Array();
		},
		/**
		 ---------------------------------------------------------------------------
		 HELPER METHODS FOR SELECTION API
		 ---------------------------------------------------------------------------
		 */

		_getItems: function () {
		  console.log("_getItems:"+this.__model);
			var selectables = [];
			if (this.__getTree().isHideRoot()) {
				var childs = null;
				if( this.__model.getChildren){
					childs = this.__model.getChildren();
				}
				for (var i = 0; childs && i < childs.getLength(); i++) {
					this.__getItemFromModel(this.__model.getChildren().getItem(i), selectables);
				}
			} else {
				this.__getItemFromModel(this.__model, selectables);
			}

			this.__selectableItems = new qx.data.Array(selectables);
			return selectables;
		},

		__checkSelectableFlag: function (model) {
			var selectable = true;
			try {
				var id = model.getId();
				selectable = model.getSelectable();
			} catch (e) {}
			return selectable;
		},
		__getItemFromModel: function (model, selectables) {
			var selectable = this.__checkSelectableFlag(model)
			if (selectable) {
				selectables.push(model);
			}
			var children = model.getChildren();
			for (var i = 0; i < children.getLength(); i++) {
				var c = children.getItem(i);
				this.__getItemFromModel(c, selectables);
			}
		},
		_isItemSelectable: function (item) {
			if (!this.__selectableItems) return false;
			for (var i = 0; i < this.__selectableItems.length; i++) {
				if (item == this.__selectableItems[i]) {
					return true;
				}
			}
			return false;
		},
		_isSelectableItem: function (item) {
			return this._isItemSelectable(item);
		},
		_getSelectableById: function (id) {
			if (!this.__selectableItems) return null;
			for (var i = 0; i < this.__selectableItems.length; i++) {
				if (id == this.__selectableItems[i].getId()) {
					return this.__selectableItems[i];
				}
			}
			return null;
		},
		_doLayout: function () {
			var splitpane = new qx.ui.splitpane.Pane("horizontal");
			splitpane.setDecorator(null);

			var leftWidget = new qx.ui.core.Widget();
			leftWidget._setLayout(new qx.ui.layout.HBox());
			leftWidget.setDecorator(null);
			leftWidget.setMinWidth(100);
			splitpane.add(leftWidget, 4);

			var rightWidget = new qx.ui.container.Composite();
			rightWidget.setLayout(new qx.ui.layout.Dock());
			rightWidget.setDecorator(null);
			rightWidget.setMinWidth(100);
			splitpane.add(rightWidget, 3);

			return splitpane;
		}
	}
});
