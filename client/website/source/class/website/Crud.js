/**
	@ignore(Hash)
*/
qx.Class.define("website.Crud", {
	extend: qx.ui.container.Composite,

	construct: function (context) {
		this.base(arguments);
console.log("CRUD");
		this._facade = context.facade;
		var layout = new qx.ui.layout.Grow(1);
		this.setLayout(layout);

		var params = context.params;
		var storeDesc = new ms123.StoreDesc({
			namespace: params.namespace
		});
		console.log("storeDesc:" + storeDesc);

/*	
		var __configManager = new ms123.config.ConfigManager();
		context.model = __configManager.getEntityModel("anfrage", storeDesc, "main-grid", "properties");
			context.modelForm = __configManager.getEntityModel("anfrage", storeDesc, "main-form", "properties");
			context.unit_id = ms123.util.IdGen.nextId();
			context.user = ms123.config.ConfigManager.getUserProperties();
			var table = new ms123.widgets.Table(context);
		this.add(table,{edge:"center"});*/


		var modules = new ms123.config.ConfigManager().getEntities(storeDesc);
		var module=null;
		for( var i=0; i< modules.length;i++){
			if( modules[i].name == params.entity){
				module = modules[i];
			}
		}
		if( module == null){
			ms123.form.Dialog.alert("crud.entity("+params.entity+") or namespace("+params.namespace+") unknown" );
			return;
		}
		console.log("module:" + qx.util.Serializer.toJson(module));
		var widgetList = ms123.MainMenu.createWidgetList(module, storeDesc, this);
		console.log("widgetList:" + qx.util.Serializer.toJson(widgetList));
		var context = {
			unit_id: ms123.util.IdGen.nextId(),
			storeDesc: storeDesc,
			config: "composite",
			widgets: widgetList
		};
		var c = new ms123.Crud(context);
		this.add(c, {
			flex:0,
			edge: "center"
		});
  var w = this.getSizeHint();
console.log("CrudContainer1:"+JSON.stringify(w));
	}
});
