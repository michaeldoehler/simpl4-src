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
	@asset(qx/icon/${qx.icontheme}/16/actions/*)
	@asset(qx/icon/${qx.icontheme}/16/places/*)
	@asset(ms123/icons/*)
	@asset(ms123/*)
*/

qx.Class.define("ms123.settings.views.FieldsEdit", {
	extend: qx.ui.container.Composite,
	include: qx.locale.MTranslation,


	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (facade) {
		this.base(arguments);
		this.facade = facade;
		this.setLayout(new qx.ui.layout.Dock());

		this._model = facade.model;
		var entity = this._model.parent.parent.parent.getId();
		var id = this._model.getId();
		var namespace = facade.storeDesc.getNamespace();
		var p = id.split(".");
		var entity = p[1];
		var view = p[3];
		console.log("entity:" + entity);
		console.log("view:" + view);

		this.facade.namespace = namespace;
		this.facade.entity = entity;
		this.facade.view = view;

		this.facade.selectables = this._getSelectableFields(namespace, entity, view);
		var selected = this._getSelectedFields(namespace, entity, view);
		this.facade.selected = this._removeUnusedFields(selected, this.facade.selectables);

		var viewSelectedItems = new ms123.settings.views.SelectedItems(facade);
		var viewSelectableItems = new ms123.settings.views.SelectableItems(facade);
		viewSelectableItems.addListener("change", function (event) {
			console.log("change:" + qx.util.Serializer.toJson(event.getData()));
			var data = event.getData();
			data.enabled = true;
			if (data.value) {
				viewSelectedItems.addRecordByNamedField(data, "name", data.name);
			} else {
				viewSelectedItems.removeRecordByNamedField("name", data.name);
			}
		}, this);

		var sp = this._splitPane(viewSelectableItems, viewSelectedItems);
		this.add(sp, {
			edge: "center",
			left: 0,
			top: 0
		});
	},


	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},


	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		_getSelectableFields: function (namespace, entity, view) {
			var filter = null;
			if (view == "export" || view == "main-grid" || view=='duplicate-check') {
				filter = "filter=!datatype.startsWith('array/team')";
			}
			console.error("_getSelectableFields.Path:" + this._model.getId());
			var cm = new ms123.config.ConfigManager();
			return cm.getFields(this.facade.storeDesc,entity, true, false,filter);
		},
		_getSelectedFields: function (namespace, entity, view) {
			var filter = null;
			if (view == "export" || view == "main-grid") {
				filter = "filter=!datatype.startsWith('array/team')";
			}
			var f = null;
			try {
				var resourceid = this._model.getId();
				f = ms123.util.Remote.rpcSync("setting:getResourceSetting", {
					namespace: namespace,
					storeId: this.facade.storeDesc.getStoreId(),
					resourceid: resourceid,
					settingsid: this.facade.settingsid,
					entity: entity,
					filter: filter,
					view: view
				});
				if (f) {;
					f = f.fields;
				} else {
					f = [];
				}
			} catch (e) {
				ms123.form.Dialog.alert("settings.views.Edit._getSelectedFields:" + e);
			}
			return this._setDisplayName(entity, f);
		},
		_removeUnusedFields: function (selected, selectables) {
			var ret = [];
			if( selected== null || selectables == null) return ret;
			for (var i = 0; i < selected.length; i++) {
				if (this._contains(selectables, selected[i])) {
					ret.push(selected[i]);
				}
			}
			return ret;
		},
		_contains: function (selectables, f) {
			for (var i = 0; i < selectables.length; i++) {
				if (selectables[i].name == f.name) return true;
			}
			return false;
		},
		_setDisplayName: function (entity, selected) {
			if (selected == null) return null;
			for (var i = 0; i < selected.length; i++) {
				selected[i].displayname = this.tr("data." + entity + "." + selected[i].name).toString();
			}
			return selected;
		},
		_splitPane: function (top, bottom) {
			var splitPane = new qx.ui.splitpane.Pane("vertical").set({
				decorator: null
			});

			splitPane.add(top, 4);
			splitPane.add(bottom, 4);
			return splitPane;
		}
	}
});
