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
	@asset(qx/icon/${qx.icontheme}/16/actions/*)
	@asset(qx/icon/${qx.icontheme}/16/apps/*)
	@asset(qx/icon/${qx.icontheme}/48/actions/*)
	@asset(qx/icon/${qx.icontheme}/48/apps/*)
	@ignore(jQuery)
	@ignore(jQuery.each)
	@ignore(jQuery.inArray)
	@ignore(jQuery.parseJSON)
	@lint ignoreDeprecated(alert,eval) 
*/

qx.Mixin.define("ms123.searchfilter.MSearchFilter",
{

	/**
	 *****************************************************************************
	 MEMBERS
	 *****************************************************************************
	 */

	members: {
		_createSearchFilter: function (params,storeDesc) {
			var f1 = this._getSearchFilterFieldSets(params.modulename,storeDesc);
			var f2 = this._getSearchFilterFields(params.modulename,storeDesc);
			var fields = f1.concat(f2);

			var root = {};
			root.id="root";
			root.title="root";
			root.children = [];
			for (var i = 0; i < fields.length; i++) {
				var f = fields[i];
				f.module = "";
				var node = {};
				node.id = f.itemval;
				node.title = f.text;
				node.module = ""; 
				node.moduleTitle = ""; 
				node.children = [];
				root.children.push(node);
			}
			var sf = new ms123.searchfilter.SearchFilter(root,fields, params);
			return sf;
		},
		_createSearchFilterWithChilds:function(entityname,params,sdesc){
			var f1 = this._getSearchFilterFieldSets(entityname, sdesc);
			var fsids = [];
			f1.each(function (e) {
				fsids.push(e.itemval);
			});
			var f2 = this._getSearchFilterFields(entityname, sdesc);
			var fields = f1.concat(f2);

			var cm = new ms123.config.ConfigManager();
			var moduleList = cm.getEntity(entityname, sdesc);
			if (moduleList && moduleList.childs != null) {
				for (var j = 0; j < moduleList.childs.length; j++) {
					var child = moduleList.childs[j];
					child.title = this.tr("data." + moduleList.name + "." + child.name);
					var fs = this._getSearchFilterFieldSets(child.modulename, sdesc);
					var ff = this._getSearchFilterFields(child.modulename, sdesc);
					child.fields = fs.concat(ff);
				}
			}

			var root = {};
			root.id = "root";
			root.title = "root";
			root.children = [];
			for (var i = 0; i < fields.length; i++) {
				var f = fields[i];
				f.module = "";
				var node = {};

				if (fsids.indexOf(f.itemval) == -1) {
					node.id = entityname + "." + f.itemval;
					f.itemval = node.id;
				} else {
					node.id = f.itemval;
				}
				node.title = f.text;
				node.module = "";
				node.moduleTitle = "";
				node.children = [];
				root.children.push(node);
			}
			for (var i = 0; (moduleList && moduleList.childs != null) && i < moduleList.childs.length; i++) {
				var child = moduleList.childs[i];
				if (child.name == entityname) continue; 
				var node = {};
				node.id = child.name;
				node.title = child.title;
				node.children = [];
				node.selectable = false;
				root.children.push(node);
				var fchildren = node.children;
				for (var j = 0; j < child.fields.length; j++) {
					var field = child.fields[j];
					field.module = child.name;
					fields.push(field);
					var fnode = {};
					fnode.id = entityname + "$" + child.name + "." + field.itemval;
					field.itemval = fnode.id;
					fnode.title = field.text;
					fnode.module = child.name;
					fnode.moduleTitle = child.title;
					fnode.children = [];
					fchildren.push(fnode);
				}
			}
			return new ms123.searchfilter.SearchFilter(root, fields, params,true);
		},
		_getSearchFilterFieldSets: function (modulename,storeDesc) {
			var allops = ['eq', 'ne', 'lt', 'le', 'gt', 'ge', 'bw', 'bn', 'in', 'inn', 'ew', 'en', 'cn', 'nc'];
			var odata = this.tr("meta.lists.odata");
			odata = odata.replace(/'/g, '"');
			try{
				odata = qx.lang.Json.parse( odata);
			}catch(e){}
			try {
				var cm = new ms123.config.ConfigManager();
      	var list  = cm.getFieldsetsForEntity(storeDesc,modulename);
				var fields = [];
				for (var i = 0; i < list.length; i++) {
					var o = list[i];
					var field = {};

					if( !o.fields || o.fields.length==0){
						continue;
					}

					field.text = this.tr(o.fsname);
					field.itemval = o.fsname;
					fields.push(field);
					var sopt = o.search_options;//eval(o.sopt);
					if( !sopt || sopt.length==0){
						sopt = ["cn", "bw", "eq", "ne"];
					}
					var ops = [];
					jQuery.each(sopt, function (index, so) {
						var pos = -1;
						if ((pos = jQuery.inArray(so, allops)) != -1) {
							ops.push({
								op: so,
								text: odata[pos]
							});
						}
					});
					field.ops = ops;
				}
				return fields;
			} catch (e) {
				return []
			}
		},
		_getSearchFilterFields: function (modulename,storeDesc) {
			var allops = ['eq', 'ne', 'lt', 'le', 'gt', 'ge', 'bw', 'bn', 'in', 'inn', 'ew', 'en', 'cn', 'nc'];
			var odata = this.tr("meta.lists.odata");
			odata = odata.replace(/'/g, '"');
			try{
			odata = qx.lang.Json.parse( odata);
			}catch(e){}

			var cm = new ms123.config.ConfigManager();
			var model = cm.getEntityViewFields(modulename,storeDesc, "search");
			if( model == undefined) return [];
			var category = cm.getCategory(modulename);
			if( !category) return [];

			var cols = model;
			var fields = [];
			for (var i = 0; i < cols.length; i++) {
				var col = cols[i];
				if (col.hidden !== true && col.edittype != "relatedto") {
					var field = {};
					field.text = this.tr(category + "." + modulename + "." + col.name);
					field.itemval = col.name;

					var sopt = this._setDefaultSearchOptions(col);
					sopt = eval(this._checkvalue(sopt));
					var ops = [];
					jQuery.each(sopt, function (index, so) {
						var pos = -1;
						if ((pos = jQuery.inArray(so, allops)) != -1) {
							ops.push({
								op: so,
								text: odata[pos]
							});
						}
					});
					var hasSearchables = false;
					if (col.searchable_items) {
						field.dataValues = col.searchable_items.getItems();
						hasSearchables = true;
					}
					if( !field.dataValues ){
						if (col.selectable_items) {
							field.dataValues = col.selectable_items.getItems();
						}
					}
					field.ops = ops;
					if (col.datatype && col.datatype == 'date') {
						field.type = col.edittype=="text" ? "date" : col.edittype;
					}else if (col.datatype && (col.datatype == 'decimal' || col.datatype=='double')) {
						field.type = "decimal";
					}else if (col.datatype && (col.datatype == 'number' || col.datatype=='integer' || col.datatype=='long')) {
						field.type = "number";
					} else {
						field.type = hasSearchables ? "select" : col.edittype;
					}
					fields.push(field);
				}
			}
			return fields;
		},
		_setDefaultSearchOptions:function(col){
			if( col.search_options ) return col.search_options;
			var search_options;
			if (col.datatype == 'date' || col.datatype == 'integer' || col.datatype == 'long' || col.id == 'id' || col.datatype=='number' || col.datatype=='decimal' ||col.datatype=='double') {
				search_options = ["gt", "lt", "eq"];
			} else if (col.edittype == "select" || col.edittype == "checkbox" || col.datatype == 'boolean') {
				search_options = ["eq", "ne"];
			} else {
				search_options = ["cn", "bw", "eq", "ne"];
			}
			return search_options;
		},
		_checkvalue:function(val){
			if( typeof val == 'string' && !val.match("^\\[") ){
				var values = val.split(",");
				var ret = [];
				for (var i=0; i < values.length;i++) {
					ret.push(values[i]);
				}
				return ret;
			}
			return val;
		},
		_showErrors: function (details) {
			ms123.form.Dialog.alert(details.message);
		},
		_selectSearchFilter: function (entityName, exclusionListTable,storeDesc, sf) {
			var filter = {
				"connector": "and",
				"children": [{
					"field": "modulename",
					"op": "eq",
					"data": entityName
				},
				{
					"field": "user",
					"op": "eq",
					"data": this._user.userid
				},
				{
					"field": "type",
					"op": "eq",
					"data": ms123.config.ConfigManager.CS_ENTITY
				}]
			};
			var cs = ms123.StoreDesc.getNamespaceConfigStoreDesc();
			var rpcParams = {
				namespace: cs.getNamespace(),
				storeId: cs.getStoreId(),
				pack: cs.getPack(),
				entity: "filter",
				filter: filter,
				orderby: "id"
			}

			var params = {
				service: "data",
				method: "query",
				parameter: rpcParams,
				async: false,
				context: this
			}
			var list = null;
			try {
				list = ms123.util.Remote.rpcAsync(params);
			} catch (e) {
				ms123.form.Dialog.alert("Composite._selectSearchFilter:" + e);
				return;
			}

			var options = [];
			if (list.rows === undefined || list.rows.length == 0) {
				ms123.form.Dialog.alert(this.tr("composite.select_dialog.no_search_saved"));
				return;
			}
			list.rows.sort(function (a, b) {
				a = a.name.toLowerCase();
				b = b.name.toLowerCase();
				if (a < b) return -1;
				if (a > b) return 1;
				return 0;
			});
			for (var r = 0; r < list.rows.length; r++) {
				var o = {};
				var row = list.rows[r];
				if (row.name) {
					o.label = row.name;
					o.value = r;
					options.push(o);
				}
			}
			var formData = {
				"filtername": {
					'type': "SelectBox",
					'label': this.tr("composite.select_dialog.name_filter"),
					'options': options
				}
			}
			var buttons = [{
				'label': this.tr("composite.select_dialog.delete"),
				'icon': "icon/22/actions/list-remove.png",
				'value': 1
			},
			{
				'label': this.tr("composite.select_dialog.cancel"),
				'icon': "icon/22/actions/dialog-cancel.png",
				'value': 3
			},
			{
				'label': this.tr("composite.select_dialog.use"),
				'icon': "icon/22/actions/dialog-ok.png",
				'value': 2
			}];

			var _this = this;
			var form = new ms123.form.Form({
				"message": "",
				"buttons": buttons,
				"allowCancel": true,
				"inWindow": true,
				"hide": false,
				"formData": formData,
				"callback": function (m, v) {
					if (m !== undefined) {
						var value = m.get("filtername");
						if (v == 1) {
							ms123.form.Dialog.confirm(_this.tr("composite.select_dialog.confirm_delete"), function (e) {
								if (e) {
									var completed = function (e) {
										ms123.form.Dialog.alert(_this.tr("composite.select_dialog.deleted"));
									};

									var cs = ms123.StoreDesc.getNamespaceConfigStoreDesc();
									var p = {
										id: list.rows[value].name + "_" + entityName,
										entity: "filter",
										namespace: cs.getNamespace(),
										storeId: cs.getStoreId(),
										pack: cs.getPack()
									};
									var params = {
										method: "delete",
										service: "data",
										context: this,
										parameter: p,
										async: false,
										failed: this._showErrors,
										completed: completed
									}
									ms123.util.Remote.rpcAsync(params);
									form.hide();
								}
							}, this);
						} else if (v == 3) {
							form.hide();
						} else if (v == 2) {
							sf.setFilter(jQuery.parseJSON(list.rows[value].filter));
							var idArray = qx.lang.Json.parse(list.rows[value].exclusion);
							if (idArray.length > 0) {
								var filter = _this._createIdFilter(idArray);
								var completed = function (data) {
									if( exclusionListTable){
										exclusionListTable.setData(data.rows);
									}
								};
								var cs = storeDesc;
								var p = {
									filter: filter,
									entity: entityName,
									pageSize: 500,
									namespace: cs.getNamespace(),
									storeId: cs.getStoreId(),
									pack: cs.getPack()
								};
								var params = {
									method: "query",
									service: "data",
									context: this,
									parameter: p,
									async: false,
									failed: this._showErrors,
									completed: completed
								}
								ms123.util.Remote.rpcAsync(params);
								form.hide();
							}
						}
					}
				},
				"context": _this
			});
			form.show();
		},

		_saveSearchFilter: function (data, entityName, exclusionListTable) {
			var formData = {
				"configname": {
					'type': "TextField",
					'label': this.tr("composite.save_dialog.name_filter"),
					'validation': {
						required: true,
						validator: "/^[A-Za-z]([0-9A-Za-z_]){2,20}$/"
					},
					'value': ""
				}
			};

			var idArray = [];
			if( exclusionListTable ){
				idArray = exclusionListTable.getIdArray();
				var idFilter = {
					label: "1",
					connector: "or",
					children: []
				};
				for (var i = 0; i < idArray.length; i++) {
					var node = new ms123.searchfilter.Node();
					node.setField("id");
					node.setOp("eq");
					node.setData(idArray[i]);
					idFilter.children.push(node);
				}
			}
			var exclusionFilter = qx.util.Serializer.toJson(idArray);

			var _this = this;
			var form = new ms123.form.Form({
				"formData": formData,
				"allowCancel": true,
				"inWindow": true,
				"callback": function (m) {
					if (m !== undefined) {
						var val = m.get("configname");

						var cs = ms123.StoreDesc.getNamespaceConfigStoreDesc();
						var rpcParams = {
							namespace: cs.getNamespace(),
							storeId: cs.getStoreId(),
							pack: cs.getPack(),
							entity: "filter",
							id: val + "_" + entityName,
							data: {
								name: val,
								type: ms123.config.ConfigManager.CS_ENTITY,
								user: this._user.userid,
								modulename: entityName,
								filter: data,
								exclusion: exclusionFilter
							}
						}
						var params = {
							service: "data",
							method: "update",
							parameter: rpcParams,
							async: false
						}
						try {
							ms123.util.Remote.rpcAsync(params);
							ms123.form.Dialog.alert(_this.tr("composite.select_dialog.saved"));
						} catch (e) {
							ms123.form.Dialog.alert("Composite._saveSearchFilter:" + e);
						}
					}
				},
				"context": _this
			});
			form.show();
		}
	}
});
