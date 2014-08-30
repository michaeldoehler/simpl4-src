/**
 */
qx.Class.define('website.BaseContentWidget', {
 extend: qx.ui.container.Composite,
	implement: website.IContentWidget,


	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (context) {
		this.base(arguments);
		this.setLayout(new qx.ui.layout.Dock());
	},


	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},


	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		_setDecorator:function( properties){
			var ownDecorator = new ms123.util.RoundSingleBorder(properties.ws_borderwidth, properties.ws_borderstyle, properties.ws_bordercolor, 5);
			var decorator = (properties.ws_borderstyle != "none" && properties.ws_borderwidth) ? ownDecorator : null;
			this.setDecorator( decorator);
		},
		destruct:function(){
		}
	}
});
