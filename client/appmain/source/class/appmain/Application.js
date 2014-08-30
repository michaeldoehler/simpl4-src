/*************************************************************************
 
 Copyright:
 
 License:
 
 Authors:
 
 *************************************************************************/
/*
#ignore($)
#ignore($.i18n)
#ignore(jQuery)
#ignore(jQuery.i18n)
*/

/**
 * This is the main application class 
 */
qx.Class.define("appmain.Application", {
	extend: qx.application.Standalone,
	include : [ms123.searchfilter.MFields],

	members: {
		/**
		 * @return {var} TODOC
		 */
		getTaskbar: function () {
			return this._taskbar;
		},
		getTopview: function () {
			return this._topview;
		},
		getCenter: function () {
			return this._center;
		},
		getMain: function () {
			return this._main;
		},

		/**
		 * This method contains the initial application code and gets called 
		 * during startup of the application
		 *
		 * @return {void} 
		 */
		main: function () {
			// Call super class
			this.base(arguments);
//			inspector.Inspector.init();

			// Enable logging in debug variant
//			if (qx.core.Variant.isSet("qx.debug", "on")) {
				// support native logging capabilities, e.g. Firebug for Firefox
				qx.log.appender.Native;

				// support additional cross-browser console. Press F7 to toggle visibility
				qx.log.appender.Console;
//			}
			qx.locale.Manager.getInstance().setLocale("de_DE"); //@@@MS Geht so nicht, zumindest keine Auswirkung auf DateField
			//alert("locale2="+qx.locale.Manager.getInstance().getLocale());

			var dock = new qx.ui.layout.Dock();
			dock.setSort("y");

			//Deaktiviert die Konsoleausgabe 
			try{
				console.log('Console enabled');
			}
			catch(e){
				console = {
						log: function(s){void(0);},
						error: function(s){void(0);},
						debug: function(s){void(0);}
				}
			}



			qx.theme.manager.Meta.getInstance().setTheme(ms123.theme.Theme);
 //qx.theme.manager.Meta.getInstance().setTheme(qx.theme.Modern);
// qx.theme.manager.Meta.getInstance().setTheme(qxet.Light);

			qx.Theme.define("qx.theme.modern.Font", {
				fonts: {
					"default": {
						size: 7,
						lineHeight: 1.0
					}
				}
			});

			this._main = (new qx.ui.container.Composite(dock));

			this._center = new qx.ui.core.Widget().set({
				//  decorator       : "main",
				backgroundColor: "#dfdfdf"
			});

			this._topview = new qx.ui.embed.Html("<div id='toparea' style='font-size:82%;background:#efefef;border:0px solid black' />").set({
				height: 32
			});
			this._topview._setLayout(new qx.ui.layout.Basic());
			var menuview = new qx.ui.embed.Html("<div id='mainmenu' style='font-size:82%' />").set({
				width: 170
			});


			this._main.add(this._topview, { edge: "north" });
			this._main.add(menuview, { edge: "west" });


			this._main.add(this._center, { edge: "center" });

			this.getRoot().add(this._main, { edge: 0 });
			//this.i18n();

			var _this = this;
			this._main.addListener("appear", function () {
				this.__loginWindow = new ms123.LoginWindow();
				this.__loginWindow.addListener("changeLoginData", function(ev) {
					var loginData = ev.getData();
					_this._initNamesspaces();
					var app = qx.core.Init.getApplication();
					var appid = app.getUserData("appid");
					var cm = new ms123.config.ConfigManager();
					cm.setDefaultNamespace(appid );
			    var modules = ms123.util.Remote.sendSync("xconfig/modules/data");
					_this._prepareMessages(loginData.language);
					_this._addTheTaskbar(modules);
					new ms123.Main(modules);
				});
				this.__loginWindow.moveTo(320,230);
				this.__loginWindow.open(); 
			}, this);
		},
		_addTheTaskbar: function(modules){
			this._taskbar = new ms123.Taskbar(modules);
			this._taskbar.setBackgroundColor("#efefef");
			this._main.add(this._taskbar, { edge: "south" });
		},
		_initNamesspaces:function(){
			var url = window.location.href;
			var p = url.split("/");
			var appid = p[p.length-2];
			console.log("appid:"+appid+"/"+url);

      //var ns = ms123.util.Remote.sendSync("xconfig/namespaces");//@@@MS TODO
			var ns = {};
			ns.common = "common";
			ns.admin = "admin";

			var messages_ns = ns.common;
			var users_ns = ns.common;
			var admin_ns = ns.admin;

			allinoneMenu = true;
			this._messages_ns = messages_ns;

			var app = qx.core.Init.getApplication();
			app.setUserData("appid", appid);
			app.setUserData("app_ns", appid);
			app.setUserData("messages_ns", messages_ns);
			app.setUserData("users_ns", users_ns);
			app.setUserData("admin_ns", admin_ns);
		},
 		_prepareMessages:function(lang){
      var postdata = 'filters={"field":"language", "op":"eq", "data":"'+lang+'"}&rows=10000&page=1';
			var ret;
			if( this._messages_ns ){
      	ret = ms123.util.Remote.sendSync("/"+this._messages_ns+"/data/message_list?query=true", "POST", null, postdata, null);
			}else{
      	ret = ms123.util.Remote.sendSync("data/message_list?query=true", "POST", null, postdata, null);
			}
      var rows = ret.rows;
      var count = rows.length;
      var m = qx.locale.Manager.getInstance();
    	m.setLocale(lang);
      console.log("count:"+count+";lang:"+m.getLanguage());
      var transMap = {};
      for(var i=0; i < count; i++){
        var row = rows[i];
        transMap[row.msgid] = row.msgstr; 
      }
      m.addTranslation( lang, transMap );
      //m.addTranslation( "en", transMap );
    }
	}
});
