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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Allows to configure realms that are exposed by routers
 */
public class Subscription {
		final String topic;
		final SubscriptionFlags flags;
		final String components[]; // non-null only for wildcard type
		final long subscriptionId;
		final Set<WampServiceImpl.WebSocket> subscribers;

		public Subscription(String topic, SubscriptionFlags flags, long subscriptionId) {
			this.topic = topic;
			this.flags = flags;
			this.components = flags == SubscriptionFlags.Wildcard ? topic.split("\\.", -1) : null;
			this.subscriptionId = subscriptionId;
			this.subscribers = new HashSet<WampServiceImpl.WebSocket>();
		}

}
