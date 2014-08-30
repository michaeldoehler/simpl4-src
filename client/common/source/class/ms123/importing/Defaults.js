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
	@asset(qx/icon/${qx.icontheme}/16/status/*)
*/


/**
 * A form widget which allows a multiple selection. 
 *
 */
qx.Class.define("ms123.importing.Defaults", {
	extend: qx.ui.core.Widget,
	include: qx.locale.MTranslation,

	/**
	 *****************************************************************************
	 CONSTRUCTOR
	 *****************************************************************************
	 */
	construct: function (context) {
		this.base(arguments);
		this._setLayout(new qx.ui.layout.HBox());
		this.__configManager = new ms123.config.ConfigManager();

		this.__storeDesc = context.storeDesc;
		var sp = this._doLayoutVertical();
		this._add(sp, {
			flex: 1
		});
		var spc = sp.getChildren();
		this._moduleTreeWidget = spc[0];
		this._formsTabWidget = spc[1];
		this._moduleTree = this._createModuleTree();
		this._formsTabView = new qx.ui.tabview.TabView().set({
			contentPadding: 0
		});
		this._formsTabWidget.add(this._formsTabView, {
			edge: "center"
		});
		this._formMap = {};
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
	},


	/**
	 *****************************************************************************
	 MEMBERS
	 *****************************************************************************
	 */


	members: {
		_createModuleTree: function () {
			var control = new qx.ui.tree.VirtualTree(null, "title", "children").set({
				focusable: false,
				hideRoot: false,
				keepFocus: true,
				openMode: "none",
				height: null,
				itemHeight: 20,
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
			this._moduleTreeWidget.add(control, {
				edge: "center"
			});
			control.addListener("open", this._onOpenM, this);
			control.addListener("dblclick", this._onDblClickModuleTree, this);
			control.addListener("click", this._onClickModuleTree, this);
			return control;
		},

		setValue: function (value) {
			var modList = value; //qx.lang.Json.parse(value);
			for (var i = 0; i < modList.length; i++) {
				var modDesc = modList[i];
				var name = modDesc["name"];
				var title = modDesc["title"];
				var content = modDesc["content"];
				var form = this._createFormPage(name, title);
				this._setFormContent(form, content);
			}
		},
		getValue: function () {
			var retlist = [];
			for (var name in this._formMap) {
				var form = this._formMap[name];
				var modDesc = {};
				modDesc["content"] = this._getFormContent(form);
				modDesc["title"] = form.getUserData("title");
				modDesc["name"] = name;
				retlist.push(modDesc);
			}
			//return qx.util.Serializer.toJson(retlist);
			return retlist;
		},

		prepareForSave: function () {
			//this._mappingTable.stopEditing();
		},
		setup: function (mainModule, maxlevel) {
			var cm = new ms123.config.ConfigManager();
			var moduleTree = cm.getEntityTree(this.__storeDesc,mainModule,maxlevel);
			this._prepareTree(moduleTree, 0);

			var moduleDelegate = {
				configureItem: function (item) {
					item.setIndent(13);
				},
				createItem: function () {
					return new ms123.importing.TreeItem();
				},
				bindItem: function (controller, item, id) {
					controller.bindDefaultProperties(item, id);
					controller.bindProperty("datatype", "leadIcon", {
						converter: function (data) {
							if (data == "list") {
								return "resource/ms123/javalistmodel.gif";
							} else if (data == "object") {
								return "resource/ms123/obj.gif";
							}
						}
					}, item, id);

				}
			};

			this._moduleTree.setDelegate(moduleDelegate);
			this._moduleTree.getSelection().removeListener("change", this.__onModuleTreeChangeSelection, this);
			var moduleModel = qx.data.marshal.Json.createModel(moduleTree);
			this._setParent(moduleModel, null);
			this._moduleTree.setModel(moduleModel);
			this._moduleTree.getSelection().addListener("change", this.__onModuleTreeChangeSelection, this);

		},

		_prepareTree: function (model) {
			if (!model.children) {
				model.children = [];
			}
			if (!model.value) {
				model.value = model.id;
			}
			model.title = this.tr(model.title)+"";
			for (var i = 0; model.children && i < model.children.length; i++) {
				var c = model.children[i];
				this._prepareTree(c);
			}
		},
		_setParent: function (node, parent) {
			var children = node.getChildren();
			node.parent = parent;
			if (children.getLength() > 0) {
				for (var i = 0; i < children.getLength(); i++) {
					var c = children.getItem(i);
					this._setParent(c, node);
				}
			}
		},

		_createFormPage: function (mainEntity, moduleTitle) {
			if (this._formMap[mainEntity]) return this._formMap[mainEntity];

			var model = this.__configManager.getEntityModel(mainEntity, this.__storeDesc, "main-form", "properties");
			var context = {};
			context.useitCheckboxes = true;
			context.useitCheckboxesDefault = true;
			context.buttons = {};
			context.model = model;
			context.config = mainEntity;
			var form = new ms123.widgets.Form(context);
			form.setUserData("title", moduleTitle);

			this._formMap[mainEntity] = form;

			var page = new qx.ui.tabview.Page(moduleTitle, "").set({
				showCloseButton: false
			});
			page.setDecorator(null);
			page.setLayout(new qx.ui.layout.Dock());
			page.add(form, {
				edge: "center"
			});
			this._formsTabView.add(page, {
				edge: 0
			});
			return form;
		},
		_getFormContent: function (form) {
			var map = {};
			var m = form.form.getModel();
			var props = qx.Class.getProperties(m.constructor);
			var items = form.form.getItems();
			for (var i = 0, l = props.length; i < l; i++) {
				var p = props[i];
				var v = items[p].getUserData("useit").getValue();
				var useit = false;
				if (typeof v == "boolean") {
					useit = v;
				} else {
					useit = (v == "ignore") ? false : true;
				}
				if (useit) {
					var val = m.get(p);
					map[p] = val;
				}
			}
			return map;
		},
		_setFormContent: function (form, data) {
			var map;
			if (typeof data == "string") {
				map = qx.lang.Json.parse(data);
			} else {
				map = data;
			}
			var model = form.form.getModel();
			var items = form.form.getItems();
			for (var p in map) {
				model.set(p, map[p]);
				if (items[p].getUserData("useit").getValue() == "ignore") {
					items[p].getUserData("useit").setValue("replace");
				} else {
					items[p].getUserData("useit").setValue(true);
				}
			}
		},

		_doLayoutVertical: function () {
			var splitpane = new qx.ui.splitpane.Pane("vertical");
			var topWidget = new qx.ui.container.Composite();
			topWidget._setLayout(new qx.ui.layout.Dock());
			topWidget.setDecorator(null);
			topWidget.setMinWidth(100);
			splitpane.add(topWidget, 3);

			var bottomWidget = new qx.ui.container.Composite();
			bottomWidget.setLayout(new qx.ui.layout.Dock());
			bottomWidget.setDecorator(null);
			bottomWidget.setMinWidth(100);
			splitpane.add(bottomWidget, 5);
			return splitpane;
		},
		__onModuleTreeChangeSelection: function (e) {
			console.log("__onChangeSelection");
			var tree = this._moduleTree;
			if (!tree) return;
			var selection = tree.getSelection();
			if (!selection || selection.getLength() == 0) return;
			var item = tree.getSelection().getItem(0);

			console.log("__onModuleTreeChangeSelection:" + item.getValue());
		},
		_onClickModuleTree: function (e) {
			var tree = this._moduleTree;
			var item = tree.getSelection().getItem(0);
			var dataType = item.getDatatype();
			var mainEntity = item.getEntity();
			var moduleTitle = item.getTitle();
			console.log("dataType:" + dataType);
			console.log("mainEntity:" + mainEntity);
			this._createFormPage(mainEntity, moduleTitle);
		},
		_onDblClickModuleTree: function (e) {},
		_onOpenM: function (e) {
			var item = e.getData();
			var tree = this._moduleTree;
			var lookup = tree.getLookupTable();
			var index = lookup.indexOf(item);
			if (index == -1) {
				tree.openNodeAndParents(item);
				index = lookup.indexOf(item);
			}
			var sel = tree.getSelection();
			sel.splice(0, 1, item);
		}
	}
});
