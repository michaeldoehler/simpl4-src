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
 * @ignore(jQuery.ajax) 
 * @ignore(jQuery.each)
 * @ignore(jQuery.inArray)
 */
qx.Class.define("ms123.bomviewer.BOMPdfViewer", {
	extend: ms123.bomviewer.BOMViewer,


	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (context) {
		this.base(arguments, context);
	},


	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},


	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		_createViewer: function () {
			var url = '/sw/resource/SD_2100097.pdf';
			var mapUrl = '/sw/resource/png_2100097/map.xml';
			var v = new ms123.pdf.PDFViewer({
				url: url,
				scale: "page-height",
				hotspots: this._getHotspots(mapUrl)
			});
			return v;
		},
		_openViewer: function (part) {
			var url = '/sw/resource/SD_' + part + '.pdf';
			var mapUrl = '/sw/resource/png_' + part + '/map.xml';
			this._viewer.open(url, this._getHotspots(mapUrl), "page-width");
		}
	},
	destruct: function () {
		console.error("BOMViewer.close");
		this._viewer.destroy();
	}
});
