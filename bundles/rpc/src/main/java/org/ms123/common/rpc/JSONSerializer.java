/*
 Java JSON RPC
 RPC Java POJO by Novlog
 http://www.novlog.com

 This library is dual-licensed under the GNU Lesser General Public License (LGPL) and the Eclipse Public License (EPL).
 Check http://qooxdoo.org/license

 This library is also licensed under the Apache license.
 Check http://www.apache.org/licenses/LICENSE-2.0

 Contribution:
 This contribution is provided by Novlog company.
 http://www.novlog.com
 */
package org.ms123.common.rpc;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.*;

public class JSONSerializer {

	/**
	 * Converts a JSON String into a Map (for objects), a List (for arrays) or a String (for simple values).
	 *
	 * @param jsonStr A String in JSON
	 * @return A Map, a List or a String
	 * @throws JSONException
	 */
	public Object unserialize(final String jsonStr) throws JSONException {
		Object result = null;
		if (jsonStr != null) {
			if (jsonStr.startsWith("{")) {
				result = unserializeObject(jsonStr);
			} else if (jsonStr.startsWith("[")) {
				result = unserializeArray(jsonStr);
			} else {
				result = jsonStr;
			}
		}
		return result;
	}

	/**
	 * Converts a JSON object into a Map.
	 *
	 * @param jsonStr A String which must be a JSON object, i.e. "{ key: value, ... }"
	 * @return A map representing the JSON object. The map keys are the attributes names, and the associated values are the attributes values.
	 * @throws JSONException
	 */
	public Map<String, Object> unserializeObject(final String jsonStr) throws JSONException {
		Map<String, Object> result = null;
		if (jsonStr != null) {
			result = JSONObjectToJava(new JSONObject(jsonStr));
		}
		return result;
	}

	/**
	 * Converts a JSON array into a List.
	 *
	 * @param jsonStr A String which must be a JSON array, i.e. "[ elt1, elt2, ... ]"
	 * @return A List representing the JSON object, and containing Strings, Maps, or Lists depending on the elements types (simple type, object, or arrays).
	 * @throws JSONException
	 */
	public List<Object> unserializeArray(final String jsonStr) throws JSONException {
		return JSONArrayToJava(new JSONArray(jsonStr));
	}

	protected Map<String, Object> JSONObjectToJava(final JSONObject jsonObject) {
		HashMap<String, Object> result = null;
		if (jsonObject != null) {
			result = new HashMap<String, Object>();
			if (JSONObject.getNames(jsonObject) != null) {
				for (final String key : JSONObject.getNames(jsonObject)) {
					final Object value = jsonObject.opt(key);
					if (value instanceof JSONObject) {
						result.put(key, JSONObjectToJava(jsonObject.optJSONObject(key)));
					} else if (value instanceof JSONArray) {
						result.put(key, JSONArrayToJava(jsonObject.optJSONArray(key)));
					} else if (value == JSONObject.NULL) {
						result.put(key, null);
					} else {
						result.put(key, jsonObject.opt(key));
					}
				}
			}
		}
		return result;
	}

	protected List<Object> JSONArrayToJava(final JSONArray jsonArray) {
		List<Object> result = null;
		if (jsonArray != null) {
			result = new ArrayList<Object>(jsonArray.length());
			for (int i = 0; i < jsonArray.length(); i++) {
				final Object elt = jsonArray.opt(i);
				if (elt instanceof JSONObject) {
					result.add(i, JSONObjectToJava(jsonArray.optJSONObject(i)));
				} else if (elt instanceof JSONArray) {
					result.add(i, JSONArrayToJava(jsonArray.optJSONArray(i)));
				} else if (elt == JSONObject.NULL) {
					result.add(null);
				} else {
					result.add(i, jsonArray.opt(i));
				}
			}
		}
		return result;
	}

	/**
	 * Converts a map into a JSON string.
	 *
	 * @param map The map to convert. The keys are the attributes names, and the values the attributes values.
	 * @return The JSON object string.
	 */
	public String serialize(final Map<String, Object> map) {
		String result = null;
		if (map != null) {
			result = new JSONObject(map).toString();
		}
		return result;
	}

	/**
	 * Converts a collection into a JSON array.
	 *
	 * @param collection The collection to convert
	 * @return the JSON array string
	 */
	public String serialize(final Collection<Object> collection) {
		String result = null;
		if (collection != null) {
			result = new JSONArray(collection).toString();
		}
		return result;
	}
}
