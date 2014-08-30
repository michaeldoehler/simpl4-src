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
 */
qx.Class.define("ms123.util.BaseResourceSelector", {
	extend: ms123.widgets.CollapsablePanel,
	include: qx.locale.MTranslation,


	/**
	 *****************************************************************************
	 CONSTRUCTOR
	 *****************************************************************************
	 */
	construct: function (facade, title) {
		this._setupIcons();
		this.base(arguments, title ? title : this.tr("util.resources"), new qx.ui.layout.Grow(), "icon/22/places/user-desktop.png");
		this.setValue(true);
		this.set({
			contentPadding: 0
		});
		this.facade = facade;


		this._resourceSelector = this._createResourceSelectorTree();
		this.add(this._resourceSelector, {
			edge: "center"
		});
		this._setup();
		this.contextMenu = this.facade.contextMenu;
		facade.leftSpace.add(this, {
			flex: 1
		});
	},

	/******************************************************************************
	 STATICS
	 ******************************************************************************/
	statics: {
		EVENT_RESOURCE_SELECTED: "resource_selected",
		NAMESPACE_TYPE: "namespace_type",
		PROCESS_TYPE: "process_type",
		PROCESSES_TYPE: "processes_type",
		FIELD_TYPE: "field_type",
		FIELDS_TYPE: "fields_type",
		FIELDSET_TYPE: "fieldset_type",
		FIELDSETS_TYPE: "fieldsets_type",
		ENTITY_TYPE: "entity_type",
		ENTITIES_TYPE: "entities_type",
		VIEW_TYPE: "view_type",
		VIEW_PROPERTY_TYPE: "view_property_type",
		PROPERTY_TYPE: "property_type",
		VIEWS_TYPE: "views_type"
	},

	/**
	 *****************************************************************************
	 PROPERTIES
	 *****************************************************************************
	 */

	events: {
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
		_setupIcons: function () {
			var NAMESPACE_ICON = "resource/ms123/folder_database.png";
			var PROCESS_ICON = "resource/ms123/sequence.png";
			var PROCESSES_ICON = "resource/ms123/folder_link.png";
			var FIELD_ICON = "resource/ms123/column.png";
			var FIELDS_ICON = "resource/ms123/attribute.png";
			var FIELDSET_ICON = "resource/ms123/sequence.png";
			var FIELDSETS_ICON = "resource/ms123/folder_link.png";
			var ENTITY_ICON = "resource/ms123/table.png";
			var ENTITIES_ICON = "resource/ms123/folder_table.png";
			var VIEW_ICON = "resource/ms123/view.png";
			var VIEW_PROPERTY_ICON = "resource/ms123/settings-16x16.png";
			var PROPERTY_ICON = "resource/ms123/settings-16x16.png";
			var VIEWS_ICON = "resource/ms123/folder_view.png";

			this._iconMapping = {};
			this._iconMapping[ms123.util.BaseResourceSelector.NAMESPACE_TYPE] = {
				icon: NAMESPACE_ICON
			};
			this._iconMapping[ms123.util.BaseResourceSelector.PROCESS_TYPE] = {
				icon: PROCESS_ICON
			};
			this._iconMapping[ms123.util.BaseResourceSelector.PROCESSES_TYPE] = {
				icon: PROCESSES_ICON
			};
			this._iconMapping[ms123.util.BaseResourceSelector.FIELD_TYPE] = {
				icon: FIELD_ICON
			};
			this._iconMapping[ms123.util.BaseResourceSelector.FIELDS_TYPE] = {
				icon: FIELDS_ICON
			};
			this._iconMapping[ms123.util.BaseResourceSelector.FIELDSET_TYPE] = {
				icon: FIELDSET_ICON
			};
			this._iconMapping[ms123.util.BaseResourceSelector.FIELDSETS_TYPE] = {
				icon: FIELDSETS_ICON
			};
			this._iconMapping[ms123.util.BaseResourceSelector.ENTITY_TYPE] = {
				icon: ENTITY_ICON
			};
			this._iconMapping[ms123.util.BaseResourceSelector.ENTITIES_TYPE] = {
				icon: ENTITIES_ICON
			};
			this._iconMapping[ms123.util.BaseResourceSelector.PROPERTY_TYPE] = {
				icon: PROPERTY_ICON
			};
			this._iconMapping[ms123.util.BaseResourceSelector.VIEW_PROPERTY_TYPE] = {
				icon: VIEW_PROPERTY_ICON
			};
			this._iconMapping[ms123.util.BaseResourceSelector.VIEW_TYPE] = {
				icon: VIEW_ICON
			};
			this._iconMapping[ms123.util.BaseResourceSelector.VIEWS_TYPE] = {
				icon: VIEWS_ICON
			};
		},
		_createResourceSelectorTree: function () {
			var control = new qx.ui.tree.VirtualTree(null, "title", "children").set({
				focusable: false,
				hideRoot: true,
				keepFocus: true,
				openMode: "none",
				height: null,
				itemHeight: 20,
				selectionMode: "one",
				//dragSelection:true,
				contentPaddingLeft: 0,
				showTopLevelOpenCloseIcons: true,
				quickSelection: false
			});
			control.setIconPath("id");
			control.setIconOptions({
				converter: (function (value, model, source, target) {
					var menu = this._createContextMenu(target, model, value);
					if (menu != null) {
						target.setContextMenu(menu);
					}
					return this._getIcon(target, model, value);
				}).bind(this)
			});
			control.addListener("click", this._onClickTree, this);
			control.addListener("open", this._onOpenNode, this);
			//control.setDraggable(true);
			control.addListener("dragstart", function (e) {
				//if (!check.isValue()) {
				// e.preventDefault();
				//}
				// Register supported types
				e.addType("value");
				e.addType("items");

				// Register supported actions
				e.addAction("copy");
				e.addAction("move");
			});
			control.addListener("droprequest", function (e) {
				console.debug("Related of droprequest: " + e.getRelatedTarget());

				var action = e.getCurrentAction();
				var type = e.getCurrentType();
				console.debug("Related of droprequest: " + action + "/" + type);
				var result;

				switch (type) {
				case "items":
					result = this.getSelection();
					if (action == "copy") {
						var copy = [];
						for (var i = 0, l = result.length; i < l; i++) {
							copy[i] = result[i].clone();
						}
						result = copy;
					}
					break;

				case "value":
					result = this.getSelection()[0].getLabel();
					break;
				}

				// Remove selected items on move
				//if (action == "move")
				//{
				//   var selection = this.getSelection();
				//  for (var i=0, l=selection.length; i<l; i++) {
				//   this.remove(selection[i]);
				//  }
				// }
				// Add data to manager
				e.addData(type, result);
			});



			return control;
		},

		_setup: function () {
			var delegate = {
				configureItem: function (item) {
					item.setIndent(13);
				},
				createItem: function () {
					return new ms123.shell.TreeItem();
				},
				bindItem: (function (controller, item, id) {
					controller.bindProperty("", "model", null, item, id);
					//controller.bindProperty( controller.getLabelPath(), "label", controller.getLabelOptions(), item, id);
					controller.bindProperty("title", "label", null, item, id);
					controller.bindProperty(controller.getIconPath(), "icon", controller.getIconOptions(), item, id);
				}).bind(this)
			};

			this._resourceSelector.setDelegate(delegate);
			var t = this._createTreeModel();

			var model = qx.data.marshal.Json.createModel(t, true);
			this.setParentModel(model);
			this._resourceSelector.setModel(model);
			this._resourceSelector.getSelection().addListener("change", this._selectionChanged, this);
		},
		setParentModel: function (model) {
			var children = model.getChildren();
			if (children.getLength() > 0) {
				for (var i = 0; i < children.getLength(); i++) {
					var c = children.getItem(i);
					c.parent = model;
					this.setParentModel(c);
				}
			}
			return null;
		},
		_createTreeModel: function () {
			var fielddummy = {
				id: "fielddummy",
				level: "3",
				title: "fielddummy",
				children: []
			};

			var namespaces = [];
			var ns1 = {};
			ns1.name = this.facade.storeDesc.getNamespace();
			namespaces.push(ns1);
			this._sortByName(namespaces);
			var nsarray = [];
			for (var n = 0; n < namespaces.length; n++) {
				var namespace = namespaces[n];
				var entityarray = [];

				var ns = {}
				ns.id = namespace.name;
				ns.title = namespace.name;
				ns.type = ms123.util.BaseResourceSelector.NAMESPACE_TYPE;
				ns.level = "1";
				ns.children = entityarray;
				nsarray.push(ns);

				var cm = new ms123.config.ConfigManager();
				var entities = cm.getEntities(this.facade.storeDesc);
				this._sortByName(entities);
				for (var i = 0; i < entities.length; i++) {
					var ename = entities[i].name;
					var m = {}
					m.id = ename;
					m.title = ename;
					m.type = ms123.util.BaseResourceSelector.ENTITY_TYPE;
					m.level = "2";
					m.namespace = namespace.name;
					m.children = [fielddummy];
					entityarray.push(m);
				}
			}

			var model = {}
			model.id = "ROOT";
			model.title = "ROOTTITLE";
			model.level = "0";
			model.children = nsarray;
			return model;
		},
		_selectionChanged: function (e) {
			console.log("_selectionChanged");
		},

		_onOpenNode: function (e) {
			var item = e.getData();
			var childs = item.getChildren();
			if (childs.getLength() == 1 && childs.getItem(0).getId() == "fielddummy") {
				var fields = null;
				try {
					var cm = new ms123.config.ConfigManager();
					fields = ms123.util.Remote.rpcSync("config:getEntityMetaFields", {
						namespace: item.getNamespace(),
						storeId: this.facade.storeDesc.getStoreId(),
						entity: item.getId()
					});
				} catch (e) {
					ms123.form.Dialog.alert("BaseResourceSelector._createTreeModel3:" + e);
					return;
				}

				this._sortByName(fields);
				var fieldarray = [];
				for (var i = 0; i < fields.length; i++) {
					var fname = fields[i].name;
					var f = {}
					f.id = fname;
					f.title = fname;
					f.type = ms123.util.BaseResourceSelector.FIELD_TYPE;
					f.level = "4";
					f.children = [];
					fieldarray.push(f);
				}

				var model = qx.data.marshal.Json.createModel(fieldarray, true);
				childs.removeAll();
				childs.append(model);
				this.setParentModel(item);
			}
		},

		_sortByKey: function (array, key) {
			array.sort(function (a, b) {
				a = a[key].toLowerCase();
				b = b[key].toLowerCase();
				if (a < b) return -1;
				if (a > b) return 1;
				return 0;
			});
		},

		_sortByName: function (array) {
			array.sort(function (a, b) {
				a = a.name.toLowerCase();
				b = b.name.toLowerCase();
				if (a < b) return -1;
				if (a > b) return 1;
				return 0;
			});
		},

		_onClickTree: function (e) {
			var model = this._resourceSelector.getSelection().getItem(0);

			var type = model.getType();
			var icons = this._iconMapping[type];
			console.log("_onClickTree:" + type);
			this.facade.raiseEvent({
				model: model,
				icon: icons.icon,
				facade: this.facade,
				type: ms123.util.BaseResourceSelector.EVENT_RESOURCE_SELECTED
			});
		},
		_getIcon: function (target, model, id) {
			var type = model.getType();
			var icons = this._iconMapping[type];
			if (icons) {
				console.log("type:" + type + "->" + icons.icon);
				return icons.icon;
			}
			return "";
		},
		_createContextMenu: function (item, model, id) {
			return null;
		}
	}
});
