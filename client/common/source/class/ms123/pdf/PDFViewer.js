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
 */
qx.Class.define('ms123.pdf.PDFViewer', {
	extend: qx.ui.container.Composite,
	include: [qx.locale.MTranslation],
	implement: ms123.bomviewer.IDrawingViewer,
	events: {
		"hotspot": "qx.event.type.Data"
	},

	construct: function (context) {
		this.base(arguments);
		this.setLayout(new qx.ui.layout.Canvas());


		context.controlContainer = this;
		var view = new ms123.pdf.PDFView(context);
		view.addListener("hotspot", function (e) {
			this.fireDataEvent("hotspot", e.getData(), null);
		}, this);
		var toolbar = new ms123.pdf.ToolBar(view);
		this.m_viewer = view;
		this.m_toolbar = toolbar;
		toolbar.setScale(context.scale);
		this.add(view, {
			width: "100%",
			height: "100%",
			top: 0,
			left: 0
		});
		this.add(toolbar, {
			top: 0,
			left: 0
		});
	},

	properties: {},

	members: {
		open: function (url, hotspots, scale) {
			this.m_toolbar.setScale(scale);
			this.m_viewer.open(url, hotspots, scale);
		},
		close: function () {
			this.m_viewer.close();
		},
		destroy: function () {},
		selectHotspot: function (href) {
			this.m_viewer.selectHotspot(href);
		}
	}
});
