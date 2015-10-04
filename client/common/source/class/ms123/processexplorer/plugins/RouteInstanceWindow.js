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
	* @ignore(Hash)
	* @ignore(Clazz)
*/

qx.Class.define("ms123.processexplorer.plugins.RouteInstanceWindow", {
	extend: qx.core.Object,
	include: [qx.locale.MTranslation],
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (facade) {
		this.base(arguments);
		this.facade = facade;
		this.facade.registerOnEvent(ms123.processexplorer.Config.EVENT_SHOW_ROUTEINSTANCE, this._handleCreateWindowEvent.bind(this));

	},


	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},

	/******************************************************************************
	 STATICS
	 ******************************************************************************/
	statics: {},
	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		_handleHideWindowEvent: function (e) {
			//this._window.close();
		},
		_handleCreateWindowEvent: function (e) {
			var value = e.value;
			var win = this._createWindow(e.name,value);
			win.open();
		},
		_createWindow: function (name,value) {
			var win = new ms123.desktop.Window(null, name, "").set({
				resizable: true,
				useMoveFrame: false,
				contentPadding: 4,
				useResizeFrame: false
			});

			win.setCaption(name);
			win.setLayout(new qx.ui.layout.Dock);
			win.setWidth(600);
			win.setHeight(400);
			win.setAllowMaximize(false);
			win.setAllowMinimize(true);
			win.setModal(false);
			win.setActive(false);
			win.minimize();
			win.center();

			var routesTabs = new qx.ui.tabview.TabView().set({
				contentPadding: 0,
				minHeight: 150
			});

			var keys = Object.keys(value);
			for( var i=0; i < keys.length;i++){
				var key = keys[i];
				var page = new qx.ui.tabview.Page(key.split("|")[0], this._getRouteIcon()).set({
					showCloseButton: false
				});

				var routeInstance = 	 new ms123.processexplorer.plugins.CamelHistoryInstance();
				routeInstance.setRouteInstanceData( value[key] );

				page.setLayout(new qx.ui.layout.Grow());
      	page.add(routeInstance);
				routesTabs.add(page, {
					edge: 0
				});
			}
      win.add(routesTabs,{edge:"center"});

			var app = qx.core.Init.getApplication();
			var ns = this.facade.storeDesc.getNamespace();
			var tb = app.getTaskbar(ns);
			var dt = app.getDesktop(ns);
			tb.addWindow(win);
			dt.add(win);
			return win;
		},
		_getRouteIcon:function(){
				return 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABEAAAAQCAYAAADwMZRfAAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAGFwAABhcBlmjpmQAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAADUSURBVDiN1dOxSgNhEATgb6MIai0qglailYTgc2hCaknpc8R3sLa28w3EF0iT1l7srBVZm7twF3I5EysH/mZ2Ztgd+GWmVR5u8VDlNq2OaxxHRL8kokhvRURsY4AdHGXmeDb85QkXGGJj4bzF3CnM3aW6JQEnuMFu26a1TiLiFB/o4TMznxd0c4C9zJzOOJxXNI94xygz3xoKvsclRiXXmdMkXpsCCnxjq+6q99DDYUvZ+zhr7GRdzJ/zj0IiYhgRd1VunQ94hW5ETP4S8oKvzHwqiR/NIQGpOnr1TgAAAABJRU5ErkJggg==';
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
