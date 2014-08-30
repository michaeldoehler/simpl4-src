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
/*
*/
qx.Class.define("ms123.config.Definitions", {
 extend: qx.core.Object,
 include : qx.locale.MTranslation,


	/******************************************************************************
	 STATICS
	 ******************************************************************************/
	members: {
		tableConfigs: function (from,managedApp) {
			return this._translate({
				"main-form": [{
					name: "name",
					header: "%configeditor.column"
				},
				{
					name: "displayname",
					header: "%configeditor.displayname"
				},
				{
					name: "mainform_tab",
					header: "%configeditor.form.tab",
					type: "SelectBox",
					/*options: [
						{label:"tab1",value:"tab1"}, 
						{label:"tab2",value:"tab2"}, 
						{label:"tab3",value:"tab3"}, 
						{label:"tab4",value:"tab4"}
					]*/
					options: function(module){
						var url = "meta/modules/" + module +"/views/main-form?what=asRow&module=list";
						if( managedApp ){
							url = "/"+managedApp+"/"+url;
						}
						var ret  = ms123.util.Remote.sendSync(url);
						if( ret && ret.properties ){
							var p  = qx.lang.Json.parse(ret.properties);
							if( p && p.formlayout && typeof p.formlayout == "string" ){
								var tabs = p.formlayout.split(";");
								var olist = [];
								if( from == "configmanager"){
									var o = {label:"default",value:"default"};
									olist.push(o);	
								}
								for(var i=0; i < tabs.length;i++){
									var tab = tabs[i];
									if( tab.indexOf( ":") != -1){
										tab = tab.split(":")[0];
									}
									var o = {label:tab,value:tab};
									olist.push(o);	
								}
								return olist;
							}
						}
						return [];
					}
				},
				{
					name: "mainform_header",
					header: "%configeditor.form.header",
					type: "TextField"
				}
				],
				"main-grid": [{
					name: "name",
					header: "%configeditor.column"
				},
				{
					name: "displayname",
					header: "%configeditor.displayname"
				}],
				"report": [{
					name: "name",
					header: "%configeditor.column"
				},
				{
					name: "displayname",
					header: "%configeditor.displayname"
				}],
				"search": [{
					name: "name",
					header: "%configeditor.column"
				},
				{
					name: "search_options",
					header: "%configeditor.search.options",
					type: "DoubleSelectBox",
					options: [
						{value:'eq', label:'%meta.lists.eq'},
						{value:'ne', label:'%meta.lists.ne'},
						{value:'lt', label:'%meta.lists.lt'},
						{value:'le', label:'%meta.lists.le'},
						{value:'gt', label:'%meta.lists.gt'},
						{value:'ge', label:'%meta.lists.ge'},
						{value:'bw', label:'%meta.lists.bw'},
						{value:'bn', label:'%meta.lists.bn'},
						{value:'in', label:'%meta.lists.in'},
						{value:'ni', label:'%meta.lists.ni'},
						{value:'cn', label:'%meta.lists.cn'},
						{value:'nc', label:'%meta.lists.nc'}
					]
				}],
				"global-search": [{
					name: "name",
					header: "%configeditor.column"
				},
				{
					name: "displayname",
					header: "%configeditor.displayname"
				}],
				"duplicate-check": [{
					name: "name",
					header: "%configeditor.column"
				},
				{
					name: "displayname",
					header: "%configeditor.displayname"
				},
				{
					name: "check_type",
					header: "%configeditor.check_type",
					type: "SelectBox",
					options: [
						{value:'both', label:'%configeditor.check_type.both'},
						{value:'phonetic', label:'%configeditor.check_type.phonetic'},
						{value:'distance', label:'%configeditor.check_type.distance'},
						{value:'equal', label:'%configeditor.check_type.equal'}
					]
				}],
				"export": [{
					name: "name",
					header: "%configeditor.column"
				},
				{
					name: "displayname",
					header: "%configeditor.displayname"
				}, {
					name: "vcardname",
					header: "%configeditor.vcardname"
				}
        ]
			});
		},
		_translate: function (o) {
			if (typeof o == "string") {
				if (o.match(/^%/)) {
					var tr = this.tr(o.substring(1));
					if (tr) {
						o = tr;
					}
				}
				return o;
			}
			for (var i in o) {
 			  if (typeof o[i] == "function")continue;
				o[i] = this._translate(o[i]);
			}
			return o;
		}
	}


});
