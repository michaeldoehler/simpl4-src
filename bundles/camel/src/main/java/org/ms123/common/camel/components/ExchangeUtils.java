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
package org.ms123.common.camel.components;

import java.util.HashMap;
import java.util.Map;
import org.apache.camel.Exchange;
import flexjson.*;

/**
 * 
 */
public class ExchangeUtils {
	public static final String CAMELBODY = "camelBody";
	private static JSONSerializer m_js = new JSONSerializer();
	protected static final String IGNORE_MESSAGE_PROPERTY = "CamelMessageHistory";

	/**
	 */
	public static Map<String, Object> prepareVariables(Exchange exchange, boolean shouldCopyVariablesFromHeader, boolean shouldCopyVariablesFromProperties, boolean shouldCopyCamelBodyToBodyAsString) {
		Map<String, Object> camelVarMap = null;
		if (shouldCopyVariablesFromProperties) {
			camelVarMap = exchange.getProperties();
			Map<String, Object> newCamelVarMap = new HashMap<String, Object>();
			for (String s : camelVarMap.keySet()) {
				if (IGNORE_MESSAGE_PROPERTY.equalsIgnoreCase(s) == false) {
					newCamelVarMap.put(s, camelVarMap.get(s));
				}
			}
			camelVarMap = newCamelVarMap;
		} else {
			camelVarMap = new HashMap<String, Object>();
			Object camelBody = null;
			if (exchange.hasOut())
				camelBody = exchange.getOut().getBody();
			else
				camelBody = exchange.getIn().getBody();
			if (camelBody instanceof Map<?, ?>) {
				Map<?, ?> camelBodyMap = (Map<?, ?>) camelBody;
				for (@SuppressWarnings("rawtypes") Map.Entry e : camelBodyMap.entrySet()) {
					if (e.getKey() instanceof String) {
						camelVarMap.put((String) e.getKey(), e.getValue());
					}
				}
			} else {
				if (shouldCopyCamelBodyToBodyAsString && !(camelBody instanceof String)) {
					camelBody = exchange.getContext().getTypeConverter().convertTo(String.class, exchange, camelBody);
				}
				camelVarMap.put(CAMELBODY, camelBody);
			}
			if (shouldCopyVariablesFromHeader) {
				for (Map.Entry<String, Object> header : exchange.getIn().getHeaders().entrySet()) {
					camelVarMap.put(header.getKey(), header.getValue());
				}
			}
		}
		return camelVarMap;
	}
	public static String prepareBody(Exchange exchange) {
		Map<String,Object> camelBodyMap = new HashMap<String, Object>();
		Object camelBody = null;
		if (exchange.hasOut()){
			camelBody = exchange.getOut().getBody();
		}else{
			camelBody = exchange.getIn().getBody();
		}
		if (camelBody != null && !(camelBody instanceof String)) {
			camelBody = m_js.deepSerialize(camelBody);
		} 
		return (String)camelBody;
	}
}
