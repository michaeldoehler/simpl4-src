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
 * Specific data cell renderer for dates.
 */
qx.Class.define("ms123.shell.FileType", {

/*
  *****************************************************************************
     PROPERTIES
  *****************************************************************************
  */

	/******************************************************************************
	 STATICS
	 ******************************************************************************/
	statics: {
		getIcon: function (target, model) {
			var type = model.getType();
			if (type==null) {
				var isOpen =  target.isOpen();
				if( isOpen ){
					return "resource/ms123/directory_open.png";
				}else{
					return "resource/ms123/directory.png";
				}
			} else {
				if (type == ms123.shell.Config.PROCESS_FT) {
					return "icon/22/actions/system-run.png";
				}else if (type == ms123.shell.Config.WEBSITE_FT) {
					return "icon/22/categories/internet.png";
				}else if (type == ms123.shell.Config.WEBPAGE_FT) {
					return "resource/ms123/webpage.png";
				}else if (type == ms123.shell.Config.CAMEL_FT) {
					return "resource/ms123/camel.png";
				}else if (type == ms123.shell.Config.STENCIL_FT) {
					return "resource/ms123/stencil.png";
				}else if (type == ms123.shell.Config.GROOVY_FT) {
					return "resource/ms123/groovy.png";
				}else if (type == ms123.shell.Config.DATAMAPPER_FT) {
					return "resource/ms123/datamapper.png";
				}else if (type == "image/svg+xml") {
					return "resource/ms123/svg.png";
				}else if (type == "image/png") {
					return "resource/ms123/png.png";
				}else if (type == "image/jpg") {
					return "resource/ms123/jpg.png";
				}else if (type == "application/vnd.oasis.opendocument.text") {
					return "resource/ms123/odt.png";
				} else if (type == ms123.shell.Config.RULE_FT) {
					return ms123.shell.FileType._getIconUrl("bpmn/activity/list/type.business.rule.png");
				} else if (type == ms123.shell.Config.DOCUMENT_FT) {
					return ms123.shell.FileType._getIconUrl("bpmn/activity/list/type.document.png");
				} else if (type == ms123.shell.Config.FORM_FT) {
					return ms123.shell.FileType._getIconUrl("bpmn/activity/list/type.user.png");
				} else if (type == ms123.shell.Config.FILTER_FT) {
					return ms123.shell.FileType._getIconUrl("bpmn/activity/list/type.filter.png");
				} else if (type == ms123.shell.Config.ENTITYTYPE_FT) {
					return "resource/ms123/table.png";
				}
			}
			return "resource/ms123/file.png";
		},
		getAllEditables:function(){
				return  ["sw.rule","sw.process","sw.filter", "sw.form", "sw.website", "sw.webpage", "sw.camel", "sw.stencil", "sw.groovy", "sw.datamapper", "sw.document"];
		},
		getAllJsonEditables:function(){
				return  ["sw.rule","sw.process","sw.filter", "sw.form","sw.website", "sw.camel",  "sw.stencil", "sw.datamapper", "sw.document"];
		},

		getAllTextEditables:function(){
				return  ["image/svg+xml", "text/html","text/x-asciidoc","text/x-yaml","text/css", "text/javascript", "application/json"];
		},
		getAllForeigns:function(){
				return  ["image/png","image/jpg","image/jpg", "application/vnd.oasis.opendocument.text", "image/svg+xml", "text/html", "text/x-asciidoc","text/x-yaml","text/javascript", "text/css", "application/json"];
		},
		_getIconUrl: function (name) {
			var am = qx.util.AliasManager.getInstance(name);
			return am.resolve("resource/ms123/stencilsets/"+ name);
		},
		getIconMapping: function () {
			var iconMap = {};
			iconMap["sw.unknown"]= "icon/16/status/dialog-error.png";
			iconMap["image/png"]= "resource/ms123/png.png";
			iconMap["image/jpg"]= "resource/ms123/jpg.png";
			iconMap["image/svg+xml"]= "resource/ms123/svg.png";
			iconMap["text/html"]= "resource/ms123/html.png";
			iconMap["text/css"]= "resource/ms123/css.png";
			iconMap["text/javascript"]= "resource/ms123/js.png";
			iconMap["text/x-asciidoc"]= "resource/ms123/asciidoc.png";
			iconMap["text/x-yaml"]= "resource/ms123/yaml.png";
			iconMap["application/json"]= "resource/ms123/json.png";
			iconMap["application/vnd.oasis.opendocument.text"]= "resource/ms123/odt.png";
			iconMap[ms123.shell.Config.PROCESS_FT]= "icon/16/actions/system-run.png";
			iconMap[ms123.shell.Config.WEBSITE_FT]= "icon/16/categories/internet.png";
			iconMap[ms123.shell.Config.WEBPAGE_FT]= "resource/ms123/webpage.png";
			iconMap[ms123.shell.Config.CAMEL_FT]= "resource/ms123/camel.png";
			iconMap[ms123.shell.Config.STENCIL_FT]= "resource/ms123/stencil.png";
			iconMap[ms123.shell.Config.GROOVY_FT]= "resource/ms123/groovy.png";
			iconMap[ms123.shell.Config.DATAMAPPER_FT]= "resource/ms123/datamapper.png";
			iconMap[ms123.shell.Config.RULE_FT] = ms123.shell.FileType._getIconUrl("bpmn/activity/list/type.business.rule.png");
			iconMap[ms123.shell.Config.FORM_FT] = ms123.shell.FileType._getIconUrl("bpmn/activity/list/type.user.png");
			iconMap[ms123.shell.Config.DOCUMENT_FT] = ms123.shell.FileType._getIconUrl("bpmn/activity/list/type.document.png");
			iconMap[ms123.shell.Config.FILTER_FT] = ms123.shell.FileType._getIconUrl("bpmn/activity/list/type.filter.png");
			return iconMap;
		},
		getFileOptions:function(){
			return  [
				{ label: qx.locale.Manager.tr("shell.nt_process"),   value: ms123.shell.Config.PROCESS_FT},
				{ label: qx.locale.Manager.tr("shell.nt_form"),   value: ms123.shell.Config.FORM_FT},
				{ label: qx.locale.Manager.tr("shell.nt_document"),   value: ms123.shell.Config.DOCUMENT_FT},
				{ label: qx.locale.Manager.tr("shell.nt_rule"),   value: ms123.shell.Config.RULE_FT},
				{ label: qx.locale.Manager.tr("shell.nt_camel"),   value: ms123.shell.Config.CAMEL_FT},
				{ label: qx.locale.Manager.tr("Stencil"),   value: ms123.shell.Config.STENCIL_FT},
				{ label: qx.locale.Manager.tr("Groovy"),   value: ms123.shell.Config.GROOVY_FT},
				{ label: qx.locale.Manager.tr("Groovy/Bean"),   value: ms123.shell.Config.GROOVY_FT+"/Bean"},
				{ label: qx.locale.Manager.tr("Groovy/Processor"),   value: ms123.shell.Config.GROOVY_FT+"/Processor"},
				{ label: qx.locale.Manager.tr("shell.nt_datamapper"),   value: ms123.shell.Config.DATAMAPPER_FT},
				{ label: qx.locale.Manager.tr("shell.nt_website"),   value: ms123.shell.Config.WEBSITE_FT},
				{ label: qx.locale.Manager.tr("shell.nt_webpage"),   value: ms123.shell.Config.WEBPAGE_FT},
				{ label: qx.locale.Manager.tr("shell.nt_filter"),   value: ms123.shell.Config.FILTER_FT}
			]
		}
	}
});
