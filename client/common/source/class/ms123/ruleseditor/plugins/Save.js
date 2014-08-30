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
*/
/**
	* @ignore(Hash)
	* @ignore(Clazz)
*/

qx.Class.define("ms123.ruleseditor.plugins.Save", {
	extend: qx.core.Object,
 include : [ qx.locale.MTranslation],
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (facade,main) {
		this.base(arguments);
		this.facade = facade;
		this.main = main;

		var save_msg = this.tr("ruleseditor.save");
		this.facade.offer({
			'name': save_msg,
			'functionality': this.save.bind(this, false),
			'group': "0",
			'icon': "icon/16/actions/document-save.png",
			'description': save_msg,
			'index': 1,
			'isEnabled': qx.lang.Function.bind(function () {
				return this.facade.getConditionColumns().length > 0 && this.facade.getActionColumns().length> 0 && this.facade.getCountRules() >0;
			}, this)
		});

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
		/**
		 * Saves the current process to the server.
		 */
		save: function (forceNew, event) {
			var json = this.facade.getJSON();
			var jsonRulesModel = qx.lang.Json.stringify(json,null,2); 
			console.log("SAVE:"+jsonRulesModel);
			this.main.fireDataEvent("save", jsonRulesModel, null);
			return true;
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
