/*
 * This file is part of SIMPL4(http://simpl4.org).
 *
 *  Copyright [2014] [Manfred Sattler] <manfred@ms123.org>
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
clazz.construct.extend( "simpl4.util.Text", {
	explode: function( text, max ) {
		text = text.replace( /  +/g, " " ).replace( /^ /, "" ).replace( / $/, "" );
		if ( typeof text === "undefined" ) return "";
		if ( typeof max === "undefined" ) max = 50;
		if ( text.length <= max ) return text;
		var exploded = text.substring( 0, max );
		text = text.substring( max );
		if ( text.charAt( 0 ) !== " " ) {
			while ( exploded.charAt( exploded.length - 1 ) !== " " && exploded.length > 0 ) {
				text = exploded.charAt( exploded.length - 1 ) + text;
				exploded = exploded.substring( 0, exploded.length - 1 );
			}
			if ( exploded.length == 0 ) {
				exploded = text.substring( 0, max );
				text = text.substring( max );
			} else {
				exploded = exploded.substring( 0, exploded.length - 1 );
			}
		} else {
			text = text.substring( 1 );
		}
		return exploded + "<br/>" + simpl4.util.Text.explode( text, max );
	}
}, {} );
