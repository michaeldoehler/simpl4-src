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
package org.ms123.common.reporting;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 *
 */
interface  MetaData {
	public final String REPORT_USER_PATH = "reports/{0}/{1}";
	public final String REPORTS_USER_PATH = "reports/{0}";

	public final String REPORT_PATH = "reports/{0}";
	public final String REPORTS_PATH = "reports";
	public final String REPORT_TYPE = "sw.report";
	public final String REPORTS_TYPE = "sw.reports";


	public List<Map> getReports(String namespace) throws Exception;
	public Map<String,List>  getReport(String namespace, String name) throws Exception;
	public void saveReport(String namespace, String name, Map<String,List> desc) throws Exception;
	public void deleteReport(String namespace, String name) throws Exception;
}
