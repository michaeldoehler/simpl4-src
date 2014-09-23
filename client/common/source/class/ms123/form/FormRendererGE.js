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
/**
 * Form renderer renderer for {@link qx.ui.form.Form}. 
 */
qx.Class.define("ms123.form.FormRendererGE", {
	extend: qx.ui.container.Scroll,
	construct: function (form, layoutDescription, useScroll, inWindow, formData) {
		this.base(arguments);
		this._form = form;

		var mainContainer = new qx.ui.container.Composite();
		mainContainer.setLayout(new qx.ui.layout.Dock());
		this.mainContainer = mainContainer;

		var formContainer = new qx.ui.container.Composite();
		formContainer.setAllowGrowX(true);
		formContainer.setLayout(new qx.ui.layout.VBox(ms123.form.FormRendererGE.SPACINGY+5));
		var	lineContainer = new qx.ui.container.Composite(new qx.ui.layout.HBox(ms123.form.FormRendererGE.SPACINGX));
		formContainer.add(lineContainer/*,{flex:1}*/);

		mainContainer.add(formContainer, {
			edge: "center"
		});
		this.add(mainContainer);
//		formContainer.setBackgroundColor("white");
		this._createFormWidgetPart(formContainer, false, layoutDescription);
		var w = formContainer.getSizeHint().width;
		formContainer.setMinWidth(w + 30);
		formContainer.setPadding(ms123.form.FormRendererGE.PADDING);
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
		setReadonly: function (readonly) {},
		_createFormWidgetPart: function (container, lineBreak, elementDesc) {
			var stencilId = elementDesc.stencil.id.toLowerCase();
			var properties = elementDesc.properties;
			console.log("properties:"+qx.util.Serializer.toJson(properties));
			console.log("._createFormWidgetPart:" + stencilId + "/" + properties.xf_id + "\t/" + container + "=" + lineBreak);
			var newContainer = container;
			switch (stencilId) {
			case "xform":
				break;
			case "group":
				var newContainer = new qx.ui.groupbox.GroupBox();
				var place = elementDesc.properties.xf_place;
				if (place == "normal") {
					newContainer.setLayout(new qx.ui.layout.VBox(ms123.form.FormRendererGE.SPACINGY));
					var lineContainer = new qx.ui.container.Composite(new qx.ui.layout.HBox(ms123.form.FormRendererGE.SPACINGX));
					newContainer.add(lineContainer);

					this._getCurrentLineContainer(container).add(newContainer, { flex:1});
					if( lineBreak){
						var lineContainer = new qx.ui.container.Composite(new qx.ui.layout.HBox(ms123.form.FormRendererGE.SPACINGX));
						container.add(lineContainer);
					}
					newContainer.setContentPadding(ms123.form.FormRendererGE.PADDING);
				} else {
					var layout = new qx.ui.layout.VBox(0);
					newContainer.setLayout(layout);
					//newContainer.setAppearance("");
					newContainer.setContentPadding(2);
					var lineContainer = new qx.ui.container.Composite(new qx.ui.layout.HBox(ms123.form.FormRendererGE.SPACINGX,"center"));
					newContainer.add(lineContainer);
					this.mainContainer.add(newContainer, {
						edge: place
					});
				}
				break;
			case "tabview":
				var tabView = new qx.ui.tabview.TabView().set({
					contentPadding: 10
				});

				var flex = properties.xf_flex ? properties.xf_flex : 1;
				this._getCurrentLineContainer(container).add(tabView ,{ flex:flex});
				if( lineBreak){
					var lineContainer = new qx.ui.container.Composite(new qx.ui.layout.HBox(ms123.form.FormRendererGE.SPACINGX));
					container.add(lineContainer);
				}
				newContainer = tabView;
				break;
			case "page":
				var page = new qx.ui.tabview.Page(properties.xf_id).set({
					showCloseButton: false
				});
				page.setLayout(new qx.ui.layout.VBox(ms123.form.FormRendererGE.SPACINGY));
				var lineContainer = new qx.ui.container.Composite(new qx.ui.layout.HBox(ms123.form.FormRendererGE.SPACINGX));
				page.add(lineContainer/*,{flex:1}*/);
				container.add(page, {});
				newContainer = page;
				break;
			case "label":
				var text = properties.xf_text;
				if( text.match(/^@/)){
					text = this.tr(text.substring(1));
				}				
				if (container instanceof qx.ui.groupbox.GroupBox) {
					container.setLegend(text);
				}
				if (container instanceof qx.ui.tabview.Page) {
					var l = new qx.ui.basic.Label(text);
					this._getCurrentLineContainer(container).add(l,{ });
					if( lineBreak){
						var lineContainer = new qx.ui.container.Composite(new qx.ui.layout.HBox(ms123.form.FormRendererGE.SPACINGX));
						container.add(lineContainer);
					}
				}
				break;
			case "tableselect":
			case "gridinput":
			case "enumselect":
			case "selector":
			case "moduleselector":
			case "actionbutton":
			case "input":
			case "alert":
			case "textarea":
				var formElement = this._form.getFormElement(elementDesc.resourceId);
console.log("stencilId:"+stencilId+"/"+formElement);
				if (formElement) {
					var fer = new ms123.form.FormElementRendererGE(formElement, elementDesc);
						var flex = properties.xf_flex ? properties.xf_flex : 1;
					this._getCurrentLineContainer(container).add(fer, {flex:flex});
					if( lineBreak){
						var lineContainer = new qx.ui.container.Composite(new qx.ui.layout.HBox(ms123.form.FormRendererGE.SPACINGX));
						container.add(lineContainer);
						//container.add(lineContainer,{flex:1});
					}
				} else {
					console.warn("No formElement:" + properties.xf_id + "/" + elementDesc.resourceId);
				}
				if (stencilId == "tableselect") {
					formElement.setDecorator(new qx.ui.decoration.Decorator(1, 'solid', 'gray'));
				}

				return;
			}

			var childs = elementDesc.childShapes;
			childs = childs.sortBy(function (element) {
				return element.bounds.upperLeft.y * 10000 + element.bounds.upperLeft.x;
			});

			for (var i = 0; i < childs.length; i++) {
				var child = childs[i];
				var UL = child.bounds.upperLeft;
				var lineBreak = false;
				if ((i + 1) < childs.length) {
					var nextUL = childs[i + 1].bounds.upperLeft;
					if (UL.y != nextUL.y) {
						lineBreak = true;
					}
				}
				this._createFormWidgetPart(newContainer, lineBreak, child);
			}
		},
		_getCurrentLineContainer:function(p){
			var lc = p.getChildren();
			return lc[lc.length-1];
		}
	}
});
