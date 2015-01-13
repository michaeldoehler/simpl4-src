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
	getEntities: function( storeDesc ) {
		var storeId = this.getStoreId();
console.error("storeId:"+storeId);
console.error("storeId:"+simpl4.util.BaseManager.getStoreId());
		var entities = simpl4.util.EntityManager.entityCache[ "entities-" + storeId ];
		if ( !entities ) {
			try {
				entities = simpl4.util.Rpc.rpcSync( "entity:getEntities", {
					storeId: storeId
				} );
				simpl4.util.EntityManager.entityCache[ "entities-" + storeId ] = entities;
			} catch ( e ) {
				alert( "EntityManager.getEntities:" + e );
				return [];
			}
		}
		return entities;
	},
	getEntity: function( name ) {
		var namespace = this.getNamespace();
		var entities = simpl4.util.EntityManager.entityCache[ "entities-" + this.getStoreId() ];
		if ( !entities ) {
			entities = this.getEntities( storeDesc );
		}
		if ( !entities ) {
			//ms123.form.Dialog.alert("ConfigManager.getEntity:" + name + "(" + namespace + ") not found");
			alert( "EntityManager.getEntity:" + name + "(" + namespace + ") not found" );
			return null;
		}
		for ( var i = 0; i < entities.length; i++ ) {
			var entity = entities[ i ];
			if ( entity.name == name ) {
				return entity;
			}
		}
		//Lookup in childs
		for ( var i = 0; i < entities.length; i++ ) {
			var entity = entities[ i ];
			for ( var j = 0; entity.childs && j < entity.childs.length; j++ ) {
				var child = entity.childs[ j ];
				if ( child.modulename == name ) {
					return child;
				}
			}
		}
		return null;
	},
	getEntityViewFields: function( entity, view, build ) {
		var buildstr = build !== false ? "true" : "no";
		var storeId = this.getStoreId();
		var namespace = this.getNamespace();
		var fields = simpl4.util.EntityManager.fieldCache[ "vf-" + entity + "-" + storeId + "-" + view + "-" + buildstr ];
		if ( fields === undefined ) {
			try {
				fields = this.__getEntityViewFields( entity, view );
				if ( !fields ) {
					fields = simpl4.util.Rpc.rpcSync( "setting:getFieldsForEntityView", {
						settingsid: "global",
						namespace: namespace,
						entity: entity,
						view: view
					} );
				}
			} catch ( e ) {
				console.log( e.stack );
				alert( "EntityManager.getEntityViewFields:" + e );
			}
			if ( build !== false ) {
				fields = this.buildColModel( fields, entity, view );
			}
			simpl4.util.EntityManager.fieldCache[ "vf-" + entity + "-" + storeId + "-" + view + "-" + buildstr ] = fields;
		}
		return fields;
	},
	__getEntityViewFields: function( entity, view ) {
		view = view || "all";
		var settings = this.getAllSettingsForEntityList( entity );
		return settings.viewFields[ view ];
	},
	__getEntityViewProperties: function( entity, view ) {
		view = view || "all";
		var settings = this.getAllSettingsForEntityList( entity );
		return settings.viewProps[ view ];
	},
	__getFieldsetsForEntity: function( entity ) {
		var settings = this.getAllSettingsForEntityList( entity );
		return settings.fieldSets;
	},
	__getPropertiesForEntity: function( entity ) {
		var settings = this.getAllSettingsForEntityList( entity );
		return settings.properties;
	},
	getAllSettingsForEntityList: function( entityList ) {
		var storeId = this.getStoreId();
		if ( !Array.isArray( entityList ) ) {
			entityList = [ entityList ];
		}
		var ok = true;
		for ( var i = 0; i < entityList.length; i++ ) {
			var e = entityList[ i ];
			if ( !simpl4.util.EntityManager.fieldCache[ "as-" + storeId + "-" + e ] ) {
				ok = false;
				break;
			}
		}
		if ( !ok ) {
			var settingList = simpl4.util.Rpc.rpcSync( "setting:getAllSettingsForEntityList", {
				namespace: this.getNamespace(),
				settingsid: "global",
				entities: entityList
			} );
			for ( var i = 0; i < entityList.length; i++ ) {
				var entity = entityList[ i ];
				var setting = settingList[ i ];
				simpl4.util.EntityManager.fieldCache[ "as-" + storeId + "-" + entity ] = setting;
			}
		}
		return simpl4.util.EntityManager.fieldCache[ "as-" + storeId + "-" + entityList[ 0 ] ];
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
		createSelectableItems: function (url, varMap, entity, name, view) {
			return new simpl4.util.SelectableItems({ url: url, varMap: varMap });
		},
	buildColModel: function( gridfields, entity, view ) {
		var storeId = this.getStoreId();
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
					col.selectable_items = this.createSelectableItems( col.selectable_items, varMap, entity, col.name, view );
				} else {
					col.selectable_items = null;
				}
				if ( this._hasSelectableItems( col.searchable_items ) ) {
					col.searchable_items = this.createSelectableItems( col.searchable_items, varMap, entity, col.name, view );
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
