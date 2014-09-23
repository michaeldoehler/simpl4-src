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

import java.util.List;
import java.util.Map;
import java.io.File;
import org.ms123.common.rpc.RpcException;

public interface GitService {

	public List getRepositories() throws RpcException;

	public List getRepositories(List<String> flags) throws RpcException;
	public List getRepositories(List<String> flags, boolean all) throws RpcException;

	public void createRepository(String name) throws RpcException;

	public void cloneRepository(String name, String fromUri) throws RpcException;

	public void deleteRepository(String name) throws RpcException;

	public void commitAll(String name, String message) throws RpcException;

	public void push(String name) throws RpcException;

	public void pull(String name) throws RpcException;

	public void add(String name, String pattern) throws RpcException;

	public String getFileContent(String repoName, String path) throws Exception;

	public boolean exists( String repoName, String path) throws RpcException;
	public String getContent(String repoName, String path) throws RpcException;
	public String searchContent(String repoName, String name, String type) throws RpcException;
	public File searchFile(String repoName, String name, String type) throws RpcException;

	public Map getContentCheckRaw(String repoName, String path) throws RpcException;

	public void putContent(String repoName, String path, String type, String content) throws RpcException;

	public Map getWorkingTree(String repoName, String path, Integer depth, List<String> includeTypeList, List<String> includePathList, List<String> excludePathList, Map mapping) throws RpcException;

	public void deleteObject(String repoName, String path) throws RpcException;
	public void deleteObjects(String repoName, String directory,String regex) throws RpcException;
	public List<String> assetList( String repoName,  String name,  String type, Boolean onlyFirst) throws RpcException;
	public FileHolderApi getFileHolder( String repoName, String path);
	public void addRemoteOrigin(String repo, String url);

}
