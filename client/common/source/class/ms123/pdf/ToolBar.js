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
 * @ignore(PDFJS.*)
 * @ignore(Promise*)
 * @lint ignoreDeprecated(alert,eval)
 * @ignore(alert*)
 */
qx.Class.define("ms123.pdf.ToolBar", {
	extend: qx.ui.container.Composite,

	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (pdfView) {
		this.base(arguments);
		this.pdfView = pdfView;
		this.homeScale = "page-fit";
		this.setLayout(new qx.ui.layout.Canvas());
		var toolbar = new qx.ui.toolbar.ToolBar();
		var homeButton = this._createButton("", "resource/openseadragon/home_rest.png");
		homeButton.addListener("execute", function () {
			pdfView.setScale(this.homeScale, true);
		}, this);
		var zoominButton = this._createButton("", "resource/openseadragon/zoomin_rest.png");
		zoominButton.addListener("execute", function () {
			pdfView.zoomIn();
		}, this);
		var zoomoutButton = this._createButton("", "resource/openseadragon/zoomout_rest.png");
		zoomoutButton.addListener("execute", function () {
			pdfView.zoomOut();
		}, this);

		toolbar.add(zoominButton);
		toolbar.add(zoomoutButton);
		toolbar.add(homeButton);
		toolbar.setBackgroundColor(null);
		toolbar.setDecorator(null);
		this.add(toolbar, {
			top: 0,
			left: 0
		});

		/*var label = new qx.ui.basic.Label("Input label");
		this.label = label;
		this.add(label, {
			top: 0,
			left: 100
		});*/
		//pdfView.addListener("mousemove", this.hover.bind(this));
	},

	/******************************************************************************
	 STATICS
	 ******************************************************************************/
	statics: {},

	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},

	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		hover: function (evt) {
			var box = this.getContentLocation("box");
			var mouseX = evt.getDocumentLeft() - box.left
			var mouseY = evt.getDocumentTop() - box.top
			var n = evt._native;
			this.label.setValue("XY:" + this.pdfView.contentElement.scrollLeft + "/" + this.pdfView.contentElement.scrollTop + "|" + mouseX + "/" + mouseY);
		},
		_createButton: function (text, icon) {
			var b = new qx.ui.toolbar.Button(null, icon);
			b.setPaddingTop(0);
			b.setPaddingBottom(0);
			b.setPaddingRight(0);
			b.setDecorator(null);
			b.setBackgroundColor(null);
			return b;
		},
		setScale: function (scale) {
			if (scale == null) {
				this.homeScale = 'page-fit';
			} else {
				this.homeScale = scale;
			}
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
