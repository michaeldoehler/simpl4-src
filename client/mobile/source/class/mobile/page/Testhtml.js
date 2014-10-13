/* ************************************************************************

   Copyright:

   License:

   Authors:

************************************************************************ */

/**
 */
qx.Class.define("mobile.page.Testhtml", {
	extend: qx.ui.mobile.page.NavigationPage,

	construct: function () {
		this.base(arguments);
		this.setTitle("Overview");
		this.setShowBackButton(true);
		this.setBackButtonText("Back");
	},


	members: {
		// overridden
		_initialize: function () {
			this.base(arguments);

			this.getContent().add(new qx.ui.mobile.basic.Label("Your first app."));

			var e = new qx.ui.mobile.embed.Html(this._getHtml("s4", "index.html"));
			this.getContent().add(e);
		},

		_getHtml: function (namespace, name) {
			ms123.util.Remote._username = "admin";
			ms123.util.Remote._password = "admin";
			return ms123.util.Remote.rpcSync("docbook:getHtml", {
				namespace: namespace,
				name: name
			});
		},

		// overridden
		_back: function () {
			qx.core.Init.getApplication().getRouting().back();
		}
	}
});
