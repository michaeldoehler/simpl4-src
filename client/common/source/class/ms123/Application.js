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
qx.Class.define("ms123.Application", {
	extend: qx.application.Standalone,

	members: {
		main: function () {
			this.base(arguments);
			delete Array.prototype.toJSON;
			this._initLogging();
			this._initTheme("brown");
			//this._initTheme("ea");

			ms123.util.RelationList;
			ms123.util.RepoList;
			this.__taskbars = {};
			this.__desktops = {};
			this.__topPanels = {};
			this.__bottomPanels = {};

			var tabView = new qx.ui.tabview.TabView().set({
				contentPadding: 0
			});
			tabView.addListener("changeSelection", function (e) {
				var ns = e._target.getSelection()[0].getUserData("namespace");
				ms123.StoreDesc.setCurrentNamespace(ns);
				//Namespace und StoreDesc-Switch @@@MS
			}, this);

			this.getRoot().add(tabView, {
				edge: 0
			});

			var self = this;
			this.__loginWindow = new ms123.LoginWindow();
			this.__loginWindow.addListener("changeLoginData", function (ev) {
				var loginData = ev.getData();
				self._initTheme(loginData.theme);
				self._user = ms123.util.Remote.rpcSync("auth:getUserProperties");
				self._branding = ms123.util.Remote.rpcSync("namespace:getBranding");
				ms123.util.Remote.setSessionTimeout( self._branding.sessionTimeout);
				ms123.config.ConfigManager.setUserProperties( self._user);
				ms123.config.ConfigManager.setBranding( self._branding);
				ms123.config.ConfigManager.setLanguage( loginData.language);
				var apps = ms123.util.Remote.rpcSync("git:getRepositories");
				var nsList = [];
				var currenNSIndex = 0;
				if (self._user.admin) {
					nsList.push("global");
					currenNSIndex = (apps.length > 1 ? 1 : 0);
				}
				for (var i = 0; i < apps.length; i++) {
					var app = apps[i];
					if (app.name == "global") {
						continue;
					} else {
						nsList.push(app.name);
					}
				}

				var pageList = self._initDesktops(tabView, nsList);
				self._removeTabIfOnlyOne(tabView, nsList);
				self._prepareMessages(nsList, loginData.language);
				//self._initTheme("ea");
				for (var i = 0; i < nsList.length; i++) {
					self._initStoreDesc(nsList[i]);
				}
				if (!self._user.admin) {
					self._initStoreDesc("global");
				}
				for (var i = 0; i < nsList.length; i++) {
					ms123.StoreDesc.setCurrentNamespace(nsList[i]);
					self._addMainMenu(nsList[i]);
					self._addTaskbar(nsList[i]);
				}
				ms123.StoreDesc.setCurrentNamespace(nsList[currenNSIndex]);
				tabView.setSelection([pageList[currenNSIndex]]);
				for (var i = 0; i < nsList.length; i++) {
					try{
						self.__desktops[nsList[i]].init();
					}catch(e){
						console.log("ns:"+nsList[i]);
						console.log("e:"+e);
					}
				}
			});
			this.__loginWindow.moveTo(320, 230);
			this.__loginWindow.open();
		},

		_removeTabIfOnlyOne: function (tabView, nsList) {
			if (nsList.length == 1) {
				var bar = tabView.getChildControl("bar");
				bar.setVisibility("hidden");
				bar.set({
					height: 0
				});
			}
		},
		_initDesktops: function (tabView, nsList) {
			var pageList = [];
			for (var i = 0; i < nsList.length; i++) {
				var page = new qx.ui.tabview.Page(nsList[i], "icon/16/actions/go-home.png");
				page.setUserData("namespace", nsList[i]);
				pageList.push(page);
				var dock = new qx.ui.layout.Dock();
				dock.setSort("y");
				page.setLayout(dock);
				tabView.add(page);
				tabView.setSelection([page]);


				var desktop = new ms123.desktop.Desktop(nsList[i], new qx.ui.window.Manager());
				//desktop.setBackgroundColor('#ffffff');

				var topPanel = new ms123.desktop.Panel();
				var bottomPanel = new ms123.desktop.Panel();
				this.__desktops[nsList[i]] = desktop;
				this.__topPanels[nsList[i]] = topPanel;
				this.__bottomPanels[nsList[i]] = bottomPanel;

				page.add(topPanel, {
					edge: "north"
				});
				page.add(desktop, {
					edge: "center"
				});
				page.add(bottomPanel, {
					edge: "south"
				});
			}
			return pageList;
		},

		getTaskbar: function (ns) {
			return this.__taskbars[ns];
		},
		getDesktop: function (ns) {
			return this.__desktops[ns];
		},
		_getTopPanel: function (ns) {
			return this.__topPanels[ns];
		},
		_getBottomPanel: function (ns) {
			return this.__bottomPanels[ns];
		},

		_addTaskbar: function (ns) {
			this.__taskbars[ns] = new ms123.desktop.TaskBar();
			this.__taskbars[ns].setBackgroundColor("#efefef");
			this.__bottomPanels[ns].add(this.__taskbars[ns]);
		},

		_addMainMenu: function (ns) {
			var panel = this._getBottomPanel(ns);
			panel.add(new ms123.MainMenu(ns));
		},

		_getStoreDesc: function (namespace) {
			return ms123.util.Remote.rpcSync("store:getStoreDescriptions", {
				namespace: namespace
			});
		},

		_initStoreDesc: function (ns) {
			ms123.StoreDesc.setCurrentNamespace(ns);
			var sdmap = this._getStoreDesc(ns);

			//var value = qx.lang.Json.stringify(sdmap, null, 4); console.log("sdmap:" + value);

			var sdescData = new ms123.StoreDesc({
				namespace: sdmap[ns + "_data"]["namespace"],
				pack: sdmap[ns + "_data"]["pack"],
				repository: sdmap[ns + "_data"]["repository"],
				storeId: sdmap[ns + "_data"]["storeId"]
			});
			var sdescMeta = new ms123.StoreDesc({
				namespace: sdmap[ns + "_meta"]["namespace"],
				pack: sdmap[ns + "_meta"]["pack"],
				repository: sdmap[ns + "_meta"]["repository"],
				storeId: sdmap[ns + "_meta"]["storeId"]
			});
			var sdescConfig = null;
			if( sdmap[ns + "_config"] != null ){
				sdescConfig = new ms123.StoreDesc({
					namespace: sdmap[ns + "_config"]["namespace"],
					pack: sdmap[ns + "_config"]["pack"],
					repository: sdmap[ns + "_config"]["repository"],
					storeId: sdmap[ns + "_config"]["storeId"]
				});
			}else{
				sdescConfig = sdescMeta;
			}


			if (ns == "global") {
				ms123.StoreDesc.setGlobalDataStoreDesc(sdescData);
				ms123.StoreDesc.setGlobalMetaStoreDesc(sdescMeta);
			} else {
				ms123.StoreDesc.setNamespaceDataStoreDesc(sdescData);
				ms123.StoreDesc.setNamespaceMetaStoreDesc(sdescMeta);
				ms123.StoreDesc.setNamespaceConfigStoreDesc(sdescConfig);
			}
		},
		_addLogout: function () {
			this._user = ms123.util.Remote.rpcSync("auth:getUserProperties");
			var tp = this._getTopPanel();

			var logout = new qx.ui.form.Button("Logout user:" + this._user.userid, "icon/22/apps/preferences-users.png");
			logout.addListener("execute", function () {
				window.location.reload();
			}, this);
			tp._add(logout);
		},

		_initLogging: function () {
			qx.dev.Debug;
			qx.log.appender.Native;
			qx.log.appender.Console;
			try {
				console.log('Console enabled');
			}
			catch (e) {
				console = {
					log: function (s) {
						void(0);
					},
					error: function (s) {
						void(0);
					},
					debug: function (s) {
						void(0);
					}
				}
			}
		},

		_initTheme: function (th) {
			if (th == "ms") {
				qx.theme.manager.Meta.getInstance().setTheme(ms123.theme.ms.Theme);
			}else if( th== "simple"){
				qx.theme.manager.Meta.getInstance().setTheme(ms123.theme.simple.Theme);
			}else if( th== "brown"){
				qx.theme.manager.Meta.getInstance().setTheme(ms123.theme.brown.Theme);
			} else {
				qx.theme.manager.Meta.getInstance().setTheme(ms123.theme.ea.Theme);
			}
			qx.Theme.define("qx.theme.modern.Font", {
				fonts: {
					"default": {
						size: 7,
						lineHeight: 1.0
					}
				}
			});
		},

		_prepareMessages: function (_nsList, lang) {
			var nsList = _nsList;
			if (nsList.indexOf("global") == -1) {
				nsList = _nsList.concat(["global"]);
			}
			var nsList = nsList.concat([]);
			for (var n = 0; n < nsList.length; n++) {
				var namespace = nsList[n];
				ms123.config.ConfigManager.installMessages(namespace,lang);
			}
		}
	}
});
