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
can.Construct.extend( "simpl4.util.SearchFilter", {
	createSearchFilter: function( entityname ) {
		var f1 = this._getSearchFilterFieldSets( entityname );
		var f2 = this._getSearchFilterFields( entityname );
		var fields = f1.concat( f2 );

		var ret = [];
		for ( var i = 0; i < fields.length; i++ ) {
			var f = fields[ i ];
			var node = {};
			node.id = f.itemval;
			node.label = f.text;
			node.type = f.type;
			node.datatype = f.datatype;
			node.edittype = f.edittype;
			node.constraints = f.constraints;
			node.dataValues = f.dataValues;
			node.input = f.input;
			node.operators = this._adaptOps( f.ops );
			ret.push( node );
		}
		return ret;
	},
	createSearchFilterWithChilds: function( entityname ) {
		var f1 = this._getSearchFilterFieldSets( entityname );
		var fsids = [];
		jQuery.each( f1, function( e ) {
			fsids.push( e.itemval );
		} );
		var f2 = this._getSearchFilterFields( entityname );
		var fields = f1.concat( f2 );

		var moduleList = simpl4.util.EntityManager.getEntity( entityname );
		if ( moduleList && moduleList.childs != null ) {
			for ( var j = 0; j < moduleList.childs.length; j++ ) {
				var child = moduleList.childs[ j ];
				child.label = tr( "data." + moduleList.name + "." + child.name );
				var fs = this._getSearchFilterFieldSets( child.modulename );
				var ff = this._getSearchFilterFields( child.modulename );
				child.fields = fs.concat( ff );
			}
		}

		var root = {};
		root.id = "root";
		root.label = "root";
		root.children = [];
		for ( var i = 0; i < fields.length; i++ ) {
			var f = fields[ i ];
			f.module = "";
			var node = {};

			if ( fsids.indexOf( f.itemval ) == -1 ) {
				node.id = entityname + "." + f.itemval;
				f.itemval = node.id;
			} else {
				node.id = f.itemval;
			}
			node.label = f.text;
			node.module = "";
			node.moduleTitle = "";
			node.children = [];
			root.children.push( node );
		}
		for ( var i = 0;
			( moduleList && moduleList.childs != null ) && i < moduleList.childs.length; i++ ) {
			var child = moduleList.childs[ i ];
			if ( child.name == entityname ) continue;
			var node = {};
			node.id = child.name;
			node.label = child.title;
			node.children = [];
			node.selectable = false;
			root.children.push( node );
			var fchildren = node.children;
			for ( var j = 0; j < child.fields.length; j++ ) {
				var field = child.fields[ j ];
				field.module = child.name;
				fields.push( field );
				var fnode = {};
				fnode.id = entityname + "$" + child.name + "." + field.itemval;
				field.itemval = fnode.id;
				fnode.label = field.text;
				fnode.module = child.name;
				fnode.moduleTitle = child.title;
				fnode.children = [];
				fchildren.push( fnode );
			}
		}
		return {
			filter: root,
			fields: fields
		};
	},
	_getSearchFilterFieldSets: function( entityname ) {
		var allops = [ 'eq', 'ne', 'lt', 'le', 'gt', 'ge', 'bw', 'bn', 'in', 'inn', 'ew', 'en', 'cn', 'nc' ];
		var odata = tr( "meta.lists.odata" );
		odata = odata.replace( /'/g, '"' );
		try {
			odata = JSON.parse( odata );
		} catch ( e ) {}
		try {
			var list = simpl4.util.EntityManager.getFieldsetsForEntity( entityname );
			var fields = [];
			for ( var i = 0; i < list.length; i++ ) {
				var o = list[ i ];
				var field = {};

				if ( !o.fields || o.fields.length == 0 ) {
					continue;
				}

				field.text = tr( o.fsname );
				field.itemval = o.fsname;
				fields.push( field );
				var sopt = o.search_options; //eval(o.sopt);
				if ( !sopt || sopt.length == 0 ) {
					sopt = [ "cn", "bw", "eq", "ne" ];
				}
				var ops = [];
				jQuery.each( sopt, function( index, so ) {
					var pos = -1;
					if ( ( pos = jQuery.inArray( so, allops ) ) != -1 ) {
						ops.push( {
							op: so,
							text: odata[ pos ]
						} );
					}
				} );
				field.ops = ops;
			}
			return fields;
		} catch ( e ) {
			return []
		}
	},
	_getSearchFilterFields: function( entityname ) {
		var allops = [ 'eq', 'ne', 'lt', 'le', 'gt', 'ge', 'bw', 'bn', 'in', 'inn', 'ew', 'en', 'cn', 'nc' ];
		var odata = tr( "meta.lists.odata" );
		odata = odata.replace( /'/g, '"' );
		try {
			odata = JSON.parse( odata );
		} catch ( e ) {}

		var model = simpl4.util.EntityManager.getEntityViewFields( entityname, "search" );
		if ( model == undefined ) return [];
		var category = "data";

		var cols = model;
		var fields = [];
		for ( var i = 0; i < cols.length; i++ ) {
			var col = cols[ i ];
			if ( col.hidden !== true && col.edittype != "relatedto" ) {
				var field = {};
				field.text = tr( category + "." + entityname + "." + col.name );
				field.itemval = col.name;

				var sopt = this._setDefaultSearchOptions( col );
				sopt = eval( this._checkvalue( sopt ) );
				var ops = [];
				jQuery.each( sopt, function( index, so ) {
					var pos = -1;
					if ( ( pos = jQuery.inArray( so, allops ) ) != -1 ) {
						ops.push( {
							op: so,
							text: odata[ pos ]
						} );
					}
				} );
				var hasSearchables = false;
				if ( col.searchable_items ) {
					field.dataValues = col.searchable_items.getItems();
					hasSearchables = true;
				}
				if ( !field.dataValues ) {
					if ( col.selectable_items ) {
						field.dataValues = col.selectable_items.getItems();
					}
				}

				field.ops = ops;
				field.edittype = col.edittype;
				field.datatype = col.datatype;
				field.constraints = col.constraints;
				field.type = "string";
				if ( col.datatype && col.datatype == 'date' ) {
					field.type = col.edittype == "text" ? "date" : col.edittype;
				} else if ( col.datatype && ( col.datatype == 'decimal' || col.datatype == 'double' ) ) {
					field.type = "double";
				} else if ( col.datatype && ( col.datatype == 'number' || col.datatype == 'integer' || col.datatype == 'long' ) ) {
					field.type = "integer";
				}
				if ( field.type === "text" ) {
					field.type = "string";
				}
				if ( field.type === "boolean" ) {
					field.type = "integer";
				}
				field.input = simpl4.util.FormManager.handleQueryBuilderSearchInput.bind( simpl4.util.FormManager );
				fields.push( field );
			}
		}
		return fields;
	},

	_adaptOps: function( list ) {
		var ret = [];
		var self = this;
		list.forEach( function( e ) {
			ret.push( self.mapOpFromSimpl4( e.op ) );
		} );
		return ret;
	},
	mapOpFromSimpl4: function( op_in ) {
		var map = {
			'eq': 'equal',
			'ne': 'not_equal',
			'in': 'in',
			'inn': 'not_in',
			'lt': 'less',
			'le': 'less_or_equal',
			'gt': 'greater',
			'ge': 'greater_or_equal',
			'bw': 'begins_with',
			'bn': 'not_begins_with',
			'cn': 'contains',
			'ew': 'ends_with',
			'en': 'not_ends_with',
			'empty': 'is_empty',
			'not_empty': 'is_not_empty'
		}
		if ( map[ op_in ] === undefined ) {
			console.error( "SearchFilter.mapOpFromSimpl4 not found:" + op_in );
		}
		return map[ op_in ];
	},
	mapOpToSimpl4: function( op_in ) {
		var map = {
			'equal': 'eq',
			'not_equal': 'ne',
			'in': 'in ',
			'not_in': 'inn',
			'less': 'lt',
			'less_or_equal': 'le',
			'greater': 'gt',
			'greater_or_equal': 'ge',
			'begins_with': 'bw',
			'not_begins_with': 'bn',
			'contains': 'cn',
			'ends_with': 'ew',
			'not_ends_with': 'en',
			'is_empty': 'empty',
			'is_not_empty': 'not_empty'
		}
		if ( map[ op_in ] === undefined ) {
			console.error( "SearchFilter.mapOpToSimpl4 not found:" + op_in );
		}
		return map[ op_in ];
	},
	_setDefaultSearchOptions: function( col ) {
		if ( col.search_options ) return col.search_options;
		var search_options;
		if ( col.datatype == 'date' || col.datatype == 'integer' || col.datatype == 'long' || col.id == 'id' || col.datatype == 'number' || col.datatype == 'decimal' || col.datatype == 'double' ) {
			search_options = [ "gt", "lt", "eq" ];
		} else if ( col.selectable_items || col.edittype == "select" || col.edittype == "checkbox" || col.datatype == 'boolean' ) {
			search_options = [ "eq", "ne" ];
		} else {
			search_options = [ "cn", "bw", "eq", "ne" ];
		}
		return search_options;
	},
	_checkvalue: function( val ) {
		if ( typeof val == 'string' && !val.match( "^\\[" ) ) {
			var values = val.split( "," );
			var ret = [];
			for ( var i = 0; i < values.length; i++ ) {
				ret.push( values[ i ] );
			}
			return ret;
		}
		return val;
	}

}, {} );
