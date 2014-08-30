/** 
 * @ignore(jQuery) 
 */
qx.Class.define('website.TreeContentWidget', {
	extend: website.BaseContentWidget,


	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (context) {
		this.base(arguments, context);
		this._context = context;
		this._createWidget(context, context.shape);
	},


	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},

	/******************************************************************************
	 EVENTS
	 ******************************************************************************/
	events: {
		"itemSelected": "qx.event.type.Data"
	},

	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		_createWidget: function (context, shape) {
			var pageRenderer = context.pageRenderer;
			var properties = shape.properties;
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
			var bgColor = pageRenderer._getColor(properties, "ws_backgroundcolor", null);
			if (bgColor) {
				var colorMgr = qx.theme.manager.Color.getInstance();
				var colors = {
					"background-tree": bgColor
				};
				colors = qx.lang.Object.mergeWith(website.theme.Color.colors, colors, true);
				colorMgr.setTheme({
					colors: colors
				});
			}

			var ws_tree = properties.ws_tree;
			if (!ws_tree) {
				ws_tree = "{}";
				ms123.form.Dialog.alert("Menu not type \"tree\"");
			}
			var treeModel = qx.lang.Json.parse(ws_tree);
			var minHeight = properties.ws_minheight === true ? height : null;
			var vTree = new qx.ui.tree.VirtualTree(null, "title", "children").set({
				decorator: null,
				focusable: false,
				hideRoot: true,
				keepFocus: true,
				openMode: "none",
				height: null,
				itemHeight: 20,
				selectionMode: "one",
				contentPaddingLeft: 0,
				showTopLevelOpenCloseIcons: true,
				quickSelection: false
			});
			this._mainWidget = vTree;
			if (label) {
				label.setBuddy(vTree);
			}
			vTree.setIconPath("label");
			vTree.setIconOptions({
				converter: (function (value, model, source, target) {
					var type = model.getType();
					if (type == ms123.shell.Config.DIRECTORY_FT) {
						var isOpen = target.isOpen();
						if (isOpen) {
							return "resource/ms123/directory_open.png";
						} else {
							return "resource/ms123/directory.png";
						}
					} else {
						if (model.getIcon) {
							return model.getIcon();
						} else {
							return "resource/ms123/file.png";
						}
					}
				}).bind(this)
			});
			vTree.addListener("click", this._onClickTree.bind(this), this);
			vTree.addListener("open", this._onOpen.bind(this), this);
			var delegate = {
				configureItem: function (item) {
					item.setIndent(13);
				},
				createItem: function () {
					return new website.TreeItem();
				},
				bindItem: (function (controller, item, id) {
					controller.bindProperty("", "model", null, item, id);
					controller.bindProperty(controller.getLabelPath(), "label", controller.getLabelOptions(), item, id);
					controller.bindProperty(controller.getIconPath(), "icon", controller.getIconOptions(), item, id);
				}).bind(this)
			};

			vTree.setDelegate(delegate);
			this._prepareTreeModel(treeModel);

			var model = qx.data.marshal.Json.createModel(treeModel, true);
			vTree.setModel(model);
			this.add(vTree, {
				edge: "center"
			});
			this.setMinHeight(minHeight);
			this.setMaxHeight(height);
			this.setMinWidth(width);
			this.setMaxWidth(width);
			var bgColor = pageRenderer._getColor(properties, "ws_backgroundcolor", null);
			if (bgColor) {
				this.setBackgroundColor(bgColor);
				vTree.setBackgroundColor(bgColor);
				website.theme.Color.colors["background-tree"] = bgColor;
			}
		},
		_prepareTreeModel: function (model, level) {
			if (!model.children) {
				model.children = [];
			}
			if (model.href && model.href.match(/wp:/)) {
				if (model.children.length == 0) {
					model.children.push({
						id: "dummy",
						value: "dummy",
						label: "dummy"
					});
				}
			}
			if (!model.type) {
				if (model.children.length > 0) {
					model.type = ms123.shell.Config.DIRECTORY_FT;
				} else {
					model.icon = model.icon || "resource/ms123/file.png";
					model.type = "";
				}
			}
			model.title = model.title || model.label || "X";
			for (var i = 0; model.children && i < model.children.length; i++) {
				var c = model.children[i];
				this._prepareTreeModel(c, level + 1);
			}
		},
		_onOpen: function (e) {
			var model = e.getData();
			var childs = model.getChildren();
			if (childs.getLength() == 1) {
				var c = childs.getItem(0);
				if (c.getLabel() == "dummy") {
					this.__onClickTree(model, null);
				}
			}
		},
		_onClickTree: function (e) {
			var treeItem = e.getTarget();
			if (treeItem instanceof website.TreeItem) {
				var model = treeItem.getModel()
				this.__onClickTree(model, treeItem);
			}
		},
		__onClickTree: function (model, treeItem) {
			var parentHref = model.getParentHref ? model.getParentHref() : null;
			console.log("Href:" + model.getHref() + "/" + model.getTarget());
			var data = {
				parentHref: parentHref,
				href: model.getHref(),
				label: model.getLabel(),
				target: model.getTarget()
			}
			if (model.getHref().match(/^#/)) {
				var to = jQuery(model.getHref());
				if (to.length > 0) {
					console.error("Existenter Subknoten selectiert");
					this._sendMessage("itemSelected", data);
					this._context.pageRenderer._trackAction("tree", model.getLabel());
				} else {
					console.error("Nicht existenter Subknoten selectiert");
					this._context.pageRenderer._linkAction(model.getParentHref(), model.getTarget(),null);
					var self = this;
					var timer = setInterval(function () {
						if (jQuery(model.getHref()).length > 0) {
							self._sendMessage("itemSelected", data);
							clearInterval(timer);
						}
					}, 50);
				}
				return;
			} else {
				console.error("Hauptknoten selectiert");
				var ret = this._context.pageRenderer._linkAction(model.getHref(), model.getTarget(), 
						{ label:model.getLabel(), scrollbar:model.getScrollbar ? model.getScrollbar() : true});
				if (ret && ret.properties && ret.properties.ws_toc) {
					var m = this._createSubTreeModel(ret.properties.ws_toc);
					this._prepareSubTreeModel(m, model.getHref(), model.getTarget());
					if(!m) return;
					var subModel = qx.data.marshal.Json.createModel(m, true);
					model.getChildren().removeAll();
					model.getChildren().append(subModel.getChildren());
					this._mainWidget.openNodeAndParents(model);
				}
			}
			data.parentHref=data.href;
			data.href=null;
console.log("_sendMessage.Data:"+JSON.stringify(data,null,2));
			this._sendMessage("itemSelected", data);
		},
		_prepareSubTreeModel: function (model, pHref, target) {
			if( !model ) return;
			delete model.parent;
			model.target = target;
			model.parentHref = pHref;
			for (var i = 0; i < model.children.length; i++) {
				var c = model.children[i];
				this._prepareSubTreeModel(c, pHref, target);
			}
		},
		_createSubTreeModel: function (toc) {
			var currentLevel = -1;
			var currentNode = null;
			var rootNode = null;
			for (var i = 0; i < toc.length; i++) {
				var headerMap = toc[i];
				var headerLevel = headerMap.level;
				if (currentLevel < headerLevel) {
					while (currentLevel < headerLevel) {
						if (currentLevel < (headerLevel - 1) || currentNode == null) {
							currentNode = this._addNode(currentNode, null);
						}
						currentLevel++;
					}
				} else {
					while (currentLevel > headerLevel) {
						currentNode = currentNode.parent;
						currentLevel--;
					}
					if (currentNode) currentNode = currentNode.parent;
				}
				currentNode = this._addNode(currentNode, headerMap);
			}
			while (currentNode != null) {
				rootNode = currentNode;
				currentNode = currentNode.parent;
			}
			return rootNode;
		},
		_addNode: function (currentNode, headerMap) {
			var node = {
				title: "X",
				href: "X",
				type: "X",
				value: "X",
				parent: currentNode,
				children: []
			}
			if (headerMap) {
				node.title = headerMap.label || 'X';
				node.href = "#" + headerMap.id;
				node.value = headerMap.id || 'X';
				node.icon = headerMap.icon || "resource/ms123/file.png";
				if (node.icon) node.icon = node.icon.replace(":", "%3A");
			}
			node.label = node.title;
			if (currentNode) {
				currentNode.children.push(node);
			}
			return node;
		},
		_sendMessage: function (name, data) {
			var eventBus = qx.event.message.Bus;
			eventBus.getInstance().dispatchByName(name, data);
		}
	}
});
