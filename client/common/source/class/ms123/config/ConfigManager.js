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
/**
	* @ignore($)
	* @ignore(jQuery)
	* @ignore(jQuery.each)
*/
qx.Class.define("ms123.config.ConfigManager", {
	extend: qx.core.Object,
	include: qx.locale.MTranslation,


	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function () {},


	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},


	/******************************************************************************
	 STATICS
	 ******************************************************************************/
	statics: {
		__messagesinstalled: {},
		__fieldcache: {},
		__settingcache: {},
		__entitycache: {},
		__categorymap: {},
		__userProperties:{},
		__branding:{},
		__language:"de",
		__STATE_FIELD: "_state",
		clearCache: function () {
			ms123.config.ConfigManager.__fieldcache = {};
			ms123.config.ConfigManager.__entitycache = {};
			ms123.config.ConfigManager.__settingcache = {};
		},
		setUserProperties:function(up){
			ms123.config.ConfigManager.__userProperties = up;
		},
		getUserProperties:function(){
			return ms123.config.ConfigManager.__userProperties;
		},
		isAdmin:function(){
			return ms123.config.ConfigManager.__userProperties.admin;
		},
		setBranding:function(up){
			ms123.config.ConfigManager.__branding = up;
		},
		setLanguage:function(lang){
			ms123.config.ConfigManager.__language = lang;
		},
		getLanguage:function(){
			return ms123.config.ConfigManager.__language;
		},
		getSystemUsage:function(){
			return ms123.config.ConfigManager.__branding["usage"];
		},
		getLoginImage:function(){
			return ms123.config.ConfigManager.__branding["loginImage"];
		},
		isRuntime:function(){
			return ms123.config.ConfigManager.__branding["usage"] == "runtime";
		},
		isTest:function(){
			return ms123.config.ConfigManager.__branding["testing"] === true;
		},
		isVimMode:function(){
			return ms123.config.ConfigManager.__branding["editMode"] == "vim";
		},
		hasGlobalSearch:function(){
			return ms123.config.ConfigManager.__branding["globalSearch"] === true;
		},
		hasTeamEditorSearch:function(){
			return ms123.config.ConfigManager.__branding["teamEditorSearch"] === true;
		},
		hasStateSelect:function(){
			return ms123.config.ConfigManager.__branding["stateSelect"] === true;
		},
		hasDocumentDD:function(){
			return ms123.config.ConfigManager.__branding["documentDD"] === true;
		},
		isDatamapperImport:function(){
			return ms123.config.ConfigManager.__branding["datamapperImport"] === true;
		},
		hasProcessesAndTasks:function(){
			return ms123.config.ConfigManager.__branding["processesAndTasks"] === true;
		},
		isSessionRestore:function(){
			return ms123.config.ConfigManager.__branding["sessionRestore"] === true;
		},
		__getMessages: function (namespace, lang) {
			var failed = function (details) {
				ms123.form.Dialog.alert("getMessagesFailed" + ":" + details.message);
			};

			try {
				var ret = ms123.util.Remote.rpcSync("message:getMessages", {
					namespace: namespace,
					lang: lang
				});
				return ret;
			} catch (e) {
				failed(e);
				return [];
			}
		},
		installMessages: function (ns, lang) {
			if( !lang ){
				lang = ms123.config.ConfigManager.getLanguage();
			}
			var installed = ms123.config.ConfigManager.__messagesinstalled[ns+"-"+lang];
			if( installed ) return;
			var rows = ms123.config.ConfigManager.__getMessages(ns, lang);
			var count = rows.length;
			var m = qx.locale.Manager.getInstance();
			m.setLocale(lang);
			var transMap = {};
			for (var i = 0; i < count; i++) {
				var row = rows[i];
				transMap[row.msgid] = row.msgstr;
			}
			m.addTranslation(lang, transMap);
			ms123.config.ConfigManager.__messagesinstalled[ns+"-"+lang]=true;
		},
		CS_ENTITY: "entity"
	},


	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		getCategory: function (entity) {
			return "data";
		},
/*		getResourceSetting: function (storeDesc,resourceid, settingsid) {
			var namespace = storeDesc.getNamespace();
			var setting = ms123.config.ConfigManager.__settingcache["setting-" + storeDesc.toString()+"-"+settingsid+"-"+resourceid];
			if (!setting) {
				try {
					setting = this.__getResourceSetting(storeDesc, resourceid);
					if( !setting){
						setting = ms123.util.Remote.rpcSync("setting:getResourceSetting", {
							namespace: namespace,
							settingsid: settingsid || "global",
							resourceid: resourceid
						});
					}
					ms123.config.ConfigManager.__settingcache["resource-" + storeDesc.toString()+"-"+settingsid+"-"+resourceid] = setting;
				} catch (e) {
					ms123.form.Dialog.alert("ConfigManager.getResourceSetting:" + e);
					return [];
				}
			}
			return setting;
		},*/
		getFields: function (storeDesc,entity,withAutoGen,withRelations,filter,mapping) {
			var storeId = storeDesc.getStoreId();
			withAutoGen = withAutoGen===true;
			withRelations = withRelations===true;
			var fields = ms123.config.ConfigManager.__fieldcache["fields-" + storeDesc.toString()+"-"+entity+"-"+withAutoGen+"-"+withRelations+"-"+filter+"-"+mapping];
			if (!fields) {
				try {
					fields = ms123.util.Remote.rpcSync("entity:getFields", {
						storeId:storeId,
						withAutoGen:withAutoGen,
						withRelations:withRelations,
						filter: filter,
						mapping: mapping,
						entity: entity
					});
					ms123.config.ConfigManager.__fieldcache["fields-" + storeDesc.toString()+"-"+entity+"-"+withAutoGen+"-"+withRelations+"-"+filter+"-"+mapping] = fields;
				} catch (e) {
					ms123.form.Dialog.alert("ConfigManager.getFields:" + e);
					return [];
				}
			}
			return fields;
		},
		getEntity: function (name, storeDesc) {
			var namespace = storeDesc.getNamespace();
			var entities = ms123.config.ConfigManager.__entitycache["entities-" + storeDesc.toString()];
			if (!entities) {
				entities = this.getEntities(storeDesc);
			}
			if (!entities) {
				ms123.form.Dialog.alert("ConfigManager.getEntity:" + name + "(" + namespace + ") not found");
				return null;
			}
			for (var i = 0; i < entities.length; i++) {
				var entity = entities[i];
				if (entity.name == name) {
					return entity;
				}
			}
			//Lookup in childs
			for (var i = 0; i < entities.length; i++) {
				var entity = entities[i];
				for(var j=0; entity.childs && j < entity.childs.length;j++){
					var child = entity.childs[j];
					if (child.modulename == name) {
						return child;
					}
				}
			}
			return null;
		},
		getEntityModel: function (entity, storeDesc, view, props) {
			var _this = this;
			var cm = new ms123.config.ConfigManager();
			var model = {
				attr: function (what) {
					if (what == "gridProps") {
						if (typeof props == "string") {
							return _this.getEntityViewProperties(entity, storeDesc, view, props);
						} else {
							return props;
						}
					}
					if (what == "colModel") {
						return _this.getEntityViewFields(entity, storeDesc, view);
					}
				}
			}
			return model;
		},
		getUser: function () {
			var user = ms123.config.ConfigManager.__entitycache["user"];
			if (!user) {
				user = ms123.util.Remote.rpcSync("auth:getUserProperties");
				ms123.config.ConfigManager.__entitycache["user"] = user;
			} else {}
			return user;
		},
		getEntityAccess: function (sdesc, entity) {
			var user = this.getUser();
			var entityDesc = this.getEntity(entity,sdesc);
			var access ={
				"update" : "all",
				"import" : "all",
				"delete" : "all"
			};
			if( entityDesc && entityDesc.write === false){
				access["update"] = "no";
				access["import"] = "no";
				access["delete"] = "no";
			}
			return access;
		},
		getEntities: function (storeDesc) {
			var storeId = storeDesc.getStoreId();
			var entities = ms123.config.ConfigManager.__entitycache["entities-" + storeDesc.toString()];
			if (!entities) {
				try {
					entities = ms123.util.Remote.rpcSync("entity:getEntities", {
						storeId: storeId
					});
					ms123.config.ConfigManager.__entitycache["entities-" + storeDesc.toString()] = entities;
				} catch (e) {
					ms123.form.Dialog.alert("ConfigManager.getEntities:" + e);
					return [];
				}
			}
			return entities;
		},
		getEntityTree: function (storeDesc,main,maxlevel,listResolved,pathid,type) {
			var storeId = storeDesc.getStoreId();
			listResolved = listResolved===true;
			pathid = pathid===true;
			var tree = ms123.config.ConfigManager.__entitycache["entitytree-" + main+"-"+maxlevel+"-"+storeDesc.toString()+"-"+listResolved+"-"+pathid+"-"+type];
			if (!tree) {
				try {
					tree = ms123.util.Remote.rpcSync("entity:getEntityTree", {
						storeId: storeDesc.getStoreId(),
						main: main,
						listResolved:listResolved,
						pathid:pathid,
						type:type,
						maxlevel:maxlevel 
					});
					ms123.config.ConfigManager.__entitycache["entitytree-" + main+"-"+maxlevel+"-"+storeDesc.toString()+"-"+listResolved+"-"+pathid+"-"+type] = tree;
				} catch (e) {
					ms123.form.Dialog.alert("ConfigManager.getEntityTree:" + e);
					return [];
				}
			}
			return ms123.util.Clone.clone(tree);
		},
		getEntityConfig: function (entity, storeDesc) {
			var cm = new ms123.config.ConfigManager();
			return this.getEntityViewProperties(entity, storeDesc);
		},

		getEntityViewProperties: function (entity, storeDesc, view) {
			if (!storeDesc) console.trace();
			var namespace = storeDesc.getNamespace();
			if (!namespace) console.trace();
			var props = ms123.config.ConfigManager.__fieldcache["mvcf-" + entity + "-" + storeDesc.toString() + "-" + view ];
			if (!props) {
				try {
					props = this.__getEntityViewProperties(storeDesc, entity, view);
					if( !props ){
						props = ms123.util.Remote.rpcSync("setting:getPropertiesForEntityView", {
							settingsid: "global",
							namespace: namespace,
							entity: entity,
							view: view
						});
					}
					if (props == null) props = {};
					ms123.config.ConfigManager.__fieldcache["mvcf-" + entity + "-" + storeDesc.toString() + "-" + view ] = props;
				} catch (e) {
					ms123.form.Dialog.alert("ConfigManager.getEntityViewProperties:" + e);
				}
			}
			return props;
		},
		getFieldsetsForEntity: function (sdesc, entity) {
			var fieldsets = ms123.config.ConfigManager.__fieldcache["fsetf-" + sdesc.toString()+"-"+entity];
			if (fieldsets === undefined) {
				fieldsets = this.__getFieldsetsForEntity(sdesc,entity);
				if( !fieldsets){
					fieldsets = ms123.util.Remote.rpcSync("setting:getFieldsetsForEntity", {
						namespace: sdesc.getNamespace(),
						settingsid: "global",
						storeId: sdesc.getStoreId(),
						entity: entity
					});
				}
				ms123.config.ConfigManager.__fieldcache["fsetf-" + sdesc.toString()+"-"+entity] = fieldsets;
			}
			return fieldsets;
		},
		getPropertiesForEntity: function (sdesc, entity) {
			var properties = ms123.config.ConfigManager.__fieldcache["eprops-" + sdesc.toString()+"-"+entity];
			if (properties === undefined) {
				properties = this.__getPropertiesForEntity(sdesc,entity);
				if( !properties){
					properties = ms123.util.Remote.rpcSync("setting:getPropertiesForEntityView", {
						namespace: sdesc.getNamespace(),
						settingsid: "global",
						view: null,
						entity: entity
					});
				}
				ms123.config.ConfigManager.__fieldcache["eprops-" + sdesc.toString()+"-"+entity] = properties;
			}
			return properties;
		},
		getEntityViewFields: function (entity, storeDesc, view,build) {
			var buildstr = build!==false ? "true" : "no";
			if (!storeDesc) console.trace();
			var namespace = storeDesc.getNamespace();
			if (!namespace) console.trace();
			var fields = ms123.config.ConfigManager.__fieldcache["vf-" + entity + "-" + storeDesc.toString() + "-" + view+"-"+buildstr];
			if (fields===undefined) {
				try {
					fields = this.__getEntityViewFields(storeDesc, entity, view);
					if( !fields){
						fields = ms123.util.Remote.rpcSync("setting:getFieldsForEntityView", {
							settingsid: "global",
							namespace: namespace,
							entity: entity,
							view: view
						});
					}
				} catch (e) {
					ms123.form.Dialog.alert("ConfigManager.getEntityViewFields:" + e);
				}
				if( build!==false ){
					fields = this.buildColModel(fields, entity, storeDesc, this.getCategory(entity), view);
				}
				ms123.config.ConfigManager.__fieldcache["vf-" + entity + "-" + storeDesc.toString() + "-" + view+"-"+buildstr] = fields;
			} 
			return fields;
		},
		__getEntityViewFields:function(sdesc, entity,view){
			view = view || "all";
			var settings = this.getAllSettingsForEntityList(sdesc,entity);
			return settings.viewFields[view];
		},
		__getEntityViewProperties:function(sdesc, entity,view){
			view = view || "all";
			var settings = this.getAllSettingsForEntityList(sdesc,entity);
			return settings.viewProps[view];
		},
		__getFieldsetsForEntity:function(sdesc, entity){
			var settings = this.getAllSettingsForEntityList(sdesc,entity);
			return settings.fieldSets;
		},
		__getPropertiesForEntity:function(sdesc, entity){
			var settings = this.getAllSettingsForEntityList(sdesc,entity);
			return settings.properties;
		},
		getAllSettingsForEntityList: function (sdesc, entityList) {
			if( !Array.isArray(entityList)){
				entityList = [entityList];
			}
			var ok = true;
			for(var i=0; i< entityList.length; i++){
				var e = entityList[i];
				if( !ms123.config.ConfigManager.__fieldcache["as-" + sdesc.toString()+"-"+e] ){
					ok = false;
					break;
				}
			}
			if(!ok){
				var settingList = ms123.util.Remote.rpcSync("setting:getAllSettingsForEntityList", {
					namespace: sdesc.getNamespace(),
					settingsid: "global",
					entities: entityList
				});
				for(var i=0; i< entityList.length; i++){
					var entity = entityList[i];
					var setting = settingList[i];	
					ms123.config.ConfigManager.__fieldcache["as-" + sdesc.toString()+"-"+entity] = setting;
				}
			}
			return ms123.config.ConfigManager.__fieldcache["as-" + sdesc.toString()+"-"+entityList[0]];
		},

		getEntityPermittedFields: function (sdesc, entity) {
			var fields = ms123.config.ConfigManager.__fieldcache["af-" + entity];
			if (fields===undefined) {
				fields = ms123.util.Remote.rpcSync("entity:getPermittedFields", {
					storeId: sdesc.getStoreId(),
					entity: entity
				});
				ms123.config.ConfigManager.__fieldcache["af-" + entity] = fields;
			}
			return fields;
		},
		getIdField: function (sdesc, entity) {
			var field = ms123.config.ConfigManager.__fieldcache["if-" + entity];
			if (field===undefined) {
				field = ms123.util.Remote.rpcSync("entity:getIdField", {
					storeId: sdesc.getStoreId(),
					entity: entity
				});
				ms123.config.ConfigManager.__fieldcache["if-" + entity] = field;
			}
			return field;
		},

		getStencilSet: function (name) {
			var stencilset = ms123.config.ConfigManager.__fieldcache["stencilset-" + name];
			if (stencilset===undefined) {
				try {
					stencilset = ms123.util.Remote.rpcSync("stencil:getStencilSet", {
						namespace:ms123.StoreDesc.getCurrentNamespace(),
						name: name
					});
					stencilset = qx.lang.Json.parse(stencilset);
				} catch (e) {
					ms123.form.Dialog.alert("ConfigManager.getStencilSet:" + e);
				}
				if (stencilset == null) stencilset = {};
				//@@@MS not caching for dev ms123.config.ConfigManager.__fieldcache["stencilset-" + name] = stencilset;
			}
			var ret = ms123.util.Clone.clone(stencilset);
			return ret;
		},

		_hasSelectableItems:function(si){
			if( si == undefined || si == null){
				return false;
			}
			if ((typeof si == 'string') && si == "") {
				return false;
			}
			if ((typeof si == 'string') && si.match(/^{/)) {
				si = qx.lang.Json.parse(si);
			}
			if (si.totalCount != null && si.totalCount == 0) {
				return false;
			}
			return true;
		},
		createSelectableItems: function (storeDesc, url, varMap, entity, name, view) {
			return new ms123.SelectableItems({ url: url, varMap: varMap,storeDesc:storeDesc });
		},

		buildModel: function (columns, props) {
			var model = {
				attr: function (what) {
					if (what == "gridProps") {
						return props;
					}
					if (what == "colModel") {
						return columns;
					}
				}
			}
			return model;
		},
		buildColModel: function (gridfields, entity, storeDesc, category, view) {
			if (gridfields == null) {
				return;
			}
			var primKey = null;
			var hasIdField = false;
			jQuery.each(gridfields, function (k, v) {
				if (v["primary_key"]) {
					primKey = v["name"];
				}
				if (v["id"] == "id") {
					hasIdField = true;
				}
			});
			var colModel = [];

			if (!(view && (view.match(/duplicate/)))) {
				if (!hasIdField && primKey == null) {
					var col = {};
					col.name = "id";
					col.id = "id";
					col.hidden = true;
					col.search_options = ["bw", "cn", "eq"];
					colModel.push(col);
				}
			}

			for (var ci = 0; ci < gridfields.length; ci++) {
				var col = {};
				var gridfield = gridfields[ci];
				col.name = col.id = gridfield.name;
				if( col.name == ms123.config.ConfigManager.__STATE_FIELD && !ms123.config.ConfigManager.isAdmin() ) continue;
				col.editable = true;
				col.edittype = gridfield.edittype;
				col.readonly = gridfield.readonly;
				col.datatype = gridfield.datatype || "string";
				if (col.datatype.match("^related")) {
					col.edittype = "relatedto";
				}


				if (col.edittype == null || col.edittype == '') {
					if (col.datatype == "boolean") {
						col.edittype = "checkbox";
					} else if (col.datatype == "binary") {
						col.edittype = "upload";
					} else if (col.datatype == "string" && col.selectable_items) {
						col.edittype = "select";
					} else {
						col.edittype = "text";
					}
				}
				if( gridfield.name.match(/^_/)){
					col.label = gridfield.label || this.tr(category + "." + gridfield.name);
				}else{
					col.label = gridfield.label || this.tr(category + "." + entity + "." + gridfield.name);
				}

				for (var prop in gridfield) {
					var val = gridfield[prop];
					if (prop != "name" /*&& prop != "constraints"*/ && prop != "formula_in" && prop != "formula_out" && prop != "datatype" && prop != "id" && !prop.match("^jcr:") && prop != "edittype" && prop != "tab") {
						if (val != null) {
							col[prop] = val;
						}
					}
				}

				if (view && (view.match(/form/) || view.match(/search/) || view.match(/grid/))) {
					if (gridfield["tab"] == null) {
						gridfield["tab"] = "tab1";
					}
					col.formoptions = {
						tab: gridfield["tab"]
					};
					var varMap = {};
					if (this._hasSelectableItems(col.selectable_items)) {
						col.selectable_items = this.createSelectableItems(storeDesc, col.selectable_items, varMap, entity, col.name, view);
					}else{
						col.selectable_items = null;
					}
					if (this._hasSelectableItems(col.searchable_items)){
					 col.searchable_items = this.createSelectableItems(storeDesc, col.searchable_items, varMap, entity, col.name, view);
					}else{
						col.searchable_items = null;
					}
				}

				if (primKey && primKey == col.name) {
					col.key = true;
				}
				colModel.push(col);
			}
			return colModel;
		}
	}
});
