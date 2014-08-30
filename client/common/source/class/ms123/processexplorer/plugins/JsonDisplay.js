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

qx.Class.define("ms123.processexplorer.plugins.JsonDisplay", {
	extend: qx.core.Object,
	include: [qx.locale.MTranslation],
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (facade) {
		this.base(arguments);
		this.facade = facade;
		this.facade.registerOnEvent(ms123.processexplorer.Config.EVENT_SHOWDETAILS, this._handleShowWindowEvent.bind(this));
		this.facade.registerOnEvent(ms123.processexplorer.Config.EVENT_HIDEDETAILS, this._handleHideWindowEvent.bind(this));

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
		_handleShowWindowEvent: function (e) {
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

      var msgArea = new qx.ui.form.TextArea();
			msgArea.setFont(qx.bom.Font.fromString("Mono, 9px")); 
			msgArea.setValue( value );
      win.add(msgArea,{edge:"center"});

			var app = qx.core.Init.getApplication();
			var ns = this.facade.storeDesc.getNamespace();
			var tb = app.getTaskbar(ns);
			var dt = app.getDesktop(ns);
			tb.addWindow(win);
			dt.add(win);
			return win;
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
