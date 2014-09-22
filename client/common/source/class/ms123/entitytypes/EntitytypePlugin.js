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
 */
qx.Class.define('ms123.entitytypes.EntitytypePlugin', {
	extend: qx.core.Object,
	implement: ms123.shell.IShellPlugin,
	include: qx.locale.MTranslation,


	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (facade) {
		this.base(arguments);
		this._facade = facade;
	},


	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},


	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		/**
		 */
		getContextMenuActions: function () {
			var contextMenu = [
			{
				nodetypes: ["sw.entitytypes"],
				clazz: ms123.entitytypes.FastEntitytypeCreate,
				param: {
					isNew: true,
					mode: "entity"
				},
				menuicon: "resource/ms123/classwizard.png",
				tabicon: "resource/ms123/classwizard.png",
				title: this.tr("entitytypes.new_fast_entitytype_create"),
				kind: "tab"
			},
			{
				nodetypes: ["sw.entitytypes"],
				clazz: ms123.entitytypes.EntitytypeEdit,
				param: {
					isNew: true,
					mode: "entity"
				},
				menuicon: "icon/16/actions/list-add.png",
				tabicon: "resource/ms123/table.png",
				title: this.tr("entitytypes.new_entitytype"),
				kind: "tab"
			},
			{
				nodetypes: ["sw.entitytypes"],
				clazz: ms123.entitytypes.DatabaseAdmin,
				param: {
					isNew: false,
					mode: "field"
				},
				menuicon: "icon/16/actions/list-add.png",
				tabicon: "resource/ms123/column.png",
				title: "DatabaseAdmin",
				kind: "tab"
			},
			{
				nodetypes: ["sw.entitytype"],
				clazz: ms123.entitytypes.EntitytypeEdit,
				param: {
					isNew: false,
					mode: "entity"
				},
				menuicon: "icon/16/actions/list-add.png",
				tabicon: "resource/ms123/table.png",
				title: this.tr("entitytypes.edit_entitytype"),
				tabtitle: "%n",
				kind: "tab"
			},
/*			{
				nodetypes: ["sw.entitytypes"],
				clazz: ms123.entitytypes.EntitytypeWizardForm,
				param: {
					isNew: true,
					mode: "entity"
				},
				menuicon: "resource/ms123/classwizardform.png",
				tabicon: "resource/ms123/classwizardform.png",
				title: this.tr("entitytypes.new_entitytype_wizard_form"),
				kind: "tab"
			},*/
			{
				nodetypes: ["sw.entitytype"],
				clazz: ms123.entitytypes.EntitytypeFieldEdit,
				param: {
					isNew: true,
					mode: "field"
				},
				menuicon: "icon/16/actions/list-add.png",
				tabicon: "resource/ms123/column.png",
				title: this.tr("entitytypes.new_field"),
				kind: "tab"
			}
/*,
			{
				nodetypes: ["sw.relations"],
				clazz: ms123.entitytypes.RelationsEdit,
				param: {
					isNew: true,
					mode: "relation"
				},
				menuicon: "icon/16/actions/list-add.png",
				tabicon: "resource/ms123/folder_link.png",
				title: this.tr("entitytypes.new_relation"),
				kind: "tab"
			}*/
			];
			return contextMenu;
		},

		/**
		 */
		getOnClickActions: function () {
			var onclick = [{
				nodetypes: ["sw.entitytype"],
				clazz: ms123.entitytypes.FastEntitytypeCreate,
				param: {
					isNew: false,
					mode: "entity"
				},
				tabicon: "resource/ms123/classwizard.png",
				title: "%n",
				kind: "tab"
			},
			{
				nodetypes: ["sw.entitytypes"],
				clazz: ms123.entitytypes.FastEntitytypeCreate,
				param: {
					isNew: true,
					mode: "entity"
				},
				menuicon: "resource/ms123/classwizard.png",
				tabicon: "resource/ms123/classwizard.png",
				title: this.tr("entitytypes.new_fast_entitytype_create"),
				kind: "tab"
			},
			{
				nodetypes: ["sw.entitytypes"],
				clazz: ms123.entitytypes.DatabaseAdmin,
				param: {
					isNew: false,
					mode: "field"
				},
				menuicon: "icon/16/actions/list-add.png",
				tabicon: "resource/ms123/column.png",
				title: "DatabaseAdmin",
				kind: "tab"
			},
			{
				nodetypes: ["sw.field"],
				clazz: ms123.entitytypes.EntitytypeFieldEdit,
				param: {
					isNew: false,
					mode: "field"
				},
				menuicon: "icon/16/actions/list-add.png",
				tabicon: "resource/ms123/column.png",
				title: "%n",
				kind: "tab"
			},
			{
				nodetypes: ["sw.relations"],
				clazz: ms123.entitytypes.RelationsEdit,
				param: {
					isNew: false,
					mode: "relation"
				},
				menuicon: "icon/16/actions/list-add.png",
				tabicon: "resource/ms123/folder_link.png",
				title: "%n",
				kind: "tab"
			}];
			return onclick;
		},

		/**
		 */
		prepareNode: function (model,level) {
			var fielddummyNode = {
				id: "fielddummy",
				value: "fielddummy",
				title: "fielddummy",
				children: []
			};
			if (model.id == "data_description" && level == 1) {
				var childs = model.children;
				for (var i = 0; i < childs.length; i++) {
					childs[i].type = "sw.pack";
				}
			}else if (model.type == "sw.pack") {
				this._currentPack = model.id;
			}else{
				if( this._currentPack )model.pack = this._currentPack;
			}
			if (model.id == "data_description" && level == 1) model.title = this.tr("entitytypes.data_description");
			if (model.id == "entitytypes" && level==3){
				model.type = "sw.entitytypes";
				model.title = this.tr("entitytypes.entitytypes");
			}
			if (model.id == "relations" && level == 3) model.title = this.tr("entitytypes.relations");
			if (model.type == "sw.entitytype") {
				model.children.push(fielddummyNode);
			}

		},
		onOpenNode: function (e) {
			var item = e.getData();
			var childs = item.getChildren();
			if (childs.getLength() == 1 && childs.getItem(0).getId() == "fielddummy") {
				var fields = null;
				try {
					var entity = item.getId();
					var data = ms123.util.Remote.rpcSync("entity:getEntitytype", {
						storeId: this._getStoreId(item.getPack()),
						name: entity
					});
					fields = this._toArray(data["fields"]);
				} catch (e) {
					ms123.form.Dialog.alert("EntitytypePlugin.onOpenNode:" + e);
					return;
				}
				if (fields) {
					console.log("fields:" + qx.util.Serializer.toJson(fields));
					this._sortByName(fields);
					var fieldarray = [];
					for (var i = 0; i < fields.length; i++) {
						var fname = fields[i].name;
						var f = {}
						f.id = fname;
						f.value = fname;
						f.entitytype = item.getId();
						f.title = fname;
						f.pack = item.getPack();
						f.type = "sw.field";
						f.children = [];
						fieldarray.push(f);
					}
				}

				var model = qx.data.marshal.Json.createModel(fieldarray, true);
				childs.removeAll();
				childs.append(model);
				this._facade.navigator.setParentModel(item);
			}
		},
		_getStoreId:function(pack){
			var storeId = this._facade.storeDesc.getStoreId();
			return storeId;
		},
		_toArray: function (map) {
			var arr = [];
			if (!map) return arr;
			for (var i in map) {
				if (map.hasOwnProperty(i)) {
					arr.push(map[i]);
				}
			}
			return arr;
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

		getExcludePaths: function () {return ["data_description/base"]},
		/**
		 */
		getIconMapping: function () {
			var iconMap = {};
			iconMap["sw.entitytypes"] = "sw.directory";
			iconMap["sw.entitytype"] = "resource/ms123/table.png";
			iconMap["sw.pack"] = "sw.directory";
			iconMap["sw.field"] = "resource/ms123/column.png";
			iconMap["sw.relations"] = "resource/ms123/folder_link.png";
			return iconMap;
		}
	}
});
