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
require("./Construct");
clazz.construct.extend( "simpl4.util.Map", {
}, {
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	init: function( iterable ) {
		this.store = {};

		if ( iterable ) {
			iterable.forEach( function( i ) {
				this.set( i[ 0 ], i[ 1 ] );
			} );
		}
	},
	get: function( key ) {
		return this.store[ key ];
	},
	put: function( key,val ) {
		return this.set( key, val );
	},
	set: function( key,val ) {
		this.store[ key ] = val;
		return this;
	},
	delete: function( key,val ) {
		if ( !this.has( key ) ){	
			return false;
		}
		delete this.store[ key ];
		return true;
	},
	clear: function() {
		this.store = {};
	},
	has: function( key,val ) {
		var hasKey = this.store[ key ];
		return hasKey ? true : false;
	},
	size: function( ) {
		return this.keys().length;
	},
	keys: function( ) {
		return Object.keys( this.store );
	},
	keys: function( ) {
		return Object.keys( this.store );
	},
	values: function( ) {
		var vals = [];
		this.forEachEntry( function( entry ) {
			vals.push( entry );
		} );
		return vals;
	},
	forEachEntry: function( iter ) {
		var keys = this.makeIterator(this.keys());
		var next;
		while ( !( next = keys.next() ).done ) {
			iter( this.get( next.value ), next.value, this );
		}
	},
	forEach: function( iter ) {
		this.forEachEntry( function( entry, key ) {
			iter( entry, key, this );
		} );
	},
	makeIterator:function( array ) {
		var nextIndex = 0;
		return {
			next: function() {
				return nextIndex < array.length ? {
					value: array[ nextIndex++ ],
					done: false
				} : {
					done: true
				};
			}
		};
	},
	toJS: function() {
		return this.store;
	}

} );

var map = new simpl4.util.Map();
map.put("key1", "val1a");
map.put("key1", "val1b");
map.put("key2", "val2");


console.log("Map:",JSON.stringify(map.toJS(),null,2));
console.log("Size:",map.size());
console.log("Values:",map.values());
console.log("Keys:",map.keys());
