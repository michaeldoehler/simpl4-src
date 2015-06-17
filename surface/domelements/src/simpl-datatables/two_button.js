/**
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

$.fn.dataTableExt.oPagination.two_button = {
	"fnInit": function( oSettings, nPaging, fnCallbackDraw ) {
		var nPrevious = document.createElement( 'span' );
		var nNext = document.createElement( 'span' );

		nPrevious.className = "paginate_button previous fa fa-angle-left fa-2x";
		nNext.className = "paginate_button next fa-angle-right fa-2x";

		nPaging.appendChild( nPrevious );
		nPaging.appendChild( nNext );

		$( nPrevious ).click( function() {
			oSettings.oApi._fnPageChange( oSettings, "previous" );
			fnCallbackDraw( oSettings );
		} );

		$( nNext ).click( function() {
			oSettings.oApi._fnPageChange( oSettings, "next" );
			fnCallbackDraw( oSettings );
		} );

		/* Disallow text selection */
		$( nPrevious ).bind( 'selectstart', function() {
			return false;
		} );
		$( nNext ).bind( 'selectstart', function() {
			return false;
		} );
	},


	"fnUpdate": function( oSettings, fnCallbackDraw ) {
		if ( !oSettings.aanFeatures.p ) {
			return;
		}

		var an = oSettings.aanFeatures.p;
		for ( var i = 0, iLen = an.length; i < iLen; i++ ) {
			var buttons = an[ i ].getElementsByTagName( 'span' );
			if ( oSettings._iDisplayStart === 0 ) {
				buttons[ 0 ].className = "paginate_disabled_previous fa fa-caret-left fa-2x";
			} else {
				buttons[ 0 ].className = "paginate_enabled_previous fa fa-caret-left fa-2x";
			}

			if ( oSettings.fnDisplayEnd() == oSettings.fnRecordsDisplay() ) {
				buttons[ 1 ].className = "paginate_disabled_next fa fa-caret-right fa-2x";
			} else {
				buttons[ 1 ].className = "paginate_enabled_next fa fa-caret-right fa-2x";
			}
		}
	}
};
