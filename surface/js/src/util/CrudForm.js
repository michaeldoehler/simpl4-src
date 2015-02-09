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
can.Construct.extend( "simpl4.util.CrudForm", {
	/******************************************************************************
	 Static
	 ******************************************************************************/
}, {
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	init: function( namespace, entityname, fields, props ) {
		this.entityname = entityname;
		this.namespace = namespace;
		var tabView = this.getTabview( props.formlayout, entityname );
		this._fields = fields;
		this._form = {};
		var tabLists = this.getTabLists( this._fields );
		var tabKeys = Object.keys( tabLists );
		console.log( "tabKeys:" + tabKeys );
		tabKeys.forEach( function( tabKey ) {
			var tabviewPage = this.getTabViewPage( tabView, tabKey );
			tabviewPage.childShapes = [];
			var layout = tabviewPage.layout;
			var row = null;
			tabLists[ tabKey ].forEach( function( f, i ) {
				if ( layout == "single" ) {
					row = {
						id: "Row",
						childShapes: []
					}
					tabviewPage.childShapes.push( row );
				}
				if ( layout == "double" && ( i == 0 || ( i % 2 ) == 0 ) ) {
					row = {
						id: "Row",
						childShapes: []
					}
					tabviewPage.childShapes.push( row );
				}
				row.childShapes.push( this.getFieldShape( f ) );
			}, this );
		}, this );
//		console.log( "tabView:" + JSON.stringify( tabView, null, 2 ) );
		this._form = tabView;
	},
	getFieldShape: function( f ) {
		var shape = {};
		shape.id = "Input";
		if( f.edittype=="select"){
			shape.id= "Enumselect";
			shape.items = f.selectable_items.getItems();
		}
		shape.xf_id = f.name;
		shape.xf_type = this.convertFieldType(f);
		shape.label = tr( "data." + this.entityname + "." + f.name );
		if ( f.constraints ) {
console.log("constraints:",f.constraints);
			var c = JSON.parse( f.constraints );
			shape.regulaConstraints = simpl4.util.FormManager.constructRegulaConstraints( c );
console.log("regulaConstraints:",shape.regulaConstraints);
		}
		return shape;
	},
	convertFieldType: function( f ) {
		var retType="text";
		if ( f.datatype && f.datatype == 'date' ) {
			retType = f.edittype == "text" ? "date" : f.edittype;
		} else if ( f.datatype && ( f.datatype == 'decimal' || f.datatype == 'double' ) ) {
			retType = "double";
		} else if ( f.datatype && ( f.datatype == 'number' || f.datatype == 'integer' || f.datatype == 'long' ) ) {
			retType = "integer";
		}
		if ( retType === "boolean" ) {
			retType = "integer";
		}
		return retType;
	},
	getTabViewPage: function( tabView, tabKey ) {
		var childShapes = tabView.childShapes;
		var ret = null;
		childShapes.forEach( function( c ) {
			if ( c.xf_id == tabKey ) {
				ret = c;
				return;
			}
		}, this );
		return ret;
	},
	getTabLists: function( fields ) {
		var ret = {};
		fields.forEach( function( f ) {
			if ( f.id == "id" ) return;
			var tab = "tab1";
			if ( f.formoptions && f.formoptions.tab ) {
				tab = f.formoptions.tab;
			}
			var list = ret[ tab ];
			if ( list === undefined ) {
				list = [];
				ret[ tab ] = list;
			}
			list.push( f );
		}, this );
		return ret;
	},
	getTabview: function( formLayout, entityname ) {
		var defLayout = {
			"id": "Tabview",
			"childShapes": [ {
				"xf_id": "tab1",
				"id": "Page",
				"childShapes": []
			} ]
		};
		if ( formLayout == null ) {
			return defLayout;
		} else {
			try {
				var lays = formLayout.split( ";" );
				var formLayout = {
					id: "Tabview",
					childShapes: []
				};
				var childShapes = formLayout.childShapes;
				for ( var i = 0; i < lays.length; i++ ) {
					var t = lays[ i ].split( ":" );
					var page = {};
					if ( t.length == 2 ) {
						page.xf_id = t[ 0 ];
						page.layout = t[ 1 ];
					}
					if ( t.length == 1 ) {
						page.xf_id = t[ 0 ];
						page.layout = "single";
					}
					page.id = "Page";
					page.title = tr( "data." + entityname + "." + page.xf_id );
					childShapes.push( page );
				}
			} catch ( e ) {
				console.log( "catch:" + e );
				return defLayout;
			}
			return formLayout;
		}
	},

	getSpec: function() {
		return this._form;
	},
	toString: function() {
		return "CrudForm:" + this._fields;
	}

} );
