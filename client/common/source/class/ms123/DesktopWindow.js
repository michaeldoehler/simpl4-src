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
qx.Class.define("ms123.DesktopWindow", {
	extend: ms123.desktop.Window,


	statics: {
		_top: 15,
		_left: 25
	},

	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (context, clazz) {
		this.base(arguments, null, context.window_title);
		var app = qx.core.Init.getApplication();

		var me = context.me;
		var self = this;
		if( me !=null && me.load){
        this._loadScript(me.load, function () {
					me.load=null;
					self._configWindow(app.getRoot(), context);
					self._init(context, clazz);
        });
		}else{
			this._configWindow(app.getRoot(), context);
			this._init(context, clazz);
		}
	},

	/*******************************************************************************
	 EVENTS
	 ***************************************************************************** */
	events: {
		"close": "qx.event.type.Event"
	},

	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},


	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
    _loadScript:function(path, callback) {
			var script = document.createElement('script');
			script.async = false;
			script.src = path;
			script.type = 'text/javascript';
			script.onload = callback || script.onload;
			document.getElementsByTagName('head')[0].appendChild(script);
    },
		_init: function (context, clazz) {
			this._context = context;
			var app = qx.core.Init.getApplication();
			var a = app.toString();
			context.appid = a.substring(0, a.indexOf("."));
			var ns = context.storeDesc.getNamespace();
			var tb = app.getTaskbar(ns);
			var dt = app.getDesktop(ns);
			dt.add(this);
			context.window = this;
			if (!clazz) {
				if (typeof context.config == "function") {
					clazz = context.config;
					console.log("clazz:" + clazz);
					this._dunit = new clazz(context);
				}
			} else {
				this._dunit = new clazz(context);
			}
			if (this._dunit) {
				this._dunit.window = this;
			}
			this.addListener("close", function (e) {
				console.log("DesktopWindow.destroy:" + this);
				//this._disposeObjects("_dunit");
				dt.updateStatus();
			}, this);
			tb.addWindow(this);
			dt.updateStatus();
		},

		getDesktopUnit: function () {
			return this._dunit;
		},
		getContext: function () {
			return this._context;
		},

		_configWindow: function (root, context) {
			var me = context.me || {};
			this.set({
				resizable: true,
				useMoveFrame: false,
				contentPadding: 4,
				useResizeFrame: false
			});
			var w = root.getInnerSize().width;
			var h = root.getInnerSize().height;

			this.setWidth((w * 0.8)|0);
			if( me.widthFactor ){
				this.setWidth((w * me.widthFactor)|0);
			}

			this.setHeight((h * 0.8)|0);
			this.setLayout(new qx.ui.layout.Grow());
			this.setAllowMaximize(true);
			this.open(ms123.DesktopWindow._left, ms123.DesktopWindow._top);
			ms123.DesktopWindow._top += 20;
			ms123.DesktopWindow._left += 10;
			if (ms123.DesktopWindow._left > 70) {
				ms123.DesktopWindow._top = 15;
				ms123.DesktopWindow._left = 25;
			}
			return this;
		}
	},
	destruct: function () {
		console.error("DesktopWindow.destruct1");
		this._disposeObjects("_dunit");
	}
});
