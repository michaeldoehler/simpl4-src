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
import java.io.IOException;
import java.util.List;

import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.*;


/**
 * Includes tree entries only if they match the configured path.
 */
public class PathPatternFilter extends TreeFilter {
	private final Matcher m_compiledPattern;
	private List<String> m_includePathList;
	private List<String> m_excludePathList;

	/**
	 * Create a new tree filter for a user supplied path.
	 * <p>
	 * Path strings use '/' to delimit directories on all platforms.
	 *
	 * @param path
	 *            the path (suffix) to filter on. Must not be the empty string.
	 * @return a new filter for the requested path.
	 * @throws IllegalArgumentException
	 *             the path supplied was the empty string.
	 */
	public static PathPatternFilter create(String pattern,
				List<String> includePathList, 
				List<String> excludePathList, 
				int flags) {
		if (pattern.length() == 0) {
			throw new IllegalArgumentException(JGitText.get().emptyPathNotPermitted);
		}
		return new PathPatternFilter(pattern,includePathList, excludePathList, flags);
	}

	private PathPatternFilter(final String pattern,
			List<String> includePathList,
			List<String> excludePathList,
			 int flags) {
		if (pattern.length() == 0) {
			throw new IllegalArgumentException(JGitText.get().cannotMatchOnEmptyString);
		}
		m_includePathList = includePathList;
		m_excludePathList = excludePathList;
		/*	if (!pattern.startsWith("^") && !pattern.startsWith(".*")) {
				pattern = ".*" + pattern;
			}
			if (!pattern.endsWith("$") && !pattern.endsWith(".*")) {
				pattern = pattern + ".*";
			}*/
		m_compiledPattern = Pattern.compile(pattern, flags).matcher("");
	}

	@Override
	public TreeFilter clone() {
		return this;
	}

	@Override
	public boolean include(TreeWalk walker) throws MissingObjectException, IncorrectObjectTypeException, IOException {
		//if (walker.isSubtree()) {
		//	return true;
		//} else {
			String path = walker.getPathString();
			String a[] = path.split("/");
			boolean b = m_compiledPattern.reset(a[a.length-1]).matches();
			if( b){
				if( path.startsWith("store")){
					b=false;
				}
				if( path.startsWith("settings")){
					b=false;
				}
				if( path.startsWith("roles")){
					b=false;
				}
				if( path.startsWith("data_description/admin")){
					b=false;
				}
				if( path.startsWith("data_description/aid")){
					b=false;
				}
				if( b && m_includePathList != null){
					b = false;
					for( String incPath : m_includePathList){
						if( path.startsWith(incPath)){
							b = true;
							break;
						}
					}
				}
				if( b && m_excludePathList != null){
					for( String exPath : m_excludePathList){
						if( path.startsWith(exPath)){
							b = false;
							break;
						}
					}
				}
			}
			return b;
		//}
	}

	@Override
	public boolean shouldBeRecursive() {
		return true;
	}

}
