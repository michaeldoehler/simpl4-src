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
	@asset(qx/icon/${qx.icontheme}/22/actions/*)
	@asset(qx/icon/${qx.icontheme}/16/apps/*)
	@asset(ms123/icons/*)
	@asset(ms123/*)
*/

qx.Class.define("ms123.shell.views.NewFile", {
	extend: ms123.shell.views.NewNode,
	include: qx.locale.MTranslation,


	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (model,param,facade) {
		this.base(arguments,model,facade);
		this.model = model;
		this._createDialog();
	},


	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},


	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		_createDialog: function () {
			var formData = {
				"name": {
					'type': "TextField",
					'label': this.tr("shell.file_name"),
					'validation': {
						required: true,
						filter:/[a-zA-Z0-9_.]/,
						validator: "/^[A-Za-z]([0-9A-Za-z_.]){1,48}$/"
					},
					'value': ""
				},
				"nodetype" : {
					'type'  : "SelectBox",
					'label' : this.tr("shell.node_type"),
					'value' : 1,
					'options' : ms123.shell.FileType.getFileOptions()
				}
			};

			var self = this;
			var form = new ms123.form.Form({
				"formData": formData,
				"allowCancel": true,
				"inWindow": true,
				"callback": function (m) {
					if (m !== undefined) {
						var val = m.get("name");
						var nt = m.get("nodetype");
						var res = self._handleTypeSpecific(val,nt);
						if( !this._assetExists(res.name,res.nt)){
							console.log("_createNode:"+JSON.stringify(res,null,2));
							self._createNode(res.name, "file", res.nt,res.content);
						}
					}
				},
				"context": self
			});
			form.show();
		},
		_handleTypeSpecific:function(name, nt){
			if( nt.match("^"+ms123.shell.Config.GROOVY_FT)){
				return this._handleGroovy(name,nt);
			}
			if( nt.match("^"+ms123.shell.Config.JAVA_FT)){
				return this._handleGroovy(name,nt);
			}
			return {
				name:name,
				nt:nt,
				content:null
			}
		},
		_addExtension:function(name,nt){
			var ext = nt.substring(2);
			if( this._endsWith(name, ext)){
				return name;
			}
			return name+ext;
		},
		_getClassName:function(name,nt){
			var ext = nt.substring(2);
			if( this._endsWith(name, ext)){
				name = name.substring(0, name.length-ext.length);
			}
			return name[0].toUpperCase() + name.slice(1);
		},
		_endsWith:function(str, suffix) {
				return str.indexOf(suffix, str.length - suffix.length) !== -1;
		},
		//Groovy specific ------------------------------------
		_handleGroovy:function(name, nt){
			var content = null;
			var maintype = nt.split("/")[0].toLowerCase();
			var subtype = null;
			try{
				subtype = nt.split("/")[1].toLowerCase();
			}catch(e){
			}
			var className = this._getClassName(name,maintype);
			if( subtype == "bean"){
				content = this._getBean(className);
			}
			if( subtype == "processor"){
				content = this._getProcessor(className);
			}
			return  {
				name:this._addExtension(name,maintype),
				nt:maintype,
				content:content
			}
		},
		_getBean:function(name){
			var code  = "import org.apache.camel.*;\n";
					code += "public class "+ name  +"{\n";
					code += "\t@Handler\n";
					code += "\tpublic String  doSomething(String body, Exchange exchange){\n";
					code += "\t}\n";
					code += "}\n";
			return code;
		},
		_getProcessor:function(name){
			var code  = "import org.apache.camel.*;\n";
					code += "public class "+ name  +" implements Processor{\n";
					code += "\tpublic void  process(Exchange exchange){\n";
					code += "\t}\n";
					code += "}\n";
			return code;
		}
		//Groovy specific end --------------------------------
	}
});
