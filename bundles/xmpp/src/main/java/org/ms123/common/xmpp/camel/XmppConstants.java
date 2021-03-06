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

/**
 * Constants used in Camel XMPP module
 *
 * @version 
 */
public interface XmppConstants {

	String SESSIONID = "xmppSessionId";
	String USERNAME = "xmppUsername";
	String PASSWORD = "xmppPassword";
	String RESOURCEID = "xmppResourceId";
	String PARTICIPANT = "participant";
	String NICKNAME = "nickname";
	String ROOM = "room";
	String COMMAND = "command";
	String PARAMETER = "parameter";
	String COMMAND_OPEN = "open";
	String COMMAND_CLOSE = "close";
	String COMMAND_ADDUSER = "addUser";
	String COMMAND_LEAVEROOM = "leaveRoom";
	String COMMAND_CHATSTATE = "chatState";
}
