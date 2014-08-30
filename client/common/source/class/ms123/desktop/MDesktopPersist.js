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

qx.Mixin.define("ms123.desktop.MDesktopPersist", {

	/*******************************************************************************
	 MEMBERS
	 ******************************************************************************/

	members: {
		init: function () {
			var cm = ms123.config.ConfigManager;
			this._isSessionRestore = cm.isSessionRestore();
			if (!this._isSessionRestore) return;
			var namespace = this._namespace;
			var store = new qx.data.store.Offline("desktop-" + namespace);
			var model = null;
			if (store.getModel() === null) {
				model = qx.data.marshal.Json.createModel({
					stateList: []
				}, true);
				store.setModel(model);
			} else {
				model = store.getModel();
			}
			this._offlineModel = model;
			var stateList = model.getStateList();
			for (var i = 0; i < stateList.getLength(); i++) {
				var state = qx.lang.Json.parse(stateList.getItem(i));
				if (state.entityName) {
					this._restoreDesktopWindow(namespace, state);
				}
			}
		},
		_getStateList: function () {
			var windowList = this.getWindows();
			var stateList = [];
			for (var i = 0; i < windowList.length; i++) {
				var c = windowList[i].getContext();
				var entityName = null;
				if (c.widgets && c.widgets.length > 0) {
					entityName = c.widgets[0].config;
				}
				var state = null;
				if (windowList[i].getDesktopUnit() && qx.Class.hasInterface(windowList[i].getDesktopUnit().constructor, ms123.IState)) {
					state = windowList[i].getDesktopUnit().getState();
				}

				var so = {
					clazz: c.config,
					entityName: entityName,
					state: state
				}
				stateList.push(qx.util.Serializer.toJson(so));
			}
			return stateList;
		},

		_restoreDesktopWindow: function (namespace, state) {
			var storeDesc = ms123.StoreDesc.getNamespaceDataStoreDescForNS(namespace);
			var m = new ms123.config.ConfigManager().getEntity(state.entityName, storeDesc);
			var widgetList = ms123.MainMenu.createWidgetList(m, storeDesc, this);
			widgetList[0].loadSync = true;
			var context = {
				storeDesc: storeDesc,
				unit_id: ms123.util.IdGen.nextId(),
				config: ms123.Crud,
				window_title: this.tr("data." + state.entityName),
				widgets: widgetList
			}
			var dw = new ms123.DesktopWindow(context);
			var dunit = dw.getDesktopUnit();
			dunit.setState(state.state);
		},
		updateStatus: function () {
			if (!this._isSessionRestore) return;
			var stateList = this._getStateList();
			this._offlineModel.setStateList(stateList);
		}
	}
});
