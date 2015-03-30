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
qx.Class.define('ms123.shell.ProjectShell', {
	extend: qx.core.Object,
	include: [qx.locale.MTranslation, ms123.baseeditor.MPlugin],

	construct: function (context) {
		this.setWindow(context.window);
		if( context.title ){
			this.getWindow().setCaption(this.tr(context.title));
		}else{
			this.getWindow().setCaption(this.tr("shell.project")+"("+context.storeDesc.getNamespace()+")");
		}
		this._pluginsData = [];
		this._eventsQueue = [];
		this._eventListeners = new Hash();
		this._facade = this.getPluginFacade();
		this._facade.storeDesc = context.storeDesc;
		this._facade.hideRoot = context.hideRoot;

		var includePluginList = context.includePluginList;

		var plugins = [];
		if( includePluginList == null){
			plugins.push ( new ms123.shell.InternalPlugin(this._facade));
			plugins.push ( new ms123.entitytypes.EntitytypePlugin(this._facade));
			plugins.push ( new ms123.enumerations.EnumPlugin(this._facade));
			plugins.push ( new ms123.messages.Plugin(this._facade));
			this._facade.includePathList = null;
		}else{
			this._facade.includePathList = [];
			if( includePluginList.indexOf("messages")!=-1){
				plugins.push ( new ms123.messages.Plugin(this._facade));
				this._facade.includePathList.push("messages");
			}
			if( includePluginList.indexOf("camel")!=-1){
				plugins.push ( new ms123.shell.InternalPlugin(this._facade));
				this._facade.includePathList.push("camel");
			}
			if( includePluginList.indexOf("forms")!=-1){
				plugins.push ( new ms123.shell.InternalPlugin(this._facade));
				this._facade.includePathList.push("forms");
			}
			if( includePluginList.indexOf("enumerations")!=-1){
				plugins.push ( new ms123.enumerations.EnumPlugin(this._facade));
				this._facade.includePathList.push("enumerations");
			}
			if( includePluginList.indexOf("internal")!=-1){
				plugins.push ( new ms123.shell.InternalPlugin(this._facade));
				this._facade.includePathList.push("processes");
				this._facade.includePathList.push("forms");
				this._facade.includePathList.push("filter");
			}
			if( includePluginList.indexOf("entitytypes")!=-1){
				plugins.push ( new ms123.entitytypes.EntitytypePlugin(this._facade));
				this._facade.includePathList.push("data_descriptions");
			}
		}
		console.log("includePathList:"+qx.util.Serializer.toJson(this._facade.includePathList));
		this._facade.pluginManager = new ms123.shell.PluginManager( plugins );
		this._facade.excludePathList =this._facade.pluginManager.getExcludePaths();

		//this._createConfig();
//		this._registerPluginsOnKeyEvents();
//		this._initEventListener();
		this._buildLayout();
	},

	properties: {
		window: {
			check: 'Object'
		}
	},

	members: {
		_buildLayout: function () {

			var window = this.getWindow();
			window.setLayout(new qx.ui.layout.Grow());

			window.set({
				contentPadding: 2
			});

			this._mainContainer = new qx.ui.container.Composite(new qx.ui.layout.Dock());
			window.add(this._mainContainer );

			this._navigator = new ms123.shell.Navigator(this._facade);
			this._facade.navigator = this._navigator;
			this._viewManager = new ms123.shell.ViewManager(this._facade);

			var sp = this._splitPane( this._navigator, this._viewManager);
			this._mainContainer.add(sp );

			
		},

		_splitPane: function (left, right) {
			var splitPane = new qx.ui.splitpane.Pane("horizontal").set({
				decorator: null
			});

			splitPane.add(left, 2);
			splitPane.add(right, 8);
			return splitPane;
		}
	}
});
