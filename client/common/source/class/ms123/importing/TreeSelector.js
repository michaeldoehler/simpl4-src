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
*/

/**
 * A form widget which allows a single selection. Looks somewhat like
 * a normal button, but opens a list of items to select when clicking on it.
 *
 * @childControl spacer {qx.ui.core.Spacer} flexible spacer widget
 * @childControl atom {qx.ui.basic.Atom} shows the text and icon of the content
 * @childControl arrow {qx.ui.basic.Image} shows the arrow to open the popup
 */
qx.Class.define("ms123.importing.TreeSelector", {
	extend: qx.ui.core.Widget,
	implement: [
	qx.ui.form.IStringForm, qx.ui.core.ISingleSelection, qx.ui.form.IForm, ms123.form.IConfig],
	include: [qx.ui.core.MSingleSelectionHandling, qx.ui.form.MForm],



	/**
	 *****************************************************************************
	 CONSTRUCTOR
	 *****************************************************************************
	 */


	construct: function (name) {
		this.base(arguments);
		this._setLayout(new qx.ui.layout.HBox());

		this.__name = name;
		// Register listener
		this.addListener("changeSelection", this.__onChangeSelection, this);
		this.setHeight(300);
	},


	/**
	 *****************************************************************************
	 PROPERTIES
	 *****************************************************************************
	 */


	properties: {
		// overridden
/*		appearance: {
			refine: true,
			init: "selectbox"
		}*/
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
					hideRoot: false,
					keepFocus: true,
					openMode: "tap",
					height: null,
					itemHeight: 20,
					//width: null,
					width: this.getWidth() + 70,
					//maxHeight: this.getMaxListHeight(),
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
				this._add(control, {
					flex: 1
				});

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
		 ---------------------------------------------------------------------------
		 DELEGATE IMPLEMENTATION
		 ---------------------------------------------------------------------------
		 */
		bindItem: function (controller, item, id) {
			controller.bindDefaultProperties(item, id);
			if (this.__model.getTooltip) {
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
			if (!this.__model) return selectables;
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
			if (!this.__selectableItems) {
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
			return false;
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
			if (!item) return;
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

			this.__changeListenerDisabled = false;
			this.fireDataEvent("changeValue", this._getModelValue(item), null);
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
			if (tree.isQuickSelection()) {} else {
				this.setSelection([item]);
			}
		},

		_onTreeChangeSelection: function (e) {
			var tree = this.__getTree();
			if (!tree) return;
			var selection = tree.getSelection();
			if (!selection || selection.getLength() == 0) return;
			var item = tree.getSelection().getItem(0);
			if (this.__changeListenerDisabled) return;

			if (!this._isItemSelectable(item)) {
				console.log("_onTreeChangeSelection.Not selectable");
				return;
			}
			if (this.__getTree().isQuickSelection()) {
				this.setSelection([item]);
			} else {
				this.setSelection([item]);
			}
		},

		/**
		 *****************************************************************************
		 PUBLIC
		 *****************************************************************************
		 */
		beforeEdit: function (context) {
			var parentData = context.parentData;
			var url = this.__supplant(this.__modelUrl, parentData);
			console.log("TreeSelector:beforeEdit:" + url);
			var model = this.__createModelFromUrl(url);
			this.setModel(model);
			this.__getTree().setMaxWidth(500);
		},
		beforeAdd: function (context) {
			var parentData = context.parentData;
			var url = this.__supplant(this.__modelUrl, parentData);
			console.log("TreeSelector:beforeAdd:" + url);
			var model = this.__createModelFromUrl(url);
			this.setModel(model);
			this.__getTree().setMaxWidth(500);
		},
		__createModelFromUrl: function (url) {
			var treeModel = ms123.util.Remote.sendSync(url);
			var rootModel = {};
			rootModel.children = [];
			rootModel.title = "ROOT";
			rootModel.children.push(treeModel);
			this._translateModel(treeModel);
			var model = qx.data.marshal.Json.createModel(rootModel);
			return model;
		},
		__supplant: function (s, o) {
			return s.replace(/@{([^{}]*)}/g, function (a, b) {
				var r = o[b];
				return typeof r === 'string' || typeof r === 'number' ? r : a;
			});
		},
		_translateModel: function (model) {
			model.title = this.tr(model.title);
			var children = model.children;
			if (children) {
				for (var i = 0; i < children.length; i++) {
					var c = children[i];
					this._translateModel(c);
				}
			} else {
				model.children = [];
			}
			return model;
		},
		setModel: function (model) {
			if (typeof model != 'string') {
				this.__model = model;
				this._getItems();
				var control = this.__getTree();
				control.setDelegate(this);
				control.getSelection().removeListener("change", this._onTreeChangeSelection, this);
				control.setModel(model);
				control.getSelection().addListener("change", this._onTreeChangeSelection, this);
			} else {
				this.__modelUrl = model.substring("url:".length);
			}
		},
		getModelSelection: function () {
			console.log("TreeSelector.getModelSelection:" + this.getSelection());
			return new qx.data.Array(this.getSelection());
		},
		setValue: function (value) {
			console.log("TreeSelector.setValue:" + value);
			if (this.__getTree()) {
				if (value == null) {
					var sel = this.__getTree().getSelection();
					if (sel && sel.getLength() > 0) {
						this.fireDataEvent("changeValue", this._getModelValue(sel.getItem(0)), null);
					}
				} else {
					var item = this._getSelectableById(value);
					console.log("TreeSelector.setValue.item:" + item);
					if (item) {
						var sel = this.__getTree().getSelection();
						sel.splice(0, 1, item);
						this.__getTree().openNodeAndParents(item);
					}
				}
			}
		},

		getValue: function () {
			var sel = this.__getTree().getSelection();
			if (sel && sel.getLength() > 0) {
				var val = this._getModelValue(sel.getItem(0));
				console.log("TreeSelector.getValue:" + val);
				return val;
			}
			return null;
		},
		_getModelValue: function (model) {
			if (model.getValue != null) {
				return model.getValue();
			}
			if (model.getId != null) {
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
							console.log("TreeSelector.setModelSelection:" + id + " -> " + x);
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
		this.__getTree().getSelection().removeListener("change");
		console.log("destruct.TreeSelector");
		this._disposeArray("__selectableItems");
		this._disposeArray("__model");
	}
});
