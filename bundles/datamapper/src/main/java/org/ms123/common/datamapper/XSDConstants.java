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
import javax.xml.namespace.QName;

/**
 * Constants
 */
public  interface XSDConstants {

	public static final String URI_2001_SCHEMA_XSD = "http://www.w3.org/2001/XMLSchema";
	public static final QName XSD_ANY = new QName(URI_2001_SCHEMA_XSD, "any");
	public static final QName XSD_BASE64 = new QName(URI_2001_SCHEMA_XSD, "base64Binary");
	public static final QName XSD_BOOLEAN = new QName(URI_2001_SCHEMA_XSD, "boolean");
	public static final QName XSD_BYTE = new QName(URI_2001_SCHEMA_XSD, "byte");
	public static final QName XSD_DATE = new QName(URI_2001_SCHEMA_XSD, "date");
	public static final QName XSD_DATETIME = new QName(URI_2001_SCHEMA_XSD, "dateTime");
	public static final QName XSD_DECIMAL = new QName(URI_2001_SCHEMA_XSD, "decimal");
	public static final QName XSD_DOUBLE = new QName(URI_2001_SCHEMA_XSD, "double");
	public static final QName XSD_FLOAT = new QName(URI_2001_SCHEMA_XSD, "float");
	public static final QName XSD_HEXBIN = new QName(URI_2001_SCHEMA_XSD, "hexBinary");
	public static final QName XSD_INT = new QName(URI_2001_SCHEMA_XSD, "int");
	public static final QName XSD_INTEGER = new QName(URI_2001_SCHEMA_XSD, "integer");
	public static final QName XSD_LONG = new QName(URI_2001_SCHEMA_XSD, "long");
	public static final QName XSD_NEGATIVEINTEGER = new QName(URI_2001_SCHEMA_XSD, "negativeInteger");
	public static final QName XSD_NONNEGATIVEINTEGER = new QName(URI_2001_SCHEMA_XSD, "nonNegativeInteger");
	public static final QName XSD_NONPOSITIVEINTEGER = new QName(URI_2001_SCHEMA_XSD, "nonPositiveInteger");
	public static final QName XSD_NORMALIZEDSTRING = new QName(URI_2001_SCHEMA_XSD, "normalizedString");
	public static final QName XSD_POSITIVEINTEGER = new QName(URI_2001_SCHEMA_XSD, "positiveInteger");
	public static final QName XSD_SHORT = new QName(URI_2001_SCHEMA_XSD, "short");
	public static final QName XSD_STRING = new QName(URI_2001_SCHEMA_XSD, "string");
	public static final QName XSD_UNSIGNEDBYTE = new QName(URI_2001_SCHEMA_XSD, "unsignedByte");
	public static final QName XSD_UNSIGNEDINT = new QName(URI_2001_SCHEMA_XSD, "unsignedInt");
	public static final QName XSD_UNSIGNEDLONG = new QName(URI_2001_SCHEMA_XSD, "unsignedLong");
	public static final QName XSD_UNSIGNEDSHORT = new QName(URI_2001_SCHEMA_XSD, "unsignedShort");

}
