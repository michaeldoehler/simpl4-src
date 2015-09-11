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
package org.ms123.common.rpc;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface CallService {

	public static String SERVICENAME = "service";
	public static String METHODNAME = "method";
	public static String METHODPARAMS = "params";
	public static String ACTION = "action";
	public static String SYNC = "sync";
	public static String AT = "at";
	public static String AT_BEFORE = "before";
	public static String AT_AFTER = "after";
	public static String METHODRESULT = "result";
	public static String PRECONDITION = "preCondition";
	public static String CAMELSERVICENAME = "camel-routing";
	public static String CAMELSERVICENAME2 = "camelRoute";

	public void callHooks(Map params);
	public Object callCamel(String methodName, Object methodParams, HttpServletRequest request, HttpServletResponse response);
	
}
