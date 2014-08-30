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
package org.ms123.common.git;

import java.util.Set;

public interface FileHolderApi {

	public int getInt(final String section, final String name, final int defaultValue);

	public int getInt(final String section, String subsection, final String name, final int defaultValue);

	public long getLong(String section, String name, long defaultValue);

	public long getLong(final String section, String subsection, final String name, final long defaultValue);

	public boolean getBoolean(final String section, final String name, final boolean defaultValue);

	public boolean getBoolean(final String section, String subsection, final String name, final boolean defaultValue);

	public String getString(final String section, String subsection, final String name);

	public Set<String> getNames(String section);

	public void setInt(final String section, final String subsection, final String name, final int value);

	public void setLong(final String section, final String subsection, final String name, final long value);

	public void setBoolean(final String section, final String subsection, final String name, final boolean value);

	public void setString(final String section, final String subsection, final String name, final String value);
	public String getContent() throws Exception;

	public void putContent(String content) throws Exception; 

	public void putContent(String type, String content) throws Exception;
	public String getType() throws Exception;
}
