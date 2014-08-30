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
	* @ignore($)
	* @ignore(Hash)
*/

qx.Class.define("ms123.graphicaleditor.plugins.shapemenu.Menu", {
	extend: qx.core.Object,
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (parentNode) {
		this.base(arguments);

		this.bounds = undefined;
		this.shapes = undefined;
		this.buttons = [];
		this.isVisible = false;
		console.log("construct menu");

		this.node = ms123.oryx.Editor.graft("http://www.w3.org/1999/xhtml", $(parentNode), ['div',
		{
			id: ms123.oryx.Editor.provideId(),
			'class': 'Oryx_ShapeMenu'
		}]);

		this.alignContainers = new Hash();
		this.numberOfButtonsPerLevel = new Hash();
	},


	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},

	/******************************************************************************
	 STATICS
	 ******************************************************************************/
	statics: {},
	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {

		addButton: function (button) {
			this.buttons.push(button);
			// lazy grafting of the align containers
			if (!this.alignContainers[button.align]) {
				this.alignContainers[button.align] = ms123.oryx.Editor.graft("http://www.w3.org/1999/xhtml", this.node, ['div',
				{
					'class': button.align
				}]);
				this.node.appendChild(this.alignContainers[button.align]);

				// add event listeners for hover effect
				var onBubble = false;
				this.alignContainers[button.align].addEventListener(ms123.oryx.Config.EVENT_MOUSEOVER, this.hoverAlignContainer.bind(this, button.align), onBubble);
				this.alignContainers[button.align].addEventListener(ms123.oryx.Config.EVENT_MOUSEOUT, this.resetAlignContainer.bind(this, button.align), onBubble);
				this.alignContainers[button.align].addEventListener(ms123.oryx.Config.EVENT_MOUSEUP, this.hoverAlignContainer.bind(this, button.align), onBubble);
			}
			button.getContentElement().__flush();
			try {
				this.alignContainers[button.align].appendChild(button.getContentElement().getDomElement());
			} catch (e) {
				console.error(e);
			}
		},

		removeAllButtons: function () {
			var me = this;
			this.buttons.each(function (value) {
				if (value.node && value.node.parentNode) value.node.parentNode.removeChild(value.node);
			});
			this.buttons = [];
		},

		closeAllButtons: function () {
			this.buttons.each(function (value) {
				value.prepareToHide()
			});
			this.isVisible = false;
		},

		/**
		 * Show the shape menu
		 */
		show: function (shapes) {

			//shapes = (shapes||[]).findAll(function(r){ return r && r.node && r.node.parent });
			if (shapes.length <= 0) return;

			this.shapes = shapes;

			var newBounds = undefined;
			var tmpBounds = undefined;

			this.shapes.each(function (value) {
				var a = value.node.getScreenCTM();
				var upL = value.absoluteXY();
				a.e = a.a * upL.x;
				a.f = a.d * upL.y;
				tmpBounds = new ms123.oryx.core.Bounds(a.e, a.f, a.e + a.a * value.bounds.width(), a.f + a.d * value.bounds.height());

/*if(value instanceof ORYX.Core.Edge) {
				tmpBounds.moveBy(value.bounds.upperLeft())
			}*/

				if (!newBounds) newBounds = tmpBounds
				else newBounds.include(tmpBounds);

			});

			this.bounds = newBounds;
			//this.bounds.moveBy({x:document.documentElement.scrollLeft, y:document.documentElement.scrollTop});
			var bounds = this.bounds;

			var a = this.bounds.upperLeft();

			var left = 0,
				leftButtonGroup = 0;
			var top = 0,
				topButtonGroup = 0;
			var bottom = 0,
				bottomButtonGroup;
			var right = 0,
				rightButtonGroup = 0;
			var size = 22;

			this.getWillShowButtons().sortBy(function (button) {
				return button.group;
			});

			this.getWillShowButtons().each(qx.lang.Function.bind(function (button) {

				var numOfButtonsPerLevel = this.getNumberOfButtonsPerLevel(button.align);

				if (button.align == ms123.oryx.Config.SHAPEMENU_LEFT) {
					// vertical levels
					if (button.group != leftButtonGroup) {
						left = 0;
						leftButtonGroup = button.group;
					}
					var x = Math.floor(left / numOfButtonsPerLevel)
					var y = left % numOfButtonsPerLevel;

					button.setLevel(x);

					button.setPosition(a.x - 5 - (x + 1) * size, a.y + numOfButtonsPerLevel * button.group * size + button.group * 0.3 * size + y * size);

					//button.setPosition(a.x-22, a.y+left*size);
					left++;
				} else if (button.align == ms123.oryx.Config.SHAPEMENU_TOP) {
					// horizontal levels
					if (button.group != topButtonGroup) {
						top = 0;
						topButtonGroup = button.group;
					}
					var x = top % numOfButtonsPerLevel;
					var y = Math.floor(top / numOfButtonsPerLevel);

					button.setLevel(y);

					button.setPosition(a.x + numOfButtonsPerLevel * button.group * size + button.group * 0.3 * size + x * size, a.y - 5 - (y + 1) * size);
					top++;
				} else if (button.align == ms123.oryx.Config.SHAPEMENU_BOTTOM) {
					// horizontal levels
					if (button.group != bottomButtonGroup) {
						bottom = 0;
						bottomButtonGroup = button.group;
					}
					var x = bottom % numOfButtonsPerLevel;
					var y = Math.floor(bottom / numOfButtonsPerLevel);

					button.setLevel(y);

					button.setPosition(a.x + numOfButtonsPerLevel * button.group * size + button.group * 0.3 * size + x * size, a.y + bounds.height() + 5 + y * size);
					bottom++;
				} else {
					// vertical levels
					if (button.group != rightButtonGroup) {
						right = 0;
						rightButtonGroup = button.group;
					}
					var x = Math.floor(right / numOfButtonsPerLevel)
					var y = right % numOfButtonsPerLevel;

					button.setLevel(x);

					button.setPosition(a.x + bounds.width() + 5 + x * size, a.y + numOfButtonsPerLevel * button.group * size + button.group * 0.3 * size + y * size - 5);
					right++;
				}

				button.show();
			}, this));
			this.isVisible = true;

		},

		/**
		 * Hide the shape menu
		 */
		hide: function () {

			this.buttons.each(function (button) {
				button.hide();
			});

			this.isVisible = false;
			//this.bounds = undefined;
			//this.shape = undefined;
		},

		hoverAlignContainer: function (align, evt) {
			this.buttons.each(function (button) {
				if (button.align == align) button.showOpaque();
			});
		},

		resetAlignContainer: function (align, evt) {
			this.buttons.each(function (button) {
				if (button.align == align) button.showTransparent();
			});
		},

		isHover: function () {
			return this.buttons.any(function (value) {
				return value.isHover();
			});
		},

		getWillShowButtons: function () {
			return this.buttons.findAll(function (value) {
				return value.willShow
			});
		},

		/**
		 * Returns a set on buttons for that align value
		 * params {String} align
		 * params {String} group
		 */
		getButtons: function (align, group) {
			return this.getWillShowButtons().findAll(function (b) {
				return b.align == align && (group === undefined || b.group == group)
			})
		},

		/**
		 * Set the number of buttons to display on each level of the shape menu in the specified align group.
		 * Example: setNumberOfButtonsPerLevel(ms123.oryx.Config.SHAPEMENU_RIGHT, 2) causes that the buttons of the right align group 
		 * will be rendered in 2 rows.
		 */
		setNumberOfButtonsPerLevel: function (align, number) {
			this.numberOfButtonsPerLevel[align] = number;
		},

		/**
		 * Returns the number of buttons to display on each level of the shape menu in the specified align group.
		 * Default value is 1
		 */
		getNumberOfButtonsPerLevel: function (align) {
			if (this.numberOfButtonsPerLevel[align]) return Math.min(this.getButtons(align, 0).length, this.numberOfButtonsPerLevel[align]);
			else return 1;
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
