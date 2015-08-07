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
simpl4.util.Map.extend( "simpl4.util.MultiMap", {
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
	set: function( key,val ) {
		var args = Array.prototype.slice.call( arguments );
		key = args.shift();
		var entry = this.get( key );
		if ( !entry ) {
			entry = [];
			this.store[ key ] = entry;
		}
		Array.prototype.push.apply( entry, args );
		return this;
	},
	delete: function( key,val ) {
		if ( !this.has( key ) ){
			return false;
		}

		if ( arguments.length == 1 ) {
			( delete this.store[ key ] );
			return true;
		} else {
			var entry = this.get( key );
			var idx = entry.indexOf( val );
			if ( idx != -1 ) {
				entry.splice( idx, 1 );
				return true;
			}
		}
		return false;
	},
	has: function( key,val ) {
		var hasKey = this.store.hasOwnProperty( key );

		if ( arguments.length == 1 || !hasKey ){
			return hasKey;
		}

		var entry = this.get( key ) || [];
		return entry.indexOf( val ) != -1;
	},
	size: function( ) {
		var keys = this.makeIterator(this.keys());
		var next, total = 0;
		while ( !( next = keys.next() ).done ) {
			total += this.get( next.value ).length;
		}
		return total;
	},
	values: function( ) {
		var vals = [];
		this.forEachEntry( function( entry ) {
			Array.prototype.push.apply( vals, entry );
		} );
		return vals;
	},
	forEach: function( iter ) {
		this.forEachEntry( function( entry, key ) {
			entry.forEach( function( item ) {
				iter( item, key, this );
			} );
		} );
	}
} );

var map = new simpl4.util.MultiMap();
map.put("key1", "val1a");
map.put("key1", "val1b");
map.put("key2", "val2");


console.log("MultiMap:",JSON.stringify(map.toJS(),null,2));
console.log("Size:",map.size());
console.log("Values:",map.values());
console.log("Keys:",map.keys());
