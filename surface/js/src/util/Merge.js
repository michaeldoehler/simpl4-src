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
clazz.construct.extend( "simpl4.util.Merge", {
	deepmerge: function( target, src ) {
		var array = Array.isArray( src );
		var dst = array && [] || {};

		if ( array ) {
			target = target || [];
			dst = dst.concat( target );
			src.forEach( function( e, i ) {
				if ( typeof dst[ i ] === 'undefined' ) {
					dst[ i ] = e;
				} else if ( typeof e === 'object' ) {
					dst[ i ] = simpl4.util.Merge.deepmerge( target[ i ], e );
				} else {
					if ( target.indexOf( e ) === -1 ) {
						dst.push( e );
					}
				}
			} );
		} else {
			if ( target && typeof target === 'object' ) {
				Object.keys( target ).forEach( function( key ) {
					dst[ key ] = target[ key ];
				} )
			}
			Object.keys( src ).forEach( function( key ) {
				if ( typeof src[ key ] !== 'object' || !src[ key ] ) {
					dst[ key ] = src[ key ];
				} else {
					if ( !target[ key ] ) {
						dst[ key ] = src[ key ];
					} else {
						dst[ key ] = simpl4.util.Merge.deepmerge( target[ key ], src[ key ] );
					}
				}
			} );
		}

		return dst;
	}
}, {} );
