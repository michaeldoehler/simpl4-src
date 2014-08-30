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
package org.ms123.common.camel.jsonconverter;

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

/**
 *
 */
@SuppressWarnings("unchecked")
public class BaseRouteJsonConverter {

	protected JSONDeserializer m_ds = new JSONDeserializer();
	protected JSONSerializer m_js = new JSONSerializer();

	BaseRouteJsonConverter() {
	}


	protected String getStencilId(Map shape) {
		Map stencil = (Map) shape.get("stencil");
		String id = ((String) stencil.get("id")).toLowerCase();
		return id;
	}

	protected boolean isOnException(Map shape) {
		Map stencil = (Map) shape.get("stencil");
		String id = ((String) stencil.get("id")).toLowerCase();
		return id.equals("onexception");
	}
	protected boolean isOnCompletion(Map shape) {
		Map stencil = (Map) shape.get("stencil");
		String id = ((String) stencil.get("id")).toLowerCase();
		return id.equals("oncompletion");
	}
	protected boolean isFrom(Map shape) {
		return !isOnException(shape);
	}

	protected void sortStartShapeList(List<Map> shapes) {
		if( isFrom(shapes.get(0))) return;
		int i=0;
		for( Map shape : shapes){
			if( isFrom(shape)){
				Map temp = shapes.get(0);
				shapes.set(0, shapes.get(i));
				shapes.set(i,temp);
				return;
			}
			i++;
		}
	}

	protected boolean isStartShapeListOk(List<Map> shapes) {
		int cFrom=0;
		int cOnException =0;
		int cOnCompletion =0;
		for(Map shape : shapes){
			if( isFrom(shape)) cFrom++;
			if( isOnException(shape)) cOnException++;
			if( isOnCompletion(shape)) cOnCompletion++;
		}
		return cFrom == 1 && cOnException<2 && cOnCompletion<2;
	}

	protected String getId(Map shape) {
		Map properties = (Map) shape.get("properties");
		String id = ((String) properties.get("overrideid"));
		if( id == null || id.length()==0){
			id = (String)shape.get("resourceId");
		}
		return id;
	}

	protected List<Map> getShapeList(List<Map> list, List<String> types) {
		List<Map> retList = new ArrayList();
		for (Map<String, Map> e : list) {
			String sid = getStencilId(e);
			if (types.contains(sid)) {
				retList.add(e);
			}
		}
		return retList;
	}

	protected List<Map> getShapeList(List<Map> list, String type) {
		List<Map> retList = new ArrayList();
		for (Map<String, Map> e : list) {
			String sid = getStencilId(e);
			if (type.toLowerCase().equals(sid)) {
				retList.add(e);
			}
		}
		return retList;
	}

	protected Map getShapeSingle(List<Map> list, String type) {
		for (Map<String, Map> e : list) {
			String sid = getStencilId(e);
			if (type.toLowerCase().equals(sid)) {
				return e;
			}
		}
		return null;
	}

	protected boolean getBoolean(Map properties, String key, boolean def) {
		try {
			if (properties.get(key) == null)
				return def;
			return (Boolean) properties.get(key);
		} catch (Exception e) {
			return def;
		}
	}

	protected int getInteger(Map properties, String key, int def) {
		try {
			if (properties.get(key) == null)
				return def;
			return ((Integer) properties.get(key)).intValue();
		} catch (Exception e) {
			return def;
		}
	}

	protected String getString(Map properties, String key, String def) {
		try {
			String val = (String) properties.get(key);
			if (val == null || "".equals(val.trim())) {
				return def;
			}
			return val;
		} catch (Exception e) {
			return def;
		}
	}

	protected boolean isEmpty(String str) {
		if (str == null || str.length() == 0)
			return true;
		return false;
	}
}
