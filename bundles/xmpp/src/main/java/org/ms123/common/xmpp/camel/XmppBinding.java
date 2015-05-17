/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ms123.common.xmpp.camel;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultHeaderFilterStrategy;
import org.apache.camel.spi.HeaderFilterStrategy;
import org.apache.camel.util.ObjectHelper;
import org.jivesoftware.smack.packet.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Strategy used to convert between a Camel {@link Exchange} and {@link XmppMessage} to and from a
 * XMPP {@link Message}
 *
 * @version 
 */
public class XmppBinding {

	private static final Logger LOG = LoggerFactory.getLogger(XmppBinding.class);

	private HeaderFilterStrategy headerFilterStrategy;

	public XmppBinding() {
		this.headerFilterStrategy = new DefaultHeaderFilterStrategy();
	}

	public XmppBinding(HeaderFilterStrategy headerFilterStrategy) {
		ObjectHelper.notNull(headerFilterStrategy, "headerFilterStrategy");
		this.headerFilterStrategy = headerFilterStrategy;
	}

	/**
     * Populates the given XMPP message from the inbound exchange
     */
	public void populateXmppMessage(Message message, Exchange exchange) {
		if( exchange.getIn().getBody() instanceof Map){
			message.setBody((String)exchange.getIn().getBody(Map.class).get("body"));
		}else{
			message.setBody(exchange.getIn().getBody(String.class));
		}
	}

	/**
     * Extracts the body from the XMPP message
     */
	public Object extractBodyFromXmpp(Exchange exchange, Message message) {
		return message.getBody();
	}

}
