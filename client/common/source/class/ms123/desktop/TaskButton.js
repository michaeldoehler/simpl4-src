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
qx.Class.define('ms123.desktop.TaskButton', {

	extend: qx.ui.form.ToggleButton,

	properties: {
		realAppName: {
			check: 'String'
		}
	},

	construct: function (label, realAppName) {
		arguments.callee.base.call(this, label);

		this.setLabel('<span style="color:#333333;font-family:Arial;font-weight:bold;font-size:12px">' + label + '</span>');
		this.setRealAppName(realAppName);

		this.getChildControl('label').set({
			rich: true,
			marginLeft: 15
		});

		this.set({
			focusable: false,
			keepFocus: true,
			padding: 3,
			paddingRight: 5,
			height: 29,
			maxHeight: 29,
			alignY: 'middle',
			textColor: '#000000',
			minWidth: 130,
			center: false
		});

		this.setFont(new qx.bom.Font(11, ['Lucida Grande', 'Verdana']));

	},

	members: {
		_miniButton: false,
		_miniButtonStyle: false,
		_miniButtonStyleOver: false,
		_eyeMenu: null,

		_buttonWithFocus: function () {
			this._miniButtonStyle = this._decoratorWhiteNone;
			this._miniButtonStyleOver = this._decoratorWhiteBlue;

			this.set({
				textColor: '#FFFFFF'
			});
		},

		_buttonWithoutFocus: function () {
			this._miniButtonStyle = this._decoratorBlueNone;
			this._miniButtonStyleOver = this._decoratorWhiteLightBlue;

			this.set({
				textColor: '#2a60ac'
			});
		}
	}
});
