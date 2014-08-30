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
qx.Class.define("ms123.processexplorer.plugins.StartImageRenderer", {
	extend: qx.ui.table.cellrenderer.Conditional,
	include: [qx.locale.MTranslation],

	/**
  *****************************************************************************
     PROPERTIES
  *****************************************************************************
  */

	properties: {},


/* 
  *****************************************************************************
     MEMBERS 
  *****************************************************************************
  */

	members: {
		createDataCellHtml: function (cellInfo, htmlArr) {
			this._msg = this.tr("processexplorer.definition.start_process");
			htmlArr.push(
			'<div class="', this._getCellClass(cellInfo), '" style="', 'left:', cellInfo.styleLeft, 'px;', 
			this._getCellSizeStyle(cellInfo.styleWidth, cellInfo.styleHeight, this._insetX, this._insetY), 
			this._getCellStyle(cellInfo), '" ', 
			this._getCellAttributes(cellInfo), '>' + 
			this._getContentHtml(cellInfo), 
			'</div>');
		},
		_getContentHtml: function (cellInfo) {
			return '<input title="'+this._msg+'" type="image" src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABHNCSVQICAgIfAhkiAAAAAlwSFlz'+
				'AAABuwAAAbsBOuzj4gAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAAMuSURB'+
				'VDjLdZNdSFNhGMdPgYkXEuGFhBGT6CaELrK7QIqCriy6SfIDlN2IWmRT1PxAGS1lzmVMmJkLTd0G'+
				'WpuWS8csY+F04nJuzp192tzO0m3uOOeO4p7edyWh2YEH3ov3/J////c+DwEAxNHicDjZCoViaHBw'+
				'sJsgiBOtra0lzc3NBeiccvTu3wO62NDQcCczMzO9r6+PR1EUuFwun1KplJtMJofdbo/19/e3/VdA'+
				'LBY/3dzcDJAkuby2tuYJBALg9/vB6/XC6uoqFgMkphQIBNUikagRNzwk0NnZ+WR3dzfKMAyEw2Ew'+
				'mpZphWqKHvmoiegXFrfdbjeg2kdOdlZWVoI1NTW5f5wTKTKZTDA5Oflhb28vHg7TjEA8EE2+/ACI'+
				'azVAXG+BpJxa4L8ejjmdTgYL6fV6M4vFSk8INDU1Fa6vrzO4K03T8LJXvpWa8xjKOsdB990CHMkc'+
				'XHw0AWkFAyB8+ymC4gFy4EGRn1dUVGQTXC63CEGyIxGw2e2h1KtF8dKu6URuDLJLZYUikR5utHyF'+
				'0/kyIB3uLZ/PBx6PB6RS6VCCPjrIMKzPWp0/+VYLjGlNOC+Ix02QXTsF50rH4VT+eyDuj8CsybGB'+
				'PkAsvL29vWJMvwc5oDDlL9o5H3G7AwxmO6h0JJwpGYWTee8SP+JKKVSAgfT+xHHlcrkUNyckEonU'+
				'bDaD1WoF0mYLJd3kxqcXbDCqcwCrXAVp7DFILvjd/VKlGkL09mY0GsUcHCh+CcFms68IhcJniKwH'+
				'zQC8GdF4H/bowOig4EKFKmH/fJkKsjhqUOq9gXg8Dui58VMzVVVVBYkZyMrKOqvRaCxGoxFnY2QT'+
				'+lWBwghOioa8F7Nwj/8N9FaKjkQi8Vgstj88PKxE0dvwCCQEysvL72q12vX5+fmdxcXFfYvFAg6n'+
				'O2giXRuuH96Yj6LiGFwoFIJgMLjD4/GqD40yhlFfX19bV1dXiZiMGQwGWFpaAiyEHCVeBLnzzMzM'+
				'WNAwBdrb2xv/2YWD6ujoEOh0OkatVpPd3d1SxMaP2fD5fF5GRkY6cpt7sAfHCuBcaMIK0UoX4YsI'+
				'8CuUd6i4uDj7uNX/BVL7wlyFKa78AAAAAElFTkSuQmCC"/input>';
		},
		_getCellClass: function (cellInfo) {
			return "qooxdoo-table-cell";
		}
	}
});
