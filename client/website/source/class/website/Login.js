/**
	@ignore(Hash)
*/
qx.Class.define("website.Login", {
	extend: qx.ui.container.Composite,

	events: {
		"changeLoginData": "qx.event.type.Data"
	},

	construct: function (context) {
		this.base(arguments);
		this._facade = context.facade;
		var layout = new qx.ui.layout.Dock(10);
		this.setLayout(layout);
		this.setWidth(350);
		this.setHeight(200);
		this.setPadding(20);
		this.setAllowGrowX(false);
		this.setAllowGrowY(false);

		this.msg = new qx.ui.basic.Label().set({
			rich:true
		});
		var currUser = new qx.ui.basic.Label().set({
			rich:true
		});
		var user = ms123.config.ConfigManager.getUserProperties();
		currUser.setValue("<h2>User:"+user.userid+"</h2>");
		this.add(currUser,{edge:"north"});
		this.add(this.msg,{edge:"south"});

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
			saveLoginData:true
		};
		var model = qx.data.marshal.Json.createModel(data);

		var form = new qx.ui.form.Form();

		var username = new qx.ui.form.TextField();
		username.setRequired(true);
		form.add(username, "Username", null, "username");

		var password = new qx.ui.form.PasswordField();
		password.setRequired(true);
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

		var saveLoginData = new qx.ui.form.CheckBox();
		form.add(saveLoginData, "SaveLoginData", null, "saveLoginData");

		var controller = new qx.data.controller.Form(null, form);
		var model = controller.createModel();

		var loginbutton = new qx.ui.form.Button("Login");
		form.addButton(loginbutton);
		var self = this;
		loginbutton.addListener("execute", function () {
			if (form.validate()) {
				var username= controller.getModel().getUsername() ? controller.getModel().getUsername() : "";
				var pw=       controller.getModel().getPassword() ? controller.getModel().getPassword() : "";
				var language= controller.getModel().getLanguage() ? controller.getModel().getLanguage() : "de";
				var saveLoginData= controller.getModel().getSaveLoginData() ? controller.getModel().getSaveLoginData() : true;
				var params = {
					url: "checkcredentials/",
					context: this,
					method: "POST",
					data: "credentials="+username+":"+pw,
					async: true,
					completed: function () {
						var loginData = {
							username: username,
							password: pw,
							language: language,
							saveLoginData: saveLoginData
						};
						ms123.util.Remote.setCredentials(loginData.username, loginData.password);
						this.fireDataEvent("changeLoginData", loginData);
						this.msg.setValue("<h2 style='color:blue'>Login ok</h2>");
						var user = ms123.util.Remote.rpcSync("auth:getUserProperties");
						console.log("user:"+qx.util.Serializer.toJson(user));
						ms123.config.ConfigManager.setUserProperties( user);


						var oldlang = ms123.config.ConfigManager.getLanguage();
						if( oldlang != loginData.language){
							ms123.config.ConfigManager.setLanguage( loginData.language);
							self._facade.setMessages( [], loginData.language);
						}

						console.log("saveLoginData:"+loginData.saveLoginData);
						if( loginData.saveLoginData){
							q.cookie.set("sw.website.login", loginData.username+"/"+loginData.password+"/"+loginData.language);
						}
					},
					failed: function () {
						if( language == "de" ){
							this.msg.setValue("<div style='color:red'>Unbekannter Username oder Passwort falsch</div>");
						}else{
							this.msg.setValue("<div style='color:red'>Unknown Username or Password wrong</div>");
						}
					}
				}
				ms123.util.Remote.send(params);
			}
		}, this);

		var renderer = new qx.ui.form.renderer.Single(form);
		this.add(renderer, {
			edge: "west"
		});

		var app = qx.core.Init.getApplication();
		var a = app.toString();
		var appid = a.substring(0, a.indexOf("."));

		var am = qx.util.AliasManager.getInstance();
	}
});
