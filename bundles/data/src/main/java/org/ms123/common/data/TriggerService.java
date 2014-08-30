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
package org.ms123.common.data;

import java.util.Map;
import java.util.List;
import javax.jdo.PersistenceManager;
import org.ms123.common.data.api.SessionContext;

public interface TriggerService {

	// public Map updateObject(Map dataMap, Map filterMap, Map hintsMap, String appName, String module, String pathInfo, String user); 
	public int INSERT = 0;

	public int UPDATE = 1;

	public int DELETE = 2;

	public Map applyInsertRules(SessionContext sessionContext, String entityName, Object insert) throws Exception;

	public Map applyUpdateRules(SessionContext sessionContext, String entityName, Object update,Object preUpdate) throws Exception;

	public Map applyDeleteRules(SessionContext sessionContext, String entityName, Object delete) throws Exception;
}
