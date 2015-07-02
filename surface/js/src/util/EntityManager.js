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
simpl4.util.BaseManager.extend( "simpl4.util.EntityManager", {
	entityCache: {},
	fieldCache: {},
	clearCache: function() {
		simpl4.util.EntityManager.fieldCache = {};
		simpl4.util.EntityManager.entityCache = {};
	},
	getFields: function( entity, withAutoGen, withRelations, filter, mapping ) {
		var args = Args( [ {
			entity: Args.STRING | Args.Required
		}, {
			withAutoGen: Args.BOOLEAN | Args.Optional
		}, {
			withRelations: Args.BOOLEAN | Args.Optional
		}, {
			filter: Args.STRING | Args.Optional
		}, {
			mapping: Args.STRING | Args.Optional
		}, {
			namespace: Args.STRING | Args.Optional
		} ], arguments );

		var namespace = args.namespace || simpl4.util.BaseManager.getNamespace();
		var withAutoGen = args.withAutoGen === true;
		var withRelations = args.withRelations === true;
		var fields = simpl4.util.EntityManager.fieldCache[ "fields-" + storeId + "-" + args.entity + "-" + withAutoGen + "-" + withRelations + "-" + args.filter + "-" + args.mapping ];
		if ( !fields ) {
			try {
				fields = simpl4.util.Rpc.rpcSync( "entity:getFields", {
					storeId: namespace + "_data",
					withAutoGen: withAutoGen,
					withRelations: withRelations,
					filter: args.filter,
					mapping: args.mapping,
					entity: args.entity
				} );
				simpl4.util.EntityManager.fieldCache[ "fields-" + storeId + "-" + args.entity + "-" + withAutoGen + "-" + withRelations + "-" + args.filter + "-" + args.mapping ] = fields;
			} catch ( e ) {
				this._error( "EntityManager.getFields", e );
				return [];
			}
		}
		return fields;
	},

	getUser: function() {
		var user = simpl4.util.EntityManager.entityCache[ "user" ];
		if ( !user ) {
			user = simpl4.util.Rpc.rpcSync( "auth:getUserProperties" );
			simpl4.util.EntityManager.entityCache[ "user" ] = user;
		} else {}
		return user;
	},
	getEntityAccess: function( entity ) {
		var args = Args( [ {
			antity: Args.STRING | Args.Required
		}, {
			namespace: Args.STRING | Args.Optional
		} ], arguments );
		var namespace = args.namespace || this.getNamespace();
		var user = this.getUser();
		var entityDesc = this.getEntity( entity, {
			namespace: namespace
		} );
		var access = {
			"update": "all",
			"import": "all",
			"delete": "all"
		};
		if ( entityDesc && entityDesc.write === false ) {
			access[ "update" ] = "no";
			access[ "import" ] = "no";
			access[ "delete" ] = "no";
		}
		return access;
	},

	hasEntityUpdatePermission: function( entity, namespace ) {
		var access = this.getEntityAccess( entity, namespace );
		console.log( "access:", access );
		var user = this.getUser();
		console.log( "user:", user );
		if ( user.admin ) return true;
		if ( access[ "update" ] === undefined || access[ "update" ] == "all" || access[ "update" ] == "owner" ) return true;
		return false;
	},
	getEntities: function() {
		var args = Args( [ {
			namespace: Args.STRING | Args.Optional
		} ], arguments );

		var namespace = args.namespace || simpl4.util.BaseManager.getNamespace();
		var entities = simpl4.util.EntityManager.entityCache[ "entities-" + namespace ];
		if ( !entities ) {
			try {
				entities = simpl4.util.Rpc.rpcSync( "entity:getEntities", {
					storeId: namespace + "_data"
				} );
				simpl4.util.EntityManager.entityCache[ "entities-" + namespace ] = entities;
			} catch ( e ) {
				this._error( "EntityManager.getEntities", e );
				return [];
			}
		}
		return entities;
	},
	getEntity: function( name ) {
		var args = Args( [ {
			name: Args.STRING | Args.Required
		}, {
			namespace: Args.STRING | Args.Optional
		} ], arguments );

		var namespace = args.namespace || this.getNamespace();
		var entities = simpl4.util.EntityManager.entityCache[ "entities-" + namespace ];
		if ( !entities ) {
			entities = this.getEntities( namespace );
		}
		if ( !entities ) {
			this._error( "EntityManager.getEntity:" + args.name + "(" + namespace + ") not found" );
			return null;
		}
		for ( var i = 0; i < entities.length; i++ ) {
			var entity = entities[ i ];
			if ( entity.name == args.name ) {
				return entity;
			}
		}
		//Lookup in childs
		for ( var i = 0; i < entities.length; i++ ) {
			var entity = entities[ i ];
			for ( var j = 0; entity.childs && j < entity.childs.length; j++ ) {
				var child = entity.childs[ j ];
				if ( child.modulename == args.name ) {
					return child;
				}
			}
		}
		return null;
	},


	getEntityViewProperties: function( entity, view ) {
		var args = Args( [ {
			entity: Args.STRING | Args.Required
		}, {
			view: Args.STRING | Args.Required
		}, {
			namespace: Args.STRING | Args.Optional
		} ], arguments );

		var namespace = args.namespace || simpl4.util.BaseManager.getNamespace();
		var props = simpl4.util.EntityManager.fieldCache[ "mvcf-" + args.entity + "-" + namespace + "-" + args.view ];
		if ( !props ) {
			try {
				props = this.__getEntityViewProperties( namespace, args.entity, args.view );
				if ( !props ) {
					props = simpl4.util.Rpc.rpcSync( "setting:getPropertiesForEntityView", {
						settingsid: "global",
						namespace: namespace,
						entity: args.entity,
						view: args.view
					} );
				}
				if ( props == null ) props = {};
				simpl4.util.EntityManager.fieldCache[ "mvcf-" + args.entity + "-" + namespace + "-" + args.view ] = props;
			} catch ( e ) {
				this._error( "EntityManager.getEntityViewProperties", e );
			}
		}
		return props;
	},
	getFieldsetsForEntity: function( entity ) {
		var args = Args( [ {
			entity: Args.STRING | Args.Required
		}, {
			namespace: Args.STRING | Args.Optional
		} ], arguments );

		var namespace = args.namespace || simpl4.util.BaseManager.getNamespace();
		var fieldsets = simpl4.util.EntityManager.fieldCache[ "fsetf-" + namespace + "-" + args.entity ];
		if ( fieldsets === undefined ) {
			fieldsets = this.__getFieldsetsForEntity( namespace, args.entity );
			if ( !fieldsets ) {
				fieldsets = simpl4.util.Rpc.rpcSync( "setting:getFieldsetsForEntity", {
					namespace: namespace,
					settingsid: "global",
					entity: args.entity
				} );
			}
			simpl4.util.EntityManager.fieldCache[ "fsetf-" + namespace + "-" + args.entity ] = fieldsets;
		}
		return fieldsets;
	},
	getPropertiesForEntity: function( entity ) {
		var args = Args( [ {
			entity: Args.STRING | Args.Required
		}, {
			namespace: Args.STRING | Args.Optional
		} ], arguments );

		var namespace = args.namespace || simpl4.util.BaseManager.getNamespace();
		var properties = simpl4.util.EntityManager.fieldCache[ "eprops-" + namespace + "-" + args.entity ];
		if ( properties === undefined ) {
			properties = this.__getPropertiesForEntity( namespace, args.entity );
			if ( !properties ) {
				properties = simpl4.util.Rpc.rpcSync( "setting:getPropertiesForEntityView", {
					namespace: namespace,
					settingsid: "global",
					view: null,
					entity: args.entity
				} );
			}
			simpl4.util.EntityManager.fieldCache[ "eprops-" + namespace + "-" + args.entity ] = properties;
		}
		return properties;
	},

	getEntityViewFields: function( entity, view, build ) {
		var args = Args( [ {
			entity: Args.STRING | Args.Required
		}, {
			namespace: Args.STRING | Args.Optional
		}, {
			view: Args.STRING | Args.Required
		}, {
			build: Args.BOOLEAN | Args.Optional
		} ], arguments );

		var buildstr = build !== false ? "true" : "no";
		var namespace = args.namespace || this.getNamespace();
		console.error( "namespace:" + namespace + "/" + args.view );
		var fields = simpl4.util.EntityManager.fieldCache[ "vf-" + args.entity + "-" + namespace + "-" + args.view + "-" + buildstr ];
		if ( fields === undefined ) {
			try {
				fields = this.__getEntityViewFields( namespace, args.entity, args.view );
				if ( !fields ) {
					fields = simpl4.util.Rpc.rpcSync( "setting:getFieldsForEntityView", {
						settingsid: "global",
						namespace: namespace,
						entity: args.entity,
						view: args.view
					} );
				}
			} catch ( e ) {
				console.log( "e;", e );
				this._error( "EntityManager.getEntityViewFields", e );
			}
			if ( build !== false ) {
				fields = this.buildColModel( fields, namespace, args.entity, args.view );
			}
			simpl4.util.EntityManager.fieldCache[ "vf-" + args.entity + "-" + namespace + "-" + args.view + "-" + buildstr ] = fields;
		}
		return fields;
	},
	__getEntityViewFields: function( namespace, entity, view ) {
		view = view || "all";
		var settings = this.getAllSettingsForEntityList( namespace, entity );
		return settings.viewFields[ view ];
	},
	__getEntityViewProperties: function( namespace, entity, view ) {
		view = view || "all";
		var settings = this.getAllSettingsForEntityList( namespace, entity );
		return settings.viewProps[ view ];
	},
	__getFieldsetsForEntity: function( namespace, entity ) {
		var settings = this.getAllSettingsForEntityList( namespace, entity );
		return settings.fieldSets;
	},
	__getPropertiesForEntity: function( namespace, entity ) {
		var settings = this.getAllSettingsForEntityList( namespace, entity );
		return settings.properties;
	},
	getAllSettingsForEntityList: function( namespace, entityList ) {
		console.log( "namespace:" + namespace );
		if ( !Array.isArray( entityList ) ) {
			entityList = [ entityList ];
		}
		var ok = true;
		for ( var i = 0; i < entityList.length; i++ ) {
			var e = entityList[ i ];
			if ( !simpl4.util.EntityManager.fieldCache[ "as-" + namespace + "-" + e ] ) {
				ok = false;
				break;
			}
		}
		if ( !ok ) {
			var settingList = simpl4.util.Rpc.rpcSync( "setting:getAllSettingsForEntityList", {
				namespace: namespace || this.getNamespace(),
				settingsid: "global",
				entities: entityList
			} );
			for ( var i = 0; i < entityList.length; i++ ) {
				var entity = entityList[ i ];
				var setting = settingList[ i ];
				simpl4.util.EntityManager.fieldCache[ "as-" + namespace + "-" + entity ] = setting;
			}
		}
		return simpl4.util.EntityManager.fieldCache[ "as-" + namespace + "-" + entityList[ 0 ] ];
	},

	getEntityPermittedFields: function( entity ) {
		var args = Args( [ {
			entity: Args.STRING | Args.Required
		}, {
			namespace: Args.STRING | Args.Optional
		} ], arguments );

		var namespace = args.namespace || simpl4.util.BaseManager.getNamespace();
		var fields = simpl4.util.EntityManager.fieldCache[ "af-" + args.entity ];
		if ( fields === undefined ) {
			fields = simpl4.util.Rpc.rpcSync( "entity:getPermittedFields", {
				storeId: namespace + "_data",
				entity: args.entity
			} );
			simpl4.util.EntityManager.fieldCache[ "af-" + args.entity ] = fields;
		}
		return fields;
	},
	_hasSelectableItems: function( si ) {
		if ( si == undefined || si == null ) {
			return false;
		}
		if ( ( typeof si == 'string' ) && si == "" ) {
			return false;
		}
		if ( ( typeof si == 'string' ) && si.match( /^{/ ) ) {
			si = JSON.parse( si );
		}
		if ( si.totalCount != null && si.totalCount == 0 ) {
			return false;
		}
		return true;
	},
	_error: function( msg, e ) {
		if ( e ) {
			console.error( msg + ":" + ( e.message || e ) );
			alert( msg + ":" + ( e.message || e ) );
		} else {
			console.error( msg );
			alert( msg );
		}
	},
	createSelectableItems: function( url, varMap, namespace, entity, name, view ) {
		return new simpl4.util.SelectableItems( {
			url: url,
			namespace: namespace,
			varMap: varMap
		} );
	},
	buildColModel: function( gridfields, namespace, entity, view ) {
		var category = "data";
		if ( gridfields == null ) {
			return;
		}
		var primKey = null;
		var hasIdField = false;
		jQuery.each( gridfields, function( k, v ) {
			if ( v[ "primary_key" ] ) {
				primKey = v[ "name" ];
			}
			if ( v[ "id" ] == "id" ) {
				hasIdField = true;
			}
		} );
		var colModel = [];

		if ( !( view && ( view.match( /duplicate/ ) ) ) ) {
			if ( !hasIdField && primKey == null ) {
				var col = {};
				col.name = "id";
				col.id = "id";
				col.hidden = true;
				col.search_options = [ "bw", "cn", "eq" ];
				colModel.push( col );
			}
		}

		for ( var ci = 0; ci < gridfields.length; ci++ ) {
			var col = {};
			var gridfield = gridfields[ ci ];
			col.name = col.id = gridfield.name;
			col.editable = true;
			col.edittype = gridfield.edittype;
			col.readonly = gridfield.readonly;
			col.datatype = gridfield.datatype || "string";
			if ( col.datatype.match( "^related" ) ) {
				col.edittype = "relatedto";
			}


			if ( col.edittype == null || col.edittype == '' ) {
				if ( col.datatype == "boolean" ) {
					col.edittype = "checkbox";
				} else if ( col.datatype == "binary" ) {
					col.edittype = "upload";
				} else if ( col.datatype == "string" && col.selectable_items ) {
					col.edittype = "select";
				} else {
					col.edittype = "text";
				}
			}
			if ( gridfield.name.match( /^_/ ) ) {
				col.label = gridfield.label || tr( category + "." + gridfield.name );
			} else {
				col.label = gridfield.label || tr( category + "." + entity + "." + gridfield.name );
			}

			for ( var prop in gridfield ) {
				var val = gridfield[ prop ];
				if ( prop != "name" /*&& prop != "constraints"*/ && prop != "formula_in" && prop != "formula_out" && prop != "datatype" && prop != "id" && !prop.match( "^jcr:" ) && prop != "edittype" && prop != "tab" ) {
					if ( val != null ) {
						col[ prop ] = val;
					}
				}
			}

			if ( view && ( view.match( /form/ ) || view.match( /search/ ) || view.match( /grid/ ) ) ) {
				if ( gridfield[ "tab" ] == null ) {
					gridfield[ "tab" ] = "tab1";
				}
				col.formoptions = {
					tab: gridfield[ "tab" ]
				};
				var varMap = {};
				if ( this._hasSelectableItems( col.selectable_items ) ) {
					col.selectable_items = this.createSelectableItems( col.selectable_items, varMap, namespace, entity, col.name, view );
				} else {
					col.selectable_items = null;
				}
				if ( this._hasSelectableItems( col.searchable_items ) ) {
					col.searchable_items = this.createSelectableItems( col.searchable_items, varMap, namespace, entity, col.name, view );
				} else {
					col.searchable_items = null;
				}
			}

			if ( primKey && primKey == col.name ) {
				col.key = true;
			}
			colModel.push( col );
		}
		return colModel;
	}
}, {} );
