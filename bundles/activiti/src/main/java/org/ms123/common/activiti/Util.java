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
package org.ms123.common.activiti;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

/**
 */
public class Util {

	private static final SimpleDateFormat shortDateFormat = new SimpleDateFormat("yyyy-MM-dd");

	private static final SimpleDateFormat longDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz");

	private static String m_datePattern = "dd.MM.yyyy HH:mm";

	public static boolean getBoolean(Map<String, Object> params, String name, boolean defaultValue) {
		boolean value = defaultValue;
		if (params.get(name) != null) {
			value = (Boolean) params.get(name);
		}
		return value;
	}

	public static int getInteger(Map<String, Object> params, String name, int defaultValue) {
		int value = defaultValue;
		if (params.get(name) != null) {
			value = (Integer) params.get(name);
		}
		return value;
	}

	public static String getString(Map<String, Object> params, String name) {
		return (String) params.get(name);
	}

	public static String getString(Map<String, Object> params, String name, String defaultValue) {
		String value = defaultValue;
		if (params.get(name) != null) {
			value = (String) params.get(name);
		}
		return value;
	}

	public static Date getDate(Map<String, Object> params, String name) {
		Date value = null;
		if (params.get(name) != null) {
			String input = (String) params.get(name);
			//this is zero time so we need to add that TZ indicator for 
			if (input.endsWith("Z")) {
				input = input.substring(0, input.length() - 1) + "GMT-00:00";
			} else {
				int inset = 6;
				String s0 = input.substring(0, input.length() - inset);
				String s1 = input.substring(input.length() - inset, input.length());
				input = s0 + "GMT" + s1;
			}
			try {
				value = longDateFormat.parse(input);
			} catch (Exception e) {
				throw new RuntimeException("Failed to parse date " + input);
			}
		}
		return value;
	}

	public static String dateToString(Date date) {
		String dateString = null;
		if (date != null) {
//			dateString = longDateFormat.format(date);
			SimpleDateFormat sdf = new SimpleDateFormat(m_datePattern);
			sdf.setTimeZone(TimeZone.getTimeZone("Europe/Berlin"));
			dateString = sdf.format(date);
		}
		return dateString;
	}

	public static Integer parseToInteger(String integer) {
		Integer parsedInteger = null;
		try {
			parsedInteger = Integer.parseInt(integer);
		} catch (Exception e) {
		}
		return parsedInteger;
	}

	public static Date parseToDate(String date) {
		Date parsedDate = null;
		try {
			parsedDate = shortDateFormat.parse(date);
		} catch (Exception e) {
		}
		return parsedDate;
	}
}
