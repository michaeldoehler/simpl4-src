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
	@asset(qx/decoration/*)
	@asset(qx/decoration/Modern/tree/closed.png)
	@lint ignoreDeprecated(alert,eval) 
*/

/**
 * A form widget which allows a single selection. Looks somewhat like
 * a normal button, but opens a list of items to select when clicking on it.
 *
 * @childControl spacer {qx.ui.core.Spacer} flexible spacer widget
 * @childControl atom {qx.ui.basic.Atom} shows the text and icon of the content
 * @childControl arrow {qx.ui.basic.Image} shows the arrow to open the popup
 */
qx.Class.define("ms123.form.TreeSelectBox", {
	extend: qx.ui.form.AbstractSelectBox,
	implement: [
	qx.ui.form.IStringForm,qx.ui.core.ISingleSelection,qx.ui.form.IForm,ms123.form.IConfig],
	include: [qx.ui.core.MSingleSelectionHandling,qx.ui.form.MForm],



	/**
	 *****************************************************************************
	 CONSTRUCTOR
	 *****************************************************************************
	 */


	construct: function (name) {
		this.base(arguments);

		this.__name = name;
		this._createChildControl("atom");
		this._createChildControl("spacer");
		this._createChildControl("arrow");

		// Register listener
		this.addListener("click", this._onClick, this);
		this.addListener("mousewheel", this._onMouseWheel, this);
		this.addListener("changeSelection", this.__onChangeSelection, this);
	},


	/**
	 *****************************************************************************
	 PROPERTIES
	 *****************************************************************************
	 */


	properties: {
		// overridden
		appearance: {
			refine: true,
			init: "selectbox"
		}
	},
	events: {
		"changeValue": "qx.event.type.Data"
		//"changeSelection": "qx.event.type.Data"
	},



	/**
	 *****************************************************************************
	 MEMBERS
	 *****************************************************************************
	 */
	members: { /** {qx.ui.form.ListItem} instance */
		__preSelectedItem: null,
		__selectableItems: null,
		__globalSearchString: null,

		/**
		 ---------------------------------------------------------------------------
		 WIDGET API
		 ---------------------------------------------------------------------------
		 */
		// overridden
		_createChildControlImpl: function (id, hash) {
			var control;

			switch (id) {
			case "tree":
				control = new qx.ui.tree.VirtualTree(null, "title", "children").set({
					focusable: false,
					hideRoot: true,
					keepFocus: true,
					openMode: "click",
					height: null,
					itemHeight: 20,
					//width: null,
					width: this.getWidth()+70,
					maxHeight: this.getMaxListHeight(),
					selectionMode: "one",
					contentPaddingLeft: 0,
					showTopLevelOpenCloseIcons: true,
					quickSelection: false
				});
				control.setIconPath("title");
	
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
				control.addListener("mousedown", this._onTreeMouseDown, this);
				control.addListener("open", this._onOpen, this);
				break;

			case "spacer":
				control = new qx.ui.core.Spacer();
				this._add(control, {
					flex: 1
				});
				break;

			case "atom":
				control = new qx.ui.basic.Atom(" ");
				control.setCenter(false);
				control.setAnonymous(true);
				control.setMinHeight(16);
				this._add(control, {
					flex: 1
				});
				break;

			case "arrow":
				control = new qx.ui.basic.Image();
				control.setAnonymous(true);
				this._add(control);
				break;
			case "popup":
				control = new qx.ui.popup.Popup(new qx.ui.layout.VBox);
				control.setAutoHide(false);
				control.setKeepActive(true);
				control.addListener("mouseup", this.close, this);
				control.add(this.__getTree());
				break;
			}
			return control || this.base(arguments, id);
		},

		// overridden
		_forwardStates: {
			focused: true
		},

		__getTree: function () {
			return this.getChildControl("tree");
		},
		/**
		 * Sets the label inside the list to match the selected TreeItem.
		 */
		__updateLabel: function () {
			var atom = this.getChildControl("atom");
			var tree = this.__getTree();
			if( !tree ){
				atom.setLabel(" ");
				return;
			}
			var sel = tree.getSelection();
			if( !sel || sel.getLength() == 0 ){
				atom.setLabel(" ");
				return;
			}
			var item = sel.getItem(0);
			if( !item.getTitle ){
				atom.setLabel(" ");
				return;
			}
			console.log("__updateLabel.item:" + item.getTitle());
			var label = item.getTitle();
			var format = this.getFormat();
			if (format != null) {
				label = format.call(this, item);
			}

			// check for translation
			if (label && label.translate) {
				label = label.translate();
			}
			label == null ? atom.resetLabel() : atom.setLabel(label);
		},
		_defaultFormat: function (item) {
			var delim = "";
			var label = "";
			var module = "";
			if (item) {
				label = item.getTitle();
				try {
					module = item.getModuleTitle();
					if (module && "" != module) {
						delim = "/";
					}
				} catch (e) {
					return label;
				}
			}
			return module + delim + label;
		},




		/**
		 ---------------------------------------------------------------------------
		 DELEGATE IMPLEMENTATION
		 ---------------------------------------------------------------------------
		 */
		bindItem: function (controller, item, id) {
			controller.bindDefaultProperties(item, id);
			if( this.__model.getTooltip ){
		  	controller.bindProperty("tooltip", "toolTipText", null, item, id);
			}
		},
		configureItem: function (item) {
			item.setIndent(13);
		},
		_createItem: function () {
			var item = new ms123.searchfilter.TreeItem();
			return item;
		},

		/**
		 ---------------------------------------------------------------------------
		 HELPER METHODS FOR SELECTION API
		 ---------------------------------------------------------------------------
		 */

		_getItems: function () {
			var selectables = [];
			if( !this.__model ) return selectables;
			if (this.__getTree().isHideRoot()) {
				var childs = this.__model.getChildren();
				for (var i = 0; i < childs.getLength(); i++) {
					this.__getItemFromModel(this.__model.getChildren().getItem(i), selectables);
				}
			} else {
				this.__getItemFromModel(this.__model, selectables);
			}

			this.__selectableItems = selectables;
			return selectables;
		},

		__checkSelectableFlag: function (model) {
			var selectable = true;
			try {
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
			if (!this.__selectableItems){
				 return null;
			}
			for (var i = 0; i < this.__selectableItems.length; i++) {
				if (id == this._getModelValue(this.__selectableItems[i])) {
					return this.__selectableItems[i];
				}
			}
			return null;
		},

		_isAllowEmptySelection: function () {
			return this.getChildrenContainer().getSelectionMode() !== "one";
		},


		/**
		 ---------------------------------------------------------------------------
		 EVENT LISTENERS
		 ---------------------------------------------------------------------------
		 */

		/**
		 * Event handler for <code>changeSelection</code>.
		 *
		 * param e {qx.event.type.Data} Data event.
		 */
		__onChangeSelection: function (e) {
			var item = e.getData()[0];
			if( !item ) return;
			var tree = this.__getTree();

			var lookup = tree.getLookupTable();
			var index = lookup.indexOf(item);
			if (index == -1) {
				tree.openNodeAndParents(item);
				index = lookup.indexOf(item);
			}
			this.__changeListenerDisabled = true;
			var sel = tree.getSelection();
			sel.splice(0, 1, item);

			this.__updateLabel();
			this.__changeListenerDisabled = false;
			this.fireDataEvent("changeValue", this._getModelValue(item), null);
		},

		/**
		 * Hides the list popup.
		 */
		close: function () {
				this.__globalSearchString = null;
			if (this.notClose) {
				this.notClose = false;
			} else {
				this.getChildControl("popup").hide();
			}
		},


		/**
		 * Toggles the popup's visibility.
		 *
		 * param e {qx.event.type.Mouse} Mouse event
		 */
		_onClick: function (e) {
			this.toggle();
		},

		/**
		 * Event handler for mousewheel event
		 *
		 * param e {qx.event.type.Mouse} Mouse event
		 */
		_onMouseWheel: function (e) {
			console.log("_onMouseWheel:" + this.getChildControl("popup").isVisible());
			if (this.getChildControl("popup").isVisible()) {
				return;
			}

			var direction = e.getWheelDelta("y") > 0 ? 1 : -1;
			var selectables = this.__selectableItems; // this.getSelectables(true);
			var selected = this.getSelection();
			console.log("_onMouseWheel.selected:" + selected);

			if (!selected || selected.length == 0) {
				selected = [selectables[0]];
			} else {}

			var index = selectables.indexOf(selected[0]) + direction;
			var max = selectables.length - 1;

			// Limit
			if (index < 0) {
				index = 0;
			} else if (index >= max) {
				index = max;
			}
			this.setSelection([selectables[index]]);
			e.stopPropagation();
			e.preventDefault();
		},

		// overridden
		_onKeyPress: function (e) {
			var iden = e.getKeyIdentifier();
			console.log("_onKeyPress.iden:" + iden);
			var popup = this.getChildControl("popup");
			if (!popup.isHidden() && iden == "Escape") {
				this.__globalSearchString = null;
				this.close();
				e.stop();
			}
			if (iden == "Escape") {
				this.__globalSearchString = null;
				return;
			}
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
					if( !popup.isHidden() ){
						this.close();
					}
					return;
				}
				var old = this.__globalSearchString;
				if( iden != '+' && iden != '.' ){
					this.__nextSearch=0;
					this.__globalSearchString += iden;
				}
				var pattern=null;
				try{
					var pattern=eval("/" + this.__globalSearchString + "/i");
				}catch(e){
					this.__globalSearchString=old;
					return;
				}
				var tree = this.__getTree()
				for (var i = this.__nextSearch; i < this.__selectableItems.length; i++) {
					var item = this.__selectableItems[i];
					var t = item.getTitle();
					if (t && t.match( pattern )) {
						var sel = tree.getSelection();
						sel.splice(0, 1, item );
						tree.getOpenNodes().forEach(function(n,i){
							if( tree.isNodeOpen(n) && i>0 ){
								tree.closeNode(n);
							}
						},this);
						tree.openNodeAndParents( item);
						this.__nextSearch = i+1;
						return;
					}else{
						this.__nextSearch=0;
					}
				}
				return;
			}
			if (iden == "Enter" || iden == "Space") {
				this.__globalSearchString = null;
				if (this.__preSelectedItem) {
					this.setSelection([this.__preSelectedItem]);
					this.__preSelectedItem = null;
				}
				this.toggle();
			} else {
				var sel = this.getSelection()[0];
				var index = this.__selectableItems.indexOf(sel);

				var a1 = this.__selectableItems.slice(0,index);				
				var a2 = this.__selectableItems.slice(index+1);				
				var a = a2.concat(a1);

				var tree = this.__getTree()
				for (var i = 0; i < a.length; i++) {
					var item = a[i];
					var t = item.getTitle();
					if (t && t.match("^" + iden)) {
						var sel = tree.getSelection();
						sel.splice(0, 1, item );
						tree.getOpenNodes().forEach(function(n,i){
							if( tree.isNodeOpen(n) && i>0 ){
								tree.closeNode(n);
							}
						},this);
						tree.openNodeAndParents( item);
						return;
					}
				}
			}
		},

		// overridden
		_onOpen: function (e) {
			var item = e.getData();
			console.log("_onOpen:(" + this.__name + "):" + item.getTitle());
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
		_onTreeMouseDown: function (e) {
			var tree = this.__getTree()
			var item = tree.getSelection().getItem(0);
			console.log("_onTreeMouseDown:" + item.getTitle());

			if (!this._isItemSelectable(item)) {
				console.log("_onTreeMouseDown.Not selectable");
				this.notClose = true;
				return;
			}
			if (tree.isQuickSelection()) {
				if (this.__preSelectedItem) {
					if (this._isItemSelectable(this.__preSelectedItem)) {
						this.setSelection([this.__preSelectedItem]);
						this.__preSelectedItem = null;
					} else {
						this.notClose = true;
					}
				}
			} else {
				this.setSelection([item]);
			}
		},

		_onTreeChangeSelection: function (e) {
			var tree = this.__getTree();
			if( !tree ) return;
			var selection = tree.getSelection();
			if( !selection || selection.getLength() == 0 ) return;
			var item = tree.getSelection().getItem(0);
			if (this.__changeListenerDisabled) return;

			if (!this._isItemSelectable(item)) {
				console.log("_onTreeChangeSelection.Not selectable");
				return;
			}
			var popup = this.getChildControl("popup");
			if (this.__getTree().isQuickSelection()) {
				if (popup.isVisible()) {
					this.__preSelectedItem = item;
				} else {
					this.setSelection([item]);
					this.__preSelectedItem = null;
				}
			} else {
				this.setSelection([item]);
			}
			this.__updateLabel();
		},

		/**
		 *****************************************************************************
		 PUBLIC
		 *****************************************************************************
		 */
    beforeSave : function(context) {
    },
		beforeEdit: function (context) {
			var parentData = context.parentData;
			var url = this.__supplant(this.__modelUrl,parentData);
			console.log("TreeSelectBox:beforeEdit:"+url);
			var model = this.__createModelFromUrl(url);
			this.setModel(model);
			this.__updateLabel();
			this.__getTree().setMaxWidth(500);
		},
		beforeAdd: function (context) {
			var parentData = context.parentData;
			var url = this.__supplant(this.__modelUrl,parentData);
			console.log("TreeSelectBox:beforeAdd:"+url);
			var model = this.__createModelFromUrl(url);
			this.setModel(model);
			this.__updateLabel();
			this.__getTree().setMaxWidth(500);
		},
		afterSave: function (context) {
		},
		__createModelFromUrl:function(url){
			var treeModel = ms123.util.Remote.sendSync(url);
			var rootModel = {};
			rootModel.children = [];
			rootModel.title = "ROOT";
			rootModel.children.push(treeModel);
			this._translateModel(treeModel);
			var model = qx.data.marshal.Json.createModel(rootModel);
			return model;
		},
		__supplant : function (s,o) {
			return s.replace(/@{([^{}]*)}/g,
				function (a, b) {
					var r = o[b];
					return typeof r === 'string' || typeof r === 'number' ? r : a;
				}
			);
		},
		_translateModel: function (model) {
			model.title = this.tr(model.title);
			var children = model.children;
			if (children) {
				for (var i = 0; i < children.length; i++) {
					var c = children[i];
					this._translateModel(c);
				}
			}else{
				model.children = [];
			}
			return model;
		},
		setModel: function (model) {
			if (typeof model != 'string' ) {
				this.__model = model;
				this._getItems();
				var control = this.__getTree();
				control.setDelegate(this);
				control.getSelection().removeListener("change", this._onTreeChangeSelection, this);
				control.setModel(model);
				control.getSelection().addListener("change", this._onTreeChangeSelection, this);
			}else{
				this.__modelUrl = model.substring("url:".length);
			}
			this.__updateLabel();
		},
		getModelSelection: function () {
			console.log("TreeSelectBox.getModelSelection:" + this.getSelection());
			return new qx.data.Array(this.getSelection());
		},
		setValue: function (value) {
			console.log("TreeSelectBox.setValue:" + value);
			if( this.__getTree()){
				if( value == null ){
						var sel = this.__getTree().getSelection();
						if( sel && sel.getLength() > 0 ){
							this.fireDataEvent("changeValue", this._getModelValue(sel.getItem(0)), null);
						}
				}else{
					var item = this._getSelectableById(value);
					console.log("TreeSelectBox.setValue.item:"+item);
					if( item ){
						var sel = this.__getTree().getSelection();
						sel.splice(0, 1, item);
						this.__getTree().openNodeAndParents(item);
					}
				}
			}
		},

		// interface implementation
		resetValue: function () {
			//this.getChildControl("textfield").setValue(null);
		},
		getValue: function () {
			var sel = this.__getTree().getSelection();
			if( sel && sel.getLength() > 0 ){
				var val = this._getModelValue(sel.getItem(0));
				console.log("TreeSelectBox.getValue:"+val );
				return val;
			}
			return null;
		},
		_getModelValue:function(model){
			if( model.getValue != null){
				return model.getValue();
			}
			if( model.getId != null){
				return model.getId();
			}
		},

		setModelSelection: function (modelSelection) {
			console.log("setModelSelection:" + modelSelection);
			if (modelSelection == undefined || modelSelection == null) {
				this.resetSelection();
				return;
			}
			if (modelSelection.getLength) {
				modelSelection = modelSelection.toArray();
			}
			if (modelSelection.length > 0) {
				var id = modelSelection[0];
				console.log("setModelSelection.id:" + id + ",t:" + (typeof id));
				if (id != null) {
					if (typeof id == "object") {
						this.setSelection(modelSelection);
					} else {
						var item = this._getSelectableById(id);
						console.log("setModelSelection.item:" + item);
						if (item == null) {
							this.resetSelection();
						} else {
							var x = qx.util.Serializer.toJson(item);
							console.log("TreeSelectBox.setModelSelection:" + id + " -> " + x);
							this.setSelection([item]);
						}
					}
				} else {
					this.resetSelection();
				}
			} else {
				this.resetSelection();
			}
		}

	},


	/**
	 *****************************************************************************
	 DESTRUCT
	 *****************************************************************************
	 */
	destruct: function () {
//		this.__getTree().getSelection().removeListener("change");
		console.log("destruct.TreeSelectBox");
		this._disposeArray("__selectableItems");
		this._disposeArray("__preSelectedItem");
		this._disposeArray("__model");
		this.__preSelectedItem = null;
	}
});
