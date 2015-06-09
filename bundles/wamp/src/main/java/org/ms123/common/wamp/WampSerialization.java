/*
 * Copyright 2014 Matthias Einwag
 *
 * The jawampa authors license this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.ms123.common.wamp;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Possible serialization methods for WAMP
 */
public enum WampSerialization {
	Json("wamp.2.json", new ObjectMapper());

	private final String stringValue;
	private final ObjectMapper objectMapper;

	WampSerialization(String stringValue, ObjectMapper objectMapper) {
		this.stringValue = stringValue;
		this.objectMapper = objectMapper;
	}

	public ObjectMapper getObjectMapper() {
		return objectMapper;
	}

	public static WampSerialization getJson() {
		return Json;
	}

}

