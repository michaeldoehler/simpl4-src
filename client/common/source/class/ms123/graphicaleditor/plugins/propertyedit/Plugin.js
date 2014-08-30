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
	* @ignore($H)
	* @ignore(Clazz)
*/
qx.Class.define("ms123.graphicaleditor.plugins.propertyedit.Plugin", {
	extend: qx.core.Object,
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (facade, parentPanel, diagramName,direction) {
		this.base(arguments);
		this.facade = facade;
		this.parentPanel = parentPanel;
		this.direction = direction;
		this.diagramName = diagramName;

		this.facade.registerOnEvent(ms123.oryx.Config.EVENT_SELECTION_CHANGED, this.onSelectionChanged.bind(this));
		this.init();
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

		init:function(){
			/* The currently selected shapes whos properties will shown */
			this.shapeSelection = new Hash();
			this.shapeSelection.shapes = new Array();
			this.shapeSelection.commonProperties = new Array();
			this.shapeSelection.commonPropertiesValues = new Hash();
			this.editor = new ms123.graphicaleditor.plugins.propertyedit.Editor(this.facade, this.diagramName, this.direction);
			this.parentPanel.add(this.editor);
			this.lastSelection = [];
		},

		onSelectionChanged: function (event) { 
			var isSameSelection = this.isSameSelection(this.lastSelection, event.elements);
			this.lastSelection = event.elements;
			/* Selected shapes */
			this.shapeSelection.shapes = event.elements;

			/* Case: nothing selected */
			if (event.elements.length == 0) {
				this.shapeSelection.shapes = [this.facade.getCanvas()];
			}

			/* subselection available */
			if (event.subSelection) {
				this.shapeSelection.shapes = [event.subSelection];
			}
			this.editor.edit( this.shapeSelection, isSameSelection );
		},
		isSameSelection : function( oldSel, newSel ){
			if( oldSel.length == 0 ) return false;
			if( oldSel.length != newSel.length ) return false;
			for( var i = 0; i < oldSel.length;i++){
				var id1 = oldSel[i].getId();
				var id2 = newSel[i].getId();
				if( id1 != id2 ){
					return false;
				}
			}
			return true;
		}

	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
