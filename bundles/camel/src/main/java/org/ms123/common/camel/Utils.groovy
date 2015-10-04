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
package org.ms123.common.camel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Collections;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import static org.ms123.common.camel.api.CamelService.PROPERTIES;
import static org.ms123.common.camel.api.CamelService.RPC;
import static org.ms123.common.camel.api.CamelService.PROCEDURENAME;
import static org.ms123.common.camel.api.CamelService.OVERRIDEID;
import static org.ms123.common.camel.api.CamelService.RESOURCEID;

@SuppressWarnings("unchecked")
@groovy.transform.CompileStatic
@groovy.transform.TypeChecked
public class Utils {
	public static String createRouteId( String baseId, int index){
		baseId = baseId.replace(".camel","");
		return baseId+":"+index;
	}
	public static String getBaseRouteId( String routeId){
		if( !routeId.matches('^.*:\\d{1,3}$')){
			return routeId;
		}
		int ind = routeId.lastIndexOf(":");
		return routeId.substring(0,ind);
	}
	public static  String getId(Map shape) {
		Map properties = (Map) shape.get(PROPERTIES);
		String id = ((String) properties.get(OVERRIDEID));
		if( id == null || id.trim().length()==0){
			id = (String)shape.get(RESOURCEID);
		}
		id = id.replace(".camel","");
		return id;
	}
}
