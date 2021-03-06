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
package org.ms123.common.system.log;
import java.util.List;
import java.util.Map;
import org.ms123.common.rpc.RpcException;

public interface LogService {

	public static String LOG_KEY = "key";
	public static String LOG_TYPE = "type";
	public static String LOG_HINT = "hint";
	public static String LOG_MSG = "msg";
	public static String LOG_TIME = "time";
	public List<Map> getLog(String key, String type) throws Exception;
	public List<Map> getLog(String key, String type, String projection, Long startTime, Long endTime) throws Exception;
	public List<Map> getLog(String key, String type, String projection, String orderby, Long startTime, Long endTime) throws RpcException;
}
