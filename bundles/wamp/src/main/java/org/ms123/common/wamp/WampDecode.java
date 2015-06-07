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

import java.util.List;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ms123.common.wamp.WampMessages.WampMessage;

public class WampDecode {

	public static WampMessage decode(byte[] buffer) throws Exception {
		return decode(buffer, WampSerialization.getJson());
	}

	public static WampMessage decode(byte[] buffer, WampSerialization serialization) throws Exception {
		ObjectMapper objectMapper = serialization.getObjectMapper();

		ArrayNode arr = objectMapper.readValue(new ByteArrayInputStream(buffer), ArrayNode.class);

		m_logger.debug("Deserialized Wamp Message:" + arr.toString());

		WampMessage recvdMessage = WampMessage.fromObjectArray(arr);
		return recvdMessage;
	}

	public static String encode(WampMessage msg) {
		return encode(msg, WampSerialization.getJson());
	}

	public static String encode(WampMessage msg, WampSerialization serialization) {
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		ObjectMapper objectMapper = serialization.getObjectMapper();
		try {
			JsonNode node = msg.toObjectArray(objectMapper);
			objectMapper.writeValue(outStream, node);

			m_logger.debug("Serialized Wamp Message:" + objectMapper.writeValueAsString(node));

		} catch (Exception e) {
			e.printStackTrace();
			try{
				outStream.close();
			}catch(Exception ee){
				ee.printStackTrace();
			}
			return null;
		}

		if (serialization.isText()) {
		} else {
		}
		try{
			outStream.close();
			String s = outStream.toString("UTF-8");
			return s;
		}catch(Exception e){
			e.printStackTrace();
			throw new RuntimeException("WampDecode.encode:", e);
		}
	}

	protected static void debug(String msg) {
		System.err.println(msg);
		m_logger.debug(msg);
	}

	protected static void info(String msg) {
		System.err.println(msg);
		m_logger.info(msg);
	}

	private static final Logger m_logger = LoggerFactory.getLogger(WampDecode.class);
}

