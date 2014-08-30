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
/*
*/
qx.Class.define("ms123.LoginWindow", {
	extend: qx.ui.window.Window,

	events: {
		"changeLoginData": "qx.event.type.Data"
	},

	construct: function () {
		//this.base(arguments, "Login", "icon/48/actions/go-home.png");
		this.base(arguments, "Login", "");
		var layout = new qx.ui.layout.Dock();
		this.setLayout(layout);
		this.setModal(true);
		this.setWidth(250);
		this.setHeight(350);

		var data = {
			firstname: "Manfred",
			lastname: "Sattler",
			language: [{
				label: "de",
				data: "de"
			},
			{
				label: "en",
				data: "en"
			}],
			theme: [ 
			{
				label: "brown",
				data: "brown"
			}, {
				label: "blue",
				data: "simple"
			}, {
				label: "red",
				data: "ms"
			},
			{
				label: "orange",
				data: "ea"
			}]
		};
		var model = qx.data.marshal.Json.createModel(data);

		var form = new qx.ui.form.Form();

		var username = new qx.ui.form.TextField();
		username.setLiveUpdate(true);
		//	username.setRequired(true);
		form.add(username, "Username", null, "username");

		var password = new qx.ui.form.PasswordField();
		password.setLiveUpdate(true);
		//	password.setRequired(true);
		form.add(password, "Password", null, "password");

		var language = new qx.ui.form.SelectBox();
		var languageController = new qx.data.controller.List(null, language);
		languageController.setDelegate({
			bindItem: function (controller, item, index) {
				controller.bindProperty("label", "label", null, item, index);
				controller.bindProperty("data", "model", null, item, index);
			}
		});
		languageController.setModel(model.getLanguage());
		form.add(language, "Language");

		var theme = new qx.ui.form.SelectBox();
		var themeController = new qx.data.controller.List(null, theme);
		themeController.setDelegate({
			bindItem: function (controller, item, index) {
				controller.bindProperty("label", "label", null, item, index);
				controller.bindProperty("data", "model", null, item, index);
			}
		});
		themeController.setModel(model.getTheme());
		form.add(theme, "Theme");

		var controller = new qx.data.controller.Form(null, form);
		var model = controller.createModel();

		var loginbutton = new qx.ui.form.Button("Login");
		form.addButton(loginbutton);
		loginbutton.addListener("execute", function () {
			this._handleLogin(form,controller);
		}, this);


		this.addListener("keypress", (function (e) {
			var iden = e.getKeyIdentifier();
			if (iden == "Enter") {
				this._handleLogin(form,controller);
			}
		}).bind(this));

		// add a reset button
		var cancelbutton = new qx.ui.form.Button("Cancel");
		form.addButton(cancelbutton);
		cancelbutton.addListener("execute", function () {
			this.close();
		}, this);

		var renderer = new qx.ui.form.renderer.Single(form);
		this.add(renderer, {
			edge: "north"
		});

		var app = qx.core.Init.getApplication();
		var a = app.toString();
		var appid = a.substring(0, a.indexOf("."));

		var am = qx.util.AliasManager.getInstance();
		//var file = am.resolve("resource/ms123/simple_workflow.png");
		//var cm = ms123.config.ConfigManager;
		//var file = am.resolve("resource/ms123/"+cm.getLoginImage());
		var file = am.resolve("resource/ms123/login_jro.png");
		var file = "global_data/repo%3alogin_logo.svg";
		var image = new qx.ui.basic.Image(file);
		this.add(image, {
			edge: "center"
		});
	},
	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		_handleLogin:function(form,controller){
			if (form.validate()) {
				var username= controller.getModel().getUsername() ? controller.getModel().getUsername() : "admin";
				var pw=       controller.getModel().getPassword() ? controller.getModel().getPassword() : "admin";
				var language= controller.getModel().getLanguage() ? controller.getModel().getLanguage() : "de";
				if( pw == null || pw==""){ pw = "admin"; }
				var params = {
					url: "checkcredentials/",
					context: this,
					method: "POST",
					data: "credentials="+username+":"+pw,
					async: true,
					completed: function (data) {
						var loginData = {
							username: username,
							password: pw,
							language: language,
							theme: controller.getModel().getTheme()
						};
						ms123.util.Remote.setCredentials(loginData.username, loginData.password);
						this.fireDataEvent("changeLoginData", loginData);
						this.close();
					},
					failed: function () {
						if( language == "de" ){
							ms123.form.Dialog.alert("Unbekannter Username oder Passwort falsch");
						}else{
							ms123.form.Dialog.alert("Unknown Username or Password wrong");
						}
					}
				}
				ms123.util.Remote.send(params);
			}
		}
	}
});
