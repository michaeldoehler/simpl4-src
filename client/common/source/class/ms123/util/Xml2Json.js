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
qx.Class.define("ms123.util.Xml2Json", {
	extend: qx.core.Object,

	/******************************************************************************
	 CONSTRUCTOR
	 ******************************************************************************/
	construct: function () {},

	/******************************************************************************
	 PROPERTIES
	 ******************************************************************************/
	properties: {},

	/******************************************************************************
	 STATICS
	 ******************************************************************************/
	statics: {
		NODE_TYPES: {
			Element: 1,
			Attribute: 2,
			Text: 3,
			CDATA: 4,
			Root: 9,
			Fragment: 11
		},
		parseXMLString: function (strXML) {
			var xmlDoc = null,
				out = null,
				isParsed = true;
			try {
				xmlDoc = ("DOMParser" in window) ? new DOMParser() : new ActiveXObject("MSXML2.DOMDocument");
				xmlDoc.async = false;
			} catch (e) {
				throw new Error("XML Parser could not be instantiated");
			}

			if ("parseFromString" in xmlDoc) {
				out = xmlDoc.parseFromString(strXML, "text/xml");
				isParsed = (out.documentElement.tagName !== "parsererror");
			} else { //If old IE
				isParsed = xmlDoc.loadXML(strXML);
				out = (isParsed) ? xmlDoc : false;
			}
			if (!isParsed) {
				throw new Error("Error parsing XML string");
			}
			return out;
		}
	},


	/******************************************************************************
	 MEMBERS
	 ******************************************************************************/
	members: {
		prefixAttr: false,
		toLower: true,
		withText: false,
		withRoot: false,
		isXML: function (o) {
			return (typeof(o) === "object" && o.nodeType !== undefined);
		},
		getRoot: function (doc) {
			return (doc.nodeType === this.constructor.NODE_TYPES.Root) ? doc.documentElement : (doc.nodeType === this.constructor.NODE_TYPES.Fragment) ? doc.firstChild : doc;
		},
		convert: function (xml) {
			var out = {},
				xdoc = typeof(xml) === "string" ? ms123.util.Xml2Json.parseXMLString(xml) : this.isXML(xml) ? xml : undefined,
				root;
			if (!xdoc) {
				throw new Error("Unable to parse XML");
			}
			//If xdoc is just a text or CDATA return value
			if (xdoc.nodeType === this.constructor.NODE_TYPES.Text || xdoc.nodeType === this.constructor.NODE_TYPES.CDATA) {
				return xdoc.nodeValue;
			}
			root = this.getRoot(xdoc);
			var rootName = this.toLower ? root.nodeName.toLowerCase() : root.nodeName;
			out[rootName] = {};
			this.process(root, out[rootName]);
			return this.withRoot ? out : out[rootName];
		},
		process: function (node, buff) {
			var child, attr, name, att_name, value, i, j, tmp, iMax, jMax;
			if (node.hasChildNodes()) {
				iMax = node.childNodes.length;
				for (i = 0; i < iMax; i++) {
					child = node.childNodes[i];
					//Check nodeType of each child node
					switch (child.nodeType) {
					case this.constructor.NODE_TYPES.Text:
						if( this.withText){
							buff.Text = buff.Text ? buff.Text + child.nodeValue.trim() : child.nodeValue.trim();
						}
						break;
					case this.constructor.NODE_TYPES.CDATA:
						if( this.withText){
							value = child[child.text ? "text" : "nodeValue"]; //IE attributes support
							buff.Text = buff.Text ? buff.Text + value : value;
						}
						break;
					case this.constructor.NODE_TYPES.Element:
						name = child.nodeName;
						if (this.toLower) {
							name = name.toLowerCase();
						}
						tmp = {};
						//Node name already exists in the buffer and it's a NodeSet
						if (name in buff) {
							if (buff[name].length) {
								this.process(child, tmp);
								buff[name].push(tmp);
							} else { //If node exists in the parent as a single entity
								this.process(child, tmp);
								buff[name] = [buff[name], tmp];
							}
						} else { //If node does not exist in the parent
							this.process(child, tmp);
							buff[name] = tmp;
						}
						break;
					}
				}
			}
			//Populate attributes
			if (node.attributes.length) {
				for (j = node.attributes.length - 1; j >= 0; j--) {
					attr = node.attributes[j];
					att_name = attr.name.trim();
					if (this.toLower) {
						att_name = att_name.toLowerCase();
					}
					value = attr.value;
					buff[(this.prefixAttr ? "@" : "") + att_name] = value;
				}
			}
		}
	}
});
