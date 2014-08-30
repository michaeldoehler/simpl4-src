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

qx.Class.define("ms123.shell.ViewManager", {
	extend: qx.ui.core.Widget,
 include : qx.locale.MTranslation,


	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (facade) {
		this.base(arguments);
		this.facade = facade;
		this._setLayout(new qx.ui.layout.Dock());
		this._mainTabs = new qx.ui.tabview.TabView().set({
			contentPadding: 0
		});
		this._add(this._mainTabs, { edge: "center" });
		this._mainTabs.addListener("changeSelection", function (e) {
			//var pid = e._target.getSelection()[0].getUserData("id");
    }, this);
		this.facade.registerOnEvent(ms123.shell.Config.EVENT_ITEM_SELECTED, this._onItemSelected.bind(this));
	},


	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},


	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		_onItemSelected: function (event) {
			var clazz = event.clazz;
			var model = event.model;
			var title = event.title;
			var tabtitle = event.tabtitle;
			var kind = event.kind;
			var icon = event.icon;
			var param = event.param;
			var facade = event.facade;
			if( kind == "tab"){
				title = tabtitle || title;
				this._createTab(clazz,title,model,param, facade,icon);
			}
			if( kind == "dialog"){
				new clazz( model,param,facade);
			}
			if( kind == "window"){
				new clazz( model,param,facade);
			}
		},
		_createTab: function (clazz,title, model,param, facade,icon) {
			title = title.replace("%n", model.getId());
			this._page = new qx.ui.tabview.Page(title,icon).set({
				showCloseButton: true
			});
			var c = new clazz( model,param, facade);
			this._page.setUserData( "component",c);
			this._page.addListener("close", function (e) {
				var page = e._target;
				console.log("ViewManager.close:"+page);
				var comp = page.getUserData("component");
				console.log("ViewManager.close:"+comp);
				var editor=null;
				try{
					editor = comp.getEditor();
				}catch(e){
				}
				console.log("ViewManager.editor:"+editor);
				if( editor && editor._destroy ) editor._destroy();
			}, this);
			this._page.setLayout(new qx.ui.layout.Dock());
			this._page.add( c, { edge:"center" });
			this._mainTabs.add(this._page, {
				edge: 0
			});
			this._mainTabs.setSelection([this._page]);
		},
		_hideTabs: function () {
			this._mainTabs.setEnabled(false);
			this._mainTabs.setVisibility("hidden");
		},
		_showTabs: function () {
			this._mainTabs.setEnabled(true);
			this._mainTabs.setVisibility("visible");
		}
	},
	destruct: function () {
	}
});
