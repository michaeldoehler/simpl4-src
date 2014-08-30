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
package org.ms123.common.docbook.rendering;

import org.ms123.common.docbook.rendering.eval.ExpressionEvaluators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;
import java.util.Map;

public class ExpressionEvaluatingXMLReader extends XMLFilterImpl {

	private static final Logger log = LoggerFactory.getLogger(ExpressionEvaluatingXMLReader.class);

	private Map<String, Object> piContext;

	public ExpressionEvaluatingXMLReader(XMLReader parent, Map<String, Object> piContext) {
		super(parent);
		this.piContext = piContext;
	}

	@Override
	public void processingInstruction(String target, String data) throws SAXException {
		ExpressionEvaluators e = ExpressionEvaluators.lookup(target);
		if (e != null) {
			System.out.println("Processing instruction for target = {}, expression = {}" + target + "|" + data);
			String result = e.getEvaluator().evaluate(data, piContext);
			if (result != null) {
				char[] resultArray = result.toString().toCharArray();
				this.characters(resultArray, 0, resultArray.length);
			}
			return;
		}
		super.processingInstruction(target, data);
	}
}
