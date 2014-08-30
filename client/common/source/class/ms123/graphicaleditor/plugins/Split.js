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

qx.Class.define("ms123.graphicaleditor.plugins.Split", {
	extend: qx.core.Object,
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (facade) {
		this.base(arguments);
		this.facade = facade;

		this._propertyPanelVisible = true;

		this.facade.offer({
			name: "graphicaleditor.togglePropertyPanel",
			description: "",
			icon: this.__getResourceUrl("view.png"),
			keyCodes: [{
				metaKeys: [ms123.oryx.Config.META_KEY_META_CTRL],
				keyCode: 65,
				keyAction: ms123.oryx.Config.KEY_ACTION_DOWN
			}],
			functionality: this.togglePropertyPanel.bind(this),
			group: "zzGroup",
			isEnabled: qx.lang.Function.bind(function () {
				return true
			}, this),
			index: 2
		});

		/*this.facade.offer({
			name: ms123.oryx.Translation.Split.toggleShapeMenu,
			description: ms123.oryx.Translation.Split.toggleShapeMenuDesc,
			icon: this.__getResourceUrl("arrow_redo.png"),
			keyCodes: [{
				metaKeys: [ms123.oryx.Config.META_KEY_META_CTRL],
				keyCode: 83,
				keyAction: ms123.oryx.Config.KEY_ACTION_DOWN
			}],
			functionality: this.toggleShapeMenu.bind(this),
			group: "zzGroup",
			isEnabled: qx.lang.Function.bind(function () {
				return true
			}, this),
			index: 0
		});*/
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
		 * 
		 */
		togglePropertyPanel: function () {
			if( this._propertyPanelVisible ){
				this.facade.container.dialogPanel.exclude();
				this._propertyPanelVisible = false;
			}else{
				this.facade.container.dialogPanel.show();
				this._propertyPanelVisible = true;
			}
		},
		/**

		 * 
		 */
		toggleShapeMenu: function () {
		},

		__getResourceUrl: function (name) {
			var am = qx.util.AliasManager.getInstance();
			return am.resolve("resource/ms123/" + name);
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
