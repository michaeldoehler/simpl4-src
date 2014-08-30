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

qx.Class.define("ms123.graphicaleditor.StencilFigure", {
	extend: ms123.draw2d.SVGFigure,
	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function (stencil,width,height) {
		this.base(arguments,stencil.getView(),width,height);
		this.outputPort = null;
		this.setDimension(50, 50);
	},

	/******************************************************************************
	 STATICS
	 ******************************************************************************/
	statics: {},

	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},

	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		setWorkflow: function ( /*:draw2d.Workflow*/ workflow) {
			this.base(arguments, workflow);

			if (workflow !== null && this.outputPort === null) {
				this.outputPort = new ms123.draw2d.OutputPort();
				this.outputPort.setMaxFanOut(5); // It is possible to add "5" Connector to this port
 				this.outputPort.setName("output");
				this.outputPort.setWorkflow(workflow);
				this.outputPort.setBackgroundColor(new ms123.draw2d.Color(245, 115, 115));
				this.addPort(this.outputPort, this.width, this.height / 2);
			}
		}
	},
	/******************************************************************************
	 DESTRUCTOR
	 ******************************************************************************/
	destruct: function () {}

});
