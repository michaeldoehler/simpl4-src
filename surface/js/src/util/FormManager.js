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
simpl4.util.BaseManager.extend("simpl4.util.FormManager", {
	getForm: function(name,namespace) {
		var failed = function(details) {
			alert("GetForm failed" + ":" + details.message);
		};

		try {
			var ret = simpl4.util.Rpc.rpcSync("git:searchContent", {
				reponame: namespace || simpl4.util.BaseManager.getNamespace(),
				name: name,
				type:"sw.form"
			});
			return ret;
		} catch (e) {
			failed(e);
			return [];
		}
	},
	handleQueryBuilderSearchInput: function( rule, filter ) {
		var regulaConstraints = null;
		if ( filter.constraints ) {
			var c = JSON.parse( filter.constraints );
			regulaConstraints = this.constructRegulaConstraints( c );
		}
		var attributes = this.constructAttributes( filter.type, filter.label, c );
		rule.find( ".rule-value-container" ).
			append( '<input-field ' + attributes + ' ' + regulaConstraints + ' name="' + rule.selector.substring( 1 ) + '_value_0" ></input-field>' );
	},
	constructRegulaConstraints: function( c ) {
		var ret = 'data-constraints="';
		var b="";
		c.forEach( function( x ) {
			var params = this._constraintParams[ x.annotation ];
			ret += b+ '@' + x.annotation;
			if ( params ) {
				ret += '(';
				if ( params.length > 0 ) {
					var key = params[ 0 ];
					var val = key=='format' ? 'YMD' : x.parameter1;
					ret += key + '=' + val;
				}
				if ( params.length > 1 ) {
					var key = params[ 1 ];
					var val = x.parameter2;
					ret += ',' + key + '=' + val;
				}
				ret += ')';
			}
			b=' ';
		}, this );
		ret += '"';
		return ret;
	},
	constructAttributes: function( xtype, label, c ) {
		var map={};
		if( c == null) c = [];
		map.type = "text";
		if ( xtype === "date" ) {
			map.type = "date";
		}
		if ( xtype === "integer" ) {
			map.type = "number";
		}
		if ( xtype === "double" ) {
			map.type = "number";
		}
		map.style="min-width:60px;"
		map.label=label;
		map.floatingLabel=false;
		map.compact=true;
		if( map.type == 'number'){
			map.step='1';
			map.preventInvalidInput=true;
		}
		if( map.type == 'double'){
			map.preventInvalidInput=true;
			map.step='any';
		}
		c.forEach( function( x ) {
			var a = x.annotation;
			if( a == 'Min' || a == 'Max' || a == 'Pattern'){
				map[a] = x.parameter1;
			}
			if( a == 'Digits'){
				var f = parseInt(x.parameter2);
				map.step= Math.pow(10,-f)+'';
			}
			if( a == 'Email'){
				map.type= 'email';
			}
			if( a == 'Url'){
				map.type= 'url';
			}
		} );
		var b = '';
		var ret = '';
		Object.keys( map ).forEach( function( key ) {
			if( map[key] === true){
				ret+= b+ key;
			}else{
				ret+= b+ key+'='+map[key];
			}
			b=' ';
		});
		return ret;
	},
	_constraintParams: {
		Max: [ "value" ],
		Min: [ "value" ],
		Range: [ "min", "max" ],
		Pattern: [ "regex" ],
		Length: [ "min", "max" ],
		Digits: [ "integer", "fraction" ],
		Past: [ "format" ],
		Future: [ "format" ],
		Step: [ "min", "max", "value" ]
	}
}, {});
