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


import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.io.File;
import java.io.RandomAccessFile;
import java.io.IOException;

import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.*;


/**
 * Includes tree entries only if they match the configured type.
 */
public class TypeFilter extends TreeFilter {
	private final List<String> m_typeList;
	private File m_basedir;

	/**
	 */
	public static TypeFilter create(File base,List<String> typeList) {
		if (typeList.size() == 0) {
			throw new IllegalArgumentException(JGitText.get().emptyPathNotPermitted);
		}
		return new TypeFilter(base,typeList);
	}

	private TypeFilter(File basedir,final List<String> typeList) {
		if (typeList.size() == 0) {
			throw new IllegalArgumentException(JGitText.get().cannotMatchOnEmptyString);
		}
		m_typeList = typeList;
		m_basedir = basedir;
	}

	@Override
	public TreeFilter clone() {
		return this;
	}

	@Override
	public boolean include(TreeWalk walker) throws MissingObjectException, IncorrectObjectTypeException, IOException {
		String path = walker.getPathString();
		File file = new File(m_basedir,path);
		FileHolder fr = new FileHolder( new File(m_basedir,path) );
		try{
			String type = GitServiceImpl.getFileType(file);
			boolean b = m_typeList.contains(type);
			return b;
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean shouldBeRecursive() {
		return true;
	}

}
