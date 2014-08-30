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
	@ignore(jQuery)
	@ignore(jQuery.extend)
	@asset(qx/icon/${qx.icontheme}/16/devices/drive-harddisk.png)
	@asset(qx/icon/${qx.icontheme}/16/actions/system-search.png)
	@asset(qx/icon/${qx.icontheme}/16/actions/view-restore.png)
	@asset(qx/icon/${qx.icontheme}/16/actions/*)
	@asset(qx/icon/${qx.icontheme}/16/places/*)
	@asset(qx/icon/${qx.icontheme}/16/apps/utilities-help.png)
	@asset(qx/icon/${qx.icontheme}/22/apps/preferences-users.png)
*/

qx.Class.define("ms123.searchfilter.SearchFilter", {
	extend: qx.ui.container.Composite,

	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (fieldsModel, fields, params,scrollToEnd) {
		this.base(arguments);
		this._fieldsModel = fieldsModel;
		this._fields = fields;
		if (this._fields.length == 0) return;
		this._params = params;
		this._createTree(scrollToEnd);
		this.addListener("keypress", (function (e) {
			var iden = e.getKeyIdentifier();
			if (iden == "Enter") {
				this.__searchButton.press();
				this.__searchButton.release();
				qx.event.Timer.once(function () {
					console.log("Execute");
					this.__searchButton.execute();
				}, this, 200);
			}
		}).bind(this));
	},
	events: {
		"change": "qx.event.type.Data"
	},

	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		scrollToEnd: function () {
			this._tree.scrollToY(1000);
		},
		_createTree: function (scrollToEnd) {
			var dock = new qx.ui.layout.Dock();
			var toolbar = this.getChildControl("toolbar");

			this.setOperations = new qx.data.Array();
			this.setOperations.push("union");
			this.setOperations.push("except");
			this.setOperations.push("intersect");


			this.existsSubSelectOperations = new qx.data.Array();
			this.existsSubSelectOperations.push("_exists_subselect");
			this.existsSubSelectOperations.push("_exists_not_subselect");

			this._setLayout(dock);
			this.setAllowGrowX(false);
			this.setAllowGrowY(false);
			this.setAllowStretchX(true);
			this.setAllowStretchY(true);

			var tree = new qx.ui.tree.Tree().set({});
			this._tree = tree;
			if( scrollToEnd === true){
				tree.addListenerOnce("appear", function () {
					tree.scrollToY(1000);
				}, this);
			}
			tree.setDecorator(null);
			tree.setRootOpenClose(true);

			this.add(tree, {
				edge: "center"
			});
			if (this._params.showToolbar === undefined || this._params.showToolbar == true) {
				this.add(toolbar, {
					edge: "south"
				});
				this._addButtons(toolbar);
			}


			// build the data
			var data = {
				label: "1",
				nodeName: "1",
				connector: "and",
				children: []
			};

			var firstField = this._fields[0];
			var data = {
				"label": "1",
				"nodeName": "1",
				"connector": "and",
				"children": [{
					"label": "1.1",
					"nodeName": "1.1",
					"connector": null,
					"field": firstField.itemval,
					"op": firstField.ops[0].op,
					"data": "",
					"children": []
				}]
			};

			this._initialData = jQuery.extend({}, data);


			this.model = qx.data.marshal.Json.createModel(data);
			//console.log("llll" + qx.dev.Debug.debugObjectToString(this.model, "", 3));
			// data binding
			var treeController = new qx.data.controller.Tree(null, tree, "children", "label");
			this.treeController = treeController;
			treeController.setDelegate(this);
			//treeController.setModel(this.model);
			this.setModel(this.model);
		},

		// delegate implementation
		bindItem: function (controller, item, id) {
			controller.bindProperty("label", "label", null, item.getUserData("label"), id);
			item.getUserData("label").setDetails(controller, item, id);
			item.getUserData("label").setModel(this.model);
			if (id.getConnector() != null) {

				var connector;
				if (this.setOperations.contains(id.getConnector())) {
					connector = new ms123.searchfilter.SetConnector(this._params);
				} else {
					connector = new ms123.searchfilter.Connector(this._params);
				}
				connector.addListener("changeValue", function (e) {
					this.fireDataEvent("change", this.model, null);
				}, this);
				item.addWidget(connector);

			connector.setPadding(0);
				item.setUserData("connector", connector);
				controller.bindProperty("connector", "value", null, item.getUserData("connector"), id);
				controller.bindPropertyReverse("connector", "value", null, item.getUserData("connector"), id);
			} else {
				if (this.existsSubSelectOperations.contains(id.getField())) {
					var existsSubSelect = new ms123.searchfilter.ExistsSubSelect();
					existsSubSelect.addListener("change", function (e) {
						this.fireDataEvent("change", this.model, null);
					}, this);
					item.addWidget(existsSubSelect);
					item.setUserData("existsSubSelect", existsSubSelect);
					controller.bindProperty("field", "field", null, item.getUserData("existsSubSelect"), id);
					controller.bindProperty("op", "op", null, item.getUserData("existsSubSelect"), id);
					controller.bindProperty("data", "data", null, item.getUserData("existsSubSelect"), id);
					controller.bindPropertyReverse("field", "field", null, item.getUserData("existsSubSelect"), id);
					controller.bindPropertyReverse("data", "data", null, item.getUserData("existsSubSelect"), id);
					controller.bindPropertyReverse("op", "op", null, item.getUserData("existsSubSelect"), id);
				} else {
					var condition = new ms123.searchfilter.Condition(this._fieldsModel, this._fields, this._params);
			condition.setPadding(0);
					condition.addListener("change", function (e) {
						this.fireDataEvent("change", this.model, null);
					}, this);
					item.addWidget(condition);
					item.setUserData("condition", condition);
					controller.bindProperty("field", "field", null, item.getUserData("condition"), id);
					controller.bindProperty("op", "op", null, item.getUserData("condition"), id);
					controller.bindProperty("data", "data", null, item.getUserData("condition"), id);
					controller.bindPropertyReverse("field", "field", null, item.getUserData("condition"), id);
					controller.bindPropertyReverse("data", "data", null, item.getUserData("condition"), id);
					controller.bindPropertyReverse("op", "op", null, item.getUserData("condition"), id);
				}
			}
		},


		// delegate implementation
		createItem: function () {
			var item = new ms123.searchfilter.TreeItem();
			item.setOpen(true);
			item.setMaxHeight(22);
			item.setPadding(0);
			item.setPaddingBottom(1);

			item.addSpacer();
			item.addOpenButton();

			var button = new ms123.searchfilter.MenuButton(this._params);
			button.addListener("change", function (e) {
				this.fireDataEvent("change", this.model, null);
			}, this);
			item.addWidget(button);
			item.setUserData("label", button);

			return item;
		},
		getModel: function () {
			return this.model;
		},
		setModel: function (model) {
			this.treeController.setModel(model);
			this.model = model
		},
		getFilter: function () {
			var f = qx.lang.Json.parse(qx.util.Serializer.toJson(this.model));
			this._removeNodeName(f);
			return qx.lang.Json.stringify(f);
		},
		setFilter: function (data) {
			this._createNodeName(data, "1");
			this.model = qx.data.marshal.Json.createModel(data);
			this.treeController.setModel(this.model);
		},
		getFilterObject: function () {
			var f = qx.lang.Json.parse(qx.util.Serializer.toJson(this.model));
			this._removeNodeName(f);
			return f;
		},
		_createChildControlImpl: function (id) {
			var control;
			switch (id) {
			case "toolbar":
				control = new qx.ui.toolbar.ToolBar().set({
					//	height:26, allowGrowY:false
				});
				control.setSpacing(5);
				break;
			}

			return control || this.base(arguments, id);
		},
		_createMenuButton: function (text, icon) {
			var b = new qx.ui.toolbar.MenuButton(text, icon);
			b.setPaddingTop(1);
			b.setPaddingBottom(1);
			return b;
		},
		_createButton: function (text, icon) {
			var b = new qx.ui.toolbar.Button(text, icon);
			b.setPaddingTop(1);
			b.setPaddingBottom(1);
			return b;
		},
		_addButtons: function (toolbar) {
			var menuPart = new qx.ui.toolbar.Part;

			toolbar.add(menuPart);
			toolbar.addSpacer();

			if (this._params.mainMenu) {
				var menu = this._createMenuButton(this.tr("searchfilter.menubutton"), "icon/16/actions/window-new.png");
				menu.setMenu(this._params.mainMenu);
				menuPart.add(menu);
			}
			if (this._params.actionMenu) {
				var menu = this._createMenuButton(this.tr("searchfilter.actionbutton"), "icon/16/actions/system-run.png");
				menu.setMenu(this._params.actionMenu);
				menuPart.add(menu);
			}
			if (this._params.stateSelect) {
				menuPart.add(this._params.stateSelect);
			}


			var btn1 = this._createButton(this.tr("searchfilter.resetbutton"), "icon/16/actions/view-restore.png");
			btn1.addListener("execute", function () {
				//var data = { label: "1", connector: "and", children: [] };
				var data = this._initialData;
				this.setFilter(data);
			}, this);
			toolbar.add(btn1);

			var btn2 = this._createButton(this.tr("searchfilter.savebutton"), "icon/16/actions/document-save.png");
			btn2.addListener("execute", function () {
				var m = qx.util.Serializer.toJson(this.model);
				this._params.onSave(m);
			}, this);

			btn2.setEnabled(true);
			toolbar.add(btn2);

			if (this._params.onSelect) {
				var btn3 = this._createButton(this.tr("searchfilter.selectbutton"), "icon/16/actions/document-save.png");
				btn3.setEnabled(true);
				btn3.addListener("execute", function () {
					this._params.onSelect(this);
				}, this);
				toolbar.add(btn3);
			}

			toolbar.add(new qx.ui.core.Spacer(), {
				flex: 1
			});

			var btn4 = this._createButton(this.tr("searchfilter.searchbutton"), "icon/16/actions/system-search.png");
			btn4.addListener("execute", function () {
				var m = qx.util.Serializer.toJson(this.model);
				this._params.onSearch(m);
			}, this);
			btn4.setEnabled(true);
			toolbar.add(btn4);
			this.__searchButton = btn4;
		},
		_removeNodeName: function (model) {
			delete model.nodeName;
			//delete model.label;
			if( model.field && model.field.id){
				model.field = model.field.id;
			}
			var children = model.children;
			if (children && children.length > 0) {
				for (var i = 0; i < children.length; i++) {
					this._removeNodeName(children[i]);
				}
			}
			return null;
		},
		_createNodeName: function (model, label) {
			model.nodeName = label;
			var children = model.children;
			if (children && children.length > 0) {
				for (var i = 0; i < children.length; i++) {
					this._createNodeName(children[i], label + "." + (i + 1));
				}
			}
			return null;
		},
		renameModelLabel: function (model, label) {
			var oLabel = model.getLabel();
			if (oLabel && oLabel.toLowerCase().match("^[a-z]")) {
				console.log("Label starts with letter");
			} else {
				model.setLabel(label);
			}
			model.setNodeName(label);
			var children = model.getChildren();
			if (children.getLength() > 0) {
				for (var i = 0; i < children.getLength(); i++) {
					this.renameModelLabel(children.getItem(i), label + "." + (i + 1));
				}
			}
			return null;
		}
	},
	destruct: function () {
		//this._disposeObjects("_editWindow");
	}
});
