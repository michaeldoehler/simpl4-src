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
package org.ms123.common.camel.api;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Endpoint;
import org.apache.camel.Route;
import java.util.*;

public interface CamelService {
	public final String CAMEL_SERVICE = "camelService";
	public final String DEFAULT_CONTEXT = "default";
	public final String PROPERTIES = "properties";
	public final String PROCEDURENAME = "urivalue_name";
	public final String RPC = "rpc";
	public final String OVERRIDEID = "overrideid";
	public final String CAMEL_TYPE = "sw.camel";
	public CamelContext getCamelContext(String namespace, String name);
	public Map getShapeByRouteId(String namespace, String routeId);
	public List<Map<String,Object>> getProcedureShapesForPrefix(String prefix);
	public Map getProcedureShape(String namespace, String procedureName);
	public void saveHistory(Exchange exchange);
	public Object camelSend(String epUri, final Map<String, Object> properties);
	public Object camelSend(String epUri, final Object body, final Map<String, Object> properties);
	public Object camelSend(String epUri, final Object body, final Map<String, Object> headers, final Map<String, Object> properties);
	public Object camelSend(String ns, Endpoint endpoint, final Object body, final Map<String, Object> headers, final Map<String, Object> properties);
	public Object camelSend(String ns, Endpoint endpoint, final Object body, final Map<String, Object> headers, final Map<String, Object> properties, String returnSpec,List<String> returnHeaderList);
	public Object camelSend(String ns, String routeName,Map<String, Object> properties);
	public Object camelSend(String ns, String routeName,Object body, Map<String, Object> headers, Map<String, Object> properties);
}
