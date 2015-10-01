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
package org.ms123.common.system.history;
import java.util.List;
import java.util.Map;
import org.ms123.common.rpc.RpcException;

public interface HistoryService {
	public static String HISTORY_TOPIC  = 	"history";

	public static String HISTORY_TYPE = "type";
	public static String HISTORY_KEY = "key";
	public static String HISTORY_HINT = "hint";
	public static String HISTORY_MSG = "msg";
	public static String HISTORY_TIME = "time";

	public static String HISTORY_ACTIVITI_START_PROCESS_EXCEPTION = "activiti/startprocess/exception";
	public static String HISTORY_ACTIVITI_JOB_EXCEPTION = 	"activiti/job/exception";
	public static String HISTORY_ACTIVITI_STARTPROCESS_EXCEPTION = 	"activiti/startprocess/exception";
	public static String HISTORY_CAMEL_HISTORY = 	"camel/history";
	public static String HISTORY_CAMEL_TRACE = 	"camel/trace";


	public static String ACTIVITI_CAMEL_CORRELATION_TYPE = "activitiCamelCorrelation";
	public static String ACC_ACTIVITI_ID = "activitiId";
	public static String ACC_ROUTE_INSTANCE_ID = "routeInstanceId";

	public static String ACTIVITI_PROCESS_ID = "__activitiProcessId";
	public static String ACTIVITI_ACTIVITY_ID = "__activitiActivityId";
	public static String CAMEL_ROUTE_DEFINITION_ID = "__routeDefinitionId";
}
