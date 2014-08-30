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
package org.ms123.common.message;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.io.Reader;

/**
 *
 */
interface MetaData {

	public final String MESSAGESLANG_PATH = "messages/{0}";

	public final String MESSAGES_PATH = "messages";

	public final String MESSAGESLANG_TYPE = "sw.messageslang";

	public final String MESSAGES_TYPE = "sw.messages";

	public List<Map> getLanguages(String namespace) throws Exception;

	public List<Map> getMessages(String namespace, String lang, Map filter) throws Exception;

	public Map<String, String> getMessage(String namespace, String lang, String id) throws Exception;

	public void saveMessage(String namespace, String lang, Map msg,boolean overwrite) throws Exception;

	public void addMessages(String namespace, String lang, List<Map> msgs,boolean overwrite) throws Exception;

	public void saveMessages(String namespace, String lang, List<Map> msgs) throws Exception;

	public void deleteMessages(String namespace, String lang,List<String> msgIds) throws Exception;

	public void deleteMessage(String namespace, String lang, String id) throws Exception;

	public List<Map> parseCSV(Reader reader) throws Exception;
}
