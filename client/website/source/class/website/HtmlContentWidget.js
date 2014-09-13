/**
 * @lint ignoreDeprecated(alert,eval) 
 * @ignore(jQuery.*) 
 */
qx.Class.define('website.HtmlContentWidget', {
	extend: website.BaseContentWidget,


	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (context) {
		this.base(arguments, context);
		this._context = context;
		this._locationId = context.locationId;
		this._createWidget(context);
		var eventBus = qx.event.message.Bus;
		var contentId = context.shape.properties.ws_id;
		this._contentId = contentId;
		eventBus.subscribe("itemSelected", this._itemSelected, this);
		this.addListenerOnce("appear", function () {
			var c = this.getContentElement().getDomElement();
			c.id = this._locationId.replace(/\./, "");
		}, this);
	},


	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},


	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		_itemSelected: function (msg) {
			var data = msg.getData();
			var parentHref = data.parentHref;
			console.log("HtmlContentWidget.itemSelected.contentId:" + this._contentId + "/p:" + parentHref);
			if (parentHref) {
				var contentId = (parentHref.indexOf(":") != -1) ? parentHref.split(":")[1] : parentHref;
				console.log("\t_contentId:" + contentId + "/contentId:" + this._contentId);
				if (contentId == this._contentId) {
					if (this._context.cellManager.isSelected(this._contentId)) {
						this.scrollTo(data.href);
					} else {
						var page = this._context.cellManager.getPage(this._contentId);
						if (page) {
							page.addListenerOnce("appear", function () {
								this.scrollTo(data.href);
							}, this);
							this._context.cellManager.selectPage(this._contentId);
						}
					}
				}
			}
		},
		_createWidget: function (context) {
			var pageRenderer = context.pageRenderer;
			var properties = context.shape.properties;
			var width = pageRenderer._getWidth(properties.ws_width);
			var height = pageRenderer._getHeight(properties.ws_height);
			this._setDecorator(properties);
			var label = null;
			if (properties.ws_title && properties.ws_title.length > 0) {
				label = new qx.ui.basic.Label(properties.ws_title);
				label.setRich(true);
				this.add(label, {
					edge: "north"
				});
			}
			this._createHtmlWidget(context.shape);

			var minHeight = properties.ws_minheight === true ? height : null;
			this.setMinHeight(minHeight);
			this.setMaxHeight(height);
			this.setMinWidth(width);
			this.setMaxWidth(width);
			if (properties.ws_aligny) this.setAlignY(properties.ws_aligny);
			if (properties.ws_alignx) this.setAlignX(properties.ws_alignx);
			var bgColor = pageRenderer._getColor(properties, "ws_backgroundcolor", null);
			if (bgColor) {
				this.setBackgroundColor(bgColor);
			} else if (pageRenderer._backgroundColor2) {
				//this.setBackgroundColor(pageRenderer._backgroundColor2);
			}
		},
		_createHtmlWidget: function (shape) {
			var scrollbar = this._context.shape.properties.ws_scrollbar;
			var overflowY = scrollbar ? "auto" : "hidden";
			var overflowX = scrollbar ? "auto" : "auto";
			console.log("overflowY:" + overflowY + "/" + scrollbar);
			var widget = new qx.ui.embed.Html(shape.properties.ws_html).set({
				overflowY: overflowY,
				overflowX: overflowX
			});
			this.add(widget, {
				edge: "center"
			});
			this._mainWidget = widget;
			widget.addListenerOnce("appear", function () {
				this.evalJS(shape);
				var c = widget.getContentElement().getDomElement();
				c.id = this._contentId.replace(/\./, "");
				var area = q("#" + this._contentId.replace(/\./, ""));
				area.hide();
				this._mainWidget.getContentElement().getDomElement().scrollTop = 0;
				area.fadeIn();
				this._context.pageRenderer._installLinkHandler(this._locationId, this._contentId);
			}, this);
		},
		destruct: function () {
			console.log("destruct:" + this);
			var eventBus = qx.event.message.Bus;
			eventBus.unsubscribe("itemSelected", this._itemSelected, this);
		},
		evalJS: function (shape) {
			if (shape.properties.ws_js) {
				if (Array.isArray(shape.properties.ws_js)) {
					for (var i = 0; i < shape.properties.ws_js.length; i++) {
						var js = shape.properties.ws_js[i];
						console.log("js:"+js);
						try{
							eval(js);
						}catch(e){
							console.error("Eval:"+e);
							console.log(e.stack);
						}
					}
				} else {
					eval(shape.properties.ws_js);
				}
			}
		},
		scrollTo: function (href) {
			if (!this._mainWidget.getContentElement().getDomElement()) {
				console.log("scrollTo:no dom");
				return;
			}
			var to = href ? jQuery(href) : 0;
			console.log("_scrollTo:" + href);
			jQuery(this._mainWidget.getContentElement().getDomElement()).scrollTo(to, 1000, {
				easing: 'circ'
			});
		}
	}
});
