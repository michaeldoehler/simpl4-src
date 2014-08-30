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
qx.Class.define('ms123.desktop.TaskBar', {
	extend: qx.ui.container.SlideBar,

	construct: function () {
		this.base(arguments);
	},

	members: {

		_taskButtons: {},

		add: function (item) {
			this.base(arguments, item);
			this.fireDataEvent('favoriteAdded', item);
		},

		remove: function (item) {
			this.base(arguments, item);
			this.fireDataEvent('favoriteRemoved', item);
		},

		addTaskButton: function (taskButton) {
			this.add(taskButton);
		},

		addWindow: function (window) {
			var hashCode = window.toHashCode();
			var flag = false;
			this._taskButtons[hashCode] = new ms123.desktop.TaskButton(window.getCaption(), "XXX" /*windowApplicationName*/ );
			this._taskButtons[hashCode]._window = window;
			var self = this;

			window.addListener('close', function () {
				this.removeWindow(window);
			}, this);

			window.addListener('changeActive', function () {
				if (this._taskButtons[hashCode]) {
					if (window.get('active')) {
						this.selectTaskButton(this._taskButtons[hashCode]);
					} else {
						this.unselectTaskButton(this._taskButtons[hashCode]);
					}
				}
			}, this);

			this._taskButtons[hashCode].clickEvent = this._taskButtons[hashCode].addListener('click', function (e) {
				if (this.get('value')) {
					if (window.getLastMode() == "maximized") {
						window.maximize();
					} else {
						window.show();
					}
					window.set('active', true);
					window.focus();
				} else {
					if (window.get('allowMinimize')) {
						window.minimize();
					}
				}
			});

			this._taskButtons[hashCode].checkedEvent = this._taskButtons[hashCode].addListener('changeValue', function (e) {
				if (this.isValue()) {
					this._buttonWithFocus();
				} else {
					this._buttonWithoutFocus();
				}
			});

			if (!flag) {
				this.addTaskButton(this._taskButtons[hashCode]);
			}
		},

		removeTaskButton: function (taskButton) {
			this.remove(taskButton);
		},

		removeWindow: function (window) {
			var hashCode = window.toHashCode();
			if (this._taskButtons[hashCode]) {
				this.removeTaskButton(this._taskButtons[hashCode]);
				delete this._taskButtons[hashCode];
			}
		},

		selectTaskButton: function (taskButton) {
			taskButton.set('value', true);
		},

		unselectTaskButton: function (taskButton) {
			taskButton.set('value', false);
		}
	}
});
