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
clazz.construct.extend( "simpl4.util.PageRegistry", {
	pageCache: {},
	observable: null,
	activePage:null,
	clearCache: function() {
		simpl4.util.PageRegistry.pageCache = {};
	},
	addPage: function( hash, page ) {
		simpl4.util.PageRegistry.pageCache[ hash ] = page;
	},
	getPage: function( hash ) {
		return simpl4.util.PageRegistry.pageCache[ hash ];
	},
	addPages: function( pages ) {
		console.log( "addPages:", pages );
		for ( var i = 0; i < pages.length; i++ ) {
			simpl4.util.PageRegistry.pageCache[ pages[ i ].hash ] = pages[ i ];
		}
	},
	getPages: function() {
		var keys = Object.keys( simpl4.util.PageRegistry.pageCache );
		var ret = []
		for ( var i = 0; i < keys.length; i++ ) {
			ret.push( simpl4.util.PageRegistry.pageCache[ keys[ i ] ] );
		}
		console.log( "getPages:", ret );
		return ret;
	},
	setActivePage:function(ap){
		this.activePage=ap;
	},
	getActivePage:function(){
		return this.activePage;
	},
	registerListener: function() {
		var aList = document.querySelectorAll( 'html /deep/ a' )
		console.log( "Xa-lIst:", aList );

		var source = Rx.DOM.fromEvent( aList, 'tap', function( e ) {
			console.log( "selector:", e );
			return e;
		} );
		return source;
	},
	subscribe: function(cb) {
		console.log( "subscribe:", this.observable );
		if ( this.observable == null ) {
			this.observable = this.registerListener();
		}
		console.log( "subscribe2:", this.observable );
		this.observable.subscribe(cb);
	}
}, {} );
