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

/**
	* @ignore(Hash)
*/
qx.Class.define("ms123.processexplorer.Config", {
	/******************************************************************************
	 STATICS
	 ******************************************************************************/
	statics: {
		EVENT_PROCESSDEFINITION_CHANGED: "processDefinitionChanged",
		EVENT_PROCESSDEPLOYMENT_CHANGED: "processDeploymentChanged",
		EVENT_CAMELROUTEDEFINITION_CHANGED: "camelRouteDefinitionChanged",
		EVENT_CAMELROUTESDEPLOYMENT_CHANGED: "camelRoutesDeploymentChanged",
		EVENT_DIAGRAM_OPENED: "diagramOpened",
		EVENT_HISTORY_OPENED: "historyOpened",
		EVENT_SHOWDETAILS: "showDetails",
		EVENT_HIDEDETAILS: "hideDetails",
		EVENT_PROCESSSTARTED: "processStarted"
	}
});
