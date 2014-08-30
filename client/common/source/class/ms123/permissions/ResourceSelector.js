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
qx.Class.define("ms123.permissions.ResourceSelector", {
	extend: ms123.util.BaseResourceSelector,
	include: qx.locale.MTranslation,


	/**
	 *****************************************************************************
	 CONSTRUCTOR
	 *****************************************************************************
	 */
	construct: function (facade) {
		this.base(arguments, facade);
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
	statics: {
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
		__createEntitiesNode:function(id){
			var entitiesNode = {
				id: id,
				title: this.tr("permissions.entities"),
				type: ms123.util.BaseResourceSelector.ENTITIES_TYPE,
				children: []
			};
			return entitiesNode;
		},
		__createFieldsNode:function(idPrefix, childs){
			var fieldsNode = {
				id: idPrefix+"/fields",
				title: this.tr("permissions.fields"),
				type: ms123.util.BaseResourceSelector.FIELDS_TYPE,
				children: childs
			};
			return fieldsNode;
		},
		_createTreeModel: function () {
			var fielddummyNode = {
				id: "fielddummy",
				title: "fielddummy",
				children: []
			};

			var namespace = this.facade.storeDesc.getNamespace();
			var entitiesId = namespace+":entities";
			var dataPackId = namespace+":entities:data";
			var entityId   = namespace+":entities:data:{entity}";

			var entitiesNode = this.__createEntitiesNode( entitiesId);

			var root = {}
			root.id = "ROOT";
			root.title = "ROOT";
			root.children = [entitiesNode];

			var cm = new ms123.config.ConfigManager();
			var entities = cm.getEntities(this.facade.storeDesc);
			this._sortByName(entities);
			for (var i = 0; i < entities.length; i++) {
				var entityName = entities[i].name;
				var id = entityId.replace("{entity}", entityName);
				var fieldsNode = fielddummyNode;


				var m = {}
				m.id = id;
				m.title = entityName;
				m.type = ms123.util.BaseResourceSelector.ENTITY_TYPE;
				m.children = [fieldsNode];
				entitiesNode.children.push(m);
			}

			return root;
		},
		_onOpenNode: function (e) {
			var item = e.getData();
			var childs = item.getChildren();
			if (childs.getLength() == 1 && childs.getItem(0).getId() == "fielddummy") {
				var cm = new ms123.config.ConfigManager();
				var entity = item.getTitle();
				var fields = cm.getFields(this.facade.storeDesc,entity, false, true);
				var idPrefix = item.getId();
				this._sortByName(fields);
				var fieldarray = [];
				for (var i = 0; i < fields.length; i++) {
					var fname = fields[i].name;
					var f = {}
					f.id = idPrefix+":"+fname;
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
			return null;
		}
	}
});
