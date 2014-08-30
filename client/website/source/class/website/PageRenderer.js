/**
 * @ignore(jQuery.*) 
 * @ignore(_paq.*) 
 */
qx.Class.define("website.PageRenderer", {
	extend: qx.ui.container.Composite,
	construct: function (context, namespace, pagename) {
		this.base(arguments);
		this._facade = context.facade;
		this._namespace = namespace;
		this._pageName = pagename;


		this._cellManagerMap = {};
		var rootShape = this._getWebsiteMain(namespace, pagename);

		var xspacing = rootShape.properties.ws_xspacing;
		var yspacing = rootShape.properties.ws_yspacing;
		var padding = rootShape.properties.ws_padding;

		qx.Class.include(ms123.util.Single, qx.ui.decoration.MBorderRadius);

		if (this._isValidColor(rootShape.properties.ws_backgroundcolor) === true) {
			this.setBackgroundColor(rootShape.properties.ws_backgroundcolor);
		}

		this._backgroundColor2 = null;
		if (this._isValidColor(rootShape.properties.ws_backgroundcolor2)) {
			this._backgroundColor2 = rootShape.properties.ws_backgroundcolor2;
			website.theme.Color.colors.background = this._backgroundColor2;
		}
		this._backgroundSelected = website.theme.Color.colors["background-selected"];
		this._textColorSelected = website.theme.Color.colors["text-selected"];
		this._textColor = website.theme.Color.colors["text"];

		if (this._isValidColor(rootShape.properties.ws_backgroundcolor)) {
			//			website.theme.Color.colors.background = rootShape.properties.ws_backgroundcolor;
			this.setBackgroundColor(rootShape.properties.ws_backgroundcolor);
			this._backgroundColor = website.theme.Color.colors["background"];
		}
		if (this._isValidColor(rootShape.properties.css_textcolor)) {
			website.theme.Color.colors.text = rootShape.properties.css_textcolor;
		}
		if (this._isValidColor(rootShape.properties.css_linkcolor)) {
			website.theme.Color.colors.link = rootShape.properties.css_linkcolor;
		}
		if (this._isValidColor(rootShape.properties.css_headercolor)) {
			website.theme.Color.colors["text-title"] = rootShape.properties.css_headercolor;
		}
		this._initTheme("brown");
		//qx.theme.manager.Color.getInstance().setTheme(website.theme.Color);
		var dock = new qx.ui.layout.Dock(xspacing, yspacing)
		dock.setSort("y");
		this.setLayout(dock);
		this.setPadding(padding);

		var context = {};
		var mainShapes = rootShape.childShapes;
		this._mainShapes = mainShapes;
		this._containerMap={};
		for (var i = 0; i < mainShapes.length; i++) {
			var mainShape = mainShapes[i];
			context.container = this;
			console.log("\n----->mp:" + context.container + "/" + mainShape.properties.ws_position);
			if( mainShape.properties.ws_position == undefined) continue;
			context.layoutMap = {
				edge: mainShape.properties.ws_position
			};
			var containerShape = this._getContainerShape(mainShape);
			if (containerShape){
				 var container = this._renderContainer(context, containerShape);
				this._containerMap[mainShape.properties.ws_position] = container;
			}
		}
		this._trackAction("page","Home");
	},

	/******************************************************************************
	 STATICS
	 ******************************************************************************/
	statics: {
		PADDING: 5,
		SPACINGX: 20,
		SPACINGY: 5
	},
	members: {
		resize:function(){
			var containerMap = this._containerMap;
			var mainShapes = this._mainShapes;
			var root = qx.core.Init.getApplication().getRoot();
			for (var i = 0; i < mainShapes.length; i++) {
				var mainShape = mainShapes[i];
				var display = mainShape.properties.ws_display;
				var position = mainShape.properties.ws_display;
				if( display == undefined || display=="") continue;
				var container = containerMap[position];
				if(container==null)continue;
				var env = ms123.util.Clone.merge({}, root.getBounds());
				var disp = true;
				try{
					disp = this.__maskedEval(display, env);	
				}catch(e){
					console.error("Resize:error in expr("+display+"):"+e);
				}
				container.setVisibility( disp ? "visible" : "excluded");
			}
		},
		_getWebsiteMain: function (namespace, pageName) {
			var rpcParams = {
				namespace: namespace,
				name: pageName
			}

			var params = {
				service: "docbook",
				method: "websiteMain",
				parameter: rpcParams,
				async: false,
				context: this
			}
			try {
				var shape = ms123.util.Remote.rpcAsync(params);
				return shape;
			} catch (e) {
				ms123.form.Dialog.alert("PageRenderer.getWebsiteMain:" + e);
				return;
			}
		},
		_getContainerShape: function (shape) {
			if (shape.childShapes.length == 0) {
				return null;
			}
			console.log("properties:"+JSON.stringify(shape.properties,null,2));
			var child = shape.childShapes[0];
			if (shape.childShapes.length == 1) {
				if (child.stencil.id.toLowerCase() == "container") {
					return child;
				}
			}
			var cshape = {};
			cshape.stencil = {
				id: "container"
			};
			cshape.properties = shape.properties;
			cshape.childShapes = shape.childShapes;
			return cshape;
		},
		_renderContainer: function (context, shape) {
			var stencilId = shape.stencil.id.toLowerCase();
			var properties = shape.properties;

			var parentContainer = context.container;

			var width = this._getWidth(shape.properties.ws_width);
			var height = this._getHeight(shape.properties.ws_height);

			var grid = this._getGridContainer(width, height, shape.properties);
			var childLines = this._getChildsAsListOfLines(shape);
			var scroll = new qx.ui.container.Scroll().set({
				scrollbarX: "off",
				maxWidth: width,
				minWidth: width,
				maxHeight: height,
				minHeight: height
			});
			var bgColor = this._getColor(shape.properties, "ws_backgroundcolor", null);
			console.log("ContainerBG:" + bgColor + "/" + shape.properties.ws_id);
			if (bgColor) {
				scroll.setBackgroundColor(bgColor);
				grid.container.setBackgroundColor(bgColor);
			} else if (this._backgroundColor2) {
				scroll.setBackgroundColor(this._backgroundColor2);
			}

			var edge = shape.properties.ws_position;
			var map = {
				edge: edge,
				flex: (edge == "north" || edge == "south") ? 0 : 1
			}

			scroll.add(grid.container, map)

			var c = grid.container;
			var id = shape.properties.ws_id;
			if (id) {
				parentContainer.addListenerOnce("appear", function () {
					c.getContentElement().getDomElement().id = id;
					console.error("ID:" + id);
				}, this);
			}

			parentContainer.add(scroll, map);
			var id = shape.properties.ws_id;
			for (var rowNr = 0; rowNr < childLines.length; rowNr++) {
				var line = childLines[rowNr];
				var weighty = this._getWeightY(line);
				grid.layout.setRowFlex(rowNr, weighty);
				var colNr = 0;
				for (var j = 0; j < line.length; j++) {
					var child = line[j];
					var colSpan = child.properties.ws_colspan || 1;
					var cellManager = this._createCellManager(context, child);
					grid.layout.setColumnFlex(colNr, 1);
					grid.container.add(cellManager.getCell(), {
						colSpan: colSpan,
						row: rowNr,
						column: colNr
					});
					colNr = colNr + colSpan;
				}
			}
			return scroll;
		},
		_createCellManager: function (context, shape) {
			var cellManager = new website.CellManager({
				pageRenderer: this,
				locationId: shape.properties.ws_id,
				shape: shape
			});

			var lmap = qx.lang.Object.mergeWith({}, context.layoutMap);
			this._cellManagerMap[shape.properties.ws_id] = cellManager;
			return cellManager;
		},
		_installLinkHandler: function (locationId, contentId) {
			var self = this;
			var cid = contentId.replace(/\./,"");
			console.log("_installLinkHandler:" + locationId+"/"+contentId+"/"+cid);
			q('#' + cid + ' a[href!="#"]').forEach(function (item) {
				var href = item.getAttribute("href");
				if( href.indexOf("#")!=-1){
					return;
				}
				var rel = item.getAttribute("rel");
				console.log("\t_installLinkHandler.href:" + href + "/rel:" + rel);
				if (!rel) item.setAttribute("rel", locationId);
				if (!href.match(/http:/) && !href.match(/mailto/)) {
					q(item).on("click", this._linkHandler, this);
				} else {
					//item.setAttribute("target","_blank");
				}
			}, this);

			q('#' + cid + ' a[href^="#"]').forEach(function (item) {
				q(item).on("click", function (e) {
					if (item.getAttribute("class") == "ui-tabs-anchor") return;
					e.preventDefault();
					var href = item.toString().split("#")[1];
					console.log("_linkHandler.target:" + locationId + "/href:" + href + "/contentId:" + contentId);
					var data = {
						parentHref: contentId,
						href: "#" + href,
						target: locationId
					}
					this._sendMessage("itemSelected", data);
				}, this);
			}, this);
		},
		_linkHandler: function (e) {
			console.log("EVENT:",e);
			var href = (e.target || e.srcElement).getAttribute("href");
			if (href.match(/^http/)) {
				this._trackAction("link", href );
				return;
			}
			e.preventDefault();
			var text = (e.target || e.srcElement).innerHTML;
			var rel = (e.target || e.srcElement).getAttribute("rel");
			var target = (e.target || e.srcElement).getAttribute("target") || rel;
			console.log("_linkHandler.href:" + href + "/target:" + target + "/" + rel+"/"+text);
			this._linkAction(href, target,{label:text});
			console.trace();
		},
		_linkAction: function (href, target, properties) {
			var label = properties ? properties.label : null;
			console.log("_linkAction.href:" + href + "/" + label);
			if (href.indexOf("#") != -1) return;
			var locationId = target;
			var contentId = null;
			var resourceId = null;
			var pageName = this._pageName;
			if (href.match(/^http:/)) {
				return null;
			} else if (href.match(/^loc:/)) {
				var x = href.split(":");
				contentId = x[1];
				if (contentId.indexOf(".") != -1) {
					x = contentId.split(".");
					pageName = x[0];
					contentId = x[1];
				}
			} else if (href.match(/^wp:/)) {
				var x = href.split(":");
				contentId = x[1];
				if (contentId.indexOf(".") != -1) {
					x = contentId.split(".");
					//namespace = x[0];//Nicht ausgewertet
					pageName = x[1];
				}
			} else if (href.match(/^res:/)) {
				var x = href.split(":");
				resourceId = x[1];
			} else if (href.match(/^wf:/)) {
				var x = href.split(":");
				var workflow = x[1];
				this._startWorkflow(workflow, locationId,label);
				return null;
			} else if (href.match(/^swf:/)) {
				var x = href.split(":");
				var swf = x[1];
				this._execSwfCmd(swf, locationId,label);
				return null;
			} else if (href.match(/^js:/)) {
				var x = href.split(":");
				var js = x[1];
				var qm = js.indexOf("?");
				var params = {};
				if (qm != -1) {
					var _js = js;
					js = js.substring(0, qm)
					params = this._getQueryParams(_js.substring(qm + 1));
				}
				this._execJavascriptCmd(js, params, locationId, properties);
				return null;
			}

			console.log("pageName:" + pageName);
			console.log("locationId:" + locationId);
			console.log("resourceId:" + resourceId);
			console.log("ContentId:" + contentId);
			console.log("Href:" + href);
			return this._replaceWidgetContent(href, locationId, pageName, contentId, resourceId, properties);
		},
		_startWorkflow: function (name, locationId,label) {
			var cellManager = this._cellManagerMap[locationId];
			var exists = cellManager.selectPage(name);
			if (exists) return;
			var uprops = ms123.config.ConfigManager.getUserProperties();
			var context = {
				userid: uprops.userid,
				parentContainer: cellManager.getCellWidget(name, locationId)
			};
			var pc = new ms123.processexplorer.ProcessController(context);
			var namespace = this._namespace;
			var nameSplit = this._splitExtension(name);
			var x = nameSplit.prefix.split(".");
			if (x.length == 2) {
				namespace = x[0];
				name = x[1] + nameSplit.ext;
			}
			console.log("namespace:" + namespace + "/" + name);
			this._trackAction("workflow",label+"/"+namespace+"."+ name);
			pc.startByName(namespace, name);
		},
		_execSwfCmd: function (name, locationId, label) {
			console.log("_execSwfCmd:" + name + "/" + locationId+"/"+label);
			var cellManager = this._cellManagerMap[locationId];
			var exists = cellManager.selectPage(label);
			if (exists) return;

			if( !name.match(/.swf$/)){
				name+=".swf";
			}
			var ownDecorator = new ms123.util.RoundSingleBorder(2, "solid", "yellow", 5);
			var swfWidget = new qx.ui.embed.Flash("repo%3a"+name, name).set({
				//decorator:ownDecorator,
				liveConnect:false,
				quality:"high",
				loop:false,
				//wmode:"window",
				maxWidth: 816,
				minWidth: 816,
				maxHeight: 590,
				minHeight: 590,
				variables:{
					playerMode:1
				}
			});

    swfWidget.__flashParamHelper("allowFullScreen","true");

			var scroll = new qx.ui.container.Scroll().set({
				//decorator:ownDecorator,
				maxWidth: null,
				minWidth: null,
				maxHeight: null,
				minHeight: null
			});

			scroll.add(swfWidget);

			swfWidget.addListener("loaded", function() {
				var flashFE = swfWidget.getFlashElement();
				var currentFrame = flashFE.CurrentFrame();
				var totalFrames = flashFE.TotalFrames();
				console.log("loaded:"+currentFrame+"/"+totalFrames);
			});
			this._trackAction("swf",label+"/"+name);
			cellManager.replace(scroll, label);
		},
		_execJavascriptCmd: function (name, params, locationId, properties) {
			console.log("_execJavascriptCmd:" + name + "/" + qx.util.Serializer.toJson(params));
			var cellManager = this._cellManagerMap[locationId];
			var exists = cellManager.selectPage(properties ? properties.label :null);
			if (exists) return;
			var context = {
				facade: this._facade,
				params: params
			};
			var obj = this._createObj(name, context);

			var scroll = new qx.ui.container.Scroll().set({
				scrollbarX: "off",
				maxWidth: null,
				minWidth: null,
				maxHeight: null,
				minHeight: null
			});

			scroll.add(obj);

			this._trackAction("javascript",name);
			cellManager.replace(scroll, properties ? properties.label : null);
		},
		_createObj: function (name, context) {
			if (name.indexOf(".") != -1) {
				var parts = name.split("\.");
				var obj = window;
				for (var i = 0; i < parts.length; i++) {
					obj = obj[parts[i]];
				}
				return new obj(context);
			} else {
				return new website[name](context);
			}
		},
		_splitExtension: function (name) {
			var dot = name.indexOf(".");
			if ((name.length - dot) < 4) {
				return {
					prefix: name.substring(0, dot),
					ext: name.substring(dot)
				};
			}
			return {
				prefix: name,
				ext: ""
			};
		},
		__maskedEval: function (scr, env) {
			return (new Function("with(this) { return " + scr + "}")).call(env);
		},
		_replaceWidgetContent: function (href, locationId, pageName, contentId, resourceId, properties) {
			if (!locationId || locationId.length == 0) {
				ms123.form.Dialog.alert("Website.PageRenderer:locationId missing");
				return;
			}
			var wp = href.match(/^wp/);
			console.log("locationId:" + locationId + "/" + resourceId + "/" + contentId);
			var rpcParams = {
				namespace: this._namespace,
				name: pageName,
				shapeId: contentId,
				resourceId: resourceId
			}

			var params = {
				service: "docbook",
				method: wp ? "websitePage" : "websiteFragment",
				parameter: rpcParams,
				async: false,
				context: this
			}
			try {
				var map = ms123.util.Remote.rpcAsync(params);
				var cellManager = this._cellManagerMap[locationId];
				var shape = map;
				if (wp) {
					shape = {
						properties: {
							ws_id: resourceId || contentId,
							ws_html: map.html,
							ws_scrollbar: properties ? properties.scrollbar : true,
							ws_toc: map.toc,
							ws_js: map.js
						},
						stencil: {
							id: 'unknown'
						}
					}
				}
				var action = shape.properties.ws_linktype == "content" ? shape.properties.ws_name : shape.properties.ws_id;
				if( properties){
					this._trackAction("page", properties.label);
				}
				cellManager.replace(shape, properties ? properties.label:null);
				return shape;
			} catch (e) {
				ms123.form.Dialog.alert("Website.replacePart:" + e);
				console.log(e.stack);
				return;
			}
		},
		_getChildsAsListOfLines: function (shape) {
			var lines = new Array();
			var childShapes = shape.childShapes;
			childShapes = childShapes.sortBy(function (element) {
				return element.bounds.upperLeft.y * 10000 + element.bounds.upperLeft.x;
			});
			if (childShapes.length == 0) return lines;
			var row = -1;
			var column = 0;
			var oldY = -1;
			var line = null;
			for (var i = 0; i < childShapes.length; i++) {
				var child = childShapes[i];
				var UL = child.bounds.upperLeft;
				if (oldY != UL.y) {
					row++;
					column = 0;
					line = new Array();
					lines.push(line);
				} else {
					column++;
				}
				oldY = UL.y;
				line.push(child);
			}
			return lines;
		},
		_getLineContainer: function () {
			var layout = new qx.ui.layout.Grid(5, 5);
			//var ownDecorator = new qx.ui.decoration.Uniform(1, "solid", "blue");
			var container = new qx.ui.container.Composite(layout).set({
				//	decorator: ownDecorator
			});
			return {
				container: container,
				layout: layout
			}
		},
		_getGridContainer: function (width, height, properties) {
			var ownDecorator = new ms123.util.RoundSingleBorder(properties.ws_borderwidth, properties.ws_borderstyle, properties.ws_bordercolor, 5);

			var layout = new qx.ui.layout.Grid(5, 5);
			var container = new qx.ui.container.Composite(layout).set({
				decorator: (properties.ws_borderstyle != "none" && properties.ws_borderwidth) ? ownDecorator : null,
				maxWidth: width,
				minWidth: width,
				height: height
				//	minHeight: hmap.minHeight
			});
			container.setPadding(1);
			container.setMargin(0);
			return {
				container: container,
				layout: layout
			}
		},
		_getWidth: function (ws) {
			if (!ws || ws == "auto") {
				return null;
			}
			if (ws.match(/^[0-9]{1,5}px$/) || ws.match(/^[0-9]{1,5}$/)) {
				return parseInt(ws);
			}
			return null;
		},
		_getHeight: function (hs) {
			if (!hs || hs == "auto") {
				return null;
			}
			if (hs.match(/^[0-9]{1,5}px$/) || hs.match(/^[0-9]{1,5}$/)) {
				return parseInt(hs);
			}
			return null;
		},
		_getWeightY: function (line) {
			var max = 0;
			for (var j = 0; j < line.length; j++) {
				var child = line[j];
				max = Math.max(max, child.properties.ws_weighty);
			}
			console.log("Max:" + max);
			return max;
		},
		_isValidColor: function (color) {
			if (color == null || color == "" || color == "none" || color == "null") return false;
			return qx.util.ColorUtil.isValidPropertyValue(color);
		},
		_getColor: function (properties, key, def) {
			var color = properties[key];
			if (!this._isValidColor(color)) return def;
			return color;
		},
		_getQueryParams: function (qs) {
			console.error("_getQueryParams:" + qs);
			var map = {};
			var params = qs.split("&");
			for (var i = 0; i < params.length; i++) {
				var pair = params[i].split("=");
				map[pair[0]] = pair[1];
			}
			return map;
		},
		_sendMessage: function (name, data) {
			var eventBus = qx.event.message.Bus;
			eventBus.getInstance().dispatchByName(name, data);
		},
		_trackAction:function(category,action,name){
			try{
				jQuery(document).attr("title", action);
				console.error("Track:"+category+"/"+action);
				_paq.push(['trackEvent', category, action])
			}catch(e){
				console.error("Track:"+e);
			}
		},
		_initTheme: function (th) {
			if (th == "ms") {
				qx.theme.manager.Meta.getInstance().setTheme(ms123.theme.ms.Theme);
			} else if (th == "brown") {
				qx.theme.manager.Meta.getInstance().setTheme(website.theme.Theme);
			} else if (th == "simple") {
				qx.theme.manager.Meta.getInstance().setTheme(website.theme.Theme);
			} else {
				console.log("Switch:" + th);
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
		}
	}
});
