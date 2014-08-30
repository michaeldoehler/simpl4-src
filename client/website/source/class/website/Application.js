/**
 * @ignore(jQuery.*) 
 * @ignore(_paq.*) 
 */

/** ************************************************************************
	@asset(website/*)
	@require(qx.module.Manipulating)
	@require(qx.module.Attribute)
	@require(qx.module.Cookie)
	@require(qx.module.Traversing)
************************************************************************ */


/**
 * This is the main application class of your custom application "website"
 */
qx.Class.define("website.Application", {
	extend: qx.application.Standalone,
	//extend: qx.application.Inline,



/*
  *****************************************************************************
     MEMBERS
  *****************************************************************************
  */

	members: {
		main: function () {

			this._initLogging();
			console.error("STarting."+window.location.href);

			delete Array.prototype.toJSON;
			try{
				ms123.form.Form;
				ms123.Task;
				ms123.Crud;
				website.Login;
				website.Crud;
			}catch(e){
			}
			var co = q.cookie.get("sw.website.login");
			if( co != null){
				var x = co.split("/");
				ms123.util.Remote.setCredentials(x[0], x[1]);
				var user = ms123.util.Remote.rpcSync("auth:getUserProperties");
				console.log("user:"+qx.util.Serializer.toJson(user));
				ms123.config.ConfigManager.setUserProperties( user);
				ms123.config.ConfigManager.setLanguage( x[2]);
			}else{
				ms123.util.Remote.setCredentials("guest", "guest");
				var user = ms123.util.Remote.rpcSync("auth:getUserProperties");
				console.log("user:"+qx.util.Serializer.toJson(user));
				ms123.config.ConfigManager.setUserProperties( user);
				ms123.config.ConfigManager.setLanguage( "de");
			}

			var branding = ms123.util.Remote.rpcSync("namespace:getBranding");
			ms123.util.Remote.setSessionTimeout( branding.sessionTimeout);

			/*var state = qx.bom.History.getInstance().getState();
				console.log("History.state:"+state);
			qx.bom.History.getInstance().addListener("request", function(e) {
				var state = e.getData();
				console.log("Histiry.state:"+state);
				//this.setApplicationState(state);
			}, this);*/

			this.setNamespaceAndPagename();
			var nsList = [];
			nsList.push(this._namespace);
			this.setMessages(nsList, ms123.config.ConfigManager.getLanguage());
			var context={
				facade:this
			}
      var pr = new website.PageRenderer(context, this._namespace,this._pageName);

			// Call super class
			this.base(arguments);

      var root = this.getRoot();
      root.add(pr, {edge: 0});
			root.addListener("resize", pr.resize,pr);
		},

		setNamespaceAndPagename: function () {
			var body = jQuery('#ms123id')[0];
			this._namespace = body.getAttribute("space");
			this._pageName = body.getAttribute("pagename");
			console.log("Namespace:"+this._namespace+"|"+this._pageName);
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
		setMessages: function (_nsList, lang) {
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
