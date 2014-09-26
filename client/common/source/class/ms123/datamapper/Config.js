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
qx.Class.define("ms123.datamapper.Config", {
	/******************************************************************************
	 STATICS
	 ******************************************************************************/
	statics: {

		EVENT_INPUTTREE_CREATED: "inputTreeCreated",
		EVENT_OUTPUTTREE_CREATED: "outputTreeCreated",
		EVENT_MAPPING_CHANGED: "mappingChanged",
		EVENT_TREE_CHANGED: "treeChanged",
		NODETYPE_ATTRIBUTE : "ntAttribute",
		NODETYPE_ELEMENT : "ntElement",
		NODETYPE_COLLECTION : "ntCollection",
		NODENAME : "name",
		NODELABEL : "label",
		NODETYPE : "label",
		FIELDTYPE : "fieldType",
		INPUT : "input",
		OUTPUT : "output",
		USE_IMPORT:"import",
		USE_CAMEL:"camel",
		PATH_DELIM:'/',
		BG_COLOR_STRUCTURE_CONNECTED: "#E0E0E0",
		BG_COLOR_STRUCTURE_SELECTED: "#E0E0E0",
		BG_COLOR_ATTRIBUTE_CONNECTED: "#F0F0F0",
		BG_COLOR_READY: "#aff97a",
		BG_COLOR_NOTREADY: "#edeaea",
		STRUCTURE_MAPPING:"structureMapping",
		ATTRIBUTE_MAPPING:"attributeMapping",
		STRUCTURE_SCOPE:"collectionScope",
		ATTRIBUTE_SCOPE:"attributeScope",
		STRUCTURE_LINECOLOR:"red",
		ATTRIBUTE_LINECOLOR:"blue",
		MAPPING_PARAM:"mapping",
		TREE_LABEL_COLOR:"black",
		TREE_LABEL_SELECTED_COLOR:"blue",
		IDPREFIX : "dm_",
		ID_INPREFIX : "dm_input",
		ID_OUTPREFIX : "dm_output",
		FORMAT_JSON : "json",
		FORMAT_XML : "xml",
		FORMAT_FW : "fw",
		FORMAT_MAP : "map",
		FORMAT_CSV : "csv",
		FORMAT_POJO : "pojo",
		FORMAT_EXCEL : "excel",
		EXAMPLE: "xxx"

	}
});
