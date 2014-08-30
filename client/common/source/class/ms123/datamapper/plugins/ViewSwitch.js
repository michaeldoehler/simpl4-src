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
	* @ignore(Hash)
*/

qx.Class.define("ms123.datamapper.plugins.ViewSwitch", {
	extend: qx.core.Object,
 include : [ qx.locale.MTranslation],
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (facade,isDefaultsEnabled) {
		this.base(arguments);
		this.facade = facade;

		var ed_msg = this.tr("datamapper.viewswitch_editor");
		var sc_msg = this.tr("datamapper.viewswitch_script");
		var pv_msg = this.tr("datamapper.viewswitch_preview");
		var def_msg = this.tr("datamapper.viewswitch_defaults");
		if( facade.use == ms123.datamapper.Config.USE_IMPORT){
			pv_msg = this.tr("datamapper.viewswitch_import");
		}
		var group = "3";
		this.facade.offer({
			name: ed_msg,
			description: ed_msg,
			radioGroup: "viewswitch",
			icon: "resource/ms123/mapping_icon.png",
			functionality: this.doGraphic.bind(this),
			group: group,
			isEnabled: qx.lang.Function.bind(function () {
				return true;
			}, this),
			index: 0
		});

		this.facade.offer({
			name: pv_msg,
			description: pv_msg,
			radioGroup: "viewswitch",
			icon: "resource/ms123/preview.png",
			functionality: this.doPreview.bind(this),
			group: group,
			isEnabled: qx.lang.Function.bind(function () {
				return true
			}, this),
			index: 1
		});

		if( isDefaultsEnabled){
			this.facade.offer({
				name: def_msg,
				description: def_msg,
				radioGroup: "viewswitch",
				icon: "resource/ms123/defaults_icon.gif",
				functionality: this.doDefaults.bind(this),
				group: group,
				isEnabled: qx.lang.Function.bind(function () {
					return true
				}, this),
				index: 2
			});
		}

	},


	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},

	/******************************************************************************
	 STATICS
	 ******************************************************************************/
	statics: {},
	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		doGraphic:function(){
			this.facade.viewStack.setSelection([this.facade.editorView]);
		},
		doDefaults:function(){
			this.facade.viewStack.setSelection([this.facade.defaultsView]);
		},
		doPreview:function(){
			this.facade.viewStack.setSelection([this.facade.previewView]);
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
