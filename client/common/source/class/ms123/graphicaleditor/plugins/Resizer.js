/**
 * Copyright (c) 2006
 * Martin Czuchra, Nicolas Peters, Daniel Polak, Willi Tscheschner
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 **/

/**
	* @ignore(Hash)
	* @ignore($)
*/
qx.Class.define("ms123.graphicaleditor.plugins.Resizer", {
	extend: qx.core.Object,

	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (parentId, orientation, facade) {
		this.base(arguments);

		this.parentId = parentId;
		this.orientation = orientation;
		this.facade = facade;
		this.node = ms123.oryx.Editor.graft("http://www.w3.org/1999/xhtml", $(this.parentId), ['div',
		{
			'class': 'resizer_' + this.orientation,
			style: 'left:0px; top:0px;'
		}]);

		this.node.addEventListener(ms123.oryx.Config.EVENT_MOUSEDOWN, this.handleMouseDown.bind(this), true);
		document.documentElement.addEventListener(ms123.oryx.Config.EVENT_MOUSEUP, this.handleMouseUp.bind(this), true);
		document.documentElement.addEventListener(ms123.oryx.Config.EVENT_MOUSEMOVE, this.handleMouseMove.bind(this), false);

		this.dragEnable = false;
		this.offSetPosition = {
			x: 0,
			y: 0
		};
		this.bounds = undefined;

		this.canvasNode = this.facade.getCanvas().node;

		this.minSize = undefined;
		this.maxSize = undefined;

		this.aspectRatio = undefined;

		this.resizeCallbacks = [];
		this.resizeStartCallbacks = [];
		this.resizeEndCallbacks = [];
		this.hide();

		// Calculate the Offset
		this.scrollNode = this.node.parentNode.parentNode.parentNode;


	},

	/******************************************************************************
	 STATICS
	 ******************************************************************************/
	statics: {

	},

	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},

	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		handleMouseDown: function (event) {
			this.dragEnable = true;

			this.offsetScroll = {
				x: this.scrollNode.scrollLeft,
				y: this.scrollNode.scrollTop
			};

			this.offSetPosition = {
				x: Event.pointerX(event) - this.position.x,
				y: Event.pointerY(event) - this.position.y
			};

			this.resizeStartCallbacks.each((function (value) {
				value(this.bounds);
			}).bind(this));

		},

		handleMouseUp: function (event) {
			this.dragEnable = false;
			this.containmentParentNode = null;
			this.resizeEndCallbacks.each((function (value) {
				value(this.bounds);
			}).bind(this));

		},

		handleMouseMove: function (event) {
			if (!this.dragEnable) {
				return
			}

			if (event.shiftKey || event.ctrlKey) {
				this.aspectRatio = this.bounds.width() / this.bounds.height();
			} else {
				this.aspectRatio = undefined;
			}

			var position = {
				x: Event.pointerX(event) - this.offSetPosition.x,
				y: Event.pointerY(event) - this.offSetPosition.y
			}


			position.x -= this.offsetScroll.x - this.scrollNode.scrollLeft;
			position.y -= this.offsetScroll.y - this.scrollNode.scrollTop;

			position.x = Math.min(position.x, this.facade.getCanvas().bounds.width())
			position.y = Math.min(position.y, this.facade.getCanvas().bounds.height())

			var offset = {
				x: position.x - this.position.x,
				y: position.y - this.position.y
			}

			if (this.aspectRatio) {
				// fixed aspect ratio
				var newAspectRatio = (this.bounds.width() + offset.x) / (this.bounds.height() + offset.y);
				if (newAspectRatio > this.aspectRatio) {
					offset.x = this.aspectRatio * (this.bounds.height() + offset.y) - this.bounds.width();
				} else if (newAspectRatio < this.aspectRatio) {
					offset.y = (this.bounds.width() + offset.x) / this.aspectRatio - this.bounds.height();
				}
			}

			// respect minimum and maximum sizes of stencil
			if (this.orientation === "northwest") {
				if (this.bounds.width() - offset.x > this.maxSize.width) {
					offset.x = -(this.maxSize.width - this.bounds.width());
					if (this.aspectRatio) offset.y = this.aspectRatio * offset.x;
				}
				if (this.bounds.width() - offset.x < this.minSize.width) {
					offset.x = -(this.minSize.width - this.bounds.width());
					if (this.aspectRatio) offset.y = this.aspectRatio * offset.x;
				}
				if (this.bounds.height() - offset.y > this.maxSize.height) {
					offset.y = -(this.maxSize.height - this.bounds.height());
					if (this.aspectRatio) offset.x = offset.y / this.aspectRatio;
				}
				if (this.bounds.height() - offset.y < this.minSize.height) {
					offset.y = -(this.minSize.height - this.bounds.height());
					if (this.aspectRatio) offset.x = offset.y / this.aspectRatio;
				}
			} else { // defaults to southeast
				if (this.bounds.width() + offset.x > this.maxSize.width) {
					offset.x = this.maxSize.width - this.bounds.width();
					if (this.aspectRatio) offset.y = this.aspectRatio * offset.x;
				}
				if (this.bounds.width() + offset.x < this.minSize.width) {
					offset.x = this.minSize.width - this.bounds.width();
					if (this.aspectRatio) offset.y = this.aspectRatio * offset.x;
				}
				if (this.bounds.height() + offset.y > this.maxSize.height) {
					offset.y = this.maxSize.height - this.bounds.height();
					if (this.aspectRatio) offset.x = offset.y / this.aspectRatio;
				}
				if (this.bounds.height() + offset.y < this.minSize.height) {
					offset.y = this.minSize.height - this.bounds.height();
					if (this.aspectRatio) offset.x = offset.y / this.aspectRatio;
				}
			}

			if (this.orientation === "northwest") {
				var oldLR = {
					x: this.bounds.lowerRight().x,
					y: this.bounds.lowerRight().y
				};
				this.bounds.extend({
					x: -offset.x,
					y: -offset.y
				});
				this.bounds.moveBy(offset);
			} else { // defaults to southeast
				this.bounds.extend(offset);
			}

			this.update();

			this.resizeCallbacks.each((function (value) {
				value(this.bounds);
			}).bind(this));

			Event.stop(event);

		},

		registerOnResizeStart: function (callback) {
			if (!this.resizeStartCallbacks.member(callback)) {
				this.resizeStartCallbacks.push(callback);
			}
		},

		unregisterOnResizeStart: function (callback) {
			if (this.resizeStartCallbacks.member(callback)) {
				this.resizeStartCallbacks = this.resizeStartCallbacks.without(callback);
			}
		},

		registerOnResizeEnd: function (callback) {
			if (!this.resizeEndCallbacks.member(callback)) {
				this.resizeEndCallbacks.push(callback);
			}
		},

		unregisterOnResizeEnd: function (callback) {
			if (this.resizeEndCallbacks.member(callback)) {
				this.resizeEndCallbacks = this.resizeEndCallbacks.without(callback);
			}
		},

		registerOnResize: function (callback) {
			if (!this.resizeCallbacks.member(callback)) {
				this.resizeCallbacks.push(callback);
			}
		},

		unregisterOnResize: function (callback) {
			if (this.resizeCallbacks.member(callback)) {
				this.resizeCallbacks = this.resizeCallbacks.without(callback);
			}
		},

		hide: function () {
			this.node.style.display = "none";
		},

		show: function () {
			if (this.bounds) this.node.style.display = "";
		},

		setBounds: function (bounds, min, max, aspectRatio) {
			this.bounds = bounds;

			if (!min) min = {
				width: ms123.oryx.Config.MINIMUM_SIZE,
				height: ms123.oryx.Config.MINIMUM_SIZE
			};

			if (!max) max = {
				width: ms123.oryx.Config.MAXIMUM_SIZE,
				height: ms123.oryx.Config.MAXIMUM_SIZE
			};

			this.minSize = min;
			this.maxSize = max;

			this.aspectRatio = aspectRatio;

			this.update();
		},

		update: function () {
			if (!this.bounds) {
				return;
			}

			var upL = this.bounds.upperLeft();

			if (this.bounds.width() < this.minSize.width) {
				this.bounds.set(upL.x, upL.y, upL.x + this.minSize.width, upL.y + this.bounds.height())
			};
			if (this.bounds.height() < this.minSize.height) {
				this.bounds.set(upL.x, upL.y, upL.x + this.bounds.width(), upL.y + this.minSize.height)
			};
			if (this.bounds.width() > this.maxSize.width) {
				this.bounds.set(upL.x, upL.y, upL.x + this.maxSize.width, upL.y + this.bounds.height())
			};
			if (this.bounds.height() > this.maxSize.height) {
				this.bounds.set(upL.x, upL.y, upL.x + this.bounds.width(), upL.y + this.maxSize.height)
			};

			var a = this.canvasNode.getScreenCTM();

			upL.x *= a.a;
			upL.y *= a.d;

			if (this.orientation === "northwest") {
				upL.x -= 13;
				upL.y -= 26;
			} else { // defaults to southeast
				upL.x += (a.a * this.bounds.width()) + 3;
				upL.y += (a.d * this.bounds.height()) + 3;
			}

			this.position = upL;

			this.node.style.left = this.position.x + "px";
			this.node.style.top = this.position.y + "px";
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
