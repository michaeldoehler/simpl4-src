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
package org.ms123.common.datamapper;
import flexjson.JSONSerializer;
import java.util.*;
import java.io.*;
import flexjson.JSONSerializer;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlObject.Factory;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlBeans;
import org.apache.xmlbeans.impl.inst2xsd.Inst2Xsd;
import org.apache.xmlbeans.impl.inst2xsd.Inst2XsdOptions;
import org.apache.xmlbeans.impl.xb.xsdschema.SchemaDocument;
import org.apache.xmlbeans.impl.xb.xsdschema.SchemaDocument.Schema;
import org.apache.xmlbeans.SchemaTypeSystem;
import org.apache.xmlbeans.SchemaGlobalElement;
import org.apache.xmlbeans.SchemaProperty;
import org.apache.xmlbeans.SchemaType;

import javax.xml.namespace.QName;
import java.math.BigInteger;

public final class XmlMetaData implements MetaData,XSDConstants,Constants {

	protected static JSONSerializer js = new JSONSerializer();
  private static final Map<QName, String> m_typeMapping = new HashMap();
	private Map<String, String> m_uriToGeneratedPrefixesMap;
	private Stack<SchemaType> m_typeStack;
	private int m_maxLevelOfRecursion = 1;

	public XmlMetaData() {
		js.prettyPrint(true);
	}

  static
  {
    m_typeMapping.put(XSD_BYTE, FIELDTYPE_BYTE);
    m_typeMapping.put(XSD_INT, FIELDTYPE_INTEGER);
    m_typeMapping.put(XSD_INTEGER, FIELDTYPE_INTEGER);
    m_typeMapping.put(XSD_SHORT, FIELDTYPE_INTEGER);
    m_typeMapping.put(XSD_NEGATIVEINTEGER, FIELDTYPE_INTEGER);
    m_typeMapping.put(XSD_NONNEGATIVEINTEGER, FIELDTYPE_INTEGER);
    m_typeMapping.put(XSD_NONPOSITIVEINTEGER, FIELDTYPE_INTEGER);
    m_typeMapping.put(XSD_POSITIVEINTEGER, FIELDTYPE_INTEGER);
    m_typeMapping.put(XSD_UNSIGNEDBYTE, FIELDTYPE_INTEGER);
    m_typeMapping.put(XSD_UNSIGNEDINT, FIELDTYPE_INTEGER);
    m_typeMapping.put(XSD_UNSIGNEDSHORT, FIELDTYPE_INTEGER);

    m_typeMapping.put(XSD_LONG, FIELDTYPE_LONG);
    m_typeMapping.put(XSD_UNSIGNEDLONG, FIELDTYPE_LONG);

    m_typeMapping.put(XSD_FLOAT, FIELDTYPE_DOUBLE);
    m_typeMapping.put(XSD_DOUBLE, FIELDTYPE_DOUBLE);

    m_typeMapping.put(XSD_BOOLEAN, FIELDTYPE_BOOLEAN);

    m_typeMapping.put(XSD_STRING, FIELDTYPE_STRING);
    m_typeMapping.put(XSD_ANY, FIELDTYPE_STRING);

    m_typeMapping.put(XSD_NORMALIZEDSTRING, FIELDTYPE_STRING);

    m_typeMapping.put(XSD_DECIMAL, FIELDTYPE_DECIMAL);

    m_typeMapping.put(XSD_DATE, FIELDTYPE_DATE);
    m_typeMapping.put(XSD_DATETIME, FIELDTYPE_DATE);

    //m_typeMapping.put(Constants.XSD_HEXBIN, SimpleMetadataFieldType.Byte);
    //m_typeMapping.put(Constants.XSD_BASE64, SimpleMetadataFieldType.Byte);
  }


	public Map generateMetadata(File stream) throws Exception {
		Reader bufferedReader = new FileReader(stream);
		return generateMetadata(bufferedReader);
	}
	public Map generateMetadata(Reader reader) throws Exception {
		def schemaRepresentation = new XmlObject[1];
		schemaRepresentation[0] = generateSchema(reader);
		XmlOptions options = new XmlOptions();
    options.setCompileDownloadUrls();
    options.setCompileNoUpaRule();

		SchemaTypeSystem sts = XmlBeans.compileXsd(schemaRepresentation, XmlBeans.getBuiltinTypeSystem(), options);
		SchemaGlobalElement[] globalElements	= sts.globalElements();
		return _generateMetadata(globalElements[0]);
	}

	private XmlObject generateSchema(Reader reader) {
		def xmlObjects = new XmlObject[1];
		xmlObjects[0] = XmlObject.Factory.parse(reader);
		SchemaDocument[] inst2xsd = Inst2Xsd.inst2xsd(xmlObjects, new Inst2XsdOptions());
		return inst2xsd[0];
	}

	private Map _generateMetadata(SchemaGlobalElement node) {
		this.m_typeStack = new Stack();
		this.m_uriToGeneratedPrefixesMap = new HashMap();
		QName name = createQNameWithPrefix(node.getName());
		Map rootMap  = new HashMap();
		rootMap.put(NODENAME,getLocalName(name));
		rootMap.put(NODETYPE,NODETYPE_ELEMENT);
		rootMap.put(ROOT,true);
		rootMap.put(FORMAT,FORMAT_XML);
		rootMap.put(CHILDREN,new ArrayList());
		_generateMetadata(node.getType(), rootMap);
		System.out.println(js.deepSerialize(rootMap));
		return rootMap;
	}

	private void _generateMetadata(SchemaType type, Map parent) {
		System.out.println("generateMetadata:"+type.getName()+"/"+parent.name);
		this.m_typeStack.push(type);
		for (SchemaProperty property : type.getProperties()) {
			SchemaType propertyType = property.getType();
			String fieldType = getFieldType(propertyType);
			System.out.print("\t:"+property.getName()+" -> "+fieldType);
			QName qName = createQNameWithPrefix(property.getName());
			if (property.isAttribute()) {
				println("\t>Attribute:"+parent.name);
				Map attributeMap = new HashMap();
				List childList = [];
				attributeMap.put(NODENAME,getLocalName(property.getName()));
				attributeMap.put(NODETYPE,NODETYPE_ATTRIBUTE);
				attributeMap.put(FIELDTYPE,fieldType);
				parent.get(CHILDREN).add(attributeMap);
			} else if (validateRecursion(propertyType)) {
				boolean isList = isList(property);
				boolean isSimpleType = isSimpleType(propertyType, fieldType);
				if (isSimpleType && isList) {
					println("\t>SimpleType");
					Map attributeMap = new HashMap();
					attributeMap.put(NODENAME,getLocalName(property.getName()));
					attributeMap.put(FIELDTYPE,"list_"+fieldType);
					attributeMap.put(NODETYPE,NODETYPE_ATTRIBUTE);
					parent.get(CHILDREN).add(attributeMap);
				} else if (isList) {
					println("\t>List");
					Map collectionMap = new HashMap();
					parent.get(CHILDREN).add(collectionMap);
					List childList = [];
					collectionMap.put(CHILDREN,childList);
					collectionMap.put(NODENAME,getLocalName(property.getName()));
					collectionMap.put(NODETYPE,NODETYPE_COLLECTION);
					_generateMetadata(propertyType, collectionMap);
				} else if (hasSimpleContentOnly(propertyType)) {
					println("\t>hasOnlySimple");
					Map attributeMap = new HashMap();
					attributeMap.put(NODENAME,getLocalName(property.getName()));
					attributeMap.put(FIELDTYPE,fieldType);
					attributeMap.put(NODETYPE,NODETYPE_ATTRIBUTE);
					parent.get(CHILDREN).add(attributeMap);
				} else {
					println("\t>else:"+parent.name);
					Map elementMap = new HashMap();
					elementMap.put(NODENAME,property.getName().localPart);
					elementMap.put(NODETYPE,NODETYPE_ELEMENT);
					elementMap.put(CHILDREN,[]);
					parent.get(CHILDREN).add(elementMap);
					_generateMetadata(propertyType, elementMap);
				}
			}
		}
		this.m_typeStack.pop();
	}

	private boolean isSimpleType(SchemaType type, String fieldType) {
		return (fieldType != null) && (isPlainElement(type));
	}

	private boolean isPlainElement(SchemaType type) {
		return ((type.getAttributeProperties() == null) || (type.getAttributeProperties().length == 0)) && ((type.getElementProperties() == null) || (type.getElementProperties().length == 0));
	}

	private boolean validateRecursion(SchemaType type) {
		int hits = 0;
		for (SchemaType stackType : this.m_typeStack) {
			if (stackType.equals(type)) {
				hits++;
			}
		}
		return this.m_maxLevelOfRecursion >= hits;
	}

	private QName createQNameWithPrefix(QName name) {
		String namespaceURI = name.getNamespaceURI();
		if (namespaceURI == null || namespaceURI == "") {
			return name;
		}
		String prefix = (String) this.m_uriToGeneratedPrefixesMap.get(namespaceURI);
		if (prefix == null) {
			prefix = generatePrefix(namespaceURI);
		}
		return new QName(name.getNamespaceURI(), name.getLocalPart(), prefix);
	}

	private String generatePrefix(String uri) {
		if (uri.isEmpty()) {
			return "";
		}
		String prefix = "ns" + this.m_uriToGeneratedPrefixesMap.size();
		this.m_uriToGeneratedPrefixesMap.put(uri, prefix);
		return prefix;
	}

	private String getLocalName(QName name){
		return name.getLocalPart();
	}

	private boolean hasSimpleContentOnly(SchemaType type) {
		return (hasSimpleContent(type)) && (isPlainElement(type));
	}

	private boolean hasSimpleContent(SchemaType type) {
		return (type.isSimpleType()) || (type.getContentType() == 2) || (type.getContentType() == 4) || (type.getComponentType() == 1);
	}

	private boolean isList(SchemaProperty property) {
		return (property.getMaxOccurs() == null) || (property.getMaxOccurs().intValue() > 1);
	}

	private String getFieldType(SchemaType type) {
		SchemaType simpleType = getSimpleBaseType(type);
		while (simpleType != null){
			String fieldType = (String) m_typeMapping.get(simpleType.getName());
			if (fieldType != null) {
				return fieldType;
			}
			simpleType = simpleType.getBaseType();
		};
		return FIELDTYPE_STRING;
	}

	private static SchemaType getSimpleBaseType(SchemaType type) {
		SchemaType basic = type;
		while ((basic.getBaseType() != null) && (!basic.isSimpleType())) {
			basic = basic.getBaseType();
		}
		return basic;
	}
}
