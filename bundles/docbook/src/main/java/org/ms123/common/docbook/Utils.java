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
package org.ms123.common.docbook;

import flexjson.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.Writer;
import java.io.Reader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.StringWriter;
import java.io.StringReader;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import nu.xom.*;
import org.apache.commons.lang.StringUtils;

/**
 *
 */
public class Utils {

	protected void printlist(String header, List<Map> list) {
		System.out.println("----->" + header);
		for (Map m : list) {
			Map properties = (Map) m.get("properties");
			System.out.println("\t" + m.get("bounds") + "/" + properties.get("xf_id"));
		}
	}

	public static void setNamespace(Element e) {
		String ns = "http://www.w3.org/1999/xhtml";
		setNamespace(e, ns);
	}

	public static void setNamespace(Element e, String namespace) {
		e.setNamespaceURI(namespace);
		XPathContext pc = new XPathContext();
		Nodes nodes = e.query("//*", pc);
		for (int i = 0; i < nodes.size(); i++) {
			Node n = nodes.get(i);
			if (n instanceof Element) {
				System.out.println("setNamespace:" + n);
				((Element) n).setNamespaceURI(namespace);
			}
		}
	}

	public static void escImageSrc(Element e) {
		XPathContext pc = new XPathContext();
		Nodes nodes = e.query("//img", pc);
		for (int i = 0; i < nodes.size(); i++) {
			Node n = nodes.get(i);
			if (n instanceof Element) {
				String src = ((Element) n).getAttribute("src").getValue();
				((Element) n).getAttribute("src").setValue(src.replace(":", "%3A"));
			}
		}
	}

	public static String xomToString(Element element) {
		try {
			Document doc = new Document(element);
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			Serializer ser = new Serializer(bos) {

				protected void writeXMLDeclaration() {
					System.out.println("writeXMLDeclaration");
				}
			};
			ser.write(doc);
			return bos.toString("utf-8");
		} catch (Exception e) {
			throw new RuntimeException("WebsiteBuilder.xomToString:", e);
		} finally {
		}
	}
	public static String tocToJson(Element element) {
		try {
			Document doc = new Document(element);
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			Serializer ser = new Serializer(bos) {

				protected void writeXMLDeclaration() {
					System.out.println("writeXMLDeclaration");
				}
			};
			ser.write(doc);
			return bos.toString("utf-8");
		} catch (Exception e) {
			throw new RuntimeException("WebsiteBuilder.xomToString:", e);
		} finally {
		}
	}
	public final static String clearName(String name, boolean stripDots, boolean ascii) {
		String temp = name;
		temp = temp.replaceAll(
				"[\u00c0\u00c1\u00c2\u00c3\u00c4\u00c5\u0100\u0102\u0104\u01cd\u01de\u01e0\u01fa\u0200\u0202\u0226]", "A");
		temp = temp.replaceAll(
				"[\u00e0\u00e1\u00e2\u00e3\u00e4\u00e5\u0101\u0103\u0105\u01ce\u01df\u01e1\u01fb\u0201\u0203\u0227]", "a");
		temp = temp.replaceAll("[\u00c6\u01e2\u01fc]", "AE");
		temp = temp.replaceAll("[\u00e6\u01e3\u01fd]", "ae");
		temp = temp.replaceAll("[\u008c\u0152]", "OE");
		temp = temp.replaceAll("[\u009c\u0153]", "oe");
		temp = temp.replaceAll("[\u00c7\u0106\u0108\u010a\u010c]", "C");
		temp = temp.replaceAll("[\u00e7\u0107\u0109\u010b\u010d]", "c");
		temp = temp.replaceAll("[\u00d0\u010e\u0110]", "D");
		temp = temp.replaceAll("[\u00f0\u010f\u0111]", "d");
		temp = temp.replaceAll("[\u00c8\u00c9\u00ca\u00cb\u0112\u0114\u0116\u0118\u011a\u0204\u0206\u0228]", "E");
		temp = temp.replaceAll("[\u00e8\u00e9\u00ea\u00eb\u0113\u0115\u0117\u0119\u011b\u01dd\u0205\u0207\u0229]", "e");
		temp = temp.replaceAll("[\u011c\u011e\u0120\u0122\u01e4\u01e6\u01f4]", "G");
		temp = temp.replaceAll("[\u011d\u011f\u0121\u0123\u01e5\u01e7\u01f5]", "g");
		temp = temp.replaceAll("[\u0124\u0126\u021e]", "H");
		temp = temp.replaceAll("[\u0125\u0127\u021f]", "h");
		temp = temp.replaceAll("[\u00cc\u00cd\u00ce\u00cf\u0128\u012a\u012c\u012e\u0130\u01cf\u0208\u020a]", "I");
		temp = temp.replaceAll("[\u00ec\u00ed\u00ee\u00ef\u0129\u012b\u012d\u012f\u0131\u01d0\u0209\u020b]", "i");
		temp = temp.replaceAll("[\u0132]", "IJ");
		temp = temp.replaceAll("[\u0133]", "ij");
		temp = temp.replaceAll("[\u0134]", "J");
		temp = temp.replaceAll("[\u0135]", "j");
		temp = temp.replaceAll("[\u0136\u01e8]", "K");
		temp = temp.replaceAll("[\u0137\u0138\u01e9]", "k");
		temp = temp.replaceAll("[\u0139\u013b\u013d\u013f\u0141]", "L");
		temp = temp.replaceAll("[\u013a\u013c\u013e\u0140\u0142\u0234]", "l");
		temp = temp.replaceAll("[\u00d1\u0143\u0145\u0147\u014a\u01f8]", "N");
		temp = temp.replaceAll("[\u00f1\u0144\u0146\u0148\u0149\u014b\u01f9\u0235]", "n");
		temp = temp.replaceAll(
				"[\u00d2\u00d3\u00d4\u00d5\u00d6\u00d8\u014c\u014e\u0150\u01d1\u01ea\u01ec\u01fe\u020c\u020e\u022a\u022c"
				+ "\u022e\u0230]", "O");
		temp = temp.replaceAll(
				"[\u00f2\u00f3\u00f4\u00f5\u00f6\u00f8\u014d\u014f\u0151\u01d2\u01eb\u01ed\u01ff\u020d\u020f\u022b\u022d"
				+ "\u022f\u0231]", "o");
		temp = temp.replaceAll("[\u0156\u0158\u0210\u0212]", "R");
		temp = temp.replaceAll("[\u0157\u0159\u0211\u0213]", "r");
		temp = temp.replaceAll("[\u015a\u015c\u015e\u0160\u0218]", "S");
		temp = temp.replaceAll("[\u015b\u015d\u015f\u0161\u0219]", "s");
		temp = temp.replaceAll("[\u00de\u0162\u0164\u0166\u021a]", "T");
		temp = temp.replaceAll("[\u00fe\u0163\u0165\u0167\u021b\u0236]", "t");
		temp = temp.replaceAll(
				"[\u00d9\u00da\u00db\u00dc\u0168\u016a\u016c\u016e\u0170\u0172\u01d3\u01d5\u01d7\u01d9\u01db\u0214\u0216]",
				"U");
		temp = temp.replaceAll(
				"[\u00f9\u00fa\u00fb\u00fc\u0169\u016b\u016d\u016f\u0171\u0173\u01d4\u01d6\u01d8\u01da\u01dc\u0215\u0217]",
				"u");
		temp = temp.replaceAll("[\u0174]", "W");
		temp = temp.replaceAll("[\u0175]", "w");
		temp = temp.replaceAll("[\u00dd\u0176\u0178\u0232]", "Y");
		temp = temp.replaceAll("[\u00fd\u00ff\u0177\u0233]", "y");
		temp = temp.replaceAll("[\u0179\u017b\u017d]", "Z");
		temp = temp.replaceAll("[\u017a\u017c\u017e]", "z");
		temp = temp.replaceAll("[\u00df]", "SS");
		temp = temp.replaceAll("[_':,;\\\\/]", " ");
		name = temp;
		name = name.replaceAll("\\s+", "");
		name = name.replaceAll("[\\(\\)]", " ");

		if (stripDots) {
			name = name.replaceAll("[\\.]", "");
		}

		if (ascii) {
			name = name.replaceAll("[^a-zA-Z0-9\\-_\\.]", "");
		}

		if (name.length() > 250) {
			name = name.substring(0, 250);
		}

		return name;
	}
}
