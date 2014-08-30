/**
 * @lint ignoreDeprecated(alert,eval) 
 */
qx.Class.define('website.CellManager', {
	extend: qx.ui.container.Composite,


	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (context) {
		this.base(arguments);
		var grow = new qx.ui.layout.Grow()
		this.setLayout(grow);
		this._locationId = context.locationId;
		this._pageRenderer = context.pageRenderer;
		this.addListenerOnce("appear", function () {
			//			this.getContentElement().getDomElement().id = this._locationId;
			console.error("XID:" + this._locationId);
		}, this);
		
		this._useTabs = this._getBoolean(context.shape.properties, "ws_usetabs");
		var padding = this._getPadding(context.shape.properties, "ws_padding");
console.log("padding:"+JSON.stringify(padding));
		if (this._useTabs) {
			var cellTab = new qx.ui.tabview.TabView().set({
				contentPadding: padding
			});
			cellTab.setDecorator(null);
			cellTab.getChildControl("pane").setDecorator(null);
			this._cellTabview = cellTab;
			this.add(this._cellTabview);
		}
		this._pages = {};
		this._bgColor = this._pageRenderer._getColor(context.shape.properties, "ws_backgroundcolor", null);
		if(this._bgColor){
			this.setBackgroundColor(this._bgColor);
		}
		this._create(context.shape);
	},


	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},
	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		_create: function (shape) {
			var stencilId = shape.stencil.id.toLowerCase();
			var properties = shape.properties;
			var contentWidget = null;
			if (stencilId == "menu" && properties.ws_menutype == "tree") {
				contentWidget = new website.TreeContentWidget({
					pageRenderer: this._pageRenderer,
					locationId: this._locationId,
					shape: shape
				});
			} else {
				contentWidget = new website.HtmlContentWidget({
					pageRenderer: this._pageRenderer,
					locationId: this._locationId,
					cellManager: this,
					shape: shape
				});
			}
			var label = this._getLabel(shape);
			this._addWidget(contentWidget, label, shape.properties);
		},
		replace: function (content,label) {
			this._removeWidget();
			if (content instanceof qx.ui.core.Widget) {
				var widget = content;
				widget.addListenerOnce("appear", function () {
console.error("appearx");
					var c = widget.getContentElement().getDomElement();
					c.id = this._locationId;
				}, this);
				if (this._pages[label]) {
					this.selectPage(label);
					return;
				}
				this._addWidget(widget, label);
			} else {
				var shape = content;
				var label = this._getLabel(shape);
				if (this._pages[label]) {
					this.selectPage(label);
					return;
				}
				this._create(shape);
			}
		},
		_removeWidget: function () {
			if (this._useTabs) {
			} else {
				if( this._widget && this._widget.destruct){
					 this._widget.destruct();
				}
				this.removeAll();
			}
		},
		_addWidget: function (w, id,properties) {
			if (this._useTabs) {
				var page = this._createPage(id);
				var bgOk=false;
				if( properties){
					var bgColor = this._pageRenderer._getColor(properties, "ws_backgroundcolor", null);
					console.log("bgColor:"+bgColor);
					if (bgColor) {
						page.setBackgroundColor(bgColor);
						bgOk=true;
					}
				}
				console.log("bgOk:"+bgOk+"/"+this._bgColor);
				if(!bgOk && this._bgColor){
						page.setBackgroundColor(this._bgColor);
				}
				page.setUserData("widget",w);
				page.add(w);
			} else {
				this._widget = w;
				this.add(w);
			}
		},
		getCell: function () {
			return this;
		},
		getCellWidget: function (id, locationId) {
			if (this._useTabs) {
				return this._createPage(id);
			} else {
				return this;
			}
		},
		isSelected: function (id) {
			if (!this._useTabs) {
				return true;
			}
			var sel = this._cellTabview.getSelection();
			if (this._pages[id] == sel[0]) {
				return true;
			}
			return false;
		},
		getPage: function (id) {
			if (!this._useTabs) {
				return this;
			}
			return this._pages[id];
		},
		selectPage: function (id) {
			if (this._pages[id]) {
				this._cellTabview.setSelection([this._pages[id]]);
//				this._pages[id].getUserData("widget").scrollTo(null);
				return true;
			}
			return false;
		},
		_createPage: function (id) {
			var page = new qx.ui.tabview.Page(this.tr(id)).set({
				showCloseButton: true
			});
			page.setDecorator(null);
			page.setUserData("id",id);
			page.setLayout(new qx.ui.layout.Grow());
			this._cellTabview.add(page, {
				edge: 0
			});
			this._cellTabview.setSelection([page]);
			var data = {
				locationId: this._locationId,
				contentId: id
			}
			this._sendMessage("pageCreated", this._locationId, id);
			console.error("pageCreated:"+id);
			this._pages[id] = page;
			page.addListener("close", function (e) {
				var id = page.getUserData("id");
				var w =  this._pages[id].getUserData("widget");
				if( w && w.destruct){
					w.destruct();
				}
				this._pages[id]=null;
				delete this._pages[id];
			}, this);
			return page
		},
		_getLabel:function(shape){
			var properties = shape.properties;
			var label = properties.ws_linktype == "content" ? properties.ws_name : shape.properties.ws_id;
			return label;
		},
		_sendMessage: function (name, locationId, id) {
			var eventBus = qx.event.message.Bus;
			eventBus.getInstance().dispatchByName(name, {
				locationId: locationId,
				contentId: id
			});
		},
		_toInt:function(s){
			try{
				var i = parseInt(s);
				if( isNaN(i)) return 0;
				return i;
			}catch(e){
				return 0;
			}
		},
		_getPadding: function (properties, key) {
			var padding = properties[key];
			if( padding == null || padding == "")return 0;
			if( padding.indexOf(",")){
				var a = padding.split(",");
				var ra = [];
				for( var i=0; i < a.length;i++){
					ra.push(this._toInt(a[i]));
				}
				return ra;
			}else{
				return this._toInt(padding);	
			}
		},
		_getBoolean: function (properties, key) {
			var bool = properties[key];
			return bool === true;
		}
	}
});
