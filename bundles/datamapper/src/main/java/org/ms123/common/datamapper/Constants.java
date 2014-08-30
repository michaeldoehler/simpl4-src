/**
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
package org.ms123.common.datamapper;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.io.IOException;
import java.io.Reader;
import java.io.File;

/**
 *
 */
public interface  Constants {

	static String NODETYPE_ATTRIBUTE = "ntAttribute";

	static String NODETYPE_ELEMENT = "ntElement";

	static String NODETYPE_COLLECTION = "ntCollection";


	static String FIELDTYPE = "fieldType";
	static String FIELDTYPE_STRING = "string";
	static String FIELDTYPE_DATE = "date";
	static String FIELDTYPE_CALENDAR = "calendar";
	static String FIELDTYPE_INTEGER = "integer";
	static String FIELDTYPE_LONG = "long";
	static String FIELDTYPE_DOUBLE = "double";
	static String FIELDTYPE_DECIMAL = "decimal";
	static String FIELDTYPE_BYTE = "byte";
	static String FIELDTYPE_BOOLEAN = "boolean";


	static String NODETYPE = "type";
	static String NODENAME = "name";
	static String NODELABEL = "label";

	static String CHILDREN = "children";

	static String ID = "id";

	static String ROOT = "root";

	static String FORMAT = "format";

	static String FORMAT_JSON = "json";
	static String FORMAT_XML = "xml";
	static String FORMAT_MAP = "map";
	static String FORMAT_CSV = "csv";
	static String FORMAT_POJO = "pojo";

	static String MAP_ROOT = "map";
	static String JSON_ROOT = "json";
	static String CSV_ROOT = "csv-set";
	static String CSV_RECORD = "record";
	static String CSV_DELIM = "columnDelim";
	static String CSV_QUOTE = "quote";
	static String CSV_HEADER = "header";

	static String TRANSFORMER_CONTEXT = "__transformerContext";

	static String SCRIPT_NAME = "__scriptName";
	static String SCRIPT_SOURCE = "__scriptSource";
	static String DATAMAPPER_CONFIG = "__datamapperConfig";

	static String INPUT = "input";

	//In Smooks set
	static String PROPERTY_NAME = "__propertyName";

	static String DECODER = "__decoder";

	static String DATAOBJECT = "__dataObject";
}
