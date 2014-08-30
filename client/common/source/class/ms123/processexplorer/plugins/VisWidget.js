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
 @ignore(vis.Network)
 */

qx.Class.define("ms123.processexplorer.plugins.VisWidget", {
	extend: qx.ui.container.Scroll,
	construct: function (options, width, height) {
		this.base(arguments);

		this._width = width;
		this._options = options;
		this._height = height;

		this.setScrollbarX("off");
		this.setScrollbarY("off");

		this._visContainer = new qx.ui.embed.Html();
		this.setHeight(height);
		this._visContainer.setHeight(height);
		this._visContainer.setMinHeight(height);
		this._visContainer.setWidth(width);
		this._visContainer.setMaxWidth(width);
		this._visContainer.setMinWidth(width);
		this.add(this._visContainer);
		this.addListener("resize", this._onResize, this);

	},

	members: {
		_onResize:function(){
			console.log("bounds:"+JSON.stringify(this.getBounds(),null,2));
			var w = this.getBounds().width;
			this._visContainer.setWidth(w);
			this._visContainer.setMaxWidth(w);
			this._visContainer.setMinWidth(w);
			this._width = w;

			qx.event.Timer.once(function () {
				this.setData(this._data);
				this.selectNode(this._selectedNode);
			}, this, 200);


		},
		setData: function (data) {
			this._data = data;
			var el = this._visContainer.getContentElement().getDomElement();
			if( data != null){
				var network = new vis.Network(el, data, this._options);
				network.setSize(this._width, this._height)
				network.zoomExtent();
			}else{
				el.innerHTML='';
			}
			this._network = network;
		},
		selectNode:function(nodeid){
			if( nodeid==null) return;
			this._selectedNode=nodeid;
			try{
				this._network.selectNodes([nodeid]);
			}catch(e){
				console.error("VisWidget.selectNodes:"+e);
			}
		}
	}
});
