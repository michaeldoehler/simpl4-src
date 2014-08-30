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
 * A form widget which allows a multiple selection. 
 *
 */
qx.Class.define("ms123.settings.ResourceSelector", {
	extend: ms123.util.BaseResourceSelector,
	include: qx.locale.MTranslation,


	/**
	 *****************************************************************************
	 CONSTRUCTOR
	 *****************************************************************************
	 */
	construct: function (facade) {
		this.base(arguments, facade);
		ms123.settings.views.FormPropertyEdit;
		ms123.settings.views.GridPropertyEdit;
		ms123.settings.views.SearchPropertyEdit;
		ms123.settings.views.GlobalSearchPropertyEdit;
		ms123.settings.views.ReportPropertyEdit;
		ms123.settings.views.ExportPropertyEdit;
		ms123.settings.views.DupCheckPropertyEdit;
	},

	/**
	 *****************************************************************************
	 PROPERTIES
	 *****************************************************************************
	 */
	events: {
		"changeSelection": "qx.event.type.Data"
	},

	/******************************************************************************
	 STATICS
	 ******************************************************************************/
	statics: {},

	properties: {
		// overridden
	},


	/**
	 *****************************************************************************
	 MEMBERS
	 *****************************************************************************
	 */


	members: {
		__createEntitiesNode: function (id, childs) {
			var entitiesNode = {
				id: id,
				title: this.tr("settings.entities"),
				type: ms123.util.BaseResourceSelector.ENTITIES_TYPE,
				//actionClass: "ms123.settings.views.EntitiesEdit",
				children: childs
			};
			return entitiesNode;
		},
		__createFieldsNode: function (idPrefix, childs) {
			var fieldsNode = {
				id: idPrefix + ".fields",
				title: this.tr("settings.fields"),
				type: ms123.util.BaseResourceSelector.FIELDS_TYPE,
				//actionClass: "ms123.settings.fields.FieldsEdit",
				children: childs
			};
			return fieldsNode;
		},
		__createFieldSetsNode: function (idPrefix, childs) {
			var fieldsetsNode = {
				id: idPrefix + ".fieldsets",
				title: this.tr("settings.fieldsets"),
				type: ms123.util.BaseResourceSelector.FIELDSETS_TYPE,
				actionClass: ms123.settings.fieldset.FieldsetsEdit,
				children: childs
			};
			return fieldsetsNode;
		},
		__createCommonPropertiesNode: function (idPrefix) {
			var node = {
				id: idPrefix + ".properties",
				title: this.tr("settings.properties"),
				type: ms123.util.BaseResourceSelector.PROPERTY_TYPE,
				actionClass: ms123.settings.CommonPropertyEdit,
				children: []
			};
			return node;
		},
		__createViewFieldsNode: function (idPrefix) {
			var node = {
				id: idPrefix + ".fields",
				title: this.tr("settings.fields"),
				type: ms123.util.BaseResourceSelector.FIELDS_TYPE,
				actionClass: ms123.settings.views.FieldsEdit,
				children: []
			};
			return node;
		},
		__createViewPropertyNode: function (clazzPrefix, idPrefix) {
			var node = {
				id: idPrefix + ".properties",
				title: this.tr("settings.view_properties"),
				type: ms123.util.BaseResourceSelector.VIEW_PROPERTY_TYPE,
				actionClass: ms123.settings.views[clazzPrefix+"PropertyEdit"],
				children: []
			};
			return node;
		},
		__createViewsNode: function (_idPrefix) {
			var idPrefix = _idPrefix + ".views";
			var idMainForm = idPrefix + ".main-form";
			var idMainGrid = idPrefix + ".main-grid";
			var idSearch = idPrefix + ".search";
			var idReport = idPrefix + ".report";
			var idGlobalSearch = idPrefix + ".global-search";
			var idDupCheck = idPrefix + ".duplicate-check";
			var idExport = idPrefix + ".export";
			var views = {
				id: idPrefix,
				title: this.tr("settings.views"),
				type: ms123.util.BaseResourceSelector.VIEWS_TYPE,
				children: [{
					id: idMainForm,
					title: this.tr("moduleeditor.main-form"),
					type: ms123.util.BaseResourceSelector.VIEW_TYPE,
					children: [this.__createViewFieldsNode(idMainForm), this.__createViewPropertyNode("Form",idMainForm)]
				},
				{
					id: idMainGrid,
					title: this.tr("moduleeditor.main-grid"),
					type: ms123.util.BaseResourceSelector.VIEW_TYPE,
					children: [this.__createViewFieldsNode(idMainGrid), this.__createViewPropertyNode("Grid",idMainGrid)]
				},
				{
					id: idSearch,
					title: this.tr("moduleeditor.search"),
					type: ms123.util.BaseResourceSelector.VIEW_TYPE,
					children: [this.__createViewFieldsNode(idSearch), this.__createViewPropertyNode("Search",idSearch)]
				},
				{
					id: idReport,
					title: this.tr("moduleeditor.report"),
					type: ms123.util.BaseResourceSelector.VIEW_TYPE,
					children: [this.__createViewFieldsNode(idReport), this.__createViewPropertyNode("Report", idReport)]
				},
				{
					id: idGlobalSearch,
					title: this.tr("moduleeditor.global-search"),
					type: ms123.util.BaseResourceSelector.VIEW_TYPE,
					children: [this.__createViewFieldsNode(idGlobalSearch), this.__createViewPropertyNode("GlobalSearch",idGlobalSearch)]
				},
				{
					id: idDupCheck,
					title: this.tr("moduleeditor.duplicate-check"),
					type: ms123.util.BaseResourceSelector.VIEW_TYPE,
					children: [this.__createViewFieldsNode(idDupCheck), this.__createViewPropertyNode("DupCheck", idDupCheck)]
				},
				{
					id: idExport,
					title: this.tr("moduleeditor.export"),
					type: ms123.util.BaseResourceSelector.VIEW_TYPE,
					children: [this.__createViewFieldsNode(idExport), this.__createViewPropertyNode("Export", idExport)]
				}]
			};
			return views;
		},
		_createTreeModel: function () {
			var fielddummyNode = {
				id: "fielddummy",
				title: "fielddummy",
				children: []
			};
			var fieldsetdummyNode = {
				id: "fieldsetdummy",
				title: "fieldsetdummy",
				children: []
			};

			var namespaces = [];
			var ns1 = {};
			namespaces.push(ns1);


			var namespace = this.facade.storeDesc.getNamespace();


			var entityarray = [];
			var entitiesNode = this.__createEntitiesNode("entities", entityarray);

			var root = {}
			root.id = "ROOT";
			root.title = "ROOT";
			root.children = [entitiesNode];

			var cm = new ms123.config.ConfigManager();
			var entities = cm.getEntities(this.facade.storeDesc);

			this._sortByName(entities);
			for (var i = 0; i < entities.length; i++) {
				var entityName = entities[i].name;
				var idPrefix = "entities." + entityName;
				var fieldsNode = this.__createFieldsNode(idPrefix, [fielddummyNode]);
				//var fieldsetsNode = this.__createFieldSetsNode( idPrefix, [fieldsetdummyNode]);
				var fieldsetsNode = this.__createFieldSetsNode(idPrefix, []);
				var commonPropertyNode = this.__createCommonPropertiesNode(idPrefix);
				var viewsNode = this.__createViewsNode(idPrefix);


				var m = {}
				m.id = "entities." + entityName;
				m.title = entityName;
				m.type = ms123.util.BaseResourceSelector.ENTITY_TYPE;
				m.namespace = namespace;
				m.children = [ /*fieldsNode,*/ viewsNode, commonPropertyNode,fieldsetsNode];
				entityarray.push(m);
			}

			return root;
		},
		_onOpenNode: function (e) {
			var item = e.getData();
			var childs = item.getChildren();
			if (childs.getLength() == 1 && childs.getItem(0).getId() == "fielddummy") {
				var fields = null;
				try {
					var p = item.parent.getId().split(".");
					var entity = p[p.length - 1];
					var cm = new ms123.config.ConfigManager();
					fields = cm.getFields(this.facade.storeDesc,entity, true, false);
				} catch (e) {
					ms123.form.Dialog.alert("ResourceSelector._createTreeModel3:" + e);
					return;
				}
				var idPrefix = item.parent.getId();
				this._sortByName(fields);
				var fieldarray = [];
				for (var i = 0; i < fields.length; i++) {
					var fname = fields[i].name;
					var f = {}
					f.id = idPrefix + "/" + fname;
					f.title = fname;
					f.type = ms123.util.BaseResourceSelector.FIELD_TYPE;
					f.children = [];
					fieldarray.push(f);
				}

				var model = qx.data.marshal.Json.createModel(fieldarray, true);
				childs.removeAll();
				childs.append(model);
				this.setParentModel(item);
			}
		},
		_createContextMenu: function (item, model, id) {
/*			var menu = new qx.ui.menu.Menu;
			
			var type = model.getType();
			var name = model.getTitle();

			menu.add(new qx.ui.menu.Separator());
			var button = new qx.ui.menu.Button(cme.title, cme.icon);
			button.setUserData("id", id);
			button.addListener("execute", function (e) {
			}, this);
			menu.add(button);
			return menu;*/
			return null;
		}
	}
});
